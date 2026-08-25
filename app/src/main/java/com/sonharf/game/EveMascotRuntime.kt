package com.sonharf.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

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

internal object EveMascotRuntime {
    var mood by mutableStateOf(EveMood.CALM)
        private set
    var animation by mutableStateOf(EveAnimationCue.IDLE_BREATHE)
        private set
    var bubbleText by mutableStateOf("")
        private set
    var isThinking by mutableStateOf(false)
        private set

    fun thinking() {
        isThinking = true
        mood = EveMood.THINKING
        animation = EveAnimationCue.IDLE_LOOK_AROUND
        bubbleText = "…"
    }

    fun apply(response: EveChatResponse) {
        isThinking = false
        mood = response.mood.toEveMood()
        animation = response.animation.toEveAnimation()
        bubbleText = response.reply.trim().take(900)
    }

    fun calm() {
        isThinking = false
        mood = EveMood.CALM
        animation = EveAnimationCue.IDLE_BREATHE
    }

    fun play(cue: EveAnimationCue, bubble: String? = null) {
        animation = cue
        bubble?.let(::setBubble)
    }

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
    "rest" -> EveAnimationCue.REST
    "go_to_rest" -> EveAnimationCue.GO_TO_REST
    "rest_to_stand" -> EveAnimationCue.REST_TO_STAND
    "walk" -> EveAnimationCue.WALK
    "run" -> EveAnimationCue.RUN
    "get_hit" -> EveAnimationCue.GET_HIT
    "attack" -> EveAnimationCue.ATTACK
    else -> EveAnimationCue.IDLE_BREATHE
}
