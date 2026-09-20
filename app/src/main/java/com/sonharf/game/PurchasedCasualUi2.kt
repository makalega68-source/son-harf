package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Native Compose interpretation of the purchased Casual Game UI #02 (new + old) family.
 *
 * The vendor PNG/PSD sources are intentionally not committed because this repository is public and
 * the purchased license disallows publishing easily extractable source assets outside a finished
 * product. These primitives preserve the useful design language: glossy color families, soft white
 * panels, thick game-like controls, reward hierarchy and playful edge decoration.
 */
internal object PurchasedCasualUi2 {
    val Blue = Color(0xFF4D83DA)
    val BlueDeep = Color(0xFF2D5FAB)
    val Green = Color(0xFF52AD56)
    val GreenDeep = Color(0xFF378C45)
    val Orange = Color(0xFFF5A623)
    val Purple = Color(0xFF8A62D3)
    val Red = Color(0xFFD95A62)
    val Navy = Color(0xFF102A56)
    val Muted = Color(0xFF6B7C9E)
    val Border = Color(0xFFD9E4F2)
    val White = Color(0xFFFFFFFF)

    val PanelShape = RoundedCornerShape(22.dp)
    val CompactPanelShape = RoundedCornerShape(16.dp)
    val ButtonShape = RoundedCornerShape(15.dp)
}

@Composable
internal fun PurchasedGameBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(Color(0xFFF7FAFE))

        // Soft colored edge marks: the center remains white/quiet for legibility.
        val marks = listOf(
            Triple(.04f, .08f, PurchasedCasualUi2.Green),
            Triple(.91f, .12f, PurchasedCasualUi2.Blue),
            Triple(.08f, .34f, PurchasedCasualUi2.Purple),
            Triple(.94f, .41f, PurchasedCasualUi2.Orange),
            Triple(.03f, .68f, PurchasedCasualUi2.Blue),
            Triple(.90f, .74f, PurchasedCasualUi2.Green),
            Triple(.12f, .92f, PurchasedCasualUi2.Orange),
            Triple(.87f, .94f, PurchasedCasualUi2.Purple),
        )
        marks.forEachIndexed { index, (xf, yf, color) ->
            val w = size.minDimension * if (index % 2 == 0) .038f else .03f
            val h = w
            rotate(degrees = if (index % 2 == 0) 12f else -10f, pivot = Offset(size.width * xf, size.height * yf)) {
                drawRoundRect(
                    color = color.copy(alpha = .10f),
                    topLeft = Offset(size.width * xf - w / 2f, size.height * yf - h / 2f),
                    size = Size(w, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .24f, h * .24f),
                )
            }
        }

        // A few low-alpha bubbles borrowed from the pack's playful spacing, not its raw artwork.
        listOf(
            Offset(size.width * .15f, size.height * .17f),
            Offset(size.width * .84f, size.height * .24f),
            Offset(size.width * .12f, size.height * .82f),
            Offset(size.width * .79f, size.height * .87f),
        ).forEachIndexed { index, center ->
            drawCircle(
                color = (if (index % 2 == 0) PurchasedCasualUi2.Blue else PurchasedCasualUi2.Green).copy(alpha = .045f),
                radius = size.minDimension * (if (index % 2 == 0) .12f else .09f),
                center = center,
            )
        }
    }
}

@Composable
internal fun PurchasedGamePanel(
    modifier: Modifier = Modifier,
    accent: Color = PurchasedCasualUi2.Blue,
    emphasized: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = if (emphasized) PurchasedCasualUi2.PanelShape else PurchasedCasualUi2.CompactPanelShape
    val surfaceModifier = modifier
        .then(if (emphasized) Modifier.shadow(7.dp, shape) else Modifier)
        .clip(shape)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    Surface(
        modifier = surfaceModifier,
        shape = shape,
        color = SonHarfTheme.Surface.copy(alpha = .985f),
        border = BorderStroke(1.dp, if (emphasized) accent.copy(alpha = .27f) else SonHarfTheme.Border),
        tonalElevation = 0.dp,
    ) {
        Column(content = content)
    }
}

@Composable
internal fun PurchasedGlossButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = PurchasedCasualUi2.Green,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    minHeight: Dp = 52.dp,
) {
    val shape = PurchasedCasualUi2.ButtonShape
    Surface(
        modifier = modifier
            .heightIn(min = minHeight)
            .shadow(if (enabled) 5.dp else 0.dp, shape)
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick),
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, if (enabled) color.copy(alpha = .75f) else SonHarfTheme.Border),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    if (enabled) Brush.verticalGradient(listOf(color.copy(alpha = .86f), color))
                    else Brush.verticalGradient(listOf(SonHarfTheme.DisabledBackground, SonHarfTheme.DisabledBackground)),
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                if (icon != null) {
                    Icon(icon, null, tint = if (enabled) Color.White else SonHarfTheme.DisabledContent, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(7.dp))
                }
                Text(
                    text,
                    color = if (enabled) Color.White else SonHarfTheme.DisabledContent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun PurchasedSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Color = SonHarfTheme.Primary,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Surface(shape = RoundedCornerShape(11.dp), color = accent.copy(alpha = .12f)) {
                Icon(icon, null, tint = accent, modifier = Modifier.padding(8.dp).size(19.dp))
            }
            Spacer(Modifier.width(9.dp))
        }
        Text(
            title,
            modifier = Modifier.weight(1f),
            color = SonHarfTheme.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                Text(actionLabel, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Rounded.ChevronRight, null, tint = accent, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
internal fun PurchasedStatPill(
    text: String,
    modifier: Modifier = Modifier,
    accent: Color = SonHarfTheme.Primary,
    icon: ImageVector? = null,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(99.dp),
        color = accent.copy(alpha = .10f),
        border = BorderStroke(1.dp, accent.copy(alpha = .18f)),
    ) {
        Row(
            Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(text, color = SonHarfTheme.TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}
