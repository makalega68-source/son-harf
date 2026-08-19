package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import java.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@Composable
fun OnlineGameScreenV2() {
    if (!SupabaseProvider.configured) { MissingBackendV2(); return }
    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("tr") }
    var room by remember { mutableStateOf<GameRoomDto?>(null) }
    var words by remember { mutableStateOf<List<GameWordDto>>(emptyList()) }
    var chat by remember { mutableStateOf<List<ChatMessageDto>>(emptyList()) }
    var triviaRound by remember { mutableStateOf<TriviaRoundDto?>(null) }
    var triviaQuestion by remember { mutableStateOf<TriviaQuestionDto?>(null) }
    var wordInput by remember { mutableStateOf("") }
    var chatInput by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Dilini seç ve rakibini bul.") }
    var busy by remember { mutableStateOf(false) }
    var matching by remember { mutableStateOf(false) }
    var showFriends by remember { mutableStateOf(false) }
    var friends by remember { mutableStateOf<List<Pair<FriendshipDto, ProfileDto>>>(emptyList()) }
    var invites by remember { mutableStateOf<List<GameInviteDto>>(emptyList()) }
    var requests by remember { mutableStateOf<List<Pair<FriendshipDto, ProfileDto>>>(emptyList()) }
    var rematchRequested by remember { mutableStateOf(false) }
    var roomJob by remember { mutableStateOf<Job?>(null) }
    var wordsJob by remember { mutableStateOf<Job?>(null) }
    var chatJob by remember { mutableStateOf<Job?>(null) }
    var matchJob by remember { mutableStateOf<Job?>(null) }
    var rematchJob by remember { mutableStateOf<Job?>(null) }

    fun friendly(t: Throwable): String {
        val raw = t.message.orEmpty()
        return when {
            "not_your_turn" in raw -> "Sıra rakibinde."
            "answers_locked" in raw -> "Şıklar 3 saniye sonra açılır."
            "blocked_relationship" in raw -> "Bu oyuncuyla etkileşim kapalı."
            "not_bot_match" in raw -> "Bot maçı hazır değil."
            else -> raw.substringBefore("URL:").trim().ifBlank { "Bağlantı hatası." }
        }
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
        roomJob?.cancel(); wordsJob?.cancel(); chatJob?.cancel(); matchJob?.cancel(); rematchJob?.cancel()
        matching = false
        rematchRequested = false
        roomJob = scope.launch {
            backend.observeRoom(r.id).catch { status = friendly(it) }.collect {
                room = it
                refreshQuiz(it)
            }
        }
        wordsJob = scope.launch { backend.observeWords(r.id).catch { status = friendly(it) }.collect { words = it } }
        if (!r.isBot) chatJob = scope.launch { backend.observeChat(r.id).catch { status = friendly(it) }.collect { chat = it } }
    }

    suspend fun ensure() {
        backend.ensurePlayer(name)
        backend.setPresence("online")
    }

    suspend fun refreshSocial() {
        friends = backend.getFriends()
        invites = backend.getIncomingGameInvites()
        requests = backend.getIncomingFriendRequests()
    }

    DisposableEffect(Unit) {
        onDispose {
            roomJob?.cancel(); wordsJob?.cancel(); chatJob?.cancel(); matchJob?.cancel(); rematchJob?.cancel()
            scope.launch {
                runCatching { backend.cancelRandomMatchmaking() }
                runCatching { backend.setPresence("offline") }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0B1020), SonHarfBg, Color(0xFF05070D))))) {
        val active = room
        if (active == null) {
            LobbyV2(
                name = name,
                onNameChange = { name = it.take(24) },
                language = language,
                onLanguageChange = { language = it },
                status = status,
                busy = busy,
                matching = matching,
                showFriends = showFriends,
                friends = friends,
                invites = invites,
                requests = requests,
                onRandom = {
                    SonHarfSoundFx.tap()
                    scope.launch {
                        busy = true
                        runCatching { ensure(); backend.startRandomMatchmaking(language) }
                            .onSuccess { matching = true; status = "Gerçek rakip aranıyor… 10 sn sonra bot devreye girebilir." }
                            .onFailure { SonHarfSoundFx.warning(); status = friendly(it) }
                        busy = false
                        if (matching) {
                            matchJob?.cancel()
                            matchJob = launch {
                                while (matching && room == null) {
                                    val found = runCatching { backend.pollRandomMatchmakingRoom() }.getOrNull()
                                    if (found != null) {
                                        room = found
                                        SonHarfSoundFx.softNotify()
                                        status = if (found.isBot) "🤖 ${found.botName ?: "Bot"} hazır. Bot maçı başlıyor." else "Rakip bulundu!"
                                        observe(found)
                                        break
                                    }
                                    delay(1200)
                                }
                            }
                        }
                    }
                },
                onCancel = {
                    SonHarfSoundFx.tap()
                    scope.launch {
                        matching = false
                        matchJob?.cancel()
                        runCatching { backend.cancelRandomMatchmaking() }
                        status = "Eşleşme iptal edildi."
                    }
                },
                onFriends = {
                    SonHarfSoundFx.tap()
                    scope.launch {
                        busy = true
                        runCatching { ensure(); refreshSocial() }
                            .onSuccess { showFriends = !showFriends }
                            .onFailure { SonHarfSoundFx.warning(); status = friendly(it) }
                        busy = false
                    }
                },
                onInvite = { id -> scope.launch { SonHarfSoundFx.tap(); runCatching { backend.inviteFriend(id, language); refreshSocial() }.onSuccess { SonHarfSoundFx.softNotify(); status = "Oyun daveti gönderildi." }.onFailure { SonHarfSoundFx.warning(); status = friendly(it) } } },
                onAcceptRequest = { id -> scope.launch { SonHarfSoundFx.tap(); runCatching { backend.respondFriendRequest(id, true); refreshSocial() }.onFailure { SonHarfSoundFx.warning(); status = friendly(it) } } },
                onInviteResponse = { id, accept ->
                    scope.launch {
                        SonHarfSoundFx.tap()
                        runCatching { backend.respondGameInvite(id, accept) }
                            .onSuccess { joined -> if (joined != null) { SonHarfSoundFx.softNotify(); room = joined; observe(joined) }; refreshSocial() }
                            .onFailure { SonHarfSoundFx.warning(); status = friendly(it) }
                    }
                }
            )
        } else {
            val me = backend.currentUserId()
            val opponent = if (active.isBot) null else if (me == active.hostId) active.guestId else active.hostId
            var lastSoundSignature by remember(active.id) { mutableStateOf<String?>(null) }

            LaunchedEffect(active.id, active.status, active.lastEvent, active.hostScore, active.guestScore, active.winnerId) {
                val signature = "${active.status}|${active.lastEvent}|${active.hostScore}|${active.guestScore}|${active.winnerId}"
                if (lastSoundSignature != signature) {
                    when {
                        active.status == "finished" -> {
                            val host = me == active.hostId
                            val myScore = if (host) active.hostScore else active.guestScore
                            val oppScore = if (host) active.guestScore else active.hostScore
                            val won = active.winnerId == me || (active.isBot && active.winnerId == null && myScore > oppScore)
                            if (won) SonHarfSoundFx.victory() else SonHarfSoundFx.defeat()
                        }
                        active.lastEvent in listOf("streak_bonus", "bot_streak_bonus", "quiz_started") -> SonHarfSoundFx.bonus()
                        active.lastEvent in listOf("invalid_word", "not_in_dictionary", "wrong_start_letter", "word_already_used", "turn_expired", "bot_failed", "opponent_disconnected") -> SonHarfSoundFx.warning()
                        active.lastEvent in listOf("valid_word", "bot_valid_word", "player_reconnected") -> SonHarfSoundFx.wordAccepted()
                        active.lastEvent in listOf("bot_quiz_won", "quiz_no_winner", "disconnect_forfeit", "sudden_death_started") -> SonHarfSoundFx.softNotify()
                    }
                    lastSoundSignature = signature
                }
            }

            LaunchedEffect(active.id) {
                while (true) {
                    if (!active.isBot) {
                        runCatching { backend.heartbeatRoom(active.id) }.onSuccess { updated -> room = updated; refreshQuiz(updated) }
                    }
                    delay(5000)
                }
            }

            LaunchedEffect(active.id, active.status, active.botTurn, active.validWordCount) {
                if (active.isBot && active.botTurn && active.status in listOf("playing", "final", "sudden_death")) {
                    delay(1800L + (active.validWordCount % 4) * 450L)
                    runCatching { backend.botTakeTurn(active.id) }
                        .onSuccess { room = it; status = gameEvent(it, me) }
                        .onFailure { SonHarfSoundFx.warning(); status = friendly(it) }
                }
            }

            LaunchedEffect(active.id, active.status, triviaRound?.id, triviaRound?.botAttempted) {
                val tr = triviaRound
                if (active.isBot && active.status == "quiz" && tr != null && !tr.botAttempted) {
                    val reveal = runCatching { Instant.parse(tr.revealAt) }.getOrNull()
                    val wait = ((reveal?.toEpochMilli() ?: 0L) - System.currentTimeMillis()).coerceAtLeast(0L) + 1600L
                    delay(wait)
                    runCatching { backend.botAnswerTrivia(active.id) }
                        .onSuccess { room = it; refreshQuiz(it); status = gameEvent(it, me) }
                        .onFailure { SonHarfSoundFx.warning(); status = friendly(it) }
                }
            }

            GameV2(
                room = active,
                me = me,
                words = words,
                chat = chat,
                triviaRound = triviaRound,
                triviaQuestion = triviaQuestion,
                wordInput = wordInput,
                onWordInputChange = { wordInput = it.take(40) },
                chatInput = chatInput,
                onChatInputChange = { chatInput = it.take(300) },
                status = status,
                busy = busy,
                rematchRequested = rematchRequested,
                onSubmitWord = {
                    SonHarfSoundFx.tap()
                    scope.launch {
                        busy = true
                        runCatching { backend.submitWord(active.id, wordInput) }
                            .onSuccess { room = it; wordInput = ""; status = gameEvent(it, me) }
                            .onFailure { SonHarfSoundFx.warning(); status = friendly(it) }
                        busy = false
                    }
                },
                onTimeout = { scope.launch { runCatching { backend.claimTurnTimeout(active.id) }.onSuccess { room = it; status = gameEvent(it, me) }.onFailure { SonHarfSoundFx.warning() } } },
                onTrivia = { idx -> scope.launch { SonHarfSoundFx.tap(); val q = triviaRound ?: return@launch; runCatching { backend.answerTrivia(q.id, idx) }.onSuccess { room = it; refreshQuiz(it); status = gameEvent(it, me) }.onFailure { SonHarfSoundFx.warning(); status = friendly(it) } } },
                onSendChat = { scope.launch { SonHarfSoundFx.tap(); runCatching { backend.sendChat(active.id, chatInput) }.onSuccess { chatInput = "" }.onFailure { SonHarfSoundFx.warning(); status = friendly(it) } } },
                onBlock = { if (opponent != null) scope.launch { SonHarfSoundFx.tap(); runCatching { backend.blockUser(opponent) }; status = "Oyuncu engellendi." } },
                onReport = { if (opponent != null) scope.launch { SonHarfSoundFx.tap(); runCatching { backend.reportUser(opponent, active.id, "Uygunsuz sohbet") }; status = "Rapor kaydedildi." } },
                onPhoto = { if (opponent != null) scope.launch { SonHarfSoundFx.tap(); runCatching { backend.setPhotoAccess(opponent, true) }; status = "Fotoğraf bu oyuncuya açıldı." } },
                onFriend = { if (opponent != null) scope.launch { SonHarfSoundFx.tap(); runCatching { backend.sendFriendRequest(opponent) }; status = "Arkadaşlık isteği gönderildi." } },
                onForfeit = { scope.launch { SonHarfSoundFx.warning(); runCatching { backend.forfeit(active.id) }.onSuccess { room = it } } },
                onExit = {
                    SonHarfSoundFx.tap()
                    roomJob?.cancel(); wordsJob?.cancel(); chatJob?.cancel(); rematchJob?.cancel()
                    room = null; words = emptyList(); chat = emptyList(); status = "Yeni rakibini seç."
                    scope.launch { runCatching { backend.setPresence("online") } }
                },
                onRematch = {
                    if (!rematchRequested) {
                        SonHarfSoundFx.softNotify()
                        rematchRequested = true
                        if (active.isBot) {
                            scope.launch {
                                runCatching { backend.restartBotMatch(active.id) }
                                    .onSuccess { next -> room = next; words = emptyList(); chat = emptyList(); status = "🤖 Rövanş başlıyor!"; observe(next) }
                                    .onFailure { SonHarfSoundFx.warning(); rematchRequested = false; status = friendly(it) }
                            }
                        } else {
                            status = "Rövanş isteği gönderildi. Rakip bekleniyor…"
                            rematchJob?.cancel()
                            rematchJob = scope.launch {
                                while (rematchRequested && room?.id == active.id) {
                                    val next = runCatching { backend.requestRematch(active.id) }.getOrNull()
                                    if (next != null && next.id != active.id) {
                                        SonHarfSoundFx.softNotify()
                                        room = next; words = emptyList(); chat = emptyList(); status = "Rövanş başlıyor!"; observe(next); break
                                    }
                                    delay(1500)
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun LobbyV2(
    name: String,
    onNameChange: (String) -> Unit,
    language: String,
    onLanguageChange: (String) -> Unit,
    status: String,
    busy: Boolean,
    matching: Boolean,
    showFriends: Boolean,
    friends: List<Pair<FriendshipDto, ProfileDto>>,
    invites: List<GameInviteDto>,
    requests: List<Pair<FriendshipDto, ProfileDto>>,
    onRandom: () -> Unit,
    onCancel: () -> Unit,
    onFriends: () -> Unit,
    onInvite: (String) -> Unit,
    onAcceptRequest: (String) -> Unit,
    onInviteResponse: (String, Boolean) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("ONLINE DÜELLO", fontSize = 30.sp, fontWeight = FontWeight.Black); Text("3 round • her round 10 geçerli kelime", color = SonHarfMuted) }
        item { Surface(color = SonHarfPurple.copy(alpha = .12f), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, SonHarfPurple.copy(alpha = .35f))) { Text(status, Modifier.fillMaxWidth().padding(14.dp)) } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { FilterChip(selected = language == "tr", onClick = { SonHarfSoundFx.tap(); onLanguageChange("tr") }, label = { Text("🇹🇷 Türkçe") }); FilterChip(selected = language == "en", onClick = { SonHarfSoundFx.tap(); onLanguageChange("en") }, label = { Text("🇬🇧 English") }) } }
        item { DarkFieldV2(name, onNameChange, "Oyuncu adı") }
        item {
            if (!matching) {
                Button(onClick = onRandom, enabled = !busy && name.trim().length >= 2, modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(20.dp)) { Text("⚡ RASTGELE RAKİP BUL", fontWeight = FontWeight.Black) }
            } else {
                Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(); Spacer(Modifier.height(8.dp)); Text("RAKİP ARANIYOR…", fontWeight = FontWeight.Black)
                        Text("Önce gerçek oyuncu • 10 sn sonra bot desteği", color = SonHarfMuted, fontSize = 11.sp)
                        TextButton(onClick = onCancel) { Text("İPTAL ET") }
                    }
                }
            }
        }
        item { OutlinedButton(onClick = onFriends, enabled = !busy && name.trim().length >= 2, modifier = Modifier.fillMaxWidth().height(54.dp), border = BorderStroke(1.dp, SonHarfCyan)) { Text("👥 ARKADAŞLARINLA OYNA", fontWeight = FontWeight.Black) } }
        if (showFriends) item {
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (invites.isNotEmpty()) Text("OYUN DAVETLERİ", color = SonHarfGold, fontWeight = FontWeight.Black)
                    invites.forEach { inv -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("${if (inv.language == "tr") "TR" else "EN"} daveti"); Row { TextButton(onClick = { onInviteResponse(inv.id, true) }) { Text("Kabul") }; TextButton(onClick = { onInviteResponse(inv.id, false) }) { Text("Reddet") } } } }
                    if (requests.isNotEmpty()) Text("ARKADAŞLIK İSTEKLERİ", color = SonHarfGold, fontWeight = FontWeight.Black)
                    requests.forEach { (_, p) -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(p.displayName); TextButton(onClick = { onAcceptRequest(p.id) }) { Text("Kabul") } } }
                    Text("ARKADAŞLAR", color = SonHarfCyan, fontWeight = FontWeight.Black)
                    if (friends.isEmpty()) Text("Henüz arkadaş yok.", color = SonHarfMuted)
                    friends.forEach { (_, p) -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column { Text(p.displayName, fontWeight = FontWeight.Bold); Text(when (p.presenceStatus) { "online" -> "Çevrimiçi"; "in_game" -> "Oyunda"; else -> "Çevrimdışı" }, color = SonHarfMuted, fontSize = 11.sp) }; Button(onClick = { onInvite(p.id) }, enabled = p.presenceStatus == "online") { Text("Davet") } } }
                }
            }
        }
    }
}

