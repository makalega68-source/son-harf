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
            listOf("kar", "mal", "sema", "ter", "masa", "kalem", "kara"),
        )

        listOf("KAR", "Kar", "kar", "MAL", "SEMA", "TER", "MASA", "KALEM", "KARA").forEach { word ->
            assertTrue("Expected valid: $word", SharedDictionaryService.isValidCached(word, "tr") == true)
        }
        assertFalse(SharedDictionaryService.isValidCached("MAKALEB", "tr") == true)

        SharedDictionaryService.installSnapshotForTests("tr", listOf("ışık", "isim", "gül", "şişe", "ölçü", "çığ"))
        listOf("IŞIK", "ışık", "İSİM", "isim", "GÜL", "ŞİŞE", "ÖLÇÜ", "ÇIĞ").forEach { word ->
            assertTrue("Turkish locale normalization failed: $word", SharedDictionaryService.isValidCached(word, "tr") == true)
        }
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
