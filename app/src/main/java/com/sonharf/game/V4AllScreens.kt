package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.sonharf.game.data.*
import com.sonharf.game.ui.home.HomeViewModel
import com.sonharf.game.ui.home.TopPlayerUiModel
import io.github.jan.supabase.postgrest.from
import java.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

object SonHarfV4Theme {
    val ScreenBg = Color(0xFFF8FAFC)
    val SurfaceWhite = Color(0xFFFFFFFF)
    val SkyBlue = Color(0xFF0284C7)
    val SkyBlueLight = Color(0xFFE0F2FE)
    val SkyBlueDark = Color(0xFF0369A1)
    val TextDark = Color(0xFF0F172A)
    val TextMuted = Color(0xFF64748B)
    val BorderLight = Color(0xFFCBD5E1)
    val Amber = Color(0xFFD97706)
    val Green = Color(0xFF16A34A)
    val Red = Color(0xFFDC2626)
    val Purple = Color(0xFF7C3AED)
}

@Composable
fun V4HomeRoute(
    onStartGameMode: (String) -> Unit,
    onOpenLeague: () -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state = viewModel.uiState
    val context = LocalContext.current
    var showVip by remember { mutableStateOf(false) }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize().background(SonHarfV4Theme.ScreenBg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SonHarfV4Theme.SkyBlue)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(SonHarfV4Theme.ScreenBg),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { V4BrandLogo() }
            item {
                Surface(
                    onClick = onOpenProfile,
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, SonHarfV4Theme.BorderLight),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        V4Avatar(state.userPhotoUrl, state.userName, 52)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(state.userName, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = SonHarfV4Theme.TextDark)
                            Text("Seviye ${state.level} • ${state.league}", fontSize = 13.sp, color = SonHarfV4Theme.TextMuted)
                        }
                        Surface(shape = RoundedCornerShape(20.dp), color = SonHarfV4Theme.SkyBlueLight) {
                            Row(Modifier.padding(horizontal = 11.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Diamond, "Elmas", tint = SonHarfV4Theme.SkyBlue, modifier = Modifier.size(19.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("${state.diamonds}", fontWeight = FontWeight.ExtraBold, color = SonHarfV4Theme.SkyBlueDark)
                            }
                        }
                    }
                }
            }
            item { V4Podium(state.topPlayers, onOpenLeague) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    V4ActionCard(
                        title = "Günlük Ödül",
                        subtitle = if (state.isDailyRewardAvailable) "+${state.dailyRewardDiamonds} elmas • Şimdi al" else "Bugün alındı",
                        icon = { Icon(Icons.Rounded.CardGiftcard, null, tint = SonHarfV4Theme.SkyBlue) },
                        modifier = Modifier.weight(1f),
                        enabled = state.isDailyRewardAvailable && !state.isActionBusy,
                        onClick = viewModel::claimDailyReward,
                    )
                    V4ActionCard(
                        title = "VIP Teklif",
                        subtitle = "Reklamsız + avantajlar",
                        icon = { Icon(Icons.Rounded.WorkspacePremium, null, tint = SonHarfV4Theme.Purple) },
                        modifier = Modifier.weight(1f),
                        onClick = { showVip = true },
                    )
                }
            }
            if (state.notice.isNotBlank()) item {
                Surface(shape = RoundedCornerShape(12.dp), color = SonHarfV4Theme.SkyBlueLight) {
                    Text(state.notice, color = SonHarfV4Theme.SkyBlueDark, modifier = Modifier.fillMaxWidth().padding(12.dp), fontWeight = FontWeight.SemiBold)
                }
            }
            item {
                Text("Oyun Seçenekleri", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = SonHarfV4Theme.TextDark)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onStartGameMode("1v1_RANKED") },
                    modifier = Modifier.fillMaxWidth().height(66.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SonHarfV4Theme.SkyBlue),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Rounded.Bolt, null, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("1v1 Hızlı Karşılaşma", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Gerçek oyuncu ile sıra tabanlı", fontSize = 12.sp, color = Color.White.copy(alpha = .88f))
                    }
                    Icon(Icons.Rounded.ChevronRight, null)
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    V4SmallMode("Lig Arenası", "Puan kazan, yüksel", Icons.Rounded.Shield, SonHarfV4Theme.Amber, Modifier.weight(1f)) { onStartGameMode("LEAGUE") }
                    V4SmallMode("Bot ile Pratik", "Kelime hızını geliştir", Icons.Rounded.SmartToy, SonHarfV4Theme.SkyBlue, Modifier.weight(1f)) { onStartGameMode("PRACTICE_BOT") }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { SonHarfShare.challenge(context, state.userName) },
                        modifier = Modifier.weight(1f).height(54.dp),
                        border = BorderStroke(1.dp, SonHarfV4Theme.SkyBlue),
                        shape = RoundedCornerShape(14.dp),
                    ) { Icon(Icons.Rounded.PersonAdd, null); Spacer(Modifier.width(6.dp)); Text("Davet Et", fontWeight = FontWeight.Bold) }
                    Button(
                        onClick = { FriendsQuickAccessState.open = true },
                        modifier = Modifier.weight(1f).height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SonHarfV4Theme.SkyBlueLight),
                        shape = RoundedCornerShape(14.dp),
                    ) { Icon(Icons.Rounded.Groups, null, tint = SonHarfV4Theme.SkyBlueDark); Spacer(Modifier.width(6.dp)); Text("Arkadaşlar (${state.onlineFriendsCount})", color = SonHarfV4Theme.SkyBlueDark, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                }
            }
            item {
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Color.White, border = BorderStroke(1.dp, SonHarfV4Theme.BorderLight)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Günlük / Haftalık Görevler", fontWeight = FontWeight.Bold, color = SonHarfV4Theme.TextDark)
                        if (state.tasks.isEmpty()) Text("Şu anda aktif görev bulunmuyor.", color = SonHarfV4Theme.TextMuted)
                        state.tasks.forEach { task ->
                            val completed = task.current >= task.target
                            val progress = if (task.target <= 0) 0f else (task.current.toFloat() / task.target).coerceIn(0f, 1f)
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SonHarfV4Theme.ScreenBg).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(task.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SonHarfV4Theme.TextDark)
                                    Spacer(Modifier.height(5.dp))
                                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(.9f).height(6.dp).clip(CircleShape), color = if (completed) SonHarfV4Theme.Green else SonHarfV4Theme.SkyBlue, trackColor = SonHarfV4Theme.BorderLight)
                                    Spacer(Modifier.height(3.dp))
                                    Text("${task.current}/${task.target} • +${task.rewardDiamonds} elmas", fontSize = 11.sp, color = SonHarfV4Theme.TextMuted)
                                }
                                if (completed) Button(onClick = { viewModel.claimTaskReward(task.id) }, enabled = !task.isClaimed && !state.isActionBusy, colors = ButtonDefaults.buttonColors(containerColor = SonHarfV4Theme.Green), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) { Text(if (task.isClaimed) "Alındı" else "Ödülü Al", fontSize = 11.sp) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showVip) VipPurchaseDialog(onVerified = { viewModel.refresh() }, onDismiss = { showVip = false })
}

