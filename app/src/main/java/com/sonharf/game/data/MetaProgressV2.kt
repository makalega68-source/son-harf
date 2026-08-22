package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class MetaProgressV2Dto(
    @SerialName("season_id") val seasonId: String,
    @SerialName("season_name") val seasonName: String,
    @SerialName("season_day") val seasonDay: Int = 1,
    @SerialName("season_days") val seasonDays: Int = 30,
    @SerialName("season_xp") val seasonXp: Int = 0,
    @SerialName("season_level") val seasonLevel: Int = 1,
    @SerialName("season_progress") val seasonProgress: Int = 0,
    @SerialName("season_target") val seasonTarget: Int = 300,
    @SerialName("daily_play_streak") val dailyPlayStreak: Int = 0,
    @SerialName("best_daily_play_streak") val bestDailyPlayStreak: Int = 0,
    @SerialName("unique_words") val uniqueWords: Int = 0,
    @SerialName("longest_word") val longestWord: String = "",
    @SerialName("longest_word_length") val longestWordLength: Int = 0,
    @SerialName("highest_score") val highestScore: Int = 0,
    @SerialName("best_streak") val bestStreak: Int = 0,
    @SerialName("cup_points") val cupPoints: Int = 0,
    @SerialName("cup_rank") val cupRank: Int = 0,
    @SerialName("cup_qualified") val cupQualified: Boolean = false,
    @SerialName("cup_active") val cupActive: Boolean = false,
    @SerialName("selected_title") val selectedTitle: String = "ÇAYLAK",
    @SerialName("available_titles") val availableTitles: Int = 1,
    @SerialName("season_pass_active") val seasonPassActive: Boolean = false,
    @SerialName("free_claimed_tiers") val freeClaimedTiers: List<Int> = emptyList(),
    @SerialName("premium_claimed_tiers") val premiumClaimedTiers: List<Int> = emptyList(),
)

suspend fun OnlineGameBackend.getMetaProgressV2(): MetaProgressV2Dto =
    SupabaseProvider.client.postgrest.rpc("get_meta_progress_v2").decodeSingle()

suspend fun OnlineGameBackend.claimSeasonReward(tier: Int, premium: Boolean): Int =
    SupabaseProvider.client.postgrest.rpc(
        "claim_season_reward_v1",
        buildJsonObject {
            put("p_tier", tier)
            put("p_premium", premium)
        },
    ).decodeSingle()

suspend fun OnlineGameBackend.setSelectedTitle(title: String): String =
    SupabaseProvider.client.postgrest.rpc(
        "set_selected_title_v1",
        buildJsonObject { put("p_title", title) },
    ).decodeSingle()
