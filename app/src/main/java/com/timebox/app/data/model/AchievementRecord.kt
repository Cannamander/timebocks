package com.timebox.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievement_record")
data class AchievementRecord(
    @PrimaryKey val achievementId: String,
    /** ISO-8601 local date: "yyyy-MM-dd" */
    val unlockedDate: String,
    val seen: Boolean = false
)