@Composable
private fun V4BrandLogo() {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        V4BrandTile("S", SonHarfV4Theme.SkyBlue)
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SON HARF", fontSize = 22.sp, fontWeight = FontWeight.Black, color = SonHarfV4Theme.TextDark, letterSpacing = 2.sp)
            Text("KELİME DÜELLOSU", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SonHarfV4Theme.SkyBlue, letterSpacing = 1.4.sp)
        }
        Spacer(Modifier.width(8.dp))
        V4BrandTile("F", SonHarfV4Theme.Amber)
    }
}

@Composable private fun V4BrandTile(letter: String, bg: Color) {
    Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(bg), contentAlignment = Alignment.Center) {
        Text(letter, color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
    }
}

@Composable
private fun V4Podium(players: List<TopPlayerUiModel>, onOpenLeague: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SonHarfV4Theme.SkyBlueLight,
        border = BorderStroke(1.5.dp, SonHarfV4Theme.SkyBlue.copy(alpha = .4f)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Haftanın En İyi 3 Oyuncusu", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = SonHarfV4Theme.SkyBlueDark)
                TextButton(onClick = onOpenLeague) { Text("Tüm Liste") }
            }
            if (players.isEmpty()) Text("Bu hafta sıralama henüz oluşmadı.", color = SonHarfV4Theme.TextMuted)
            else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                listOfNotNull(players.find { it.rank == 2 }, players.find { it.rank == 1 }, players.find { it.rank == 3 }).forEach { p ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(max = 108.dp)) {
                        V4Avatar(p.photoUrl, p.name, if (p.rank == 1) 58 else 48)
                        Spacer(Modifier.height(4.dp))
                        Text("${p.rank}. ${p.name}", maxLines = 1, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SonHarfV4Theme.TextDark)
                        Text("${p.score} galibiyet", fontSize = 10.sp, color = SonHarfV4Theme.SkyBlueDark)
                    }
                }
            }
        }
    }
}

