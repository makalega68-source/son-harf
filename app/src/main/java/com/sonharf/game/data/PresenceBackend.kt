package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ComebackGiftDto(
    val gift: Int = 0,
    @SerialName("days_away") val daysAway: Int = 0,
)

/**
 * Marks the player as seen. A player back after 3+ days away gets a Son Coin gift from Obi
 * (at most once every 14 days); the server decides and credits it.
 */
object PresenceBackend {
    suspend fun touch(): ComebackGiftDto =
        SupabaseProvider.client.postgrest.rpc("touch_presence_v1").decodeAs<ComebackGiftDto>()
}
