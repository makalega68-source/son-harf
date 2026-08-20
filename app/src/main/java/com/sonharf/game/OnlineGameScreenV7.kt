package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

private val V7Bg = Color(0xFF020713)
private val V7Panel = Color(0xFF08111F)
private val V7Panel2 = Color(0xFF0D1728)
private val V7Stroke = Color(0xFF21324A)
private val V7Blue = Color(0xFF18B8FF)
private val V7Purple = Color(0xFF6A42F4)
private val V7Magenta = Color(0xFFB51FE8)
private val V7Gold = Color(0xFFF5C04D)
private val V7Red = Color(0xFFF25B75)
private val V7Green = Color(0xFF4BC765)
private val V7Muted = Color(0xFF8C98AD)
private val V7Text = Color(0xFFF3F6FF)

@Composable
fun OnlineGameScreenV7() {
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
    var notice by remember { mutableStateOf("Düelloya hazırsın.") }
    var busy by remember { mutableStateOf(false) }
    var matching by remember { mutableStateOf(false) }
    var showPrivate by remember { mutableStateOf(false) }
    var showFriends by remember { mutableStateOf(false) }
    var showChat by remember { mutableStateOf(false) }
    var friends by remember { mutableStateOf<List<Pair<FriendshipDto, ProfileDto>>>(emptyList()) }
    var invites by remember { mutableStateOf<List<GameInviteDto>>(emptyList()) }
    var roomJob by remember { mutableStateOf<Job?>(null) }
    var wordsJob by remember { mutableStateOf<Job?>(null) }
    var chatJob by remember { mutableStateOf<Job?>(null) }
    var matchJob by remember { mutableStateOf<Job?>(null) }

    fun friendly(raw: String) = when {
        "not_your_turn" in raw -> "Sıra rakibinde."
        "word_already_used" in raw -> "Bu kelime daha önce kullanıldı."
        "wrong_start_letter" in raw -> "Kelime son harfle başlamalı."
        "not_in_dictionary" in raw -> "Bu kelime sözlükte bulunamadı."
        "invalid_word" in raw -> "Bu kelime geçerli değil."
        "turn_expired" in raw -> "Süren doldu. −1 puan."
        "vip_required" in raw -> "Özel oda oluşturmak için VIP gerekli."
        "player_already_in_game" in raw -> "Aktif maçına dönülüyor…"
        else -> "Bağlantı sorunu. Yeniden deneniyor."
    }
    fun failedEvent(e: String?) = e in setOf("word_already_used", "wrong_start_letter", "not_in_dictionary", "invalid_word", "turn_expired")
    fun eventMessage(e: String?) = when (e) {
        "word_already_used" -> "Bu kelime daha önce kullanıldı."
        "wrong_start_letter" -> "Kelime son harfle başlamalı."
        "not_in_dictionary" -> "Bu kelime sözlükte bulunamadı."
        "invalid_word" -> "Bu kelime geçerli değil."
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
        roomJob = scope.launch { backend.observeRoom(r.id).catch { notice = friendly(it.message.orEmpty()) }.collect { room = it; refreshQuiz(it); refreshOpponent(it) } }
        wordsJob = scope.launch { backend.observeWords(r.id).catch { notice = friendly(it.message.orEmpty()) }.collect { words = it } }
        if (!r.isBot) chatJob = scope.launch { backend.observeChat(r.id).catch { notice = friendly(it.message.orEmpty()) }.collect { chat = it } }
    }

    LaunchedEffect(Unit) {
        busy = true
        runCatching { ensureProfile() }.onSuccess { p ->
            language = SonHarfPreferences.language(androidx.compose.ui.platform.LocalContext.current)
            val old = runCatching { activeRoom() }.getOrNull()
            if (old != null) { room = old; observe(old); notice = "${p.displayName}, aktif maçına dönüldü." }
            else notice = "${p.displayName}, düelloya hazırsın."
        }.onFailure { notice = friendly(it.message.orEmpty()) }
        busy = false
    }

    val active = room
    if (active == null) {
        V7Lobby(
            profile = profile, language = language, matching = matching, notice = notice,
            showPrivate = showPrivate, showFriends = showFriends, privateCode = privateCode, friends = friends, invites = invites,
            onLanguage = { language = it },
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
            onFriends = { scope.launch { friends = runCatching { backend.getFriends() }.getOrDefault(emptyList()); invites = runCatching { backend.getIncomingGameInvites() }.getOrDefault(emptyList()); showFriends = !showFriends; showPrivate = false } },
            onCreate = { scope.launch { if (profile?.isVip != true) { notice = "Özel oda oluşturmak için VIP gerekli."; return@launch }; runCatching { backend.createPrivateRoom(language) }.onSuccess { room = it; observe(it) }.onFailure { notice = friendly(it.message.orEmpty()) } } },
            onJoin = { scope.launch { runCatching { backend.joinPrivateRoom(privateCode) }.onSuccess { room = it; language = it.language; observe(it) }.onFailure { notice = friendly(it.message.orEmpty()) } } },
            onInvite = { id -> scope.launch { runCatching { backend.inviteFriend(id, language) }.onSuccess { notice = "Davet gönderildi." }.onFailure { notice = friendly(it.message.orEmpty()) } } },
            onInviteResponse = { id, accept -> scope.launch { runCatching { backend.respondGameInvite(id, accept) }.onSuccess { if (it != null) { room = it; observe(it) } } } }
        )
    } else {
        val me = backend.currentUserId()
        LaunchedEffect(active.currentPlayerId, active.validWordCount, active.roundNo) { wordInput = "" }
        LaunchedEffect(active.id) { while (true) { if (!active.isBot && active.status != "waiting") runCatching { backend.heartbeatRoom(active.id) }.onSuccess { room = it }; delay(5000) } }
        LaunchedEffect(active.id, active.status, active.botTurn, active.validWordCount) {
            if (active.isBot && active.botTurn && active.status in listOf("playing", "final", "sudden_death")) {
                delay(1500L + (active.validWordCount % 4) * 300L)
                runCatching { backend.botTakeTurn(active.id) }.onSuccess { room = it }.onFailure { notice = friendly(it.message.orEmpty()) }
            }
        }
        V7Arena(
            room = active, me = me, playerName = profile?.displayName ?: "Sen",
            opponentName = if (active.isBot) "${active.botName ?: "KelimeBot"} BOT" else opponentProfile?.displayName ?: "Rakip",
            words = words, wordInput = wordInput, onWordInput = { wordInput = it.take(40) }, notice = notice, busy = busy,
            triviaRound = triviaRound, triviaQuestion = triviaQuestion,
            onSubmit = {
                scope.launch {
                    val submitted = wordInput.trim(); if (submitted.isBlank()) return@launch
                    busy = true; SonHarfSoundFx.tap()
                    runCatching { backend.submitWord(active.id, submitted) }.onSuccess { result ->
                        room = result
                        if (failedEvent(result.lastEvent) && result.lastEventPlayerId == me) { notice = eventMessage(result.lastEvent); SonHarfSoundFx.warning() }
                        else { wordInput = ""; notice = "Kelime kabul edildi: ${submitted.uppercase()}"; SonHarfSoundFx.wordAccepted() }
                    }.onFailure { notice = friendly(it.message.orEmpty()); SonHarfSoundFx.warning() }
                    busy = false
                }
            },
            onTimeout = { scope.launch { runCatching { backend.claimTurnTimeout(active.id) }.onSuccess { room = it } } },
            onTrivia = { idx -> scope.launch { val q = triviaRound ?: return@launch; runCatching { backend.answerTrivia(q.id, idx) }.onSuccess { room = it; refreshQuiz(it) }.onFailure { notice = friendly(it.message.orEmpty()) } } },
            onChat = { showChat = true },
            onForfeit = { scope.launch { runCatching { backend.forfeit(active.id) }.onSuccess { room = it } } },
            onExit = { roomJob?.cancel(); wordsJob?.cancel(); chatJob?.cancel(); room = null; words = emptyList(); chat = emptyList(); notice = "Yeni düelloya hazırsın." },
            onRematch = { scope.launch { runCatching { if (active.isBot) backend.restartBotMatch(active.id) else backend.requestRematch(active.id) }.onSuccess { room = it; words = emptyList(); chat = emptyList(); if (it.id != active.id) observe(it) }.onFailure { notice = friendly(it.message.orEmpty()) } } },
            onAddFriend = { scope.launch { val opponent = opponentProfile ?: return@launch; runCatching { backend.sendFriendRequest(opponent.id) }.onSuccess { notice = "Arkadaşlık isteği gönderildi." } } }
        )
        if (showChat && !active.isBot) V7ChatDialog(chat, me, chatInput, { chatInput = it.take(300) }, { showChat = false }) { scope.launch { runCatching { backend.sendChat(active.id, chatInput) }.onSuccess { chatInput = "" } } }
    }
}

