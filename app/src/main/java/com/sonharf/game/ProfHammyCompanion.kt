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
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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

/** Lobby-safe Prof. Hammy card. */
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
                size = 94.dp,
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
 * Real-time layered 2D mascot rig.
 *
 * No static-frame slideshow is used. Head, body, ears, eyes, pupils, paws and mouth are animated
 * independently while this public API remains render-only and backward compatible.
 */
@Composable
fun ProfHammyCompanion(
    modifier: Modifier = Modifier,
    mood: ProfHammyMood = ProfHammyMood.IDLE,
    size: Dp = 92.dp,
    speaking: Boolean = false,
) {
    val transition = rememberInfiniteTransition(label = "ProfHammyLayeredRig")

    val bodyBob by transition.animateFloat(
        initialValue = -0.7f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "HammyBodyBob",
    )
    val breathing by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "HammyBreathing",
    )
    val headTilt by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "HammyHeadTilt",
    )
    val gazeX by transition.animateFloat(
        initialValue = -0.9f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "HammyGazeX",
    )
    val gazeY by transition.animateFloat(
        initialValue = -0.35f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(2700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "HammyGazeY",
    )
    val gesture by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (mood) {
                    ProfHammyMood.EXCITED, ProfHammyMood.PANIC -> 320
                    ProfHammyMood.HAPPY, ProfHammyMood.WINK -> 700
                    else -> 1250
                },
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "HammyGesture",
    )
    val mouthPulse by transition.animateFloat(
        initialValue = 0.12f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(185, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "HammyMouthPulse",
    )

    val blinkOpen = remember { Animatable(1f) }
    val earTwitch = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        var doubleBlink = false
        while (true) {
            delay(if (doubleBlink) 3400L else 2200L)
            blinkOpen.animateTo(0.07f, animationSpec = tween(65))
            blinkOpen.animateTo(1f, animationSpec = tween(95))
            if (!doubleBlink) {
                delay(115L)
                blinkOpen.animateTo(0.10f, animationSpec = tween(55))
                blinkOpen.animateTo(1f, animationSpec = tween(80))
            }
            doubleBlink = !doubleBlink
        }
    }

    LaunchedEffect(Unit) {
        var alternate = false
        while (true) {
            delay(if (alternate) 4500L else 3300L)
            earTwitch.animateTo(1f, animationSpec = tween(80))
            earTwitch.animateTo(-0.55f, animationSpec = tween(90))
            earTwitch.animateTo(0f, animationSpec = tween(130))
            alternate = !alternate
        }
    }

    val energy = when (mood) {
        ProfHammyMood.EXCITED -> 1.0f
        ProfHammyMood.PANIC -> 0.95f
        ProfHammyMood.HAPPY, ProfHammyMood.WINK -> 0.65f
        ProfHammyMood.FOCUSED -> 0.35f
        ProfHammyMood.SAD -> 0.20f
        ProfHammyMood.IDLE -> 0.28f
    }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                translationY = bodyBob * (0.4f + energy)
                scaleX = 0.995f + breathing * 0.005f
                scaleY = 0.990f + breathing * 0.010f
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size)) {
            drawProfHammyRig(
                mood = mood,
                eyeOpen = blinkOpen.value,
                gazeX = gazeX,
                gazeY = gazeY,
                earTwitch = earTwitch.value,
                gesture = gesture,
                mouthPulse = mouthPulse,
                speaking = speaking,
                headTilt = headTilt,
                breathing = breathing,
            )
        }
    }
}

