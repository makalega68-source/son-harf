package com.sonharf.game

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class WalnutIvoryStoreContractTest {
    private fun source(path: String) = File(path).readText()

    @Test fun oneOwnedSetIsSoldThenEquippedFromProfile() {
        val migration = source("../supabase/migrations/20260926102000_walnut_ivory_board_set.sql")
        val shop = source("src/main/java/com/sonharf/game/EconomyShopScreen.kt")
        val profile = source("src/main/java/com/sonharf/game/ProfileOwnedThemesSection.kt")
        assertTrue(migration.contains("'theme_walnut_ivory', 'game_theme'"))
        assertTrue(migration.contains("600, false, true, 45"))
        assertTrue(migration.contains("'theme_black','theme_dark_arena','theme_walnut_ivory'"))
        assertTrue(migration.contains("drop constraint if exists shop_items_runtime_sale_guard_v2"))
        assertTrue(migration.contains("id in ('theme_black','theme_walnut_ivory')"))
        listOf(
            "keyboard_crystal", "keyboard_obsidian", "keyboard_midnight", "keyboard_black_gold",
            "keyboard_premium_white", "keyboard_sakura", "keyboard_ocean", "keyboard_forest",
            "keyboard_royal_purple", "name_cyan", "name_sapphire", "name_amethyst",
            "name_aurelia", "name_emerald", "name_ruby", "name_sunset",
        ).forEach { assertTrue("Existing active catalog item lost: $it", migration.contains("'$it'")) }
        assertTrue(shop.contains("2 -> items.filter { it.id == WALNUT_IVORY_THEME_ID }"))
        assertTrue(shop.contains("b.purchaseShopItem(item.id)"))
        assertTrue(shop.contains("if (mine) {\n                                onCollection()"))
        assertTrue(profile.contains("itemId != null && itemId !in owned"))
        assertTrue(profile.contains("it.id == WALNUT_IVORY_THEME_ID && it.id in owned"))
        assertTrue(profile.contains("backend.equipShopItem(itemId)"))
        assertTrue(profile.contains("backend.equipDefaultGameTheme()"))
        assertTrue(profile.contains("active = SonHarfCosmetics.gameThemeId == null"))
    }

    @Test fun allSiegeBoardsKeepOriginalPaletteAsDefault() {
        val online = source("src/main/java/com/sonharf/game/WordSiegePanMatch.kt")
        val practice = source("src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt")
        val classic = source("src/main/java/com/sonharf/game/WordSiegeExperience.kt")
        assertTrue(online.contains("else Color(0xFFF3EEDF)"))
        assertTrue(online.contains("else Color(0xFF5FAF73)"))
        assertTrue(online.contains("else Color(0xFFD9776F)"))
        assertTrue(practice.contains("else Color(0xFFF3EEDF)"))
        assertTrue(practice.contains("else Color(0xFF5FAF73)"))
        assertTrue(practice.contains("else Color(0xFFD9776F)"))
        assertTrue(classic.contains("else Color(0xFF35C878)"))
        assertTrue(classic.contains("else Color(0xFFFF5F57)"))
    }
}
