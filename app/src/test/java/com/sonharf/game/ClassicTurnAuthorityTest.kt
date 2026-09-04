package com.sonharf.game

import com.sonharf.game.data.GameRoomDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassicTurnAuthorityTest {
    private val me = "me"

    private fun room(
        player: String? = me,
        deadline: String? = "2026-09-04T12:00:10Z",
        round: Int = 1,
        words: Int = 4,
        status: String = "playing",
    ) = GameRoomDto(
        id = "match-1",
        code = "ABC123",
        hostId = me,
        guestId = "rival",
        status = status,
        currentPlayerId = player,
        turnDeadline = deadline,
        roundNo = round,
        validWordCount = words,
    )

    @Test fun countdownFlowsTenToZeroFromFakeClock() {
        val anchor = classicDeadlineAnchor(
            "2026-09-04T12:00:10Z",
            wallEpochMsNow = 1_767_528_000_000L,
            elapsedRealtimeMsNow = 50_000L,
        )!!
        val shown = (0L..10_000L step 1_000L).map { delta ->
            classicShownSeconds(classicRemainingMs(anchor, 50_000L + delta))
        }
        assertEquals(listOf(10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0), shown)
    }

    @Test fun deviceWallClockChangeAfterAnchorCannotIncreaseTimer() {
        val anchor = classicDeadlineAnchor(
            "2026-09-04T12:00:10Z",
            wallEpochMsNow = 1_767_528_000_000L,
            elapsedRealtimeMsNow = 100L,
        )!!
        assertEquals(7_000L, classicRemainingMs(anchor, 3_100L))
        // There is deliberately no wall-clock input after anchoring.
        assertEquals(3_000L, classicRemainingMs(anchor, 7_100L))
    }

    @Test fun delayedOlderAcceptedWordSnapshotIsRejected() {
        val current = room(player = "rival", words = 5, deadline = "2026-09-04T12:00:20Z")
        val stale = room(player = me, words = 4, deadline = "2026-09-04T12:00:10Z")
        assertFalse(classicShouldAcceptRoom(current, stale))
    }

    @Test fun delayedOldTimeoutSnapshotIsRejectedAtSameWordRevision() {
        val current = room(player = "rival", words = 4, deadline = "2026-09-04T12:00:55Z")
        val stale = room(player = me, words = 4, deadline = "2026-09-04T12:00:10Z")
        assertFalse(classicShouldAcceptRoom(current, stale))
    }

    @Test fun authoritativeTimeoutWithLaterDeadlineIsAccepted() {
        val current = room(player = me, words = 4, deadline = "2026-09-04T12:00:10Z")
        val next = room(player = "rival", words = 4, deadline = "2026-09-04T12:00:55Z")
        assertTrue(classicShouldAcceptRoom(current, next))
    }

    @Test fun duplicateSnapshotIsIdempotent() {
        val current = room()
        assertTrue(classicShouldAcceptRoom(current, current.copy()))
        assertEquals(ClassicTurnPhase.MY_TURN, classicTurnPhase(current, me))
    }

    @Test fun submittingIsScopedToExactTurnToken() {
        val current = room()
        val token = classicTurnToken(current)
        assertEquals(ClassicTurnPhase.SUBMITTING, classicTurnPhase(current, me, token))
        val nextTurn = current.copy(currentPlayerId = "rival", validWordCount = 5, turnDeadline = "2026-09-04T12:00:55Z")
        assertEquals(ClassicTurnPhase.OPPONENT_TURN, classicTurnPhase(nextTurn, me, token))
    }

    @Test fun finishedSnapshotCannotRegressToPlaying() {
        val finished = room(status = "finished", player = null, deadline = null)
        assertFalse(classicShouldAcceptRoom(finished, room()))
        assertEquals(ClassicTurnPhase.FINISHED, classicTurnPhase(finished, me))
    }

    @Test fun oldTurnTimerCallbackCannotChangePhase() {
        val old = room(player = me, deadline = "2026-09-04T12:00:10Z")
        val current = room(player = "rival", words = 5, deadline = "2026-09-04T12:00:55Z")
        assertFalse(classicShouldAcceptRoom(current, old))
        assertEquals(ClassicTurnPhase.OPPONENT_TURN, classicTurnPhase(current, me))
    }
}
