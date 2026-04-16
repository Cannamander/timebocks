package com.timebox.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streak_record")
data class StreakRecord(
    @PrimaryKey val id: Int = 1,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    /** ISO-8601 local date: "yyyy-MM-dd" of last clean day. */
    val lastCleanDate: String = ""
)

