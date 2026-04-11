package com.timebox.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timebox.app.BuildConfig
import com.timebox.app.data.repository.AppLimitRepository
import com.timebox.app.service.ServiceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

data class SettingsState(
    val isMonitoring: Boolean = false,
    val totalAppsLimited: Int = 0,
    val appVersion: String = BuildConfig.VERSION_NAME
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appLimitRepository: AppLimitRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            appLimitRepository.getAllLimits().collect { limits ->
                _state.update {
                    it.copy(
                        totalAppsLimited = limits.size,
                        isMonitoring = ServiceManager.isRunning(appContext)
                    )
                }
            }
        }
    }

    fun refreshMonitoringState() {
        _state.update {
            it.copy(isMonitoring = ServiceManager.isRunning(appContext))
        }
    }

    fun toggleMonitoring(context: Context) {
        if (_state.value.isMonitoring) {
            ServiceManager.stopMonitoring(context)
        } else {
            ServiceManager.startMonitoring(context)
        }
        _state.update { it.copy(isMonitoring = ServiceManager.isRunning(context)) }
    }

    fun clearAllLimits() {
        viewModelScope.launch {
            appLimitRepository.clearAllLimits()
        }
    }

    suspend fun exportData(): String {
        val list = appLimitRepository.getAllLimits().first()
        val arr = JSONArray()
        list.forEach { limit ->
            val o = JSONObject()
            o.put("packageName", limit.packageName)
            o.put("appName", limit.appName)
            o.put("dailyLimitMs", limit.dailyLimitMs)
            o.put("isEnabled", limit.isEnabled)
            arr.put(o)
        }
        return arr.toString()
    }
}
