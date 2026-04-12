package com.timebox.app.util

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.max

object TimeUtils {

    /** Length of each timebox window from [com.timebox.app.data.model.AppLimit.windowStartEpochMs]. */
    const val WINDOW_MS_24H: Long = 24L * 60L * 60L * 1000L

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun windowEndEpochMs(windowStartEpochMs: Long): Long =
        windowStartEpochMs + WINDOW_MS_24H

    /** Subtitle for dashboard / block overlay: time until the current 24h window rolls. */
    fun formatRollingResetSubtitle(windowStartEpochMs: Long): String {
        if (windowStartEpochMs <= 0L) return "Resets 24h after last save"
        val end = windowEndEpochMs(windowStartEpochMs)
        val left = max(0L, end - System.currentTimeMillis())
        return if (left <= 0L) "Renewing window…" else "Resets in ${formatDuration(left)}"
    }

    fun formatDuration(ms: Long): String {
        if (ms < 60_000L) return "< 1m"
        val totalMinutes = ms / 60_000L
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }

    fun formatTimeRemaining(limitMs: Long, usedMs: Long): String {
        if (usedMs >= limitMs) return "Limit reached"
        val left = max(0L, limitMs - usedMs)
        val totalMinutes = left / 60_000L
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) {
            "${hours}h ${minutes}m left"
        } else {
            "${minutes}m left"
        }
    }

    fun getTodayMidnightMs(): Long {
        val start = LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault())
        return start.toInstant().toEpochMilli()
    }

    fun getTodayDateString(): String = LocalDate.now().format(dateFormatter)

    fun getMsUntilMidnight(): Long {
        val zone = ZoneId.systemDefault()
        val now = java.time.ZonedDateTime.now(zone)
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(zone)
        return ChronoUnit.MILLIS.between(now, nextMidnight)
    }
}
