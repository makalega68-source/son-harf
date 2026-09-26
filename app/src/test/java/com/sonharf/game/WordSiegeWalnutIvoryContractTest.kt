package com.sonharf.game

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeWalnutIvoryContractTest {
    private fun source(name: String): String = File("src/main/java/com/sonharf/game/$name.kt").readText()

    @Test fun frameAndTilesShareTheMaterialPaletteWithoutChangingBoardGeometry() {
        val style = source("WordSiegeWalnutIvory")
        val online = source("WordSiegePanMatch")
        val practice = source("WordSiegePracticeBoard")
        assertTrue(style.contains("frame = Color(0xFF503526)"))
        assertTrue(style.contains("ivory = Color(0xFFF7F0E2)"))
        assertTrue(style.contains("mine = Color(0xFF3E8058)"))
        assertTrue(style.contains("rival = Color(0xFFAB534C)"))
        assertTrue(style.contains("val tile = Brush.verticalGradient"))
        listOf(online, practice).forEach { board ->
            assertTrue(board.contains("WordSiegeWalnutIvory.boardGrain"))
            assertTrue(board.contains("WordSiegeWalnutIvory.tile"))
            assertTrue(board.contains("collectIsPressedAsState()"))
            assertTrue(board.contains("val regionGap = 1.25.dp"))
            assertTrue(board.contains("WordSiegeBoardTapAction.PLACE"))
            assertTrue(board.contains("WordSiegeBoardTapAction.TOGGLE_VIEWPORT"))
        }
        assertTrue(online.contains("private val PanSiegeCellSize = 52.dp"))
        assertTrue(practice.contains("private val PracticeSiegeCellSize = 52.dp"))
    }
}
