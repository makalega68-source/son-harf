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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sonharf.game.data.*
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val B11Bg = Color(0xFFF2EFE6)
private val B11Card = Color(0xFFFFFCF4)
private val B11Ink = Color(0xFF263238)
private val B11Muted = Color(0xFF68736D)
private val B11Teal = Color(0xFF1C8C8C)
private val B11Gold = Color(0xFFF1B83B)
private val B11Green = Color(0xFF4E9A62)
private val B11Coral = Color(0xFFD96B57)
private val B11Purple = Color(0xFF8066A8)

@Composable
fun V11BattleScreen(onLeaveBattle: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var meProfile by remember { mutableStateOf<V6ProfileDto?>(null) }
    var meAvatar by remember { mutableStateOf<String?>(null) }
    var opponent by remember { mutableStateOf<V6ProfileDto?>(null) }
    var opponentAvatar by remember { mutableStateOf<String?>(null) }
    var room by remember { mutableStateOf<GameRoomDto?>(null) }
    var words by remember { mutableStateOf<List<GameWordDto>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("Düelloya hazır") }
    var busy by remember { mutableStateOf(false) }
    var matching by remember { mutableStateOf(false) }
    var showChat by remember { mutableStateOf(false) }
    var chat by remember { mutableStateOf<List<ChatMessageDto>>(emptyList()) }
    var triviaRound by remember { mutableStateOf<TriviaRoundDto?>(null) }
    var triviaQuestion by remember { mutableStateOf<TriviaQuestionDto?>(null) }
    var estimate by remember { mutableStateOf("") }
    var estimateSubmittedRound by remember { mutableStateOf<String?>(null) }

    suspend fun loadSelf() {
        val uid = backend.currentUserId() ?: return
        meProfile = runCatching { v6LoadProfile(uid) }.getOrNull()
        meAvatar = runCatching { AvatarSignedUrl.resolve(meProfile?.avatarPath) }.getOrNull()
    }
    suspend fun loadOpponent(r: GameRoomDto) {
        if (r.isBot) { opponent = null; opponentAvatar = null; return }
        val uid = backend.currentUserId()
        val oid = if (r.hostId == uid) r.guestId else r.hostId
        opponent = oid?.let { runCatching { v6LoadProfile(it) }.getOrNull() }
        opponentAvatar = runCatching { AvatarSignedUrl.resolve(opponent?.avatarPath) }.getOrNull()
    }
    suspend fun findActive(): GameRoomDto? {
        val uid = backend.currentUserId() ?: return null
        return SupabaseProvider.client.from("game_rooms").select().decodeList<GameRoomDto>()
            .filter { (it.hostId == uid || it.guestId == uid) && it.status in listOf("waiting","playing","quiz","final","sudden_death","paused","finished") }
            .maxByOrNull { it.validWordCount }
    }

    LaunchedEffect(Unit) {
        loadSelf()
        room = runCatching { findActive() }.getOrNull()
        room?.let { loadOpponent(it) }
    }

    val active = room
    if (active == null) {
        V11Lobby(
            profile = meProfile,
            avatar = meAvatar,
            matching = matching,
            notice = notice,
            onBack = onLeaveBattle,
            onRandom = {
                if (!busy) scope.launch {
                    busy = true
                    runCatching { backend.startRandomMatchmaking(if (SonHarfUiState.language == "en") "en" else "tr") }
                        .onSuccess { matching = true; notice = "Ratingine yakın rakip aranıyor…" }
                        .onFailure { notice = "Eşleşme başlatılamadı." }
                    busy = false
                    while (matching && room == null) {
                        val found = runCatching { backend.pollRandomMatchmakingRoom() }.getOrNull()
                        if (found != null) { room = found; loadOpponent(found); matching = false; break }
                        delay(700)
                    }
                }
            },
            onCancel = { scope.launch { matching = false; runCatching { backend.cancelRandomMatchmaking() } } },
        )
        return
    }

    val me = backend.currentUserId()
    LaunchedEffect(active.id) {
        var tick = 0
        while (isActive) {
            runCatching { backend.getRoom(active.id) }.onSuccess { room = it; loadOpponent(it) }
            runCatching { backend.getWords(active.id) }.onSuccess { words = it }
            if (showChat) runCatching { backend.getChat(active.id) }.onSuccess { chat = it }
            tick++
            if (tick % 7 == 0 && !active.isBot) runCatching { backend.heartbeatRoom(active.id) }
            delay(600)
        }
    }

    LaunchedEffect(active.currentPlayerId, active.validWordCount, active.roundNo) { input = "" }
    LaunchedEffect(active.id, active.botTurn, active.status, active.validWordCount) {
        if (active.isBot && active.botTurn && active.status in listOf("playing","final","sudden_death")) {
            delay(650)
            runCatching { backend.botTakeTurn(active.id) }.onSuccess { room = it }
        }
    }
    LaunchedEffect(active.id, active.status, active.validWordCount) {
        if (active.status == "quiz") {
            triviaRound = runCatching { backend.getActiveTriviaRound(active.id) }.getOrNull()
            triviaQuestion = triviaRound?.let { runCatching { backend.getTriviaQuestion(it.questionId) }.getOrNull() }
        } else {
            triviaRound = null; triviaQuestion = null; estimate = ""; estimateSubmittedRound = null
        }
    }
    LaunchedEffect(triviaRound?.id, triviaRound?.revealAt, active.status) {
        val tr = triviaRound ?: return@LaunchedEffect
        if (active.status != "quiz") return@LaunchedEffect
        val base = runCatching { Instant.parse(tr.revealAt).epochSecond }.getOrNull() ?: return@LaunchedEffect
        while (isActive && room?.status == "quiz") {
            if (Instant.now().epochSecond >= base + 20L) {
                runCatching {
                    SupabaseProvider.client.postgrest.rpc(
                        "claim_estimate_timeout_v1",
                        buildJsonObject { put("p_round_id", tr.id) },
                    ).decodeSingle<GameRoomDto>()
                }.onSuccess { room = it }
                break
            }
            delay(500)
        }
    }

    if (active.status == "finished") {
        V11Finished(active, me, backend, onLeaveBattle) { room = it }
        return
    }

    V11Arena(
        room = active,
        me = me,
        myName = meProfile?.displayName ?: "Sen",
        myAvatar = meAvatar,
        oppName = if (active.isBot) active.botName ?: "KelimeBot" else opponent?.displayName ?: "Rakip",
        oppAvatar = opponentAvatar,
        words = words,
        input = input,
        notice = notice,
        busy = busy,
        trivia = triviaQuestion,
        estimate = estimate,
        estimateSubmitted = triviaRound?.id == estimateSubmittedRound,
        onInput = { input = it.take(40) },
        onSubmit = {
            val submitted = input.trim()
            if (submitted.length < 2 || busy) return@V11Arena
            scope.launch {
                busy = true
                runCatching { backend.submitWord(active.id, submitted) }
                    .onSuccess { result ->
                        room = result
                        input = ""
                        notice = when (result.lastEvent) {
                            "invalid_word", "not_in_dictionary" -> "−1 puan • Geçersiz kelime • sıra rakibe geçti"
                            "wrong_start_letter" -> "−1 puan • Yanlış başlangıç harfi • sıra rakibe geçti"
                            "word_already_used" -> "−1 puan • Bu kelime kullanıldı • sıra rakibe geçti"
                            "turn_expired" -> "−1 puan • Süren doldu • sıra rakibe geçti"
                            else -> "✨ ${submitted.uppercase()} kabul edildi"
                        }
                    }
                    .onFailure { error ->
                        delay(120)
                        runCatching { backend.getRoom(active.id) }.onSuccess { room = it }
                        notice = when {
                            "not_your_turn" in error.message.orEmpty() -> "Sıra rakibinde"
                            else -> "Hamle sunucudan tekrar kontrol edildi"
                        }
                    }
                busy = false
            }
        },
        onForfeit = { scope.launch { runCatching { backend.forfeit(active.id) }.onSuccess { room = it } } },
        onExit = onLeaveBattle,
        onChat = { showChat = true; scope.launch { chat = runCatching { backend.getChat(active.id) }.getOrDefault(emptyList()) } },
        onEstimateChange = { estimate = it.filter(Char::isDigit).take(9) },
        onEstimate = {
            val tr = triviaRound ?: return@V11Arena
            val value = estimate.toIntOrNull() ?: return@V11Arena
            if (busy || estimateSubmittedRound == tr.id) return@V11Arena
            scope.launch {
                busy = true
                estimateSubmittedRound = tr.id
                runCatching { backend.answerTrivia(tr.id, value) }
                    .onSuccess { result ->
                        room = result
                        notice = when (result.lastEvent) {
                            "estimate_waiting" -> "🌍 Tahminin kaydedildi • rakip bekleniyor"
                            "estimate_exact" -> "🎯 Tam isabet! +5 bonus puan"
                            "estimate_won" -> "🌍 En yakın tahmin! +3 bonus puan"
                            "estimate_bot_won" -> "Botun tahmini daha yakındı"
                            "estimate_tie" -> "Tahminler eşit uzaklıkta"
                            else -> "Tahmin sonucu işlendi"
                        }
                    }
                    .onFailure { estimateSubmittedRound = null; notice = "Tahmin gönderilemedi" }
                busy = false
            }
        },
    )

    if (showChat) {
        V11Chat(
            messages = chat,
            me = me,
            enabled = meProfile?.allowMatchChat != false,
            onDismiss = { showChat = false },
            onSend = { text -> scope.launch { runCatching { backend.sendChat(active.id, text) }.onSuccess { chat = runCatching { backend.getChat(active.id) }.getOrDefault(chat) } } },
        )
    }
}