@Composable
private fun GameV2(
    room: GameRoomDto,
    me: String?,
    words: List<GameWordDto>,
    chat: List<ChatMessageDto>,
    triviaRound: TriviaRoundDto?,
    triviaQuestion: TriviaQuestionDto?,
    wordInput: String,
    onWordInputChange: (String) -> Unit,
    chatInput: String,
    onChatInputChange: (String) -> Unit,
    status: String,
    busy: Boolean,
    rematchRequested: Boolean,
    onSubmitWord: () -> Unit,
    onTimeout: () -> Unit,
    onTrivia: (Int) -> Unit,
    onSendChat: () -> Unit,
    onBlock: () -> Unit,
    onReport: () -> Unit,
    onPhoto: () -> Unit,
    onFriend: () -> Unit,
    onForfeit: () -> Unit,
    onExit: () -> Unit,
    onRematch: () -> Unit,
) {
    val host = me == room.hostId
    val myScore = if (host) room.hostScore else room.guestScore
    val oppScore = if (host) room.guestScore else room.hostScore
    val myRounds = if (host) room.hostRounds else room.guestRounds
    val oppRounds = if (host) room.guestRounds else room.hostRounds
    val myTurn = room.currentPlayerId == me && room.status in listOf("playing", "sudden_death", "final")
    val required = words.lastOrNull()?.normalizedWord?.lastOrNull()?.uppercaseChar()
    val opponentLabel = if (room.isBot) "${room.botName ?: "KelimeBot"} • BOT" else "RAKİP"
    var seconds by remember(room.turnDeadline) { mutableStateOf(45) }

    LaunchedEffect(room.turnDeadline, room.currentPlayerId, room.status) {
        while (room.turnDeadline != null && room.status in listOf("playing", "sudden_death", "final")) {
            val d = runCatching { Instant.parse(room.turnDeadline) }.getOrNull()
            seconds = d?.epochSecond?.minus(Instant.now().epochSecond)?.toInt()?.coerceAtLeast(0) ?: 45
            if (seconds <= 0) { onTimeout(); break }
            delay(1000)
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(if (room.status == "sudden_death") "ANİ ÖLÜM" else "ROUND ${room.roundNo}/3", color = SonHarfCyan, fontWeight = FontWeight.Black)
                            Text(when { room.status == "paused" -> "MAÇ DURAKLATILDI"; room.isBot && room.botTurn -> "BOT DÜŞÜNÜYOR"; myTurn -> "SIRA SENDE"; else -> "RAKİBİN SIRASI" }, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        }
                        if (room.status != "paused") Surface(color = if (seconds <= 10) Color(0xFF5A202C) else SonHarfPurple, shape = RoundedCornerShape(999.dp)) { Text("$seconds sn", Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontWeight = FontWeight.Black) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("SEN $myScore • $myRounds round", fontWeight = FontWeight.Bold)
                        Text("${room.roundWordCount}/10", color = SonHarfMuted)
                        Text("$oppRounds round • $oppScore $opponentLabel", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        if (room.status == "paused") item { ReconnectPanel(room, me) }
        else if (room.status == "quiz") item { TriviaV2(triviaRound, triviaQuestion, onTrivia) }
        else if (room.status != "finished") item {
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SON HARF", color = SonHarfMuted, fontSize = 12.sp)
                    Text(required?.toString() ?: "•", fontSize = 64.sp, fontWeight = FontWeight.Black, color = SonHarfCyan)
                    Text(if (required == null) "İlk kelimeyi yaz" else "$required ile başlayan kelime yaz", color = SonHarfMuted)
                    Spacer(Modifier.height(14.dp))
                    DarkFieldV2(wordInput, onWordInputChange, "Kelime")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onSubmitWord, enabled = myTurn && wordInput.trim().length >= 2 && !busy, modifier = Modifier.fillMaxWidth()) { Text("GÖNDER", fontWeight = FontWeight.Black) }
                    Text("Doğru +3 • Hatalı/Süre −1 • 5 kusursuz doğru +3", color = SonHarfMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

        if (room.status == "finished") item {
            val botWon = room.isBot && room.winnerId == null && room.guestScore > room.hostScore
            val title = when { room.winnerId == me -> "KAZANDIN ✦"; botWon -> "BOT KAZANDI"; else -> "MAÇ BİTTİ" }
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF11263A)), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(title, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text("Round $myRounds - $oppRounds • Puan $myScore - $oppScore", color = SonHarfMuted)
                    Button(onClick = onRematch, enabled = !rematchRequested, modifier = Modifier.fillMaxWidth()) { Text(if (rematchRequested) "HAZIRLANIYOR…" else "↻ TEKRAR OYNA", fontWeight = FontWeight.Black) }
                    if (!room.isBot) OutlinedButton(onClick = onFriend, modifier = Modifier.fillMaxWidth()) { Text("+ ARKADAŞ EKLE") }
                    TextButton(onClick = onExit) { Text("LOBİYE DÖN") }
                }
            }
        }

        item { if (status.isNotBlank()) Text(status, color = SonHarfMuted, fontSize = 11.sp) }

        if (!room.isBot) item {
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("CANLI SOHBET", color = SonHarfCyan, fontWeight = FontWeight.Black)
                        Row { TextButton(onClick = onPhoto) { Text("Fotoğraf", fontSize = 10.sp) }; TextButton(onClick = onReport) { Text("Rapor", fontSize = 10.sp) }; TextButton(onClick = onBlock) { Text("Engelle", color = Color(0xFFFF8894), fontSize = 10.sp) } }
                    }
                    if (chat.isNotEmpty()) Text(chat.takeLast(3).joinToString("\n") { (if (it.senderId == me) "Sen: " else "Rakip: ") + it.body }, color = SonHarfMuted, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = chatInput, onValueChange = onChatInputChange, placeholder = { Text("Mesaj yaz") }, singleLine = true, modifier = Modifier.weight(1f))
                        Button(onClick = onSendChat, enabled = chatInput.isNotBlank() && room.status != "paused") { Text("➤") }
                    }
                }
            }
        }

        if (room.status != "finished" && room.status != "paused") item { OutlinedButton(onClick = onForfeit, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, Color(0xFF6C3340))) { Text("PES ET", color = Color(0xFFFF8894)) } }
    }
}

