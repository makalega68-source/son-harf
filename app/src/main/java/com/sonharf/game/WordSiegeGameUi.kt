package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Kelime Kuşatması match presentation. The board reads as a tactical territory map first and a
 * word board second. Blue = player, purple = rival, turquoise = positive/progress, orange =
 * critical/reward. No gameplay state is changed here.
 */
internal object WordSiegeGameUi {
    val Background: Color get() = SonHarfTheme.Background
    val Surface: Color get() = SonHarfTheme.Surface
    val SurfaceSoft: Color get() = SonHarfTheme.GameSurface
    val Text: Color get() = SonHarfTheme.TextPrimary
    val Muted: Color get() = SonHarfTheme.TextSecondary
    val Border: Color get() = SonHarfTheme.GameTileBorder
    val Blue: Color get() = SonHarfTheme.Primary
    // Historical field name kept for call-site compatibility; rival identity is premium purple.
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
    Surface(
        modifier = modifier,
        color = lerp(WordSiegeGameUi.Surface, accent, if (active) .075f else .025f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (active) 1.5.dp else 1.dp, accent.copy(alpha = if (active) .52f else .18f)),
        shadowElevation = if (active) 4.dp else 1.dp,
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FramedProfilePhotoAvatar(
                    avatarPath = avatarPath,
                    gender = gender,
                    name = name,
                    size = 42.dp,
                    accent = accent,
                    frameId = frameId,
                    visible = avatarVisible,
                    showGenderBadge = false,
                    isPro = isPro,
                )
                Spacer(Modifier.width(7.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        name,
                        color = WordSiegeGameUi.Text,
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(sh("$area bölge", "$area cells"), color = WordSiegeGameUi.Muted, fontSize = 9.sp, lineHeight = 12.sp, maxLines = 1)
                        if (isBot) Text("BOT", color = accent, fontSize = 9.sp, lineHeight = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
                if (leading) {
                    WordSiegeLeaderCrown()
                    Spacer(Modifier.width(4.dp))
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
            HorizontalDivider(color = WordSiegeGameUi.Border.copy(alpha = .7f))
            WordSiegeScoreLine(sh("Kelime Puanı", "Word Score"), wordPoints)
            WordSiegeScoreLine(sh("Bölge Puanı", "Territory Score"), territoryPoints)
        }
    }
}

@Composable
private fun WordSiegeScoreLine(label: String, value: Int) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = WordSiegeGameUi.Muted, fontSize = 10.sp, lineHeight = 13.sp, maxLines = 1)
        Text("$value", color = WordSiegeGameUi.Text, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Black, maxLines = 1)
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
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(13.dp),
        color = if (enabled) WordSiegeGameUi.Surface else WordSiegeGameUi.DisabledBackground,
        border = BorderStroke(1.dp, WordSiegeGameUi.Border),
    ) {
        Column(
            Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, null, Modifier.size(17.dp), tint = if (enabled) WordSiegeGameUi.Blue else WordSiegeGameUi.DisabledContent)
            Text(label, color = if (enabled) WordSiegeGameUi.Text else WordSiegeGameUi.DisabledContent, fontSize = 9.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
internal fun WordSiegeOwnershipLegend() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = WordSiegeGameUi.Surface.copy(alpha = .92f),
        border = BorderStroke(1.dp, WordSiegeGameUi.Border),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(sh("● Sen", "● You"), color = WordSiegeGameUi.Blue, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Text(sh("● Rakip", "● Rival"), color = WordSiegeGameUi.Red, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Text(sh("1 küp = 2 puan", "1 cube = 2 points"), color = WordSiegeGameUi.Muted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun WordSiegeLeaderCrown() {
    val description = sh("Lider", "Leader")
    Canvas(Modifier.size(14.dp).semantics { contentDescription = description }) {
        val crown = Path().apply {
            moveTo(size.width * .08f, size.height * .28f)
            lineTo(size.width * .3f, size.height * .48f)
            lineTo(size.width * .5f, size.height * .1f)
            lineTo(size.width * .7f, size.height * .48f)
            lineTo(size.width * .92f, size.height * .28f)
            lineTo(size.width * .8f, size.height * .86f)
            lineTo(size.width * .2f, size.height * .86f)
            close()
        }
        drawPath(crown, WordSiegeGameUi.Gold)
    }
}
