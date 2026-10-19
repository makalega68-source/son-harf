package com.sonharf.game

import com.sonharf.game.data.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class SocialSessionFlowTest {
    private val id = "12345678-1234-1234-1234-123456789abc"
    private fun siege(id: String, turn: String = "me", status: String = "playing") = WordSiegeGameDto(id, "me", "rival", status, currentPlayerId = turn)
    private fun duel(id: String, turn: String = "me", status: String = "playing", botTurn: Boolean = false) = GameRoomDto(id, "ABC123", "me", "rival", status = status, currentPlayerId = turn, botTurn = botTurn)
    @Test fun ongoingGamesCombineModesAndPrioritizeMyTurn() {
        val rows = unifiedHomeMatches(listOf(siege("s1", "rival")), listOf(duel("d1")), "me")
        assertEquals(listOf("d1", "s1"), rows.map { it.id })
        assertEquals(listOf("son_harf", "siege"), rows.map { it.kind })
    }
    @Test fun finishedAndForeignGamesNeverAppearAsOngoing() {
        val foreign = siege("foreign").copy(playerOneId = "other", playerTwoId = "rival")
        assertTrue(unifiedHomeMatches(listOf(siege("finished", status = "finished"), foreign), listOf(duel("done", status = "finished")), "me").isEmpty())
        assertTrue(unifiedHomeMatches(listOf(siege("one")), listOf(duel("two")), null).isEmpty())
    }
    @Test fun pausedAndBotTurnsDoNotPretendToBePlayerTurns() {
        val rows = unifiedHomeMatches(emptyList(), listOf(duel("paused", status = "paused"), duel("bot", botTurn = true)), "me")
        assertEquals(HomeTurn.WAITING, rows.first { it.id == "paused" }.turn)
        assertEquals(HomeTurn.RIVAL, rows.first { it.id == "bot" }.turn)
    }
    @Test fun finalAndQuizAreResumableAndDuplicatesAreRemoved() {
        assertEquals(2, unifiedHomeMatches(emptyList(), listOf(duel("quiz", status = "quiz"), duel("quiz", status = "quiz"), duel("final", status = "final")), "me").size)
    }
    @Test fun inviteAndMatchLinksRoundTrip() {
        assertEquals(PlayerTarget("invite", "ABCDEF123456"), PlayerLinks.parse("kelimetahti://invite/abcdef123456"))
        listOf("siege", "series", "son_harf").forEach { kind ->
            assertEquals(PlayerTarget(kind,id), PlayerLinks.parse("kelimetahti://$kind/$id"))
        }
        assertEquals(PlayerTarget("activity", null), PlayerLinks.parse("kelimetahti://activity/inbox"))
    }
    @Test fun unrelatedOrMalformedLinksAreRejected() {
        listOf(null, "https://invite/ABCDEF123456", "kelimetahti://invite/not-a-code", "kelimetahti://siege/not-a-uuid", "kelimetahti://invite/ABCDEF123456/extra", "kelimetahti://invite/ABCDEF123456?other=user", "kelimetahti://user@invite/ABCDEF123456").forEach { assertNull(it, PlayerLinks.parse(it)) }
    }
    @Test fun onlyRecentUnreadUsefulEventsProduceNotifications() {
        val now = Instant.parse("2026-10-02T10:00:00Z")
        val event = SocialActivityDto(id,"me",kind="your_turn",targetKind="siege",targetId=id,createdAt="2026-10-02T09:59:00Z")
        assertTrue(MatchNotifications.eligible(event,now))
        assertFalse(MatchNotifications.eligible(event.copy(readAt=now.toString()),now))
        assertFalse(MatchNotifications.eligible(event.copy(kind="friend_online"),now))
        assertFalse(MatchNotifications.eligible(event.copy(createdAt="2026-09-30T09:59:00Z"),now))
        assertFalse(MatchNotifications.eligible(event.copy(createdAt="2026-10-03T09:59:00Z"),now))
    }
    @Test fun matchNotificationsOpenMatchAndInvitationsOpenInbox() {
        val event = SocialActivityDto(id,"me",kind="your_turn",targetKind="son_harf",targetId=id,createdAt="")
        assertEquals(PlayerTarget("son_harf",id),PlayerLinks.parse(MatchNotifications.notificationLink(event)))
        assertEquals(PlayerTarget("activity",null),PlayerLinks.parse(MatchNotifications.notificationLink(event.copy(kind="rematch",targetKind="activity"))))
    }
}
