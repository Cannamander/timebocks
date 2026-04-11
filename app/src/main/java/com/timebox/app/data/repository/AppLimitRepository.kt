package com.timebox.app.data.repository

import com.timebox.app.data.db.AppLimitDao
import com.timebox.app.data.model.AppLimit
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
        appLimitDao.upsertLimit(
            AppLimit(
                packageName = packageName,
                appName = appName,
                dailyLimitMs = dailyLimitMs,
                isEnabled = true,
                iconByteArray = existing?.iconByteArray
            )
        )
    }

    suspend fun deleteLimit(packageName: String) = appLimitDao.deleteLimit(packageName)

    suspend fun setEnabled(packageName: String, enabled: Boolean) =
        appLimitDao.setEnabled(packageName, enabled)

    suspend fun clearAllLimits() = appLimitDao.deleteAll()
}
