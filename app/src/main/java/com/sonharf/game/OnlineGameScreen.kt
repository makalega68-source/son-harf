package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.sonharf.game.data.ChatMessageDto
import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.GameWordDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.TriviaQuestionDto
import com.sonharf.game.data.TriviaRoundDto
import java.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@Composable
fun OnlineGameScreen() {
    if (!SupabaseProvider.configured) {
        MissingBackendConfig()
        return
    }

    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var playerName by remember { mutableStateOf("") }
    var roomCode by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf("tr") }
    var room by remember { mutableStateOf<GameRoomDto?>(null) }
    var words by remember { mutableStateOf<List<GameWordDto>>(emptyList()) }
    var chat by remember { mutableStateOf<List<ChatMessageDto>>(emptyList()) }
    var triviaRound by remember { mutableStateOf<TriviaRoundDto?>(null) }
    var triviaQuestion by remember { mutableStateOf<TriviaQuestionDto?>(null) }
    var wordInput by remember { mutableStateOf("") }
    var chatInput by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Oda oluştur veya arkadaşının koduyla düelloya katıl.") }
    var roomJob by remember { mutableStateOf<Job?>(null) }
    var wordsJob by remember { mutableStateOf<Job?>(null) }
    var chatJob by remember { mutableStateOf<Job?>(null) }

    fun readableError(t: Throwable): String {
        val raw = t.message.orEmpty()
        return when {
            "anonymous_provider_disabled" in raw -> "Anonim giriş kapalı."
            "not_your_turn" in raw -> "Sıra rakibinde."
            "answers_locked" in raw -> "Şıkları görmek için 3 saniyelik okuma süresinin bitmesini bekle."
            "room_not_available" in raw -> "Oda bulunamadı, dolu veya oyunculardan biri diğerini engellemiş."
            else -> raw.substringBefore("URL:").trim().ifBlank { "Bağlantı hatası oluştu." }
        }
    }

    suspend fun refreshQuiz(active: GameRoomDto) {
        if (active.status == "quiz") {
            val q = backend.getActiveTriviaRound(active.id)
            triviaRound = q
            triviaQuestion = q?.let { backend.getTriviaQuestion(it.questionId) }
        } else {
            triviaRound = null
            triviaQuestion = null
        }
    }

    fun startObservers(active: GameRoomDto) {
        roomJob?.cancel(); wordsJob?.cancel(); chatJob?.cancel()
        roomJob = scope.launch {
            backend.observeRoom(active.id)
                .catch { message = readableError(it) }
                .collect {
                    room = it
                    refreshQuiz(it)
                }
        }
        wordsJob = scope.launch {
            backend.observeWords(active.id).catch { message = readableError(it) }.collect { words = it }
        }
        chatJob = scope.launch {
            backend.observeChat(active.id).catch { message = readableError(it) }.collect { chat = it }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            roomJob?.cancel(); wordsJob?.cancel(); chatJob?.cancel()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B1020), SonHarfBg, Color(0xFF090A11))))
    ) {
        if (room == null) {
            LobbyScreen(
                playerName = playerName,
                onPlayerNameChange = { playerName = it.take(24) },
                roomCode = roomCode,
                onRoomCodeChange = { roomCode = it.uppercase().take(6) },
                selectedLanguage = selectedLanguage,
                onLanguageChange = { selectedLanguage = it },
                busy = busy,
                message = message,
                onCreateRoom = {
                    scope.launch {
                        busy = true
                        runCatching {
                            backend.ensurePlayer(playerName)
                            backend.createRoom(selectedLanguage)
                        }.onSuccess {
                            room = it
                            roomCode = it.code
                            message = "Oda hazır. Kodu arkadaşına gönder: ${it.code}"
                            startObservers(it)
                        }.onFailure { message = readableError(it) }
                        busy = false
                    }
                },
                onJoinRoom = {
                    scope.launch {
                        busy = true
                        runCatching {
                            backend.ensurePlayer(playerName)
                            backend.joinRoom(roomCode)
                        }.onSuccess {
                            room = it
                            selectedLanguage = it.language
                            message = "Odaya katıldın. Düello başladı."
                            startObservers(it)
                        }.onFailure { message = readableError(it) }
                        busy = false
                    }
                }
            )
        } else {
            val activeRoom = room ?: return@Box
            val me = backend.currentUserId()
            val myTurn = activeRoom.currentPlayerId == me && activeRoom.status in listOf("playing", "final", "sudden_death")
            val waiting = activeRoom.status == "waiting"
            val opponentId = if (me == activeRoom.hostId) activeRoom.guestId else activeRoom.hostId

            ActiveGameScreen(
                activeRoom = activeRoom,
                me = me,
                myTurn = myTurn,
                waiting = waiting,
                words = words,
                chat = chat,
                triviaRound = triviaRound,
                triviaQuestion = triviaQuestion,
                wordInput = wordInput,
                onWordInputChange = { wordInput = it.take(40) },
                chatInput = chatInput,
                onChatInputChange = { chatInput = it.take(300) },
                message = message,
                busy = busy,
                onSubmitWord = {
                    scope.launch {
                        busy = true
                        runCatching { backend.submitWord(activeRoom.id, wordInput) }
                            .onSuccess {
                                room = it
                                wordInput = ""
                                message = eventText(it, me)
                            }
                            .onFailure { message = readableError(it) }
                        busy = false
                    }
                },
                onTimeout = {
                    scope.launch {
                        runCatching { backend.claimTurnTimeout(activeRoom.id) }
                            .onSuccess { room = it; message = eventText(it, me) }
                            .onFailure { message = readableError(it) }
                    }
                },
                onAnswerTrivia = { index ->
                    val roundId = triviaRound?.id ?: return@ActiveGameScreen
                    scope.launch {
                        runCatching { backend.answerTrivia(roundId, index) }
                            .onSuccess { room = it; refreshQuiz(it); message = eventText(it, me) }
                            .onFailure { message = readableError(it) }
                    }
                },
                onForfeit = {
                    scope.launch {
                        runCatching { backend.forfeit(activeRoom.id) }
                            .onSuccess { room = it }
                            .onFailure { message = readableError(it) }
                    }
                },
                onSendChat = {
                    scope.launch {
                        runCatching { backend.sendChat(activeRoom.id, chatInput) }
                            .onSuccess { chatInput = "" }
                            .onFailure { message = readableError(it) }
                    }
                },
                onBlockOpponent = {
                    val target = opponentId ?: return@ActiveGameScreen
                    scope.launch {
                        runCatching { backend.blockUser(target) }
                            .onSuccess { message = "Oyuncu engellendi. Sohbet ve yeniden eşleşme kapatıldı." }
                            .onFailure { message = readableError(it) }
                    }
                },
                onAllowPhoto = {
                    val target = opponentId ?: return@ActiveGameScreen
                    scope.launch {
                        runCatching { backend.setPhotoAccess(target, true) }
                            .onSuccess { message = "Profil fotoğrafı bu oyuncuya özel açıldı." }
                            .onFailure { message = readableError(it) }
                    }
                }
            )
        }
    }
}

