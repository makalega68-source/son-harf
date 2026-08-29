package com.sonharf.game

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class LightScreen { HOME, SON_HARF, KELIME_ARENASI, TAKIM_ARENASI, GUNLUK_ARENA, KELIME_AVI, KELIME_SAVASI, COMPETITION, LEAGUE, MARKET, TASKS, PROFILE }

private val LightBg = Color(0xFFF7F9FC)
private val LightSurface = Color.White
private val LightSurface2 = Color(0xFFF0F4F8)
private val LightText = Color(0xFF182235)
private val LightMuted = Color(0xFF718096)
private val LightBlue = Color(0xFF1769E0)
private val LightGreen = Color(0xFF22B95F)
private val LightGold = Color(0xFFF3A81A)
private val LightBorder = Color(0xFFDDE5EE)

@Composable
fun LightWordThemeApp() {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val context = LocalContext.current
    var screen by remember { mutableStateOf(LightScreen.HOME) }
    var gameKey by remember { mutableIntStateOf(0) }
    var arenaInitialRoomId by remember { mutableStateOf<String?>(null) }
    var teamArenaInitialRoomId by remember { mutableStateOf<String?>(null) }
    val arenaRequest = WordArenaNavigation.request
    val teamArenaRequest = TeamArenaNavigation.request
    var authChecked by remember { mutableStateOf(false) }
    var authenticated by remember { mutableStateOf(false) }
    var lastHomeBack by remember { mutableLongStateOf(0L) }

    val openMode: (String) -> Unit = { mode ->
        when (mode) {
            "duel" -> { gameKey += 1; screen = LightScreen.SON_HARF }
            "word_arena" -> {
                arenaInitialRoomId = null
                WordArenaNavigation.clearRoom()
                screen = LightScreen.KELIME_ARENASI
            }
            "team_arena" -> {
                teamArenaInitialRoomId = null
                TeamArenaNavigation.clearRoom()
                screen = LightScreen.TAKIM_ARENASI
            }
            "daily_arena" -> screen = LightScreen.GUNLUK_ARENA
            "daily_cipher" -> screen = LightScreen.KELIME_AVI
            "bil_bakalim" -> screen = LightScreen.KELIME_SAVASI
            else -> screen = LightScreen.TASKS
        }
    }

    LaunchedEffect(Unit) {
        authenticated = SupabaseProvider.configured && hasVerifiedMembershipSession()
        authChecked = true
    }
    LaunchedEffect(SonHarfUiState.homeRequest) {
        if (SonHarfUiState.homeRequest > 0) screen = LightScreen.HOME
    }
    LaunchedEffect(arenaRequest, authenticated) {
        if (authenticated && arenaRequest > 0) {
            val requestedRoom = WordArenaNavigation.roomId
            if (!requestedRoom.isNullOrBlank()) {
                arenaInitialRoomId = requestedRoom
                screen = LightScreen.KELIME_ARENASI
            }
        }
    }
    LaunchedEffect(teamArenaRequest, authenticated) {
        if (authenticated && teamArenaRequest > 0) {
            val requestedRoom = TeamArenaNavigation.roomId
            if (!requestedRoom.isNullOrBlank()) {
                teamArenaInitialRoomId = requestedRoom
                screen = LightScreen.TAKIM_ARENASI
            }
        }
    }

    BackHandler(enabled = authenticated) {
        if (screen == LightScreen.HOME) {
            val now = System.currentTimeMillis()
            if (now - lastHomeBack < 1800L) (context as? Activity)?.finish() else lastHomeBack = now
        } else screen = LightScreen.HOME
    }

    if (!authChecked) {
        Box(Modifier.fillMaxSize().background(LightBg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = LightBlue)
        }
        return
    }
    if (!authenticated) {
        RequiredAuthGate { authenticated = true; screen = LightScreen.HOME }
        return
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (screen in setOf(LightScreen.HOME, LightScreen.LEAGUE, LightScreen.MARKET, LightScreen.TASKS, LightScreen.PROFILE)) {
                LightBottomBar(
                    screen,
                    { screen = LightScreen.HOME },
                    { screen = LightScreen.LEAGUE },
                    { screen = LightScreen.MARKET },
                    { screen = LightScreen.TASKS },
                    { screen = LightScreen.PROFILE },
                )
            }
        },
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).background(
                Brush.verticalGradient(listOf(Color.White, LightBg, Color(0xFFF3F7FC)))
            )
        ) {
            when (screen) {
                LightScreen.HOME -> LightHomeScreen(
                    backend,
                    onSonHarf = { gameKey += 1; screen = LightScreen.SON_HARF },
                    onKelimeArenasi = {
                        arenaInitialRoomId = null
                        WordArenaNavigation.clearRoom()
                        screen = LightScreen.KELIME_ARENASI
                    },
                    onTakimArenasi = {
                        teamArenaInitialRoomId = null
                        TeamArenaNavigation.clearRoom()
                        screen = LightScreen.TAKIM_ARENASI
                    },
                    onGunlukArena = { screen = LightScreen.GUNLUK_ARENA },
                    onKelimeAvi = { screen = LightScreen.KELIME_AVI },
                    onKelimeSavasi = { screen = LightScreen.KELIME_SAVASI },
                    onCompetition = { screen = LightScreen.COMPETITION },
                    onLeague = { screen = LightScreen.LEAGUE },
                    onMarket = { screen = LightScreen.MARKET },
                    onTasks = { screen = LightScreen.TASKS },
                    onProfile = { screen = LightScreen.PROFILE },
                    onRouteMode = openMode,
                )
                LightScreen.SON_HARF -> key(gameKey) {
                    Box(Modifier.fillMaxSize()) {
                        TargetNeonGameScreen(autoStartMatchmaking = true)
                        ModeEntryOverlay(
                            key = "duel-$gameKey",
                            title = sh("SON HARF DÜELLOSU", "LAST LETTER DUEL"),
                            subtitle = sh("Rakibini geç • serini büyüt • rating kazan", "Beat your rival • build your streak • gain rating"),
                        )
                    }
                }
                LightScreen.KELIME_ARENASI -> Box(Modifier.fillMaxSize()) {
                    WordArenaScreen(
                        initialRoomId = arenaInitialRoomId,
                        onExit = {
                            arenaInitialRoomId = null
                            WordArenaNavigation.clearRoom()
                            screen = LightScreen.HOME
                        },
                    )
                    ModeEntryOverlay(
                        key = "word-arena-${arenaRequest}-${arenaInitialRoomId.orEmpty()}",
                        title = sh("KELİME ARENASI", "WORD ARENA"),
                        subtitle = sh("60 saniye • aynı harfler • canlı skor", "60 seconds • same letters • live score"),
                    )
                }
                LightScreen.TAKIM_ARENASI -> Box(Modifier.fillMaxSize()) {
                    TeamArenaScreen(
                        initialRoomId = teamArenaInitialRoomId,
                        onExit = {
                            teamArenaInitialRoomId = null
                            TeamArenaNavigation.clearRoom()
                            screen = LightScreen.HOME
                        },
                    )
                    ModeEntryOverlay(
                        key = "team-arena-${teamArenaRequest}-${teamArenaInitialRoomId.orEmpty()}",
                        title = sh("TAKIM ARENASI", "TEAM ARENA"),
                        subtitle = sh("2v2 • takım skoru • MVP mücadelesi", "2v2 • team score • MVP battle"),
                    )
                }
                LightScreen.GUNLUK_ARENA -> Box(Modifier.fillMaxSize()) {
                    DailyArenaScreen { screen = LightScreen.HOME }
                    ModeEntryOverlay(
                        key = "daily-arena",
                        title = sh("GÜNLÜK ARENA", "DAILY ARENA"),
                        subtitle = sh("Hedef rakibi yakala • günlük sıralamayı tırman", "Catch the target rival • climb today's ranking"),
                    )
                }
                LightScreen.KELIME_AVI -> Box(Modifier.fillMaxSize()) {
                    DailyCipherScreen { screen = LightScreen.HOME }
                    ModeEntryOverlay(
                        key = "daily-cipher",
                        title = sh("SEN VS GÜNÜN ŞİFRESİ", "YOU VS DAILY CIPHER"),
                        subtitle = sh("6 hak • tek hedef • günlük seri", "6 tries • one target • daily streak"),
                    )
                }
                LightScreen.KELIME_SAVASI -> Box(Modifier.fillMaxSize()) {
                    TrackedBilBakalimStandaloneScreen { screen = LightScreen.HOME }
                    ModeEntryOverlay(
                        key = "bil-bakalim",
                        title = sh("BİL BAKALIM DÜELLOSU", "TRIVIA DUEL"),
                        subtitle = sh("Hız • doğruluk • seri • final sorusu", "Speed • accuracy • streak • final question"),
                    )
                }
                LightScreen.COMPETITION -> CompetitionHubScreen { screen = LightScreen.HOME }
                LightScreen.LEAGUE -> LeaderboardExperienceScreen { screen = LightScreen.HOME }
                LightScreen.MARKET -> EconomyShopScreen(onBack = { screen = LightScreen.HOME })
                LightScreen.TASKS -> LightTasksScreen(backend, openMode)
                LightScreen.PROFILE -> ProfileExperienceScreen(onBack = { screen = LightScreen.HOME })
            }
        }
    }
}

