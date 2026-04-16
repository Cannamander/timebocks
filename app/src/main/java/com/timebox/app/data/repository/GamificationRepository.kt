package com.timebox.app.data.repository

import android.util.Log
import com.timebox.app.data.db.AchievementRecordDao
import com.timebox.app.data.db.BocksLedgerDao
import com.timebox.app.data.db.ExtensionLogDao
import com.timebox.app.data.db.StreakRecordDao
import com.timebox.app.data.model.AchievementRecord
import com.timebox.app.data.model.BocksLedger
import com.timebox.app.data.model.StreakRecord
import com.timebox.app.data.store.AppPreferences
import com.timebox.app.util.TimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GamificationRepository @Inject constructor(
    private val extensionLogDao: ExtensionLogDao,
    private val bocksLedgerDao: BocksLedgerDao,
    private val streakRecordDao: StreakRecordDao,
    private val achievementRecordDao: AchievementRecordDao,
    private val appPreferences: AppPreferences
) {
    private val tag = "GamificationRepository"
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private fun today(): String = TimeUtils.getTodayDateString()
    private fun yesterday(): String = LocalDate.now().minusDays(1).format(dateFormatter)

    /**
     * Ensures singleton gamification rows exist before collecting Room [Flow]s that observe them.
     * Safe to call on every cold start (INSERT OR IGNORE).
     */
    suspend fun bootstrap() {
        bocksLedgerDao.initIfNeeded()
        streakRecordDao.initIfNeeded()
    }

    suspend fun getExtensionCount(packageName: String): Int =
        extensionLogDao.getExtensionCount(packageName, today())

    suspend fun incrementExtension(packageName: String): Int {
        val d = today()
        extensionLogDao.incrementExtension(packageName, d)
        return extensionLogDao.getExtensionCount(packageName, d)
    }

    suspend fun hadAnyExtensionToday(): Boolean =
        extensionLogDao.getAnyExtensionToday(today())

    fun getBocksBalance(): Flow<Int> =
        bocksLedgerDao.getBalance().map { ledger -> ledger?.balance ?: 0 }

    fun getBocksLedger(): Flow<BocksLedger> =
        bocksLedgerDao.getBalance().map { it ?: BocksLedger() }

    suspend fun getBocksBalanceOnce(): Int {
        bocksLedgerDao.initIfNeeded()
        return bocksLedgerDao.getBalanceOnce()?.balance ?: 0
    }

    suspend fun awardBocks(amount: Int, reason: String) {
        if (amount <= 0) return
        bocksLedgerDao.initIfNeeded()
        bocksLedgerDao.addBocks(amount)
        Log.d(tag, "Awarded $amount Bocks ($reason)")
    }

    suspend fun spendBocks(amount: Int): Boolean {
        if (amount <= 0) return true
        return bocksLedgerDao.spendBocks(amount)
    }

    fun getStreak(): Flow<StreakRecord> =
        streakRecordDao.getStreak().map { row -> row ?: StreakRecord() }

    suspend fun getStreakOnce(): StreakRecord? = streakRecordDao.getStreakOnce()

    /**
     * End-of-day logic (run after midnight): checks whether [yesterday] had any extensions.
     * - Clean day: increment streak + award 10 Bocks
     * - Otherwise: reset current streak to 0
     */
    suspend fun processEndOfDay() {
        val d = yesterday()
        streakRecordDao.initIfNeeded()
        bocksLedgerDao.initIfNeeded()

        val hadExtensions = extensionLogDao.getAnyExtensionToday(d)
        val current = streakRecordDao.getStreakOnce() ?: StreakRecord()

        val updated = if (!hadExtensions) {
            val newCurrent = current.currentStreak + 1
            current.copy(
                currentStreak = newCurrent,
                longestStreak = maxOf(current.longestStreak, newCurrent),
                lastCleanDate = d
            ).also {
                awardBocks(10, "CLEAN_DAY_STREAK")
                awardStreakMilestoneIfNeeded(newCurrent)
            }
        } else {
            current.copy(currentStreak = 0)
        }

        streakRecordDao.updateStreak(updated)
    }

    private suspend fun awardStreakMilestoneIfNeeded(currentStreak: Int) {
        val milestoneReward = when (currentStreak) {
            3 -> 20
            7 -> 50
            14 -> 100
            30 -> 200
            else -> 0
        }
        if (milestoneReward <= 0) return

        awardBocks(milestoneReward, "STREAK_$currentStreak")

        // Only set if none pending; dashboard will clear after showing.
        val existing = appPreferences.getPendingStreakMilestone()
        if (existing == 0) {
            appPreferences.setPendingStreakMilestone(currentStreak)
        }
    }

    suspend fun unlockIfNotAlready(achievementId: String): Boolean {
        val existing = achievementRecordDao.getById(achievementId)
        if (existing != null) return false
        val d = today()
        achievementRecordDao.unlockAchievement(
            AchievementRecord(achievementId = achievementId, unlockedDate = d, seen = false)
        )
        return true
    }

    suspend fun getUnseenAchievements(): List<AchievementRecord> =
        achievementRecordDao.getUnseen()

    suspend fun markAchievementSeen(achievementId: String) {
        achievementRecordDao.markSeen(achievementId)
    }
}

