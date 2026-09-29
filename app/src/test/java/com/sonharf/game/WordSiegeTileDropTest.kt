package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Test

/** Drag-and-drop: where pending tiles end up after a drop. */
class WordSiegeTileDropTest {
    @Test fun droppingTilesFollowsDragRules() {
        val board = List(WordSiegeBoardSpec.CellCount) { com.sonharf.game.data.WordSiegeCellDto() }
        // Rack → empty cell.
        assertEquals(mapOf(10 to 2), wordSiegeDropTile(emptyMap(), board, 2, null, 10))
        // Board → board moves the tile.
        assertEquals(mapOf(11 to 2), wordSiegeDropTile(mapOf(10 to 2), board, 2, 10, 11))
        // Board → off the board returns it to the rack.
        assertEquals(emptyMap<Int, Int>(), wordSiegeDropTile(mapOf(10 to 2), board, 2, 10, null))
        // Board → another pending tile swaps them.
        assertEquals(mapOf(11 to 2, 10 to 5), wordSiegeDropTile(mapOf(10 to 2, 11 to 5), board, 2, 10, 11))
        // Rack → a pending tile sends the old one back to the rack.
        assertEquals(mapOf(11 to 3), wordSiegeDropTile(mapOf(11 to 5), board, 3, null, 11))
        // A filled cell never takes a tile.
        val filled = board.toMutableList().also { it[12] = com.sonharf.game.data.WordSiegeCellDto(letter = "A") }
        assertEquals(emptyMap<Int, Int>(), wordSiegeDropTile(emptyMap(), filled, 1, null, 12))
    }
}
