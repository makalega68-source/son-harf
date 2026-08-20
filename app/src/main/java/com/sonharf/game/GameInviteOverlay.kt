package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.GameInviteDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Keeps incoming friend-to-game invitations available from every redesigned screen. */
@Composable
fun GameInviteOverlay() {
    if (!SupabaseProvider.configured || SupabaseProvider.client.auth.currentUserOrNull() == null) return

    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var invite by remember { mutableStateOf<GameInviteDto?>(null) }
    var sender by remember { mutableStateOf<ProfileDto?>(null) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            if (invite == null && !busy) {
                val next = runCatching { backend.getIncomingGameInvites().firstOrNull() }.getOrNull()
                if (next != null) {
                    invite = next
                    sender = runCatching { backend.getProfile(next.senderId) }.getOrNull()
                    SonHarfSoundFx.softNotify()
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = CircleShape, color = SonHarfPurple.copy(alpha = .20f), border = BorderStroke(1.dp, SonHarfCyan.copy(alpha = .35f))) {
                    Text("⚔", Modifier.padding(12.dp), fontSize = 24.sp)
                }
                Column {
                    Text(sh("DÜELLO DAVETİ", "DUEL INVITE"), color = SonHarfText, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Text(sender?.displayName ?: sh("Bir arkadaşın", "A friend"), color = SonHarfCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(sh("Seni Son Harf düellosuna davet ediyor.", "invited you to a Son Harf duel."), color = SonHarfText, fontSize = 15.sp)
                Text("${current.language.uppercase()} • ${sh("Özel eşleşme", "Private match")}", color = SonHarfMuted, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(
                enabled = !busy,
                onClick = {
                    scope.launch {
                        busy = true
                        runCatching { backend.respondGameInvite(current.id, true) }
                            .onSuccess {
                                invite = null
                                sender = null
                                SonHarfGameNavigation.requestLobby()
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
                        runCatching { backend.respondGameInvite(current.id, false) }
                        invite = null
                        sender = null
                        busy = false
                    }
                },
                border = BorderStroke(1.dp, SonHarfPink),
            ) { Text(sh("REDDET", "DECLINE"), color = SonHarfPink, fontWeight = FontWeight.Bold) }
        },
    )
}
