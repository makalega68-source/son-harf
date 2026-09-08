package com.sonharf.game

import android.os.SystemClock
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sonharf.game.data.OnlineGameBackend
import kotlinx.coroutines.delay
import java.time.Instant

private val mageCatRejectedEvents = setOf(
    "word_already_used",
    "wrong_start_letter",
    "not_in_dictionary",
    "invalid_word",
    "ends_with_soft_g",
    "turn_expired",
)

@Composable
internal fun MageCatLiveDuelOverlay(roomId: String) {
    val backend = remember { OnlineGameBackend() }
    val runtime = remember(roomId) { MageCatRuntime() }
    var cue by remember(roomId) { mutableStateOf<MageCatCue?>(null) }
    var resultMode by remember(roomId) { mutableStateOf(false) }
    var initialized by remember(roomId) { mutableStateOf(false) }
    var lastValidWordCount by remember(roomId) { mutableStateOf(0) }
    var lastStatus by remember(roomId) { mutableStateOf<String?>(null) }
    var lastEventSignature by remember(roomId) { mutableStateOf<String?>(null) }
    var warnedDeadline by remember(roomId) { mutableStateOf<String?>(null) }

    LaunchedEffect(roomId) {
        runtime.onScreenChanged(MageCatScreen.MATCH)
        while (true) {
            val snapshot = runCatching { backend.getRoom(roomId) }.getOrNull()
            if (snapshot != null) {
                val me = backend.currentUserId()
                val eventSignature = listOf(
                    snapshot.lastEvent,
                    snapshot.lastEventPlayerId,
                    snapshot.validWordCount,
                    snapshot.hostScore,
                    snapshot.guestScore,
                    snapshot.currentPlayerId,
                    snapshot.turnDeadline,
                ).joinToString("|")

                if (!initialized) {
                    initialized = true
                    lastValidWordCount = snapshot.validWordCount
                    lastStatus = snapshot.status
                    lastEventSignature = eventSignature
                } else {
                    val newAcceptedWord = snapshot.validWordCount > lastValidWordCount && snapshot.lastEventPlayerId == me
                    if (newAcceptedWord) {
                        val streak = if (me == snapshot.hostId) snapshot.hostStreak else snapshot.guestStreak
                        MageCatGameEventMapper.acceptedWord(isMine = true, streak = streak)?.let { event ->
                            cue = runtime.onEvent(
                                event = event,
                                nowMs = SystemClock.elapsedRealtime(),
                                playerInputActive = false,
                                matchFinished = false,
                            ).cue
                        }
                    } else if (
                        eventSignature != lastEventSignature &&
                        snapshot.lastEventPlayerId == me &&
                        snapshot.lastEvent in mageCatRejectedEvents
                    ) {
                        MageCatGameEventMapper.rejectedWord(isMine = true)?.let { event ->
                            cue = runtime.onEvent(
                                event = event,
                                nowMs = SystemClock.elapsedRealtime(),
                                playerInputActive = true,
                                matchFinished = false,
                            ).cue
                        }
                    }

                    val deadline = snapshot.turnDeadline
                    if (
                        snapshot.currentPlayerId == me &&
                        snapshot.status in setOf("playing", "final", "sudden_death") &&
                        !deadline.isNullOrBlank() &&
                        warnedDeadline != deadline
                    ) {
                        val remainingSeconds = runCatching {
                            ((Instant.parse(deadline).toEpochMilli() - System.currentTimeMillis()) / 1000L).toInt()
                        }.getOrNull()
                        if (remainingSeconds != null && remainingSeconds in 1..5) {
                            warnedDeadline = deadline
                            MageCatGameEventMapper.timePressure(isMine = true)?.let { event ->
                                cue = runtime.onEvent(
                                    event = event,
                                    nowMs = SystemClock.elapsedRealtime(),
                                    playerInputActive = true,
                                    matchFinished = false,
                                ).cue
                            }
                        }
                    }

                    if (snapshot.status == "finished" && lastStatus != "finished") {
                        resultMode = true
                        runtime.onScreenChanged(MageCatScreen.RESULT)
                        MageCatGameEventMapper.matchResult(snapshot.winnerId, me)?.let { event ->
                            cue = runtime.onEvent(
                                event = event,
                                nowMs = SystemClock.elapsedRealtime(),
                                matchFinished = true,
                            ).cue
                        }
                    }

                    lastValidWordCount = snapshot.validWordCount
                    lastStatus = snapshot.status
                    lastEventSignature = eventSignature
                }
            }
            delay(650L)
        }
    }

    LaunchedEffect(cue) {
        val current = cue ?: return@LaunchedEffect
        delay(current.duration.inWholeMilliseconds)
        if (cue === current) cue = null
    }

    Box(Modifier.fillMaxSize()) {
        if (resultMode) {
            MageCatResultMascot(
                cue = cue,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .size(MageCatResultDefaultSize),
            )
        } else {
            MageCatMatchMascot(
                cue = cue,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 106.dp, end = 8.dp)
                    .size(MageCatMatchDefaultSize),
            )
        }
    }
}
