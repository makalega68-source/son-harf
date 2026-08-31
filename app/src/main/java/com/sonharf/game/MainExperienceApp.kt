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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal object MainUi {
    val Background: Color get() = SonHarfCosmetics.gamePalette.background
    val Surface: Color get() = SonHarfCosmetics.gamePalette.surface
    val SurfaceSoft: Color get() = SonHarfCosmetics.gamePalette.surfaceSoft
    val Text: Color get() = SonHarfCosmetics.gamePalette.text
    val Muted: Color get() = SonHarfCosmetics.gamePalette.muted
    val Blue: Color get() = SonHarfCosmetics.gamePalette.accent
    val BlueDeep: Color get() = SonHarfCosmetics.gamePalette.accent
    val BlueSoft: Color get() = SonHarfCosmetics.gamePalette.accent.copy(alpha = .10f)
    val Border: Color get() = SonHarfCosmetics.gamePalette.border
    val Green = Color(0xFF22A85A)
    val Gold = Color(0xFFF3A81A)
    val Red = Color(0xFFD83A48)
    val Purple: Color get() = SonHarfCosmetics.gamePalette.secondary
}

private enum class MainDestination {
    HOME, GAME, WORD_SIEGE, LEAGUE, SOCIAL, STYLE, PROFILE, TASKS, SEASON, REWARDS,
    PROFILE_STYLE, VIP, SETTINGS, PROFILE_DETAILS, ACCOUNT, DAILY_CHALLENGE,
}

