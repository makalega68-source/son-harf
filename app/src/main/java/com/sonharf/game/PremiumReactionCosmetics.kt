package com.sonharf.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val VipEmojiReactions = listOf("👑", "⚡", "🔥", "👏", "😎", "✨")

/** Cosmetic-only quick reactions. They reuse the normal chat transport and never affect gameplay. */
@Composable
internal fun VipEmojiReactionRow(
    enabled: Boolean = true,
    onSend: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        VipEmojiReactions.forEach { reaction ->
            Surface(
                modifier = Modifier
                    .size(38.dp)
                    .clickable(enabled = enabled) { onSend(reaction) },
                shape = CircleShape,
                color = SonHarfTheme.PrimarySoft,
                shadowElevation = 1.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(reaction, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * One-shot win celebration for the Crown Victory cosmetic. The product art stays text-free;
 * this overlay is decorative and input-transparent.
 */
@Composable
internal fun CrownVictoryCelebration(
    eventKey: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val progress = remember(eventKey) { Animatable(0f) }
    LaunchedEffect(eventKey) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(1100))
    }
    val p = progress.value
    val envelope = when {
        p < .18f -> p / .18f
        p < .72f -> 1f
        else -> ((1f - p) / .28f).coerceIn(0f, 1f)
    }
    val crownBase = if (compact) 44f else 86f
    val crownTravel = if (compact) 10f else 22f
    val crownTop = if (compact) 4.dp else 22.dp
    val sparkTop = if (compact) 44.dp else 96.dp
    val sparkSize = if (compact) 14.sp else 24.sp

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Image(
            painter = painterResource(R.drawable.store_art_victory_crown),
            contentDescription = null,
            modifier = Modifier
                .padding(top = crownTop)
                .size((crownBase + crownTravel * p).dp)
                .rotate(-4f + 8f * p)
                .alpha(envelope.coerceIn(0f, 1f)),
        )
        Text(
            text = "✦  ✦  ✦",
            color = Color(0xFFE0B45C),
            fontSize = sparkSize,
            modifier = Modifier.padding(top = sparkTop).alpha(envelope * .92f),
        )
    }
}
