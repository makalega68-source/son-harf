package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class VipMatchAnalysisDto(
    @SerialName("match_id") val matchId: String,
    val mode: String,
    @SerialName("opponent_id") val opponentId: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("best_word") val bestWord: String? = null,
    @SerialName("longest_word") val longestWord: String? = null,
    @SerialName("fastest_response_ms") val fastestResponseMs: Int? = null,
    @SerialName("slowest_response_ms") val slowestResponseMs: Int? = null,
    @SerialName("avg_response_ms") val avgResponseMs: Int? = null,
    @SerialName("word_count") val wordCount: Int = 0,
    @SerialName("avg_word_length") val avgWordLength: Double? = null,
    @SerialName("highest_move_score") val highestMoveScore: Int? = null,
    @SerialName("critical_time_responses") val criticalTimeResponses: Int = 0,
    @SerialName("territory_gained") val territoryGained: Int = 0,
    @SerialName("territory_lost") val territoryLost: Int = 0,
    @SerialName("turning_point") val turningPoint: JsonObject = buildJsonObject {},
    @SerialName("score_breakdown") val scoreBreakdown: JsonObject = buildJsonObject {},
    val words: JsonElement? = null,
    val moves: JsonElement? = null,
)

@Serializable
data class VipRivalAnalysisDto(
    @SerialName("opponent_id") val opponentId: String,
    @SerialName("total_matches") val totalMatches: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    @SerialName("average_score") val averageScore: Double? = null,
    @SerialName("last_five") val lastFive: JsonElement? = null,
    @SerialName("last_match_at") val lastMatchAt: String? = null,
    @SerialName("longest_win_streak") val longestWinStreak: Int = 0,
    @SerialName("arch_rival") val archRival: Boolean = false,
)

suspend fun OnlineGameBackend.getVipMatchAnalysis(matchId: String, mode: String): VipMatchAnalysisDto =
    SupabaseProvider.client.postgrest.rpc(
        "get_vip_match_analysis_v1",
        buildJsonObject {
            put("p_match_id", matchId)
            put("p_mode", mode)
        },
    ).decodeSingle()

suspend fun OnlineGameBackend.getVipRivalAnalysis(opponentId: String): VipRivalAnalysisDto =
    SupabaseProvider.client.postgrest.rpc(
        "get_vip_rival_analysis_v1",
        buildJsonObject { put("p_opponent_id", opponentId) },
    ).decodeSingle()
