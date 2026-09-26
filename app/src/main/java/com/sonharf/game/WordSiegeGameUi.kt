package com.sonharf.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.drawscope.rotate
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
    val Background = Color(0xFFE6ECF2)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceSoft = Color(0xFFF3F6F9)
    val Text = Color(0xFF243142)
    val Muted = Color(0xFF6B7A8C)
    val Border = Color(0xFFD2DBE5)
    val Blue = Color(0xFF3E9F4D)
    val Red = Color(0xFFD0514A)
    val Gold = Color(0xFFE0A82E)
    val Navy = Color(0xFF2C3E55)
    val NavySoft = Color(0xFF3A4F6B)
    val PremiumSurface = Color(0xFFFFF3D6)
    val PremiumBorder = Color(0xFFE0A82E)
    val DisabledBackground = Color(0xFFE3E9EF)
    val DisabledContent = Color(0xFF9AA7B5)
}

@Composable
internal fun WordSiegeGameTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = WordSiegeGameUi.Blue, onPrimary = Color(0xFFFFFFFF),
            secondary = WordSiegeGameUi.Gold, onSecondary = Color(0xFF243142),
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
        color = lerp(WordSiegeGameUi.Background, accent, if (active) .30f else .20f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(if (active) 2.dp else 1.dp, accent.copy(alpha = if (active) 1f else .55f)),
        shadowElevation = if (active) 3.dp else 1.dp,
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center) {
                    if (leading) WordSiegeLeaderHalo(Modifier.size(60.dp))
                    ProfilePhotoAvatarWithGender(
                        avatarPath = avatarPath, gender = gender, name = name,
                        size = 52.dp, accent = accent, visible = avatarVisible,
                    )
                    if (leading) WordSiegeLeaderBadge(Modifier.align(Alignment.BottomEnd).offset(x = 3.dp, y = 3.dp))
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
                        color = accent.copy(alpha = .35f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "$score",
                                Modifier.semantics { contentDescription = totalDescription },
                                color = if (lossGlow.value > 0f) lerp(WordSiegeGameUi.Text, Color(0xFFB94B4B), lossGlow.value) else WordSiegeGameUi.Text,
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
            contentColor = WordSiegeGameUi.Gold,
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
            Text(sh("● Sen", "● You"), Modifier.weight(1f), color = Color(0xFF3E9F4D), fontSize = 10.sp, fontWeight = FontWeight.Black)
            Text(
                sh("1 küp = 2 puan", "1 cube = 2 points"),
                Modifier.weight(1f),
                color = WordSiegeGameUi.Muted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(sh("Rakip ●", "Rival ●"), Modifier.weight(1f), color = Color(0xFFD0514A), fontSize = 10.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.End)
        }
    }
}

/** The leader's photo gets a slowly shimmering gold halo instead of a drawn crown. */
@Composable
private fun WordSiegeLeaderHalo(modifier: Modifier) {
    val description = sh("Lider", "Leader")
    val shimmer by rememberInfiniteTransition(label = "leader-halo")
        .animateFloat(0f, 360f, infiniteRepeatable(tween(3_600, easing = LinearEasing)), label = "leader-shimmer")
    Canvas(modifier.semantics { contentDescription = description }) {
        val stroke = 3.dp.toPx()
        val ring = Brush.sweepGradient(
            listOf(Color(0xFFB07F1E), Color(0xFFF2C14E), Color(0xFFFFF4C4), Color(0xFFF2C14E), Color(0xFFB07F1E)),
        )
        rotate(shimmer) {
            drawCircle(ring, radius = size.minDimension / 2f - stroke / 2f, style = Stroke(width = stroke))
        }
        drawCircle(Color(0x33F2C14E), radius = size.minDimension / 2f, style = Stroke(width = 1.dp.toPx()))
    }
}

/** A small gold medal with a star in the photo's corner. */
@Composable
private fun WordSiegeLeaderBadge(modifier: Modifier) {
    Box(
        modifier
            .size(20.dp)
            .shadow(3.dp, CircleShape)
            .background(Brush.verticalGradient(listOf(Color(0xFFFFE9A3), Color(0xFFE0A82E), Color(0xFFB07F1E))), CircleShape)
            .border(1.5.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text("★", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}
