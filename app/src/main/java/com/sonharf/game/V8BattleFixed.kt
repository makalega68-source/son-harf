package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sonharf.game.data.*
import io.github.jan.supabase.postgrest.from
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private val B8Bg = Color(0xFFF8FAFC)
private val B8White = Color.White
private val B8Blue = Color(0xFF0284C7)
private val B8BlueLight = Color(0xFFE0F2FE)
private val B8Text = Color(0xFF0F172A)
private val B8Muted = Color(0xFF64748B)
private val B8Green = Color(0xFF2E6F5E)
private val B8Coral = Color(0xFFE05A47)
private val B8Amber = Color(0xFFE5A93C)
private val B8Purple = Color(0xFF7C3AED)

@Composable
fun V8BattleScreenFixed(onLeaveBattle: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var meProfile by remember { mutableStateOf<V6ProfileDto?>(null) }
    var meVip by remember { mutableStateOf(false) }
    var meAvatar by remember { mutableStateOf<String?>(null) }
    var opponent by remember { mutableStateOf<V6ProfileDto?>(null) }
    var opponentAvatar by remember { mutableStateOf<String?>(null) }
    var room by remember { mutableStateOf<GameRoomDto?>(null) }
    var words by remember { mutableStateOf<List<GameWordDto>>(emptyList()) }
    var chat by remember { mutableStateOf<List<ChatMessageDto>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("Düelloya hazır") }
    var matching by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var showChat by remember { mutableStateOf(false) }
    var showWords by remember { mutableStateOf(false) }
    var triviaRound by remember { mutableStateOf<TriviaRoundDto?>(null) }
    var triviaQuestion by remember { mutableStateOf<TriviaQuestionDto?>(null) }

    suspend fun loadSelf() {
        val id = backend.currentUserId() ?: return
        meProfile = runCatching { v6LoadProfile(id) }.getOrNull()
        meAvatar = runCatching { AvatarSignedUrl.resolve(meProfile?.avatarPath) }.getOrNull()
        meVip = runCatching { backend.getProfile(id).isVip }.getOrDefault(false)
    }
    suspend fun loadOpponent(r: GameRoomDto) {
        if (r.isBot) { opponent = null; opponentAvatar = null; return }
        val me = backend.currentUserId()
        val id = if (r.hostId == me) r.guestId else r.hostId
        opponent = id?.let { runCatching { v6LoadProfile(it) }.getOrNull() }
        opponentAvatar = runCatching { AvatarSignedUrl.resolve(opponent?.avatarPath) }.getOrNull()
    }
    suspend fun findActive(): GameRoomDto? {
        val me = backend.currentUserId() ?: return null
        return SupabaseProvider.client.from("game_rooms").select().decodeList<GameRoomDto>()
            .filter { (it.hostId == me || it.guestId == me) && it.status in listOf("waiting", "playing", "quiz", "final", "sudden_death", "paused") }
            .maxByOrNull { it.validWordCount }
    }

    LaunchedEffect(Unit) {
        loadSelf()
        room = runCatching { findActive() }.getOrNull()
        room?.let { loadOpponent(it) }
    }

    val active = room
    if (active == null) {
        BattleLobbyFixed(meProfile, meAvatar, matching, notice, onLeaveBattle,
            onRandom = {
                scope.launch {
                    if (busy) return@launch
                    busy = true
                    runCatching { backend.startRandomMatchmaking("tr") }
                        .onSuccess { matching = true; notice = "Rakip aranıyor…" }
                        .onFailure { notice = "Eşleşme başlatılamadı." }
                    busy = false
                    while (matching && room == null) {
                        val found = runCatching { backend.pollRandomMatchmakingRoom() }.getOrNull()
                        if (found != null) { room = found; loadOpponent(found); matching = false; break }
                        delay(800)
                    }
                }
            },
            onCancel = { scope.launch { matching = false; runCatching { backend.cancelRandomMatchmaking() } } }
        )
        return
    }

    val me = backend.currentUserId()

    LaunchedEffect(active.id) {
        var heartbeatTick = 0
        while (isActive) {
            runCatching { backend.getRoom(active.id) }.onSuccess { room = it; loadOpponent(it) }
            runCatching { backend.getWords(active.id) }.onSuccess { words = it }
            runCatching { backend.getChat(active.id) }.onSuccess { chat = it }
            heartbeatTick++
            if (heartbeatTick % 7 == 0 && !active.isBot) runCatching { backend.heartbeatRoom(active.id) }.onSuccess { room = it }
            delay(700)
        }
    }

    LaunchedEffect(active.currentPlayerId, active.validWordCount, active.roundNo, words.size) {
        val req = words.lastOrNull()?.normalizedWord?.lastOrNull()?.uppercaseChar()
        input = if (active.currentPlayerId == me && active.status in listOf("playing", "final", "sudden_death") && req != null) req.toString() else ""
    }

    LaunchedEffect(active.turnDeadline, active.currentPlayerId, active.status) {
        if (active.status in listOf("playing", "final", "sudden_death") && active.turnDeadline != null) {
            while (isActive) {
                val left = runCatching { Instant.parse(active.turnDeadline).epochSecond - Instant.now().epochSecond }.getOrDefault(1)
                if (left <= 0) { runCatching { backend.claimTurnTimeout(active.id) }.onSuccess { room = it }; break }
                delay(800)
            }
        }
    }

    LaunchedEffect(active.id, active.botTurn, active.status, active.validWordCount) {
        if (active.isBot && active.botTurn && active.status in listOf("playing", "final", "sudden_death")) {
            delay(900)
            runCatching { backend.botTakeTurn(active.id) }
                .onSuccess { room = it; notice = "" }
                .onFailure {
                    notice = "Bot sırası yenileniyor…"
                    delay(900)
                    runCatching { backend.botTakeTurn(active.id) }.onSuccess { room = it }
                }
        }
    }

    LaunchedEffect(active.id, active.status, active.validWordCount) {
        if (active.status == "quiz") {
            triviaRound = runCatching { backend.getActiveTriviaRound(active.id) }.getOrNull()
            triviaQuestion = triviaRound?.let { runCatching { backend.getTriviaQuestion(it.questionId) }.getOrNull() }
            if (active.isBot) { delay(1200); runCatching { backend.botAnswerTrivia(active.id) }.onSuccess { room = it } }
        } else { triviaRound = null; triviaQuestion = null }
    }

    if (active.status == "finished") { BattleFinishedFixed(active, me, onLeaveBattle); return }

    BattleArenaFixed(
        room = active, me = me,
        myName = meProfile?.displayName ?: "Sen", myAvatar = meAvatar,
        oppName = if (active.isBot) active.botName ?: "KelimeBot" else opponent?.displayName ?: "Rakip",
        oppAvatar = opponentAvatar, words = words, input = input, notice = notice, busy = busy, vip = meVip,
        onInput = { input = it.take(40) },
        onSubmit = {
            val submitted = input.trim()
            if (submitted.length < 2) return@BattleArenaFixed
            scope.launch {
                busy = true
                val beforeCount = active.validWordCount
                val result = runCatching { backend.submitWord(active.id, submitted) }
                if (result.isSuccess) {
                    room = result.getOrThrow(); input = ""; notice = "${submitted.uppercase()} kabul edildi."
                } else {
                    delay(180)
                    val refreshedRoom = runCatching { backend.getRoom(active.id) }.getOrNull()
                    val refreshedWords = runCatching { backend.getWords(active.id) }.getOrDefault(words)
                    val last = refreshedWords.lastOrNull()
                    val acceptedOnServer = (refreshedRoom?.validWordCount ?: beforeCount) > beforeCount ||
                        (last?.playerId == me && last.word.trim().equals(submitted, ignoreCase = true))
                    if (acceptedOnServer) {
                        if (refreshedRoom != null) room = refreshedRoom
                        words = refreshedWords
                        input = ""
                        notice = "${submitted.uppercase()} kabul edildi."
                    } else {
                        val raw = result.exceptionOrNull()?.message.orEmpty()
                        notice = when {
                            "not_your_turn" in raw -> "Sıra rakibinde."
                            "wrong_start_letter" in raw -> "Kelime doğru harfle başlamalı."
                            "word_already_used" in raw -> "Bu kelime daha önce kullanıldı."
                            "not_in_dictionary" in raw -> "Kelime sözlükte bulunamadı."
                            "turn_expired" in raw -> "Süren doldu."
                            else -> "Hamle doğrulanamadı. Tekrar deneyin."
                        }
                        if (refreshedRoom != null) room = refreshedRoom
                        words = refreshedWords
                    }
                }
                busy = false
            }
        },
        onForfeit = { scope.launch { runCatching { backend.forfeit(active.id) }.onSuccess { room = it } } },
        onExit = onLeaveBattle,
        onChat = { showChat = true },
        onWords = { if (meVip) showWords = true else notice = "Çıkan Kelimeler VIP özelliğidir." },
        trivia = triviaQuestion,
        onTrivia = { index -> scope.launch { val r = triviaRound ?: return@launch; busy = true; runCatching { backend.answerTrivia(r.id, index) }.onSuccess { room = it; notice = "Bilgi sorusu yanıtlandı." }.onFailure { notice = "Yanıt gönderilemedi." }; busy = false } }
    )

    if (showChat) {
        BattleChatSheetFixed(chat, me, active.language, meProfile?.allowMatchChat != false, { showChat = false }) { text ->
            scope.launch {
                runCatching { backend.sendChat(active.id, text) }
                    .onSuccess { chat = runCatching { backend.getChat(active.id) }.getOrDefault(chat) }
                    .onFailure { notice = "Mesaj gönderilemedi." }
            }
        }
    }

    if (showWords) AlertDialog(onDismissRequest = { showWords = false }, title = { Text("Çıkan Kelimeler") }, text = { LazyColumn(Modifier.heightIn(max = 420.dp)) { items(words, key = { it.id }) { Text(it.word.uppercase(), Modifier.fillMaxWidth().padding(8.dp)) } } }, confirmButton = { TextButton(onClick = { showWords = false }) { Text("Kapat") } })
}

