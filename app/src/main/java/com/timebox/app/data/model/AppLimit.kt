package com.timebox.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_limits")
data class AppLimit(
    @PrimaryKey val packageName: String,
    val appName: String,
    val dailyLimitMs: Long,
    val isEnabled: Boolean = true,
    val iconByteArray: ByteArray? = null,
    /** Start of the current 24h window; usage is counted from this instant. 0 = uninitialized (treated as now on first roll). */
    @ColumnInfo(name = "window_start_epoch_ms")
    val windowStartEpochMs: Long = 0L
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AppLimit
        if (packageName != other.packageName) return false
        if (appName != other.appName) return false
        if (dailyLimitMs != other.dailyLimitMs) return false
        if (isEnabled != other.isEnabled) return false
        if (iconByteArray != null) {
            if (other.iconByteArray == null) return false
            if (!iconByteArray.contentEquals(other.iconByteArray)) return false
        } else if (other.iconByteArray != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + appName.hashCode()
        result = 31 * result + dailyLimitMs.hashCode()
        result = 31 * result + windowStartEpochMs.hashCode()
        result = 31 * result + isEnabled.hashCode()
        result = 31 * result + (iconByteArray?.contentHashCode() ?: 0)
        return result
    }
}
