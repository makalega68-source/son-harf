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
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
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
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { MascotSelectionRuntime.load(context) }

    val resolvedId = MascotCatalog.item(mascotId).id
    val modelLocation = remember(resolvedId) {
        MascotCatalog.modelLocation(context, resolvedId)
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

    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val cameraNode = rememberCameraNode(engine) {
        position = Position(x = 0f, y = 0.25f, z = 3.2f)
    }
    val modelInstance = rememberModelInstance(
        modelLoader = modelLoader,
        fileLocation = modelLocation,
    )
    val clip = MascotCatalog.clip(resolvedId, motion)
    val staticPreview = BuildConfig.DEBUG && resolvedId == MascotCatalog.CHIBI_WIZARD_ID

    Box(modifier, contentAlignment = Alignment.Center) {
        if (modelInstance == null) {
            CircularProgressIndicator(color = SonHarfCyan, strokeWidth = 2.dp)
        } else {
            key(resolvedId, clip) {
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
                        autoAnimate = !staticPreview,
                        animationName = if (staticPreview) null else clip,
                        animationLoop = !staticPreview && motion !in setOf(
                            MascotMotion.VICTORY,
                            MascotMotion.DEFEAT,
                            MascotMotion.CRITICAL,
                        ),
                        animationSpeed = when (motion) {
                            MascotMotion.RUN -> 1.15f
                            MascotMotion.CRITICAL -> 1.08f
                            else -> 1f
                        },
                        scaleToUnits = if (resolvedId == MascotCatalog.DEFAULT_ID) 1.15f else 1.25f,
                        centerOrigin = Position(x = 0f, y = -1f, z = 0f),
                        rotation = Rotation(x = 0f, y = 0f, z = 0f),
                        isEditable = false,
                    )
                }
            }
        }
    }
}
