package com.sonharf.game

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Higgsfield theme components; sizes and colours follow the package's SVG components. */
internal object Hf {
    // "Kelimelik tarzı" light board-game palette (theme D).
    val Ground = Color(0xFFE6ECF2)
    val Surface = Color(0xFFFFFFFF)
    val Navy = Color(0xFF2C3E55)
    val Border = Color(0xFFD2DBE5)
    /** Primary text on the ground and on white cards. */
    val Text = Color(0xFF243142)
    /** Text and icons on green, red and navy fills. */
    val OnAccent = Color(0xFFFFFFFF)
    /** Card and window fill (was the ivory surface of the dark theme). */
    val Ivory = Color(0xFFFFFDF7)
    val Gold = Color(0xFFE0A82E)
    val GoldLight = Color(0xFFF2C14E)
    val GoldDeep = Color(0xFFB07F1E)
    val Green = Color(0xFF3E9F4D)
    val GreenLight = Color(0xFF52B360)
    val GreenPressed = Color(0xFF2B7537)
    val Red = Color(0xFFD0514A)
    val RedLight = Color(0xFFE57A73)
    val Muted = Color(0xFF9AA7B5)
    val Disabled = Color(0xFFB5C0CC)
    val TextMuted = Color(0xFF6B7A8C)
    val Ink = Color(0xFF243142)
    val PlayerPanel = Color(0xFFE1F2E3)
    val RivalPanel = Color(0xFFFBE4E2)
    /** Letter tiles: warm cream with brown letters, like a word-board game. */
    val Tile = Color(0xFFF7E3A6)
    val TileInk = Color(0xFF4A3217)
    val TileBorder = Color(0xFFC9A560)
    val TileShade = Color(0xFFD9BD7A)
    // Board bonus squares.
    val BonusH2 = Color(0xFF5DADE2)
    val BonusH3 = Color(0xFFE573A5)
    val BonusK2 = Color(0xFF7DC36B)
    val BonusK3 = Color(0xFFE0914A)

    val ButtonShape = RoundedCornerShape(16.dp)
    val CardShape = RoundedCornerShape(16.dp)
    val WindowShape = RoundedCornerShape(20.dp)
    val PillShape = RoundedCornerShape(50)
}

/** Centered title flanked by thin champagne rules. */
@Composable
internal fun HfTitleRule(
    title: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 26.sp,
    color: Color = Hf.Text,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HfRule(Modifier.weight(1f))
        Text(
            title,
            modifier = Modifier.padding(horizontal = 14.dp),
            color = color,
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        HfRule(Modifier.weight(1f))
    }
}

@Composable
internal fun HfRule(modifier: Modifier = Modifier) {
    Box(
        modifier.height(1.5.dp).background(
            Brush.horizontalGradient(listOf(Hf.Gold.copy(alpha = .15f), Hf.Gold, Hf.Gold.copy(alpha = .15f))),
        ),
    )
}

private fun Modifier.hfTopHighlight(alpha: Float = .14f, inset: Dp = 18.dp): Modifier = drawBehind {
    val y = 3.dp.toPx()
    drawLine(
        Color.White.copy(alpha = alpha),
        Offset(inset.toPx(), y),
        Offset(size.width - inset.toPx(), y),
        strokeWidth = 1.5.dp.toPx(),
    )
}

/** 288×56 primary action: green gradient, light-green rim, 0.98 press scale. */
@Composable
internal fun HfPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    danger: Boolean = false,
    trailingChevron: Boolean = true,
    height: Dp = 56.dp,
    fontSize: TextUnit = 20.sp,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .98f else 1f, tween(if (pressed) 80 else 120), label = "hfPress")
    val base = when {
        !enabled -> Hf.Disabled
        danger -> Hf.Red
        pressed -> Hf.GreenPressed
        else -> Hf.Green
    }
    val rim = when {
        !enabled -> Color(0xFFC3CCD6)
        danger -> Hf.RedLight
        pressed -> Hf.Green
        else -> Hf.GreenLight
    }
    Box(
        modifier
            .scale(scale)
            .fillMaxWidth()
            .height(height)
            .shadow(if (enabled) 6.dp else 0.dp, Hf.ButtonShape, clip = false)
            .background(
                Brush.verticalGradient(listOf(base.copy(alpha = .98f), base.copy(alpha = .82f))),
                Hf.ButtonShape,
            )
            .border(2.dp, rim, Hf.ButtonShape)
            .then(if (pressed) Modifier else Modifier.hfTopHighlight())
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text,
                color = if (enabled) Hf.OnAccent else Hf.OnAccent.copy(alpha = .75f),
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                letterSpacing = .8.sp,
                maxLines = 1,
            )
            if (trailingChevron) {
                Spacer(Modifier.width(10.dp))
                Icon(Icons.Rounded.ChevronRight, null, tint = Hf.OnAccent, modifier = Modifier.size(26.dp))
            }
        }
    }
}

