package com.sonharf.game

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class StyleAndOfflineSiegeRegressionTest {
    private fun read(path: String) = File(path).readText()

    @Test
    fun practiceUsesPersistentCanonicalDictionaryWithoutDivergentFallback() {
        val dictionary = read("src/main/java/com/sonharf/game/data/SharedDictionaryService.kt")
        val practice = read("src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt")

        assertTrue(dictionary.contains("get_dictionary_snapshot_v2"))
        assertTrue(dictionary.contains("preloadCanonical"))
        assertTrue(dictionary.contains("restorePersisted"))
        assertFalse(dictionary.contains("offlinePracticeTurkish"))
        assertFalse(dictionary.contains("offlinePracticeWords"))
        assertTrue(practice.contains("SharedDictionaryService.preloadCanonical"))
        assertTrue(practice.contains("dictionaryReady"))
        assertTrue(practice.contains("ANA SÖZLÜK"))
        assertTrue(practice.contains("showRestart"))
        assertTrue(practice.contains("Mevcut alıştırmadaki ilerleme sıfırlanacak"))
    }

    @Test
    fun styleShowsBackendProfileFramesAndNeverOverlaysBrokenImage() {
        val frames = read("src/main/java/com/sonharf/game/PurchasedStyleUi.kt")

        assertTrue(frames.contains("BitmapFactory.decodeStream"))
        assertTrue(frames.contains("SafeFrameArtwork"))
        assertTrue(frames.contains("it.kind == \"profile_frame\""))
        assertFalse(frames.contains("it.id in PurchasedFrameCatalog.ids"))
        assertTrue(frames.contains("legacyFrameSpec"))
        assertFalse(frames.contains("verifiedStagedFrameIds"))
        assertTrue(frames.contains("spec.id in inventory || equippedId == spec.id"))
        assertTrue(frames.contains("equipped -> Icon(Icons.Rounded.CheckCircle"))
        assertFalse(frames.contains("BrokenImage"))
    }
}
