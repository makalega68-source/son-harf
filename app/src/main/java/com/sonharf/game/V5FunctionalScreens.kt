package com.sonharf.game

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.*
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val V5Fire = Color(0xFFEA580C)

@Serializable
private data class V5Profile(
    val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_path") val avatarPath: String? = null,
    @SerialName("avatar_visibility") val avatarVisibility: String = "hidden",
    @SerialName("allow_match_chat") val allowMatchChat: Boolean = true,
    val diamonds: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    @SerialName("best_streak") val bestStreak: Int = 0,
    val rating: Int = 1000,
)

private object V5PhotoStorage {
    private val http = HttpClient(OkHttp)

    private suspend fun headers(): Pair<String, String> {
        val session = SupabaseProvider.client.auth.currentSessionOrNull() ?: error("not_authenticated")
        return session.accessToken to BuildConfig.SUPABASE_KEY
    }

    suspend fun upload(bytes: ByteArray, contentType: String): String {
        val uid = SupabaseProvider.client.auth.currentUserOrNull()?.id ?: error("not_authenticated")
        val ext = when {
            contentType.contains("png", true) -> "png"
            contentType.contains("webp", true) -> "webp"
            else -> "jpg"
        }
        val path = "$uid/avatar-${UUID.randomUUID()}.$ext"
        val (token, apiKey) = headers()
        val response = http.post("${BuildConfig.SUPABASE_URL}/storage/v1/object/profile-photos/$path") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header("apikey", apiKey)
            header("x-upsert", "true")
            contentType(ContentType.parse(contentType.ifBlank { "image/jpeg" }))
            setBody(bytes)
        }
        if (!response.status.isSuccess()) error("avatar_upload_failed_${response.status.value}")
        return path
    }

    suspend fun download(path: String): ByteArray {
        val (token, apiKey) = headers()
        val response = http.get("${BuildConfig.SUPABASE_URL}/storage/v1/object/authenticated/profile-photos/$path") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header("apikey", apiKey)
        }
        if (!response.status.isSuccess()) error("avatar_download_failed_${response.status.value}")
        return response.bodyAsBytes()
    }
}

private suspend fun v5Profile(id: String): V5Profile? =
    SupabaseProvider.client.from("profiles").select { filter { eq("id", id) } }.decodeList<V5Profile>().firstOrNull()

private suspend fun v5SaveAvatar(path: String): V5Profile =
    SupabaseProvider.client.postgrest.rpc("set_avatar_path", buildJsonObject { put("p_path", path) }).decodeSingle()

