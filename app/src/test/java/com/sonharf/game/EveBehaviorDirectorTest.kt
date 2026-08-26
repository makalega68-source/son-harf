package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Test

class EveBehaviorDirectorTest {
    private fun context(
        fullness: Int = 70,
        happiness: Int = 80,
        energy: Int = 80,
        idleMinutes: Long = 3,
        mood: EveMood? = null,
        moodAge: Long? = null,
    ) = EveBehaviorContext(
        fullness = fullness,
        happiness = happiness,
        energy = energy,
        minutesSinceInteraction = idleMinutes,
        recentConversationMood = mood,
        conversationAgeMinutes = moodAge,
    )

    @Test
    fun criticalNeedsHavePriority() {
        assertEquals(EveHomeIntent.SLEEP, EveBehaviorDirector.decide(context(energy = 15, fullness = 10)))
        assertEquals(EveHomeIntent.ASK_FOOD, EveBehaviorDirector.decide(context(fullness = 20)))
    }

    @Test
    fun recentConversationMoodChangesBehavior() {
        assertEquals(EveHomeIntent.CELEBRATE, EveBehaviorDirector.decide(context(mood = EveMood.HAPPY, moodAge = 2)))
        assertEquals(EveHomeIntent.COMFORT, EveBehaviorDirector.decide(context(mood = EveMood.SUPPORTIVE, moodAge = 3)))
        assertEquals(EveHomeIntent.APPROACH_LOOK, EveBehaviorDirector.decide(context(mood = EveMood.CURIOUS, moodAge = 1)))
    }

    @Test
    fun staleConversationDoesNotOverrideCurrentLifeState() {
        assertEquals(
            EveHomeIntent.NORMAL,
            EveBehaviorDirector.decide(context(mood = EveMood.HAPPY, moodAge = 40)),
        )
    }

    @Test
    fun boredomRequestsAttentionWithoutPunishment() {
        assertEquals(
            EveHomeIntent.ASK_PET,
            EveBehaviorDirector.decide(context(idleMinutes = EveBehaviorDirector.BORED_AFTER_MINUTES + 1)),
        )
    }
}
