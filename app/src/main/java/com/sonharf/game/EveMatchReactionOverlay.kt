package com.sonharf.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import java.time.Instant
import kotlinx.coroutines.delay

internal enum class EveMatchReactionKind {
    WORD_CORRECT,
    STRONG_WORD,
    STREAK_3,
    TIME_LOW,
    OPPONENT_MISTAKE,
    MATCH_WIN,
    MATCH_LOSE,
}

internal object EveMatchReactionRuntime {
    var activeMatch by mutableStateOf(false)
        private set
    var message by mutableStateOf("")
        private set
    var nonce by mutableIntStateOf(0)
        private set
    var durationMs by mutableLongStateOf(0L)
        private set

    fun setMatchActive(active: Boolean) {
        activeMatch = active
        if (!active) message = ""
    }

    fun show(kind: EveMatchReactionKind, language: String) {
        EveMascotRuntime.wakeForGameplay()
        val en = language == "en"
        when (kind) {
            EveMatchReactionKind.WORD_CORRECT -> {
                message = if (en) "Nice!" else "Güzel!"
                durationMs = 1_150L
                EveMascotRuntime.happyReaction(message)
            }
            EveMatchReactionKind.STRONG_WORD -> {
                message = if (en) "Great word!" else "Harika kelime!"
                durationMs = 1_450L
                EveMascotRuntime.giftReaction(message)
            }
            EveMatchReactionKind.STREAK_3 -> {
                message = if (en) "3-word streak! Keep going!" else "3'lü seri! Devam!"
                durationMs = 1_700L
                EveMascotRuntime.giftReaction(message)
            }
            EveMatchReactionKind.TIME_LOW -> {
                message = if (en) "Hurry! Time is running out." else "Hızlan! Süre azalıyor."
                durationMs = 1_900L
                EveMascotRuntime.play(
                    EveAnimationCue.IDLE_LOOK_AROUND,
                    bubble = message,
                    returnToIdleAfterMs = durationMs,
                )
            }
            EveMatchReactionKind.OPPONENT_MISTAKE -> {
                message = if (en) "Chance!" else "Fırsat!"
                durationMs = 1_100L
                EveMascotRuntime.petReaction(message)
            }
            EveMatchReactionKind.MATCH_WIN -> {
                message = if (en) "We won! Great job!" else "Kazandık! Harikasın!"
                durationMs = 2_700L
                EveMascotRuntime.giftReaction(message)
            }
            EveMatchReactionKind.MATCH_LOSE -> {
                message = if (en) "We'll try again." else "Tekrar deneriz."
                durationMs = 2_400L
                EveMascotRuntime.sadReaction(message)
            }
        }
        nonce += 1
    }

    fun clearMessage(expectedNonce: Int) {
        if (expectedNonce == nonce) message = ""
    }
}

/**
 * Read-only bridge from the authoritative room state to EVE reactions.
 *
 * It never writes game state, score, turn or networking data. A historical finished room is
 * ignored unless it is the exact room that was already being observed live, preventing old
 * results from replaying after app launch.
 */
