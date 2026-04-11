package com.timebox.app.service

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

object ServiceManager {

    fun startMonitoring(context: Context) {
        val intent = Intent(context, UsageMonitorService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopMonitoring(context: Context) {
        context.stopService(Intent(context, UsageMonitorService::class.java))
    }

    fun isRunning(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        val services = am.getRunningServices(Int.MAX_VALUE) ?: return false
        val target = UsageMonitorService::class.java.name
        return services.any { it.service.className == target }
    }
}
