package com.timebox.app.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timebox.app.data.model.AppLimit
import com.timebox.app.data.model.AchievementRecord
import com.timebox.app.data.repository.AppLimitRepository
import com.timebox.app.data.repository.GamificationRepository
import com.timebox.app.data.store.AppPreferences
import com.timebox.app.percy.PercyDialogue
import com.timebox.app.percy.PercyLines
import com.timebox.app.service.ServiceManager
import com.timebox.app.service.DailySummaryManager
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
import kotlinx.coroutines.supervisorScope
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
    val isLoading: Boolean = true,
    val bocksBalance: Int = 0,
    val currentStreak: Int = 0,
    val percyLine: String = "",
    val unseenAchievements: List<AchievementRecord> = emptyList(),
    val showDailySummary: Boolean = false,
    val dailySummaryLine: String = "",
    val pendingStreakMilestone: Int = 0
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val appLimitRepository: AppLimitRepository,
    private val usageStatsHelper: UsageStatsHelper,
    private val gamificationRepository: GamificationRepository,
    private val dailySummaryManager: DailySummaryManager,
    private val appPreferences: AppPreferences,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Room Flows for singleton rows do not emit until a row exists; seed defaults first.
            gamificationRepository.bootstrap()
            supervisorScope {
                launch {
                    appLimitRepository.getEnabledLimits().collect { limits ->
                        refreshUsageInternal(limits)
                    }
                }
                launch {
                    gamificationRepository.getBocksBalance().collect { bocks ->
                        _state.update { it.copy(bocksBalance = bocks) }
                        updatePercyLine()
                    }
                }
                launch {
                    gamificationRepository.getStreak().collect { streak ->
                        _state.update { it.copy(currentStreak = streak.currentStreak) }
                        updatePercyLine()
                    }
                }
                launch {
                    _state.update { it.copy(unseenAchievements = gamificationRepository.getUnseenAchievements()) }
                }
                launch {
                    val pending = appPreferences.getPendingStreakMilestone()
                    if (pending > 0) {
                        _state.update { it.copy(pendingStreakMilestone = pending) }
                    }
                }
                launch {
                    if (dailySummaryManager.shouldShowSummary()) {
                        val line = dailySummaryManager.getDailySummaryDialogue()
                        _state.update { it.copy(showDailySummary = true, dailySummaryLine = line) }
                    }
                }
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
            _state.update { it.copy(unseenAchievements = gamificationRepository.getUnseenAchievements()) }
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
        updatePercyLine()
    }

    fun onAchievementSeen(achievementId: String) {
        viewModelScope.launch {
            gamificationRepository.markAchievementSeen(achievementId)
            _state.update { it.copy(unseenAchievements = gamificationRepository.getUnseenAchievements()) }
        }
    }

    fun onDailySummaryDismissed() {
        viewModelScope.launch {
            dailySummaryManager.markSummarySeen()
            _state.update { it.copy(showDailySummary = false, dailySummaryLine = "") }
        }
    }

    fun onStreakMilestoneSeen() {
        viewModelScope.launch {
            appPreferences.setPendingStreakMilestone(0)
            _state.update { it.copy(pendingStreakMilestone = 0) }
        }
    }

    private fun updatePercyLine() {
        val s = _state.value
        val streak = s.currentStreak
        val worst = s.items.firstOrNull { it.percentUsed >= 0.8f }?.appName
        val line = when {
            streak >= 7 -> PercyLines.get(PercyDialogue.StreakMilestone(streak))
            worst != null -> PercyLines.get(PercyDialogue.FirstExtension(worst))
            else -> "Everything's fine. For now."
        }
        _state.update { it.copy(percyLine = line) }
    }
}
