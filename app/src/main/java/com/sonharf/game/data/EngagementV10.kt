package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class DailyGoalV10Dto(
    val id: String,
    @SerialName("title_tr") val titleTr: String,
    @SerialName("title_en") val titleEn: String,
    val target: Int,
    @SerialName("reward_diamonds") val rewardDiamonds: Int,
    val progress: Int,
    val claimed: Boolean,
)

@Serializable
data class MatchResultV10Dto(
    val won: Boolean,
    @SerialName("xp_gain") val xpGain: Int,
    @SerialName("diamonds_awarded") val diamondsAwarded: Int,
    @SerialName("league_points") val leaguePoints: Int,
    @SerialName("current_rating") val currentRating: Int,
    @SerialName("current_streak") val currentStreak: Int,
)

@Serializable
data class MetaDashboardV10Dto(
    @SerialName("total_matches") val totalMatches: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    @SerialName("win_rate") val winRate: Int = 0,
    @SerialName("valid_words") val validWords: Int = 0,
    @SerialName("longest_word") val longestWord: String = "-",
    @SerialName("favorite_start_letter") val favoriteStartLetter: String = "-",
    @SerialName("best_streak") val bestStreak: Int = 0,
    val rating: Int = 1000,
    @SerialName("season_league") val seasonLeague: String = "BRONZ",
    @SerialName("achievements_unlocked") val achievementsUnlocked: Int = 0,
    @SerialName("achievement_total") val achievementTotal: Int = 10,
    @SerialName("checkin_streak") val checkinStreak: Int = 0,
)

suspend fun OnlineGameBackend.getDailyGoalsV10(): List<DailyGoalV10Dto> =
    SupabaseProvider.client.postgrest.rpc("get_daily_goals_v10").decodeList()

suspend fun OnlineGameBackend.claimDailyGoalV10(goalId: String): Int =
    SupabaseProvider.client.postgrest.rpc(
        "claim_daily_goal_v10",
        buildJsonObject { put("p_goal_id", goalId) },
    ).decodeSingle()

suspend fun OnlineGameBackend.getMetaDashboardV10(): MetaDashboardV10Dto =
    SupabaseProvider.client.postgrest.rpc("get_meta_dashboard_v10").decodeSingle()

suspend fun OnlineGameBackend.claimMatchResultV10(roomId: String): MatchResultV10Dto =
    SupabaseProvider.client.postgrest.rpc(
        "claim_match_result_v10",
        buildJsonObject { put("p_room_id", roomId) },
    ).decodeSingle()
