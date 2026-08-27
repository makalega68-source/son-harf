package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.FriendshipDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Global accepted/decline flow for the detailed friend-request notification preference. */
@Composable
fun FriendRequestOverlay() {
    if (!SupabaseProvider.configured || SupabaseProvider.client.auth.currentUserOrNull() == null) return
    val context = LocalContext.current
    if (!SonHarfPreferences.friendRequestNotificationsEnabled(context)) return

    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var request by remember { mutableStateOf<Pair<FriendshipDto, ProfileDto>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            if (request == null && !busy) {
                val next = runCatching { backend.getIncomingFriendRequests().firstOrNull() }.getOrNull()
                if (next != null) {
                    request = next
                    SonHarfSoundFx.softNotify()
                    SonHarfPreferences.hapticTap(context)
                }
            }
            delay(3500)
        }
    }

    val current = request ?: return
    val friendship = current.first
    val profile = current.second
    val friendId = if (friendship.userId == backend.currentUserId()) friendship.friendId else friendship.userId

    AlertDialog(
        onDismissRequest = { },
        containerColor = SonHarfSurface,
        shape = RoundedCornerShape(26.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SocialAvatar(null, profile.displayName, 52.dp, accent = SonHarfCyan)
                Column {
                    Text(sh("ARKADAŞLIK İSTEĞİ", "FRIEND REQUEST"), color = SonHarfText, fontWeight = FontWeight.Black, fontSize = 19.sp)
                    Text(profile.displayName, color = SonHarfCyan, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(sh("Seni arkadaş listesine eklemek istiyor.", "wants to add you as a friend."), color = SonHarfMuted, fontSize = 14.sp)
                if (notice.isNotBlank()) Text(notice, color = SonHarfPink, fontSize = 11.sp)
            }
        },
        confirmButton = {
            Button(
                enabled = !busy,
                onClick = {
                    scope.launch {
                        busy = true
                        runCatching { backend.respondFriendRequest(friendId, true) }
                            .onSuccess {
                                notice = ""
                                request = null
                            }
                            .onFailure {
                                notice = sh("Arkadaşlık isteği kabul edilemedi. Tekrar dene.", "Friend request could not be accepted. Try again.")
                            }
                        busy = false
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SonHarfPurple),
            ) { Text(sh("KABUL ET", "ACCEPT"), fontWeight = FontWeight.Black) }
        },
        dismissButton = {
            OutlinedButton(
                enabled = !busy,
                onClick = {
                    scope.launch {
                        busy = true
                        runCatching { backend.respondFriendRequest(friendId, false) }
                            .onSuccess {
                                notice = ""
                                request = null
                            }
                            .onFailure {
                                notice = sh("Arkadaşlık isteği reddedilemedi. Tekrar dene.", "Friend request could not be declined. Try again.")
                            }
                        busy = false
                    }
                },
                border = BorderStroke(1.dp, SonHarfPink),
            ) { Text(sh("REDDET", "DECLINE"), color = SonHarfPink) }
        },
    )
}
