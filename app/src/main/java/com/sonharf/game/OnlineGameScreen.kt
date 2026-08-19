package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.sonharf.game.data.ChatMessageDto
import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.GameWordDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import kotlinx.coroutines.Job
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
    var room by remember { mutableStateOf<GameRoomDto?>(null) }
    var words by remember { mutableStateOf<List<GameWordDto>>(emptyList()) }
    var chat by remember { mutableStateOf<List<ChatMessageDto>>(emptyList()) }
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
            "anonymous_provider_disabled" in raw -> "Supabase'te anonim giriş kapalı. Authentication > Providers > Anonymous bölümünden etkinleştir."
            "not_your_turn" in raw -> "Sıra rakibinde."
            "wrong_start_letter" in raw -> "Kelime yanlış harfle başlıyor."
            "word_already_used" in raw -> "Bu kelime daha önce kullanıldı."
            "turn_expired" in raw -> "Süren doldu."
            "room_not_available" in raw -> "Oda bulunamadı veya dolu."
            else -> raw.substringBefore("URL:").trim().ifBlank { "Bağlantı hatası oluştu." }
        }
    }

    fun startObservers(active: GameRoomDto) {
        roomJob?.cancel(); wordsJob?.cancel(); chatJob?.cancel()
        roomJob = scope.launch {
            backend.observeRoom(active.id).catch { message = readableError(it) }.collect { room = it }
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
                busy = busy,
                message = message,
                onCreateRoom = {
                    scope.launch {
                        busy = true
                        runCatching {
                            backend.ensurePlayer(playerName)
                            backend.createRoom()
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
            val myTurn = activeRoom.currentPlayerId == me && activeRoom.status == "playing"
            val waiting = activeRoom.status == "waiting"

            ActiveGameScreen(
                activeRoom = activeRoom,
                me = me,
                myTurn = myTurn,
                waiting = waiting,
                words = words,
                chat = chat,
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
                            .onSuccess { room = it; wordInput = ""; message = "Hamle gönderildi." }
                            .onFailure { message = readableError(it) }
                        busy = false
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
                }
            )
        }
    }
}

@Composable
private fun LobbyScreen(
    playerName: String,
    onPlayerNameChange: (String) -> Unit,
    roomCode: String,
    onRoomCodeChange: (String) -> Unit,
    busy: Boolean,
    message: String,
    onCreateRoom: () -> Unit,
    onJoinRoom: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("ONLINE DÜELLO", fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text("Arkadaşını çağır, son harfle kapış.", color = SonHarfMuted)
        }

        item {
            Surface(color = SonHarfPurple.copy(alpha = .12f), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, SonHarfPurple.copy(alpha = .35f))) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("⚡", fontSize = 18.sp)
                    Text(message, color = SonHarfText, fontSize = 13.sp, lineHeight = 18.sp)
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("OYUNCU KİMLİĞİ", color = SonHarfCyan, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    DarkTextField(value = playerName, onValueChange = onPlayerNameChange, label = "Oyuncu adı", placeholder = "Örn. Ümit")
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("ODA OLUŞTUR", color = SonHarfCyan, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Text("Davet kodunu sen üret", color = SonHarfMuted, fontSize = 12.sp)
                        }
                        Text("＋", fontSize = 26.sp, color = SonHarfPurple)
                    }
                    Button(
                        onClick = onCreateRoom,
                        enabled = !busy && playerName.trim().length >= 2,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SonHarfPurple)
                    ) { Text(if (busy) "HAZIRLANIYOR…" else "ÖZEL ODA OLUŞTUR", fontWeight = FontWeight.Black) }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HorizontalDivider(Modifier.weight(1f), color = SonHarfSurface2)
                Text("VEYA", color = SonHarfMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                HorizontalDivider(Modifier.weight(1f), color = SonHarfSurface2)
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("KODLA KATIL", color = SonHarfGold, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
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
        }
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
    wordInput: String,
    onWordInputChange: (String) -> Unit,
    chatInput: String,
    onChatInputChange: (String) -> Unit,
    message: String,
    busy: Boolean,
    onSubmitWord: () -> Unit,
    onForfeit: () -> Unit,
    onSendChat: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(22.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("ODA ${activeRoom.code}", color = SonHarfCyan, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Text(if (waiting) "Rakip bekleniyor" else if (myTurn) "SIRA SENDE" else "RAKİBİN SIRASI", fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
                Surface(color = if (myTurn) SonHarfPurple else SonHarfSurface2, shape = RoundedCornerShape(999.dp)) {
                    Text("45 sn", Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontWeight = FontWeight.Black)
                }
            }
        }

        if (waiting) {
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfPurple.copy(alpha = .15f)), shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, SonHarfPurple.copy(alpha = .35f))) {
                Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("ARKADAŞINA BU KODU GÖNDER", color = SonHarfMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(activeRoom.code, fontSize = 38.sp, fontWeight = FontWeight.Black, letterSpacing = 5.sp, color = SonHarfCyan)
                    Text("İkinci telefonda Kodla Katıl alanına yazılmalı.", color = SonHarfMuted, fontSize = 12.sp)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            if (words.isEmpty()) {
                item {
                    Surface(color = SonHarfSurface.copy(alpha = .7f), shape = RoundedCornerShape(18.dp)) {
                        Text(
                            if (waiting) "Rakip bağlandığında oyun başlayacak." else "İlk kelimeyi oynayacak kişi hamlesini yapabilir.",
                            Modifier.fillMaxWidth().padding(18.dp),
                            color = SonHarfMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            items(words, key = { it.id }) { item ->
                val mine = item.playerId == me
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                    Surface(
                        color = if (mine) SonHarfPurple.copy(alpha = .28f) else SonHarfSurface,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, if (mine) SonHarfPurple.copy(alpha = .5f) else SonHarfSurface2)
                    ) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                            Text(item.word.uppercase(), fontWeight = FontWeight.Black, fontSize = 18.sp)
                            Text(if (mine) "Sen" else "Rakip", color = SonHarfMuted, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        if (activeRoom.status == "playing") {
            val required = words.lastOrNull()?.normalizedWord?.lastOrNull()?.uppercaseChar()
            if (required != null) {
                Surface(color = SonHarfCyan.copy(alpha = .1f), shape = RoundedCornerShape(14.dp)) {
                    Text("Yeni kelime  $required  ile başlamalı", Modifier.fillMaxWidth().padding(10.dp), color = SonHarfCyan, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                }
            }
            DarkTextField(wordInput, onWordInputChange, if (myTurn) "Kelime yaz" else "Rakibin hamlesini bekle", "Kelime")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onSubmitWord,
                    enabled = myTurn && wordInput.trim().length >= 2 && !busy,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SonHarfPurple)
                ) { Text("GÖNDER", fontWeight = FontWeight.Black) }
                OutlinedButton(
                    onClick = onForfeit,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF6B7A)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF8894))
                ) { Text("PES ET") }
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
                Text("CANLI SOHBET", color = SonHarfCyan, fontSize = 11.sp, fontWeight = FontWeight.Black)
                if (chat.isNotEmpty()) {
                    Text(chat.takeLast(2).joinToString("\n") { (if (it.senderId == me) "Sen: " else "Rakip: ") + it.body }, fontSize = 12.sp, color = SonHarfMuted)
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
                    Button(onClick = onSendChat, enabled = chatInput.isNotBlank(), shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
                        Text("➤")
                    }
                }
            }
        }
    }
}

@Composable
private fun MissingBackendConfig() {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(color = SonHarfPurple.copy(alpha = .15f), shape = RoundedCornerShape(28.dp)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("⚡", fontSize = 34.sp)
                Text("Online altyapı hazır", fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("Supabase publishable key bağlandığında iki telefonlu oda testi aktif olacak.", color = SonHarfMuted, textAlign = TextAlign.Center)
            }
        }
    }
}
