package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import io.github.jan.supabase.postgrest.from
import java.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch

@Composable
fun OnlineGameScreenV6() {
    if (!SupabaseProvider.configured) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Sunucu bağlantısı yapılandırılmamış.") }
        return
    }
    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var opponentProfile by remember { mutableStateOf<ProfileDto?>(null) }
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
    var showPrivate by remember { mutableStateOf(false) }
    var showFriends by remember { mutableStateOf(false) }
    var showChat by remember { mutableStateOf(false) }
    var friends by remember { mutableStateOf<List<Pair<FriendshipDto, ProfileDto>>>(emptyList()) }
    var invites by remember { mutableStateOf<List<GameInviteDto>>(emptyList()) }
    var friendRequests by remember { mutableStateOf<List<Pair<FriendshipDto, ProfileDto>>>(emptyList()) }
    var roomJob by remember { mutableStateOf<Job?>(null) }
    var wordsJob by remember { mutableStateOf<Job?>(null) }
    var chatJob by remember { mutableStateOf<Job?>(null) }
    var matchJob by remember { mutableStateOf<Job?>(null) }

    fun friendly(raw: String) = when {
        "player_already_in_game" in raw -> "Aktif maçına dönülüyor…"
        "not_your_turn" in raw -> "Sıra rakibinde."
        "word_already_used" in raw -> "Bu kelime daha önce kullanıldı."
        "wrong_start_letter" in raw -> "Kelime son harfle başlamalı."
        "not_in_dictionary" in raw -> "Bu kelime sözlükte bulunamadı. Başka bir kelime dene."
        "invalid_word" in raw -> "Bu kelime geçerli değil. Başka bir kelime dene."
        "turn_expired" in raw -> "Süren doldu. −1 puan."
        "vip_required" in raw -> "Özel oda oluşturmak yalnızca VIP oyunculara açık."
        raw.isNotBlank() -> "İşlem yapılamadı: ${raw.takeLast(120)}"
        else -> "İşlem yapılamadı."
    }
    fun failedEvent(e: String?) = e in setOf("word_already_used", "wrong_start_letter", "not_in_dictionary", "invalid_word", "turn_expired")
    fun eventMessage(e: String?) = when (e) {
        "word_already_used" -> "Bu kelime daha önce kullanıldı."
        "wrong_start_letter" -> "Kelime son harfle başlamalı."
        "not_in_dictionary" -> "Bu kelime sözlükte bulunamadı. Başka bir kelime dene."
        "invalid_word" -> "Bu kelime geçerli değil. Başka bir kelime dene."
        "turn_expired" -> "Süren doldu. −1 puan."
        else -> "Hamle işlenemedi."
    }

    suspend fun ensureProfile(): ProfileDto {
        if (backend.currentUserId() == null) backend.ensurePlayer("Oyuncu")
        val id = requireNotNull(backend.currentUserId())
        return runCatching { backend.getProfile(id) }.getOrElse { backend.ensurePlayer("Oyuncu") }.also { profile = it }
    }
    suspend fun activeRoom(): GameRoomDto? {
        val me = backend.currentUserId() ?: return null
        return SupabaseProvider.client.from("game_rooms").select().decodeList<GameRoomDto>()
            .filter { (it.hostId == me || it.guestId == me) && it.status in listOf("waiting", "playing", "quiz", "final", "sudden_death", "paused") }
            .maxByOrNull { it.validWordCount }
    }
    suspend fun refreshQuiz(r: GameRoomDto) {
        if (r.status == "quiz") {
            triviaRound = backend.getActiveTriviaRound(r.id)
            triviaQuestion = triviaRound?.let { backend.getTriviaQuestion(it.questionId) }
        } else { triviaRound = null; triviaQuestion = null }
    }
    suspend fun refreshOpponent(r: GameRoomDto) {
        if (r.isBot) { opponentProfile = null; return }
        val me = backend.currentUserId()
        val id = if (r.hostId == me) r.guestId else r.hostId
        opponentProfile = id?.let { runCatching { backend.getProfile(it) }.getOrNull() }
    }
    fun observe(r: GameRoomDto) {
        roomJob?.cancel(); wordsJob?.cancel(); chatJob?.cancel(); matchJob?.cancel(); matching = false
        scope.launch { refreshOpponent(r) }
        roomJob = scope.launch {
            backend.observeRoom(r.id).retryWhen { _, _ -> delay(900); true }.collect {
                room = it; refreshQuiz(it); refreshOpponent(it)
            }
        }
        wordsJob = scope.launch {
            backend.observeWords(r.id).retryWhen { _, _ -> delay(900); true }.collect { words = it }
        }
        if (!r.isBot) chatJob = scope.launch {
            backend.observeChat(r.id).retryWhen { _, _ -> delay(1200); true }.collect { chat = it }
        }
    }

    LaunchedEffect(Unit) {
        busy = true
        runCatching { ensureProfile() }.onSuccess { p ->
            val old = runCatching { activeRoom() }.getOrNull()
            if (old != null) { room = old; observe(old); notice = "${p.displayName}, aktif maçına dönüldü." }
            else notice = "${p.displayName}, düelloya hazırsın."
        }.onFailure { notice = friendly(it.message.orEmpty()) }
        busy = false
    }

    val active = room
    if (active == null) {
        AuroraDuelLobby(
            playerName = profile?.displayName ?: "Oyuncu", isVip = profile?.isVip == true, language = language, matching = matching, notice = notice,
            showPrivate = showPrivate, showFriends = showFriends, privateCode = privateCode, friends = friends, invites = invites, friendRequests = friendRequests,
            onLanguage = { language = it; SonHarfSoundFx.tap() },
            onPrivateCode = { privateCode = it.filter(Char::isLetterOrDigit).uppercase().take(6) },
            onRandom = {
                scope.launch {
                    busy = true
                    runCatching { ensureProfile(); backend.startRandomMatchmaking(language) }
                        .onSuccess { matching = true; notice = "Rakip aranıyor…" }
                        .onFailure {
                            if ("player_already_in_game" in it.message.orEmpty()) {
                                val old = runCatching { activeRoom() }.getOrNull()
                                if (old != null) { room = old; observe(old) } else notice = friendly(it.message.orEmpty())
                            } else notice = friendly(it.message.orEmpty())
                        }
                    busy = false
                    if (matching) matchJob = launch {
                        while (matching && room == null) {
                            val found = runCatching { backend.pollRandomMatchmakingRoom() }.getOrNull()
                            if (found != null) { room = found; observe(found); SonHarfSoundFx.softNotify(); break }
                            delay(900)
                        }
                    }
                }
            },
            onCancel = { scope.launch { matching = false; matchJob?.cancel(); runCatching { backend.cancelRandomMatchmaking() }; notice = "Eşleşme iptal edildi." } },
            onPrivate = { showPrivate = !showPrivate; showFriends = false },
            onFriends = { scope.launch { friends = runCatching { backend.getFriends() }.getOrDefault(emptyList()); invites = runCatching { backend.getIncomingGameInvites() }.getOrDefault(emptyList()); friendRequests = runCatching { backend.getIncomingFriendRequests() }.getOrDefault(emptyList()); showFriends = !showFriends; showPrivate = false } },
            onCreate = { scope.launch { if (profile?.isVip != true) { notice = "Özel oda oluşturmak yalnızca VIP oyunculara açık."; return@launch }; busy = true; runCatching { backend.createPrivateRoom(language) }.onSuccess { room = it; observe(it) }.onFailure { notice = friendly(it.message.orEmpty()) }; busy = false } },
            onJoin = { scope.launch { busy = true; runCatching { backend.joinPrivateRoom(privateCode) }.onSuccess { room = it; language = it.language; observe(it) }.onFailure { notice = friendly(it.message.orEmpty()) }; busy = false } },
            onInvite = { id -> scope.launch { runCatching { backend.inviteFriend(id, language) }.onSuccess { notice = "Davet gönderildi." }.onFailure { notice = friendly(it.message.orEmpty()) } } },
            onInviteResponse = { id, accept -> scope.launch { runCatching { backend.respondGameInvite(id, accept) }.onSuccess { if (it != null) { room = it; observe(it) }; invites = runCatching { backend.getIncomingGameInvites() }.getOrDefault(emptyList()) }.onFailure { notice = friendly(it.message.orEmpty()) } } },
            onFriendResponse = { id, accept -> scope.launch { runCatching { backend.respondFriendRequest(id, accept) }.onSuccess { friendRequests = runCatching { backend.getIncomingFriendRequests() }.getOrDefault(emptyList()); friends = runCatching { backend.getFriends() }.getOrDefault(emptyList()) }.onFailure { notice = friendly(it.message.orEmpty()) } } }
        )
    } else {
        val me = backend.currentUserId()
        LaunchedEffect(active.currentPlayerId, active.validWordCount, active.roundNo) { wordInput = "" }
        LaunchedEffect(active.id) { while (true) { if (!active.isBot && active.status != "waiting") runCatching { backend.heartbeatRoom(active.id) }.onSuccess { room = it }; delay(5000) } }
        LaunchedEffect(active.id, active.status, active.botTurn, active.validWordCount) {
            if (active.isBot && active.botTurn && active.status in listOf("playing", "final", "sudden_death")) {
                delay(1600L + (active.validWordCount % 4) * 350L)
                runCatching { backend.botTakeTurn(active.id) }.onSuccess { room = it }.onFailure { notice = friendly(it.message.orEmpty()) }
            }
        }
        AuroraArena(
            room = active, me = me, playerName = profile?.displayName ?: "Sen",
            opponentName = if (active.isBot) "${active.botName ?: "KelimeBot"} BOT" else opponentProfile?.displayName ?: "Rakip",
            words = words, wordInput = wordInput, onWordInput = { wordInput = it.take(40) }, notice = notice, busy = busy,
            triviaRound = triviaRound, triviaQuestion = triviaQuestion,
            onSubmit = {
                scope.launch {
                    val submitted = wordInput.trim(); if (submitted.isBlank()) return@launch
                    busy = true; SonHarfSoundFx.tap()
                    val fresh = runCatching { backend.getRoom(active.id) }.getOrElse { active }
                    if (fresh.currentPlayerId != me || fresh.botTurn) {
                        room = fresh; notice = "Sıra rakibinde."; busy = false; return@launch
                    }
                    runCatching { backend.submitWord(fresh.id, submitted) }.onSuccess { result ->
                        room = result
                        wordInput = ""
                        words = runCatching { backend.getWords(result.id) }.getOrDefault(words)
                        if (failedEvent(result.lastEvent) && result.lastEventPlayerId == me) {
                            notice = eventMessage(result.lastEvent); SonHarfSoundFx.warning()
                        } else {
                            notice = "Kelime kabul edildi: ${submitted.uppercase()}"; SonHarfSoundFx.wordAccepted()
                        }
                    }.onFailure {
                        notice = friendly(it.message.orEmpty())
                        SonHarfSoundFx.warning()
                    }
                    busy = false
                }
            },
            onTimeout = { scope.launch { runCatching { backend.claimTurnTimeout(active.id) }.onSuccess { room = it } } },
            onTrivia = { idx -> scope.launch { val q = triviaRound ?: return@launch; runCatching { backend.answerTrivia(q.id, idx) }.onSuccess { room = it; refreshQuiz(it) }.onFailure { notice = friendly(it.message.orEmpty()) } } },
            onChat = { showChat = true },
            onForfeit = { scope.launch { runCatching { backend.forfeit(active.id) }.onSuccess { room = it } } },
            onExit = { roomJob?.cancel(); wordsJob?.cancel(); chatJob?.cancel(); room = null; words = emptyList(); chat = emptyList(); notice = "Yeni düelloya hazırsın." },
            onRematch = { scope.launch { runCatching { if (active.isBot) backend.restartBotMatch(active.id) else backend.requestRematch(active.id) }.onSuccess { room = it; words = emptyList(); chat = emptyList(); if (it.id != active.id) observe(it) }.onFailure { notice = friendly(it.message.orEmpty()) } } }
        )
        if (showChat && !active.isBot) AuroraChatDialog(chat, me, chatInput, { chatInput = it.take(300) }, { showChat = false }) { scope.launch { runCatching { backend.sendChat(active.id, chatInput) }.onSuccess { chatInput = "" } } }
    }
}

