package com.sonharf.game

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the cosmetic paths that are visible in the live Premier match. */
class PremiumCosmeticApplicationContractTest {
    @Test
    fun premierMatchUsesTheEquippedKeyboardAndNameStyle() {
        val premier = source("PremierWordDuelScreen.kt")

        assertTrue(premier.contains("val palette = SonHarfCosmetics.keyboardPalette"))
        assertTrue(premier.contains("color = palette.background"))
        assertTrue(premier.contains("nameColor = SonHarfCosmetics.playerNameColor"))
    }

    @Test
    fun storeKeyboardCardsUseCleanPremiumArtworkInsteadOfScreenshots() {
        val preview = source("StoreProductPreview.kt")

        assertTrue(preview.contains("PremiumKeyboardProductArtwork(itemId"))
        assertTrue(preview.contains("browser screenshot"))
    }

    private fun source(name: String): String = File("src/main/java/com/sonharf/game/$name").readText()
}
