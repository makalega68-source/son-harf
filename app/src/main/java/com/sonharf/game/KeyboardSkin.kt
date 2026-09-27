package com.sonharf.game

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/** Which face a key wears: a letter, a secondary key (clear/backspace) or the action key (send). */
internal enum class KeyKind { LETTER, ALT, ACTION }

/**
 * The keyboard tray. Image skins draw their painted panel behind the keys in slices, so the crown
 * and corners keep their shape at any keyboard size and only the flat middle stretches, and pad
 * the keys into the panel's key well. Other skins get a gradient with a rim, or stay flat.
 * The panel never takes touches: it is drawn behind the real key buttons.
 */
@Composable
internal fun Modifier.keyboardTray(p: WordKeyboardPalette, shape: Shape, crownBand: Dp = 30.dp): Modifier {
    val panel = rememberSkinImage(p.panelImage)
    if (panel != null) {
        val iw = panel.width
        val ih = panel.height
        // Source slices, as fractions of the panel: key well 7.5% in from the sides, 29% from the
        // top (crown band) and 14% from the bottom; the crown is the middle 33.6% of the top band.
        val side = (iw * .075f).toInt()
        val top = (ih * .29f).toInt()
        val bottom = (ih * .14f).toInt()
        val crown = (iw * .168f).toInt()
        val dpPerPx = crownBand / top.toFloat()
        return drawBehind {
            val s = crownBand.toPx() / top
            val w = size.width
            val h = size.height
            val dSide = side * s
            val dTop = top * s
            val dBottom = bottom * s
            val c0 = maxOf(dSide, w / 2f - crown * s)
            val c1 = minOf(w - dSide, w / 2f + crown * s)
            drawSlices(panel, intArrayOf(0, side, iw / 2 - crown, iw / 2 + crown, iw - side, iw), intArrayOf(0, top), floatArrayOf(0f, dSide, c0, c1, w - dSide, w), floatArrayOf(0f, dTop))
            drawSlices(panel, intArrayOf(0, side, iw - side, iw), intArrayOf(top, ih - bottom, ih), floatArrayOf(0f, dSide, w - dSide, w), floatArrayOf(dTop, h - dBottom, h))
        }.padding(start = dpPerPx * side, end = dpPerPx * side, top = crownBand, bottom = dpPerPx * bottom)
    }
    val top = p.trayTop ?: return this
    val rim = p.rim
    val tray = background(Brush.verticalGradient(listOf(top, p.background)), shape)
    return if (rim == null) tray else tray.border(
        1.5.dp,
        Brush.verticalGradient(listOf(lerp(rim, Color.White, .45f), rim, lerp(rim, Color.Black, .25f))),
        shape,
    )
}

/** Draws [image] as a grid: source cell (sx[i]..sx[i+1], sy[j]..sy[j+1]) into destination cell. */
internal fun DrawScope.drawSlices(image: ImageBitmap, sx: IntArray, sy: IntArray, dx: FloatArray, dy: FloatArray, colorFilter: ColorFilter? = null) {
    for (j in 0 until sy.size - 1) for (i in 0 until sx.size - 1) {
        val sw = sx[i + 1] - sx[i]
        val sh = sy[j + 1] - sy[j]
        val x0 = dx[i].roundToInt()
        val y0 = dy[j].roundToInt()
        val dw = dx[i + 1].roundToInt() - x0
        val dh = dy[j + 1].roundToInt() - y0
        if (sw <= 0 || sh <= 0 || dw <= 0 || dh <= 0) continue
        drawImage(
            image,
            srcOffset = IntOffset(sx[i], sy[j]),
            srcSize = IntSize(sw, sh),
            dstOffset = IntOffset(x0, y0),
            dstSize = IntSize(dw, dh),
            filterQuality = FilterQuality.Medium,
            colorFilter = colorFilter,
        )
    }
}

/** Key and panel images are decoded once per process and shared by every key. */
private object SkinImages {
    private val cache = HashMap<Int, ImageBitmap?>()

