package com.sonharf.game

import com.sonharf.game.data.ShopItemDto

/**
 * Ownership may outlive storefront rotation. Keep this list aligned with cosmetics that have a
 * real runtime renderer; retired-but-working cosmetics remain usable without being sold again.
 */
internal fun ShopItemDto.isSupportedOwnedStyle(): Boolean = when (kind) {
    "game_theme" -> id in setOf("theme_black", "theme_dark_arena")
    "profile_frame" -> id in PurchasedFrameCatalog.ids
    "name_style" -> id in setOf("name_cyan", "name_sapphire", "name_amethyst", "name_aurelia")
    "keyboard_theme" -> id in setOf(
        "keyboard_crystal",
        "keyboard_obsidian",
        "keyboard_midnight",
        "keyboard_black_gold",
        "keyboard_premium_white",
    )
    // Victory effects and emoji packs remain excluded until their in-match runtime behavior is wired.
    else -> false
}
