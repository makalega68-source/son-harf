package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sonharf.game.data.ChatMessageDto
import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.GameWordDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Regression guard for the approved gameplay keyboard/chat contract.
 * This intentionally lives outside the arena implementations so later visual refactors
 * cannot silently remove the custom keyboard again.
 */
@Composable
fun RegressionGuardOverlay() {
    if (!SupabaseProvider.configured) return

    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var room by remember { mutableStateOf<GameRoomDto?>(null) }
    var words by remember { mutableStateOf<List<GameWordDto>>(emptyList()) }
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var wordInput by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var showChat by remember { mutableStateOf(false) }
    var showVipNotice by remember { mutableStateOf(false) }
    var confirmForfeit by remember { mutableStateOf(false) }
    var chatMessages by remember { mutableStateOf<List<ChatMessageDto>>(emptyList()) }
    var chatInput by remember { mutableStateOf("") }

    fun friendly(raw: String): String = when {
        "not_your_turn" in raw -> "Sıra rakibinde."
        "word_already_used" in raw -> "Bu kelime daha önce kullanıldı."
        "wrong_start_letter" in raw -> "Kelime son harfle başlamalı."
        "not_in_dictionary" in raw -> "Bu kelime sözlükte bulunamadı."
        "invalid_word" in raw -> "Bu kelime geçerli değil."
        "turn_expired" in raw -> "Süren doldu."
        else -> "Hamle gönderilemedi. Tekrar dene."
    }

    LaunchedEffect(Unit) {
        var lastRoomId: String? = null
        while (true) {
            val me = backend.currentUserId()
            if (me == null) {
                room = null
                words = emptyList()
                profile = null
            } else {
                profile = profile ?: runCatching { backend.getProfile(me) }.getOrNull()
                val active = runCatching {
                    SupabaseProvider.client.from("game_rooms").select().decodeList<GameRoomDto>()
                        .filter {
                            (it.hostId == me || it.guestId == me) &&
                                it.status in listOf("playing", "final", "sudden_death")
                        }
                        .maxByOrNull { it.validWordCount }
                }.getOrNull()
                room = active
                if (active == null) {
                    words = emptyList()
                    wordInput = ""
                    showChat = false
                    lastRoomId = null
                } else {
                    if (active.id != lastRoomId) {
                        wordInput = ""
                        notice = null
                        lastRoomId = active.id
                    }
                    words = runCatching { backend.getWords(active.id) }.getOrDefault(words)
                    if (showChat && !active.isBot) {
                        chatMessages = runCatching { backend.getChat(active.id) }.getOrDefault(chatMessages)
                    }
                }
            }
            delay(450)
        }
    }

    val active = room ?: return
    val me = backend.currentUserId()
    val myTurn = active.currentPlayerId == me && active.status in listOf("playing", "final", "sudden_death")
    val isVip = profile?.isVip == true
    val requiredLetter = words.lastOrNull()?.normalizedWord?.lastOrNull()?.uppercaseChar()?.toString()

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        StableGameKeyboard(
            language = active.language,
            value = wordInput,
            enabled = myTurn && !busy,
            requiredLetter = requiredLetter,
            notice = notice,
            chatEnabled = !active.isBot && isVip,
            onKey = { key -> if (myTurn && !busy && wordInput.length < 40) wordInput += key.lowercase() },
            onBackspace = { if (myTurn && !busy && wordInput.isNotEmpty()) wordInput = wordInput.dropLast(1) },
            onSend = {
                if (!myTurn || busy || wordInput.isBlank()) return@StableGameKeyboard
                val submitted = wordInput.trim()
                busy = true
                scope.launch {
                    runCatching { backend.submitWord(active.id, submitted) }
                        .onSuccess { updated ->
                            room = updated
                            wordInput = ""
                            notice = when (updated.lastEvent) {
                                "word_already_used" -> "Bu kelime daha önce kullanıldı."
                                "wrong_start_letter" -> "Kelime ${requiredLetter ?: "son harf"} ile başlamalı."
                                "not_in_dictionary" -> "Bu kelime sözlükte bulunamadı."
                                "invalid_word" -> "Bu kelime geçerli değil."
                                "turn_expired" -> "Süren doldu."
                                else -> "${submitted.uppercase()} kabul edildi."
                            }
                        }
                        .onFailure { notice = friendly(it.message.orEmpty()) }
                    busy = false
                }
            },
            onForfeit = { confirmForfeit = true },
            onChat = {
                if (active.isBot) return@StableGameKeyboard
                if (!isVip) showVipNotice = true
                else {
                    showChat = true
                    scope.launch { chatMessages = runCatching { backend.getChat(active.id) }.getOrDefault(emptyList()) }
                }
            },
        )
    }

    if (confirmForfeit) {
        AlertDialog(
            onDismissRequest = { confirmForfeit = false },
            title = { Text(sh("PES ET", "FORFEIT"), fontWeight = FontWeight.Black) },
            text = { Text(sh("Maçtan çıkarsan mağlup sayılacaksın. Emin misin?", "You will lose the match if you leave. Are you sure?")) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmForfeit = false
                        scope.launch {
                            busy = true
                            runCatching { backend.forfeit(active.id) }
                                .onSuccess {
                                    room = null
                                    words = emptyList()
                                    wordInput = ""
                                    SonHarfUiState.homeRequest += 1
                                }
                                .onFailure { notice = friendly(it.message.orEmpty()) }
                            busy = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SonHarfPink),
                ) { Text(sh("EVET, PES ET", "YES, FORFEIT")) }
            },
            dismissButton = { TextButton(onClick = { confirmForfeit = false }) { Text(sh("OYUNA DÖN", "RETURN TO GAME")) } },
        )
    }

    if (showVipNotice) {
        AlertDialog(
            onDismissRequest = { showVipNotice = false },
            title = { Text("VIP") },
            text = { Text(sh("Oyun içi sohbet VIP üyelerine özeldir.", "In-game chat is for VIP members.")) },
            confirmButton = { TextButton(onClick = { showVipNotice = false }) { Text(sh("TAMAM", "OK")) } },
        )
    }

    if (showChat && !active.isBot && isVip) {
        StableChatDialog(
            language = active.language,
            messages = chatMessages,
            me = me,
            value = chatInput,
            onValue = { chatInput = it.take(300) },
            onDismiss = { showChat = false },
            onSend = {
                val text = chatInput.trim()
                if (text.isBlank()) return@StableChatDialog
                scope.launch {
                    runCatching { backend.sendChat(active.id, text) }
                        .onSuccess {
                            chatInput = ""
                            chatMessages = runCatching { backend.getChat(active.id) }.getOrDefault(chatMessages)
                        }
                }
            },
        )
    }
}

