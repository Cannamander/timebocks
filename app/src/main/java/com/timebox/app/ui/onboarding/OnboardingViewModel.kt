package com.timebox.app.ui.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import com.timebox.app.util.PermissionHelper
import com.timebox.app.percy.PercyDialogue
import com.timebox.app.percy.PercyLines
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

    private val _percyDialogue = MutableStateFlow(PercyLines.get(PercyDialogue.Welcome))
    val percyDialogue: StateFlow<String> = _percyDialogue.asStateFlow()

    val allGranted: Boolean
        get() {
            val s = _uiState.value
            return s.usageGranted && s.overlayGranted && s.batteryGranted
        }

    init {
        refreshPermissions()
    }

    fun refreshPermissions() {
        val newState = OnboardingState(
            usageGranted = PermissionHelper.hasUsageStatsPermission(appContext),
            overlayGranted = PermissionHelper.hasOverlayPermission(appContext),
            batteryGranted = PermissionHelper.hasBatteryOptimizationExemption(appContext)
        )
        _uiState.update { newState }

        if (newState.usageGranted && newState.overlayGranted && newState.batteryGranted) {
            _percyDialogue.value = PercyLines.get(PercyDialogue.PermissionsGranted)
        }
    }

    fun openUsageSettings(context: Context) {
        _percyDialogue.value = PercyLines.get(PercyDialogue.PermissionsExplainer)
        PermissionHelper.openUsageAccessSettings(context)
    }

    fun openOverlaySettings(context: Context) {
        _percyDialogue.value = PercyLines.get(PercyDialogue.PermissionsExplainer)
        PermissionHelper.openOverlaySettings(context)
    }

    fun openBatterySettings(context: Context) {
        _percyDialogue.value = PercyLines.get(PercyDialogue.PermissionsExplainer)
        PermissionHelper.openBatteryOptimizationSettings(context)
    }
}
