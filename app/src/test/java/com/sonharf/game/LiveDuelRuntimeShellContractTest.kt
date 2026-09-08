package com.sonharf.game

import com.sonharf.game.data.GameRoomDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveDuelRuntimeShellContractTest {
    private val me = "00000000-0000-0000-0000-000000000001"
    private val opponent = "00000000-0000-0000-0000-000000000002"
    private val roomId = "00000000-0000-0000-0000-000000000010"

    @Test
    fun transientRoomFetchFailureKeepsRunningDuelMounted() {
        val result = resolveTrackedDuelRoomId(
            currentRoomId = roomId,
            currentUserId = me,
            roomResult = Result.failure(IllegalStateException("temporary network failure")),
        )

        assertEquals(roomId, result)
    }

    @Test
    fun transientMissingSessionKeepsRunningDuelMounted() {
        val result = resolveTrackedDuelRoomId(
            currentRoomId = roomId,
            currentUserId = null,
            roomResult = Result.failure(IllegalStateException("session refreshing")),
        )

        assertEquals(roomId, result)
    }

    @Test
    fun successfulActiveSnapshotKeepsRunningDuelMounted() {
        val result = resolveTrackedDuelRoomId(
            currentRoomId = roomId,
            currentUserId = me,
            roomResult = Result.success(room(status = "playing")),
        )

        assertEquals(roomId, result)
    }

    @Test
    fun successfulFinishedSnapshotReleasesDuelSurface() {
        val result = resolveTrackedDuelRoomId(
            currentRoomId = roomId,
            currentUserId = me,
            roomResult = Result.success(room(status = "finished")),
        )

        assertNull(result)
    }

    @Test
    fun successfulNonParticipantSnapshotReleasesDuelSurface() {
        val result = resolveTrackedDuelRoomId(
            currentRoomId = roomId,
            currentUserId = me,
            roomResult = Result.success(
                room(
                    status = "playing",
                    hostId = opponent,
                    guestId = "00000000-0000-0000-0000-000000000003",
                ),
            ),
        )

        assertNull(result)
    }

    private fun room(
        status: String,
        hostId: String = me,
        guestId: String? = opponent,
    ) = GameRoomDto(
        id = roomId,
        code = "ABC123",
        hostId = hostId,
        guestId = guestId,
        status = status,
    )
}
