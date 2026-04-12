package com.timebox.app.data.repository

import com.timebox.app.data.db.AppLimitDao
import com.timebox.app.data.model.AppLimit
import com.timebox.app.util.BypassAllowance
import com.timebox.app.util.TimeUtils
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLimitRepository @Inject constructor(
    private val appLimitDao: AppLimitDao
) {
    fun getAllLimits(): Flow<List<AppLimit>> = appLimitDao.getAllLimits()

    fun getEnabledLimits(): Flow<List<AppLimit>> = appLimitDao.getEnabledLimits()

    suspend fun getEnabledLimitsSnapshot(): List<AppLimit> =
        appLimitDao.getEnabledLimitsSnapshot()

    suspend fun getLimitByPackage(packageName: String): AppLimit? =
        appLimitDao.getLimitByPackage(packageName)

    suspend fun upsertLimit(limit: AppLimit) = appLimitDao.upsertLimit(limit)

    suspend fun upsertLimit(
        packageName: String,
        appName: String,
        dailyLimitMs: Long
    ) {
        val existing = appLimitDao.getLimitByPackage(packageName)
        val now = System.currentTimeMillis()
        appLimitDao.upsertLimit(
            AppLimit(
                packageName = packageName,
                appName = appName,
                dailyLimitMs = dailyLimitMs,
                isEnabled = true,
                iconByteArray = existing?.iconByteArray,
                windowStartEpochMs = now
            )
        )
    }

    suspend fun deleteLimit(packageName: String) = appLimitDao.deleteLimit(packageName)

    suspend fun setEnabled(packageName: String, enabled: Boolean) =
        appLimitDao.setEnabled(packageName, enabled)

    suspend fun clearAllLimits() = appLimitDao.deleteAll()

    /**
     * Initializes [AppLimit.windowStartEpochMs] if needed and advances the 24h window when expired,
     * clearing in-memory bypass for that app for the new period.
     */
    suspend fun roll24hWindowIfNeeded(limit: AppLimit): AppLimit {
        val now = System.currentTimeMillis()
        var start = limit.windowStartEpochMs
        if (start == 0L) {
            val initialized = limit.copy(windowStartEpochMs = now)
            appLimitDao.upsertLimit(initialized)
            return initialized
        }
        var newStart = start
        var advanced = false
        while (now - newStart >= TimeUtils.WINDOW_MS_24H) {
            BypassAllowance.clearPackage(limit.packageName)
            newStart += TimeUtils.WINDOW_MS_24H
            advanced = true
        }
        return if (advanced) {
            val updated = limit.copy(windowStartEpochMs = newStart)
            appLimitDao.upsertLimit(updated)
            updated
        } else {
            limit
        }
    }
}
