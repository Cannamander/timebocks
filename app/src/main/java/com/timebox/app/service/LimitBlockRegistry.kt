package com.timebox.app.service

/**
 * Tracks which apps have already triggered the block overlay today (in-memory).
 */
object LimitBlockRegistry {
    private val blockedToday = mutableSetOf<String>()

    fun markBlocked(packageName: String) {
        synchronized(this) { blockedToday.add(packageName) }
    }

    fun clearBlock(packageName: String) {
        synchronized(this) { blockedToday.remove(packageName) }
    }

    fun isBlocked(packageName: String): Boolean =
        synchronized(this) { blockedToday.contains(packageName) }

    fun clearAll() {
        synchronized(this) { blockedToday.clear() }
    }
}
