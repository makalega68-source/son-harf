package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
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
 * - no procedural Filament contact-shadow material: SceneView 4.31.0's contact-shadow material
 *   aborts on the Vulkan/SwiftShader path with "Normalized format does not exist",
 * - grounding uses only SceneView's standard transparent-colour material and primitive geometry.
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
    val materialLoader = rememberMaterialLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, EveAssetPolicy.MODEL_ASSET)
    val cue = EveMascotRuntime.animation
    val cueVersion = EveMascotRuntime.animationVersion

    // The previous contact-shadow material crashes the software Vulkan CI backend. These three
    // concentric, nearly-flat primitives use SceneView's ordinary transparent colour material,
    // which follows the same Filament material path used by other production geometry. The layers
    // form a soft ambient-occlusion-like pool without adding a custom shader/material asset.
    val groundOuter = remember(materialLoader) {
        materialLoader.createColorInstance(
            color = Color.Black.copy(alpha = 0.035f),
            metallic = 0f,
            roughness = 1f,
            reflectance = 0f,
        )
    }
    val groundMiddle = remember(materialLoader) {
        materialLoader.createColorInstance(
            color = Color.Black.copy(alpha = 0.050f),
            metallic = 0f,
            roughness = 1f,
            reflectance = 0f,
        )
    }
    val groundInner = remember(materialLoader) {
        materialLoader.createColorInstance(
            color = Color.Black.copy(alpha = 0.070f),
            metallic = 0f,
            roughness = 1f,
            reflectance = 0f,
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

    Box(modifier, contentAlignment = Alignment.Center) {
        if (modelInstance == null) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
        } else {
            SceneView(
                modifier = Modifier.fillMaxSize(),
                surfaceType = SurfaceType.Surface,
                isOpaque = false,
                engine = engine,
                modelLoader = modelLoader,
                materialLoader = materialLoader,
                cameraManipulator = null,
            ) {
                if (!compact) {
                    CylinderNode(
                        radius = 0.36f,
                        height = 0.003f,
                        sideCount = 64,
                        materialInstance = groundOuter,
                        position = Position(0f, -0.288f, 0.015f),
                        scale = Scale(x = 1.0f, y = 1.0f, z = 0.62f),
                        apply = {
                            isShadowCaster = false
                            isShadowReceiver = false
                            isTouchable = false
                            setCulling(false)
                        },
                    )
                    CylinderNode(
                        radius = 0.285f,
                        height = 0.003f,
                        sideCount = 64,
                        materialInstance = groundMiddle,
                        position = Position(0f, -0.286f, 0.015f),
                        scale = Scale(x = 1.0f, y = 1.0f, z = 0.62f),
                        apply = {
                            isShadowCaster = false
                            isShadowReceiver = false
                            isTouchable = false
                            setCulling(false)
                        },
                    )
                    CylinderNode(
                        radius = 0.205f,
                        height = 0.003f,
                        sideCount = 64,
                        materialInstance = groundInner,
                        position = Position(0f, -0.284f, 0.015f),
                        scale = Scale(x = 1.0f, y = 1.0f, z = 0.62f),
                        apply = {
                            isShadowCaster = false
                            isShadowReceiver = false
                            isTouchable = false
                            setCulling(false)
                        },
                    )
                }

                ModelNode(
                    modelInstance = modelInstance,
                    scaleToUnits = if (compact) 1.0f else 0.90f,
                    // Bottom-align the accepted GLB's AABB to a stable world-space ground plane.
                    // -0.28 m preserves the previous on-screen framing while giving the feet a
                    // deterministic floor reference for every idle/reaction clip.
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
