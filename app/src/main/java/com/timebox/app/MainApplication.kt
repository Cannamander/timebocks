package com.timebox.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.timebox.app.service.MidnightResetWorker
import com.timebox.app.util.TimeUtils
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class MainApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun getWorkManagerConfiguration(): Configuration =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleMidnightReset()
    }

    private fun scheduleMidnightReset() {
        val wm = WorkManager.getInstance(this)
        val work = PeriodicWorkRequestBuilder<MidnightResetWorker>(1, TimeUnit.DAYS)
            .setFlex(1, TimeUnit.HOURS)
            .setInitialDelay(TimeUtils.getMsUntilMidnight(), TimeUnit.MILLISECONDS)
            .build()
        wm.enqueueUniquePeriodicWork(
            "midnight_reset",
            ExistingPeriodicWorkPolicy.KEEP,
            work
        )
    }
}
