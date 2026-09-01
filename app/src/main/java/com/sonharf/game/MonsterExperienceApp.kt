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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

/**
 * Active visual shell based on the purchased Monster Livescore mobile UI language.
 *
 * Product systems are preserved; layout, visual hierarchy, navigation chrome and
 * card system are intentionally rebuilt from scratch instead of reskinning the
 * previous Son Harf home screen.
 */
internal object MonsterUi {
    val Background = Color(0xFF101114)
    val Surface = Color(0xFF181A1F)
    val SurfaceRaised = Color(0xFF202228)
    val SurfaceSoft = Color(0xFF25272E)
    val Text = Color(0xFFF7F7F8)
    val Muted = Color(0xFF8E929D)
    val Border = Color(0xFF2C2F36)
    val Accent = Color(0xFFEAFB17)
    val AccentText = Color(0xFF101114)
    val Live = Color(0xFFFF3B30)
    val Coral = Color(0xFFFF5B4D)
    val Orange = Color(0xFFFF8A3D)
    val Green = Color(0xFF47C77A)
    val Gold = Color(0xFFFFC857)
}

private enum class MonsterDestination {
    HOME,
    GAME,
    WORD_SIEGE,
    LEAGUE,
    SOCIAL,
    STYLE,
    PROFILE,
    TASKS,
    VIP,
    SETTINGS,
    PROFILE_DETAILS,
    ACCOUNT,
    DAILY_CHALLENGE,
}

