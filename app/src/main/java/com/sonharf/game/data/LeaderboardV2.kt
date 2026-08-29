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
    val rating: Int = 1000,
    @SerialName("league_name") val leagueName: String = "BRONZ",
)

suspend fun OnlineGameBackend.getLeaderboardV2(
    language: String,
    period: String,
    limit: Int = 50,
): List<LeaderboardV2Row> =
    SupabaseProvider.client.postgrest.rpc(
        "get_rating_leaderboard_v1",
        buildJsonObject {
            put("p_language", language.lowercase())
            put("p_period", period.lowercase())
            put("p_limit", limit.coerceIn(1, 100))
        },
    ).decodeList()
