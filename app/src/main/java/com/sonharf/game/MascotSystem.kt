package com.sonharf.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Legacy mascot API retained so older screens still compile while the old pet implementation is gone.
 * Every motion is now translated to an Eve animation cue.
 */
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

internal data class MascotAnimationDef(
    val id: String,
    val motion: MascotMotion,
    val clipName: String,
    val unlockLevel: Int,
    val loop: Boolean,
)

internal object MascotAnimationRegistry {
    val all = listOf(
        MascotAnimationDef("idle", MascotMotion.IDLE, "IdleBreathe", 1, true),
        MascotAnimationDef("walk", MascotMotion.WALK, "Walk", 1, true),
        MascotAnimationDef("turn_left", MascotMotion.TURN_LEFT, "WalkTurnL", 1, false),
        MascotAnimationDef("turn_right", MascotMotion.TURN_RIGHT, "WalkTurnR", 1, false),
        MascotAnimationDef("look_at_player", MascotMotion.LOOK_AT_PLAYER, "IdleLookAround", 1, true),
        MascotAnimationDef("greeting", MascotMotion.GREETING, "IdleLookAround", 1, true),
        MascotAnimationDef("thinking", MascotMotion.THINKING, "IdleLookAround", 1, true),
        MascotAnimationDef("critical", MascotMotion.CRITICAL, "IdleBreathe", 1, true),
        MascotAnimationDef("victory", MascotMotion.VICTORY, "IdleBreathe", 1, true),
        MascotAnimationDef("defeat", MascotMotion.DEFEAT, "Rest", 1, true),
        MascotAnimationDef("sit", MascotMotion.SIT, "Rest", 1, true),
        MascotAnimationDef("run", MascotMotion.RUN, "Run", 1, true),
    )

    fun definition(motion: MascotMotion): MascotAnimationDef = all.first { it.motion == motion }

    fun unlocked(level: Int): List<MascotAnimationDef> =
        all.filter { level.coerceAtLeast(1) >= it.unlockLevel }

    fun nextUnlockLevel(level: Int): Int = ((level.coerceAtLeast(1) / 10) + 1) * 10
}

internal object MascotRuntime {
    var motion by mutableStateOf(MascotMotion.IDLE)
        private set
    var message by mutableStateOf("")
        private set
    var petName by mutableStateOf("Eve")
        private set
    var playerLevel by mutableIntStateOf(1)
        private set
    var playerXp by mutableIntStateOf(0)
        private set
    var inActiveMatch by mutableStateOf(false)
        private set

    fun rename(value: String) {
        // Eve is a named character, not a renameable generic pet. Kept as a no-op compatibility hook.
        petName = "Eve"
    }

    fun syncProgress(xp: Int, level: Int) {
        playerXp = xp.coerceAtLeast(0)
        playerLevel = level.coerceAtLeast(1)
    }

    fun setMatchActive(active: Boolean) {
        inActiveMatch = active
    }

    fun react(next: MascotMotion, language: String = SonHarfUiState.language) {
        motion = next
        message = localizedMessage(next, language)
        EveMascotRuntime.play(next.toEveCue(), message.takeIf { it.isNotBlank() })
    }

    private fun localizedMessage(motion: MascotMotion, language: String): String {
        val en = language == "en"
        return when (motion) {
            MascotMotion.GREETING -> if (en) "I'm here. 🤍" else "Buradayım. 🤍"
            MascotMotion.THINKING -> if (en) "Let me think…" else "Bir düşüneyim…"
            MascotMotion.CRITICAL -> if (en) "You've got this." else "Yapabilirsin."
            MascotMotion.VICTORY -> if (en) "That was lovely!" else "Harikaydın!"
            MascotMotion.DEFEAT -> if (en) "Stay with it; the next one is yours." else "Canını sıkma, sıradaki senin."
            MascotMotion.LOOK_AT_PLAYER -> if (en) "I'm listening." else "Seni dinliyorum."
            MascotMotion.SIT -> if (en) "A tiny rest." else "Biraz dinleniyorum."
            MascotMotion.RUN -> if (en) "Let's go!" else "Hadi!"
            MascotMotion.IDLE,
            MascotMotion.WALK,
            MascotMotion.TURN_LEFT,
            MascotMotion.TURN_RIGHT -> ""
        }
    }
}

private fun MascotMotion.toEveCue(): EveAnimationCue = when (this) {
    MascotMotion.IDLE -> EveAnimationCue.IDLE_BREATHE
    MascotMotion.WALK -> EveAnimationCue.WALK
    MascotMotion.TURN_LEFT -> EveAnimationCue.WALK
    MascotMotion.TURN_RIGHT -> EveAnimationCue.WALK
    MascotMotion.LOOK_AT_PLAYER,
    MascotMotion.GREETING,
    MascotMotion.THINKING -> EveAnimationCue.IDLE_LOOK_AROUND
    MascotMotion.CRITICAL,
    MascotMotion.VICTORY -> EveAnimationCue.IDLE_BREATHE
    MascotMotion.DEFEAT,
    MascotMotion.SIT -> EveAnimationCue.REST
    MascotMotion.RUN -> EveAnimationCue.RUN
}

/** Old autonomous-pet polling is intentionally removed. Eve's behavior is driven by chat/game context. */
@Composable
internal fun MascotBehaviorBridge() = Unit
