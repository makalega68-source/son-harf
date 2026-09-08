package com.sonharf.game.mascot

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Lightweight vector fallback for the Mage Cat companion. It uses no bitmap or
 * video asset, keeping frame cost predictable on the live duel surface.
 */
@Composable
fun MageCatCompanion(
    modifier: Modifier = Modifier,
    size: Dp = 68.dp,
    speechBubbleText: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val mood = MageCatDirector.currentMood
    val transition = rememberInfiniteTransition(label = "mage_cat_motion")
    val floatAnim by transition.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (mood == MageCatMood.PANIC) 180 else 950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mage_cat_float",
    )
    val scaleAnim by transition.animateFloat(
        initialValue = .985f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mage_cat_breathe",
    )

    LaunchedEffect(mood) {
        if (mood == MageCatMood.HAPPY || mood == MageCatMood.WINK || mood == MageCatMood.SAD) {
            delay(1500)
            MageCatDirector.resetToIdle()
        }
    }

    val interaction = if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)
    Column(
        modifier = modifier
            .then(interaction)
            .graphicsLayer {
                translationY = if (mood == MageCatMood.PANIC) floatAnim * 2.2f else floatAnim
                scaleX = scaleAnim
                scaleY = scaleAnim
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!speechBubbleText.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.White.copy(alpha = .96f),
                shadowElevation = 3.dp,
                modifier = Modifier.padding(bottom = 3.dp),
            ) {
                Text(
                    text = speechBubbleText,
                    color = Color(0xFF172033),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                )
            }
        }

        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            when (mood) {
                                MageCatMood.EXCITED, MageCatMood.HAPPY -> Color(0xFFFBBF24)
                                MageCatMood.PANIC, MageCatMood.ANGRY -> Color(0xFFEF4444)
                                MageCatMood.SAD, MageCatMood.CRYING -> Color(0xFF38BDF8)
                                else -> Color(0xFF6366F1)
                            },
                            Color(0xFF0F172A),
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            MageCatFace(mood)
        }
    }
}

@Composable
private fun MageCatFace(mood: MageCatMood) {
    val eyeColor = when (MageCatDirector.eyeColorVariant) {
        "green" -> Color(0xFF10B981)
        "orange" -> Color(0xFFF97316)
        else -> Color(0xFF38BDF8)
    }

    Canvas(modifier = Modifier.size(58.dp).padding(7.dp)) {
        val w = size.width
        val h = size.height
        val fur = Color(0xFF334155)
        val leftEye = Offset(w * .35f, h * .52f)
        val rightEye = Offset(w * .65f, h * .52f)

        drawCircle(fur, radius = w * .16f, center = Offset(w * .25f, h * .22f))
        drawCircle(fur, radius = w * .16f, center = Offset(w * .75f, h * .22f))
        drawCircle(fur, radius = w * .37f, center = Offset(w * .50f, h * .56f))

        when (mood) {
            MageCatMood.HAPPY -> {
                drawArc(eyeColor, 180f, 180f, false, Offset(leftEye.x - 10f, leftEye.y - 10f), Size(20f, 20f), style = Stroke(4f))
                drawArc(eyeColor, 180f, 180f, false, Offset(rightEye.x - 10f, rightEye.y - 10f), Size(20f, 20f), style = Stroke(4f))
            }
            MageCatMood.PANIC, MageCatMood.EXCITED -> {
                drawCircle(Color.White, 12f, leftEye)
                drawCircle(eyeColor, 7f, leftEye)
                drawCircle(Color.White, 12f, rightEye)
                drawCircle(eyeColor, 7f, rightEye)
            }
            MageCatMood.SAD, MageCatMood.CRYING -> {
                drawArc(Color(0xFF7DD3FC), 0f, 180f, false, Offset(leftEye.x - 10f, leftEye.y - 5f), Size(20f, 16f), style = Stroke(4f))
                drawArc(Color(0xFF7DD3FC), 0f, 180f, false, Offset(rightEye.x - 10f, rightEye.y - 5f), Size(20f, 16f), style = Stroke(4f))
            }
            MageCatMood.WINK -> {
                drawCircle(eyeColor, 9f, leftEye)
                drawLine(eyeColor, Offset(rightEye.x - 9f, rightEye.y), Offset(rightEye.x + 9f, rightEye.y), 4f)
            }
            else -> {
                drawCircle(eyeColor, 9f, leftEye)
                drawCircle(Color.White, 3f, Offset(leftEye.x - 2f, leftEye.y - 2f))
                drawCircle(eyeColor, 9f, rightEye)
                drawCircle(Color.White, 3f, Offset(rightEye.x - 2f, rightEye.y - 2f))
            }
        }

        drawCircle(Color(0xFFF472B6), radius = 3f, center = Offset(w * .50f, h * .66f))
    }
}
