package com.timebox.app.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.timebox.app.data.repository.UsageRepository
import com.timebox.app.util.TimeUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@HiltWorker
class MidnightResetWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val usageRepository: UsageRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        val yesterday = LocalDate.now().minusDays(1).format(formatter)
        usageRepository.deleteOldLogs(yesterday)
        return Result.success()
    }
}