@Composable
private fun AuroraDuelLobby(
    playerName: String, isVip: Boolean, language: String, matching: Boolean, notice: String, showPrivate: Boolean, showFriends: Boolean,
    privateCode: String, friends: List<Pair<FriendshipDto, ProfileDto>>, invites: List<GameInviteDto>, friendRequests: List<Pair<FriendshipDto, ProfileDto>>, onLanguage: (String) -> Unit,
    onPrivateCode: (String) -> Unit, onRandom: () -> Unit, onCancel: () -> Unit, onPrivate: () -> Unit, onFriends: () -> Unit,
    onCreate: () -> Unit, onJoin: () -> Unit, onInvite: (String) -> Unit, onInviteResponse: (String, Boolean) -> Unit,
    onFriendResponse: (String, Boolean) -> Unit
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("DÜELLO", fontSize = 24.sp, fontWeight = FontWeight.Black)
                Surface(shape = RoundedCornerShape(99.dp), color = MaterialTheme.colorScheme.surface) { Text(playerName, Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = SonHarfCyan, fontWeight = FontWeight.Bold) }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF091324)), shape = RoundedCornerShape(28.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = .05f))) {
                Box(Modifier.fillMaxWidth().height(330.dp).background(Brush.radialGradient(listOf(Color(0xFF22104E), Color(0xFF0D1530), Color(0xFF08111F)))), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(225.dp).clip(CircleShape).background(Brush.sweepGradient(listOf(SonHarfPurple, SonHarfCyan, SonHarfPink, SonHarfPurple))).padding(3.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFF081220)), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (matching) CircularProgressIndicator(Modifier.size(38.dp), strokeWidth = 3.dp, color = SonHarfCyan)
                                Spacer(Modifier.height(12.dp))
                                Text(if (matching) "RAKİP\nARANIYOR" else "DÜELLOYA\nHAZIR", textAlign = TextAlign.Center, fontWeight = FontWeight.Black, fontSize = 22.sp)
                                Text(if (matching) "Gerçek oyuncu • sonra BOT" else "Rakibini bul ve başla", color = SonHarfMuted, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = language == "tr", onClick = { onLanguage("tr") }, label = { Text("🇹🇷 TÜRKÇE") }, modifier = Modifier.weight(1f))
                FilterChip(selected = language == "en", onClick = { onLanguage("en") }, label = { Text("🇬🇧 ENGLISH") }, modifier = Modifier.weight(1f))
            }
        }
        item {
            if (matching) Button(onClick = onCancel, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF641A35)), shape = RoundedCornerShape(18.dp)) { Text("✕  İPTAL", fontWeight = FontWeight.Black) }
            else Button(onClick = onRandom, modifier = Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = SonHarfGold, contentColor = Color(0xFF1A1100)), shape = RoundedCornerShape(20.dp)) { Text("DÜELLOYA GİR  ⚡", fontWeight = FontWeight.Black, fontSize = 18.sp) }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedButton(onClick = onFriends, modifier = Modifier.weight(1f).height(50.dp), border = BorderStroke(1.dp, SonHarfGreen.copy(alpha = .55f))) { Text("👥 ARKADAŞ") }
                OutlinedButton(onClick = onPrivate, modifier = Modifier.weight(1f).height(50.dp), border = BorderStroke(1.dp, SonHarfPurple.copy(alpha = .55f))) { Text("♛ ÖZEL ODA") }
            }
        }
        item { Text(notice, color = SonHarfMuted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
        if (showPrivate) item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("ÖZEL ODA", color = SonHarfPurple, fontWeight = FontWeight.Black)
                    Button(onClick = onCreate, enabled = isVip, modifier = Modifier.fillMaxWidth()) { Text(if (isVip) "VIP ODA OLUŞTUR" else "VIP GEREKLİ") }
                    if (!isVip) Text("Oda oluşturma VIP oyunculara özeldir. Oda koduyla katılabilirsin.", color = SonHarfGold, fontSize = 9.sp)
                    OutlinedTextField(privateCode, onPrivateCode, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("6 haneli oda kodu") })
                    OutlinedButton(onClick = onJoin, enabled = privateCode.length == 6, modifier = Modifier.fillMaxWidth()) { Text("ODA KODUYLA KATIL") }
                }
            }
        }
        if (showFriends) item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (friendRequests.isNotEmpty()) Text("ARKADAŞLIK İSTEKLERİ", color = SonHarfCyan, fontWeight = FontWeight.Black, fontSize = 10.sp)
                    friendRequests.forEach { (_, p) -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(p.displayName); Row { TextButton(onClick = { onFriendResponse(p.id, true) }) { Text("Kabul") }; TextButton(onClick = { onFriendResponse(p.id, false) }) { Text("Reddet") } } } }
                    if (invites.isNotEmpty()) Text("OYUN DAVETLERİ", color = SonHarfCyan, fontWeight = FontWeight.Black, fontSize = 10.sp)
                    invites.forEach { i -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Maç daveti"); Row { TextButton(onClick = { onInviteResponse(i.id, true) }) { Text("Kabul") }; TextButton(onClick = { onInviteResponse(i.id, false) }) { Text("Reddet") } } } }
                    Text("ARKADAŞLAR", color = SonHarfCyan, fontWeight = FontWeight.Black, fontSize = 10.sp)
                    if (friends.isEmpty()) Text("Arkadaş eklemek için Ayarlar > Arkadaşlar ve İstekler bölümünü kullan.", color = SonHarfMuted, fontSize = 9.sp)
                    friends.forEach { (_, p) -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(p.displayName); Button(onClick = { onInvite(p.id) }, enabled = p.presenceStatus == "online") { Text(if (p.presenceStatus == "online") "Davet" else "Çevrimdışı") } } }
                }
            }
        }
    }
}

