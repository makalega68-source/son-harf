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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

private val NBg = Color(0xFF030613)
private val NPanel = Color(0xFF081127)
private val NPanel2 = Color(0xFF0E1732)
private val NCyan = Color(0xFF00E9FF)
private val NBlue = Color(0xFF2A75FF)
private val NPurple = Color(0xFF7A35FF)
private val NPink = Color(0xFFFF3FCF)
private val NGold = Color(0xFFFFB817)
private val NText = Color(0xFFF7F8FF)
private val NMuted = Color(0xFF8D98B8)

enum class MockScreen { HOME, GAME, PRIVATE, SHOP, PROFILE, LEAGUE, MORE }

@Composable
fun MockupSonHarfApp() {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var screen by remember { mutableStateOf(MockScreen.HOME) }
    var profile by remember { mutableStateOf<ProfileDto?>(null) }

    LaunchedEffect(Unit) {
        if (backend != null) {
            val id = backend.currentUserId()
            if (id != null) profile = runCatching { backend.getProfile(id) }.getOrNull()
        }
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF040717), Color(0xFF05081C), Color(0xFF02040D)))
        )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (screen !in listOf(MockScreen.GAME, MockScreen.PRIVATE, MockScreen.LEAGUE)) {
                    MockBottomBar(screen) { screen = it }
                }
            }
        ) { pad ->
            Box(Modifier.fillMaxSize().padding(pad)) {
                when (screen) {
                    MockScreen.HOME -> MockHome(profile, { screen = MockScreen.GAME }, { screen = MockScreen.PRIVATE }, { screen = MockScreen.LEAGUE }, { screen = MockScreen.MORE })
                    MockScreen.GAME -> MockGameScreen(onBack = { screen = MockScreen.HOME }, initialPrivate = false)
                    MockScreen.PRIVATE -> MockGameScreen(onBack = { screen = MockScreen.HOME }, initialPrivate = true)
                    MockScreen.SHOP -> MockShop(profile)
                    MockScreen.PROFILE -> MockProfile(profile)
                    MockScreen.LEAGUE -> MockLeague(backend, profile, onBack = { screen = MockScreen.HOME }, onPlay = { screen = MockScreen.GAME })
                    MockScreen.MORE -> MockMore(profile, onLeague = { screen = MockScreen.LEAGUE }, onPrivate = { screen = MockScreen.PRIVATE })
                }
            }
        }
    }
}

@Composable
private fun MockBottomBar(current: MockScreen, onGo: (MockScreen) -> Unit) {
    Surface(color = Color(0xFF050A18), shadowElevation = 18.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceAround) {
            listOf(
                Triple(MockScreen.HOME, "⌂", "Ana Sayfa"),
                Triple(MockScreen.GAME, "⚔", "Oyna"),
                Triple(MockScreen.SHOP, "🛒", "Mağaza"),
                Triple(MockScreen.PROFILE, "♙", "Profil"),
                Triple(MockScreen.MORE, "•••", "Daha Fazla")
            ).forEach { (screen, icon, label) ->
                val selected = current == screen
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).clickable { onGo(screen) }.padding(vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(icon, color = if (selected) NPink else NMuted, fontSize = 20.sp)
                    Text(label, color = if (selected) NPink else NMuted, fontSize = 9.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
private fun MockHome(profile: ProfileDto?, onPlay: () -> Unit, onPrivate: () -> Unit, onLeague: () -> Unit, onGoals: () -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NeonAvatar(profile?.displayName ?: "Oyuncu")
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(profile?.displayName ?: "Oyuncu", color = NText, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text("Lv. ${((profile?.wins ?: 0) / 10) + 1}", color = NMuted, fontSize = 10.sp)
                        Box(Modifier.width(92.dp).height(4.dp).clip(CircleShape).background(Color(0xFF1A2240))) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(.58f).background(NPurple))
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CurrencyPill("🏆", "${(profile?.wins ?: 0) * 10 + 250}", NGold)
                    CurrencyPill("💎", "${profile?.diamonds ?: 0}", NCyan)
                }
            }
        }
        item { NeonHeroLogo() }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HomeAction("⚡", "HIZLI OYNA", "Eşleş & Başla", NPink, Modifier.weight(1f), onPlay)
                HomeAction("🔒", "ÖZEL ODA", "Arkadaşlarınla oyna", NPurple, Modifier.weight(1f), onPrivate)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HomeAction("🏆", "LİG", "Sıranı yükselt", NGold, Modifier.weight(1f), onLeague)
                HomeAction("🎯", "GÖREVLER", "Ödülleri kazan", NPink, Modifier.weight(1f), onGoals)
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, NBlue.copy(alpha = .65f))
            ) {
                Row(
                    Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(NPanel, Color(0xFF10172D), Color(0xFF29111C)))).padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("GÜNLÜK ÖDÜL", color = NCyan, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Text("Yarın geri gel, ödülünü al!", color = NMuted, fontSize = 10.sp)
                    }
                    Text("🧰", fontSize = 44.sp)
                }
            }
        }
    }
}

