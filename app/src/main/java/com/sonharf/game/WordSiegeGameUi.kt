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
 * Kelime Kuşatması match-only presentation.
 * Calm, board-first visual treatment; never mutates selected cosmetics or gameplay state.
 */
internal object WordSiegeGameUi {
    val Background = Color(0xFFF6F4EE)
    val Surface = Color(0xFFFFFEFA)
    val SurfaceSoft = Color(0xFFF0F2EC)
    val Text = Color(0xFF18322A)
    val Muted = Color(0xFF66766F)
    val Border = Color(0xFFD4DAD3)
    val Blue = Color(0xFF6F8794)
    val Red = Color(0xFFA4554F)
    val Gold = Color(0xFFB58A39)
    val DisabledBackground = Color(0xFFE7E9E4)
    val DisabledContent = Color(0xFF8B948F)
}

@Composable
internal fun WordSiegeGameTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF285943), onPrimary = Color.White,
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
        color = lerp(WordSiegeGameUi.Surface, accent, .035f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (active) 1.5.dp else 1.dp, accent.copy(alpha = if (active) .55f else .18f)),
        shadowElevation = if (active) 2.dp else 0.dp,
    ) {
        Column(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfilePhotoAvatarWithGender(
                    avatarPath = avatarPath,
                    gender = gender,
                    name = name,
                    size = 38.dp,
                    accent = accent,
                    visible = avatarVisible,
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        name,
                        color = WordSiegeGameUi.Text,
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            sh("$area bölge", "$area areas"),
                            color = WordSiegeGameUi.Muted,
                            fontSize = 9.sp,
                            lineHeight = 12.sp,
                            maxLines = 1,
                        )
                        if (isBot) {
                            Surface(shape = RoundedCornerShape(99.dp), color = accent.copy(alpha = .10f)) {
                                Text(
                                    "BOT",
                                    Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    color = accent,
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Black,
                                )
                            }
                        }
                    }
                }
                if (leading) {
                    WordSiegeLeaderCrown()
                    Spacer(Modifier.width(5.dp))
                }
                val totalDescription = sh("Toplam $score", "Total $score")
                Text(
                    "$score",
                    Modifier.semantics { contentDescription = totalDescription },
                    color = accent,
                    fontSize = 24.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                )
            }
            HorizontalDivider(color = WordSiegeGameUi.Border.copy(alpha = .65f))
            WordSiegeScoreEquation(wordPoints, territoryPoints, score, accent)
        }
    }
}

@Composable
private fun WordSiegeScoreEquation(wordPoints: Int, territoryPoints: Int, total: Int, accent: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        WordSiegeScoreMetric(sh("Kelime", "Word"), wordPoints, Modifier.weight(1f))
        Text("+", color = WordSiegeGameUi.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        WordSiegeScoreMetric(sh("Bölge", "Territory"), territoryPoints, Modifier.weight(1f))
        Text("=", color = WordSiegeGameUi.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text(sh("Toplam", "Total"), color = WordSiegeGameUi.Muted, fontSize = 7.sp, fontWeight = FontWeight.Bold)
            Text(total.toString(), color = accent, fontSize = 13.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun WordSiegeScoreMetric(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, color = WordSiegeGameUi.Muted, fontSize = 7.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(value.toString(), color = WordSiegeGameUi.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(13.dp),
        color = if (enabled) WordSiegeGameUi.Surface else WordSiegeGameUi.DisabledBackground,
        border = BorderStroke(1.dp, WordSiegeGameUi.Border),
    ) {
        TextButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(13.dp),
            contentPadding = PaddingValues(2.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = WordSiegeGameUi.Text,
                disabledContentColor = WordSiegeGameUi.DisabledContent,
            ),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Icon(icon, null, Modifier.size(18.dp))
                Text(label, fontSize = 9.sp, lineHeight = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

@Composable
internal fun WordSiegeOwnershipLegend() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = WordSiegeGameUi.Surface,
        border = BorderStroke(1.dp, WordSiegeGameUi.Border.copy(alpha = .75f)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(sh("● Sen", "● You"), color = Color(0xFF3F7C53), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(sh("● Rakip", "● Rival"), color = WordSiegeGameUi.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(sh("1 bölge = 2 puan", "1 area = 2 points"), color = WordSiegeGameUi.Muted, fontSize = 9.sp)
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
