package com.timebox.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

@Composable
fun PercyWidget(
    dialogueLine: String,
    emotion: PercyEmotion = PercyEmotion.CALM,
    modifier: Modifier = Modifier,
    percySizeDp: Int = 80
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(
            painter = painterResource(id = emotion.toDrawableRes()),
            contentDescription = "Percy is ${emotion.name.lowercase()}",
            modifier = Modifier.size(percySizeDp.dp)
        )

        if (dialogueLine.isNotBlank()) {
            SpeechBubble(
                text = dialogueLine,
                modifier = Modifier.weight(1f)
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
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