@Composable
private fun NeonHeroLogo() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, NPurple.copy(alpha = .35f))
    ) {
        Box(
            Modifier.fillMaxWidth().height(250.dp).background(Brush.radialGradient(listOf(NPurple.copy(alpha = .30f), Color(0xFF07122C), NBg))),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(176.dp).clip(CircleShape).background(Brush.sweepGradient(listOf(NPink, NPurple, NCyan, NBlue, NPink))).padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.fillMaxSize().clip(CircleShape).background(NBg), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("♛", color = NGold, fontSize = 40.sp)
                        Text("SON", color = NPink, fontSize = 39.sp, fontWeight = FontWeight.Black)
                        Text("HARF", color = NCyan, fontSize = 39.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeAction(icon: String, title: String, subtitle: String, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(92.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = .78f)),
        onClick = onClick
    ) {
        Row(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(accent.copy(alpha = .13f), NPanel))).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 28.sp)
            Spacer(Modifier.width(9.dp))
            Column {
                Text(title, color = NText, fontWeight = FontWeight.Black, fontSize = 13.sp)
                Text(subtitle, color = NMuted, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun MockGameScreen(onBack: () -> Unit, initialPrivate: Boolean) {
    if (!SupabaseProvider.configured) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Sunucu bağlantısı yok", color = NText) }
        return
    }
    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var opponent by remember { mutableStateOf<ProfileDto?>(null) }
    var room by remember { mutableStateOf<GameRoomDto?>(null) }
    var words by remember { mutableStateOf<List<GameWordDto>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var matching by remember { mutableStateOf(false) }
    var privateMode by remember { mutableStateOf(initialPrivate) }
    var privateCode by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("Düelloya hazırsın") }
    var roomJob by remember { mutableStateOf<Job?>(null) }
    var wordsJob by remember { mutableStateOf<Job?>(null) }
    var matchJob by remember { mutableStateOf<Job?>(null) }

    suspend fun ensureProfile(): ProfileDto {
        if (backend.currentUserId() == null) backend.ensurePlayer("Oyuncu")
        val id = requireNotNull(backend.currentUserId())
        return backend.getProfile(id).also { profile = it }
    }
    suspend fun loadOpponent(r: GameRoomDto) {
        if (r.isBot) { opponent = null; return }
        val me = backend.currentUserId()
        val other = if (r.hostId == me) r.guestId else r.hostId
        opponent = other?.let { runCatching { backend.getProfile(it) }.getOrNull() }
    }
    fun observe(r: GameRoomDto) {
        roomJob?.cancel(); wordsJob?.cancel(); matchJob?.cancel(); matching = false
        scope.launch { loadOpponent(r) }
        roomJob = scope.launch { backend.observeRoom(r.id).catch { notice = "Bağlantı yenileniyor…" }.collect { room = it; loadOpponent(it) } }
        wordsJob = scope.launch { backend.observeWords(r.id).catch { }.collect { words = it } }
    }
    fun cancelMatching(afterCancel: (() -> Unit)? = null) {
        if (!matching) {
            afterCancel?.invoke()
            return
        }
        matching = false
        matchJob?.cancel()
        matchJob = null
        scope.launch {
            runCatching { backend.cancelRandomMatchmaking() }
                .onSuccess { notice = "Eşleşme iptal edildi" }
                .onFailure { notice = "Eşleşme iptal edilemedi" }
            afterCancel?.invoke()
        }
    }

    LaunchedEffect(Unit) { runCatching { ensureProfile() } }

    val active = room
    if (active == null) {
        if (privateMode) {
            PrivateRoomPanel(
                code = privateCode,
                onCode = { privateCode = it.filter(Char::isLetterOrDigit).uppercase().take(6) },
                onBack = { if (initialPrivate) onBack() else privateMode = false },
                onCreate = {
                    scope.launch {
                        runCatching { ensureProfile(); backend.createPrivateRoom("tr") }
                            .onSuccess { room = it; observe(it) }
                            .onFailure { notice = "Oda oluşturulamadı" }
                    }
                },
                onJoin = {
                    scope.launch {
                        runCatching { ensureProfile(); backend.joinPrivateRoom(privateCode) }
                            .onSuccess { room = it; observe(it) }
                            .onFailure { notice = "Oda bulunamadı" }
                    }
                }
            )
        } else {
            DuelLobbyPanel(
                player = profile?.displayName ?: "Oyuncu",
                matching = matching,
                notice = notice,
                onBack = { cancelMatching(onBack) },
                onRandom = {
                    scope.launch {
                        runCatching { ensureProfile(); backend.startRandomMatchmaking("tr") }
                            .onSuccess {
                                matching = true; notice = "Rakip aranıyor…"
                                matchJob = launch {
                                    while (matching && room == null) {
                                        val found = runCatching { backend.pollRandomMatchmakingRoom() }.getOrNull()
                                        if (found != null) { room = found; observe(found); break }
                                        delay(800)
                                    }
                                }
                            }
                            .onFailure { notice = "Eşleşme başlatılamadı" }
                    }
                },
                onCancel = { cancelMatching() },
                onPrivate = { privateMode = true }
            )
        }
    } else {
        val me = backend.currentUserId()
        val myHost = active.hostId == me
        val myScore = if (myHost) active.hostScore else active.guestScore
        val otherScore = if (myHost) active.guestScore else active.hostScore
        val otherName = if (active.isBot) (active.botName ?: "KelimeBot") else opponent?.displayName ?: "Rakip"

        LaunchedEffect(active.currentPlayerId, active.validWordCount) { input = "" }
        LaunchedEffect(active.id, active.botTurn, active.validWordCount) {
            if (active.isBot && active.botTurn && active.status in listOf("playing", "final", "sudden_death")) {
                delay(1500)
                runCatching { backend.botTakeTurn(active.id) }.onSuccess { room = it }
            }
        }

        NeonArena(
            room = active,
            myName = profile?.displayName ?: "Sen",
            otherName = otherName,
            myScore = myScore,
            otherScore = otherScore,
            myTurn = active.currentPlayerId == me,
            words = words,
            input = input,
            onInput = { input = it.take(32) },
            onBack = onBack,
            onSubmit = {
                val value = input.trim()
                if (value.isNotBlank()) scope.launch {
                    runCatching { backend.submitWord(active.id, value) }
                        .onSuccess { room = it; input = ""; SonHarfSoundFx.wordAccepted() }
                        .onFailure { notice = "Kelime kabul edilmedi"; SonHarfSoundFx.warning() }
                }
            },
            onForfeit = { scope.launch { runCatching { backend.forfeit(active.id) }.onSuccess { room = it } } }
        )
    }
}

@Composable
private fun DuelLobbyPanel(player: String, matching: Boolean, notice: String, onBack: () -> Unit, onRandom: () -> Unit, onCancel: () -> Unit, onPrivate: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { TopTitle("‹", "DÜELLO", onBack) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = NPanel), shape = RoundedCornerShape(28.dp), border = BorderStroke(1.dp, NPurple.copy(alpha = .5f))) {
                Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚔", fontSize = 58.sp)
                    Text("$player hazır", color = NText, fontWeight = FontWeight.Black, fontSize = 24.sp)
                    Text(notice, color = NCyan, fontSize = 12.sp)
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = if (matching) onCancel else onRandom,
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (matching) NPurple else NPink)
                    ) { Text(if (matching) "✕ EŞLEŞMEYİ İPTAL ET" else "⚡ HEMEN OYNA", fontWeight = FontWeight.Black) }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = onPrivate, enabled = !matching, modifier = Modifier.fillMaxWidth().height(54.dp), border = BorderStroke(1.dp, NPurple), shape = RoundedCornerShape(14.dp)) {
                        Text("🔒 ÖZEL ODA", color = if (matching) NMuted else NText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivateRoomPanel(code: String, onCode: (String) -> Unit, onBack: () -> Unit, onCreate: () -> Unit, onJoin: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { TopTitle("‹", "⌂  ÖZEL ODA", onBack) }
        item { FieldCard("ODA ADI", "Kağan’ın Odası") }
        item {
            NeonSection("OYNAMA MODU") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModeBox("🎮", "KLASİK", "Standart kurallar", true, Modifier.weight(1f))
                    ModeBox("⏱", "SÜRELİ", "Zamana karşı", false, Modifier.weight(1f))
                }
            }
        }
        item {
            NeonSection("TUR SAYISI") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("−", color = NText, fontSize = 30.sp)
                    Text("3 TUR", color = NText, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text("+", color = NText, fontSize = 30.sp)
                }
            }
        }
        item {
            NeonSection("DAVET KODU") {
                OutlinedTextField(
                    value = code,
                    onValueChange = onCode,
                    singleLine = true,
                    placeholder = { Text("KAGAN24", color = NMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = NText, unfocusedTextColor = NText, focusedBorderColor = NCyan, unfocusedBorderColor = Color(0xFF263251))
                )
            }
        }
        item {
            Button(onClick = onCreate, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = NPink)) {
                Text("⚡ ODAYI OLUŞTUR", fontWeight = FontWeight.Black)
            }
        }
        item {
            OutlinedButton(onClick = onJoin, modifier = Modifier.fillMaxWidth().height(54.dp), border = BorderStroke(1.dp, NPurple), shape = RoundedCornerShape(14.dp)) {
                Text("KODLA ODAYA KATIL", color = NText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun NeonArena(
    room: GameRoomDto,
    myName: String,
    otherName: String,
    myScore: Int,
    otherScore: Int,
    myTurn: Boolean,
    words: List<GameWordDto>,
    input: String,
    onInput: (String) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    onForfeit: () -> Unit
) {
    var seconds by remember(room.turnDeadline) { mutableIntStateOf(35) }
    LaunchedEffect(room.turnDeadline) {
        while (true) {
            seconds = room.turnDeadline?.let { raw ->
                runCatching { ((Instant.parse(raw).toEpochMilli() - System.currentTimeMillis()) / 1000L).toInt().coerceIn(0, 99) }.getOrDefault(35)
            } ?: 35
            delay(500)
        }
    }
    val required = words.lastOrNull()?.word?.lastOrNull()?.uppercaseChar()?.toString() ?: "K"

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            RoundPlayer(myName, myScore, NCyan)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TUR ${room.roundNo}/3", color = NText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Box(Modifier.size(64.dp).clip(CircleShape).background(Brush.sweepGradient(listOf(NCyan, NPurple, NPink, NCyan))).padding(3.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.fillMaxSize().clip(CircleShape).background(NPanel), contentAlignment = Alignment.Center) { Text("$seconds", color = NText, fontSize = 24.sp, fontWeight = FontWeight.Black) }
                }
            }
            RoundPlayer(otherName, otherScore, NPink)
        }
        Spacer(Modifier.height(10.dp))
        Card(colors = CardDefaults.cardColors(containerColor = NPanel), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, NPurple.copy(alpha = .42f))) {
            Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (myTurn) "SIRA SENDE" else "RAKİP OYNUYOR", color = if (myTurn) NCyan else NPink, fontWeight = FontWeight.Black, fontSize = 12.sp)
                Spacer(Modifier.height(18.dp))
                Text(if (words.isEmpty()) "BİR ŞEY" else "SON HARF", color = NText, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("$required _ _ _ _ _", color = Color(0xFFE7F7FF), fontSize = 42.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                Text("Kelimeyi yaz", color = NCyan, fontSize = 11.sp)
                Spacer(Modifier.height(16.dp))
                Surface(color = Color(0xFF0A1022), shape = RoundedCornerShape(13.dp), border = BorderStroke(1.dp, Color(0xFF273153))) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (input.isBlank()) "Kelimenizi yazın…" else input, color = if (input.isBlank()) NMuted else NText, modifier = Modifier.weight(1f))
                        Text("➤", color = if (myTurn && input.isNotBlank()) Color.White else NMuted, fontSize = 25.sp, modifier = Modifier.clickable(enabled = myTurn && input.isNotBlank()) { onSubmit() })
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
            items(words.takeLast(8)) { Surface(color = NPanel2, shape = RoundedCornerShape(99.dp), border = BorderStroke(1.dp, NPurple.copy(alpha = .28f))) { Text(it.word.uppercase(), color = NText, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) } }
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniArenaButton("🔀", "PAS", NPurple, Modifier.weight(1f))
            MiniArenaButton("💡", "İPUCU", NGold, Modifier.weight(1f))
            MiniArenaButton("💬", "SOHBET", NPink, Modifier.weight(1f))
        }
        Spacer(Modifier.height(9.dp))
        NeonKeyboard(input, onInput, enabled = myTurn)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onForfeit, modifier = Modifier.weight(1f), border = BorderStroke(1.dp, NPink), shape = RoundedCornerShape(99.dp)) { Text("⚑ PES ET", color = NPink) }
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f), border = BorderStroke(1.dp, NCyan), shape = RoundedCornerShape(99.dp)) { Text("⌂ ÇIKIŞ", color = NCyan) }
        }
    }
}

