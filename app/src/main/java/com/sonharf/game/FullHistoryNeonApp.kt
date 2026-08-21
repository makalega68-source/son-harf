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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.launch

private enum class FullNeonScreen { HOME, GAME, SHOP, PROFILE, HUB, LEAGUE }

private val FullBg = Color(0xFF030613)
private val FullPanel = Color(0xFF081127)
private val FullPanel2 = Color(0xFF0E1732)
private val FullCyan = Color(0xFF00E9FF)
private val FullPurple = Color(0xFF7A35FF)
private val FullPink = Color(0xFFFF3FCF)
private val FullGold = Color(0xFFFFB817)
private val FullText = Color(0xFFF7F8FF)
private val FullMuted = Color(0xFF8D98B8)

/**
 * Production shell for the approved neon mockup.
 *
 * This intentionally reuses the mature production screens and backend flows instead
 * of the static mock screens so registration, cosmetics, billing, rewards, social,
 * profile privacy, progression, league, history, chat/trivia and game safeguards stay intact.
 */
@Composable
fun FullHistoryNeonApp() {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var screen by remember { mutableStateOf(FullNeonScreen.HOME) }
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
        runCatching { backend?.getPreferredGameMode() }.getOrNull()?.let { SonHarfGameModeState.mode = it }
        SonHarfCosmetics.apply(runCatching { backend?.getEquippedCosmetics() }.getOrNull())
    }
    LaunchedEffect(lobbyRequest) {
        if (authenticated && lobbyRequest > 0) {
            gameKey += 1
            screen = FullNeonScreen.GAME
        }
    }

    if (!authChecked) {
        Box(Modifier.fillMaxSize().background(FullBg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = FullCyan)
        }
        return
    }
    if (!authenticated) {
        RequiredAuthGate { authenticated = true; screen = FullNeonScreen.HOME }
        return
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF040717), FullBg, Color(0xFF02040D)))
        )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (screen !in setOf(FullNeonScreen.GAME, FullNeonScreen.LEAGUE)) {
                    FullNeonBottomBar(screen) { next ->
                        SonHarfSoundFx.tap()
                        screen = next
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (screen) {
                    FullNeonScreen.HOME -> FullNeonHome(
                        backend = backend,
                        onPlay = { gameKey += 1; screen = FullNeonScreen.GAME },
                        onPrivate = { gameKey += 1; screen = FullNeonScreen.GAME },
                        onShop = { screen = FullNeonScreen.SHOP },
                        onProfile = { screen = FullNeonScreen.PROFILE },
                        onHub = { screen = FullNeonScreen.HUB },
                        onLeague = { screen = FullNeonScreen.LEAGUE },
                    )
                    FullNeonScreen.GAME -> key(gameKey) { TargetNeonGameScreen() }
                    FullNeonScreen.SHOP -> EconomyShopScreen()
                    FullNeonScreen.PROFILE -> ProfileExperienceScreen()
                    FullNeonScreen.HUB -> MetaHubScreen()
                    FullNeonScreen.LEAGUE -> LeaderboardExperienceScreen { screen = FullNeonScreen.HOME }
                }
            }
        }

        // Mature gameplay overlays keep the historical feedback/result/combo/expert fixes alive.
        if (screen == FullNeonScreen.GAME) {
            if (SonHarfGameModeState.mode == "expert") ExpertArenaOverlay() else SketchGameOverlayV9()
            ComboOverlayV9()
        }
    }
}

