package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class AtelierDailyStartDto(
    val started: Boolean = false,
    @SerialName("already_played") val alreadyPlayed: Boolean = false,
    val finished: Boolean = false,
    val score: Int = 0,
    val day: String = "",
)

@Serializable
data class AtelierDailyFinishDto(
    val rank: Int = 0,
    val total: Int = 0,
    val score: Int = 0,
)

@Serializable
data class AtelierBoardRowDto(
    val rank: Int = 0,
    @SerialName("user_id") val userId: String = "",
    val score: Int = 0,
    val days: Int = 0,
    val name: String = "",
    @SerialName("avatar_path") val avatarPath: String? = null,
    val me: Boolean = false,
)

@Serializable
data class AtelierBoardMeDto(val rank: Int = 0, val score: Int = 0)

@Serializable
data class AtelierTodayDto(val started: Boolean = false, val finished: Boolean = false, val score: Int = 0)

@Serializable
data class AtelierBoardDto(
    val scope: String = "daily",
    val day: String = "",
    val rows: List<AtelierBoardRowDto> = emptyList(),
    val me: AtelierBoardMeDto? = null,
    val total: Int = 0,
    val today: AtelierTodayDto = AtelierTodayDto(),
)

@Serializable
data class AtelierWeeklyRewardDto(
    val rank: Int? = null,
    val reward: Int = 0,
    val claimed: Boolean = false,
)

@Serializable
data class AtelierWeeklyClaimDto(
    val claimed: Boolean = false,
    val rank: Int? = null,
    val reward: Int = 0,
    val reason: String? = null,
)

/**
 * Kelime Atölyesi competition: one official daily run per language (same starting letters for
 * everyone), today's and this week's leaderboards, and last week's top-10 Son Coin reward.
 */
object AtelierCompetitionBackend {
    suspend fun startDaily(language: String): AtelierDailyStartDto =
        SupabaseProvider.client.postgrest.rpc("start_atelier_daily_v1", buildJsonObject { put("p_language", language) })
            .decodeAs<AtelierDailyStartDto>()

    suspend fun finishDaily(language: String, score: Int, words: Int, tasks: Int): AtelierDailyFinishDto =
        SupabaseProvider.client.postgrest.rpc(
            "finish_atelier_daily_v1",
            buildJsonObject {
                put("p_language", language)
                put("p_score", score)
                put("p_words", words)
                put("p_tasks", tasks)
            },
        ).decodeAs<AtelierDailyFinishDto>()

    suspend fun board(language: String, weekly: Boolean): AtelierBoardDto =
        SupabaseProvider.client.postgrest.rpc(
            "get_atelier_board_v1",
            buildJsonObject {
                put("p_language", language)
                put("p_scope", if (weekly) "weekly" else "daily")
            },
        ).decodeAs<AtelierBoardDto>()

    suspend fun weeklyReward(language: String): AtelierWeeklyRewardDto =
        SupabaseProvider.client.postgrest.rpc("get_atelier_weekly_reward_status_v1", buildJsonObject { put("p_language", language) })
            .decodeAs<AtelierWeeklyRewardDto>()

    suspend fun claimWeeklyReward(language: String): AtelierWeeklyClaimDto =
        SupabaseProvider.client.postgrest.rpc("claim_atelier_weekly_reward_v1", buildJsonObject { put("p_language", language) })
            .decodeAs<AtelierWeeklyClaimDto>()
}
