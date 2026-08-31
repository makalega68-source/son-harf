package com.sonharf.game

import com.sonharf.game.data.WordSiegeCellDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WordSiegeDirectionTest {
    private fun board(vararg occupied: Int): List<WordSiegeCellDto> =
        List(81) { index -> WordSiegeCellDto(letter = if (index in occupied) "A" else null) }

    @Test
    fun multipleTilesInSameRowAreHorizontal() {
        assertEquals(WordSiegeDirection.HORIZONTAL, detectWordSiegeDirection(board(), listOf(36, 38)))
    }

    @Test
    fun multipleTilesInSameColumnAreVertical() {
        assertEquals(WordSiegeDirection.VERTICAL, detectWordSiegeDirection(board(), listOf(22, 40)))
    }

    @Test
    fun mixedRowsAndColumnsAreInvalid() {
        assertNull(detectWordSiegeDirection(board(), listOf(36, 46)))
    }

    @Test
    fun existingLetterMaySitBetweenNewHorizontalTiles() {
        assertEquals(WordSiegeDirection.HORIZONTAL, detectWordSiegeDirection(board(37), listOf(36, 38)))
    }

    @Test
    fun singleTileUsesHorizontalExistingWord() {
        assertEquals(WordSiegeDirection.HORIZONTAL, detectWordSiegeDirection(board(36, 37, 38), listOf(39)))
    }

    @Test
    fun singleTileUsesVerticalExistingWord() {
        assertEquals(WordSiegeDirection.VERTICAL, detectWordSiegeDirection(board(13, 22, 31), listOf(40)))
    }

    @Test
    fun isolatedSingleTileHasNoForcedDirection() {
        assertNull(detectWordSiegeDirection(board(), listOf(40)))
    }

    @Test
    fun singleTileAtCrossingUsesLongerExistingAxis() {
        assertEquals(WordSiegeDirection.HORIZONTAL, detectWordSiegeDirection(board(37, 38, 39, 31), listOf(40)))
    }
}
