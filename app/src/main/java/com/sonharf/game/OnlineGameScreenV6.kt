package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/**
 * Clean-slate integration point for the next Son Harf game implementation.
 *
 * The previous classic Son Harf game foundation and UI were deliberately removed.
 * The new game code supplied by the product owner will replace this empty slot.
 */
@Composable
fun OnlineGameScreenV6() {
    BackHandler { SonHarfUiState.homeRequest += 1 }
}
