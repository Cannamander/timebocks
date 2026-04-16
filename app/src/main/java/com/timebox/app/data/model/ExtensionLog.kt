package com.timebox.app.data.model

import androidx.room.Entity

@Entity(tableName = "extension_log", primaryKeys = ["packageName", "date"])
data class ExtensionLog(
    val packageName: String,
    /** ISO-8601 local date: "yyyy-MM-dd" */
    val date: String,
    /** How many extensions were used for this app on [date]. */
    val extensionCount: Int = 0
)

