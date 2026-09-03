package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiege15x15InteractionContractTest {
    @Test fun `online board keeps exact cell index under board transform and double tap never places`() {
        val source = projectFile("app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt").readText()

        assertExplicitBoardOriginBeforeRequiredSize(
            source,
            "requiredSize(PanSiegeCellSize * WordSiegeBoardSpec.Size)",
        )
        assertTrue(source.contains("val index = WordSiegeBoardSpec.index(row, column)"))
        assertTrue(source.contains("onClick = { onCell(index) }"))
        assertTrue(source.contains("onDoubleClick = ::toggleViewport"))
        assertTrue(source.contains(".combinedClickable("))
        assertFalse(source.contains("detectTapGestures"))
        assertFalse(source.contains("Çift dokun:"))
        assertTrue(source.contains("clampWordSiegeBoardPan"))
        assertTrue(source.contains("WordSiegeBoardSpec.CenterIndex"))
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