private fun eventText(room: GameRoomDto, me: String?): String = when (room.lastEvent) {
    "streak_bonus" -> if (room.lastEventPlayerId == me) "🔥 KELİME SERİSİ! +3 ekstra puan" else "Rakip seri yaptı. Baskı artıyor!"
    "invalid_word" -> "Geçersiz kelime: -1 puan ve sıra değişti."
    "not_in_dictionary" -> "Sözlükte kabul edilmeyen kelime: -1 puan."
    "wrong_start_letter" -> "Yanlış başlangıç harfi: -1 puan."
    "word_already_used" -> "Bu kelime daha önce kullanıldı: -1 puan."
    "turn_expired" -> "Süre doldu: -1 puan ve sıra rakibe geçti."
    "quiz_started" -> "🧠 Bonus soru! Önce 3 saniye oku."
    "quiz_won" -> "⚡ Bonus kapıldı! Oyun devam ediyor."
    "final_started" -> "🏁 FİNAL ZİNCİRİ başladı!"
    else -> "Hamle işlendi."
}

@Composable
private fun LobbyScreen(
    playerName: String,
    onPlayerNameChange: (String) -> Unit,
    roomCode: String,
    onRoomCodeChange: (String) -> Unit,
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit,
    busy: Boolean,
    message: String,
    onCreateRoom: () -> Unit,
    onJoinRoom: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("ONLINE DÜELLO", fontSize = 30.sp, fontWeight = FontWeight.Black)
        Text("Önce oyun dilini seç, sonra odanı aç.", color = SonHarfMuted)

        Surface(color = SonHarfPurple.copy(alpha = .12f), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, SonHarfPurple.copy(alpha = .35f))) {
            Text(message, Modifier.fillMaxWidth().padding(14.dp), color = SonHarfText, fontSize = 13.sp)
        }

        Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("OYUN DİLİ", color = SonHarfCyan, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = selectedLanguage == "tr",
                        onClick = { onLanguageChange("tr") },
                        label = { Text("🇹🇷 Türkçe") }
                    )
                    FilterChip(
                        selected = selectedLanguage == "en",
                        onClick = { onLanguageChange("en") },
                        label = { Text("🇬🇧 English") }
                    )
                }
                Text("Oda açıldıktan sonra dil değişmez.", color = SonHarfMuted, fontSize = 11.sp)
            }
        }

        DarkTextField(value = playerName, onValueChange = onPlayerNameChange, label = "Oyuncu adı", placeholder = "Oyuncu")

        Button(
            onClick = onCreateRoom,
            enabled = !busy && playerName.trim().length >= 2,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SonHarfPurple)
        ) { Text(if (busy) "HAZIRLANIYOR…" else "ÖZEL ODA OLUŞTUR", fontWeight = FontWeight.Black) }

        HorizontalDivider(color = SonHarfSurface2)
        DarkTextField(value = roomCode, onValueChange = onRoomCodeChange, label = "6 haneli oda kodu", placeholder = "ABC123")
        OutlinedButton(
            onClick = onJoinRoom,
            enabled = !busy && playerName.trim().length >= 2 && roomCode.length == 6,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, SonHarfCyan.copy(alpha = .7f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SonHarfCyan)
        ) { Text("ODAYA KATIL", fontWeight = FontWeight.Black) }
    }
}

