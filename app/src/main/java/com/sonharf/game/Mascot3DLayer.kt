package com.sonharf.game

import android.content.Context
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * Real-time mascot renderer. GLB/glTF + skin/skeleton animation only.
 *
 * The SceneView is intentionally limited to a small moving viewport instead of a full-screen touch
 * surface. Navigation, score, timer, word input and primary buttons remain outside its hit area.
 */
@Composable
internal fun Mascot3DLayer(modifier: Modifier = Modifier) {
    if (!MascotPolicy.SKELETAL_ASSET_READY) return

    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences("son_harf_mascot", Context.MODE_PRIVATE)
    }
    var renameOpen by remember { mutableStateOf(false) }
    var renameDraft by remember { mutableStateOf("") }
    var roamingIndex by remember { mutableIntStateOf(0) }
    var roamCycle by remember { mutableIntStateOf(0) }
    var locomotionMotion by remember { mutableStateOf(MascotMotion.IDLE) }
    var facingRight by remember { mutableStateOf(true) }

    val requestedMotion = MascotRuntime.motion
    val inActiveMatch = MascotRuntime.inActiveMatch
    val level = MascotRuntime.playerLevel

    LaunchedEffect(Unit) {
        MascotRuntime.rename(preferences.getString("name", "Dostum") ?: "Dostum")
    }

    LaunchedEffect(requestedMotion, inActiveMatch, level) {
        locomotionMotion = MascotMotion.IDLE
        if (requestedMotion != MascotMotion.IDLE || inActiveMatch) return@LaunchedEffect

        while (true) {
            delay(3000)
            val next = (roamingIndex + 1) % 3
            val movingRight = next > roamingIndex
            locomotionMotion = if (movingRight) MascotMotion.TURN_RIGHT else MascotMotion.TURN_LEFT
            delay(450)
            facingRight = movingRight

            val useRun = level >= 20 && roamCycle % 5 == 4
            locomotionMotion = if (useRun) MascotMotion.RUN else MascotMotion.WALK
            roamingIndex = next
            delay(if (useRun) 1250 else 2200)

            locomotionMotion = MascotMotion.LOOK_AT_PLAYER
            delay(850)
            if (level >= 10 && roamCycle % 4 == 3) {
                locomotionMotion = MascotMotion.SIT
                delay(1500)
            }
            locomotionMotion = MascotMotion.IDLE
            roamCycle += 1
        }
    }

    val effectiveMotion = if (requestedMotion == MascotMotion.IDLE && !inActiveMatch) {
        locomotionMotion
    } else {
        requestedMotion
    }
    val animation = MascotAnimationRegistry.definition(effectiveMotion)

    BoxWithConstraints(modifier.fillMaxSize()) {
        val viewport = when {
            inActiveMatch && maxWidth < 390.dp -> 92.dp
            inActiveMatch -> 106.dp
            maxWidth < 390.dp -> 112.dp
            else -> 132.dp
        }
        val side = 10.dp
        val left = side
        val middle = ((maxWidth - viewport) / 2).coerceAtLeast(side)
        val right = (maxWidth - viewport - side).coerceAtLeast(side)
        val bottomExclusion = if (inActiveMatch) 170.dp else 126.dp
        val floor = (maxHeight - viewport - bottomExclusion).coerceAtLeast(180.dp)
        val targetX = if (inActiveMatch) {
            right
        } else {
            when (roamingIndex) {
                0 -> left
                1 -> middle
                else -> right
            }
        }
        val targetY = when (requestedMotion) {
            MascotMotion.THINKING,
            MascotMotion.CRITICAL -> (floor - 62.dp).coerceAtLeast(150.dp)
            else -> floor
        }

        val moveDuration = if (effectiveMotion == MascotMotion.RUN) 1150 else 2100
        val x by animateDpAsState(
            targetValue = targetX,
            animationSpec = tween(durationMillis = moveDuration, easing = LinearEasing),
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

        Surface(
            onClick = {
                renameDraft = MascotRuntime.petName
                renameOpen = true
                MascotRuntime.react(MascotMotion.LOOK_AT_PLAYER)
            },
            modifier = Modifier.offset(x = x, y = (y - 22.dp).coerceAtLeast(0.dp)),
            tonalElevation = 2.dp,
            shadowElevation = 2.dp,
        ) {
            Text(
                text = "${MascotRuntime.petName}  •  Lv $level",
                modifier = Modifier.offset(x = 6.dp).size(width = viewport - 12.dp, height = 20.dp),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }

        val message = MascotRuntime.message
        if (message.isNotBlank() && requestedMotion != MascotMotion.IDLE) {
            Surface(
                modifier = Modifier.offset(x = x, y = (y - 48.dp).coerceAtLeast(0.dp)),
                tonalElevation = 2.dp,
                shadowElevation = 2.dp,
            ) {
                Text(
                    text = message,
                    modifier = Modifier.size(width = viewport, height = 38.dp),
                    fontSize = 8.sp,
                    maxLines = 2,
                )
            }
        }
    }

    if (renameOpen) {
        AlertDialog(
            onDismissRequest = { renameOpen = false },
            title = { Text(sh("Maskotunun adı", "Mascot name")) },
            text = {
                OutlinedTextField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it.take(18) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    MascotRuntime.rename(renameDraft)
                    preferences.edit().putString("name", MascotRuntime.petName).apply()
                    renameOpen = false
                    MascotRuntime.react(MascotMotion.GREETING)
                }) { Text(sh("Kaydet", "Save")) }
            },
            dismissButton = {
                TextButton(onClick = { renameOpen = false }) { Text(sh("Vazgeç", "Cancel")) }
            },
        )
    }
}
