package com.sonharf.game.mascot

import android.graphics.BitmapFactory
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.R
import kotlinx.coroutines.delay

/** Uses the licensed Mage Cat package-derived runtime artwork. No Canvas fallback. */
@Composable
fun MageCatCompanion(
    modifier: Modifier = Modifier,
    size: Dp = 86.dp,
    speechBubbleText: String? = null,
    onClick: () -> Unit = {},
) {
    val mood = MageCatDirector.currentMood
    val resources = LocalContext.current.resources

    // Decode through Android BitmapFactory instead of painterResource. A malformed/unsupported
    // device decoder must never be able to crash the authenticated home shell.
    val mascotBitmap = remember(resources) {
        runCatching {
            resources.openRawResource(R.drawable.mage_cat_runtime).use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }.getOrNull()
    }

    val motion = rememberInfiniteTransition(label = "MageCatPackageMotion")
    val floatAnim by motion.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (mood == MageCatMood.PANIC) 180 else 1050, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "MageCatFloat",
    )
    val scaleAnim by motion.animateFloat(
        initialValue = .985f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "MageCatBreath",
    )

    LaunchedEffect(mood) {
        if (mood in setOf(MageCatMood.HAPPY, MageCatMood.WINK)) {
            delay(1500)
            MageCatDirector.resetToIdle()
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                translationY = if (mood == MageCatMood.PANIC) floatAnim * 2.3f else floatAnim
                scaleX = scaleAnim
                scaleY = scaleAnim
                rotationZ = when (mood) {
                    MageCatMood.PANIC -> floatAnim * .7f
                    MageCatMood.SAD, MageCatMood.CRYING -> -2f
                    MageCatMood.EXCITED -> floatAnim * .35f
                    else -> 0f
                }
                alpha = if (mood == MageCatMood.TIRED) .82f else 1f
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (!speechBubbleText.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    shadowElevation = 5.dp,
                    modifier = Modifier.padding(bottom = 2.dp),
                ) {
                    Text(
                        speechBubbleText,
                        color = Color(0xFF0F172A),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    )
                }
            }
            if (mascotBitmap != null) {
                Image(
                    bitmap = mascotBitmap,
                    contentDescription = "Mage Cat",
                    modifier = Modifier.size(size),
                    contentScale = ContentScale.Fit,
                )
            } else {
                // Preserve layout without substituting an imitation mascot.
                Spacer(Modifier.size(size))
            }
        }
    }
}