@Composable
fun SonHarfMainApp(onSignedOut: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    var destination by remember { mutableStateOf(MainDestination.HOME) }
    val homeRequest = SonHarfUiState.homeRequest

    LaunchedEffect(homeRequest) { if (homeRequest > 0) destination = MainDestination.HOME }
    LaunchedEffect(destination) {
        if (destination != MainDestination.GAME) {
            while (true) { runCatching { backend.setPresence("online") }; delay(55_000) }
        }
    }

    BackHandler(enabled = destination != MainDestination.HOME) {
        destination = when (destination) {
            MainDestination.PROFILE_DETAILS, MainDestination.PROFILE_STYLE, MainDestination.ACCOUNT,
            MainDestination.SETTINGS, MainDestination.VIP -> MainDestination.PROFILE
            MainDestination.DAILY_CHALLENGE, MainDestination.SEASON, MainDestination.REWARDS -> MainDestination.TASKS
            else -> MainDestination.HOME
        }
    }

    val topLevel = destination in setOf(MainDestination.HOME, MainDestination.LEAGUE, MainDestination.SOCIAL, MainDestination.STYLE, MainDestination.PROFILE)

    Scaffold(
        containerColor = MainUi.Background,
        bottomBar = {
            if (topLevel) {
                MainBottomNavigation(
                    destination = destination,
                    onHome = { destination = MainDestination.HOME },
                    onLeague = { destination = MainDestination.LEAGUE },
                    onSocial = { destination = MainDestination.SOCIAL },
                    onStyle = { destination = MainDestination.STYLE },
                    onProfile = { destination = MainDestination.PROFILE },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(if (topLevel) padding else PaddingValues(0.dp)).background(MainUi.Background)) {
            when (destination) {
                MainDestination.HOME -> MainHomeScreen(
                    backend = backend,
                    onPlay = { destination = MainDestination.GAME },
                    onWordSiege = { destination = MainDestination.WORD_SIEGE },
                    onLeague = { destination = MainDestination.LEAGUE },
                    onSocial = { destination = MainDestination.SOCIAL },
                    onStyle = { destination = MainDestination.STYLE },
                    onProfile = { destination = MainDestination.PROFILE },
                    onTasks = { destination = MainDestination.TASKS },
                    onSeason = { destination = MainDestination.SEASON },
                    onRewards = { destination = MainDestination.REWARDS },
                    onVip = { destination = MainDestination.VIP },
                    onSettings = { destination = MainDestination.SETTINGS },
                )
                MainDestination.GAME -> OnlineGameScreenV6()
                MainDestination.WORD_SIEGE -> WordSiegeExperienceScreen { destination = MainDestination.HOME }
                MainDestination.LEAGUE -> LeaderboardExperienceScreen { destination = MainDestination.HOME }
                MainDestination.SOCIAL -> MainSocialScreen(backend = backend, onPlay = { destination = MainDestination.GAME })
                MainDestination.STYLE -> EconomyShopScreen(onOpenProfileAppearance = { destination = MainDestination.PROFILE_STYLE })
                MainDestination.PROFILE -> MainPlayerProfileScreen(
                    backend = backend,
                    onEdit = { destination = MainDestination.PROFILE_DETAILS },
                    onAppearance = { destination = MainDestination.PROFILE_STYLE },
                    onVip = { destination = MainDestination.VIP },
                    onSettings = { destination = MainDestination.SETTINGS },
                    onSocial = { destination = MainDestination.SOCIAL },
                )
                MainDestination.TASKS -> MainRetentionScreen(backend = backend, onBack = { destination = MainDestination.HOME }, onPlay = { destination = MainDestination.GAME }, onDailyChallenge = { destination = MainDestination.DAILY_CHALLENGE })
                MainDestination.SEASON -> SeasonCenterScreen(backend = backend, onBack = { destination = MainDestination.HOME })
                MainDestination.REWARDS -> RewardCenterScreen(onOpenTasks = { destination = MainDestination.TASKS })
                MainDestination.PROFILE_STYLE -> ProfileStyleInventoryScreen(backend = backend, onBack = { destination = MainDestination.PROFILE })
                MainDestination.VIP -> MainVipScreen(backend = backend, onBack = { destination = MainDestination.PROFILE })
                MainDestination.SETTINGS -> MainSettingsScreen(backend = backend, onBack = { destination = MainDestination.PROFILE }, onAccount = { destination = MainDestination.ACCOUNT }, onSignedOut = onSignedOut)
                MainDestination.PROFILE_DETAILS -> CompleteProfileScreen(initialTab = 0, onBack = { destination = MainDestination.PROFILE })
                MainDestination.ACCOUNT -> CompleteProfileScreen(initialTab = 1, onBack = { destination = MainDestination.SETTINGS })
                MainDestination.DAILY_CHALLENGE -> DailyCipherScreen { destination = MainDestination.TASKS }
            }
        }
    }
}

@Composable
private fun MainBottomNavigation(
    destination: MainDestination,
    onHome: () -> Unit,
    onLeague: () -> Unit,
    onSocial: () -> Unit,
    onStyle: () -> Unit,
    onProfile: () -> Unit,
) {
    NavigationBar(containerColor = MainUi.Surface, tonalElevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
        listOf(
            Triple(MainDestination.HOME, Icons.Rounded.Home, sh("Ana Sayfa", "Home")) to onHome,
            Triple(MainDestination.LEAGUE, Icons.Rounded.EmojiEvents, sh("Lig", "League")) to onLeague,
            Triple(MainDestination.SOCIAL, Icons.Rounded.Groups, sh("Sosyal", "Social")) to onSocial,
            Triple(MainDestination.STYLE, Icons.Rounded.Checkroom, "Style") to onStyle,
            Triple(MainDestination.PROFILE, Icons.Rounded.Person, sh("Profil", "Profile")) to onProfile,
        ).forEach { (item, action) ->
            NavigationBarItem(
                selected = destination == item.first,
                onClick = { SonHarfSoundFx.tap(); action() },
                icon = { Icon(item.second, contentDescription = item.third) },
                label = { Text(item.third, fontSize = 9.sp, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = MainUi.Blue, selectedTextColor = MainUi.Blue, unselectedIconColor = MainUi.Muted, unselectedTextColor = MainUi.Muted, indicatorColor = MainUi.BlueSoft),
            )
        }
    }
}

@Composable
private fun MainHomeScreen(
    backend: OnlineGameBackend,
    onPlay: () -> Unit,
    onWordSiege: () -> Unit,
    onLeague: () -> Unit,
    onSocial: () -> Unit,
    onStyle: () -> Unit,
    onProfile: () -> Unit,
    onTasks: () -> Unit,
    onSeason: () -> Unit,
    onRewards: () -> Unit,
    onVip: () -> Unit,
    onSettings: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var growth by remember { mutableStateOf<GrowthDashboardDto?>(null) }
    var meta by remember { mutableStateOf<MetaProgressV2Dto?>(null) }
    var missions by remember { mutableStateOf<List<UnifiedMissionDto>>(emptyList()) }
    var rival by remember { mutableStateOf<ArchRivalDto?>(null) }
    var equipped by remember { mutableStateOf<EquippedCosmeticsDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var dailyBusy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    suspend fun reload() = coroutineScope {
        loading = true
        val id = backend.currentUserId()
        val profileTask = async { id?.let { runCatching { backend.getProfile(it) }.getOrNull() } }
        val growthTask = async { runCatching { backend.getGrowthDashboard() }.getOrNull() }
        val metaTask = async { runCatching { backend.getMetaProgressV2() }.getOrNull() }
        val missionTask = async { runCatching { backend.getUnifiedMissions() }.getOrDefault(emptyList()) }
        val rivalTask = async { runCatching { backend.getArchRival() }.getOrNull() }
        val equippedTask = async { runCatching { backend.getEquippedCosmetics() }.getOrNull() }
        profile = profileTask.await(); growth = growthTask.await(); meta = metaTask.await(); missions = missionTask.await(); rival = rivalTask.await(); equipped = equippedTask.await()
        SonHarfCosmetics.apply(equipped)
        loading = false
    }

    LaunchedEffect(Unit) { reload() }

    val p = profile
    val g = growth
    val league = ratingLeagueProgress(p?.rating ?: 1000)
    val mission = missions.firstOrNull { it.scope == "daily" && !it.claimed } ?: missions.firstOrNull { !it.claimed }
    val xpProgress = g?.let { (it.levelProgress.toFloat() / it.levelTarget.coerceAtLeast(1)).coerceIn(0f, 1f) } ?: 0f
    val seasonProgress = ((meta?.seasonProgress ?: 0).toFloat() / (meta?.seasonTarget ?: 300).coerceAtLeast(1)).coerceIn(0f, 1f)
    val seasonRemaining = ((meta?.seasonTarget ?: 300) - (meta?.seasonProgress ?: 0)).coerceAtLeast(0)
    val frameAccent = if (equipped?.profileFrameId != null) SonHarfCosmetics.profileAccent else if (p?.isVip == true) MainUi.Gold else MainUi.Blue

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(108.dp)) {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    SonHarfBrandLogo(modifier = Modifier.width(220.dp), size = 82.dp)
                    Text(sh("Kelimeyi Sürdür, Rakibini Geç", "Keep the word going, beat your rival"), color = MainUi.Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                }
                IconButton(onClick = onSettings, modifier = Modifier.align(Alignment.TopEnd)) { Icon(Icons.Rounded.Settings, contentDescription = sh("Ayarlar", "Settings"), tint = MainUi.Text) }
            }
        }

        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = MainUi.Blue, trackColor = MainUi.BlueSoft) }

        item {
            Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onProfile), shape = RoundedCornerShape(22.dp), color = MainUi.Surface, border = BorderStroke(1.dp, MainUi.Border)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProfilePhotoAvatarWithGender(avatarPath = p?.avatarPath, gender = p?.gender, name = p?.displayName ?: sh("Oyuncu", "Player"), size = 56.dp, accent = frameAccent)
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(p?.displayName ?: sh("Profil yükleniyor", "Loading profile"), color = MainUi.Text, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (p?.isVip == true) {
                                    Spacer(Modifier.width(6.dp))
                                    Surface(shape = RoundedCornerShape(8.dp), color = MainUi.Gold.copy(alpha = .14f)) { Text("VIP", Modifier.padding(horizontal = 6.dp, vertical = 3.dp), color = MainUi.Gold, fontSize = 8.sp, fontWeight = FontWeight.Black) }
                                }
                            }
                            Text("${sh("Seviye", "Level")} ${g?.level ?: 1} • ${meta?.selectedTitle ?: g?.nextTitle.orEmpty()}", color = MainUi.Muted, fontSize = 10.sp, maxLines = 1)
                        }
                        Surface(shape = RoundedCornerShape(99.dp), color = MainUi.Gold.copy(alpha = .13f)) { Text("◈ ${p?.diamonds ?: 0}", Modifier.padding(horizontal = 11.dp, vertical = 7.dp), color = MainUi.Gold, fontSize = 11.sp, fontWeight = FontWeight.Black) }
                    }
                    LinearProgressIndicator(progress = { xpProgress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = MainUi.Blue, trackColor = MainUi.SurfaceSoft)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${g?.xp ?: 0} XP", color = MainUi.Muted, fontSize = 9.sp)
                        Text("${g?.levelProgress ?: 0}/${g?.levelTarget ?: 500}", color = MainUi.Muted, fontSize = 9.sp)
                    }
                }
            }
        }

        item {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MainGameModeCard(
                    title = "SON HARF",
                    subtitle = sh("10 saniyelik anlık 1v1 kelime düellosu", "Live 1v1 word duel with 10-second turns"),
                    badge = sh("ANLIK", "LIVE"), icon = Icons.Rounded.Bolt, accent = MainUi.Blue,
                    modifier = Modifier.fillMaxWidth(), onClick = { SonHarfSoundFx.tap(); onPlay() },
                )
                MainGameModeCard(
                    title = sh("KELİME KUŞATMASI", "WORD SIEGE"),
                    subtitle = sh("Harflerini tahtaya yerleştir, alanı ele geçir", "Place tiles on the board and capture territory"),
                    badge = "1v1", icon = Icons.Rounded.GridOn, accent = MainUi.Purple,
                    modifier = Modifier.fillMaxWidth(), onClick = { SonHarfSoundFx.tap(); onWordSiege() },
                )
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MainCompactStat(Icons.Rounded.EmojiEvents, league.leagueName, "${p?.rating ?: 1000} rating", MainUi.Gold, Modifier.weight(1f), onLeague)
                MainCompactStat(Icons.Rounded.LocalFireDepartment, "${g?.currentWinStreak ?: 0}", sh("Galibiyet serisi", "Win streak"), Color(0xFFF97316), Modifier.weight(1f), onTasks)
            }
        }

        item {
            Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onTasks), shape = RoundedCornerShape(20.dp), color = MainUi.Surface, border = BorderStroke(1.dp, MainUi.Border)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = MainUi.Green.copy(alpha = .10f)) { Icon(Icons.Rounded.TaskAlt, null, tint = MainUi.Green, modifier = Modifier.padding(8.dp).size(20.dp)) }
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(sh("GÜNLÜK GÖREV", "DAILY MISSION"), color = MainUi.Green, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            Text(mission?.let { if (SonHarfUiState.isEnglish) it.titleEn else it.titleTr } ?: sh("Bugünün görevleri tamamlandı", "Today's missions are complete"), color = MainUi.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(mission?.let { "+${it.rewardCoins} SC" } ?: "✓", color = if (mission == null) MainUi.Green else MainUi.Gold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                    if (mission != null) {
                        LinearProgressIndicator(progress = { (mission.progress.toFloat() / mission.target.coerceAtLeast(1)).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = MainUi.Green, trackColor = MainUi.SurfaceSoft)
                        Text("${mission.progress.coerceAtMost(mission.target)}/${mission.target}", color = MainUi.Muted, fontSize = 9.sp)
                    }
                }
            }
        }

        item {
            Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onSeason), shape = RoundedCornerShape(20.dp), color = MainUi.BlueSoft) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(sh("SEZON YAKIN HEDEFİ", "SEASON NEARBY GOAL"), color = MainUi.Blue, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            Text(sh("Sezon ${(meta?.seasonLevel ?: 1) + 1}'e $seasonRemaining XP kaldı", "$seasonRemaining XP to Season ${(meta?.seasonLevel ?: 1) + 1}"), color = MainUi.Text, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                        Text("${meta?.dailyPlayStreak ?: 0} 🔥", color = MainUi.Text, fontWeight = FontWeight.Black)
                    }
                    LinearProgressIndicator(progress = { seasonProgress }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape), color = MainUi.Blue, trackColor = Color.White)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(meta?.seasonName ?: sh("Aktif sezon", "Active season"), color = MainUi.Muted, fontSize = 9.sp)
                        Text(sh("SEZONU AÇ", "OPEN SEASON"), color = MainUi.Blue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (g != null && !g.dailyClaimed) {
            item {
                OutlinedButton(
                    onClick = {
                        if (dailyBusy) return@OutlinedButton
                        scope.launch {
                            dailyBusy = true
                            val reward = runCatching { backend.claimDailyCheckin() }.getOrDefault(0)
                            notice = if (reward > 0) sh("+$reward Son Coin hesabına eklendi.", "+$reward Son Coins added.") else sh("Bugünün ödülü daha önce alındı.", "Today's reward was already claimed.")
                            reload(); dailyBusy = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp), enabled = !dailyBusy, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, MainUi.Gold.copy(alpha = .55f)),
                ) {
                    Icon(Icons.Rounded.CardGiftcard, null, tint = MainUi.Gold); Spacer(Modifier.width(7.dp))
                    Text(sh("GÜNLÜK ÖDÜLÜ AL  +${g.dailyReward} SC", "CLAIM DAILY REWARD  +${g.dailyReward} SC"), color = MainUi.Text, fontWeight = FontWeight.Black)
                }
            }
        }

        rival?.let { arch ->
            item {
                Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onSocial), shape = RoundedCornerShape(18.dp), color = MainUi.Surface, border = BorderStroke(1.dp, MainUi.Border)) {
                    Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("⚔", fontSize = 24.sp); Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(sh("EZELİ RAKİP", "ARCH RIVAL"), color = MainUi.Gold, fontSize = 8.sp, fontWeight = FontWeight.Black)
                            Text(arch.displayName, color = MainUi.Text, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        }
                        Text("${arch.myPoints}:${arch.theirPoints}", color = MainUi.Text, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MainHomeShortcut(Icons.Rounded.TaskAlt, sh("Görevler", "Missions"), Modifier.weight(1f), onTasks)
                MainHomeShortcut(Icons.Rounded.CalendarMonth, sh("Sezon", "Season"), Modifier.weight(1f), onSeason)
                MainHomeShortcut(Icons.Rounded.CardGiftcard, sh("Ödüller", "Rewards"), Modifier.weight(1f), onRewards)
                MainHomeShortcut(Icons.Rounded.Checkroom, "Style", Modifier.weight(1f), onStyle)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MainHomeShortcut(Icons.Rounded.Groups, sh("Sosyal", "Social"), Modifier.weight(1f), onSocial)
                MainHomeShortcut(Icons.Rounded.WorkspacePremium, "VIP", Modifier.weight(1f), onVip)
            }
        }

        notice?.let { message -> item { Text(message, Modifier.fillMaxWidth(), color = MainUi.Green, fontSize = 10.sp, fontWeight = FontWeight.Bold) } }
        item { Spacer(Modifier.height(4.dp)) }
    }
}