@Composable
private fun BattleLobbyFixed(profile: V6ProfileDto?, avatar: String?, matching: Boolean, notice: String, onBack: () -> Unit, onRandom: () -> Unit, onCancel: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().background(B8Bg), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Geri") }; Text("SON HARF", Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Black, fontSize = 22.sp); Spacer(Modifier.width(48.dp)) } }
        item { Surface(shape = RoundedCornerShape(18.dp), color = B8White) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { BattleAvatarFixed(avatar, profile?.displayName ?: "Oyuncu", 56); Spacer(Modifier.width(12.dp)); Text(if (matching) "Rakip aranıyor…" else "Düelloya hazırsın", fontWeight = FontWeight.Bold) } } }
        item { if (matching) OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("EŞLEŞMEYİ İPTAL ET") } else Button(onClick = onRandom, modifier = Modifier.fillMaxWidth().height(64.dp), colors = ButtonDefaults.buttonColors(containerColor = B8Blue)) { Text("1v1 HIZLI KARŞILAŞMA", fontWeight = FontWeight.Black) } }
        item { Text(notice, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = B8Muted) }
    }
}

@Composable
private fun BattleArenaFixed(room: GameRoomDto, me: String?, myName: String, myAvatar: String?, oppName: String, oppAvatar: String?, words: List<GameWordDto>, input: String, notice: String, busy: Boolean, vip: Boolean, onInput: (String) -> Unit, onSubmit: () -> Unit, onForfeit: () -> Unit, onExit: () -> Unit, onChat: () -> Unit, onWords: () -> Unit, trivia: TriviaQuestionDto?, onTrivia: (Int) -> Unit) {
    val host = me == room.hostId
    val myScore = if (host) room.hostScore else room.guestScore
    val oppScore = if (host) room.guestScore else room.hostScore
    val myTurn = room.currentPlayerId == me && room.status in listOf("playing", "final", "sudden_death")
    val last = words.lastOrNull()?.word?.uppercase().orEmpty()
    val req = words.lastOrNull()?.normalizedWord?.lastOrNull()?.uppercaseChar()
    var seconds by remember(room.turnDeadline) { mutableIntStateOf(45) }
    LaunchedEffect(room.turnDeadline, room.currentPlayerId, room.status) { while (isActive && room.turnDeadline != null && room.status in listOf("playing", "final", "sudden_death")) { seconds = runCatching { (Instant.parse(room.turnDeadline).epochSecond - Instant.now().epochSecond).toInt().coerceAtLeast(0) }.getOrDefault(45); delay(1000) } }

    Column(Modifier.fillMaxSize().background(Color(0xFFF9F6F0)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Text("🔥 Kelime Düellosu", Modifier.weight(1f), fontWeight = FontWeight.Black, fontSize = 18.sp, color = B8Text); Surface(onClick = onWords, shape = RoundedCornerShape(12.dp), color = if (vip) B8Purple else B8White, border = BorderStroke(1.dp, B8Purple)) { Text(if (vip) "📜 Çıkan Kelimeler" else "🔒 Çıkan Kelimeler", Modifier.padding(9.dp), color = if (vip) Color.White else B8Purple, fontSize = 11.sp, fontWeight = FontWeight.Bold) }; IconButton(onClick = onExit) { Icon(Icons.Rounded.Close, "Çık") } }
        Surface(shape = RoundedCornerShape(14.dp), color = B8White) { Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) { Text("CANLI 1v1 • Klasik Son Harf", Modifier.weight(1f), color = B8Green, fontWeight = FontWeight.Bold); TextButton(onClick = onChat) { Icon(Icons.Rounded.ChatBubble, null); Spacer(Modifier.width(4.dp)); Text("Sohbet") } } }
        Surface(shape = RoundedCornerShape(20.dp), color = B8White, shadowElevation = 3.dp) { Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { BattlePlayerFixed(myName, myAvatar, myScore, myTurn, Modifier.weight(1f)); Box(Modifier.size(64.dp).clip(CircleShape).background(if (seconds <= 5) B8Coral else B8Amber), contentAlignment = Alignment.Center) { Text("$seconds", color = Color.White, fontWeight = FontWeight.Black, fontSize = 25.sp) }; BattlePlayerFixed(oppName, oppAvatar, oppScore, !myTurn, Modifier.weight(1f)) }; Spacer(Modifier.height(8.dp)); Text(when { room.status == "quiz" -> "BİLGİ SORUSU"; room.status == "paused" -> "BAĞLANTI BEKLENİYOR"; myTurn -> "SIRA SİZDE"; else -> "RAKİP BEKLENİYOR…" }, fontWeight = FontWeight.Black, color = if (myTurn) B8Green else B8Muted); Spacer(Modifier.height(7.dp)); Text(if (last.isBlank()) "İlk kelimeyi siz başlatın" else "Son Kelime: $last", color = B8Muted, fontSize = 16.sp) } }
        if (room.status == "quiz") {
            Surface(shape = RoundedCornerShape(16.dp), color = B8White, border = BorderStroke(1.dp, B8Amber)) { Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { Text(trivia?.question ?: "Bilgi sorusu yükleniyor…", fontWeight = FontWeight.Black, color = B8Text); listOf(trivia?.optionA, trivia?.optionB, trivia?.optionC, trivia?.optionD).forEachIndexed { i, o -> if (o != null) OutlinedButton(onClick = { onTrivia(i) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(o) } } } }
        } else {
            Surface(Modifier.fillMaxWidth().heightIn(min = 72.dp), shape = RoundedCornerShape(14.dp), color = B8White, border = BorderStroke(2.dp, B8Green.copy(alpha = .4f))) { Column(Modifier.fillMaxWidth().padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(if (input.isBlank()) if (req == null) "Kelime yazın…" else "'$req' ile başlayan kelime yazın…" else input, fontWeight = FontWeight.Black, fontSize = 22.sp, color = if (input.isBlank()) B8Muted else B8Text); if (notice.isNotBlank()) Text(notice, color = if (notice.contains("kabul edildi")) B8Green else B8Coral, fontSize = 11.sp, textAlign = TextAlign.Center) } }
            Spacer(Modifier.weight(1f))
            BattleKeyboardFixed(myTurn && !busy, myTurn && !busy && input.length >= 2 && (req == null || input.firstOrNull()?.uppercaseChar() == req), { c -> onInput(if (input.isEmpty() && req != null) "$req$c" else input + c) }, { if (input.length > if (req == null) 0 else 1) onInput(input.dropLast(1)) }, onSubmit)
        }
        TextButton(onClick = onForfeit, modifier = Modifier.align(Alignment.CenterHorizontally)) { Icon(Icons.Rounded.Flag, null, tint = B8Coral); Text(" Pes Et", color = B8Coral, fontWeight = FontWeight.Bold) }
    }
}

