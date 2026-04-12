package com.timebox.app.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timebox.app.data.model.AppLimit
import com.timebox.app.data.repository.AppLimitRepository
import com.timebox.app.service.ServiceManager
import com.timebox.app.util.BypassAllowance
import com.timebox.app.util.TimeUtils
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
    /** Limit including temporary bypass allowance. */
    val effectiveLimitMs: Long,
    val usedMs: Long,
    val percentUsed: Float,
    val isBlocked: Boolean,
    val resetSubtitle: String
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
        val now = System.currentTimeMillis()
        val items = limits.map { limit ->
            val rolled = appLimitRepository.roll24hWindowIfNeeded(limit)
            val used = usageStatsHelper.getUsageMsInRange(
                rolled.packageName,
                rolled.windowStartEpochMs,
                now
            )
            val extra = BypassAllowance.getExtraMs(rolled.packageName)
            val effectiveLimit = rolled.dailyLimitMs + extra
            val pct = if (effectiveLimit > 0) {
                (used.toFloat() / effectiveLimit.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            DashboardItem(
                packageName = rolled.packageName,
                appName = rolled.appName,
                dailyLimitMs = rolled.dailyLimitMs,
                effectiveLimitMs = effectiveLimit,
                usedMs = used,
                percentUsed = pct,
                isBlocked = used >= effectiveLimit,
                resetSubtitle = TimeUtils.formatRollingResetSubtitle(rolled.windowStartEpochMs)
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
