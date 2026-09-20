package com.sonharf.game.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Thin client bridge over the deployed server-authoritative PRO private-room RPCs. */
class PrivateRoomBackend(private val supabase: SupabaseClient = SupabaseProvider.client) {
    suspend fun create(language: String): GameRoomDto =
        supabase.postgrest.rpc(
            "create_room_normal_v1",
            buildJsonObject { put("p_language", SharedDictionaryService.canonicalLanguage(language)) },
        ).decodeSingle()

    suspend fun join(code: String): GameRoomDto =
        supabase.postgrest.rpc(
            "join_room_by_code",
            buildJsonObject { put("p_code", code.trim().uppercase()) },
        ).decodeSingle()

    suspend fun cancel(roomId: String) {
        supabase.postgrest.rpc(
            "cancel_private_room",
            buildJsonObject { put("p_room_id", roomId) },
        )
    }
}
