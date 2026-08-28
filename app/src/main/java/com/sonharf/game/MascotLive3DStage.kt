package com.sonharf.game

import android.content.pm.PackageManager
import android.util.Log
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.filament.Engine
import io.github.sceneview.SceneView
import io.github.sceneview.SurfaceType
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader

@Composable
internal fun MascotLive3DStage(
    modifier: Modifier = Modifier,
    mascotId: String = MascotSelectionRuntime.selectedId,
    motion: MascotMotion = MascotRuntime.motion,
    displayScale: Float = 1f,
    appearanceTint: Color? = null,
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { MascotSelectionRuntime.load(context) }

    val resolvedId = MascotCatalog.item(mascotId).id
    val effectiveTint = appearanceTint
    val modelLocation = remember(resolvedId) {
        MascotCatalog.modelLocation(context, resolvedId)
    }
    val vulkanSupported = remember(context) {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL)
    }

    if (!vulkanSupported) {
        Surface(
            modifier = modifier.padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF102A43),
            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (SonHarfUiState.isEnglish) "3D mascot is not supported on this device" else "Bu cihaz 3D maskotu desteklemiyor",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }
        return
    }

    if (modelLocation == null) {
        Surface(
            modifier = modifier.padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF102A43),
            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (SonHarfUiState.isEnglish) "Mascot asset is not ready" else "Maskot dosyası hazır değil",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }
        return
    }

    val engine = rememberEngine(
        engineCreator = { _ -> Engine.create(Engine.Backend.VULKAN) },
    )
    val modelLoader = rememberModelLoader(engine)
    val cameraNode = rememberCameraNode(engine) {
        position = Position(x = 0f, y = 0.12f, z = 3.0f)
    }
    val modelInstance = rememberModelInstance(
        modelLoader = modelLoader,
        fileLocation = modelLocation,
    )
    val clip = MascotCatalog.clip(resolvedId, motion)

    LaunchedEffect(modelInstance, effectiveTint) {
        val instance = modelInstance ?: return@LaunchedEffect
        if (effectiveTint != null) {
            var applied = 0
            instance.materialInstances.forEach { material ->
                runCatching {
                    material.setParameter(
                        "baseColorFactor",
                        effectiveTint.red,
                        effectiveTint.green,
                        effectiveTint.blue,
                        effectiveTint.alpha,
                    )
                    applied += 1
                }
            }
            Log.i("MascotSmoke", "MASCOT_TINT_APPLIED id=$resolvedId materials=$applied")
        }
    }

    LaunchedEffect(modelInstance, resolvedId, clip) {
        if (modelInstance != null) {
            kotlinx.coroutines.delay(1_200)
            Log.i("MascotSmoke", "MASCOT_RENDER_READY id=$resolvedId clip=$clip")
        }
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        if (modelInstance == null) {
            CircularProgressIndicator(color = SonHarfCyan, strokeWidth = 2.dp)
        } else {
            SceneView(
                modifier = Modifier.fillMaxSize(),
                surfaceType = SurfaceType.TextureSurface,
                engine = engine,
                modelLoader = modelLoader,
                isOpaque = false,
                autoCenterContent = true,
                autoFitContent = false,
                cameraNode = cameraNode,
                cameraManipulator = null,
            ) {
                ModelNode(
                    modelInstance = modelInstance,
                    autoAnimate = false,
                    animationName = clip,
                    animationLoop = MascotMotionPolicy.loops(motion),
                    animationSpeed = when (motion) {
                        MascotMotion.RUN -> 1.15f
                        MascotMotion.CRITICAL -> 1.08f
                        else -> 1f
                    },
                    scaleToUnits = (1.0f * displayScale).coerceIn(0.75f, 2.2f),
                    centerOrigin = Position(x = 0f, y = 0f, z = 0f),
                    rotation = Rotation(x = 0f, y = 0f, z = 0f),
                    isEditable = false,
                )
            }
        }
    }
}
