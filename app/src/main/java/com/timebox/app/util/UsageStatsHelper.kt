package com.timebox.app.util

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageStatsHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        /** Set to true during dev to simulate rapid usage growth (see PROMPT 10). */
        const val TEST_MODE = false
    }

    private val usageStatsManager: UsageStatsManager?
        get() = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    fun getTodayUsageMs(packageName: String): Long {
        if (!PermissionHelper.hasUsageStatsPermission(context)) return 0L
        val manager = usageStatsManager ?: return 0L
        val begin = TimeUtils.getTodayMidnightMs()
        val end = System.currentTimeMillis()
        val stats = manager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            begin,
            end
        ) ?: return 0L
        var total = 0L
        for (s in stats) {
            if (s.packageName == packageName) {
                total += s.totalTimeInForeground
            }
        }
        if (TEST_MODE) total += 60_000L
        return total
    }

    fun getTodayUsageForAllApps(): Map<String, Long> {
        if (!PermissionHelper.hasUsageStatsPermission(context)) return emptyMap()
        val manager = usageStatsManager ?: return emptyMap()
        val begin = TimeUtils.getTodayMidnightMs()
        val end = System.currentTimeMillis()
        val stats = manager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            begin,
            end
        ) ?: return emptyMap()
        val map = mutableMapOf<String, Long>()
        for (s in stats) {
            map[s.packageName] = map.getOrDefault(s.packageName, 0L) + s.totalTimeInForeground
        }
        return map
    }

    fun getCurrentForegroundApp(): String? {
        if (!PermissionHelper.hasUsageStatsPermission(context)) return null
        val manager = usageStatsManager ?: return null
        val end = System.currentTimeMillis()
        val begin = end - TimeUnit.SECONDS.toMillis(5)
        val stats = manager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, begin, end)
            ?: return null
        var latest: UsageStats? = null
        for (s in stats) {
            val current = latest
            if (current == null || s.lastTimeUsed > current.lastTimeUsed) {
                latest = s
            }
        }
        val pkg = latest?.packageName ?: return null
        return pkg.takeIf { it != context.packageName }
    }
}
