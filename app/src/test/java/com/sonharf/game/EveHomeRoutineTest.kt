package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EveSleepWakeBehaviorTest {
    @Test
    fun sleepsOnlyAfterOneFullMinuteWithoutInteraction() {
        assertFalse(EveInactivityPolicy.shouldSleep(59_999L))
        assertTrue(EveInactivityPolicy.shouldSleep(60_000L))
        assertTrue(EveInactivityPolicy.shouldSleep(120_000L))
    }

    @Test
    fun autonomousIdleDoesNotReuseHeadBowingHomeAliases() {
        val clips = EveAnimationCue.values().toList().map { it.clipName }
        assertFalse(EveAnimationCue.entries.any { it.name == "HOME_DIG_RIGHT_PAW" })
        assertFalse(EveAnimationCue.entries.any { it.name == "HOME_SIT_HOLD" })
        assertTrue("IdleBreathe" in clips)
        assertTrue("IdleLookAround" in clips)
        assertTrue("Rest" in clips)
    }

    @Test
    fun xpPromptStillUsesPlayerNameExactly() {
        assertEquals("Hadi Ümit bana xp topla", eveHomeXpPrompt("  Ümit  "))
        assertEquals("Hadi Oyuncu bana xp topla", eveHomeXpPrompt("   "))
    }
}
