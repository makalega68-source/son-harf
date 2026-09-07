package com.sonharf.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay
import java.time.Instant

private val ACTIVE_STATUSES = setOf("playing", "final", "sudden_death", "paused")
private const val MAX_CONSECUTIVE_FAILURES_BEFORE_SOFT_WARNING = 5
private const val POLL_INTERVAL_ACTIVE_MS = 2_000L
private const val POLL_INTERVAL_DISCOVER_MS = 1_250L

private sealed class RoomWatchState {
    data object Discovering : RoomWatchState()
    data class Active(val roomId: String, val consecutiveFailures: Int = 0) : RoomWatchState()
    data object Idle : RoomWatchState()
}

/**
 * Keeps the verified V1 shell/lobby intact while preserving an active duel through
 * transient room/network failures. Server state remains authoritative: an active
 * room is cleared only after a successful server snapshot positively shows that
 * the player is no longer participating or the room is no longer active.
 */
@Composable
internal fun LiveDuelRuntimeShell(onSignedOut: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    var watchState by remember { mutableStateOf<RoomWatchState>(RoomWatchState.Discovering) }
    var hypeState by remember { mutableStateOf(HypeState()) }

    LaunchedEffect(Unit) {
        while (true) {
            val me = backend.currentUserId()

            if (me == null || !SupabaseProvider.configured) {
                watchState = RoomWatchState.Idle
            } else {
                when (val current = watchState) {
                    is RoomWatchState.Active -> {
                        val result = runCatching { backend.getRoom(current.roomId) }
                        watchState = if (result.isSuccess) {
                            val serverRoom = result.getOrNull()
                            when {
                                serverRoom == null -> current.copy(
                                    consecutiveFailures = current.consecutiveFailures + 1,
                                )

                                !userIsParticipant(serverRoom, me) || serverRoom.status !in ACTIVE_STATUSES -> {
                                    RoomWatchState.Discovering
                                }

                                else -> {
                                    hypeState = hypeState.updateFrom(serverRoom, me)
                                    RoomWatchState.Active(serverRoom.id)
                                }
                            }
                        } else {
                            current.copy(consecutiveFailures = current.consecutiveFailures + 1)
                        }
                    }

                    RoomWatchState.Discovering,
                    RoomWatchState.Idle,
                    -> {
                        val discovered = discoverActiveRoom(backend, me)
                        watchState = if (discovered != null) {
                            hypeState = HypeState().updateFrom(discovered, me)
                            RoomWatchState.Active(discovered.id)
                        } else {
                            RoomWatchState.Idle
                        }
                    }
                }
            }

            delay(
                when (watchState) {
                    is RoomWatchState.Active -> POLL_INTERVAL_ACTIVE_MS
                    else -> POLL_INTERVAL_DISCOVER_MS
                },
            )
        }
    }

    when (val state = watchState) {
        is RoomWatchState.Active -> {
            Box(modifier = Modifier.fillMaxSize()) {
                RefinedDuelOverlay()
                BotTurnWatchdogOverlay()

                CompetitiveHypeOverlay(
                    hype = hypeState,
                    modifier = Modifier.align(Alignment.TopCenter),
                )

                if (state.consecutiveFailures >= MAX_CONSECUTIVE_FAILURES_BEFORE_SOFT_WARNING) {
                    ReconnectingBanner(modifier = Modifier.align(Alignment.TopCenter))
                }
            }
        }

        RoomWatchState.Discovering,
        RoomWatchState.Idle,
        -> MonsterExperienceApp(onSignedOut = onSignedOut)
    }
}

private suspend fun discoverActiveRoom(
    backend: OnlineGameBackend,
    userId: String,
): GameRoomDto? = runCatching {
    SupabaseProvider.client
        .from("game_rooms")
        .select()
        .decodeList<GameRoomDto>()
        .asSequence()
        .filter {
            userIsParticipant(it, userId) &&
                it.status in ACTIVE_STATUSES &&
                (it.isBot || it.guestId != null)
        }
        .maxByOrNull {
            runCatching { Instant.parse(it.createdAt) }.getOrDefault(Instant.EPOCH)
        }
}.getOrNull()

private fun userIsParticipant(room: GameRoomDto, userId: String): Boolean =
    room.hostId == userId || room.guestId == userId

private data class HypeState(
    val myScore: Int = 0,
    val opponentScore: Int = 0,
    val myStreak: Int = 0,
    val opponentStreak: Int = 0,
    val roundNo: Int = 0,
    val isSuddenDeath: Boolean = false,
) {
    fun updateFrom(room: GameRoomDto, userId: String): HypeState {
        val amHost = room.hostId == userId
        return HypeState(
            myScore = if (amHost) room.hostScore else room.guestScore,
            opponentScore = if (amHost) room.guestScore else room.hostScore,
            myStreak = if (amHost) room.hostStreak else room.guestStreak,
            opponentStreak = if (amHost) room.guestStreak else room.hostStreak,
            roundNo = room.roundNo,
            isSuddenDeath = room.status == "sudden_death",
        )
    }

    val scoreDelta: Int get() = myScore - opponentScore
    val isNeckAndNeck: Boolean get() = kotlin.math.abs(scoreDelta) <= 2
    val leadingMultiplier: Int
        get() = when {
            myStreak >= 5 -> 3
            myStreak >= 3 -> 2
            else -> 1
        }
}

@Composable
private fun CompetitiveHypeOverlay(hype: HypeState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF1A0033), Color(0xFF3D0066), Color(0xFF1A0033)),
                ),
            )
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScorePip(label = "SEN", value = hype.myScore, hot = hype.myStreak >= 3)
            Spacer(Modifier.width(14.dp))
            AnimatedVisibility(visible = hype.isNeckAndNeck) {
                Text(
                    "⚡ BAŞ BAŞA",
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                )
            }
            Spacer(Modifier.width(14.dp))
            ScorePip(label = "RAKİP", value = hype.opponentScore, hot = hype.opponentStreak >= 3)
        }

        if (hype.leadingMultiplier > 1) {
            Spacer(Modifier.height(4.dp))
            Text(
                "🔥 STREAK x${hype.leadingMultiplier}",
                color = Color(0xFFFF6B00),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
        }

        if (hype.isSuddenDeath) {
            Spacer(Modifier.height(4.dp))
            PulsingText("⚔ ANİ ÖLÜM", Color(0xFFFF1744))
        }
    }
}

@Composable
private fun ScorePip(label: String, value: Int, hot: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
        Text(
            value.toString(),
            color = if (hot) Color(0xFFFFD700) else Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
        )
    }
}

@Composable
private fun PulsingText(text: String, color: Color) {
    val infinite = rememberInfiniteTransition(label = "pulse")
    val alpha by infinite.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )
    Text(
        text,
        color = color.copy(alpha = alpha),
        fontWeight = FontWeight.Black,
        fontSize = 13.sp,
    )
}

@Composable
private fun ReconnectingBanner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xCC000000))
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            "Bağlantı zayıf — maçın korunuyor…",
            color = Color.White,
            fontSize = 11.sp,
        )
    }
}