@Composable
private fun V7Lobby(
    profile: ProfileDto?, language: String, matching: Boolean, notice: String, showPrivate: Boolean, showFriends: Boolean,
    privateCode: String, friends: List<Pair<FriendshipDto, ProfileDto>>, invites: List<GameInviteDto>, onLanguage: (String) -> Unit,
    onPrivateCode: (String) -> Unit, onRandom: () -> Unit, onCancel: () -> Unit, onPrivate: () -> Unit, onFriends: () -> Unit,
    onCreate: () -> Unit, onJoin: () -> Unit, onInvite: (String) -> Unit, onInviteResponse: (String, Boolean) -> Unit
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) { Text("‹", color = V7Text, fontSize = 28.sp); Spacer(Modifier.width(8.dp)); Text("DÜELLO", color = V7Text, fontWeight = FontWeight.Black, fontSize = 19.sp) }
                Surface(color = V7Panel2, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, V7Stroke)) { Text(if (language == "tr") "🇹🇷  Türkçe" else "🇬🇧  English", Modifier.padding(horizontal = 11.dp, vertical = 8.dp), color = V7Text, fontSize = 9.sp) }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = V7Panel), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, V7Stroke)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().height(265.dp), contentAlignment = Alignment.Center) {
                        repeat(4) { i -> Box(Modifier.size((218 - i * 38).dp).clip(CircleShape).background(Color.Transparent).then(Modifier), contentAlignment = Alignment.Center) }
                        Box(Modifier.size(185.dp).clip(CircleShape).background(Brush.sweepGradient(listOf(V7Blue, V7Purple, V7Magenta, V7Blue))).padding(3.dp), contentAlignment = Alignment.Center) {
                            Box(Modifier.fillMaxSize().clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFF101B33), Color(0xFF061020)))), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(if (matching) "RAKİP\nARANIYOR" else "DÜELLOYA\nHAZIR", color = V7Text, textAlign = TextAlign.Center, fontWeight = FontWeight.Black, fontSize = 22.sp)
                                    Spacer(Modifier.height(9.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { repeat(3) { Box(Modifier.size(6.dp).clip(CircleShape).background(if (matching) V7Blue.copy(alpha = 1f - it * .2f) else V7Stroke)) } }
                                }
                            }
                        }
                    }
                    Text(if (matching) "Tahmini bekleme süresi: 5 - 10 sn" else notice, color = V7Muted, fontSize = 9.sp)
                    Spacer(Modifier.height(14.dp))
                    if (matching) Button(onClick = onCancel, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6E1D3E)), shape = RoundedCornerShape(12.dp)) { Text("✕  İPTAL", fontWeight = FontWeight.Black) }
                    else Button(onClick = onRandom, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = V7Purple), shape = RoundedCornerShape(12.dp)) { Text("DÜELLOYA GİR", fontWeight = FontWeight.Black) }
                    Spacer(Modifier.height(11.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onFriends, modifier = Modifier.weight(1f).height(48.dp), border = BorderStroke(1.dp, V7Stroke)) { Text("♟  ARKADAŞ DAVET ET", fontSize = 8.sp, color = V7Blue) }
                        OutlinedButton(onClick = onPrivate, modifier = Modifier.weight(1f).height(48.dp), border = BorderStroke(1.dp, V7Stroke)) { Text("♛  ÖZEL ODA KATIL", fontSize = 8.sp, color = V7Blue) }
                    }
                    Spacer(Modifier.height(12.dp))
                    Surface(color = V7Panel2, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, V7Stroke)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Text("♢", color = V7Gold); Spacer(Modifier.width(8.dp)); Column { Text("İpucu", color = V7Muted, fontSize = 9.sp); Text("Uzun kelimeler daha fazla puan kazandırır!", color = V7Muted, fontSize = 8.sp) } }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = language == "tr", onClick = { onLanguage("tr") }, label = { Text("🇹🇷 Türkçe") }, modifier = Modifier.weight(1f))
                FilterChip(selected = language == "en", onClick = { onLanguage("en") }, label = { Text("🇬🇧 English") }, modifier = Modifier.weight(1f))
            }
        }
        if (showPrivate) item {
            Card(colors = CardDefaults.cardColors(containerColor = V7Panel), border = BorderStroke(1.dp, V7Stroke), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("ÖZEL ODA", color = V7Text, fontWeight = FontWeight.Black)
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(V7Panel2)) {
                        Text("Oda Oluştur", Modifier.weight(1f).clickable(onClick = onCreate).padding(12.dp), color = if (profile?.isVip == true) V7Text else V7Muted, textAlign = TextAlign.Center, fontSize = 9.sp)
                        Text("Oda Katıl", Modifier.weight(1f).padding(12.dp), color = V7Text, textAlign = TextAlign.Center, fontSize = 9.sp)
                    }
                    if (profile?.isVip != true) Text("Oda oluşturma VIP üyelere özeldir. Oda koduyla katılım herkese açıktır.", color = V7Gold, fontSize = 8.sp)
                    OutlinedTextField(privateCode, onPrivateCode, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Oda kodu") })
                    Button(onClick = onJoin, enabled = privateCode.length == 6, modifier = Modifier.fillMaxWidth()) { Text("ODAYA KATIL") }
                }
            }
        }
        if (showFriends) item {
            Card(colors = CardDefaults.cardColors(containerColor = V7Panel), border = BorderStroke(1.dp, V7Stroke), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ARKADAŞLAR", color = V7Text, fontWeight = FontWeight.Black)
                    invites.forEach { i -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text("Maç daveti", color = V7Text, fontSize = 9.sp); Row { TextButton(onClick = { onInviteResponse(i.id, true) }) { Text("KABUL") }; TextButton(onClick = { onInviteResponse(i.id, false) }) { Text("REDDET") } } } }
                    if (friends.isEmpty() && invites.isEmpty()) Text("Arkadaş veya bekleyen davet yok.", color = V7Muted, fontSize = 9.sp)
                    friends.forEach { (_, p) -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Row(verticalAlignment = Alignment.CenterVertically) { V7Avatar(p.displayName, 30.dp, V7Blue); Spacer(Modifier.width(7.dp)); Column { Text(p.displayName, color = V7Text, fontSize = 9.sp); Text(if (p.presenceStatus == "online") "Çevrimiçi" else "Çevrimdışı", color = if (p.presenceStatus == "online") V7Green else V7Muted, fontSize = 7.sp) } }; Button(onClick = { onInvite(p.id) }, enabled = p.presenceStatus == "online") { Text("DAVET", fontSize = 8.sp) } } }
                }
            }
        }
    }
}

