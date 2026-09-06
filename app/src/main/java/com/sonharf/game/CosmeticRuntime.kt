package com.sonharf.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.sonharf.game.data.EquippedCosmeticsDto

object SonHarfCosmetics {
    var profileFrameId by mutableStateOf<String?>(null)
    var nameStyleId by mutableStateOf<String?>(null)
    var gameThemeId by mutableStateOf<String?>(null)
    var keyboardThemeId by mutableStateOf<String?>(null)
    var victoryEffectId by mutableStateOf<String?>(null)
    var emojiPackId by mutableStateOf<String?>(null)

    fun apply(e: EquippedCosmeticsDto?) {
        profileFrameId = e?.profileFrameId?.takeIf { it in PurchasedFrameCatalog.ids }
        nameStyleId = e?.nameStyleId
        gameThemeId = e?.gameThemeId
        keyboardThemeId = e?.keyboardThemeId
        victoryEffectId = e?.victoryEffectId
        emojiPackId = e?.emojiPackId
    }

    val profileAccent: Color
        get() = when (profileFrameId) {
            PurchasedFrameCatalog.GOLD, PurchasedFrameCatalog.GOLD_CROWN -> SonHarfGold
            PurchasedFrameCatalog.MINT -> SonHarfCyan
            PurchasedFrameCatalog.PURPLE -> SonHarfPurple
            PurchasedFrameCatalog.GREEN -> Color(0xFF2FAE68)
            PurchasedFrameCatalog.RED -> Color(0xFFD84C4C)
            PurchasedFrameCatalog.CHRISTMAS -> Color(0xFFC73D3D)
            PurchasedFrameCatalog.HALLOWEEN -> Color(0xFFEF7D22)
            else -> SonHarfMuted
        }

    val playerNameColor: Color
        get() = if (nameStyleId == "name_cyan") SonHarfCyan else SonHarfText

    val keyboardIsNeon: Boolean get() = keyboardThemeId == "keyboard_neon"
    /** The only sellable match theme. It changes presentation only, never match rules. */
    val darkArenaTheme: Boolean get() = gameThemeId == "theme_dark_arena"
    // Kept for compatibility with an already-equipped legacy item. It is no longer sold.
    val monsterBlueTheme: Boolean get() = gameThemeId == "theme_monster_blue"
    // Retained only so older arena code compiles; Aurora is retired from sale.
    val auroraTheme: Boolean get() = gameThemeId == "theme_aurora"
    val crownVictory: Boolean get() = victoryEffectId == "victory_crown"
}
