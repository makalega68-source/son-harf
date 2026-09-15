package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Master GDD v3 club entry point.
 *
 * Chat is intentionally a full-page surface instead of a compact dashboard widget. The existing
 * CompetitionHubScreen remains the authoritative club-management surface for members, missions,
 * ranking and inter-club challenges.
 */
@Composable
internal fun KelimeKusatmasiClubScreen() {
    var showClubCenter by remember { mutableStateOf(false) }

    if (showClubCenter) {
        CompetitionHubScreen(onBack = { showClubCenter = false }, clubEntry = true)
        return
    }

    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    var club by remember { mutableStateOf<MyClubDto?>(null) }
    var members by remember { mutableStateOf<List<ClubMemberDto>>(emptyList()) }
    var messages by remember { mutableStateOf<List<ClubMessageDto>>(emptyList()) }
    var myUserId by remember { mutableStateOf<String?>(null) }
    var menuMessageId by remember { mutableStateOf<String?>(null) }
    var input by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var sending by remember { mutableStateOf(false) }
    var lastMessageSentAt by remember { mutableStateOf(0L) }
    var notice by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() {
        val b = backend
        if (b == null) {
            loading = false
            notice = sh("Kulüp sohbeti için sunucu bağlantısı gerekli.", "A server connection is required for club chat.")
            return
        }
        loading = true
        myUserId = b.currentUserId()
        runCatching { b.getMyClub() }
            .onSuccess { current ->
                club = current
                if (current == null) {
                    members = emptyList()
                    messages = emptyList()
                } else {
                    members = runCatching { b.getClubMembers(current.clubId) }.getOrDefault(emptyList())
                    messages = runCatching { b.getClubMessages(current.clubId) }.getOrDefault(emptyList())
                }
                notice = null
            }
            .onFailure { notice = sh("Kulüp bilgisi alınamadı.", "Club information could not be loaded.") }
        loading = false
    }

    LaunchedEffect(Unit) { refresh() }
    LaunchedEffect(club?.clubId) {
        while (club != null) {
            delay(3500)
            val current = club ?: break
            val b = backend ?: break
            messages = runCatching { b.getClubMessages(current.clubId) }.getOrDefault(messages)
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(sh("KULÜP SOHBETİ", "CLUB CHAT"), color = SonHarfTheme.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text(
                    club?.let { "[${it.tag}] ${it.name}" } ?: sh("Kulüp iletişim merkezi", "Club communication center"),
                    color = SonHarfTheme.TextSecondary,
                    fontSize = 12.sp,
                )
            }
            FilledTonalButton(
                onClick = { showClubCenter = true },
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Rounded.Groups, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(sh("MERKEZ", "CENTER"), fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = SonHarfTheme.Border)
        Spacer(Modifier.height(10.dp))

        when {
            loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SonHarfTheme.Primary)
            }
            club == null -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = SonHarfTheme.Surface,
                    border = BorderStroke(1.dp, SonHarfTheme.Border),
                ) {
                    Column(
                        Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Rounded.Groups, null, tint = SonHarfTheme.Primary, modifier = Modifier.size(36.dp))
                        Text(sh("Bir kulübe katıl veya kendi kulübünü kur.", "Join a club or create your own."), color = SonHarfTheme.TextPrimary, fontWeight = FontWeight.Bold)
                        Button(onClick = { showClubCenter = true }) {
                            Text(sh("KULÜP MERKEZİNE GİT", "OPEN CLUB CENTER"), fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
            else -> {
                notice?.let {
                    Surface(shape = RoundedCornerShape(12.dp), color = SonHarfTheme.Error.copy(alpha = .12f)) {
                        Text(it, Modifier.fillMaxWidth().padding(10.dp), color = SonHarfTheme.TextPrimary, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (messages.isEmpty()) {
                        item {
                            Text(
                                sh("Henüz mesaj yok. İlk mesajı sen gönder.", "No messages yet. Send the first one."),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                color = SonHarfTheme.TextSecondary,
                                fontSize = 13.sp,
                            )
                        }
                    } else {
                        items(messages, key = { it.id }) { message ->
                            val sender = members.firstOrNull { it.userId == message.senderId }?.displayName ?: sh("Üye", "Member")
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = SonHarfTheme.Surface,
                                border = BorderStroke(1.dp, SonHarfTheme.Border.copy(alpha = .7f)),
                            ) {
                                Column(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 8.dp)) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(sender, modifier = Modifier.weight(1f), color = SonHarfTheme.Primary, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                        if (message.senderId != myUserId) {
                                            Box {
                                                IconButton(
                                                    onClick = { menuMessageId = message.id },
                                                    modifier = Modifier.size(34.dp),
                                                ) {
                                                    Icon(Icons.Rounded.MoreVert, sh("Mesaj seçenekleri", "Message options"), modifier = Modifier.size(18.dp), tint = SonHarfTheme.TextSecondary)
                                                }
                                                DropdownMenu(
                                                    expanded = menuMessageId == message.id,
                                                    onDismissRequest = { menuMessageId = null },
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text(sh("Raporla", "Report")) },
                                                        onClick = {
                                                            menuMessageId = null
                                                            scope.launch {
                                                                val b = backend ?: return@launch
                                                                runCatching { b.reportClubMember(message.senderId) }
                                                                    .onSuccess { notice = sh("Rapor moderasyona iletildi.", "Report sent to moderation.") }
                                                                    .onFailure { notice = sh("Rapor gönderilemedi.", "Report could not be sent.") }
                                                            }
                                                        },
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text(sh("Engelle", "Block")) },
                                                        onClick = {
                                                            menuMessageId = null
                                                            scope.launch {
                                                                val b = backend ?: return@launch
                                                                runCatching { b.blockClubMember(message.senderId) }
                                                                    .onSuccess {
                                                                        messages = messages.filterNot { it.senderId == message.senderId }
                                                                        notice = sh("Oyuncu engellendi.", "Player blocked.")
                                                                    }
                                                                    .onFailure { notice = sh("Oyuncu engellenemedi.", "Player could not be blocked.") }
                                                            }
                                                        },
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Text(message.body, color = SonHarfTheme.TextPrimary, fontSize = 14.sp, lineHeight = 19.sp)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = SonHarfTheme.Border)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it.take(300) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text(sh("Kulübe yaz…", "Message club…")) },
                        shape = RoundedCornerShape(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val current = club ?: return@IconButton
                            val outgoing = input.trim()
                            if (outgoing.isEmpty() || sending) return@IconButton
                            val now = System.currentTimeMillis()
                            if (now - lastMessageSentAt < 1_500L) {
                                notice = sh("Mesajları çok hızlı gönderiyorsun.", "You're sending messages too quickly.")
                                return@IconButton
                            }
                            lastMessageSentAt = now
                            scope.launch {
                                val b = backend ?: return@launch
                                sending = true
                                runCatching { b.sendClubMessage(current.clubId, outgoing) }
                                    .onSuccess {
                                        input = ""
                                        messages = runCatching { b.getClubMessages(current.clubId) }.getOrDefault(messages)
                                        notice = null
                                    }
                                    .onFailure { notice = clubChatSendError(it) }
                                sending = false
                            }
                        },
                        enabled = input.isNotBlank() && !sending,
                    ) {
                        Icon(Icons.Rounded.Send, sh("Gönder", "Send"), tint = SonHarfTheme.Primary)
                    }
                }
            }
        }
    }
}

private fun clubChatSendError(error: Throwable): String {
    val raw = error.message.orEmpty()
    return when {
        raw.contains("chat_suspended", ignoreCase = true) ->
            sh("Sohbet erişimin geçici olarak askıya alınmış.", "Your chat access is temporarily suspended.")
        raw.contains("club_chat_rate_limited", ignoreCase = true) ->
            sh("Mesajları çok hızlı gönderiyorsun. Kısa süre sonra tekrar dene.", "You're sending messages too quickly. Try again shortly.")
        raw.contains("club_chat_duplicate_message", ignoreCase = true) ->
            sh("Aynı mesajı art arda gönderemezsin.", "You can't repeat the same message immediately.")
        else -> sh("Mesaj gönderilemedi.", "Message could not be sent.")
    }
}