@Composable
private fun V11Lobby(profile: V6ProfileDto?, avatar: String?, matching: Boolean, notice: String, onBack: () -> Unit, onRandom: () -> Unit, onCancel: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().background(B11Bg), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Geri") }; Text("SON HARF", Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Black, fontSize = 24.sp, color = B11Teal); Spacer(Modifier.width(48.dp)) } }
        item { Surface(shape = RoundedCornerShape(20.dp), color = B11Card, border = BorderStroke(1.dp, B11Gold.copy(alpha=.45f))) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { V11Avatar(avatar, profile?.displayName ?: "Oyuncu", 58); Spacer(Modifier.width(12.dp)); Column { Text(if (matching) "Rakip aranıyor…" else "Düelloya hazırsın", fontWeight = FontWeight.Black, color = B11Ink); Text("Ratinge yakın akıllı eşleştirme", fontSize = 11.sp, color = B11Muted) } } } }
        item { if (matching) OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("EŞLEŞMEYİ İPTAL ET") } else Button(onClick = onRandom, modifier = Modifier.fillMaxWidth().height(64.dp), colors = ButtonDefaults.buttonColors(containerColor = B11Teal)) { Text("1v1 HIZLI KARŞILAŞMA", fontWeight = FontWeight.Black) } }
        item { Text(notice, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = B11Muted) }
    }
}

