package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.delay

// SON HARF ACTION UI ADAPTATION: original Compose implementation; no third-party binary assets bundled.
internal object MonsterUi {
    val Background = SonHarfTheme.Background
    val Surface = SonHarfTheme.Surface
    val SurfaceRaised = SonHarfTheme.Surface
    val SurfaceSoft = SonHarfTheme.SurfaceSecondary
    val Text = SonHarfTheme.TextPrimary
    val Muted = SonHarfTheme.TextSecondary
    val Border = SonHarfTheme.Border
    val Accent: Color get() = SonHarfTheme.PrimaryBlue
    val AccentText = Color.White
    val Live = SonHarfTheme.Error
    val Coral = SonHarfTheme.Error
    val Orange = SonHarfTheme.Warning
    val Green = SonHarfTheme.Success
    val Gold = SonHarfTheme.Warning
}

private data class MonsterHomeStats(
    val rating: Int = 1000,
)

private enum class MonsterDestination { HOME, GAME, WORD_SIEGE, LEAGUE, SOCIAL, STYLE, PROFILE, TASKS, VIP, SETTINGS, PROFILE_DETAILS, ACCOUNT, DAILY_CHALLENGE }

@Composable
fun MonsterExperienceApp(onSignedOut: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    var destination by remember { mutableStateOf(MonsterDestination.HOME) }
    var isPremium by remember { mutableStateOf(false) }
    val homeRequest = SonHarfUiState.homeRequest

    LaunchedEffect(Unit) {
        runCatching { SonHarfCosmetics.apply(backend.getEquippedCosmetics()) }
        val id = backend.currentUserId()
        if (id != null) isPremium = runCatching { backend.getProfile(id).isVip }.getOrDefault(false)
    }
    LaunchedEffect(homeRequest) { if (homeRequest > 0) destination = MonsterDestination.HOME }
    LaunchedEffect(destination) {
        if (destination != MonsterDestination.GAME) while (true) {
            runCatching { backend.setPresence("online") }
            delay(55_000)
        }
    }
    BackHandler(enabled = destination != MonsterDestination.HOME) {
        destination = when (destination) {
            MonsterDestination.PROFILE_DETAILS, MonsterDestination.ACCOUNT, MonsterDestination.SETTINGS, MonsterDestination.VIP -> MonsterDestination.PROFILE
            MonsterDestination.DAILY_CHALLENGE -> MonsterDestination.TASKS
            else -> MonsterDestination.HOME
        }
    }

    val topLevel = destination in setOf(MonsterDestination.HOME, MonsterDestination.LEAGUE, MonsterDestination.SOCIAL, MonsterDestination.STYLE, MonsterDestination.PROFILE)
    val isGameplay = destination in setOf(MonsterDestination.GAME, MonsterDestination.WORD_SIEGE, MonsterDestination.DAILY_CHALLENGE)
    Scaffold(
        containerColor = SonHarfTheme.Background,
        topBar = { SonHarfTopAdBanner(visible = !isGameplay, isPremium = isPremium) },
        bottomBar = {
            if (topLevel) MonsterBottomBar(
                destination,
                { destination = MonsterDestination.HOME },
                { destination = MonsterDestination.LEAGUE },
                { destination = MonsterDestination.SOCIAL },
                { destination = MonsterDestination.STYLE },
                { destination = MonsterDestination.PROFILE },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(SonHarfTheme.Background)) {
            when (destination) {
                MonsterDestination.HOME -> MonsterHomeScreen(
                    backend,
                    { destination = MonsterDestination.GAME },
                    { destination = MonsterDestination.WORD_SIEGE },
                    { destination = MonsterDestination.LEAGUE },
                    { destination = MonsterDestination.SOCIAL },
                    { destination = MonsterDestination.STYLE },
                    { destination = MonsterDestination.PROFILE },
                    { destination = MonsterDestination.TASKS },
                    { destination = MonsterDestination.VIP },
                    { destination = MonsterDestination.SETTINGS },
                )
                MonsterDestination.GAME -> OnlineGameScreenV6()
                MonsterDestination.WORD_SIEGE -> WordSiegeExperienceScreen { destination = MonsterDestination.HOME }
                MonsterDestination.LEAGUE -> LeaderboardExperienceScreen { destination = MonsterDestination.HOME }
                MonsterDestination.SOCIAL -> MainSocialScreen(backend = backend, onPlay = { destination = MonsterDestination.GAME })
                MonsterDestination.STYLE -> MonsterStyleStoreScreen()
                MonsterDestination.PROFILE -> MainPlayerProfileScreen(backend, { destination = MonsterDestination.PROFILE_DETAILS }, { destination = MonsterDestination.VIP }, { destination = MonsterDestination.SETTINGS }, { destination = MonsterDestination.SOCIAL })
                MonsterDestination.TASKS -> MainRetentionScreen(backend, { destination = MonsterDestination.HOME }, { destination = MonsterDestination.GAME }, { destination = MonsterDestination.DAILY_CHALLENGE })
                MonsterDestination.VIP -> MainVipScreen(backend) { destination = MonsterDestination.PROFILE }
                MonsterDestination.SETTINGS -> MainSettingsScreen(backend, { destination = MonsterDestination.PROFILE }, { destination = MonsterDestination.ACCOUNT }, onSignedOut)
                MonsterDestination.PROFILE_DETAILS -> CompleteProfileScreen(0) { destination = MonsterDestination.PROFILE }
                MonsterDestination.ACCOUNT -> CompleteProfileScreen(1) { destination = MonsterDestination.SETTINGS }
                MonsterDestination.DAILY_CHALLENGE -> DailyCipherScreen { destination = MonsterDestination.TASKS }
            }
        }
    }
}

@Composable
private fun MonsterHomeScreen(
    backend: OnlineGameBackend,
    onPlay: () -> Unit,
    onSiege: () -> Unit,
    onLeague: () -> Unit,
    onSocial: () -> Unit,
    onStyle: () -> Unit,
    onProfile: () -> Unit,
    onTasks: () -> Unit,
    onVip: () -> Unit,
    onSettings: () -> Unit,
) {
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var stats by remember { mutableStateOf(MonsterHomeStats()) }
    var weeklyTop by remember { mutableStateOf<List<LeaderboardV2Row>>(emptyList()) }
    var goals by remember { mutableStateOf<List<GoalRowDto>>(emptyList()) }

    LaunchedEffect(Unit) {
        val id = backend.currentUserId()
        if (id != null) runCatching { backend.getProfile(id) }.onSuccess { profile = it }
        runCatching { backend.getLeaderboardV2(limit = 100) }.onSuccess { rows ->
            rows.firstOrNull { it.userId == backend.currentUserId() }?.let { row ->
                stats = stats.copy(rating = row.rating)
            }
        }
        weeklyTop = runCatching { backend.getLeaderboardV2(SonHarfUiState.language, "week", 3) }.getOrDefault(emptyList())
        goals = runCatching { backend.getGoals() }.getOrDefault(emptyList())
    }

    val activeGoal = goals.firstOrNull { !it.claimed } ?: goals.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("SON HARF", color = MonsterUi.Text, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text(sh("Kelimeyi Sürdür, Rakibini Geç", "Keep the word going, beat your rival"), color = MonsterUi.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                MonsterIconButton(Icons.Rounded.Notifications, onTasks)
                Spacer(Modifier.width(7.dp))
                MonsterIconButton(Icons.Rounded.Settings, onSettings)
            }
        }
        item {
            Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onProfile), shape = RoundedCornerShape(17.dp), color = MonsterUi.SurfaceRaised, border = BorderStroke(1.dp, MonsterUi.Border)) {
                Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = MonsterUi.Accent.copy(alpha = .12f)) {
                        Icon(Icons.Rounded.Person, null, tint = MonsterUi.Accent, modifier = Modifier.padding(8.dp).size(21.dp))
                    }
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(profile?.displayName ?: sh("OYUNCU", "PLAYER"), color = MonsterUi.Text, fontWeight = FontWeight.Black, fontSize = 13.sp, maxLines = 1)
                        Text("🏆 ${stats.rating} RP", color = MonsterUi.Muted, fontSize = 8.sp)
                    }
                    Surface(shape = RoundedCornerShape(99.dp), color = MonsterUi.Gold.copy(alpha = .12f)) {
                        Text("SC ${profile?.diamonds ?: 0}", Modifier.padding(horizontal = 9.dp, vertical = 6.dp), color = MonsterUi.Text, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        item { MonsterLiveMatchCard(profile, stats, onPlay) }
        item {
            Text(sh("OYUN MODLARI", "GAME MODES"), color = MonsterUi.Text, fontWeight = FontWeight.Black, fontSize = 11.sp)
            Spacer(Modifier.height(7.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MonsterQuickCard(sh("KLASİK", "CLASSIC"), sh("Kelime düellosu", "Word duel"), Icons.Rounded.SportsEsports, MonsterUi.Accent, Modifier.weight(1f), onPlay)
                MonsterSiegeQuickCard(Modifier.weight(1f), onSiege)
            }
            Spacer(Modifier.height(8.dp))
            MonsterLeagueCard(stats.rating, onLeague)
        }
        item {
            MonsterSectionTitle(sh("HAFTANIN EN İYİLERİ", "BEST THIS WEEK"), sh("TÜM SIRALAMA", "FULL RANKING"), onLeague)
            Spacer(Modifier.height(7.dp))
            MonsterWeeklyTopThree(weeklyTop, onLeague)
        }
        if (activeGoal != null) {
            item {
                val title = if (SonHarfUiState.isEnglish) activeGoal.titleEn else activeGoal.titleTr
                Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onTasks), shape = RoundedCornerShape(17.dp), color = MonsterUi.Green.copy(alpha = .07f), border = BorderStroke(1.dp, MonsterUi.Green.copy(alpha = .24f))) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.TrackChanges, null, tint = MonsterUi.Green, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(sh("BUGÜNKÜ HEDEF", "TODAY'S GOAL"), color = MonsterUi.Green, fontSize = 8.sp, fontWeight = FontWeight.Black)
                            Text(title, color = MonsterUi.Text, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text("${activeGoal.progress.coerceAtMost(activeGoal.target)}/${activeGoal.target}", color = MonsterUi.Muted, fontSize = 8.sp)
                        }
                        Text("SC ${activeGoal.rewardDiamonds}", color = MonsterUi.Text, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun MonsterLiveMatchCard(profile: ProfileDto?, stats: MonsterHomeStats, onPlay: () -> Unit) {
    Surface(shape = RoundedCornerShape(22.dp), color = MonsterUi.SurfaceRaised, border = BorderStroke(1.dp, MonsterUi.Border)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(MonsterUi.Live))
                Spacer(Modifier.width(6.dp))
                Text(sh("CANLI EŞLEŞME", "LIVE MATCH"), color = MonsterUi.Muted, fontSize = 9.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                Text("1v1", color = MonsterUi.Text, fontWeight = FontWeight.Black)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(profile?.displayName ?: sh("OYUNCU", "PLAYER"), color = MonsterUi.Text, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    Text("🏆 ${stats.rating}", color = MonsterUi.Gold, fontSize = 9.sp)
                }
                Text("VS", color = MonsterUi.Text, fontSize = 34.sp, fontWeight = FontWeight.Black)
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(sh("RAKİP", "RIVAL"), color = MonsterUi.Muted, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    Text(sh("Eşleşme bekliyor", "Waiting"), color = MonsterUi.Muted, fontSize = 8.sp)
                }
            }
            Button(
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MonsterUi.Accent, contentColor = MonsterUi.AccentText),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 5.dp, pressedElevation = 1.dp),
            ) {
                Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(7.dp))
                Text(sh("OYNA", "PLAY"), fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = .8.sp)
            }
        }
    }
}

