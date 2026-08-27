package com.sonharf.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import java.time.Instant
import kotlinx.coroutines.delay

internal enum class MascotMotion {
    IDLE,
    WALK,
    TURN_LEFT,
    TURN_RIGHT,
    LOOK_AT_PLAYER,
    GREETING,
    THINKING,
    CRITICAL,
    VICTORY,
    DEFEAT,
    SIT,
    RUN,
}
internal enum class MascotMatchEvent {
    WORD_CORRECT,
    STRONG_WORD,
    STREAK_3,
    TIME_LOW,
    OPPONENT_MISTAKE,
    MATCH_WIN,
    MATCH_LOSE,
}

internal data class MascotMatchReaction(
    val motion: MascotMotion,
    val trMessage: String,
    val enMessage: String,
    val durationMs: Long,
)

internal object MascotMatchReactionRegistry {
    fun definition(event: MascotMatchEvent): MascotMatchReaction = when (event) {
        MascotMatchEvent.WORD_CORRECT -> MascotMatchReaction(
            MascotMotion.GREETING, "Güzel!", "Nice!", 1100L,
        )
        MascotMatchEvent.STRONG_WORD -> MascotMatchReaction(
            MascotMotion.RUN, "Harika kelime!", "Great word!", 1400L,
        )
        MascotMatchEvent.STREAK_3 -> MascotMatchReaction(
            MascotMotion.VICTORY, "3'lü seri! Devam!", "3-word streak! Keep going!", 1650L,
        )
        MascotMatchEvent.TIME_LOW -> MascotMatchReaction(
            MascotMotion.CRITICAL, "Hızlan! Süre azalıyor.", "Hurry! Time is running out.", 1800L,
        )
        MascotMatchEvent.OPPONENT_MISTAKE -> MascotMatchReaction(
            MascotMotion.LOOK_AT_PLAYER, "Fırsat!", "Chance!", 1050L,
        )
        MascotMatchEvent.MATCH_WIN -> MascotMatchReaction(
            MascotMotion.VICTORY, "Kazandık! Harikasın!", "We won! Great job!", 2600L,
        )
        MascotMatchEvent.MATCH_LOSE -> MascotMatchReaction(
            MascotMotion.DEFEAT, "Tekrar deneriz.", "We'll try again.", 2300L,
        )
    }
}

internal data class MascotAnimationDef(
    val id: String,
    val motion: MascotMotion,
    val clipName: String,
    val unlockLevel: Int,
    val loop: Boolean,
)

/** Single source of truth for animation clip names and level gates. */
internal object MascotAnimationRegistry {
    val all = listOf(
        MascotAnimationDef("idle", MascotMotion.IDLE, "Idle", 1, true),
        MascotAnimationDef("walk", MascotMotion.WALK, "Walk", 1, true),
        MascotAnimationDef("turn_left", MascotMotion.TURN_LEFT, "Turn_Left", 1, false),
        MascotAnimationDef("turn_right", MascotMotion.TURN_RIGHT, "Turn_Right", 1, false),
        MascotAnimationDef("look_at_player", MascotMotion.LOOK_AT_PLAYER, "Look_At_Player", 1, true),
        MascotAnimationDef("greeting", MascotMotion.GREETING, "Greeting", 1, false),
        MascotAnimationDef("thinking", MascotMotion.THINKING, "Thinking", 1, true),
        MascotAnimationDef("critical", MascotMotion.CRITICAL, "Critical", 1, true),
        MascotAnimationDef("victory", MascotMotion.VICTORY, "Victory", 1, false),
        MascotAnimationDef("defeat", MascotMotion.DEFEAT, "Defeat", 1, false),
        MascotAnimationDef("sit", MascotMotion.SIT, "Sit", 10, true),
        MascotAnimationDef("run", MascotMotion.RUN, "Run", 20, true),
    )

    fun definition(motion: MascotMotion): MascotAnimationDef = all.first { it.motion == motion }

    fun unlocked(level: Int): List<MascotAnimationDef> =
        all.filter { level.coerceAtLeast(1) >= it.unlockLevel }

    fun nextUnlockLevel(level: Int): Int = ((level.coerceAtLeast(1) / 10) + 1) * 10
}

