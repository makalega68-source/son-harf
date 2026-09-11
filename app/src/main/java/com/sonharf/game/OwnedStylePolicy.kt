package com.sonharf.game

import com.sonharf.game.data.ShopItemDto

/**
 * Runtime support and sale availability are separate concerns. A retired item may remain usable by
 * its owner when the artwork/runtime integration is still present in this app version.
 */
internal fun ShopItemDto.isSupportedOwnedStyle(): Boolean = when (kind) {
    "game_theme" -> id == "theme_dark_arena"
    "profile_frame" -> id in PurchasedFrameCatalog.ids
    "name_style" -> id == "name_cyan"
    "keyboard_theme" -> id == "keyboard_neon"
    else -> false
}
