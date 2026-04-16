package com.timebox.app.ui.block

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.timebox.app.ui.components.PercyEmotion
import com.timebox.app.ui.components.PercyWidget
import com.timebox.app.data.repository.AppLimitRepository
import com.timebox.app.util.AppInfoHelper
import com.timebox.app.util.BypassAllowance
import com.timebox.app.util.PackageIcon
import com.timebox.app.util.TimeUtils
import com.timebox.app.util.UsageStatsHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import javax.inject.Inject
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@AndroidEntryPoint
class BlockOverlayActivity : ComponentActivity() {

    @Inject
    lateinit var appInfoHelper: AppInfoHelper

    @Inject
    lateinit var appLimitRepository: AppLimitRepository

    @Inject
    lateinit var usageStatsHelper: UsageStatsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: run {
            finish()
            return
        }

        setContent {
            MaterialTheme {
                BlockOverlayScreen(
                    packageName = packageName,
                    appInfoHelper = appInfoHelper,
                    appLimitRepository = appLimitRepository,
                    usageStatsHelper = usageStatsHelper,
                    onGoHome = {
                        startActivity(
                            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                        )
                        finish()
                    },
                    onBypassComplete = {
                        BypassAllowance.add(packageName, 5 * 60_000L)
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_PACKAGE_NAME: String = "extra_package_name"
    }
}

@Composable
private fun BlockOverlayScreen(
    packageName: String,
    appInfoHelper: AppInfoHelper,
    appLimitRepository: AppLimitRepository,
    usageStatsHelper: UsageStatsHelper,
    onGoHome: () -> Unit,
    onBypassComplete: () -> Unit,
    viewModel: BlockOverlayViewModel = hiltViewModel()
) {
    BackHandler { }

    val bg = Color(0xFF0A0A0A)
    val accentRed = Color(0xFFC62828)

    val vmState by viewModel.state.collectAsStateWithLifecycle()

    var appName by remember { mutableStateOf(packageName) }
    var limitMs by remember { mutableLongStateOf(0L) }
    var usedMs by remember { mutableLongStateOf(0L) }
    var resetSubtitle by remember { mutableStateOf("") }

    LaunchedEffect(packageName) {
        appName = appInfoHelper.getAppName(packageName)
        val limit = appLimitRepository.getLimitByPackage(packageName) ?: return@LaunchedEffect
        val rolled = appLimitRepository.roll24hWindowIfNeeded(limit)
        val extra = BypassAllowance.getExtraMs(packageName)
        limitMs = rolled.dailyLimitMs + extra
        val now = System.currentTimeMillis()
        usedMs = usageStatsHelper.getUsageMsInRange(
            packageName,
            rolled.windowStartEpochMs,
            now
        )
        resetSubtitle = TimeUtils.formatRollingResetSubtitle(rolled.windowStartEpochMs)

        viewModel.onScreenOpened(packageName = packageName, appName = appName)
    }

    val holdInteractionSource = remember { MutableInteractionSource() }
    val isPressingHold by holdInteractionSource.collectIsPressedAsState()
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var holdCompleted by remember { mutableStateOf(false) }

    val holdDurationMs = 10_000L

    LaunchedEffect(isPressingHold) {
        if (!isPressingHold) {
            holdProgress = 0f
            holdCompleted = false
            return@LaunchedEffect
        }
        if (holdCompleted) return@LaunchedEffect
        var ms = 0L
        while (ms < holdDurationMs) {
            delay(50)
            if (!isPressingHold) {
                holdProgress = 0f
                holdCompleted = false
                return@LaunchedEffect
            }
            ms += 50
            holdProgress = (ms.toFloat() / holdDurationMs.toFloat()).coerceIn(0f, 1f)
        }
        holdProgress = 1f
        holdCompleted = true
        viewModel.onExtendTapped(packageName = packageName, appName = appName)
    }

    val animatedProgress by animateFloatAsState(
        targetValue = holdProgress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 100),
        label = "bypassProgress"
    )

    LaunchedEffect(vmState.blockPhase) {
        when (vmState.blockPhase) {
            BlockPhase.ExtensionGranted -> {
                delay(1500)
                onBypassComplete()
            }
            BlockPhase.WalkedAway -> {
                delay(1200)
                onGoHome()
            }
            else -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        PercyWidget(
            dialogueLine = vmState.percyDialogue,
            emotion = when (vmState.blockPhase) {
                BlockPhase.LimitReached -> PercyEmotion.WORRIED
                BlockPhase.FirstExtension -> PercyEmotion.SKEPTICAL
                BlockPhase.SecondChallenge -> PercyEmotion.WORRIED
                BlockPhase.ThirdRefusal -> if (vmState.showBocksOption) PercyEmotion.ANGRY else PercyEmotion.PANIC
                BlockPhase.BocksPrompt -> PercyEmotion.SKEPTICAL
                BlockPhase.ExtensionGranted -> PercyEmotion.DISAPPOINTED
                BlockPhase.WalkedAway -> PercyEmotion.HAPPY
            },
            modifier = Modifier.fillMaxWidth(),
            percySizeDp = 120
        )

        Spacer(modifier = Modifier.height(16.dp))
        AsyncImage(
            model = PackageIcon(packageName),
            contentDescription = appName,
            modifier = Modifier.size(96.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = appName,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = accentRed,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Time's up for $appName",
            color = accentRed,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "You've reached your limit for this 24h window.",
            color = Color.White.copy(alpha = 0.9f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Used this window: ${TimeUtils.formatDuration(usedMs)} / ${TimeUtils.formatDuration(limitMs)} limit",
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = resetSubtitle,
            color = Color.White.copy(alpha = 0.75f)
        )
        Spacer(modifier = Modifier.weight(1f))

        val holdSecondsLeft =
            (((1f - holdProgress) * (holdDurationMs / 1000f)).coerceAtLeast(0f)).roundToInt()

        when (vmState.blockPhase) {
            BlockPhase.LimitReached -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Press and hold for 10 seconds to add 5 minutes to your limit for this window.",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { },
                        interactionSource = holdInteractionSource,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val stroke = 8.dp.toPx()
                                drawArc(
                                    color = Color.White.copy(alpha = 0.2f),
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = accentRed,
                                    startAngle = -90f,
                                    sweepAngle = 360f * animatedProgress,
                                    useCenter = false,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                            }
                            Text(
                                text = if (isPressingHold && holdProgress < 1f) {
                                    "${holdSecondsLeft}s"
                                } else {
                                    "Hold here"
                                },
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Release before 10 seconds and progress resets.",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 12.sp
                    )
                }
            }
            BlockPhase.SecondChallenge -> {
                var answer by remember { mutableStateOf("") }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = vmState.challengeQuestion.ifBlank { "Why do you actually need more time?" },
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TextField(
                        value = answer,
                        onValueChange = { answer = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Type a real reason (10+ characters)") }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.onChallengeAnswered(packageName, appName, answer) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = answer.trim().isNotEmpty()
                    ) {
                        Text("Submit")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.onGoHomeTapped(appName) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Never mind")
                    }
                }
            }
            BlockPhase.ThirdRefusal -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (vmState.showBocksOption) {
                        Text(
                            text = "Bocks: ${vmState.bocksBalance}",
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.onSpendBocks(packageName, appName) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Spend 20 Bocks for 5 more minutes")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "This uses Bocks you've earned through discipline.",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
            BlockPhase.ExtensionGranted -> {
                Text(
                    text = "Extension granted. Don't make me regret this.",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp
                )
            }
            BlockPhase.WalkedAway -> {
                Text(
                    text = "Good. Walk away.",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp
                )
            }
            BlockPhase.FirstExtension,
            BlockPhase.BocksPrompt -> Unit
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = { viewModel.onGoHomeTapped(appName) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Go Home")
        }
        Text(
            text = "Leaving does not add time or reset usage. Opening this app again stays blocked until you use extra time or the window resets.",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp)
        )
    }
}
