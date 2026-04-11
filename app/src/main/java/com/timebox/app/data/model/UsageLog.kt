package com.timebox.app.data.model

import androidx.room.Entity

@Entity(tableName = "usage_logs", primaryKeys = ["packageName", "date"])
data class UsageLog(
    val packageName: String,
    val date: String,
    val usedMs: Long
)
