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

        assertTrue(premier.contains("val palette = SonHarfCosmetics.keyboardPalette"))
        assertTrue(premier.contains("color = palette.background"))
        assertTrue(premier.contains("nameColor = SonHarfCosmetics.playerNameColor"))
    }

    @Test
    fun storeKeyboardCardsUseTextFreeCanvaAlignedArtworkInsteadOfScreenshots() {
        val preview = source("StoreProductPreview.kt")

        listOf(
            "R.drawable.store_art_keyboard_crystal",
            "R.drawable.store_art_keyboard_obsidian",
            "R.drawable.store_art_keyboard_midnight",
            "R.drawable.store_art_keyboard_black_gold",
            "R.drawable.store_art_keyboard_premium_white",
        ).forEach { drawable -> assertTrue("Missing keyboard artwork $drawable", preview.contains(drawable)) }

        assertTrue(preview.contains("contentScale = ContentScale.Fit"))
        assertTrue(preview.contains("Color.Transparent"))
        assertFalse(preview.contains("browser screenshot", ignoreCase = true))
    }

    private fun source(name: String): String = File("src/main/java/com/sonharf/game/$name").readText()
}