@Composable
private fun NeonKeyboard(input: String, onInput: (String) -> Unit, enabled: Boolean) {
    val rows = listOf("QWERTYUIOPĞÜ", "ASDFGHJKLŞİ", "ZXCVBNMÖÇ")
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { c ->
                    Surface(
                        color = Color(0xFF091126),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0xFF293452)),
                        modifier = Modifier.weight(1f).clickable(enabled = enabled) { onInput(input + c.lowercaseChar()) }
                    ) { Text(c.toString(), color = if (enabled) NText else NMuted, textAlign = TextAlign.Center, fontSize = 13.sp, modifier = Modifier.padding(vertical = 9.dp)) }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(color = NPanel2, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f).clickable(enabled = enabled && input.isNotEmpty()) { onInput(input.dropLast(1)) }) {
                Text("⌫", color = NText, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 10.dp))
            }
            Surface(color = NPurple.copy(alpha = .28f), shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(2f)) {
                Text("BOŞLUK", color = NMuted, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 10.dp))
            }
        }
    }
}

@Composable
private fun MockShop(profile: ProfileDto?) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("🛒  MAĞAZA", color = NText, fontSize = 24.sp, fontWeight = FontWeight.Black); CurrencyPill("💎", "${profile?.diamonds ?: 0}", NCyan) } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) { TabPill("ÖNE ÇIKANLAR", true); TabPill("JETONLAR", false); TabPill("GÜÇLENDİRİCİLER", false) } }
        item { Text("ÖZEL PAKETLER", color = NText, fontWeight = FontWeight.Bold) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ShopPack("BAŞLANGIÇ PAKETİ", "🧰", "🏆 500   💎 100", "₺49,99", NCyan, Modifier.weight(1f))
                ShopPack("PRO PAKETİ", "🎁", "🏆 1.500   💎 350", "₺129,99", NGold, Modifier.weight(1f))
            }
        }
        item { Text("JETON PAKETLERİ", color = NText, fontWeight = FontWeight.Bold) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CoinPack("500", "₺19,99", NCyan, Modifier.weight(1f)); CoinPack("1.200", "₺39,99", Color(0xFFC8F25B), Modifier.weight(1f)); CoinPack("2.500", "₺79,99", NGold, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MockProfile(profile: ProfileDto?) {
    val wins = profile?.wins ?: 0
    val losses = profile?.losses ?: 0
    val matches = wins + losses
    val rate = if (matches == 0) 0 else wins * 100 / matches
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        item { Text("PROFİL", color = NText, fontWeight = FontWeight.Black, fontSize = 22.sp) }
        item { Box(Modifier.size(120.dp).clip(CircleShape).background(Brush.sweepGradient(listOf(NCyan, NPurple, NPink, NCyan))).padding(4.dp), contentAlignment = Alignment.Center) { Box(Modifier.fillMaxSize().clip(CircleShape).background(NPanel), contentAlignment = Alignment.Center) { Text((profile?.displayName ?: "O").take(1).uppercase(), color = NText, fontSize = 46.sp, fontWeight = FontWeight.Black) } } }
        item { Text(profile?.displayName ?: "Oyuncu", color = NText, fontWeight = FontWeight.Black, fontSize = 24.sp) }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatCard("GALİBİYET", "$wins", Modifier.weight(1f)); StatCard("MAĞLUBİYET", "$losses", Modifier.weight(1f)); StatCard("KAZANMA %", "%$rate", Modifier.weight(1f)) } }
        item {
            NeonSection("İSTATİSTİKLER") {
                StatLine("Oynanan Maç", "$matches"); StatLine("Kelime Bilgisi", "${wins * 9 + 38}"); StatLine("En Uzun Seri", "${(wins / 3).coerceAtLeast(1)}"); StatLine("En Yüksek Puan", "${wins * 25 + 100}")
            }
        }
        item {
            NeonSection("ROZETLER") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { listOf("🥇", "💠", "♛", "🏆", "🔥").forEach { Text(it, fontSize = 34.sp) } }
            }
        }
    }
}

