package com.sonharf.game

import com.sonharf.game.data.GameInviteDto
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstReleaseGameAvailabilityTest {
    @After fun clearPendingInvitations() { LastLetterInvitations.clear() }

    @Test fun lastLetterRemainsPlayableForRoomHistoryAndNotificationIdentifiers() {
        listOf("son_harf", "last_letter", "normal", "expert").forEach {
            assertTrue(it, GameAvailability.isAvailable(it))
        }
    }

    @Test fun deferredGamesAndUnknownTargetsCannotBecomePlayableThroughOldLinks() {
        listOf("siege", "series", "classic", "word_siege", "atelier", "workshop", "word_workshop", "arena", "", "unknown").forEach {
            assertFalse(it, GameAvailability.isAvailable(it))
        }
        assertFalse(GameAvailability.dailyPuzzles)
    }

    @Test fun automaticEntryOnlyTracksExplicitInvitationsFromTheCurrentAccount() {
        assertTrue(LastLetterInvitations.pendingFor("player-a").isEmpty())
        LastLetterInvitations.await(invite("invite-a", "player-a"))
        LastLetterInvitations.await(invite("invite-b", "player-b"))
        assertEquals(setOf("invite-a"), LastLetterInvitations.pendingFor("player-a"))
        assertEquals(setOf("invite-b"), LastLetterInvitations.pendingFor("player-b"))
        LastLetterInvitations.remove("invite-a")
        assertTrue(LastLetterInvitations.pendingFor("player-a").isEmpty())
        assertEquals(setOf("invite-b"), LastLetterInvitations.pendingFor("player-b"))
        LastLetterInvitations.clear()
        assertTrue(LastLetterInvitations.pendingFor("player-b").isEmpty())
    }

    private fun invite(id: String, sender: String) = GameInviteDto(
        id = id, senderId = sender, receiverId = "rival", status = "pending",
        createdAt = "2026-10-19T10:00:00Z",
    )
}
