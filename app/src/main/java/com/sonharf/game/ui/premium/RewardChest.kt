package com.sonharf.game.ui.premium

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sonharf.game.R
import com.sonharf.game.SonHarfTheme
import com.sonharf.game.ui.vfx.LocalVfx
import com.sonharf.game.ui.vfx.VfxEvent
import kotlin.math.PI
import kotlin.math.sin

/**
 * G3.6 — Günlük ödül sandığı.
 *
 * Idle: yumuşak sallanma (yavaş wiggle). Dokununca açılır:
 *   1) hafif ölçek pompası (1.0 -> 1.18 -> 1.0)
 *   2) vfx_flare_cross parlar (üst katman)
 *   3) LocalVfx.play(VfxEvent.Reward) yıldız yağmuru tetikler
 *   4) [onOpened] callback çağırılır (ödül sunucudan alınır)
 *
 * "Animasyonları azalt" açıksa wiggle durur; sadece parlama kalır.
 */
@Composable
fun RewardChest(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 128.dp,
    tint: Color = SonHarfTheme.GoldBright,
    onOpened: () -> Unit = {},
) {
    val reducedMotion = rememberReducedMotion()
    var opened by remember { mutableStateOf(false) }
    val openScale = remember { Animatable(1f) }
    val vfx = LocalVfx.current

    val transition = rememberInfiniteTransition(label = "chest-wiggle")
    val wiggleT by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (reducedMotion || opened) 0f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "chest-t",
    )
    val wiggleAngle = if (reducedMotion || opened) 0f
    else (sin(wiggleT * 2 * PI.toFloat()) * 6f)

    LaunchedEffect(opened) {
        if (opened) {
            openScale.snapTo(1f)
            openScale.animateTo(1.18f, tween(140))
            openScale.animateTo(1f, tween(220))
            vfx.play(VfxEvent.Reward(Offset.Zero))
            onOpened()
        }
    }

    Box(
        modifier = modifier
            .size(sizeDp)
            .graphicsLayer(
                rotationZ = wiggleAngle,
                scaleX = openScale.value,
                scaleY = openScale.value,
            )
            .let { m ->
                if (opened) m else m.clickable { opened = true }
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.vfx_light_burst_gold),
            contentDescription = "Ödül sandığı",
            colorFilter = ColorFilter.tint(tint, BlendMode.Modulate),
            modifier = Modifier.size(sizeDp),
        )
        if (opened) {
            Image(
                painter = painterResource(id = R.drawable.vfx_flare_cross),
                contentDescription = null,
                colorFilter = ColorFilter.tint(SonHarfTheme.GoldPale, BlendMode.Modulate),
                modifier = Modifier.size(sizeDp * 1.2f),
            )
        }
    }
}