@Composable
private fun MockLeague(backend: OnlineGameBackend?, profile: ProfileDto?, onBack: () -> Unit, onPlay: () -> Unit) {
    var leaders by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    LaunchedEffect(Unit) { if (backend != null) leaders = runCatching { backend.getLeaderboard(10) }.getOrDefault(emptyList()) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { TopTitle("‹", "LİG", onBack) }
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("💎", fontSize = 90.sp)
                Text("ELMAS LİG", color = NCyan, fontWeight = FontWeight.Black, fontSize = 24.sp)
                Text("SIRALAMAN: ${leaders.indexOfFirst { it.profile.id == profile?.id }.let { if (it < 0) "—" else (it + 1).toString() }}", color = NMuted)
                Text("🏆 5.000 - 5.999", color = NGold, fontSize = 12.sp)
            }
        }
        item {
            NeonSection("LİDERLER") {
                if (leaders.isEmpty()) Text("Sıralama yükleniyor…", color = NMuted)
                leaders.forEachIndexed { index, entry ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${index + 1}", color = if (index < 3) NGold else NText, modifier = Modifier.width(28.dp), fontWeight = FontWeight.Black)
                        NeonAvatar(entry.profile.displayName, 34.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(entry.profile.displayName, color = NText, modifier = Modifier.weight(1f))
                        Text("${entry.profile.wins * 10 + 5000}", color = NMuted)
                    }
                }
            }
        }
        item { Button(onClick = onPlay, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = NPink)) { Text("⚡ HEMEN OYNA", fontWeight = FontWeight.Black) } }
    }
}

