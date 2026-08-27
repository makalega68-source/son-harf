package com.sonharf.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

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
    val all = MascotMotion.entries.map { motion ->
        MascotAnimationDef(
            id = motion.name.lowercase(),
            motion = motion,
            clipName = MascotCatalog.clip(MascotCatalog.DEFAULT_ID, motion),
            unlockLevel = 1,
            loop = motion !in setOf(MascotMotion.VICTORY, MascotMotion.DEFEAT, MascotMotion.CRITICAL),
        )
    }

    fun definition(motion: MascotMotion): MascotAnimationDef = all.first { it.motion == motion }
    fun unlocked(level: Int): List<MascotAnimationDef> = all.filter { level.coerceAtLeast(1) >= it.unlockLevel }
    fun nextUnlockLevel(level: Int): Int = ((level.coerceAtLeast(1) / 10) + 1) * 10
}

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

    fun rename(value: String) {
        petName = value.trim().take(18).ifBlank { "Dostum" }
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
    }

    private fun localizedMessage(motion: MascotMotion, language: String): String {
        val en = language == "en"
        return when (motion) {
            MascotMotion.GREETING -> if (en) "Ready!" else "Hazırım!"
            MascotMotion.THINKING -> if (en) "Let me think…" else "Bir düşüneyim…"
            MascotMotion.CRITICAL -> if (en) "Focus!" else "Odaklan!"
            MascotMotion.VICTORY -> if (en) "Great game!" else "Harika oynadın!"
            MascotMotion.DEFEAT -> if (en) "Next one." else "Sıradaki bizim."
            MascotMotion.LOOK_AT_PLAYER -> if (en) "I'm listening." else "Seni dinliyorum."
            MascotMotion.SIT -> if (en) "Resting." else "Dinleniyorum."
            MascotMotion.RUN -> if (en) "Let's go!" else "Hadi!"
            MascotMotion.IDLE,
            MascotMotion.WALK,
            MascotMotion.TURN_LEFT,
            MascotMotion.TURN_RIGHT -> ""
        }
    }
}

@Composable
internal fun MascotBehaviorBridge() = Unit
