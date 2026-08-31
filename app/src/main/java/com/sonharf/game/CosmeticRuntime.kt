package com.sonharf.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.sonharf.game.data.EquippedCosmeticsDto

internal data class SonHarfKeyboardPalette(
    val background: Color,
    val key: Color,
    val keyAlt: Color,
    val text: Color,
    val action: Color,
    val accent: Color,
)

internal data class SonHarfGamePalette(
    val background: Color,
    val surface: Color,
    val surfaceSoft: Color,
    val accent: Color,
    val secondary: Color,
)

object SonHarfCosmetics {
    var profileFrameId by mutableStateOf<String?>(null)
    var nameStyleId by mutableStateOf<String?>(null)
    var gameThemeId by mutableStateOf<String?>(null)
    var keyboardThemeId by mutableStateOf<String?>(null)
    var victoryEffectId by mutableStateOf<String?>(null)
    var emojiPackId by mutableStateOf<String?>(null)

    fun apply(e: EquippedCosmeticsDto?) {
        profileFrameId = e?.profileFrameId
        nameStyleId = e?.nameStyleId
        gameThemeId = e?.gameThemeId
        keyboardThemeId = e?.keyboardThemeId
        victoryEffectId = e?.victoryEffectId
        emojiPackId = e?.emojiPackId
    }

    val profileAccent: Color
        get() = frameAccent(profileFrameId)

    fun frameAccent(id: String?): Color = when {
        id == null -> SonHarfMuted
        "gold" in id -> SonHarfGold
        "neon" in id -> SonHarfCyan
        "starter" in id || "founder" in id || "light" in id -> SonHarfPurple
        "vip" in id -> Color(0xFFF5B82E)
        "ruby" in id || "red" in id -> Color(0xFFE84B67)
        "emerald" in id || "green" in id -> Color(0xFF24B47E)
        "ice" in id || "cyan" in id -> SonHarfCyan
        else -> SonHarfPurple
    }

    val playerNameColor: Color
        get() = when {
            nameStyleId?.contains("cyan") == true -> SonHarfCyan
            nameStyleId?.contains("gold") == true -> SonHarfGold
            nameStyleId?.contains("black") == true -> Color(0xFF111827)
            else -> SonHarfText
        }

    val keyboardPalette: SonHarfKeyboardPalette
        get() = when {
            keyboardThemeId?.contains("neon") == true -> SonHarfKeyboardPalette(
                background = Color(0xFF050818),
                key = Color(0xFF111A35),
                keyAlt = Color(0xFF17264A),
                text = Color(0xFFF6FBFF),
                action = Color(0xFF25D9FF),
                accent = Color(0xFF9B5CFF),
            )
            keyboardThemeId?.contains("gold") == true -> SonHarfKeyboardPalette(
                background = Color(0xFF17120A),
                key = Color(0xFF2A2110),
                keyAlt = Color(0xFF3A2B12),
                text = Color(0xFFFFF8E8),
                action = Color(0xFFFFB31A),
                accent = Color(0xFFE5A323),
            )
            keyboardThemeId?.contains("ice") == true -> SonHarfKeyboardPalette(
                background = Color(0xFFEAF7FF),
                key = Color(0xFFFFFFFF),
                keyAlt = Color(0xFFDCEFFC),
                text = Color(0xFF18314B),
                action = Color(0xFF2A8FD8),
                accent = Color(0xFF5EC7F2),
            )
            else -> SonHarfKeyboardPalette(
                background = Color(0xFF070A18),
                key = Color(0xFF121833),
                keyAlt = Color(0xFF1C2347),
                text = Color(0xFFF7F8FF),
                action = Color(0xFFFFB31A),
                accent = Color(0xFF8A5CFF),
            )
        }

    val gamePalette: SonHarfGamePalette
        get() = when {
            gameThemeId?.contains("aurora") == true -> SonHarfGamePalette(
                background = Color(0xFF07111E),
                surface = Color(0xFF10233A),
                surfaceSoft = Color(0xFF16344A),
                accent = Color(0xFF35D5D0),
                secondary = Color(0xFFA86BFF),
            )
            gameThemeId?.contains("sunset") == true -> SonHarfGamePalette(
                background = Color(0xFF211018),
                surface = Color(0xFF38202A),
                surfaceSoft = Color(0xFF4A2A32),
                accent = Color(0xFFFF8A4C),
                secondary = Color(0xFFFF4F87),
            )
            gameThemeId?.contains("midnight") == true -> SonHarfGamePalette(
                background = Color(0xFF050713),
                surface = Color(0xFF0C1022),
                surfaceSoft = Color(0xFF121936),
                accent = Color(0xFF2188FF),
                secondary = Color(0xFF8A5CFF),
            )
            else -> SonHarfGamePalette(
                background = Color(0xFFF7F9FC),
                surface = Color.White,
                surfaceSoft = Color(0xFFF0F4F8),
                accent = Color(0xFF1769E0),
                secondary = Color(0xFF6B4FD3),
            )
        }

    val keyboardIsNeon: Boolean get() = keyboardThemeId?.contains("neon") == true
    val auroraTheme: Boolean get() = gameThemeId?.contains("aurora") == true
    val crownVictory: Boolean get() = victoryEffectId == "victory_crown"
}
