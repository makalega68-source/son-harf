package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassicServerAuthorityTest {
    @Test
    fun monotonicDeadline_usesCeil_neverNegative_neverRises() {
        val anchor = ClassicMonotonicDeadlineAnchor(
            serverDeadlineEpochMs = 10_500L,
            wallEpochMsAtAnchor = 5_000L,
            elapsedRealtimeMsAtAnchor = 100_000L,
        )
        assertEquals(6, anchor.displaySeconds(100_000L))
        assertEquals(5, anchor.displaySeconds(100_501L))
        assertEquals(1, anchor.displaySeconds(105_499L))
        assertEquals(0, anchor.displaySeconds(105_500L))
        assertEquals(0L, anchor.remainingMs(999_999L))
    }

    @Test
    fun staleAndDuplicateSnapshots_areRejected() {
        val current = version(round = 3, words = 8, deadline = 20_000L, player = "b")
        assertFalse(shouldAcceptClassicSnapshot(current, current.copy()))
        assertFalse(shouldAcceptClassicSnapshot(current, version(round = 2, words = 99, deadline = 99_000L, player = "a")))
        assertFalse(shouldAcceptClassicSnapshot(current, version(round = 3, words = 7, deadline = 99_000L, player = "a")))
        assertFalse(shouldAcceptClassicSnapshot(current, version(round = 3, words = 8, deadline = 19_999L, player = "a")))
        assertTrue(shouldAcceptClassicSnapshot(current, version(round = 3, words = 9, deadline = 30_000L, player = "a")))
        assertTrue(shouldAcceptClassicSnapshot(current, version(round = 4, words = 0, deadline = 30_000L, player = "a")))
    }

    @Test
    fun finishedSnapshot_cannotBeReopenedByLateEvent() {
        val current = version(status = "finished", round = 5, words = 12, deadline = null, player = null)
        val late = version(status = "playing", round = 5, words = 12, deadline = 50_000L, player = "a")
        assertFalse(shouldAcceptClassicSnapshot(current, late))
    }

    @Test
    fun localTimerZero_isOnlyPresentationState() {
        val anchor = ClassicMonotonicDeadlineAnchor(2_000L, 1_000L, 10_000L)
        assertEquals(0, anchor.displaySeconds(11_000L))
        // No turn/player data exists in the timer anchor: local zero cannot mutate server turn state.
        assertEquals(0L, anchor.remainingMs(12_000L))
    }

    private fun version(
        status: String = "playing",
        round: Int,
        words: Int,
        deadline: Long?,
        player: String?,
    ) = ClassicSnapshotVersion(
        roomId = "room",
        status = status,
        roundNo = round,
        validWordCount = words,
        deadlineEpochMs = deadline,
        currentPlayerId = player,
        lastEvent = null,
        lastEventPlayerId = null,
        hostScore = 10,
        guestScore = 9,
    )
}
