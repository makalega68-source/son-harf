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
        assertTrue(item("name_sapphire", "name_style").isRuntimeReadyStyle())
        assertTrue(item("keyboard_crystal", "keyboard_theme").isRuntimeReadyStyle())
        assertTrue(item("keyboard_obsidian", "keyboard_theme").isRuntimeReadyStyle())
        assertFalse(item("keyboard_neon", "keyboard_theme").isRuntimeReadyStyle())
        assertFalse(item("frame_round_ocean", "profile_frame").isRuntimeReadyStyle())
        assertTrue(item("victory_crown", "victory_effect").isRuntimeReadyStyle())
        assertTrue(item("emoji_vip", "emoji_pack").isRuntimeReadyStyle())
        assertFalse(item("unknown", "profile_frame").isRuntimeReadyStyle())
        assertFalse(item("victory_unknown", "victory_effect").isRuntimeReadyStyle())
        assertFalse(item("emoji_unknown", "emoji_pack").isRuntimeReadyStyle())
    }

    @Test fun inactiveAndMismatchedProductsCannotBeOffered() {
        assertFalse(item("theme_dark_arena", "game_theme").copy(active = false).isRuntimeReadyStyle())
        assertFalse(item("theme_dark_arena", "name_style").isRuntimeReadyStyle())
        assertFalse(item("victory_crown", "victory_effect").copy(active = false).isRuntimeReadyStyle())
        assertFalse(item("emoji_vip", "emoji_pack").copy(active = false).isRuntimeReadyStyle())
    }

    @Test fun equippedStateUsesTheCorrectSlotForHistoricalCompatibility() {
        val selected = EquippedCosmeticsDto(
            userId = "test",
            gameThemeId = "theme_dark_arena",
            profileFrameId = "frame_round_ocean",
            victoryEffectId = "victory_crown",
            emojiPackId = "emoji_vip",
        )
        assertTrue(selected.isEquipped(item("theme_dark_arena", "game_theme")))
        assertTrue(selected.isEquipped(item("frame_round_ocean", "profile_frame")))
        assertTrue(selected.isEquipped(item("victory_crown", "victory_effect")))
        assertTrue(selected.isEquipped(item("emoji_vip", "emoji_pack")))
        assertFalse(selected.isEquipped(item("frame_round_botanic", "profile_frame")))
        assertFalse((null as EquippedCosmeticsDto?).isEquipped(item("theme_dark_arena", "game_theme")))
    }

    @Test fun retiringSupportedItemsStopsSalesWhileFullyRetiredFramesStayHistoricalOnly() {
        assertFalse(item("frame_round_ocean", "profile_frame").copy(active = false).isRuntimeReadyStyle())
        assertFalse(item("frame_round_ocean", "profile_frame").isSupportedOwnedStyle())

        listOf(
            item("theme_dark_arena", "game_theme"),
            item("name_sapphire", "name_style"),
            item("keyboard_crystal", "keyboard_theme"),
            item("victory_crown", "victory_effect"),
            item("emoji_vip", "emoji_pack"),
        ).forEach { product ->
            val retired = product.copy(active = false)
            assertFalse(retired.isRuntimeReadyStyle())
            assertTrue(retired.isSupportedOwnedStyle())
        }
    }

    @Test fun retiredNeonKeyboardIsNotReintroducedIntoTheCollection() {
        assertFalse(item("keyboard_neon", "keyboard_theme").isSupportedOwnedStyle())
    }

    @Test fun ownershipNeverEnablesUnsupportedOrMismatchedRuntimeAssets() {
        assertFalse(item("unknown", "profile_frame").isSupportedOwnedStyle())
        assertFalse(item("frame_round_ocean", "profile_frame").isSupportedOwnedStyle())
        assertFalse(item("frame_round_ocean", "game_theme").isSupportedOwnedStyle())
        assertTrue(item("victory_crown", "victory_effect").isSupportedOwnedStyle())
        assertTrue(item("emoji_vip", "emoji_pack").isSupportedOwnedStyle())
        assertFalse(item("victory_unknown", "victory_effect").isSupportedOwnedStyle())
        assertFalse(item("emoji_unknown", "emoji_pack").isSupportedOwnedStyle())
    }
}
