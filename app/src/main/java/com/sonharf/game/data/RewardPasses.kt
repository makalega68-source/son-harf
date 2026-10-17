package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Rewarded-video reward kinds. Quotas and eligibility are enforced by the server. */
object RewardKeys {
    const val COINS = "diamonds"
    const val KEYBOARD_DAY = "keyboard_day"
    const val THEME_DAY = "theme_day"
    const val QUICK_GAMES = "quick_games"
    const val HINTS_SON_HARF = "hints_son_harf"
    const val HINTS_SIEGE = "hints_siege"
    const val HINTS_WORKSHOP = "hints_workshop"
    const val DAILY_DOUBLE = "daily_double"
}

@Serializable
data class RewardPassItemDto(
    @SerialName("item_id") val itemId: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
)

@Serializable
data class RewardUsageDto(
    val key: String,
    val max: Int = 0,
    val used: Int = 0,
    val amount: Int = 0,
    @SerialName("period_days") val periodDays: Int = 1,
)

@Serializable
data class RewardPassesDto(
    @SerialName("hints_son_harf") val hintsSonHarf: Int = 0,
    @SerialName("hints_siege") val hintsSiege: Int = 0,
    @SerialName("hints_workshop") val hintsWorkshop: Int = 0,
    val keyboard: RewardPassItemDto? = null,
    val theme: RewardPassItemDto? = null,
    @SerialName("quick_games_left") val quickGamesLeft: Int = 0,
    @SerialName("quick_games_expires_at") val quickGamesExpiresAt: String? = null,
    val usage: List<RewardUsageDto> = emptyList(),
) {
    fun usage(key: String): RewardUsageDto? = usage.firstOrNull { it.key == key }
    fun left(key: String): Int = usage(key)?.let { (it.max - it.used).coerceAtLeast(0) } ?: 0
}

suspend fun OnlineGameBackend.getRewardPasses(): RewardPassesDto =
    SupabaseProvider.client.postgrest.rpc("get_reward_passes_v1").decodeAs()

/** Spends one rewarded hint ('son_harf', 'siege', 'kelime_atolyesi'); returns how many are left. */
suspend fun OnlineGameBackend.useRewardHint(game: String): Int =
    SupabaseProvider.client.postgrest.rpc("use_reward_hint_v1", buildJsonObject { put("p_game", game) }).decodeAs()
