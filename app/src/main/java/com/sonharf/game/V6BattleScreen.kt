package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
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
import com.sonharf.game.data.*
import io.github.jan.supabase.postgrest.from
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private val BattleCream = Color(0xFFF9F6F0)
private val BattleWhite = Color(0xFFFFFFFF)
private val BattleSage = Color(0xFF2E6F5E)
private val BattleCoral = Color(0xFFE05A47)
private val BattleAmber = Color(0xFFE5A93C)
private val BattleText = Color(0xFF1E293B)
private val BattleMuted = Color(0xFF64748B)
private val BattleKey = Color(0xFFECE7DE)
private val BattleKeyBorder = Color(0xFFD6CFC4)

@Composable
fun V6BattleScreen(onLeaveBattle: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var meProfile by remember { mutableStateOf<V6ProfileDto?>(null) }
    var opponent by remember { mutableStateOf<V6ProfileDto?>(null) }
    var meAvatar by remember { mutableStateOf<String?>(null) }
    var opponentAvatar by remember { mutableStateOf<String?>(null) }
    var room by remember { mutableStateOf<GameRoomDto?>(null) }
    var words by remember { mutableStateOf<List<GameWordDto>>(emptyList()) }
    var chat by remember { mutableStateOf<List<ChatMessageDto>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("Düelloya hazır") }
    var matching by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var showChat by remember { mutableStateOf(false) }

    suspend fun loadSelf() {
        val id = backend.currentUserId() ?: return
        meProfile = runCatching { v6LoadProfile(id) }.getOrNull()
        meAvatar = runCatching { AvatarSignedUrl.resolve(meProfile?.avatarPath) }.getOrNull()
    }

    suspend fun loadOpponent(active: GameRoomDto) {
        if (active.isBot) {
            opponent = null
            opponentAvatar = null
            return
        }
        val me = backend.currentUserId()
        val opponentId = if (active.hostId == me) active.guestId else active.hostId
        opponent = opponentId?.let { runCatching { v6LoadProfile(it) }.getOrNull() }
        opponentAvatar = runCatching { AvatarSignedUrl.resolve(opponent?.avatarPath) }.getOrNull()
    }

    suspend fun findActiveRoom(): GameRoomDto? {
        val me = backend.currentUserId() ?: return null
        return SupabaseProvider.client.from("game_rooms")
            .select()
            .decodeList<GameRoomDto>()
            .filter {
                (it.hostId == me || it.guestId == me) &&
                    it.status in listOf("waiting", "playing", "quiz", "final", "sudden_death", "paused")
            }
            .maxByOrNull { it.validWordCount }
    }

    LaunchedEffect(Unit) {
        loadSelf()
        room = runCatching { findActiveRoom() }.getOrNull()
        room?.let { loadOpponent(it) }
    }

    val active = room
    if (active == null) {
        V6BattleLobby(
            profile = meProfile,
            avatar = meAvatar,
            matching = matching,
            notice = notice,
            onBack = onLeaveBattle,
            onRandom = {
                scope.launch {
                    if (busy) return@launch
                    busy = true
                    runCatching { backend.startRandomMatchmaking("tr") }
                        .onSuccess {
                            matching = true
                            notice = "Rakip aranıyor…"
                        }
                        .onFailure { notice = "Eşleşme başlatılamadı. Tekrar dene." }
                    busy = false
                    while (matching && room == null) {
                        val found = runCatching { backend.pollRandomMatchmakingRoom() }.getOrNull()
                        if (found != null) {
                            room = found
                            loadOpponent(found)
                            matching = false
                            break
                        }
                        delay(800)
                    }
                }
            },
            onCancel = {
                scope.launch {
                    matching = false
                    runCatching { backend.cancelRandomMatchmaking() }
                    notice = "Eşleşme iptal edildi."
                }
            },
        )
        return
    }

    val me = backend.currentUserId()

    LaunchedEffect(active.id) {
        while (isActive) {
            runCatching { backend.getRoom(active.id) }
                .onSuccess { fresh ->
                    room = fresh
                    loadOpponent(fresh)
                }
            runCatching { backend.getWords(active.id) }.onSuccess { words = it }
            runCatching { backend.getChat(active.id) }.onSuccess { chat = it }
            delay(650)
        }
    }

    LaunchedEffect(active.currentPlayerId, active.validWordCount, active.roundNo, words.size) {
        val required = words.lastOrNull()?.normalizedWord?.lastOrNull()?.uppercaseChar()
        input = if (
            active.currentPlayerId == me &&
            active.status in listOf("playing", "final", "sudden_death") &&
            required != null
        ) required.toString() else ""
    }

    LaunchedEffect(active.id, active.botTurn, active.status, active.validWordCount) {
        if (active.isBot && active.botTurn && active.status in listOf("playing", "final", "sudden_death")) {
            delay(750)
            runCatching { backend.botTakeTurn(active.id) }
                .onSuccess { room = it }
                .onFailure { notice = "Bot sırası yenileniyor…" }
        }
    }

    V6BattleArena(
        room = active,
        me = me,
        myName = meProfile?.displayName ?: "Sen",
        myAvatar = meAvatar,
        opponentName = if (active.isBot) active.botName ?: "KelimeBot" else opponent?.displayName ?: "Rakip",
        opponentAvatar = opponentAvatar,
        words = words,
        input = input,
        notice = notice,
        busy = busy,
        onInput = { input = it.take(40) },
        onSubmit = {
            val submitted = input.trim()
            if (submitted.length < 2) return@V6BattleArena
            scope.launch {
                busy = true
                runCatching { backend.submitWord(active.id, submitted) }
                    .onSuccess {
                        room = it
                        input = ""
                        notice = "${submitted.uppercase()} kabul edildi."
                    }
                    .onFailure { error ->
                        val raw = error.message.orEmpty()
                        notice = when {
                            "not_your_turn" in raw -> "Sıra rakibinde."
                            "wrong_start_letter" in raw -> "Kelime doğru harfle başlamalı."
                            "word_already_used" in raw -> "Bu kelime daha önce kullanıldı."
                            "not_in_dictionary" in raw -> "Kelime sözlükte bulunamadı."
                            "turn_expired" in raw -> "Süren doldu."
                            else -> "Kelime gönderilemedi. Tekrar dene."
                        }
                    }
                busy = false
            }
        },
        onForfeit = {
            scope.launch {
                runCatching { backend.forfeit(active.id) }
                    .onSuccess { room = it }
            }
        },
        onExit = onLeaveBattle,
        onChat = { showChat = true },
        onTacticInfo = { tactic -> notice = "$tactic jokeri backend oyun davranışı tamamlandığında aktif olacak." },
    )

    if (showChat) {
        V6ChatSheet(
            messages = chat,
            me = me,
            enabled = meProfile?.allowMatchChat != false,
            onDismiss = { showChat = false },
            onSend = { raw ->
                scope.launch {
                    val text = raw.trim().take(300)
                    if (text.isBlank()) return@launch
                    runCatching { backend.sendChat(active.id, text) }
                        .onSuccess {
                            chat = runCatching { backend.getChat(active.id) }.getOrDefault(chat)
                            notice = "Mesaj gönderildi."
                        }
                        .onFailure { notice = "Mesaj gönderilemedi. Sohbet iznini kontrol et." }
                }
            },
        )
    }
}