@Composable
private fun DarkTextField(value: String, onValueChange: (String) -> Unit, label: String, placeholder: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = SonHarfMuted.copy(alpha = .65f)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SonHarfCyan,
            unfocusedBorderColor = SonHarfSurface2,
            focusedLabelColor = SonHarfCyan,
            cursorColor = SonHarfCyan,
            focusedContainerColor = Color(0xFF0D1322),
            unfocusedContainerColor = Color(0xFF0D1322)
        )
    )
}

@Composable
private fun ActiveGameScreen(
    activeRoom: GameRoomDto,
    me: String?,
    myTurn: Boolean,
    waiting: Boolean,
    words: List<GameWordDto>,
    chat: List<ChatMessageDto>,
    triviaRound: TriviaRoundDto?,
    triviaQuestion: TriviaQuestionDto?,
    wordInput: String,
    onWordInputChange: (String) -> Unit,
    chatInput: String,
    onChatInputChange: (String) -> Unit,
    message: String,
    busy: Boolean,
    onSubmitWord: () -> Unit,
    onTimeout: () -> Unit,
    onAnswerTrivia: (Int) -> Unit,
    onForfeit: () -> Unit,
    onSendChat: () -> Unit,
    onBlockOpponent: () -> Unit,
    onAllowPhoto: () -> Unit
) {
    val isHost = me == activeRoom.hostId
    val myScore = if (isHost) activeRoom.hostScore else activeRoom.guestScore
    val opponentScore = if (isHost) activeRoom.guestScore else activeRoom.hostScore
    val myStreak = if (isHost) activeRoom.hostStreak else activeRoom.guestStreak
    val required = words.lastOrNull()?.normalizedWord?.lastOrNull()?.uppercaseChar()

    var secondsLeft by remember(activeRoom.turnDeadline) { mutableStateOf(45) }
    LaunchedEffect(activeRoom.turnDeadline, activeRoom.currentPlayerId, activeRoom.status) {
        while (activeRoom.turnDeadline != null && activeRoom.status in listOf("playing", "final", "sudden_death")) {
            val deadline = runCatching { Instant.parse(activeRoom.turnDeadline) }.getOrNull()
            val left = deadline?.epochSecond?.minus(Instant.now().epochSecond)?.toInt()?.coerceAtLeast(0) ?: 45
            secondsLeft = left
            if (left <= 0) {
                onTimeout()
                break
            }
            delay(1000)
        }
    }

    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("ODA ${activeRoom.code}  •  ${if (activeRoom.language == "tr") "TR" else "EN"}", color = SonHarfCyan, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Text(if (waiting) "Rakip bekleniyor" else if (myTurn) "SIRA SENDE" else "RAKİBİN SIRASI", fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                    Surface(color = if (secondsLeft <= 10) Color(0xFF5A202C) else SonHarfPurple, shape = RoundedCornerShape(999.dp)) {
                        Text("$secondsLeft sn", Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontWeight = FontWeight.Black)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("SEN  $myScore", fontWeight = FontWeight.Black)
                    Text("Kelime ${activeRoom.validWordCount}/45", color = SonHarfMuted)
                    Text("$opponentScore  RAKİP", fontWeight = FontWeight.Black)
                }
                if (myStreak >= 4) Text("🔥 Seri: $myStreak doğru", color = SonHarfGold, fontWeight = FontWeight.Bold)
            }
        }

        if (waiting) {
            Surface(color = SonHarfPurple.copy(alpha = .15f), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("DAVET KODU", color = SonHarfMuted)
                    Text(activeRoom.code, fontSize = 36.sp, fontWeight = FontWeight.Black, letterSpacing = 5.sp, color = SonHarfCyan)
                }
            }
        }

        if (activeRoom.status == "quiz") {
            TriviaPanel(triviaRound, triviaQuestion, onAnswerTrivia)
        } else if (!waiting && activeRoom.status != "finished") {
            Surface(color = SonHarfSurface, shape = RoundedCornerShape(24.dp), modifier = Modifier.weight(1f)) {
                Column(
                    Modifier.fillMaxSize().padding(18.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("SON HARF", color = SonHarfMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(required?.toString() ?: "•", fontSize = 64.sp, fontWeight = FontWeight.Black, color = SonHarfCyan)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (required == null) "İlk kelimeyi yaz" else "${required} ile başlayan yeni bir kelime yaz",
                        color = SonHarfMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    DarkTextField(wordInput, onWordInputChange, if (myTurn) "Kelime" else "Rakibin hamlesini bekle", "Kelime")
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = onSubmitWord,
                        enabled = myTurn && wordInput.trim().length >= 2 && !busy,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SonHarfPurple)
                    ) { Text("GÖNDER", fontWeight = FontWeight.Black) }
                    Text("Geçerli +3 • Hatalı/Süre −1 • Her 5 kusursuz doğru +3", color = SonHarfMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 10.dp))
                }
            }
        }

        if (activeRoom.status == "finished") {
            val won = activeRoom.winnerId == me
            Card(colors = CardDefaults.cardColors(containerColor = if (won) Color(0xFF193A32) else Color(0xFF3A1D27)), shape = RoundedCornerShape(20.dp)) {
                Text(if (won) "KAZANDIN  ✦" else "MAÇ BİTTİ", Modifier.fillMaxWidth().padding(18.dp), textAlign = TextAlign.Center, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
        }

        if (message.isNotBlank()) Text(message, color = SonHarfMuted, fontSize = 11.sp)

        Surface(color = SonHarfSurface, shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("CANLI SOHBET", color = SonHarfCyan, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick = onAllowPhoto) { Text("Fotoğrafı aç", fontSize = 10.sp) }
                        TextButton(onClick = onBlockOpponent) { Text("Engelle", color = Color(0xFFFF8894), fontSize = 10.sp) }
                    }
                }
                if (chat.isNotEmpty()) {
                    Text(chat.takeLast(3).joinToString("\n") { (if (it.senderId == me) "Sen: " else "Rakip: ") + it.body }, fontSize = 12.sp, color = SonHarfMuted)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = chatInput,
                        onValueChange = onChatInputChange,
                        placeholder = { Text("Mesaj yaz", color = SonHarfMuted.copy(alpha = .6f)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SonHarfCyan, unfocusedBorderColor = SonHarfSurface2)
                    )
                    Button(onClick = onSendChat, enabled = chatInput.isNotBlank(), shape = RoundedCornerShape(14.dp)) { Text("➤") }
                }
            }
        }

        OutlinedButton(onClick = onForfeit, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, Color(0xFF6C3340))) {
            Text("PES ET", color = Color(0xFFFF8894))
        }
    }
}

