package com.timebox.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.timebox.app.ui.NavRoutes
import com.timebox.app.ui.applist.AppListScreen
import com.timebox.app.ui.dashboard.DashboardScreen
import com.timebox.app.ui.onboarding.OnboardingScreen
import com.timebox.app.ui.settings.SettingsScreen
import com.timebox.app.util.PermissionHelper

private val TimeboxDarkColors = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    background = Color(0xFF0F0F0F),
    surface = Color(0xFF1A1A1A),
    error = Color(0xFFCF6679)
)

@Composable
fun TimeboxApp() {
    val context = LocalContext.current
    val startDestination = remember {
        if (PermissionHelper.hasUsageStatsPermission(context) &&
            PermissionHelper.hasOverlayPermission(context)
        ) {
            NavRoutes.DASHBOARD
        } else {
            NavRoutes.ONBOARDING
        }
    }

    val navController = rememberNavController()

    MaterialTheme(colorScheme = TimeboxDarkColors) {
        Surface(color = MaterialTheme.colorScheme.background) {
            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                composable(NavRoutes.ONBOARDING) {
                    OnboardingScreen(
                        onComplete = {
                            navController.navigate(NavRoutes.DASHBOARD) {
                                popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                            }
                        }
                    )
                }
                composable(NavRoutes.DASHBOARD) {
                    DashboardScreen(
                        onAddApps = { navController.navigate(NavRoutes.APP_LIST) },
                        onSettings = { navController.navigate(NavRoutes.SETTINGS) },
                        onOpenAppList = { navController.navigate(NavRoutes.APP_LIST) }
                    )
                }
                composable(NavRoutes.APP_LIST) {
                    AppListScreen(
                        onDone = { navController.popBackStack() }
                    )
                }
                composable(NavRoutes.SETTINGS) {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onOpenAppList = { navController.navigate(NavRoutes.APP_LIST) },
                        onResetPermissions = {
                            navController.navigate(NavRoutes.ONBOARDING) {
                                popUpTo(NavRoutes.DASHBOARD) { inclusive = false }
                            }
                        }
                    )
                }
            }
        }
    }
}
