package com.sonharf.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
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

@Composable
fun OnlineGameScreenV6() {
    if (!SupabaseProvider.configured) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(sh("Sunucu bağlantısı yapılandırılmamış.", "Server connection is not configured.")) }
        return
    }
    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var opponentProfile by remember { mutableStateOf<ProfileDto?>(null) }
    var language by remember { mutableStateOf(SonHarfUiState.language) }
    var room by remember { mutableStateOf<GameRoomDto?>(null) }
    var words by remember { mutableStateOf<List<GameWordDto>>(emptyList()) }
    var chat by remember { mutableStateOf<List<ChatMessageDto>>(emptyList()) }
    var triviaRound by remember { mutableStateOf<TriviaRoundDto?>(null) }
    var triviaQuestion by remember { mutableStateOf<TriviaQuestionDto?>(null) }
    var wordInput by remember { mutableStateOf("") }
    var chatInput by remember { mutableStateOf("") }
    var privateCode by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf(sh("Hazır", "Ready")) }
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
        "player_already_in_game" in raw -> sh("Aktif maçına dönülüyor…", "Returning to your active match…")
        "not_your_turn" in raw -> sh("Sıra rakibinde.", "It is your opponent's turn.")
        "word_already_used" in raw -> sh("Bu kelime daha önce kullanıldı.", "This word has already been used.")
        "wrong_start_letter" in raw -> sh("Kelime son harfle başlamalı.", "The word must start with the last letter.")
        "not_in_dictionary" in raw -> sh("Bu kelime sözlükte bulunamadı.", "This word was not found in the dictionary.")
        "invalid_word" in raw -> sh("Bu kelime geçerli değil.", "This word is not valid.")
        "ends_with_soft_g" in raw -> sh("Ğ ile biten kelimeler kullanılamaz.", "Words ending with Ğ cannot be used.")
        "turn_expired" in raw -> sh("Süren doldu. −1 puan.", "Your time expired. −1 point.")
        "vip_required" in raw -> sh("Özel oda açmak için VIP gerekli.", "VIP is required to create a private room.")
        else -> sh("İşlem tekrar deneniyor.", "Retrying the action.")
    }
    fun failedEvent(e: String?) = e in setOf("word_already_used", "wrong_start_letter", "not_in_dictionary", "invalid_word", "ends_with_soft_g", "turn_expired")
    fun eventMessage(e: String?) = when (e) {
        "word_already_used" -> sh("Bu kelime daha önce kullanıldı.", "This word has already been used.")
        "wrong_start_letter" -> sh("Kelime son harfle başlamalı.", "The word must start with the last letter.")
        "not_in_dictionary" -> sh("Bu kelime sözlükte bulunamadı.", "This word was not found in the dictionary.")
        "invalid_word" -> sh("Bu kelime geçerli değil.", "This word is not valid.")
        "ends_with_soft_g" -> sh("Ğ ile biten kelimeler kullanılamaz.", "Words ending with Ğ cannot be used.")
        "turn_expired" -> sh("Süren doldu. −1 puan.", "Your time expired. −1 point.")
        else -> sh("Hamle işlenemedi.", "The move could not be processed.")
    }

    suspend fun ensureProfile(): ProfileDto {
        if (backend.currentUserId() == null) backend.ensurePlayer(sh("Oyuncu", "Player"))
        val id = requireNotNull(backend.currentUserId())
        return runCatching { backend.getProfile(id) }.getOrElse { backend.ensurePlayer(sh("Oyuncu", "Player")) }.also { profile = it }
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
            backend.observeRoom(r.id)
                .catch { notice = friendly(it.message.orEmpty()) }
                .collect {
                    room = it
                    refreshQuiz(it)
                    refreshOpponent(it)
                    if (
                        notice.contains("Bağlantı sorunu", true) ||
                        notice.contains("Connection problem", true) ||
                        notice.contains("İşlem tekrar deneniyor", true) ||
                        notice.contains("Retrying the action", true)
                    ) {
                        notice = sh("Bağlantı aktif.", "Connection active.")
                    }
                }
        }
        wordsJob = scope.launch {
            backend.observeWords(r.id)
                .catch { notice = friendly(it.message.orEmpty()) }
                .collect {
                    words = it
                    if (
                        notice.contains("İşlem tekrar deneniyor", true) ||
                        notice.contains("Retrying the action", true)
                    ) {
                        notice = sh("Bağlantı aktif.", "Connection active.")
                    }
                }
        }
        if (!r.isBot) chatJob = scope.launch { backend.observeChat(r.id).catch { notice = friendly(it.message.orEmpty()) }.collect { chat = it } }
    }

    LaunchedEffect(Unit) {
        busy = true
        runCatching { ensureProfile() }.onSuccess { p ->
            val old = runCatching { activeRoom() }.getOrNull()
            if (old != null) { room = old; language = old.language; observe(old); notice = sh("${p.displayName}, aktif maçına dönüldü.", "${p.displayName}, returned to your active match.") }
            else notice = sh("${p.displayName}, düelloya hazırsın.", "${p.displayName}, you are ready to duel.")
        }.onFailure { notice = friendly(it.message.orEmpty()) }
        busy = false
    }

    val active = room
    if (active == null) {
        AuroraDuelLobby(
            playerName = profile?.displayName ?: sh("Oyuncu", "Player"), playerAvatarPath = profile?.avatarPath?.takeIf { profile?.avatarVisibility != "hidden" }, playerGender = profile?.gender, language = language, matching = matching, notice = notice,
            showPrivate = showPrivate, showFriends = showFriends, privateCode = privateCode, friends = friends, invites = invites,
            onLanguage = { language = it; SonHarfSoundFx.tap() },
            onPrivateCode = { privateCode = it.filter(Char::isLetterOrDigit).uppercase().take(6) },
            onRandom = {
                scope.launch {
                    busy = true
                    runCatching { ensureProfile(); backend.startRandomMatchmaking(language) }
                        .onSuccess { matching = true; notice = sh("Rakip aranıyor…", "Searching for an opponent…") }
                        .onFailure {
                            if ("player_already_in_game" in it.message.orEmpty()) {
                                val old = runCatching { activeRoom() }.getOrNull()
                                if (old != null) { room = old; language = old.language; observe(old) } else notice = friendly(it.message.orEmpty())
                            } else notice = friendly(it.message.orEmpty())
                        }
                    busy = false
                    if (matching) matchJob = launch {
                        while (matching && room == null) {
                            val found = runCatching { backend.pollRandomMatchmakingRoom() }.getOrNull()
                            if (found != null) { room = found; language = found.language; observe(found); SonHarfSoundFx.softNotify(); break }
                            delay(900)
                        }
                    }
                }
            },
            onCancel = { scope.launch { matching = false; matchJob?.cancel(); runCatching { backend.cancelRandomMatchmaking() }; notice = sh("Eşleşme iptal edildi.", "Matchmaking cancelled.") } },
            onPrivate = { showPrivate = !showPrivate; showFriends = false },
            onFriends = { scope.launch { friends = runCatching { backend.getFriends() }.getOrDefault(emptyList()); invites = runCatching { backend.getIncomingGameInvites() }.getOrDefault(emptyList()); showFriends = !showFriends; showPrivate = false } },
            onCreate = { scope.launch { busy = true; runCatching { backend.createPrivateRoom(language) }.onSuccess { room = it; observe(it) }.onFailure { notice = friendly(it.message.orEmpty()) }; busy = false } },
            onJoin = { scope.launch { busy = true; runCatching { backend.joinPrivateRoom(privateCode) }.onSuccess { room = it; language = it.language; observe(it) }.onFailure { notice = friendly(it.message.orEmpty()) }; busy = false } },
            onInvite = { id -> scope.launch { runCatching { backend.inviteFriend(id, language) }; notice = sh("Davet gönderildi.", "Invite sent.") } },
            onInviteResponse = { id, accept -> scope.launch { runCatching { backend.respondGameInvite(id, accept) }.onSuccess { if (it != null) { room = it; language = it.language; observe(it) } } } }
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
        LaunchedEffect(active.status, triviaRound?.id, triviaRound?.resolvedAt) {
            val q = triviaRound
            if (active.status == "quiz" && q?.resolvedAt != null) {
                delay(5200)
                runCatching { backend.finishTriviaResult(q.id) }
                    .onSuccess {
                        room = it
                        refreshQuiz(it)
                        notice = sh("Bonus tamamlandı. Düello devam ediyor.", "Bonus complete. Duel resumed.")
                    }
                    .onFailure { notice = friendly(it.message.orEmpty()) }
            }
        }
        ReferenceDuelArena(
            room = active, me = me, playerName = profile?.displayName ?: sh("Sen", "You"), playerAvatarPath = profile?.avatarPath?.takeIf { profile?.avatarVisibility != "hidden" }, playerGender = profile?.gender, playerRating = profile?.rating ?: 1000,
            opponentName = if (active.isBot) "${active.botName ?: if (active.language == "en") "WordBot" else "KelimeBot"} BOT" else opponentProfile?.displayName ?: sh("Rakip", "Opponent"),
            opponentAvatarPath = if (active.isBot) null else opponentProfile?.avatarPath?.takeIf { opponentProfile?.avatarVisibility != "hidden" }, opponentGender = if (active.isBot) null else opponentProfile?.gender, opponentRating = if (active.isBot) 1000 else opponentProfile?.rating ?: 1000,
            words = words, wordInput = wordInput, onWordInput = { wordInput = it.take(40) }, notice = notice, busy = busy,
            triviaRound = triviaRound, triviaQuestion = triviaQuestion,
            onSubmit = {
                scope.launch {
                    val submitted = wordInput.trim(); if (submitted.isBlank()) return@launch
                    wordInput = ""; busy = true; SonHarfSoundFx.tap()
                    runCatching { backend.submitWord(active.id, submitted) }.onSuccess { result ->
                        room = result
                        if (failedEvent(result.lastEvent) && result.lastEventPlayerId == me) { notice = eventMessage(result.lastEvent); SonHarfSoundFx.warning() }
                        else { notice = sh("Kelime kabul edildi: ${submitted.uppercase()}", "Word accepted: ${submitted.uppercase()}"); SonHarfSoundFx.wordAccepted() }
                    }.onFailure { notice = friendly(it.message.orEmpty()); SonHarfSoundFx.warning() }
                    busy = false
                }
            },
            onTimeout = { scope.launch { runCatching { backend.claimTurnTimeout(active.id) }.onSuccess { room = it } } },
            onTrivia = { estimate -> scope.launch { val q = triviaRound ?: return@launch; runCatching { backend.answerTrivia(q.id, estimate) }.onSuccess { room = it; refreshQuiz(it) }.onFailure { notice = friendly(it.message.orEmpty()) } } },
            onTriviaTimeout = { scope.launch { val q = triviaRound ?: return@launch; runCatching { backend.claimTriviaTimeout(q.id) }.onSuccess { room = it; refreshQuiz(it) }.onFailure { notice = friendly(it.message.orEmpty()) } } },
            onChat = { showChat = true },
            onForfeit = { scope.launch { runCatching { backend.forfeit(active.id) }.onSuccess { room = it } } },
            onExit = { roomJob?.cancel(); wordsJob?.cancel(); chatJob?.cancel(); room = null; words = emptyList(); chat = emptyList(); notice = sh("Yeni düelloya hazırsın.", "You are ready for a new duel.") },
            onRematch = { scope.launch { runCatching { if (active.isBot) backend.restartBotMatch(active.id) else backend.requestRematch(active.id) }.onSuccess { room = it; words = emptyList(); chat = emptyList(); if (it.id != active.id) observe(it) }.onFailure { notice = friendly(it.message.orEmpty()) } } }
        )
        if (showChat && !active.isBot) AuroraChatDialog(chat, me, chatInput, { chatInput = it.take(300) }, { showChat = false }) { scope.launch { runCatching { backend.sendChat(active.id, chatInput) }.onSuccess { chatInput = "" } } }
    }
}