@Composable
fun MonsterExperienceApp(onSignedOut: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    var destination by remember { mutableStateOf(MonsterDestination.HOME) }
    val homeRequest = SonHarfUiState.homeRequest

    LaunchedEffect(homeRequest) {
        if (homeRequest > 0) destination = MonsterDestination.HOME
    }
    LaunchedEffect(destination) {
        if (destination != MonsterDestination.GAME) {
            while (true) {
                runCatching { backend.setPresence("online") }
                delay(55_000)
            }
        }
    }

    BackHandler(enabled = destination != MonsterDestination.HOME) {
        destination = when (destination) {
            MonsterDestination.PROFILE_DETAILS,
            MonsterDestination.ACCOUNT,
            MonsterDestination.SETTINGS,
            MonsterDestination.VIP -> MonsterDestination.PROFILE
            MonsterDestination.DAILY_CHALLENGE -> MonsterDestination.TASKS
            else -> MonsterDestination.HOME
        }
    }

    val topLevel = destination in setOf(
        MonsterDestination.HOME,
        MonsterDestination.LEAGUE,
        MonsterDestination.SOCIAL,
        MonsterDestination.STYLE,
        MonsterDestination.PROFILE,
    )

    Scaffold(
        containerColor = MonsterUi.Background,
        bottomBar = {
            if (topLevel) {
                MonsterBottomBar(
                    destination = destination,
                    onHome = { destination = MonsterDestination.HOME },
                    onLeague = { destination = MonsterDestination.LEAGUE },
                    onSocial = { destination = MonsterDestination.SOCIAL },
                    onStyle = { destination = MonsterDestination.STYLE },
                    onProfile = { destination = MonsterDestination.PROFILE },
                )
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(if (topLevel) padding else PaddingValues(0.dp))
                .background(MonsterUi.Background),
        ) {
            when (destination) {
                MonsterDestination.HOME -> MonsterHomeScreen(
                    backend = backend,
                    onPlay = { destination = MonsterDestination.GAME },
                    onWordSiege = { destination = MonsterDestination.WORD_SIEGE },
                    onLeague = { destination = MonsterDestination.LEAGUE },
                    onSocial = { destination = MonsterDestination.SOCIAL },
                    onStyle = { destination = MonsterDestination.STYLE },
                    onProfile = { destination = MonsterDestination.PROFILE },
                    onTasks = { destination = MonsterDestination.TASKS },
                    onVip = { destination = MonsterDestination.VIP },
                    onSettings = { destination = MonsterDestination.SETTINGS },
                )
                MonsterDestination.GAME -> OnlineGameScreenV6()
                MonsterDestination.WORD_SIEGE -> WordSiegeExperienceScreen { destination = MonsterDestination.HOME }
                MonsterDestination.LEAGUE -> LeaderboardExperienceScreen { destination = MonsterDestination.HOME }
                MonsterDestination.SOCIAL -> MainSocialScreen(backend = backend, onPlay = { destination = MonsterDestination.GAME })
                MonsterDestination.STYLE -> EconomyShopScreen()
                MonsterDestination.PROFILE -> MainPlayerProfileScreen(
                    backend = backend,
                    onEdit = { destination = MonsterDestination.PROFILE_DETAILS },
                    onVip = { destination = MonsterDestination.VIP },
                    onSettings = { destination = MonsterDestination.SETTINGS },
                    onSocial = { destination = MonsterDestination.SOCIAL },
                )
                MonsterDestination.TASKS -> MainRetentionScreen(
                    backend = backend,
                    onBack = { destination = MonsterDestination.HOME },
                    onPlay = { destination = MonsterDestination.GAME },
                    onDailyChallenge = { destination = MonsterDestination.DAILY_CHALLENGE },
                )
                MonsterDestination.VIP -> MainVipScreen(backend = backend, onBack = { destination = MonsterDestination.PROFILE })
                MonsterDestination.SETTINGS -> MainSettingsScreen(
                    backend = backend,
                    onBack = { destination = MonsterDestination.PROFILE },
                    onAccount = { destination = MonsterDestination.ACCOUNT },
                    onSignedOut = onSignedOut,
                )
                MonsterDestination.PROFILE_DETAILS -> CompleteProfileScreen(initialTab = 0, onBack = { destination = MonsterDestination.PROFILE })
                MonsterDestination.ACCOUNT -> CompleteProfileScreen(initialTab = 1, onBack = { destination = MonsterDestination.SETTINGS })
                MonsterDestination.DAILY_CHALLENGE -> DailyCipherScreen { destination = MonsterDestination.TASKS }
            }
        }
    }
}

@Composable
private fun MonsterBottomBar(
    destination: MonsterDestination,
    onHome: () -> Unit,
    onLeague: () -> Unit,
    onSocial: () -> Unit,
    onStyle: () -> Unit,
    onProfile: () -> Unit,
) {
    NavigationBar(containerColor = MonsterUi.Surface, tonalElevation = 0.dp) {
        listOf(
            Triple(MonsterDestination.HOME, Icons.Rounded.SportsEsports, sh("Oyna", "Play")) to onHome,
            Triple(MonsterDestination.LEAGUE, Icons.Rounded.EmojiEvents, sh("Lig", "League")) to onLeague,
            Triple(MonsterDestination.SOCIAL, Icons.Rounded.Groups, sh("Sosyal", "Social")) to onSocial,
            Triple(MonsterDestination.STYLE, Icons.Rounded.Checkroom, "Style") to onStyle,
            Triple(MonsterDestination.PROFILE, Icons.Rounded.Person, sh("Profil", "Profile")) to onProfile,
        ).forEach { (item, action) ->
            NavigationBarItem(
                selected = destination == item.first,
                onClick = { SonHarfSoundFx.tap(); action() },
                icon = { Icon(item.second, contentDescription = item.third) },
                label = { Text(item.third, fontSize = 9.sp, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MonsterUi.Accent,
                    selectedTextColor = MonsterUi.Accent,
                    unselectedIconColor = MonsterUi.Muted,
                    unselectedTextColor = MonsterUi.Muted,
                    indicatorColor = Color.Transparent,
                ),
            )
        }
    }
}

@Composable
private fun MonsterHomeScreen(
    backend: OnlineGameBackend,
    onPlay: () -> Unit,
    onWordSiege: () -> Unit,
    onLeague: () -> Unit,
    onSocial: () -> Unit,
    onStyle: () -> Unit,
    onProfile: () -> Unit,
    onTasks: () -> Unit,
    onVip: () -> Unit,
    onSettings: () -> Unit,
) {
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var growth by remember { mutableStateOf<GrowthDashboardDto?>(null) }
    var meta by remember { mutableStateOf<MetaProgressV2Dto?>(null) }
    var missions by remember { mutableStateOf<List<UnifiedMissionDto>>(emptyList()) }
    var rival by remember { mutableStateOf<ArchRivalDto?>(null) }
    var loading by remember { mutableStateOf(true) }

    suspend fun reload() = coroutineScope {
        loading = true
        val id = backend.currentUserId()
        val profileTask = async { id?.let { runCatching { backend.getProfile(it) }.getOrNull() } }
        val growthTask = async { runCatching { backend.getGrowthDashboard() }.getOrNull() }
        val metaTask = async { runCatching { backend.getMetaProgressV2() }.getOrNull() }
        val missionTask = async { runCatching { backend.getUnifiedMissions() }.getOrDefault(emptyList()) }
        val rivalTask = async { runCatching { backend.getArchRival() }.getOrNull() }
        profile = profileTask.await()
        growth = growthTask.await()
        meta = metaTask.await()
        missions = missionTask.await()
        rival = rivalTask.await()
        loading = false
    }
    LaunchedEffect(Unit) { reload() }

    val p = profile
    val g = growth
    val mission = missions.firstOrNull { it.scope == "daily" && !it.claimed } ?: missions.firstOrNull { !it.claimed }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("SON HARF", color = MonsterUi.Text, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(MonsterUi.Live))
                        Spacer(Modifier.width(6.dp))
                        Text(sh("CANLI KELİME ARENASI", "LIVE WORD ARENA"), color = MonsterUi.Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Surface(shape = CircleShape, color = MonsterUi.SurfaceRaised, onClick = onSettings) {
                    Icon(Icons.Rounded.Notifications, null, tint = MonsterUi.Text, modifier = Modifier.padding(11.dp).size(20.dp))
                }
                Spacer(Modifier.width(8.dp))
                Surface(shape = CircleShape, color = MonsterUi.SurfaceRaised, onClick = onProfile) {
                    Icon(Icons.Rounded.Person, null, tint = MonsterUi.Text, modifier = Modifier.padding(11.dp).size(20.dp))
                }
            }
        }

        if (loading) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = MonsterUi.Accent, trackColor = MonsterUi.SurfaceSoft) }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                MonsterModePill(sh("Düello", "Duel"), true, onPlay)
                MonsterModePill(sh("Kuşatma", "Siege"), false, onWordSiege)
                MonsterModePill(sh("Lig", "League"), false, onLeague)
                MonsterModePill(sh("Görev", "Mission"), false, onTasks)
            }
        }

        item {
            MonsterLiveMatchCard(
                playerName = p?.displayName ?: sh("Oyuncu", "Player"),
                rating = p?.rating ?: 1000,
                streak = g?.currentWinStreak ?: 0,
                onPlay = onPlay,
            )
        }

        item {
            Text(sh("REKABET MERKEZİ", "COMPETITION HUB"), color = MonsterUi.Text, fontSize = 13.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MonsterStatCard(Icons.Rounded.EmojiEvents, sh("Lig", "League"), "${p?.rating ?: 1000}", MonsterUi.Gold, Modifier.weight(1f), onLeague)
                MonsterStatCard(Icons.Rounded.LocalFireDepartment, sh("Seri", "Streak"), "${g?.currentWinStreak ?: 0}", MonsterUi.Coral, Modifier.weight(1f), onTasks)
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onTasks),
                shape = RoundedCornerShape(16.dp),
                color = MonsterUi.Surface,
                border = BorderStroke(1.dp, MonsterUi.Border),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(10.dp), color = MonsterUi.Accent.copy(alpha = .12f)) {
                            Icon(Icons.Rounded.TaskAlt, null, tint = MonsterUi.Accent, modifier = Modifier.padding(8.dp).size(19.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(sh("GÜNÜN GÖREVİ", "TODAY'S MISSION"), color = MonsterUi.Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text(
                                mission?.let { if (SonHarfUiState.isEnglish) it.titleEn else it.titleTr }
                                    ?: sh("Bugünün görevleri tamamlandı", "Today's missions are complete"),
                                color = MonsterUi.Text,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(mission?.let { "+${it.rewardCoins} SC" } ?: "✓", color = MonsterUi.Accent, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                    if (mission != null) {
                        LinearProgressIndicator(
                            progress = { (mission.progress.toFloat() / mission.target.coerceAtLeast(1)).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                            color = MonsterUi.Accent,
                            trackColor = MonsterUi.SurfaceSoft,
                        )
                    }
                }
            }
        }

        rival?.let { arch ->
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onSocial),
                    shape = RoundedCornerShape(16.dp),
                    color = MonsterUi.Surface,
                    border = BorderStroke(1.dp, MonsterUi.Border),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = MonsterUi.Coral.copy(alpha = .14f)) {
                            Icon(Icons.Rounded.Swords, null, tint = MonsterUi.Coral, modifier = Modifier.padding(9.dp).size(19.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(sh("EZELİ RAKİP", "ARCH RIVAL"), color = MonsterUi.Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text(arch.displayName, color = MonsterUi.Text, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                        Text("${arch.myPoints}:${arch.theirPoints}", color = MonsterUi.Text, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        item {
            Text(sh("OYUNCU MERKEZİ", "PLAYER HUB"), color = MonsterUi.Text, fontSize = 13.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MonsterShortcut(Icons.Rounded.Groups, sh("Sosyal", "Social"), Modifier.weight(1f), onSocial)
                MonsterShortcut(Icons.Rounded.Checkroom, "Style", Modifier.weight(1f), onStyle)
                MonsterShortcut(Icons.Rounded.WorkspacePremium, "VIP", Modifier.weight(1f), onVip)
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MonsterUi.SurfaceRaised,
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(sh("SEVİYE", "LEVEL") + " ${g?.level ?: 1}", color = MonsterUi.Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text(meta?.selectedTitle ?: g?.nextTitle.orEmpty(), color = MonsterUi.Text, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                    Text("${g?.xp ?: 0} XP", color = MonsterUi.Accent, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        item { Spacer(Modifier.height(4.dp)) }
    }
}

@Composable
private fun MonsterModePill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { SonHarfSoundFx.tap(); onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MonsterUi.Accent else MonsterUi.Surface,
        border = if (selected) null else BorderStroke(1.dp, MonsterUi.Border),
    ) {
        Text(label, Modifier.padding(horizontal = 13.dp, vertical = 9.dp), color = if (selected) MonsterUi.AccentText else MonsterUi.Muted, fontSize = 10.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun MonsterLiveMatchCard(playerName: String, rating: Int, streak: Int, onPlay: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { SonHarfSoundFx.tap(); onPlay() },
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(MonsterUi.Coral, Color(0xFFFF315E))))
                .padding(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(8.dp), color = Color.Black.copy(alpha = .18f)) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(MonsterUi.Accent))
                            Spacer(Modifier.width(5.dp))
                            Text(sh("CANLI EŞLEŞME", "LIVE MATCH"), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Icon(Icons.Rounded.FavoriteBorder, null, tint = Color.White.copy(alpha = .9f), modifier = Modifier.size(19.dp))
                }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(shape = CircleShape, color = Color.White.copy(alpha = .16f)) {
                            Icon(Icons.Rounded.Person, null, tint = Color.White, modifier = Modifier.padding(12.dp).size(24.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(playerName, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("VS", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                        Text("$rating RATING", color = Color.White.copy(alpha = .75f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(shape = CircleShape, color = Color.White.copy(alpha = .16f)) {
                            Icon(Icons.Rounded.PersonSearch, null, tint = Color.White, modifier = Modifier.padding(12.dp).size(24.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(sh("Rakip Bul", "Find Rival"), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("🔥 $streak " + sh("galibiyet serisi", "win streak"), color = Color.White.copy(alpha = .82f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Surface(shape = RoundedCornerShape(10.dp), color = MonsterUi.Accent) {
                        Text(sh("OYNA", "PLAY"), Modifier.padding(horizontal = 17.dp, vertical = 9.dp), color = MonsterUi.AccentText, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun MonsterStatCard(icon: ImageVector, title: String, value: String, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(92.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MonsterUi.Surface,
        border = BorderStroke(1.dp, MonsterUi.Border),
    ) {
        Row(Modifier.fillMaxSize().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = accent.copy(alpha = .12f)) {
                Icon(icon, null, tint = accent, modifier = Modifier.padding(9.dp).size(21.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, color = MonsterUi.Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text(value, color = MonsterUi.Text, fontSize = 17.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun MonsterShortcut(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(78.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        color = MonsterUi.Surface,
        border = BorderStroke(1.dp, MonsterUi.Border),
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = MonsterUi.Accent, modifier = Modifier.size(21.dp))
            Spacer(Modifier.height(7.dp))
            Text(label, color = MonsterUi.Text, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}
