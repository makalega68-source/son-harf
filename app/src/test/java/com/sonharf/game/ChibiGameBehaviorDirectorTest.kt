package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChibiGameBehaviorDirectorTest {
    @Test
    fun repeatedPlayerTurnDoesNotReuseSameChoreography() {
        val director = ChibiGameBehaviorDirector("tr")
        val first = requireNotNull(
            director.plan(
                ChibiGameEvent.PLAYER_TURN,
                ChibiBehaviorContext(playerName = "Ümit"),
                nowMs = 10_000L,
            )
        )
        val second = requireNotNull(
            director.plan(
                ChibiGameEvent.PLAYER_TURN,
                ChibiBehaviorContext(playerName = "Ümit"),
                nowMs = 12_500L,
            )
        )

        assertNotEquals(first.id, second.id)
        assertNotEquals(first.steps.map { it.motion }, second.steps.map { it.motion })
    }

    @Test
    fun lowValueAmbientIsRateLimited() {
        val director = ChibiGameBehaviorDirector("tr")
        val first = director.plan(ChibiGameEvent.AMBIENT, nowMs = 1_000L)
        val blocked = director.plan(ChibiGameEvent.AMBIENT, nowMs = 3_000L)
        val later = director.plan(ChibiGameEvent.AMBIENT, nowMs = 9_000L)

        assertTrue(first != null)
        assertNull(blocked)
        assertTrue(later != null)
    }

    @Test
    fun winAndLossChoreographiesReturnToIdle() {
        val winDirector = ChibiGameBehaviorDirector("tr")
        val lossDirector = ChibiGameBehaviorDirector("tr")

        val win = requireNotNull(winDirector.plan(ChibiGameEvent.WIN, nowMs = 1_000L))
        val loss = requireNotNull(lossDirector.plan(ChibiGameEvent.LOSS, nowMs = 1_000L))

        assertEquals(MascotMotion.IDLE, win.steps.last().motion)
        assertEquals(MascotMotion.IDLE, loss.steps.last().motion)
        assertTrue(win.steps.count { it.motion == MascotMotion.VICTORY } == 1)
        assertTrue(loss.steps.count { it.motion == MascotMotion.DEFEAT } == 1)
    }

    @Test
    fun importantMomentsEscalatePriority() {
        val director = ChibiGameBehaviorDirector("tr")
        val normal = requireNotNull(
            director.plan(
                ChibiGameEvent.PLAYER_WORD,
                ChibiBehaviorContext(word = "merhaba"),
                nowMs = 10_000L,
            )
        )
        val streak = requireNotNull(
            director.plan(
                ChibiGameEvent.PLAYER_STREAK,
                ChibiBehaviorContext(streak = 3),
                nowMs = 13_000L,
            )
        )
        val critical = requireNotNull(
            director.plan(
                ChibiGameEvent.TIME_CRITICAL,
                ChibiBehaviorContext(seconds = 4),
                nowMs = 16_000L,
            )
        )
        val win = requireNotNull(director.plan(ChibiGameEvent.WIN, nowMs = 18_000L))

        assertTrue(normal.priority < streak.priority)
        assertTrue(streak.priority < critical.priority)
        assertTrue(critical.priority < win.priority)
    }

    @Test
    fun sampledPlansNeverRepeatTheSameRealClipBackToBack() {
        val events = listOf(
            ChibiGameEvent.MATCH_START,
            ChibiGameEvent.PLAYER_TURN,
            ChibiGameEvent.RIVAL_TURN,
            ChibiGameEvent.PLAYER_WORD,
            ChibiGameEvent.PLAYER_LONG_WORD,
            ChibiGameEvent.PLAYER_STREAK,
            ChibiGameEvent.RIVAL_WORD,
            ChibiGameEvent.TIME_WARNING,
            ChibiGameEvent.TIME_CRITICAL,
            ChibiGameEvent.WIN,
            ChibiGameEvent.LOSS,
        )

        events.forEachIndexed { index, event ->
            val director = ChibiGameBehaviorDirector("tr")
            val plan = requireNotNull(
                director.plan(
                    event,
                    ChibiBehaviorContext(
                        playerName = "Ümit",
                        word = "merhaba",
                        streak = 3,
                        seconds = 5,
                    ),
                    nowMs = 100_000L + index * 20_000L,
                )
            )
            val clips = plan.steps.map { MascotCatalog.clip(MascotCatalog.CHIBI_WIZARD_ID, it.motion) }
            clips.zipWithNext().forEach { (a, b) ->
                assertNotEquals("Adjacent duplicate clip in ${plan.id}", a, b)
            }
        }
    }

    @Test
    fun playerWordVariantsActuallyVary() {
        val director = ChibiGameBehaviorDirector("tr")
        val plans = listOf(
            requireNotNull(director.plan(ChibiGameEvent.PLAYER_WORD, ChibiBehaviorContext(word = "kalem"), 10_000L)),
            requireNotNull(director.plan(ChibiGameEvent.PLAYER_WORD, ChibiBehaviorContext(word = "masa"), 12_000L)),
            requireNotNull(director.plan(ChibiGameEvent.PLAYER_WORD, ChibiBehaviorContext(word = "araba"), 14_000L)),
        )

        assertEquals(3, plans.map { it.id }.toSet().size)
    }
}