@Composable
fun V5ProfileScreen(onOpenPreferences: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<V5Profile?>(null) }
    var avatarBytes by remember { mutableStateOf<ByteArray?>(null) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }

    suspend fun refresh() {
        val uid = SupabaseProvider.client.auth.currentUserOrNull()?.id
        profile = uid?.let { runCatching { v5Profile(it) }.getOrNull() }
        avatarBytes = profile?.avatarPath?.let { runCatching { V5PhotoStorage.download(it) }.getOrNull() }
        loading = false
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            runCatching {
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("photo_read_failed")
                }
                require(bytes.isNotEmpty() && bytes.size <= 5 * 1024 * 1024) { "photo_size" }
                val type = context.contentResolver.getType(uri) ?: "image/jpeg"
                require(type in listOf("image/jpeg", "image/png", "image/webp")) { "photo_type" }
                val path = V5PhotoStorage.upload(bytes, type)
                profile = v5SaveAvatar(path)
                avatarBytes = bytes
            }.onSuccess { notice = "Profil fotoğrafı kaydedildi." }
                .onFailure { notice = "Fotoğraf yüklenemedi. JPG/PNG/WEBP ve 5 MB sınırını kontrol et." }
            busy = false
        }
    }

    LaunchedEffect(Unit) { if (SupabaseProvider.configured) refresh() else loading = false }

    if (loading) {
        Box(Modifier.fillMaxSize().background(SonHarfV4Theme.ScreenBg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SonHarfV4Theme.SkyBlue)
        }
        return
    }

    val p = profile
    val total = (p?.wins ?: 0) + (p?.losses ?: 0)
    val rate = if (total == 0) 0 else (p?.wins ?: 0) * 100 / total

    LazyColumn(
        Modifier.fillMaxSize().background(SonHarfV4Theme.ScreenBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("OYUNCU KARTI", color = SonHarfV4Theme.SkyBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                TextButton(onClick = onOpenPreferences) { Text("GİZLİLİK & TERCİHLER", color = SonHarfV4Theme.TextMuted, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            }
            HorizontalDivider(color = SonHarfV4Theme.BorderLight)
        }
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    V5AvatarBytes(avatarBytes, p?.displayName ?: "Oyuncu", 98)
                    IconButton(
                        onClick = { picker.launch("image/*") },
                        enabled = !busy,
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(SonHarfV4Theme.SkyBlue),
                    ) { Icon(Icons.Rounded.PhotoCamera, "Fotoğrafı değiştir", tint = Color.White, modifier = Modifier.size(18.dp)) }
                }
                Spacer(Modifier.height(8.dp))
                Text(p?.displayName ?: "Oyuncu", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = SonHarfV4Theme.TextDark)
                Text("SON HARF OYUNCUSU", fontSize = 12.sp, color = SonHarfV4Theme.TextMuted)
                if (notice.isNotBlank()) Text(notice, color = SonHarfV4Theme.SkyBlueDark, fontSize = 12.sp, textAlign = TextAlign.Center)
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth(.55f), color = SonHarfV4Theme.SkyBlue)
            }
        }
        item {
            Text("İSTATİSTİKLERİM", fontWeight = FontWeight.Bold, color = SonHarfV4Theme.TextDark)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V5Metric("${p?.wins ?: 0}", "Galibiyet", Modifier.weight(1f))
                V5Metric("${p?.losses ?: 0}", "Mağlubiyet", Modifier.weight(1f))
                V5Metric("%$rate", "Kazanma", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V5Metric("$total", "Maç", Modifier.weight(1f))
                V5Metric("${p?.diamonds ?: 0}", "Elmas", Modifier.weight(1f))
                V5Metric("${p?.bestStreak ?: 0}", "En İyi Seri", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun V5AvatarBytes(bytes: ByteArray?, name: String, size: Int) {
    val bitmap = remember(bytes) { bytes?.let { runCatching { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }.getOrNull() } }
    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = bitmap,
            contentDescription = "$name profil fotoğrafı",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size.dp).clip(CircleShape).background(SonHarfV4Theme.SkyBlueLight),
        )
    } else {
        Box(Modifier.size(size.dp).clip(CircleShape).background(SonHarfV4Theme.SkyBlueLight), contentAlignment = Alignment.Center) {
            Text(name.take(1).uppercase(), color = SonHarfV4Theme.SkyBlueDark, fontSize = (size / 2.2).sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun V5Metric(value: String, label: String, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(14.dp), color = Color.White, border = BorderStroke(1.dp, SonHarfV4Theme.BorderLight)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Black, color = SonHarfV4Theme.SkyBlueDark, fontSize = 18.sp)
            Text(label, color = SonHarfV4Theme.TextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
fun V5BattleScreen(onLeaveBattle: () -> Unit) {
    if (!SupabaseProvider.configured) {
        Box(Modifier.fillMaxSize().background(SonHarfV4Theme.ScreenBg), contentAlignment = Alignment.Center) { Text("Sunucu bağlantısı yok", color = SonHarfV4Theme.TextDark) }
        return
    }
    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<V5Profile?>(null) }
    var opponent by remember { mutableStateOf<V5Profile?>(null) }
    var room by remember { mutableStateOf<GameRoomDto?>(null) }
    var words by remember { mutableStateOf<List<GameWordDto>>(emptyList()) }
    var chat by remember { mutableStateOf<List<ChatMessageDto>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("Düelloya hazır") }
    var matching by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var showChat by remember { mutableStateOf(false) }
    var roomJob by remember { mutableStateOf<Job?>(null) }
    var wordsJob by remember { mutableStateOf<Job?>(null) }
    var chatJob by remember { mutableStateOf<Job?>(null) }
    var matchJob by remember { mutableStateOf<Job?>(null) }

    suspend fun loadMe() {
        if (backend.currentUserId() == null) backend.ensurePlayer("Oyuncu")
        backend.currentUserId()?.let { profile = runCatching { v5Profile(it) }.getOrNull() }
    }

    suspend fun findActive(): GameRoomDto? {
        val me = backend.currentUserId() ?: return null
        return SupabaseProvider.client.from("game_rooms").select().decodeList<GameRoomDto>()
            .filter { (it.hostId == me || it.guestId == me) && it.status in listOf("waiting", "playing", "quiz", "final", "sudden_death", "paused") }
            .maxByOrNull { it.validWordCount }
    }

    suspend fun loadOpponent(r: GameRoomDto) {
        if (r.isBot) { opponent = null; return }
        val me = backend.currentUserId()
        val id = if (r.hostId == me) r.guestId else r.hostId
        opponent = id?.let { runCatching { v5Profile(it) }.getOrNull() }
    }

    fun observe(r: GameRoomDto) {
        roomJob?.cancel(); wordsJob?.cancel(); chatJob?.cancel(); matchJob?.cancel(); matching = false
        scope.launch { loadOpponent(r) }
        roomJob = scope.launch { backend.observeRoom(r.id).catch { notice = "Bağlantı yenileniyor…" }.collect { room = it; loadOpponent(it) } }
        wordsJob = scope.launch { backend.observeWords(r.id).catch { }.collect { words = it } }
        chatJob = scope.launch { backend.observeChat(r.id).catch { }.collect { chat = it } }
    }

    LaunchedEffect(Unit) {
        loadMe()
        findActive()?.let { room = it; observe(it) }
    }

    val active = room
    if (active == null) {
        V5Lobby(
            profile = profile,
            matching = matching,
            notice = notice,
            onBack = onLeaveBattle,
            onRandom = {
                scope.launch {
                    busy = true
                    runCatching { loadMe(); backend.startRandomMatchmaking("tr") }
                        .onSuccess {
                            matching = true; notice = "Rakip aranıyor…"
                            matchJob = launch {
                                while (matching && room == null) {
                                    backend.pollRandomMatchmakingRoom()?.let { room = it; observe(it); return@launch }
                                    delay(800)
                                }
                            }
                        }
                        .onFailure { notice = "Eşleşme başlatılamadı." }
                    busy = false
                }
            },
            onBot = {
                SonHarfGameModeState.mode = "normal"
                scope.launch {
                    busy = true
                    runCatching { backend.startRandomMatchmaking("tr") }
                        .onFailure { notice = "Bot maçı başlatılamadı." }
                    busy = false
                }
            },
            onCancel = { scope.launch { matching = false; matchJob?.cancel(); runCatching { backend.cancelRandomMatchmaking() }; notice = "Eşleşme iptal edildi" } },
        )
        return
    }

    val me = backend.currentUserId()
    LaunchedEffect(active.currentPlayerId, active.validWordCount, active.roundNo) { input = "" }
    LaunchedEffect(active.id) {
        while (true) {
            if (!active.isBot && active.status != "waiting") runCatching { backend.heartbeatRoom(active.id) }.onSuccess { room = it }
            delay(5000)
        }
    }
    LaunchedEffect(active.id, active.status, active.botTurn, active.validWordCount) {
        if (active.isBot && active.botTurn && active.status in listOf("playing", "final", "sudden_death")) {
            delay(1400)
            runCatching { backend.botTakeTurn(active.id) }.onSuccess { room = it }
        }
    }

    V5Arena(
        room = active,
        me = me,
        profile = profile,
        opponent = opponent,
        words = words,
        input = input,
        notice = notice,
        busy = busy,
        onInput = { input = it.take(40) },
        onSubmit = {
            val submitted = input.trim()
            if (submitted.length < 2) return@V5Arena
            scope.launch {
                busy = true
                runCatching { backend.submitWord(active.id, submitted) }
                    .onSuccess { room = it; input = ""; notice = "${submitted.uppercase()} kabul edildi" }
                    .onFailure { notice = when {
                        "not_your_turn" in it.message.orEmpty() -> "Sıra rakibinde."
                        "wrong_start_letter" in it.message.orEmpty() -> "Kelime doğru harfle başlamalı."
                        "word_already_used" in it.message.orEmpty() -> "Bu kelime daha önce kullanıldı."
                        else -> "Kelime gönderilemedi."
                    } }
                busy = false
            }
        },
        onForfeit = { scope.launch { runCatching { backend.forfeit(active.id) }.onSuccess { room = it } } },
        onExit = { roomJob?.cancel(); wordsJob?.cancel(); chatJob?.cancel(); onLeaveBattle() },
        onChat = { showChat = true },
    )

    if (showChat) {
        V5ChatDrawer(
            messages = chat,
            me = me,
            enabled = profile?.allowMatchChat != false,
            onDismiss = { showChat = false },
            onSend = { text ->
                scope.launch {
                    runCatching { backend.sendChat(active.id, text) }
                        .onSuccess { chat = runCatching { backend.getChat(active.id) }.getOrDefault(chat) }
                        .onFailure { notice = "Mesaj gönderilemedi." }
                }
            },
        )
    }
}

@Composable
private fun V5Lobby(profile: V5Profile?, matching: Boolean, notice: String, onBack: () -> Unit, onRandom: () -> Unit, onBot: () -> Unit, onCancel: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().background(SonHarfV4Theme.ScreenBg), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Geri") }; Spacer(Modifier.weight(1f)); Text("SON HARF", fontWeight = FontWeight.Black, fontSize = 22.sp, color = SonHarfV4Theme.TextDark); Spacer(Modifier.weight(1f)); Spacer(Modifier.width(48.dp)) } }
        item {
            Surface(shape = RoundedCornerShape(18.dp), color = Color.White, border = BorderStroke(1.dp, SonHarfV4Theme.BorderLight)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(54.dp).clip(CircleShape).background(SonHarfV4Theme.SkyBlueLight), contentAlignment = Alignment.Center) { Text(profile?.displayName?.take(1)?.uppercase() ?: "O", fontWeight = FontWeight.Black, color = SonHarfV4Theme.SkyBlueDark, fontSize = 22.sp) }
                    Spacer(Modifier.width(12.dp)); Column { Text(profile?.displayName ?: "Oyuncu", fontWeight = FontWeight.Bold, color = SonHarfV4Theme.TextDark); Text(if (matching) "Rakip aranıyor…" else "Düelloya hazırsın", color = SonHarfV4Theme.TextMuted) }
                }
            }
        }
        if (matching) {
            item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = SonHarfV4Theme.SkyBlue) } }
            item { OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().height(56.dp), border = BorderStroke(1.dp, SonHarfV4Theme.Red)) { Text("EŞLEŞMEYİ İPTAL ET", color = SonHarfV4Theme.Red) } }
        } else {
            item { Button(onClick = onRandom, modifier = Modifier.fillMaxWidth().height(64.dp), colors = ButtonDefaults.buttonColors(containerColor = SonHarfV4Theme.SkyBlue), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Rounded.Bolt, null); Spacer(Modifier.width(8.dp)); Text("1v1 HIZLI KARŞILAŞMA", fontWeight = FontWeight.Black) } }
            item { OutlinedButton(onClick = onBot, modifier = Modifier.fillMaxWidth().height(54.dp), border = BorderStroke(1.dp, SonHarfV4Theme.SkyBlue)) { Icon(Icons.Rounded.SmartToy, null); Spacer(Modifier.width(8.dp)); Text("BOT İLE PRATİK", color = SonHarfV4Theme.SkyBlueDark) } }
        }
        item { Text(notice, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = SonHarfV4Theme.TextMuted, fontSize = 12.sp) }
    }
}

