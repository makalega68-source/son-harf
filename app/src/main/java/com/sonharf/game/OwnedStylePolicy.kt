package com.sonharf.game

import com.sonharf.game.data.ShopItemDto

/**
 * Ownership may outlive storefront rotation. Keep this list aligned with cosmetics that still have
 * an active runtime renderer. Fully retired cosmetics remain in server ownership history but are not
 * presented as usable/equippable styles.
 */
internal fun ShopItemDto.isSupportedOwnedStyle(): Boolean = when (kind) {
    "game_theme" -> id in setOf("theme_black", "theme_dark_arena")
    "profile_frame" -> false
    "name_style" -> id in setOf("name_cyan", "name_sapphire", "name_amethyst", "name_aurelia")
    "keyboard_theme" -> id in setOf(
        "keyboard_crystal",
        "keyboard_obsidian",
        "keyboard_midnight",
        "keyboard_black_gold",
        "keyboard_premium_white",
    )
    "victory_effect" -> id == "victory_crown"
    "emoji_pack" -> id == "emoji_vip"
    else -> false
}
