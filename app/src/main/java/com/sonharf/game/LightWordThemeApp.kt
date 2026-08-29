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
            "duel", "word_arena", "team_arena", "bil_bakalim" -> {
                arenaInitialRoomId = null
                WordArenaNavigation.clearRoom()
                screen = LightScreen.KELIME_ARENASI
            }
            "daily_cipher", "daily_arena", "semantic_path", "word_conquest" -> screen = LightScreen.KELIME_AVI
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
                    WordConquestGameScreen { screen = LightScreen.HOME }
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

    val rating = profile?.rating ?: 1000
    val league = ratingLeagueProgress(rating)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("KELİME TAHTI", color = LightText, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    Text("Kelimeyi Fethet, Tahtını Koru", color = LightMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
                Box(Modifier.clickable { SonHarfSoundFx.tap(); onProfile() }) {
                    ProfilePhotoAvatar(
                        avatarPath = profile?.avatarPath,
                        name = profile?.displayName ?: "Oyuncu",
                        size = 46.dp,
                        visible = true,
                        accent = LightBlue,
                    )
                }
            }
        }

        item {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.EmojiEvents, null, tint = LightGold, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(league.leagueName, color = LightText, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        Text(
                            if (league.nextAt == null) "$rating rating • ${sh("En üst lig", "Top league")}"
                            else "$rating rating • ${league.pointsToNext} ${sh("puan kaldı", "points left")}",
                            color = LightMuted,
                            fontSize = 10.sp,
                        )
                    }
                    Text("${growth?.currentWinStreak ?: 0} 🔥", color = LightText, fontWeight = FontWeight.Black, fontSize = 15.sp)
                }
                LinearProgressIndicator(
                    progress = { league.progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = LightBlue,
                    trackColor = Color(0xFFE8EEF6),
                )
            }
        }

        item {
            Text("İKİ ANA OYUN", color = LightMuted, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }

        item {
            LightGameCard(
                icon = Icons.Rounded.GridView,
                title = "KELİME FETHİ",
                subtitle = "Kelime bul, kareleri ele geçir.",
                buttonText = "FETHET",
                accent = LightGreen,
                onClick = onKelimeAvi,
            )
        }

        item {
            LightGameCard(
                icon = Icons.Rounded.Bolt,
                title = "KELİME DÜELLOSU",
                subtitle = "Aynı harflerden kelime üret; canlı rakibini geç.",
                buttonText = "DÜELLO",
                accent = LightBlue,
                onClick = onKelimeArenasi,
            )
        }

        if (nextRoute != null) {
            item {
                val m = nextRoute
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable(enabled = m.modeKey != "route") { onRouteMode(m.modeKey) },
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFEAF3FF),
                ) {
                    Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = LightBlue.copy(alpha = .12f)) {
                            Icon(Icons.Rounded.TaskAlt, null, tint = LightBlue, modifier = Modifier.padding(9.dp).size(18.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("BUGÜNÜN ROTASI", color = LightBlue, fontSize = 8.sp, fontWeight = FontWeight.Black)
                            Text(if (SonHarfUiState.isEnglish) m.titleEn else m.titleTr, color = LightText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("+${m.rewardCoins} ◆", color = LightGold, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { SonHarfSoundFx.tap(); onCompetition() },
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFFF1F4FF),
                border = BorderStroke(1.dp, Color(0xFFDCE3FF)),
            ) {
                Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Groups, null, tint = Color(0xFF3557C8))
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text("SOSYAL & REKABET", color = LightText, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Text("Arkadaşlar • ezeli rakip • kulüpler • turnuvalar", color = LightMuted, fontSize = 9.sp)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = LightMuted)
                }
            }
        }

        if (topClubs.isNotEmpty() || topPlayers.isNotEmpty()) {
            item {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("HAFTANIN ZİRVESİ", color = LightMuted, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HomeWeeklyClubTile(topClubs, Modifier.weight(1f))
                        HomeWeeklyPlayerTile(topPlayers, topPlayerProfiles, Modifier.weight(1f))
                    }
                }
            }
        }

        rival?.let { r ->
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("⚔", fontSize = 22.sp)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("EZELİ RAKİP", color = LightGold, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        Text(r.displayName, color = LightText, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                    Text("${r.myPoints}:${r.theirPoints}", color = LightText, fontSize = 17.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HomeQuickAction(Icons.Rounded.Groups, "Sosyal", Modifier.weight(1f), onCompetition)
                HomeQuickAction(Icons.Rounded.ShoppingCart, "Market", Modifier.weight(1f), onMarket)
                HomeQuickAction(Icons.Rounded.TaskAlt, "Görevler", Modifier.weight(1f), onTasks)
                HomeQuickAction(Icons.Rounded.EmojiEvents, "Lig", Modifier.weight(1f), onLeague)
            }
        }
        item { Spacer(Modifier.height(6.dp)) }
    }
}

@Composable
private fun HomeModeTile(
    icon: ImageVector,
    title: String,
    meta: String,
    accent: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.height(112.dp).clickable { SonHarfSoundFx.tap(); onClick() },
        shape = RoundedCornerShape(20.dp),
        color = accent.copy(alpha = .10f),
    ) {
        Column(Modifier.fillMaxSize().padding(13.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(27.dp))
            Column {
                Text(title, color = LightText, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1)
                Text(meta, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HomeWeeklyClubTile(clubs: List<ClubDirectoryRowDto>, modifier: Modifier) {
    Surface(modifier = modifier.height(118.dp), shape = RoundedCornerShape(18.dp), color = Color(0xFFFFF7E8)) {
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("🏆 KULÜPLER", color = LightGold, fontSize = 8.sp, fontWeight = FontWeight.Black)
            if (clubs.isEmpty()) {
                Text("Sıralama oluşuyor", color = LightMuted, fontSize = 9.sp)
            } else {
                clubs.take(3).forEachIndexed { index, club ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${index + 1}", color = LightGold, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(15.dp))
                        Text(club.name, color = LightText, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f))
                        Text("${club.weeklyPoints}", color = LightMuted, fontSize = 8.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeWeeklyPlayerTile(
    players: List<WeeklyTournamentLeaderboardRowDto>,
    profiles: Map<String, ProfileDto?>,
    modifier: Modifier,
) {
    Surface(modifier = modifier.height(118.dp), shape = RoundedCornerShape(18.dp), color = Color(0xFFEEF4FF)) {
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("🏆 OYUNCULAR", color = LightBlue, fontSize = 8.sp, fontWeight = FontWeight.Black)
            if (players.isEmpty()) {
                Text("Sıralama oluşuyor", color = LightMuted, fontSize = 9.sp)
            } else {
                players.take(3).forEachIndexed { index, player ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProfilePhotoAvatar(
                            avatarPath = profiles[player.userId]?.avatarPath,
                            name = player.displayName,
                            size = 22.dp,
                            visible = true,
                            accent = LightBlue,
                        )
                        Spacer(Modifier.width(5.dp))
                        Text("${index + 1}. ${player.displayName}", color = LightText, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f))
                        Text("${player.points}", color = LightMuted, fontSize = 8.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeQuickAction(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clickable { SonHarfSoundFx.tap(); onClick() }.padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = LightBlue, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = LightText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
    var busyMissionId by remember { mutableStateOf<String?>(null) }
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
                    busy = busyMissionId == mission.missionId,
                    onPlay = onPlayMode,
                    onClaim = {
                        val b = backend ?: return@UnifiedMissionCard
                        scope.launch {
                            if (busyMissionId != null) return@launch
                            busyMissionId = mission.missionId
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
                            busyMissionId = null
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