@Composable
private fun AuroraDuelLobby(
    playerName: String, playerAvatarPath: String?, playerGender: String?, language: String, matching: Boolean, notice: String, showPrivate: Boolean, showFriends: Boolean,
    privateCode: String, friends: List<Pair<FriendshipDto, ProfileDto>>, invites: List<GameInviteDto>, onLanguage: (String) -> Unit,
    onPrivateCode: (String) -> Unit, onRandom: () -> Unit, onCancel: () -> Unit, onPrivate: () -> Unit, onFriends: () -> Unit,
    onCreate: () -> Unit, onJoin: () -> Unit, onInvite: (String) -> Unit, onInviteResponse: (String, Boolean) -> Unit
) {
    val gameBg = Color(0xFF050713)
    val gamePanel = Color(0xFF0C1022)
    val gamePanel2 = Color(0xFF121936)
    val gameText = Color(0xFFF7F8FF)
    val gameMuted = Color(0xFF9AA6C1)
    val gameBlue = Color(0xFF2188FF)
    val gameViolet = Color(0xFF8A5CFF)
    val gameMagenta = Color(0xFFE347FF)
    val gameGold = Color(0xFFFFB31A)
    val gameCyan = Color(0xFF31D3FF)
    val gamePink = Color(0xFFFF4F87)

    val infinite = rememberInfiniteTransition(label = "duel-lobby")
    val pulse by infinite.animateFloat(
        initialValue = .97f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    val ringAlpha by infinite.animateFloat(
        initialValue = .55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ring-alpha",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF050713),
                        Color(0xFF090B1B),
                        Color(0xFF060817),
                    )
                )
            )
            .statusBarsPadding(),
    ) {
        Box(
            Modifier
                .size(360.dp)
                .align(Alignment.TopCenter)
                .offset(y = 70.dp)
                .background(
                    Brush.radialGradient(
                        listOf(gameMagenta.copy(alpha = .16f), Color.Transparent)
                    ),
                    CircleShape,
                )
        )
        Box(
            Modifier
                .size(320.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 150.dp, y = 110.dp)
                .background(
                    Brush.radialGradient(
                        listOf(gameBlue.copy(alpha = .14f), Color.Transparent)
                    ),
                    CircleShape,
                )
        )

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProfilePhotoAvatarWithGender(
                        avatarPath = playerAvatarPath,
                        gender = playerGender,
                        name = playerName,
                        size = 44.dp,
                        accent = gameBlue,
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(playerName, color = gameText, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        Text(sh("Hazır oyuncu", "Ready player"), color = gameMuted, fontSize = 9.sp)
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = gamePanel2,
                        border = BorderStroke(1.dp, gameGold.copy(alpha = .34f)),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("🏆", fontSize = 15.sp)
                            Spacer(Modifier.width(5.dp))
                            Text(sh("DÜELLO", "DUEL"), color = gameGold, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF11152D),
                                    Color(0xFF090C1C),
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(310.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        gameViolet.copy(alpha = .17f),
                                        gameBlue.copy(alpha = .07f),
                                        Color.Transparent,
                                    )
                                ),
                                CircleShape,
                            )
                    )
                    Box(
                        Modifier
                            .size(240.dp)
                            .graphicsLayer {
                                scaleX = if (matching) pulse else 1f
                                scaleY = if (matching) pulse else 1f
                            }
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    listOf(
                                        gameBlue.copy(alpha = ringAlpha),
                                        gameMagenta.copy(alpha = ringAlpha),
                                        gameGold.copy(alpha = ringAlpha),
                                        gameCyan.copy(alpha = ringAlpha),
                                        gameBlue.copy(alpha = ringAlpha),
                                    )
                                )
                            )
                            .padding(4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            Color(0xFF17213E),
                                            Color(0xFF090D1F),
                                            Color(0xFF060816),
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (matching) {
                                    CircularProgressIndicator(
                                        Modifier.size(34.dp),
                                        strokeWidth = 3.dp,
                                        color = gameGold,
                                        trackColor = gamePanel2,
                                    )
                                    Spacer(Modifier.height(10.dp))
                                } else {
                                    Text(
                                        "SON",
                                        color = Color(0xFFC7D5FF),
                                        fontSize = 25.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 2.sp,
                                    )
                                    Text(
                                        "HARF",
                                        color = gameGold,
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp,
                                    )
                                    Text(
                                        sh("DÜELLO", "DUEL"),
                                        color = gameViolet,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 5.sp,
                                    )
                                    Spacer(Modifier.height(10.dp))
                                }
                                Text(
                                    if (matching) sh("RAKİP ARANIYOR", "SEARCHING OPPONENT")
                                    else sh("DÜELLOYA HAZIR", "READY TO DUEL"),
                                    color = gameText,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Black,
                                    fontSize = if (matching) 20.sp else 14.sp,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    if (matching) sh("Önce gerçek oyuncu, sonra BOT", "Real player first, then BOT")
                                    else sh("Kelimeyi sürdür, rakibini geç", "Continue the word, beat your rival"),
                                    color = gameMuted,
                                    fontSize = 9.sp,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GameLanguagePill(
                        selected = language == "tr",
                        label = "🇹🇷  TÜRKÇE",
                        accent = gameViolet,
                        modifier = Modifier.weight(1f),
                    ) { onLanguage("tr") }
                    GameLanguagePill(
                        selected = language == "en",
                        label = "🇬🇧  ENGLISH",
                        accent = gameBlue,
                        modifier = Modifier.weight(1f),
                    ) { onLanguage("en") }
                }
            }

            item {
                if (matching) {
                    Button(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = gamePink.copy(alpha = .20f),
                            contentColor = gamePink,
                        ),
                        border = BorderStroke(1.dp, gamePink.copy(alpha = .55f)),
                    ) {
                        Text(sh("✕  EŞLEŞMEYİ İPTAL ET", "✕  CANCEL MATCHMAKING"), fontWeight = FontWeight.Black)
                    }
                } else {
                    Button(
                        onClick = onRandom,
                        modifier = Modifier.fillMaxWidth().height(68.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = gameGold,
                            contentColor = Color(0xFF241300),
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 2.dp),
                    ) {
                        Text(
                            sh("⚡  DÜELLOYA GİR", "⚡  ENTER DUEL"),
                            fontWeight = FontWeight.Black,
                            fontSize = 19.sp,
                        )
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GameLobbyAction(
                        icon = "👥",
                        title = sh("ARKADAŞ", "FRIENDS"),
                        subtitle = sh("Davet et", "Invite"),
                        accent = gameCyan,
                        modifier = Modifier.weight(1f),
                        onClick = onFriends,
                    )
                    GameLobbyAction(
                        icon = "♛",
                        title = sh("ÖZEL ODA", "PRIVATE ROOM"),
                        subtitle = sh("Kodla gir", "Join by code"),
                        accent = gameViolet,
                        modifier = Modifier.weight(1f),
                        onClick = onPrivate,
                    )
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = gamePanel.copy(alpha = .92f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = .06f)),
                ) {
                    Text(
                        notice,
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        color = gameMuted,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                }
            }

            if (showPrivate) item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = gamePanel),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, gameViolet.copy(alpha = .42f)),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text(sh("♛  ÖZEL ODA", "♛  PRIVATE ROOM"), color = gameViolet, fontWeight = FontWeight.Black)
                        Button(
                            onClick = onCreate,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = gameViolet),
                        ) {
                            Text(sh("VIP ODA OLUŞTUR", "CREATE VIP ROOM"), fontWeight = FontWeight.Black)
                        }
                        OutlinedTextField(
                            privateCode,
                            onPrivateCode,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text(sh("6 haneli oda kodu", "6-character room code"), color = gameMuted) },
                            textStyle = LocalTextStyle.current.copy(color = gameText, fontWeight = FontWeight.Bold),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = gameCyan,
                                unfocusedBorderColor = gameMuted.copy(alpha = .35f),
                                cursorColor = gameCyan,
                                focusedContainerColor = gamePanel2,
                                unfocusedContainerColor = gamePanel2,
                            ),
                        )
                        OutlinedButton(
                            onClick = onJoin,
                            enabled = privateCode.length == 6,
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, gameCyan.copy(alpha = .6f)),
                        ) {
                            Text(sh("ODA KODUYLA KATIL", "JOIN WITH ROOM CODE"), color = gameCyan)
                        }
                    }
                }
            }

            if (showFriends) item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = gamePanel),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, gameCyan.copy(alpha = .36f)),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(sh("👥  ARKADAŞLAR", "👥  FRIENDS"), color = gameCyan, fontWeight = FontWeight.Black)
                        invites.forEach { i ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(sh("Maç daveti", "Game invite"), color = gameText)
                                Row {
                                    TextButton(onClick = { onInviteResponse(i.id, true) }) { Text(sh("Kabul", "Accept"), color = gameCyan) }
                                    TextButton(onClick = { onInviteResponse(i.id, false) }) { Text(sh("Reddet", "Decline"), color = gamePink) }
                                }
                            }
                        }
                        friends.forEach { (_, p) ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(p.displayName, color = gameText, fontWeight = FontWeight.Bold)
                                    Text(
                                        if (p.presenceStatus == "online") sh("Çevrimiçi", "Online") else sh("Çevrimdışı", "Offline"),
                                        color = if (p.presenceStatus == "online") gameCyan else gameMuted,
                                        fontSize = 9.sp,
                                    )
                                }
                                Button(
                                    onClick = { onInvite(p.id) },
                                    enabled = p.presenceStatus == "online",
                                    colors = ButtonDefaults.buttonColors(containerColor = gameBlue),
                                ) {
                                    Text(sh("Davet", "Invite"))
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(10.dp)) }
        }
    }
}

