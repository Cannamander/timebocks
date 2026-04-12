package com.timebox.app.util

import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory extra allowance per app for the current day (bypass), not persisted to Room.
 */
object BypassAllowance {
    private val extraMsByPackage = ConcurrentHashMap<String, Long>()

    fun add(packageName: String, ms: Long) {
        extraMsByPackage.merge(packageName, ms, Long::plus)
    }

    fun getExtraMs(packageName: String): Long = extraMsByPackage[packageName] ?: 0L

    fun clearPackage(packageName: String) {
        extraMsByPackage.remove(packageName)
    }

    fun clearForNewDay() {
        extraMsByPackage.clear()
    }
}