@Composable
private fun V4ActionCard(title: String, subtitle: String, icon: @Composable () -> Unit, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(onClick = onClick, enabled = enabled, modifier = modifier.height(90.dp), shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, SonHarfV4Theme.BorderLight)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = SonHarfV4Theme.SkyBlueLight, modifier = Modifier.size(44.dp)) { Box(contentAlignment = Alignment.Center) { icon() } }
            Spacer(Modifier.width(9.dp))
            Column { Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SonHarfV4Theme.TextDark); Text(subtitle, fontSize = 11.sp, color = SonHarfV4Theme.TextMuted) }
        }
    }
}

@Composable
private fun V4SmallMode(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.height(88.dp), shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, SonHarfV4Theme.BorderLight)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, null, tint = accent)
            Column { Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SonHarfV4Theme.TextDark); Text(subtitle, fontSize = 11.sp, color = SonHarfV4Theme.TextMuted) }
        }
    }
}

@Composable
fun V4BattleScreen(onLeaveBattle: () -> Unit) {
    if (!SupabaseProvider.configured) {
        Box(Modifier.fillMaxSize().background(SonHarfV4Theme.ScreenBg), contentAlignment = Alignment.Center) { Text("Sunucu bağlantısı yok", color = SonHarfV4Theme.TextDark) }
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
    var wordInput by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("Düelloya hazır") }
    var busy by remember { mutableStateOf(false) }
    var matching by remember { mutableStateOf(false) }
    var privateCode by remember { mutableStateOf("") }
    var showPrivate by remember { mutableStateOf(false) }
    var showChat by remember { mutableStateOf(false) }
    var roomJob by remember { mutableStateOf<Job?>(null) }
    var wordsJob by remember { mutableStateOf<Job?>(null) }
    var chatJob by remember { mutableStateOf<Job?>(null) }
    var matchJob by remember { mutableStateOf<Job?>(null) }

    fun friendly(raw: String) = when {
        "not_your_turn" in raw -> "Sıra rakibinde."
        "word_already_used" in raw -> "Bu kelime daha önce kullanıldı."
        "wrong_start_letter" in raw -> "Kelime son harfle başlamalı."
        "not_in_dictionary" in raw -> "Bu kelime sözlükte bulunamadı."
        "turn_expired" in raw -> "Süren doldu."
        "vip_required" in raw -> "Özel oda için VIP gerekli."
        else -> "Bağlantı sorunu. Yeniden deneniyor."
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
        chatJob = scope.launch { backend.observeChat(r.id).catch { }.collect { chat = it } }
    }

    LaunchedEffect(Unit) {
        busy = true
        runCatching { ensureProfile() }.onSuccess {
            val old = runCatching { activeRoom() }.getOrNull()
            if (old != null) { room = old; language = old.language; observe(old) }
        }.onFailure { notice = friendly(it.message.orEmpty()) }
        busy = false
    }

    val active = room
    Box(Modifier.fillMaxSize().background(SonHarfV4Theme.ScreenBg)) {
        if (active == null) {
            V4Lobby(
                player = profile,
                language = language,
                matching = matching,
                notice = notice,
                privateCode = privateCode,
                showPrivate = showPrivate,
                onLanguage = { language = it },
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
                                if (found != null) { room = found; language = found.language; observe(found); SonHarfSoundFx.softNotify(); break }
                                delay(800)
                            }
                        }
                    }
                },
                onCancel = { scope.launch { matching = false; matchJob?.cancel(); runCatching { backend.cancelRandomMatchmaking() }; notice = "Eşleşme iptal edildi" } },
                onCreate = { scope.launch { busy = true; runCatching { backend.createPrivateRoom(language) }.onSuccess { room = it; observe(it) }.onFailure { notice = friendly(it.message.orEmpty()) }; busy = false } },
                onJoin = { scope.launch { busy = true; runCatching { backend.joinPrivateRoom(privateCode) }.onSuccess { room = it; language = it.language; observe(it) }.onFailure { notice = friendly(it.message.orEmpty()) }; busy = false } },
                onBack = onLeaveBattle,
            )
        } else {
            val me = backend.currentUserId()
            LaunchedEffect(active.currentPlayerId, active.validWordCount, active.roundNo) { wordInput = "" }
            LaunchedEffect(active.id) {
                while (true) {
                    if (!active.isBot && active.status != "waiting") runCatching { backend.heartbeatRoom(active.id) }.onSuccess { room = it }
                    delay(5000)
                }
            }
            LaunchedEffect(active.id, active.status, active.botTurn, active.validWordCount) {
                if (active.isBot && active.botTurn && active.status in listOf("playing", "final", "sudden_death")) {
                    delay(1500L + (active.validWordCount % 4) * 300L)
                    runCatching { backend.botTakeTurn(active.id) }.onSuccess { room = it }.onFailure { notice = friendly(it.message.orEmpty()) }
                }
            }
            V4Arena(
                room = active,
                me = me,
                player = profile,
                opponent = opponentProfile,
                words = words,
                wordInput = wordInput,
                notice = notice,
                busy = busy,
                onKey = { c -> if (wordInput.length < 40) wordInput += c },
                onDelete = { if (wordInput.isNotEmpty()) wordInput = wordInput.dropLast(1) },
                onSubmit = {
                    scope.launch {
                        val submitted = wordInput.trim(); if (submitted.isBlank()) return@launch
                        wordInput = ""; busy = true
                        runCatching { backend.submitWord(active.id, submitted) }
                            .onSuccess { room = it; notice = if (it.lastEventPlayerId == me && it.lastEvent != null && it.lastEvent != "word_accepted") friendly(it.lastEvent ?: "") else "${submitted.uppercase()} kabul edildi" }
                            .onFailure { notice = friendly(it.message.orEmpty()) }
                        busy = false
                    }
                },
                onTimeout = { scope.launch { runCatching { backend.claimTurnTimeout(active.id) }.onSuccess { room = it } } },
                onForfeit = { scope.launch { runCatching { backend.forfeit(active.id) }.onSuccess { room = it } } },
                onExit = { roomJob?.cancel(); wordsJob?.cancel(); chatJob?.cancel(); room = null; words = emptyList(); chat = emptyList(); onLeaveBattle() },
                onChat = { showChat = true },
            )
        }

        if (showChat && active != null) {
            V4ChatDrawer(
                messages = chat,
                me = backend.currentUserId(),
                onDismiss = { showChat = false },
                onSend = { text -> scope.launch { runCatching { backend.sendChat(active.id, text) } } },
            )
        }
    }
}

