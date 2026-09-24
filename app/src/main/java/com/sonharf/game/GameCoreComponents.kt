package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal fun gameText(tr: String, en: String): String =
    if (SonHarfUiState.language.lowercase().startsWith("en")) en else tr

@Composable
internal fun GameSurface(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    borderColor: Color = GameColors.Border,
    content: @Composable ColumnScope.() -> Unit,
) {
    val surfacePainter = painterResource(R.drawable.theme_pack_new_surface)
    Surface(
        modifier = modifier.paint(surfacePainter, sizeToIntrinsics = false, contentScale = ContentScale.FillBounds),
        shape = GameShapes.Large,
        color = Color.Transparent,
        shadowElevation = if (elevated) GameElevation.Low else GameElevation.Flat,
    ) {
        Column(Modifier.padding(GameSpacing.Lg), content = content)
    }
}

@Composable
internal fun GameSectionHeader(title: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), color = GameColors.TextPrimary, style = MaterialTheme.typography.titleMedium)
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, color = GameColors.PrimaryBlue, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun GameActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    backgroundRes: Int,
    enabled: Boolean,
    icon: ImageVector?,
) {
    val resolvedBackground = if (enabled) backgroundRes else R.drawable.theme_pack_old_button_dark
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .heightIn(min = 48.dp)
            .paint(painterResource(resolvedBackground), sizeToIntrinsics = false, contentScale = ContentScale.FillBounds),
        shape = GameShapes.Medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = GameColors.DisabledContent,
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun GamePrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null) =
    GameActionButton(text, onClick, modifier, R.drawable.theme_pack_old_button_green, enabled, icon)

@Composable
internal fun GameSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null) =
    GameActionButton(text, onClick, modifier, R.drawable.theme_pack_old_button_blue, enabled, icon)

@Composable
internal fun GameTertiaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null) =
    GameActionButton(text, onClick, modifier, R.drawable.theme_pack_old_button_dark, enabled, icon)

@Composable
internal fun GameDangerButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null) =
    GameActionButton(text, onClick, modifier, R.drawable.theme_pack_old_button_red, enabled, icon)

@Composable
internal fun GameIconButton(
    icon: ImageVector,
    description: String = "",
    onClick: () -> Unit,
    tint: Color = GameColors.TextPrimary,
    contentDescription: String? = null,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(48.dp),
        shape = CircleShape,
        color = GameColors.SecondarySurface,
        border = BorderStroke(1.dp, if (enabled) GameColors.Border else GameColors.Divider),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription ?: description,
                tint = if (enabled) tint else GameColors.DisabledContent,
                modifier = Modifier.size(23.dp),
            )
        }
    }
}

@Composable
internal fun GameStatChip(icon: ImageVector, text: String, accent: Color = GameColors.PrimaryBlue, modifier: Modifier = Modifier) {
    Surface(modifier, shape = GameShapes.Pill, color = accent.copy(alpha = .14f)) {
        Row(Modifier.padding(horizontal = 9.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
            Text(text, color = GameColors.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
internal fun LeagueBadge(text: String) {
    Surface(shape = GameShapes.Pill, color = GameColors.PrestigeGold.copy(alpha = .14f), border = BorderStroke(1.dp, GameColors.PrestigeGold.copy(alpha = .35f))) {
        Row(Modifier.padding(horizontal = 9.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.MilitaryTech, null, tint = GameColors.PrestigeGold, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(text, color = GameColors.PrestigeGold, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
internal fun RatingBadge(rating: Int) = GameStatChip(Icons.Rounded.EmojiEvents, "$rating RP", GameColors.RewardAmber)

@Composable
internal fun CurrencyChip(amount: Int) {
    Surface(shape = GameShapes.Pill, color = GameColors.RewardAmber.copy(alpha = .14f)) {
        Row(Modifier.padding(horizontal = 9.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            GameCoinIcon(17.dp)
            Spacer(Modifier.width(5.dp))
            Text(amount.toString(), color = GameColors.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
internal fun GameProgress(progress: Float, color: Color, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier.fillMaxWidth().height(6.dp),
        color = color,
        trackColor = GameColors.SecondarySurface,
    )
}

@Composable
internal fun MissionProgress(progress: Float, modifier: Modifier = Modifier) = GameProgress(progress, GameColors.PlayGreen, modifier)

@Composable
internal fun XPProgress(progress: Float, modifier: Modifier = Modifier) = GameProgress(progress, GameColors.PrimaryBlue, modifier)

@Composable
internal fun LeagueProgress(progress: Float, modifier: Modifier = Modifier) = GameProgress(progress, GameColors.PrestigeGold, modifier)

@Composable
internal fun GameEmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    GameSurface(modifier = modifier) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GameBadgeIcon(icon, GameColors.PrimaryBlue, size = 48.dp)
            Text(title, color = GameColors.TextPrimary, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Text(body, color = GameColors.TextSecondary, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            if (actionText != null && onAction != null) {
                GameSecondaryButton(actionText, onAction, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
