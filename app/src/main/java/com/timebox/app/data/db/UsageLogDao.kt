package com.timebox.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.timebox.app.data.model.UsageLog
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageLogDao {
    @Query(
        "SELECT * FROM usage_logs WHERE packageName = :packageName AND date = :date LIMIT 1"
    )
    suspend fun getUsageForDate(packageName: String, date: String): UsageLog?

    @Query("SELECT * FROM usage_logs WHERE date = :date")
    fun getAllUsageForDate(date: String): Flow<List<UsageLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUsage(log: UsageLog)

    @Query("DELETE FROM usage_logs WHERE date < :cutoffDate")
    suspend fun deleteOldLogs(cutoffDate: String)
}
