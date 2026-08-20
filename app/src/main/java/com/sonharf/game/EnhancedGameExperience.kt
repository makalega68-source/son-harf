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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.ChatMessageDto
import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.GameWordDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Final root used by MainActivity.
 * Keeps the existing Aurora UI and combo effects intact, then adds two gameplay-only layers:
 * 1) last accepted word -> required last letter transition in the same visual focal point
 * 2) an always-visible in-game keyboard so Android's system keyboard never covers word entry
 */
@Composable
fun AuroraSonHarfAppEnhanced() {
    Box(Modifier.fillMaxSize()) {
        AuroraSonHarfAppWithCombo()
        EnhancedGameLayer()
    }
}

@Composable
private fun EnhancedGameLayer() {
    if (!SupabaseProvider.configured) return

    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var room by remember { mutableStateOf<GameRoomDto?>(null) }
    var words by remember { mutableStateOf<List<GameWordDto>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var showLastWord by remember { mutableStateOf(false) }
    var showChat by remember { mutableStateOf(false) }
    var chat by remember { mutableStateOf<List<ChatMessageDto>>(emptyList()) }
    var chatInput by remember { mutableStateOf("") }
    var confirmForfeit by remember { mutableStateOf(false) }

    val me = backend.currentUserId()

    suspend fun findActiveRoom(): GameRoomDto? {
        val uid = backend.currentUserId() ?: return null
        return SupabaseProvider.client.from("game_rooms").select().decodeList<GameRoomDto>()
            .filter {
                (it.hostId == uid || it.guestId == uid) &&
                    it.status in listOf("playing", "final", "sudden_death", "quiz", "paused")
            }
            .maxByOrNull { it.validWordCount }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val active = runCatching { findActiveRoom() }.getOrNull()
            room = active
            if (active != null) {
                words = runCatching { backend.getWords(active.id) }.getOrDefault(words)
                if (!active.isBot && showChat) {
                    chat = runCatching { backend.getChat(active.id) }.getOrDefault(chat)
                }
            } else {
                words = emptyList()
                input = ""
                showChat = false
            }
            delay(500)
        }
    }

    val lastWord = words.lastOrNull()
    LaunchedEffect(lastWord?.id) {
        if (lastWord != null) {
            showLastWord = true
            delay(720)
            showLastWord = false
        }
    }

    LaunchedEffect(room?.currentPlayerId, room?.validWordCount, room?.roundNo) {
        input = ""
        notice = null
    }

    val active = room ?: return
    if (active.status !in listOf("playing", "final", "sudden_death")) return

    val myTurn = active.currentPlayerId == me
    val normalizedLast = lastWord?.normalizedWord
    val requiredLetter = normalizedLast?.lastOrNull()?.uppercaseChar()?.toString() ?: "•"
    val centerText = if (showLastWord && normalizedLast != null) normalizedLast.uppercase() else requiredLetter
    val centerLabel = if (showLastWord && normalizedLast != null) "SON KELİME" else "SON HARF"

    Box(Modifier.fillMaxSize()) {
        LastWordToLetterFocus(
            label = centerLabel,
            text = centerText,
            isWord = showLastWord && normalizedLast != null,
            modifier = Modifier.align(Alignment.Center).offset(y = (-72).dp)
        )

        InGameKeyboard(
            language = active.language,
            value = input,
            enabled = myTurn && !busy,
            notice = notice,
            chatEnabled = !active.isBot,
            onKey = { key ->
                if (myTurn && !busy && input.length < 40) {
                    input += key
                    SonHarfSoundFx.tap()
                }
            },
            onBackspace = {
                if (myTurn && !busy && input.isNotEmpty()) {
                    input = input.dropLast(1)
                    SonHarfSoundFx.tap()
                }
            },
            onSend = {
                if (!myTurn || busy || input.isBlank()) return@InGameKeyboard
                val submitted = input.trim()
                input = ""
                busy = true
                scope.launch {
                    runCatching { backend.submitWord(active.id, submitted) }
                        .onSuccess { result ->
                            room = result
                            notice = when (result.lastEvent) {
                                "word_already_used" -> "Bu kelime daha önce kullanıldı."
                                "wrong_start_letter" -> "Kelime $requiredLetter ile başlamalı."
                                "not_in_dictionary" -> "Bu kelime sözlükte bulunamadı."
                                "invalid_word" -> "Bu kelime geçerli değil."
                                "turn_expired" -> "Süren doldu. −1 puan."
                                else -> "${submitted.uppercase()} kabul edildi."
                            }
                            if (result.lastEvent in setOf("word_already_used", "wrong_start_letter", "not_in_dictionary", "invalid_word", "turn_expired")) {
                                SonHarfSoundFx.warning()
                            } else {
                                SonHarfSoundFx.wordAccepted()
                            }
                        }
                        .onFailure {
                            notice = "Bağlantı sorunu. Yeniden deneniyor."
                            SonHarfSoundFx.warning()
                        }
                    busy = false
                }
            },
            onChat = {
                if (!active.isBot) {
                    showChat = true
                    scope.launch { chat = runCatching { backend.getChat(active.id) }.getOrDefault(emptyList()) }
                }
            },
            onForfeit = { confirmForfeit = true },
            onBonus = { notice = "Bonus turu geldiğinde soru otomatik açılır." },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showChat && !active.isBot) {
        EnhancedChatDialog(
            messages = chat,
            me = me,
            input = chatInput,
            onInput = { chatInput = it.take(300) },
            onDismiss = { showChat = false },
            onSend = {
                val text = chatInput.trim()
                if (text.isNotEmpty()) {
                    scope.launch {
                        runCatching { backend.sendChat(active.id, text) }
                            .onSuccess {
                                chatInput = ""
                                chat = runCatching { backend.getChat(active.id) }.getOrDefault(chat)
                            }
                            .onFailure { notice = "Mesaj gönderilemedi." }
                    }
                }
            }
        )
    }

    if (confirmForfeit) {
        AlertDialog(
            onDismissRequest = { confirmForfeit = false },
            title = { Text("PES ET") },
            text = { Text("Maçtan çıkarsan mağlup sayılacaksın. Emin misin?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmForfeit = false
                    scope.launch { runCatching { backend.forfeit(active.id) } }
                }) { Text("EVET, PES ET", color = SonHarfPink) }
            },
            dismissButton = { TextButton(onClick = { confirmForfeit = false }) { Text("VAZGEÇ") } }
        )
    }
}

