package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.delay

internal object MonsterUi {
    val Background = Color(0xFFF7FAFF)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceRaised = Color(0xFFF0F5FC)
    val SurfaceSoft = Color(0xFFE8EEF7)
    val Text = Color(0xFF10213A)
    val Muted = Color(0xFF62758F)
    val Border = Color(0xFFD5E2F0)
    val Accent: Color get() = if (SonHarfCosmetics.monsterBlueTheme) Color(0xFF1677FF) else Color(0xFF64748B)
    val AccentText = Color.White
    val Live = Color(0xFFFF4D4F)
    val Coral = Color(0xFFFF6B61)
    val Orange = Color(0xFFF59E0B)
    val Green = Color(0xFF168A55)
    val Gold = Color(0xFFD68A00)
}

private data class MonsterHomeStats(
    val rating: Int = 1000,
    val winStreak: Int = 0,
    val dailyWins: Int = 0,
    val unreadNotifications: Int = 0,
)

private enum class MonsterDestination { HOME, GAME, WORD_SIEGE, LEAGUE, SOCIAL, STYLE, PROFILE, TASKS, VIP, SETTINGS, PROFILE_DETAILS, ACCOUNT, DAILY_CHALLENGE }

@Composable
fun MonsterExperienceApp(onSignedOut: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    var destination by remember { mutableStateOf(MonsterDestination.HOME) }
    val homeRequest = SonHarfUiState.homeRequest

    LaunchedEffect(Unit) {
        runCatching { SonHarfCosmetics.apply(backend.getEquippedCosmetics()) }
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
    Scaffold(
        containerColor = MonsterUi.Background,
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
        Box(Modifier.fillMaxSize().padding(if (topLevel) padding else PaddingValues(0.dp)).background(MonsterUi.Background)) {
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

    LaunchedEffect(Unit) {
        val id = backend.currentUserId()
        if (id != null) {
            runCatching { backend.getProfile(id) }.onSuccess { p ->
                profile = p
            }
        }
        runCatching { backend.getLeaderboardV2(limit = 100) }.onSuccess { rows ->
            val me = backend.currentUserId()
            rows.firstOrNull { it.userId == me }?.let { row ->
                stats = stats.copy(rating = row.rating)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("SON HARF", color = MonsterUi.Text, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text(sh("KELİMEYİ SÜRDÜR, RAKİBİNİ GEÇ", "KEEP THE WORD GOING, BEAT YOUR RIVAL"), color = MonsterUi.Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                MonsterIconButton(Icons.Rounded.Notifications, onTasks)
                Spacer(Modifier.width(7.dp))
                MonsterIconButton(Icons.Rounded.Settings, onSettings)
            }
        }
        item { MonsterLiveMatchCard(profile, stats, onPlay) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MonsterQuickCard(sh("KELİME\nKUŞATMASI", "WORD\nSIEGE"), sh("Alanı ele geçir", "Capture territory"), Icons.Rounded.GridView, MonsterUi.Gold, Modifier.weight(1f), onSiege)
                MonsterQuickCard(sh("LİG &\nRATING", "LEAGUE &\nRATING"), "${stats.rating} RP", Icons.Rounded.EmojiEvents, MonsterUi.Accent, Modifier.weight(1f), onLeague)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MonsterStatCard(stats.winStreak.toString(), sh("GALİBİYET SERİSİ", "WIN STREAK"), Icons.Rounded.LocalFireDepartment, MonsterUi.Coral, Modifier.weight(1f))
                MonsterStatCard(stats.dailyWins.toString(), sh("BUGÜN GALİBİYET", "WINS TODAY"), Icons.Rounded.Bolt, MonsterUi.Green, Modifier.weight(1f))
                MonsterStatCard(stats.unreadNotifications.toString(), sh("BİLDİRİM", "NOTICES"), Icons.Rounded.Notifications, MonsterUi.Gold, Modifier.weight(1f))
            }
        }
        item {
            MonsterSectionTitle(sh("OYUNCU MERKEZİ", "PLAYER HUB"), sh("TÜMÜ", "ALL"), onProfile)
            Spacer(Modifier.height(7.dp))
            MonsterHubRow(Icons.Rounded.TaskAlt, sh("Günlük görevler", "Daily missions"), sh("AÇ", "OPEN"), MonsterUi.Accent, onTasks)
            Spacer(Modifier.height(7.dp))
            MonsterHubRow(Icons.Rounded.Groups, sh("Sosyal & arkadaşlar", "Social & friends"), sh("AÇ", "OPEN"), MonsterUi.Green, onSocial)
            Spacer(Modifier.height(7.dp))
            MonsterHubRow(Icons.Rounded.PersonSearch, sh("Ezeli rakip", "Top rival"), sh("GÖR", "VIEW"), MonsterUi.Coral, onSocial)
            Spacer(Modifier.height(7.dp))
            MonsterHubRow(Icons.Rounded.Diamond, "STYLE", sh("Görünüm & koleksiyon", "Looks & collection"), MonsterUi.Gold, onStyle)
            Spacer(Modifier.height(7.dp))
            MonsterHubRow(Icons.Rounded.WorkspacePremium, "VIP", sh("Premium avantajlar", "Premium benefits"), MonsterUi.Gold, onVip)
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun MonsterLiveMatchCard(profile: ProfileDto?, stats: MonsterHomeStats, onPlay: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MonsterUi.Surface, border = BorderStroke(1.dp, MonsterUi.Border)) {
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
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MonsterUi.Accent, contentColor = MonsterUi.AccentText),
            ) { Text(sh("OYNA", "PLAY"), fontWeight = FontWeight.Black, fontSize = 15.sp) }
        }
    }
}

@Composable
private fun MonsterQuickCard(title: String, subtitle: String, icon: ImageVector, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.height(128.dp).clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), color = MonsterUi.Surface, border = BorderStroke(1.dp, MonsterUi.Border)) {
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
private fun MonsterStatCard(value: String, label: String, icon: ImageVector, accent: Color, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(15.dp), color = MonsterUi.Surface, border = BorderStroke(1.dp, MonsterUi.Border)) {
        Column(Modifier.padding(10.dp)) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(17.dp))
            Spacer(Modifier.height(7.dp))
            Text(value, color = MonsterUi.Text, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text(label, color = MonsterUi.Muted, fontSize = 6.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun MonsterHubRow(icon: ImageVector, title: String, value: String, accent: Color, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(14.dp), color = MonsterUi.Surface, border = BorderStroke(1.dp, MonsterUi.Border)) {
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
    Surface(color = MonsterUi.Surface, tonalElevation = 0.dp) {
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
