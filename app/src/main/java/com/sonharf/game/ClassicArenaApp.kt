package com.sonharf.game

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.launch

private enum class ClassicScreen { HOME, GAME, SHOP, PROFILE, HUB, LEAGUE }

private val ClassicBg = Color(0xFF071525)
private val ClassicBgDeep = Color(0xFF04101D)
private val ClassicSurface = Color(0xFF0D2033)
private val ClassicSurface2 = Color(0xFF132A40)
private val ClassicBorder = Color(0xFF28445D)
private val ClassicGold = Color(0xFFD8AD62)
private val ClassicGoldSoft = Color(0xFFF0D49A)
private val ClassicCream = Color(0xFFF4EEE2)
private val ClassicText = Color(0xFFF4F6F8)
private val ClassicMuted = Color(0xFFA9B6C3)
private val ClassicGreen = Color(0xFF7DA887)
private val ClassicBlue = Color(0xFF84AFCB)

/**
 * Calm premium shell for the middle-age+ audience.
 * Existing gameplay, auth, backend, profile, shop, league, social and overlay systems are reused.
 */
@Composable
fun ClassicArenaApp() {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var screen by remember { mutableStateOf(ClassicScreen.HOME) }
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
            screen = ClassicScreen.GAME
        }
    }

    if (!authChecked) {
        Box(Modifier.fillMaxSize().background(ClassicBg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ClassicGold)
        }
        return
    }
    if (!authenticated) {
        RequiredAuthGate { authenticated = true; screen = ClassicScreen.HOME }
        return
    }

    Scaffold(
        containerColor = ClassicBg,
        bottomBar = {
            if (screen !in setOf(ClassicScreen.GAME, ClassicScreen.LEAGUE)) {
                ClassicBottomBar(screen) { next ->
                    SonHarfSoundFx.tap()
                    if (next == ClassicScreen.GAME) gameKey += 1
                    screen = next
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(ClassicBg, ClassicBgDeep))).padding(padding)) {
            when (screen) {
                ClassicScreen.HOME -> ClassicHome(
                    backend = backend,
                    onPlay = { gameKey += 1; screen = ClassicScreen.GAME },
                    onShop = { screen = ClassicScreen.SHOP },
                    onProfile = { screen = ClassicScreen.PROFILE },
                    onHub = { screen = ClassicScreen.HUB },
                    onLeague = { screen = ClassicScreen.LEAGUE },
                )
                ClassicScreen.GAME -> key(gameKey) { TargetNeonGameScreen() }
                ClassicScreen.SHOP -> EconomyShopScreen()
                ClassicScreen.PROFILE -> ProfileExperienceScreen()
                ClassicScreen.HUB -> MetaHubScreen()
                ClassicScreen.LEAGUE -> LeaderboardExperienceScreen { screen = ClassicScreen.HOME }
            }

            if (screen == ClassicScreen.GAME) {
                if (SonHarfGameModeState.mode == "expert") ExpertArenaOverlay() else SketchGameOverlayV9()
                ComboOverlayV9()
            }
        }
    }
}

@Composable
private fun ClassicBottomBar(current: ClassicScreen, onGo: (ClassicScreen) -> Unit) {
    Surface(color = ClassicBgDeep, shadowElevation = 12.dp) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            ClassicNav("⌂", sh("Ana Sayfa", "Home"), current == ClassicScreen.HOME, Modifier.weight(1f)) { onGo(ClassicScreen.HOME) }
            ClassicNav("▶", sh("Oyna", "Play"), current == ClassicScreen.GAME, Modifier.weight(1f)) { onGo(ClassicScreen.GAME) }
            ClassicNav("♛", sh("Arena", "Arena"), current == ClassicScreen.LEAGUE, Modifier.weight(1f)) { onGo(ClassicScreen.LEAGUE) }
            ClassicNav("◉", sh("Sosyal", "Social"), current == ClassicScreen.HUB, Modifier.weight(1f)) { onGo(ClassicScreen.HUB) }
            ClassicNav("♙", sh("Profil", "Profile"), current == ClassicScreen.PROFILE, Modifier.weight(1f)) { onGo(ClassicScreen.PROFILE) }
        }
    }
}