/** 288×56 secondary action: graphite fill with a champagne rim. */
@Composable
internal fun HfSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    trailingChevron: Boolean = false,
    height: Dp = 56.dp,
    fontSize: TextUnit = 17.sp,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .98f else 1f, tween(if (pressed) 80 else 120), label = "hfPress2")
    Box(
        modifier
            .scale(scale)
            .height(height)
            .background(Brush.verticalGradient(listOf(Hf.Surface.copy(alpha = .98f), Hf.Surface.copy(alpha = .82f))), Hf.ButtonShape)
            .border(2.dp, Hf.Gold, Hf.ButtonShape)
            .hfTopHighlight()
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = Hf.Gold, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
            }
            Text(text, color = Hf.Text, fontSize = fontSize, fontWeight = FontWeight.Black, letterSpacing = .6.sp, maxLines = 1)
            if (trailingChevron) {
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Rounded.ChevronRight, null, tint = Hf.Gold, modifier = Modifier.size(24.dp))
            }
        }
    }
}

/** Graphite card with a champagne hairline (görev kartı). */
@Composable
internal fun HfCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderColor: Color = Hf.Border,
    color: Color = Hf.Surface,
    shape: androidx.compose.ui.graphics.Shape = Hf.CardShape,
    content: @Composable () -> Unit,
) {
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = color, border = BorderStroke(1.5.dp, borderColor), content = content)
    } else {
        Surface(modifier = modifier, shape = shape, color = color, border = BorderStroke(1.5.dp, borderColor), content = content)
    }
}

/** Ivory card (mağaza kartı / pencere) with a champagne rim. */
@Composable
internal fun HfIvoryCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
    shape: androidx.compose.ui.graphics.Shape = Hf.CardShape,
    content: @Composable () -> Unit,
) {
    val border = if (selected) BorderStroke(3.dp, Hf.Green) else BorderStroke(1.5.dp, Hf.Gold)
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = Hf.Ivory, contentColor = Hf.Ink, border = border, content = content)
    } else {
        Surface(modifier = modifier, shape = shape, color = Hf.Ivory, contentColor = Hf.Ink, border = border, content = content)
    }
}

/** Outlined pill (sayaç / rozet zemini). */
@Composable
internal fun HfPill(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderColor: Color = Hf.Gold,
    color: Color = Hf.Ground,
    content: @Composable RowScope.() -> Unit,
) {
    val body: @Composable () -> Unit = {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content,
        )
    }
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = Hf.PillShape, color = color, border = BorderStroke(1.5.dp, borderColor), content = body)
    } else {
        Surface(modifier = modifier, shape = Hf.PillShape, color = color, border = BorderStroke(1.5.dp, borderColor), content = body)
    }
}

/** Gold coin drawn from coin-rozet-zemin. */
@Composable
internal fun HfCoin(size: Dp = 22.dp) {
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension / 2f
        drawCircle(
            Brush.radialGradient(listOf(Hf.GoldLight, Hf.Gold, Hf.GoldDeep), center = Offset(r * .8f, r * .7f), radius = r * 1.2f),
            radius = r,
        )
        drawCircle(Hf.Ivory.copy(alpha = .72f), radius = r * .56f, style = androidx.compose.ui.graphics.drawscope.Stroke(width = r * .15f))
    }
}

internal enum class HfTileTone { IVORY, SELECTED, PLAYER, RIVAL }

