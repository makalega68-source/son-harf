package com.sonharf.game.data

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedDictionaryServiceTest {
    @After
    fun cleanup() = SharedDictionaryService.clearForTests()

    @Test
    fun turkishNormalizationAndCanonicalParityIncludesTwoLetterWords() {
        SharedDictionaryService.installSnapshotForTests(
            "tr",
            listOf("ar", "al", "el", "kar", "mal", "sema", "ter", "masa", "kalem", "kara", "sel", "ser"),
        )

        listOf("AR", "AL", "EL", "KAR", "Kar", "kar", "MAL", "SEMA", "TER", "MASA", "KALEM", "KARA", "SEL", "SER").forEach { word ->
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
    fun englishUsesSameApiWithSeparateProductionDataset() {
        SharedDictionaryService.installSnapshotForTests(
            "en",
            listOf("cat", "dog", "house", "game", "word", "play", "water", "light", "world", "friend"),
        )

        listOf("CAT", "DOG", "HOUSE", "GAME", "WORD", "PLAY", "WATER", "LIGHT", "WORLD", "FRIEND").forEach { word ->
            assertTrue("Expected valid: $word", SharedDictionaryService.isValidCached(word, "en") == true)
            assertTrue("Expected blocking canonical valid: $word", SharedDictionaryService.isValidWordBlocking(word, "en"))
        }
        assertFalse(SharedDictionaryService.isValidCached("MAKALEB", "en") == true)
    }

    @Test
    fun turkishAndEnglishSnapshotsNeverCrossFallback() {
        SharedDictionaryService.installSnapshotForTests("tr", listOf("el", "sel", "ışık"))
        SharedDictionaryService.installSnapshotForTests("en", listOf("cat", "word", "light"))

        assertTrue(SharedDictionaryService.isValidCached("EL", "tr") == true)
        assertFalse(SharedDictionaryService.isValidCached("EL", "en") == true)
        assertTrue(SharedDictionaryService.isValidCached("CAT", "en") == true)
        assertFalse(SharedDictionaryService.isValidCached("CAT", "tr") == true)
    }
}
