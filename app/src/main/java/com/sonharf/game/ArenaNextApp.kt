package com.sonharf.game

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.GrowthDashboardDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SocialProfileDto
import com.sonharf.game.data.SupabaseProvider
import kotlinx.coroutines.launch

private enum class ArenaNextScreen { HOME, GAME, SHOP, PROFILE, HUB, LEAGUE }

private val ArenaInk = Color(0xFF05060B)
private val ArenaInk2 = Color(0xFF0A0D18)
private val ArenaPanel = Color(0xFF101526)
private val ArenaPanelHi = Color(0xFF171E34)
private val ArenaLime = Color(0xFFC8FF35)
private val ArenaViolet = Color(0xFF8657FF)
private val ArenaCyan = Color(0xFF29E7FF)
private val ArenaCoral = Color(0xFFFF5D73)
private val ArenaGold = Color(0xFFFFC857)
private val ArenaWhite = Color(0xFFF8FAFF)
private val ArenaMuted = Color(0xFF98A2BA)

/**
 * Arena Next is the new visual shell for Son Harf.
 *
 * It deliberately keeps the production gameplay, profile, shop, league, social and
 * backend surfaces intact. Only the top-level navigation and home presentation are
 * replaced, so the redesign does not discard existing product capability.
 */
@Composable
fun ArenaNextApp() {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var screen by remember { mutableStateOf(ArenaNextScreen.HOME) }
    var gameKey by remember { mutableIntStateOf(0) }
    var authChecked by remember { mutableStateOf(false) }
    var authenticated by remember { mutableStateOf(false) }
    val lobbyRequest = SonHarfGameNavigation.lobbyRequest

    LaunchedEffect(Unit) {
        authenticated = SupabaseProvider.configured && hasVerifiedMembershipSession()
        authChecked = true
    }
    LaunchedEffect(authenticated) {
        if (!authenticated) return@LaunchedEffect
        runCatching { backend?.getPreferredGameMode() }.getOrNull()?.let {
            SonHarfGameModeState.mode = it
        }
        SonHarfCosmetics.apply(runCatching { backend?.getEquippedCosmetics() }.getOrNull())
    }
    LaunchedEffect(lobbyRequest) {
        if (authenticated && lobbyRequest > 0) {
            gameKey += 1
            screen = ArenaNextScreen.GAME
        }
    }

    if (!authChecked) {
        Box(Modifier.fillMaxSize().background(ArenaInk), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ArenaLime)
        }
        return
    }
    if (!authenticated) {
        RequiredAuthGate {
            authenticated = true
            screen = ArenaNextScreen.HOME
        }
        return
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(Color(0xFF0B1020), ArenaInk, Color(0xFF04050A))
            )
        )
    ) {
        ArenaAmbientBackground()
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (screen !in setOf(ArenaNextScreen.GAME, ArenaNextScreen.LEAGUE)) {
                    ArenaBottomBar(screen) { next ->
                        SonHarfSoundFx.tap()
                        if (next == ArenaNextScreen.GAME) gameKey += 1
                        screen = next
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (screen) {
                    ArenaNextScreen.HOME -> ArenaHome(
                        backend = backend,
                        onPlay = { gameKey += 1; screen = ArenaNextScreen.GAME },
                        onShop = { screen = ArenaNextScreen.SHOP },
                        onProfile = { screen = ArenaNextScreen.PROFILE },
                        onHub = { screen = ArenaNextScreen.HUB },
                        onLeague = { screen = ArenaNextScreen.LEAGUE },
                    )
                    ArenaNextScreen.GAME -> key(gameKey) { TargetNeonGameScreen() }
                    ArenaNextScreen.SHOP -> EconomyShopScreen()
                    ArenaNextScreen.PROFILE -> ProfileExperienceScreen()
                    ArenaNextScreen.HUB -> MetaHubScreen()
                    ArenaNextScreen.LEAGUE -> LeaderboardExperienceScreen { screen = ArenaNextScreen.HOME }
                }
            }
        }

        if (screen == ArenaNextScreen.GAME) {
            if (SonHarfGameModeState.mode == "expert") ExpertArenaOverlay() else SketchGameOverlayV9()
            ComboOverlayV9()
        }
    }
}

