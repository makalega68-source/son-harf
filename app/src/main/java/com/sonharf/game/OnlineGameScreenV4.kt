package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import java.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Composable
fun OnlineGameScreenV4() {
    if (!SupabaseProvider.configured) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Sunucu bağlantısı yapılandırılmamış.") }
        return
    }
    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var language by remember { mutableStateOf("tr") }
    var room by remember { mutableStateOf<GameRoomDto?>(null) }
    var words by remember { mutableStateOf<List<GameWordDto>>(emptyList()) }
    var chat by remember { mutableStateOf<List<ChatMessageDto>>(emptyList()) }
    var triviaRound by remember { mutableStateOf<TriviaRoundDto?>(null) }
    var triviaQuestion by remember { mutableStateOf<TriviaQuestionDto?>(null) }
    var wordInput by remember { mutableStateOf("") }
    var chatInput by remember { mutableStateOf("") }
    var privateCode by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("Hazır") }
    var busy by remember { mutableStateOf(false) }
    var matching by remember { mutableStateOf(false) }
    var showFriends by remember { mutableStateOf(false) }
    var showPrivate by remember { mutableStateOf(false) }
    var friends by remember { mutableStateOf<List<Pair<FriendshipDto, ProfileDto>>>(emptyList()) }
    var invites by remember { mutableStateOf<List<GameInviteDto>>(emptyList()) }
    var roomJob by remember { mutableStateOf<Job?>(null) }
    var wordsJob by remember { mutableStateOf<Job?>(null) }
    var chatJob by remember { mutableStateOf<Job?>(null) }
    var matchJob by remember { mutableStateOf<Job?>(null) }

    fun friendly(t: Throwable): String {
        val raw = t.message.orEmpty()
        return when {
            "player_already_in_game" in raw -> "Aktif maçın bulundu. Maça dönülüyor…"
            "not_your_turn" in raw -> "Sıra rakibinde."
            "word_already_used" in raw -> "Bu kelime bu maçta daha önce kullanıldı."
            "wrong_start_letter" in raw -> "Kelime son harfle başlamalı."
            "not_in_dictionary" in raw -> "Bu kelime sözlükte bulunamadı."
            "room_not_available" in raw -> "Bu oda artık kullanılamıyor."
            "answers_locked" in raw -> "Şıklar 3 saniye sonra açılacak."
            else -> "İşlem tamamlanamadı. Tekrar dene."
        }
    }

    suspend fun ensureProfile(): ProfileDto {
        if (backend.currentUserId() == null) backend.ensurePlayer("Oyuncu")
        val id = requireNotNull(backend.currentUserId())
        return runCatching { backend.getProfile(id) }.getOrElse { backend.ensurePlayer("Oyuncu") }.also {
            profile = it
            runCatching { backend.setPresence("online") }
        }
    }

    suspend fun activeRoom(): GameRoomDto? {
        val me = backend.currentUserId() ?: return null
        val all = SupabaseProvider.client.from("game_rooms").select().decodeList<GameRoomDto>()
        return all.filter {
            (it.hostId == me || it.guestId == me) && it.status in listOf("waiting", "playing", "quiz", "final", "sudden_death", "paused")
        }.maxByOrNull { it.validWordCount }
    }

    suspend fun refreshQuiz(r: GameRoomDto) {
        if (r.status == "quiz") {
            triviaRound = backend.getActiveTriviaRound(r.id)
            triviaQuestion = triviaRound?.let { backend.getTriviaQuestion(it.questionId) }
        } else {
            triviaRound = null
            triviaQuestion = null
        }
    }

    fun observe(r: GameRoomDto) {
        roomJob?.cancel(); wordsJob?.cancel(); chatJob?.cancel(); matchJob?.cancel()
        matching = false
        roomJob = scope.launch { backend.observeRoom(r.id).catch { notice = friendly(it) }.collect { room = it; refreshQuiz(it) } }
        wordsJob = scope.launch { backend.observeWords(r.id).catch { notice = friendly(it) }.collect { words = it } }
        if (!r.isBot) chatJob = scope.launch { backend.observeChat(r.id).catch { notice = friendly(it) }.collect { chat = it } }
    }

    suspend fun submitCompat(r: GameRoomDto, word: String): GameRoomDto {
        return runCatching { backend.submitWord(r.id, word) }.getOrElse {
            runCatching {
                SupabaseProvider.client.postgrest.rpc("submit_word_v2", buildJsonObject { put("p_room_id", r.id); put("p_word", word.trim()) }).decodeSingle<GameRoomDto>()
            }.getOrElse {
                SupabaseProvider.client.postgrest.rpc("submit_word", buildJsonObject { put("p_room_id", r.id); put("p_word", word.trim()) }).decodeSingle()
            }
        }
    }

    suspend fun answerCompat(roundId: String, index: Int): GameRoomDto {
        return runCatching { backend.answerTrivia(roundId, index) }.getOrElse {
            runCatching {
                SupabaseProvider.client.postgrest.rpc("answer_trivia_v2", buildJsonObject { put("p_round_id", roundId); put("p_answer_index", index) }).decodeSingle<GameRoomDto>()
            }.getOrElse {
                SupabaseProvider.client.postgrest.rpc("answer_trivia", buildJsonObject { put("p_round_id", roundId); put("p_answer_index", index) }).decodeSingle()
            }
        }
    }

    LaunchedEffect(Unit) {
        busy = true
        runCatching { ensureProfile() }
            .onSuccess { p ->
                val old = runCatching { activeRoom() }.getOrNull()
                if (old != null) { room = old; notice = "${p.displayName}, aktif maçına dönüldü."; observe(old) }
                else notice = "${p.displayName}, düelloya hazırsın."
            }
            .onFailure { notice = friendly(it) }
        busy = false
    }

    DisposableEffect(Unit) {
        onDispose {
            roomJob?.cancel(); wordsJob?.cancel(); chatJob?.cancel(); matchJob?.cancel()
            scope.launch { runCatching { backend.cancelRandomMatchmaking() }; runCatching { backend.setPresence("offline") } }
        }
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF070B14), Color(0xFF0A1020), Color(0xFF05070D))))) {
        val active = room
        if (active == null) {
            DuelLobbyV4(
                playerName = profile?.displayName ?: "Oyuncu",
                language = language,
                onLanguage = { language = it; SonHarfSoundFx.tap() },
                notice = notice,
                busy = busy,
                matching = matching,
                showFriends = showFriends,
                showPrivate = showPrivate,
                privateCode = privateCode,
                onPrivateCode = { privateCode = it.filter(Char::isLetterOrDigit).uppercase().take(6) },
                friends = friends,
                invites = invites,
                onRandom = {
                    scope.launch {
                        busy = true
                        runCatching { ensureProfile(); backend.startRandomMatchmaking(language) }
                            .onSuccess { matching = true; notice = "Gerçek rakip aranıyor…" }
                            .onFailure {
                                if ("player_already_in_game" in it.message.orEmpty()) {
                                    val old = runCatching { activeRoom() }.getOrNull()
                                    if (old != null) { room = old; observe(old); notice = "Aktif maça dönüldü." }
                                    else notice = "Önceki maç kapanıyor. Tekrar dene."
                                } else notice = friendly(it)
                            }
                        busy = false
                        if (matching) {
                            matchJob = launch {
                                while (matching && room == null) {
                                    val found = runCatching { backend.pollRandomMatchmakingRoom() }.getOrNull()
                                    if (found != null) { room = found; observe(found); notice = if (found.isBot) "Bot rakip hazır." else "Rakip bulundu!"; SonHarfSoundFx.softNotify(); break }
                                    delay(1000)
                                }
                            }
                        }
                    }
                },
                onCancel = { scope.launch { matching = false; matchJob?.cancel(); runCatching { backend.cancelRandomMatchmaking() }; notice = "Eşleşme iptal edildi." } },
                onFriends = {
                    scope.launch {
                        busy = true
                        runCatching { ensureProfile(); friends = backend.getFriends(); invites = backend.getIncomingGameInvites() }
                        showFriends = !showFriends; showPrivate = false; busy = false
                    }
                },
                onPrivate = { showPrivate = !showPrivate; showFriends = false },
                onCreatePrivate = { scope.launch { busy = true; runCatching { ensureProfile(); backend.createPrivateRoom(language) }.onSuccess { room = it; observe(it) }.onFailure { notice = friendly(it) }; busy = false } },
                onJoinPrivate = { scope.launch { busy = true; runCatching { ensureProfile(); backend.joinPrivateRoom(privateCode) }.onSuccess { room = it; language = it.language; observe(it) }.onFailure { notice = friendly(it) }; busy = false } },
                onInvite = { id -> scope.launch { runCatching { backend.inviteFriend(id, language) }; notice = "Davet gönderildi." } },
                onInviteResponse = { id, accept -> scope.launch { runCatching { backend.respondGameInvite(id, accept) }.onSuccess { if (it != null) { room = it; observe(it) } } } },
            )
        } else {
            val me = backend.currentUserId()
            val opponent = if (active.isBot) null else if (me == active.hostId) active.guestId else active.hostId
            LaunchedEffect(active.id) {
                while (true) {
                    if (!active.isBot && active.status != "waiting") runCatching { backend.heartbeatRoom(active.id) }.onSuccess { room = it }
                    delay(5000)
                }
            }
            LaunchedEffect(active.id, active.status, active.botTurn, active.validWordCount) {
                if (active.isBot && active.botTurn && active.status in listOf("playing", "final", "sudden_death")) {
                    delay(1800L + (active.validWordCount % 4) * 450L)
                    runCatching { backend.botTakeTurn(active.id) }.onSuccess { room = it }
                }
            }
            DuelArenaV4(
                room = active,
                me = me,
                playerName = profile?.displayName ?: "Sen",
                words = words,
                chat = chat,
                triviaRound = triviaRound,
                triviaQuestion = triviaQuestion,
                wordInput = wordInput,
                onWordInput = { wordInput = it.take(40) },
                chatInput = chatInput,
                onChatInput = { chatInput = it.take(300) },
                notice = notice,
                busy = busy,
                onSubmit = { scope.launch { if (wordInput.isBlank()) return@launch; busy = true; runCatching { submitCompat(active, wordInput) }.onSuccess { room = it; wordInput = ""; notice = "Hamle işlendi."; SonHarfSoundFx.wordAccepted() }.onFailure { notice = friendly(it); SonHarfSoundFx.warning() }; busy = false } },
                onTimeout = { scope.launch { runCatching { backend.claimTurnTimeout(active.id) }.onSuccess { room = it } } },
                onTrivia = { idx -> scope.launch { val q = triviaRound ?: return@launch; runCatching { answerCompat(q.id, idx) }.onSuccess { room = it; refreshQuiz(it) }.onFailure { notice = friendly(it) } } },
                onChat = { scope.launch { runCatching { backend.sendChat(active.id, chatInput) }.onSuccess { chatInput = "" } } },
                onBlock = { if (opponent != null) scope.launch { runCatching { backend.blockUser(opponent) }; notice = "Oyuncu engellendi." } },
                onReport = { if (opponent != null) scope.launch { runCatching { backend.reportUser(opponent, active.id, "Uygunsuz davranış") }; notice = "Rapor kaydedildi." } },
                onPhoto = { if (opponent != null) scope.launch { runCatching { backend.setPhotoAccess(opponent, true) }; notice = "Fotoğraf yalnızca bu oyuncuya açıldı." } },
                onForfeit = { scope.launch { runCatching { backend.forfeit(active.id) }.onSuccess { room = it } } },
                onExit = { roomJob?.cancel(); wordsJob?.cancel(); chatJob?.cancel(); room = null; words = emptyList(); chat = emptyList(); notice = "Yeni düelloya hazırsın." },
                onRematch = { scope.launch { runCatching { if (active.isBot) backend.restartBotMatch(active.id) else backend.requestRematch(active.id) }.onSuccess { room = it; if (it.id != active.id) { words = emptyList(); chat = emptyList(); observe(it) } }.onFailure { notice = friendly(it) } } },
            )
        }
    }
}