@Composable
private fun ClassicNav(icon: String, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(icon, color = if (selected) ClassicGoldSoft else ClassicMuted, fontSize = 20.sp)
        Text(label, color = if (selected) ClassicText else ClassicMuted, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun ClassicHome(
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
        val uid = backend?.currentUserId()
        if (uid != null) {
            profile = runCatching { backend.getProfile(uid) }.getOrNull()
            social = runCatching { backend.getSocialProfile(uid) }.getOrNull()
        }
        growth = runCatching { backend?.getGrowthDashboard() }.getOrNull()
        runCatching { backend?.getPreferredGameMode() }.getOrNull()?.let {
            mode = it
            SonHarfGameModeState.mode = it
        }
        runCatching { backend?.logEvent("home_open_classic_premium") }
    }

    LaunchedEffect(Unit) { reload() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ClassicHeader(profile, social, growth, onProfile) }
        item { ClassicHero(mode, onModeChange = { next ->
            mode = next
            SonHarfGameModeState.mode = next
            scope.launch { runCatching { backend?.setPreferredGameMode(next) } }
        }, onPlay = onPlay) }
        item { ClassicQuickMenu(onLeague, onHub, onShop) }
        item { ClassicDailyReward(growth, rewardMessage) {
            val current = growth ?: return@ClassicDailyReward
            if (current.dailyClaimed) {
                rewardMessage = sh("Bugünün ödülünü aldınız.", "Today's reward is already claimed.")
            } else {
                scope.launch {
                    val reward = runCatching { backend?.claimDailyCheckin() ?: 0 }.getOrDefault(0)
                    rewardMessage = if (reward > 0) "+$reward ◆" else sh("Ödül alınamadı.", "Reward unavailable.")
                    reload()
                }
            }
        } }
        item { ClassicProgressCards(growth, onLeague, onHub) }
        item { ClassicStats(growth) }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun ClassicHeader(profile: ProfileDto?, social: SocialProfileDto?, growth: GrowthDashboardDto?, onProfile: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            onClick = onProfile,
            shape = CircleShape,
            color = ClassicSurface2,
            border = BorderStroke(1.dp, ClassicGold.copy(alpha = .8f)),
            modifier = Modifier.size(52.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text((profile?.displayName ?: "S").take(1).uppercase(), color = ClassicCream, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(profile?.displayName ?: sh("Son Harf Oyuncusu", "Son Harf Player"), color = ClassicText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text("${sh("Seviye", "Level")} ${growth?.level ?: 1}${if (social?.presenceStatus == "online") "  •  ${sh("Çevrimiçi", "Online")}" else ""}", color = ClassicMuted, fontSize = 12.sp)
            Spacer(Modifier.height(5.dp))
            LinearProgressIndicator(
                progress = { growth?.let { it.levelProgress.toFloat() / it.levelTarget.coerceAtLeast(1) }?.coerceIn(0f, 1f) ?: 0f },
                modifier = Modifier.width(130.dp).height(5.dp).clip(CircleShape),
                color = ClassicGold,
                trackColor = ClassicSurface2,
            )
        }
        ClassicCurrency("●", "${profile?.diamonds ?: 0}")
    }
}

@Composable
private fun ClassicCurrency(icon: String, value: String) {
    Surface(shape = RoundedCornerShape(14.dp), color = ClassicSurface, border = BorderStroke(1.dp, ClassicBorder)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, color = ClassicGold, fontSize = 10.sp)
            Spacer(Modifier.width(5.dp))
            Text(value, color = ClassicText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ClassicHero(mode: String, onModeChange: (String) -> Unit, onPlay: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = ClassicSurface), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, ClassicBorder)) {
        Column(
            Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF10253A), Color(0xFF0C1B2C)))).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("♛", color = ClassicGoldSoft, fontSize = 22.sp)
                    Text("SON HARF", color = ClassicCream, fontSize = 34.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Serif, letterSpacing = 1.sp)
                    Text(sh("Canlı Kelime Arenası", "Live Word Arena"), color = ClassicGoldSoft, fontSize = 13.sp, letterSpacing = 1.sp)
                    Spacer(Modifier.height(9.dp))
                    Text(sh("Kelimenin son harfiyle devam edin, serinizi koruyun ve rakibinizi geçin.", "Continue with the last letter, protect your streak and beat your rival."), color = ClassicMuted, fontSize = 13.sp, lineHeight = 19.sp)
                }
                Surface(shape = CircleShape, color = ClassicGold.copy(alpha = .12f), border = BorderStroke(1.dp, ClassicGold.copy(alpha = .6f)), modifier = Modifier.size(76.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text("S•H", color = ClassicGoldSoft, fontFamily = FontFamily.Serif, fontSize = 23.sp, fontWeight = FontWeight.Bold) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ClassicModeChip(sh("NORMAL", "NORMAL"), mode == "normal") { onModeChange("normal") }
                ClassicModeChip(sh("UZMAN", "EXPERT"), mode == "expert") { onModeChange("expert") }
            }
            Button(
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ClassicGold, contentColor = Color(0xFF241A0A)),
            ) {
                Text(sh("HEMEN OYNA", "PLAY NOW"), fontSize = 16.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(10.dp))
                Text("›", fontSize = 22.sp)
            }
        }
    }
}