private fun DrawScope.drawProfHammyRig(
    mood: ProfHammyMood,
    eyeOpen: Float,
    gazeX: Float,
    gazeY: Float,
    earTwitch: Float,
    gesture: Float,
    mouthPulse: Float,
    speaking: Boolean,
    headTilt: Float,
    breathing: Float,
) {
    val w = size.width
    val h = size.height
    val cx = w * 0.50f

    val fur = Color(0xFFE8A044)
    val furDark = Color(0xFFB86A22)
    val furLight = Color(0xFFF6C46E)
    val cream = Color(0xFFFFE7C2)
    val earPink = Color(0xFFF2A4A0)
    val eye = Color(0xFF33231A)
    val glasses = Color(0xFF4B3022)
    val nose = Color(0xFFE98D8D)
    val blush = Color(0xFFF39A98)
    val bow = Color(0xFF5F8365)
    val bowDark = Color(0xFF446247)
    val pencil = Color(0xFFE7B34F)

    // Ground shadow — reacts subtly to breathing so the body feels planted.
    val shadowScale = 1f - breathing * 0.04f
    drawOval(
        color = Color.Black.copy(alpha = 0.10f),
        topLeft = Offset(w * (0.31f + 0.01f * breathing), h * 0.885f),
        size = Size(w * 0.38f * shadowScale, h * 0.055f),
    )

    // BODY LAYER
    val bodyTop = h * 0.55f
    val bodyHeight = h * (0.34f + breathing * 0.012f)
    drawOval(
        color = furDark.copy(alpha = 0.22f),
        topLeft = Offset(w * 0.265f, bodyTop + h * 0.015f),
        size = Size(w * 0.47f, bodyHeight),
    )
    drawOval(
        color = fur,
        topLeft = Offset(w * 0.275f, bodyTop),
        size = Size(w * 0.45f, bodyHeight),
    )
    drawOval(
        color = cream,
        topLeft = Offset(w * 0.355f, h * 0.635f),
        size = Size(w * 0.29f, h * 0.22f),
    )
    drawOval(
        color = furLight.copy(alpha = 0.42f),
        topLeft = Offset(w * 0.34f, h * 0.575f),
        size = Size(w * 0.18f, h * 0.08f),
    )

    // FEET LAYER
    val footLift = if (mood == ProfHammyMood.EXCITED) h * 0.008f * gesture else 0f
    drawOval(
        color = furDark,
        topLeft = Offset(w * 0.285f, h * 0.81f - footLift),
        size = Size(w * 0.18f, h * 0.095f),
    )
    drawOval(
        color = furDark,
        topLeft = Offset(w * 0.535f, h * 0.81f + footLift),
        size = Size(w * 0.18f, h * 0.095f),
    )
    drawOval(
        color = cream.copy(alpha = 0.80f),
        topLeft = Offset(w * 0.315f, h * 0.832f - footLift),
        size = Size(w * 0.11f, h * 0.05f),
    )
    drawOval(
        color = cream.copy(alpha = 0.80f),
        topLeft = Offset(w * 0.575f, h * 0.832f + footLift),
        size = Size(w * 0.11f, h * 0.05f),
    )

    // ARM / PAW LAYERS — each has an independent pivot.
    val leftShoulder = Offset(w * 0.31f, h * 0.64f)
    val rightShoulder = Offset(w * 0.69f, h * 0.64f)
    val leftAngle = when (mood) {
        ProfHammyMood.PANIC -> -26f - gesture * 24f
        ProfHammyMood.EXCITED -> -18f - gesture * 13f
        ProfHammyMood.HAPPY, ProfHammyMood.WINK -> -10f - gesture * 6f
        ProfHammyMood.SAD -> 11f
        else -> -3f - gesture * 2f
    }
    val rightAngle = when (mood) {
        ProfHammyMood.EXCITED -> -48f + gesture * 23f
        ProfHammyMood.WINK -> -34f + gesture * 14f
        ProfHammyMood.HAPPY -> -24f + gesture * 9f
        ProfHammyMood.PANIC -> 26f + gesture * 24f
        ProfHammyMood.SAD -> -9f
        else -> 4f + gesture * 2f
    }

    rotate(leftAngle, pivot = leftShoulder) {
        drawRoundRect(
            color = fur,
            topLeft = Offset(leftShoulder.x - w * 0.038f, leftShoulder.y),
            size = Size(w * 0.095f, h * 0.205f),
            cornerRadius = CornerRadius(w * 0.05f),
        )
        drawCircle(
            color = cream,
            radius = w * 0.042f,
            center = Offset(leftShoulder.x + w * 0.010f, leftShoulder.y + h * 0.185f),
        )
    }
    rotate(rightAngle, pivot = rightShoulder) {
        drawRoundRect(
            color = fur,
            topLeft = Offset(rightShoulder.x - w * 0.055f, rightShoulder.y),
            size = Size(w * 0.095f, h * 0.205f),
            cornerRadius = CornerRadius(w * 0.05f),
        )
        val rightPaw = Offset(rightShoulder.x - w * 0.005f, rightShoulder.y + h * 0.185f)
        drawCircle(color = cream, radius = w * 0.042f, center = rightPaw)
        if (mood == ProfHammyMood.WINK || mood == ProfHammyMood.HAPPY || mood == ProfHammyMood.EXCITED) {
            drawLine(
                color = pencil,
                start = Offset(rightPaw.x + w * 0.008f, rightPaw.y + h * 0.010f),
                end = Offset(rightPaw.x + w * 0.095f, rightPaw.y - h * 0.060f),
                strokeWidth = w * 0.025f,
            )
            drawLine(
                color = Color(0xFF5A4636),
                start = Offset(rightPaw.x + w * 0.090f, rightPaw.y - h * 0.055f),
                end = Offset(rightPaw.x + w * 0.105f, rightPaw.y - h * 0.070f),
                strokeWidth = w * 0.017f,
            )
        }
    }

    // Bow tie belongs to the body, not the head.
    drawOval(
        color = bow,
        topLeft = Offset(w * 0.37f, h * 0.595f),
        size = Size(w * 0.16f, h * 0.085f),
    )
    drawOval(
        color = bow,
        topLeft = Offset(w * 0.47f, h * 0.595f),
        size = Size(w * 0.16f, h * 0.085f),
    )
    drawCircle(color = bowDark, radius = w * 0.035f, center = Offset(cx, h * 0.638f))

    // HEAD RIG — everything inside rotates around the neck independently of the body.
    val headPivot = Offset(cx, h * 0.49f)
    val moodTiltMultiplier = when (mood) {
        ProfHammyMood.EXCITED -> 1.7f
        ProfHammyMood.PANIC -> 2.0f
        ProfHammyMood.WINK, ProfHammyMood.HAPPY -> 1.0f
        ProfHammyMood.SAD -> 0.45f
        else -> 0.65f
    }

    rotate(headTilt * moodTiltMultiplier, pivot = headPivot) {
        val leftEar = Offset(
            w * (0.30f - earTwitch * 0.007f),
            h * (0.235f + earTwitch * 0.007f),
        )
        val rightEar = Offset(
            w * (0.70f + earTwitch * 0.006f),
            h * (0.235f - earTwitch * 0.006f),
        )

        drawCircle(color = furDark, radius = w * 0.13f, center = leftEar)
        drawCircle(color = furDark, radius = w * 0.13f, center = rightEar)
        drawCircle(color = fur, radius = w * 0.118f, center = leftEar)
        drawCircle(color = fur, radius = w * 0.118f, center = rightEar)
        drawCircle(color = earPink, radius = w * 0.070f, center = leftEar)
        drawCircle(color = earPink, radius = w * 0.070f, center = rightEar)

        drawCircle(
            color = furDark.copy(alpha = 0.24f),
            radius = w * 0.305f,
            center = Offset(cx, h * 0.425f + h * 0.008f),
        )
        drawCircle(color = fur, radius = w * 0.30f, center = Offset(cx, h * 0.415f))
        drawOval(
            color = furLight.copy(alpha = 0.55f),
            topLeft = Offset(w * 0.35f, h * 0.235f),
            size = Size(w * 0.25f, h * 0.11f),
        )

        // Muzzle / cheeks.
        drawOval(
            color = cream,
            topLeft = Offset(w * 0.315f, h * 0.425f),
            size = Size(w * 0.25f, h * 0.20f),
        )
        drawOval(
            color = cream,
            topLeft = Offset(w * 0.435f, h * 0.425f),
            size = Size(w * 0.25f, h * 0.20f),
        )

        val leftEye = Offset(w * 0.405f, h * 0.392f)
        val rightEye = Offset(w * 0.595f, h * 0.392f)
        val eyeRadius = w * 0.055f
        val lensRadius = w * 0.095f
        val effectiveEyeOpen = if (mood == ProfHammyMood.PANIC) eyeOpen.coerceAtLeast(0.62f) else eyeOpen

        if (mood == ProfHammyMood.WINK) {
            drawLine(
                color = eye,
                start = Offset(leftEye.x - w * 0.045f, leftEye.y),
                end = Offset(leftEye.x + w * 0.045f, leftEye.y),
                strokeWidth = w * 0.020f,
            )
            drawRigEye(rightEye, eyeRadius, eye, mood, gazeX, gazeY, effectiveEyeOpen)
        } else {
            val gx = if (mood == ProfHammyMood.FOCUSED) gazeX * 0.35f else gazeX
            val gy = when (mood) {
                ProfHammyMood.SAD -> gazeY * 0.35f + 0.30f
                ProfHammyMood.FOCUSED -> gazeY * 0.35f
                else -> gazeY
            }
            val scale = if (mood == ProfHammyMood.PANIC) 1.18f else if (mood == ProfHammyMood.SAD) 0.90f else 1f
            drawRigEye(leftEye, eyeRadius * scale, eye, mood, gx, gy, effectiveEyeOpen)
            drawRigEye(rightEye, eyeRadius * scale, eye, mood, gx, gy, effectiveEyeOpen)
        }

        // Eyebrows create clearer expression separation.
        when (mood) {
            ProfHammyMood.FOCUSED -> {
                drawLine(
                    color = furDark,
                    start = Offset(w * 0.35f, h * 0.31f),
                    end = Offset(w * 0.45f, h * 0.335f),
                    strokeWidth = w * 0.018f,
                )
                drawLine(
                    color = furDark,
                    start = Offset(w * 0.55f, h * 0.335f),
                    end = Offset(w * 0.65f, h * 0.31f),
                    strokeWidth = w * 0.018f,
                )
            }
            ProfHammyMood.SAD -> {
                drawLine(
                    color = furDark,
                    start = Offset(w * 0.35f, h * 0.34f),
                    end = Offset(w * 0.45f, h * 0.31f),
                    strokeWidth = w * 0.016f,
                )
                drawLine(
                    color = furDark,
                    start = Offset(w * 0.55f, h * 0.31f),
                    end = Offset(w * 0.65f, h * 0.34f),
                    strokeWidth = w * 0.016f,
                )
            }
            ProfHammyMood.EXCITED -> {
                drawArc(
                    color = furDark,
                    startAngle = 195f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(w * 0.34f, h * 0.29f),
                    size = Size(w * 0.12f, h * 0.07f),
                    style = Stroke(width = w * 0.014f),
                )
                drawArc(
                    color = furDark,
                    startAngle = 225f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(w * 0.54f, h * 0.29f),
                    size = Size(w * 0.12f, h * 0.07f),
                    style = Stroke(width = w * 0.014f),
                )
            }
            else -> Unit
        }

        // Glasses are a stable identity layer above animated eyes.
        drawCircle(color = glasses, radius = lensRadius, center = leftEye, style = Stroke(width = w * 0.021f))
        drawCircle(color = glasses, radius = lensRadius, center = rightEye, style = Stroke(width = w * 0.021f))
        drawLine(
            color = glasses,
            start = Offset(leftEye.x + lensRadius, leftEye.y),
            end = Offset(rightEye.x - lensRadius, rightEye.y),
            strokeWidth = w * 0.020f,
        )

        drawCircle(color = nose, radius = w * 0.030f, center = Offset(cx, h * 0.505f))

        if (mood == ProfHammyMood.HAPPY || mood == ProfHammyMood.EXCITED || mood == ProfHammyMood.WINK) {
            drawCircle(color = blush.copy(alpha = 0.32f), radius = w * 0.035f, center = Offset(w * 0.34f, h * 0.505f))
            drawCircle(color = blush.copy(alpha = 0.32f), radius = w * 0.035f, center = Offset(w * 0.66f, h * 0.505f))
        }

        drawRigMouth(
            mood = mood,
            speaking = speaking,
            mouthPulse = mouthPulse,
            eyeColor = eye,
            w = w,
            h = h,
        )
    }

    // Tiny excitement accents live outside the head pivot so they feel like scene particles.
    if (mood == ProfHammyMood.EXCITED) {
        val accent = MaterialTheme.colorScheme.primary
        drawCircle(color = accent.copy(alpha = 0.34f), radius = w * 0.018f, center = Offset(w * 0.15f, h * 0.31f))
        drawCircle(color = accent.copy(alpha = 0.30f), radius = w * 0.014f, center = Offset(w * 0.85f, h * 0.26f))
        drawCircle(color = accent.copy(alpha = 0.24f), radius = w * 0.010f, center = Offset(w * 0.81f, h * 0.43f))
    }
}

