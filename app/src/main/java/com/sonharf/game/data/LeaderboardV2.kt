package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class LeaderboardV2Row(
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String,
    val wins: Int = 0,
    val losses: Int = 0,
    val matches: Int = 0,
    @SerialName("win_rate") val winRate: Double = 0.0,
)

@Serializable
data class LeaderboardV3Row(
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String,
    val wins: Int = 0,
    val losses: Int = 0,
    val matches: Int = 0,
    @SerialName("win_rate") val winRate: Double = 0.0,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

suspend fun OnlineGameBackend.getLeaderboardV2(
    language: String,
    period: String,
    limit: Int = 50,
): List<LeaderboardV2Row> =
    SupabaseProvider.client.postgrest.rpc(
        "get_leaderboard_v2",
        buildJsonObject {
            put("p_language", language.lowercase())
            put("p_period", period.lowercase())
            put("p_limit", limit.coerceIn(1, 100))
        },
    ).decodeList()

suspend fun OnlineGameBackend.getLeaderboardV3(
    language: String,
    period: String,
    limit: Int = 50,
): List<LeaderboardV3Row> =
    SupabaseProvider.client.postgrest.rpc(
        "get_leaderboard_v3",
        buildJsonObject {
            put("p_language", language.lowercase())
            put("p_period", period.lowercase())
            put("p_limit", limit.coerceIn(1, 100))
        },
    ).decodeList()
