package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// Kelime Kuşatması meta layer (supabase/migrations/20260924190000_siege_meta_v1.sql).
// Every value is computed and every reward is granted on the server for auth.uid().

@Serializable
data class SiegeMatchHistoryDto(
    @SerialName("game_id") val gameId: String,
    @SerialName("opponent_id") val opponentId: String,
    @SerialName("display_name") val displayName: String,
    val result: String,
    @SerialName("my_word_score") val myWordScore: Int,
    @SerialName("my_area_score") val myAreaScore: Int,
    @SerialName("their_word_score") val theirWordScore: Int,
    @SerialName("their_area_score") val theirAreaScore: Int,
    @SerialName("my_cells") val myCells: Int,
    @SerialName("their_cells") val theirCells: Int,
    @SerialName("finished_at") val finishedAt: String? = null,
)

@Serializable
data class SiegeRivalDto(
    @SerialName("opponent_id") val opponentId: String,
    @SerialName("display_name") val displayName: String,
    val matches: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    @SerialName("last10_wins") val last10Wins: Int = 0,
    @SerialName("last10_losses") val last10Losses: Int = 0,
    @SerialName("last_played_at") val lastPlayedAt: String? = null,
    @SerialName("is_friend") val isFriend: Boolean = false,
)

@Serializable
data class SiegeMissionDto(
    @SerialName("mission_id") val missionId: String,
    @SerialName("title_tr") val titleTr: String,
    @SerialName("title_en") val titleEn: String,
    val target: Int,
    val progress: Int,
    @SerialName("reward_coins") val rewardCoins: Int,
    val completed: Boolean,
    val claimed: Boolean,
    @SerialName("period_start") val periodStart: String,
)

@Serializable
data class SiegeMissionClaimDto(
    val success: Boolean = false,
    @SerialName("mission_id") val missionId: String = "",
    @SerialName("reward_coins") val rewardCoins: Int = 0,
    val balance: Int = 0,
)

@Serializable
data class DailyRewardCycleDto(
    @SerialName("cycle_day") val cycleDay: Int,
    val streak: Int,
    @SerialName("claimed_today") val claimedToday: Boolean,
    @SerialName("today_reward") val todayReward: Int,
    val rewards: List<Int> = emptyList(),
    val vip: Boolean = false,
)

@Serializable
data class DailyRewardClaimDto(
    val success: Boolean = false,
    val reason: String? = null,
    @SerialName("cycle_day") val cycleDay: Int = 0,
    val reward: Int = 0,
    val balance: Int = 0,
)

@Serializable
data class PublicCosmeticsDto(
    @SerialName("profile_frame_id") val profileFrameId: String? = null,
    @SerialName("name_style_id") val nameStyleId: String? = null,
    @SerialName("badge_id") val badgeId: String? = null,
    @SerialName("title_style_id") val titleStyleId: String? = null,
    @SerialName("nameplate_id") val nameplateId: String? = null,
)

suspend fun OnlineGameBackend.getSiegeMatchHistory(limit: Int = 30): List<SiegeMatchHistoryDto> =
    SupabaseProvider.client.postgrest.rpc(
        "get_siege_match_history_v1",
        buildJsonObject { put("p_limit", limit.coerceIn(1, 100)) },
    ).decodeList()

suspend fun OnlineGameBackend.getSiegeRivals(limit: Int = 20): List<SiegeRivalDto> =
    SupabaseProvider.client.postgrest.rpc(
        "get_siege_rivals_v1",
        buildJsonObject { put("p_limit", limit.coerceIn(1, 50)) },
    ).decodeList()

suspend fun OnlineGameBackend.getSiegeMissions(): List<SiegeMissionDto> =
    SupabaseProvider.client.postgrest.rpc("get_siege_missions_v1").decodeList()

suspend fun OnlineGameBackend.claimSiegeMission(missionId: String): SiegeMissionClaimDto =
    SupabaseProvider.client.postgrest.rpc(
        "claim_siege_mission_v1",
        buildJsonObject { put("p_mission_id", missionId) },
    ).decodeAs()

suspend fun OnlineGameBackend.getDailyRewardCycle(): DailyRewardCycleDto? =
    SupabaseProvider.client.postgrest.rpc("get_daily_reward_cycle_v1").decodeList<DailyRewardCycleDto>().firstOrNull()

suspend fun OnlineGameBackend.claimDailyRewardCycle(): DailyRewardClaimDto =
    SupabaseProvider.client.postgrest.rpc("claim_daily_reward_cycle_v1").decodeAs()

suspend fun OnlineGameBackend.getPublicCosmetics(userId: String): PublicCosmeticsDto? =
    SupabaseProvider.client.postgrest.rpc(
        "get_public_cosmetics_v1",
        buildJsonObject { put("p_user_id", userId) },
    ).decodeList<PublicCosmeticsDto>().firstOrNull()
