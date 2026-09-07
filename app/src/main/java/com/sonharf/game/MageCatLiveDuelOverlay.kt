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

/**
 * Aktif V1 düello ekranına Mage Cat'i bağlayan düşük riskli overlay.
 * Oyun mutasyonu yapmaz; yalnızca server-authoritative oda snapshot'larını okuyup
 * doğru kelime, seri ve maç sonucu olaylarını maskot cue'larına dönüştürür.
 */
@Composable
internal fun MageCatLiveDuelOverlay(roomId: String) {
    val backend = remember { OnlineGameBackend() }
    val runtime = remember(roomId) { MageCatRuntime() }
    var cue by remember(roomId) { mutableStateOf<MageCatCue?>(null) }
    var resultMode by remember(roomId) { mutableStateOf(false) }
    var initialized by remember(roomId) { mutableStateOf(false) }
    var lastValidWordCount by remember(roomId) { mutableStateOf(0) }
    var lastStatus by remember(roomId) { mutableStateOf<String?>(null) }

    LaunchedEffect(roomId) {
        runtime.onScreenChanged(MageCatScreen.MATCH)
        while (true) {
            val snapshot = runCatching { backend.getRoom(roomId) }.getOrNull()
            if (snapshot != null) {
                val me = backend.currentUserId()
                if (!initialized) {
                    initialized = true
                    lastValidWordCount = snapshot.validWordCount
                    lastStatus = snapshot.status
                } else {
                    val newAcceptedWord = snapshot.validWordCount > lastValidWordCount && snapshot.lastEventPlayerId == me
                    if (newAcceptedWord) {
                        val streak = if (me == snapshot.hostId) snapshot.hostStreak else snapshot.guestStreak
                        val event = MageCatGameEventMapper.acceptedWord(isMine = true, streak = streak)
                        if (event != null) {
                            cue = runtime.onEvent(
                                event = event,
                                nowMs = SystemClock.elapsedRealtime(),
                                playerInputActive = false,
                                matchFinished = false,
                            ).cue
                        }
                    }

                    if (snapshot.status == "finished" && lastStatus != "finished") {
                        resultMode = true
                        runtime.onScreenChanged(MageCatScreen.RESULT)
                        val event = MageCatGameEventMapper.matchResult(snapshot.winnerId, me)
                        cue = event?.let {
                            runtime.onEvent(
                                event = it,
                                nowMs = SystemClock.elapsedRealtime(),
                                matchFinished = true,
                            ).cue
                        }
                    }

                    lastValidWordCount = snapshot.validWordCount
                    lastStatus = snapshot.status
                }
            }
            delay(700L)
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
                    .padding(end = 10.dp)
                    .size(124.dp),
            )
        } else {
            MageCatMatchMascot(
                cue = cue,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 112.dp, end = 10.dp)
                    .size(MageCatMatchDefaultSize),
            )
        }
    }
}
