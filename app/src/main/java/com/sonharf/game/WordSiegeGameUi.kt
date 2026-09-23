package com.sonharf.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

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
    scoreArrivalTick: Int = 0,
    scoreLossTick: Int = 0,
    onScoreCenterChanged: (Offset) -> Unit = {},
) {
    val scoreScale = remember { Animatable(1f) }
    val scoreGlow = remember { Animatable(0f) }
    val lossScale = remember { Animatable(1f) }
    val lossGlow = remember { Animatable(0f) }

    LaunchedEffect(scoreArrivalTick) {
        if (scoreArrivalTick <= 0) return@LaunchedEffect
        scoreScale.snapTo(1f)
        scoreGlow.snapTo(0f)
        coroutineScope {
            launch {
                scoreScale.animateTo(1.14f, tween(90))
                scoreScale.animateTo(1f, tween(170))
            }
            launch {
                scoreGlow.animateTo(1f, tween(70))
                scoreGlow.animateTo(0f, tween(260))
            }
        }
    }
    LaunchedEffect(scoreLossTick) {
        if (scoreLossTick <= 0) return@LaunchedEffect
        lossScale.snapTo(1f)
        lossGlow.snapTo(0f)
        coroutineScope {
            launch {
                lossScale.animateTo(.91f, tween(70))
                lossScale.animateTo(1f, tween(130))
            }
            launch {
                lossGlow.animateTo(1f, tween(55))
                lossGlow.animateTo(0f, tween(210))
            }
        }
    }
    Surface(
        modifier = modifier.height(92.dp),
        color = lerp(WordSiegeGameUi.Surface, accent, if (active) .055f else .018f),
        shape = RoundedCornerShape(18.dp),
        shadowElevation = if (active) 3.dp else 1.dp,
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.padding(top = if (leading) 4.dp else 0.dp)) {
                    ProfilePhotoAvatarWithGender(
                        avatarPath = avatarPath, gender = gender, name = name,
                        size = 52.dp, accent = accent, visible = avatarVisible,
                    )
                    if (leading) {
                        Box(Modifier.align(Alignment.TopCenter).offset(y = (-7).dp)) {
                            WordSiegeLeaderCrown()
                        }
                    }
                }
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        name,
                        color = WordSiegeGameUi.Text,
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (isBot) sh("$area küp • BOT", "$area cubes • BOT") else sh("$area küp", "$area cubes"),
                        color = WordSiegeGameUi.Muted,
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        fontWeight = if (isBot) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val totalDescription = sh("Toplam $score", "Total $score")
                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .height(46.dp)
                        .graphicsLayer {
                            val combinedScale = scoreScale.value * lossScale.value
                            scaleX = combinedScale
                            scaleY = combinedScale
                        }
                        .drawBehind {
                            if (scoreGlow.value > 0f) {
                                drawCircle(
                                    color = accent.copy(alpha = .55f * scoreGlow.value),
                                    radius = size.maxDimension * (.58f + .10f * scoreGlow.value),
                                    style = Stroke(width = 2.dp.toPx()),
                                )
                            }
                            if (lossGlow.value > 0f) {
                                drawCircle(
                                    color = Color(0xFFB94B4B).copy(alpha = .62f * lossGlow.value),
                                    radius = size.maxDimension * (.58f + .08f * lossGlow.value),
                                    style = Stroke(width = 2.dp.toPx()),
                                )
                            }
                        }
                        .onGloballyPositioned { coordinates ->
                            onScoreCenterChanged(
                                coordinates.localToWindow(
                                    Offset(coordinates.size.width / 2f, coordinates.size.height / 2f),
                                ),
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    val scoreFontSize = when {
                        score >= 10_000 -> 14.sp
                        score >= 1_000 -> 17.sp
                        else -> 21.sp
                    }
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(13.dp),
                        color = accent.copy(alpha = .10f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "$score",
                                Modifier.semantics { contentDescription = totalDescription },
                                color = if (lossGlow.value > 0f) lerp(accent, Color(0xFFB94B4B), lossGlow.value) else accent,
                                fontSize = scoreFontSize,
                                lineHeight = 24.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                            )
                        }
                    }
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
        modifier = modifier.height(40.dp).padding(horizontal = 1.dp),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(1.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = WordSiegeGameUi.Navy,
            disabledContentColor = WordSiegeGameUi.DisabledContent,
        ),
        border = BorderStroke(1.dp, if (enabled) WordSiegeGameUi.Border else WordSiegeGameUi.Border.copy(alpha = .45f)),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(icon, null, Modifier.size(15.dp))
            Text(label, fontSize = 9.sp, lineHeight = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
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
    Canvas(
        Modifier
            .size(width = 23.dp, height = 17.dp)
            .semantics { contentDescription = description },
    ) {
        val crown = Path().apply {
            moveTo(size.width * .08f, size.height * .32f)
            lineTo(size.width * .28f, size.height * .55f)
            lineTo(size.width * .39f, size.height * .19f)
            lineTo(size.width * .50f, size.height * .51f)
            lineTo(size.width * .62f, size.height * .12f)
            lineTo(size.width * .72f, size.height * .55f)
            lineTo(size.width * .92f, size.height * .30f)
            lineTo(size.width * .82f, size.height * .86f)
            lineTo(size.width * .18f, size.height * .86f)
            close()
        }
        val gold = Brush.verticalGradient(
            listOf(Color(0xFFFFF0A8), Color(0xFFF4C44F), Color(0xFFD69424)),
        )
        drawPath(crown, brush = gold)
        drawPath(crown, color = Color(0xFF7A5218), style = Stroke(width = 1.05.dp.toPx()))
        drawLine(
            color = Color(0xFFFFF4C4),
            start = Offset(size.width * .22f, size.height * .70f),
            end = Offset(size.width * .78f, size.height * .70f),
            strokeWidth = 1.2.dp.toPx(),
        )
        drawCircle(Color(0xFFE85D5D), 1.35.dp.toPx(), Offset(size.width * .39f, size.height * .61f))
        drawCircle(Color(0xFF4E8FD4), 1.35.dp.toPx(), Offset(size.width * .62f, size.height * .59f))
    }
}
