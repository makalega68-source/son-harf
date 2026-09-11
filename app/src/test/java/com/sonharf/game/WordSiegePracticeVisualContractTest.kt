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

    @Test fun practiceBoardUsesTerritoryFirstPaletteStrategicZonesAndThreatBorders() {
        val text = source()
        assertTrue(text.contains("PracticeSiegeNeutral = Color(0xFFFAF7EF)"))
        assertTrue(text.contains("PracticeSiegeMine = Color(0xFFA8C7B1)"))
        assertTrue(text.contains("PracticeSiegeRival = Color(0xFFAFCDE0)"))
        assertTrue(text.contains("PracticeSiegeThreat = Color(0xFFD8903D)"))
        assertTrue(text.contains("PracticeZoneWatch"))
        assertTrue(text.contains("PracticeZoneCritical"))
        assertTrue(text.contains("PracticeZoneFort"))
        assertTrue(text.contains("PracticeZoneSiege"))
        assertTrue(text.contains("PracticeZoneCrown"))
        assertTrue(text.contains("WordSiegeBoardSpec.displayBonusLabel(activeZone)"))
        assertTrue(text.contains("practiceCellThreatened"))
        assertTrue(text.contains("val regionGap = if (owner != 0) .45.dp else 1.15.dp"))
        assertTrue(text.contains("highlightAlpha.animateTo(0.42f"))
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