@Composable
private fun V7Arena(
    room: GameRoomDto, me: String?, playerName: String, opponentName: String, words: List<GameWordDto>, wordInput: String,
    onWordInput: (String) -> Unit, notice: String, busy: Boolean, triviaRound: TriviaRoundDto?, triviaQuestion: TriviaQuestionDto?,
    onSubmit: () -> Unit, onTimeout: () -> Unit, onTrivia: (Int) -> Unit, onChat: () -> Unit, onForfeit: () -> Unit,
    onExit: () -> Unit, onRematch: () -> Unit, onAddFriend: () -> Unit
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
        V7Result(playerName, opponentName, myRounds, oppRounds, room.winnerId == me, onRematch, onAddFriend, onExit)
        return
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 11.dp, vertical = 8.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("‹", color = V7Text, fontSize = 28.sp)
            Row { Text("⌁", color = V7Text, fontSize = 18.sp); Spacer(Modifier.width(17.dp)); Text("•••", color = V7Text, fontSize = 16.sp) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            V7PlayerCard("SEN", playerName, myScore, myTurn, V7Purple, Modifier.weight(1f))
            Column(Modifier.width(92.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ROUND ${room.roundNo}/3", color = V7Text, fontWeight = FontWeight.Black, fontSize = 10.sp)
                Text("${room.roundWordCount}/10", color = V7Text, fontSize = 10.sp)
                LinearProgressIndicator(progress = { room.roundWordCount.coerceIn(0, 10) / 10f }, modifier = Modifier.fillMaxWidth().height(4.dp), color = V7Gold, trackColor = V7Panel2)
            }
            V7PlayerCard("RAKİP", opponentName, oppScore, !myTurn, V7Red, Modifier.weight(1f))
        }
        Box(Modifier.fillMaxWidth().height(68.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(62.dp).clip(CircleShape).background(Brush.sweepGradient(listOf(V7Blue, V7Purple, V7Magenta, V7Blue))).padding(3.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.fillMaxSize().clip(CircleShape).background(V7Panel), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("$seconds", color = V7Text, fontSize = 22.sp, fontWeight = FontWeight.Black); Text("SANİYE", color = V7Muted, fontSize = 6.sp) } }
            }
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SON HARF", color = V7Text, fontSize = 10.sp)
            Spacer(Modifier.height(5.dp))
            Box(Modifier.size(126.dp).clip(CircleShape).background(Brush.sweepGradient(listOf(V7Blue, V7Purple, V7Magenta, V7Blue))).padding(4.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.fillMaxSize().clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFF17203A), Color(0xFF070D18)))), contentAlignment = Alignment.Center) { Text(required, color = V7Blue, fontSize = 68.sp, fontWeight = FontWeight.Black) }
            }
            Spacer(Modifier.height(8.dp))
            Text(if (last == null) "İlk kelimeyi sen başlat." else "“$required” ile başlayan bir kelime yaz", color = V7Muted, fontSize = 9.sp)
        }
        if (words.isNotEmpty()) LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(words.takeLast(8)) { w -> Surface(shape = RoundedCornerShape(8.dp), color = V7Panel2, border = BorderStroke(1.dp, V7Stroke)) { Text(w.word.uppercase(), Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = V7Text, fontSize = 9.sp, fontWeight = FontWeight.Bold) } }
        }
        if (notice.isNotBlank()) Text(notice, Modifier.fillMaxWidth(), color = if (notice.startsWith("Bu ") || notice.contains("doldu")) V7Red else V7Muted, fontSize = 8.sp, textAlign = TextAlign.Center)
        if (room.status == "quiz" && triviaRound != null && triviaQuestion != null) {
            Card(colors = CardDefaults.cardColors(containerColor = V7Panel), border = BorderStroke(1.dp, V7Purple), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("★ BONUS +${triviaRound.bonusPoints}", color = V7Gold, fontWeight = FontWeight.Black, fontSize = 9.sp)
                    Text(triviaQuestion.question, color = V7Text, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    listOf(triviaQuestion.optionA, triviaQuestion.optionB, triviaQuestion.optionC, triviaQuestion.optionD).forEachIndexed { i, s -> OutlinedButton(onClick = { onTrivia(i) }, modifier = Modifier.fillMaxWidth().heightIn(min = 34.dp)) { Text(s, fontSize = 8.sp) } }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            OutlinedTextField(
                value = wordInput, onValueChange = onWordInput, enabled = myTurn && !busy, singleLine = true, modifier = Modifier.weight(1f).height(54.dp),
                placeholder = { Text(if (myTurn) "Kelimenizi yazın…" else "Rakibin sırası…", fontSize = 10.sp) }, shape = RoundedCornerShape(22.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { if (myTurn && wordInput.isNotBlank() && !busy) { focus.clearFocus(); onSubmit() } })
            )
            Button(onClick = { focus.clearFocus(); onSubmit() }, enabled = myTurn && wordInput.isNotBlank() && !busy, modifier = Modifier.size(50.dp), shape = CircleShape, contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = V7Blue)) { Text("➤", color = Color(0xFF03101A), fontSize = 20.sp) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            OutlinedButton(onClick = onChat, modifier = Modifier.weight(1f).height(40.dp), border = BorderStroke(1.dp, V7Purple)) { Text("● SOHBET", color = V7Blue, fontSize = 8.sp) }
            OutlinedButton(onClick = {}, modifier = Modifier.weight(1f).height(40.dp), border = BorderStroke(1.dp, V7Gold)) { Text("★ BONUS", color = V7Gold, fontSize = 8.sp) }
            OutlinedButton(onClick = onForfeit, modifier = Modifier.weight(1f).height(40.dp), border = BorderStroke(1.dp, V7Red)) { Text("⚑ PES ET", color = V7Red, fontSize = 8.sp) }
        }
    }
}