@Composable
private fun ArenaAmbientBackground() {
    Canvas(Modifier.fillMaxSize()) {
        drawCircle(
            color = ArenaViolet.copy(alpha = .09f),
            radius = size.minDimension * .48f,
            center = Offset(size.width * .92f, size.height * .08f),
        )
        drawCircle(
            color = ArenaCyan.copy(alpha = .06f),
            radius = size.minDimension * .42f,
            center = Offset(size.width * .05f, size.height * .56f),
        )
        val gap = 42.dp.toPx()
        var y = 0f
        while (y < size.height) {
            drawLine(Color.White.copy(alpha = .018f), Offset(0f, y), Offset(size.width, y), 1f)
            y += gap
        }
    }
}

@Composable
private fun ArenaBottomBar(current: ArenaNextScreen, onGo: (ArenaNextScreen) -> Unit) {
    Surface(
        color = ArenaInk2.copy(alpha = .98f),
        tonalElevation = 0.dp,
        shadowElevation = 24.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArenaNavItem("⌂", sh("Ana", "Home"), current == ArenaNextScreen.HOME, Modifier.weight(1f)) { onGo(ArenaNextScreen.HOME) }
            ArenaNavItem("◎", sh("Merkez", "Hub"), current == ArenaNextScreen.HUB, Modifier.weight(1f)) { onGo(ArenaNextScreen.HUB) }
            Box(Modifier.weight(1.15f), contentAlignment = Alignment.Center) {
                Surface(
                    onClick = { onGo(ArenaNextScreen.GAME) },
                    shape = CircleShape,
                    color = ArenaLime,
                    shadowElevation = 12.dp,
                    modifier = Modifier.size(62.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("S↻H", color = ArenaInk, fontWeight = FontWeight.Black, fontSize = 17.sp)
                    }
                }
            }
            ArenaNavItem("◇", sh("Mağaza", "Shop"), current == ArenaNextScreen.SHOP, Modifier.weight(1f)) { onGo(ArenaNextScreen.SHOP) }
            ArenaNavItem("♙", sh("Profil", "Profile"), current == ArenaNextScreen.PROFILE, Modifier.weight(1f)) { onGo(ArenaNextScreen.PROFILE) }
        }
    }
}

@Composable
private fun ArenaNavItem(icon: String, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(icon, color = if (selected) ArenaLime else ArenaMuted, fontSize = 19.sp, fontWeight = FontWeight.Black)
        Text(label, color = if (selected) ArenaWhite else ArenaMuted, fontSize = 9.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun ArenaHome(
    backend: OnlineGameBackend?,
    onPlay: () -> Unit,
    onShop: () -> Unit,
    onProfile: () -> Unit,
    onHub: () -> Unit,
    onLeague: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var social by remember { mutableStateOf<SocialProfileDto?>(null) }
    var growth by remember { mutableStateOf<GrowthDashboardDto?>(null) }
    var mode by remember { mutableStateOf(SonHarfGameModeState.mode) }
    var rewardMessage by remember { mutableStateOf("") }

    suspend fun reload() {
        val id = backend?.currentUserId()
        if (id != null) {
            profile = runCatching { backend.getProfile(id) }.getOrNull()
            social = runCatching { backend.getSocialProfile(id) }.getOrNull()
        }
        growth = runCatching { backend?.getGrowthDashboard() }.getOrNull()
        runCatching { backend?.getPreferredGameMode() }.getOrNull()?.let {
            mode = it
            SonHarfGameModeState.mode = it
        }
        runCatching { backend?.logEvent("home_open_arena_next") }
    }

    LaunchedEffect(Unit) { reload() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ArenaTopBar(profile, social, growth, onProfile)
        }
        item {
            ArenaHero(
                mode = mode,
                onModeChange = { next ->
                    mode = next
                    SonHarfGameModeState.mode = next
                    scope.launch { runCatching { backend?.setPreferredGameMode(next) } }
                },
                onPlay = onPlay,
            )
        }
        item {
            ArenaQuickActions(onLeague, onHub, onShop)
        }
        item {
            ArenaDailyCard(
                growth = growth,
                message = rewardMessage,
                onClaim = {
                    val dashboard = growth ?: return@ArenaDailyCard
                    if (dashboard.dailyClaimed) {
                        rewardMessage = sh("Bugünün ödülü alındı.", "Today's reward is already claimed.")
                    } else {
                        scope.launch {
                            val reward = runCatching { backend?.claimDailyCheckin() ?: 0 }.getOrDefault(0)
                            rewardMessage = if (reward > 0) "+$reward ◆" else sh("Ödül alınamadı.", "Reward unavailable.")
                            reload()
                        }
                    }
                },
            )
        }
        item {
            ArenaStats(growth)
        }
        item {
            ArenaSeasonCard(onHub)
        }
        item { Spacer(Modifier.height(18.dp)) }
    }
}

@Composable
private fun ArenaTopBar(
    profile: ProfileDto?,
    social: SocialProfileDto?,
    growth: GrowthDashboardDto?,
    onProfile: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            onClick = onProfile,
            shape = CircleShape,
            color = ArenaPanelHi,
            border = BorderStroke(2.dp, if (profile?.isVip == true) ArenaGold else ArenaLime),
            modifier = Modifier.size(50.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    (profile?.displayName ?: "S").take(1).uppercase(),
                    color = ArenaWhite,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                profile?.displayName ?: sh("Oyuncu", "Player"),
                color = ArenaWhite,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
            )
            Text(
                if (social?.isOnline == true) sh("● Çevrimiçi", "● Online") else sh("Arena oyuncusu", "Arena player"),
                color = if (social?.isOnline == true) ArenaLime else ArenaMuted,
                fontSize = 10.sp,
            )
        }
        ArenaCurrency("◆", "${profile?.diamonds ?: 0}", ArenaCyan)
        Spacer(Modifier.width(6.dp))
        ArenaCurrency("XP", "${growth?.xp ?: 0}", ArenaGold)
    }
}