@Composable
private fun MockMore(profile: ProfileDto?, onLeague: () -> Unit, onPrivate: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("OYUNCU MERKEZİ", color = NText, fontSize = 24.sp, fontWeight = FontWeight.Black) }
        item {
            NeonSection("KARİYER MERKEZİ") {
                Text(profile?.displayName ?: "Oyuncu", color = NText, fontSize = 25.sp, fontWeight = FontWeight.Black)
                Text("BRONZ • ÇAYLAK", color = NGold, fontWeight = FontWeight.Bold)
                LinearProgressIndicator(progress = { .66f }, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), color = NPurple, trackColor = Color(0xFF202943))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatCard("🏆 Zafer", "${profile?.wins ?: 0}", Modifier.weight(1f)); StatCard("🔥 Seri", "3", Modifier.weight(1f)); StatCard("🧠 Kelime", "38", Modifier.weight(1f)) }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HomeAction("🏆", "LİG", "Sıralamayı gör", NGold, Modifier.weight(1f), onLeague)
                HomeAction("🔒", "ÖZEL ODA", "Arkadaşınla oyna", NPurple, Modifier.weight(1f), onPrivate)
            }
        }
        item {
            NeonSection("GÜNLÜK MEYDAN OKUMA") {
                Text("Bugün 3 düello tamamla", color = NMuted)
                LinearProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), color = NPurple, trackColor = Color(0xFF202943))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("3/3", color = NText, fontSize = 22.sp, fontWeight = FontWeight.Black); Text("💎 30", color = NCyan, fontSize = 20.sp, fontWeight = FontWeight.Black) }
            }
        }
    }
}

