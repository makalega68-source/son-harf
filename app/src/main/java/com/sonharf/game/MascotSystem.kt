package com.sonharf.game

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

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

private const val MASCOT_REFERENCE_IMAGE =
    "https://d8j0ntlcm91z4.cloudfront.net/user_3IF9zXlHgFrus43xyiNWubj7Vka/hf_20260823_105103_a34c7b6a-3fef-4ac9-a9f2-d76040d05b0b.png"

private data class MascotAnchor(val x: Dp, val y: Dp)

private suspend fun loadTransparentMascot(): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val original = URL(MASCOT_REFERENCE_IMAGE).openStream().use(BitmapFactory::decodeStream)
            ?: return@runCatching null
        val bitmap = original.copy(Bitmap.Config.ARGB_8888, true)
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val c = pixels[i]
            val r = android.graphics.Color.red(c)
            val g = android.graphics.Color.green(c)
            val b = android.graphics.Color.blue(c)
            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            val spread = max - min
            val grayStudioBackground = spread < 42 && max > 118
            val veryLightNeutral = spread < 65 && max > 205
            if (grayStudioBackground || veryLightNeutral) {
                pixels[i] = c and 0x00FFFFFF
            }
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        bitmap
    }.getOrNull()
}

@Composable
internal fun MascotFloatingOverlay(modifier: Modifier = Modifier) {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var lastReactionKey by remember { mutableStateOf("") }
    var roamingIndex by remember { mutableIntStateOf(0) }
    val motion = MascotRuntime.motion
    val mascotBitmap by produceState<Bitmap?>(initialValue = null) {
        value = loadTransparentMascot()
    }

    LaunchedEffect(Unit) {
        MascotRuntime.react(MascotMotion.GREETING)
        delay(2600)
        MascotRuntime.react(MascotMotion.IDLE)
    }

    LaunchedEffect(motion) {
        if (motion == MascotMotion.IDLE) {
            while (true) {
                delay(3900)
                roamingIndex = (roamingIndex + 1) % 5
            }
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
                        active.status == "finished" && active.winnerId == me -> MascotRuntime.react(MascotMotion.VICTORY, active.language)
                        active.status == "finished" && active.winnerId != null && active.winnerId != me -> MascotRuntime.react(MascotMotion.DEFEAT, active.language)
                        active.status in listOf("final", "sudden_death") || active.finalMovesRemaining in 1..2 -> MascotRuntime.react(MascotMotion.CRITICAL, active.language)
                        active.currentPlayerId == me && active.status in listOf("playing", "final", "sudden_death") -> MascotRuntime.react(MascotMotion.THINKING, active.language)
                        else -> MascotRuntime.react(MascotMotion.IDLE, active.language)
                    }
                }
            }
            delay(1200)
        }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val size = if (maxWidth < 390.dp) 92.dp else 108.dp
        val side = 8.dp
        val leftX = side
        val centerX = ((maxWidth - size) / 2).coerceAtLeast(side)
        val rightX = (maxWidth - size - side).coerceAtLeast(side)
        val walkingY = (maxHeight - size - 96.dp).coerceAtLeast(150.dp)
        val upperY = (maxHeight * 0.60f).coerceAtMost(walkingY)

        val anchors = listOf(
            MascotAnchor(leftX, walkingY),
            MascotAnchor(centerX, walkingY),
            MascotAnchor(rightX, walkingY),
            MascotAnchor(centerX, upperY),
            MascotAnchor(leftX, walkingY),
        )

        val target = when (motion) {
            MascotMotion.GREETING -> MascotAnchor(rightX, walkingY)
            MascotMotion.THINKING -> MascotAnchor(rightX, upperY)
            MascotMotion.CRITICAL -> MascotAnchor(leftX, upperY)
            MascotMotion.VICTORY -> MascotAnchor(centerX, walkingY)
            MascotMotion.DEFEAT -> MascotAnchor(leftX, walkingY)
            MascotMotion.IDLE -> anchors[roamingIndex % anchors.size]
        }

        val previousIndex = (roamingIndex - 1 + anchors.size) % anchors.size
        val facingRight = if (motion == MascotMotion.IDLE) {
            target.x >= anchors[previousIndex].x
        } else {
            target.x >= centerX
        }

        val x by animateDpAsState(
            targetValue = target.x,
            animationSpec = tween(durationMillis = 2300, easing = LinearEasing),
            label = "mascot-walk-x",
        )
        val y by animateDpAsState(
            targetValue = target.y,
            animationSpec = tween(durationMillis = 900),
            label = "mascot-walk-y",
        )

        val step = rememberInfiniteTransition(label = "mascot-steps")
        val bob by step.animateFloat(
            initialValue = 0f,
            targetValue = -7f,
            animationSpec = infiniteRepeatable(
                animation = tween(170, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "mascot-step-bob",
        )
        val lean by step.animateFloat(
            initialValue = -1.5f,
            targetValue = 1.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(340, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "mascot-step-lean",
        )

        mascotBitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Son Harf mascot",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .offset(x = x, y = y)
                    .size(size)
                    .graphicsLayer {
                        scaleX = if (facingRight) 1f else -1f
                        translationY = if (motion == MascotMotion.IDLE) bob else bob * 0.45f
                        rotationZ = if (motion == MascotMotion.IDLE) lean else 0f
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.88f)
                    },
            )
        }
    }
}
