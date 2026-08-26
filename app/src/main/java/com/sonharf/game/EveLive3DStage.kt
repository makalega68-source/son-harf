package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.filament.gltfio.Animator
import io.github.sceneview.SceneView
import io.github.sceneview.SurfaceType
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberFillLightNode
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import kotlinx.coroutines.delay
import kotlin.math.min

/**
 * Small Filament-native skeletal mixer for Eve.
 *
 * SceneView's ModelNode playAnimation API switches clips immediately. Eve instead owns the
 * animator timeline and uses Filament Animator.applyCrossFade in the documented order:
 * applyAnimation(current) -> applyCrossFade(previous) -> updateBoneMatrices(). This blends bone
 * poses rather than fading two rendered images, so reactions remain a single real GLB character.
 */
private class EveAnimationMixer(
    private val blendDurationNanos: Long,
) {
    private data class Track(
        val index: Int,
        val startedAtNanos: Long,
        val loop: Boolean,
    )

    private var current: Track? = null
    private var previous: Track? = null
    private var blendStartedAtNanos: Long = 0L

    fun transition(
        animator: Animator,
        animationName: String,
        loop: Boolean,
        nowNanos: Long = System.nanoTime(),
    ) {
        val index = (0 until animator.animationCount)
            .firstOrNull { animator.getAnimationName(it) == animationName }
            ?: return

        previous = current
        current = Track(
            index = index,
            startedAtNanos = nowNanos,
            loop = loop,
        )
        blendStartedAtNanos = nowNanos
    }

    fun apply(animator: Animator, frameTimeNanos: Long) {
        val active = current ?: return
        animator.applyAnimation(
            active.index,
            animationTime(animator, active, frameTimeNanos),
        )

        previous?.let { old ->
            val alpha = if (blendDurationNanos <= 0L) {
                1f
            } else {
                ((frameTimeNanos - blendStartedAtNanos).toDouble() / blendDurationNanos.toDouble())
                    .toFloat()
                    .coerceIn(0f, 1f)
            }

            if (alpha < 1f) {
                animator.applyCrossFade(
                    old.index,
                    animationTime(animator, old, frameTimeNanos),
                    alpha,
                )
            } else {
                previous = null
            }
        }

        animator.updateBoneMatrices()
    }

    private fun animationTime(
        animator: Animator,
        track: Track,
        frameTimeNanos: Long,
    ): Float {
        val duration = animator.getAnimationDuration(track.index).coerceAtLeast(0.0001f)
        val elapsedSeconds = ((frameTimeNanos - track.startedAtNanos).coerceAtLeast(0L) / 1_000_000_000.0)
            .toFloat()
        return if (track.loop) {
            elapsedSeconds % duration
        } else {
            min(elapsedSeconds, duration)
        }
    }
}

/**
 * Production live companion stage used by the active room and home overlay.
 *
 * Guarantees:
 * - real Eve GLB only; no 2D fallback,
 * - the full room keeps SurfaceView-backed Vulkan for IME/window-relayout stability,
 * - compact home Eve uses TextureSurface because SceneView's Surface type is always Z-ordered
 *   behind Compose and therefore cannot be a true floating overlay over home cards,
 * - animationVersion restarts the same named clip deterministically,
 * - clip changes use Filament skeletal cross-fade rather than image/viewport swapping,
 * - no extra SceneView geometry/material is injected into the Vulkan scene. Both the procedural
 *   contact shadow and a later primitive grounding experiment hit Filament's
 *   "Normalized format does not exist" abort on the software Vulkan validation backend,
 * - the non-compact room gets a Compose-only grounding pool,
 * - lighting only tunes SceneView's existing directional key/fill nodes. No custom material or
 *   renderer resource is introduced for the lighting pass.
 */
