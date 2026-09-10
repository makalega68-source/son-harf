package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class PremierTurnClockDto(
    @SerialName("remaining_ms") val remainingMs: Long,
    @SerialName("server_now") val serverNow: String? = null,
    @SerialName("turn_deadline") val turnDeadline: String? = null,
)

suspend fun fetchPremierTurnClock(roomId: String): PremierTurnClockDto =
    SupabaseProvider.client.postgrest.rpc(
        "get_premier_turn_clock_v1",
        buildJsonObject { put("p_room_id", roomId) },
    ).decodeSingle()
