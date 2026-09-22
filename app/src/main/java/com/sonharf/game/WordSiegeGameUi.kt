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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Match-only presentation. Never changes the selected cosmetic theme or game state. */
internal object WordSiegeGameUi {
    val Background = Color(0xFFEAF6F8)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceSoft = Color(0xFFE0F3F5)
    val Text = Color(0xFF0B1B33)
    val Muted = Color(0xFF3B4B66)
    val Border = Color(0xFFC3D6E4)
    val Blue = Color(0xFF14B8B0)
    val Red = Color(0xFFE8622C)
    val Gold = Color(0xFF8B6CF0)
    val Navy = Color(0xFF102C4C)
    val NavySoft = Color(0xFF173B62)
    val PremiumSurface = Color(0xFFF6F3FF)
    val PremiumBorder = Color(0xFFB7A3FF)
    val DisabledBackground = Color(0xFFDDE5EE)
    val DisabledContent = Color(0xFF5E6D84)
}

@Composable
internal fun WordSiegeGameTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = WordSiegeGameUi.Blue, onPrimary = Color(0xFF0B1B33),
            secondary = WordSiegeGameUi.Gold, onSecondary = Color(0xFF0B1B33),
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
    val edge = if (active) accent else WordSiegeGameUi.Border
    Surface(
        modifier = modifier,
        color = lerp(WordSiegeGameUi.Surface, accent, if (active) .065f else .025f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (active) 1.5.dp else 1.dp, edge.copy(alpha = if (active) .72f else .72f)),
        shadowElevation = if (active) 4.dp else 1.dp,
    ) {
        Column(Modifier.padding(horizontal = 9.dp, vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = accent.copy(alpha = .09f),
                    border = BorderStroke(1.dp, accent.copy(alpha = .22f)),
                ) {
                    Box(Modifier.padding(2.dp)) {
                        ProfilePhotoAvatarWithGender(
                            avatarPath = avatarPath, gender = gender, name = name,
                            size = 30.dp, accent = accent, visible = avatarVisible,
                        )
                    }
                }
                Spacer(Modifier.width(7.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        name,
                        color = WordSiegeGameUi.Text,
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(sh("$area küp", "$area cubes"), color = WordSiegeGameUi.Muted, fontSize = 9.sp, lineHeight = 11.sp)
                        if (isBot) {
                            Surface(shape = RoundedCornerShape(99.dp), color = accent.copy(alpha = .12f)) {
                                Text("BOT", Modifier.padding(horizontal = 5.dp, vertical = 1.dp), color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
                if (leading) {
                    WordSiegeLeaderCrown()
                    Spacer(Modifier.width(4.dp))
                }
                val totalDescription = sh("Toplam $score", "Total $score")
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accent.copy(alpha = .10f),
                    border = BorderStroke(1.dp, accent.copy(alpha = .18f)),
                ) {
                    Text(
                        "$score",
                        Modifier.padding(horizontal = 8.dp, vertical = 4.dp).semantics { contentDescription = totalDescription },
                        color = accent,
                        fontSize = 21.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                WordSiegeScoreMetric(sh("Kelime Puanı", "Word Points"), wordPoints, accent, Modifier.weight(1f))
                WordSiegeScoreMetric(sh("Bölge Puanı", "Territory Points"), territoryPoints, accent, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun WordSiegeScoreMetric(label: String, value: Int, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(9.dp),
        color = WordSiegeGameUi.SurfaceSoft.copy(alpha = .78f),
        border = BorderStroke(1.dp, accent.copy(alpha = .10f)),
    ) {
        Row(Modifier.padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), color = WordSiegeGameUi.Muted, fontSize = 8.sp, lineHeight = 10.sp, maxLines = 1)
            Text("$value", color = WordSiegeGameUi.Text, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
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
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp).padding(horizontal = 2.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(2.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = WordSiegeGameUi.Navy,
            disabledContentColor = WordSiegeGameUi.DisabledContent,
        ),
        border = BorderStroke(1.dp, if (enabled) WordSiegeGameUi.Border else WordSiegeGameUi.Border.copy(alpha = .45f)),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(icon, null, Modifier.size(17.dp))
            Text(label, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
internal fun WordSiegeOwnershipLegend() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = WordSiegeGameUi.Surface.copy(alpha = .94f),
        border = BorderStroke(1.dp, WordSiegeGameUi.Border.copy(alpha = .75f)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(sh("● Sen", "● You"), Modifier.weight(1f), color = Color(0xFF3F7C53), fontSize = 10.sp, fontWeight = FontWeight.Black)
            Text(
                sh("1 küp = 2 puan", "1 cube = 2 points"),
                Modifier.weight(1f),
                color = WordSiegeGameUi.Muted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(sh("Rakip ●", "Rival ●"), Modifier.weight(1f), color = Color(0xFF9B4D4A), fontSize = 10.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.End)
        }
    }
}

@Composable
private fun WordSiegeLeaderCrown() {
    val description = sh("Lider", "Leader")
    Canvas(Modifier.size(13.dp).semantics { contentDescription = description }) {
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