@Composable
private fun StableGameKeyboard(
    language: String,
    value: String,
    enabled: Boolean,
    requiredLetter: String?,
    notice: String?,
    chatEnabled: Boolean,
    onKey: (String) -> Unit,
    onBackspace: () -> Unit,
    onSend: () -> Unit,
    onForfeit: () -> Unit,
    onChat: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
        color = SonHarfBg,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, SonHarfCyan.copy(alpha = .28f)),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedButton(onClick = onForfeit, modifier = Modifier.weight(1f).height(42.dp), border = BorderStroke(1.dp, SonHarfPink)) {
                    Text("⚑ ${sh("PES ET", "FORFEIT")}", color = SonHarfPink, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                OutlinedButton(onClick = onChat, modifier = Modifier.weight(1f).height(42.dp), border = BorderStroke(1.dp, SonHarfCyan.copy(alpha = .65f))) {
                    Text(if (chatEnabled) "💬 ${sh("SOHBET", "CHAT")}" else "🔒 ${sh("SOHBET • VIP", "CHAT • VIP")}", color = SonHarfCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Surface(
                    Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = SonHarfSurface,
                    border = BorderStroke(2.dp, if (enabled) SonHarfCyan else SonHarfMuted.copy(alpha = .24f)),
                ) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            when {
                                value.isNotBlank() -> value.uppercase()
                                !enabled -> sh("Rakibin sırası…", "Opponent's turn…")
                                requiredLetter != null -> sh("$requiredLetter ile başlayan kelime yaz", "Write a word starting with $requiredLetter")
                                else -> sh("Kelimenizi yazın…", "Type your word…")
                            },
                            color = if (value.isBlank()) SonHarfMuted else SonHarfText,
                            fontSize = 17.sp,
                            fontWeight = if (value.isBlank()) FontWeight.Normal else FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Button(
                    onClick = onSend,
                    enabled = enabled && value.isNotBlank(),
                    modifier = Modifier.size(50.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SonHarfCyan),
                ) { Text("➤", color = Color.White, fontSize = 20.sp) }
            }
            if (!notice.isNullOrBlank()) {
                Text(notice, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = if (notice.contains("gönderilemedi", true) || notice.contains("bulunamadı", true) || notice.contains("geçerli", true)) SonHarfPink else SonHarfMuted, fontSize = 9.sp, maxLines = 1)
            }
            StableKeyRows(language, enabled, onKey, onBackspace)
        }
    }
}

