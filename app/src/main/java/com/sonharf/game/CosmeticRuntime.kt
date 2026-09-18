package com.sonharf.game

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.sonharf.game.data.EquippedCosmeticsDto

object SonHarfCosmetics {
    private const val PREFS = "son_harf_equipped_style_cache"
    const val BLACK_THEME_ID = "theme_dark_arena"

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
        gameThemeId = prefs.getString("game_theme_id", null)
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

    val blackThemeActive: Boolean get() = gameThemeId == BLACK_THEME_ID

    val profileAccent: Color
        get() = when (profileFrameId) {
            PurchasedFrameCatalog.GOLDEN_AVATAR -> SonHarfTheme.ActionOrange
            PurchasedFrameCatalog.OCEAN -> SonHarfTheme.Turquoise
            PurchasedFrameCatalog.LILAC -> SonHarfTheme.Purple
            PurchasedFrameCatalog.BOTANIC -> Color(0xFF2FAE68)
            PurchasedFrameCatalog.ROSE -> Color(0xFFD84C4C)
            else -> SonHarfTheme.Primary
        }

    val playerNameColor: Color
        get() = when (nameStyleId) {
            "name_cyan" -> Color(0xFF2B9CB5)
            "name_sapphire" -> Color(0xFF2E6FB7)
            "name_amethyst" -> Color(0xFF7D5CA8)
            "name_aurelia" -> Color(0xFF9C742D)
            else -> SonHarfTheme.TextPrimary
        }

    /** Cosmetic keyboards remain presentation-only and never grant a gameplay benefit. */
    val keyboardPalette: WordKeyboardPalette
        get() = keyboardPaletteFor(keyboardThemeId)

    /** Single palette source for the live keyboard and its store preview. */
    fun keyboardPaletteFor(themeId: String?): WordKeyboardPalette = when (themeId) {
        "keyboard_crystal" -> WordKeyboardPalette(
            background = Color(0xFFE8F1F5),
            key = Color(0xFFF8FCFF),
            keyAlt = Color(0xFFD6E5ED),
            text = Color(0xFF26353E),
            action = SonHarfTheme.Primary,
            actionText = Color.White,
            border = Color(0xFF9ABBCB),
            secondaryBorder = SonHarfTheme.Turquoise,
        )
        "keyboard_obsidian" -> WordKeyboardPalette(
            background = Color(0xFF151719),
            key = Color(0xFF24272A),
            keyAlt = Color(0xFF343535),
            text = Color(0xFFF7F1E3),
            action = SonHarfTheme.Purple,
            actionText = Color.White,
            border = Color(0xFF6A6254),
            secondaryBorder = SonHarfTheme.ActionOrange,
        )
        else -> WordKeyboardPalette(
            background = SonHarfTheme.PrimarySoft,
            key = SonHarfTheme.Surface,
            keyAlt = SonHarfTheme.Purple.copy(alpha = .09f),
            text = SonHarfTheme.TextPrimary,
            action = SonHarfTheme.Primary,
            actionText = Color.White,
            border = SonHarfTheme.Border,
            secondaryBorder = SonHarfTheme.Turquoise,
        )
    }

    /** Legacy identifier retained as the server/store key for Black Theme compatibility. */
    val darkArenaTheme: Boolean get() = blackThemeActive
    val monsterBlueTheme: Boolean get() = false
    val auroraTheme: Boolean get() = false

    val crownVictory: Boolean get() = victoryEffectId == "victory_crown"
}

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