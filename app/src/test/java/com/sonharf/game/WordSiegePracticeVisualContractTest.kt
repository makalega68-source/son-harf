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

    @Test fun practiceBoardUsesLivelyCalmBonusSurfacesAndSubtleLatestMoveHighlight() {
        val text = source()
        assertTrue(text.contains("PracticeSiegeBoardSurface = Color(0xFFD9E4E7)"))
        assertTrue(text.contains("PracticeSiegeNeutral = Color(0xFFF8FAF9)"))
        assertTrue(text.contains("PracticeBonus2H"))
        assertTrue(text.contains("PracticeBonus3H"))
        assertTrue(text.contains("PracticeBonus2K"))
        assertTrue(text.contains("PracticeBonus3K"))
        assertTrue(text.contains("PracticeBonus4K"))
        assertTrue(text.contains("PracticeBonusStar"))
        assertTrue(text.contains("WordSiegeBoardSpec.displayBonusLabel(activeBonus)"))
        assertTrue(text.contains("highlightAlpha.animateTo(0.42f"))
        assertTrue(text.contains(".padding(1.25.dp)"))
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
