package com.timebox.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.timebox.app.data.model.StreakRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface StreakRecordDao {

    @Query("SELECT * FROM streak_record WHERE id = 1 LIMIT 1")
    fun getStreak(): Flow<StreakRecord>

    @Query("SELECT * FROM streak_record WHERE id = 1 LIMIT 1")
    suspend fun getStreakOnce(): StreakRecord?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun initIfNeeded(row: StreakRecord = StreakRecord())

    @Update
    suspend fun updateStreak(record: StreakRecord)
}

