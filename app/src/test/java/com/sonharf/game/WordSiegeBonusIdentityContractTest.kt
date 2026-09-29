package com.sonharf.game

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test fun eachBonusFamilyHasItsOwnMarkAndShape() {
        val mark = listOf(File("src/main/java/com/sonharf/game/WordSiegeBonusMark.kt"), File("app/src/main/java/com/sonharf/game/WordSiegeBonusMark.kt"))
            .first(File::exists).readText()
        assertTrue(mark.contains("CutCornerShape"))
        assertTrue(mark.contains("RoundedCornerShape(50)"))
        assertTrue(mark.contains("CircleShape"))
        assertTrue(mark.contains("\"2H\" -> \"◆\""))
        assertTrue(mark.contains("\"3K\" -> \"≈+\""))
        assertTrue(mark.contains("WordSiegeBoardSpec.CenterBonus -> \"✦\""))
    }
}
