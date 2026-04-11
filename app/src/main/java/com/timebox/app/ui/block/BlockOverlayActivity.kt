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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.timebox.app.data.repository.AppLimitRepository
import com.timebox.app.service.LimitBlockRegistry
import com.timebox.app.util.AppInfoHelper
import com.timebox.app.util.BypassAllowance
import com.timebox.app.util.PackageIcon
import com.timebox.app.util.TimeUtils
import com.timebox.app.util.UsageStatsHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject

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
                        LimitBlockRegistry.clearBlock(packageName)
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
    onBypassComplete: () -> Unit
) {
    BackHandler { }

    val bg = Color(0xFF0A0A0A)
    val accentRed = Color(0xFFC62828)

    var appName by remember { mutableStateOf(packageName) }
    var limitMs by remember { mutableLongStateOf(0L) }
    var usedMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(packageName) {
        appName = appInfoHelper.getAppName(packageName)
        val limit = appLimitRepository.getLimitByPackage(packageName)
        limitMs = limit?.dailyLimitMs ?: 0L
        usedMs = usageStatsHelper.getTodayUsageMs(packageName)
    }

    var bypassStage by remember { mutableIntStateOf(0) }
    var countdownLeft by remember { mutableIntStateOf(30) }

    LaunchedEffect(bypassStage) {
        if (bypassStage != 1) return@LaunchedEffect
        for (i in 30 downTo 1) {
            countdownLeft = i
            delay(1000)
        }
        bypassStage = 2
    }

    val progress = if (bypassStage == 1) {
        (30 - countdownLeft) / 30f
    } else {
        0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300),
        label = "bypassProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
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
            text = "You've reached your daily limit.",
            color = Color.White.copy(alpha = 0.9f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Used today: ${TimeUtils.formatDuration(usedMs)} / ${TimeUtils.formatDuration(limitMs)} limit",
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Resets at midnight",
            color = Color.White.copy(alpha = 0.75f)
        )
        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onGoHome,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Go Home")
        }
        Spacer(modifier = Modifier.height(12.dp))
        when (bypassStage) {
            0 -> TextButton(onClick = {
                countdownLeft = 30
                bypassStage = 1
            }) {
                Text("I need more time", color = Color.White.copy(alpha = 0.85f))
            }
            1 -> {
                Box(
                    modifier = Modifier.size(120.dp),
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
                        text = "${countdownLeft}s",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This bypass won't be saved.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
            2 -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "This bypass won't be saved.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onBypassComplete) {
                        Text("Bypass for 5 minutes")
                    }
                }
            }
        }
    }
}