@Composable
private fun V4Lobby(
    player: ProfileDto?,
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
    onBack: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize().background(SonHarfV4Theme.ScreenBg), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Geri", tint = SonHarfV4Theme.TextDark) }
                Spacer(Modifier.weight(1f)); V4BrandLogo(); Spacer(Modifier.weight(1f)); Spacer(Modifier.width(48.dp))
            }
        }
        item {
            Surface(shape = RoundedCornerShape(18.dp), color = Color.White, border = BorderStroke(1.dp, SonHarfV4Theme.BorderLight), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    V4Avatar(player?.avatarUrl, player?.displayName ?: "Oyuncu", 58)
                    Spacer(Modifier.width(12.dp))
                    Column { Text(player?.displayName ?: "Oyuncu", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SonHarfV4Theme.TextDark); Text(if (matching) "Rakip aranıyor…" else "Düelloya hazırsın", color = SonHarfV4Theme.TextMuted) }
                }
            }
        }
        if (matching) {
            item { Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(color = SonHarfV4Theme.SkyBlue); Spacer(Modifier.height(14.dp)); Text("RAKİP BULUNUYOR", fontSize = 24.sp, fontWeight = FontWeight.Black, color = SonHarfV4Theme.SkyBlueDark) } } }
            item { OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().height(56.dp), border = BorderStroke(1.dp, SonHarfV4Theme.Red)) { Text("EŞLEŞMEYİ İPTAL ET", color = SonHarfV4Theme.Red, fontWeight = FontWeight.Bold) } }
        } else {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(selected = language == "tr", onClick = { onLanguage("tr") }, label = { Text("🇹🇷 TÜRKÇE") }, modifier = Modifier.weight(1f))
                    FilterChip(selected = language == "en", onClick = { onLanguage("en") }, label = { Text("🇬🇧 ENGLISH") }, modifier = Modifier.weight(1f))
                }
            }
            item { Button(onClick = onRandom, modifier = Modifier.fillMaxWidth().height(66.dp), colors = ButtonDefaults.buttonColors(containerColor = SonHarfV4Theme.SkyBlue), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Rounded.Bolt, null); Spacer(Modifier.width(8.dp)); Text("HEMEN OYNA", fontWeight = FontWeight.Black, fontSize = 18.sp) } }
            item { OutlinedButton(onClick = onPrivateToggle, modifier = Modifier.fillMaxWidth().height(56.dp), border = BorderStroke(1.dp, SonHarfV4Theme.SkyBlue), shape = RoundedCornerShape(16.dp)) { Text("ODA KUR / ODAYA KATIL", color = SonHarfV4Theme.SkyBlue, fontWeight = FontWeight.Bold) } }
            if (showPrivate) item {
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, SonHarfV4Theme.BorderLight), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onCreate, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = SonHarfV4Theme.Purple)) { Text("VIP ODA OLUŞTUR") }
                        OutlinedTextField(privateCode, onPrivateCode, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("6 haneli oda kodu") })
                        OutlinedButton(onClick = onJoin, enabled = privateCode.length == 6, modifier = Modifier.fillMaxWidth()) { Text("KATIL") }
                    }
                }
            }
            item { Text(notice, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = SonHarfV4Theme.TextMuted, fontSize = 12.sp) }
        }
    }
}

