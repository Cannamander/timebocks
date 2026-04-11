package com.timebox.app.ui.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import com.timebox.app.util.PermissionHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class OnboardingState(
    val usageGranted: Boolean = false,
    val overlayGranted: Boolean = false,
    val batteryGranted: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingState())
    val uiState: StateFlow<OnboardingState> = _uiState.asStateFlow()

    val allGranted: Boolean
        get() {
            val s = _uiState.value
            return s.usageGranted && s.overlayGranted && s.batteryGranted
        }

    init {
        refreshPermissions()
    }

    fun refreshPermissions() {
        _uiState.update {
            OnboardingState(
                usageGranted = PermissionHelper.hasUsageStatsPermission(appContext),
                overlayGranted = PermissionHelper.hasOverlayPermission(appContext),
                batteryGranted = PermissionHelper.hasBatteryOptimizationExemption(appContext)
            )
        }
    }

    fun openUsageSettings(context: Context) {
        PermissionHelper.openUsageAccessSettings(context)
    }

    fun openOverlaySettings(context: Context) {
        PermissionHelper.openOverlaySettings(context)
    }

    fun openBatterySettings(context: Context) {
        PermissionHelper.openBatteryOptimizationSettings(context)
    }
}
