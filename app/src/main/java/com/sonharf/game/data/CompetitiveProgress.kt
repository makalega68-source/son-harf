package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class CompetitiveSeasonDto(
    @SerialName("season_id") val seasonId: String,
    @SerialName("name_tr") val nameTr: String,
    @SerialName("name_en") val nameEn: String,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("ends_at") val endsAt: String,
    val rating: Int = 1000,
    @SerialName("peak_rating") val peakRating: Int = 1000,
    @SerialName("league_name") val leagueName: String = "BRONZ",
    @SerialName("season_rank") val seasonRank: Long = 0,
    @SerialName("player_count") val playerCount: Long = 0,
    val matches: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    @SerialName("valid_words") val validWords: Int = 0,
    @SerialName("next_rating") val nextRating: Int? = null,
    @SerialName("points_to_next") val pointsToNext: Int = 0,
    @SerialName("honor_count") val honorCount: Long = 0,
    @SerialName("latest_honor_tr") val latestHonorTr: String? = null,
    @SerialName("latest_honor_en") val latestHonorEn: String? = null,
)

@Serializable
data class SeasonLeaderboardRowDto(
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String,
    val rating: Int,
    @SerialName("peak_rating") val peakRating: Int,
    @SerialName("league_name") val leagueName: String,
    val matches: Int,
    val wins: Int,
    val losses: Int,
    val draws: Int,
    @SerialName("win_rate") val winRate: Double = 0.0,
    @SerialName("rank_no") val rankNo: Long,
)

@Serializable
data class AchievementProgressDto(
    val code: String,
    val icon: String,
    @SerialName("title_tr") val titleTr: String,
    @SerialName("title_en") val titleEn: String,
    @SerialName("description_tr") val descriptionTr: String,
    @SerialName("description_en") val descriptionEn: String,
    @SerialName("current_value") val currentValue: Int,
    val target: Int,
    @SerialName("reward_coin") val rewardCoin: Int,
    val unlocked: Boolean,
    @SerialName("unlocked_at") val unlockedAt: String? = null,
)

suspend fun OnlineGameBackend.getCompetitiveSeason(): CompetitiveSeasonDto =
    SupabaseProvider.client.postgrest.rpc("get_competitive_season_v1").decodeSingle()

suspend fun OnlineGameBackend.getSeasonLeaderboard(limit: Int = 50): List<SeasonLeaderboardRowDto> =
    SupabaseProvider.client.postgrest.rpc(
        "get_season_leaderboard_v1",
        buildJsonObject { put("p_limit", limit.coerceIn(1, 100)) },
    ).decodeList()

suspend fun OnlineGameBackend.getAchievements(): List<AchievementProgressDto> =
    SupabaseProvider.client.postgrest.rpc("get_achievements_v1").decodeList()