@Composable
private fun V5Arena(
    room: GameRoomDto,
    me: String?,
    profile: V5Profile?,
    opponent: V5Profile?,
    words: List<GameWordDto>,
    input: String,
    notice: String,
    busy: Boolean,
    onInput: (String) -> Unit,
    onSubmit: () -> Unit,
    onForfeit: () -> Unit,
    onExit: () -> Unit,
    onChat: () -> Unit,
) {
    val host = me == room.hostId
    val myScore = if (host) room.hostScore else room.guestScore
    val oppScore = if (host) room.guestScore else room.hostScore
    val myStreak = if (host) room.hostStreak else room.guestStreak
    val myTurn = room.currentPlayerId == me && room.status in listOf("playing", "final", "sudden_death")
    val lastWord = words.lastOrNull()?.word?.uppercase().orEmpty()
    val required = words.lastOrNull()?.normalizedWord?.lastOrNull()?.uppercaseChar()
    var seconds by remember(room.turnDeadline) { mutableStateOf(45) }

    LaunchedEffect(room.turnDeadline, room.currentPlayerId, room.status) {
        while (room.turnDeadline != null && room.status in listOf("playing", "final", "sudden_death")) {
            seconds = runCatching { (Instant.parse(room.turnDeadline).epochSecond - Instant.now().epochSecond).toInt().coerceAtLeast(0) }.getOrDefault(45)
            delay(1000)
        }
    }

    Column(Modifier.fillMaxSize().background(SonHarfV4Theme.ScreenBg).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onForfeit, border = BorderStroke(1.dp, SonHarfV4Theme.Red), contentPadding = PaddingValues(horizontal = 9.dp, vertical = 6.dp)) { Icon(Icons.Rounded.Flag, null, tint = SonHarfV4Theme.Red, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Pes Et", color = SonHarfV4Theme.Red, fontSize = 12.sp) }
            Spacer(Modifier.weight(1f)); if (myStreak > 1) Text("🔥 ${myStreak}x Seri", color = V5Fire, fontWeight = FontWeight.Black, fontSize = 13.sp); Spacer(Modifier.weight(1f)); IconButton(onClick = onExit) { Icon(Icons.Rounded.Close, "Ayrıl") }
        }
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Color.White, border = BorderStroke(1.dp, SonHarfV4Theme.BorderLight)) {
            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                V5Player(profile?.displayName ?: "Sen", myScore, myTurn, Modifier.weight(1f))
                Box(Modifier.size(52.dp).clip(CircleShape).background(SonHarfV4Theme.SkyBlueLight).then(Modifier), contentAlignment = Alignment.Center) { Text("$seconds", color = SonHarfV4Theme.SkyBlueDark, fontWeight = FontWeight.Black, fontSize = 19.sp) }
                V5Player(if (room.isBot) room.botName ?: "KelimeBot" else opponent?.displayName ?: "Rakip", oppScore, !myTurn, Modifier.weight(1f))
            }
        }
        Surface(Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(20.dp), color = Color.White, border = BorderStroke(1.dp, SonHarfV4Theme.BorderLight)) {
            Box(Modifier.fillMaxSize().padding(14.dp)) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(if (myTurn) "Sıra Sende!" else "Rakibin Sırası…", fontWeight = FontWeight.Bold, color = if (myTurn) SonHarfV4Theme.Green else SonHarfV4Theme.TextMuted)
                    Spacer(Modifier.height(8.dp))
                    Text(if (required == null) "İlk kelimeyi yaz" else "Başlangıç Harfi: '$required'", color = SonHarfV4Theme.Amber, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    V5Tiles(if (input.isBlank()) required?.toString().orEmpty() else input)
                    Spacer(Modifier.height(14.dp))
                    if (lastWord.isNotBlank()) Text("Son Kelime: $lastWord", color = SonHarfV4Theme.TextMuted, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp)); Text(notice, color = SonHarfV4Theme.TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
                IconButton(onClick = onChat, modifier = Modifier.align(Alignment.BottomEnd).size(46.dp).clip(RoundedCornerShape(13.dp)).background(SonHarfV4Theme.SkyBlue)) { Icon(Icons.Rounded.ChatBubble, "Sohbet", tint = Color.White) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { if (myTurn) onInput(required?.toString().orEmpty()) }, enabled = myTurn && !busy, modifier = Modifier.weight(1f).height(42.dp), border = BorderStroke(1.dp, SonHarfV4Theme.SkyBlue)) { Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text("Temizle") }
            OutlinedButton(onClick = { }, enabled = false, modifier = Modifier.weight(1f).height(42.dp), border = BorderStroke(1.dp, SonHarfV4Theme.BorderLight)) { Icon(Icons.Rounded.Lightbulb, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text("İpucu yakında") }
        }
        V5Keyboard(
            enabled = myTurn && !busy,
            submitEnabled = myTurn && !busy && input.length >= 2 && (required == null || input.firstOrNull()?.uppercaseChar() == required),
            onKey = { c ->
                if (!myTurn || busy) return@V5Keyboard
                val next = if (input.isEmpty() && required != null) "$required$c" else input + c
                onInput(next)
            },
            onDelete = {
                if (input.length > if (required == null) 0 else 1) onInput(input.dropLast(1))
            },
            onSubmit = onSubmit,
        )
    }
}

