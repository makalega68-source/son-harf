package com.sonharf.game

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    var message by remember { mutableStateOf("İki telefonla test için bir oda oluştur veya koda katıl.") }
    var roomJob by remember { mutableStateOf<Job?>(null) }
    var wordsJob by remember { mutableStateOf<Job?>(null) }
    var chatJob by remember { mutableStateOf<Job?>(null) }

    fun readableError(t: Throwable): String {
        val raw = t.message.orEmpty()
        return when {
            "not_your_turn" in raw -> "Sıra rakibinde."
            "wrong_start_letter" in raw -> "Kelime yanlış harfle başlıyor."
            "word_already_used" in raw -> "Bu kelime daha önce kullanıldı."
            "turn_expired" in raw -> "Süren doldu."
            "room_not_available" in raw -> "Oda bulunamadı veya dolu."
            else -> raw.ifBlank { "Bağlantı hatası oluştu." }
        }
    }

    fun startObservers(active: GameRoomDto) {
        roomJob?.cancel(); wordsJob?.cancel(); chatJob?.cancel()
        roomJob = scope.launch {
            backend.observeRoom(active.id)
                .catch { message = readableError(it) }
                .collect { room = it }
        }
        wordsJob = scope.launch {
            backend.observeWords(active.id)
                .catch { message = readableError(it) }
                .collect { words = it }
        }
        chatJob = scope.launch {
            backend.observeChat(active.id)
                .catch { message = readableError(it) }
                .collect { chat = it }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            roomJob?.cancel(); wordsJob?.cancel(); chatJob?.cancel()
        }
    }

    if (room == null) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Online Düello", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(message)
            OutlinedTextField(
                value = playerName,
                onValueChange = { playerName = it.take(24) },
                label = { Text("Oyuncu adı") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        runCatching {
                            backend.ensurePlayer(playerName)
                            backend.createRoom()
                        }.onSuccess {
                            room = it
                            roomCode = it.code
                            message = "Oda hazır. Kodu diğer telefona yaz: ${it.code}"
                            startObservers(it)
                        }.onFailure { message = readableError(it) }
                        busy = false
                    }
                },
                enabled = !busy && playerName.trim().length >= 2,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (busy) "Hazırlanıyor…" else "Özel Oda Oluştur") }

            HorizontalDivider()
            OutlinedTextField(
                value = roomCode,
                onValueChange = { roomCode = it.uppercase().take(6) },
                label = { Text("6 haneli oda kodu") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(
                onClick = {
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
                },
                enabled = !busy && playerName.trim().length >= 2 && roomCode.length == 6,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Odaya Katıl") }
        }
        return
    }

    val activeRoom = room ?: return
    val me = backend.currentUserId()
    val myTurn = activeRoom.currentPlayerId == me && activeRoom.status == "playing"
    val waiting = activeRoom.status == "waiting"

    Column(
        Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("ODA ${activeRoom.code}", fontWeight = FontWeight.Black)
                Text(if (waiting) "Rakip bekleniyor" else if (myTurn) "Sıra sende" else "Rakibin sırası")
            }
            Surface(shape = RoundedCornerShape(999.dp), tonalElevation = 3.dp) {
                Text("45 sn", Modifier.padding(horizontal = 14.dp, vertical = 7.dp), fontWeight = FontWeight.Bold)
            }
        }

        if (waiting) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Oda kodunu paylaş", fontWeight = FontWeight.Bold)
                    Text(activeRoom.code, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                    Text("İkinci telefonda Odaya Katıl bölümüne bu kodu yaz.")
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            if (words.isEmpty()) {
                item { Text(if (waiting) "Rakip bağlandığında oyun başlayacak." else "İlk kelimeyi oynayacak kişi yazabilir.") }
            }
            items(words, key = { it.id }) { item ->
                Surface(shape = RoundedCornerShape(14.dp), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(item.word.uppercase(), fontWeight = FontWeight.Bold)
                        Text(if (item.playerId == me) "Sen" else "Rakip")
                    }
                }
            }
        }

        if (activeRoom.status == "playing") {
            val required = words.lastOrNull()?.normalizedWord?.lastOrNull()?.uppercaseChar()
            if (required != null) Text("Yeni kelime $required ile başlamalı", color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(
                value = wordInput,
                onValueChange = { wordInput = it.take(40) },
                label = { Text(if (myTurn) "Kelime yaz" else "Rakibin hamlesini bekle") },
                enabled = myTurn,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        scope.launch {
                            busy = true
                            runCatching { backend.submitWord(activeRoom.id, wordInput) }
                                .onSuccess { room = it; wordInput = ""; message = "Hamle gönderildi." }
                                .onFailure { message = readableError(it) }
                            busy = false
                        }
                    },
                    enabled = myTurn && wordInput.trim().length >= 2 && !busy,
                    modifier = Modifier.weight(1f)
                ) { Text("Gönder") }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            runCatching { backend.forfeit(activeRoom.id) }
                                .onSuccess { room = it }
                                .onFailure { message = readableError(it) }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Pes Et") }
            }
        }

        if (activeRoom.status == "finished") {
            val won = activeRoom.winnerId == me
            Card(Modifier.fillMaxWidth()) {
                Text(if (won) "Kazandın 🎉" else "Maç bitti", Modifier.padding(18.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }

        Text(message, style = MaterialTheme.typography.bodySmall)
        HorizontalDivider()
        Text("Sohbet", fontWeight = FontWeight.Bold)
        if (chat.isNotEmpty()) {
            Text(chat.takeLast(3).joinToString("\n") { (if (it.senderId == me) "Sen: " else "Rakip: ") + it.body })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = chatInput,
                onValueChange = { chatInput = it.take(300) },
                label = { Text("Mesaj") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    scope.launch {
                        runCatching { backend.sendChat(activeRoom.id, chatInput) }
                            .onSuccess { chatInput = "" }
                            .onFailure { message = readableError(it) }
                    }
                },
                enabled = chatInput.isNotBlank()
            ) { Text("Gönder") }
        }
    }
}

@Composable
private fun MissingBackendConfig() {
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Online test altyapısı hazır", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text("Supabase publishable key bağlandığında iki telefonlu oda testi aktif olacak.")
    }
}