@Composable
private fun FullNeonBottomBar(current: FullNeonScreen, onGo: (FullNeonScreen) -> Unit) {
    Surface(color = Color(0xFF050A18).copy(alpha = .99f), shadowElevation = 18.dp) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            listOf(
                Triple(FullNeonScreen.HOME, "⌂", sh("Ana Sayfa", "Home")),
                Triple(FullNeonScreen.GAME, "⚔", sh("Oyna", "Play")),
                Triple(FullNeonScreen.SHOP, "◇", sh("Mağaza", "Shop")),
                Triple(FullNeonScreen.PROFILE, "♙", sh("Profil", "Profile")),
                Triple(FullNeonScreen.HUB, "◎", sh("Merkez", "Hub")),
            ).forEach { (target, icon, label) ->
                val selected = current == target
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable { onGo(target) }.padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        shape = RoundedCornerShape(13.dp),
                        color = if (selected) FullPurple.copy(alpha = .22f) else Color.Transparent,
                        border = if (selected) BorderStroke(1.dp, FullCyan.copy(alpha = .38f)) else null,
                    ) {
                        Text(
                            icon,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp),
                            color = if (selected) FullCyan else FullMuted,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    Text(label, color = if (selected) FullText else FullMuted, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun FullNeonHome(
    backend: OnlineGameBackend?,
    onPlay: () -> Unit,
    onPrivate: () -> Unit,
    onShop: () -> Unit,
    onProfile: () -> Unit,
    onHub: () -> Unit,
    onLeague: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var social by remember { mutableStateOf<SocialProfileDto?>(null) }
    var growth by remember { mutableStateOf<GrowthDashboardDto?>(null) }
    var goals by remember { mutableStateOf<List<GoalRowDto>>(emptyList()) }
    var mode by remember { mutableStateOf(SonHarfGameModeState.mode) }
    var rewardNotice by remember { mutableStateOf("") }

    suspend fun reload() {
        val id = backend?.currentUserId()
        if (id != null) {
            profile = runCatching { backend.getProfile(id) }.getOrNull()
            social = runCatching { backend.getSocialProfile(id) }.getOrNull()
        }
        growth = runCatching { backend?.getGrowthDashboard() }.getOrNull()
        goals = runCatching { backend?.getGoals().orEmpty() }.getOrDefault(emptyList())
        runCatching { backend?.getPreferredGameMode() }.getOrNull()?.let { mode = it; SonHarfGameModeState.mode = it }
        runCatching { backend?.logEvent("home_open_neon") }
    }

    LaunchedEffect(Unit) { reload() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SocialAvatar(
                        gender = social?.gender,
                        name = profile?.displayName ?: sh("Oyuncu", "Player"),
                        size = 48.dp,
                        accent = if (profile?.isVip == true) FullGold else FullCyan,
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(profile?.displayName ?: sh("Oyuncu", "Player"), color = FullText, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text("Lv. ${growth?.level ?: 1}", color = FullMuted, fontSize = 10.sp)
                        Box(Modifier.width(98.dp).height(4.dp).clip(CircleShape).background(Color(0xFF1A2240))) {
                            val ratio = growth?.let { it.levelProgress.toFloat() / it.levelTarget.coerceAtLeast(1) } ?: 0f
                            Box(Modifier.fillMaxHeight().fillMaxWidth(ratio.coerceIn(0f, 1f)).background(FullPurple))
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    FullCurrency("🏆", "${growth?.xp ?: ((profile?.wins ?: 0) * 10)}", FullGold)
                    FullCurrency("◆", "${profile?.diamonds ?: 0}", FullCyan)
                }
            }
        }

        item { ApprovedNeonHero() }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FullHomeAction("⚡", sh("HIZLI OYNA", "QUICK PLAY"), sh("Eşleş & Başla", "Match & start"), FullPink, Modifier.weight(1f), onPlay)
                FullHomeAction("⌂", sh("ÖZEL ODA", "PRIVATE ROOM"), sh("Arkadaşlarınla oyna", "Play with friends"), FullPurple, Modifier.weight(1f), onPrivate)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FullHomeAction("🏆", sh("LİG", "LEAGUE"), sh("Sıranı yükselt", "Climb the ranks"), FullGold, Modifier.weight(1f), onLeague)
                FullHomeAction("🎯", sh("GÖREVLER", "GOALS"), sh("Ödülleri kazan", "Earn rewards"), FullPink, Modifier.weight(1f), onHub)
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, FullCyan.copy(alpha = .52f)),
                onClick = {
                    val d = growth ?: return@Card
                    if (d.dailyClaimed) { rewardNotice = sh("Bugünün ödülünü zaten aldın.", "Today's reward is already claimed."); return@Card }
                    scope.launch {
                        val reward = runCatching { backend?.claimDailyCheckin() ?: 0 }.getOrDefault(0)
                        rewardNotice = if (reward > 0) "+$reward ◆" else sh("Ödül alınamadı.", "Reward could not be claimed.")
                        reload()
                    }
                },
            ) {
                Row(
                    Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(FullPanel, Color(0xFF10172D), Color(0xFF29111C)))).padding(15.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(sh("GÜNLÜK ÖDÜL", "DAILY REWARD"), color = FullCyan, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Text(
                            if (growth?.dailyClaimed == true) sh("Bugünün ödülü alındı ✓", "Today's reward claimed ✓") else sh("Dokun ve günlük ödülünü al", "Tap to claim today's reward"),
                            color = FullMuted,
                            fontSize = 10.sp,
                        )
                        if (rewardNotice.isNotBlank()) Text(rewardNotice, color = FullGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(if (growth?.dailyClaimed == true) "✓" else "🧰", fontSize = 40.sp, color = FullGold)
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = FullPanel), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, FullPurple.copy(alpha = .35f))) {
                Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(sh("OYUN MODU", "GAME MODE"), color = FullText, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            Text(if (mode == "expert") sh("Uzman: 15/15/15 • artan çarpan", "Expert: 15/15/15 • rising multiplier") else sh("Normal: klasik 3 round", "Normal: classic 3 rounds"), color = FullMuted, fontSize = 9.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(selected = mode == "normal", onClick = { mode = "normal"; SonHarfGameModeState.mode = "normal"; scope.launch { runCatching { backend?.setPreferredGameMode("normal") } } }, label = { Text("NORMAL", fontSize = 9.sp) })
                            FilterChip(selected = mode == "expert", onClick = { mode = "expert"; SonHarfGameModeState.mode = "expert"; scope.launch { runCatching { backend?.setPreferredGameMode("expert") } } }, label = { Text(sh("UZMAN", "EXPERT"), fontSize = 9.sp) })
                        }
                    }
                }
            }
        }

        if (growth != null) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FullMetric("🏆", "${growth!!.wins}", sh("Zafer", "Wins"), Modifier.weight(1f))
                    FullMetric("🔥", "${growth!!.currentWinStreak}", sh("Seri", "Streak"), Modifier.weight(1f))
                    FullMetric("🧠", "${growth!!.validWords}", sh("Kelime", "Words"), Modifier.weight(1f))
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FullShortcut("👥", sh("Arkadaşlar", "Friends"), FullCyan, Modifier.weight(1f)) { FriendsQuickAccessState.open = true }
                FullShortcut("🛒", sh("Mağaza", "Shop"), FullPink, Modifier.weight(1f), onShop)
                FullShortcut("♙", sh("Profil", "Profile"), FullPurple, Modifier.weight(1f), onProfile)
                FullShortcut("◎", sh("Merkez", "Hub"), FullGold, Modifier.weight(1f), onHub)
            }
        }

        if (goals.isNotEmpty()) {
            item { Text(sh("BU HAFTANIN HEDEFLERİ", "THIS WEEK'S GOALS"), color = FullText, fontWeight = FontWeight.Black, fontSize = 13.sp) }
            items(goals.take(2).size) { index ->
                val goal = goals[index]
                val title = if (SonHarfUiState.isEnglish) goal.titleEn else goal.titleTr
                Card(colors = CardDefaults.cardColors(containerColor = FullPanel), shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(title, color = FullText, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Spacer(Modifier.height(5.dp))
                            LinearProgressIndicator(
                                progress = { goal.progress.toFloat() / goal.target.coerceAtLeast(1) },
                                modifier = Modifier.fillMaxWidth(),
                                color = FullPurple,
                                trackColor = Color(0xFF202943),
                            )
                            Text("${goal.progress.coerceAtMost(goal.target)}/${goal.target}", color = FullMuted, fontSize = 9.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("◆ ${goal.rewardDiamonds}", color = FullCyan, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(4.dp)) }
    }
}

@Composable
private fun ApprovedNeonHero() {
    val transition = rememberInfiniteTransition(label = "approvedLogo")
    val pulse by transition.animateFloat(.98f, 1.025f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "logoPulse")
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, FullPurple.copy(alpha = .55f)),
    ) {
        Box(
            Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 250.dp).background(
                Brush.radialGradient(listOf(FullPurple.copy(alpha = .31f), Color(0xFF07122C), FullBg))
            ),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(205.dp).scale(pulse), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val stroke = w * .045f
                    drawOval(
                        brush = Brush.linearGradient(listOf(FullPink, FullPurple, FullCyan)),
                        topLeft = Offset(w * .10f, h * .31f),
                        size = Size(w * .51f, h * .42f),
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                    drawOval(
                        brush = Brush.linearGradient(listOf(FullCyan, Color(0xFF288CFF), FullPurple)),
                        topLeft = Offset(w * .40f, h * .31f),
                        size = Size(w * .51f, h * .42f),
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                    // crown matching the approved concept
                    val y = h * .20f
                    drawLine(FullGold, Offset(w*.36f,y+h*.05f), Offset(w*.43f,y-h*.055f), strokeWidth=stroke*.62f, cap=StrokeCap.Round)
                    drawLine(FullGold, Offset(w*.43f,y-h*.055f), Offset(w*.50f,y+h*.02f), strokeWidth=stroke*.62f, cap=StrokeCap.Round)
                    drawLine(FullGold, Offset(w*.50f,y+h*.02f), Offset(w*.58f,y-h*.07f), strokeWidth=stroke*.62f, cap=StrokeCap.Round)
                    drawLine(FullGold, Offset(w*.58f,y-h*.07f), Offset(w*.65f,y+h*.05f), strokeWidth=stroke*.62f, cap=StrokeCap.Round)
                    drawLine(FullGold, Offset(w*.36f,y+h*.05f), Offset(w*.65f,y+h*.05f), strokeWidth=stroke*.62f, cap=StrokeCap.Round)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset(y = 18.dp)) {
                    Text("SON", color = FullPink, fontSize = 36.sp, fontWeight = FontWeight.Black, lineHeight = 34.sp)
                    Text("HARF", color = FullCyan, fontSize = 36.sp, fontWeight = FontWeight.Black, lineHeight = 34.sp)
                }
            }
        }
    }
}

