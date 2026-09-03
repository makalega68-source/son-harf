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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

internal const val PURCHASED_BOARD_PLACE_VFX_MS = 360
internal const val PURCHASED_BOARD_RESOLVE_VFX_MS = 620

internal enum class PurchasedBoardVfxKind { PLACEMENT, RESOLVED }
private val PurchasedBoardVfxDirections = listOf(-.9f to -.6f, .9f to -.45f, .1f to .9f)

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

/** Short, clipped and input-transparent feedback for actions inside a board cell. */
@Composable
internal fun PurchasedBoardActionVfx(
    eventKey: String,
    kind: PurchasedBoardVfxKind,
    modifier: Modifier = Modifier,
) {
    val progress = remember(eventKey, kind) { Animatable(0f) }
    val durationMs = when (kind) {
        PurchasedBoardVfxKind.PLACEMENT -> PURCHASED_BOARD_PLACE_VFX_MS
        PurchasedBoardVfxKind.RESOLVED -> PURCHASED_BOARD_RESOLVE_VFX_MS
    }
    LaunchedEffect(eventKey, kind) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMs))
    }

    val p = progress.value
    val envelope = if (p < .16f) p / .16f else ((1f - p) / .84f).coerceIn(0f, 1f)
    val tint = if (kind == PurchasedBoardVfxKind.PLACEMENT) MainUi.Blue else MainUi.Gold
    val count = if (kind == PurchasedBoardVfxKind.PLACEMENT) 2 else 3
    val distance = if (kind == PurchasedBoardVfxKind.PLACEMENT) 9f else 13f

    Box(modifier.fillMaxSize().clipToBounds(), contentAlignment = Alignment.Center) {
        repeat(count) { index ->
            val (xDirection, yDirection) = PurchasedBoardVfxDirections[index]
            Image(
                painter = painterResource(R.drawable.vfx_twinkle),
                contentDescription = null,
                colorFilter = ColorFilter.tint(tint),
                modifier = Modifier
                    .offset(
                        x = (xDirection * distance * p).dp,
                        y = (yDirection * distance * p).dp,
                    )
                    .size((7f + p * 5f + (index % 2) * 2f).dp)
                    .rotate(index * 47f + p * 75f)
                    .alpha(envelope * if (kind == PurchasedBoardVfxKind.PLACEMENT) .42f else .58f),
            )
        }
    }
}