@Composable
internal fun EveLive3DStage(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val context = LocalContext.current
    val assetAvailable = remember {
        runCatching { context.assets.open(EveAssetPolicy.MODEL_ASSET).use { } }.isSuccess
    }

    if (!assetAvailable) {
        val message = "FATAL: ${EveAssetPolicy.MODEL_ASSET} is missing from the APK"
        if (!BuildConfig.DEBUG) error(message)
        Surface(
            modifier = modifier.padding(12.dp),
            color = Color(0xFF7F1D1D),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFFFB4AB)),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "3D EVE ASSET HATASI\n${EveAssetPolicy.MODEL_ASSET}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                )
            }
        }
        return
    }

    val engine = rememberEngine(
        engineCreator = { eglContext ->
            runCatching {
                com.google.android.filament.Engine.create(
                    com.google.android.filament.Engine.Backend.VULKAN,
                )
            }.getOrElse {
                com.google.android.filament.Engine.create(eglContext)
            }
        },
    )
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, EveAssetPolicy.MODEL_ASSET)
    val mainLightNode = rememberMainLightNode(engine) {
        intensity = 8_500f
        lightDirection = Direction(-0.45f, -0.75f, -0.48f)
    }
    val fillLightNode = rememberFillLightNode(engine) {
        intensity = 2_500f
        lightDirection = Direction(0.68f, -0.47f, 0.56f)
    }
    val cue = EveMascotRuntime.animation
    val cueVersion = EveMascotRuntime.animationVersion
    val animationMixer = remember(modelInstance, compact) {
        EveAnimationMixer(
            blendDurationNanos = if (compact) 180_000_000L else 240_000_000L,
        )
    }

    var loadTimedOut by remember { mutableStateOf(false) }
    var liveModelNode by remember(modelInstance) {
        mutableStateOf<io.github.sceneview.node.ModelNode?>(null)
    }

    LaunchedEffect(modelInstance) {
        if (modelInstance == null) {
            delay(8_000)
            loadTimedOut = true
        } else {
            loadTimedOut = false
        }
    }

    if (loadTimedOut && modelInstance == null) {
        val message = "FATAL: Eve GLB exists but SceneView could not create a model instance"
        if (!BuildConfig.DEBUG) error(message)
        Surface(
            modifier = modifier.padding(12.dp),
            color = Color(0xFF7F1D1D),
            shape = RoundedCornerShape(16.dp),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "3D EVE YÜKLEME HATASI\nGLB APK içinde fakat ModelNode oluşturulamadı.",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                )
            }
        }
        return
    }

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        if (modelInstance == null) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
        } else {
            SceneView(
                modifier = Modifier.fillMaxSize(),
                surfaceType = if (compact) SurfaceType.TextureSurface else SurfaceType.Surface,
                isOpaque = false,
                engine = engine,
                modelLoader = modelLoader,
                mainLightNode = mainLightNode,
                fillLightNode = fillLightNode,
                cameraManipulator = null,
                onFrame = { frameTimeNanos ->
                    liveModelNode?.let { node ->
                        animationMixer.apply(node.animator, frameTimeNanos)
                    }
                },
            ) {
                ModelNode(
                    modelInstance = modelInstance,
                    scaleToUnits = if (compact) 1.0f else 0.90f,
                    centerOrigin = Position(0f, -1.0f, 0f),
                    autoAnimate = false,
                    animationName = null,
                    animationLoop = cue.loop,
                    position = Position(0f, -0.28f, 0f),
                    rotation = Rotation(y = 0f),
                    apply = {
                        liveModelNode = this
                    },
                )
            }

            if (!compact) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = maxHeight * 0.218f)
                        .width(maxWidth * 0.30f)
                        .height(maxHeight * 0.018f)
                        .background(Color.Black.copy(alpha = 0.055f), CircleShape),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = maxHeight * 0.216f)
                        .width(maxWidth * 0.21f)
                        .height(maxHeight * 0.011f)
                        .background(Color.Black.copy(alpha = 0.080f), CircleShape),
                )
            }
        }
    }

    LaunchedEffect(liveModelNode, cue.clipName, cue.loop, cueVersion) {
        val node = liveModelNode ?: return@LaunchedEffect
        animationMixer.transition(
            animator = node.animator,
            animationName = cue.clipName,
            loop = cue.loop,
        )
    }
}
