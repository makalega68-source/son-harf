package com.sonharf.game

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Match-only material palette; no saved cosmetic preference or game state is changed. */
internal object WordSiegeWalnutIvory {
    val frame = Color(0xFF503526)
    val frameEdge = Color(0xFF8C6848)
    val frameShade = Color(0xFF322419)
    val board = Color(0xFFE9DEC8)
    val boardGrain = Brush.linearGradient(
        listOf(Color(0xFFF1E7D4), board, Color(0xFFE8DBC3), Color(0xFFEFE4D0)),
    )
    val ivory = Color(0xFFF7F0E2)
    val ivoryShade = Color(0xFFE9DECB)
    val ivoryHighlight = Color(0xFFFFFCF5)
    val ink = Color(0xFF302A24)
    val secondaryInk = Color(0xFF62584C)
    val bevel = Color(0xFFBBAA90)
    val mine = Color(0xFF3E8058)
    val rival = Color(0xFFAB534C)
    val selection = Color(0xFFAD7E3D)
    val tile = Brush.verticalGradient(listOf(ivoryHighlight, ivory, ivoryShade))
}
