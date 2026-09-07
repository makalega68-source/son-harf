package com.sonharf.game

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MageCatDirectorTest {
    private fun director() = MageCatDirector(
        selector = MageCatBehaviorSelector(random = Random(17), historySize = 3),
        reactionCooldownMs = 2_800L,
        ambientCooldownMs = 9_000L,
    )

    @Test
    fun ambientMotionIsSuppressedWhilePlayerIsTyping() {
        val cue = director().cue(
            MageCatEvent.IDLE,
            MageCatContext(
                nowMs = 10_000L,
                screen = MageCatScreen.MATCH,
                playerInputActive = true,
            ),
        )

        assertNull(cue)
    }

    @Test
    fun blockingUiSuppressesAllMascotCues() {
        val cue = director().cue(
            MageCatEvent.VICTORY,
            MageCatContext(
                nowMs = 10_000L,
                screen = MageCatScreen.RESULT,
                blockingUiVisible = true,
            ),
        )

        assertNull(cue)
    }

    @Test
    fun repeatedCorrectWordReactionRespectsCooldown() {
        val director = director()
        val first = director.cue(
            MageCatEvent.CORRECT_WORD,
            MageCatContext(nowMs = 10_000L, screen = MageCatScreen.MATCH),
        )
        val second = director.cue(
            MageCatEvent.CORRECT_WORD,
            MageCatContext(nowMs = 11_000L, screen = MageCatScreen.MATCH),
        )

        assertNotNull(first)
        assertNull(second)
    }

    @Test
    fun victoryUsesHeroProminenceEvenAfterRecentReaction() {
        val director = director()
        director.cue(
            MageCatEvent.CORRECT_WORD,
            MageCatContext(nowMs = 10_000L, screen = MageCatScreen.MATCH),
        )
        val victory = director.cue(
            MageCatEvent.VICTORY,
            MageCatContext(nowMs = 10_500L, screen = MageCatScreen.RESULT, matchFinished = true),
        )

        assertNotNull(victory)
        assertEquals(MageCatProminence.HERO, victory?.prominence)
    }
}
