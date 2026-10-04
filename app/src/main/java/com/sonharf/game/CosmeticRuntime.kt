package com.sonharf.game

import android.content.Context
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.size
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
    var mascotHatId by mutableStateOf<String?>(null)

    // A keyboard or theme won with a rewarded video: worn for a day, then the equipped one returns.
    var rewardKeyboardId by mutableStateOf<String?>(null)
    var rewardKeyboardUntil by mutableStateOf(0L)
    var rewardThemeId by mutableStateOf<String?>(null)
    var rewardThemeUntil by mutableStateOf(0L)

    /** The keyboard in use: a live rewarded one first, otherwise the equipped one. */
    val activeKeyboardId: String?
        get() = rewardKeyboardId?.takeIf { System.currentTimeMillis() < rewardKeyboardUntil } ?: keyboardThemeId

    /** The theme in use: a live rewarded one first, otherwise the equipped one. */
    val activeThemeId: String?
        get() = rewardThemeId?.takeIf { System.currentTimeMillis() < rewardThemeUntil } ?: gameThemeId

    fun apply(e: EquippedCosmeticsDto?) {
        profileFrameId = e?.profileFrameId?.takeIf { !it.isNullOrBlank() }
        nameStyleId = e?.nameStyleId
        gameThemeId = e?.gameThemeId
        keyboardThemeId = e?.keyboardThemeId
        victoryEffectId = e?.victoryEffectId
        emojiPackId = e?.emojiPackId
        mascotHatId = e?.mascotHatId?.takeIf { it in MascotHats.ids }
    }

    fun restore(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        profileFrameId = prefs.getString("profile_frame_id", null)?.takeIf { !it.isNullOrBlank() }
        gameThemeId = prefs.getString("game_theme_id", null)?.takeIf { it in setOf("theme_black", "theme_dark_arena", WALNUT_IVORY_THEME_ID) }
        nameStyleId = prefs.getString("name_style_id", null)
        keyboardThemeId = prefs.getString("keyboard_theme_id", null)
        victoryEffectId = prefs.getString("victory_effect_id", null)
        mascotHatId = prefs.getString("mascot_hat_id", null)?.takeIf { it in MascotHats.ids }
    }

    fun applyAndPersist(context: Context, e: EquippedCosmeticsDto?) {
        apply(e)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("profile_frame_id", profileFrameId)
            .putString("game_theme_id", gameThemeId)
            .putString("name_style_id", nameStyleId)
            .putString("keyboard_theme_id", keyboardThemeId)
            .putString("victory_effect_id", victoryEffectId)
            .putString("mascot_hat_id", mascotHatId)
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
        get() = keyboardPaletteFor(activeKeyboardId)

    /** Single palette source for the live keyboard and its store preview/art direction. */
    fun keyboardPaletteFor(themeId: String?): WordKeyboardPalette = when (themeId) {
        // Painted skins (Kelime Tahtı keyboard pack): Crystal = "Kristal Taç", Obsidian = "Renk Ustası",
        // Midnight = "Gece Işığı", Black Gold = "Altın Taç", Premium White = "Beyaz Taç". The colours
        // below remain as the fallback look should an image fail to load.
        // Crystal: faceted icy keys lit cyan from below, on a silver tray with a gold rim.
        "keyboard_crystal" -> WordKeyboardPalette(
            background = Color.Transparent, key = Color(0xFFA9DDFF), keyAlt = Color(0xFFC4C6F4),
            text = Color(0xFF10304A), action = Color(0xFF1E7FE0), actionText = Color(0xFF10304A),
            border = Color(0xFF7FCBFF), secondaryBorder = Color(0xFFA9A6EE),
            keyTop = Color(0xFFF2FBFF), altTop = Color(0xFFF2F1FF), actionTop = Color(0xFF7FDBFF),
            glow = Color(0xFF2EC8FF), trayTop = Color(0xFFF8F9FC), rim = Color(0xFFD4AF37), crystal = true,
            keyImage = R.drawable.keyboard_crystal_key, panelImage = R.drawable.keyboard_crystal_panel,
            panelSide = .035f, panelTop = .25f, panelBottom = .06f,
        )
        // Obsidian: glossy black keys, violet secondary keys, a gold send key, teal underglow, blue rim.
        "keyboard_obsidian" -> WordKeyboardPalette(
            background = Color.Transparent, key = Color(0xFF15171C), keyAlt = Color(0xFF221A38),
            text = Color(0xFFFFFFFF), action = Color(0xFFD49A2A), actionText = Color(0xFFFFFFFF),
            border = Color(0xFF3A3F4C), secondaryBorder = Color(0xFF8F6BFF),
            keyTop = Color(0xFF2C2F38), altTop = Color(0xFF3D2F66), actionTop = Color(0xFFFFD36A),
            glow = Color(0xFF22D3C5), trayTop = Color(0xFF1E2027), rim = Color(0xFF2E5BFF),
            keyImage = R.drawable.keyboard_obsidian_key, panelImage = R.drawable.keyboard_obsidian_panel,
            panelSide = .03f, panelTop = .18f, panelBottom = .045f,
        )
        // Midnight: deep navy keys glowing blue-violet from below, cyan-to-blue send key.
        "keyboard_midnight" -> WordKeyboardPalette(
            background = Color.Transparent, key = Color(0xFF131B36), keyAlt = Color(0xFF1D1745),
            text = Color(0xFFFFFFFF), action = Color(0xFF3C5BFF), actionText = Color(0xFFFFFFFF),
            border = Color(0xFF3552A8), secondaryBorder = Color(0xFF8E6BFF),
            keyTop = Color(0xFF26335F), altTop = Color(0xFF33296B), actionTop = Color(0xFF3FD0FF),
            glow = Color(0xFF6D7CFF), trayTop = Color(0xFF1B2757), rim = Color(0xFF3D6BFF),
            keyImage = R.drawable.keyboard_midnight_key, panelImage = R.drawable.keyboard_midnight_panel,
            panelSide = .035f, panelTop = .12f, panelBottom = .05f,
        )
        // Black Gold: black keys with gold rims and letters, a solid gold send key, gold tray edge.
        "keyboard_black_gold" -> WordKeyboardPalette(
            background = Color.Transparent, key = Color(0xFF101114), keyAlt = Color(0xFF1A1712),
            text = Color(0xFFFFF9DF), action = Color(0xFFC8922E), actionText = Color(0xFFFFF9DF),
            border = Color(0xFF8A6A2E), secondaryBorder = Color(0xFFE0B45C),
            keyTop = Color(0xFF2E3036), altTop = Color(0xFF3A3222), actionTop = Color(0xFFFFE08A),
            glow = Color(0xFFFFC857), trayTop = Color(0xFF1A1B1F), rim = Color(0xFFE0B45C),
            keyImage = R.drawable.keyboard_black_gold_key, panelImage = R.drawable.keyboard_black_gold_panel,
            panelSide = .04f, panelTop = .22f, panelBottom = .07f,
        )
        // Premium White: pearl-white glossy keys with a soft sky-blue glow and a blue send key.
        "keyboard_premium_white" -> WordKeyboardPalette(
            background = Color.Transparent, key = Color(0xFFE3ECF7), keyAlt = Color(0xFFD9E6F5),
            text = Color(0xFF153849), action = Color(0xFF2A72E5), actionText = Color(0xFF153849),
            border = Color(0xFFC3D6EC), secondaryBorder = Color(0xFF8CC4F5),
            keyTop = Color(0xFFFFFFFF), altTop = Color(0xFFF4F8FD), actionTop = Color(0xFF6CC3FF),
            glow = Color(0xFF7FC4FF), trayTop = Color(0xFFFFFFFF), rim = Color(0xFFB8D4F5),
            keyImage = R.drawable.keyboard_premium_white_key, panelImage = R.drawable.keyboard_premium_white_panel,
            panelSide = .035f, panelTop = .20f, panelBottom = .08f,
        )
        // Default keyboard: cream letter keys, light grey special keys, green enter on a light tray.
        else -> WordKeyboardPalette(
            background = Color(0xFFDDE4EC), key = Color(0xFFF7E3A6), keyAlt = Color(0xFFC3CCD6),
            text = Color(0xFF4A3217), action = Color(0xFF3E9F4D), actionText = Color(0xFFFFFFFF),
            border = Color(0xFFC9A560), secondaryBorder = Color(0xFF9AA7B5),
        )
    }

    /** Black Theme is the live catalog id; dark_arena remains an ownership/cache compatibility alias. */
    val darkArenaTheme: Boolean get() = activeThemeId in setOf("theme_black", "theme_dark_arena")
    /** Ceviz & Fildişi is a full theme: warm ivory surfaces and walnut ink across the app and
     *  every game, not only the siege board's wood (which WordSiegeWalnutIvory draws). */
    val walnutTheme: Boolean get() = activeThemeId == WALNUT_IVORY_THEME_ID
    // Kept for compatibility with an already-equipped legacy item. It is no longer sold.
    val monsterBlueTheme: Boolean get() = activeThemeId == "theme_monster_blue"
    // Retained only so older arena code compiles; Aurora is retired from sale.
    val auroraTheme: Boolean get() = activeThemeId == "theme_aurora"
    val crownVictory: Boolean get() = victoryEffectId == "victory_crown"
    /** The hat Obi wears everywhere, bought with Son Coin. */
    internal val mascotHat: WordSiegeMascotHat get() = MascotHats.hatFor(mascotHatId)
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
    /** Gradient tops for glossy key faces; null keeps flat keys (the default keyboard). */
    val keyTop: Color? = null,
    val altTop: Color? = null,
    val actionTop: Color? = null,
    /** Light under each key, as in the store art. */
    val glow: Color? = null,
    /** Tray gradient top (the bottom is [background]) and the tray's rim. */
    val trayTop: Color? = null,
    val rim: Color? = null,
    /** Faceted cut-glass keys (Crystal). */
    val crystal: Boolean = false,
    /** Painted skin: every key wears [keyImage] and the keys sit in [panelImage] (the tray). */
    val keyImage: Int? = null,
    val panelImage: Int? = null,
    /** Frame insets of [panelImage] as fractions of its width (side) and height (top, bottom). */
    val panelSide: Float = .04f,
    val panelTop: Float = .2f,
    val panelBottom: Float = .06f,
)

