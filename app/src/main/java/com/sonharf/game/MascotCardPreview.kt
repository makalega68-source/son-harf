package com.sonharf.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.sceneview.SceneView
import io.github.sceneview.SurfaceType
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader

/**
 * Fixed, front-facing render of the production white cat mascot for compact home cards.
 * Uses the same rigged GLB as the live mascot; no bitmap/video substitute is allowed.
 */
@Composable
internal fun MascotCardPreview(modifier: Modifier = Modifier) {
    if (!MascotPolicy.SKELETAL_ASSET_READY) return

    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, MascotPolicy.MODEL_ASSET)
    val idle = MascotAnimationRegistry.definition(MascotMotion.IDLE)

    SceneView(
        modifier = modifier,
        surfaceType = SurfaceType.TextureSurface,
        isOpaque = false,
        engine = engine,
        modelLoader = modelLoader,
        cameraManipulator = null,
    ) {
        modelInstance?.let { instance ->
            ModelNode(
                modelInstance = instance,
                scaleToUnits = 1.12f,
                centerOrigin = Position(0f, -0.82f, 0f),
                autoAnimate = false,
                animationName = idle.clipName,
                animationLoop = true,
                position = Position(0f, -0.18f, 0f),
                rotation = Rotation(y = 0f),
            )
        }
    }
}
