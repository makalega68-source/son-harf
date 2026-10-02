package com.sonharf.game.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
internal data class SocialActivityDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("actor_id") val actorId: String? = null,
    val kind: String,
    @SerialName("target_kind") val targetKind: String,
    @SerialName("target_id") val targetId: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("read_at") val readAt: String? = null,
)
internal suspend fun OnlineGameBackend.getSocialInbox(): List<SocialActivityDto> {
    val me = currentUserId() ?: return emptyList()
    return SupabaseProvider.client.postgrest.rpc("get_social_inbox_v1").decodeList<SocialActivityDto>()
}
internal suspend fun OnlineGameBackend.markSocialActivityRead(id: String? = null) {
    val me = currentUserId() ?: return
    SupabaseProvider.client.from("social_activity").update(buildJsonObject { put("read_at", java.time.Instant.now().toString()) }) {
        filter { eq("user_id", me); if (id != null) eq("id", id) }
    }
}
internal suspend fun OnlineGameBackend.getLastLetterRooms(): List<GameRoomDto> {
    val me = currentUserId() ?: return emptyList()
    return SupabaseProvider.client.postgrest.rpc("get_my_last_letter_rooms_v1").decodeList<GameRoomDto>()
}
internal suspend fun OnlineGameBackend.getInviteCode(): String =
    SupabaseProvider.client.postgrest.rpc("get_my_invite_code_v1").decodeAs()
internal suspend fun OnlineGameBackend.useInviteCode(code: String): String =
    SupabaseProvider.client.postgrest.rpc("use_player_invite_code_v1",buildJsonObject { put("p_code",code.trim().uppercase()) }).decodeAs()

internal suspend fun OnlineGameBackend.requestWordSiegeRematch(gameId: String): WordSiegeInviteDto =
    SupabaseProvider.client.postgrest.rpc("request_word_siege_rematch_v1", buildJsonObject { put("p_game_id",gameId) }).decodeAs()
