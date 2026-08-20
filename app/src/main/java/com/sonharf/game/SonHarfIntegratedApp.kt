package com.sonharf.game

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.launch

object SonHarfGameNavigation {
    var lobbyRequest by mutableIntStateOf(0)
        private set

    fun requestLobby() {
        lobbyRequest += 1
    }
}

@Composable
fun SonHarfIntegratedApp() {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var screen by remember { mutableStateOf(AppScreen.HOME) }
    var gameKey by remember { mutableIntStateOf(0) }
    var authChecked by remember { mutableStateOf(false) }
    var authenticated by remember { mutableStateOf(false) }
    var showVip by remember { mutableStateOf(false) }
    val lobbyRequest = SonHarfGameNavigation.lobbyRequest

    LaunchedEffect(Unit) {
        authenticated = SupabaseProvider.configured && hasVerifiedMembershipSession()
        authChecked = true
    }
    LaunchedEffect(authenticated) {
        if (!authenticated) return@LaunchedEffect
        val mode = runCatching { backend?.getPreferredGameMode() }.getOrNull()
        if (mode != null) SonHarfGameModeState.mode = mode
        SonHarfCosmetics.apply(runCatching { backend?.getEquippedCosmetics() }.getOrNull())
    }
    LaunchedEffect(lobbyRequest) {
        if (lobbyRequest > 0 && authenticated) {
            gameKey += 1
            screen = AppScreen.GAME
        }
    }

    if (!authChecked) {
        Box(Modifier.fillMaxSize().background(SonHarfBg), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    if (!authenticated) {
        RequiredAuthGate { authenticated = true; screen = AppScreen.HOME }
        return
    }

    val pageGradient = if (SonHarfUiState.darkMode) {
        listOf(Color(0xFF020711), SonHarfBg, Color(0xFF08192A))
    } else {
        listOf(Color(0xFFDDF2FF), SonHarfBg, Color(0xFFE6F6FF))
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = SonHarfBg,
            bottomBar = { if (screen != AppScreen.LEADERBOARD) IntegratedBottomBar(screen) { screen = it } },
        ) { pad ->
            Box(
                Modifier.fillMaxSize().padding(pad).background(Brush.verticalGradient(pageGradient)),
            ) {
                when (screen) {
                    AppScreen.HOME -> IntegratedHomeScreen(
                        onPlay = { screen = AppScreen.GAME },
                        onShop = { screen = AppScreen.SHOP },
                        onHub = { screen = AppScreen.MORE },
                        onLeaderboard = { screen = AppScreen.LEADERBOARD },
                        onVip = { showVip = true },
                    )
                    AppScreen.GAME -> key(gameKey) { OnlineGameScreenV6() }
                    AppScreen.SHOP -> EconomyShopScreen()
                    AppScreen.PROFILE -> ProfileExperienceScreen()
                    AppScreen.MORE -> MetaHubScreen()
                    AppScreen.LEADERBOARD -> LeaderboardExperienceScreen { screen = AppScreen.HOME }
                }
            }
        }
        if (SonHarfGameModeState.mode == "expert") ExpertArenaOverlay() else SketchGameOverlayV9()
        ComboOverlayV9()
    }
    if (showVip) VipPurchaseDialog { showVip = false }
}

@Composable
private fun IntegratedBottomBar(screen: AppScreen, onChange: (AppScreen) -> Unit) {
    NavigationBar(containerColor = SonHarfSurface.copy(alpha = .97f), tonalElevation = 0.dp) {
        listOf(
            Triple(AppScreen.HOME, "⌂", sh("Ana Sayfa", "Home")),
            Triple(AppScreen.GAME, "⚔", sh("Oyna", "Play")),
            Triple(AppScreen.SHOP, "◇", sh("Mağaza", "Shop")),
            Triple(AppScreen.PROFILE, "♙", sh("Profil", "Profile")),
            Triple(AppScreen.MORE, "◎", sh("Merkez", "Hub")),
        ).forEach { (target, icon, label) ->
            NavigationBarItem(
                selected = screen == target,
                onClick = { SonHarfSoundFx.tap(); onChange(target) },
                icon = { Text(icon, color = if (screen == target) SonHarfCyan else SonHarfMuted, fontSize = 20.sp, fontWeight = FontWeight.Black) },
                label = { Text(label, fontSize = 9.sp) },
                colors = NavigationBarItemDefaults.colors(indicatorColor = SonHarfPurple.copy(alpha = .14f)),
            )
        }
    }
}

@Composable
private fun IntegratedHomeScreen(
    onPlay: () -> Unit,
    onShop: () -> Unit,
    onHub: () -> Unit,
    onLeaderboard: () -> Unit,
    onVip: () -> Unit,
) {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var socialProfile by remember { mutableStateOf<SocialProfileDto?>(null) }
    var goals by remember { mutableStateOf<List<GoalRowDto>>(emptyList()) }
    var weeklyTop by remember { mutableStateOf<List<LeaderboardV2Row>>(emptyList()) }
    var mode by remember { mutableStateOf(SonHarfGameModeState.mode) }

    LaunchedEffect(Unit) {
        val id = backend?.currentUserId()
        if (id != null) {
            profile = runCatching { backend.getProfile(id) }.getOrNull()
            socialProfile = runCatching { backend.getSocialProfile(id) }.getOrNull()
        }
        goals = runCatching { backend?.getGoals().orEmpty() }.getOrDefault(emptyList())
        weeklyTop = runCatching { backend?.getLeaderboardV2(SonHarfUiState.language, "week", 3).orEmpty() }.getOrDefault(emptyList())
        val remoteMode = runCatching { backend?.getPreferredGameMode() }.getOrNull()
        if (remoteMode != null) mode = remoteMode
        SonHarfGameModeState.mode = mode
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SonHarfLogo()
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(99.dp), color = SonHarfCyan.copy(alpha = .12f), border = BorderStroke(1.dp, SonHarfCyan.copy(alpha = .3f))) {
                        Text("💎 ${profile?.diamonds ?: 0}", Modifier.padding(horizontal = 11.dp, vertical = 7.dp), color = SonHarfCyan, fontWeight = FontWeight.Black)
                    }
                    Surface(onClick = onHub, shape = CircleShape, color = SonHarfSurface) {
                        Text("🎯 ${goals.count { it.progress >= it.target && !it.claimed }}", Modifier.padding(horizontal = 10.dp, vertical = 7.dp), fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                SocialAvatar(
                    gender = socialProfile?.gender,
                    name = profile?.displayName ?: sh("Oyuncu", "Player"),
                    size = 52.dp,
                    accent = SonHarfCosmetics.profileAccent,
                )
                Column {
                    Text(profile?.displayName ?: sh("Oyuncu", "Player"), color = SonHarfCosmetics.playerNameColor, fontSize = 19.sp, fontWeight = FontWeight.Black)
                    Text(if (profile?.isVip == true) "VIP AKTİF" else sh("Düelloya hazır", "Ready to duel"), color = if (profile?.isVip == true) SonHarfGold else SonHarfMuted, fontSize = 10.sp)
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface.copy(alpha = .96f)), shape = RoundedCornerShape(28.dp), border = BorderStroke(1.dp, SonHarfCyan.copy(alpha = .18f))) {
                Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(sh("HEMEN OYNA", "PLAY NOW"), fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Text(if (mode == "expert") sh("UZMAN MODU • 15/15/15 • ×1/×2/×3", "EXPERT MODE • 15/15/15 • ×1/×2/×3") else sh("NORMAL MOD • 3 × 10", "NORMAL MODE • 3 × 10"), color = if (mode == "expert") SonHarfGold else SonHarfCyan, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = mode == "normal", onClick = { mode = "normal"; SonHarfGameModeState.mode = "normal"; scope.launch { runCatching { backend?.setPreferredGameMode("normal") } } }, label = { Text("NORMAL") })
                        FilterChip(selected = mode == "expert", onClick = { mode = "expert"; SonHarfGameModeState.mode = "expert"; scope.launch { runCatching { backend?.setPreferredGameMode("expert") } } }, label = { Text(sh("UZMAN", "EXPERT")) })
                    }
                    Button(onClick = onPlay, modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(22.dp), colors = ButtonDefaults.buttonColors(containerColor = SonHarfGold, contentColor = Color(0xFF241600))) {
                        Text(sh("DÜELLOYA GİR  ⚡", "ENTER DUEL  ⚡"), fontWeight = FontWeight.Black, fontSize = 18.sp)
                    }
                    if (mode == "expert") Text(sh("1. round son 1 harf • 2. round son 2 harf ×2 • 3. round son 3 harf ×3", "Round 1 last 1 letter • round 2 last 2 letters ×2 • round 3 last 3 letters ×3"), color = SonHarfMuted, fontSize = 9.sp, textAlign = TextAlign.Center)
                }
            }
        }

        item { AnimatedVipHomeCard(active = profile?.isVip == true, onClick = onVip) }

        item {
            Text(sh("HAFTANIN İLK 3 OYUNCUSU", "TOP 3 THIS WEEK"), fontWeight = FontWeight.Black, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            WeeklyPodium(weeklyTop, onLeaderboard)
        }

        item {
            Text(sh("OYUNCU MERKEZİ", "PLAYER HUB"), fontWeight = FontWeight.Black, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Card(onClick = onHub, colors = CardDefaults.cardColors(containerColor = SonHarfSurface.copy(alpha = .94f)), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, SonHarfBlue.copy(alpha = .20f))) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HubShortcut("🎯", sh("Hedefler", "Goals"), Modifier.weight(1f))
                        HubShortcut("🏆", sh("Lig", "League"), Modifier.weight(1f))
                        HubShortcut("🎮", sh("Oyunlarım", "Games"), Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HubShortcut("📰", sh("Haberler", "News"), Modifier.weight(1f))
                        HubShortcut("?", sh("Kurallar", "Rules"), Modifier.weight(1f))
                        HubShortcut("⚙", sh("Ayarlar", "Settings"), Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickHomeCard("🎯", sh("Hedefler", "Goals"), onHub, Modifier.weight(1f))
                QuickHomeCard("👥", sh("Arkadaşlar", "Friends"), { FriendsQuickAccessState.open = true }, Modifier.weight(1f))
                QuickHomeCard("🛍", sh("Mağaza", "Shop"), onShop, Modifier.weight(1f))
            }
        }

        if (goals.isNotEmpty()) {
            item { Text(sh("BU HAFTANIN HEDEFLERİ", "THIS WEEK'S GOALS"), fontWeight = FontWeight.Black, fontSize = 13.sp) }
            items(goals.take(2).size) { index ->
                val g = goals[index]
                val title = if (SonHarfUiState.isEnglish) g.titleEn else g.titleTr
                Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(17.dp)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold, fontSize = 11.sp); Text("${g.progress.coerceAtMost(g.target)}/${g.target}", color = SonHarfMuted, fontSize = 9.sp) }
                        Text("💎 ${g.rewardDiamonds}", color = SonHarfCyan, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun SonHarfLogo() {
    val pulse by rememberInfiniteTransition(label = "brand").animateFloat(.96f, 1.04f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "brandPulse")
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(42.dp).scale(pulse).background(Brush.radialGradient(listOf(SonHarfCyan, SonHarfPurple)), CircleShape), contentAlignment = Alignment.Center) {
            Text("S↻H", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
        Column {
            Text("SON HARF", fontWeight = FontWeight.Black, fontSize = 17.sp, letterSpacing = 1.sp)
            Text(sh("KELİME DÜELLOSU", "WORD DUEL"), color = SonHarfMuted, fontSize = 7.sp, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun AnimatedVipHomeCard(active: Boolean, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "vipHome")
    val glow by transition.animateFloat(.14f, .46f, infiniteRepeatable(tween(1150), RepeatMode.Reverse), label = "vipHomeGlow")
    val crownScale by transition.animateFloat(.96f, 1.06f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "vipHomeCrown")
    val base = if (SonHarfUiState.darkMode) Color(0xFF171006) else Color(0xFFFFFAED)

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.6.dp, SonHarfGold.copy(alpha = .62f + glow / 5f)),
    ) {
        Box(
            Modifier.fillMaxWidth().background(
                Brush.horizontalGradient(
                    listOf(
                        SonHarfGold.copy(alpha = .16f + glow / 3f),
                        base,
                        SonHarfPurple.copy(alpha = .13f),
                    )
                )
            ).padding(16.dp)
        ) {
            Text("✦", color = SonHarfGold.copy(alpha = .50f + glow), fontSize = 17.sp, modifier = Modifier.align(Alignment.TopEnd))
            Text("✧", color = SonHarfGold.copy(alpha = .34f + glow), fontSize = 13.sp, modifier = Modifier.align(Alignment.BottomCenter))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                Box(
                    Modifier.size(64.dp).scale(crownScale).background(
                        Brush.radialGradient(listOf(Color(0xFFFFE6A3), SonHarfGold.copy(alpha = .30f))), CircleShape
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("♛", fontSize = 39.sp, color = Color(0xFF9B6200), fontWeight = FontWeight.Black)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(color = SonHarfGold.copy(alpha = .16f), shape = RoundedCornerShape(99.dp), border = BorderStroke(1.dp, SonHarfGold.copy(alpha = .35f))) {
                        Text("SON HARF VIP  ✦", Modifier.padding(horizontal = 9.dp, vertical = 3.dp), color = SonHarfGold, fontWeight = FontWeight.Black, fontSize = 8.sp, letterSpacing = .8.sp)
                    }
                    Text(if (active) sh("VIP AYRICALIKLARIN AKTİF", "VIP BENEFITS ACTIVE") else sh("VIP'E GEÇ", "GO VIP"), color = SonHarfGold, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Text(sh("Reklamsız • özel oda • premium kozmetik", "No ads • private rooms • premium cosmetics"), color = SonHarfText, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                    Text(sh("Her ay 400 elmas  •  Gelişmiş istatistik", "400 diamonds monthly  •  Advanced stats"), color = SonHarfMuted, fontSize = 8.sp)
                }
                Text("›", color = SonHarfGold, fontSize = 32.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun WeeklyPodium(rows: List<LeaderboardV2Row>, onClick: () -> Unit) {
    if (rows.isEmpty()) {
        Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(20.dp)) {
            Text(sh("Bu hafta sıralama henüz oluşmadı. İlk galibiyetini al!", "Weekly ranking has not formed yet. Get the first win!"), Modifier.fillMaxWidth().padding(18.dp), color = SonHarfMuted, textAlign = TextAlign.Center)
        }
        return
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.Bottom) {
        rows.forEachIndexed { index, row ->
            val medal = when (index) { 0 -> "🥇"; 1 -> "🥈"; else -> "🥉" }
            val podiumGender = when (index) { 0 -> "kadın"; 1 -> "erkek"; else -> "kadın" }
            Card(onClick = onClick, modifier = Modifier.weight(1f).height(if (index == 0) 142.dp else 128.dp), colors = CardDefaults.cardColors(containerColor = if (index == 0) SonHarfGold.copy(alpha = .12f) else SonHarfSurface), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, if (index == 0) SonHarfGold.copy(alpha = .45f) else SonHarfMuted.copy(alpha = .12f))) {
                Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Box(contentAlignment = Alignment.TopCenter) {
                        SocialAvatar(podiumGender, row.displayName, size = if (index == 0) 58.dp else 50.dp, fallbackIndex = index, accent = if (index == 0) SonHarfGold else SonHarfCyan)
                        Text(medal, fontSize = 21.sp, modifier = Modifier.offset(y = (-8).dp, x = 20.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(row.displayName, fontWeight = FontWeight.Black, fontSize = 10.sp, maxLines = 1)
                    Text("${row.wins}W • ${row.winRate.toInt()}%", color = SonHarfCyan, fontSize = 8.sp)
                }
            }
        }
    }
}

@Composable
private fun HubShortcut(icon: String, title: String, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = SonHarfSurface2.copy(alpha = .65f)) {
        Column(Modifier.padding(vertical = 9.dp, horizontal = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 18.sp); Text(title, fontWeight = FontWeight.Bold, fontSize = 8.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun QuickHomeCard(icon: String, title: String, onClick: () -> Unit, modifier: Modifier) {
    Card(onClick = onClick, modifier = modifier.height(88.dp), colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(17.dp)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(icon, fontSize = 24.sp); Text(title, fontWeight = FontWeight.Bold, fontSize = 9.sp, textAlign = TextAlign.Center)
        }
    }
}
