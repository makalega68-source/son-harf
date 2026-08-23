package com.sonharf.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.sceneview.SceneView
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader

/**
 * Real-time mascot renderer. This layer accepts GLB/glTF only.
 * It is production-gated until the asset contains a real quadruped skin and skeletal clips.
 */
@Composable
internal fun Mascot3DLayer(modifier: Modifier = Modifier) {
    if (!MascotPolicy.SKELETAL_ASSET_READY) return

    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val animation = MascotAnimationRegistry.definition(MascotRuntime.motion)

    SceneView(
        modifier = modifier,
        engine = engine,
        modelLoader = modelLoader,
        cameraManipulator = null,
    ) {
        rememberModelInstance(modelLoader, MascotPolicy.MODEL_ASSET)?.let { instance ->
            ModelNode(
                modelInstance = instance,
                scaleToUnits = 1f,
                autoAnimate = true,
                animationName = animation.clipName,
                animationLoop = animation.loop,
            )
        }
    }
}