/** The emblem of the equipped name style: the store image, shown beside the player's name. */
@androidx.compose.runtime.Composable
internal fun NameStyleEmblem(size: androidx.compose.ui.unit.Dp, styleId: String? = SonHarfCosmetics.nameStyleId) {
    val res = when (styleId) {
        "name_cyan" -> R.drawable.store_art_name_cyan
        "name_sapphire" -> R.drawable.store_art_name_sapphire
        "name_amethyst" -> R.drawable.store_art_name_amethyst
        "name_aurelia" -> R.drawable.store_art_name_aurelia
        else -> return
    }
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(res),
        contentDescription = null,
        modifier = androidx.compose.ui.Modifier.size(size),
    )
}

/** Readable, distinct typefaces; no font download or decorative glyph substitution. */
internal fun premiumNameStyle(id: String?): androidx.compose.ui.text.TextStyle {
    // Each signature has its own system typeface (not just a colour); Typeface.create falls back
    // to the default family on devices that lack one, so names always stay readable.
    fun system(name: String, style: Int = android.graphics.Typeface.BOLD) =
        androidx.compose.ui.text.font.FontFamily(android.graphics.Typeface.create(name, style))
    val family = when (id) {
        "name_aurelia" -> system("serif")                                   // classic engraved serif
        "name_amethyst" -> system("cursive")                                // flowing script
        "name_sapphire" -> system("sans-serif-condensed")                   // tight athletic sans
        "name_cyan" -> system("sans-serif-smallcaps", android.graphics.Typeface.NORMAL) // small caps
        else -> androidx.compose.ui.text.font.FontFamily.SansSerif
    }
    return androidx.compose.ui.text.TextStyle(
        fontFamily = family,
        fontWeight = if (id == "name_cyan") androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Bold,
        letterSpacing = when (id) {
            "name_amethyst" -> 0.sp
            "name_cyan" -> 1.sp
            "name_aurelia" -> .6.sp
            else -> .3.sp
        },
    )
}
