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
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(accent.copy(alpha = .24f), SonHarfSurface2)))
            .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.fillMaxSize().clip(CircleShape).background(SonHarfSurface),
            contentAlignment = Alignment.Center,
        ) {
            Text(genderAvatarEmoji(gender, fallbackIndex), fontSize = (size.value * .55f).sp, textAlign = TextAlign.Center)
        }
    }
}

object FriendsQuickAccessState {
    var open by mutableStateOf(false)
}

@Composable
fun FriendsQuickAccessOverlay() {
    if (!SupabaseProvider.configured || SupabaseProvider.client.auth.currentUserOrNull() == null) return
    if (!FriendsQuickAccessState.open) {
        Box(Modifier.fillMaxSize().statusBarsPadding().padding(top = 6.dp, end = 12.dp), contentAlignment = Alignment.TopEnd) {
            Surface(
                modifier = Modifier.size(46.dp).clickable { FriendsQuickAccessState.open = true },
                shape = CircleShape,
                color = SonHarfSurface.copy(alpha = .96f),
                border = BorderStroke(1.dp, SonHarfCyan.copy(alpha = .35f)),
                shadowElevation = 5.dp,
            ) {
                Box(contentAlignment = Alignment.Center) { Text("👥", fontSize = 22.sp) }
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
    val me = backend.currentUserId()

    suspend fun reloadFriends() {
        loading = true
        friends = runCatching { backend.getAcceptedFriendProfiles() }.getOrDefault(emptyList())
        loading = false
    }

    suspend fun reloadMessages() {
        val friend = selected ?: return
        messages = runCatching { backend.getDirectMessages(friend.id) }.getOrDefault(messages)
    }

    LaunchedEffect(Unit) { reloadFriends() }
    LaunchedEffect(selected?.id) {
        while (selected != null) {
            reloadMessages()
            delay(900)
        }
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (selected == null) sh("ARKADAŞLAR", "FRIENDS") else selected!!.displayName, fontWeight = FontWeight.Black, fontSize = 22.sp) },
        text = {
            if (selected == null) {
                Column(Modifier.fillMaxWidth().heightIn(min = 250.dp, max = 520.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(sh("Yalnızca kabul edilmiş arkadaşlarınla özel sohbet edebilirsin.", "You can privately chat only with accepted friends."), color = SonHarfMuted, fontSize = 13.sp)
                    if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (!loading && friends.isEmpty()) Text(sh("Henüz kabul edilmiş arkadaşın yok.", "You do not have accepted friends yet."), Modifier.fillMaxWidth().padding(vertical = 30.dp), textAlign = TextAlign.Center, color = SonHarfMuted)
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        items(friends, key = { it.second.id }) { (_, profile) ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { selected = profile },
                                colors = CardDefaults.cardColors(containerColor = SonHarfSurface2.copy(alpha = .72f)),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    SocialAvatar(profile.gender, profile.displayName, 48.dp, accent = if (profile.isVip) SonHarfGold else SonHarfCyan)
                                    Column(Modifier.weight(1f)) {
                                        Text(profile.displayName, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                        Text(if (profile.presenceStatus == "online") sh("Çevrimiçi", "Online") else sh("Çevrimdışı", "Offline"), color = if (profile.presenceStatus == "online") SonHarfGreen else SonHarfMuted, fontSize = 12.sp)
                                    }
                                    Text("›", fontSize = 28.sp, color = SonHarfPurple)
                                }
                            }
                        }
                    }
                }
            } else {
                val friend = selected!!
                Column(Modifier.fillMaxWidth().heightIn(min = 380.dp, max = 560.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SocialAvatar(friend.gender, friend.displayName, 64.dp, accent = if (friend.isVip) SonHarfGold else SonHarfPurple)
                        Column {
                            Text(friend.displayName, fontWeight = FontWeight.Black, fontSize = 20.sp)
                            Text("${friend.wins}W • ${friend.losses}L", color = SonHarfMuted, fontSize = 12.sp)
                            Text(sh("✓ Arkadaşın — sohbet açık", "✓ Friend — chat enabled"), color = SonHarfGreen, fontSize = 12.sp)
                        }
                    }
                    HorizontalDivider()
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(messages.takeLast(80), key = { it.id }) { msg ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = if (msg.senderId == me) Arrangement.End else Arrangement.Start) {
                                Surface(
                                    color = if (msg.senderId == me) SonHarfPurple.copy(alpha = .16f) else SonHarfSurface2,
                                    shape = RoundedCornerShape(14.dp),
                                ) {
                                    Text(msg.body, Modifier.padding(horizontal = 11.dp, vertical = 8.dp).widthIn(max = 250.dp), fontSize = 14.sp)
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        OutlinedTextField(input, { input = it.take(300) }, modifier = Modifier.weight(1f), singleLine = true, placeholder = { Text(sh("Mesaj yaz…", "Type a message…")) })
                        Button(onClick = {
                            if (input.isBlank()) return@Button
                            val outgoing = input
                            input = ""
                            scope.launch { runCatching { backend.sendDirectMessage(friend.id, outgoing) }; reloadMessages() }
                        }, enabled = input.isNotBlank()) { Text("➤") }
                    }
                }
            }
        },
        confirmButton = {
            if (selected == null) TextButton(onClick = onClose) { Text(sh("KAPAT", "CLOSE")) }
            else TextButton(onClick = { selected = null; messages = emptyList(); input = "" }) { Text(sh("ARKADAŞLARA DÖN", "BACK TO FRIENDS")) }
        },
        dismissButton = {
            if (selected != null) TextButton(onClick = onClose) { Text(sh("KAPAT", "CLOSE")) }
        },
    )
}
