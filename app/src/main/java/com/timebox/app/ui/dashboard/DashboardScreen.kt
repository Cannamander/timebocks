package com.timebox.app.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.timebox.app.data.model.AchievementDefinitions
import com.timebox.app.ui.components.PercyEmotion
import com.timebox.app.ui.components.PercyWidget
import com.timebox.app.util.PackageIcon
import com.timebox.app.util.TimeUtils
import kotlin.math.max
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAddApps: () -> Unit,
    onSettings: () -> Unit,
    onOpenAppList: (String?) -> Unit,
    onOpenAchievements: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.onResume()
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(30_000)
            viewModel.refreshUsage()
        }
    }

    LaunchedEffect(state.unseenAchievements) {
        val first = state.unseenAchievements.firstOrNull() ?: return@LaunchedEffect
        val def = AchievementDefinitions.getById(first.achievementId)
        val title = def?.title ?: first.achievementId
        val reward = def?.bocksReward ?: 0
        snackbarHostState.showSnackbar("Achievement unlocked: $title — +$reward Bocks")
        viewModel.onAchievementSeen(first.achievementId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Timebox") },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddApps) {
                Icon(Icons.Default.Add, contentDescription = "Add apps")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            MonitoringBanner(
                isMonitoring = state.isMonitoring,
                onToggle = { viewModel.toggleMonitoring(context) }
            )
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.items.isEmpty()) {
                EmptyDashboard(
                    onAddApps = onAddApps,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            } else {
                if (state.pendingStreakMilestone > 0) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable { viewModel.onStreakMilestoneSeen() },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Streak milestone: ${state.pendingStreakMilestone} days",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap to dismiss",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                if (state.showDailySummary) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161618))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Percy's morning briefing", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.padding(4.dp))
                            Text(state.dailySummaryLine, color = Color.White.copy(alpha = 0.9f))
                            Spacer(modifier = Modifier.padding(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { viewModel.onDailySummaryDismissed() }) {
                                    Text("Got it")
                                }
                            }
                        }
                    }
                }

                PercyWidget(
                    dialogueLine = state.percyLine,
                    emotion = if (state.items.any { it.percentUsed >= 0.8f }) PercyEmotion.WORRIED else PercyEmotion.CALM,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    percySizeDp = 80
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        label = "Bocks",
                        value = state.bocksBalance.toString(),
                        sub = "earned",
                        valueColor = Color(0xFFFFB300),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenAchievements() }
                    )
                    StatCard(
                        label = "Streak",
                        value = state.currentStreak.toString(),
                        sub = "days",
                        valueColor = if (state.currentStreak > 0) Color(0xFF81C784) else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.weight(1f)
                    )
                }

                val within = state.items.count { !it.isBlocked }
                val total = state.items.size
                val summaryColor = when {
                    state.items.any { it.isBlocked } -> Color(0xFFCF6679)
                    state.items.any { it.percentUsed >= 0.8f } -> Color(0xFFFFB74D)
                    else -> Color(0xFF81C784)
                }
                SummaryCard(
                    text = "$within of $total apps within limit today",
                    color = summaryColor
                )
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(state.items, key = { it.packageName }) { item ->
                        DashboardRow(
                            item = item,
                            onClick = { onOpenAppList(item.packageName) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    sub: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f))
            Text(text = value, fontWeight = FontWeight.Bold, color = valueColor)
            Text(text = sub, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun MonitoringBanner(
    isMonitoring: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isMonitoring) {
            Text(
                text = "● Monitoring active · Tap to pause",
                color = Color(0xFF81C784),
                fontWeight = FontWeight.Medium
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "⚠ Monitoring paused",
                    color = Color(0xFFFFB74D),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.padding(8.dp))
                Text(
                    text = "Tap to start",
                    color = Color(0xFF64B5F6)
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(text: String, color: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DashboardRow(
    item: DashboardItem,
    onClick: () -> Unit
) {
    val progressColor = when {
        item.isBlocked || item.percentUsed > 0.8f -> Color(0xFFCF6679)
        item.percentUsed >= 0.5f -> Color(0xFFFFB74D)
        else -> Color(0xFF81C784)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = PackageIcon(item.packageName),
            contentDescription = item.appName,
            modifier = Modifier
                .size(48.dp)
                .padding(end = 12.dp),
            contentScale = ContentScale.Fit
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(item.appName, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(
                progress = { item.percentUsed },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                color = progressColor,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
            val subtitle = if (item.isBlocked) {
                "Blocked · ${item.resetSubtitle}"
            } else {
                val leftMs = max(0L, item.effectiveLimitMs - item.usedMs)
                "${TimeUtils.formatDuration(item.usedMs)} used · ${TimeUtils.formatDuration(leftMs)} left"
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun EmptyDashboard(
    onAddApps: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No apps limited yet",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.padding(16.dp))
        androidx.compose.material3.Button(onClick = onAddApps) {
            Text("Add apps")
        }
    }
}