@Composable
private fun MainGameModeCard(
    title: String,
    subtitle: String,
    badge: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(modifier = modifier.height(112.dp).clickable(onClick = onClick), shape = RoundedCornerShape(24.dp), color = Color.Transparent, shadowElevation = 5.dp) {
        Row(
            Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = .88f), accent.copy(alpha = .72f)))).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(modifier = Modifier.size(58.dp), shape = RoundedCornerShape(18.dp), color = Color.White.copy(alpha = .16f), border = BorderStroke(1.dp, Color.White.copy(alpha = .22f))) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(31.dp)) }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(title, color = Color.White, fontSize = 20.sp, lineHeight = 22.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(5.dp))
                Text(subtitle, color = Color.White.copy(alpha = .84f), fontSize = 10.sp, lineHeight = 13.sp, maxLines = 2)
                Spacer(Modifier.height(7.dp))
                Text(sh("OYNA", "PLAY"), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxHeight()) {
                Surface(shape = RoundedCornerShape(99.dp), color = Color.White.copy(alpha = .16f)) { Text(badge, Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black) }
                Surface(shape = CircleShape, color = Color.White.copy(alpha = .18f)) { Icon(Icons.Rounded.ArrowForward, null, tint = Color.White, modifier = Modifier.padding(8.dp).size(18.dp)) }
            }
        }
    }
}

