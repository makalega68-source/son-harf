package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegePracticeBoardContractTest {
    @Test fun `practice board uses exact cell taps with pannable close and fit modes`() {
        val board = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt").readText()
        val screen = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt").readText()

        assertTrue(board.contains("PracticeSiegeCellSize = 52.dp"))
        assertTrue(board.contains("WordSiegeBoardSpec.Size"))
        assertTrue(board.contains("detectDragGestures"))
        assertTrue(board.contains("combinedClickable"))
        assertTrue(board.contains("WordSiegeBoardTapAction.TOGGLE_VIEWPORT"))
        assertFalse(board.contains("boardIndexAt"))
        assertFalse(board.contains("Çift dokun:"))
        assertTrue(board.contains("wordSiegeBoardTransform"))
        assertTrue(board.contains("clampWordSiegeBoardPan"))
        assertTrue(board.contains("clipToBounds"))
        assertTrue(board.contains("onGloballyPositioned"))
        assertTrue(board.contains("WordSiegeBoardAccessibility.BoardLetterPoint"))
        assertTrue(board.contains("WordSiegeBoardAccessibility.BoardBonus"))
        assertTrue(board.contains("WordSiegeBoardAccessibility.RackPoint"))
        assertTrue(screen.contains("WordSiegePracticeBoard("))
        assertTrue(screen.contains("WordSiegePracticeRackTile("))
        assertFalse(screen.contains("transfer\""))
    }

    @Test fun `practice pending controls preserve rack indices`() {
        val screen = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt").readText()
        assertTrue(screen.contains("wordSiegeShuffledRackIndices"))
        assertTrue(screen.contains("wordSiegeUndoPendingPlacement"))
        assertTrue(screen.contains("GERİ AL"))
        assertTrue(screen.contains("KARIŞTIR"))
        assertTrue(screen.contains("wordSiegePracticeMoveNotice"))
    }

    @Test fun `new practice games deal from the canonical shuffled bag instead of fixed racks`() {
        val engine = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeEngine.kt").readText()
        val spec = projectFile("app/src/main/java/com/sonharf/game/WordSiegeBoardSpec.kt").readText()
        assertTrue(engine.contains("WordSiegeBoardSpec.shuffledBag"))
        assertTrue(engine.contains("playerRack = shuffled.take(7)"))
        assertTrue(engine.contains("botRack = shuffled.drop(7).take(7)"))
        assertTrue(spec.contains("canonicalBag"))
        assertTrue(spec.contains("shuffle(random)"))
        assertFalse(engine.contains("playerRack = if (english) \"PLANETS\" else \"KALEMTR\""))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