@Composable
private fun V6BattleLobby(
    profile: V6ProfileDto?,
    avatar: String?,
    matching: Boolean,
    notice: String,
    onBack: () -> Unit,
    onRandom: () -> Unit,
    onCancel: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize().background(V6Light.bg),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Geri", tint = V6Light.text) }
                Spacer(Modifier.weight(1f))
                Text("SON HARF", fontWeight = FontWeight.Black, fontSize = 22.sp, color = V6Light.text)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(48.dp))
            }
        }
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = V6Light.white,
                border = BorderStroke(1.dp, V6Light.border),
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    V6Avatar(avatar, profile?.displayName ?: "Oyuncu", 54)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(profile?.displayName ?: "Oyuncu", fontWeight = FontWeight.Bold, color = V6Light.text)
                        Text(if (matching) "Rakip aranıyor…" else "Düelloya hazırsın", color = V6Light.muted)
                    }
                }
            }
        }
        if (matching) {
            item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = V6Light.blue) } }
            item {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    border = BorderStroke(1.dp, V6Light.red),
                ) { Text("EŞLEŞMEYİ İPTAL ET", color = V6Light.red) }
            }
        } else {
            item {
                Button(
                    onClick = onRandom,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = V6Light.blue),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Rounded.Bolt, null)
                    Spacer(Modifier.width(8.dp))
                    Text("1v1 HIZLI KARŞILAŞMA", fontWeight = FontWeight.Black)
                }
            }
        }
        item { Text(notice, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = V6Light.muted, fontSize = 12.sp) }
    }
}

