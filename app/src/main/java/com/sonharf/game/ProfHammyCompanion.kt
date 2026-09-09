package com.sonharf.game

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

enum class ProfHammyMood {
    IDLE,
    HAPPY,
    EXCITED,
    FOCUSED,
    PANIC,
    SAD,
    WINK,
}

private data class ProfHammyHomeReaction(
    val mood: ProfHammyMood,
    val textTr: String,
    val textEn: String,
)

private val profHammyHomeReactions = listOf(
    ProfHammyHomeReaction(
        mood = ProfHammyMood.WINK,
        textTr = "Bugün kelimeler senden yana! 😉",
        textEn = "Words are on your side today! 😉",
    ),
    ProfHammyHomeReaction(
        mood = ProfHammyMood.EXCITED,
        textTr = "Bir seri yakalamaya ne dersin? 🔥",
        textEn = "How about starting a streak? 🔥",
    ),
    ProfHammyHomeReaction(
        mood = ProfHammyMood.FOCUSED,
        textTr = "İlk harfi gör, kelimeyi zihninde tamamla.",
        textEn = "See the first letter and finish the word in your mind.",
    ),
    ProfHammyHomeReaction(
        mood = ProfHammyMood.HAPPY,
        textTr = "Hazırsan arenada görüşürüz! 🌟",
        textEn = "Ready? I’ll see you in the arena! 🌟",
    ),
)

private const val PROF_HAMMY_DEFAULT_TR = "Hazırsan başlayalım. Bugün güzel bir kelime bul!"
private const val PROF_HAMMY_DEFAULT_EN = "Ready? Let’s find a great word today!"

/**
 * Lobby-safe Prof. Hammy presentation.
 *
 * Reactions are local UI state only. This component has no access to Premier match state,
 * score, rating, target letters, word suggestions, inventory or economy state.
 */
