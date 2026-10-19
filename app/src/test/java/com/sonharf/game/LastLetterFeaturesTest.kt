package com.sonharf.game

import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.GameWordDto
import org.junit.Assert.*
import org.junit.Test

class LastLetterFeaturesTest {
    private fun room(id: String, winner: String?, parent: String? = null, swapped: Boolean = false) = GameRoomDto(
        id = id, code = "ABC123", hostId = if (swapped) "b" else "a", guestId = if (swapped) "a" else "b",
        status = "finished", winnerId = winner, rematchOf = parent,
    )

    @Test fun threeSeparateRematchesAggregateAcrossHostChangesAndUnorderedHistory() {
        val first = room("one", "a")
        val second = room("two", "b", "one", swapped = true)
        val third = room("three", "a", "two")
        val summary = lastLetterSeries(third, listOf(second, first), "a")!!
        assertEquals(3, summary.matchNumber)
        assertEquals(3, summary.played)
        assertEquals(2, summary.myWins)
        assertEquals(1, summary.rivalWins)
        assertTrue(summary.complete)
        val reverse = lastLetterSeries(third, listOf(first, second), "b")!!
        assertEquals(1, reverse.myWins)
        assertEquals(2, reverse.rivalWins)
    }

    @Test fun fourthMatchStartsANewThreeMatchSeriesWithoutResettingMatchScores() {
        val all = listOf(room("1", "a"), room("2", "a", "1"), room("3", "b", "2"))
        val fourth = room("4", "b", "3")
        val summary = lastLetterSeries(fourth, all, "a")!!
        assertEquals(1, summary.matchNumber)
        assertEquals(1, summary.played)
        assertEquals(0, summary.myWins)
        assertEquals(1, summary.rivalWins)
        assertFalse(summary.complete)
    }

    @Test fun seriesNeverInventsAResultForMissingForeignOrCyclicHistory() {
        val current = room("two", "a", "one")
        assertNull(lastLetterSeries(current, emptyList(), "a"))
        assertNull(lastLetterSeries(current, listOf(room("one", "b").copy(guestId = "c")), "a"))
        assertNull(lastLetterSeries(current, listOf(room("one", "b", "two")), "a"))
        assertNull(lastLetterSeries(room("one", "a").copy(isBot = true), emptyList(), "a"))
        assertNull(lastLetterSeries(room("one", "a"), emptyList(), "outsider"))
    }

    @Test fun dailyClockUsesServerTimeAndDoesNotRestartWhenReturningFromBackground() {
        val now = "2026-10-19T10:00:00Z"
        val end = "2026-10-19T10:01:00Z"
        assertEquals(60, dailyLastLetterRemaining(end, now, 0))
        assertEquals(59, dailyLastLetterRemaining(end, now, 1_250))
        assertEquals(20, dailyLastLetterRemaining(end, now, 40_000))
        assertEquals(0, dailyLastLetterRemaining(end, now, 61_000))
        assertEquals(0, dailyLastLetterRemaining("invalid", now, 0))
    }

    @Test fun obiReportsActualWordLengthAndOnlyAnnouncesANewPersonalRecordOrStreak() {
        val record = lastLetterObservation("ihtimal", listOf("kalem", "masa"), 1, "tr")!!
        assertTrue(record.contains("İHTİMAL"))
        assertTrue(record.contains("7 harf"))
        assertFalse(record.contains("puan"))
        assertNull(lastLetterObservation("kalem", listOf("ihtimal"), 3, "tr"))
        assertTrue(lastLetterObservation("kalem", listOf("ihtimal"), 5, "tr")!!.contains("5 geçerli kelime"))
        assertTrue(lastLetterObservation("forest", listOf("leaf"), 1, "en")!!.contains("6 letters"))
    }

    @Test fun matchNoteNeverAttributesABotOrRivalWordToThePlayer() {
        fun word(id: Long, player: String?, value: String, bot: Boolean = false) = GameWordDto(
            id = id, roomId = "room", playerId = player, word = value, normalizedWord = value,
            createdAt = "2026-10-19T10:00:00Z", isBot = bot,
        )
        val words = listOf(word(1, "a", "kalem"), word(2, "b", "kıvılcım"), word(3, "a", "orman"), word(4, null, "arkadaşlık", true))
        assertEquals("kalem", lastLetterLongest(words, "a"))
        assertEquals("kıvılcım", lastLetterLongest(words, "b"))
        assertNull(lastLetterLongest(words, null))
    }

    @Test fun shortDefinitionsNeverSubstituteAWordThatWasNotPlayed() {
        assertNotNull(LastLetterDiscoveries.meaning("KALEM", "tr"))
        assertNotNull(LastLetterDiscoveries.meaning("FOREST", "en"))
        assertNull(LastLetterDiscoveries.meaning("madeupwordxyz", "en"))
        assertNull(LastLetterDiscoveries.meaning("kalem", "en"))
    }
}