@Composable
private fun V4Arena(
    room: GameRoomDto,
    me: String?,
    player: ProfileDto?,
    opponent: ProfileDto?,
    words: List<GameWordDto>,
    wordInput: String,
    notice: String,
    busy: Boolean,
    onKey: (Char) -> Unit,
    onDelete: () -> Unit,
    onSubmit: () -> Unit,
    onTimeout: () -> Unit,
    onForfeit: () -> Unit,
    onExit: () -> Unit,
    onChat: () -> Unit,
) {
    val host = me == room.hostId
    val myScore = if (host) room.hostScore else room.guestScore
    val oppScore = if (host) room.guestScore else room.hostScore
    val myTurn = room.currentPlayerId == me && room.status in listOf("playing", "final", "sudden_death")
    var seconds by remember(room.turnDeadline) { mutableStateOf(45) }

    LaunchedEffect(room.turnDeadline, room.currentPlayerId, room.status) {
        while (room.turnDeadline != null && room.status in listOf("playing", "final", "sudden_death")) {
            seconds = runCatching { (Instant.parse(room.turnDeadline).epochSecond - Instant.now().epochSecond).toInt().coerceAtLeast(0) }.getOrDefault(45)
            if (seconds <= 0) { onTimeout(); break }
            delay(1000)
        }
    }

    if (room.status == "finished") {
        Box(Modifier.fillMaxSize().background(SonHarfV4Theme.ScreenBg))
        return
    }

    val lastWord = words.lastOrNull()?.word?.uppercase().orEmpty()
    val required = words.lastOrNull()?.normalizedWord?.lastOrNull()?.uppercaseChar()?.toString().orEmpty()

    Column(Modifier.fillMaxSize().background(SonHarfV4Theme.ScreenBg).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onForfeit, border = BorderStroke(1.dp, SonHarfV4Theme.Red), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp)) { Icon(Icons.Rounded.Flag, null, tint = SonHarfV4Theme.Red, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(4.dp)); Text("Pes Et", color = SonHarfV4Theme.Red, fontSize = 12.sp) }
            Spacer(Modifier.weight(1f))
            Text("TUR ${room.roundNo}/3", fontWeight = FontWeight.Black, color = SonHarfV4Theme.TextDark)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onExit) { Icon(Icons.Rounded.Close, "Ayrıl", tint = SonHarfV4Theme.TextMuted) }
        }

        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Color.White, border = BorderStroke(1.dp, SonHarfV4Theme.BorderLight)) {
            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                V4ArenaPlayer(player?.displayName ?: "Sen", player?.avatarUrl, myScore, myTurn, Modifier.weight(1f))
                Box(Modifier.size(52.dp).clip(CircleShape).background(SonHarfV4Theme.SkyBlueLight).border(2.dp, SonHarfV4Theme.SkyBlue, CircleShape), contentAlignment = Alignment.Center) { Text("$seconds", fontSize = 19.sp, fontWeight = FontWeight.Black, color = SonHarfV4Theme.SkyBlueDark) }
                V4ArenaPlayer(if (room.isBot) room.botName ?: "KelimeBot" else opponent?.displayName ?: "Rakip", if (room.isBot) null else opponent?.avatarUrl, oppScore, !myTurn, Modifier.weight(1f))
            }
        }

        Surface(Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(20.dp), color = Color.White, border = BorderStroke(1.dp, SonHarfV4Theme.BorderLight)) {
            Box(Modifier.fillMaxSize().padding(14.dp)) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(if (myTurn) "Sıra sende" else "Rakibin sırası", fontWeight = FontWeight.Bold, color = if (myTurn) SonHarfV4Theme.Green else SonHarfV4Theme.TextMuted)
                    Spacer(Modifier.height(10.dp))
                    Text(if (required.isBlank()) "İlk kelimeyi yaz" else "Başlangıç Harfi: '$required'", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SonHarfV4Theme.Amber)
                    Spacer(Modifier.height(16.dp))
                    if (wordInput.isNotEmpty()) V4LetterTiles(wordInput)
                    else if (required.isNotEmpty()) V4LetterTiles(required)
                    Spacer(Modifier.height(18.dp))
                    if (lastWord.isNotBlank()) Text("Rakibin / Son Kelime: $lastWord", color = SonHarfV4Theme.TextDark, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    Text(notice, color = SonHarfV4Theme.TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
                IconButton(onClick = onChat, modifier = Modifier.align(Alignment.BottomEnd).size(46.dp).clip(RoundedCornerShape(13.dp)).background(SonHarfV4Theme.SkyBlue)) { Icon(Icons.Rounded.ChatBubble, "Sohbet", tint = Color.White) }
            }
        }

        V4GameKeyboard(enabled = myTurn && !busy, onKey = onKey, onDelete = onDelete, onSubmit = onSubmit)
    }
}

