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
import kotlinx.coroutines.isActive
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

/**
 * Small home-only root motions layered on the real rigged GLB.
 *
 * These are not fake animation clips. Skeletal motion still comes only from animation names that
 * really exist inside eve.glb. The root pulses merely give the compact companion a visible jump,
 * body wiggle or sleepy settle without changing the room renderer or its Surface/IME path.
 */
internal enum class EveMotionEffect {
    NONE,
    BOUNCE,
    WIGGLE,
    SAD_SETTLE,
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
    private data class WeightedIdle(
        val cue: EveAnimationCue,
        val weight: Int,
        val minHoldMs: Long,
        val maxHoldMs: Long,
    )

    private val animationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val recentIdleClips = ArrayDeque<EveAnimationCue>(1)
    private val idlePool = listOf(
        WeightedIdle(EveAnimationCue.IDLE_LOOK_AROUND, 52, 3_200L, 4_800L),
        WeightedIdle(EveAnimationCue.IDLE_GRAZE, 28, 3_800L, 5_600L),
        WeightedIdle(EveAnimationCue.GRAZE_ONCE, 20, 2_400L, 3_100L),
    )

    private var resetJob: Job? = null
    private var motionResetJob: Job? = null
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

    var motionEffect by mutableStateOf(EveMotionEffect.NONE)
        private set
    var motionVersion by mutableIntStateOf(0)
        private set

    var bubbleText by mutableStateOf("")
        private set
    var isThinking by mutableStateOf(false)
        private set

