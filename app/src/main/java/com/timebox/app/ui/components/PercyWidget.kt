package com.timebox.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.timebox.app.R

enum class PercyEmotion {
    CALM,
    HAPPY,
    WORRIED,
    SKEPTICAL,
    ANGRY,
    PANIC,
    DISAPPOINTED,
    SLEEPING
}

@DrawableRes
fun PercyEmotion.toDrawableRes(): Int = when (this) {
    PercyEmotion.CALM -> R.drawable.percy_calm
    PercyEmotion.HAPPY -> R.drawable.percy_happy
    PercyEmotion.WORRIED -> R.drawable.percy_worried
    PercyEmotion.SKEPTICAL -> R.drawable.percy_skeptical
    PercyEmotion.ANGRY -> R.drawable.percy_angry
    PercyEmotion.PANIC -> R.drawable.percy_panic
    PercyEmotion.DISAPPOINTED -> R.drawable.percy_disappointed
    PercyEmotion.SLEEPING -> R.drawable.percy_sleeping
}

/**
 * Percy + speech bubble. Uses a **column** layout so the mascot is not squeezed beside long text
 * (which was clipping him in a tight [Row]). Vector assets include negative path coordinates; the
 * drawable viewports were expanded so the full chicken renders.
 */
@Composable
fun PercyWidget(
    dialogueLine: String,
    emotion: PercyEmotion = PercyEmotion.CALM,
    modifier: Modifier = Modifier,
    percySizeDp: Int = 80
) {
    val artHeight = (percySizeDp + 28).dp
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(artHeight),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = emotion.toDrawableRes()),
                contentDescription = "Percy is ${emotion.name.lowercase()}",
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .fillMaxWidth()
                    .height(artHeight),
                contentScale = ContentScale.Fit
            )
        }
        if (dialogueLine.isNotBlank()) {
            SpeechBubble(
                text = dialogueLine,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun SpeechBubble(
    text: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161618))
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            AnimatedContent(
                targetState = text,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "percySpeech"
            ) { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}