private fun DrawScope.drawRigMouth(
    mood: ProfHammyMood,
    speaking: Boolean,
    mouthPulse: Float,
    eyeColor: Color,
    w: Float,
    h: Float,
) {
    if (speaking && mood != ProfHammyMood.SAD && mood != ProfHammyMood.PANIC) {
        val talkHeight = h * (0.035f + 0.065f * mouthPulse)
        drawOval(
            color = eyeColor,
            topLeft = Offset(w * 0.455f, h * 0.535f),
            size = Size(w * 0.09f, talkHeight),
        )
        if (mouthPulse > 0.50f) {
            drawRect(
                color = Color.White,
                topLeft = Offset(w * 0.478f, h * 0.54f),
                size = Size(w * 0.044f, h * 0.027f),
            )
        }
        return
    }

    when (mood) {
        ProfHammyMood.SAD -> drawArc(
            color = eyeColor,
            startAngle = 205f,
            sweepAngle = 130f,
            useCenter = false,
            topLeft = Offset(w * 0.45f, h * 0.535f),
            size = Size(w * 0.10f, h * 0.07f),
            style = Stroke(width = w * 0.018f),
        )
        ProfHammyMood.PANIC -> drawOval(
            color = eyeColor,
            topLeft = Offset(w * 0.465f, h * 0.535f),
            size = Size(w * 0.07f, h * 0.075f),
        )
        ProfHammyMood.FOCUSED -> drawLine(
            color = eyeColor,
            start = Offset(w * 0.465f, h * 0.565f),
            end = Offset(w * 0.535f, h * 0.565f),
            strokeWidth = w * 0.016f,
        )
        ProfHammyMood.EXCITED -> {
            drawOval(
                color = eyeColor,
                topLeft = Offset(w * 0.445f, h * 0.525f),
                size = Size(w * 0.11f, h * 0.095f),
            )
            drawRect(
                color = Color.White,
                topLeft = Offset(w * 0.477f, h * 0.532f),
                size = Size(w * 0.046f, h * 0.028f),
            )
        }
        ProfHammyMood.IDLE -> drawArc(
            color = eyeColor,
            startAngle = 18f,
            sweepAngle = 145f,
            useCenter = false,
            topLeft = Offset(w * 0.455f, h * 0.525f),
            size = Size(w * 0.09f, h * 0.07f),
            style = Stroke(width = w * 0.016f),
        )
        else -> {
            drawArc(
                color = eyeColor,
                startAngle = 15f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(w * 0.445f, h * 0.515f),
                size = Size(w * 0.11f, h * 0.09f),
                style = Stroke(width = w * 0.018f),
            )
            drawRect(
                color = Color.White,
                topLeft = Offset(w * 0.482f, h * 0.535f),
                size = Size(w * 0.036f, h * 0.036f),
            )
        }
    }
}