@Composable
private fun MainCompactStat(icon: ImageVector, value: String, label: String, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.height(84.dp).clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), color = MainUi.Surface, border = BorderStroke(1.dp, MainUi.Border)) {
        Row(Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(13.dp), color = accent.copy(alpha = .10f)) { Icon(icon, null, tint = accent, modifier = Modifier.padding(9.dp).size(22.dp)) }
            Spacer(Modifier.width(9.dp))
            Column { Text(value, color = MainUi.Text, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1); Text(label, color = MainUi.Muted, fontSize = 8.sp, maxLines = 1) }
        }
    }
}

@Composable
private fun MainHomeShortcut(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.height(70.dp).clickable(onClick = onClick), shape = RoundedCornerShape(17.dp), color = MainUi.Surface, border = BorderStroke(1.dp, MainUi.Border)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = MainUi.Blue, modifier = Modifier.size(21.dp)); Spacer(Modifier.height(4.dp))
            Text(label, color = MainUi.Text, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
internal fun MainScreenHeader(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
    actionIcon: ImageVector? = null,
    actionDescription: String = "",
    onAction: (() -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = sh("Geri", "Back"), tint = MainUi.Text) }
        Column(Modifier.weight(1f)) { Text(title, color = MainUi.Text, fontSize = 24.sp, fontWeight = FontWeight.Black); Text(subtitle, color = MainUi.Muted, fontSize = 10.sp) }
        if (actionIcon != null && onAction != null) IconButton(onClick = onAction) { Icon(actionIcon, contentDescription = actionDescription, tint = MainUi.Text) }
    }
}

@Composable
internal fun MainMetricCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = MainUi.Surface, border = BorderStroke(1.dp, MainUi.Border)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = MainUi.Text, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(label, color = MainUi.Muted, fontSize = 8.5.sp, maxLines = 1)
        }
    }
}

@Composable
internal fun MainSectionTitle(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = MainUi.Text, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = .3.sp)
        if (action != null && onAction != null) TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp)) { Text(action, color = MainUi.Blue, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
    }
}
