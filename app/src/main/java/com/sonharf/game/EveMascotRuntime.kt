package com.sonharf.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.ArrayDeque
import kotlin.random.Random
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

internal enum class EveBehaviorState {
    IDLE_BASE,
    IDLE_FLAVOR,
    INTERACTING,
    RESTING,
}

internal object EveAssetPolicy {
    const val MODEL_ASSET = "models/eve/eve.glb"
}

/**
 * The accepted Eve GLB has large root translation in locomotion/combat clips. Only clips already
 * verified to remain inside the fixed companion viewport are allowed here. Locomotion/combat
 * clips stay disabled until in-place variants are exported from Blender.
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
    private data class WeightedIdle(val cue: EveAnimationCue, val weight: Int)

    private val animationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val recentIdleClips = ArrayDeque<EveAnimationCue>(2)
    private val idlePool = listOf(
        WeightedIdle(EveAnimationCue.IDLE_BREATHE, 60),
        WeightedIdle(EveAnimationCue.IDLE_LOOK_AROUND, 25),
        WeightedIdle(EveAnimationCue.IDLE_GRAZE, 15),
    )

    private var resetJob: Job? = null
    private var autonomousIdleJob: Job? = null

    var mood by mutableStateOf(EveMood.CALM)
        private set
    var behaviorState by mutableStateOf(EveBehaviorState.IDLE_BASE)
        private set
    var animation by mutableStateOf(EveAnimationCue.IDLE_BREATHE)
        private set
    /**
     * Monotonic replay token. The renderer observes this in addition to [animation], so requesting
     * the same clip twice still restarts it from frame zero.
     */
    var animationVersion by mutableIntStateOf(0)
        private set
    var bubbleText by mutableStateOf("")
        private set
    var isThinking by mutableStateOf(false)
        private set

    fun startLivingBehavior() {
        if (autonomousIdleJob?.isActive == true) return
        autonomousIdleJob = animationScope.launch {
            while (true) {
                delay(Random.nextLong(6_000L, 12_001L))
                if (!isThinking && behaviorState == EveBehaviorState.IDLE_BASE) {
                    playAutonomousIdle(selectNextIdle())
                }
            }
        }
    }

    fun stopLivingBehavior() {
        autonomousIdleJob?.cancel()
        autonomousIdleJob = null
    }

    fun thinking() {
        resetJob?.cancel()
        isThinking = true
        mood = EveMood.THINKING
        behaviorState = EveBehaviorState.INTERACTING
        setAnimation(EveAnimationCue.IDLE_BREATHE)
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
        behaviorState = EveBehaviorState.IDLE_BASE
        setAnimation(EveAnimationCue.IDLE_BREATHE)
    }

    fun play(
        cue: EveAnimationCue,
        bubble: String? = null,
        returnToIdleAfterMs: Long = 2_400,
    ) {
        resetJob?.cancel()
        val safeCue = cue.safeForCompanionStage()
        isThinking = false
        behaviorState = if (safeCue == EveAnimationCue.REST) {
            EveBehaviorState.RESTING
        } else {
            EveBehaviorState.INTERACTING
        }
        setAnimation(safeCue)
        bubble?.let(::setBubble)

        if (returnToIdleAfterMs > 0) {
            scheduleReturnToBaseIdle(returnToIdleAfterMs)
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

    private fun playAutonomousIdle(cue: EveAnimationCue) {
        resetJob?.cancel()
        val safeCue = cue.safeForCompanionStage()
        behaviorState = if (safeCue == EveAnimationCue.IDLE_BREATHE) {
            EveBehaviorState.IDLE_BASE
        } else {
            EveBehaviorState.IDLE_FLAVOR
        }
        setAnimation(safeCue)
        rememberIdle(safeCue)
        if (safeCue != EveAnimationCue.IDLE_BREATHE) {
            scheduleReturnToBaseIdle(2_600L)
        }
    }

    private fun scheduleReturnToBaseIdle(delayMs: Long) {
        val versionAtStart = animationVersion
        resetJob = animationScope.launch {
            delay(delayMs)
            if (animationVersion == versionAtStart) {
                behaviorState = EveBehaviorState.IDLE_BASE
                mood = if (mood == EveMood.THINKING) EveMood.CALM else mood
                setAnimation(EveAnimationCue.IDLE_BREATHE)
            }
        }
    }

    private fun setAnimation(cue: EveAnimationCue) {
        animation = cue
        animationVersion++
    }

    private fun selectNextIdle(): EveAnimationCue {
        val available = idlePool.filterNot { recentIdleClips.contains(it.cue) }
            .ifEmpty { idlePool }
        val totalWeight = available.sumOf { it.weight }
        var roll = Random.nextInt(totalWeight.coerceAtLeast(1))
        for (candidate in available) {
            if (roll < candidate.weight) return candidate.cue
            roll -= candidate.weight
        }
        return EveAnimationCue.IDLE_BREATHE
    }

    private fun rememberIdle(cue: EveAnimationCue) {
        if (recentIdleClips.size >= 2) recentIdleClips.removeFirst()
        recentIdleClips.addLast(cue)
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
