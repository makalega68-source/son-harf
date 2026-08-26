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
        assertEquals(0.45f, EveAnimationCue.HOME_DIG_RIGHT_PAW.playbackSpeed, 0.001f)
        assertEquals(0.72f, EveAnimationCue.HOME_SIT_HOLD.holdAtSeconds ?: -1f, 0.001f)
    }

    @Test
    fun productionRoutineTimingIsOneMinuteThenOneMinute() {
        val timing = EveHomeRoutineTiming()
        assertEquals(60_000L, timing.digMs)
        assertEquals(60_000L, timing.sitMs)
    }
}
