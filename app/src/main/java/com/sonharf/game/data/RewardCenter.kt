package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class RewardCenterStatusDto(
    @SerialName("diamond_ads_used") val diamondAdsUsed: Int = 0,
    @SerialName("diamond_ads_limit") val diamondAdsLimit: Int = 3,
    @SerialName("diamond_per_ad") val diamondPerAd: Int = 10,
    @SerialName("chest_ads_used") val chestAdsUsed: Int = 0,
    @SerialName("chest_ads_limit") val chestAdsLimit: Int = 2,
    @SerialName("trial_ads_used") val trialAdsUsed: Int = 0,
    @SerialName("trial_ads_limit") val trialAdsLimit: Int = 1,
    @SerialName("chest_keys") val chestKeys: Int = 0,
    @SerialName("trial_item_id") val trialItemId: String? = null,
    @SerialName("trial_expires_at") val trialExpiresAt: String? = null,
)

@Serializable
data class RewardClaimDto(
    val success: Boolean = false,
    @SerialName("reward_type") val rewardType: String? = null,
    @SerialName("diamonds_awarded") val diamondsAwarded: Int = 0,
    @SerialName("chest_keys_awarded") val chestKeysAwarded: Int = 0,
    @SerialName("trial_item_id") val trialItemId: String? = null,
    @SerialName("trial_expires_at") val trialExpiresAt: String? = null,
    val diamonds: Int? = null,
    @SerialName("chest_keys") val chestKeys: Int? = null,
)

suspend fun OnlineGameBackend.getRewardCenterStatus(): RewardCenterStatusDto =
    SupabaseProvider.client.postgrest.rpc("get_reward_center_status").decodeSingle()

suspend fun OnlineGameBackend.claimRewardedAd(rewardType: String, adResponseId: String): RewardClaimDto =
    SupabaseProvider.client.postgrest.rpc(
        "claim_rewarded_ad",
        buildJsonObject {
            put("p_reward_type", rewardType)
            put("p_ad_response_id", adResponseId)
        },
    ).decodeSingle()

suspend fun OnlineGameBackend.openRewardChest(): RewardClaimDto =
    SupabaseProvider.client.postgrest.rpc("open_reward_chest").decodeSingle()

suspend fun OnlineGameBackend.equipRewardTrial(): RewardClaimDto =
    SupabaseProvider.client.postgrest.rpc("equip_reward_trial").decodeSingle()
