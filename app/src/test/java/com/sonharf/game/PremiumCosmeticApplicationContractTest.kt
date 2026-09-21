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
        val sharedKeyboard = source("AndroidWordKeyboard.kt")

        assertTrue(premier.contains("AndroidWordKeyboard("))
        assertTrue(sharedKeyboard.contains("useEquippedCosmetic: Boolean = true"))
        assertTrue(sharedKeyboard.contains("SonHarfCosmetics.keyboardPalette.takeIf { useEquippedCosmetic }"))
        assertTrue(sharedKeyboard.contains("equippedPalette?.background"))
        assertTrue(sharedKeyboard.contains("equippedPalette?.key"))
        assertTrue(sharedKeyboard.contains("equippedPalette?.action"))
        assertTrue(premier.contains("nameColor = SonHarfCosmetics.playerNameColor"))
    }

    @Test
    fun everySellableKeyboardHasADistinctRuntimePalette() {
        val runtime = source("CosmeticRuntime.kt")

        listOf(
            "\"keyboard_crystal\" -> WordKeyboardPalette(",
            "\"keyboard_obsidian\" -> WordKeyboardPalette(",
            "\"keyboard_midnight\" -> WordKeyboardPalette(",
            "\"keyboard_black_gold\" -> WordKeyboardPalette(",
            "\"keyboard_premium_white\" -> WordKeyboardPalette(",
        ).forEach { mapping -> assertTrue("Missing keyboard runtime mapping $mapping", runtime.contains(mapping)) }
    }

    @Test
    fun equippedNameStyleOverridesProWhiteOnTheProfile() {
        val profile = source("MainPlayerProfileScreen.kt")

        assertTrue(profile.contains("val hasEquippedNameStyle = !SonHarfCosmetics.nameStyleId.isNullOrBlank()"))
        assertTrue(profile.contains("hasEquippedNameStyle -> SonHarfCosmetics.playerNameColor"))
        assertTrue(profile.contains("color = displayNameColor"))
        assertFalse(profile.contains("color = if (p?.isVip == true) Color.White else SonHarfCosmetics.playerNameColor"))
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
