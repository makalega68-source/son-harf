package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.FriendshipDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.getPublicCosmetics
import com.sonharf.game.data.inviteFriendToWordSiege
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** How the viewer relates to another player, read from the server's friendship rows. */
internal enum class PlayerRelation { SELF, FRIEND, REQUEST_SENT, REQUEST_RECEIVED, NONE }

internal fun playerRelation(me: String?, other: String, friendships: List<FriendshipDto>): PlayerRelation {
    if (me == null) return PlayerRelation.NONE
    if (me == other) return PlayerRelation.SELF
    val row = friendships.firstOrNull { (it.userId == me && it.friendId == other) || (it.userId == other && it.friendId == me) }
        ?: return PlayerRelation.NONE
    return when {
        row.status == "accepted" -> PlayerRelation.FRIEND
        row.status == "pending" && row.requestedBy == me -> PlayerRelation.REQUEST_SENT
        row.status == "pending" -> PlayerRelation.REQUEST_RECEIVED
        else -> PlayerRelation.NONE
    }
}

/** Head-to-head record against this player, from the server's rival history. */
internal data class PlayerHeadToHead(val matches: Int, val wins: Int, val losses: Int)

/** Reasons offered when reporting a player; the server stores the chosen text. */
internal fun playerReportReasons(): List<String> = listOf(
    gameText("Uygunsuz kullanıcı adı", "Inappropriate username"),
    gameText("Hakaret veya taciz", "Abuse or harassment"),
    gameText("Hile şüphesi", "Suspected cheating"),
    gameText("Spam veya dolandırıcılık", "Spam or scam"),
)

/** One direct-message thread in the Messages tab: the other player and the latest message. */
internal data class DirectConversation(val friendId: String, val lastBody: String, val lastFromMe: Boolean, val lastId: Long)

/** Groups the player's direct messages (RLS returns only their own) into threads, newest first. */
internal fun directConversations(me: String, messages: List<DirectMessageDto>): List<DirectConversation> =
    messages
        .filter { it.senderId == me || it.receiverId == me }
        .groupBy { if (it.senderId == me) it.receiverId else it.senderId }
        .map { (friendId, thread) ->
            val last = thread.maxBy { it.id }
            DirectConversation(friendId, last.body, last.senderId == me, last.id)
        }
        .sortedByDescending { it.lastId }

internal suspend fun OnlineGameBackend.getDirectConversations(): List<DirectConversation> {
    val me = currentUserId() ?: return emptyList()
    val messages = SupabaseProvider.client.from("direct_messages").select().decodeList<DirectMessageDto>()
    return directConversations(me, messages)
}

/** report_player without a match room (the RPC's room and message are optional). */
internal suspend fun OnlineGameBackend.reportPlayer(userId: String, reason: String): Int =
    SupabaseProvider.client.postgrest.rpc(
        "report_player",
        buildJsonObject { put("p_reported_id", userId); put("p_reason", reason) },
    ).decodeSingle()

