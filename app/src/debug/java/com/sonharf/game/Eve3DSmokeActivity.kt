package com.sonharf.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader

/**
 * Debug-only visual smoke surface for CI/emulator verification of the accepted Eve GLB.
 * This experiment forces Filament's Vulkan backend to distinguish a SwiftShader OpenGL
 * uniform-limit failure from a model/runtime incompatibility. It is not part of release UI.
 */
class Eve3DSmokeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EveMascotRuntime.calm()
        setContent {
            MaterialTheme {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF073B32), Color(0xFF176B52), Color(0xFF0A4A3C)),
                            ),
                        ),
                ) {
                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        Text(
                            "REAL EVE 3D SMOKE · VULKAN",
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            EveAssetPolicy.MODEL_ASSET,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            color = Color.White.copy(alpha = .72f),
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                        )
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(620.dp)
                                .padding(top = 14.dp),
                            color = Color.White.copy(alpha = .08f),
                        ) {
                            EveVulkanSmokeStage(Modifier.fillMaxSize())
                        }
                    }
                    Text(
                        "Real GLB · Vulkan · No 2D fallback",
                        modifier = Modifier.align(Alignment.BottomCenter).padding(18.dp),
                        color = Color.White.copy(alpha = .65f),
                        fontSize = 10.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun EveVulkanSmokeStage(modifier: Modifier = Modifier) {
    val engine = rememberEngine(
        engineCreator = { Engine.create(Engine.Backend.VULKAN) },
    )
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, EveAssetPolicy.MODEL_ASSET)
    val cue = EveMascotRuntime.animation

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (modelInstance == null) {
            CircularProgressIndicator(color = Color.White)
        } else {
            SceneView(
                modifier = Modifier.fillMaxSize(),
                surfaceType = SurfaceType.TextureSurface,
                isOpaque = false,
                engine = engine,
                modelLoader = modelLoader,
                cameraManipulator = null,
            ) {
                ModelNode(
                    modelInstance = modelInstance,
                    scaleToUnits = 0.90f,
                    centerOrigin = Position(0f, -0.60f, 0f),
                    autoAnimate = false,
                    animationName = cue.clipName,
                    animationLoop = cue.loop,
                    position = Position(0f, -.10f, 0f),
                    rotation = Rotation(y = 0f),
                )
            }
        }
    }
}
