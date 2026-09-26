package com.sonharf.game

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Match-only material palette; no saved cosmetic preference or game state is changed. */
internal object WordSiegeWalnutIvory {
    val frame = Color(0xFF503526)
    val frameEdge = Color(0xFF8C6848)
    val frameShade = Color(0xFF322419)
    val board = Color(0xFFD9C5A7)
    val boardGrain = Brush.linearGradient(
        listOf(Color(0xFFE1D0B5), board, Color(0xFFD4BE9D), Color(0xFFDECAAC)),
    )
    val empty = Color(0xFFD4BFA0)
    val emptyEdge = Color(0xFFAA9170)
    val ivory = Color(0xFFFBF6EC)
    val ivoryShade = Color(0xFFE9DFD0)
    val ivoryHighlight = Color(0xFFFFFDF8)
    val ink = Color(0xFF302A24)
    val secondaryInk = Color(0xFF62584C)
    val bevel = Color(0xFFAF9A7E)
    val mine = Color(0xFF236D48)
    val rival = Color(0xFFA83E38)
    val selection = Color(0xFFAD7E3D)
    val tile = Brush.verticalGradient(listOf(ivoryHighlight, ivory, ivoryShade))
}
