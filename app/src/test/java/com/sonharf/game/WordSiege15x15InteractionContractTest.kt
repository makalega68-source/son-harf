package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiege15x15InteractionContractTest {
    @Test fun `online match delegates to the same immutable-index 15x15 board`() {
        val online = projectFile("app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt").readText()
        val board = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt").readText()

        assertTrue(online.contains("WordSiegePracticeBoard("))
        assertTrue(online.contains("onCell = onBoardCell"))
        assertExplicitBoardOriginBeforeRequiredSize(
            board,
            "requiredSize(PracticeSiegeCellSize * WordSiegeBoardSpec.Size)",
        )
        assertTrue(board.contains("val index = WordSiegeBoardSpec.index(row, column)"))
        assertTrue(board.contains("onClick = { onCell(index) }"))
        assertTrue(board.contains("onDoubleClick = ::toggleMode"))
        assertTrue(board.contains(".combinedClickable("))
        assertTrue(board.contains("clampWordSiegeBoardPan"))
        assertTrue(board.contains("WordSiegeBoardSpec.CenterIndex"))
        assertFalse(board.contains("detectTapGestures"))
        assertFalse(board.contains("Çift dokun:"))
    }

    @Test fun `practice board maps each rendered cell directly to its immutable board index`() {
        val source = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt").readText()

        assertExplicitBoardOriginBeforeRequiredSize(
            source,
            "requiredSize(PracticeSiegeCellSize * WordSiegeBoardSpec.Size)",
        )
        assertTrue(source.contains("val index = WordSiegeBoardSpec.index(row, column)"))
        assertTrue(source.contains("onClick = { onCell(index) }"))
        assertTrue(source.contains("onDoubleClick = ::toggleMode"))
        assertTrue(source.contains("WordSiegeZoneRules.zoneIdForIndex(index)"))
        assertTrue(source.contains("fortress = WordSiegeZoneRules.isFortress(zoneId)"))
        assertFalse(source.contains("Çift dokun:"))
    }

    private fun assertExplicitBoardOriginBeforeRequiredSize(source: String, requiredSize: String) {
        val explicitOrigin = source.indexOf("wrapContentSize(Alignment.TopStart, unbounded = true)")
        val oversizedBoard = source.indexOf(requiredSize)
        assertTrue("Oversized board must have an explicit top-left layout origin", explicitOrigin >= 0)
        assertTrue("Explicit origin must wrap requiredSize to prevent implicit centering", oversizedBoard > explicitOrigin)
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
