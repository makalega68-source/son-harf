package com.sonharf.game

import androidx.compose.runtime.Composable

/**
 * Classic Son Harf duel invitations are intentionally disabled while the game
 * is rebuilt from the clean-slate integration point. The composable remains as
 * a source-compatible app-shell hook only; it performs no polling, matchmaking
 * or navigation.
 */
@Composable
fun GameInviteOverlay() = Unit
