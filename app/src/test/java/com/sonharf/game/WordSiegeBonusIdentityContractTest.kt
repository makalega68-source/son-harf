package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class WordSiegeBonusIdentityContractTest {
    @Test fun bonusCellsUseTheBoardsOwnNamesInBothLanguages() {
        assertEquals("Harf Bonus ×2", WordSiegeBoardSpec.bonusLongName("2H", true))
        assertEquals("Harf Bonus ×3", WordSiegeBoardSpec.bonusLongName("3H", true))
        assertEquals("Kelime Bonus ×2", WordSiegeBoardSpec.bonusLongName("2K", true))
        assertEquals("Kelime Bonus ×3", WordSiegeBoardSpec.bonusLongName("3K", true))
        assertEquals("Başlangıç ×4", WordSiegeBoardSpec.bonusLongName("4K", true))
        assertEquals("Word Bonus ×3", WordSiegeBoardSpec.bonusLongName("3K", false))
        listOf("2H", "3H", "2K", "3K", "4K").forEach { code ->
            listOf(true, false).forEach { tr ->
                assertFalse(WordSiegeBoardSpec.displayBonusLabel(code, tr).contains("×"))
            }
        }
    }

    @Test fun persistedCodesAndScoringStayTheSame() {
        assertEquals("4K", WordSiegeBoardSpec.CenterBonus)
        assertEquals("3Y", WordSiegeBoardSpec.StarBonus)
        assertEquals(25, WordSiegeBoardSpec.StarBonusPoints)
    }

    @Test fun multiplierCellsUseOnlyTheRequestedSuperscriptLabels() {
        val labels = mapOf("2H" to "HB²", "3H" to "HB³", "2K" to "KB²", "3K" to "KB³")
        labels.forEach { (code, expected) ->
            listOf(true, false).forEach { turkish ->
                assertEquals(expected, WordSiegeBoardSpec.displayBonusLabel(code, turkish))
            }
        }
        assertEquals("", WordSiegeBoardSpec.displayBonusLabel(WordSiegeBoardSpec.CenterBonus))
        assertEquals("+25", WordSiegeBoardSpec.displayBonusLabel(WordSiegeBoardSpec.StarBonus))
    }
}