@Composable private fun V5Player(name: String, score: Int, active: Boolean, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(42.dp).clip(CircleShape).background(SonHarfV4Theme.SkyBlueLight), contentAlignment = Alignment.Center) { Text(name.take(1).uppercase(), color = SonHarfV4Theme.SkyBlueDark, fontWeight = FontWeight.Black) }; Text(name, maxLines = 1, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SonHarfV4Theme.TextDark); Text("$score puan", fontSize = 10.sp, color = if (active) SonHarfV4Theme.SkyBlueDark else SonHarfV4Theme.TextMuted) }
}

@Composable private fun V5Tiles(word: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        word.take(8).forEachIndexed { index, c ->
            val bg = if (index == 0 || (index == word.take(8).lastIndex && word.length > 1)) SonHarfV4Theme.Amber else SonHarfV4Theme.SkyBlue
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(bg), contentAlignment = Alignment.Center) { Text(c.toString(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp) }
        }
    }
}

@Composable
private fun V5Keyboard(enabled: Boolean, submitEnabled: Boolean, onKey: (Char) -> Unit, onDelete: () -> Unit, onSubmit: () -> Unit) {
    val r1 = listOf('Q','W','E','R','T','Y','U','I','O','P','Ğ','Ü')
    val r2 = listOf('A','S','D','F','G','H','J','K','L','Ş','İ')
    val r3 = listOf('Z','X','C','V','B','N','M','Ö','Ç')
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        V5KeyRow(r1, enabled, onKey); V5KeyRow(r2, enabled, onKey)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Button(onClick = onSubmit, enabled = submitEnabled, modifier = Modifier.weight(2f).height(46.dp), colors = ButtonDefaults.buttonColors(containerColor = SonHarfV4Theme.SkyBlue), contentPadding = PaddingValues(0.dp)) { Text("ONAY", fontWeight = FontWeight.Black, fontSize = 12.sp) }
            r3.forEach { c -> V5Key(c, Modifier.weight(1f), enabled) { onKey(c) } }
            OutlinedButton(onClick = onDelete, enabled = enabled, modifier = Modifier.weight(1.7f).height(46.dp), border = BorderStroke(1.dp, SonHarfV4Theme.Red), contentPadding = PaddingValues(0.dp)) { Text("SİL", color = SonHarfV4Theme.Red, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
        }
    }
}

