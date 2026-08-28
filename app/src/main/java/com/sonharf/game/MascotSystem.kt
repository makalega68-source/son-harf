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
        val character = LetharaLore.characterForMascot(MascotSelectionRuntime.selectedId)
        return when (motion) {
            MascotMotion.GREETING -> if (en) "The seal stirs. I'm ready." else "Mühür kıpırdandı. Hazırım."
            MascotMotion.THINKING -> if (en) "Quiet... the Word Weave is moving." else "Sessiz... Söz Dokusu hareket ediyor."
            MascotMotion.CRITICAL -> if (en) "Hold the final letter!" else "Son harfi bırakma!"
            MascotMotion.VICTORY -> if (en) "A memory spark! The old stars saw that." else "Bir hafıza kıvılcımı! Eski yıldızlar bunu gördü."
            MascotMotion.DEFEAT -> if (en) "The weave bent, not broke." else "Doku büküldü, kırılmadı."
            MascotMotion.LOOK_AT_PLAYER -> if (en) "I'm listening, Remembrancer." else "Dinliyorum, Hatırlatıcı."
            MascotMotion.SIT -> if (en) "Even seals need a quiet moment." else "Mühürlerin bile sessiz bir ana ihtiyacı olur."
            MascotMotion.RUN -> if (en) "The path is open!" else "Yol açıldı!"
            MascotMotion.IDLE -> if (playerXp > 0 && playerXp % 90 == 0) {
                LetharaLore.randomWhisper(character, language, playerXp + playerLevel)
            } else ""
            MascotMotion.WALK,
            MascotMotion.TURN_LEFT,
            MascotMotion.TURN_RIGHT -> ""
        }
    }
}

@Composable
internal fun MascotBehaviorBridge() = Unit