@Composable
private fun TopTitle(back: String, title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(color = NPanel, shape = CircleShape, border = BorderStroke(1.dp, Color(0xFF2A3556)), modifier = Modifier.clickable { onBack() }) { Text(back, color = NText, fontSize = 27.sp, modifier = Modifier.padding(horizontal = 13.dp, vertical = 5.dp)) }
        Spacer(Modifier.width(12.dp)); Text(title, color = NText, fontWeight = FontWeight.Black, fontSize = 21.sp)
    }
}

@Composable
private fun NeonAvatar(name: String, size: androidx.compose.ui.unit.Dp = 48.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(Brush.sweepGradient(listOf(NPink, NPurple, NCyan, NPink))).padding(3.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().clip(CircleShape).background(NPanel), contentAlignment = Alignment.Center) { Text(name.take(1).uppercase(), color = NText, fontWeight = FontWeight.Black) }
    }
}

@Composable
private fun CurrencyPill(icon: String, value: String, accent: Color) {
    Surface(color = NPanel, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFF263251))) {
        Row(Modifier.padding(horizontal = 9.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) { Text(icon); Spacer(Modifier.width(5.dp)); Text(value, color = accent, fontWeight = FontWeight.Black, fontSize = 12.sp) }
    }
}

@Composable
private fun NeonSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = NPanel), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFF202D51))) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) { Text(title, color = NText, fontWeight = FontWeight.Black, fontSize = 13.sp); Spacer(Modifier.height(10.dp)); content() }
    }
}

