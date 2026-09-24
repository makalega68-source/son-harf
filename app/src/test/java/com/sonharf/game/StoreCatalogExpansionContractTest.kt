package com.sonharf.game

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every sellable Son Coin cosmetic must have a real runtime effect, store art and a server allow-list entry. */
class StoreCatalogExpansionContractTest {
    private val newKeyboards = listOf("keyboard_sakura", "keyboard_ocean", "keyboard_forest", "keyboard_royal_purple")
    private val newNames = listOf("name_emerald", "name_ruby", "name_sunset")

    @Test
    fun newKeyboardsHaveTheirOwnLivePalette() {
        val default = SonHarfCosmetics.keyboardPaletteFor(null)
        val palettes = newKeyboards.map { SonHarfCosmetics.keyboardPaletteFor(it) }
        palettes.forEach { assertTrue(it != default) }
        assertEquals(palettes.size, palettes.toSet().size)
    }

    @Test
    fun everyNameStyleHasAColour() {
        (listOf("name_cyan", "name_sapphire", "name_amethyst", "name_aurelia") + newNames).forEach {
            assertNotNull(it, SonHarfCosmetics.nameStyleColorFor(it))
        }
    }

    @Test
    fun clientAllowListsServerAllowListAndArtAgree() {
        val store = source("app/src/main/java/com/sonharf/game/MonsterStyleStoreScreen.kt")
        val owned = source("app/src/main/java/com/sonharf/game/OwnedStylePolicy.kt")
        val preview = source("app/src/main/java/com/sonharf/game/StoreProductPreview.kt")
        val migration = source("supabase/migrations/20260925000000_store_catalog_expansion_v1.sql")
        (newKeyboards + newNames).forEach { id ->
            assertTrue("store allow-list $id", store.contains("\"$id\""))
            assertTrue("owned allow-list $id", owned.contains("\"$id\""))
            assertTrue("store art $id", preview.contains("\"$id\" -> R.drawable.store_art_"))
            assertTrue("server allow-list $id", migration.contains("'$id'"))
        }
        assertTrue(migration.contains("shop_items_runtime_sale_guard_v2"))
    }

    @Test
    fun blackThemeRestylesTheProfessionalShell() {
        val design = source("app/src/main/java/com/sonharf/game/GameDesignSystem.kt")
        assertTrue(design.contains("object GameDarkPalette"))
        assertTrue(design.contains("if (SonHarfCosmetics.darkArenaTheme) GameDarkPalette.AppBackground else GameLightPalette.AppBackground"))
        assertTrue(design.contains("if (GameColors.isDark) darkColorScheme("))
    }

    @Test
    fun nameStyleShowsOnProfileAndHome() {
        assertTrue(source("app/src/main/java/com/sonharf/game/ProfessionalProfileScreen.kt").contains("SonHarfCosmetics.nameStyleColor ?: GameColors.TextPrimary"))
        assertTrue(source("app/src/main/java/com/sonharf/game/ProfessionalHomeScreen.kt").contains("SonHarfCosmetics.nameStyleColor ?: GameColors.TextPrimary"))
    }

    @Test
    fun illustratedArtIsUsedForLiveCosmetics() {
        listOf(
            "theme_black", "keyboard_midnight", "keyboard_black_gold", "keyboard_premium_white",
            "name_cyan", "name_sapphire", "name_amethyst", "name_aurelia", "victory_crown", "emoji_vip",
        ).forEach { assertNotNull(it, StoreProductArtwork.forCosmetic(it)) }
    }

    private fun source(path: String): String =
        listOf(File(path), File("../$path")).first(File::exists).readText()
}