@Composable private fun V4ArenaPlayer(name: String, photo: String?, score: Int, active: Boolean, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        V4Avatar(photo, name, 46)
        Spacer(Modifier.height(4.dp))
        Text(name, maxLines = 1, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SonHarfV4Theme.TextDark)
        Text("$score puan", fontSize = 10.sp, color = if (active) SonHarfV4Theme.SkyBlueDark else SonHarfV4Theme.TextMuted)
    }
}

@Composable private fun V4LetterTiles(word: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        word.take(8).forEachIndexed { index, c ->
            val first = index == 0
            val last = index == word.take(8).lastIndex && word.length > 1
            val bg = when { first || last -> SonHarfV4Theme.Amber; else -> SonHarfV4Theme.SkyBlue }
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(bg), contentAlignment = Alignment.Center) { Text(c.toString(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
private fun V4GameKeyboard(enabled: Boolean, onKey: (Char) -> Unit, onDelete: () -> Unit, onSubmit: () -> Unit) {
    val r1 = listOf('Q','W','E','R','T','Y','U','I','O','P','Ğ','Ü')
    val r2 = listOf('A','S','D','F','G','H','J','K','L','Ş','İ')
    val r3 = listOf('Z','X','C','V','B','N','M','Ö','Ç')
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        V4KeyboardRow(r1, enabled, onKey)
        V4KeyboardRow(r2, enabled, onKey)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onSubmit, enabled = enabled, modifier = Modifier.weight(1.7f).height(46.dp), colors = ButtonDefaults.buttonColors(containerColor = SonHarfV4Theme.SkyBlue), contentPadding = PaddingValues(0.dp)) { Text("ONAY", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            r3.forEach { c -> V4Key(c, Modifier.weight(1f), enabled) { onKey(c) } }
            OutlinedButton(onClick = onDelete, enabled = enabled, modifier = Modifier.weight(1.4f).height(46.dp), border = BorderStroke(1.dp, SonHarfV4Theme.Red), contentPadding = PaddingValues(0.dp)) { Text("SİL", color = SonHarfV4Theme.Red, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
        }
    }
}

@Composable private fun V4KeyboardRow(chars: List<Char>, enabled: Boolean, onKey: (Char) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) { chars.forEach { c -> V4Key(c, Modifier.weight(1f), enabled) { onKey(c) } } }
}

@Composable private fun V4Key(c: Char, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, enabled = enabled, modifier = modifier.height(46.dp), color = Color.White, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, SonHarfV4Theme.BorderLight)) {
        Box(contentAlignment = Alignment.Center) { Text(c.toString(), color = SonHarfV4Theme.TextDark, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
    }
}

@Composable
private fun V4ChatDrawer(messages: List<ChatMessageDto>, me: String?, onDismiss: () -> Unit, onSend: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .35f)).clickable { onDismiss() }) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.62f).align(Alignment.BottomCenter).clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)).background(Color.White).clickable(enabled = false) {}.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Oyun İçi Sohbet", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 17.sp, color = SonHarfV4Theme.TextDark); IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Kapat") } }
            HorizontalDivider(color = SonHarfV4Theme.BorderLight)
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
                items(messages, key = { it.id }) { msg ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (msg.senderId == me) Arrangement.End else Arrangement.Start) {
                        Surface(shape = RoundedCornerShape(12.dp), color = if (msg.senderId == me) SonHarfV4Theme.SkyBlue else SonHarfV4Theme.SkyBlueLight) {
                            Text(msg.body, Modifier.padding(horizontal = 11.dp, vertical = 8.dp), color = if (msg.senderId == me) Color.White else SonHarfV4Theme.TextDark, fontSize = 13.sp)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(input, { input = it.take(300) }, modifier = Modifier.weight(1f), singleLine = true, placeholder = { Text("Mesaj yaz…") })
                Spacer(Modifier.width(7.dp))
                IconButton(onClick = { val t = input.trim(); if (t.isNotEmpty()) { onSend(t); input = "" } }, modifier = Modifier.clip(CircleShape).background(SonHarfV4Theme.SkyBlue)) { Icon(Icons.Rounded.Send, "Gönder", tint = Color.White) }
            }
        }
    }
}

