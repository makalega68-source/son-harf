package com.sonharf.game.data

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedDictionaryServiceTest {
    @After
    fun cleanup() = SharedDictionaryService.clearForTests()

    @Test
    fun turkishNormalizationAndCanonicalParity() {
        SharedDictionaryService.installSnapshotForTests(
            "tr",
            listOf("kar", "mal", "sema", "ter", "masa", "kalem", "kara", "sel", "ser"),
        )

        listOf("KAR", "Kar", "kar", "MAL", "SEMA", "TER", "MASA", "KALEM", "KARA", "SEL", "SER").forEach { word ->
            assertTrue("Expected valid: $word", SharedDictionaryService.isValidCached(word, "tr") == true)
            assertTrue("Expected blocking canonical valid: $word", SharedDictionaryService.isValidWordBlocking(word, "tr"))
        }
        assertFalse(SharedDictionaryService.isValidCached("MAKALEB", "tr") == true)

        SharedDictionaryService.installSnapshotForTests("tr", listOf("ışık", "isim", "gül", "şişe", "ölçü", "çığ"))
        listOf("IŞIK", "ışık", "İSİM", "isim", "GÜL", "ŞİŞE", "ÖLÇÜ", "ÇIĞ").forEach { word ->
            assertTrue("Turkish locale normalization failed: $word", SharedDictionaryService.isValidCached(word, "tr") == true)
        }
    }

    @Test
    fun noSnapshotNeverPretendsCanonicalWordIsInvalidFromSmallerList() {
        SharedDictionaryService.clearForTests()
        assertFalse(SharedDictionaryService.hasSnapshot("tr"))
        assertFalse(SharedDictionaryService.isValidWordBlocking("SEL", "tr"))
        assertTrue(SharedDictionaryService.practiceCandidates("tr", "SELA", 100).isEmpty())
    }

    @Test
    fun englishUsesSameApiWithSeparateDataset() {
        SharedDictionaryService.installSnapshotForTests("en", listOf("apple", "table", "water", "planet", "reading"))

        listOf("APPLE", "Apple", "table", "WATER").forEach { word ->
            assertTrue("Expected valid: $word", SharedDictionaryService.isValidCached(word, "en") == true)
        }
        assertFalse(SharedDictionaryService.isValidCached("MAKALEB", "en") == true)
    }
}