@Composable
private fun V11Arena(
    room: GameRoomDto, me: String?, myName: String, myAvatar: String?, oppName: String, oppAvatar: String?, words: List<GameWordDto>,
    input: String, notice: String, busy: Boolean, trivia: TriviaQuestionDto?, estimate: String, estimateSubmitted: Boolean,
    onInput: (String) -> Unit, onSubmit: () -> Unit, onForfeit: () -> Unit, onExit: () -> Unit, onChat: () -> Unit,
    onEstimateChange: (String) -> Unit, onEstimate: () -> Unit,
) {
    val host = me == room.hostId
    val myScore = if (host) room.hostScore else room.guestScore
    val oppScore = if (host) room.guestScore else room.hostScore
    val myTurn = room.currentPlayerId == me && room.status in listOf("playing","final","sudden_death")
    val last = words.lastOrNull()?.word?.uppercase().orEmpty()
    val req = words.lastOrNull()?.normalizedWord?.lastOrNull()?.uppercaseChar()
    var seconds by remember(room.turnDeadline) { mutableIntStateOf(45) }
    LaunchedEffect(room.turnDeadline, room.currentPlayerId, room.status) {
        while (isActive && room.turnDeadline != null && room.status in listOf("playing","final","sudden_death")) {
            seconds = runCatching { (Instant.parse(room.turnDeadline).epochSecond - Instant.now().epochSecond).toInt().coerceAtLeast(0) }.getOrDefault(45)
            delay(1000)
        }
    }

    Column(Modifier.fillMaxSize().background(B11Bg).padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Text("SON HARF", Modifier.weight(1f), fontWeight = FontWeight.Black, fontSize = 19.sp, color = B11Teal); IconButton(onClick = onExit) { Icon(Icons.Rounded.Close, "Çık") } }
        Surface(shape = RoundedCornerShape(14.dp), color = B11Teal) { Row(Modifier.fillMaxWidth().padding(9.dp), verticalAlignment = Alignment.CenterVertically) { Text("CANLI 1v1 • SON HARF", Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Black); TextButton(onClick = onChat) { Text("💬 Sohbet", color = Color.White) } } }
        Surface(shape = RoundedCornerShape(20.dp), color = B11Card, border = BorderStroke(1.dp, B11Gold.copy(alpha=.45f))) {
            Column(Modifier.fillMaxWidth().padding(13.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    V11Player(myName, myAvatar, myScore, Modifier.weight(1f))
                    Box(Modifier.size(66.dp).clip(CircleShape).background(if (seconds <= 5) B11Coral else B11Gold), contentAlignment = Alignment.Center) { Text(if (room.status == "quiz") "🌍" else "$seconds", color = Color.White, fontWeight = FontWeight.Black, fontSize = if (room.status == "quiz") 27.sp else 25.sp) }
                    V11Player(oppName, oppAvatar, oppScore, Modifier.weight(1f))
                }
                if (room.status != "quiz") {
                    Text(if (myTurn) "⚡ SIRA SİZDE" else "RAKİP DÜŞÜNÜYOR…", fontWeight = FontWeight.Black, color = if (myTurn) B11Green else B11Muted, fontSize = 16.sp)
                    Text("SON KELİME", color = B11Muted, fontSize = 11.sp)
                    Text(if (last.isBlank()) "İLK KELİME" else last, color = Color(0xFF243B64), fontSize = 30.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        if (notice.isNotBlank()) Surface(shape = RoundedCornerShape(12.dp), color = if ("−1" in notice) Color(0xFFFBE8E2) else Color(0xFFE2F2E5)) { Text(notice, Modifier.fillMaxWidth().padding(9.dp), textAlign = TextAlign.Center, color = if ("−1" in notice) B11Coral else B11Green, fontWeight = FontWeight.Bold, fontSize = 11.sp) }

        if (room.status == "quiz") {
            Surface(shape = RoundedCornerShape(18.dp), color = B11Card, border = BorderStroke(2.dp, B11Gold)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🌍 DÜNYADAN TAHMİN", color = B11Purple, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text(trivia?.question ?: "Soru yükleniyor…", color = B11Ink, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 17.sp)
                    Text("En yakın tahmin +3 puan • Tam doğru +5 puan", color = B11Muted, textAlign = TextAlign.Center, fontSize = 11.sp)
                    OutlinedTextField(value = estimate, onValueChange = onEstimateChange, enabled = !estimateSubmitted && !busy, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Sayısal tahminin") }, placeholder = { Text("Örn. 8849") })
                    Button(onClick = onEstimate, enabled = estimate.isNotBlank() && !estimateSubmitted && !busy, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = B11Teal)) { Text(if (estimateSubmitted) "TAHMİN GÖNDERİLDİ" else "TAHMİNİ GÖNDER", fontWeight = FontWeight.Black) }
                    if (estimateSubmitted) Text("Rakibin tahmini bekleniyor…", color = B11Muted)
                }
            }
        } else {
            Surface(Modifier.fillMaxWidth().heightIn(min = 76.dp), shape = RoundedCornerShape(14.dp), color = B11Card, border = BorderStroke(2.dp, B11Teal.copy(alpha=.45f))) {
                Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.Center) { Text(if (input.isBlank()) if (req == null) "Kelime yazın…" else "$req ile başlayan kelime yazın…" else input, fontWeight = FontWeight.Black, fontSize = if (input.isBlank()) 18.sp else 28.sp, textAlign = TextAlign.Center, color = if (input.isBlank()) B11Muted else B11Teal) }
            }
            Spacer(Modifier.weight(1f))
            V11Keyboard(enabled = myTurn && !busy, submitEnabled = myTurn && !busy && input.length >= 2, onKey = { onInput(input + it) }, onDelete = { if (input.isNotEmpty()) onInput(input.dropLast(1)) }, onSubmit = onSubmit)
        }
        TextButton(onClick = onForfeit, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("🚩 Pes Et", color = B11Coral, fontWeight = FontWeight.Bold) }
    }
}

@Composable private fun V11Keyboard(enabled: Boolean, submitEnabled: Boolean, onKey: (Char) -> Unit, onDelete: () -> Unit, onSubmit: () -> Unit) {
    val rows = listOf(listOf('Q','W','E','R','T','Y','U','I','O','P','Ğ','Ü'), listOf('A','S','D','F','G','H','J','K','L','Ş','İ'), listOf('Z','X','C','V','B','N','M','Ö','Ç'))
    Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFDCE7E1)) { Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        rows.take(2).forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) { row.forEach { c -> V11Key(c, Modifier.weight(1f), enabled) { onKey(c) } } } }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Button(onClick = onDelete, enabled = enabled, modifier = Modifier.weight(1.7f).height(46.dp), colors = ButtonDefaults.buttonColors(containerColor = B11Coral), contentPadding = PaddingValues(0.dp)) { Text("⌫ SİL", fontSize = 10.sp) }
            rows[2].forEach { c -> V11Key(c, Modifier.weight(1f), enabled) { onKey(c) } }
            Button(onClick = onSubmit, enabled = submitEnabled, modifier = Modifier.weight(1.9f).height(46.dp), colors = ButtonDefaults.buttonColors(containerColor = B11Teal), contentPadding = PaddingValues(0.dp)) { Text("✓ ONAY", fontSize = 10.sp, fontWeight = FontWeight.Black) }
        }
    } }
}
@Composable private fun V11Key(c: Char, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) { Surface(onClick = onClick, enabled = enabled, modifier = modifier.height(46.dp), shape = RoundedCornerShape(8.dp), color = Color(0xFFFFF5CE), border = BorderStroke(1.dp, Color(0xFFE5C967))) { Box(contentAlignment = Alignment.Center) { Text(c.toString(), fontWeight = FontWeight.Black, fontSize = 14.sp, color = B11Ink) } } }
@Composable private fun V11Player(name: String, url: String?, score: Int, modifier: Modifier) { Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) { V11Avatar(url, name, 50); Text(name, maxLines = 1, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = B11Ink); Text("$score puan", fontSize = 10.sp, color = B11Muted) } }
@Composable private fun V11Avatar(url: String?, name: String, size: Int) { var failed by remember(url) { mutableStateOf(false) }; if (!url.isNullOrBlank() && !failed) AsyncImage(model = url, contentDescription = "$name profil fotoğrafı", contentScale = ContentScale.Crop, modifier = Modifier.size(size.dp).clip(CircleShape), onError = { failed = true }) else Box(Modifier.size(size.dp).clip(CircleShape).background(Color(0xFFD9EEE8)), contentAlignment = Alignment.Center) { Text(name.take(1).uppercase(), fontWeight = FontWeight.Black, fontSize = (size/2.2).sp, color = B11Teal) } }