@Composable
private fun V6BattleArena(
    room: GameRoomDto,
    me: String?,
    myName: String,
    myAvatar: String?,
    opponentName: String,
    opponentAvatar: String?,
    words: List<GameWordDto>,
    input: String,
    notice: String,
    busy: Boolean,
    onInput: (String) -> Unit,
    onSubmit: () -> Unit,
    onForfeit: () -> Unit,
    onExit: () -> Unit,
    onChat: () -> Unit,
    onTacticInfo: (String) -> Unit,
) {
    val host = me == room.hostId
    val myScore = if (host) room.hostScore else room.guestScore
    val opponentScore = if (host) room.guestScore else room.hostScore
    val streak = if (host) room.hostStreak else room.guestStreak
    val myTurn = room.currentPlayerId == me && room.status in listOf("playing", "final", "sudden_death")
    val lastWord = words.lastOrNull()?.word?.uppercase().orEmpty()
    val required = words.lastOrNull()?.normalizedWord?.lastOrNull()?.uppercaseChar()
    var seconds by remember(room.turnDeadline) { mutableStateOf(45) }

    LaunchedEffect(room.turnDeadline, room.currentPlayerId, room.status) {
        while (isActive && room.turnDeadline != null && room.status in listOf("playing", "final", "sudden_death")) {
            seconds = runCatching {
                (Instant.parse(room.turnDeadline).epochSecond - Instant.now().epochSecond).toInt().coerceAtLeast(0)
            }.getOrDefault(45)
            delay(1000)
        }
    }

    Column(
        Modifier.fillMaxSize().background(BattleCream).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = BattleAmber, modifier = Modifier.size(38.dp)) {
                Box(contentAlignment = Alignment.Center) { Text("🔥", fontSize = 18.sp) }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                if (streak > 1) "$streak Kelime Seri" else "Kelime Düellosu",
                color = BattleText,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
            )
            Spacer(Modifier.weight(1f))
            Surface(shape = RoundedCornerShape(18.dp), color = BattleSage.copy(alpha = .12f), border = BorderStroke(1.dp, BattleSage)) {
                Text("Puan: $myScore", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = BattleSage, fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
            IconButton(onClick = onExit) { Icon(Icons.Rounded.Close, "Ayrıl", tint = BattleText) }
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = BattleWhite,
            shadowElevation = 2.dp,
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("CANLI 1v1", color = BattleMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (room.status == "quiz") "Bilgi Sorusu Turu" else "Klasik Son Harf",
                        color = BattleSage,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                Surface(onClick = onChat, shape = RoundedCornerShape(20.dp), color = BattleCoral.copy(alpha = .10f)) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.ChatBubble, null, tint = BattleCoral, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Canlı Sohbet", color = BattleCoral, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = BattleWhite,
            shadowElevation = 4.dp,
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    V6ModernBattlePlayer(myName, myAvatar, myScore, myTurn, Modifier.weight(1f))
                    Box(
                        Modifier.size(62.dp).clip(CircleShape).background(if (seconds <= 5) BattleCoral else BattleAmber),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("$seconds", color = Color.White, fontWeight = FontWeight.Black, fontSize = 25.sp)
                    }
                    V6ModernBattlePlayer(opponentName, opponentAvatar, opponentScore, !myTurn, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    when {
                        room.status == "quiz" -> "BİLGİ SORUSU TURU"
                        room.status == "paused" -> "MAÇ DURAKLATILDI"
                        myTurn -> "SIRA SİZDE"
                        else -> "RAKİP BEKLENİYOR…"
                    },
                    color = if (myTurn) BattleSage else BattleMuted,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(6.dp))
                if (lastWord.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Son Kelime: ", color = BattleMuted, fontSize = 14.sp)
                        if (lastWord.length > 1) Text(lastWord.dropLast(1), color = BattleText, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text(lastWord.takeLast(1), color = BattleCoral, fontSize = 23.sp, fontWeight = FontWeight.Black)
                    }
                } else {
                    Text("İlk kelimeyi siz başlatın", color = BattleMuted, fontSize = 14.sp)
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onTacticInfo("Ğ Kilidi") },
                enabled = myTurn && !busy,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BattleAmber),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                Icon(Icons.Rounded.Bolt, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Ğ Kilidi", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { onTacticInfo("Süre Dondur") },
                enabled = myTurn && !busy,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BattleSage),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                Text("❄ Süre Dondur", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Surface(
            Modifier.fillMaxWidth().heightIn(min = 70.dp),
            shape = RoundedCornerShape(14.dp),
            color = BattleWhite,
            border = BorderStroke(2.dp, BattleSage.copy(alpha = .42f)),
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (input.isBlank()) {
                    Text(
                        if (required == null) "Kelime yazın…" else "'$required' ile başlayan kelime yazın…",
                        color = BattleMuted.copy(alpha = .72f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                } else {
                    Text(input, color = BattleText, fontSize = 23.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
                if (notice.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(notice, color = BattleCoral, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        V6OnScreenKeyboard(
            enabled = myTurn && !busy,
            submitEnabled = myTurn && !busy && input.length >= 2 &&
                (required == null || input.firstOrNull()?.uppercaseChar() == required),
            onKey = { char ->
                val next = if (input.isEmpty() && required != null) "$required$char" else input + char
                onInput(next)
            },
            onDelete = {
                if (input.length > if (required == null) 0 else 1) onInput(input.dropLast(1))
            },
            onSubmit = onSubmit,
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = onForfeit) {
                Icon(Icons.Rounded.Flag, null, tint = BattleCoral, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Pes Et", color = BattleCoral, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun V6ModernBattlePlayer(name: String, avatar: String?, score: Int, active: Boolean, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(50.dp).clip(CircleShape).background(if (active) BattleSage.copy(alpha = .16f) else BattleCream).padding(2.dp),
            contentAlignment = Alignment.Center,
        ) {
            V6Avatar(avatar, name, 46)
        }
        Text(name, maxLines = 1, color = BattleText, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Text("$score puan", color = if (active) BattleSage else BattleMuted, fontWeight = FontWeight.Bold, fontSize = 10.sp)
    }
}

@Composable
private fun V6OnScreenKeyboard(
    enabled: Boolean,
    submitEnabled: Boolean,
    onKey: (Char) -> Unit,
    onDelete: () -> Unit,
    onSubmit: () -> Unit,
) {
    val row1 = listOf('Q','W','E','R','T','Y','U','I','O','P','Ğ','Ü')
    val row2 = listOf('A','S','D','F','G','H','J','K','L','Ş','İ')
    val row3 = listOf('Z','X','C','V','B','N','M','Ö','Ç')

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = BattleWhite,
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(6.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf(row1, row2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    row.forEach { char -> V6KeyboardKey(char, Modifier.weight(1f), enabled) { onKey(char) } }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onDelete,
                    enabled = enabled,
                    modifier = Modifier.weight(1.7f).height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BattleCoral, disabledContainerColor = BattleCoral.copy(alpha = .35f)),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(8.dp),
                ) { Text("⌫ SİL", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp) }

                row3.forEach { char -> V6KeyboardKey(char, Modifier.weight(1f), enabled) { onKey(char) } }

                Button(
                    onClick = onSubmit,
                    enabled = submitEnabled,
                    modifier = Modifier.weight(1.9f).height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BattleSage, disabledContainerColor = BattleSage.copy(alpha = .35f)),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(8.dp),
                ) { Text("✓ ONAY", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp) }
            }
        }
    }
}

@Composable
private fun V6KeyboardKey(char: Char, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(46.dp),
        color = if (enabled) BattleKey else BattleKey.copy(alpha = .55f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, BattleKeyBorder),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(char.toString(), color = if (enabled) BattleText else BattleMuted, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun V6ChatSheet(
    messages: List<ChatMessageDto>,
    me: String?,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit,
) {
    var input by remember { mutableStateOf("") }
    val quick = listOf("İyi oyunlar!", "Çok iyi kelime!", "Hadi bakalım :)", "Tebrikler!")
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = V6Light.white) {
        Column(
            Modifier.fillMaxWidth().heightIn(min = 420.dp, max = 650.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Oyun İçi Sohbet", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = V6Light.text)
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Kapat") }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                items(quick) { phrase ->
                    SuggestionChip(onClick = { onSend(phrase) }, enabled = enabled, label = { Text(phrase) })
                }
            }
            HorizontalDivider(color = V6Light.border)
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 10.dp),
            ) {
                items(messages, key = { it.id }) { msg ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = if (msg.senderId == me) Arrangement.End else Arrangement.Start,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (msg.senderId == me) V6Light.blue else V6Light.blueLight,
                        ) {
                            Text(
                                msg.body,
                                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                color = if (msg.senderId == me) Color.White else V6Light.text,
                            )
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().imePadding(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.take(300) },
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("Mesaj yaz…") },
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val text = input.trim()
                        if (text.isNotBlank()) {
                            onSend(text)
                            input = ""
                        }
                    },
                    enabled = enabled,
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(V6Light.blue),
                ) { Icon(Icons.Rounded.Send, "Gönder", tint = Color.White) }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
