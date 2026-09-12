package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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
    @SerialName("avg_response_ms") val averageResponseMs: Int? = null,
    @SerialName("word_count") val wordCount: Int = 0,
    @SerialName("avg_word_length") val averageWordLength: Double? = null,
    @SerialName("highest_move_score") val highestMoveScore: Int? = null,
    @SerialName("critical_time_responses") val criticalTimeResponses: Int = 0,
    @SerialName("territory_gained") val territoryGained: Int = 0,
    @SerialName("territory_lost") val territoryLost: Int = 0,
    @SerialName("turning_point") val turningPoint: JsonObject = JsonObject(emptyMap()),
    @SerialName("score_breakdown") val scoreBreakdown: JsonObject = JsonObject(emptyMap()),
)

suspend fun OnlineGameBackend.getVipMatchAnalysis(matchId: String, mode: String): VipMatchAnalysisDto =
    SupabaseProvider.client.postgrest.rpc(
        "get_vip_match_analysis_v1",
        buildJsonObject {
            put("p_match_id", matchId)
            put("p_mode", mode)
        },
    ).decodeAs()
