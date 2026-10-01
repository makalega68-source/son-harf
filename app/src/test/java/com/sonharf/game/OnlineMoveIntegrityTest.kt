package com.sonharf.game

import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.WordSiegeCellDto
import org.junit.Assert.*
import org.junit.Test

class OnlineMoveIntegrityTest {
    private val board = List(225) { WordSiegeCellDto() }
    @Test fun aSecondRackDropPreservesTheFirstLetter() {
        val first = wordSiegeDropTile(emptyMap(), board, 0, null, 112)
        assertEquals(mapOf(112 to 0, 113 to 1), wordSiegeDropTile(first, board, 1, null, 113))
    }
    @Test fun anOccupiedRackDropCannotEraseTheExistingLetter() {
        val first = mapOf(112 to 0)
        assertEquals(first, wordSiegeDropTile(first, board, 1, null, 112))
    }
    @Test fun movingBoardLettersStillSwapsThem() {
        assertEquals(mapOf(112 to 1, 113 to 0),
            wordSiegeDropTile(mapOf(112 to 0, 113 to 1), board, 0, 112, 113))
    }
    @Test fun anOlderTurnCannotReturnEvenWhenItsDeadlineLooksNewer() {
        val room = GameRoomDto(id="room", code="TEST", hostId="host", guestId="guest",
            status="playing", currentPlayerId="guest", actionSeq=20,
            turnDeadline="2026-10-01T21:00:15Z")
        assertTrue(premierRoomSnapshotIsOlder(room.copy(currentPlayerId="host",
            actionSeq=19, turnDeadline="2026-10-01T21:00:16Z"), room))
        assertFalse(premierRoomSnapshotIsOlder(room.copy(currentPlayerId="host",
            actionSeq=21), room))
    }
}