@Composable
private fun LightBottomBar(
    screen: LightScreen,
    onHome: () -> Unit,
    onLeague: () -> Unit,
    onMarket: () -> Unit,
    onTasks: () -> Unit,
    onProfile: () -> Unit,
) {
    Surface(color = Color.White, shadowElevation = 10.dp, border = BorderStroke(1.dp, LightBorder)) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            LightBottomItem(Icons.Rounded.Home, "Ana Sayfa", screen == LightScreen.HOME, Modifier.weight(1f), onHome)
            LightBottomItem(Icons.Rounded.EmojiEvents, "Lig", screen == LightScreen.LEAGUE, Modifier.weight(1f), onLeague)
            LightBottomItem(Icons.Rounded.ShoppingCart, "Market", screen == LightScreen.MARKET, Modifier.weight(1f), onMarket)
            LightBottomItem(Icons.Rounded.TaskAlt, "Görevler", screen == LightScreen.TASKS, Modifier.weight(1f), onTasks)
            LightBottomItem(Icons.Rounded.Person, "Profil", screen == LightScreen.PROFILE, Modifier.weight(1f), onProfile)
        }
    }
}

@Composable
private fun LightBottomItem(icon: ImageVector, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).clickable { SonHarfSoundFx.tap(); onClick() }.padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = if (selected) LightBlue else LightMuted, modifier = Modifier.size(23.dp))
        Text(label, color = if (selected) LightBlue else LightMuted, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun LightHomeScreen(
    backend: OnlineGameBackend?,
    onSonHarf: () -> Unit,
    onKelimeArenasi: () -> Unit,
    onTakimArenasi: () -> Unit,
    onGunlukArena: () -> Unit,
    onKelimeAvi: () -> Unit,
    onKelimeSavasi: () -> Unit,
    onCompetition: () -> Unit,
    onLeague: () -> Unit,
    onMarket: () -> Unit,
    onTasks: () -> Unit,
    onProfile: () -> Unit,
    onRouteMode: (String) -> Unit,
) {
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var growth by remember { mutableStateOf<GrowthDashboardDto?>(null) }
    var rival by remember { mutableStateOf<ArchRivalDto?>(null) }
    var topClubs by remember { mutableStateOf<List<ClubDirectoryRowDto>>(emptyList()) }
    var topPlayers by remember { mutableStateOf<List<WeeklyTournamentLeaderboardRowDto>>(emptyList()) }
    var topPlayerProfiles by remember { mutableStateOf<Map<String, ProfileDto?>>(emptyMap()) }
    var unifiedMissions by remember { mutableStateOf<List<UnifiedMissionDto>>(emptyList()) }

    LaunchedEffect(backend, SonHarfUiState.homeRequest) {
        val b = backend ?: return@LaunchedEffect
        val me = b.currentUserId() ?: return@LaunchedEffect
        profile = runCatching { b.getProfile(me) }.getOrNull()
        growth = runCatching { b.getGrowthDashboard() }.getOrNull()
        rival = runCatching { b.getArchRival() }.getOrNull()
        topClubs = runCatching { b.getClubDirectory(3) }.getOrDefault(emptyList())
        topPlayers = runCatching { b.getWeeklyTournamentLeaderboard(3) }.getOrDefault(emptyList())
        val profiles = mutableMapOf<String, ProfileDto?>()
        topPlayers.forEach { row ->
            profiles[row.userId] = runCatching { b.getProfile(row.userId) }.getOrNull()
        }
        topPlayerProfiles = profiles
        unifiedMissions = runCatching { b.getUnifiedMissions() }.getOrDefault(emptyList())
    }

    val nextRoute = unifiedMissions.firstOrNull {
        it.scope == "daily" && !it.completed && it.modeKey != "route"
    } ?: unifiedMissions.firstOrNull {
        it.scope == "weekly" && !it.completed && it.modeKey != "route"
    } ?: unifiedMissions.firstOrNull { !it.claimed }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("SON HARF", color = LightText, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Text("Kelimeyi Sürdür, Rakibini Geç", color = LightMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
                Surface(
                    onClick = onProfile,
                    shape = CircleShape,
                    color = LightSurface,
                    border = BorderStroke(1.dp, LightBorder),
                ) {
                    Box(Modifier.padding(2.dp), contentAlignment = Alignment.Center) {
                        ProfilePhotoAvatar(
                            avatarPath = profile?.avatarPath,
                            name = profile?.displayName ?: "Oyuncu",
                            size = 42.dp,
                            visible = true,
                            accent = LightBlue,
                        )
                    }
                }
            }
        }

        item {
            val rating = profile?.rating ?: 1000
            val league = ratingLeagueProgress(rating)
            Surface(shape = RoundedCornerShape(19.dp), color = LightSurface, border = BorderStroke(1.dp, LightBorder)) {
                Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.EmojiEvents, null, tint = LightGold, modifier = Modifier.size(34.dp))
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(league.leagueName, color = LightText, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Text(
                                if (league.nextAt == null) "$rating rating • ${sh("En üst lig", "Top league")}"
                                else "$rating rating • ${league.pointsToNext} ${sh("puan kaldı", "points left")}",
                                color = LightMuted,
                                fontSize = 10.sp,
                            )
                        }
                        Text((growth?.currentWinStreak ?: 0).toString() + " 🔥", color = LightText, fontWeight = FontWeight.Black)
                    }
                    LinearProgressIndicator(
                        progress = { league.progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = LightBlue,
                        trackColor = LightSurface2,
                    )
                }
            }
        }

        item { UnifiedRouteCard(nextRoute, onRouteMode) }
        item { WeeklyClubPodiumCard(topClubs) }
        item { WeeklyPlayerPodiumCard(topPlayers, topPlayerProfiles) }

        if (rival != null) {
            item {
                val r = rival!!
                Surface(shape = RoundedCornerShape(19.dp), color = LightSurface, border = BorderStroke(1.dp, LightBorder)) {
                    Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(42.dp), shape = CircleShape, color = LightGold.copy(alpha = .12f)) {
                            Box(contentAlignment = Alignment.Center) { Text("⚔", fontSize = 22.sp) }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("EZELİ RAKİP", color = LightGold, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            Text(r.displayName, color = LightText, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Text("${r.matches} maç • Sen ${r.wins} - ${r.losses} Rakip", color = LightMuted, fontSize = 10.sp)
                        }
                        Text("${r.myPoints}:${r.theirPoints}", color = LightText, fontWeight = FontWeight.Black, fontSize = 17.sp)
                    }
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFD5E3F7)),
                shadowElevation = 5.dp,
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Son Harf", color = LightText, fontWeight = FontWeight.Black, fontSize = 22.sp)
                    Text("Son harften yeni kelime üret, rakibini geç.", color = LightMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
                    Button(
                        onClick = { SonHarfSoundFx.tap(); onSonHarf() },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LightBlue, contentColor = Color.White),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 5.dp, pressedElevation = 1.dp),
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("OYNA", fontSize = 22.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { SonHarfSoundFx.tap(); onKelimeArenasi() },
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                border = BorderStroke(1.dp, LightBlue.copy(alpha = .28f)),
                shadowElevation = 3.dp,
            ) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(58.dp), shape = RoundedCornerShape(18.dp), color = LightBlue.copy(alpha = .11f)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Bolt, null, tint = LightBlue, modifier = Modifier.size(34.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Kelime Arenası", color = LightText, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text("Aynı harfler • 60 saniye • Benzersiz kelime 2×", color = LightMuted, fontSize = 10.sp)
                    }
                    Surface(shape = RoundedCornerShape(12.dp), color = LightBlue) {
                        Text("DÜELLO", Modifier.padding(horizontal = 12.dp, vertical = 9.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        item {
            LightGameCard(
                Icons.Rounded.Groups,
                "Takım Arenası",
                "2v2 • 4 arkadaş • ortak takım skoru • 60 saniye.",
                "2v2",
                LightBlue,
                onTakimArenasi,
            )
        }

        item {
            LightGameCard(
                Icons.Rounded.EmojiEvents,
                "Günlük Arena",
                "Herkes aynı harflerle yarışır • tek resmî deneme • günlük seri.",
                "BUGÜN",
                LightGold,
                onGunlukArena,
            )
        }

        item { Text("DİĞER OYUNLAR", color = LightMuted, fontSize = 11.sp, fontWeight = FontWeight.Black) }

        item {
            LightGameCard(Icons.Rounded.Search, "Kelime Avı", "Günün kelimesini ipuçlarıyla bul.", "BAŞLA", LightGreen, onKelimeAvi)
        }
        item {
            LightGameCard(Icons.Rounded.Bolt, "Bil Bakalım", "Bilgi yarışmasında tahmin et, puanı kap.", "OYNA", LightGold, onKelimeSavasi)
        }
        item {
            LightGameCard(Icons.Rounded.Groups, "Rekabet Merkezi", "Kulüpler, kulüp sohbeti ve ücretsiz Haftalık Kelime Kupası.", "GİR", LightBlue, onCompetition)
        }
        item {
            LightGameCard(Icons.Rounded.ShoppingCart, "Market", "VIP, Sezon Bileti, Son Coin ve Style ürünleri.", "AÇ", LightBlue, onMarket)
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LightShortcut(Icons.Rounded.EmojiEvents, "Lig", Modifier.weight(1f), onLeague)
                LightShortcut(Icons.Rounded.TaskAlt, "Görevler", Modifier.weight(1f), onTasks)
                LightShortcut(Icons.Rounded.Person, "Profil", Modifier.weight(1f), onProfile)
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun LightGameCard(icon: ImageVector, title: String, subtitle: String, buttonText: String, accent: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { SonHarfSoundFx.tap(); onClick() },
        shape = RoundedCornerShape(20.dp),
        color = LightSurface,
        border = BorderStroke(1.dp, LightBorder),
        shadowElevation = 2.dp,
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(52.dp), shape = RoundedCornerShape(16.dp), color = accent.copy(alpha = .12f)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = accent, modifier = Modifier.size(29.dp)) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = LightText, fontWeight = FontWeight.Black, fontSize = 17.sp)
                Text(subtitle, color = LightMuted, fontSize = 11.sp, lineHeight = 15.sp)
            }
            Surface(shape = RoundedCornerShape(12.dp), color = accent) {
                Text(buttonText, Modifier.padding(horizontal = 13.dp, vertical = 9.dp), color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun LightShortcut(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.clickable { SonHarfSoundFx.tap(); onClick() }, shape = RoundedCornerShape(17.dp), color = LightSurface, border = BorderStroke(1.dp, LightBorder)) {
        Column(Modifier.padding(vertical = 13.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = LightBlue, modifier = Modifier.size(23.dp))
            Spacer(Modifier.height(5.dp))
            Text(label, color = LightText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LightTasksScreen(
    backend: OnlineGameBackend?,
    onPlayMode: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var goals by remember { mutableStateOf<List<GoalRowDto>>(emptyList()) }
    var unified by remember { mutableStateOf<List<UnifiedMissionDto>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }

    suspend fun reload() {
        goals = runCatching { backend?.getGoals().orEmpty() }.getOrDefault(emptyList())
        unified = runCatching { backend?.getUnifiedMissions().orEmpty() }.getOrDefault(emptyList())
    }

    LaunchedEffect(Unit) { reload() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Görevler", color = LightText, fontSize = 25.sp, fontWeight = FontWeight.Black)
            Text("Tüm oyunlar aynı rotaya hizmet eder. Rotayı tamamla, hesabını ilerlet.", color = LightMuted, fontSize = 10.sp)
        }

        item {
            Text("OYUN ROTASI", color = LightBlue, fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
        if (unified.isEmpty()) {
            item {
                Surface(shape = RoundedCornerShape(18.dp), color = LightSurface, border = BorderStroke(1.dp, LightBorder)) {
                    Text("Oyun rotası hazırlanıyor.", Modifier.fillMaxWidth().padding(16.dp), color = LightMuted)
                }
            }
        } else {
            items(unified, key = { it.missionId }) { mission ->
                UnifiedMissionCard(
                    mission = mission,
                    busy = busy,
                    onPlay = onPlayMode,
                    onClaim = {
                        val b = backend ?: return@UnifiedMissionCard
                        scope.launch {
                            busy = true
                            runCatching { b.claimUnifiedMission(mission.missionId) }
                                .onSuccess {
                                    SonHarfSoundFx.missionComplete()
                                    notice = "+${it.rewardCoins} Son Coin • Görev tamamlandı"
                                    reload()
                                }
                                .onFailure {
                                    notice = "Ödül şu anda alınamadı."
                                    SonHarfSoundFx.warning()
                                }
                            busy = false
                        }
                    },
                )
            }
        }

        if (notice.isNotBlank()) {
            item {
                Text(notice, Modifier.fillMaxWidth(), color = if (notice.startsWith("+")) LightGreen else LightMuted, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            }
        }

        item {
            Text("DİĞER GÖREVLER", color = LightMuted, fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
        if (goals.isEmpty()) {
            item {
                Surface(shape = RoundedCornerShape(18.dp), color = LightSurface, border = BorderStroke(1.dp, LightBorder)) {
                    Text("Yeni görevler hazırlanıyor.", Modifier.fillMaxWidth().padding(16.dp), color = LightMuted)
                }
            }
        } else {
            items(goals) { g ->
                val done = g.progress >= g.target
                Surface(shape = RoundedCornerShape(18.dp), color = LightSurface, border = BorderStroke(1.dp, LightBorder)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (done) Icons.Rounded.CheckCircle else Icons.Rounded.TrackChanges, null, tint = if (done) LightGreen else LightBlue)
                            Spacer(Modifier.width(9.dp))
                            Text(if (SonHarfUiState.isEnglish) g.titleEn else g.titleTr, color = LightText, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("+" + g.rewardDiamonds, color = LightGold, fontWeight = FontWeight.Black)
                        }
                        LinearProgressIndicator(
                            progress = { (g.progress.toFloat() / g.target.coerceAtLeast(1)).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = if (done) LightGreen else LightBlue,
                            trackColor = LightSurface2,
                        )
                        Text(g.progress.coerceAtMost(g.target).toString() + "/" + g.target, color = LightMuted, fontSize = 9.sp)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}
