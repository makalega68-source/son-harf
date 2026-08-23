package com.sonharf.game

import android.content.Context
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.mascotdata2.MascotEmbeddedModel
import io.github.jan.supabase.postgrest.from
import io.github.sceneview.SceneView
import io.github.sceneview.SurfaceType
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import kotlinx.coroutines.delay

internal enum class MascotMotion { IDLE, GREETING, THINKING, CRITICAL, VICTORY, DEFEAT }

internal data class MascotAnimationDef(
    val id: String,
    val motion: MascotMotion,
    val unlockLevel: Int,
)

internal object MascotAnimationRegistry {
    val core = listOf(
        MascotAnimationDef("idle", MascotMotion.IDLE, 1),
        MascotAnimationDef("greeting", MascotMotion.GREETING, 1),
        MascotAnimationDef("thinking", MascotMotion.THINKING, 1),
        MascotAnimationDef("critical", MascotMotion.CRITICAL, 1),
        MascotAnimationDef("victory", MascotMotion.VICTORY, 1),
        MascotAnimationDef("defeat", MascotMotion.DEFEAT, 1),
        MascotAnimationDef("level_10_motion", MascotMotion.GREETING, 10),
        MascotAnimationDef("level_20_motion", MascotMotion.VICTORY, 20),
        MascotAnimationDef("level_30_motion", MascotMotion.THINKING, 30),
        MascotAnimationDef("level_40_motion", MascotMotion.IDLE, 40),
        MascotAnimationDef("level_50_motion", MascotMotion.VICTORY, 50),
    )

    fun nextUnlockLevel(level: Int): Int = ((level.coerceAtLeast(1) / 10) + 1) * 10
}

internal object MascotRuntime {
    var motion by mutableStateOf(MascotMotion.GREETING)
    var message by mutableStateOf("Buradayım. Hadi başlayalım!")
    var playerLevel by mutableIntStateOf(1)
    var playerXp by mutableIntStateOf(0)

    fun syncProgress(xp: Int, level: Int) {
        playerXp = xp.coerceAtLeast(0)
        playerLevel = level.coerceAtLeast(1)
    }

    fun react(motion: MascotMotion, language: String = SonHarfUiState.language) {
        this.motion = motion
        message = if (language == "en") when (motion) {
            MascotMotion.GREETING -> "I'm here. Let's play!"
            MascotMotion.IDLE -> "Level $playerLevel"
            MascotMotion.THINKING -> "I'm thinking about the best next move."
            MascotMotion.CRITICAL -> "Time is tight. Focus!"
            MascotMotion.VICTORY -> "We won! Great game."
            MascotMotion.DEFEAT -> "That was close."
        } else when (motion) {
            MascotMotion.GREETING -> "Buradayım. Hadi oynayalım!"
            MascotMotion.IDLE -> "Seviye $playerLevel"
            MascotMotion.THINKING -> "En iyi hamleyi düşünüyorum."
            MascotMotion.CRITICAL -> "Süre daralıyor. Odaklan!"
            MascotMotion.VICTORY -> "Kazandık! Harika oynadın."
            MascotMotion.DEFEAT -> "Çok yakındı."
        }
    }

    fun think(language: String = SonHarfUiState.language) = react(MascotMotion.THINKING, language)
}

private data class MascotAnchor(val x: Dp, val y: Dp)

private fun mascotName(context: Context): String =
    context.getSharedPreferences("son_harf_mascot", Context.MODE_PRIVATE)
        .getString("name", "Dostum") ?: "Dostum"

private fun setMascotName(context: Context, value: String) {
    context.getSharedPreferences("son_harf_mascot", Context.MODE_PRIVATE)
        .edit().putString("name", value.take(18)).apply()
}

