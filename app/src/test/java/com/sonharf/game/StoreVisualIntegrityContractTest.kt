package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreVisualIntegrityContractTest {
    @Test
    fun activeStoreUsesRuntimeBackedPreviewsInsteadOfPlaceholders() {
        val economy = projectFile("app/src/main/java/com/sonharf/game/EconomyShopScreen.kt").readText()
        val styleStore = projectFile("app/src/main/java/com/sonharf/game/MonsterStyleStoreScreen.kt").readText()
        val preview = projectFile("app/src/main/java/com/sonharf/game/StoreProductPreview.kt").readText()
        val runtime = projectFile("app/src/main/java/com/sonharf/game/CosmeticRuntime.kt").readText()

        assertTrue(economy.contains("StoreProductPreview("))
        assertTrue(styleStore.contains("StoreProductPreview("))
        assertTrue(preview.contains("PurchasedProfileFrameOverlay("))
        assertTrue(preview.contains("R.drawable.store_art_theme_black"))
        assertTrue(preview.contains("R.drawable.store_art_keyboard_crystal"))
        assertTrue(preview.contains("R.drawable.store_art_keyboard_obsidian"))
        assertTrue(preview.contains("R.drawable.store_art_keyboard_midnight"))
        assertTrue(preview.contains("R.drawable.store_art_keyboard_black_gold"))
        assertTrue(preview.contains("R.drawable.store_art_keyboard_premium_white"))
        assertTrue(preview.contains("R.drawable.store_art_victory_crown"))
        assertTrue(preview.contains("R.drawable.store_art_emoji_vip"))
        assertTrue(runtime.contains("fun keyboardPaletteFor(themeId: String?)"))
        assertTrue(runtime.contains("gameThemeId in setOf(\"theme_black\", \"theme_dark_arena\")"))

        assertFalse(economy.contains("Text(\"A\""))
        assertFalse(economy.contains("👑  ⚡  😎  🔥"))
        assertFalse(styleStore.contains("TEMSİLİ GÖRSEL"))
        assertFalse(styleStore.contains("DarkArenaPreview"))
    }

    @Test
    fun premiumEffectsEnterCatalogOnlyAfterRuntimeHooksExist() {
        val styleStore = projectFile("app/src/main/java/com/sonharf/game/MonsterStyleStoreScreen.kt").readText()
        val siege = projectFile("app/src/main/java/com/sonharf/game/WordSiegeExperience.kt").readText()
        val effects = projectFile("app/src/main/java/com/sonharf/game/PremiumReactionCosmetics.kt").readText()
        val catalog = projectFile("app/src/main/java/com/sonharf/game/billing/ProductCatalog.kt").readText()
        val activeOneTime = catalog.substringAfter("val oneTimeProducts").substringBefore("\n    )")

        assertTrue(styleStore.contains("StoreTab(sh(\"EFEKTLER\", \"EFFECTS\")"))
        assertTrue(styleStore.contains("\"victory_effect\" -> id == \"victory_crown\""))
        assertTrue(styleStore.contains("\"emoji_pack\" -> id == \"emoji_vip\""))
        assertTrue(siege.contains("won && SonHarfCosmetics.crownVictory"))
        assertTrue(siege.contains("SonHarfCosmetics.emojiPackId == \"emoji_vip\""))
        assertTrue(effects.contains("VipEmojiReactions"))
        assertFalse(activeOneTime.contains("STARTER_STYLE_PACK"))
    }

    @Test
    fun playProductsUseTruthfulPurchaseStatesAndDistinctCoinPackArtwork() {
        val play = projectFile("app/src/main/java/com/sonharf/game/GooglePlayProductsCard.kt").readText()

        listOf(
            "R.drawable.premium_coin_500",
            "R.drawable.premium_coin_1500",
            "R.drawable.premium_coin_3500",
            "R.drawable.premium_coin_8000",
        ).forEach { artwork -> assertTrue("Missing coin artwork mapping $artwork", play.contains(artwork)) }
        assertFalse(play.contains("R.drawable.style_icon_coin"))
        assertTrue(play.contains("@DrawableRes imageRes: Int"))
        assertTrue(play.contains("product != null"))
        assertTrue(play.contains("PLAY'DE YOK"))
        assertFalse(play.contains("?: \"PLAY\""))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
