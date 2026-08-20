package com.sonharf.game

import com.sonharf.game.data.OnlineGameBackend

/**
 * Compatibility bridge for the neon home screen game-mode selector.
 *
 * The production backend does not persist a preferred game mode yet, so keep the
 * selection in the existing app-level state instead of coupling the UI to a
 * database column/RPC that is not deployed.
 */
suspend fun OnlineGameBackend.getPreferredGameMode(): String =
    SonHarfGameModeState.mode.takeIf { it == "normal" || it == "expert" } ?: "normal"

suspend fun OnlineGameBackend.setPreferredGameMode(mode: String) {
    SonHarfGameModeState.mode = mode.takeIf { it == "normal" || it == "expert" } ?: "normal"
}
