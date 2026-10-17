package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class HintBuyDto(
    val success: Boolean = false,
    @SerialName("son_coin_spent") val spent: Int = 0,
    @SerialName("son_coin_balance") val balance: Int = 0,
)

/** Extra in-game hints bought with Son Coin (Son Harf and Kelime Atölyesi). */
object GameHintBackend {
    const val HINT_PRICE = 25

    /** Spends [HINT_PRICE] Son Coin on one extra hint; null when it could not be bought. */
    suspend fun buyHint(game: String): HintBuyDto? = runCatching {
        SupabaseProvider.client.postgrest.rpc("buy_game_hint_v1", buildJsonObject { put("p_game", game) })
            .decodeList<HintBuyDto>().firstOrNull()?.takeIf { it.success }
    }.getOrNull()
}
