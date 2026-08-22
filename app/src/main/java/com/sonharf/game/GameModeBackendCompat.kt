package com.sonharf.game

import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Server-authoritative game-mode preference used by matchmaking. */
suspend fun OnlineGameBackend.getPreferredGameMode(): String {
    val mode = runCatching {
        SupabaseProvider.client.postgrest.rpc("get_game_mode_v1").decodeSingle<String>()
    }.getOrNull()?.takeIf { it == "normal" || it == "expert" } ?: SonHarfGameModeState.mode
    SonHarfGameModeState.mode = mode.takeIf { it == "normal" || it == "expert" } ?: "normal"
    return SonHarfGameModeState.mode
}

suspend fun OnlineGameBackend.setPreferredGameMode(mode: String) {
    val normalized = mode.takeIf { it == "normal" || it == "expert" } ?: "normal"
    SupabaseProvider.client.postgrest.rpc(
        "set_game_mode_v1",
        buildJsonObject { put("p_mode", normalized) },
    )
    SonHarfGameModeState.mode = normalized
}