@Composable
fun V4ProfileScreen(onOpenPreferences: () -> Unit) {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        val b = backend
        if (b != null) {
            val id = b.currentUserId()
            if (id != null) profile = runCatching { b.getProfile(id) }.getOrNull()
        }
        loading = false
    }
    if (loading) { Box(Modifier.fillMaxSize().background(SonHarfV4Theme.ScreenBg), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = SonHarfV4Theme.SkyBlue) }; return }
    val p = profile
    LazyColumn(Modifier.fillMaxSize().background(SonHarfV4Theme.ScreenBg), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("OYUNCU KARTI", color = SonHarfV4Theme.SkyBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                TextButton(onClick = onOpenPreferences) { Text("GİZLİLİK & TERCİHLER", color = SonHarfV4Theme.TextMuted, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            }
            HorizontalDivider(color = SonHarfV4Theme.BorderLight)
        }
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                V4Avatar(p?.avatarUrl, p?.displayName ?: "Oyuncu", 92)
                Spacer(Modifier.height(8.dp))
                Text(p?.displayName ?: "Oyuncu", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = SonHarfV4Theme.TextDark)
                Text(if (p?.isVip == true) "VIP • SON HARF OYUNCUSU" else "SON HARF OYUNCUSU", fontSize = 12.sp, color = SonHarfV4Theme.TextMuted)
            }
        }
        item {
            Text("İSTATİSTİKLERİM", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SonHarfV4Theme.TextDark)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V4StatCard("${p?.wins ?: 0}", "Galibiyet", Modifier.weight(1f))
                V4StatCard("${p?.losses ?: 0}", "Mağlubiyet", Modifier.weight(1f))
                val total = (p?.wins ?: 0) + (p?.losses ?: 0)
                val rate = if (total == 0) 0 else ((p?.wins ?: 0) * 100 / total)
                V4StatCard("%$rate", "Kazanma", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V4StatCard("${(p?.wins ?: 0) + (p?.losses ?: 0)}", "Maç", Modifier.weight(1f))
                V4StatCard("${p?.diamonds ?: 0}", "Elmas", Modifier.weight(1f))
                V4StatCard(if (p?.allowMatchChat == true) "Açık" else "Kapalı", "Sohbet", Modifier.weight(1f))
            }
        }
    }
}

@Composable private fun V4StatCard(value: String, label: String, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = Color.White, border = BorderStroke(1.dp, SonHarfV4Theme.BorderLight)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = SonHarfV4Theme.SkyBlueDark); Text(label, fontSize = 11.sp, color = SonHarfV4Theme.TextMuted) }
    }
}

@Composable
fun V4PreferencesScreen(onBack: () -> Unit) {
    var gameInvites by remember { mutableStateOf(true) }
    var friendRequests by remember { mutableStateOf(true) }
    var announcements by remember { mutableStateOf(true) }
    LazyColumn(Modifier.fillMaxSize().background(SonHarfV4Theme.ScreenBg), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Geri", tint = SonHarfV4Theme.TextDark) }; Text("UYGULAMA TERCİHLERİ", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SonHarfV4Theme.TextDark) } }
        item { V4Preference("Oyun Davetleri", "Arkadaşların seni düelloya çağırdığında uyar.", gameInvites) { gameInvites = it } }
        item { V4Preference("Arkadaşlık İstekleri", "Yeni arkadaşlık isteği geldiğinde uyar.", friendRequests) { friendRequests = it } }
        item { V4Preference("Sistem Duyuruları", "Ödül, bakım ve önemli oyun duyuruları.", announcements) { announcements = it } }
        item { Text("Bu üç bildirim tercihi V4 arayüz durumudur; hesap seviyesinde kalıcılaştırma ayrı backend paketiyle yapılacaktır.", color = SonHarfV4Theme.TextMuted, fontSize = 11.sp) }
    }
}

@Composable private fun V4Preference(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, SonHarfV4Theme.BorderLight), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SonHarfV4Theme.TextDark); Text(subtitle, fontSize = 12.sp, color = SonHarfV4Theme.TextMuted) }
            Switch(checked = checked, onCheckedChange = onChecked, colors = SwitchDefaults.colors(checkedTrackColor = SonHarfV4Theme.SkyBlue))
        }
    }
}

