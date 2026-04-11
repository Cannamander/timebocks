package com.timebox.app.ui.applist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timebox.app.data.repository.AppLimitRepository
import com.timebox.app.util.AppInfoHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppListItem(
    val packageName: String,
    val appName: String,
    val isLimited: Boolean,
    val dailyLimitMs: Long
)

data class AppListState(
    val apps: List<AppListItem> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = ""
)

sealed class AppListEvent {
    data class ShowTimePicker(val packageName: String, val appName: String) : AppListEvent()
}

@HiltViewModel
class AppListViewModel @Inject constructor(
    private val appLimitRepository: AppLimitRepository,
    private val appInfoHelper: AppInfoHelper
) : ViewModel() {

    private val _state = MutableStateFlow(AppListState())
    val state: StateFlow<AppListState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<AppListEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AppListEvent> = _events.asSharedFlow()

    private var installedApps: List<com.timebox.app.util.AppInfo> = emptyList()
    private var limitsByPackage: Map<String, Long> = emptyMap()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            installedApps = appInfoHelper.getInstalledUserApps()
            appLimitRepository.getAllLimits().collect { list ->
                limitsByPackage = list.associate { it.packageName to it.dailyLimitMs }
                emitApps()
            }
        }
    }

    private fun emitApps() {
        val q = _state.value.searchQuery.trim().lowercase()
        val items = installedApps.map { info ->
            val limitMs = limitsByPackage[info.packageName] ?: 0L
            AppListItem(
                packageName = info.packageName,
                appName = info.appName,
                isLimited = limitMs > 0L,
                dailyLimitMs = limitMs
            )
        }.filter {
            q.isEmpty() || it.appName.lowercase().contains(q) ||
                it.packageName.lowercase().contains(q)
        }
        _state.update { it.copy(apps = items, isLoading = false) }
    }

    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
        emitApps()
    }

    fun onAppSwitchChanged(packageName: String, appName: String, checked: Boolean) {
        if (checked) {
            viewModelScope.launch {
                _events.emit(AppListEvent.ShowTimePicker(packageName, appName))
            }
        } else {
            viewModelScope.launch {
                appLimitRepository.deleteLimit(packageName)
                limitsByPackage = limitsByPackage - packageName
                emitApps()
            }
        }
    }

    fun saveLimit(packageName: String, appName: String, limitMs: Long) {
        viewModelScope.launch {
            appLimitRepository.upsertLimit(packageName, appName, limitMs)
            limitsByPackage = limitsByPackage + (packageName to limitMs)
            emitApps()
        }
    }

    fun restoreLimit(packageName: String, appName: String, limitMs: Long) {
        saveLimit(packageName, appName, limitMs)
    }
}
