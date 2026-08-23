package com.sonharf.game

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

    fun definition(motion: MascotMotion): MascotAnimationDef =
        all.first { it.motion == motion }

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

    fun rename(value: String) {
        val clean = value.trim().take(18)
        if (clean.isNotBlank()) petName = clean
    }

    fun syncProgress(xp: Int, level: Int) {
        playerXp = xp.coerceAtLeast(0)
        playerLevel = level.coerceAtLeast(1)
    }

    fun react(next: MascotMotion, language: String = SonHarfUiState.language) {
        val allowed = MascotAnimationRegistry.definition(next).unlockLevel <= playerLevel
        motion = if (allowed) next else MascotMotion.IDLE
        message = localizedMessage(motion, language)
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
