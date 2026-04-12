package com.timebox.app.util

import java.util.concurrent.ConcurrentHashMap

/**
 * Prevents spamming [android.content.Context.startActivity] for the block overlay while still
 * allowing a new launch after the user dismisses it (unlike a permanent "blocked today" flag).
 */
object OverlayLaunchDebouncer {
    private const val COOLDOWN_MS = 1_200L
    private val lastLaunchMs = ConcurrentHashMap<String, Long>()

    fun shouldLaunch(packageName: String): Boolean {
        val now = System.currentTimeMillis()
        val prev = lastLaunchMs[packageName] ?: 0L
        if (now - prev < COOLDOWN_MS) return false
        lastLaunchMs[packageName] = now
        return true
    }
}
