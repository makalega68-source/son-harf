package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal fun socialActivityLabel(kind: String): String = when(kind) {
    "your_turn" -> sh("Sıra sende", "Your turn")
    "challenge" -> sh("Meydan okuma", "Challenge")
    "rematch" -> sh("Rövanş istendi", "Rematch requested")
    "match_finished" -> sh("Maç bitti", "Match finished")
    "friend_request" -> sh("Arkadaşlık isteği", "Friend request")
    "friend_accepted" -> sh("Arkadaşlık kabul edildi", "Friend request accepted")
    "friend_online" -> sh("Arkadaşın çevrimiçi", "Friend is online")
    else -> sh("Yeni aktivite", "New activity")
}
@Composable
internal fun SocialInboxHistory(backend: OnlineGameBackend, onOpen: (String, String?) -> Unit) {
    var rows by remember { mutableStateOf<List<SocialActivityDto>>(emptyList()) }
    var names by remember { mutableStateOf<Map<String,String>>(emptyMap()) }
    var unreadOnly by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    var ready by remember { mutableStateOf(false) }
    val scope=rememberCoroutineScope()
    val foreground=rememberAppForeground()
    suspend fun reload() {
        gameRequestResult { backend.getSocialInbox() }.onSuccess { fetched ->
            rows=fetched; ready=true; error=false
            fetched.mapNotNull { it.actorId }.distinct().filterNot(names::containsKey).forEach { id ->
                gameRequestResult { backend.getProfile(id) }.getOrNull()?.let { names=names+(id to it.displayName) }
            }
        }.onFailure { error=true }
    }
    LaunchedEffect(foreground) { if(foreground) while(true) { reload(); delay(15_000) } }
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text(sh("GELEN KUTUSU", "INBOX"),color=Hf.Text,fontWeight=FontWeight.Bold)
            TextButton(onClick={ scope.launch { gameRequestResult { backend.markSocialActivityRead() }.onSuccess { reload() }.onFailure { error=true } } },enabled=rows.any { it.readAt==null }) {
                Text(sh("Tümünü okundu yap", "Mark all read"),fontSize=11.sp)
            }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            HomeSessionFilter(sh("Tümü", "All"),!unreadOnly) { unreadOnly=false }
            HomeSessionFilter(sh("Yeni ${rows.count { it.readAt==null }}", "New ${rows.count { it.readAt==null }}"),unreadOnly) { unreadOnly=true }
        }
        if(error) TextButton(onClick={scope.launch { reload() }}) { Text(sh("Gelen kutusu güncellenemedi · Yenile", "Inbox unavailable · Retry"),color=Hf.Red) }
        if(!ready && !error) LinearProgressIndicator(Modifier.fillMaxWidth(),color=Hf.Green)
        val shown=rows.filter { !unreadOnly || it.readAt==null }.take(30)
        if(ready && shown.isEmpty()) Text(sh("Yeni bildirim yok", "No new notifications"),color=Hf.TextMuted,fontSize=12.sp)
        shown.forEach { event ->
            SocialInboxRow(event, event.actorId?.let(names::get)) {
                scope.launch {
                    gameRequestResult { backend.markSocialActivityRead(event.id) }.onSuccess {
                        rows=rows.map { if(it.id==event.id) it.copy(readAt=java.time.Instant.now().toString()) else it }
                        onOpen(event.targetKind,event.targetId)
                    }.onFailure { error=true }
                }
            }
        }
    }
}


@Composable
internal fun SocialInboxRow(event: SocialActivityDto, actorName: String?, onClick: () -> Unit) {
    Surface(onClick=onClick,shape=RoundedCornerShape(14.dp),color=Hf.Surface,
        border=BorderStroke(1.dp,if(event.readAt==null) Hf.Green else Hf.Border)) {
        Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(socialActivityLabel(event.kind),color=Hf.Text,fontWeight=if(event.readAt==null) FontWeight.Bold else FontWeight.Normal,fontSize=14.sp)
            Text(listOfNotNull(actorName,socialDate(event.createdAt)).joinToString(" · "),color=Hf.TextMuted,fontSize=11.sp)
        }
    }
}