@Composable
private fun ReconnectPanel(room: GameRoomDto, me: String?) {
    var seconds by remember(room.reconnectDeadline) { mutableStateOf(60) }
    LaunchedEffect(room.reconnectDeadline) {
        while (room.reconnectDeadline != null) {
            val deadline = runCatching { Instant.parse(room.reconnectDeadline) }.getOrNull()
            seconds = deadline?.epochSecond?.minus(Instant.now().epochSecond)?.toInt()?.coerceAtLeast(0) ?: 0
            if (seconds <= 0) break
            delay(1000)
        }
    }
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2134)), shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, SonHarfGold.copy(alpha = .45f))) {
        Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("BAĞLANTI KOPTU", color = SonHarfGold, fontWeight = FontWeight.Black)
            Text(if (room.disconnectedPlayerId == me) "Bağlantın geri geldiğinde maç otomatik devam eder." else "Rakibin yeniden bağlanması bekleniyor.", textAlign = TextAlign.Center)
            Text("$seconds sn", fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text("60 saniye içinde dönmezse bağlı kalan oyuncu hükmen kazanır.", color = SonHarfMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun TriviaV2(round: TriviaRoundDto?, q: TriviaQuestionDto?, onAnswer: (Int) -> Unit) {
    if (round == null || q == null) { Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }
    var unlocked by remember(round.revealAt) { mutableStateOf(false) }
    var seconds by remember(round.revealAt) { mutableStateOf(3) }
    LaunchedEffect(round.revealAt) {
        while (true) {
            val reveal = runCatching { Instant.parse(round.revealAt) }.getOrNull()
            seconds = reveal?.epochSecond?.minus(Instant.now().epochSecond)?.toInt()?.coerceAtLeast(0) ?: 0
            unlocked = seconds <= 0
            if (unlocked) break
            delay(250)
        }
    }
    Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text("🧠 GENEL KÜLTÜR • +${round.bonusPoints}", color = SonHarfGold, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp)); Text(q.question, fontSize = 20.sp, fontWeight = FontWeight.Black); Spacer(Modifier.height(10.dp))
            if (!unlocked) Text("Şıklar $seconds saniye sonra açılacak…", color = SonHarfCyan)
            else listOf(q.optionA, q.optionB, q.optionC, q.optionD).forEachIndexed { i, option -> OutlinedButton(onClick = { SonHarfSoundFx.tap(); onAnswer(i) }, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) { Text("${'A' + i}) $option", modifier = Modifier.fillMaxWidth()) } }
        }
    }
}

