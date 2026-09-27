package com.sonharf.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode

internal const val WALNUT_IVORY_THEME_ID = "theme_walnut_ivory"

/**
 * Match-only material palette. The original board remains the default.
 * Dark oiled walnut with brass trim; empty squares are recessed wood so the
 * ivory tiles and the jewel-toned ownership rings read at a glance.
 */
internal object WordSiegeWalnutIvory {
    val enabled: Boolean get() = SonHarfCosmetics.gameThemeId == WALNUT_IVORY_THEME_ID
    val frame = Color(0xFF3A2417)
    val frameEdge = Color(0xFFC39A5B)
    val frameShade = Color(0xFF22150C)
    val board = Color(0xFF6B4630)
    // Repeating diagonal streaks read as quarter-sawn walnut grain.
    val boardGrain = Brush.linearGradient(
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
    )
    val empty = Color(0xFF553624)
    val emptyEdge = Color(0xFF8A6446)
    val ivory = Color(0xFFFAF3E3)
    val ivoryShade = Color(0xFFE7D8BC)
    val ivoryHighlight = Color(0xFFFFFDF6)
    val ink = Color(0xFF2A2018)
    val secondaryInk = Color(0xFF7A6650)
    val bevel = Color(0xFFCDB58E)
    val mine = Color(0xFF2E9A62)
    val rival = Color(0xFFC8473C)
    val selection = Color(0xFFE2AE45)
    val tile = Brush.verticalGradient(listOf(ivoryHighlight, ivory, ivoryShade))

    // Bonus squares are inlaid stained veneer, darker than ivory so they never look like tiles.
    val bonus2H = Color(0xFF3F5E78)
    val bonus3H = Color(0xFF7A3F5C)
    val bonus2K = Color(0xFF4B6B3E)
    val bonus3K = Color(0xFF8A5530)
    val bonus4K = Color(0xFF5B4A82)
    val bonusStar = Color(0xFF9C7A2E)
    val bonusBorder = Color(0xFFB08A5A)
    val bonusLabel = Color(0xFFF4E6C8)
}
