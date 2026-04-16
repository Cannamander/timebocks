package com.timebox.app.data.repository

import com.timebox.app.data.db.ExtensionLogDao
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementChecker @Inject constructor(
    private val gamificationRepository: GamificationRepository,
    private val appLimitRepository: AppLimitRepository,
    private val extensionLogDao: ExtensionLogDao
) {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    suspend fun checkEndOfDayAchievements(date: String) {
        val hadExtensions = extensionLogDao.getAnyExtensionToday(date)

        // NO_FUNNY_BOCKSINESS — clean day
        if (!hadExtensions) {
            val wasNew = gamificationRepository.unlockIfNotAlready("NO_FUNNY_BOCKSINESS")
            if (wasNew) gamificationRepository.awardBocks(15, "NO_FUNNY_BOCKSINESS")
        }

        // IRON_BEAK — 7 day streak
        val streak = gamificationRepository.getStreakOnce()
        if ((streak?.currentStreak ?: 0) >= 7) {
            val wasNew = gamificationRepository.unlockIfNotAlready("IRON_BEAK")
            if (wasNew) gamificationRepository.awardBocks(50, "IRON_BEAK")
        }

        // SOCIALLY_STABLE — all tracked social apps stayed within limits (no extensions)
        val socialPackages = listOf(
            "com.instagram.android",
            "com.zhiliaoapp.musically",
            "com.twitter.android",
            "com.facebook.katana",
            "com.snapchat.android",
            "com.google.android.youtube"
        )
        val enabledLimits = appLimitRepository.getEnabledLimitsSnapshot()
        val trackedSocial = enabledLimits.filter { it.packageName in socialPackages }
        if (trackedSocial.isNotEmpty()) {
            val allClean = trackedSocial.all { app ->
                extensionLogDao.getExtensionCount(app.packageName, date) == 0
            }
            if (allClean) {
                val wasNew = gamificationRepository.unlockIfNotAlready("SOCIALLY_STABLE")
                if (wasNew) gamificationRepository.awardBocks(20, "SOCIALLY_STABLE")
            }
        }
    }

    fun yesterday(): String = LocalDate.now().minusDays(1).format(dateFormatter)
}

