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

internal object MascotMotionPolicy {
    private val oneShotDurations = mapOf(
        MascotMotion.GREETING to 1_650L,
        MascotMotion.CRITICAL to 1_250L,
        MascotMotion.VICTORY to 2_050L,
        MascotMotion.DEFEAT to 1_850L,
    )

    private val priorities = mapOf(
        MascotMotion.IDLE to 0,
        MascotMotion.WALK to 10,
        MascotMotion.TURN_LEFT to 10,
        MascotMotion.TURN_RIGHT to 10,
        MascotMotion.SIT to 20,
        MascotMotion.LOOK_AT_PLAYER to 25,
        MascotMotion.THINKING to 30,
        MascotMotion.RUN to 35,
        MascotMotion.GREETING to 40,
        MascotMotion.CRITICAL to 60,
        MascotMotion.DEFEAT to 90,
        MascotMotion.VICTORY to 100,
    )

    fun isOneShot(motion: MascotMotion): Boolean = motion in oneShotDurations
    fun durationMs(motion: MascotMotion): Long? = oneShotDurations[motion]
    fun loops(motion: MascotMotion): Boolean = !isOneShot(motion)
    fun priority(motion: MascotMotion): Int = priorities[motion] ?: 0

    fun canInterrupt(current: MascotMotion, next: MascotMotion): Boolean =
        !isOneShot(current) || priority(next) > priority(current)
}

internal object MascotAnimationRegistry {
    val all = MascotMotion.entries.map { motion ->
        MascotAnimationDef(
            id = motion.name.lowercase(),
            motion = motion,
            clipName = MascotCatalog.clip(MascotCatalog.DEFAULT_ID, motion),
            unlockLevel = 1,
            loop = MascotMotionPolicy.loops(motion),
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

    fun react(
        next: MascotMotion,
        language: String = SonHarfUiState.language,
        force: Boolean = false,
    ) {
        if (!force && next != motion && !MascotMotionPolicy.canInterrupt(motion, next)) return
        if (next == motion && !force) return
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
internal fun MascotBehaviorBridge() {
    val activeMotion = MascotRuntime.motion
    MascotSoundBridge(activeMotion)
    androidx.compose.runtime.LaunchedEffect(activeMotion) {
        val duration = MascotMotionPolicy.durationMs(activeMotion) ?: return@LaunchedEffect
        kotlinx.coroutines.delay(duration)
        if (MascotRuntime.motion == activeMotion) {
            MascotRuntime.react(MascotMotion.IDLE, force = true)
        }
    }
}
