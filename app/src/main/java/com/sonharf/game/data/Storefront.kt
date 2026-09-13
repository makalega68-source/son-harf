package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.delay

@Serializable
data class StoreBundleDto(
    val id: String,
    @SerialName("name_tr") val nameTr: String,
    @SerialName("name_en") val nameEn: String,
    @SerialName("diamond_price") val diamondPrice: Int,
    val section: String,
    @SerialName("available_until") val availableUntil: String? = null,
    val owned: Boolean = false,
    val items: List<ShopItemDto> = emptyList(),
)
@Serializable
data class StorefrontDto(
    @SerialName("daily_claimed") val dailyClaimed: Boolean = false,
    @SerialName("daily_reward") val dailyReward: Int = 0,
    @SerialName("rewarded_enabled") val rewardedEnabled: Boolean = false,
    val bundles: List<StoreBundleDto> = emptyList(),
)
suspend fun OnlineGameBackend.getStorefront(): StorefrontDto =
    SupabaseProvider.client.postgrest.rpc("get_storefront_v1").decodeAs()
suspend fun OnlineGameBackend.purchaseStoreBundle(bundleId: String) {
    SupabaseProvider.client.postgrest.rpc("purchase_store_bundle_v1", buildJsonObject { put("p_bundle_id", bundleId) })
}
suspend fun OnlineGameBackend.prepareStoreReward(rewardType: String, trialItemId: String? = null): String =
    SupabaseProvider.client.postgrest.rpc("prepare_store_ad_v1", buildJsonObject {
        put("p_reward_type", rewardType)
        if (trialItemId != null) put("p_trial_item_id", trialItemId)
    }).decodeAs()
suspend fun OnlineGameBackend.awaitVerifiedStoreReward(rewardType: String, intentId: String, trialItemId: String? = null): RewardClaimDto {
    repeat(6) { attempt ->
        try { return claimRewardedAd(rewardType, intentId, trialItemId) }
        catch (error: Exception) {
            if ("ad_verification_pending" !in error.message.orEmpty() || attempt == 5) throw error
            delay(1_500)
        }
    }
    error("ad_verification_pending")
}
