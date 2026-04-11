package com.timebox.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import coil.compose.LocalImageLoader
import androidx.compose.runtime.CompositionLocalProvider
import com.timebox.app.service.ServiceManager
import com.timebox.app.util.PermissionHelper
import com.timebox.app.util.TimeboxImageLoader
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var timeboxImageLoader: TimeboxImageLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (PermissionHelper.hasUsageStatsPermission(this) &&
            PermissionHelper.hasOverlayPermission(this)
        ) {
            ServiceManager.startMonitoring(this)
        }
        setContent {
            CompositionLocalProvider(LocalImageLoader provides timeboxImageLoader.imageLoader) {
                TimeboxApp()
            }
        }
    }
}
