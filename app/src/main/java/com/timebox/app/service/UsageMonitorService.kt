package com.timebox.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.timebox.app.MainActivity
import com.timebox.app.R
import com.timebox.app.data.repository.AppLimitRepository
import com.timebox.app.data.repository.UsageRepository
import com.timebox.app.ui.block.BlockOverlayActivity
import com.timebox.app.util.PermissionHelper
import com.timebox.app.util.TimeUtils
import com.timebox.app.util.UsageStatsHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class UsageMonitorService : LifecycleService() {

    @Inject
    lateinit var appLimitRepository: AppLimitRepository

    @Inject
    lateinit var usageRepository: UsageRepository

    @Inject
    lateinit var usageStatsHelper: UsageStatsHelper

    private var monitorJob: Job? = null
    private var lastDate: String = TimeUtils.getTodayDateString()
    private var lastLimitsRefreshMs: Long = 0L
    private var cachedLimitsMs: Map<String, Long> = emptyMap()

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val notification = buildMonitorNotification(null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        monitorJob = lifecycleScope.launch { runMonitorLoop() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_screen_time_monitor),
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private suspend fun runMonitorLoop() {
        val loopDelayMs = if (UsageStatsHelper.TEST_MODE) 500L else 2000L
        while (isActive) {
            val today = TimeUtils.getTodayDateString()
            if (today != lastDate) {
                LimitBlockRegistry.clearAll()
                com.timebox.app.util.BypassAllowance.clearForNewDay()
                lastDate = today
            }

            if (!PermissionHelper.hasUsageStatsPermission(this)) {
                showPermissionNotification()
                delay(loopDelayMs)
                continue
            }

            val now = System.currentTimeMillis()
            if (now - lastLimitsRefreshMs > 30_000L) {
                val limits = appLimitRepository.getEnabledLimitsSnapshot()
                cachedLimitsMs = limits.associate { it.packageName to it.dailyLimitMs }
                lastLimitsRefreshMs = now
            }

            val foreground = usageStatsHelper.getCurrentForegroundApp()
            if (foreground == null || foreground == PACKAGE_SELF) {
                delay(loopDelayMs)
                continue
            }

            val baseLimitMs = cachedLimitsMs[foreground]
            if (baseLimitMs == null) {
                delay(loopDelayMs)
                continue
            }
            val extraMs = com.timebox.app.util.BypassAllowance.getExtraMs(foreground)
            val limitMs = baseLimitMs + extraMs

            val usedMs = usageStatsHelper.getTodayUsageMs(foreground)
            try {
                usageRepository.updateTodayUsage(foreground, usedMs)
            } catch (_: Exception) {
                // ignore persistence failures for MVP
            }

            if (usedMs >= limitMs && !LimitBlockRegistry.isBlocked(foreground)) {
                LimitBlockRegistry.markBlocked(foreground)
                val overlayIntent = Intent(this, BlockOverlayActivity::class.java).apply {
                    putExtra(BlockOverlayActivity.EXTRA_PACKAGE_NAME, foreground)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(overlayIntent)
            }

            delay(loopDelayMs)
        }
    }

    private fun showPermissionNotification() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(getString(R.string.notification_usage_permission_title))
            .setContentText(getString(R.string.notification_usage_permission_text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(PERMISSION_NOTIFICATION_ID, notification)
    }

    private fun buildMonitorNotification(subText: String?): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle(getString(R.string.notification_monitor_title))
            .setContentText(getString(R.string.notification_monitor_body))
            .setSubText(subText)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "timebox_monitor"
        private const val NOTIFICATION_ID = 42
        private const val PERMISSION_NOTIFICATION_ID = 43
        private const val PACKAGE_SELF = "com.timebox.app"
    }
}