@Composable private fun V5KeyRow(chars: List<Char>, enabled: Boolean, onKey: (Char) -> Unit) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) { chars.forEach { c -> V5Key(c, Modifier.weight(1f), enabled) { onKey(c) } } } }
@Composable private fun V5Key(c: Char, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) { Surface(onClick = onClick, enabled = enabled, modifier = modifier.height(46.dp), color = Color.White, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, SonHarfV4Theme.BorderLight)) { Box(contentAlignment = Alignment.Center) { Text(c.toString(), color = if (enabled) SonHarfV4Theme.TextDark else SonHarfV4Theme.TextMuted, fontWeight = FontWeight.Bold, fontSize = 15.sp) } } }

@Composable
private fun V5ChatDrawer(messages: List<ChatMessageDto>, me: String?, enabled: Boolean, onDismiss: () -> Unit, onSend: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    val quick = listOf("İyi oyunlar!", "Çok iyi kelime!", "Hadi bakalım :)", "Tebrikler!")
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .35f)).clickable { onDismiss() }) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.66f).align(Alignment.BottomCenter).clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)).background(Color.White).clickable(enabled = false) {}.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Oyun İçi Sohbet", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = SonHarfV4Theme.TextDark); IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Kapat") } }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(vertical = 7.dp)) { items(quick) { phrase -> SuggestionChip(onClick = { if (enabled) onSend(phrase) }, enabled = enabled, label = { Text(phrase, fontSize = 12.sp) }) } }
            HorizontalDivider(color = SonHarfV4Theme.BorderLight)
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(vertical = 9.dp)) {
                items(messages, key = { it.id }) { msg -> Row(Modifier.fillMaxWidth(), horizontalArrangement = if (msg.senderId == me) Arrangement.End else Arrangement.Start) { Surface(shape = RoundedCornerShape(12.dp), color = if (msg.senderId == me) SonHarfV4Theme.SkyBlue else SonHarfV4Theme.SkyBlueLight) { Text(msg.body, Modifier.padding(horizontal = 11.dp, vertical = 8.dp), color = if (msg.senderId == me) Color.White else SonHarfV4Theme.TextDark, fontSize = 13.sp) } } }
            }
            if (!enabled) Text("Maç sohbeti profil ayarlarında kapalı.", color = SonHarfV4Theme.TextMuted, fontSize = 12.sp)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(input, { input = it.take(300) }, enabled = enabled, modifier = Modifier.weight(1f), singleLine = true, placeholder = { Text("Mesaj yaz…") })
                Spacer(Modifier.width(7.dp)); IconButton(onClick = { val text = input.trim(); if (text.isNotEmpty() && enabled) { onSend(text); input = "" } }, enabled = enabled, modifier = Modifier.clip(CircleShape).background(SonHarfV4Theme.SkyBlue)) { Icon(Icons.Rounded.Send, "Gönder", tint = Color.White) }
            }
        }
    }
}
