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

/**
 * Production live companion stage used by the active room and home dock.
 *
 * Guarantees:
 * - real Eve GLB only; no 2D fallback,
 * - SurfaceView-backed Vulkan path retained for IME stability,
 * - animationVersion restarts the same named clip deterministically,
 * - no extra SceneView geometry/material is injected into the Vulkan scene. Both the procedural
 *   contact shadow and a later primitive grounding experiment hit Filament's
 *   "Normalized format does not exist" abort on the software Vulkan validation backend,
 * - the non-compact room gets a Compose-only grounding pool. SurfaceType.Surface renders behind
 *   Compose layers, so this adds depth without touching Filament resources or the Vulkan scene,
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
        // Soft front-left key: preserves the bright companion look while giving the face, ears
        // and feather layers enough directional contrast to read as a 3D volume.
        intensity = 8_500f
        lightDirection = Direction(-0.45f, -0.75f, -0.48f)
    }
    val fillLightNode = rememberFillLightNode(engine) {
        // Opposite high fill keeps the shadow side readable without flattening the key light.
        intensity = 2_500f
        lightDirection = Direction(0.68f, -0.47f, 0.56f)
    }
    val cue = EveMascotRuntime.animation
    val cueVersion = EveMascotRuntime.animationVersion

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
                surfaceType = SurfaceType.Surface,
                isOpaque = false,
                engine = engine,
                modelLoader = modelLoader,
                mainLightNode = mainLightNode,
                fillLightNode = fillLightNode,
                cameraManipulator = null,
            ) {
                ModelNode(
                    modelInstance = modelInstance,
                    scaleToUnits = if (compact) 1.0f else 0.90f,
                    // Bottom-align the accepted GLB's AABB. The previous framing placed the feet
                    // at roughly -0.28 m for both sizes; making that relationship explicit keeps
                    // the model position stable across clips and future camera tuning.
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
                // Pure Compose visual grounding. Keep it just below the feet so it does not paint
                // over Eve despite SurfaceView living behind Compose layers. The two ellipses give
                // a soft-edged contact pool without RenderEffect/blur or any Filament material.
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
        node.playingAnimations.keys.toList().forEach { index ->
            node.stopAnimation(index)
        }
        node.playAnimation(
            animationName = cue.clipName,
            loop = cue.loop,
        )
    }
}