@Composable private fun V11Chat(messages: List<ChatMessageDto>, me: String?, enabled: Boolean, onDismiss: () -> Unit, onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val emojis = listOf("😀","😂","😍","😎","🤔","👏","🔥","🎉","👍","💪","🏆","🤝","😅","😮","😢","❤️","🥳","🙏","💯","🌍")
    Surface(Modifier.fillMaxSize(), color = B11Card) { Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().background(B11Teal).padding(8.dp), verticalAlignment = Alignment.CenterVertically) { Text("SON HARF SOHBET", Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Black, textAlign = TextAlign.Center); IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Kapat", tint = Color.White) } }
        LazyColumn(Modifier.weight(1f).padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { items(messages, key = { it.id }) { m -> Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.senderId == me) Arrangement.End else Arrangement.Start) { Surface(shape = RoundedCornerShape(16.dp), color = if (m.senderId == me) B11Teal else Color(0xFFE8E3D8)) { Text(m.body, Modifier.widthIn(max=300.dp).padding(10.dp), color = if (m.senderId == me) Color.White else B11Ink) } } } }
        LazyRow(Modifier.fillMaxWidth().background(Color(0xFFFFF8E4)), contentPadding = PaddingValues(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) { items(emojis) { e -> TextButton(onClick = { if (text.length + e.length <= 300) text += e }) { Text(e, fontSize = 22.sp) } } }
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(text, { text = it.take(300) }, Modifier.weight(1f), enabled = enabled, placeholder = { Text("Mesaj yaz…") }); Spacer(Modifier.width(6.dp)); Button(onClick = { val t=text.trim(); if(t.isNotEmpty()){ onSend(t); text="" } }, enabled = enabled && text.isNotBlank()) { Text("GÖNDER") } }
    } }
}

