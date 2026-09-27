package com.sonharf.game

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Which face a key wears: a letter, a secondary key (clear/backspace) or the action key (send). */
internal enum class KeyKind { LETTER, ALT, ACTION }

/**
 * The keyboard tray: a vertical gradient with the skin's rim, like the frame around the keys in
 * the store image. Skins without a tray gradient keep their flat background.
 */
internal fun Modifier.keyboardTray(p: WordKeyboardPalette, shape: Shape): Modifier {
    val top = p.trayTop ?: return this
    val rim = p.rim
    val tray = background(Brush.verticalGradient(listOf(top, p.background)), shape)
    return if (rim == null) tray else tray.border(
        1.5.dp,
        Brush.verticalGradient(listOf(lerp(rim, Color.White, .45f), rim, lerp(rim, Color.Black, .25f))),
        shape,
    )
}

/**
 * Paints one key the way its store image shows it: a glossy gradient face, a coloured glow
 * underneath (the neon/crystal light in the art), cut facets for the crystal set and a thin rim.
 * The default keyboard (no gradient in its palette) keeps its flat cream keys.
 */
internal fun Modifier.keyFace(p: WordKeyboardPalette, kind: KeyKind, enabled: Boolean, radius: Dp): Modifier = drawBehind {
    val alpha = if (enabled) 1f else .55f
    val r = CornerRadius(radius.toPx())
    val bottom = when (kind) { KeyKind.ACTION -> p.action; KeyKind.ALT -> p.keyAlt; KeyKind.LETTER -> p.key }
    val top = when (kind) { KeyKind.ACTION -> p.actionTop; KeyKind.ALT -> p.altTop; KeyKind.LETTER -> p.keyTop } ?: bottom
    val rimColor = when (kind) {
        KeyKind.ACTION -> p.action.copy(alpha = .82f)
        KeyKind.ALT -> p.secondaryBorder.copy(alpha = if (p.keyTop == null) .55f else .9f)
        KeyKind.LETTER -> p.border
    }
    val glow = p.glow
    // Light spilling out from under the key.
    if (glow != null) {
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color.Transparent, glow.copy(alpha = .85f * alpha)),
                startY = size.height * .45f,
                endY = size.height + 3.dp.toPx(),
            ),
            topLeft = Offset(-1.dp.toPx(), 1.5.dp.toPx()),
            size = Size(size.width + 2.dp.toPx(), size.height + 1.dp.toPx()),
            cornerRadius = r,
        )
    }
    drawRoundRect(Brush.verticalGradient(listOf(top, bottom)), cornerRadius = r, alpha = alpha)
    if (p.crystal) {
        // Cut-glass facets: a bright upper-left plane, a deeper lower-right plane and a table line.
        val w = size.width
        val h = size.height
        val clip = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, w, h, r)) }
        clipPath(clip) {
            drawPath(
                Path().apply { moveTo(0f, 0f); lineTo(w * .62f, 0f); lineTo(w * .3f, h * .52f); lineTo(0f, h * .78f); close() },
                Color.White.copy(alpha = .38f * alpha),
            )
            drawPath(
                Path().apply { moveTo(w, h); lineTo(w * .35f, h); lineTo(w * .72f, h * .46f); lineTo(w, h * .2f); close() },
                bottom.copy(alpha = .55f * alpha),
            )
            drawLine(Color.White.copy(alpha = .7f * alpha), Offset(w * .18f, h * .2f), Offset(w * .5f, h * .2f), strokeWidth = 1.2.dp.toPx())
        }
    }
    if (p.keyTop != null) {
        // Glossy top half, as on the polished keys in the store images.
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = .28f * alpha), Color.Transparent), endY = size.height * .5f),
            size = Size(size.width, size.height * .5f),
            cornerRadius = r,
        )
    }
    if (glow != null) {
        drawLine(
            glow.copy(alpha = .95f * alpha),
            Offset(r.x, size.height - 1.2.dp.toPx()),
            Offset(size.width - r.x, size.height - 1.2.dp.toPx()),
            strokeWidth = 1.6.dp.toPx(),
        )
    }
    drawRoundRect(rimColor.copy(alpha = rimColor.alpha * alpha), cornerRadius = r, style = Stroke(1.dp.toPx()))
}

/** A keyboard key drawn with the equipped skin; presses shrink it slightly like a real key. */
@Composable
internal fun SkinKey(
    kind: KeyKind,
    enabled: Boolean,
    radius: Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val palette = SonHarfCosmetics.keyboardPalette
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier
            .graphicsLayer { val s = if (pressed) .94f else 1f; scaleX = s; scaleY = s }
            .shadow(if (kind == KeyKind.ACTION) 5.dp else 1.5.dp, RoundedCornerShape(radius), clip = false)
            .keyFace(palette, kind, enabled, radius)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Label colour for a key of [kind] with the equipped skin. */
internal fun WordKeyboardPalette.labelColor(kind: KeyKind, enabled: Boolean): Color = when {
    !enabled -> text.copy(alpha = .42f)
    kind == KeyKind.ACTION -> actionText
    else -> text
}
