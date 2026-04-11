package com.timebox.app.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timebox.app.data.model.AppLimit
import com.timebox.app.data.repository.AppLimitRepository
import com.timebox.app.service.ServiceManager
import com.timebox.app.util.UsageStatsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardItem(
    val packageName: String,
    val appName: String,
    val dailyLimitMs: Long,
    val usedMs: Long,
    val percentUsed: Float,
    val isBlocked: Boolean
)

data class DashboardState(
    val items: List<DashboardItem> = emptyList(),
    val isMonitoring: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val appLimitRepository: AppLimitRepository,
    private val usageStatsHelper: UsageStatsHelper,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            appLimitRepository.getEnabledLimits().collect { limits ->
                refreshUsageInternal(limits)
            }
        }
    }

    fun onResume() {
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

    fun refreshUsage() {
        viewModelScope.launch {
            val limits = appLimitRepository.getEnabledLimitsSnapshot()
            refreshUsageInternal(limits)
        }
    }

    private suspend fun refreshUsageInternal(limits: List<AppLimit>) {
        val items = limits.map { limit ->
            val used = usageStatsHelper.getTodayUsageMs(limit.packageName)
            val pct = if (limit.dailyLimitMs > 0) {
                (used.toFloat() / limit.dailyLimitMs.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            DashboardItem(
                packageName = limit.packageName,
                appName = limit.appName,
                dailyLimitMs = limit.dailyLimitMs,
                usedMs = used,
                percentUsed = pct,
                isBlocked = used >= limit.dailyLimitMs
            )
        }
        _state.update {
            it.copy(
                items = items.sortedBy { row -> row.appName },
                isLoading = false,
                isMonitoring = ServiceManager.isRunning(appContext)
            )
        }
    }
}