@Composable
private fun FullHomeAction(icon: String, title: String, subtitle: String, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.heightIn(min = 88.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = .72f)),
    ) {
        Row(
            Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(accent.copy(alpha = .14f), FullPanel))).padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(icon, fontSize = 27.sp, color = accent)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = FullText, fontWeight = FontWeight.Black, fontSize = 12.sp, maxLines = 1)
                Text(subtitle, color = FullMuted, fontSize = 9.sp, maxLines = 2)
            }
        }
    }
}

@Composable
private fun FullCurrency(icon: String, value: String, accent: Color) {
    Surface(color = FullPanel, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, accent.copy(alpha = .25f))) {
        Row(Modifier.padding(horizontal = 9.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 11.sp)
            Spacer(Modifier.width(4.dp))
            Text(value, color = accent, fontWeight = FontWeight.Black, fontSize = 11.sp)
        }
    }
}

@Composable
private fun FullMetric(icon: String, value: String, label: String, modifier: Modifier) {
    Surface(modifier, color = FullPanel, shape = RoundedCornerShape(15.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = .07f))) {
        Column(Modifier.padding(vertical = 11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 20.sp)
            Text(value, color = FullText, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text(label, color = FullMuted, fontSize = 8.sp)
        }
    }
}

@Composable
private fun FullShortcut(icon: String, label: String, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick),
        color = FullPanel,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = .20f)),
    ) {
        Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 20.sp)
            Text(label, color = FullMuted, fontSize = 8.sp, textAlign = TextAlign.Center)
        }
    }
}
