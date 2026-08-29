package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class TeamArenaActiveDto(
    val active: Boolean = false,
    @SerialName("room_id") val roomId: String? = null,
    val status: String? = null,
)

@Serializable
data class TeamArenaCreateDto(
    @SerialName("room_id") val roomId: String,
    val status: String,
    val language: String,
    @SerialName("expires_at") val expiresAt: String,
)

@Serializable
data class TeamArenaInviteCreatedDto(
    @SerialName("invite_id") val inviteId: String,
    @SerialName("room_id") val roomId: String,
    @SerialName("receiver_id") val receiverId: String,
    val team: Int,
    val seat: Int,
    @SerialName("expires_at") val expiresAt: String,
)

@Serializable
data class TeamArenaInviteDto(
    @SerialName("invite_id") val inviteId: String,
    @SerialName("room_id") val roomId: String,
    @SerialName("sender_id") val senderId: String,
    val language: String,
    val team: Int,
    val seat: Int,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class TeamArenaActionDto(
    val status: String = "",
    @SerialName("room_id") val roomId: String? = null,
    val team: Int? = null,
    val seat: Int? = null,
    @SerialName("starts_at") val startsAt: String? = null,
    @SerialName("ends_at") val endsAt: String? = null,
)

@Serializable
data class TeamArenaRoomDto(
    @SerialName("room_id") val roomId: String,
    @SerialName("host_id") val hostId: String,
    val language: String,
    val status: String,
    val letters: String? = null,
    @SerialName("starts_at") val startsAt: String? = null,
    @SerialName("ends_at") val endsAt: String? = null,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("team_a_score") val teamAScore: Int = 0,
    @SerialName("team_b_score") val teamBScore: Int = 0,
    @SerialName("winner_team") val winnerTeam: Int? = null,
    @SerialName("my_team") val myTeam: Int,
    @SerialName("is_host") val isHost: Boolean = false,
    @SerialName("member_count") val memberCount: Long = 0,
    @SerialName("ready_count") val readyCount: Long = 0,
)

@Serializable
data class TeamArenaMemberDto(
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String,
    val team: Int,
    val seat: Int,
    val ready: Boolean = false,
    @SerialName("is_host") val isHost: Boolean = false,
    @SerialName("presence_status") val presenceStatus: String = "offline",
)

@Serializable
data class TeamArenaWordDto(
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String,
    val team: Int,
    val word: String,
    @SerialName("normalized_word") val normalizedWord: String,
    @SerialName("base_points") val basePoints: Int = 0,
    val combo: Int = 1,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class TeamArenaSubmitDto(
    val accepted: Boolean = false,
    val status: String = "",
    val word: String? = null,
    @SerialName("normalized_word") val normalizedWord: String? = null,
    @SerialName("base_points") val basePoints: Int = 0,
    val combo: Int = 1,
    val team: Int? = null,
    @SerialName("team_score") val teamScore: Int = 0,
    @SerialName("opponent_score") val opponentScore: Int = 0,
    @SerialName("team_word_count") val teamWordCount: Int = 0,
)

@Serializable
data class TeamArenaRematchDto(
    val status: String = "",
    @SerialName("room_id") val roomId: String,
    @SerialName("invited_count") val invitedCount: Int = 0,
    val reused: Boolean = false,
)

suspend fun OnlineGameBackend.getMyActiveTeamArena(): TeamArenaActiveDto =
    SupabaseProvider.client.postgrest.rpc("get_my_active_team_arena_v1").decodeSingle()

suspend fun OnlineGameBackend.createTeamArena(language: String): TeamArenaCreateDto =
    SupabaseProvider.client.postgrest.rpc(
        "create_team_arena_v1",
        buildJsonObject {
            put("p_language", if (language.lowercase() == "en") "en" else "tr")
        },
    ).decodeSingle()

suspend fun OnlineGameBackend.inviteFriendToTeamArena(
    roomId: String,
    friendId: String,
    team: Int,
): TeamArenaInviteCreatedDto =
    SupabaseProvider.client.postgrest.rpc(
        "invite_friend_to_team_arena_v1",
        buildJsonObject {
            put("p_room_id", roomId)
            put("p_friend_id", friendId)
            put("p_team", team.coerceIn(1, 2))
        },
    ).decodeSingle()

suspend fun OnlineGameBackend.getIncomingTeamArenaInvites(): List<TeamArenaInviteDto> =
    SupabaseProvider.client.postgrest.rpc("get_incoming_team_arena_invites_v1").decodeList()

suspend fun OnlineGameBackend.respondTeamArenaInvite(
    inviteId: String,
    accept: Boolean,
): TeamArenaActionDto =
    SupabaseProvider.client.postgrest.rpc(
        "respond_team_arena_invite_v1",
        buildJsonObject {
            put("p_invite_id", inviteId)
            put("p_accept", accept)
        },
    ).decodeSingle()

suspend fun OnlineGameBackend.getTeamArenaRoom(roomId: String): TeamArenaRoomDto =
    SupabaseProvider.client.postgrest.rpc(
        "get_team_arena_room_v1",
        buildJsonObject { put("p_room_id", roomId) },
    ).decodeSingle()

suspend fun OnlineGameBackend.getTeamArenaMembers(roomId: String): List<TeamArenaMemberDto> =
    SupabaseProvider.client.postgrest.rpc(
        "get_team_arena_members_v1",
        buildJsonObject { put("p_room_id", roomId) },
    ).decodeList()

suspend fun OnlineGameBackend.setTeamArenaReady(roomId: String, ready: Boolean): Boolean =
    SupabaseProvider.client.postgrest.rpc(
        "set_team_arena_ready_v1",
        buildJsonObject {
            put("p_room_id", roomId)
            put("p_ready", ready)
        },
    ).decodeSingle()

suspend fun OnlineGameBackend.startTeamArena(roomId: String): TeamArenaActionDto =
    SupabaseProvider.client.postgrest.rpc(
        "start_team_arena_v1",
        buildJsonObject { put("p_room_id", roomId) },
    ).decodeSingle()

suspend fun OnlineGameBackend.submitTeamArenaWord(roomId: String, word: String): TeamArenaSubmitDto =
    SupabaseProvider.client.postgrest.rpc(
        "submit_team_arena_word_v1",
        buildJsonObject {
            put("p_room_id", roomId)
            put("p_word", word.trim())
        },
    ).decodeSingle()

suspend fun OnlineGameBackend.getTeamArenaWords(roomId: String): List<TeamArenaWordDto> =
    SupabaseProvider.client.postgrest.rpc(
        "get_team_arena_words_v1",
        buildJsonObject { put("p_room_id", roomId) },
    ).decodeList()

suspend fun OnlineGameBackend.cancelTeamArenaLobby(roomId: String): Boolean =
    SupabaseProvider.client.postgrest.rpc(
        "cancel_team_arena_lobby_v1",
        buildJsonObject { put("p_room_id", roomId) },
    ).decodeSingle()

suspend fun OnlineGameBackend.leaveTeamArenaLobby(roomId: String): Boolean =
    SupabaseProvider.client.postgrest.rpc(
        "leave_team_arena_lobby_v1",
        buildJsonObject { put("p_room_id", roomId) },
    ).decodeSingle()

suspend fun OnlineGameBackend.createTeamArenaRematch(roomId: String): TeamArenaRematchDto =
    SupabaseProvider.client.postgrest.rpc(
        "create_team_arena_rematch_v1",
        buildJsonObject { put("p_room_id", roomId) },
    ).decodeSingle()
