package com.timebox.app.util

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.os.Process
import android.os.Build
import android.provider.Settings

object PermissionHelper {

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasOverlayPermission(context: Context): Boolean =
        Settings.canDrawOverlays(context)

    fun hasBatteryOptimizationExemption(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun openUsageAccessSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun openOverlaySettings(context: Context) {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /**
     * Opens battery optimization settings for this app.
     * [Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS] often does nothing on API 31+ / emulators;
     * we fall back to app details where the user can tap **Battery** → **Unrestricted**.
     */
    fun openBatteryOptimizationSettings(context: Context) {
        val packageUri = Uri.parse("package:${context.packageName}")
        val pm = context.packageManager
        val newTask = Intent.FLAG_ACTIVITY_NEW_TASK

        val requestIgnore = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = packageUri
            addFlags(newTask)
        }
        if (requestIgnore.resolveActivity(pm) != null) {
            try {
                context.startActivity(requestIgnore)
                return
            } catch (_: Exception) {
                // Some builds resolve the intent but show no UI — fall through
            }
        }

        val appDetails = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = packageUri
            addFlags(newTask)
        }
        context.startActivity(appDetails)
    }
}
