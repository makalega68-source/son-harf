package com.sonharf.game

import com.sonharf.game.data.WordSiegeGameDto
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class HomeSocialProgressTest {
    private fun game(id: String, status: String = "playing", turn: String? = "me", one: String = "me", two: String? = "rival", winner: String? = null) =
        WordSiegeGameDto(id = id, playerOneId = one, playerTwoId = two, status = status, currentPlayerId = turn, winnerId = winner)

    @Test fun homeNeverOffersAnotherPlayersGameOrFinishedGameAsAnActiveTurn() {
        assertNull(homeTurn(game("x", one = "someone", two = "else"), "me"))
        assertNull(homeTurn(game("x", status = "finished"), "me"))
        assertNull(homeTurn(game("x"), null))
        assertEquals(HomeTurn.WAITING, homeTurn(game("x", status = "waiting", two = null), "me"))
        assertEquals(HomeTurn.YOURS, homeTurn(game("x", one = "rival", two = "me"), "me"))
        assertEquals(HomeTurn.RIVAL, homeTurn(game("x", turn = "rival"), "me"))
    }

    @Test fun bothPoolsAreDeduplicatedAndUrgentTurnsComeFirst() {
        val waiting = game("waiting", status = "waiting")
        val rival = game("rival", turn = "rival")
        val later = game("later").copy(gameMode = "series", turnDeadline = "2026-10-02T16:00:00Z")
        val sooner = game("sooner").copy(turnDeadline = "2026-10-02T15:00:00Z")
        assertEquals(listOf("sooner", "later", "rival", "waiting"), homeGames(listOf(waiting, rival, later, sooner, later, game("done", "finished")), "me").map { it.id })
    }

    @Test fun siegeRecordsRespectPlayerSideAndCountDrawsInTheAverage() {
        val a = game("a", status = "finished", winner = "me").copy(playerOneWordScore = 100, playerOneAreaScore = 20, playerOneArea = 10)
        val b = game("b", status = "finished", one = "rival", two = "me").copy(playerTwoWordScore = 50, playerTwoAreaScore = 10, playerTwoArea = 5)
        val stats = siegeRecordSummary(listOf(a, b, a, game("live"), game("other", "finished", one = "other", two = "someone")), "me")
        assertEquals(2, stats.count)
        assertEquals(1, stats.wins)
        assertEquals(90, stats.averageScore)
        assertEquals(15, stats.finalCubes)
        assertEquals(3, stats.averageControl)
        assertEquals(SiegeRecordSummary(0, 0, 0, 0, 0), siegeRecordSummary(listOf(a), null))
    }

    @Test fun calendarUsesIstanbulAndEveningWorkshopReplacesItsBonusWithDoubleXp() {
        val now = Instant.parse("2026-10-02T15:30:00Z").toEpochMilli() // 18:30 Istanbul
        val events = atelierCalendar(now)
        val zone = ZoneId.of("Europe/Istanbul")
        assertEquals(19, Instant.ofEpochMilli(events.first().first).atZone(zone).hour)
        assertEquals(2.0, events.first().second, 0.0)
        assertEquals(1, events.count { Instant.ofEpochMilli(it.first).atZone(zone).let { t -> t.dayOfMonth == 2 && t.hour == 22 } })
        assertTrue(events.filter { it.second == 2.0 }.all { Instant.ofEpochMilli(it.first).atZone(zone).hour in 19..21 })
        assertEquals(1.5, events.first { Instant.ofEpochMilli(it.first).atZone(zone).hour == 22 }.second, 0.0)
        assertTrue(events.all { it.first >= now })
        assertEquals(events.size, events.map { it.first }.distinct().size)
    }

    @Test fun throneHourUsesExactIstanbulBoundaries() {
        fun instant(value: String) = Instant.parse(value).toEpochMilli()
        assertFalse(throneHourActive(instant("2026-10-09T15:59:59Z")))
        assertTrue(throneHourActive(instant("2026-10-09T16:00:00Z")))
        assertTrue(throneHourActive(instant("2026-10-09T18:59:59Z")))
        assertFalse(throneHourActive(instant("2026-10-09T19:00:00Z")))
    }

    @Test fun productAndModeNamesStayDistinctInBothLanguages() {
        val previous = SonHarfUiState.language
        try {
            SonHarfUiState.language = "tr"
            assertEquals("Kelime Kuşatması", sh("Kelime Kuşatması", "Word Siege"))
            assertEquals("Kelime Tahtı", sh("Kelime Tahtı", "Kelime Tahtı"))
            SonHarfUiState.language = "en"
            assertEquals("Word Siege", sh("Kelime Kuşatması", "Word Siege"))
        } finally { SonHarfUiState.language = previous }
    }
}
