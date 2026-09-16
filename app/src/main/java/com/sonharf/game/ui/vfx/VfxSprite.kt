package com.sonharf.game.ui.vfx

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * Draws a single VFX texture as an Image with rotation/scale/alpha/tint (G3.0).
 * Uses `Modifier.graphicsLayer` for transforms so no Canvas math is needed.
 */
@Composable
fun VfxSprite(
    painter: Painter,
    center: Offset,
    sizeDp: Float,
    alpha: Float = 1f,
    scale: Float = 1f,
    rotationDeg: Float = 0f,
    tint: Color? = null,
) {
    val density = LocalDensity.current
    val halfPx = with(density) { (sizeDp / 2f).dp.toPx() }
    val offsetPx = Offset(center.x - halfPx, center.y - halfPx)
    Image(
        painter = painter,
        contentDescription = null,
        colorFilter = tint?.let { ColorFilter.tint(it, BlendMode.Modulate) },
        modifier = Modifier
            .offset { androidx.compose.ui.unit.IntOffset(offsetPx.x.toInt(), offsetPx.y.toInt()) }
            .size(sizeDp.dp)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                rotationZ = rotationDeg,
                alpha = alpha.coerceIn(0f, 1f),
            ),
    )
}

/** Convenience painter loader that returns null when the resource id is 0. */
@Composable
fun vfxPainterOrNull(resId: Int): Painter? =
    if (resId == 0) null else painterResource(id = resId)
