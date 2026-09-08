package com.sonharf.game.mascot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sonharf.game.SonHarfUiState
import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.findPremierActiveRoom
import com.sonharf.game.data.getRoom
import com.sonharf.game.data.isPremierFinished
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.delay

/**
 * Read-only mascot layer. It observes authoritative room snapshots only and never
 * submits words, mutates score/rating, claims timeout or advances the bot.
 */
@Composable
fun ReactiveMageCatOverlay() {
    if (!SupabaseProvider.configured || !SonHarfUiState.inMatch) return
    val backend = remember { OnlineGameBackend() }
    var room by remember { mutableStateOf<GameRoomDto?>(null) }
    var phrase by remember { mutableStateOf<String?>(null) }
    var lastEvent by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(SonHarfUiState.inMatch) {
        if (!SonHarfUiState.inMatch) return@LaunchedEffect
        MageCatDirector.onMatchStart()
        var activeId: String? = null
        while (SonHarfUiState.inMatch) {
            val next = runCatching {
                if (activeId == null) backend.findPremierActiveRoom()
                else backend.getRoom(requireNotNull(activeId))
            }.getOrNull()
            if (next != null) {
                activeId = next.id
                val previous = room
                room = next
                val myId = backend.currentUserId()
                val amHost = myId == next.hostId
                val myScore = if (amHost) next.hostScore else next.guestScore
                val previousScore = previous?.let { if (amHost) it.hostScore else it.guestScore } ?: myScore
                val streak = if (amHost) next.hostStreak else next.guestStreak
                val deadline = next.turnDeadline?.let { runCatching { Instant.parse(it) }.getOrNull() }
                val seconds = deadline?.let { Duration.between(Instant.now(), it).seconds.toInt().coerceAtLeast(0) }
                if (seconds != null) MageCatDirector.onTimeUrgent(seconds)

                if (next.lastEvent != lastEvent) {
                    lastEvent = next.lastEvent
                    when {
                        next.isPremierFinished() && next.winnerId == myId -> {
                            MageCatDirector.onMatchVictory()
                            phrase = if (SonHarfUiState.isEnglish) "Victory! ✨" else "Zafer! ✨"
                        }
                        next.isPremierFinished() -> {
                            MageCatDirector.onMatchDefeat()
                            phrase = if (SonHarfUiState.isEnglish) "Rematch?" else "Rövanş?"
                        }
                        myScore > previousScore -> {
                            MageCatDirector.onCorrectWord(5, streak)
                            phrase = if (streak >= 3) {
                                if (SonHarfUiState.isEnglish) "Great streak! 🔥" else "Harika seri! 🔥"
                            } else null
                        }
                        next.lastEvent in setOf("invalid_word", "not_in_dictionary", "wrong_start_letter", "turn_expired") -> {
                            MageCatDirector.onWrongWordOrTimeout()
                            phrase = if (SonHarfUiState.isEnglish) "Try again!" else "Tekrar dene!"
                        }
                    }
                }
            }
            delay(650)
        }
    }

    Box(
        Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 8.dp, vertical = 150.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        MageCatCompanion(
            size = 72.dp,
            speechBubbleText = phrase,
            onClick = { MageCatDirector.onLobbyGreet() },
        )
    }
}
