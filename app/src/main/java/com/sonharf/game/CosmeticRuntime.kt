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
        profileFrameId = e?.profileFrameId?.takeIf { it in PurchasedFrameCatalog.ids }
        nameStyleId = e?.nameStyleId
        gameThemeId = e?.gameThemeId
        keyboardThemeId = e?.keyboardThemeId
        victoryEffectId = e?.victoryEffectId
        emojiPackId = e?.emojiPackId
    }

    fun restore(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        profileFrameId = prefs.getString("profile_frame_id", null)?.takeIf { it in PurchasedFrameCatalog.ids }
        gameThemeId = prefs.getString("game_theme_id", null)?.takeIf { it == "theme_dark_arena" }
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
        get() = when (nameStyleId) {
            "name_cyan" -> Color(0xFF2B9CB5)
            "name_sapphire" -> Color(0xFF2E6FB7)
            "name_amethyst" -> Color(0xFF7D5CA8)
            "name_aurelia" -> Color(0xFF9C742D)
            else -> SonHarfText
        }

    /**
     * Product skins affect only letter-input presentation. The legacy neon skin deliberately
     * resolves to the default look after its retirement; it never grants a gameplay benefit.
     */
    val keyboardPalette: WordKeyboardPalette
        get() = when (keyboardThemeId) {
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
            else -> WordKeyboardPalette(
                background = Color(0xFFF0F5F1), key = Color(0xFFFFFEF8), keyAlt = Color(0xFFDDE9E1),
                text = Color(0xFF213C31), action = Color(0xFF4F7964), actionText = Color.White,
                border = Color(0xFFC4D5CA), secondaryBorder = Color(0xFF6D9080),
            )
        }
    /** The only sellable match theme. It changes presentation only, never match rules. */
    val darkArenaTheme: Boolean get() = gameThemeId == "theme_dark_arena"
    // Kept for compatibility with an already-equipped legacy item. It is no longer sold.
    val monsterBlueTheme: Boolean get() = gameThemeId == "theme_monster_blue"
    // Retained only so older arena code compiles; Aurora is retired from sale.
    val auroraTheme: Boolean get() = gameThemeId == "theme_aurora"
    val crownVictory: Boolean get() = victoryEffectId == "victory_crown"
}

/** Shared by every embedded word keyboard so a purchased skin is real in every supported mode. */
internal data class WordKeyboardPalette(
    val background: Color,
    val key: Color,
    val keyAlt: Color,
    val text: Color,
    val action: Color,
    val actionText: Color,
    val border: Color,
    val secondaryBorder: Color,
)
