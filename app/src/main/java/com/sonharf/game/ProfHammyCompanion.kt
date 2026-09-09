package com.sonharf.game

import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.semantics.Role
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

private const val PROF_HAMMY_COACH_LEAD_THRESHOLD = 3

private fun profHammyCoachMessage(wins: Int, losses: Int): ProfHammyHomeReaction {
    val safeWins = wins.coerceAtLeast(0)
    val safeLosses = losses.coerceAtLeast(0)
    val totalMatches = safeWins + safeLosses

    return when {
        totalMatches <= 3 -> ProfHammyHomeReaction(
            mood = ProfHammyMood.FOCUSED,
            textTr = "İlk maçlarda hızdan önce doğru kelime. Sakin başla!",
            textEn = "In your first matches, accuracy comes before speed. Start calm!",
        )

        safeWins >= safeLosses + PROF_HAMMY_COACH_LEAD_THRESHOLD -> ProfHammyHomeReaction(
            mood = ProfHammyMood.HAPPY,
            textTr = "Güzel bir ritim yakaladın. Aynı sakinlikle devam et! 🌟",
            textEn = "You’ve found a good rhythm. Keep that same calm focus! 🌟",
        )

        safeLosses >= safeWins + PROF_HAMMY_COACH_LEAD_THRESHOLD -> ProfHammyHomeReaction(
            mood = ProfHammyMood.FOCUSED,
            textTr = "Bugün yeni bir sayfa. Sakin başla, ritmini kur.",
            textEn = "Today is a fresh start. Begin calm and find your rhythm.",
        )

        else -> ProfHammyHomeReaction(
            mood = ProfHammyMood.WINK,
            textTr = "Hazırsan başlayalım. Bugün güzel bir kelime bul!",
            textEn = "Ready? Let’s find a great word today!",
        )
    }
}

/**
 * Lobby-safe Prof. Hammy presentation.
 *
 * Coaching uses only aggregate lobby statistics supplied by the caller. Reactions are local UI
 * state only. This component has no access to Premier match state, score, rating, target letters,
 * word suggestions, inventory or economy state.
 */
@Composable
fun ProfHammyHomeCard(
    wins: Int = 0,
    losses: Int = 0,
    modifier: Modifier = Modifier,
) {
    val coachMessage = remember(wins, losses) { profHammyCoachMessage(wins, losses) }
    var reactionId by remember { mutableIntStateOf(0) }
    var mood by remember { mutableStateOf(coachMessage.mood) }
    var activeReaction by remember { mutableStateOf<ProfHammyHomeReaction?>(null) }

    LaunchedEffect(reactionId) {
        delay(if (reactionId == 0) 2200L else 1800L)
        mood = ProfHammyMood.IDLE
        activeReaction = null
    }

    val visibleMessage = activeReaction ?: coachMessage

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClickLabel = sh("Prof. Hammy ile etkileş", "Interact with Prof. Hammy"),
            ) {
                val nextId = reactionId + 1
                val reaction = profHammyHomeReactions[(nextId - 1) % profHammyHomeReactions.size]
                reactionId = nextId
                mood = reaction.mood
                activeReaction = reaction
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
                speaking = mood != ProfHammyMood.IDLE,
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
                    text = sh(visibleMessage.textTr, visibleMessage.textEn),
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

/**
 * Real-time procedural 2D mascot surface.
 *
 * It is not a slideshow and does not swap static photos. Eyes, pupils, ears, paws, body motion and
 * mouth are drawn and animated continuously in Compose. The API stays render-only and stateless.
 */
@Composable
fun ProfHammyCompanion(
    modifier: Modifier = Modifier,
    mood: ProfHammyMood = ProfHammyMood.IDLE,
    size: Dp = 92.dp,
    speaking: Boolean = false,
) {
    val transition = rememberInfiniteTransition(label = "ProfHammyLiveMotion")

    val floatY by transition.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (mood == ProfHammyMood.EXCITED) 1100 else 1900,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ProfHammyFloat",
    )
    val breathing by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ProfHammyBreathing",
    )
    val headSway by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ProfHammyHeadSway",
    )
    val gazeX by transition.animateFloat(
        initialValue = -0.9f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ProfHammyGazeX",
    )
    val gazeY by transition.animateFloat(
        initialValue = -0.35f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(2700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ProfHammyGazeY",
    )
    val pawWave by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (mood == ProfHammyMood.EXCITED) 300 else 620,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ProfHammyPawWave",
    )
    val mouthPulse by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(190, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ProfHammyMouthPulse",
    )

    val blinkOpen = remember { Animatable(1f) }
    val earTwitch = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        var longGap = false
        while (true) {
            delay(if (longGap) 3200L else 2100L)
            blinkOpen.animateTo(0.08f, animationSpec = tween(70))
            blinkOpen.animateTo(1f, animationSpec = tween(90))
            if (!longGap) {
                delay(120L)
                blinkOpen.animateTo(0.12f, animationSpec = tween(55))
                blinkOpen.animateTo(1f, animationSpec = tween(80))
            }
            longGap = !longGap
        }
    }

    LaunchedEffect(Unit) {
        var alternate = false
        while (true) {
            delay(if (alternate) 4300L else 3300L)
            earTwitch.animateTo(1f, animationSpec = tween(80))
            earTwitch.animateTo(-0.55f, animationSpec = tween(90))
            earTwitch.animateTo(0f, animationSpec = tween(120))
            alternate = !alternate
        }
    }

    val moodRotationMultiplier = when (mood) {
        ProfHammyMood.EXCITED -> 1.7f
        ProfHammyMood.PANIC -> 2.2f
        ProfHammyMood.WINK, ProfHammyMood.HAPPY -> 0.8f
        else -> 0.45f
    }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                translationY = floatY
                rotationZ = headSway * moodRotationMultiplier
                scaleX = 0.992f + breathing * 0.008f
                scaleY = 0.986f + breathing * 0.014f
            },
        contentAlignment = Alignment.Center,
    ) {
        ProfHammyCanvas(
            mood = mood,
            size = size,
            eyeOpen = blinkOpen.value,
            gazeX = gazeX,
            gazeY = gazeY,
            earTwitch = earTwitch.value,
            pawWave = pawWave,
            mouthPulse = mouthPulse,
            speaking = speaking,
        )
    }
}

