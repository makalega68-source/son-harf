package com.sonharf.game.mascot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sonharf.game.SonHarfUiState
import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.OnlineGameBackend
import kotlinx.coroutines.delay
import java.time.Instant

private val rejectionEvents = setOf(
    "word_already_used",
    "wrong_start_letter",
    "not_in_dictionary",
    "invalid_word",
    "ends_with_soft_g",
    "turn_expired",
)

private fun secondsUntil(deadline: String?): Int {
    if (deadline.isNullOrBlank()) return 0
    return runCatching {
        val remaining = Instant.parse(deadline).toEpochMilli() - System.currentTimeMillis()
        if (remaining <= 0L) 0 else ((remaining + 999L) / 1000L).toInt()
    }.getOrDefault(0)
}

/**
 * Read-only duel companion. Server state remains authoritative; this component
 * never submits a word or writes score/timer/inventory state.
 */
@Composable
fun ReactiveMageCatOverlay(roomId: String) {
    val backend = remember { OnlineGameBackend() }
    var snapshot by remember(roomId) { mutableStateOf<GameRoomDto?>(null) }
    var previous by remember(roomId) { mutableStateOf<GameRoomDto?>(null) }
    var seconds by remember(roomId) { mutableIntStateOf(0) }

    DisposableEffect(roomId) {
        MageCatDirector.onMatchStart()
        onDispose { MageCatDirector.resetToIdle() }
    }

    LaunchedEffect(roomId) {
        while (true) {
            val incoming = runCatching { backend.getRoom(roomId) }.getOrNull()
            if (incoming != null) {
                val me = backend.currentUserId()
                val old = snapshot
                previous = old
                snapshot = incoming

                val myScore = if (me == incoming.hostId) incoming.hostScore else incoming.guestScore
                val oldScore = old?.let { if (me == it.hostId) it.hostScore else it.guestScore } ?: myScore
                val myStreak = if (me == incoming.hostId) incoming.hostStreak else incoming.guestStreak

                when {
                    incoming.status == "finished" && incoming.winnerId == me -> MageCatDirector.onMatchVictory()
                    incoming.status == "finished" && incoming.winnerId != null -> MageCatDirector.onMatchDefeat()
                    incoming.lastEventPlayerId == me && incoming.lastEvent in rejectionEvents -> MageCatDirector.onWrongWordOrTimeout()
                    myScore > oldScore -> MageCatDirector.onCorrectWord(length = 0, streak = myStreak)
                }
            }
            delay(1_500)
        }
    }

    LaunchedEffect(snapshot?.turnDeadline, snapshot?.currentPlayerId, snapshot?.status) {
        while (true) {
            val current = snapshot
            seconds = secondsUntil(current?.turnDeadline)
            val me = backend.currentUserId()
            if (current != null && current.currentPlayerId == me && current.status in setOf("playing", "final", "sudden_death")) {
                MageCatDirector.onTimeUrgent(seconds)
            }
            if (seconds <= 0) break
            delay(250)
        }
    }

    val mood = MageCatDirector.currentMood
    val english = SonHarfUiState.language.lowercase() == "en"
    val message = when (mood) {
        MageCatMood.PANIC -> if (english) "Hurry!" else "Hızlı ol!"
        MageCatMood.ANGRY -> if (english) "Focus!" else "Odaklan!"
        MageCatMood.EXCITED -> if (english) "Great streak!" else "Harika seri!"
        MageCatMood.HAPPY -> if (english) "Nice word!" else "Güzel kelime!"
        MageCatMood.SAD -> if (english) "Next one." else "Sıradaki olur."
        MageCatMood.CRYING -> if (english) "Rematch?" else "Rövanş?"
        else -> null
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopStart,
    ) {
        MageCatCompanion(
            modifier = Modifier.padding(start = 8.dp, top = 92.dp),
            size = 58.dp,
            speechBubbleText = message,
        )
    }
}
