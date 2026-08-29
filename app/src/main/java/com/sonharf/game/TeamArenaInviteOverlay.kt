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
import com.sonharf.game.data.TeamArenaInviteDto
import com.sonharf.game.data.getIncomingTeamArenaInvites
import com.sonharf.game.data.respondTeamArenaInvite
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Incoming friend-to-Team-Arena invitations, available across the active shell. */
@Composable
fun TeamArenaInviteOverlay() {
    if (!SupabaseProvider.configured || SupabaseProvider.client.auth.currentUserOrNull() == null) return
    val context = LocalContext.current
    if (!SonHarfPreferences.gameInviteNotificationsEnabled(context)) return

    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var invite by remember { mutableStateOf<TeamArenaInviteDto?>(null) }
    var sender by remember { mutableStateOf<ProfileDto?>(null) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    var notifiedInviteId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            if (invite == null && !busy) {
                val next = runCatching { backend.getIncomingTeamArenaInvites().firstOrNull() }.getOrNull()
                if (next != null) {
                    invite = next
                    sender = runCatching { backend.getProfile(next.senderId) }.getOrNull()
                }
            }
            delay(2500)
        }
    }

    val modalKind = GameInviteModalKind.TEAM_ARENA
    val activeModal = GameInviteModalCoordinator.activeKind

    LaunchedEffect(invite?.inviteId) {
        GameInviteModalCoordinator.setPending(modalKind, invite != null)
    }

    DisposableEffect(Unit) {
        onDispose { GameInviteModalCoordinator.clear(modalKind) }
    }

    LaunchedEffect(activeModal, invite?.inviteId) {
        val key = invite?.inviteId
        if (activeModal == modalKind && key != null && notifiedInviteId != key) {
            SonHarfSoundFx.softNotify()
            SonHarfPreferences.hapticTap(context)
            notifiedInviteId = key
        }
    }

    val current = invite ?: return
    if (activeModal != modalKind) return

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
                    color = SonHarfGold.copy(alpha = .11f),
                    border = BorderStroke(1.dp, SonHarfGold.copy(alpha = .35f)),
                ) {
                    Text("👥", Modifier.padding(12.dp), fontSize = 24.sp)
                }
                Column {
                    Text(
                        sh("TAKIM ARENASI DAVETİ", "TEAM ARENA INVITE"),
                        color = SonHarfText,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                    )
                    Text(
                        sender?.displayName ?: sh("Bir arkadaşın", "A friend"),
                        color = SonHarfBlue,
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
                        sh("Seni 2v2 Kelime Arenası takım lobisine davet ediyor.", "Invites you to a 2v2 Word Arena team lobby."),
                        "invited you to a 2v2 Word Arena team lobby.",
                    ),
                    color = SonHarfText,
                    fontSize = 14.sp,
                )
                Text(
                    "${current.language.uppercase()} • ${sh("Takım", "Team")} ${if (current.team == 1) "A" else "B"} • ${sh("60 saniye", "60 seconds")}",
                    color = SonHarfMuted,
                    fontSize = 11.sp,
                )
                Text(
                    sh(
                        sh("Takım arkadaşınla aynı kelimeyi iki kez yazamazsınız. Rakip takımın kelimeleri maç bitene kadar gizli.", "You and your teammate cannot score the same word twice. The rival team’s words stay hidden until the match ends."),
                        "Your team cannot score the same word twice. Opponent words stay hidden until the match ends.",
                    ),
                    color = SonHarfMuted,
                    fontSize = 10.sp,
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
                        runCatching { backend.respondTeamArenaInvite(current.inviteId, true) }
                            .onSuccess { result ->
                                val room = result.roomId
                                if (result.status == "joined" && !room.isNullOrBlank()) {
                                    notice = ""
                                    invite = null
                                    sender = null
                                    FriendsQuickAccessState.open = false
                                    TeamArenaNavigation.requestRoom(room)
                                } else {
                                    notice = sh("Takım lobisine katılınamadı.", "Could not join the team lobby.")
                                }
                            }
                            .onFailure { error ->
                                notice = when {
                                    "player_already_in_game" in error.message.orEmpty() ->
                                        sh("Önce aktif maçını bitir.", "Finish your active match first.")
                                    "team_arena_already_active" in error.message.orEmpty() ->
                                        sh("Zaten başka bir Takım Arenası lobisindesin.", "You are already in another Team Arena lobby.")
                                    else -> sh("Davet kabul edilemedi.", "Invite could not be accepted.")
                                }
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
                        runCatching { backend.respondTeamArenaInvite(current.inviteId, false) }
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
