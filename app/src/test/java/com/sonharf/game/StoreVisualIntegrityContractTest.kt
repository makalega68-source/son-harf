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
        assertTrue(runtime.contains("fun keyboardPaletteFor(themeId: String?)"))
        assertTrue(runtime.contains("gameThemeId in setOf(\"theme_black\", \"theme_dark_arena\")"))

        assertFalse(economy.contains("Text(\"A\""))
        assertFalse(economy.contains("👑  ⚡  😎  🔥"))
        assertFalse(styleStore.contains("TEMSİLİ GÖRSEL"))
        assertFalse(styleStore.contains("DarkArenaPreview"))
    }

    @Test
    fun productsWithoutVerifiedRuntimeBehaviorStayOutOfTheActiveCatalog() {
        val styleStore = projectFile("app/src/main/java/com/sonharf/game/MonsterStyleStoreScreen.kt").readText()
        val catalog = projectFile("app/src/main/java/com/sonharf/game/billing/ProductCatalog.kt").readText()
        val activeOneTime = catalog.substringAfter("val oneTimeProducts").substringBefore("\n    )")

        assertTrue(styleStore.contains("Effects and emoji remain hidden until their in-match behavior is connected"))
        assertFalse(styleStore.contains("\"victory_effect\" -> id == \"victory_crown\""))
        assertFalse(styleStore.contains("\"emoji_pack\" -> id == \"emoji_vip\""))
        assertFalse(activeOneTime.contains("STARTER_STYLE_PACK"))
    }

    @Test
    fun playProductsNeverPretendAnUnavailablePriceIsARealPurchaseButton() {
        val play = projectFile("app/src/main/java/com/sonharf/game/GooglePlayProductsCard.kt").readText()

        assertTrue(play.contains("style_icon_coin"))
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
