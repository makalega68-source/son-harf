package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class WeeklyTopPlayerV210(
    @SerialName("user_id") val userId: String = "",
    @SerialName("username") val username: String = "Oyuncu",
    val rp: Int = 0,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

suspend fun OnlineGameBackend.getWeeklyTopV210(limit: Int = 3): List<WeeklyTopPlayerV210> =
    SupabaseProvider.client.postgrest.rpc(
        "weekly_top",
        buildJsonObject { put("p_limit", limit.coerceIn(1, 100)) },
    ).decodeList()

suspend fun OnlineGameBackend.getMyWeeklyRpV210(): Int =
    SupabaseProvider.client.postgrest.rpc("my_weekly_rp").decodeSingle()
