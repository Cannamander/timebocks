package com.timebox.app.data.repository

import com.timebox.app.data.db.UsageLogDao
import com.timebox.app.data.model.UsageLog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageRepository @Inject constructor(
    private val usageLogDao: UsageLogDao
) {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    suspend fun getTodayUsage(packageName: String): Long {
        val log = usageLogDao.getUsageForDate(packageName, getTodayDate())
        return log?.usedMs ?: 0L
    }

    suspend fun updateTodayUsage(packageName: String, usedMs: Long) {
        usageLogDao.upsertUsage(
            UsageLog(
                packageName = packageName,
                date = getTodayDate(),
                usedMs = usedMs
            )
        )
    }

    fun getTodayDate(): String = LocalDate.now().format(dateFormatter)

    suspend fun deleteOldLogs(cutoffDate: String) = usageLogDao.deleteOldLogs(cutoffDate)
}