@Composable
private fun MonsterSiegeQuickCard(modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(128.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MonsterUi.SurfaceRaised,
        border = BorderStroke(1.dp, MonsterUi.Border),
    ) {
        Column(
            Modifier.fillMaxSize().padding(11.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Image(
                painter = painterResource(R.drawable.kelime_kusatma_logo),
                contentDescription = sh("Kelime Kuşatması", "Word Siege"),
                modifier = Modifier.size(72.dp),
                contentScale = ContentScale.Fit,
            )
            Text(
                sh("Alanı ele geçir", "Capture territory"),
                color = MonsterUi.Muted,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun MonsterQuickCard(title: String, subtitle: String, icon: ImageVector, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.height(128.dp).clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), color = MonsterUi.SurfaceRaised, border = BorderStroke(1.dp, MonsterUi.Border)) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp))
            Column {
                Text(title, color = MonsterUi.Text, fontSize = 14.sp, lineHeight = 15.sp, fontWeight = FontWeight.Black)
                Text(subtitle, color = MonsterUi.Muted, fontSize = 8.sp)
            }
        }
    }
}

@Composable
private fun MonsterLeagueCard(rating: Int, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(17.dp), color = MonsterUi.Accent.copy(alpha = .06f), border = BorderStroke(1.dp, MonsterUi.Accent.copy(alpha = .20f))) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.EmojiEvents, null, tint = MonsterUi.Accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(sh("LİG & RATING", "LEAGUE & RATING"), color = MonsterUi.Text, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text("$rating RP", color = MonsterUi.Muted, fontSize = 9.sp)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = MonsterUi.Muted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun MonsterWeeklyTopThree(rows: List<LeaderboardV2Row>, onClick: () -> Unit) {
    if (rows.isEmpty()) {
        Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(17.dp), color = MonsterUi.SurfaceRaised, border = BorderStroke(1.dp, MonsterUi.Border)) {
            Text(sh("Bu hafta sıralama henüz oluşmadı. İlk galibiyetini al!", "Weekly ranking has not formed yet. Get the first win!"), Modifier.padding(15.dp), color = MonsterUi.Muted, fontSize = 9.sp)
        }
        return
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.Bottom) {
        rows.take(3).forEachIndexed { index, row ->
            val medal = when (index) { 0 -> "🥇"; 1 -> "🥈"; else -> "🥉" }
            val accent = if (index == 0) MonsterUi.Gold else MonsterUi.Accent
            Surface(modifier = Modifier.weight(1f).height(if (index == 0) 112.dp else 104.dp).clickable(onClick = onClick), shape = RoundedCornerShape(17.dp), color = accent.copy(alpha = if (index == 0) .12f else .06f), border = BorderStroke(1.dp, accent.copy(alpha = if (index == 0) .40f else .20f))) {
                Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(medal, fontSize = if (index == 0) 24.sp else 21.sp)
                    Text(row.displayName, color = MonsterUi.Text, fontWeight = FontWeight.Black, fontSize = 9.sp, maxLines = 1)
                    Text(sh("${row.wins} haftalık galibiyet", "${row.wins} weekly wins"), color = MonsterUi.Muted, fontSize = 7.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun MonsterHubRow(icon: ImageVector, title: String, value: String, accent: Color, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(14.dp), color = MonsterUi.SurfaceRaised, border = BorderStroke(1.dp, MonsterUi.Border)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(10.dp), color = accent.copy(alpha = .12f)) {
                Icon(icon, null, tint = accent, modifier = Modifier.padding(8.dp).size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(title, color = MonsterUi.Text, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text(value, color = MonsterUi.Muted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Rounded.ChevronRight, null, tint = MonsterUi.Muted, modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
private fun MonsterSectionTitle(title: String, action: String, onAction: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = MonsterUi.Text, fontWeight = FontWeight.Black, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text(action, color = MonsterUi.Accent, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.clickable(onClick = onAction))
    }
}

@Composable
private fun MonsterIconButton(icon: ImageVector, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(12.dp), color = MonsterUi.SurfaceRaised, border = BorderStroke(1.dp, MonsterUi.Border)) {
        Icon(icon, null, tint = MonsterUi.Text, modifier = Modifier.padding(9.dp).size(18.dp))
    }
}

@Composable
private fun MonsterBottomBar(current: MonsterDestination, home: () -> Unit, league: () -> Unit, social: () -> Unit, style: () -> Unit, profile: () -> Unit) {
    Surface(color = SonHarfTheme.Surface, tonalElevation = 0.dp, border = BorderStroke(1.dp, SonHarfTheme.Border)) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().height(60.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            MonsterNavItem(Icons.Rounded.Home, sh("ANA", "HOME"), current == MonsterDestination.HOME, home)
            MonsterNavItem(Icons.Rounded.EmojiEvents, sh("LİG", "LEAGUE"), current == MonsterDestination.LEAGUE, league)
            MonsterNavItem(Icons.Rounded.Groups, sh("SOSYAL", "SOCIAL"), current == MonsterDestination.SOCIAL, social)
            MonsterNavItem(Icons.Rounded.Diamond, "STYLE", current == MonsterDestination.STYLE, style)
            MonsterNavItem(Icons.Rounded.Person, sh("PROFİL", "PROFILE"), current == MonsterDestination.PROFILE, profile)
        }
    }
}

@Composable
private fun MonsterNavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick).padding(horizontal = 7.dp, vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = if (selected) MonsterUi.Accent else MonsterUi.Muted, modifier = Modifier.size(20.dp))
        Text(label, color = if (selected) MonsterUi.Accent else MonsterUi.Muted, fontSize = 7.sp, fontWeight = FontWeight.Bold)
    }
}
