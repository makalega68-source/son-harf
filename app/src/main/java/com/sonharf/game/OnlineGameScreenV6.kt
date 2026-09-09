package com.sonharf.game

import androidx.compose.runtime.Composable

/** Stable integration point for the rebuilt Unified Pro Premier duel. */
@Composable
fun OnlineGameScreenV6() {
    // Ranked Premier is skill-only: no mascot or purchasable gameplay-power overlays.
    PremierWordDuelScreen()
}
