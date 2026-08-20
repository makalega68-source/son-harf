package com.sonharf.game

import androidx.compose.runtime.Composable

/**
 * Single-renderer application root.
 *
 * OnlineGameScreenV6 already owns the duel lobby, active arena, private-room flow,
 * chat, trivia and input handling. Older full-screen enhancement/waiting layers
 * must not be mounted globally because they duplicate the arena and leak game UI
 * over Home/Shop/Profile screens.
 */
@Composable
fun AuroraSonHarfAppPrivateEnhanced() {
    AuroraSonHarfAppWithCombo()
}
