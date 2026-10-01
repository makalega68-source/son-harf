package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Test

class MascotLauncherIconTest {
    @Test
    fun `launcher mood follows requested five day cycle`() {
        val expected = listOf(
            MascotLauncherIcon.Mood.HAPPY,  // day 0: app just opened
            MascotLauncherIcon.Mood.SAD,    // day 1
            MascotLauncherIcon.Mood.ANGRY,  // day 2
            MascotLauncherIcon.Mood.ANGRY,  // day 3: no separate face was specified
            MascotLauncherIcon.Mood.SLEEPY, // day 4: closed eyes
            MascotLauncherIcon.Mood.HAPPY,  // day 5: cycle restarts
            MascotLauncherIcon.Mood.SAD,    // day 6
            MascotLauncherIcon.Mood.ANGRY,  // day 7
            MascotLauncherIcon.Mood.ANGRY,  // day 8
            MascotLauncherIcon.Mood.SLEEPY, // day 9
            MascotLauncherIcon.Mood.HAPPY,  // day 10
        )

        expected.forEachIndexed { day, mood ->
            assertEquals("Unexpected mood on day $day", mood, MascotLauncherIcon.moodForAwayDays(day.toLong()))
        }
    }

    @Test
    fun `next transition skips redundant angry day and repeats`() {
        assertEquals(1L, MascotLauncherIcon.nextTransitionDay(0))
        assertEquals(2L, MascotLauncherIcon.nextTransitionDay(1))
        assertEquals(4L, MascotLauncherIcon.nextTransitionDay(2))
        assertEquals(4L, MascotLauncherIcon.nextTransitionDay(3))
        assertEquals(5L, MascotLauncherIcon.nextTransitionDay(4))
        assertEquals(6L, MascotLauncherIcon.nextTransitionDay(5))
        assertEquals(9L, MascotLauncherIcon.nextTransitionDay(7))
        assertEquals(10L, MascotLauncherIcon.nextTransitionDay(9))
    }
}