@Composable
fun ProfHammyHomeCard(
    modifier: Modifier = Modifier,
) {
    var reactionId by remember { mutableIntStateOf(0) }
    var mood by remember { mutableStateOf(ProfHammyMood.HAPPY) }
    var speechTr by remember { mutableStateOf(PROF_HAMMY_DEFAULT_TR) }
    var speechEn by remember { mutableStateOf(PROF_HAMMY_DEFAULT_EN) }

    LaunchedEffect(reactionId) {
        delay(if (reactionId == 0) 2200L else 1800L)
        mood = ProfHammyMood.IDLE
        if (reactionId > 0) {
            speechTr = PROF_HAMMY_DEFAULT_TR
            speechEn = PROF_HAMMY_DEFAULT_EN
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                val nextId = reactionId + 1
                val reaction = profHammyHomeReactions[(nextId - 1) % profHammyHomeReactions.size]
                reactionId = nextId
                mood = reaction.mood
                speechTr = reaction.textTr
                speechEn = reaction.textEn
            },
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)),
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfHammyCompanion(
                mood = mood,
                size = 92.dp,
            )
            Spacer(Modifier.width(13.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = "PROF. HAMMY",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = sh("Kelime öğretmenin • Dokun ve selamlaş", "Your word coach • Tap to say hi"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = sh(speechTr, speechEn),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Stateless render-only mascot surface. */
@Composable
fun ProfHammyCompanion(
    modifier: Modifier = Modifier,
    mood: ProfHammyMood = ProfHammyMood.IDLE,
    size: Dp = 92.dp,
) {
    val transition = rememberInfiniteTransition(label = "ProfHammyIdle")
    val floatY by transition.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (mood == ProfHammyMood.EXCITED) 1200 else 1800,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ProfHammyFloat",
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { translationY = floatY },
        contentAlignment = Alignment.Center,
    ) {
        ProfHammyCanvas(mood = mood, size = size)
    }
}

@Composable
private fun ProfHammyCanvas(
    mood: ProfHammyMood,
    size: Dp,
) {
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface

    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w * 0.50f

        val fur = Color(0xFFE59A3A)
        val furDark = Color(0xFFB86A22)
        val cream = Color(0xFFFFE7C2)
        val ear = Color(0xFFF4A7A0)
        val eye = Color(0xFF3A2418)
        val glasses = Color(0xFF4A2E20)
        val nose = Color(0xFFE78682)
        val bow = Color(0xFF547A59)

        // Ears.
        drawCircle(
            color = fur,
            radius = w * 0.18f,
            center = Offset(w * 0.24f, h * 0.25f),
        )
        drawCircle(
            color = fur,
            radius = w * 0.18f,
            center = Offset(w * 0.76f, h * 0.25f),
        )
        drawCircle(
            color = ear,
            radius = w * 0.105f,
            center = Offset(w * 0.24f, h * 0.25f),
        )
        drawCircle(
            color = ear,
            radius = w * 0.105f,
            center = Offset(w * 0.76f, h * 0.25f),
        )

        // Head and professor cheeks.
        drawCircle(
            color = fur,
            radius = w * 0.39f,
            center = Offset(cx, h * 0.51f),
        )
        drawCircle(
            color = furDark.copy(alpha = 0.18f),
            radius = w * 0.34f,
            center = Offset(cx, h * 0.55f),
            style = Stroke(width = w * 0.025f),
        )
        drawCircle(
            color = cream,
            radius = w * 0.19f,
            center = Offset(w * 0.36f, h * 0.63f),
        )
        drawCircle(
            color = cream,
            radius = w * 0.19f,
            center = Offset(w * 0.64f, h * 0.63f),
        )

        val leftEye = Offset(w * 0.36f, h * 0.47f)
        val rightEye = Offset(w * 0.64f, h * 0.47f)
        val eyeRadius = w * 0.072f
        val lensRadius = w * 0.125f

        when (mood) {
            ProfHammyMood.WINK -> {
                drawLine(
                    color = eye,
                    start = Offset(leftEye.x - w * 0.055f, leftEye.y),
                    end = Offset(leftEye.x + w * 0.055f, leftEye.y),
                    strokeWidth = w * 0.027f,
                )
                drawEye(rightEye, eyeRadius, eye, mood)
            }

            ProfHammyMood.PANIC -> {
                drawCircle(color = Color.White, radius = eyeRadius * 1.35f, center = leftEye)
                drawCircle(color = eye, radius = eyeRadius * 0.72f, center = leftEye)
                drawCircle(color = Color.White, radius = eyeRadius * 1.35f, center = rightEye)
                drawCircle(color = eye, radius = eyeRadius * 0.72f, center = rightEye)
            }

            ProfHammyMood.SAD -> {
                drawEye(leftEye.copy(y = leftEye.y + h * 0.015f), eyeRadius * 0.88f, eye, mood)
                drawEye(rightEye.copy(y = rightEye.y + h * 0.015f), eyeRadius * 0.88f, eye, mood)
            }

            ProfHammyMood.FOCUSED -> {
                drawEye(leftEye, eyeRadius * 0.88f, eye, mood)
                drawEye(rightEye, eyeRadius * 0.88f, eye, mood)
                drawLine(
                    color = glasses,
                    start = Offset(leftEye.x - w * 0.06f, leftEye.y - h * 0.10f),
                    end = Offset(leftEye.x + w * 0.04f, leftEye.y - h * 0.07f),
                    strokeWidth = w * 0.018f,
                )
                drawLine(
                    color = glasses,
                    start = Offset(rightEye.x - w * 0.04f, rightEye.y - h * 0.07f),
                    end = Offset(rightEye.x + w * 0.06f, rightEye.y - h * 0.10f),
                    strokeWidth = w * 0.018f,
                )
            }

            else -> {
                drawEye(leftEye, eyeRadius, eye, mood)
                drawEye(rightEye, eyeRadius, eye, mood)
            }
        }

        // Glasses: always visible, matching the concept art identity.
        drawCircle(
            color = glasses,
            radius = lensRadius,
            center = leftEye,
            style = Stroke(width = w * 0.025f),
        )
        drawCircle(
            color = glasses,
            radius = lensRadius,
            center = rightEye,
            style = Stroke(width = w * 0.025f),
        )
        drawLine(
            color = glasses,
            start = Offset(leftEye.x + lensRadius, leftEye.y),
            end = Offset(rightEye.x - lensRadius, rightEye.y),
            strokeWidth = w * 0.024f,
        )

        // Nose, teeth and expression.
        drawCircle(
            color = nose,
            radius = w * 0.035f,
            center = Offset(cx, h * 0.61f),
        )

        when (mood) {
            ProfHammyMood.SAD -> {
                drawArc(
                    color = eye,
                    startAngle = 205f,
                    sweepAngle = 130f,
                    useCenter = false,
                    topLeft = Offset(w * 0.43f, h * 0.64f),
                    size = androidx.compose.ui.geometry.Size(w * 0.14f, h * 0.10f),
                    style = Stroke(width = w * 0.022f),
                )
            }

            ProfHammyMood.PANIC -> {
                drawCircle(
                    color = eye,
                    radius = w * 0.055f,
                    center = Offset(cx, h * 0.70f),
                )
            }

            else -> {
                drawArc(
                    color = eye,
                    startAngle = 15f,
                    sweepAngle = 150f,
                    useCenter = false,
                    topLeft = Offset(w * 0.41f, h * 0.61f),
                    size = androidx.compose.ui.geometry.Size(w * 0.18f, h * 0.15f),
                    style = Stroke(width = w * 0.025f),
                )
                drawRect(
                    color = Color.White,
                    topLeft = Offset(w * 0.47f, h * 0.64f),
                    size = androidx.compose.ui.geometry.Size(w * 0.055f, h * 0.075f),
                )
            }
        }

        // Signature green professor bow tie.
        drawOval(
            color = bow,
            topLeft = Offset(w * 0.31f, h * 0.78f),
            size = androidx.compose.ui.geometry.Size(w * 0.24f, h * 0.13f),
        )
        drawOval(
            color = bow,
            topLeft = Offset(w * 0.45f, h * 0.78f),
            size = androidx.compose.ui.geometry.Size(w * 0.24f, h * 0.13f),
        )
        drawCircle(
            color = bow.copy(alpha = 0.92f),
            radius = w * 0.055f,
            center = Offset(cx, h * 0.845f),
        )

        if (mood == ProfHammyMood.EXCITED || mood == ProfHammyMood.HAPPY) {
            drawCircle(
                color = primary.copy(alpha = 0.35f),
                radius = w * 0.022f,
                center = Offset(w * 0.09f, h * 0.48f),
            )
            drawCircle(
                color = primary.copy(alpha = 0.35f),
                radius = w * 0.016f,
                center = Offset(w * 0.90f, h * 0.36f),
            )
        }

        // Subtle lower badge plate to visually ground the character on any theme.
        drawRoundRect(
            color = surface.copy(alpha = 0.28f),
            topLeft = Offset(w * 0.28f, h * 0.91f),
            size = androidx.compose.ui.geometry.Size(w * 0.44f, h * 0.035f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.02f),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEye(
    center: Offset,
    radius: Float,
    eyeColor: Color,
    mood: ProfHammyMood,
) {
    drawCircle(color = Color.White, radius = radius * 1.18f, center = center)
    drawCircle(color = eyeColor, radius = radius, center = center)
    val highlightScale = if (mood == ProfHammyMood.EXCITED) 0.36f else 0.28f
    drawCircle(
        color = Color.White,
        radius = radius * highlightScale,
        center = Offset(center.x - radius * 0.30f, center.y - radius * 0.34f),
    )
}