    @Synchronized
    fun get(context: Context, res: Int): ImageBitmap? = cache.getOrPut(res) {
        runCatching {
            BitmapFactory.decodeResource(context.resources, res, BitmapFactory.Options().apply { inScaled = false })?.asImageBitmap()
        }.getOrNull()
    }
}

@Composable
internal fun rememberSkinImage(res: Int?): ImageBitmap? {
    if (res == null) return null
    val context = LocalContext.current
    return remember(res) { SkinImages.get(context, res) }
}

private val PressedBrightness = ColorFilter.colorMatrix(ColorMatrix().apply { setToScale(1.18f, 1.18f, 1.18f, 1f) })

/**
 * Paints one key the way its store image shows it: a glossy gradient face, a coloured glow
 * underneath (the neon/crystal light in the art), cut facets for the crystal set and a thin rim.
 * The default keyboard (no gradient in its palette) keeps its flat cream keys.
 */
internal fun Modifier.keyFace(
    p: WordKeyboardPalette,
    kind: KeyKind,
    enabled: Boolean,
    radius: Dp,
    image: ImageBitmap? = null,
    pressed: Boolean = false,
): Modifier = drawBehind {
    val alpha = if (enabled) 1f else .55f
    if (image != null) {
        // The painted key, sliced so the rim keeps its thickness on wide keys (send, clear).
        // Pressed: 2 dp down and brighter, as in the design notes; disabled keys fade.
        val iw = image.width
        val ih = image.height
        val sc = (minOf(iw, ih) * .3f).toInt()
        val w = size.width
        val h = size.height
        val dc = minOf(minOf(w, h) * .3f, minOf(w, h) / 2f)
        val shift = if (pressed) 2.dp.toPx() else 0f
        drawContext.canvas.saveLayer(androidx.compose.ui.geometry.Rect(0f, -shift, w, h + shift), androidx.compose.ui.graphics.Paint().apply { this.alpha = alpha })
        drawSlices(
            image,
            intArrayOf(0, sc, iw - sc, iw),
            intArrayOf(0, sc, ih - sc, ih),
            floatArrayOf(0f, dc, w - dc, w),
            floatArrayOf(shift, dc + shift, h - dc + shift, h + shift),
            colorFilter = if (pressed) PressedBrightness else null,
        )
        drawContext.canvas.restore()
        return@drawBehind
    }
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
    palette: WordKeyboardPalette = SonHarfCosmetics.keyboardPalette,
    interactive: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val image = rememberSkinImage(palette.keyImage)
    val base = modifier.graphicsLayer { val s = if (pressed) .96f else 1f; scaleX = s; scaleY = s }
    val faced = if (image != null) {
        base.keyFace(palette, kind, enabled, radius, image, pressed)
    } else {
        base.shadow(if (kind == KeyKind.ACTION) 5.dp else 1.5.dp, RoundedCornerShape(radius), clip = false)
            .keyFace(palette, kind, enabled, radius)
    }
    Box(
        if (interactive) faced.clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick) else faced,
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

/**
 * Store and collection preview of a keyboard skin: the real keyboard component (same panel, same
 * key painting, same letters) drawn small and without touch, so what is sold is what is used.
 */
@Composable
internal fun KeyboardSkinPreview(themeId: String, modifier: Modifier = Modifier) {
    val palette = SonHarfCosmetics.keyboardPaletteFor(themeId)
    val rows = listOf(listOf("Q", "W", "E", "R", "T", "Y"), listOf("A", "S", "D", "F", "G"), listOf("Z", "X", "C", "V"))
    Column(
        modifier
            .keyboardTray(palette, RoundedCornerShape(10.dp), crownBand = 16.dp)
            .padding(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        rows.forEachIndexed { index, row ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = (index * 6).dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                row.forEach { letter ->
                    SkinKey(
                        KeyKind.LETTER,
                        enabled = true,
                        radius = 4.dp,
                        modifier = Modifier.weight(1f).height(18.dp),
                        onClick = {},
                        palette = palette,
                        interactive = false,
                    ) {
                        Text(letter, color = palette.labelColor(KeyKind.LETTER, true), fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}
