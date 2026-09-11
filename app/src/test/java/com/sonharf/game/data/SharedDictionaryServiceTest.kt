package com.sonharf.game.data

import org.junit.After
import org.junit.Assert.assertEquals
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
        listOf("IŞIK", "ışık", "İSİM", "isim", "GÜL", "ŞİŞE", "ÖLÇÜ").forEach { word ->
            assertTrue("Turkish locale normalization failed: $word", SharedDictionaryService.isValidCached(word, "tr") == true)
        }
        assertFalse("Terminal soft-ğ must never enter the playable snapshot", SharedDictionaryService.isValidCached("ÇIĞ", "tr") == true)
    }

    @Test
    fun invalidSourceLeakIsRemovedFromAllLocalValidationPaths() {
        SharedDictionaryService.installSnapshotForTests("tr", listOf("amlat", "kalem", "masa"))

        assertFalse("AMLAT must never survive a stale or test snapshot", SharedDictionaryService.isValidCached("AMLAT", "tr") == true)
        assertFalse("AMLAT must never be accepted by blocking practice validation", SharedDictionaryService.isValidWordBlocking("AMLAT", "tr"))
        assertTrue(SharedDictionaryService.isValidWordBlocking("KALEM", "tr"))
    }

    @Test
    fun sensitiveWordsStayHumanValidButAreNeverBotCandidates() {
        SharedDictionaryService.installSnapshotForTests("tr", listOf("am", "penis", "sik", "kalem", "masa"))

        listOf("AM", "PENİS", "SİK").forEach { word ->
            assertTrue("Human player should retain canonical dictionary access: $word", SharedDictionaryService.isValidWordBlocking(word, "tr"))
            assertFalse("Bot must not choose sensitive word: $word", SharedDictionaryService.isBotAllowedWord(word, "tr"))
        }
        assertTrue(SharedDictionaryService.isBotAllowedWord("KALEM", "tr"))
    }

    @Test
    fun turkishBotUsesOnlyVerifiedHeadwordSubset() {
        SharedDictionaryService.installSnapshotForTests(
            language = "tr",
            words = listOf("kalem", "zir", "bağın", "masa"),
            botWords = listOf("kalem", "zir", "masa"),
        )

        assertTrue(SharedDictionaryService.isValidWordBlocking("BAĞIN", "tr"))
        assertFalse("Canonical-only corpus entries must not leak into AI play", SharedDictionaryService.isBotAllowedWord("BAĞIN", "tr"))
        assertTrue("Exact verified headword must remain available to AI", SharedDictionaryService.isBotAllowedWord("ZİR", "tr"))
    }

    @Test
    fun tdkHeadwordBuilderIntersectsCanonicalAndAppliesBotSafetyPolicy() {
        val verified = SharedDictionaryService.buildTurkishBotSnapshot(
            canonicalWords = setOf("kalem", "zir", "bağın", "penis", "amlat"),
            tdkHeadwords = listOf("kalem", "zir", "penis", "not in canonical", "iki kelime"),
        )

        assertEquals(setOf("kalem", "zir"), verified)
    }

    @Test
    fun invisibleDictionaryArtifactsAreRemovedBeforeCanonicalLookup() {
        assertEquals("istanbul", SharedDictionaryService.normalize("\uFEFFİSTANBUL\r\n", "tr"))
        assertEquals("kalem", SharedDictionaryService.normalize("KA\u200BLEM", "tr"))
        assertEquals("masa", SharedDictionaryService.normalize("\u00A0MASA\u00A0", "tr"))
        assertEquals("isim", SharedDictionaryService.normalize("\tİSİM\t", "tr"))

        SharedDictionaryService.installSnapshotForTests("tr", listOf("istanbul", "kalem", "masa", "isim"))
        listOf("\uFEFFİSTANBUL\r", "KA\u200BLEM", "\u00A0MASA\u00A0", "\tİSİM\n").forEach { word ->
            assertTrue("Dirty UTF-8 artifact should not break lookup: $word", SharedDictionaryService.isValidCached(word, "tr") == true)
            assertTrue("Blocking lookup must use the same cleanup: $word", SharedDictionaryService.isValidWordBlocking(word, "tr"))
        }
    }

    @Test
    fun unicodeEquivalentSpellingsUseSameCanonicalKeyAsServer() {
        val decomposed = "c\u0327ilek"
        assertEquals("çilek", SharedDictionaryService.normalize(decomposed, "tr"))

        SharedDictionaryService.installSnapshotForTests("tr", listOf("çilek"))
        assertTrue(SharedDictionaryService.isValidCached(decomposed, "tr") == true)
        assertTrue(SharedDictionaryService.isValidWordBlocking(decomposed, "tr"))
    }

    @Test
    fun noSnapshotNeverPretendsCanonicalWordIsValidFromSmallerList() {
        SharedDictionaryService.clearForTests()
        assertFalse(SharedDictionaryService.hasSnapshot("tr"))
        assertFalse(SharedDictionaryService.isValidWordBlocking("SEL", "tr"))
        assertTrue(SharedDictionaryService.practiceCandidates("tr", "SELA", 100).isEmpty())
    }

    @Test
    fun englishUsesSameApiWithSeparateProductionDataset() {
        SharedDictionaryService.installSnapshotForTests(
            "en",
            listOf("cat", "dog", "house", "game", "word", "play", "water", "light", "world", "friend", "apple"),
        )

        listOf("CAT", "DOG", "HOUSE", "GAME", "WORD", "PLAY", "WATER", "LIGHT", "WORLD", "FRIEND", "APPLE").forEach { word ->
            assertTrue("Expected valid: $word", SharedDictionaryService.isValidCached(word, "en") == true)
            assertTrue("Expected blocking canonical valid: $word", SharedDictionaryService.isValidWordBlocking(word, "en"))
        }
        assertFalse(SharedDictionaryService.isValidCached("MAKALEB", "en") == true)
    }

    @Test
    fun turkishAndEnglishSnapshotsNeverCrossFallback() {
        SharedDictionaryService.installSnapshotForTests("tr", listOf("el", "sel", "ışık", "kalem"))
        SharedDictionaryService.installSnapshotForTests("en", listOf("cat", "word", "light", "apple"))

        assertTrue(SharedDictionaryService.isValidCached("EL", "tr") == true)
        assertFalse(SharedDictionaryService.isValidCached("EL", "en") == true)
        assertTrue(SharedDictionaryService.isValidCached("CAT", "en") == true)
        assertFalse(SharedDictionaryService.isValidCached("CAT", "tr") == true)
        assertTrue(SharedDictionaryService.isValidCached("KALEM", "tr") == true)
        assertFalse(SharedDictionaryService.isValidCached("KALEM", "en") == true)
        assertTrue(SharedDictionaryService.isValidCached("APPLE", "en") == true)
        assertFalse(SharedDictionaryService.isValidCached("APPLE", "tr") == true)
    }

    @Test
    fun canonicalLanguageUsesOnlySupportedGameLanguages() {
        assertEquals("tr", SharedDictionaryService.canonicalLanguage("tr"))
        assertEquals("en", SharedDictionaryService.canonicalLanguage("EN"))
        assertEquals("tr", SharedDictionaryService.canonicalLanguage("de"))
    }
}
