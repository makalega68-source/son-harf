package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class DisplayNameChangeDto(
    val changed: Boolean = false,
    @SerialName("display_name") val displayName: String = "",
)

/** Player-name change: 2-24 characters, unique, at most once a day (checked on the server). */
object DisplayNameBackend {
    suspend fun change(name: String): DisplayNameChangeDto =
        SupabaseProvider.client.postgrest.rpc("change_display_name_v1", buildJsonObject { put("p_name", name) })
            .decodeAs<DisplayNameChangeDto>()
}
