package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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

private val TGbg = Color(0xFFF7F9FC)
private val TGpanel = Color.White
private val TGpanel2 = Color(0xFFF0F4F8)
private val TGcyan = Color(0xFF1769E0)
private val TGpurple = Color(0xFF6B4FD3)
private val TGpink = Color(0xFFE95B72)
private val TGgold = Color(0xFFF3A81A)
private val TGblue = Color(0xFF1769E0)
private val TGgreen = Color(0xFF22B95F)
private val TGtext = Color(0xFF182235)
private val TGmuted = Color(0xFF718096)

@Composable
fun TargetNeonGameScreen(
    autoStartMatchmaking: Boolean = false,
) {
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
    var autoStartConsumed by remember(autoStartMatchmaking) { mutableStateOf(false) }

    fun friendly(raw: String) = when {
        "not_your_turn" in raw -> "Sıra rakibinde."
        "word_already_used" in raw -> "Bu kelime daha önce kullanıldı."
        "wrong_start_letter" in raw -> "Kelime son harfle başlamalı."
        "not_in_dictionary" in raw -> "Bu kelime sözlükte bulunamadı."
        "invalid_word" in raw -> "Bu kelime geçerli değil."
        "turn_expired" in raw -> "Süren doldu."
        "vip_required" in raw -> "Özel oda oluşturmak için aktif VIP üyeliği gerekli."
        "team_arena_active" in raw || "team_arena_already_active" in raw ->
            "Takım Arenası maçın sürüyor. Önce 2v2 maçı bitir."
        "word_arena_match_active" in raw -> "Aktif Kelime Arenası maçını bitir."
        "daily_arena_active" in raw -> "Aktif Resmî Koşuyu bitir."
        "player_already_in_game" in raw -> "Devam eden bir maçın varken yeni oda oluşturamazsın."
        "room_not_available" in raw -> "Oda bulunamadı veya artık müsait değil."
        "timeout" in raw.lowercase() || "unreachable" in raw.lowercase() || "connect" in raw.lowercase() ->
            "Sunucuya ulaşılamadı. Yeniden deneniyor."
        else -> "Düello şu anda başlatılamadı. Tekrar dene."
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
        notice = sh("Düello başladı", "Duel started")
        scope.launch { refreshOpponent(r) }
        roomJob = scope.launch { backend.observeRoom(r.id).catch { notice = friendly(it.message.orEmpty()) }.collect { room = it; refreshOpponent(it) } }
        wordsJob = scope.launch { backend.observeWords(r.id).catch { notice = friendly(it.message.orEmpty()) }.collect { words = it } }
        chatJob = scope.launch { backend.observeChat(r.id).catch { notice = friendly(it.message.orEmpty()) }.collect { chatMessages = it } }
    }

    fun startRandomSearch() {
        if (busy || matching || room != null) return
        scope.launch {
            busy = true
            runCatching {
                ensureProfile()
                backend.startRandomMatchmaking(language)
            }.onSuccess {
                matching = true
                notice = sh("Rakip aranıyor…", "Searching for an opponent…")
            }.onFailure {
                val old = runCatching { activeRoom() }.getOrNull()
                if (old != null) {
                    room = old
                    observe(old)
                } else {
                    notice = friendly(it.message.orEmpty())
                }
            }
            busy = false
            if (matching && room == null) {
                matchJob?.cancel()
                matchJob = scope.launch {
                    while (matching && room == null) {
                        val found = runCatching { backend.pollRandomMatchmakingRoom() }.getOrNull()
                        if (found != null) {
                            room = found
                            language = found.language
                            SonHarfUiState.language = found.language
                            observe(found)
                            SonHarfSoundFx.softNotify()
                            break
                        }
                        delay(800)
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        busy = true
        runCatching { ensureProfile() }.onSuccess {
            val old = runCatching { activeRoom() }.getOrNull()
            if (old != null) { room = old; language = old.language; SonHarfUiState.language = old.language; observe(old) }
        }.onFailure {
            // Passive preload failures should not look like a connection outage before
            // the player has attempted an online action.
            notice = sh("Düelloya hazır", "Ready to duel")
        }
        busy = false
    }

    LaunchedEffect(autoStartMatchmaking, profile?.id, room?.id, busy) {
        if (autoStartMatchmaking && !autoStartConsumed && profile != null && room == null && !busy) {
            autoStartConsumed = true
            startRandomSearch()
        }
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White, TGbg, Color(0xFFF1F6FC))))) {
        val active = room
        if (active == null) {
            TargetLobby(
                playerName = profile?.displayName ?: "Oyuncu",
                playerAvatarPath = profile?.avatarPath,
                playerGender = profile?.gender,
                language = language,
                matching = matching,
                busy = busy,
                notice = notice,
                privateCode = privateCode,
                showPrivate = showPrivate,
                onLanguage = { next -> language = next; SonHarfUiState.language = next },
                onPrivateCode = { privateCode = it.filter(Char::isLetterOrDigit).uppercase().take(6) },
                onPrivateToggle = { showPrivate = !showPrivate },
                onRandom = { startRandomSearch() },
                onCancel = { scope.launch { matching = false; matchJob?.cancel(); runCatching { backend.cancelRandomMatchmaking() }; notice = "Eşleşme iptal edildi" } },
                onCreate = { scope.launch { busy = true; runCatching { backend.createPrivateRoom(language) }.onSuccess { room = it; notice = "Özel oda oluşturuldu: ${it.code}"; observe(it) }.onFailure { notice = friendly(it.message.orEmpty()) }; busy = false } },
                onJoin = { scope.launch { busy = true; runCatching { backend.joinPrivateRoom(privateCode) }.onSuccess { room = it; language = it.language; SonHarfUiState.language = it.language; observe(it) }.onFailure { notice = friendly(it.message.orEmpty()) }; busy = false } },
            )
        } else {
            val me = backend.currentUserId()
            LaunchedEffect(active.currentPlayerId, active.validWordCount, active.roundNo) { wordInput = "" }
            LaunchedEffect(active.id, active.status, active.isBot) {
                while (room?.id == active.id) {
                    val current = room ?: break
                    if (current.status == "finished" || current.status == "cancelled") break
                    if (!current.isBot && current.status != "waiting") {
                        runCatching { backend.heartbeatRoom(current.id) }
                            .onSuccess { room = it }
                            .onFailure { notice = friendly(it.message.orEmpty()) }
                    }
                    delay(5000)
                }
            }
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
                myRating = profile?.rating,
                opponentRating = opponentProfile?.rating,
                words = words,
                chatMessages = chatMessages,
                wordInput = wordInput,
                onWordInput = {
                    val next = it.take(40)
                    if (next.length > wordInput.length) SonHarfSoundFx.typingClick()
                    wordInput = next
                },
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
                onExit = {
                    matching = false
                    matchJob?.cancel()
                    roomJob?.cancel()
                    wordsJob?.cancel()
                    chatJob?.cancel()
                    room = null
                    words = emptyList()
                    chatMessages = emptyList()
                    notice = sh("Yeni düelloya hazırsın", "Ready for a new duel")
                },
                onRematch = {
                    scope.launch {
                        busy = true
                        runCatching { if (active.isBot) backend.restartBotMatch(active.id) else backend.requestRematch(active.id) }
                            .onSuccess { room = it; words = emptyList(); if (it.id != active.id) observe(it) }
                            .onFailure { notice = friendly(it.message.orEmpty()) }
                        busy = false
                    }
                },
            )
        }
    }
}