@Composable
internal fun MascotFloatingOverlay(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val modelLocation = remember(context) {
        Uri.fromFile(MascotEmbeddedModel.ensureFile(context)).toString()
    }

    var lastReactionKey by remember { mutableStateOf("") }
    var roamingIndex by remember { mutableIntStateOf(0) }
    var isWalking by remember { mutableStateOf(false) }
    var walkingRight by remember { mutableStateOf(false) }
    var renameOpen by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(mascotName(context)) }
    var renameValue by remember { mutableStateOf(name) }
    val motion = MascotRuntime.motion

    LaunchedEffect(Unit) {
        MascotRuntime.react(MascotMotion.GREETING)
        delay(2200)
        MascotRuntime.react(MascotMotion.IDLE)
    }

    LaunchedEffect(motion) {
        if (motion == MascotMotion.IDLE) {
            while (true) {
                delay(3200)
                val next = (roamingIndex + 1) % 4
                walkingRight = next > roamingIndex || (roamingIndex == 3 && next == 0)
                isWalking = true
                roamingIndex = next
                delay(2350)
                isWalking = false
            }
        } else {
            isWalking = false
        }
    }

    LaunchedEffect(backend) {
        while (true) {
            runCatching {
                val b = backend ?: return@runCatching
                val me = b.currentUserId() ?: return@runCatching
                val growth = b.getGrowthDashboard()
                MascotRuntime.syncProgress(growth.xp, growth.level)
                val rooms = SupabaseProvider.client.from("game_rooms").select().decodeList<GameRoomDto>()
                val active = rooms
                    .filter {
                        (it.hostId == me || it.guestId == me) &&
                            it.status in listOf("waiting", "playing", "quiz", "final", "sudden_death", "paused", "finished")
                    }
                    .maxByOrNull { it.validWordCount }

                if (active == null) {
                    if (lastReactionKey != "idle-${growth.level}-${growth.xp}") {
                        lastReactionKey = "idle-${growth.level}-${growth.xp}"
                        MascotRuntime.react(MascotMotion.IDLE)
                    }
                    return@runCatching
                }

                val key = "${active.id}-${active.status}-${active.winnerId}-${active.finalMovesRemaining}-${active.validWordCount}"
                if (key != lastReactionKey) {
                    lastReactionKey = key
                    when {
                        active.status == "finished" && active.winnerId == me ->
                            MascotRuntime.react(MascotMotion.VICTORY, active.language)
                        active.status == "finished" && active.winnerId != null && active.winnerId != me ->
                            MascotRuntime.react(MascotMotion.DEFEAT, active.language)
                        active.status in listOf("final", "sudden_death") || active.finalMovesRemaining in 1..2 ->
                            MascotRuntime.react(MascotMotion.CRITICAL, active.language)
                        active.currentPlayerId == me && active.status in listOf("playing", "final", "sudden_death") ->
                            MascotRuntime.react(MascotMotion.THINKING, active.language)
                        else -> MascotRuntime.react(MascotMotion.IDLE, active.language)
                    }
                }
            }
            delay(1200)
        }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val mascotSize = if (maxWidth < 390.dp) 112.dp else 128.dp
        val side = 8.dp
        val leftX = side
        val midLeftX = ((maxWidth - mascotSize) * 0.34f).coerceAtLeast(side)
        val midRightX = ((maxWidth - mascotSize) * 0.66f).coerceAtLeast(side)
        val rightX = (maxWidth - mascotSize - side).coerceAtLeast(side)
        val maxFloor = (maxHeight - mascotSize - 92.dp).coerceAtLeast(190.dp)
        val floorY = (maxHeight * 0.76f).coerceAtMost(maxFloor)

        val anchors = listOf(
            MascotAnchor(leftX, floorY),
            MascotAnchor(midLeftX, floorY),
            MascotAnchor(midRightX, floorY),
            MascotAnchor(rightX, floorY),
        )

        val target = when (motion) {
            MascotMotion.GREETING -> MascotAnchor(rightX, floorY)
            MascotMotion.THINKING -> MascotAnchor(rightX, floorY)
            MascotMotion.CRITICAL -> MascotAnchor(leftX, floorY)
            MascotMotion.VICTORY -> MascotAnchor(midRightX, floorY)
            MascotMotion.DEFEAT -> MascotAnchor(midLeftX, floorY)
            MascotMotion.IDLE -> anchors[roamingIndex % anchors.size]
        }

        val x by animateDpAsState(
            targetValue = target.x,
            animationSpec = tween(durationMillis = if (isWalking) 2250 else 650, easing = LinearEasing),
            label = "mascot-3d-walk-x",
        )
        val y by animateDpAsState(
            targetValue = target.y,
            animationSpec = tween(durationMillis = 500),
            label = "mascot-3d-floor-y",
        )

        val targetYaw = when {
            motion != MascotMotion.IDLE -> 0f
            isWalking && walkingRight -> 90f
            isWalking && !walkingRight -> -90f
            else -> 0f
        }
        val yaw by animateFloatAsState(
            targetValue = targetYaw,
            animationSpec = tween(durationMillis = 320),
            label = "mascot-3d-yaw",
        )

        Box(
            modifier = Modifier
                .offset(x = x, y = y)
                .size(mascotSize)
                .clickable {
                    renameValue = name
                    renameOpen = true
                },
        ) {
            RealMascotScene(
                modelLocation = modelLocation,
                motion = motion,
                isWalking = isWalking,
                yaw = yaw,
            )
        }
    }

    if (renameOpen) {
        AlertDialog(
            onDismissRequest = { renameOpen = false },
            title = { Text(if (SonHarfUiState.language == "en") "Mascot name" else "Maskotunun adı") },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it.take(18) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val clean = renameValue.trim().ifBlank {
                        if (SonHarfUiState.language == "en") "Buddy" else "Dostum"
                    }
                    setMascotName(context, clean)
                    name = clean
                    renameOpen = false
                }) {
                    Text(if (SonHarfUiState.language == "en") "Save" else "Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameOpen = false }) {
                    Text(if (SonHarfUiState.language == "en") "Cancel" else "Vazgeç")
                }
            },
        )
    }
}

@Composable
private fun RealMascotScene(
    modelLocation: String,
    motion: MascotMotion,
    isWalking: Boolean,
    yaw: Float,
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val cameraNode = rememberCameraNode(engine) {
        position = Position(x = 0f, y = 0.25f, z = 3.2f)
    }
    val modelInstance = rememberModelInstance(
        modelLoader = modelLoader,
        fileLocation = modelLocation,
    )
    val animationName = when {
        motion == MascotMotion.VICTORY -> "Victory"
        isWalking -> "Walk"
        else -> "Idle"
    }

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
        modelInstance?.let { instance ->
            ModelNode(
                modelInstance = instance,
                autoAnimate = false,
                animationName = animationName,
                animationLoop = true,
                animationSpeed = if (isWalking) 1.15f else 1f,
                scaleToUnits = 1.15f,
                centerOrigin = Position(x = 0f, y = -1f, z = 0f),
                rotation = Rotation(x = 0f, y = yaw, z = 0f),
                isEditable = false,
            )
        }
    }
}
