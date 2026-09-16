package com.sonharf.game.ui.premium

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.R
import com.sonharf.game.SonHarfTheme

/**
 * G3.6 — Maskot tepkileri.
 *
 * Mevcut maskot sistemi zaten var; bu overlay küçük bir vfx_twinkle
 * parıltı bulutu + reaksiyon emojisi göstererek maskotun etrafında
 * tepki verir. Yeni maskot EKLENMEZ (spec: "yeni maskot EKLEME").
 *
 * Kullanım:
 *   Box {
 *       MascotView(...)   // caller'ın mevcut maskot
 *       MascotReactionOverlay(
 *           reaction = MascotReaction.HAPPY,
 *           triggerKey = someEventId,
 *           modifier = Modifier.align(Alignment.TopEnd),
 *       )
 *   }
 */
enum class MascotReaction(val emoji: String, val tintAlpha: Float) {
    HAPPY("✨", 0.9f),
    WOW("😮", 1.0f),
    SAD("💧", 0.7f),
}

@Composable
fun MascotReactionOverlay(
    reaction: MascotReaction?,
    triggerKey: Any?,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 56.dp,
) {
    val reducedMotion = rememberReducedMotion()
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.6f) }

    LaunchedEffect(triggerKey) {
        if (reaction == null || triggerKey == null) return@LaunchedEffect
        alpha.snapTo(0f); scale.snapTo(0.6f)
        alpha.animateTo(1f, tween(if (reducedMotion) 60 else 180))
        scale.animateTo(1.15f, tween(if (reducedMotion) 40 else 180))
        scale.animateTo(1f, tween(140))
        // Hold ~600ms then fade.
        kotlinx.coroutines.delay(600)
        alpha.animateTo(0f, tween(if (reducedMotion) 60 else 220))
    }

    val current = reaction ?: return
    Box(
        modifier = modifier
            .size(sizeDp)
            .graphicsLayer(alpha = alpha.value, scaleX = scale.value, scaleY = scale.value),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.vfx_twinkle),
            contentDescription = null,
            colorFilter = ColorFilter.tint(
                color = SonHarfTheme.GoldPale.copy(alpha = current.tintAlpha),
                blendMode = BlendMode.Modulate,
            ),
            modifier = Modifier.size(sizeDp),
        )
        Text(
            text = current.emoji,
            style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Black),
            modifier = Modifier.padding(2.dp),
        )
    }
}