/**
 * Deterministic first-stage behavior engine. No API key and no generative AI in the APK.
 * Rendering is handled separately by Mascot3DLayer.
 */
internal object MascotRuntime {
    var motion by mutableStateOf(MascotMotion.IDLE)
        private set
    var message by mutableStateOf("")
        private set
    var petName by mutableStateOf("Dostum")
        private set
    var playerLevel by mutableIntStateOf(1)
        private set
    var playerXp by mutableIntStateOf(0)
        private set
    var inActiveMatch by mutableStateOf(false)
        private set
    var reactionNonce by mutableIntStateOf(0)
        private set
    var reactionDurationMs by mutableStateOf(0L)
        private set

    fun rename(value: String) {
        val clean = value.trim().take(18)
        if (clean.isNotBlank()) petName = clean
    }

    fun syncProgress(xp: Int, level: Int) {
        playerXp = xp.coerceAtLeast(0)
        playerLevel = level.coerceAtLeast(1)
    }

    fun setMatchActive(active: Boolean) {
        inActiveMatch = active
    }

    fun react(next: MascotMotion, language: String = SonHarfUiState.language) {
        val allowed = MascotAnimationRegistry.definition(next).unlockLevel <= playerLevel
        motion = if (allowed) next else MascotMotion.IDLE
        message = localizedMessage(motion, language)
    }
    fun reactMatch(event: MascotMatchEvent, language: String = SonHarfUiState.language) {
        val reaction = MascotMatchReactionRegistry.definition(event)
        val allowed = MascotAnimationRegistry.definition(reaction.motion).unlockLevel <= playerLevel
        motion = if (allowed) reaction.motion else MascotMotion.IDLE
        val en = language == "en"
        message = if (en) reaction.enMessage else reaction.trMessage
        reactionDurationMs = reaction.durationMs
        reactionNonce += 1
    }

    fun clearTransientReaction(expectedNonce: Int) {
        if (expectedNonce != reactionNonce) return
        reactionDurationMs = 0L
        motion = MascotMotion.IDLE
        message = ""
    }

    private fun localizedMessage(motion: MascotMotion, language: String): String {
        val en = language == "en"
        return when (motion) {
            MascotMotion.GREETING -> if (en) "I'm here. Let's play!" else "Buradayım. Hadi oynayalım!"
            MascotMotion.THINKING -> if (en) "I'm thinking." else "Düşünüyorum."
            MascotMotion.CRITICAL -> if (en) "Time is tight. Focus!" else "Süre daralıyor. Odaklan!"
            MascotMotion.VICTORY -> if (en) "Great game!" else "Harika oynadın!"
            MascotMotion.DEFEAT -> if (en) "That was close." else "Çok yakındı."
            MascotMotion.LOOK_AT_PLAYER -> if (en) "Ready?" else "Hazır mısın?"
            MascotMotion.SIT -> if (en) "Taking a short rest." else "Biraz dinleniyorum."
            MascotMotion.RUN -> if (en) "Let's go!" else "Hadi!"
            MascotMotion.IDLE,
            MascotMotion.WALK,
            MascotMotion.TURN_LEFT,
            MascotMotion.TURN_RIGHT -> ""
        }
    }
}

/**
 * Read-only game-state bridge for the mascot. It restores the useful behavior logic from the old
 * overlay without restoring any bitmap/video rendering. Failures are deliberately non-fatal so
 * the mascot can never break gameplay or networking.
 */