    /**
     * Continuous companion loop.
     *
     * IdleBreathe itself keeps looping, and the controller now inserts a new real GLB behavior
     * within roughly a second after each previous behavior finishes. This removes the visible
     * multi-second "animate, freeze, wait" rhythm while still preserving occasional sleep.
     */
    fun startLivingBehavior() {
        if (autonomousIdleJob?.isActive == true) return
        autonomousIdleJob = animationScope.launch {
            if (!isThinking && behaviorState == EveBehaviorState.IDLE_BASE) {
                publishAnimation(EveAnimationCue.IDLE_BREATHE)
            }
            delay(Random.nextLong(650L, 1_101L))
            while (isActive) {
                when {
                    isThinking || behaviorState == EveBehaviorState.INTERACTING -> delay(220L)
                    behaviorState == EveBehaviorState.RESTING -> delay(300L)
                    behaviorState == EveBehaviorState.IDLE_BASE -> {
                        when (Random.nextInt(100)) {
                            in 0..7 -> playAutonomousSleep()
                            in 8..21 -> playAutonomousPlay()
                            else -> playAutonomousIdle(selectNextIdle())
                        }
                        delay(Random.nextLong(650L, 1_201L))
                    }
                    else -> delay(280L)
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
        publishMotion(EveMotionEffect.NONE)
        publishAnimation(EveAnimationCue.IDLE_BREATHE)
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
        publishMotion(EveMotionEffect.NONE)
        publishAnimation(EveAnimationCue.IDLE_BREATHE)
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
        publishAnimation(safeCue)
        bubble?.let(::setBubble)

        if (returnToIdleAfterMs > 0) {
            scheduleReturnToBaseIdle(returnToIdleAfterMs)
        }
    }

    /** Visible positive reaction for a direct tap on the home companion. */
    fun touchReaction() {
        when (Random.nextInt(3)) {
            0 -> react(
                cue = EveAnimationCue.IDLE_LOOK_AROUND,
                nextMood = EveMood.HAPPY,
                effect = EveMotionEffect.BOUNCE,
                effectDurationMs = 900L,
                returnToIdleAfterMs = 2_200L,
            )
            1 -> react(
                cue = EveAnimationCue.IDLE_LOOK_AROUND,
                nextMood = EveMood.CURIOUS,
                effect = EveMotionEffect.WIGGLE,
                effectDurationMs = 1_150L,
                returnToIdleAfterMs = 2_300L,
            )
            else -> react(
                cue = EveAnimationCue.GRAZE_ONCE,
                nextMood = EveMood.HAPPY,
                effect = EveMotionEffect.BOUNCE,
                effectDurationMs = 760L,
                returnToIdleAfterMs = 2_500L,
            )
        }
    }

    fun petReaction(bubble: String? = null) = react(
        cue = EveAnimationCue.IDLE_LOOK_AROUND,
        nextMood = EveMood.HAPPY,
        effect = EveMotionEffect.WIGGLE,
        effectDurationMs = 1_100L,
        bubble = bubble,
        returnToIdleAfterMs = 2_100L,
    )

    fun feedReaction(bubble: String? = null) = react(
        cue = EveAnimationCue.GRAZE_ONCE,
        nextMood = EveMood.HAPPY,
        effect = EveMotionEffect.BOUNCE,
        effectDurationMs = 800L,
        bubble = bubble,
        returnToIdleAfterMs = 2_700L,
    )

    fun giftReaction(bubble: String? = null) = react(
        cue = EveAnimationCue.IDLE_LOOK_AROUND,
        nextMood = EveMood.CELEBRATING,
        effect = EveMotionEffect.BOUNCE,
        effectDurationMs = 1_000L,
        bubble = bubble,
        returnToIdleAfterMs = 2_600L,
    )

    fun happyReaction(bubble: String? = null) = react(
        cue = EveAnimationCue.IDLE_LOOK_AROUND,
        nextMood = EveMood.HAPPY,
        effect = EveMotionEffect.BOUNCE,
        effectDurationMs = 1_000L,
        bubble = bubble,
        returnToIdleAfterMs = 2_300L,
    )

    fun sadReaction(bubble: String? = null) = react(
        cue = EveAnimationCue.REST,
        nextMood = EveMood.TIRED,
        effect = EveMotionEffect.SAD_SETTLE,
        effectDurationMs = 2_800L,
        bubble = bubble,
        returnToIdleAfterMs = 2_800L,
    )

    fun setBubble(text: String) {
        bubbleText = text.trim().take(900)
    }

    private fun react(
        cue: EveAnimationCue,
        nextMood: EveMood,
        effect: EveMotionEffect,
        effectDurationMs: Long,
        bubble: String? = null,
        returnToIdleAfterMs: Long,
    ) {
        resetJob?.cancel()
        isThinking = false
        mood = nextMood
        behaviorState = if (cue == EveAnimationCue.REST) EveBehaviorState.RESTING else EveBehaviorState.INTERACTING
        publishAnimation(cue.safeForCompanionStage())
        publishMotion(effect, effectDurationMs)
        bubble?.let(::setBubble)
        scheduleReturnToBaseIdle(returnToIdleAfterMs)
    }

    private fun playAutonomousIdle(cue: EveAnimationCue) {
        resetJob?.cancel()
        val safeCue = cue.safeForCompanionStage()
        behaviorState = EveBehaviorState.IDLE_FLAVOR
        mood = EveMood.CALM
        publishAnimation(safeCue)
        rememberIdle(safeCue)
        scheduleReturnToBaseIdle(holdDurationFor(safeCue))
    }

    private fun playAutonomousPlay() {
        resetJob?.cancel()
        behaviorState = EveBehaviorState.IDLE_FLAVOR
        mood = EveMood.HAPPY
        publishAnimation(EveAnimationCue.IDLE_LOOK_AROUND)
        publishMotion(EveMotionEffect.BOUNCE, Random.nextLong(720L, 981L))
        scheduleReturnToBaseIdle(Random.nextLong(2_000L, 2_801L))
    }

    private fun playAutonomousSleep() {
        resetJob?.cancel()
        behaviorState = EveBehaviorState.RESTING
        mood = EveMood.TIRED
        publishAnimation(EveAnimationCue.REST)
        val sleepMs = Random.nextLong(6_500L, 10_501L)
        publishMotion(EveMotionEffect.SAD_SETTLE, sleepMs)
        scheduleReturnToBaseIdle(sleepMs)
    }

    private fun scheduleReturnToBaseIdle(delayMs: Long) {
        val versionAtStart = animationVersion
        resetJob = animationScope.launch {
            delay(delayMs)
            if (animationVersion == versionAtStart) {
                behaviorState = EveBehaviorState.IDLE_BASE
                mood = EveMood.CALM
                publishMotion(EveMotionEffect.NONE)
                publishAnimation(EveAnimationCue.IDLE_BREATHE)
            }
        }
    }

    private fun publishAnimation(cue: EveAnimationCue) {
        animation = cue
        animationVersion++
    }

    private fun publishMotion(effect: EveMotionEffect, durationMs: Long = 0L) {
        motionResetJob?.cancel()
        motionEffect = effect
        motionVersion++

        if (effect != EveMotionEffect.NONE && durationMs > 0L) {
            val versionAtStart = motionVersion
            motionResetJob = animationScope.launch {
                delay(durationMs)
                if (motionVersion == versionAtStart) {
                    motionEffect = EveMotionEffect.NONE
                    motionVersion++
                }
            }
        }
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
        return EveAnimationCue.IDLE_LOOK_AROUND
    }

    private fun holdDurationFor(cue: EveAnimationCue): Long {
        val profile = idlePool.firstOrNull { it.cue == cue } ?: return 2_800L
        return Random.nextLong(profile.minHoldMs, profile.maxHoldMs + 1L)
    }

    private fun rememberIdle(cue: EveAnimationCue) {
        if (recentIdleClips.size >= 1) recentIdleClips.removeFirst()
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