@Composable
private fun AuroraArena(
    room: GameRoomDto, me: String?, playerName: String, opponentName: String, words: List<GameWordDto>, wordInput: String,
    onWordInput: (String) -> Unit, notice: String, busy: Boolean, triviaRound: TriviaRoundDto?, triviaQuestion: TriviaQuestionDto?,
    onSubmit: () -> Unit, onTimeout: () -> Unit, onTrivia: (Int) -> Unit, onChat: () -> Unit, onForfeit: () -> Unit,
    onExit: () -> Unit, onRematch: () -> Unit
) {
    val host = me == room.hostId
    val myScore = if (host) room.hostScore else room.guestScore
    val oppScore = if (host) room.guestScore else room.hostScore
    val myRounds = if (host) room.hostRounds else room.guestRounds
    val oppRounds = if (host) room.guestRounds else room.hostRounds
    val myTurn = room.currentPlayerId == me && room.status in listOf("playing", "final", "sudden_death")
    val last = words.lastOrNull()?.normalizedWord
    val required = last?.lastOrNull()?.uppercaseChar()?.toString() ?: "•"
    var seconds by remember(room.turnDeadline) { mutableStateOf(45) }
    val focus = LocalFocusManager.current

    LaunchedEffect(room.turnDeadline, room.currentPlayerId, room.status) {
        while (room.turnDeadline != null && room.status in listOf("playing", "final", "sudden_death")) {
            seconds = runCatching { (Instant.parse(room.turnDeadline).epochSecond - Instant.now().epochSecond).toInt().coerceAtLeast(0) }.getOrDefault(45)
            if (seconds <= 0) { onTimeout(); break }
            if (seconds in 1..5) SonHarfSoundFx.countdown()
            delay(1000)
        }
    }

    if (room.status == "finished") {
        val won = room.winnerId == me
        Box(Modifier.fillMaxSize().padding(18.dp), contentAlignment = Alignment.Center) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(28.dp), border = BorderStroke(1.dp, if (won) SonHarfGold.copy(alpha = .5f) else SonHarfPink.copy(alpha = .35f))) {
                Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("MAÇ SONUCU", fontWeight = FontWeight.Black)
                    Text(if (won) "KAZANAN" else "MAÇ BİTTİ", color = if (won) SonHarfCyan else SonHarfMuted, fontSize = 14.sp)
                    Text(if (won) playerName else opponentName, fontSize = 34.sp, fontWeight = FontWeight.Black, color = if (won) SonHarfCyan else SonHarfPink)
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("$myRounds", fontSize = 42.sp, fontWeight = FontWeight.Black); Text("  -  ", color = SonHarfMuted); Text("$oppRounds", fontSize = 42.sp, fontWeight = FontWeight.Black) }
                    Button(onClick = onRematch, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp)) { Text("TEKRAR OYNA", fontWeight = FontWeight.Black) }
                    OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("LOBİYE DÖN") }
                }
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            AuroraPlayerCard(playerName, myScore, myRounds, myTurn, SonHarfCyan, Modifier.weight(1f))
            Box(Modifier.size(70.dp).clip(CircleShape).background(Brush.sweepGradient(listOf(SonHarfCyan, SonHarfGold, SonHarfPink, SonHarfCyan))).padding(3.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFF0B1424)), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("$seconds", fontSize = 25.sp, fontWeight = FontWeight.Black); Text("sn", fontSize = 8.sp, color = SonHarfMuted) } }
            }
            AuroraPlayerCard(opponentName, oppScore, oppRounds, !myTurn, SonHarfPink, Modifier.weight(1f))
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = .05f))) {
            Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (room.status == "sudden_death") "ANİ ÖLÜM" else "ROUND ${room.roundNo}/3", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { repeat(10) { i -> Box(Modifier.size(if (i == room.roundWordCount.coerceAtMost(9)) 7.dp else 5.dp).clip(CircleShape).background(if (i < room.roundWordCount) SonHarfCyan else SonHarfSurface2)) } }
                    Text("${room.roundWordCount}/10", fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
                Text(if (myTurn) "SIRA SENDE" else if (room.isBot && room.botTurn) "BOT DÜŞÜNÜYOR" else "RAKİBİN SIRASI", color = if (myTurn) SonHarfCyan else SonHarfMuted, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Box(Modifier.size(158.dp).clip(CircleShape).background(Brush.sweepGradient(listOf(SonHarfCyan, SonHarfPurple, SonHarfPink, SonHarfCyan))).padding(4.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.fillMaxSize().clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFF17203A), Color(0xFF090E19)))), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("SON HARF", color = SonHarfMuted, fontSize = 9.sp); Text(required, fontSize = 72.sp, fontWeight = FontWeight.Black) }
                    }
                }
                Text(if (last == null) "İlk kelimeyi sen başlat." else "$required ile başlayan bir kelime yaz", color = SonHarfMuted, fontSize = 11.sp)
            }
        }

        if (words.isNotEmpty()) {
            Column {
                Text("KELİME ZİNCİRİ", color = SonHarfMuted, fontSize = 9.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(5.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(words.takeLast(18)) { w -> Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, Color.White.copy(alpha = .05f))) { Text(w.word.uppercase(), Modifier.padding(horizontal = 11.dp, vertical = 7.dp), fontWeight = FontWeight.Bold, fontSize = 10.sp) } }
                }
            }
        }

        Surface(color = if (notice.startsWith("Bu ") || notice.contains("geçerli") || notice.contains("sözlük")) Color(0xFF391520) else MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp)) {
            Text(notice, Modifier.fillMaxWidth().padding(8.dp), color = if (notice.startsWith("Bu ") || notice.contains("geçerli") || notice.contains("sözlük")) Color(0xFFFF7D9B) else SonHarfMuted, fontSize = 10.sp, textAlign = TextAlign.Center)
        }

        if (room.status == "quiz" && triviaRound != null && triviaQuestion != null) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1330)), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, SonHarfPurple.copy(alpha = .45f))) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("⭐ BONUS +${triviaRound.bonusPoints}", color = SonHarfGold, fontWeight = FontWeight.Black)
                    Text(triviaQuestion.question, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    listOf(triviaQuestion.optionA, triviaQuestion.optionB, triviaQuestion.optionC, triviaQuestion.optionD).forEachIndexed { i, s -> OutlinedButton(onClick = { onTrivia(i) }, modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp)) { Text(s, fontSize = 10.sp) } }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = wordInput, onValueChange = onWordInput, enabled = myTurn && !busy, singleLine = true, modifier = Modifier.weight(1f),
                placeholder = { Text(if (myTurn) "Kelimenizi yazın…" else "Rakibin sırası…") }, shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { if (myTurn && wordInput.isNotBlank() && !busy) { focus.clearFocus(); onSubmit() } })
            )
            Button(onClick = { focus.clearFocus(); onSubmit() }, enabled = myTurn && wordInput.isNotBlank() && !busy, modifier = Modifier.size(56.dp), shape = CircleShape, contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = SonHarfBlue)) { Text("➤", fontSize = 22.sp) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            OutlinedButton(onClick = onForfeit, modifier = Modifier.weight(1f).height(42.dp), border = BorderStroke(1.dp, SonHarfPink.copy(alpha = .55f))) { Text("⚑ PES ET", color = SonHarfPink, fontSize = 10.sp) }
            OutlinedButton(onClick = onChat, modifier = Modifier.weight(1f).height(42.dp), border = BorderStroke(1.dp, SonHarfCyan.copy(alpha = .45f))) { Text("● SOHBET", color = SonHarfCyan, fontSize = 10.sp) }
            OutlinedButton(onClick = {}, modifier = Modifier.weight(1f).height(42.dp), border = BorderStroke(1.dp, SonHarfGold.copy(alpha = .45f))) { Text("★ BONUS", color = SonHarfGold, fontSize = 10.sp) }
        }
    }
}

