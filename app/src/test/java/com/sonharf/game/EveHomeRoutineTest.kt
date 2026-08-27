package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EveHomeRoutineTest {
    @Test
    fun xpPromptUsesPlayerNameExactly() {
        assertEquals("Hadi Ümit bana xp topla", eveHomeXpPrompt("  Ümit  "))
        assertEquals("Hadi Oyuncu bana xp topla", eveHomeXpPrompt("   "))
    }

    @Test
    fun homeRoutineUsesOnlyRealAcceptedGlbClips() {
        assertEquals("Attack", EveAnimationCue.HOME_DIG_RIGHT_PAW.clipName)
        assertEquals("GoToRest", EveAnimationCue.HOME_SIT_HOLD.clipName)
        assertEquals("Rest", EveAnimationCue.REST.clipName)

        assertTrue(EveAnimationCue.HOME_DIG_RIGHT_PAW.loop)
        assertFalse(EveAnimationCue.HOME_SIT_HOLD.loop)
        assertEquals(0.32f, EveAnimationCue.HOME_DIG_RIGHT_PAW.playbackSpeed, 0.001f)
        assertEquals(0.98f, EveAnimationCue.HOME_SIT_HOLD.holdAtSeconds ?: -1f, 0.001f)
    }

    @Test
    fun productionRoutineTimingIsOneMinuteThenOneMinute() {
        val timing = EveHomeRoutineTiming()
        assertEquals(60_000L, timing.digMs)
        assertEquals(60_000L, timing.sitMs)
    }

    @Test
    fun contextualReactionsOwnHomeUntilTheirAnimationWindowEnds() {
        assertEquals(6_500L, EveHomeIntent.ASK_PET.homeHoldMs())
        assertEquals(6_500L, EveHomeIntent.ASK_FOOD.homeHoldMs())
        assertEquals(5_200L, EveHomeIntent.APPROACH_LOOK.homeHoldMs())
        assertEquals(5_600L, EveHomeIntent.COMFORT.homeHoldMs())
        assertEquals(4_800L, EveHomeIntent.CELEBRATE.homeHoldMs())
        assertEquals(8_000L, EveHomeIntent.SLEEP.homeHoldMs())
    }
}