@Composable
private fun GameLanguagePill(
    selected: Boolean,
    label: String,
    accent: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .height(50.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (selected) accent.copy(alpha = .18f) else Color(0xFF0C1022),
        border = BorderStroke(1.dp, if (selected) accent.copy(alpha = .75f) else Color.White.copy(alpha = .10f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (selected) Color.White else Color(0xFF9AA6C1),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun GameLobbyAction(
    icon: String,
    title: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .height(92.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF0C1022),
        border = BorderStroke(1.dp, accent.copy(alpha = .40f)),
        shadowElevation = 4.dp,
    ) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(icon, fontSize = 22.sp)
            Column {
                Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Text(subtitle, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AuroraArena(
    room: GameRoomDto, me: String?, playerName: String, playerAvatarPath: String?, playerGender: String?, playerRating: Int, opponentName: String, opponentAvatarPath: String?, opponentGender: String?, opponentRating: Int, words: List<GameWordDto>, wordInput: String,
    onWordInput: (String) -> Unit, notice: String, busy: Boolean, triviaRound: TriviaRoundDto?, triviaQuestion: TriviaQuestionDto?,
    onSubmit: () -> Unit, onTimeout: () -> Unit, onTrivia: (Int) -> Unit, onChat: () -> Unit, onForfeit: () -> Unit,
    onExit: () -> Unit, onRematch: () -> Unit
) {
    val host = me == room.hostId
    val myScore = if (host) room.hostScore else room.guestScore
    val oppScore = if (host) room.guestScore else room.hostScore
    val myRounds = if (host) room.hostRounds else room.guestRounds
    val oppRounds = if (host) room.guestRounds else room.hostRounds
    val liveWordPhase = room.status in listOf("playing", "final", "sudden_death")
    val myTurn = room.currentPlayerId == me && liveWordPhase
    val last = words.lastOrNull()?.normalizedWord
    val required = last?.lastOrNull()?.uppercaseChar()?.toString() ?: "•"
    var seconds by remember(room.turnDeadline) { mutableStateOf(7) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(room.turnDeadline, room.currentPlayerId, room.status) {
        while (room.turnDeadline != null && liveWordPhase) {
            seconds = runCatching {
                (Instant.parse(room.turnDeadline).epochSecond - Instant.now().epochSecond).toInt().coerceAtLeast(0)
            }.getOrDefault(7)
            if (seconds <= 0) {
                SonHarfSoundFx.warning()
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onTimeout()
                break
            }
            if (seconds in 1..5) SonHarfSoundFx.countdown()
            if (seconds in 1..3) {
                SonHarfSoundFx.heartbeat()
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            delay(1000)
        }
    }

    val arenaBgTop = Color(0xFF050713)
    val arenaBgBottom = Color(0xFF080B1A)
    val arenaPanel = Color(0xFF0B1024)
    val arenaPanel2 = Color(0xFF141C3A)
    val arenaText = Color(0xFFF7F8FF)
    val arenaMuted = Color(0xFF9AA6C1)
    val arenaBlue = Color(0xFF2188FF)
    val arenaViolet = Color(0xFF8A5CFF)
    val arenaMagenta = Color(0xFFE347FF)
    val arenaGold = Color(0xFFFFB31A)
    val arenaCyan = Color(0xFF31D3FF)
    val arenaPink = Color(0xFFFF4F87)
    val timerAccent = when {
        seconds <= 5 -> arenaPink
        seconds <= 15 -> arenaGold
        else -> arenaCyan
    }
    val myAccent = arenaBlue
    val opponentAccent = arenaPink

    val arenaMotion = rememberInfiniteTransition(label = "arena-motion")
    val corePulse by arenaMotion.animateFloat(
        initialValue = .98f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "core-pulse",
    )
    val urgentPulse by arenaMotion.animateFloat(
        initialValue = .92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "urgent-pulse",
    )

    if (room.status == "finished") {
        val draw = room.winnerId == null
        val won = room.winnerId == me
        Box(
            Modifier
                .fillMaxSize()
                .background(
                Brush.verticalGradient(
                    listOf(arenaBgTop, Color(0xFF0B0B22), arenaBgBottom)
                )
            )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = arenaPanel),
                shape = RoundedCornerShape(30.dp),
                border = BorderStroke(
                    1.dp,
                    when {
                        draw -> SonHarfGold.copy(alpha = .55f)
                        won -> myAccent.copy(alpha = .65f)
                        else -> opponentAccent.copy(alpha = .55f)
                    },
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(15.dp),
                ) {
                    Text(sh("MAÇ SONUCU", "MATCH RESULT"), color = arenaMuted, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Text(
                        when {
                            draw -> sh("BERABERE", "DRAW")
                            won -> sh("ZAFER", "VICTORY")
                            else -> sh("MAÇ BİTTİ", "MATCH OVER")
                        },
                        color = when {
                            draw -> SonHarfGold
                            won -> myAccent
                            else -> opponentAccent
                        },
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        if (draw) "$playerName  •  $opponentName" else if (won) playerName else opponentName,
                        color = arenaText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$myRounds", color = myAccent, fontSize = 46.sp, fontWeight = FontWeight.Black)
                        Text("   :   ", color = arenaMuted, fontSize = 22.sp)
                        Text("$oppRounds", color = opponentAccent, fontSize = 46.sp, fontWeight = FontWeight.Black)
                    }
                    Button(
                        onClick = onRematch,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = myAccent),
                    ) {
                        Text(sh("RÖVANŞ  ⚡", "REMATCH  ⚡"), fontWeight = FontWeight.Black)
                    }
                    OutlinedButton(
                        onClick = onExit,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        border = BorderStroke(1.dp, arenaMuted.copy(alpha = .45f)),
                    ) {
                        Text(sh("LOBİYE DÖN", "BACK TO LOBBY"), color = arenaText)
                    }
                }
            }
        }
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(arenaBgTop, arenaBgBottom)))
            .statusBarsPadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AuroraPlayerCard(playerName, playerAvatarPath, playerGender, playerRating, myScore, myRounds, myTurn, myAccent, Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(82.dp)
                    .graphicsLayer {
                        if (seconds <= 5) {
                            scaleX = urgentPulse
                            scaleY = urgentPulse
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(arenaBlue, arenaCyan, arenaViolet, arenaPink, arenaGold, arenaBlue)
                            )
                        )
                        .padding(4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF172344), Color(0xFF080C1C))
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$seconds", color = arenaText, fontSize = 29.sp, fontWeight = FontWeight.Black)
                            Text(sh("SN", "SEC"), color = timerAccent, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                Text(
                    "💣",
                    modifier = Modifier.align(Alignment.TopCenter).offset(y = (-9).dp),
                    fontSize = 18.sp,
                )
            }

            AuroraPlayerCard(opponentName, opponentAvatarPath, opponentGender, opponentRating, oppScore, oppRounds, !myTurn && liveWordPhase, opponentAccent, Modifier.weight(1f))
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 10.dp),
            shape = RoundedCornerShape(28.dp),
            color = arenaPanel,
            border = BorderStroke(1.dp, if (myTurn) myAccent.copy(alpha = .55f) else Color.White.copy(alpha = .08f)),
            shadowElevation = 8.dp,
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(
                                if (myTurn) arenaMagenta.copy(alpha = .16f) else opponentAccent.copy(alpha = .08f),
                                arenaBlue.copy(alpha = .05f),
                                Color.Transparent,
                            ),
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                ArenaEnergyBackdrop(
                    primary = arenaBlue,
                    secondary = arenaMagenta,
                    pulse = corePulse,
                )
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (room.status == "sudden_death") opponentAccent.copy(alpha = .18f) else arenaPanel2,
                        ) {
                            Text(
                                if (room.status == "sudden_death") sh("ANİ ÖLÜM", "SUDDEN DEATH") else "ROUND ${room.roundNo}/3",
                                Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                color = if (room.status == "sudden_death") opponentAccent else arenaText,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(10) { i ->
                                Box(
                                    Modifier
                                        .size(if (i < room.roundWordCount) 7.dp else 5.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (i < room.roundWordCount) {
                                                if (i % 2 == 0) arenaBlue else arenaViolet
                                            } else arenaMuted.copy(alpha = .16f)
                                        )
                                )
                            }
                        }

                        Text(
                            "${room.roundWordCount}/10",
                            color = arenaText,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        when {
                            myTurn -> sh("SIRA SENDE  ⚡", "YOUR TURN  ⚡")
                            room.isBot && room.botTurn -> sh("BOT DÜŞÜNÜYOR…", "BOT IS THINKING…")
                            else -> sh("RAKİBİN HAMLESİ", "OPPONENT'S MOVE")
                        },
                        color = if (myTurn) myAccent else arenaMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                    )

                    Spacer(Modifier.weight(.25f))

                    Surface(
                        modifier = Modifier
                            .size(148.dp)
                            .graphicsLayer {
                                scaleX = if (myTurn) corePulse else 1f
                                scaleY = if (myTurn) corePulse else 1f
                            },
                        shape = CircleShape,
                        color = arenaPanel2,
                        border = BorderStroke(
                            4.dp,
                            Brush.sweepGradient(
                                listOf(
                                    arenaBlue,
                                    arenaViolet,
                                    arenaMagenta,
                                    arenaGold,
                                    arenaCyan,
                                    arenaBlue,
                                )
                            ),
                        ),
                        shadowElevation = 12.dp,
                    ) {
                        Box(
                            Modifier.background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFF253160),
                                        Color(0xFF131A39),
                                        Color(0xFF090C1E),
                                    )
                                )
                            ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(sh("SON HARF", "LAST LETTER"), color = arenaMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(required, color = arenaText, fontSize = 70.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        if (last == null) sh("İlk kelimeyi sen başlat.", "Start with the first word.")
                        else sh("$required ile başlayan kelimeyi kur", "Build a word starting with $required"),
                        color = arenaMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )

                    Spacer(Modifier.weight(.2f))

                    if (words.isNotEmpty()) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(
                                sh("KELİME ZİNCİRİ", "WORD CHAIN"),
                                color = arenaMuted,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                            )
                            Spacer(Modifier.height(5.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(words.takeLast(12)) { w ->
                                    Surface(
                                        modifier = Modifier.heightIn(min = 34.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF101633),
                                        border = BorderStroke(1.dp, arenaCyan.copy(alpha = .30f)),
                                    ) {
                                        Text(
                                            w.word.trim().ifBlank { w.normalizedWord.trim() }.uppercase(),
                                            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val warningNotice =
            notice.startsWith("Bu ") || notice.startsWith("This ") || notice.contains("doldu") ||
                notice.contains("expired") || notice.contains("sorunu") || notice.contains("problem")

        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp),
            shape = RoundedCornerShape(12.dp),
            color = if (warningNotice) opponentAccent.copy(alpha = .13f) else arenaPanel2.copy(alpha = .72f),
            border = BorderStroke(
                1.dp,
                if (warningNotice) opponentAccent.copy(alpha = .32f) else arenaViolet.copy(alpha = .18f),
            ),
        ) {
            Text(
                notice,
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                color = if (warningNotice) Color(0xFFFFA9B6) else arenaMuted,
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }

        if (room.status == "quiz" && triviaRound != null && triviaQuestion != null) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
                colors = CardDefaults.cardColors(containerColor = SonHarfPurple.copy(alpha = .22f)),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, SonHarfPurple.copy(alpha = .55f)),
            ) {
                Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("★ BONUS +${triviaRound.bonusPoints}", color = SonHarfGold, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    Text(triviaQuestion.question, color = arenaText, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    val options = listOf(triviaQuestion.optionA, triviaQuestion.optionB, triviaQuestion.optionC, triviaQuestion.optionD)
                    options.chunked(2).forEachIndexed { rowIndex, pair ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            pair.forEachIndexed { colIndex, option ->
                                OutlinedButton(
                                    onClick = { onTrivia(rowIndex * 2 + colIndex) },
                                    modifier = Modifier.weight(1f).heightIn(min = 34.dp),
                                    border = BorderStroke(1.dp, SonHarfPurple.copy(alpha = .65f)),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 3.dp),
                                ) {
                                    Text(option, color = arenaText, fontSize = 9.sp, maxLines = 2)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ArenaActionButton(sh("⚑ PES ET", "⚑ FORFEIT"), opponentAccent, Modifier.weight(1f), onForfeit)
                ArenaActionButton(sh("● SOHBET", "● CHAT"), myAccent, Modifier.weight(1f), onChat)
                ArenaActionButton("★ BONUS", arenaGold, Modifier.weight(1f)) { }
            }

            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF0D1430),
                border = BorderStroke(
                    1.dp,
                    if (myTurn) arenaBlue.copy(alpha = .80f) else arenaViolet.copy(alpha = .22f),
                ),
            ) {
                Row(
                    Modifier.fillMaxWidth().height(50.dp).padding(start = 14.dp, end = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (wordInput.isBlank()) {
                            if (myTurn) sh("Kelimenizi yazın…", "Type your word…") else sh("Kelimeyi hazırlayabilirsin…", "You can prepare a word…")
                        } else wordInput,
                        color = if (wordInput.isBlank()) arenaMuted else arenaText,
                        fontSize = if (wordInput.isBlank()) 14.sp else 19.sp,
                        fontWeight = if (wordInput.isBlank()) FontWeight.Medium else FontWeight.Black,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                    )
                    Button(
                        onClick = onSubmit,
                        enabled = myTurn && wordInput.isNotBlank() && !busy,
                        modifier = Modifier.height(40.dp),
                        shape = RoundedCornerShape(13.dp),
                        contentPadding = PaddingValues(horizontal = 15.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = arenaGold,
                            contentColor = Color(0xFF241300),
                            disabledContainerColor = arenaMuted.copy(alpha = .18f),
                        ),
                    ) {
                        Text("➤", fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        EmbeddedWordKeyboard(
            value = wordInput,
            language = room.language,
            enabled = !busy && room.status != "quiz",
            submitEnabled = myTurn && !busy && room.status != "quiz",
            maxLength = 40,
            onValueChange = onWordInput,
            onSubmit = onSubmit,
            modifier = Modifier.navigationBarsPadding(),
        )
    }
}

@Composable
private fun ArenaActionButton(
    label: String,
    accent: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = .48f)),
        shape = RoundedCornerShape(13.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun AuroraPlayerCard(
    name: String,
    avatarPath: String?,
    gender: String?,
    rating: Int,
    score: Int,
    rounds: Int,
    active: Boolean,
    accent: Color,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier.height(86.dp),
        color = Color.Transparent,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, if (active) accent.copy(alpha = .86f) else accent.copy(alpha = .30f)),
        shadowElevation = if (active) 9.dp else 3.dp,
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            accent.copy(alpha = if (active) .24f else .10f),
                            Color(0xFF0A1024),
                            Color(0xFF0A1024),
                        )
                    )
                )
                .padding(horizontal = 9.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (avatarPath == null && name.contains("BOT", ignoreCase = true)) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = .20f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🤖", fontSize = 25.sp)
                }
            } else {
                ProfilePhotoAvatarWithGender(
                    avatarPath = avatarPath,
                    gender = gender,
                    name = name,
                    size = 48.dp,
                    accent = accent,
                )
            }

            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        name,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        color = Color(0xFFF7F8FF),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                    )
                    if (active) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(accent))
                    }
                }
                Text(
                    score.toString(),
                    color = Color.White,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Black,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🏆", fontSize = 8.sp)
                    Spacer(Modifier.width(3.dp))
                    Text(
                        rating.toString(),
                        color = Color(0xFFFFC247),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        "$rounds R",
                        color = Color(0xFF95A4BE),
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ArenaEnergyBackdrop(
    primary: Color,
    secondary: Color,
    pulse: Float,
) {
    Canvas(Modifier.fillMaxSize()) {
        val center = Offset(size.width * .5f, size.height * .48f)
        repeat(26) { i ->
            val fx = ((i * 37) % 101) / 100f
            val fy = ((i * 61 + 17) % 97) / 96f
            val radius = if (i % 4 == 0) 2.1f else 1.15f
            drawCircle(
                color = if (i % 3 == 0) secondary.copy(alpha = .18f * pulse) else primary.copy(alpha = .16f * pulse),
                radius = radius,
                center = Offset(size.width * fx, size.height * fy),
            )
        }
        repeat(7) { i ->
            val x = size.width * (.08f + i * .14f)
            val target = Offset(x, if (i % 2 == 0) size.height * .18f else size.height * .82f)
            drawLine(
                color = if (i % 2 == 0) primary.copy(alpha = .09f * pulse) else secondary.copy(alpha = .08f * pulse),
                start = center,
                end = target,
                strokeWidth = 1.2f,
            )
        }
    }
}

@Composable private fun AuroraChatDialog(chat: List<ChatMessageDto>, me: String?, input: String, onInput: (String) -> Unit, onClose: () -> Unit, onSend: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(sh("SOHBET", "CHAT"), fontWeight = FontWeight.Black) },
        text = {
            Column(Modifier.heightIn(max = 420.dp)) {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) { items(chat.takeLast(30)) { m -> Surface(color = if (m.senderId == me) SonHarfPurple.copy(alpha = .14f) else SonHarfSurface2, shape = RoundedCornerShape(12.dp)) { Text(m.body, Modifier.padding(9.dp), fontSize = 11.sp) } } }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(input, onInput, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text(sh("Mesaj yaz…", "Type a message…")) })
            }
        },
        confirmButton = { TextButton(onClick = onSend, enabled = input.isNotBlank()) { Text(sh("GÖNDER", "SEND")) } },
        dismissButton = { TextButton(onClick = onClose) { Text(sh("KAPAT", "CLOSE")) } }
    )
}
