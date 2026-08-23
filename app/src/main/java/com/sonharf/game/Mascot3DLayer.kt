package com.sonharf.game

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.sceneview.SceneView
import io.github.sceneview.SurfaceType
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import kotlinx.coroutines.delay

/**
 * Real-time mascot renderer. GLB/glTF + skeletal animation only.
 *
 * The SceneView is intentionally limited to the mascot's own small moving viewport instead of a
 * full-screen touch surface. This keeps navigation, score, timer, word input and primary buttons
 * outside the 3D renderer's hit area. The viewport uses TextureView + transparent clear so the pet
 * appears directly on top of the Compose UI without a rectangular background.
 */
@Composable
internal fun Mascot3DLayer(modifier: Modifier = Modifier) {
    if (!MascotPolicy.SKELETAL_ASSET_READY) return

    var roamingIndex by remember { mutableIntStateOf(0) }
    var isWalking by remember { mutableStateOf(false) }
    var facingRight by remember { mutableStateOf(true) }
    val requestedMotion = MascotRuntime.motion

    LaunchedEffect(requestedMotion) {
        if (requestedMotion != MascotMotion.IDLE) {
            isWalking = false
            return@LaunchedEffect
        }
        while (true) {
            delay(3000)
            val next = (roamingIndex + 1) % 3
            facingRight = next > roamingIndex || (roamingIndex == 2 && next == 0)
            isWalking = true
            roamingIndex = next
            delay(2200)
            isWalking = false
        }
    }

    val effectiveMotion = if (requestedMotion == MascotMotion.IDLE && isWalking) {
        MascotMotion.WALK
    } else {
        requestedMotion
    }
    val animation = MascotAnimationRegistry.definition(effectiveMotion)

    BoxWithConstraints(modifier.fillMaxSize()) {
        val viewport = if (maxWidth < 390.dp) 112.dp else 132.dp
        val side = 10.dp
        val left = side
        val middle = ((maxWidth - viewport) / 2).coerceAtLeast(side)
        val right = (maxWidth - viewport - side).coerceAtLeast(side)
        // Keep a permanent exclusion zone above bottom navigation and gameplay input controls.
        val floor = (maxHeight - viewport - 126.dp).coerceAtLeast(180.dp)
        val targetX = when (roamingIndex) {
            0 -> left
            1 -> middle
            else -> right
        }
        val targetY = when (requestedMotion) {
            MascotMotion.THINKING,
            MascotMotion.CRITICAL -> (floor - 72.dp).coerceAtLeast(150.dp)
            else -> floor
        }

        val x by animateDpAsState(
            targetValue = targetX,
            animationSpec = tween(durationMillis = 2100, easing = LinearEasing),
            label = "mascot-3d-x",
        )
        val y by animateDpAsState(
            targetValue = targetY,
            animationSpec = tween(durationMillis = 650),
            label = "mascot-3d-y",
        )

        val engine = rememberEngine()
        val modelLoader = rememberModelLoader(engine)
        val modelInstance = rememberModelInstance(modelLoader, MascotPolicy.MODEL_ASSET)

        SceneView(
            modifier = Modifier.offset(x = x, y = y).size(viewport),
            surfaceType = SurfaceType.TextureSurface,
            isOpaque = false,
            engine = engine,
            modelLoader = modelLoader,
            cameraManipulator = null,
        ) {
            modelInstance?.let { instance ->
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 0.82f,
                    centerOrigin = Position(0f, -1f, 0f),
                    autoAnimate = false,
                    animationName = animation.clipName,
                    animationLoop = animation.loop,
                    position = Position(0f, -0.35f, 0f),
                    rotation = Rotation(y = if (facingRight) 90f else -90f),
                )
            }
        }
    }
}