@Composable private fun BattlePlayerFixed(name: String, url: String?, score: Int, active: Boolean, modifier: Modifier) { Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) { BattleAvatarFixed(url, name, 50); Text(name, maxLines = 1, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = B8Text); Text("$score puan", fontSize = 10.sp, color = if (active) B8Green else B8Muted) } }

@Composable
private fun BattleKeyboardFixed(enabled: Boolean, submitEnabled: Boolean, onKey: (Char) -> Unit, onDelete: () -> Unit, onSubmit: () -> Unit) {
    val r1 = listOf('Q','W','E','R','T','Y','U','I','O','P','Ğ','Ü'); val r2 = listOf('A','S','D','F','G','H','J','K','L','Ş','İ'); val r3 = listOf('Z','X','C','V','B','N','M','Ö','Ç')
    Surface(shape = RoundedCornerShape(16.dp), color = B8White, shadowElevation = 2.dp) { Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { listOf(r1, r2).forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) { row.forEach { c -> BattleKeyFixed(c, Modifier.weight(1f), enabled) { onKey(c) } } } }; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) { Button(onClick = onDelete, enabled = enabled, modifier = Modifier.weight(1.7f).height(46.dp), colors = ButtonDefaults.buttonColors(containerColor = B8Coral), contentPadding = PaddingValues(0.dp)) { Text("⌫ SİL", fontSize = 11.sp) }; r3.forEach { c -> BattleKeyFixed(c, Modifier.weight(1f), enabled) { onKey(c) } }; Button(onClick = onSubmit, enabled = submitEnabled, modifier = Modifier.weight(1.9f).height(46.dp), colors = ButtonDefaults.buttonColors(containerColor = B8Green), contentPadding = PaddingValues(0.dp)) { Text("✓ ONAY", fontSize = 11.sp) } } } }
}
@Composable private fun BattleKeyFixed(c: Char, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) { Surface(onClick = onClick, enabled = enabled, modifier = modifier.height(46.dp), shape = RoundedCornerShape(8.dp), color = Color(0xFFECE7DE), border = BorderStroke(1.dp, Color(0xFFD6CFC4))) { Box(contentAlignment = Alignment.Center) { Text(c.toString(), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = B8Text) } } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BattleChatSheetFixed(messages: List<ChatMessageDto>, me: String?, language: String, enabled: Boolean, onDismiss: () -> Unit, onSend: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    val isEnglish = language.lowercase().startsWith("en")
    val rows = if (isEnglish) listOf(
        listOf("Q","W","E","R","T","Y","U","I","O","P"),
        listOf("A","S","D","F","G","H","J","K","L"),
        listOf("Z","X","C","V","B","N","M")
    ) else listOf(
        listOf("Q","W","E","R","T","Y","U","I","O","P","Ğ","Ü"),
        listOf("A","S","D","F","G","H","J","K","L","Ş","İ"),
        listOf("Z","X","C","V","B","N","M","Ö","Ç")
    )
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = B8White) {
        Column(Modifier.fillMaxWidth().heightIn(min = 470.dp, max = 680.dp).padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text(if (isEnglish) "In-Game Chat" else "Oyun İçi Sohbet", Modifier.weight(1f), fontWeight = FontWeight.Black, fontSize = 22.sp); IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Kapat") } }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) { items(messages, key = { it.id }) { m -> Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.senderId == me) Arrangement.End else Arrangement.Start) { Surface(shape = RoundedCornerShape(12.dp), color = if (m.senderId == me) B8Blue else B8BlueLight) { Text(m.body, Modifier.widthIn(max = 260.dp).padding(10.dp), color = if (m.senderId == me) Color.White else B8Text) } } } }
            Surface(Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(13.dp), color = B8White, border = BorderStroke(1.2.dp, B8Blue.copy(alpha = .55f))) { Text(if (input.isBlank()) if (isEnglish) "Type a message…" else "Mesaj yaz…" else input, Modifier.padding(12.dp), color = if (input.isBlank()) B8Muted else B8Text, maxLines = 3, fontSize = 14.sp) }
            rows.forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) { row.forEach { key -> Button(onClick = { if (enabled && input.length < 300) input += key }, enabled = enabled && input.length < 300, modifier = Modifier.weight(1f).height(40.dp), shape = RoundedCornerShape(7.dp), contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFECE7DE), contentColor = B8Text)) { Text(key, fontSize = 11.sp, fontWeight = FontWeight.Bold) } } } }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Button(onClick = { if (input.isNotEmpty()) input = input.dropLast(1) }, enabled = enabled && input.isNotEmpty(), modifier = Modifier.weight(1f).height(42.dp), colors = ButtonDefaults.buttonColors(containerColor = B8Coral)) { Text(if (isEnglish) "⌫ DEL" else "⌫ SİL", fontSize = 11.sp) }
                Button(onClick = { if (enabled && input.length < 300 && input.isNotEmpty() && !input.endsWith(" ")) input += " " }, enabled = enabled && input.isNotEmpty() && input.length < 300, modifier = Modifier.weight(1.4f).height(42.dp)) { Text(if (isEnglish) "SPACE" else "BOŞLUK", fontSize = 11.sp) }
                Button(onClick = { val t = input.trim(); if (t.isNotBlank()) { onSend(t); input = "" } }, enabled = enabled && input.isNotBlank(), modifier = Modifier.weight(1.4f).height(42.dp), colors = ButtonDefaults.buttonColors(containerColor = B8Green)) { Text(if (isEnglish) "SEND" else "GÖNDER", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable private fun BattleFinishedFixed(room: GameRoomDto, me: String?, onExit: () -> Unit) { val won = room.winnerId == me; Box(Modifier.fillMaxSize().background(B8Bg).padding(24.dp), contentAlignment = Alignment.Center) { Surface(shape = RoundedCornerShape(22.dp), color = B8White) { Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(if (won) "🏆 Kazandınız!" else "Maç Tamamlandı", fontWeight = FontWeight.Black, fontSize = 26.sp, color = B8Text); Spacer(Modifier.height(10.dp)); Text("${room.hostScore} - ${room.guestScore}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = B8Green); Spacer(Modifier.height(18.dp)); Button(onClick = onExit) { Text("Ana Sayfaya Dön") } } } } }

@Composable private fun BattleAvatarFixed(url: String?, name: String, size: Int) { var failed by remember(url) { mutableStateOf(false) }; if (!url.isNullOrBlank() && !failed) { AsyncImage(model = url, contentDescription = "$name profil fotoğrafı", contentScale = ContentScale.Crop, modifier = Modifier.size(size.dp).clip(CircleShape).background(B8BlueLight), onError = { failed = true }) } else Box(Modifier.size(size.dp).clip(CircleShape).background(B8BlueLight), contentAlignment = Alignment.Center) { Text(name.take(1).uppercase(), fontWeight = FontWeight.Black, fontSize = (size / 2.2).sp, color = B8Blue) } }
