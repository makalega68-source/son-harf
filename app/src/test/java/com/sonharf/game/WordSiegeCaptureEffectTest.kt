package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WordSiegeCaptureEffectTest {
    @Test fun neutralAndOpponentOwnedCubesAreBothIncluded() {
        val previous = owners(0 to 0, 1 to 2, 2 to 1, 3 to 0)
        val current = owners(0 to 1, 1 to 1, 2 to 1, 3 to 0)

        val batch = wordSiegeCaptureBatch(
            updateKey = "move-2",
            previousOwners = previous,
            currentOwners = current,
            capturingOwner = 1,
        )

        assertNotNull(batch)
        assertEquals(listOf(0, 1), batch!!.indices)
        assertEquals(setOf(1), batch.opponentIndices)
        assertEquals(4, batch.points)
        assertEquals(1, batch.opponentLossPoints)
    }

    @Test fun initialAndRepeatedServerUpdateNeverReplay() {
        val initial = owners(0 to 1, 1 to 2)
        val tracker = WordSiegeCaptureTracker("10", initial)

        assertNull(tracker.preview("10", initial, 1, expectedCaptured = 1))

        val next = initial.toMutableList().apply { this[1] = 1 }
        val batch = tracker.preview("11", next, 1, expectedCaptured = 1)
        assertNotNull(batch)
        tracker.consume(batch!!, next)

        assertNull(tracker.preview("11", next, 1, expectedCaptured = 1))

        val reconnect = WordSiegeCaptureTracker("11", next)
        assertNull(reconnect.preview("11", next, 1, expectedCaptured = 1))
    }

    @Test fun passRejectedAndResetStatesProduceNoSyntheticCapture() {
        val initial = owners(0 to 1, 1 to 2)
        val tracker = WordSiegeCaptureTracker("20", initial)

        assertNull(tracker.preview("21", initial, 2, expectedCaptured = 0))
        assertNull(tracker.preview("20", initial, 1, expectedCaptured = 1))

        val reset = List(WordSiegeBoardSpec.CellCount) { 0 }
        val newGameTracker = WordSiegeCaptureTracker(null, reset)
        assertNull(newGameTracker.preview(null, reset, null, expectedCaptured = 0))
    }

    @Test fun trackerWaitsForCompleteBoardRowBeforeConsumingMove() {
        val initial = owners(0 to 0, 1 to 2)
        val tracker = WordSiegeCaptureTracker("30", initial)
        val partial = initial.toMutableList().apply { this[0] = 1 }
        assertNull(tracker.preview("31", partial, 1, expectedCaptured = 2))

        val complete = partial.toMutableList().apply { this[1] = 1 }
        val batch = tracker.preview("31", complete, 1, expectedCaptured = 2)
        assertEquals(listOf(0, 1), batch!!.indices)
    }

    @Test fun visibleScoreWithholdsOnlyPendingCapturePoints() {
        assertEquals(38, wordSiegeDisplayedScore(actualScore = 42, pendingCapturePoints = 4))
        assertEquals(42, wordSiegeDisplayedScore(actualScore = 42, pendingCapturePoints = 0))
        assertEquals(0, wordSiegeDisplayedScore(actualScore = 0, pendingCapturePoints = 4))
        assertEquals(43, wordSiegeDisplayedScore(actualScore = 42, pendingCapturePoints = 0, pendingLossPoints = 1))
    }

    private fun owners(vararg changes: Pair<Int, Int>): List<Int> =
        MutableList(WordSiegeBoardSpec.CellCount) { 0 }.apply {
            changes.forEach { (index, owner) -> this[index] = owner }
        }
}
