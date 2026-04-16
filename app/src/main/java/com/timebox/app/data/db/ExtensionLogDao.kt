package com.timebox.app.data.db

import androidx.room.Dao
import androidx.room.Query

@Dao
interface ExtensionLogDao {

    @Query("SELECT extensionCount FROM extension_log WHERE packageName = :packageName AND date = :date LIMIT 1")
    suspend fun getExtensionCountOrNull(packageName: String, date: String): Int?

    suspend fun getExtensionCount(packageName: String, date: String): Int =
        getExtensionCountOrNull(packageName, date) ?: 0

    @Query(
        "SELECT EXISTS(SELECT 1 FROM extension_log WHERE date = :date AND extensionCount > 0)"
    )
    suspend fun getAnyExtensionToday(date: String): Boolean

    @Query(
        """
        INSERT OR REPLACE INTO extension_log(packageName, date, extensionCount)
        VALUES(
          :packageName,
          :date,
          COALESCE((SELECT extensionCount FROM extension_log WHERE packageName = :packageName AND date = :date), 0) + 1
        )
        """
    )
    suspend fun incrementExtension(packageName: String, date: String)

    @Query("DELETE FROM extension_log WHERE date = :date")
    suspend fun resetAllForDate(date: String)
}