@Composable
private fun ProfHammyCanvas(
    mood: ProfHammyMood,
    size: Dp,
    eyeOpen: Float,
    gazeX: Float,
    gazeY: Float,
    earTwitch: Float,
    pawWave: Float,
    mouthPulse: Float,
    speaking: Boolean,
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

        val leftEarCenter = Offset(
            w * (0.24f - earTwitch * 0.006f),
            h * (0.25f + earTwitch * 0.008f),
        )
        val rightEarCenter = Offset(
            w * (0.76f + earTwitch * 0.004f),
            h * (0.25f - earTwitch * 0.006f),
        )

        // Ears.
        drawCircle(color = fur, radius = w * 0.18f, center = leftEarCenter)
        drawCircle(color = fur, radius = w * 0.18f, center = rightEarCenter)
        drawCircle(color = ear, radius = w * 0.105f, center = leftEarCenter)
        drawCircle(color = ear, radius = w * 0.105f, center = rightEarCenter)

        // Head and cheeks.
        drawCircle(color = fur, radius = w * 0.39f, center = Offset(cx, h * 0.51f))
        drawCircle(
            color = furDark.copy(alpha = 0.18f),
            radius = w * 0.34f,
            center = Offset(cx, h * 0.55f),
            style = Stroke(width = w * 0.025f),
        )
        drawCircle(color = cream, radius = w * 0.19f, center = Offset(w * 0.36f, h * 0.63f))
        drawCircle(color = cream, radius = w * 0.19f, center = Offset(w * 0.64f, h * 0.63f))

        // Paws make the vector mascot feel like a small rig instead of a floating face.
        val pawAmplitude = when (mood) {
            ProfHammyMood.EXCITED -> 0.050f
            ProfHammyMood.HAPPY, ProfHammyMood.WINK -> 0.022f
            else -> 0.006f
        }
        val leftPawY = h * (0.77f - pawAmplitude * pawWave)
        val rightPawY = h * (0.77f + pawAmplitude * pawWave)
        drawOval(
            color = fur,
            topLeft = Offset(w * 0.16f, leftPawY),
            size = androidx.compose.ui.geometry.Size(w * 0.18f, h * 0.13f),
        )
        drawOval(
            color = fur,
            topLeft = Offset(w * 0.66f, rightPawY),
            size = androidx.compose.ui.geometry.Size(w * 0.18f, h * 0.13f),
        )
        drawCircle(color = cream, radius = w * 0.035f, center = Offset(w * 0.25f, leftPawY + h * 0.065f))
        drawCircle(color = cream, radius = w * 0.035f, center = Offset(w * 0.75f, rightPawY + h * 0.065f))

        val leftEye = Offset(w * 0.36f, h * 0.47f)
        val rightEye = Offset(w * 0.64f, h * 0.47f)
        val eyeRadius = w * 0.072f
        val lensRadius = w * 0.125f
        val effectiveEyeOpen = when (mood) {
            ProfHammyMood.PANIC -> eyeOpen.coerceAtLeast(0.58f)
            else -> eyeOpen
        }

        when (mood) {
            ProfHammyMood.WINK -> {
                drawLine(
                    color = eye,
                    start = Offset(leftEye.x - w * 0.055f, leftEye.y),
                    end = Offset(leftEye.x + w * 0.055f, leftEye.y),
                    strokeWidth = w * 0.027f,
                )
                drawLiveEye(rightEye, eyeRadius, eye, mood, gazeX, gazeY, effectiveEyeOpen)
            }

            ProfHammyMood.PANIC -> {
                drawLiveEye(leftEye, eyeRadius * 1.18f, eye, mood, gazeX, gazeY, effectiveEyeOpen)
                drawLiveEye(rightEye, eyeRadius * 1.18f, eye, mood, gazeX, gazeY, effectiveEyeOpen)
            }

            ProfHammyMood.SAD -> {
                drawLiveEye(leftEye.copy(y = leftEye.y + h * 0.015f), eyeRadius * 0.88f, eye, mood, gazeX * 0.45f, gazeY + 0.25f, effectiveEyeOpen)
                drawLiveEye(rightEye.copy(y = rightEye.y + h * 0.015f), eyeRadius * 0.88f, eye, mood, gazeX * 0.45f, gazeY + 0.25f, effectiveEyeOpen)
            }

            ProfHammyMood.FOCUSED -> {
                drawLiveEye(leftEye, eyeRadius * 0.88f, eye, mood, gazeX * 0.35f, gazeY * 0.35f, effectiveEyeOpen)
                drawLiveEye(rightEye, eyeRadius * 0.88f, eye, mood, gazeX * 0.35f, gazeY * 0.35f, effectiveEyeOpen)
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
                drawLiveEye(leftEye, eyeRadius, eye, mood, gazeX, gazeY, effectiveEyeOpen)
                drawLiveEye(rightEye, eyeRadius, eye, mood, gazeX, gazeY, effectiveEyeOpen)
            }
        }

        // Glasses stay on top of the animated eyes.
        drawCircle(color = glasses, radius = lensRadius, center = leftEye, style = Stroke(width = w * 0.025f))
        drawCircle(color = glasses, radius = lensRadius, center = rightEye, style = Stroke(width = w * 0.025f))
        drawLine(
            color = glasses,
            start = Offset(leftEye.x + lensRadius, leftEye.y),
            end = Offset(rightEye.x - lensRadius, rightEye.y),
            strokeWidth = w * 0.024f,
        )

        // Nose.
        drawCircle(color = nose, radius = w * 0.035f, center = Offset(cx, h * 0.61f))

        // Mouth: while speaking, the opening changes continuously instead of swapping images.
        if (speaking && mood != ProfHammyMood.SAD && mood != ProfHammyMood.PANIC) {
            val talkHeight = h * (0.055f + 0.085f * mouthPulse)
            drawOval(
                color = eye,
                topLeft = Offset(w * 0.42f, h * 0.65f),
                size = androidx.compose.ui.geometry.Size(w * 0.16f, talkHeight),
            )
            if (mouthPulse > 0.45f) {
                drawRect(
                    color = Color.White,
                    topLeft = Offset(w * 0.47f, h * 0.655f),
                    size = androidx.compose.ui.geometry.Size(w * 0.06f, h * 0.04f),
                )
            }
        } else {
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
                    drawOval(
                        color = eye,
                        topLeft = Offset(w * 0.445f, h * 0.655f),
                        size = androidx.compose.ui.geometry.Size(w * 0.11f, h * 0.11f),
                    )
                }

                ProfHammyMood.FOCUSED -> {
                    drawLine(
                        color = eye,
                        start = Offset(w * 0.45f, h * 0.69f),
                        end = Offset(w * 0.55f, h * 0.69f),
                        strokeWidth = w * 0.018f,
                    )
                }

                ProfHammyMood.EXCITED -> {
                    drawOval(
                        color = eye,
                        topLeft = Offset(w * 0.42f, h * 0.635f),
                        size = androidx.compose.ui.geometry.Size(w * 0.16f, h * 0.14f),
                    )
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(w * 0.47f, h * 0.645f),
                        size = androidx.compose.ui.geometry.Size(w * 0.06f, h * 0.045f),
                    )
                }

                ProfHammyMood.IDLE -> {
                    drawArc(
                        color = eye,
                        startAngle = 22f,
                        sweepAngle = 136f,
                        useCenter = false,
                        topLeft = Offset(w * 0.435f, h * 0.63f),
                        size = androidx.compose.ui.geometry.Size(w * 0.13f, h * 0.11f),
                        style = Stroke(width = w * 0.019f),
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
        drawCircle(color = bow.copy(alpha = 0.92f), radius = w * 0.055f, center = Offset(cx, h * 0.845f))

        if (mood == ProfHammyMood.EXCITED || mood == ProfHammyMood.HAPPY) {
            drawCircle(color = primary.copy(alpha = 0.35f), radius = w * 0.022f, center = Offset(w * 0.09f, h * 0.48f))
            drawCircle(color = primary.copy(alpha = 0.35f), radius = w * 0.016f, center = Offset(w * 0.90f, h * 0.36f))
        }

        if (mood == ProfHammyMood.EXCITED) {
            drawCircle(color = primary.copy(alpha = 0.28f), radius = w * 0.018f, center = Offset(w * 0.18f, h * 0.18f))
            drawCircle(color = primary.copy(alpha = 0.28f), radius = w * 0.014f, center = Offset(w * 0.82f, h * 0.20f))
        }

        drawRoundRect(
            color = surface.copy(alpha = 0.28f),
            topLeft = Offset(w * 0.28f, h * 0.91f),
            size = androidx.compose.ui.geometry.Size(w * 0.44f, h * 0.035f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.02f),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLiveEye(
    center: Offset,
    radius: Float,
    eyeColor: Color,
    mood: ProfHammyMood,
    gazeX: Float,
    gazeY: Float,
    openness: Float,
) {
    val open = openness.coerceIn(0.08f, 1f)
    if (open < 0.22f) {
        drawLine(
            color = eyeColor,
            start = Offset(center.x - radius * 0.9f, center.y),
            end = Offset(center.x + radius * 0.9f, center.y),
            strokeWidth = radius * 0.30f,
        )
        return
    }

    val whiteRadiusX = radius * 1.18f
    val whiteRadiusY = radius * 1.18f * open
    drawOval(
        color = Color.White,
        topLeft = Offset(center.x - whiteRadiusX, center.y - whiteRadiusY),
        size = androidx.compose.ui.geometry.Size(whiteRadiusX * 2f, whiteRadiusY * 2f),
    )

    if (open < 0.42f) return

    val pupilRadius = radius * when (mood) {
        ProfHammyMood.PANIC -> 0.66f
        ProfHammyMood.FOCUSED -> 0.78f
        else -> 0.84f
    }
    val pupilCenter = Offset(
        x = center.x + radius * 0.24f * gazeX.coerceIn(-1f, 1f),
        y = center.y + radius * 0.16f * gazeY.coerceIn(-1f, 1f),
    )
    drawCircle(color = eyeColor, radius = pupilRadius, center = pupilCenter)

    val highlightScale = if (mood == ProfHammyMood.EXCITED) 0.34f else 0.26f
    drawCircle(
        color = Color.White,
        radius = pupilRadius * highlightScale,
        center = Offset(
            pupilCenter.x - pupilRadius * 0.32f,
            pupilCenter.y - pupilRadius * 0.34f,
        ),
    )
}