@Composable
fun V4StoreScreen() {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var items by remember { mutableStateOf<List<ShopItemDto>>(emptyList()) }
    var owned by remember { mutableStateOf<Set<String>>(emptySet()) }
    var equipped by remember { mutableStateOf<EquippedCosmeticsDto?>(null) }
    var busy by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }

    suspend fun reload() {
        val b = backend ?: return
        val id = b.currentUserId() ?: return
        profile = runCatching { b.getProfile(id) }.getOrNull()
        items = runCatching { b.getShopItems() }.getOrDefault(emptyList())
        owned = runCatching { b.getInventory() }.getOrDefault(emptySet())
        equipped = runCatching { b.getEquippedCosmetics() }.getOrNull()
    }
    LaunchedEffect(Unit) { reload(); loading = false }

    fun isEquipped(item: ShopItemDto): Boolean = when (item.kind) {
        "profile_frame" -> equipped?.profileFrameId == item.id
        "name_style" -> equipped?.nameStyleId == item.id
        "game_theme" -> equipped?.gameThemeId == item.id
        "keyboard_theme" -> equipped?.keyboardThemeId == item.id
        "victory_effect" -> equipped?.victoryEffectId == item.id
        "emoji_pack" -> equipped?.emojiPackId == item.id
        else -> false
    }

    LazyColumn(Modifier.fillMaxSize().background(SonHarfV4Theme.ScreenBg), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Oyun Mağazası", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = SonHarfV4Theme.TextDark); Text("Kozmetik ve kişiselleştirme", color = SonHarfV4Theme.TextMuted, fontSize = 12.sp) }
                Surface(shape = RoundedCornerShape(20.dp), color = SonHarfV4Theme.SkyBlueLight) { Text("◆ ${profile?.diamonds ?: 0}", Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = SonHarfV4Theme.SkyBlueDark, fontWeight = FontWeight.Black) }
            }
        }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = SonHarfV4Theme.SkyBlue) }
        items(items, key = { it.id }) { item ->
            val mine = item.id in owned
            val active = isEquipped(item)
            Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(if (active) 1.5.dp else 1.dp, if (active) SonHarfV4Theme.SkyBlue else SonHarfV4Theme.BorderLight), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(12.dp), color = SonHarfV4Theme.SkyBlueLight, modifier = Modifier.size(48.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.AutoAwesome, null, tint = if (item.vipOnly) SonHarfV4Theme.Purple else SonHarfV4Theme.SkyBlue) } }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text(item.nameTr, fontWeight = FontWeight.Bold, color = SonHarfV4Theme.TextDark); Text(item.descriptionTr, fontSize = 11.sp, color = SonHarfV4Theme.TextMuted); Text(if (mine) "Sahipsin" else "◆ ${item.diamondPrice}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (mine) SonHarfV4Theme.Green else SonHarfV4Theme.SkyBlueDark) }
                    Button(
                        onClick = {
                            scope.launch {
                                busy = item.id
                                if (mine) runCatching { backend?.equipShopItem(item.id) }.onSuccess { notice = "${item.nameTr} etkinleştirildi." }
                                else runCatching { backend?.purchaseShopItem(item.id) }.onSuccess { notice = "Satın alma tamamlandı." }.onFailure { notice = if ("insufficient_diamonds" in it.message.orEmpty()) "Yeterli elmas yok." else "Satın alma tamamlanamadı." }
                                reload(); busy = null
                            }
                        },
                        enabled = busy == null && (!item.vipOnly || profile?.isVip == true),
                        colors = ButtonDefaults.buttonColors(containerColor = if (active) SonHarfV4Theme.Green else SonHarfV4Theme.SkyBlue),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    ) { Text(if (busy == item.id) "…" else if (active) "AKTİF" else if (mine) "KULLAN" else "AL", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
        if (notice.isNotBlank()) item { Surface(shape = RoundedCornerShape(12.dp), color = SonHarfV4Theme.SkyBlueLight) { Text(notice, Modifier.fillMaxWidth().padding(12.dp), color = SonHarfV4Theme.SkyBlueDark, textAlign = TextAlign.Center) } }
    }
}

@Composable
private fun V4Avatar(url: String?, name: String, size: Int) {
    if (!url.isNullOrBlank()) {
        AsyncImage(model = url, contentDescription = "$name profil fotoğrafı", contentScale = ContentScale.Crop, modifier = Modifier.size(size.dp).clip(CircleShape).border(2.dp, SonHarfV4Theme.SkyBlue, CircleShape))
    } else {
        Box(Modifier.size(size.dp).clip(CircleShape).background(SonHarfV4Theme.SkyBlueLight).border(2.dp, SonHarfV4Theme.SkyBlue, CircleShape), contentAlignment = Alignment.Center) {
            Text(name.take(1).uppercase(), color = SonHarfV4Theme.SkyBlueDark, fontWeight = FontWeight.Bold, fontSize = (size / 2.2).sp)
        }
    }
}