@Composable
private fun DuelLobbyV4(
    playerName: String, language: String, onLanguage: (String) -> Unit, notice: String, busy: Boolean, matching: Boolean,
    showFriends: Boolean, showPrivate: Boolean, privateCode: String, onPrivateCode: (String) -> Unit,
    friends: List<Pair<FriendshipDto, ProfileDto>>, invites: List<GameInviteDto>, onRandom: () -> Unit, onCancel: () -> Unit,
    onFriends: () -> Unit, onPrivate: () -> Unit, onCreatePrivate: () -> Unit, onJoinPrivate: () -> Unit,
    onInvite: (String) -> Unit, onInviteResponse: (String, Boolean) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("DÜELLO", fontSize = 34.sp, fontWeight = FontWeight.Black); Text("Hızlı eşleş • oyna • kazan", color = SonHarfMuted) }
                Surface(color = SonHarfSurface2, shape = RoundedCornerShape(999.dp)) { Text(playerName, Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = SonHarfCyan, fontWeight = FontWeight.Bold) }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111A2D)), shape = RoundedCornerShape(28.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = .06f))) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("OYUN DİLİ", color = SonHarfMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(selected = language == "tr", onClick = { onLanguage("tr") }, label = { Text("🇹🇷 TÜRKÇE") })
                        FilterChip(selected = language == "en", onClick = { onLanguage("en") }, label = { Text("🇬🇧 ENGLISH") })
                    }
                    if (!matching) {
                        Button(onClick = onRandom, enabled = !busy, modifier = Modifier.fillMaxWidth().height(68.dp), shape = RoundedCornerShape(22.dp), colors = ButtonDefaults.buttonColors(containerColor = SonHarfPurple)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("RASTGELE RAKİP BUL", fontWeight = FontWeight.Black, fontSize = 17.sp); Text("Önce gerçek oyuncu • gerekirse bot", fontSize = 10.sp) }
                        }
                    } else {
                        Surface(color = SonHarfPurple.copy(alpha = .12f), shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, SonHarfPurple.copy(alpha = .35f))) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp); Spacer(Modifier.width(12.dp)); Column { Text("RAKİP ARANIYOR", fontWeight = FontWeight.Black); Text("10 saniye sonra bot devreye girebilir", color = SonHarfMuted, fontSize = 10.sp) } }
                                TextButton(onClick = onCancel) { Text("İPTAL") }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onFriends, enabled = !busy, modifier = Modifier.weight(1f).height(52.dp), border = BorderStroke(1.dp, SonHarfCyan)) { Text("ARKADAŞ", fontWeight = FontWeight.Bold) }
                        OutlinedButton(onClick = onPrivate, enabled = !busy, modifier = Modifier.weight(1f).height(52.dp), border = BorderStroke(1.dp, SonHarfPurple)) { Text("ÖZEL ODA", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
        item { Surface(color = Color(0xFF0D1526), shape = RoundedCornerShape(16.dp)) { Text(notice, Modifier.fillMaxWidth().padding(12.dp), color = SonHarfMuted, fontSize = 12.sp) } }
        if (showPrivate) item {
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("ÖZEL ODA", color = SonHarfCyan, fontWeight = FontWeight.Black)
                    Button(onClick = onCreatePrivate, modifier = Modifier.fillMaxWidth()) { Text("YENİ ODA OLUŞTUR") }
                    OutlinedTextField(value = privateCode, onValueChange = onPrivateCode, label = { Text("6 haneli kod") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedButton(onClick = onJoinPrivate, enabled = privateCode.length == 6, modifier = Modifier.fillMaxWidth()) { Text("KODLA KATIL") }
                }
            }
        }
        if (showFriends) item {
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (invites.isNotEmpty()) Text("DAVETLER", color = SonHarfGold, fontWeight = FontWeight.Black)
                    invites.forEach { inv -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(if (inv.language == "tr") "Türkçe maç daveti" else "English match invite"); Row { TextButton(onClick = { onInviteResponse(inv.id, true) }) { Text("Kabul") }; TextButton(onClick = { onInviteResponse(inv.id, false) }) { Text("Reddet") } } } }
                    Text("ARKADAŞLAR", color = SonHarfCyan, fontWeight = FontWeight.Black)
                    if (friends.isEmpty()) Text("Henüz çevrimiçi arkadaş yok.", color = SonHarfMuted)
                    friends.forEach { (_, p) -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column { Text(p.displayName, fontWeight = FontWeight.Bold); Text(if (p.presenceStatus == "online") "Çevrimiçi" else if (p.presenceStatus == "in_game") "Oyunda" else "Çevrimdışı", color = SonHarfMuted, fontSize = 11.sp) }; Button(onClick = { onInvite(p.id) }, enabled = p.presenceStatus == "online") { Text("Davet") } } }
                }
            }
        }
    }
}