@Composable
private fun ClassicModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) ClassicGold.copy(alpha = .16f) else ClassicBgDeep,
        border = BorderStroke(1.dp, if (selected) ClassicGold else ClassicBorder),
    ) {
        Text(label, Modifier.padding(horizontal = 16.dp, vertical = 9.dp), color = if (selected) ClassicGoldSoft else ClassicMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ClassicQuickMenu(onLeague: () -> Unit, onHub: () -> Unit, onShop: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ClassicMenu("◎", sh("Görevler", "Goals"), Modifier.weight(1f), onHub)
        ClassicMenu("▣", sh("Günlük Ödül", "Daily"), Modifier.weight(1f), onHub)
        ClassicMenu("♛", sh("Ligler", "Leagues"), Modifier.weight(1f), onLeague)
        ClassicMenu("◇", sh("Mağaza", "Shop"), Modifier.weight(1f), onShop)
        ClassicMenu("♙", sh("Dolabım", "Locker"), Modifier.weight(1f), onShop)
    }
}

@Composable
private fun ClassicMenu(icon: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(15.dp)).background(ClassicSurface).clickable(onClick = onClick).padding(vertical = 11.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(icon, color = ClassicGoldSoft, fontSize = 21.sp)
        Text(label, color = ClassicText, fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
private fun ClassicDailyReward(growth: GrowthDashboardDto?, message: String, onClaim: () -> Unit) {
    Card(
        onClick = onClaim,
        colors = CardDefaults.cardColors(containerColor = ClassicSurface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, ClassicBorder),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(sh("GÜNLÜK SERİ", "DAILY STREAK"), color = ClassicGoldSoft, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(sh("Her gün oynayın, ödülleri kaçırmayın.", "Play daily and keep your rewards."), color = ClassicMuted, fontSize = 11.sp)
                }
                Text(if (growth?.dailyClaimed == true) "✓" else "🎁", fontSize = 28.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                val rewards = listOf("100", "150", "200", "◆5", "300", "◆10", "500")
                rewards.forEachIndexed { index, reward ->
                    val active = index == 2
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (active) ClassicGold.copy(alpha = .14f) else ClassicBgDeep).padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("${sh("G", "D")}${index + 1}", color = if (active) ClassicGoldSoft else ClassicMuted, fontSize = 8.sp)
                        Text(reward, color = ClassicText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (message.isNotBlank()) Text(message, color = ClassicGoldSoft, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ClassicProgressCards(growth: GrowthDashboardDto?, onLeague: () -> Unit, onHub: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(
            onClick = onHub,
            colors = CardDefaults.cardColors(containerColor = ClassicSurface),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, ClassicBorder),
            modifier = Modifier.weight(1f),
        ) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(sh("SEZON 12", "SEASON 12"), color = ClassicCream, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(sh("Arena Şampiyonası", "Arena Championship"), color = ClassicGoldSoft, fontSize = 11.sp)
                Text("♛", color = ClassicGold, fontSize = 31.sp)
                LinearProgressIndicator(progress = { .72f }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape), color = ClassicGold, trackColor = ClassicBgDeep)
                Text("7.250 / 10.000", color = ClassicMuted, fontSize = 10.sp)
            }
        }
        Card(
            onClick = onLeague,
            colors = CardDefaults.cardColors(containerColor = ClassicSurface),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, ClassicBorder),
            modifier = Modifier.weight(1f),
        ) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(sh("LİGİN", "YOUR LEAGUE"), color = ClassicGoldSoft, fontSize = 11.sp)
                Text(growth?.leagueName ?: sh("ALTIN I", "GOLD I"), color = ClassicCream, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("♜", color = ClassicGold, fontSize = 31.sp)
                LinearProgressIndicator(progress = { .71f }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape), color = ClassicGold, trackColor = ClassicBgDeep)
                Text(sh("Sıralamanı yükselt", "Climb your ranking"), color = ClassicMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun ClassicStats(growth: GrowthDashboardDto?) {
    Card(colors = CardDefaults.cardColors(containerColor = ClassicSurface), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, ClassicBorder)) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(sh("ARENA İSTATİSTİKLERİ", "ARENA STATISTICS"), color = ClassicCream, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ClassicStat(sh("Maçlar", "Matches"), "${growth?.totalMatches ?: 0}")
                ClassicStat(sh("Kazanma", "Win Rate"), growth?.let { if (it.totalMatches > 0) "%${(it.wins * 100 / it.totalMatches)}" else "%0" } ?: "%0")
                ClassicStat(sh("En Uzun Seri", "Best Streak"), "${growth?.bestStreak ?: 0}")
                ClassicStat(sh("Toplam XP", "Total XP"), "${growth?.xp ?: 0}")
            }
        }
    }
}

@Composable
private fun ClassicStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = ClassicMuted, fontSize = 9.sp)
        Text(value, color = ClassicText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