@Composable
internal fun MascotBehaviorBridge() {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var lastReactionKey by remember { mutableStateOf("") }

    LaunchedEffect(backend) {
        var previousRoom: GameRoomDto? = null
        while (true) {
            runCatching {
                val b = backend ?: run {
                    MascotRuntime.setMatchActive(false)
                    previousRoom = null
                    return@runCatching
                }
                val me = b.currentUserId() ?: run {
                    MascotRuntime.setMatchActive(false)
                    previousRoom = null
                    return@runCatching
                }

                val growth = b.getGrowthDashboard()
                MascotRuntime.syncProgress(growth.xp, growth.level)

                val active = SupabaseProvider.client
                    .from("game_rooms")
                    .select()
                    .decodeList<GameRoomDto>()
                    .filter {
                        (it.hostId == me || it.guestId == me) &&
                            it.status in listOf("waiting", "playing", "quiz", "final", "sudden_death", "paused", "finished")
                    }
                    .maxByOrNull { it.validWordCount }

                if (active == null) {
                    MascotRuntime.setMatchActive(false)
                    previousRoom = null
                    val idleKey = "idle-${growth.level}-${growth.xp}"
                    if (lastReactionKey != idleKey) {
                        lastReactionKey = idleKey
                        MascotRuntime.react(MascotMotion.IDLE)
                    }
                    return@runCatching
                }

                MascotRuntime.setMatchActive(active.status != "finished")

                val host = active.hostId == me
                val myScore = if (host) active.hostScore else active.guestScore
                val myStreak = if (host) active.hostStreak else active.guestStreak
                val previousSameRoom = previousRoom?.takeIf { it.id == active.id }
                val previousMyScore = previousSameRoom?.let { if (host) it.hostScore else it.guestScore } ?: myScore
                val previousMyStreak = previousSameRoom?.let { if (host) it.hostStreak else it.guestStreak } ?: myStreak
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
                    active.lastEvent !in failedEvents &&
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

                val reactionKey: String
                val reaction: () -> Unit

                when {
                    active.status == "finished" && active.winnerId == me -> {
                        reactionKey = "result-${active.id}-win-${active.winnerId}"
                        reaction = { MascotRuntime.reactMatch(MascotMatchEvent.MATCH_WIN, active.language) }
                    }
                    active.status == "finished" && active.winnerId != null && active.winnerId != me -> {
                        reactionKey = "result-${active.id}-lose-${active.winnerId}"
                        reaction = { MascotRuntime.reactMatch(MascotMatchEvent.MATCH_LOSE, active.language) }
                    }
                    active.currentPlayerId == me &&
                        active.status in listOf("playing", "final", "sudden_death") &&
                        secondsLeft != null &&
                        secondsLeft in 1..10 -> {
                        reactionKey = "time-low-${active.id}-${active.turnDeadline}"
                        reaction = { MascotRuntime.reactMatch(MascotMatchEvent.TIME_LOW, active.language) }
                    }
                    myAcceptedWord && myStreak == 3 && previousMyStreak < 3 -> {
                        reactionKey = "streak3-${active.id}-${active.validWordCount}"
                        reaction = { MascotRuntime.reactMatch(MascotMatchEvent.STREAK_3, active.language) }
                    }
                    myAcceptedWord && (
                        active.lastEvent == "streak_bonus" ||
                            myScore - previousMyScore >= 6
                        ) -> {
                        reactionKey = "strong-${active.id}-${active.validWordCount}"
                        reaction = { MascotRuntime.reactMatch(MascotMatchEvent.STRONG_WORD, active.language) }
                    }
                    myAcceptedWord -> {
                        reactionKey = "word-${active.id}-${active.validWordCount}"
                        reaction = { MascotRuntime.reactMatch(MascotMatchEvent.WORD_CORRECT, active.language) }
                    }
                    opponentFailed -> {
                        reactionKey = "opponent-fail-${active.id}-${active.lastEvent}-${active.currentPlayerId}"
                        reaction = { MascotRuntime.reactMatch(MascotMatchEvent.OPPONENT_MISTAKE, active.language) }
                    }
                    active.status in listOf("final", "sudden_death") || active.finalMovesRemaining in 1..2 -> {
                        reactionKey = "critical-${active.id}-${active.status}-${active.finalMovesRemaining}"
                        reaction = { MascotRuntime.react(MascotMotion.CRITICAL, active.language) }
                    }
                    active.currentPlayerId == me &&
                        active.status in listOf("playing", "final", "sudden_death") -> {
                        reactionKey = "thinking-${active.id}-${active.currentPlayerId}-${active.turnDeadline}"
                        reaction = { MascotRuntime.react(MascotMotion.THINKING, active.language) }
                    }
                    else -> {
                        reactionKey = "idle-${active.id}-${active.status}-${active.currentPlayerId}"
                        reaction = { MascotRuntime.react(MascotMotion.IDLE, active.language) }
                    }
                }

                if (reactionKey != lastReactionKey) {
                    lastReactionKey = reactionKey
                    reaction()
                }
                previousRoom = active
            }
            delay(1200)
        }
    }
}
