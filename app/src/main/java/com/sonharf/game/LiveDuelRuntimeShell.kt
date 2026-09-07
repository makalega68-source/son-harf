package com.sonharf.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
private const val POLL_INTERVAL_IDLE_MS = 5_000L

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
 *
 * LaunchedEffect is composition-scoped, so this polling coroutine is cancelled as
 * soon as the shell leaves composition. Idle discovery is intentionally slower to
 * avoid unnecessary radio/CPU wakeups outside a live match.
 */
@Composable
internal fun LiveDuelRuntimeShell(onSignedOut: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    var watchState by remember { mutableStateOf<RoomWatchState>(RoomWatchState.Discovering) }

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

                                else -> RoomWatchState.Active(serverRoom.id)
                            }
                        } else {
                            // Transient network/backend failure: preserve the active arena.
                            current.copy(consecutiveFailures = current.consecutiveFailures + 1)
                        }
                    }

                    RoomWatchState.Discovering,
                    RoomWatchState.Idle,
                    -> {
                        val discovered = discoverActiveRoom(backend, me)
                        watchState = if (discovered != null) {
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
                    RoomWatchState.Discovering -> POLL_INTERVAL_DISCOVER_MS
                    RoomWatchState.Idle -> POLL_INTERVAL_IDLE_MS
                },
            )
        }
    }

    when (val state = watchState) {
        is RoomWatchState.Active -> {
            Box(modifier = Modifier.fillMaxSize()) {
                RefinedDuelOverlay()
                BotTurnWatchdogOverlay(roomId = state.roomId)

                // Presentation-only warning. The match itself stays mounted and authoritative.
                if (state.consecutiveFailures >= MAX_CONSECUTIVE_FAILURES_BEFORE_SOFT_WARNING) {
                    ReconnectingBanner(modifier = Modifier.align(Alignment.TopCenter))
                }
            }
        }

        RoomWatchState.Discovering -> {
            // Never construct the lobby/banner while we are still checking for a live match.
            // This removes the short ad lifecycle window that previously existed on restore.
            Box(
                modifier = Modifier.fillMaxSize().background(MainUi.Background),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MainUi.Blue)
            }
        }

        RoomWatchState.Idle -> MonsterExperienceApp(onSignedOut = onSignedOut)
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
            sh("Bağlantı zayıf — maçın korunuyor…", "Weak connection — match preserved…"),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
