package com.sonharf.game

/**
 * High-level context for Eve's autonomous home behavior.
 *
 * This layer is deliberately pure and deterministic: renderer/Compose code only executes the
 * selected intent. Needs never alter ranked gameplay or grant competitive power.
 */
internal data class EveBehaviorContext(
    val fullness: Int,
    val happiness: Int,
    val energy: Int,
    val minutesSinceInteraction: Long,
    val recentConversationMood: EveMood? = null,
    val conversationAgeMinutes: Long? = null,
)

internal enum class EveHomeIntent {
    NORMAL,
    APPROACH_LOOK,
    ASK_PET,
    ASK_FOOD,
    SLEEP,
    COMFORT,
    CELEBRATE,
}

internal object EveBehaviorDirector {
    const val HUNGRY_THRESHOLD = 32
    const val SLEEPY_THRESHOLD = 22
    const val BORED_AFTER_MINUTES = 28L
    const val RECENT_CONVERSATION_MINUTES = 12L

    fun decide(context: EveBehaviorContext): EveHomeIntent {
        // Critical physical needs win over flavor behavior.
        if (context.energy <= SLEEPY_THRESHOLD) return EveHomeIntent.SLEEP
        if (context.fullness <= HUNGRY_THRESHOLD) return EveHomeIntent.ASK_FOOD

        val moodIsRecent = context.recentConversationMood != null &&
            (context.conversationAgeMinutes ?: Long.MAX_VALUE) <= RECENT_CONVERSATION_MINUTES

        if (moodIsRecent) {
            return when (context.recentConversationMood) {
                EveMood.CELEBRATING, EveMood.HAPPY -> EveHomeIntent.CELEBRATE
                EveMood.SUPPORTIVE, EveMood.ENCOURAGING -> EveHomeIntent.COMFORT
                EveMood.CURIOUS, EveMood.THINKING -> EveHomeIntent.APPROACH_LOOK
                EveMood.TIRED -> EveHomeIntent.SLEEP
                EveMood.CALM, null -> EveHomeIntent.NORMAL
            }
        }

        if (context.happiness <= 34) return EveHomeIntent.ASK_PET
        if (context.minutesSinceInteraction >= BORED_AFTER_MINUTES) return EveHomeIntent.ASK_PET

        return EveHomeIntent.NORMAL
    }
}
