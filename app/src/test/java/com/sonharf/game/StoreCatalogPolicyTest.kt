package com.sonharf.game

import com.sonharf.game.data.EquippedCosmeticsDto
import com.sonharf.game.data.ShopItemDto
import org.junit.Assert.*
import org.junit.Test

class StoreCatalogPolicyTest {
    private fun item(id: String, kind: String) = ShopItemDto(id, kind, "Ürün", "Item", diamondPrice = 120)

    @Test fun onlyConnectedCosmeticsAreOffered() {
        assertTrue(item("theme_dark_arena", "game_theme").isRuntimeReadyStyle())
        assertFalse(item("theme_monster_blue", "game_theme").isRuntimeReadyStyle())
        assertTrue(item("name_cyan", "name_style").isRuntimeReadyStyle())
        assertTrue(item("keyboard_neon", "keyboard_theme").isRuntimeReadyStyle())
        assertTrue(item("frame_asset_red", "profile_frame").isRuntimeReadyStyle())
        assertFalse(item("victory_crown", "victory_effect").isRuntimeReadyStyle())
        assertFalse(item("emoji_vip", "emoji_pack").isRuntimeReadyStyle())
        assertFalse(item("unknown", "profile_frame").isRuntimeReadyStyle())
    }

    @Test fun inactiveAndMismatchedProductsCannotBeOffered() {
        assertFalse(item("theme_dark_arena", "game_theme").copy(active = false).isRuntimeReadyStyle())
        assertFalse(item("theme_dark_arena", "name_style").isRuntimeReadyStyle())
    }

    @Test fun equippedStateUsesTheCorrectSlot() {
        val selected = EquippedCosmeticsDto(userId = "test", gameThemeId = "theme_dark_arena", profileFrameId = "frame_asset_red")
        assertTrue(selected.isEquipped(item("theme_dark_arena", "game_theme")))
        assertTrue(selected.isEquipped(item("frame_asset_red", "profile_frame")))
        assertFalse(selected.isEquipped(item("frame_asset_green", "profile_frame")))
        assertFalse((null as EquippedCosmeticsDto?).isEquipped(item("theme_dark_arena", "game_theme")))
    }
}
