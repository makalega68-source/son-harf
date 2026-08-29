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
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.WordArenaInviteDto
import com.sonharf.game.data.getIncomingWordArenaInvites
import com.sonharf.game.data.getProfile
import com.sonharf.game.data.respondWordArenaInvite
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Incoming friend-to-Word-Arena invitations, available across the active shell. */
@Composable
fun WordArenaInviteOverlay() {
    if (!SupabaseProvider.configured || SupabaseProvider.client.auth.currentUserOrNull() == null) return
    val context = LocalContext.current
    if (!SonHarfPreferences.gameInviteNotificationsEnabled(context)) return

    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var invite by remember { mutableStateOf<WordArenaInviteDto?>(null) }
    var sender by remember { mutableStateOf<ProfileDto?>(null) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            if (invite == null && !busy) {
                val next = runCatching { backend.getIncomingWordArenaInvites().firstOrNull() }.getOrNull()
                if (next != null) {
                    invite = next
                    sender = runCatching { backend.getProfile(next.senderId) }.getOrNull()
                    SonHarfSoundFx.softNotify()
                    SonHarfPreferences.hapticTap(context)
                }
            }
            delay(2500)
        }
    }

    val current = invite ?: return
    AlertDialog(
        onDismissRequest = { },
        containerColor = SonHarfSurface,
        shape = RoundedCornerShape(26.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = SonHarfBlue.copy(alpha = .10f),
                    border = BorderStroke(1.dp, SonHarfCyan.copy(alpha = .35f)),
                ) {
                    Text("⚡", Modifier.padding(12.dp), fontSize = 24.sp)
                }
                Column {
                    Text(
                        sh("KELİME ARENASI DAVETİ", "WORD ARENA INVITE"),
                        color = SonHarfText,
                        fontWeight = FontWeight.Black,
                        fontSize = 19.sp,
                    )
                    Text(
                        sender?.displayName ?: sh("Bir arkadaşın", "A friend"),
                        color = SonHarfCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    sh(
                        "Seni 60 saniyelik Kelime Arenası düellosuna davet ediyor.",
                        "invited you to a 60-second Word Arena duel.",
                    ),
                    color = SonHarfText,
                    fontSize = 14.sp,
                )
                Text(
                    "${current.language.uppercase()} • ${sh("Aynı harfler • Aynı süre", "Same letters • Same time")}",
                    color = SonHarfMuted,
                    fontSize = 11.sp,
                )
                if (notice.isNotBlank()) {
                    Text(notice, color = SonHarfPink, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !busy,
                onClick = {
                    scope.launch {
                        busy = true
                        runCatching { backend.respondWordArenaInvite(current.inviteId, true) }
                            .onSuccess { result ->
                                val room = result.roomId
                                if (result.status == "matched" && !room.isNullOrBlank()) {
                                    notice = ""
                                    invite = null
                                    sender = null
                                    FriendsQuickAccessState.open = false
                                    WordArenaNavigation.requestRoom(room)
                                } else {
                                    notice = sh("Arena odası açılamadı.", "Arena room could not be opened.")
                                }
                            }
                            .onFailure {
                                notice = sh(
                                    "Davet kabul edilemedi. Tekrar dene.",
                                    "Invite could not be accepted. Try again.",
                                )
                            }
                        busy = false
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SonHarfBlue),
            ) {
                Text(sh("KABUL ET", "ACCEPT"), fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            OutlinedButton(
                enabled = !busy,
                onClick = {
                    scope.launch {
                        busy = true
                        runCatching { backend.respondWordArenaInvite(current.inviteId, false) }
                            .onSuccess {
                                notice = ""
                                invite = null
                                sender = null
                            }
                            .onFailure {
                                notice = sh("Davet reddedilemedi.", "Invite could not be declined.")
                            }
                        busy = false
                    }
                },
                border = BorderStroke(1.dp, SonHarfPink),
            ) {
                Text(sh("REDDET", "DECLINE"), color = SonHarfPink, fontWeight = FontWeight.Bold)
            }
        },
    )
}
