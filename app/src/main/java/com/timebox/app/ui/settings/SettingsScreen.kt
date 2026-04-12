package com.timebox.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.timebox.app.util.PermissionHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAppList: () -> Unit,
    onResetPermissions: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showClearDialog by remember { mutableStateOf(false) }
    var exportedJson by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshMonitoringState()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SectionLabel("Monitoring")
            ListItem(
                headlineContent = { Text("Background monitoring") },
                trailingContent = {
                    Switch(
                        checked = state.isMonitoring,
                        onCheckedChange = { viewModel.toggleMonitoring(context) }
                    )
                }
            )
            ListItem(
                headlineContent = { Text("Battery optimization") },
                supportingContent = {
                    Text(
                        if (PermissionHelper.hasBatteryOptimizationExemption(context)) {
                            "Unrestricted"
                        } else {
                            "Restricted — tap to request exemption"
                        }
                    )
                },
                modifier = Modifier.padding(bottom = 8.dp),
                trailingContent = {
                    TextButton(onClick = { PermissionHelper.openBatteryOptimizationSettings(context) }) {
                        Text("Open")
                    }
                }
            )
            HorizontalDivider()
            SectionLabel("Limits")
            ListItem(
                headlineContent = { Text("Apps limited") },
                supportingContent = { Text("${state.totalAppsLimited}") },
                modifier = Modifier.padding(vertical = 4.dp),
                trailingContent = {
                    TextButton(onClick = onOpenAppList) {
                        Text("Manage")
                    }
                }
            )
            ListItem(
                headlineContent = { Text("Reset time") },
                supportingContent = { Text("24h rolling from last save per app") }
            )
            ListItem(
                headlineContent = { Text("Clear all limits") },
                supportingContent = { Text("Removes every configured limit") },
                trailingContent = {
                    TextButton(onClick = { showClearDialog = true }) {
                        Text("Clear")
                    }
                }
            )
            ListItem(
                headlineContent = { Text("Export limits (JSON)") },
                supportingContent = { Text("Copy a JSON snapshot for transparency") },
                trailingContent = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                exportedJson = viewModel.exportData()
                            }
                        }
                    ) {
                        Text("Export")
                    }
                }
            )
            HorizontalDivider()
            SectionLabel("About")
            ListItem(
                headlineContent = { Text("Version") },
                supportingContent = { Text(state.appVersion) }
            )
            ListItem(
                headlineContent = { Text("Permissions") },
                supportingContent = { Text("Re-run the permission setup") },
                trailingContent = {
                    TextButton(onClick = onResetPermissions) {
                        Text("Open")
                    }
                }
            )
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear all limits?") },
            text = { Text("This removes every app limit from this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllLimits()
                        showClearDialog = false
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    exportedJson?.let { json ->
        AlertDialog(
            onDismissRequest = { exportedJson = null },
            title = { Text("Exported data") },
            text = {
                Text(
                    text = json,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val clip = android.content.ClipData.newPlainText("timebox_limits", json)
                        (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager)
                            .setPrimaryClip(clip)
                        exportedJson = null
                    }
                ) {
                    Text("Copy & close")
                }
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
