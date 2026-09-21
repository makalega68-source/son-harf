package com.sonharf.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Kelime Kuşatması match presentation. The board remains the existing tactical territory map;
 * this layer only owns HUD presentation and therefore cannot alter gameplay state.
 */
internal object WordSiegeGameUi {
    val Background: Color get() = SonHarfTheme.Background
    val Surface: Color get() = SonHarfTheme.Surface
    val SurfaceSoft: Color get() = SonHarfTheme.GameSurface
    val Text: Color get() = SonHarfTheme.TextPrimary
    val Muted: Color get() = SonHarfTheme.TextSecondary
    val Border: Color get() = SonHarfTheme.GameTileBorder
    val Blue: Color get() = SonHarfTheme.Primary
    val Red: Color get() = SonHarfTheme.Purple
    val Gold: Color get() = SonHarfTheme.ActionOrange
    val DisabledBackground: Color get() = SonHarfTheme.DisabledBackground
    val DisabledContent: Color get() = SonHarfTheme.DisabledContent
}

@Composable
internal fun WordSiegeGameTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = SonHarfTheme.Primary,
            onPrimary = Color.White,
            secondary = SonHarfTheme.Turquoise,
            onSecondary = Color.White,
            tertiary = SonHarfTheme.Purple,
            onTertiary = Color.White,
            background = WordSiegeGameUi.Background,
            onBackground = WordSiegeGameUi.Text,
            surface = WordSiegeGameUi.Surface,
            onSurface = WordSiegeGameUi.Text,
            surfaceVariant = WordSiegeGameUi.SurfaceSoft,
            onSurfaceVariant = WordSiegeGameUi.Muted,
            outline = WordSiegeGameUi.Border,
            error = SonHarfTheme.Error,
        ),
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content,
    )
}

@Composable
internal fun WordSiegeScoreCard(
    name: String,
    score: Int,
    wordPoints: Int,
    territoryPoints: Int,
    area: Int,
    accent: Color,
    active: Boolean,
    leading: Boolean,
    avatarPath: String?,
    gender: String?,
    avatarVisible: Boolean,
    isBot: Boolean,
    frameId: String? = null,
    isPro: Boolean = false,
    modifier: Modifier = Modifier,
) {
    PurchasedPanel(
        modifier = modifier,
        asset = if (active) PurchasedUiAsset.PANEL_LARGE else PurchasedUiAsset.PANEL_SMALL,
        contentPadding = PaddingValues(horizontal = 11.dp, vertical = 10.dp),
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PurchasedAvatarFrame(Modifier.size(62.dp)) {
                    FramedProfilePhotoAvatar(
                        avatarPath = avatarPath,
                        gender = gender,
                        name = name,
                        size = 50.dp,
                        accent = accent,
                        frameId = frameId,
                        visible = avatarVisible,
                        showGenderBadge = false,
                        isPro = isPro,
                    )
                }
                Spacer(Modifier.width(7.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        name,
                        color = Color(0xFF4A2D20),
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(sh("$area bölge", "$area cells"), color = Color(0xFF765746), fontSize = 9.sp, lineHeight = 12.sp, maxLines = 1)
                        if (isBot) Text("BOT", color = accent, fontSize = 8.sp, lineHeight = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
                if (leading) {
                    PurchasedAsset(PurchasedUiAsset.ICON_CROWN, Modifier.size(30.dp))
                    Spacer(Modifier.width(3.dp))
                }
                val totalDescription = sh("Toplam $score", "Total $score")
                Text(
                    "$score",
                    Modifier.semantics { contentDescription = totalDescription },
                    color = accent,
                    fontSize = 23.sp,
                    lineHeight = 27.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                WordSiegeScoreLine(sh("Kelime", "Word"), wordPoints, PurchasedUiAsset.ICON_GAMES, Modifier.weight(1f))
                WordSiegeScoreLine(sh("Bölge", "Territory"), territoryPoints, PurchasedUiAsset.ICON_TROPHY, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun WordSiegeScoreLine(
    label: String,
    value: Int,
    asset: PurchasedUiAsset,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        PurchasedAsset(asset, Modifier.size(22.dp))
        Spacer(Modifier.width(3.dp))
        Column {
            Text(label, color = Color(0xFF765746), fontSize = 8.sp, lineHeight = 10.sp, maxLines = 1)
            Text("$value", color = Color(0xFF4A2D20), fontSize = 11.sp, lineHeight = 13.sp, fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}

@Composable
internal fun WordSiegeCompactAction(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    PurchasedPanel(
        modifier = modifier
            .heightIn(min = 54.dp)
            .clickable(enabled = enabled, onClick = onClick),
        asset = PurchasedUiAsset.PANEL_SMALL,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
    ) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                icon,
                null,
                Modifier.size(18.dp),
                tint = if (enabled) Color(0xFF6B3CA6) else WordSiegeGameUi.DisabledContent,
            )
            Text(
                label,
                color = if (enabled) Color(0xFF4A2D20) else WordSiegeGameUi.DisabledContent,
                fontSize = 8.5.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun WordSiegeOwnershipLegend() {
    PurchasedPanel(
        modifier = Modifier.fillMaxWidth(),
        asset = PurchasedUiAsset.PANEL_SMALL,
        contentPadding = PaddingValues(horizontal = 11.dp, vertical = 7.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(sh("● Sen", "● You"), color = WordSiegeGameUi.Blue, fontSize = 9.sp, fontWeight = FontWeight.Black)
            Text(sh("● Rakip", "● Rival"), color = WordSiegeGameUi.Red, fontSize = 9.sp, fontWeight = FontWeight.Black)
            Row(verticalAlignment = Alignment.CenterVertically) {
                PurchasedAsset(PurchasedUiAsset.ICON_TROPHY, Modifier.size(20.dp))
                Spacer(Modifier.width(3.dp))
                Text(sh("1 küp = 2 puan", "1 cube = 2 points"), color = Color(0xFF765746), fontSize = 8.sp)
            }
        }
    }
}