@Composable
private fun ArenaCurrency(icon: String, value: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(15.dp),
        color = ArenaPanel,
        border = BorderStroke(1.dp, accent.copy(alpha = .4f)),
    ) {
        Row(Modifier.padding(horizontal = 9.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, color = accent, fontWeight = FontWeight.Black, fontSize = 10.sp)
            Spacer(Modifier.width(4.dp))
            Text(value, color = ArenaWhite, fontWeight = FontWeight.Black, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ArenaHero(mode: String, onModeChange: (String) -> Unit, onPlay: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "arenaHero").animateFloat(
        initialValue = .85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "arenaHeroPulse",
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(30.dp),
        border = BorderStroke(1.dp, ArenaViolet.copy(alpha = .55f)),
        modifier = Modifier.fillMaxWidth().shadow(18.dp, RoundedCornerShape(30.dp)),
    ) {
        Box(
            Modifier.fillMaxWidth().background(
                Brush.linearGradient(listOf(Color(0xFF1B1637), Color(0xFF10172B), Color(0xFF0B1B21)))
            ).padding(20.dp)
        ) {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(ArenaViolet.copy(alpha = .18f * pulse), size.minDimension * .42f, Offset(size.width * .83f, size.height * .18f))
                drawCircle(ArenaLime.copy(alpha = .08f), size.minDimension * .30f, Offset(size.width * .08f, size.height * .88f), style = Stroke(5f))
            }
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Surface(shape = RoundedCornerShape(50), color = ArenaLime.copy(alpha = .13f)) {
                            Text(
                                sh("CANLI KELİME ARENASI", "LIVE WORD ARENA"),
                                Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                color = ArenaLime,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("SON HARF", color = ArenaWhite, fontWeight = FontWeight.Black, fontSize = 34.sp, letterSpacing = 1.sp)
                        Text(
                            sh("Son harfi yakala. Zinciri bozma. Arenayı ele geçir.", "Catch the last letter. Keep the chain alive. Own the arena."),
                            color = ArenaMuted,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                        )
                    }
                    Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.fillMaxSize()) {
                            drawCircle(ArenaLime.copy(alpha = .13f))
                            drawCircle(ArenaLime.copy(alpha = .7f), style = Stroke(3f))
                        }
                        Text("∞\n♛", color = ArenaLime, fontWeight = FontWeight.Black, fontSize = 22.sp, textAlign = TextAlign.Center, lineHeight = 21.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ArenaModeChip(sh("NORMAL", "NORMAL"), mode == "normal") { onModeChange("normal") }
                    ArenaModeChip(sh("UZMAN", "EXPERT"), mode == "expert") { onModeChange("expert") }
                }
                Button(
                    onClick = onPlay,
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ArenaLime, contentColor = ArenaInk),
                ) {
                    Text("⚡  ${sh("RAKİP BUL VE OYNA", "FIND RIVAL & PLAY")}", fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
                Text(
                    sh("Hızlı eşleşme • 3 round • gerçek oyuncular", "Quick match • 3 rounds • real players"),
                    color = ArenaMuted,
                    fontSize = 10.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ArenaModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) ArenaViolet else ArenaPanelHi,
        border = BorderStroke(1.dp, if (selected) ArenaViolet else Color.White.copy(alpha = .08f)),
    ) {
        Text(label, Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = ArenaWhite, fontWeight = FontWeight.Black, fontSize = 10.sp)
    }
}

@Composable
private fun ArenaQuickActions(onLeague: () -> Unit, onHub: () -> Unit, onShop: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        ArenaAction("♛", sh("LİG", "LEAGUE"), sh("Sıralama", "Rankings"), ArenaGold, Modifier.weight(1f), onLeague)
        ArenaAction("◎", sh("GÖREV", "QUESTS"), sh("Ödüller", "Rewards"), ArenaCoral, Modifier.weight(1f), onHub)
        ArenaAction("◇", sh("DOLAP", "LOCKER"), sh("Kozmetik", "Cosmetics"), ArenaCyan, Modifier.weight(1f), onShop)
    }
}

@Composable
private fun ArenaAction(icon: String, title: String, subtitle: String, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = ArenaPanel,
        border = BorderStroke(1.dp, accent.copy(alpha = .28f)),
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(icon, color = accent, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(title, color = ArenaWhite, fontWeight = FontWeight.Black, fontSize = 11.sp)
            Text(subtitle, color = ArenaMuted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun ArenaDailyCard(growth: GrowthDashboardDto?, message: String, onClaim: () -> Unit) {
    Surface(
        onClick = onClaim,
        shape = RoundedCornerShape(24.dp),
        color = ArenaPanel,
        border = BorderStroke(1.dp, ArenaCyan.copy(alpha = .28f)),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(ArenaCyan.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                Text(if (growth?.dailyClaimed == true) "✓" else "✦", color = ArenaCyan, fontSize = 25.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(sh("GÜNLÜK SERİ ÖDÜLÜ", "DAILY STREAK REWARD"), color = ArenaWhite, fontWeight = FontWeight.Black, fontSize = 12.sp)
                Text(
                    if (growth?.dailyClaimed == true) sh("Bugünkü kasa açıldı", "Today's chest opened") else sh("Dokun, ödülünü şimdi al", "Tap to claim your reward"),
                    color = ArenaMuted,
                    fontSize = 10.sp,
                )
                if (message.isNotBlank()) Text(message, color = ArenaLime, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Text(if (growth?.dailyClaimed == true) "ALINDI" else sh("AL", "CLAIM"), color = if (growth?.dailyClaimed == true) ArenaMuted else ArenaLime, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ArenaStats(growth: GrowthDashboardDto?) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(sh("ARENA KARNESİ", "ARENA STATS"), color = ArenaWhite, fontWeight = FontWeight.Black, fontSize = 14.sp)
            Text("LV ${growth?.level ?: 1}", color = ArenaLime, fontWeight = FontWeight.Black, fontSize = 11.sp)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            ArenaStat("♛", "${growth?.wins ?: 0}", sh("Zafer", "Wins"), ArenaGold, Modifier.weight(1f))
            ArenaStat("🔥", "${growth?.currentWinStreak ?: 0}", sh("Seri", "Streak"), ArenaCoral, Modifier.weight(1f))
            ArenaStat("Aa", "${growth?.validWords ?: 0}", sh("Kelime", "Words"), ArenaCyan, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ArenaStat(icon: String, value: String, label: String, accent: Color, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(18.dp), color = ArenaPanel, border = BorderStroke(1.dp, Color.White.copy(alpha = .06f))) {
        Column(Modifier.padding(13.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, color = accent, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text(value, color = ArenaWhite, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text(label, color = ArenaMuted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun ArenaSeasonCard(onHub: () -> Unit) {
    Surface(
        onClick = onHub,
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, ArenaViolet.copy(alpha = .42f)),
    ) {
        Row(
            Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF291A48), Color(0xFF131A2B)))).padding(17.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(sh("SEZON 01 • KELİME TAHTI", "SEASON 01 • WORD THRONE"), color = ArenaGold, fontWeight = FontWeight.Black, fontSize = 11.sp)
                Spacer(Modifier.height(4.dp))
                Text(sh("Görevleri bitir, özel kozmetikleri aç.", "Finish quests and unlock exclusive cosmetics."), color = ArenaWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(7.dp))
                Text(sh("SEZON MERKEZİNE GİT  →", "OPEN SEASON HUB  →"), color = ArenaLime, fontWeight = FontWeight.Black, fontSize = 10.sp)
            }
            Text("♛", color = ArenaGold, fontSize = 46.sp, fontWeight = FontWeight.Black)
        }
    }
}
