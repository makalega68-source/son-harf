package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LetharaLoreTest {
    @Test
    fun sixSealsAreCanonicalAndUnique() {
        assertEquals(6, LetharaLore.characters.size)
        assertEquals(listOf("Lyra", "Kael", "Neris", "Ryvan", "Mivo", "Selen"), LetharaLore.characters.map { it.name })
        assertEquals(6, LetharaLore.characters.map { it.key }.toSet().size)
    }

    @Test
    fun runtimeMascotsMapToCanonicalCharacters() {
        assertEquals("Neris", LetharaLore.characterForMascot(MascotCatalog.DEFAULT_ID).name)
        assertEquals("Neris", LetharaLore.characterForMascot(MascotCatalog.CHIBI_WIZARD_ID).name)
        assertEquals("Lyra", LetharaLore.characterForMascot(MascotCatalog.LEGACY_WHITE_ID).name)
    }

    @Test
    fun storyUnlocksProgressForward() {
        val levels = LetharaLore.chapters.map { it.unlockLevel }
        assertTrue(levels.zipWithNext().all { (a, b) -> b > a })
        assertEquals(1, levels.first())
        assertTrue(levels.last() >= 30)
    }

    @Test
    fun mascotProgressionDoesNotDefineMatchPower() {
        val loreText = (LetharaLore.chapters.map { it.bodyTr } + LetharaLore.chapters.map { it.bodyEn }).joinToString(" ")
        assertTrue(loreText.contains("PvP", ignoreCase = true))
        assertFalse(loreText.contains("puan bonusu", ignoreCase = true))
    }
}
