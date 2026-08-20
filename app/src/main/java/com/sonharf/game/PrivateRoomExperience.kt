package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.inviteFriendToPrivateRoom
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
private data class PrivateWaitingRoomDto(
    val id: String,
    val code: String,
    @SerialName("host_id") val hostId: String,
    @SerialName("guest_id") val guestId: String? = null,
    val status: String,
    val language: String,
    @SerialName("room_type") val roomType: String,
)

@Composable
fun PrivateRoomWaitingLayer() {
    if (!SupabaseProvider.configured) return

    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var room by remember { mutableStateOf<PrivateWaitingRoomDto?>(null) }
    var friends by remember { mutableStateOf<List<ProfileDto>>(emptyList()) }
    var loadingFriends by remember { mutableStateOf(false) }
    var busyFriend by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var closing by remember { mutableStateOf(false) }

    suspend fun refreshWaitingRoom() {
        val me = backend.currentUserId() ?: return
        room = SupabaseProvider.client.from("game_rooms")
            .select()
            .decodeList<PrivateWaitingRoomDto>()
            .filter { it.hostId == me && it.roomType == "private" && it.status == "waiting" && it.guestId == null }
            .maxByOrNull { it.id }
    }

    LaunchedEffect(Unit) {
        while (true) {
            runCatching { refreshWaitingRoom() }
            delay(600)
        }
    }

    val activeRoom = room ?: return

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SonHarfBg,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Text(
                    sh("ÖZEL ODA", "PRIVATE ROOM"),
                    color = SonHarfCyan,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    sh("Rakibini bekliyorsun", "Waiting for your opponent"),
                    color = SonHarfMuted,
                    fontSize = 12.sp,
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SonHarfSurface),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, SonHarfPurple.copy(alpha = .45f)),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(sh("ODA KODU", "ROOM CODE"), color = SonHarfMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(activeRoom.code, color = SonHarfGold, fontSize = 38.sp, fontWeight = FontWeight.Black, letterSpacing = 5.sp)
                        Text(
                            if (activeRoom.language == "tr") "🇹🇷 TÜRKÇE" else "🇬🇧 ENGLISH",
                            color = SonHarfCyan,
                            fontWeight = FontWeight.Bold,
                        )
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(activeRoom.code))
                                notice = sh("Oda kodu kopyalandı.", "Room code copied.")
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(sh("KODU KOPYALA", "COPY CODE")) }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        scope.launch {
                            loadingFriends = true
                            friends = runCatching { backend.getFriends().map { it.second } }.getOrDefault(emptyList())
                            notice = if (friends.isEmpty()) sh("Davet edilebilecek arkadaş bulunamadı.", "No friends available to invite.") else null
                            loadingFriends = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SonHarfPurple),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text(sh("ARKADAŞ DAVET ET", "INVITE A FRIEND"), fontWeight = FontWeight.Black)
                }
            }

            if (loadingFriends) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }

            if (friends.isNotEmpty()) {
                item { Text(sh("ARKADAŞLAR", "FRIENDS"), color = SonHarfCyan, fontWeight = FontWeight.Black, fontSize = 12.sp) }
                items(friends, key = { it.id }) { friend ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SonHarfSurface),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(friend.displayName, fontWeight = FontWeight.Bold)
                                Text(
                                    if (friend.presenceStatus == "online") sh("Çevrimiçi", "Online") else sh("Çevrimdışı", "Offline"),
                                    color = if (friend.presenceStatus == "online") SonHarfGreen else SonHarfMuted,
                                    fontSize = 10.sp,
                                )
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        busyFriend = friend.id
                                        runCatching { backend.inviteFriendToPrivateRoom(activeRoom.id, friend.id) }
                                            .onSuccess { notice = sh("${friend.displayName} davet edildi.", "${friend.displayName} invited.") }
                                            .onFailure {
                                                notice = when {
                                                    "invite_already_pending" in it.message.orEmpty() -> sh("Bu arkadaş için davet zaten bekliyor.", "An invite is already pending for this friend.")
                                                    "friend_in_game" in it.message.orEmpty() -> sh("Arkadaşın şu anda maçta.", "Your friend is currently in a match.")
                                                    else -> sh("Davet gönderilemedi.", "Invite could not be sent.")
                                                }
                                            }
                                        busyFriend = null
                                    }
                                },
                                enabled = busyFriend == null,
                            ) {
                                Text(if (busyFriend == friend.id) "…" else sh("DAVET", "INVITE"))
                            }
                        }
                    }
                }
            }

            if (!notice.isNullOrBlank()) {
                item {
                    Surface(color = SonHarfSurface2, shape = RoundedCornerShape(14.dp)) {
                        Text(
                            notice!!,
                            Modifier.fillMaxWidth().padding(12.dp),
                            color = SonHarfMuted,
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            closing = true
                            runCatching {
                                SupabaseProvider.client.postgrest.rpc(
                                    "cancel_private_room",
                                    buildJsonObject { put("p_room_id", activeRoom.id) },
                                )
                            }.onSuccess {
                                room = null
                                friends = emptyList()
                            }.onFailure {
                                notice = sh("Oda kapatılamadı.", "Room could not be closed.")
                            }
                            closing = false
                        }
                    },
                    enabled = !closing,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    border = BorderStroke(1.dp, SonHarfPink.copy(alpha = .55f)),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(sh("ODAYI KAPAT", "CLOSE ROOM"), color = SonHarfPink, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