private fun DrawScope.drawRigEye(
    center: Offset,
    radius: Float,
    eyeColor: Color,
    mood: ProfHammyMood,
    gazeX: Float,
    gazeY: Float,
    openness: Float,
) {
    val open = openness.coerceIn(0.07f, 1f)
    if (open < 0.22f) {
        drawLine(
            color = eyeColor,
            start = Offset(center.x - radius * 0.9f, center.y),
            end = Offset(center.x + radius * 0.9f, center.y),
            strokeWidth = radius * 0.30f,
        )
        return
    }

    val whiteX = radius * 1.20f
    val whiteY = radius * 1.18f * open
    drawOval(
        color = Color.White,
        topLeft = Offset(center.x - whiteX, center.y - whiteY),
        size = Size(whiteX * 2f, whiteY * 2f),
    )

    if (open < 0.42f) return

    val pupilRadius = radius * when (mood) {
        ProfHammyMood.PANIC -> 0.64f
        ProfHammyMood.FOCUSED -> 0.78f
        else -> 0.84f
    }
    val pupilCenter = Offset(
        x = center.x + radius * 0.25f * gazeX.coerceIn(-1f, 1f),
        y = center.y + radius * 0.18f * gazeY.coerceIn(-1f, 1f),
    )
    drawCircle(color = eyeColor, radius = pupilRadius, center = pupilCenter)

    val highlight = if (mood == ProfHammyMood.EXCITED) 0.34f else 0.27f
    drawCircle(
        color = Color.White,
        radius = pupilRadius * highlight,
        center = Offset(
            pupilCenter.x - pupilRadius * 0.32f,
            pupilCenter.y - pupilRadius * 0.34f,
        ),
    )
}