@Composable
private fun FieldCard(label: String, value: String) { NeonSection(label) { Text(value, color = NText, fontSize = 17.sp) } }

@Composable
private fun ModeBox(icon: String, title: String, subtitle: String, selected: Boolean, modifier: Modifier) {
    Surface(modifier = modifier, color = NPanel2, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, if (selected) NCyan else Color(0xFF293452))) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(icon, fontSize = 28.sp); Text(title, color = NText, fontWeight = FontWeight.Black); Text(subtitle, color = NMuted, fontSize = 9.sp) }
    }
}

@Composable
private fun RoundPlayer(name: String, score: Int, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(105.dp)) { NeonAvatar(name, 44.dp); Text(name.take(10), color = NText, fontSize = 11.sp); Text("$score", color = accent, fontWeight = FontWeight.Black, fontSize = 26.sp) }
}

@Composable
private fun MiniArenaButton(icon: String, label: String, accent: Color, modifier: Modifier) {
    Surface(modifier = modifier, color = NPanel, shape = RoundedCornerShape(99.dp), border = BorderStroke(1.dp, accent.copy(alpha = .45f))) {
        Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { Text(icon); Spacer(Modifier.width(5.dp)); Text(label, color = NText, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun TabPill(text: String, selected: Boolean) { Surface(color = if (selected) NPurple.copy(alpha = .25f) else NPanel, shape = RoundedCornerShape(9.dp), border = BorderStroke(1.dp, if (selected) NPurple else Color(0xFF25304C))) { Text(text, color = if (selected) Color.White else NMuted, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) } }

@Composable
private fun ShopPack(title: String, icon: String, bonus: String, price: String, accent: Color, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = NPanel), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, accent.copy(alpha = .55f))) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(title, color = NText, fontWeight = FontWeight.Bold, fontSize = 11.sp); Text(icon, fontSize = 54.sp); Text(bonus, color = NMuted, fontSize = 9.sp); Spacer(Modifier.height(8.dp)); Surface(color = accent.copy(alpha = .20f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, accent)) { Text(price, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) } }
    }
}

@Composable
private fun CoinPack(amount: String, price: String, accent: Color, modifier: Modifier) { Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = NPanel), border = BorderStroke(1.dp, accent.copy(alpha = .5f))) { Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(amount, color = accent, fontWeight = FontWeight.Black, fontSize = 18.sp); Text("🪙", fontSize = 38.sp); Text(price, color = NText, fontWeight = FontWeight.Bold, fontSize = 11.sp) } } }

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier) { Surface(modifier = modifier, color = NPanel2, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFF263252))) { Column(Modifier.padding(vertical = 13.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(label, color = NMuted, fontSize = 9.sp); Text(value, color = NText, fontWeight = FontWeight.Black, fontSize = 20.sp) } } }

@Composable
private fun StatLine(label: String, value: String) { Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = NMuted); Text(value, color = NText, fontWeight = FontWeight.Bold) } }
