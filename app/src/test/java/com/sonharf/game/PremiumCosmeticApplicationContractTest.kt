package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the cosmetic paths that are visible in the live Premier match and store. */
class PremiumCosmeticApplicationContractTest {
    @Test
    fun premierMatchUsesTheEquippedKeyboardAndNameStyle() {
        val premier = source("PremierWordDuelScreen.kt")
        val keyboard = source("SharedInputPrimitives.kt")
        assertTrue(premier.contains("EmbeddedWordKeyboard("))
        assertTrue(premier.contains("nameColor = SonHarfCosmetics.playerNameColor"))
        assertTrue(keyboard.contains("val palette = SonHarfCosmetics.keyboardPalette"))
        assertTrue(keyboard.contains("SonHarfCosmetics.keyboardThemeId"))
        assertTrue(keyboard.contains("overlayColor = palette.key"))
        assertTrue(keyboard.contains("overlayColor = palette.keyAlt"))
        assertTrue(keyboard.contains("overlayColor = palette.action"))
        assertTrue(keyboard.contains("PurchasedUiAsset.BUTTON_BLUE"))
        assertTrue(keyboard.contains("PurchasedUiAsset.BUTTON_GREEN"))
    }

    @Test
    fun storeKeyboardAndThemeCardsUseRuntimeBackedArtworkInsteadOfScreenshots() {
        val preview = source("StoreProductPreview.kt")
        assertTrue(preview.contains("RealKeyboardPreview(item.id, expanded)"))
        assertTrue(preview.contains("SonHarfCosmetics.keyboardPaletteFor(itemId)"))
        assertTrue(preview.contains("BlackThemePreview(expanded)"))
        assertTrue(preview.contains("BLACK THEME"))
        assertFalse(preview.contains("browser screenshot"))
        assertFalse(preview.contains("RealDarkArenaThemePreview"))
    }

    private fun source(name: String): String = File("src/main/java/com/sonharf/game/$name").readText()
}
