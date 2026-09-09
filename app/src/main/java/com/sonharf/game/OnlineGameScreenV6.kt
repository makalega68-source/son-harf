package com.sonharf.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sonharf.game.mascot.ReactiveMageCatOverlay

/** Stable integration point for the rebuilt Unified Pro Premier duel. */
@Composable
fun OnlineGameScreenV6() {
    Box(Modifier.fillMaxSize()) {
        PremierWordDuelScreen()
        ReactiveMageCatOverlay()
        // Ranked Premier is skill-only. Purchasable gameplay boosters must never alter a live duel.
    }
}