@Composable private fun V7PlayerCard(role: String, name: String, score: Int, active: Boolean, accent: Color, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = V7Panel), border = BorderStroke(1.dp, if (active) accent else V7Stroke), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            V7Avatar(name, 34.dp, accent); Spacer(Modifier.width(6.dp)); Column { Text(role, color = V7Muted, fontSize = 6.sp); Text(name, color = V7Text, fontSize = 8.sp, maxLines = 1); Text(score.toString(), color = V7Text, fontSize = 20.sp, fontWeight = FontWeight.Black) }
        }
    }
}

@Composable private fun V7Result(playerName: String, opponentName: String, myRounds: Int, oppRounds: Int, won: Boolean, onRematch: () -> Unit, onAddFriend: () -> Unit, onExit: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Card(colors = CardDefaults.cardColors(containerColor = V7Panel), border = BorderStroke(1.dp, V7Stroke), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Text("KAZANAN", color = V7Text, fontWeight = FontWeight.Black, fontSize = 11.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(if (won) "♛" else "", color = V7Gold, fontSize = 20.sp); V7Avatar(playerName, 70.dp, if (won) V7Gold else V7Blue) }
                    Spacer(Modifier.width(16.dp)); Text("$myRounds - $oppRounds", color = V7Text, fontWeight = FontWeight.Black, fontSize = 30.sp); Spacer(Modifier.width(16.dp)); V7Avatar(opponentName, 62.dp, V7Red)
                }
                Text(if (won) playerName else opponentName, color = V7Blue, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("ROUND SONUÇLARI", color = V7Muted, fontSize = 8.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { repeat(3) { i -> Surface(shape = RoundedCornerShape(12.dp), color = V7Panel2, border = BorderStroke(1.dp, V7Stroke)) { Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("${i + 1}. ROUND", color = V7Muted, fontSize = 6.sp); Text(if (i < myRounds) "✓" else "✦", color = if (i < myRounds) V7Green else V7Red, fontSize = 18.sp) } } } }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onRematch, modifier = Modifier.weight(1f)) { Text("⚔ RÖVANŞ İSTE", fontSize = 8.sp) }
                    OutlinedButton(onClick = onAddFriend, modifier = Modifier.weight(1f)) { Text("♟ ARKADAŞ EKLE", fontSize = 8.sp) }
                }
                Button(onClick = onExit, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = V7Blue), shape = RoundedCornerShape(10.dp)) { Text("LOBİYE DÖN", color = Color(0xFF03101A), fontWeight = FontWeight.Black) }
            }
        }
    }
}

