package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeWalnutIvoryContractTest {
    private fun source(name: String): String = File("src/main/java/com/sonharf/game/$name.kt").readText()

    @Test fun frameAndTilesShareTheMaterialPaletteWithoutChangingBoardGeometry() {
        val style = source("WordSiegeWalnutIvory")
        val online = source("WordSiegePanMatch")
        val practice = source("WordSiegePracticeBoard")
        assertTrue(style.contains("frame = Color(0xFF8A6239)"))
        assertTrue(style.contains("empty = Color(0xFFB48A57)"))
        assertTrue(style.contains("ivory = Color(0xFFFAF3E3)"))
        assertTrue(style.contains("mine = Color(0xFF2E9A62)"))
        assertTrue(style.contains("rival = Color(0xFFC8473C)"))
        assertTrue(style.contains("val tile = Brush.verticalGradient"))
        assertTrue(style.contains("SonHarfCosmetics.walnutTheme"))
        listOf(online, practice).forEach { board ->
            assertTrue(board.contains("WordSiegeWalnutIvory.boardGrain"))
            assertTrue(board.contains("WordSiegeWalnutIvory.tile"))
            assertTrue(board.contains("4.dp else .75.dp"))
            assertTrue(board.contains("collectIsPressedAsState()"))
            assertTrue(board.contains("val regionGap = 1.25.dp"))
            assertTrue(board.contains("WordSiegeBoardTapAction.PLACE"))
            assertFalse(board.contains("WordSiegeBoardTapAction.TOGGLE_VIEWPORT"))
        }
        assertTrue(online.contains("if (owner == myOwner) Alignment.TopStart else Alignment.TopEnd"))
        assertTrue(practice.contains("if (WordSiegeWalnutIvory.enabled && owner != myOwner) Alignment.TopEnd else Alignment.TopStart"))
        assertTrue(online.contains("private val PanSiegeCellSize = 52.dp"))
        assertTrue(practice.contains("private val PracticeSiegeCellSize = 52.dp"))
    }
}
