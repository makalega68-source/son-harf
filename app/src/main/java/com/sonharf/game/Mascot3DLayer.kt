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
import kotlin.math.abs
import kotlin.random.Random

/**
 * Real-time mascot renderer. GLB/glTF + skin/skeleton animation only.
 *
 * Menu behavior is intentionally calm: the pet spends most of its time idle or watching the
 * player and only occasionally makes a short move. Match behavior is driven by MascotRuntime.
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
    var roamingIndex by remember { mutableIntStateOf(1) }
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
            // A real pet should not patrol continuously. Long calm pauses are the default.
            delay(Random.nextLong(6500L, 15001L))

            when (Random.nextInt(100)) {
                in 0..39 -> {
                    locomotionMotion = MascotMotion.LOOK_AT_PLAYER
                    delay(Random.nextLong(1200L, 2601L))
                    locomotionMotion = MascotMotion.IDLE
                }

                in 40..59 -> {
                    // Do nothing visible for another short interval. This prevents clockwork motion.
                    locomotionMotion = MascotMotion.IDLE
                    delay(Random.nextLong(3000L, 7501L))
                }

                in 60..69 -> {
                    if (level >= 10) {
                        locomotionMotion = MascotMotion.SIT
                        delay(Random.nextLong(2600L, 5201L))
                    } else {
                        locomotionMotion = MascotMotion.LOOK_AT_PLAYER
                        delay(Random.nextLong(1000L, 2201L))
                    }
                    locomotionMotion = MascotMotion.IDLE
                }

                else -> {
                    val candidates = (0..2).filter { it != roamingIndex }
                    val next = candidates.random()
                    val movingRight = next > roamingIndex
                    val distance = abs(next - roamingIndex)

                    locomotionMotion = if (movingRight) MascotMotion.TURN_RIGHT else MascotMotion.TURN_LEFT
                    delay(Random.nextLong(550L, 951L))
                    facingRight = movingRight

                    // Running is a rare high-level flourish, never the normal roaming behavior.
                    val useRun = level >= 20 && distance > 1 && Random.nextInt(100) < 5
                    locomotionMotion = if (useRun) MascotMotion.RUN else MascotMotion.WALK
                    roamingIndex = next
                    delay(
                        if (useRun) Random.nextLong(1100L, 1501L)
                        else Random.nextLong(2100L, 3301L) + (distance - 1) * 500L,
                    )

                    if (Random.nextInt(100) < 35) {
                        locomotionMotion = MascotMotion.LOOK_AT_PLAYER
                        delay(Random.nextLong(1000L, 2301L))
                    }
                    locomotionMotion = MascotMotion.IDLE
                }
            }
        }
    }

    val effectiveMotion = if (requestedMotion == MascotMotion.IDLE && !inActiveMatch) {
        locomotionMotion
    } else {
        requestedMotion
    }
    val animation = MascotAnimationRegistry.definition(effectiveMotion)

    BoxWithConstraints(modifier.fillMaxSize()) {
        // The previous 112-132 dp viewport made the animal read like a tiny insect on real phones.
        val viewport = when {
            inActiveMatch && maxWidth < 390.dp -> 118.dp
            inActiveMatch -> 132.dp
            maxWidth < 390.dp -> 178.dp
            else -> 198.dp
        }
        val side = 8.dp
        val left = side
        val middle = ((maxWidth - viewport) / 2).coerceAtLeast(side)
        val right = (maxWidth - viewport - side).coerceAtLeast(side)
        val bottomExclusion = if (inActiveMatch) 178.dp else 132.dp
        val floor = (maxHeight - viewport - bottomExclusion).coerceAtLeast(150.dp)
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
            MascotMotion.CRITICAL -> (floor - 54.dp).coerceAtLeast(130.dp)
            else -> floor
        }

        val moveDuration = if (effectiveMotion == MascotMotion.RUN) 1250 else 2700
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

        val rotationY = when (effectiveMotion) {
            MascotMotion.LOOK_AT_PLAYER,
            MascotMotion.GREETING,
            MascotMotion.THINKING,
            MascotMotion.CRITICAL,
            MascotMotion.VICTORY,
            MascotMotion.DEFEAT,
            MascotMotion.SIT -> 0f
            else -> if (facingRight) 42f else -42f
        }

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
                    scaleToUnits = if (inActiveMatch) 0.98f else 1.22f,
                    centerOrigin = Position(0f, -0.82f, 0f),
                    autoAnimate = false,
                    animationName = animation.clipName,
                    animationLoop = animation.loop,
                    position = Position(0f, -0.18f, 0f),
                    rotation = Rotation(y = rotationY),
                )
            }
        }

        Surface(
            onClick = {
                renameDraft = MascotRuntime.petName
                renameOpen = true
                MascotRuntime.react(MascotMotion.LOOK_AT_PLAYER)
            },
            modifier = Modifier.offset(x = x, y = (y - 24.dp).coerceAtLeast(0.dp)),
            tonalElevation = 2.dp,
            shadowElevation = 2.dp,
        ) {
            Text(
                text = "${MascotRuntime.petName}  •  Lv $level",
                modifier = Modifier.offset(x = 8.dp).size(width = viewport - 16.dp, height = 22.dp),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }

        val message = MascotRuntime.message
        if (message.isNotBlank() && requestedMotion != MascotMotion.IDLE) {
            Surface(
                modifier = Modifier.offset(x = x, y = (y - 52.dp).coerceAtLeast(0.dp)),
                tonalElevation = 2.dp,
                shadowElevation = 2.dp,
            ) {
                Text(
                    text = message,
                    modifier = Modifier.size(width = viewport, height = 40.dp),
                    fontSize = 9.sp,
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
