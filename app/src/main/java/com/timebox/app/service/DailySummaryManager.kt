package com.timebox.app.service

import com.timebox.app.data.db.ExtensionLogDao
import com.timebox.app.data.repository.GamificationRepository
import com.timebox.app.data.store.AppPreferences
import com.timebox.app.percy.PercyDialogue
import com.timebox.app.percy.PercyLines
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailySummaryManager @Inject constructor(
    private val gamificationRepository: GamificationRepository,
    private val extensionLogDao: ExtensionLogDao,
    private val appPreferences: AppPreferences
) {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private fun yesterday(): String = LocalDate.now().minusDays(1).format(dateFormatter)

    suspend fun shouldShowSummary(): Boolean = appPreferences.hasUnseenDailySummary()

    suspend fun getDailySummaryDialogue(): String {
        val date = yesterday()
        val streak = gamificationRepository.getStreakOnce()?.currentStreak ?: 0
        val hadExtensions = extensionLogDao.getAnyExtensionToday(date)
        return if (!hadExtensions) {
            PercyLines.get(PercyDialogue.NextDayClean(streak))
        } else {
            PercyLines.get(PercyDialogue.NextDayHadExtensions(streak))
        }
    }

    suspend fun markSummarySeen() {
        appPreferences.setHasUnseenDailySummary(false)
    }
}

