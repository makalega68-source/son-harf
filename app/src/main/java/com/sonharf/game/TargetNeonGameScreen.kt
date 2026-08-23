package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
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

private val TGbg = Color(0xFFF4FBFF)
private val TGpanel = Color(0xFFFFFFFF)
private val TGpanel2 = Color(0xFFEAF8FF)
private val TGcyan = Color(0xFF38C7F4)
private val TGpurple = Color(0xFF6ED6F7)
private val TGpink = Color(0xFFFF7891)
private val TGgold = Color(0xFF24AEE4)
private val TGblue = Color(0xFF1799D0)
private val TGgreen = Color(0xFF39D875)
private val TGtext = Color(0xFF173B57)
private val TGmuted = Color(0xFF6D879A)

@Composable
fun TargetNeonGameScreen() {
    if (!SupabaseProvider.configured) {
        Box(Modifier.fillMaxSize().background(TGbg), contentAlignment = Alignment.Center) { Text("Sunucu bağlantısı yok", color = TGtext) }
        return
    }

    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var opponentProfile by remember { mutableStateOf<ProfileDto?>(null) }
    var language by remember { mutableStateOf(SonHarfUiState.language) }
    var room by remember { mutableStateOf<GameRoomDto?>(null) }
    var words by remember { mutableStateOf<List<GameWordDto>>(emptyList()) }
    var chatMessages by remember { mutableStateOf<List<ChatMessageDto>>(emptyList()) }
    var wordInput by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("Düelloya hazır") }
    var busy by remember { mutableStateOf(false) }
    var matching by remember { mutableStateOf(false) }
    var privateCode by remember { mutableStateOf("") }
    var showPrivate by remember { mutableStateOf(false) }
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
        "turn_expired" in raw -> "Süren doldu."
        "vip_required" in raw -> "Özel oda oluşturmak için aktif VIP üyeliği gerekli."
        "player_already_in_game" in raw -> "Devam eden bir maçın varken yeni oda oluşturamazsın."
        "room_not_available" in raw -> "Oda bulunamadı veya artık müsait değil."
        else -> "Bağlantı sorunu. Yeniden deneniyor."
    }

    suspend fun ensureProfile(): ProfileDto {
        if (backend.currentUserId() == null) backend.ensurePlayer("Oyuncu")
        val id = requireNotNull(backend.currentUserId())
        return runCatching { backend.getProfile(id) }.getOrElse { backend.ensurePlayer("Oyuncu") }.also { profile = it }
    }

    suspend fun activeRoom(): GameRoomDto? {
        val me = backend.currentUserId() ?: return null
        val candidates = SupabaseProvider.client.from("game_rooms").select().decodeList<GameRoomDto>()
            .filter { (it.hostId == me || it.guestId == me) && it.status in listOf("waiting", "playing", "quiz", "final", "sudden_death", "paused") }
        val sameLanguage = candidates.filter { it.language == language }
        return (sameLanguage.ifEmpty { candidates }).maxByOrNull { it.validWordCount }
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
        roomJob = scope.launch { backend.observeRoom(r.id).catch { notice = friendly(it.message.orEmpty()) }.collect { room = it; refreshOpponent(it) } }
        wordsJob = scope.launch { backend.observeWords(r.id).catch { notice = friendly(it.message.orEmpty()) }.collect { words = it } }
        chatJob = scope.launch { backend.observeChat(r.id).catch { notice = friendly(it.message.orEmpty()) }.collect { chatMessages = it } }
    }

    LaunchedEffect(Unit) {
        busy = true
        runCatching { ensureProfile() }.onSuccess {
            val old = runCatching { activeRoom() }.getOrNull()
            if (old != null) { room = old; language = old.language; SonHarfUiState.language = old.language; observe(old) }
        }.onFailure { notice = friendly(it.message.orEmpty()) }
        busy = false
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White, TGbg, Color(0xFFE7F6FF))))) {
        val active = room
        if (active == null) {
            TargetLobby(
                playerName = profile?.displayName ?: "Oyuncu",
                language = language,
                matching = matching,
                notice = notice,
                privateCode = privateCode,
                showPrivate = showPrivate,
                onLanguage = { next -> language = next; SonHarfUiState.language = next },
                onPrivateCode = { privateCode = it.filter(Char::isLetterOrDigit).uppercase().take(6) },
                onPrivateToggle = { showPrivate = !showPrivate },
                onRandom = {
                    scope.launch {
                        busy = true
                        runCatching { ensureProfile(); backend.startRandomMatchmaking(language) }
                            .onSuccess { matching = true; notice = "Rakip aranıyor…" }
                            .onFailure {
                                val old = runCatching { activeRoom() }.getOrNull()
                                if (old != null) { room = old; observe(old) } else notice = friendly(it.message.orEmpty())
                            }
                        busy = false
                        if (matching) matchJob = launch {
                            while (matching && room == null) {
                                val found = runCatching { backend.pollRandomMatchmakingRoom() }.getOrNull()
                                if (found != null) { room = found; language = found.language; SonHarfUiState.language = found.language; observe(found); SonHarfSoundFx.softNotify(); break }
                                delay(800)
                            }
                        }
                    }
                },
                onCancel = { scope.launch { matching = false; matchJob?.cancel(); runCatching { backend.cancelRandomMatchmaking() }; notice = "Eşleşme iptal edildi" } },
                onCreate = { scope.launch { busy = true; runCatching { backend.createPrivateRoom(language) }.onSuccess { room = it; notice = "Özel oda oluşturuldu: ${it.code}"; observe(it) }.onFailure { notice = friendly(it.message.orEmpty()) }; busy = false } },
                onJoin = { scope.launch { busy = true; runCatching { backend.joinPrivateRoom(privateCode) }.onSuccess { room = it; language = it.language; SonHarfUiState.language = it.language; observe(it) }.onFailure { notice = friendly(it.message.orEmpty()) }; busy = false } },
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
            TargetArena(
                room = active,
                me = me,
                playerName = profile?.displayName ?: "Sen",
                opponentName = if (active.isBot) "${active.botName ?: "KelimeBot"} BOT" else opponentProfile?.displayName ?: "Rakip",
                playerAvatarPath = profile?.avatarPath,
                playerGender = profile?.gender,
                opponentAvatarPath = opponentProfile?.avatarPath,
                opponentGender = opponentProfile?.gender,
                opponentAvatarVisible = true,
                isVip = profile?.isVip == true,
                words = words,
                chatMessages = chatMessages,
                wordInput = wordInput,
                onWordInput = { wordInput = it.take(40) },
                notice = notice,
                busy = busy,
                onSubmit = {
                    scope.launch {
                        val submitted = wordInput.trim(); if (submitted.isBlank()) return@launch
                        wordInput = ""
                        busy = true
                        runCatching { backend.submitWord(active.id, submitted) }
                            .onSuccess { updated ->
                                room = updated
                                val rejected = updated.lastEventPlayerId == me && updated.lastEvent in setOf(
                                    "word_already_used", "wrong_start_letter", "not_in_dictionary", "invalid_word", "turn_expired"
                                )
                                notice = if (rejected) friendly(updated.lastEvent.orEmpty()) else "${submitted.uppercase()} kabul edildi"
                            }
                            .onFailure { notice = friendly(it.message.orEmpty()) }
                        busy = false
                    }
                },
                onTimeout = { scope.launch { runCatching { backend.claimTurnTimeout(active.id) }.onSuccess { room = it } } },
                onForfeit = {
                    scope.launch {
                        runCatching { backend.forfeit(active.id) }
                            .onSuccess {
                                room = it
                                if (it.status == "finished") SonHarfUiState.homeRequest += 1
                            }
                            .onFailure { notice = friendly(it.message.orEmpty()) }
                    }
                },
                onSendChat = { text -> scope.launch { runCatching { backend.sendChat(active.id, text) }.onFailure { notice = friendly(it.message.orEmpty()) } } },
                onExit = { roomJob?.cancel(); wordsJob?.cancel(); chatJob?.cancel(); room = null; words = emptyList(); chatMessages = emptyList(); notice = "Yeni düelloya hazırsın" },
                onRematch = { scope.launch { runCatching { if (active.isBot) backend.restartBotMatch(active.id) else backend.requestRematch(active.id) }.onSuccess { room = it; words = emptyList(); if (it.id != active.id) observe(it) } } },
            )
        }
    }
}