/**
 * Another player's profile: identity, league and record, and the social actions. Every
 * action is a server call (friend request, siege invite, block, report); rules such as
 * "invites only between friends" stay on the server.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayerProfileSheet(
    backend: OnlineGameBackend,
    playerId: String,
    friendships: List<FriendshipDto>,
    headToHead: PlayerHeadToHead?,
    onMessage: (ProfileDto) -> Unit,
    onChanged: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val me = remember { backend.currentUserId() }
    var profile by remember(playerId) { mutableStateOf<ProfileDto?>(null) }
    var frameId by remember(playerId) { mutableStateOf<String?>(null) }
    var relation by remember(playerId, friendships) { mutableStateOf(playerRelation(me, playerId, friendships)) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var confirmBlock by remember { mutableStateOf(false) }
    var reporting by remember { mutableStateOf(false) }

    LaunchedEffect(playerId) {
        profile = runCatching { backend.getProfile(playerId) }.getOrNull()
        frameId = runCatching { backend.getPublicCosmetics(playerId) }.getOrNull()?.profileFrameId
        if (profile == null) notice = gameText("Profil yüklenemedi.", "Profile could not be loaded.")
    }

    fun act(block: suspend () -> String) {
        if (busy) return
        busy = true
        scope.launch {
            notice = block()
            busy = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = GameColors.ElevatedBackground,
        contentColor = GameColors.TextPrimary,
    ) {
        PlayerProfileContent(
            profile = profile,
            frameId = frameId,
            relation = relation,
            headToHead = headToHead,
            busy = busy,
            notice = notice,
            onAddFriend = {
                act {
                    runCatching { backend.sendFriendRequest(playerId) }.fold(
                        onSuccess = { relation = PlayerRelation.REQUEST_SENT; onChanged(); gameText("Arkadaşlık isteği gönderildi.", "Friend request sent.") },
                        onFailure = { gameText("İstek gönderilemedi.", "Request could not be sent.") },
                    )
                }
            },
            onInvite = {
                act {
                    runCatching { backend.inviteFriendToWordSiege(playerId, if (SonHarfUiState.isEnglish) "en" else "tr") }.fold(
                        onSuccess = { gameText("Kelime Kuşatması daveti gönderildi.", "Siege invite sent.") },
                        onFailure = { error ->
                            val raw = error.message.orEmpty()
                            when {
                                "not_friends" in raw -> gameText("Maç daveti için arkadaş olmalısınız.", "You need to be friends to invite.")
                                "invite_pending" in raw -> gameText("Bekleyen bir davetin var.", "An invite is already pending.")
                                else -> wordSiegeFriendlyError(raw)
                            }
                        },
                    )
                }
            },
            onMessage = { profile?.let(onMessage) },
            onBlock = { confirmBlock = true },
            onReport = { reporting = true },
        )
        Spacer(Modifier.navigationBarsPadding().height(12.dp))
    }

    if (confirmBlock) {
        AlertDialog(
            onDismissRequest = { confirmBlock = false },
            containerColor = GameColors.PrimarySurface,
            titleContentColor = GameColors.TextPrimary,
            textContentColor = GameColors.TextSecondary,
            title = { Text(gameText("Oyuncu engellensin mi?", "Block this player?")) },
            text = { Text(gameText("Engellenen oyuncu sana davet ve mesaj gönderemez. Ayarlar'dan kaldırabilirsin.", "Blocked players cannot invite or message you. You can undo this in Settings.")) },
            confirmButton = {
                TextButton(onClick = {
                    confirmBlock = false
                    act {
                        runCatching { backend.blockUser(playerId) }.fold(
                            onSuccess = { onChanged(); gameText("Oyuncu engellendi.", "Player blocked.") },
                            onFailure = { gameText("Engelleme yapılamadı.", "Could not block the player.") },
                        )
                    }
                }) { Text(gameText("ENGELLE", "BLOCK"), color = GameColors.Danger) }
            },
            dismissButton = {
                TextButton(onClick = { confirmBlock = false }) { Text(gameText("Vazgeç", "Cancel"), color = GameColors.TextSecondary) }
            },
        )
    }

    if (reporting) {
        AlertDialog(
            onDismissRequest = { reporting = false },
            containerColor = GameColors.PrimarySurface,
            titleContentColor = GameColors.TextPrimary,
            textContentColor = GameColors.TextSecondary,
            title = { Text(gameText("Şikâyet nedeni", "Report reason")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    playerReportReasons().forEach { reason ->
                        TextButton(
                            onClick = {
                                reporting = false
                                act {
                                    runCatching { backend.reportPlayer(playerId, reason) }.fold(
                                        onSuccess = { gameText("Şikâyetin alındı. Teşekkürler.", "Report received. Thank you.") },
                                        onFailure = { gameText("Şikâyet gönderilemedi.", "Report could not be sent.") },
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(reason, Modifier.fillMaxWidth(), color = GameColors.TextPrimary) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { reporting = false }) { Text(gameText("Vazgeç", "Cancel"), color = GameColors.TextSecondary) }
            },
        )
    }
}

@Composable
internal fun PlayerProfileContent(
    profile: ProfileDto?,
    frameId: String? = null,
    relation: PlayerRelation,
    headToHead: PlayerHeadToHead?,
    busy: Boolean,
    notice: String?,
    onAddFriend: () -> Unit,
    onInvite: () -> Unit,
    onMessage: () -> Unit,
    onBlock: () -> Unit,
    onReport: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = GameSpacing.ScreenHorizontal),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (profile == null) {
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = GameColors.PrimaryBlue, trackColor = GameColors.SecondarySurface)
            notice?.let { Text(it, color = GameColors.TextSecondary, style = MaterialTheme.typography.bodySmall) }
            return@Column
        }
        val league = ratingLeagueProgress(profile.rating)
        val matches = profile.wins + profile.losses
        Row(verticalAlignment = Alignment.CenterVertically) {
            FramedProfilePhotoAvatar(
                avatarPath = profile.avatarPath,
                gender = profile.gender,
                name = profile.displayName,
                size = 76.dp,
                frameId = frameId,
                accent = if (profile.isVip) GameColors.PrestigeGold else GameColors.PrimaryBlue,
                visible = profile.avatarVisibility != "hidden",
                isPro = profile.isVip,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        profile.displayName,
                        Modifier.weight(1f, fill = false),
                        color = GameColors.TextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (profile.isVip) {
                        Spacer(Modifier.width(6.dp))
                        Surface(shape = GameShapes.Pill, color = GameColors.PrestigeGold.copy(alpha = .16f)) {
                            Text("PRO", Modifier.padding(horizontal = 7.dp, vertical = 3.dp), color = GameColors.PrestigeGold, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    LeagueBadge(league.leagueName)
                    RatingBadge(profile.rating)
                }
                Text(
                    if (profile.presenceStatus == "online") gameText("Çevrimiçi", "Online") else gameText("Çevrimdışı", "Offline"),
                    color = if (profile.presenceStatus == "online") GameColors.PlayGreen else GameColors.TextTertiary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileFact(Modifier.weight(1f), profile.wins.toString(), gameText("Galibiyet", "Wins"))
            ProfileFact(Modifier.weight(1f), if (matches == 0) "–" else "%${profile.wins * 100 / matches}", gameText("Kazanma", "Win rate"))
            ProfileFact(
                Modifier.weight(1f),
                headToHead?.let { "${it.wins}–${it.losses}" } ?: "–",
                gameText("Aranızda", "Head-to-head"),
            )
        }
        headToHead?.takeIf { it.matches > 0 }?.let { record ->
            Text(
                gameText("Son ${record.matches} maç: ${record.wins}–${record.losses}", "Last ${record.matches} matches: ${record.wins}–${record.losses}"),
                color = GameColors.RewardAmber,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        when (relation) {
            PlayerRelation.SELF -> Unit
            PlayerRelation.FRIEND -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GamePrimaryButton(
                    text = if (headToHead != null && headToHead.matches > 0) gameText("RÖVANŞ", "REMATCH") else gameText("MAÇA DAVET", "INVITE"),
                    onClick = onInvite,
                    modifier = Modifier.weight(1f),
                    enabled = !busy,
                    icon = Icons.Rounded.GridView,
                )
                GameSecondaryButton(gameText("MESAJ", "MESSAGE"), onMessage, Modifier.weight(1f), enabled = !busy, icon = Icons.Rounded.ChatBubbleOutline)
            }
            PlayerRelation.REQUEST_SENT -> StatusPill(gameText("Arkadaşlık isteği gönderildi", "Friend request sent"), GameColors.TacticalTurquoise)
            PlayerRelation.REQUEST_RECEIVED -> StatusPill(gameText("Sana arkadaşlık isteği gönderdi • Davetler", "Sent you a friend request • Invites"), GameColors.RewardAmber)
            PlayerRelation.NONE -> GamePrimaryButton(
                text = gameText("ARKADAŞ EKLE", "ADD FRIEND"),
                onClick = onAddFriend,
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
                icon = Icons.Rounded.PersonAdd,
            )
        }
        if (relation != PlayerRelation.SELF) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onBlock,
                    enabled = !busy,
                    modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                    shape = GameShapes.Medium,
                    border = BorderStroke(1.dp, GameColors.Border),
                ) {
                    Icon(Icons.Rounded.Block, null, tint = GameColors.TextSecondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(gameText("Engelle", "Block"), color = GameColors.TextSecondary)
                }
                OutlinedButton(
                    onClick = onReport,
                    enabled = !busy,
                    modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                    shape = GameShapes.Medium,
                    border = BorderStroke(1.dp, GameColors.Danger.copy(alpha = .5f)),
                ) {
                    Icon(Icons.Rounded.Flag, null, tint = GameColors.Danger, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(gameText("Şikâyet et", "Report"), color = GameColors.Danger)
                }
            }
        }
        notice?.let { Text(it, color = GameColors.TextSecondary, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun ProfileFact(modifier: Modifier, value: String, label: String) {
    Surface(modifier = modifier, shape = GameShapes.Medium, color = GameColors.PrimarySurface, border = BorderStroke(1.dp, GameColors.Border)) {
        Column(Modifier.padding(vertical = 10.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = GameColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
            Text(label, color = GameColors.TextSecondary, fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
private fun StatusPill(text: String, accent: Color) {
    Surface(shape = GameShapes.Medium, color = accent.copy(alpha = .12f), border = BorderStroke(1.dp, accent.copy(alpha = .4f))) {
        Text(text, Modifier.fillMaxWidth().padding(12.dp), color = accent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

/** A direct-message thread with a friend; messages are read and written through RLS-protected rows. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DirectMessageSheet(
    backend: OnlineGameBackend,
    friend: ProfileDto,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val me = remember { backend.currentUserId() }
    var messages by remember(friend.id) { mutableStateOf<List<DirectMessageDto>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(friend.id) {
        while (true) {
            runCatching { backend.getDirectMessages(friend.id) }.onSuccess { messages = it }
            kotlinx.coroutines.delay(4_000)
        }
    }
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = GameColors.ElevatedBackground,
        contentColor = GameColors.TextPrimary,
    ) {
        Column(Modifier.fillMaxWidth().imePadding().padding(horizontal = GameSpacing.ScreenHorizontal)) {
            Text(friend.displayName, color = GameColors.TextPrimary, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (messages.isEmpty()) item {
                    Text(gameText("Henüz mesaj yok. İlk mesajı sen gönder.", "No messages yet. Say hello first."), color = GameColors.TextTertiary, style = MaterialTheme.typography.bodySmall)
                }
                items(messages, key = { it.id }) { message ->
                    val mine = message.senderId == me
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                        Surface(
                            shape = GameShapes.Medium,
                            color = if (mine) GameColors.PrimaryBlue.copy(alpha = .28f) else GameColors.PrimarySurface,
                        ) {
                            Text(message.body, Modifier.padding(horizontal = 11.dp, vertical = 8.dp).widthIn(max = 260.dp), color = GameColors.TextPrimary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            error?.let { Text(it, color = GameColors.Danger, style = MaterialTheme.typography.labelSmall) }
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.take(300) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(gameText("Mesaj yaz", "Write a message")) },
                    singleLine = true,
                    shape = GameShapes.Medium,
                )
                Spacer(Modifier.width(8.dp))
                GameIconButton(
                    icon = Icons.Rounded.Send,
                    description = gameText("Gönder", "Send"),
                    enabled = input.isNotBlank() && !sending,
                    onClick = {
                        val text = input
                        sending = true
                        scope.launch {
                            runCatching { backend.sendDirectMessage(friend.id, text) }
                                .onSuccess {
                                    input = ""
                                    error = null
                                    messages = runCatching { backend.getDirectMessages(friend.id) }.getOrDefault(messages)
                                }
                                .onFailure { error = gameText("Mesaj gönderilemedi.", "Message could not be sent.") }
                            sending = false
                        }
                    },
                )
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}
