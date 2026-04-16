package com.timebox.app.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "timebocks_prefs")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val HAS_UNSEEN_DAILY_SUMMARY = booleanPreferencesKey("hasUnseenDailySummary")
        val PENDING_STREAK_MILESTONE = intPreferencesKey("pendingStreakMilestone")
    }

    fun hasUnseenDailySummaryFlow(): Flow<Boolean> =
        context.dataStore.data.map { it[Keys.HAS_UNSEEN_DAILY_SUMMARY] ?: false }

    suspend fun hasUnseenDailySummary(): Boolean =
        context.dataStore.data.first()[Keys.HAS_UNSEEN_DAILY_SUMMARY] ?: false

    suspend fun setHasUnseenDailySummary(value: Boolean) {
        context.dataStore.edit { it[Keys.HAS_UNSEEN_DAILY_SUMMARY] = value }
    }

    fun pendingStreakMilestoneFlow(): Flow<Int> =
        context.dataStore.data.map { it[Keys.PENDING_STREAK_MILESTONE] ?: 0 }

    suspend fun getPendingStreakMilestone(): Int =
        context.dataStore.data.first()[Keys.PENDING_STREAK_MILESTONE] ?: 0

    suspend fun setPendingStreakMilestone(value: Int) {
        context.dataStore.edit { it[Keys.PENDING_STREAK_MILESTONE] = value }
    }
}

