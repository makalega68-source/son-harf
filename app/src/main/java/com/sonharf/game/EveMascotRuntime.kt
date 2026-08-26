package com.sonharf.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal enum class EveMood {
    CALM,
    HAPPY,
    THINKING,
    ENCOURAGING,
    CURIOUS,
    SUPPORTIVE,
    TIRED,
    CELEBRATING,
}

internal enum class EveAnimationCue(val clipName: String, val loop: Boolean) {
    IDLE_BREATHE("IdleBreathe", true),
    IDLE_LOOK_AROUND("IdleLookAround", true),
    IDLE_GRAZE("IdleGraze", true),
    GRAZE_ONCE("GrazeOnce", false),
    REST("Rest", true),
    GO_TO_REST("GoToRest", false),
    REST_TO_STAND("RestToGoBackUp", false),
    WALK("Walk", true),
    RUN("Run", true),
    GET_HIT("GetHit", false),
    ATTACK("Attack", false),
}

internal object EveAssetPolicy {
    const val MODEL_ASSET = "models/eve/eve.glb"
}

/**
 * The accepted Eve GLB has large root translation in locomotion/combat clips. Only the clips
 * verified to stay inside the fixed companion viewport are allowed in the room. Unsafe requests
 * are mapped to a visible in-place reaction instead of moving Eve out of frame.
 */
private fun EveAnimationCue.safeForCompanionStage(): EveAnimationCue = when (this) {
    EveAnimationCue.IDLE_BREATHE,
    EveAnimationCue.IDLE_LOOK_AROUND,
    EveAnimationCue.IDLE_GRAZE,
    EveAnimationCue.GRAZE_ONCE,
    EveAnimationCue.REST -> this

    EveAnimationCue.GO_TO_REST,
    EveAnimationCue.REST_TO_STAND,
    EveAnimationCue.WALK,
    EveAnimationCue.RUN,
    EveAnimationCue.GET_HIT,
    EveAnimationCue.ATTACK -> EveAnimationCue.IDLE_LOOK_AROUND
}

internal object EveMascotRuntime {
    private val animationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var resetJob: Job? = null

    var mood by mutableStateOf(EveMood.CALM)
        private set
    var animation by mutableStateOf(EveAnimationCue.IDLE_BREATHE)
        private set
    var animationVersion by mutableIntStateOf(0)
        private set
    var bubbleText by mutableStateOf("")
        private set
    var isThinking by mutableStateOf(false)
        private set

    fun thinking() {
        resetJob?.cancel()
        isThinking = true
        mood = EveMood.THINKING
        animation = EveAnimationCue.IDLE_BREATHE
        animationVersion++
        bubbleText = "…"
    }

    fun apply(response: EveChatResponse) {
        isThinking = false
        mood = response.mood.toEveMood()
        bubbleText = response.reply.trim().take(900)
        play(
            cue = response.animation.toEveAnimation(),
            returnToIdleAfterMs = 2_800,
        )
    }

    fun calm() {
        resetJob?.cancel()
        isThinking = false
        mood = EveMood.CALM
        animation = EveAnimationCue.IDLE_BREATHE
        animationVersion++
    }

    fun play(
        cue: EveAnimationCue,
        bubble: String? = null,
        returnToIdleAfterMs: Long = 2_400,
    ) {
        resetJob?.cancel()
        animation = cue.safeForCompanionStage()
        animationVersion++
        bubble?.let(::setBubble)

        if (animation != EveAnimationCue.IDLE_BREATHE && returnToIdleAfterMs > 0) {
            val versionAtStart = animationVersion
            resetJob = animationScope.launch {
                delay(returnToIdleAfterMs)
                if (animationVersion == versionAtStart) {
                    animation = EveAnimationCue.IDLE_BREATHE
                    animationVersion++
                }
            }
        }
    }

    fun petReaction(bubble: String? = null) =
        play(EveAnimationCue.IDLE_LOOK_AROUND, bubble, returnToIdleAfterMs = 2_000)

    fun feedReaction(bubble: String? = null) =
        play(EveAnimationCue.GRAZE_ONCE, bubble, returnToIdleAfterMs = 2_700)

    fun giftReaction(bubble: String? = null) =
        play(EveAnimationCue.IDLE_LOOK_AROUND, bubble, returnToIdleAfterMs = 2_600)

    fun happyReaction(bubble: String? = null) =
        play(EveAnimationCue.IDLE_LOOK_AROUND, bubble, returnToIdleAfterMs = 2_200)

    fun setBubble(text: String) {
        bubbleText = text.trim().take(900)
    }
}

internal fun String.toEveMood(): EveMood = when (lowercase()) {
    "happy" -> EveMood.HAPPY
    "thinking" -> EveMood.THINKING
    "encouraging" -> EveMood.ENCOURAGING
    "curious" -> EveMood.CURIOUS
    "supportive" -> EveMood.SUPPORTIVE
    "tired" -> EveMood.TIRED
    "celebrating" -> EveMood.CELEBRATING
    else -> EveMood.CALM
}

internal fun String.toEveAnimation(): EveAnimationCue = when (lowercase()) {
    "idle_look_around" -> EveAnimationCue.IDLE_LOOK_AROUND
    "idle_graze" -> EveAnimationCue.IDLE_GRAZE
    "graze_once" -> EveAnimationCue.GRAZE_ONCE
    "rest" -> EveAnimationCue.REST
    "go_to_rest" -> EveAnimationCue.GO_TO_REST
    "rest_to_stand" -> EveAnimationCue.REST_TO_STAND
    "walk" -> EveAnimationCue.WALK
    "run" -> EveAnimationCue.RUN
    "get_hit" -> EveAnimationCue.GET_HIT
    "attack" -> EveAnimationCue.ATTACK
    else -> EveAnimationCue.IDLE_BREATHE
}
