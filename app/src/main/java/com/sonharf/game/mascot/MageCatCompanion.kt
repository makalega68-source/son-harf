package com.sonharf.game.mascot

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.sonharf.game.R

/** AI-redrawn 2D expressions based on the owner's licensed Mage Cat. */
@Composable
fun MageCatCompanion(
    modifier: Modifier = Modifier,
    size: Dp = 86.dp,
    speechBubbleText: String? = null,
    onClick: (() -> Unit)? = null,
    moodOverride: MageCatMood? = null,
    eventKey: Long = 0,
    animateIdle: Boolean = true,
) {
    val mood = moodOverride ?: MageCatDirector.currentMood
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val motionEnabled = ValueAnimator.areAnimatorsEnabled()
    val frames by produceState<List<ImageBitmap>>(emptyList(), context.resources) {
        value = withContext(Dispatchers.Default) {
            MageCatFrames.load(context.resources)
        }
    }
    val lift = remember { Animatable(0f) }
    val tilt = remember { Animatable(0f) }
    LaunchedEffect(mood, eventKey, motionEnabled) {
        lift.snapTo(0f); tilt.snapTo(0f)
        if (motionEnabled) {
            when (mood) {
                MageCatMood.EXCITED, MageCatMood.HAPPY -> repeat(if (mood == MageCatMood.EXCITED) 2 else 1) {
                    lift.animateTo(-5f, tween(180)); lift.animateTo(0f, tween(240))
                }
                MageCatMood.WINK -> { tilt.animateTo(-5f, tween(180)); tilt.animateTo(0f, tween(260)) }
                MageCatMood.SAD, MageCatMood.CRYING -> { tilt.animateTo(3f, tween(300)); tilt.animateTo(0f, tween(400)) }
                else -> Unit
            }
        }
        if (moodOverride == null && mood != MageCatMood.IDLE) {
            delay(2600)
            if (MageCatDirector.currentMood == mood) MageCatDirector.resetToIdle()
        }
    }
    val breath = remember { Animatable(1f) }
    LaunchedEffect(animateIdle, motionEnabled) {
        breath.snapTo(1f)
        if (animateIdle && motionEnabled) while (true) {
            breath.animateTo(1.018f, tween(1800)); breath.animateTo(1f, tween(1800))
        }
    }
    val index = when (mood) {
        MageCatMood.IDLE -> 0; MageCatMood.HAPPY -> 1; MageCatMood.EXCITED -> 2
        MageCatMood.PANIC -> 3; MageCatMood.ANGRY -> 4; MageCatMood.SAD -> 5
        MageCatMood.CRYING -> 6; MageCatMood.WINK -> 7; MageCatMood.TIRED -> 8
    }
    Column(modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally) {
        if (!speechBubbleText.isNullOrBlank()) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.widthIn(max = 160.dp)) {
                Text(speechBubbleText, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp, maxLines = 3, modifier = Modifier.padding(8.dp))
            }
        }
        val frame = frames.getOrNull(index)
        if (frame != null) Image(bitmap = frame, contentDescription = "Mage Cat",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(size).graphicsLayer {
                translationY = lift.value * density; rotationZ = tilt.value
                scaleX = breath.value; scaleY = breath.value
            })
        else Spacer(Modifier.size(size))
    }
}

private object MageCatFrames {
    @Volatile private var cached: List<ImageBitmap>? = null
    @Synchronized fun load(resources: android.content.res.Resources): List<ImageBitmap> {
        cached?.let { return it }
        return runCatching {
            val options = BitmapFactory.Options().apply { inScaled = false; inSampleSize = 2 }
            val atlas = resources.openRawResource(R.drawable.mage_cat_expressions).use {
                requireNotNull(BitmapFactory.decodeStream(it, null, options))
            }
            require(atlas.width >= 300 && atlas.height >= 300)
            val frames = (0..8).map { cell ->
                val x = cell % 3 * atlas.width / 3
                val y = cell / 3 * atlas.height / 3
                Bitmap.createBitmap(atlas, x, y, atlas.width / 3, atlas.height / 3).asImageBitmap()
            }
            atlas.recycle()
            frames.also { cached = it }
        }.getOrDefault(emptyList())
    }
}
