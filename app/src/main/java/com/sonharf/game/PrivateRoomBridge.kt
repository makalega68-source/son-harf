package com.sonharf.game

import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

suspend fun OnlineGameBackend.createPrivateRoom(language: String): GameRoomDto =
    SupabaseProvider.client.postgrest.rpc(
        "create_room",
        buildJsonObject { put("p_language", language) },
    ).decodeSingle()

suspend fun OnlineGameBackend.joinPrivateRoom(code: String): GameRoomDto =
    SupabaseProvider.client.postgrest.rpc(
        "join_room_by_code",
        buildJsonObject { put("p_code", code.trim().uppercase()) },
    ).decodeSingle()
