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
        MascotMotion.GREETING to 1_450L,
        MascotMotion.CRITICAL to 1_150L,
        MascotMotion.VICTORY to 2_250L,
        MascotMotion.DEFEAT to 1_700L,
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
            clipName = MascotCatalog.clip(MascotCatalog.CHIBI_WIZARD_ID, motion),
            unlockLevel = 1,
            loop = MascotMotionPolicy.loops(motion),
        )
    }

    fun definition(motion: MascotMotion): MascotAnimationDef = all.first { it.motion == motion }
    fun unlocked(level: Int): List<MascotAnimationDef> = all
    fun nextUnlockLevel(level: Int): Int = Int.MAX_VALUE
}

internal object MascotRuntime {
    var motion by mutableStateOf(MascotMotion.IDLE)
        private set
    var message by mutableStateOf("")
        private set
    val petName: String get() = "Chibi"
    var playerLevel by mutableIntStateOf(1)
        private set
    var playerXp by mutableIntStateOf(0)
        private set
    var inActiveMatch by mutableStateOf(false)
        private set

    fun rename(value: String) = Unit

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
        customMessage: String? = null,
    ) {
        if (!force && next != motion && !MascotMotionPolicy.canInterrupt(motion, next)) return
        if (next == motion && !force && customMessage == null) return
        motion = next
        message = customMessage
            ?.trim()
            ?.replace(Regex("\\s+"), " ")
            ?.take(140)
            ?.takeIf { it.isNotBlank() }
            ?: localizedMessage(next, language)
    }

    fun say(
        text: String,
        motion: MascotMotion = MascotMotion.LOOK_AT_PLAYER,
        force: Boolean = true,
    ) {
        react(motion, force = force, customMessage = text)
    }

    private fun localizedMessage(motion: MascotMotion, language: String): String {
        val en = language == "en"
        return when (motion) {
            MascotMotion.GREETING -> if (en) "Chibi's here. Let's get moving." else "Chibi burada. Hadi akışı başlatalım."
            MascotMotion.THINKING -> if (en) "Final letter first. Then choose calmly." else "Önce son harf. Sonra sakin seç."
            MascotMotion.CRITICAL -> if (en) "Clock's running! Pick the safe word." else "Süre gidiyor! Güvenli kelimeyi seç."
            MascotMotion.VICTORY -> if (en) "That was clean! I like this streak." else "İşte bu! Bu oyun hoşuma gitti."
            MascotMotion.DEFEAT -> if (en) "Shake it off. Next chain, better move." else "Bitti gitti. Sonraki zincirde daha iyisini yaparız."
            MascotMotion.LOOK_AT_PLAYER -> if (en) "I'm watching. Your move." else "Seni izliyorum. Hamle sende."
            MascotMotion.SIT -> if (en) "Tiny break. Then back in." else "Mini mola. Sonra yine oyundayız."
            MascotMotion.RUN -> if (en) "I'm coming—let's play!" else "Geliyorum—hadi oynayalım!"
            MascotMotion.IDLE,
            MascotMotion.WALK,
            MascotMotion.TURN_LEFT,
            MascotMotion.TURN_RIGHT -> ""
        }
    }
}

@Composable
internal fun MascotBehaviorBridge() {
    val activeMotion = MascotRuntime.motion
    androidx.compose.runtime.LaunchedEffect(activeMotion) {
        val duration = MascotMotionPolicy.durationMs(activeMotion) ?: return@LaunchedEffect
        kotlinx.coroutines.delay(duration)
        if (MascotRuntime.motion == activeMotion) {
            MascotRuntime.react(MascotMotion.IDLE, force = true)
        }
    }
}
