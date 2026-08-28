package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.FriendshipDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SocialProfileDto(
    val id: String,
    @SerialName("display_name") val displayName: String,
    val gender: String? = null,
    @SerialName("avatar_visibility") val avatarVisibility: String = "hidden",
    @SerialName("avatar_path") val avatarPath: String? = null,
    @SerialName("presence_status") val presenceStatus: String = "offline",
    @SerialName("is_vip") val isVip: Boolean = false,
    val wins: Int = 0,
    val losses: Int = 0,
)

@Serializable
data class DirectMessageDto(
    val id: Long,
    @SerialName("sender_id") val senderId: String,
    @SerialName("receiver_id") val receiverId: String,
    val body: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
private data class DirectMessageWrite(
    @SerialName("sender_id") val senderId: String,
    @SerialName("receiver_id") val receiverId: String,
    val body: String,
)

suspend fun OnlineGameBackend.getSocialProfile(id: String): SocialProfileDto =
    SupabaseProvider.client.from("profiles")
        .select { filter { eq("id", id) } }
        .decodeSingle()

suspend fun OnlineGameBackend.getAcceptedFriendProfiles(): List<Pair<FriendshipDto, SocialProfileDto>> {
    val me = currentUserId() ?: return emptyList()
    return getFriendships()
        .filter { it.status == "accepted" }
        .mapNotNull { friendship ->
            val friendId = if (friendship.userId == me) friendship.friendId else friendship.userId
            runCatching { friendship to getSocialProfile(friendId) }.getOrNull()
        }
}

suspend fun OnlineGameBackend.getDirectMessages(friendId: String): List<DirectMessageDto> {
    val me = currentUserId() ?: return emptyList()
    return SupabaseProvider.client.from("direct_messages")
        .select()
        .decodeList<DirectMessageDto>()
        .filter { (it.senderId == me && it.receiverId == friendId) || (it.senderId == friendId && it.receiverId == me) }
        .sortedBy { it.id }
}

suspend fun OnlineGameBackend.sendDirectMessage(friendId: String, text: String) {
    val me = requireNotNull(currentUserId())
    val body = text.trim().take(300)
    require(body.isNotBlank())
    SupabaseProvider.client.from("direct_messages").insert(DirectMessageWrite(me, friendId, body))
}

fun genderAvatarEmoji(gender: String?, fallbackIndex: Int = 0): String = when (gender?.lowercase()) {
    "kadın", "kadin", "female", "woman" -> "👩🏻"
    "erkek", "male", "man" -> "👨🏻"
    "diğer", "diger", "other" -> "🧑🏻"
    else -> when (fallbackIndex % 3) { 0 -> "👩🏻"; 1 -> "👨🏻"; else -> "👩🏻" }
}

@Composable
fun SocialAvatar(
    gender: String?,
    name: String,
    size: Dp = 48.dp,
    fallbackIndex: Int = 0,
    accent: Color = SonHarfCyan,
) {
    SocialAvatar(
        avatarPath = null,
        gender = gender,
        name = name,
        size = size,
        fallbackIndex = fallbackIndex,
        accent = accent,
    )
}

@Composable
fun SocialAvatar(
    avatarPath: String?,
    gender: String?,
    name: String,
    size: Dp = 48.dp,
    fallbackIndex: Int = 0,
    accent: Color = SonHarfCyan,
) {
    ProfilePhotoAvatarWithGender(
        avatarPath = avatarPath,
        gender = gender,
        name = name,
        size = size,
        accent = accent,
    )
}

object FriendsQuickAccessState {
    var open by mutableStateOf(false)
}

@Composable
fun FriendsQuickAccessOverlay() {
    if (!SupabaseProvider.configured || SupabaseProvider.client.auth.currentUserOrNull() == null) return
    if (!FriendsQuickAccessState.open) {
        // Keep quick access visually consistent with the premium cyan UI.
        Box(
            Modifier.fillMaxSize().statusBarsPadding().padding(top = 7.dp, end = 10.dp),
            contentAlignment = Alignment.TopEnd,
        ) {
            Surface(
                modifier = Modifier.size(42.dp).clickable { FriendsQuickAccessState.open = true },
                shape = RoundedCornerShape(14.dp),
                color = SonHarfSurface.copy(alpha = .98f),
                border = BorderStroke(1.dp, SonHarfCyan.copy(alpha = .48f)),
                shadowElevation = 3.dp,
            ) {
                Box(contentAlignment = Alignment.Center) { Text("👥", fontSize = 20.sp) }
            }
        }
        return
    }
    FriendsHubDialog(onClose = { FriendsQuickAccessState.open = false })
}

@Composable
private fun FriendsHubDialog(onClose: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var friends by remember { mutableStateOf<List<Pair<FriendshipDto, SocialProfileDto>>>(emptyList()) }
    var selected by remember { mutableStateOf<SocialProfileDto?>(null) }
    var messages by remember { mutableStateOf<List<DirectMessageDto>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var notice by remember { mutableStateOf("") }
    var inviteBusy by remember { mutableStateOf(false) }
    var messageBusy by remember { mutableStateOf(false) }
    val me = backend.currentUserId()

    suspend fun reloadFriends() {
        loading = true
        runCatching { backend.getAcceptedFriendProfiles() }
            .onSuccess { friends = it }
            .onFailure {
                friends = emptyList()
                notice = sh("Arkadaş listesi yüklenemedi.", "Friend list could not be loaded.")
            }
        loading = false
    }

    suspend fun reloadMessages() {
        val friend = selected ?: return
        messages = runCatching { backend.getDirectMessages(friend.id) }.getOrDefault(messages)
    }

    LaunchedEffect(Unit) { reloadFriends() }
    LaunchedEffect(selected?.id) {
        notice = ""
        while (selected != null) {
            reloadMessages()
            delay(900)
        }
    }

    AlertDialog(
        onDismissRequest = onClose,
        containerColor = SonHarfSurface,
        shape = RoundedCornerShape(26.dp),
        title = {
            Text(
                if (selected == null) sh("ARKADAŞLAR", "FRIENDS") else selected!!.displayName,
                color = SonHarfText,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
            )
        },
        text = {
            if (selected == null) {
                Column(Modifier.fillMaxWidth().heightIn(min = 250.dp, max = 520.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(sh("Kabul edilmiş arkadaşlarınla sohbet et veya düello başlat.", "Chat with accepted friends or start a duel."), color = SonHarfMuted, fontSize = 13.sp)
                    if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = SonHarfCyan)
                    if (!loading && friends.isEmpty()) {
                        Text(sh("Henüz kabul edilmiş arkadaşın yok.", "You do not have accepted friends yet."), Modifier.fillMaxWidth().padding(vertical = 30.dp), textAlign = TextAlign.Center, color = SonHarfMuted)
                    }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        items(friends, key = { it.second.id }) { (_, profile) ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { selected = profile },
                                colors = CardDefaults.cardColors(containerColor = SonHarfSurface2.copy(alpha = .82f)),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, if (profile.presenceStatus == "online") SonHarfCyan.copy(alpha = .25f) else Color.White.copy(alpha = .05f)),
                            ) {
                                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    SocialAvatar(profile.avatarPath, profile.gender, profile.displayName, 48.dp, accent = if (profile.isVip) SonHarfGold else SonHarfCyan)
                                    Column(Modifier.weight(1f)) {
                                        Text(profile.displayName, color = SonHarfText, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                        Text(
                                            if (profile.presenceStatus == "online") "● ${sh("Çevrimiçi", "Online")}" else sh("Çevrimdışı", "Offline"),
                                            color = if (profile.presenceStatus == "online") SonHarfGreen else SonHarfMuted,
                                            fontSize = 12.sp,
                                        )
                                    }
                                    Text("›", fontSize = 28.sp, color = SonHarfPurple)
                                }
                            }
                        }
                    }
                }
            } else {
                val friend = selected!!
                Column(Modifier.fillMaxWidth().heightIn(min = 410.dp, max = 600.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SocialAvatar(friend.avatarPath, friend.gender, friend.displayName, 64.dp, accent = if (friend.isVip) SonHarfGold else SonHarfPurple)
                        Column(Modifier.weight(1f)) {
                            Text(friend.displayName, color = SonHarfText, fontWeight = FontWeight.Black, fontSize = 20.sp)
                            Text("${friend.wins}W • ${friend.losses}L", color = SonHarfMuted, fontSize = 12.sp)
                            Text(sh("✓ Arkadaşın — özel sohbet açık", "✓ Friend — private chat enabled"), color = SonHarfGreen, fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                inviteBusy = true
                                runCatching { backend.inviteFriend(friend.id, SonHarfUiState.language) }
                                    .onSuccess { notice = sh("Düello daveti gönderildi.", "Duel invitation sent."); SonHarfSoundFx.softNotify() }
                                    .onFailure { notice = sh("Davet gönderilemedi.", "Invitation could not be sent.") }
                                inviteBusy = false
                            }
                        },
                        enabled = friend.presenceStatus == "online" && !inviteBusy,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SonHarfPurple),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(if (inviteBusy) "…" else "⚔ ${sh("DÜELLOYA DAVET ET", "INVITE TO DUEL")}", fontWeight = FontWeight.Black)
                    }
                    if (friend.presenceStatus != "online") Text(sh("Düello daveti için arkadaşının çevrimiçi olması gerekir.", "Your friend must be online for a duel invitation."), color = SonHarfMuted, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    if (notice.isNotBlank()) Text(notice, color = SonHarfGold, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

                    HorizontalDivider(color = SonHarfMuted.copy(alpha = .14f))
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(messages.takeLast(80), key = { it.id }) { msg ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = if (msg.senderId == me) Arrangement.End else Arrangement.Start) {
                                Surface(
                                    color = if (msg.senderId == me) SonHarfPurple.copy(alpha = .22f) else SonHarfSurface2,
                                    shape = RoundedCornerShape(14.dp),
                                ) {
                                    Text(msg.body, Modifier.padding(horizontal = 11.dp, vertical = 8.dp).widthIn(max = 250.dp), color = SonHarfText, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it.take(300) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { Text(sh("Mesaj yaz…", "Type a message…")) },
                        )
                        Button(
                            onClick = {
                                val outgoing = input.trim()
                                if (outgoing.isBlank() || messageBusy) return@Button
                                input = ""
                                scope.launch {
                                    messageBusy = true
                                    runCatching { backend.sendDirectMessage(friend.id, outgoing) }
                                        .onSuccess {
                                            notice = ""
                                            reloadMessages()
                                        }
                                        .onFailure {
                                            input = outgoing
                                            notice = sh("Mesaj gönderilemedi. Tekrar dene.", "Message could not be sent. Try again.")
                                        }
                                    messageBusy = false
                                }
                            },
                            enabled = input.isNotBlank() && !messageBusy,
                        ) { Text(if (messageBusy) "…" else "➤") }
                    }
                }
            }
        },
        confirmButton = {
            if (selected == null) TextButton(onClick = onClose) { Text(sh("KAPAT", "CLOSE")) }
            else TextButton(onClick = { selected = null; messages = emptyList(); input = ""; notice = "" }) { Text(sh("ARKADAŞLARA DÖN", "BACK TO FRIENDS")) }
        },
        dismissButton = {
            if (selected != null) TextButton(onClick = onClose) { Text(sh("KAPAT", "CLOSE")) }
        },
    )
}