@Composable
private fun TriviaPanel(round: TriviaRoundDto?, question: TriviaQuestionDto?, onAnswer: (Int) -> Unit) {
    if (round == null || question == null) {
        Surface(color = SonHarfSurface, shape = RoundedCornerShape(24.dp), modifier = Modifier.weight(1f)) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
        return
    }

    var unlocked by remember(round.revealAt) { mutableStateOf(false) }
    var readSeconds by remember(round.revealAt) { mutableStateOf(3) }
    LaunchedEffect(round.revealAt) {
        while (true) {
            val reveal = runCatching { Instant.parse(round.revealAt) }.getOrNull()
            val left = reveal?.epochSecond?.minus(Instant.now().epochSecond)?.toInt()?.coerceAtLeast(0) ?: 0
            readSeconds = left
            unlocked = left <= 0
            if (unlocked) break
            delay(250)
        }
    }

    Surface(color = SonHarfSurface, shape = RoundedCornerShape(24.dp), modifier = Modifier.weight(1f)) {
        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.Center) {
            Text("🧠 GENEL KÜLTÜR • +${round.bonusPoints}", color = SonHarfGold, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(14.dp))
            Text(question.question, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(14.dp))
            if (!unlocked) {
                Text("Şıklar $readSeconds saniye sonra açılacak…", color = SonHarfCyan, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            } else {
                listOf(question.optionA, question.optionB, question.optionC, question.optionD).forEachIndexed { index, option ->
                    OutlinedButton(onClick = { onAnswer(index) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(14.dp)) {
                        Text("${'A' + index})  $option", modifier = Modifier.fillMaxWidth())
                    }
                }
                Text("İlk doğru cevap bonusu kapar.", color = SonHarfMuted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun MissingBackendConfig() {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(color = SonHarfPurple.copy(alpha = .15f), shape = RoundedCornerShape(28.dp)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("⚡", fontSize = 34.sp)
                Text("Online altyapı hazır", fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("Supabase bağlantısı tamamlandığında iki telefonlu oda testi aktif olacak.", color = SonHarfMuted, textAlign = TextAlign.Center)
            }
        }
    }
}
