package com.sonharf.game

import com.sonharf.game.data.WordSiegeGameDto
import com.sonharf.game.data.WordSiegeMoveDto
import com.sonharf.game.data.WordSiegePlacedTileDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeVipAnalysisTest {
    private val me = "00000000-0000-0000-0000-000000000001"
    private val rival = "00000000-0000-0000-0000-000000000002"

    @Test
    fun normalPlayerSeesOnlyValidTotal() {
        val valid = wordSiegeAnalysisVisibility(vip = false, validPreview = true)
        assertTrue(valid.showTotal)
        assertFalse(valid.showBreakdown)
        assertFalse(valid.showTerritoryPreview)

        val invalid = wordSiegeAnalysisVisibility(vip = false, validPreview = false)
        assertFalse(invalid.showTotal)
        assertFalse(invalid.showBreakdown)
    }

    @Test
    fun vipPlayerGetsBreakdownAndTerritoryPreviewOnlyForValidMove() {
        val valid = wordSiegeAnalysisVisibility(vip = true, validPreview = true)
        assertTrue(valid.showTotal)
        assertTrue(valid.showBreakdown)
        assertTrue(valid.showTerritoryPreview)

        val invalid = wordSiegeAnalysisVisibility(vip = true, validPreview = false)
        assertFalse(invalid.showTotal)
        assertFalse(invalid.showBreakdown)
        assertFalse(invalid.showTerritoryPreview)
    }

    @Test
    fun liveStatsUseOnlyPublicMatchHistoryAndOwnProgress() {
        val game = WordSiegeGameDto(
            id = "game",
            playerOneId = me,
            playerTwoId = rival,
            status = "playing",
            playerOneArea = 6,
            playerTwoArea = 4,
            bag = "GIZLIHARFLER",
        )
        val moves = listOf(
            move(1, me, "ARA", listOf("A", "R", "A"), 8),
            move(2, rival, "TAM", listOf("T", "M"), 7),
            move(3, me, "ARAÇ", listOf("Ç"), 11),
        )

        val stats = wordSiegeLiveStats(game, moves, me)
        assertEquals(6, stats.totalTilesPlayed)
        assertEquals(4, stats.myTilesPlayed)
        assertEquals(2, stats.myWordCount)
        assertEquals(6, stats.myArea)
    }

    @Test
    fun finishedLetterDistributionIsDeterministic() {
        assertEquals("A×2  B×1  Ç×1", wordSiegeLetterDistribution(listOf("A", "B", "A", "Ç")))
    }

    private fun move(
        id: Long,
        player: String,
        word: String,
        letters: List<String>,
        score: Int,
    ) = WordSiegeMoveDto(
        id = id,
        gameId = "game",
        playerId = player,
        primaryWord = word,
        formedWords = listOf(word),
        placedTiles = letters.mapIndexed { index, letter ->
            WordSiegePlacedTileDto(index = index, letter = letter, owner = if (player == me) 1 else 2)
        },
        wordScore = score,
    )
}