@Composable
private fun TargetLobby(
    playerName: String,
    language: String,
    matching: Boolean,
    notice: String,
    privateCode: String,
    showPrivate: Boolean,
    onLanguage: (String) -> Unit,
    onPrivateCode: (String) -> Unit,
    onPrivateToggle: () -> Unit,
    onRandom: () -> Unit,
    onCancel: () -> Unit,
    onCreate: () -> Unit,
    onJoin: () -> Unit,
) {
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val privateCompact = showPrivate || imeVisible
    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding().padding(horizontal = 18.dp, vertical = if (privateCompact) 8.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (privateCompact) 8.dp else 12.dp),
    ) {
        Text(if (matching) "RAKİP BULUNUYOR" else "DÜELLO", color = TGtext, fontWeight = FontWeight.Black, fontSize = 18.sp)
        if (matching) {
            Text("RAKİP\nBULUNUYOR!", color = TGcyan, fontWeight = FontWeight.Black, fontSize = 36.sp, textAlign = TextAlign.Center, lineHeight = 38.sp)
            TargetMatchCard(playerName, "Usta", "1250", TGcyan)
            Text("VS", color = TGpurple, fontWeight = FontWeight.Black, fontSize = 42.sp)
            TargetMatchCard("RAKİP ARANIYOR", "…", "", TGpink)
            Spacer(Modifier.weight(1f))
            CircularProgressIndicator(color = TGcyan, strokeWidth = 3.dp)
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().height(50.dp), border = BorderStroke(1.dp, TGpink), shape = RoundedCornerShape(16.dp)) { Text("İPTAL", color = TGpink, fontWeight = FontWeight.Black) }
        } else {
            if (!imeVisible) {
                Card(colors = CardDefaults.cardColors(containerColor = Color.Transparent), shape = RoundedCornerShape(26.dp), border = BorderStroke(1.dp, TGpurple.copy(alpha = .55f))) {
                    Box(Modifier.fillMaxWidth().height(if (privateCompact) 138.dp else 205.dp).background(Brush.radialGradient(listOf(TGpurple.copy(alpha = .28f), TGpanel))), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("SON HARF", color = TGcyan, fontSize = if (privateCompact) 34.sp else 43.sp, fontWeight = FontWeight.Black)
                            Text("CANLI KELİME DÜELLOSU", color = TGtext, fontSize = 9.sp, letterSpacing = 1.1.sp)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = language == "tr", onClick = { onLanguage("tr") }, label = { Text("🇹🇷 TÜRKÇE", maxLines = 1) }, modifier = Modifier.weight(1f))
                    FilterChip(selected = language == "en", onClick = { onLanguage("en") }, label = { Text("🇬🇧 ENGLISH", maxLines = 1) }, modifier = Modifier.weight(1f))
                }
                if (!showPrivate) {
                    Button(onClick = onRandom, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = TGgold, contentColor = Color(0xFF211500)), shape = RoundedCornerShape(17.dp)) { Text("HEMEN OYNA", fontSize = 17.sp, fontWeight = FontWeight.Black) }
                }
                Button(onClick = onPrivateToggle, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = TGblue), shape = RoundedCornerShape(17.dp)) { Text(if (showPrivate) "ÖZEL ODAYI KAPAT" else "ODA KUR / ODAYA KATIL", fontWeight = FontWeight.Black, maxLines = 1) }
            }
            if (showPrivate) {
                Card(modifier = Modifier.fillMaxWidth().weight(1f, fill = false), colors = CardDefaults.cardColors(containerColor = TGpanel), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, TGpurple.copy(alpha = .45f))) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("ÖZEL ODA", color = TGtext, fontWeight = FontWeight.Black, fontSize = 14.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                        Button(onClick = onCreate, modifier = Modifier.fillMaxWidth().height(46.dp), colors = ButtonDefaults.buttonColors(containerColor = TGpurple)) { Text("VIP ODA OLUŞTUR", fontWeight = FontWeight.Black, maxLines = 1) }
                        OutlinedTextField(
                            privateCode,
                            onPrivateCode,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Oda kodu") },
                            placeholder = { Text("6 haneli kod") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                        )
                        OutlinedButton(onClick = onJoin, enabled = privateCode.length == 6, modifier = Modifier.fillMaxWidth().height(46.dp)) { Text("KATIL / ONAYLA", fontWeight = FontWeight.Black) }
                    }
                }
            }
            if (!imeVisible) Text(notice, color = TGmuted, fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

@Composable
private fun TargetMatchCard(name: String, rank: String, score: String, accent: Color) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = TGpanel), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, accent)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            TargetAvatar(name, accent, 54.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(name, color = TGtext, fontWeight = FontWeight.Black, fontSize = 17.sp); Text(rank, color = accent, fontSize = 10.sp) }
            if (score.isNotBlank()) Text("🏆 $score", color = TGgold, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TargetArena(
    room: GameRoomDto,
    me: String?,
    playerName: String,
    opponentName: String,
    playerAvatarPath: String?,
    playerGender: String?,
    opponentAvatarPath: String?,
    opponentGender: String?,
    opponentAvatarVisible: Boolean,
    isVip: Boolean,
    words: List<GameWordDto>,
    chatMessages: List<ChatMessageDto>,
    wordInput: String,
    onWordInput: (String) -> Unit,
    notice: String,
    busy: Boolean,
    onSubmit: () -> Unit,
    onTimeout: () -> Unit,
    onForfeit: () -> Unit,
    onSendChat: (String) -> Unit,
    onExit: () -> Unit,
    onRematch: () -> Unit,
) {
    val host = me == room.hostId
    val myScore = if (host) room.hostScore else room.guestScore
    val oppScore = if (host) room.guestScore else room.hostScore
    val myRounds = if (host) room.hostRounds else room.guestRounds
    val oppRounds = if (host) room.guestRounds else room.hostRounds
    val myTurn = room.currentPlayerId == me && room.status in listOf("playing", "final", "sudden_death")
    var seconds by remember(room.turnDeadline) { mutableStateOf(45) }
    val focus = LocalFocusManager.current
    var showChat by remember { mutableStateOf(false) }
    var showChain by remember { mutableStateOf(false) }
    var showVipNotice by remember { mutableStateOf(false) }
    var chatInput by remember { mutableStateOf("") }
    var confirmForfeit by remember { mutableStateOf(false) }
    DisposableEffect(room.id) { SonHarfUiState.inMatch = true; onDispose { SonHarfUiState.inMatch = false } }
    BackHandler { confirmForfeit = true }

    LaunchedEffect(room.turnDeadline, room.currentPlayerId, room.status) {
        while (room.turnDeadline != null && room.status in listOf("playing", "final", "sudden_death")) {
            seconds = runCatching { (Instant.parse(room.turnDeadline).epochSecond - Instant.now().epochSecond).toInt().coerceAtLeast(0) }.getOrDefault(45)
            if (seconds <= 0) { onTimeout(); break }
            delay(1000)
        }
    }

    if (room.status == "finished") {
        Box(Modifier.fillMaxSize().background(TGbg), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(sh("MAÇ TAMAMLANDI", "MATCH FINISHED"), color = TGtext, fontWeight = FontWeight.Black, fontSize = 24.sp)
                Text(sh("Sonuç özeti açılmazsa ana menüye güvenle dönebilirsin.", "If the result summary does not open, you can safely return home."), color = TGmuted, fontSize = 12.sp, textAlign = TextAlign.Center)
                Button(onClick = onExit, colors = ButtonDefaults.buttonColors(containerColor = TGcyan)) {
                    Text(sh("ANA MENÜ", "HOME"), color = Color.White, fontWeight = FontWeight.Black)
                }
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp).imePadding()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            TargetArenaPlayer(playerName, playerAvatarPath, playerGender, true, myScore, myRounds, myTurn, TGcyan, Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(76.dp).clip(CircleShape).background(Brush.sweepGradient(listOf(TGcyan, TGpurple, TGpink, TGcyan))).padding(3.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.fillMaxSize().clip(CircleShape).background(TGpanel), contentAlignment = Alignment.Center) { Text("$seconds", color = TGtext, fontWeight = FontWeight.Black, fontSize = 28.sp) }
            }
            Spacer(Modifier.width(8.dp))
            TargetArenaPlayer(opponentName, opponentAvatarPath, opponentGender, opponentAvatarVisible, oppScore, oppRounds, !myTurn, TGpink, Modifier.weight(1f))
        }

        Spacer(Modifier.height(14.dp))
        Text("TUR ${room.roundNo}/3", color = TGtext, fontWeight = FontWeight.Black, fontSize = 13.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(12.dp))

        Card(colors = CardDefaults.cardColors(containerColor = TGpanel), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, TGpurple.copy(alpha = .35f))) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (words.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(190.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (myTurn) "SIRA SENDE" else "RAKİBİN SIRASI", color = if (myTurn) TGcyan else TGpink, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Spacer(Modifier.height(24.dp))
                            Text("İLK KELİME", color = TGtext, fontWeight = FontWeight.Black, fontSize = 40.sp)
                            Text("İlk kelimeyi yaz", color = TGmuted, fontSize = 12.sp)
                        }
                    }
                } else {
                    words.takeLast(5).forEachIndexed { index, w ->
                        val lastChar = w.normalizedWord.lastOrNull()?.uppercaseChar()?.toString().orEmpty()
                        val selected = index == words.takeLast(5).lastIndex
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (selected) TGgold.copy(alpha = .06f) else Color.Transparent).padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val wordSize = when { w.word.length >= 18 -> 15.sp; w.word.length >= 14 -> 17.sp; w.word.length >= 10 -> 20.sp; else -> 23.sp }
                            Text(w.word.uppercase(), color = TGtext, fontSize = wordSize, letterSpacing = if (w.word.length >= 14) .3.sp else 1.2.sp, maxLines = 1, modifier = Modifier.weight(1f))
                            Surface(color = Color.Transparent, shape = RoundedCornerShape(5.dp), border = BorderStroke(1.dp, if (selected) TGgold else TGcyan)) {
                                Text(lastChar, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = TGtext, fontWeight = FontWeight.Black, fontSize = 18.sp)
                            }
                            Spacer(Modifier.width(12.dp)); Text("✓", color = TGgreen, fontWeight = FontWeight.Black, fontSize = 22.sp)
                        }
                        if (index != words.takeLast(5).lastIndex) HorizontalDivider(color = Color.White.copy(alpha = .08f))
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TargetPower("💡", "JOKER", "3", TGgold, Modifier.weight(1f))
            TargetPower("⤨", "KARIŞTIR", "2", TGcyan, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VipLockedAction("💬 SOHBET", isVip, { showChat = true }, { showVipNotice = true }, Modifier.weight(1f))
            VipLockedAction("⛓ KELİME ZİNCİRİ", isVip, { showChain = true }, { showVipNotice = true }, Modifier.weight(1f))
        }
        Spacer(Modifier.weight(1f))

        OutlinedTextField(
            value = wordInput,
            onValueChange = onWordInput,
            enabled = myTurn && !busy,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(if (myTurn) "Kelimenizi yazın…" else "Rakibin sırası…") },
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Send,
                showKeyboardOnFocus = true,
                hintLocales = LocaleList(Locale(if (room.language == "tr") "tr-TR" else "en-US")),
            ),
            keyboardActions = KeyboardActions(onSend = { if (myTurn && wordInput.isNotBlank() && !busy) { focus.clearFocus(); onSubmit() } }),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TGcyan, unfocusedBorderColor = Color.White.copy(alpha = .16f), focusedTextColor = TGtext, unfocusedTextColor = TGtext),
        )
        Spacer(Modifier.height(10.dp))
        Button(onClick = { focus.clearFocus(); onSubmit() }, enabled = myTurn && wordInput.isNotBlank() && !busy, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = TGgold, contentColor = Color(0xFF211500)), shape = RoundedCornerShape(14.dp)) { Text("GÖNDER", fontWeight = FontWeight.Black, fontSize = 16.sp) }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { confirmForfeit = true }, modifier = Modifier.weight(1f), border = BorderStroke(1.dp, TGpink)) { Text("⚑ PES ET", color = TGpink, fontSize = 10.sp) }
            Surface(modifier = Modifier.weight(1f), color = TGpanel, shape = RoundedCornerShape(12.dp)) { Text(notice, Modifier.padding(12.dp), color = TGmuted, fontSize = 9.sp, textAlign = TextAlign.Center) }
        }

        if (confirmForfeit) {
            AlertDialog(
                onDismissRequest = { confirmForfeit = false },
                icon = {
                    Surface(shape = CircleShape, color = TGpink.copy(alpha = .12f)) {
                        Text("⚑", modifier = Modifier.padding(14.dp), color = TGpink, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    }
                },
                title = { Text(sh("Maçtan çıkılsın mı?", "Leave the match?"), fontWeight = FontWeight.Black, textAlign = TextAlign.Center) },
                text = { Text(sh("Pes edersen maç rakibin lehine tamamlanır. Bu işlem geri alınamaz.", "If you forfeit, the match ends in your opponent's favor. This cannot be undone."), textAlign = TextAlign.Center) },
                confirmButton = {
                    Button(
                        onClick = { confirmForfeit = false; onForfeit() },
                        colors = ButtonDefaults.buttonColors(containerColor = TGpink),
                        shape = RoundedCornerShape(14.dp),
                    ) { Text(sh("PES ET VE ÇIK", "FORFEIT & LEAVE"), color = Color.White, fontWeight = FontWeight.Black) }
                },
                dismissButton = {
                    OutlinedButton(onClick = { confirmForfeit = false }, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, TGcyan.copy(alpha = .55f))) {
                        Text(sh("OYUNA DÖN", "RETURN TO GAME"), color = TGblue, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = TGpanel,
                titleContentColor = TGtext,
                textContentColor = TGmuted,
                shape = RoundedCornerShape(28.dp),
            )
        }
        if (showVipNotice) {
            AlertDialog(
                onDismissRequest = { showVipNotice = false },
                confirmButton = { TextButton(onClick = { showVipNotice = false }) { Text("TAMAM") } },
                title = { Text("VIP ÖZELLİĞİ") },
                text = { Text("Oyun içi sohbet ve tam kelime zinciri VIP üyelerine özeldir.") },
            )
        }
        if (showChain) {
            AlertDialog(
                onDismissRequest = { showChain = false },
                confirmButton = { TextButton(onClick = { showChain = false }) { Text("KAPAT") } },
                title = { Text("KELİME ZİNCİRİ") },
                text = {
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                        items(words.takeLast(30)) { w ->
                            Text(w.word.uppercase(), modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp), color = TGtext, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            HorizontalDivider(color = Color.White.copy(alpha = .08f))
                        }
                    }
                },
            )
        }
        if (showChat) {
            AlertDialog(
                onDismissRequest = { showChat = false },
                confirmButton = { TextButton(onClick = { showChat = false }) { Text("KAPAT") } },
                title = { Text("OYUN SOHBETİ") },
                text = {
                    Column(Modifier.fillMaxWidth().heightIn(max = 440.dp)) {
                        LazyColumn(Modifier.weight(1f, fill = false).fillMaxWidth().heightIn(min = 120.dp, max = 300.dp)) {
                            items(chatMessages.takeLast(40)) { message ->
                                val mine = message.senderId == me
                                Column(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
                                    Text(if (mine) "Sen" else opponentName, color = if (mine) TGcyan else TGpink, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Surface(color = if (mine) TGcyan.copy(alpha = .12f) else TGpink.copy(alpha = .12f), shape = RoundedCornerShape(10.dp)) {
                                        Text(message.body, Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = TGtext, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = chatInput,
                            onValueChange = { chatInput = it.take(300) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("Mesaj yaz…") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Send,
                                showKeyboardOnFocus = true,
                                hintLocales = LocaleList(Locale(if (room.language == "tr") "tr-TR" else "en-US")),
                            ),
                            keyboardActions = KeyboardActions(onSend = {
                                val text = chatInput.trim()
                                if (text.isNotEmpty()) { onSendChat(text); chatInput = "" }
                            }),
                        )
                        Spacer(Modifier.height(6.dp))
                        Button(
                            onClick = { val text = chatInput.trim(); if (text.isNotEmpty()) { onSendChat(text); chatInput = "" } },
                            enabled = chatInput.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("GÖNDER") }
                    }
                },
            )
        }
    }
}

@Composable private fun TargetArenaPlayer(name: String, avatarPath: String?, gender: String?, avatarVisible: Boolean, score: Int, rounds: Int, active: Boolean, accent: Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            ProfilePhotoAvatar(avatarPath, name, 48.dp, visible = avatarVisible, accent = accent)
            val normalizedGender = gender?.trim()?.lowercase()
            val female = normalizedGender in setOf("kadın", "kadin", "female", "woman")
            val male = normalizedGender in setOf("erkek", "male", "man")
            if (female || male) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).size(17.dp),
                    shape = CircleShape,
                    color = if (female) Color(0xFFFF76A8) else Color(0xFF439EF2),
                    border = BorderStroke(1.dp, Color.White),
                ) { Box(contentAlignment = Alignment.Center) { Text(if (female) "♀" else "♂", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black) } }
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(name, color = TGtext, fontWeight = FontWeight.Black, fontSize = 10.sp, maxLines = 1)
        Text("🏆 $score", color = TGgold, fontSize = 8.sp)
        Text("$rounds round", color = if (active) accent else TGmuted, fontSize = 8.sp)
    }
}

@Composable private fun TargetPower(icon: String, label: String, count: String, accent: Color, modifier: Modifier) {
    Surface(modifier = modifier, color = TGpanel, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, accent.copy(alpha = .28f))) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(icon); Spacer(Modifier.width(6.dp)); Text(label, color = TGtext, fontSize = 9.sp, fontWeight = FontWeight.Black); Spacer(Modifier.width(6.dp)); Text(count, color = accent, fontWeight = FontWeight.Black)
        }
    }
}