@Composable private fun V7ChatDialog(chat: List<ChatMessageDto>, me: String?, input: String, onInput: (String) -> Unit, onClose: () -> Unit, onSend: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("SOHBET", fontWeight = FontWeight.Black); Text("×", Modifier.clickable(onClick = onClose), fontSize = 24.sp) } },
        text = {
            Column(Modifier.heightIn(max = 470.dp)) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(V7Panel2)) {
                    listOf("Maç Sohbeti", "Arkadaşlar", "Sistem").forEachIndexed { i, t -> Text(t, Modifier.weight(1f).background(if (i == 0) V7Purple.copy(alpha = .35f) else Color.Transparent).padding(9.dp), color = if (i == 0) V7Text else V7Muted, textAlign = TextAlign.Center, fontSize = 8.sp) }
                }
                Spacer(Modifier.height(10.dp))
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(chat.takeLast(40)) { m -> Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.senderId == me) Arrangement.End else Arrangement.Start) { Surface(color = if (m.senderId == me) V7Purple.copy(alpha = .22f) else V7Panel2, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, V7Stroke)) { Text(m.body, Modifier.padding(horizontal = 10.dp, vertical = 8.dp), color = V7Text, fontSize = 9.sp) } } }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedTextField(input, onInput, modifier = Modifier.weight(1f), singleLine = true, placeholder = { Text("Mesaj yaz…") }, shape = RoundedCornerShape(18.dp))
                    Button(onClick = onSend, enabled = input.isNotBlank(), modifier = Modifier.size(48.dp), shape = CircleShape, contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = V7Blue)) { Text("➤") }
                }
                Text("Saygılı ve dostça sohbet edin.", Modifier.fillMaxWidth().padding(top = 7.dp), color = V7Muted, fontSize = 7.sp, textAlign = TextAlign.Center)
            }
        },
        confirmButton = {}
    )
}

@Composable private fun V7Avatar(name: String, size: androidx.compose.ui.unit.Dp, accent: Color) { Box(Modifier.size(size).clip(CircleShape).background(Brush.sweepGradient(listOf(accent, V7Blue, V7Purple, accent))).padding(2.dp), contentAlignment = Alignment.Center) { Box(Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFF172135)), contentAlignment = Alignment.Center) { Text(name.take(1).uppercase(), color = V7Text, fontWeight = FontWeight.Black, fontSize = (size.value * .36f).sp) } } }
