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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Match-only presentation. Never changes the selected cosmetic theme or game state. */
internal object WordSiegeGameUi {
    val Background = Color(0xFFF8FAF4)
    val Surface = Color(0xFFFFFEF8)
    val SurfaceSoft = Color(0xFFEEF5EF)
    val Text = Color(0xFF213C31)
    val Muted = Color(0xFF52675C)
    val Border = Color(0xFFD1DDD5)
    val Blue = Color(0xFF557A87)
    val Red = Color(0xFF9B4D4A)
    val Gold = Color(0xFFAB8131)
    val DisabledBackground = Color(0xFFE4EAE5)
    val DisabledContent = Color(0xFF667B6F)
}

@Composable
internal fun WordSiegeGameTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF527867), onPrimary = Color.White,
            secondary = WordSiegeGameUi.Blue, onSecondary = Color.White,
            background = WordSiegeGameUi.Background, onBackground = WordSiegeGameUi.Text,
            surface = WordSiegeGameUi.Surface, onSurface = WordSiegeGameUi.Text,
            surfaceVariant = WordSiegeGameUi.SurfaceSoft, onSurfaceVariant = WordSiegeGameUi.Muted,
            outline = WordSiegeGameUi.Border, error = WordSiegeGameUi.Red,
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
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = lerp(WordSiegeGameUi.Surface, accent, .04f),
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = if (active) .5f else .18f)),
    ) {
        Column(Modifier.padding(horizontal = 7.dp, vertical = 5.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfilePhotoAvatarWithGender(
                    avatarPath = avatarPath, gender = gender, name = name,
                    size = 26.dp, accent = accent, visible = avatarVisible,
                )
                Spacer(Modifier.width(5.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, color = WordSiegeGameUi.Text, fontSize = 12.sp, lineHeight = 14.sp,
                        fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(sh("$area küp", "$area cubes"), color = WordSiegeGameUi.Muted,
                            fontSize = 10.sp, lineHeight = 12.sp, maxLines = 1)
                        if (isBot) Text("BOT", color = accent, fontSize = 9.sp, lineHeight = 12.sp)
                    }
                }
                if (leading) {
                    WordSiegeLeaderCrown()
                    Spacer(Modifier.width(3.dp))
                }
                val totalDescription = sh("Toplam $score", "Total $score")
                Text("$score", Modifier.semantics { contentDescription = totalDescription },
                    color = accent, fontSize = 22.sp, lineHeight = 26.sp,
                    fontWeight = FontWeight.Bold, maxLines = 1)
            }
            WordSiegeScoreLine(sh("Kelime Puanı", "Word Points"), wordPoints)
            WordSiegeScoreLine(sh("Bölge Puanı", "Territory Points"), territoryPoints)
        }
    }
}

@Composable
private fun WordSiegeScoreLine(label: String, value: Int) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = WordSiegeGameUi.Muted,
            fontSize = 11.sp, lineHeight = 14.sp, maxLines = 1)
        Text("$value", color = WordSiegeGameUi.Text, fontSize = 11.sp,
            lineHeight = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
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
    TextButton(
        onClick = onClick, enabled = enabled, modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(2.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = WordSiegeGameUi.Muted),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(icon, null, Modifier.size(17.dp))
            Text(label, fontSize = 11.sp, lineHeight = 14.sp,
                fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

@Composable
internal fun WordSiegeOwnershipLegend() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(sh("● Sen", "● You"), color = Color(0xFF3F7C53), fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold)
        Text(sh("● Rakip", "● Rival"), color = WordSiegeGameUi.Red, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold)
        Text(sh("1 küp = 2 puan", "1 cube = 2 points"), color = WordSiegeGameUi.Muted, fontSize = 11.sp, lineHeight = 14.sp)
    }
}

@Composable
private fun WordSiegeLeaderCrown() {
    val description = sh("Lider", "Leader")
    Canvas(Modifier.size(12.dp).semantics { contentDescription = description }) {
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
