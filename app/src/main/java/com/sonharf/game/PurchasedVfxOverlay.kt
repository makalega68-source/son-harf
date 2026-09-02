package com.sonharf.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/** Cosmetic-only, bounded Compose adaptation of a purchased Eric Wang VFX texture. */
@Composable
internal fun PurchasedVictoryVfx(eventKey: String, modifier: Modifier = Modifier) {
    val progress = remember(eventKey) { Animatable(0f) }
    LaunchedEffect(eventKey) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(1050))
    }
    val p = progress.value
    val alpha = if (p < .18f) p / .18f else (1f - p).coerceIn(0f, 1f)
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        repeat(8) { i ->
            val dx = ((i % 4) - 1.5f) * (34f + 54f * p)
            val dy = ((i / 4) * 2 - 1) * (30f + 54f * p)
            Image(
                painterResource(R.drawable.vfx_twinkle),
                null,
                Modifier.offset(dx.dp, dy.dp).size((15f + (i % 3) * 3f).dp).rotate(i * 31f + p * 100f).alpha(alpha),
            )
        }
        Image(painterResource(R.drawable.vfx_twinkle), null, Modifier.offset(y = (-54).dp).size(42.dp).rotate(p * 90f).alpha(alpha))
    }
}
