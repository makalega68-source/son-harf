package com.sonharf.game.companion

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class CompanionMoment(val frameIndex: Int) {
    LOBBY(7),
    LOADING(0),
    RESULT_WIN(2),
    RESULT_LOSS(6),
    BIG_SIEGE(1),
    LEAGUE_UP(2),
}

/**
 * Pure presentation component for the purchased 2D companion asset.
 *
 * It receives an immutable UI snapshot (`moment` + optional copy), performs no backend reads,
 * polling, economy/game writes or gameplay decisions, and respects Android's reduced-motion
 * setting. Keeping the input one-way makes it safe to use around competitive surfaces.
 */
@Composable
fun LicensedCatCompanion(
    moment: CompanionMoment,
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    message: String? = null,
    accessibilityLabel: String? = null,
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val motionEnabled = ValueAnimator.areAnimatorsEnabled()
    val frames by produceState<List<ImageBitmap>>(emptyList(), context.resources) {
        value = withContext(Dispatchers.Default) {
            LicensedCatFrames.load(context.resources)
        }
    }
    val lift = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }

    LaunchedEffect(moment, motionEnabled) {
        lift.snapTo(0f)
        scale.snapTo(1f)
        if (motionEnabled) {
            when (moment) {
                CompanionMoment.RESULT_WIN,
                CompanionMoment.BIG_SIEGE,
                CompanionMoment.LEAGUE_UP,
                -> {
                    lift.animateTo(-4f, tween(160))
                    scale.animateTo(1.025f, tween(160))
                    lift.animateTo(0f, tween(220))
                    scale.animateTo(1f, tween(220))
                }
                else -> Unit
            }
        }
    }

    val semanticModifier = if (accessibilityLabel.isNullOrBlank()) modifier else {
        modifier.semantics { contentDescription = accessibilityLabel }
    }

    Column(
        modifier = semanticModifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!message.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.widthIn(max = 220.dp),
            ) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
        }

        val frame = frames.getOrNull(moment.frameIndex)
        if (frame == null) {
            Spacer(Modifier.size(size))
        } else {
            Image(
                bitmap = frame,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(size)
                    .graphicsLayer {
                        translationY = lift.value * density
                        scaleX = scale.value
                        scaleY = scale.value
                    },
            )
        }
    }
}

private object LicensedCatFrames {
    @Volatile
    private var cached: List<ImageBitmap>? = null

    @Synchronized
    fun load(resources: android.content.res.Resources): List<ImageBitmap> {
        cached?.let { return it }
        return runCatching {
            val options = BitmapFactory.Options().apply {
                inScaled = false
                inSampleSize = 2
            }
            val atlas = resources.openRawResource(R.drawable.licensed_cat_companion_atlas).use {
                requireNotNull(BitmapFactory.decodeStream(it, null, options))
            }
            require(atlas.width >= 300 && atlas.height >= 300)
            val frames = (0..8).map { cell ->
                val cellWidth = atlas.width / 3
                val cellHeight = atlas.height / 3
                val x = cell % 3 * cellWidth
                val y = cell / 3 * cellHeight
                Bitmap.createBitmap(atlas, x, y, cellWidth, cellHeight).asImageBitmap()
            }
            atlas.recycle()
            frames.also { cached = it }
        }.getOrDefault(emptyList())
    }
}
