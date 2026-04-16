package com.timebox.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.timebox.app.data.model.AchievementRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementRecordDao {

    @Query("SELECT * FROM achievement_record")
    fun getAll(): Flow<List<AchievementRecord>>

    @Query("SELECT * FROM achievement_record WHERE achievementId = :id LIMIT 1")
    suspend fun getById(id: String): AchievementRecord?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun unlockAchievement(record: AchievementRecord)

    @Query("UPDATE achievement_record SET seen = 1 WHERE achievementId = :achievementId")
    suspend fun markSeen(achievementId: String)

    @Query("SELECT * FROM achievement_record WHERE seen = 0")
    suspend fun getUnseen(): List<AchievementRecord>
}

