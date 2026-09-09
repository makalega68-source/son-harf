package com.sonharf.game

import com.sonharf.game.mascot.*
import org.junit.Assert.*
import org.junit.Test

class MageCatBrainTest {
    private val base = CompanionSnapshot("match-a", 10, 20, true, 20)
    @Test fun opponentTimerCannotPanic() {
        val brain = MageCatBrain()
        brain.observe(base, 0)
        assertEquals(MageCatMood.IDLE, brain.observe(base.copy(myTurn = false, secondsLeft = 2), 4000).mood)
    }
    @Test fun repeatedValidWordsEachReactEvenWithSameEventName() {
        val brain = MageCatBrain()
        brain.observe(base, 0)
        val first = brain.observe(base.copy(myScore = 12), 100)
        val second = brain.observe(base.copy(myScore = 14), 200)
        assertEquals(MageCatMood.HAPPY, second.mood)
        assertTrue(second.sequence > first.sequence)
        assertNotEquals(first.tr, second.tr)
    }
    @Test fun resultWinsOverScoreAndTimerAndDoesNotReplay() {
        val brain = MageCatBrain()
        brain.observe(base, 0)
        val end = base.copy(myScore = 40, secondsLeft = 0, finished = true, won = true)
        val result = brain.observe(end, 100)
        assertEquals(MageCatMood.EXCITED, result.mood)
        assertEquals(result.sequence, brain.observe(end, 8000).sequence)
    }
    @Test fun rematchDoesNotInheritVictory() {
        val brain = MageCatBrain()
        brain.observe(base.copy(finished = true, won = true), 0)
        assertEquals(MageCatMood.ANGRY, brain.observe(base.copy(matchId = "match-b"), 100).mood)
    }
    @Test fun noPanicWithoutTimerOrOnExpiredTimer() {
        for (seconds in listOf(null, 0)) {
            assertNotEquals(MageCatMood.PANIC, MageCatBrain().observe(base.copy(secondsLeft = seconds), 0).mood)
        }
    }
    @Test fun urgencyPlaysOnlyOncePerTurn() {
        val brain = MageCatBrain()
        brain.observe(base, 0)
        val urgent = base.copy(secondsLeft = 3)
        val first = brain.observe(urgent, 100)
        assertEquals(MageCatMood.PANIC, first.mood)
        assertEquals(first.sequence, brain.observe(urgent.copy(secondsLeft = 2), 200).sequence)
        assertEquals(MageCatMood.ANGRY, brain.observe(urgent.copy(secondsLeft = 1), 3000).mood)
    }
    @Test fun rejectionAndTerritoryLossGiveSupport() {
        val brain = MageCatBrain()
        brain.observe(base.copy(territory = 6), 0)
        assertEquals(MageCatMood.SAD, brain.observe(base.copy(territory = 4), 100).mood)
        assertEquals(MageCatMood.SAD, brain.observe(base.copy(territory = 4, rejectedWords = 1), 200).mood)
    }
    @Test fun reconnectSnapshotIsNotMistakenForComeback() {
        val brain = MageCatBrain()
        assertNotEquals(MageCatMood.EXCITED, brain.observe(base.copy(myScore = 100), 0).mood)
    }
    @Test fun ownTimeoutIsDeduplicated() {
        val brain = MageCatBrain()
        brain.observe(base, 0)
        val timeout = base.copy(myTurn = false, ownFailureKey = "turn-2")
        val reaction = brain.observe(timeout, 100)
        assertEquals(MageCatMood.SAD, reaction.mood)
        assertEquals(reaction.sequence, brain.observe(timeout, 200).sequence)
    }
    @Test fun drawIsDistinctFromDefeat() {
        assertEquals(MageCatMood.HAPPY, MageCatBrain().observe(base.copy(finished = true, draw = true), 0).mood)
    }
}
