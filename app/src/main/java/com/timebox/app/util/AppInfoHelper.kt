package com.timebox.app.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppInfoHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val packageManager: PackageManager = context.packageManager

    fun getInstalledUserApps(): List<AppInfo> {
        val myPackage = context.packageName
        val apps = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledApplications(0)
            }
        } catch (_: RuntimeException) {
            emptyList()
        }
        return apps
            .asSequence()
            .filter { app ->
                (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0 &&
                    app.packageName != myPackage
            }
            .mapNotNull { app ->
                try {
                    val label = packageManager.getApplicationLabel(app).toString()
                    AppInfo(packageName = app.packageName, appName = label)
                } catch (_: PackageManager.NameNotFoundException) {
                    null
                }
            }
            .sortedBy { it.appName.lowercase() }
            .toList()
    }

    fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    fun getAppIconBitmap(packageName: String): Bitmap? {
        return try {
            val drawable = packageManager.getApplicationIcon(packageName)
            drawableToBitmap(drawable)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = drawable.intrinsicWidth.coerceAtLeast(1)
        val height = drawable.intrinsicHeight.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
