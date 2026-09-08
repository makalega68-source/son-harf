package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VipEntitlementsDto(
    @SerialName("is_vip") val isVip: Boolean = false,
    @SerialName("daily_jokers_claimed") val dailyJokersClaimed: Boolean = false,
    @SerialName("freezer_count") val freezerCount: Int = 0,
    @SerialName("swap_count") val swapCount: Int = 0,
    @SerialName("hint_count") val hintCount: Int = 0,
    @SerialName("multiplier_count") val multiplierCount: Int = 0,
    @SerialName("streak_shield_count") val streakShieldCount: Int = 0,
    @SerialName("xp_multiplier") val xpMultiplier: Int = 1,
    @SerialName("diamond_multiplier") val coinMultiplier: Int = 1,
    @SerialName("rewarded_ad_bypass") val rewardedAdBypass: Boolean = false,
    @SerialName("used_words_access") val usedWordsAccess: Boolean = true,
    @SerialName("direct_messages_access") val directMessagesAccess: Boolean = true,
    @SerialName("ranked_live_assist") val rankedLiveAssist: Boolean = false,
    @SerialName("post_match_analysis") val postMatchAnalysis: Boolean = false,
    @SerialName("saved_friend_list") val savedFriendList: Boolean = false,
    @SerialName("private_rooms") val privateRooms: Boolean = false,
)

@Serializable
data class VipDailyHelperClaimDto(
    val success: Boolean = false,
    @SerialName("already_claimed") val alreadyClaimed: Boolean = false,
    @SerialName("freezer_count") val freezerCount: Int = 0,
    @SerialName("swap_count") val swapCount: Int = 0,
    @SerialName("hint_count") val hintCount: Int = 0,
    @SerialName("multiplier_count") val multiplierCount: Int = 0,
    @SerialName("streak_shield_count") val streakShieldCount: Int = 0,
)

suspend fun OnlineGameBackend.getVipEntitlements(): VipEntitlementsDto =
    SupabaseProvider.client.postgrest.rpc("get_vip_entitlements_v7").decodeSingle()

suspend fun OnlineGameBackend.claimVipDailyHelpers(): VipDailyHelperClaimDto =
    SupabaseProvider.client.postgrest.rpc("claim_vip_daily_jokers_v7").decodeSingle()
