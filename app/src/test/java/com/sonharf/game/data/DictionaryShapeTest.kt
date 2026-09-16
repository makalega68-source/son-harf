package com.sonharf.game.data

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DictionaryShapeTest {
    @After
    fun cleanup() = SharedDictionaryService.clearForTests()

    @Test
    fun circumflexInputFoldsToUnaccented() {
        SharedDictionaryService.installSnapshotForTests("tr", listOf("kağıt"))
        assertEquals(true, SharedDictionaryService.isValidCached("kâğıt", "tr"))
        assertEquals(true, SharedDictionaryService.isValidCached("kağıt", "tr"))
        assertEquals("kağıt", SharedDictionaryService.normalize("KÂĞIT", "tr"))
    }

    @Test
    fun circumflexIsStrippedInAllPositions() {
        assertEquals("hala", SharedDictionaryService.normalize("hâlâ", "tr"))
        assertEquals("ruzgar", SharedDictionaryService.normalize("rûzgar", "tr"))
        assertEquals("iman", SharedDictionaryService.normalize("îman", "tr"))
    }

    @Test
    fun uppercaseCircumflexAlsoFolds() {
        assertEquals("kagıt", SharedDictionaryService.normalize("KÂGIT", "tr"))
        assertTrue(SharedDictionaryService.normalize("HÂLÂ", "tr") == "hala")
    }

    @Test
    fun invalidInputScoresZero() {
        assertEquals(0, DictionaryEngine.calculatePoints("ki tap", "tr"))
        assertEquals(0, DictionaryEngine.calculatePoints("kitap1", "tr"))
        assertEquals(0, DictionaryEngine.calculatePoints("kitap!", "tr"))
        assertEquals(0, DictionaryEngine.calculatePoints("k", "tr"))
    }

    @Test
    fun validWordScores() {
        // K1 + İ1 + T1 + A1 + P5 = 9
        assertEquals(9, DictionaryEngine.calculatePoints("kitap", "tr"))
    }

    @Test
    fun turkishCaseIsStable() {
        assertEquals(
            DictionaryEngine.calculatePoints("İĞNE", "tr"),
            DictionaryEngine.calculatePoints("iğne", "tr"),
        )
        assertTrue(SharedDictionaryService.hasValidShape("IŞIK", "tr"))
    }
}
