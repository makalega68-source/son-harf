package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class StoreSeasonRewardDto(
    val level: Int,
    val track: String,
    @SerialName("reward_type") val rewardType: String,
    @SerialName("reward_key") val rewardKey: String = "",
    val amount: Int = 0,
    val unlocked: Boolean = false,
    @SerialName("premium_access") val premiumAccess: Boolean = false,
    val claimed: Boolean = false,
)

@Serializable
data class StoreSeasonDto(
    val active: Boolean = false,
    @SerialName("season_id") val seasonId: String? = null,
    @SerialName("starts_at") val startsAt: String? = null,
    @SerialName("ends_at") val endsAt: String? = null,
    @SerialName("duration_days") val durationDays: Int = 0,
    val level: Int = 1,
    @SerialName("premium_active") val premiumActive: Boolean = false,
    val rewards: List<StoreSeasonRewardDto> = emptyList(),
)

suspend fun OnlineGameBackend.getStoreSeason(): StoreSeasonDto =
    SupabaseProvider.client.postgrest.rpc("get_store_season_v1").decodeSingle()

suspend fun OnlineGameBackend.claimStoreSeasonReward(reward: StoreSeasonRewardDto) {
    SupabaseProvider.client.postgrest.rpc(
        "claim_store_season_reward_v1",
        buildJsonObject {
            put("p_level", reward.level)
            put("p_track", reward.track)
            put("p_reward_type", reward.rewardType)
            put("p_reward_key", reward.rewardKey)
        },
    )
}
