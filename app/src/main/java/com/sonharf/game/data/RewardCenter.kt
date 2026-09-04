package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class RewardCenterStatusDto(
    @SerialName("coin_ads_used") val coinAdsUsed: Int = 0,
    @SerialName("coin_ads_limit") val coinAdsLimit: Int = 3,
    @SerialName("coin_per_ad") val coinPerAd: Int = 10,
    @SerialName("trial_ads_used") val trialAdsUsed: Int = 0,
    @SerialName("trial_ads_limit") val trialAdsLimit: Int = 1,
    @SerialName("trial_item_id") val trialItemId: String? = null,
    @SerialName("trial_mode") val trialMode: String? = null,
    @SerialName("trial_matches_remaining") val trialMatchesRemaining: Int? = null,
    @SerialName("trial_expires_at") val trialExpiresAt: String? = null,
    @SerialName("piggy_tier") val piggyTier: Int = 0,
    @SerialName("piggy_bonus_sc") val piggyBonusSc: Int = 0,
    @SerialName("piggy_match_progress") val piggyMatchProgress: Int = 0,
    @SerialName("piggy_match_target") val piggyMatchTarget: Int = 8,
)

@Serializable
data class RewardClaimDto(
    val success: Boolean = false,
    @SerialName("reward_type") val rewardType: String? = null,
    @SerialName("diamonds_awarded") val diamondsAwarded: Int = 0,
    @SerialName("trial_item_id") val trialItemId: String? = null,
    @SerialName("trial_mode") val trialMode: String? = null,
    @SerialName("trial_matches_remaining") val trialMatchesRemaining: Int? = null,
    @SerialName("trial_expires_at") val trialExpiresAt: String? = null,
    @SerialName("bonus_sc") val bonusSc: Int = 0,
    val diamonds: Int? = null,
    val balance: Int? = null,
)

suspend fun OnlineGameBackend.getRewardCenterStatus(): RewardCenterStatusDto =
    SupabaseProvider.client.postgrest.rpc("get_store_reward_status_v1").decodeSingle()

suspend fun OnlineGameBackend.claimRewardedAd(
    rewardType: String,
    adResponseId: String,
    trialItemId: String? = null,
): RewardClaimDto =
    SupabaseProvider.client.postgrest.rpc(
        "claim_store_rewarded_ad_v1",
        buildJsonObject {
            put("p_reward_type", rewardType)
            put("p_ad_response_id", adResponseId)
            if (trialItemId != null) put("p_trial_item_id", trialItemId)
        },
    ).decodeSingle()

suspend fun OnlineGameBackend.openPiggyBank(): RewardClaimDto =
    SupabaseProvider.client.postgrest.rpc("open_piggy_bank_v2").decodeSingle()

suspend fun OnlineGameBackend.equipRewardTrial(): RewardClaimDto =
    SupabaseProvider.client.postgrest.rpc("equip_style_trial_v1").decodeSingle()