@Composable
internal fun EveMatchReactionBridge() {
    val backend = androidx.compose.runtime.remember {
        if (SupabaseProvider.configured) OnlineGameBackend() else null
    }
    var lastReactionKey by androidx.compose.runtime.remember { mutableStateOf("") }

    LaunchedEffect(backend) {
        var previousRoom: GameRoomDto? = null

        while (true) {
            runCatching {
                val b = backend ?: run {
                    EveMatchReactionRuntime.setMatchActive(false)
                    previousRoom = null
                    return@runCatching
                }
                val me = b.currentUserId() ?: run {
                    EveMatchReactionRuntime.setMatchActive(false)
                    previousRoom = null
                    return@runCatching
                }

                val rooms = SupabaseProvider.client
                    .from("game_rooms")
                    .select()
                    .decodeList<GameRoomDto>()
                    .filter { it.hostId == me || it.guestId == me }

                val liveRoom = rooms
                    .filter {
                        it.status in listOf(
                            "waiting",
                            "playing",
                            "quiz",
                            "final",
                            "sudden_death",
                            "paused",
                        )
                    }
                    .maxByOrNull { it.validWordCount }

                val active = liveRoom ?: previousRoom?.let { previous ->
                    rooms.firstOrNull { it.id == previous.id && it.status == "finished" }
                }

                if (active == null) {
                    EveMatchReactionRuntime.setMatchActive(false)
                    previousRoom = null
                    lastReactionKey = ""
                    return@runCatching
                }

                val isPlayable = active.status in listOf(
                    "playing",
                    "quiz",
                    "final",
                    "sudden_death",
                    "paused",
                )
                EveMatchReactionRuntime.setMatchActive(isPlayable || active.status == "finished")

                val host = active.hostId == me
                val myScore = if (host) active.hostScore else active.guestScore
                val myStreak = if (host) active.hostStreak else active.guestStreak
                val previousSameRoom = previousRoom?.takeIf { it.id == active.id }
                val previousMyScore = previousSameRoom?.let {
                    if (host) it.hostScore else it.guestScore
                } ?: myScore
                val previousMyStreak = previousSameRoom?.let {
                    if (host) it.hostStreak else it.guestStreak
                } ?: myStreak

                val secondsLeft = active.turnDeadline?.let { deadline ->
                    runCatching {
                        (Instant.parse(deadline).epochSecond - Instant.now().epochSecond)
                            .toInt()
                            .coerceAtLeast(0)
                    }.getOrNull()
                }

                val failedEvents = setOf(
                    "word_already_used",
                    "wrong_start_letter",
                    "not_in_dictionary",
                    "invalid_word",
                    "turn_expired",
                )

                val wordAdvanced = previousSameRoom != null &&
                    active.validWordCount > previousSameRoom.validWordCount
                val myAcceptedWord = wordAdvanced &&
                    (active.lastEvent == null || active.lastEvent !in failedEvents) &&
                    (active.lastEventPlayerId == me || myScore > previousMyScore)

                val opponentFailed = previousSameRoom != null &&
                    active.lastEventPlayerId != null &&
                    active.lastEventPlayerId != me &&
                    active.lastEvent in failedEvents &&
                    (
                        active.lastEvent != previousSameRoom.lastEvent ||
                            active.lastEventPlayerId != previousSameRoom.lastEventPlayerId ||
                            active.currentPlayerId != previousSameRoom.currentPlayerId
                    )

                val reactionKey: String?
                val kind: EveMatchReactionKind?

                when {
                    active.status == "finished" && active.winnerId == me -> {
                        reactionKey = "result-${active.id}-win-${active.winnerId}"
                        kind = EveMatchReactionKind.MATCH_WIN
                    }
                    active.status == "finished" &&
                        active.winnerId != null &&
                        active.winnerId != me -> {
                        reactionKey = "result-${active.id}-lose-${active.winnerId}"
                        kind = EveMatchReactionKind.MATCH_LOSE
                    }
                    active.currentPlayerId == me &&
                        active.status in listOf("playing", "final", "sudden_death") &&
                        secondsLeft != null &&
                        secondsLeft in 1..10 -> {
                        reactionKey = "time-low-${active.id}-${active.turnDeadline}"
                        kind = EveMatchReactionKind.TIME_LOW
                    }
                    myAcceptedWord && myStreak == 3 && previousMyStreak < 3 -> {
                        reactionKey = "streak3-${active.id}-${active.validWordCount}"
                        kind = EveMatchReactionKind.STREAK_3
                    }
                    myAcceptedWord && (
                        active.lastEvent == "streak_bonus" ||
                            myScore - previousMyScore >= 6
                        ) -> {
                        reactionKey = "strong-${active.id}-${active.validWordCount}"
                        kind = EveMatchReactionKind.STRONG_WORD
                    }
                    myAcceptedWord -> {
                        reactionKey = "word-${active.id}-${active.validWordCount}"
                        kind = EveMatchReactionKind.WORD_CORRECT
                    }
                    opponentFailed -> {
                        reactionKey = "opponent-fail-${active.id}-${active.lastEvent}-${active.currentPlayerId}"
                        kind = EveMatchReactionKind.OPPONENT_MISTAKE
                    }
                    else -> {
                        reactionKey = null
                        kind = null
                    }
                }

                if (reactionKey != null && kind != null && reactionKey != lastReactionKey) {
                    lastReactionKey = reactionKey
                    EveMatchReactionRuntime.show(kind, active.language)
                }

                previousRoom = active
            }

            delay(900L)
        }
    }
}

/**
 * Compact, non-clickable match companion. The stage uses the same accepted rigged EVE GLB and the
 * same animation runtime as HOME/room; no face rig or 2D fallback is introduced.
 */
@Composable
internal fun EveMatchReactionOverlay() {
    if (!EveMatchReactionRuntime.activeMatch) return

    val context = LocalContext.current
    val nonce = EveMatchReactionRuntime.nonce
    val duration = EveMatchReactionRuntime.durationMs

    LaunchedEffect(nonce) {
        if (nonce <= 0) return@LaunchedEffect
        SonHarfPreferences.hapticTap(context)
        delay(duration)
        EveMatchReactionRuntime.clearMessage(nonce)
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 4.dp, bottom = 154.dp)
                .width(126.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val message = EveMatchReactionRuntime.message
            if (message.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xEEFFFFFF),
                    shadowElevation = 4.dp,
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                        color = Color(0xFF163D36),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(118.dp)
                    .height(126.dp),
            ) {
                EveLive3DStage(
                    modifier = Modifier.fillMaxSize(),
                    compact = true,
                )
            }
        }
    }
}