@Composable
private fun DuelArenaV4(
    room: GameRoomDto, me: String?, playerName: String, words: List<GameWordDto>, chat: List<ChatMessageDto>,
    triviaRound: TriviaRoundDto?, triviaQuestion: TriviaQuestionDto?, wordInput: String, onWordInput: (String) -> Unit,
    chatInput: String, onChatInput: (String) -> Unit, notice: String, busy: Boolean, onSubmit: () -> Unit,
    onTimeout: () -> Unit, onTrivia: (Int) -> Unit, onChat: () -> Unit, onBlock: () -> Unit, onReport: () -> Unit,
    onPhoto: () -> Unit, onForfeit: () -> Unit, onExit: () -> Unit, onRematch: () -> Unit,
) {
    val host = me == room.hostId
    val myScore = if (host) room.hostScore else room.guestScore
    val oppScore = if (host) room.guestScore else room.hostScore
    val myRounds = if (host) room.hostRounds else room.guestRounds
    val oppRounds = if (host) room.guestRounds else room.hostRounds
    val myTurn = room.currentPlayerId == me && room.status in listOf("playing", "final", "sudden_death")
    val last = words.lastOrNull()?.normalizedWord
    val required = last?.lastOrNull()?.uppercaseChar()?.toString() ?: "•"
    val opponent = if (room.isBot) "${room.botName ?: "KelimeBot"}  BOT" else "Rakip"
    var seconds by remember(room.turnDeadline) { mutableStateOf(45) }

    LaunchedEffect(room.turnDeadline, room.currentPlayerId, room.status) {
        while (room.turnDeadline != null && room.status in listOf("playing", "final", "sudden_death")) {
            seconds = runCatching { (Instant.parse(room.turnDeadline).epochSecond - Instant.now().epochSecond).toInt().coerceAtLeast(0) }.getOrDefault(45)
            if (seconds <= 0) { onTimeout(); break }
            delay(1000)
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DuelPlayerCard(playerName, myScore, myRounds, myTurn, Modifier.weight(1f))
                Surface(color = if (seconds <= 10) Color(0xFF5B2332) else Color(0xFF172136), shape = RoundedCornerShape(22.dp), modifier = Modifier.width(76.dp).height(92.dp)) { Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text("$seconds", fontSize = 28.sp, fontWeight = FontWeight.Black); Text("sn", color = SonHarfMuted, fontSize = 10.sp) } }
                DuelPlayerCard(opponent, oppScore, oppRounds, !myTurn, Modifier.weight(1f), right = true)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF101827)), shape = RoundedCornerShape(28.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = .05f))) {
                Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (room.status == "sudden_death") "ANİ ÖLÜM" else "ROUND ${room.roundNo}/3", color = SonHarfCyan, fontWeight = FontWeight.Black)
                        Text("${room.roundWordCount}/10", color = SonHarfMuted, fontWeight = FontWeight.Bold)
                    }
                    Text(if (myTurn) "SIRA SENDE" else if (room.isBot && room.botTurn) "BOT DÜŞÜNÜYOR" else "RAKİBİN SIRASI", fontSize = 15.sp, fontWeight = FontWeight.Black)
                    Surface(color = SonHarfPurple.copy(alpha = .18f), shape = RoundedCornerShape(999.dp), border = BorderStroke(1.dp, SonHarfPurple.copy(alpha = .3f))) {
                        Column(Modifier.padding(horizontal = 26.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("SON HARF", color = SonHarfMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold); Text(required, color = SonHarfCyan, fontSize = 42.sp, fontWeight = FontWeight.Black) }
                    }
                    Text(if (last == null) "İlk kelimeyi sen başlat." else "${last.uppercase()} → $required ile başlayan kelime yaz", color = SonHarfMuted, textAlign = TextAlign.Center, fontSize = 12.sp)
                    OutlinedTextField(value = wordInput, onValueChange = onWordInput, enabled = myTurn && !busy, singleLine = true, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Kelime yaz…") }, shape = RoundedCornerShape(18.dp))
                    Button(onClick = onSubmit, enabled = myTurn && !busy && wordInput.isNotBlank(), modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp)) { Text("GÖNDER", fontWeight = FontWeight.Black) }
                    Text("Doğru +3  •  Hatalı/Süre −1  •  5 kusursuz +3", color = SonHarfMuted, fontSize = 10.sp)
                }
            }
        }
        if (words.isNotEmpty()) item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("KELİME ZİNCİRİ", color = SonHarfMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) { items(words.takeLast(20)) { w -> Surface(color = SonHarfSurface2, shape = RoundedCornerShape(12.dp)) { Text(w.word.uppercase(), Modifier.padding(horizontal = 11.dp, vertical = 7.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold) } } }
            }
        }
        if (room.status == "quiz" && triviaRound != null && triviaQuestion != null) item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF17152C)), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, SonHarfPurple.copy(alpha = .35f))) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("BONUS • +${triviaRound.bonusPoints}", color = SonHarfGold, fontWeight = FontWeight.Black)
                    Text(triviaQuestion.question, fontWeight = FontWeight.Bold)
                    listOf(triviaQuestion.optionA, triviaQuestion.optionB, triviaQuestion.optionC, triviaQuestion.optionD).forEachIndexed { index, option -> OutlinedButton(onClick = { onTrivia(index) }, modifier = Modifier.fillMaxWidth()) { Text(option) } }
                }
            }
        }
        item { Surface(color = Color(0xFF0D1526), shape = RoundedCornerShape(14.dp)) { Text(notice, Modifier.fillMaxWidth().padding(11.dp), color = SonHarfMuted, fontSize = 11.sp) } }
        if (!room.isBot && room.status !in listOf("finished", "waiting")) item {
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("SOHBET", color = SonHarfCyan, fontWeight = FontWeight.Black)
                        Row { TextButton(onClick = onPhoto) { Text("Fotoğraf") }; TextButton(onClick = onReport) { Text("Rapor") }; TextButton(onClick = onBlock) { Text("Engelle") } }
                    }
                    chat.takeLast(4).forEach { m -> Text(if (m.senderId == me) "Sen: ${m.body}" else "Rakip: ${m.body}", color = SonHarfMuted, fontSize = 12.sp) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(value = chatInput, onValueChange = onChatInput, modifier = Modifier.weight(1f), singleLine = true, placeholder = { Text("Mesaj…") }); Button(onClick = onChat, enabled = chatInput.isNotBlank()) { Text("➤") } }
                }
            }
        }
        if (room.status == "finished") item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF161D31)), shape = RoundedCornerShape(26.dp)) {
                Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (room.winnerId == me) "KAZANDIN" else "MAÇ TAMAMLANDI", fontSize = 28.sp, fontWeight = FontWeight.Black, color = if (room.winnerId == me) SonHarfGold else SonHarfText)
                    Text("Round $myRounds - $oppRounds   •   Puan $myScore - $oppScore", color = SonHarfMuted)
                    Button(onClick = onRematch, modifier = Modifier.fillMaxWidth()) { Text("TEKRAR OYNA", fontWeight = FontWeight.Black) }
                    OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) { Text("LOBİYE DÖN") }
                }
            }
        } else if (room.status != "waiting") item { OutlinedButton(onClick = onForfeit, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, Color(0xFFA8455B))) { Text("PES ET", color = Color(0xFFFF8FA5)) } }
        if (room.status == "waiting") item { Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(22.dp)) { Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("ODA KODU", color = SonHarfMuted); Text(room.code, fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp); TextButton(onClick = onExit) { Text("Lobiye dön") } } } }
    }
}

@Composable
private fun DuelPlayerCard(name: String, score: Int, rounds: Int, active: Boolean, modifier: Modifier, right: Boolean = false) {
    Card(modifier = modifier.height(92.dp), colors = CardDefaults.cardColors(containerColor = if (active) SonHarfPurple.copy(alpha = .15f) else SonHarfSurface), shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, if (active) SonHarfPurple.copy(alpha = .35f) else Color.White.copy(alpha = .04f))) {
        Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = if (right) Alignment.End else Alignment.Start, verticalArrangement = Arrangement.SpaceBetween) {
            Text(name, maxLines = 1, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (active) SonHarfCyan else SonHarfMuted)
            Text(score.toString(), fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("$rounds round", color = SonHarfMuted, fontSize = 10.sp)
        }
    }
}
