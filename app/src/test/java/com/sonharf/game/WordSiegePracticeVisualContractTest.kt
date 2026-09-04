package com.sonharf.game

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class WordSiegePracticeVisualContractTest {
    private fun source(): String {
        val direct = File("src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt")
        val fromRoot = File("app/src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt")
        return when {
            direct.exists() -> direct.readText()
            fromRoot.exists() -> fromRoot.readText()
            else -> error("Missing WordSiegePracticeBoard.kt")
        }
    }

    @Test fun practiceBoardUsesCalmBackgroundAndBorderlessCellSeparation() {
        val text = source()
        assertTrue(text.contains("PracticeSiegeBoardSurface = Color(0xFFDDE6EB)"))
        assertTrue(text.contains("PracticeSiegeNeutral = Color(0xFFF8FAF9)"))
        assertTrue(text.contains(".padding(1.6.dp)"))

        val cellStart = text.indexOf("private fun WordSiegePracticeBoardCell")
        val rackStart = text.indexOf("internal fun WordSiegePracticeRackTile")
        assertTrue(cellStart >= 0 && rackStart > cellStart)
        val cellSection = text.substring(cellStart, rackStart)
        assertFalse(cellSection.contains("BorderStroke("))
        assertFalse(cellSection.contains("wordSiegeBoardBorderWidthDp"))
    }

    @Test fun visualChangePreservesViewportVfxAndInteractionContracts() {
        val text = source()
        assertTrue(text.contains("PurchasedBoardActionVfxOverlay("))
        assertTrue(text.contains("WordSiegeBoardViewportMode.CLOSE"))
        assertTrue(text.contains("wordSiegeBoardTransform("))
        assertTrue(text.contains("detectTransformGestures"))
        assertTrue(text.contains("WordSiegeBoardTapAction.TOGGLE_VIEWPORT"))
        assertTrue(text.contains("WordSiegeBoardTapAction.PLACE"))
    }
}
