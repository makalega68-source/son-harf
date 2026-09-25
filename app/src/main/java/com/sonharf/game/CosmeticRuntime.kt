package com.sonharf.game

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.sonharf.game.data.EquippedCosmeticsDto

object SonHarfCosmetics {
    private const val PREFS = "son_harf_equipped_style_cache"
    var profileFrameId by mutableStateOf<String?>(null)
    var nameStyleId by mutableStateOf<String?>(null)
    var gameThemeId by mutableStateOf<String?>(null)
    var keyboardThemeId by mutableStateOf<String?>(null)
    var victoryEffectId by mutableStateOf<String?>(null)
    var emojiPackId by mutableStateOf<String?>(null)

    fun apply(e: EquippedCosmeticsDto?) {
        profileFrameId = e?.profileFrameId?.takeIf { !it.isNullOrBlank() }
        nameStyleId = e?.nameStyleId
        gameThemeId = e?.gameThemeId
        keyboardThemeId = e?.keyboardThemeId
        victoryEffectId = e?.victoryEffectId
        emojiPackId = e?.emojiPackId
    }

    fun restore(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        profileFrameId = prefs.getString("profile_frame_id", null)?.takeIf { !it.isNullOrBlank() }
        gameThemeId = prefs.getString("game_theme_id", null)?.takeIf { it in setOf("theme_black", "theme_dark_arena") }
        nameStyleId = prefs.getString("name_style_id", null)
        keyboardThemeId = prefs.getString("keyboard_theme_id", null)
    }

    fun applyAndPersist(context: Context, e: EquippedCosmeticsDto?) {
        apply(e)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("profile_frame_id", profileFrameId)
            .putString("game_theme_id", gameThemeId)
            .putString("name_style_id", nameStyleId)
            .putString("keyboard_theme_id", keyboardThemeId)
            .apply()
    }

    val profileAccent: Color
        get() = when (profileFrameId) {
            PurchasedFrameCatalog.GOLDEN_AVATAR -> SonHarfGold
            PurchasedFrameCatalog.OCEAN -> SonHarfCyan
            PurchasedFrameCatalog.LILAC -> SonHarfPurple
            PurchasedFrameCatalog.BOTANIC -> Color(0xFF2FAE68)
            PurchasedFrameCatalog.ROSE -> Color(0xFFD84C4C)
            else -> SonHarfMuted
        }

    val playerNameColor: Color
        get() = when (nameStyleId) {
            "name_cyan" -> Color(0xFF2B9CB5)
            "name_sapphire" -> Color(0xFF2E6FB7)
            "name_amethyst" -> Color(0xFF7D5CA8)
            "name_aurelia" -> Color(0xFF9C742D)
            else -> SonHarfText
        }

    /** Product skins affect only letter-input presentation and never gameplay. */
    val keyboardPalette: WordKeyboardPalette
        get() = keyboardPaletteFor(keyboardThemeId)

    /** Single palette source for the live keyboard and its store preview/art direction. */
    fun keyboardPaletteFor(themeId: String?): WordKeyboardPalette = when (themeId) {
        "keyboard_crystal" -> WordKeyboardPalette(
            background = Color(0xFFE8F1F5), key = Color(0xFFF8FCFF), keyAlt = Color(0xFFD6E5ED),
            text = Color(0xFF26353E), action = Color(0xFF537FA1), actionText = Color.White,
            border = Color(0xFF9ABBCB), secondaryBorder = Color(0xFFA9BFCA),
        )
        "keyboard_obsidian" -> WordKeyboardPalette(
            background = Color(0xFF151719), key = Color(0xFF24272A), keyAlt = Color(0xFF343535),
            text = Color(0xFFF7F1E3), action = Color(0xFFB9914D), actionText = Color(0xFF21180A),
            border = Color(0xFF6A6254), secondaryBorder = Color(0xFFB9914D),
        )
        "keyboard_midnight" -> WordKeyboardPalette(
            background = Color(0xFF0E1625), key = Color(0xFF1B2740), keyAlt = Color(0xFF263551),
            text = Color(0xFFEDF3FF), action = Color(0xFF5C7CFA), actionText = Color.White,
            border = Color(0xFF31466B), secondaryBorder = Color(0xFF13D8D0),
        )
        "keyboard_black_gold" -> WordKeyboardPalette(
            background = Color(0xFF090A0D), key = Color(0xFF17191D), keyAlt = Color(0xFF252119),
            text = Color(0xFFFFF0CF), action = Color(0xFFE0B45C), actionText = Color(0xFF21170A),
            border = Color(0xFF6E592E), secondaryBorder = Color(0xFFE0B45C),
        )
        "keyboard_premium_white" -> WordKeyboardPalette(
            background = Color(0xFFF3F6FA), key = Color.White, keyAlt = Color(0xFFE8EEF5),
            text = Color(0xFF263238), action = Color(0xFF2A72E5), actionText = Color.White,
            border = Color(0xFFCFD9E6), secondaryBorder = Color(0xFF14B8B1),
        )
        // Higgsfield default: ivory letter keys, grey special keys, green enter on graphite.
        else -> WordKeyboardPalette(
            background = Color(0xFF171C1B), key = Color(0xFFF0EDE3), keyAlt = Color(0xFFD1D3D0),
            text = Color(0xFF171C1B), action = Color(0xFF32845E), actionText = Color(0xFFF0EDE3),
            border = Color(0xFFC8C1B4), secondaryBorder = Color(0xFF9EA39F),
        )
    }

    /** Black Theme is the live catalog id; dark_arena remains an ownership/cache compatibility alias. */
    val darkArenaTheme: Boolean get() = gameThemeId in setOf("theme_black", "theme_dark_arena")
    // Kept for compatibility with an already-equipped legacy item. It is no longer sold.
    val monsterBlueTheme: Boolean get() = gameThemeId == "theme_monster_blue"
    // Retained only so older arena code compiles; Aurora is retired from sale.
    val auroraTheme: Boolean get() = gameThemeId == "theme_aurora"
    val crownVictory: Boolean get() = victoryEffectId == "victory_crown"
}

/** Shared by every embedded word keyboard so a purchased skin is real in every supported mode. */
data class WordKeyboardPalette(
    val background: Color,
    val key: Color,
    val keyAlt: Color,
    val text: Color,
    val action: Color,
    val actionText: Color,
    val border: Color,
    val secondaryBorder: Color,
)
