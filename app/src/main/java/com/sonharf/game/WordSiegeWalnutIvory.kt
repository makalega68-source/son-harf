package com.sonharf.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode

internal const val WALNUT_IVORY_THEME_ID = "theme_walnut_ivory"

/** One set of board materials: frame, board surface, recessed squares, tiles and inks. */
internal class BoardMaterial(
    val frame: Color,
    val frameEdge: Color,
    val frameShade: Color,
    val board: Color,
    val boardGrain: Brush,
    val empty: Color,
    val emptyEdge: Color,
    val ivory: Color,
    val ivoryShade: Color,
    val ivoryHighlight: Color,
    val ink: Color,
    val secondaryInk: Color,
    val bevel: Color,
    val mine: Color,
    val rival: Color,
    val selection: Color,
    val bonus2H: Color,
    val bonus3H: Color,
    val bonus2K: Color,
    val bonus3K: Color,
    val bonus4K: Color,
    val bonusStar: Color,
    val bonusBorder: Color,
    val bonusLabel: Color,
) {
    val tile = Brush.verticalGradient(listOf(ivoryHighlight, ivory, ivoryShade))
}

/**
 * Match-only material skins for the purchasable board themes; the original board stays the
 * default. Each skin is built from its store image so the board the player buys is the board they
 * play on:
 * - Ceviz & Fildişi: dark oiled walnut with brass trim, ivory tiles with brown letters.
 * - Siyah Tema: black marble with a gold rim, glossy black tiles with engraved gold letters.
 * Both keep the emerald glow for the player's own tiles, as in the art.
 */
internal object WordSiegeWalnutIvory {
    val enabled: Boolean get() = SonHarfCosmetics.walnutTheme || SonHarfCosmetics.darkArenaTheme

    private val walnut = BoardMaterial(
        frame = Color(0xFF3A2417),
        frameEdge = Color(0xFFC39A5B),
        frameShade = Color(0xFF22150C),
        board = Color(0xFF6B4630),
        // Repeating diagonal streaks read as quarter-sawn walnut grain.
        boardGrain = Brush.linearGradient(
            0f to Color(0xFF74503A),
            .18f to Color(0xFF684430),
            .34f to Color(0xFF5E3C29),
            .52f to Color(0xFF6F4A34),
            .70f to Color(0xFF643F2C),
            .86f to Color(0xFF79553D),
            1f to Color(0xFF74503A),
            start = Offset.Zero,
            end = Offset(90f, 150f),
            tileMode = TileMode.Mirror,
        ),
        empty = Color(0xFF553624),
        emptyEdge = Color(0xFF8A6446),
        ivory = Color(0xFFFAF3E3),
        ivoryShade = Color(0xFFE7D8BC),
        ivoryHighlight = Color(0xFFFFFDF6),
        ink = Color(0xFF2A2018),
        secondaryInk = Color(0xFF7A6650),
        bevel = Color(0xFFCDB58E),
        mine = Color(0xFF2E9A62),
        rival = Color(0xFFC8473C),
        selection = Color(0xFFE2AE45),
        // Bonus squares are inlaid stained veneer, darker than ivory so they never look like tiles.
        bonus2H = Color(0xFF3F5E78),
        bonus3H = Color(0xFF7A3F5C),
        bonus2K = Color(0xFF4B6B3E),
        bonus3K = Color(0xFF8A5530),
        bonus4K = Color(0xFF5B4A82),
        bonusStar = Color(0xFF9C7A2E),
        bonusBorder = Color(0xFFB08A5A),
        bonusLabel = Color(0xFFF4E6C8),
    )

    private val blackGold = BoardMaterial(
        frame = Color(0xFF0B0B0D),
        frameEdge = Color(0xFFD9AE4F),
        frameShade = Color(0xFF000000),
        board = Color(0xFF16171A),
        // Soft grey veins drifting across black marble.
        boardGrain = Brush.linearGradient(
            0f to Color(0xFF1B1C20),
            .22f to Color(0xFF121316),
            .40f to Color(0xFF26272C),
            .47f to Color(0xFF15161A),
            .70f to Color(0xFF1D1E22),
            .88f to Color(0xFF101114),
            1f to Color(0xFF1B1C20),
            start = Offset.Zero,
            end = Offset(160f, 110f),
            tileMode = TileMode.Mirror,
        ),
        empty = Color(0xFF1E1F24),
        emptyEdge = Color(0xFF3A3A40),
        // "Ivory" slots hold the glossy black tile face in this skin.
        ivory = Color(0xFF1F2025),
        ivoryShade = Color(0xFF0E0F12),
        ivoryHighlight = Color(0xFF3B3C43),
        ink = Color(0xFFF2C75C),
        secondaryInk = Color(0xFFBFA15A),
        bevel = Color(0xFFB8903A),
        mine = Color(0xFF2FCB7A),
        rival = Color(0xFFD94A4A),
        selection = Color(0xFFF2C75C),
        bonus2H = Color(0xFF1F3550),
        bonus3H = Color(0xFF4A1F36),
        bonus2K = Color(0xFF223D2A),
        bonus3K = Color(0xFF4F3418),
        bonus4K = Color(0xFF342A52),
        bonusStar = Color(0xFF5A4716),
        bonusBorder = Color(0xFF8A6D2E),
        bonusLabel = Color(0xFFF2D58A),
    )

    private val m: BoardMaterial get() = if (SonHarfCosmetics.darkArenaTheme) blackGold else walnut

    val frame: Color get() = m.frame
    val frameEdge: Color get() = m.frameEdge
    val frameShade: Color get() = m.frameShade
    val board: Color get() = m.board
    val boardGrain: Brush get() = m.boardGrain
    val empty: Color get() = m.empty
    val emptyEdge: Color get() = m.emptyEdge
    val ivory: Color get() = m.ivory
    val ivoryShade: Color get() = m.ivoryShade
    val ivoryHighlight: Color get() = m.ivoryHighlight
    val ink: Color get() = m.ink
    val secondaryInk: Color get() = m.secondaryInk
    val bevel: Color get() = m.bevel
    val mine: Color get() = m.mine
    val rival: Color get() = m.rival
    val selection: Color get() = m.selection
    val tile: Brush get() = m.tile
    val bonus2H: Color get() = m.bonus2H
    val bonus3H: Color get() = m.bonus3H
    val bonus2K: Color get() = m.bonus2K
    val bonus3K: Color get() = m.bonus3K
    val bonus4K: Color get() = m.bonus4K
    val bonusStar: Color get() = m.bonusStar
    val bonusBorder: Color get() = m.bonusBorder
    val bonusLabel: Color get() = m.bonusLabel
}