@Composable private fun AuroraPlayerCard(name: String, score: Int, rounds: Int, active: Boolean, accent: Color, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = if (active) accent.copy(alpha = .11f) else MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, if (active) accent.copy(alpha = .5f) else Color.White.copy(alpha = .05f))) {
        Row(Modifier.padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(accent.copy(alpha = .18f)), contentAlignment = Alignment.Center) { Text(name.take(1).uppercase(), fontWeight = FontWeight.Black) }
            Spacer(Modifier.width(7.dp))
            Column { Text(name, maxLines = 1, fontSize = 9.sp, color = if (active) accent else SonHarfMuted); Text(score.toString(), fontSize = 22.sp, fontWeight = FontWeight.Black); Text("$rounds round", color = SonHarfMuted, fontSize = 7.sp) }
        }
    }
}

@Composable private fun AuroraChatDialog(chat: List<ChatMessageDto>, me: String?, input: String, onInput: (String) -> Unit, onClose: () -> Unit, onSend: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("SOHBET", fontWeight = FontWeight.Black) },
        text = {
            Column(Modifier.heightIn(max = 420.dp)) {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) { items(chat.takeLast(30)) { m -> Surface(color = if (m.senderId == me) SonHarfPurple.copy(alpha = .14f) else MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp)) { Text(m.body, Modifier.padding(9.dp), fontSize = 11.sp) } } }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(input, onInput, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Mesaj yaz…") })
            }
        },
        confirmButton = { TextButton(onClick = onSend, enabled = input.isNotBlank()) { Text("GÖNDER") } },
        dismissButton = { TextButton(onClick = onClose) { Text("KAPAT") } }
    )
}
