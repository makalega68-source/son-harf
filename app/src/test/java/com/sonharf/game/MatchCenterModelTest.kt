package com.sonharf.game

import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.WordSiegeGameDto
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchCenterModelTest {
    private val rival = ProfileDto(id = "b", displayName = "Deniz", rating = 1320)

    private fun game(
        status: String = "playing",
        current: String? = "a",
        winner: String? = null,
    ) = WordSiegeGameDto(
        id = "g1",
        playerOneId = "a",
        playerTwoId = "b",
        status = status,
        currentPlayerId = current,
        winnerId = winner,
        playerOneWordScore = 40,
        playerTwoWordScore = 55,
        playerOneAreaScore = 30,
        playerTwoAreaScore = 10,
        playerOneArea = 15,
        playerTwoArea = 5,
        lastMoveAt = "2026-09-24T10:00:00Z",
    )

    @Test
    fun `total score is word points plus territory points from the server row`() {
        val card = siegeMatchCard(game(), "a", mapOf("b" to rival))
        assertEquals(70, card.myTotal)
        assertEquals(65, card.rivalTotal)
        // Lower word points can still lead on total thanks to territory.
        assertTrue(card.myWordScore < card.rivalWordScore && card.myTotal > card.rivalTotal)
        assertEquals("Deniz", card.rivalName)
        assertEquals(1320, card.rivalRating)
        assertTrue(card.myTurn)
    }

    @Test
    fun `scores are mirrored for player two`() {
        val card = siegeMatchCard(game(current = "a"), "b", emptyMap())
        assertEquals(65, card.myTotal)
        assertEquals(70, card.rivalTotal)
        assertFalse(card.myTurn)
    }

    @Test
    fun `map control is the share of board cells`() {
        val card = siegeMatchCard(game(), "a", emptyMap())
        assertEquals(((15 * 100f) / WordSiegeBoardSpec.CellCount).toInt(), card.myMapControl)
        assertEquals(((5 * 100f) / WordSiegeBoardSpec.CellCount).toInt(), card.rivalMapControl)
    }

    @Test
    fun `result reflects the winner`() {
        assertNull(siegeMatchCard(game(), "a", emptyMap()).result)
        assertEquals("win", siegeMatchCard(game("finished", null, "a"), "a", emptyMap()).result)
        assertEquals("loss", siegeMatchCard(game("finished", null, "b"), "a", emptyMap()).result)
        assertEquals("draw", siegeMatchCard(game("finished", null, null), "a", emptyMap()).result)
    }

    @Test
    fun `elapsed label counts minutes hours and days`() {
        val now = Instant.parse("2026-09-24T12:00:00Z")
        assertEquals("5 dk önce", siegeElapsedLabel("2026-09-24T11:55:00Z", now))
        assertEquals("2 sa önce", siegeElapsedLabel("2026-09-24T10:00:00Z", now))
        assertEquals("3 gün önce", siegeElapsedLabel("2026-09-21T12:00:00Z", now))
        assertNull(siegeElapsedLabel("not-a-date", now))
        assertNull(siegeElapsedLabel(null, now))
    }
}
