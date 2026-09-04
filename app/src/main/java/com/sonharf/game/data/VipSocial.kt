package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class VipSavedSocialDto(
    @SerialName("other_user_id") val otherUserId: String,
    @SerialName("display_name") val displayName: String,
    val favorite: Boolean = false,
    @SerialName("arch_rival") val archRival: Boolean = false,
    @SerialName("presence_status") val presenceStatus: String = "hidden",
    @SerialName("last_seen_at") val lastSeenAt: String? = null,
    @SerialName("is_friend") val isFriend: Boolean = false,
    @SerialName("can_invite") val canInvite: Boolean = false,
)

suspend fun OnlineGameBackend.setVipRelationshipMark(
    otherUserId: String,
    favorite: Boolean,
    archRival: Boolean,
) {
    SupabaseProvider.client.postgrest.rpc(
        "set_vip_relationship_mark_v1",
        buildJsonObject {
            put("p_other_user_id", otherUserId)
            put("p_favorite", favorite)
            put("p_arch_rival", archRival)
        },
    )
}

suspend fun OnlineGameBackend.getVipSavedSocial(): List<VipSavedSocialDto> =
    SupabaseProvider.client.postgrest.rpc("get_vip_saved_social_v1").decodeList()
