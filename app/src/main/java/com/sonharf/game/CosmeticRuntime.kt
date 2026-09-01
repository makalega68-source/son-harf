package com.sonharf.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.sonharf.game.data.EquippedCosmeticsDto

data class SonHarfKeyboardPalette(
    val background: Color,
    val key: Color,
    val keyAlt: Color,
    val text: Color,
    val action: Color,
    val accent: Color,
)

data class SonHarfGamePalette(
    val background: Color,
    val surface: Color,
    val surfaceSoft: Color,
    val text: Color,
    val muted: Color,
    val border: Color,
    val accent: Color,
    val secondary: Color,
    val tileText: Color,
    val tilePointText: Color,
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

    val profileAccent: Color get() = frameAccent(profileFrameId)

    fun frameAccent(id: String?): Color = when {
        id == null -> SonHarfMuted
        "black_gold" in id -> Color(0xFFD6A84B)
        "royal_gold" in id -> Color(0xFFE6B84B)
        "crystal" in id -> Color(0xFF79B7FF)
        "purple_prestige" in id -> Color(0xFF8D67E8)
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
            else -> if (isMidnight) Color(0xFFF5F7FF) else SonHarfText
        }

    fun keyboardPaletteFor(id: String?): SonHarfKeyboardPalette = when {
        id?.contains("premium_white") == true -> SonHarfKeyboardPalette(
            background = Color(0xFFF1F4F8), key = Color.White, keyAlt = Color(0xFFE4EAF1),
            text = Color(0xFF172033), action = Color(0xFF1769E0), accent = Color(0xFF68758C),
        )
        id?.contains("crystal") == true -> SonHarfKeyboardPalette(
            background = Color(0xFFEAF5FF), key = Color(0xFFF8FCFF), keyAlt = Color(0xFFDCEBFA),
            text = Color(0xFF15283D), action = Color(0xFF397FD4), accent = Color(0xFF7D6BE8),
        )
        id?.contains("midnight") == true -> SonHarfKeyboardPalette(
            background = Color(0xFF050713), key = Color(0xFF11172A), keyAlt = Color(0xFF1A233B),
            text = Color(0xFFF5F7FF), action = Color(0xFF2188FF), accent = Color(0xFF8A5CFF),
        )
        id?.contains("neon") == true -> SonHarfKeyboardPalette(
            background = Color(0xFF08101E), key = Color(0xFFEAF8FF), keyAlt = Color(0xFFD8EDF7),
            text = Color(0xFF101820), action = Color(0xFF25D9FF), accent = Color(0xFF8D5CFF),
        )
        id?.contains("gold") == true -> SonHarfKeyboardPalette(
            background = Color(0xFF17120A), key = Color(0xFF2A2110), keyAlt = Color(0xFF3A2B12),
            text = Color(0xFFFFF8E8), action = Color(0xFFFFB31A), accent = Color(0xFFE5A323),
        )
        id?.contains("ice") == true -> SonHarfKeyboardPalette(
            background = Color(0xFFEAF7FF), key = Color.White, keyAlt = Color(0xFFDCEFFC),
            text = Color(0xFF18314B), action = Color(0xFF2A8FD8), accent = Color(0xFF5EC7F2),
        )
        else -> SonHarfKeyboardPalette(
            background = Color(0xFF070A18), key = Color(0xFF121833), keyAlt = Color(0xFF1C2347),
            text = Color(0xFFF7F8FF), action = Color(0xFFFFB31A), accent = Color(0xFF8A5CFF),
        )
    }

    val keyboardPalette: SonHarfKeyboardPalette get() = keyboardPaletteFor(keyboardThemeId)

    fun gamePaletteFor(id: String?): SonHarfGamePalette = when {
        id?.contains("aurora") == true -> SonHarfGamePalette(
            background = Color(0xFF07111E), surface = Color(0xFF10233A), surfaceSoft = Color(0xFF16344A),
            text = Color(0xFFF4FAFF), muted = Color(0xFF9BB1C8), border = Color(0xFF294765),
            accent = Color(0xFF35D5D0), secondary = Color(0xFFA86BFF),
            tileText = Color(0xFF101216), tilePointText = Color(0xFF4E5968),
        )
        id?.contains("sunset") == true -> SonHarfGamePalette(
            background = Color(0xFF211018), surface = Color(0xFF38202A), surfaceSoft = Color(0xFF4A2A32),
            text = Color(0xFFFFF5F7), muted = Color(0xFFC8A7AF), border = Color(0xFF67404B),
            accent = Color(0xFFFF8A4C), secondary = Color(0xFFFF4F87),
            tileText = Color(0xFF101216), tilePointText = Color(0xFF4E5968),
        )
        id?.contains("midnight") == true -> SonHarfGamePalette(
            background = Color(0xFF050713), surface = Color(0xFF0C1022), surfaceSoft = Color(0xFF121936),
            text = Color(0xFFF5F7FF), muted = Color(0xFFA8B1C5), border = Color(0xFF29324C),
            accent = Color(0xFF2188FF), secondary = Color(0xFF8A5CFF),
            tileText = Color(0xFF101216), tilePointText = Color(0xFF4E5968),
        )
        else -> SonHarfGamePalette(
            background = Color(0xFFF7F9FC), surface = Color.White, surfaceSoft = Color(0xFFF0F4F8),
            text = Color(0xFF182235), muted = Color(0xFF718096), border = Color(0xFFDDE5EE),
            accent = Color(0xFF1769E0), secondary = Color(0xFF6B4FD3),
            tileText = Color(0xFF101216), tilePointText = Color(0xFF4E5968),
        )
    }

    val gamePalette: SonHarfGamePalette get() = gamePaletteFor(gameThemeId)
    val isMidnight: Boolean get() = gameThemeId?.contains("midnight") == true
    val keyboardIsNeon: Boolean get() = keyboardThemeId?.contains("neon") == true
    val auroraTheme: Boolean get() = gameThemeId?.contains("aurora") == true
    val crownVictory: Boolean get() = victoryEffectId == "victory_crown"
}
