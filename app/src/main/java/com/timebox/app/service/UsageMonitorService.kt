package com.timebox.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import com.timebox.app.util.BypassAllowance
import com.timebox.app.util.OverlayLaunchDebouncer
import com.timebox.app.util.PermissionHelper
import com.timebox.app.util.UsageStatsHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext
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
    private var lastLimitedPackageRefreshMs: Long = 0L
    private var limitedPackages: Set<String> = emptySet()

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
        val slowDelayMs = if (UsageStatsHelper.TEST_MODE) 500L else 2_000L
        val fastDelayMs = if (UsageStatsHelper.TEST_MODE) 250L else 750L

        while (coroutineContext.isActive) {
            val now = System.currentTimeMillis()

            if (now - lastLimitedPackageRefreshMs > 30_000L) {
                limitedPackages = appLimitRepository.getEnabledLimitsSnapshot()
                    .map { it.packageName }
                    .toSet()
                lastLimitedPackageRefreshMs = now
            }

            if (!PermissionHelper.hasUsageStatsPermission(this)) {
                showPermissionNotification()
                delay(slowDelayMs)
                continue
            }

            val foreground = usageStatsHelper.getCurrentForegroundApp()
            var delayMs = slowDelayMs

            if (foreground == null || foreground == PACKAGE_SELF) {
                delay(delayMs)
                continue
            }

            if (foreground !in limitedPackages) {
                delay(delayMs)
                continue
            }

            delayMs = fastDelayMs

            val limitRow = appLimitRepository.getLimitByPackage(foreground)
            if (limitRow == null) {
                delay(delayMs)
                continue
            }
            val rolled = appLimitRepository.roll24hWindowIfNeeded(limitRow)
            val usedMs = usageStatsHelper.getUsageMsInRange(
                foreground,
                rolled.windowStartEpochMs,
                now
            )
            try {
                usageRepository.updateTodayUsage(foreground, usedMs)
            } catch (_: Exception) {
                // ignore
            }

            val limitMs = rolled.dailyLimitMs + BypassAllowance.getExtraMs(foreground)

            if (usedMs >= limitMs && OverlayLaunchDebouncer.shouldLaunch(foreground)) {
                startActivity(
                    Intent(this, BlockOverlayActivity::class.java).apply {
                        putExtra(BlockOverlayActivity.EXTRA_PACKAGE_NAME, foreground)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                )
            }

            delay(delayMs)
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
