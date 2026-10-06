package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Real available friends → one explicit invite → the existing accepted-invite room flow. */
@Composable
internal fun HomePlayTogether(profile: ProfileDto?, onSocial: () -> Unit) {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    var friends by remember(profile?.id) { mutableStateOf<List<ProfileDto>>(emptyList()) }
    var inviting by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(profile?.id, profile?.isVip, backend) {
        if (profile?.isVip != true || backend == null) return@LaunchedEffect
        while (true) {
            gameRequestResult { backend.getFriends() }.onSuccess { rows ->
                friends = rows.map { it.second }.filter { it.presenceStatus == "online" }
                    .sortedBy { it.displayName }.take(3)
            }
            delay(45_000)
        }
    }
    Surface(shape = RoundedCornerShape(18.dp), color = SonHarfTheme.Surface,
        border = BorderStroke(.8.dp, SonHarfTheme.Border)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Groups, null, tint = SonHarfTheme.Primary)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(sh("Birlikte oyna", "Play together"), color = SonHarfTheme.TextPrimary,
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (friends.isEmpty()) sh("Arkadaş bul · davet et · rövanş oyna", "Find friends · invite · play a rematch")
                        else sh("Arkadaşların şimdi oynamaya hazır", "Your friends are ready to play now"),
                        color = SonHarfTheme.TextSecondary, fontSize = 11.sp)
                }
                TextButton(onClick = onSocial) { Text(sh("ARKADAŞLAR", "FRIENDS"), fontSize = 10.sp) }
            }
            friends.forEach { friend ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    ProfilePhotoAvatarWithGender(friend.avatarPath, friend.gender, friend.displayName, 32.dp,
                        accent = SonHarfTheme.Primary, visible = friend.avatarVisibility != "hidden",
                        frameId = rememberPlayerFrame(friend.id))
                    Spacer(Modifier.width(8.dp))
                    Text(friend.displayName, Modifier.weight(1f), color = SonHarfTheme.TextPrimary,
                        fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    OutlinedButton(
                        enabled = inviting == null,
                        onClick = {
                            if (inviting != null || backend == null) return@OutlinedButton
                            inviting = friend.id
                            scope.launch {
                                try {
                                    gameRequestResult { backend.inviteFriendToWordSiege(friend.id, SonHarfUiState.language) }
                                        .onSuccess { invite ->
                                            WordSiegeLaunchConfig.awaitInvite(invite.id)
                                            notice = sh("Davet gönderildi; kabul edilince maça gireceksiniz.", "Invite sent; you'll enter the match when accepted.")
                                            SonHarfSoundFx.softNotify()
                                        }.onFailure {
                                            notice = sh("Davet gönderilemedi. Arkadaşlar bölümünden kontrol et.", "Invite could not be sent. Check the Friends page.")
                                        }
                                } finally { inviting = null }
                            }
                        }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp),
                    ) { Text(if (inviting == friend.id) "…" else sh("OYNA", "PLAY"), fontSize = 10.sp) }
                }
            }
            notice?.let { Text(it, color = SonHarfTheme.TextSecondary, fontSize = 11.sp) }
        }
    }
}