@Composable private fun TargetWinner(won: Boolean, playerName: String, opponentName: String, myScore: Int, oppScore: Int, onRematch: () -> Unit, onExit: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        Card(colors = CardDefaults.cardColors(containerColor = TGpanel), shape = RoundedCornerShape(30.dp), border = BorderStroke(1.dp, TGpurple.copy(alpha = .55f))) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (won) "KAZANDIN!" else "MAÇ BİTTİ", color = if (won) TGgold else TGpink, fontWeight = FontWeight.Black, fontSize = 42.sp)
                Spacer(Modifier.height(20.dp)); Text("🏆", fontSize = 94.sp)
                Spacer(Modifier.height(12.dp)); Text(if (won) playerName else opponentName, color = TGtext, fontWeight = FontWeight.Black, fontSize = 22.sp)
                Spacer(Modifier.height(16.dp))
                Surface(color = TGbg, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = .10f))) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Sen", color = TGmuted); Text("+$myScore", color = TGtext, fontWeight = FontWeight.Black) }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Rakip", color = TGmuted); Text("+$oppScore", color = TGtext, fontWeight = FontWeight.Black) }
                        HorizontalDivider(color = Color.White.copy(alpha = .10f))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("TOPLAM", color = TGcyan, fontWeight = FontWeight.Black); Text(if (won) "+25 🏆  +10 ◆" else "+5 ◆", color = TGgold, fontWeight = FontWeight.Black) }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Button(onClick = onRematch, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = TGgold, contentColor = Color(0xFF211500)), shape = RoundedCornerShape(14.dp)) { Text("TEKRAR OYNA", fontWeight = FontWeight.Black) }
                Spacer(Modifier.height(10.dp))
                Button(onClick = onExit, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = TGpurple), shape = RoundedCornerShape(14.dp)) { Text("ANA MENÜ", fontWeight = FontWeight.Black) }
            }
        }
    }
}

@Composable private fun TargetAvatar(name: String, accent: Color, size: androidx.compose.ui.unit.Dp) {
    Box(Modifier.size(size).clip(CircleShape).background(Brush.sweepGradient(listOf(TGcyan, TGpurple, TGpink, TGgold, TGcyan))).padding(3.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().clip(CircleShape).background(TGpanel2), contentAlignment = Alignment.Center) { Text(name.take(1).uppercase(), color = TGtext, fontWeight = FontWeight.Black, fontSize = (size.value * .38f).sp) }
    }
}