@Composable
private fun StableKeyRows(language: String, enabled: Boolean, onKey: (String) -> Unit, onBackspace: () -> Unit, onSpace: (() -> Unit)? = null) {
    val rows = if (language == "tr") {
        listOf(
            listOf("Q","W","E","R","T","Y","U","I","O","P","Ğ","Ü"),
            listOf("A","S","D","F","G","H","J","K","L","Ş","İ"),
            listOf("Z","X","C","V","B","N","M","Ö","Ç"),
        )
    } else {
        listOf(
            listOf("Q","W","E","R","T","Y","U","I","O","P"),
            listOf("A","S","D","F","G","H","J","K","L"),
            listOf("Z","X","C","V","B","N","M"),
        )
    }
    rows.forEachIndexed { rowIndex, row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            if (rowIndex == 2) Spacer(Modifier.weight(.28f))
            row.forEach { key ->
                Button(
                    onClick = { onKey(key) },
                    enabled = enabled,
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SonHarfSurface2, contentColor = SonHarfText),
                ) { Text(key, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
            if (rowIndex == 2) Spacer(Modifier.weight(.28f))
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(onClick = onBackspace, enabled = enabled, modifier = Modifier.weight(1f).height(38.dp)) { Text("⌫", fontSize = 18.sp) }
        if (onSpace != null) {
            OutlinedButton(onClick = onSpace, enabled = enabled, modifier = Modifier.weight(2f).height(38.dp)) { Text(sh("BOŞLUK", "SPACE"), fontSize = 10.sp) }
        }
    }
}

@Composable
private fun StableChatDialog(
    language: String,
    messages: List<ChatMessageDto>,
    me: String?,
    value: String,
    onValue: (String) -> Unit,
    onDismiss: () -> Unit,
    onSend: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            Modifier.fillMaxWidth().heightIn(max = 690.dp),
            shape = RoundedCornerShape(24.dp),
            color = SonHarfBg,
            border = BorderStroke(1.dp, SonHarfCyan.copy(alpha = .35f)),
        ) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(sh("OYUN SOHBETİ", "GAME CHAT"), color = SonHarfText, fontWeight = FontWeight.Black, fontSize = 17.sp)
                    TextButton(onClick = onDismiss) { Text("×", fontSize = 24.sp) }
                }
                LazyColumn(Modifier.fillMaxWidth().heightIn(min = 90.dp, max = 220.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    items(messages.takeLast(40)) { message ->
                        val mine = message.senderId == me
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                            Surface(color = if (mine) SonHarfCyan.copy(alpha = .14f) else SonHarfPink.copy(alpha = .11f), shape = RoundedCornerShape(11.dp)) {
                                Text(message.body, Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = SonHarfText, fontSize = 12.sp)
                            }
                        }
                    }
                }
                Surface(Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(13.dp), color = SonHarfSurface, border = BorderStroke(1.dp, SonHarfCyan.copy(alpha = .5f))) {
                    Box(Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                        Text(if (value.isBlank()) sh("Mesaj yaz…", "Type a message…") else value, color = if (value.isBlank()) SonHarfMuted else SonHarfText, fontSize = 14.sp, maxLines = 1)
                    }
                }
                StableKeyRows(language, true, { onValue(value + it.lowercase()) }, { if (value.isNotEmpty()) onValue(value.dropLast(1)) }, { if (value.isNotBlank() && !value.endsWith(" ")) onValue("$value ") })
                Button(onClick = onSend, enabled = value.isNotBlank(), modifier = Modifier.fillMaxWidth().height(44.dp), colors = ButtonDefaults.buttonColors(containerColor = SonHarfCyan)) {
                    Text(sh("GÖNDER", "SEND"), color = Color.White, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