@Composable private fun V11Finished(room: GameRoomDto, me: String?, backend: OnlineGameBackend, onExit: () -> Unit, onRoom: (GameRoomDto) -> Unit) {
    val scope = rememberCoroutineScope(); val won = room.winnerId == me; var busy by remember { mutableStateOf(false) }; var note by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize().background(B11Bg).padding(20.dp), contentAlignment = Alignment.Center) { Surface(shape = RoundedCornerShape(24.dp), color = B11Card, border = BorderStroke(2.dp, if (won) B11Gold else B11Teal)) { Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (won) "🏆 ZAFER!" else "DÜELLO TAMAMLANDI", fontWeight = FontWeight.Black, fontSize = 27.sp, color = B11Ink); Text("${room.hostScore} - ${room.guestScore}", fontSize = 26.sp, fontWeight = FontWeight.Black)
        Button(onClick = { if(!busy) scope.launch { busy=true; val next=if(room.isBot) runCatching{backend.restartBotMatch(room.id)}.getOrNull() else runCatching{backend.requestRematch(room.id)}.getOrNull(); if(next!=null) onRoom(next) else note="Rövanş başlatılamadı"; busy=false } }, enabled=!busy, modifier=Modifier.fillMaxWidth().height(52.dp), colors=ButtonDefaults.buttonColors(containerColor=B11Teal)) { Text("↻ TEKRAR OYNA", fontWeight=FontWeight.Black) }
        OutlinedButton(onClick=onExit, modifier=Modifier.fillMaxWidth()) { Text("ANA SAYFAYA DÖN") }; if(note.isNotBlank()) Text(note,color=B11Coral)
    } } }
}