@Composable
private fun TargetLobby(
    playerName: String,
    playerAvatarPath: String?,
    playerGender: String?,
    language: String,
    matching: Boolean,
    busy: Boolean,
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
            TargetMatchCard(playerName, playerAvatarPath, playerGender, "Usta", "1250", TGcyan)
            Text("VS", color = TGpurple, fontWeight = FontWeight.Black, fontSize = 42.sp)
            TargetMatchCard("RAKİP ARANIYOR", null, null, "…", "", TGpink)
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
                    Button(onClick = onRandom, enabled = !busy, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = TGgold, contentColor = Color(0xFF211500)), shape = RoundedCornerShape(17.dp)) { Text(if (busy) "…" else "HEMEN OYNA", fontSize = 17.sp, fontWeight = FontWeight.Black) }
                }
                Button(onClick = onPrivateToggle, enabled = !busy, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = TGblue), shape = RoundedCornerShape(17.dp)) { Text(if (showPrivate) "ÖZEL ODAYI KAPAT" else "ODA KUR / ODAYA KATIL", fontWeight = FontWeight.Black, maxLines = 1) }
            }
            if (showPrivate) {
                Card(modifier = Modifier.fillMaxWidth().weight(1f, fill = false), colors = CardDefaults.cardColors(containerColor = TGpanel), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, TGpurple.copy(alpha = .45f))) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("ÖZEL ODA", color = TGtext, fontWeight = FontWeight.Black, fontSize = 14.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                        Button(onClick = onCreate, enabled = !busy, modifier = Modifier.fillMaxWidth().height(46.dp), colors = ButtonDefaults.buttonColors(containerColor = TGpurple)) { Text(if (busy) "…" else "VIP ODA OLUŞTUR", fontWeight = FontWeight.Black, maxLines = 1) }
                        OutlinedTextField(
                            privateCode,
                            onPrivateCode,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Oda kodu") },
                            placeholder = { Text("6 haneli kod") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                        )
                        OutlinedButton(onClick = onJoin, enabled = privateCode.length == 6 && !busy, modifier = Modifier.fillMaxWidth().height(46.dp)) { Text(if (busy) "…" else "KATIL / ONAYLA", fontWeight = FontWeight.Black) }
                    }
                }
            }
            if (!imeVisible) Text(notice, color = TGmuted, fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

@Composable
private fun TargetMatchCard(
    name: String,
    avatarPath: String?,
    gender: String?,
    rank: String,
    score: String,
    accent: Color,
) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = TGpanel), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, accent)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            ProfilePhotoAvatarWithGender(
                avatarPath = avatarPath,
                gender = gender,
                name = name,
                size = 54.dp,
                accent = accent,
            )
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
    myRating: Int?,
    opponentRating: Int?,
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
    val keyboard = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val wordFocusRequester = remember { FocusRequester() }
    var showChat by remember { mutableStateOf(false) }
    var showChain by remember { mutableStateOf(false) }
    var chatInput by remember { mutableStateOf("") }
    var confirmForfeit by remember { mutableStateOf(false) }
    DisposableEffect(room.id) {
        SonHarfUiState.inMatch = true
        onDispose { SonHarfUiState.inMatch = false }
    }

    BackHandler(enabled = room.status != "finished") { confirmForfeit = true }

    LaunchedEffect(room.id, room.status, myTurn, busy) {
        if (room.status in listOf("playing", "final", "sudden_death")) {
            delay(140)
            runCatching { wordFocusRequester.requestFocus() }
            keyboard?.show()
        } else {
            keyboard?.hide()
        }
    }

    LaunchedEffect(room.turnDeadline, room.currentPlayerId, room.status) {
        while (room.turnDeadline != null && room.status in listOf("playing", "final", "sudden_death")) {
            seconds = runCatching {
                (Instant.parse(room.turnDeadline).epochSecond - Instant.now().epochSecond).toInt().coerceAtLeast(0)
            }.getOrDefault(45)
            if (seconds <= 0) {
                onTimeout()
                break
            }
            delay(1000)
        }
    }

    if (room.status == "finished") {
        BackHandler { onExit() }
        val won = room.winnerId == me
        val draw = room.winnerId == null
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFFFFBF2), TGbg)))
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(shape = CircleShape, color = TGgold.copy(alpha = .12f)) {
                Icon(Icons.Rounded.EmojiEvents, null, tint = TGgold, modifier = Modifier.padding(13.dp).size(32.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                if (draw) sh("BERABERE", "DRAW") else if (won) sh("ZAFER", "VICTORY") else sh("MAÇ TAMAMLANDI", "MATCH COMPLETE"),
                color = if (won) TGgold else TGtext,
                fontWeight = FontWeight.Black,
                fontSize = if (won) 42.sp else 30.sp,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Text("$myRounds", color = TGblue, fontSize = 42.sp, fontWeight = FontWeight.Black)
                Text("—", color = TGtext, fontSize = 34.sp, fontWeight = FontWeight.Light)
                Text("$oppRounds", color = TGpink, fontSize = 42.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(18.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = TGpanel,
                border = BorderStroke(1.dp, Color(0xFFDDE5EE)),
                shadowElevation = 3.dp,
            ) {
                Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(sh("Senin maç puanın", "Your match score"), color = TGmuted, fontSize = 11.sp)
                        Text("$myScore", color = TGgreen, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(sh("Rakip maç puanı", "Rival match score"), color = TGmuted, fontSize = 11.sp)
                        Text("$oppScore", color = TGpink, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                    HorizontalDivider(color = Color(0xFFE7ECF2))
                    Text(
                        if (draw) sh("Beraberlikte rating değişmez.", "Rating does not change on a draw.")
                        else sh("Lig ve rating sonucu hesabına işlendi.", "League and rating result was applied to your account."),
                        color = TGmuted,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onRematch,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TGblue),
            ) {
                Text(sh("RÖVANŞ", "REMATCH"), color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
            Spacer(Modifier.height(9.dp))
            OutlinedButton(
                onClick = onExit,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, TGblue.copy(alpha = .45f)),
            ) {
                Text(sh("YENİ RAKİP", "NEW RIVAL"), color = TGblue, fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
        }
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = TGpanel,
            border = BorderStroke(1.dp, Color(0xFFDDE5EE)),
            shadowElevation = 2.dp,
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    ProfilePhotoAvatarWithGender(
                        avatarPath = playerAvatarPath,
                        gender = playerGender,
                        name = playerName,
                        size = 36.dp,
                        accent = TGblue,
                    )
                    Spacer(Modifier.width(7.dp))
                    Column {
                        Text(playerName, color = TGblue, fontWeight = FontWeight.Black, fontSize = 11.sp, maxLines = 1)
                        Text("🏆 $myScore • $myRounds " + sh("tur", "round"), color = TGmuted, fontSize = 8.sp, maxLines = 1)
                    }
                }
                Box(
                    Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Brush.sweepGradient(listOf(TGblue, TGblue, TGpink, TGpink)))
                        .padding(4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.fillMaxSize().clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$seconds", color = TGtext, fontWeight = FontWeight.Black, fontSize = 28.sp)
                            Text(sh("sn", "sec"), color = TGmuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Row(
                    Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(opponentName, color = TGpink, fontWeight = FontWeight.Black, fontSize = 11.sp, maxLines = 1)
                        Text("🏆 $oppScore • $oppRounds " + sh("tur", "round"), color = TGmuted, fontSize = 8.sp, maxLines = 1)
                    }
                    Spacer(Modifier.width(7.dp))
                    ProfilePhotoAvatarWithGender(
                        avatarPath = opponentAvatarPath,
                        gender = opponentGender,
                        name = opponentName,
                        size = 36.dp,
                        accent = TGpink,
                    )
                }
            }
        }

        Spacer(Modifier.height(7.dp))
        CompetitionLeadStrip(
            myScore = myScore,
            opponentScore = oppScore,
            myStreak = if (host) room.hostStreak else room.guestStreak,
            opponentStreak = if (host) room.guestStreak else room.hostStreak,
            myAction = if (room.lastEventPlayerId == me) sh("Hamlen skor tabelasına işlendi.", "Your move changed the scoreboard.") else null,
            opponentAction = if (room.lastEventPlayerId != null && room.lastEventPlayerId != me) sh("Rakip hamle yaptı.", "Rival made a move.") else null,
        )
        Spacer(Modifier.height(7.dp))
        CompetitionMatchIntro(
            key = room.id,
            myName = playerName,
            opponentName = opponentName,
            myAvatarPath = playerAvatarPath,
            opponentAvatarPath = opponentAvatarPath,
            myGender = playerGender,
            opponentGender = opponentGender,
            myRating = myRating,
            opponentRating = opponentRating,
        )
        Spacer(Modifier.height(7.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("TUR ${room.roundNo}/3", color = TGmuted, fontWeight = FontWeight.Black, fontSize = 10.sp)
            Surface(shape = RoundedCornerShape(99.dp), color = (if (myTurn) TGblue else TGpink).copy(alpha = .08f)) {
                Text(
                    if (myTurn) sh("SIRA SENDE", "YOUR TURN") else sh("SIRA RAKİPTE", "RIVAL'S TURN"),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = if (myTurn) TGblue else TGpink,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        val activeLetter = words.lastOrNull()?.normalizedWord?.lastOrNull()?.uppercaseChar()?.toString().orEmpty()
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = true),
            colors = CardDefaults.cardColors(containerColor = TGpanel),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, Color(0xFFDDE5EE)),
        ) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (words.isEmpty()) {
                    Text(if (myTurn) sh("İLK KELİME", "FIRST WORD") else sh("RAKİP BAŞLIYOR", "RIVAL STARTS"), color = TGtext, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    Text(sh("Kelime zincirini başlat", "Start the word chain"), color = TGmuted, fontSize = 11.sp)
                } else {
                    // The arena only shows the current word. Full history stays in
                    // the Word Chain sheet without pushing the gameplay layout.
                    val current = words.last()
                    val upper = current.word.uppercase()
                    val prefix = upper.dropLast(1)
                    val last = upper.takeLast(1)
                    Text(sh("SON KELİME", "CURRENT WORD"), color = TGmuted, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(prefix, color = TGtext, fontSize = if (upper.length > 12) 23.sp else 30.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                        Text(last, color = TGblue, fontSize = if (upper.length > 12) 23.sp else 30.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    if (activeLetter.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = TGblue.copy(alpha = .07f),
                            border = BorderStroke(1.dp, TGblue.copy(alpha = .24f)),
                            shadowElevation = 1.dp,
                        ) {
                            Row(
                                Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Column {
                                    Text(sh("SON HARF", "LAST LETTER"), color = TGmuted, fontSize = 7.sp, fontWeight = FontWeight.Black)
                                    Text(sh("Sıradaki kelime bununla başlar", "Next word starts here"), color = TGmuted, fontSize = 7.sp)
                                }
                                Surface(shape = RoundedCornerShape(12.dp), color = TGblue) {
                                    Text(
                                        activeLetter,
                                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 4.dp),
                                        color = Color.White,
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Black,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { showChat = true },
                enabled = !room.isBot,
                modifier = Modifier.weight(1f),
                border = BorderStroke(1.dp, TGcyan.copy(alpha = .55f)),
            ) { Text(if (room.isBot) sh("BOTTA SOHBET YOK", "NO BOT CHAT") else sh("💬 SOHBET", "💬 CHAT"), color = TGcyan, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
            OutlinedButton(
                onClick = { showChain = true },
                modifier = Modifier.weight(1f),
                border = BorderStroke(1.dp, TGblue.copy(alpha = .45f)),
            ) { Text(sh("⛓ KELİME ZİNCİRİ", "⛓ WORD CHAIN"), color = TGblue, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
        }
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = wordInput,
            onValueChange = { value -> if (myTurn && !busy) onWordInput(value) },
            enabled = true,
            singleLine = true,
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).focusRequester(wordFocusRequester),
            textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
            label = { Text(sh("Kelimen", "Your word"), fontSize = 12.sp) },
            placeholder = {
                Text(
                    if (!myTurn) sh("Rakibin sırası…", "Rival's turn…")
                    else if (activeLetter.isBlank()) sh("İlk kelimeyi yaz…", "Type the first word…")
                    else activeLetter + sh(" ile başlayan kelime yaz", " — type a word"),
                    fontSize = 14.sp,
                )
            },
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Send,
                showKeyboardOnFocus = true,
                hintLocales = LocaleList(Locale(if (room.language == "tr") "tr-TR" else "en-US")),
            ),
            keyboardActions = KeyboardActions(onSend = { if (myTurn && wordInput.isNotBlank() && !busy) onSubmit() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TGcyan,
                unfocusedBorderColor = Color(0xFFD5DEE9),
                focusedTextColor = TGtext,
                unfocusedTextColor = TGtext,
                cursorColor = TGcyan,
            ),
        )
        Spacer(Modifier.height(7.dp))
        Button(
            onClick = { onSubmit() },
            enabled = myTurn && wordInput.isNotBlank() && !busy,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TGblue,
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFE2E7EE),
                disabledContentColor = Color(0xFF8A94A3),
            ),
            shape = RoundedCornerShape(16.dp),
        ) { Text(sh("GÖNDER", "SEND"), fontWeight = FontWeight.Black, fontSize = 17.sp) }
        if (!imeVisible) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { confirmForfeit = true },
                    modifier = Modifier.weight(1f).height(46.dp),
                    border = BorderStroke(1.dp, TGpink),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("⚑ PES ET", color = TGpink, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                Surface(
                    modifier = Modifier.weight(1f).heightIn(min = 46.dp),
                    color = TGpanel2,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFD5DEE9)),
                ) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Text(notice, color = TGmuted, fontSize = 10.sp, lineHeight = 13.sp, textAlign = TextAlign.Center, maxLines = 2)
                    }
                }
            }
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
        if (showChain) {
            AlertDialog(
                onDismissRequest = { showChain = false },
                confirmButton = { TextButton(onClick = { showChain = false }) { Text("KAPAT") } },
                title = { Text("KELİME ZİNCİRİ") },
                text = {
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                        items(words.takeLast(30)) { w ->
                            Text(w.word.uppercase(), modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp), color = TGtext, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            HorizontalDivider(color = TGmuted.copy(alpha = .14f))
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