/** Letter tile (harf taşı / küp). Letters are always live text, never baked into art. */
@Composable
internal fun HfLetterTile(
    letter: String,
    size: Dp,
    modifier: Modifier = Modifier,
    tone: HfTileTone = HfTileTone.IVORY,
    fontSize: TextUnit = (size.value * .5f).sp,
    elevation: Dp = 3.dp,
) {
    val (fill, rim, ink) = when (tone) {
        HfTileTone.IVORY -> Triple(Hf.Tile, Hf.TileBorder, Hf.TileInk)
        HfTileTone.SELECTED -> Triple(Hf.GoldLight, Hf.GoldDeep, Hf.TileInk)
        HfTileTone.PLAYER -> Triple(Hf.Green, Hf.GreenLight, Hf.OnAccent)
        HfTileTone.RIVAL -> Triple(Hf.Red, Hf.RedLight, Hf.OnAccent)
    }
    val shape = RoundedCornerShape(size * .17f)
    Box(
        modifier
            .size(size)
            .shadow(elevation, shape, clip = false)
            .background(fill, shape)
            .border(1.5.dp, rim, shape)
            .drawBehind {
                drawLine(
                    Color.Black.copy(alpha = .16f),
                    Offset(this.size.width * .12f, this.size.height - 2.dp.toPx()),
                    Offset(this.size.width * .88f, this.size.height - 2.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(letter, color = ink, fontSize = fontSize, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

/** Segmented tab row (sekme): green selected, champagne-rimmed normal. */
@Composable
internal fun HfSegmentedTabs(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(1.5.dp, Hf.Gold.copy(alpha = .8f), Hf.ButtonShape)
            .padding(2.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val isSelected = index == selected
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(
                        if (isSelected) Modifier.background(Hf.Green, RoundedCornerShape(14.dp)).border(1.5.dp, Hf.GreenLight, RoundedCornerShape(14.dp))
                        else Modifier,
                    )
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (isSelected) Hf.OnAccent else Hf.Text,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Pill chip row (mağaza kategorileri). */
@Composable
internal fun HfChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 44.dp),
        shape = Hf.PillShape,
        color = if (selected) Hf.Green else Hf.Ground,
        border = BorderStroke(1.5.dp, if (selected) Hf.GreenLight else Hf.Gold),
    ) {
        Box(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(label, color = Hf.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

/** Gold round frame used around avatars (avatar-cerceve). */
internal fun Modifier.hfAvatarRing(width: Dp = 3.dp): Modifier = this
    .border(width, Brush.linearGradient(listOf(Hf.GoldLight, Hf.Gold, Hf.GoldDeep)), CircleShape)
    .padding(width + 1.dp)

@Composable
internal fun hfIcon(res: Int): Painter = painterResource(res)

@Composable
internal fun HfIconImage(res: Int, tint: Color = Hf.Gold, size: Dp = 28.dp, description: String? = null) {
    Icon(painterResource(res), description, tint = tint, modifier = Modifier.size(size))
}

@Composable
internal fun HfIconVector(icon: ImageVector, tint: Color = Hf.Gold, size: Dp = 28.dp, description: String? = null) {
    Icon(icon, description, tint = tint, modifier = Modifier.size(size))
}

/** Thin progress bar: champagne fill on a graphite track. */
@Composable
internal fun HfProgressBar(progress: Float, modifier: Modifier = Modifier, color: Color = Hf.Gold) {
    Canvas(modifier.height(8.dp)) {
        val r = CornerRadius(size.height / 2f)
        drawRoundRect(Color(0xFFD8E0E8), cornerRadius = r)
        val w = size.width * progress.coerceIn(0f, 1f)
        if (w > 0f) drawRoundRect(color, size = androidx.compose.ui.geometry.Size(w, size.height), cornerRadius = r)
    }
}

/** Reserved space for a game icon; left empty until the new game artwork arrives. */
@Composable
internal fun HfGameIconSlot(width: Dp, height: Dp = width, modifier: Modifier = Modifier) {
    Box(modifier.size(width, height))
}

/**
 * Ivory confirmation window from the 20-ayril-onayi preview: dark title and text, a green button that
 * keeps the player where they are and a (red when destructive) button for the action.
 */
@Composable
internal fun HfConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = Hf.WindowShape,
            color = Hf.Ivory,
            border = BorderStroke(2.dp, Hf.Gold),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(title, color = Hf.Ink, fontSize = 21.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Text(message, color = Color(0xFF56657A), fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                HfPrimaryButton(dismissText, onClick = onDismiss, trailingChevron = false, height = 50.dp, fontSize = 16.sp)
                HfPrimaryButton(
                    confirmText,
                    onClick = onConfirm,
                    danger = destructive,
                    trailingChevron = false,
                    height = 50.dp,
                    fontSize = 16.sp,
                )
            }
        }
    }
}