@Composable
private fun LastWordToLetterFocus(label: String, text: String, isWord: Boolean, modifier: Modifier = Modifier) {
    val fontSize = when {
        !isWord -> 72.sp
        text.length <= 4 -> 48.sp
        text.length <= 7 -> 38.sp
        text.length <= 10 -> 30.sp
        else -> 24.sp
    }

    Box(
        modifier
            .size(174.dp)
            .clip(CircleShape)
            .background(Brush.sweepGradient(listOf(SonHarfCyan, SonHarfPurple, SonHarfPink, SonHarfCyan)))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.fillMaxSize().clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF17203A), Color(0xFF090E19)))),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, color = SonHarfMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = text,
                    color = SonHarfText,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun InGameKeyboard(
    language: String,
    value: String,
    enabled: Boolean,
    notice: String?,
    chatEnabled: Boolean,
    onKey: (String) -> Unit,
    onBackspace: () -> Unit,
    onSend: () -> Unit,
    onChat: () -> Unit,
    onForfeit: () -> Unit,
    onBonus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = if (language == "tr") {
        listOf(
            listOf("Q","W","E","R","T","Y","U","I","O","P","Ğ","Ü"),
            listOf("A","S","D","F","G","H","J","K","L","Ş","İ"),
            listOf("Z","X","C","V","B","N","M","Ö","Ç")
        )
    } else {
        listOf(
            listOf("Q","W","E","R","T","Y","U","I","O","P"),
            listOf("A","S","D","F","G","H","J","K","L"),
            listOf("Z","X","C","V","B","N","M")
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF050B15),
        tonalElevation = 0.dp,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = .08f))
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                KeyboardActionButton("⚑ PES ET", SonHarfPink, onForfeit, Modifier.weight(1f))
                KeyboardActionButton("● SOHBET", SonHarfCyan, onChat, Modifier.weight(1f), enabled = chatEnabled)
                KeyboardActionButton("★ BONUS", SonHarfGold, onBonus, Modifier.weight(1f))
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    modifier = Modifier.weight(1f).height(48.dp),
                    color = SonHarfSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (enabled) SonHarfCyan.copy(alpha = .45f) else Color.White.copy(alpha = .08f))
                ) {
                    Box(Modifier.fillMaxSize().padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
                        Text(
                            if (value.isBlank()) if (enabled) "Kelimenizi yazın…" else "Rakibin sırası…" else value.uppercase(),
                            color = if (value.isBlank()) SonHarfMuted else SonHarfText,
                            fontSize = 18.sp,
                            fontWeight = if (value.isBlank()) FontWeight.Normal else FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
                Button(
                    onClick = onSend,
                    enabled = enabled && value.isNotBlank(),
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SonHarfBlue)
                ) { Text("➤", fontSize = 20.sp) }
            }

            if (!notice.isNullOrBlank()) {
                Text(
                    notice,
                    color = if (notice.contains("geçerli") || notice.contains("bulunamadı") || notice.contains("başlamalı") || notice.contains("sorunu")) Color(0xFFFF829D) else SonHarfMuted,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 9.sp,
                    maxLines = 1
                )
            }

            rows.forEachIndexed { rowIndex, row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (rowIndex == 2) Spacer(Modifier.weight(.35f))
                    row.forEach { key ->
                        Button(
                            onClick = { onKey(key) },
                            enabled = enabled,
                            modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF152137),
                                contentColor = SonHarfText,
                                disabledContainerColor = Color(0xFF0C1422),
                                disabledContentColor = SonHarfMuted.copy(alpha = .45f)
                            )
                        ) { Text(key, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    }
                    if (rowIndex == 2) {
                        Button(
                            onClick = onBackspace,
                            enabled = enabled && value.isNotEmpty(),
                            modifier = Modifier.weight(1.35f).height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25182A))
                        ) { Text("⌫", fontSize = 18.sp) }
                        Spacer(Modifier.weight(.35f))
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyboardActionButton(
    text: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(34.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = .55f)),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
    ) { Text(text, color = if (enabled) accent else SonHarfMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun EnhancedChatDialog(
    messages: List<ChatMessageDto>,
    me: String?,
    input: String,
    onInput: (String) -> Unit,
    onDismiss: () -> Unit,
    onSend: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("MAÇ SOHBETİ", fontWeight = FontWeight.Black) },
        text = {
            Column(Modifier.heightIn(max = 420.dp)) {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(messages.takeLast(30)) { message ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = if (message.senderId == me) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                color = if (message.senderId == me) SonHarfPurple.copy(alpha = .18f) else SonHarfSurface2,
                                shape = RoundedCornerShape(12.dp)
                            ) { Text(message.body, Modifier.padding(9.dp), fontSize = 11.sp) }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = onInput,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Mesaj yaz…") }
                )
            }
        },
        confirmButton = { TextButton(onClick = onSend, enabled = input.isNotBlank()) { Text("GÖNDER") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("KAPAT") } }
    )
}
