package com.sonharf.game

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class StyleAndOfflineSiegeRegressionTest {
    private fun read(path: String) = File(path).readText()

    @Test
    fun practiceActionsDoNotRequireDictionaryNetwork() {
        val dictionary = read("src/main/java/com/sonharf/game/data/SharedDictionaryService.kt")
        val practice = read("src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt")

        assertTrue(dictionary.contains("offlinePracticeWords"))
        assertTrue(dictionary.contains("snapshots[lang] ?: offlinePracticeWords(lang)"))
        assertFalse(dictionary.substringAfter("fun isValidWordBlocking").substringBefore("fun isValidCached").contains("preload("))
        assertTrue(practice.contains("runCatching { OnlineGameBackend() }.getOrNull()"))
        assertTrue(practice.contains("showRestart"))
        assertTrue(practice.contains("Mevcut alıştırmadaki ilerleme sıfırlanacak"))
    }

    @Test
    fun styleNeverOverlaysBrokenImageAndGuardsUnavailableArtworkActions() {
        val frames = read("src/main/java/com/sonharf/game/PurchasedStyleUi.kt")

        assertTrue(frames.contains("BitmapFactory.decodeStream"))
        assertTrue(frames.contains("SafeFrameArtwork"))
        assertTrue(frames.contains("!assetReady -> Text"))
        assertTrue(frames.contains("equipped -> Icon(Icons.Rounded.CheckCircle"))
        assertFalse(frames.contains("BrokenImage"))
    }
}
