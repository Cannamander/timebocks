package com.timebox.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.timebox.app.data.model.AppLimit
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLimitDao {
    @Query("SELECT * FROM app_limits ORDER BY appName ASC")
    fun getAllLimits(): Flow<List<AppLimit>>

    @Query("SELECT * FROM app_limits WHERE isEnabled = 1 ORDER BY appName ASC")
    fun getEnabledLimits(): Flow<List<AppLimit>>

    @Query("SELECT * FROM app_limits WHERE isEnabled = 1 ORDER BY appName ASC")
    suspend fun getEnabledLimitsSnapshot(): List<AppLimit>

    @Query("SELECT * FROM app_limits WHERE packageName = :packageName LIMIT 1")
    suspend fun getLimitByPackage(packageName: String): AppLimit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLimit(limit: AppLimit)

    @Query("DELETE FROM app_limits WHERE packageName = :packageName")
    suspend fun deleteLimit(packageName: String)

    @Query("DELETE FROM app_limits")
    suspend fun deleteAll()

    @Query("UPDATE app_limits SET isEnabled = :enabled WHERE packageName = :packageName")
    suspend fun setEnabled(packageName: String, enabled: Boolean)
}
