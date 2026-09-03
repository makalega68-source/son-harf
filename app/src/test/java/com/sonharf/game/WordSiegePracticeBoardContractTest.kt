package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegePracticeBoardContractTest {
    @Test fun `practice board uses readable pannable close and fit modes`() {
        val board = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt").readText()
        val screen = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt").readText()

        assertTrue(board.contains("PracticeSiegeCellSize = 52.dp"))
        assertTrue(board.contains("detectDragGestures"))
        assertTrue(board.contains("detectTapGestures(onDoubleTap"))
        assertTrue(board.contains("wordSiegeFitScale"))
        assertTrue(board.contains("WordSiegeBoardAccessibility.BoardLetterPoint"))
        assertTrue(board.contains("WordSiegeBoardAccessibility.BoardBonus"))
        assertTrue(board.contains("WordSiegeBoardAccessibility.RackPoint"))
        assertTrue(screen.contains("WordSiegePracticeBoard("))
        assertTrue(screen.contains("WordSiegePracticeRackTile("))
        assertFalse(screen.contains("transfer\""))
    }

    @Test fun `practice pending controls preserve backend rack indices`() {
        val screen = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt").readText()
        assertTrue(screen.contains("wordSiegeShuffledRackIndices"))
        assertTrue(screen.contains("wordSiegeUndoPendingPlacement"))
        assertTrue(screen.contains("GERİ AL"))
        assertTrue(screen.contains("KARIŞTIR"))
        assertTrue(screen.contains("wordSiegePracticeMoveNotice"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