@Composable
private fun DarkFieldV2(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SonHarfCyan, unfocusedBorderColor = SonHarfSurface2, focusedContainerColor = Color(0xFF0D1322), unfocusedContainerColor = Color(0xFF0D1322)))
}

@Composable
private fun MissingBackendV2() { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Supabase bağlantısı yapılandırılmalı.", textAlign = TextAlign.Center) } }

private fun gameEvent(room: GameRoomDto, me: String?): String = when (room.lastEvent) {
    "streak_bonus" -> if (room.lastEventPlayerId == me) "🔥 SÖZ FIRTINASI! +3 ekstra" else "Rakip seri yaptı!"
    "bot_streak_bonus" -> "🤖 Bot seri bonusu aldı."
    "bot_valid_word" -> "🤖 Bot kelimesini oynadı."
    "bot_failed" -> "🤖 Bot hata yaptı; sıra sende."
    "invalid_word", "not_in_dictionary", "wrong_start_letter", "word_already_used" -> "Geçersiz hamle: −1 ve sıra değişti."
    "turn_expired" -> "Süre doldu: −1 ve sıra değişti."
    "quiz_started" -> "🧠 Bonus soru! 3 saniye oku."
    "bot_quiz_won" -> "🤖 Bot bonus soruyu aldı."
    "quiz_no_winner" -> "Bonus soruda doğru cevap çıkmadı."
    "opponent_disconnected" -> "Rakibin bağlantısı koptu; maç duraklatıldı."
    "player_reconnected" -> "Bağlantı geri geldi; maç devam ediyor."
    "disconnect_forfeit" -> "Bağlantı süresi doldu; maç hükmen sonuçlandı."
    "match_finished" -> "Maç tamamlandı."
    "sudden_death_started" -> "⚡ ANİ ÖLÜM! İlk hata kaybettirir."
    else -> "Hamle işlendi."
}
