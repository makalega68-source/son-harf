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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SupabaseProvider
import kotlinx.coroutines.launch

private val NeonInk = Color(0xFF050816)
private val NeonPanel = Color(0xFF0B1024)
private val NeonPanel2 = Color(0xFF111936)
private val NeonCyan = Color(0xFF00E5FF)
private val NeonViolet = Color(0xFF7B2FFF)
private val NeonPink = Color(0xFFFF4D8D)
private val NeonGold = Color(0xFFFFC107)
private val NeonBlue = Color(0xFF178BFF)
private val NeonGreen = Color(0xFF41E38A)

/**
 * Visual refresh root. It keeps the existing production backend/game screens intact,
 * while replacing the shell and home experience with the livelier neon direction.
 */
@Composable
fun NeonSonHarfApp() {
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
    LaunchedEffect(lobbyRequest) {
        if (lobbyRequest > 0 && authenticated) {
            gameKey += 1
            screen = AppScreen.GAME
        }
    }

    if (!authChecked) {
        Box(Modifier.fillMaxSize().background(NeonInk), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = NeonCyan)
        }
        return
    }
    if (!authenticated) {
        RequiredAuthGate { authenticated = true; screen = AppScreen.HOME }
        return
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF070A1A), Color(0xFF060918), Color(0xFF030510))
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (screen != AppScreen.LEADERBOARD) {
                    NeonBottomBar(screen) { screen = it }
                }
            },
        ) { pad ->
            Box(Modifier.fillMaxSize().padding(pad)) {
                when (screen) {
                    AppScreen.HOME -> NeonHomeScreen(
                        backend = backend,
                        onPlay = { screen = AppScreen.GAME },
                        onShop = { screen = AppScreen.SHOP },
                        onProfile = { screen = AppScreen.PROFILE },
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
private fun NeonBottomBar(screen: AppScreen, onChange: (AppScreen) -> Unit) {
    NavigationBar(
        containerColor = Color(0xFF070B18).copy(alpha = .98f),
        tonalElevation = 0.dp,
    ) {
        listOf(
            Triple(AppScreen.HOME, "⌂", sh("Ana Sayfa", "Home")),
            Triple(AppScreen.GAME, "⚔", sh("Oyna", "Play")),
            Triple(AppScreen.SHOP, "◇", sh("Mağaza", "Shop")),
            Triple(AppScreen.PROFILE, "♙", sh("Profil", "Profile")),
            Triple(AppScreen.MORE, "◎", sh("Merkez", "Hub")),
        ).forEach { (target, icon, label) ->
            val selected = screen == target
            NavigationBarItem(
                selected = selected,
                onClick = { SonHarfSoundFx.tap(); onChange(target) },
                icon = {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) NeonViolet.copy(alpha = .22f) else Color.Transparent,
                        border = if (selected) BorderStroke(1.dp, NeonCyan.copy(alpha = .35f)) else null,
                    ) {
                        Text(
                            icon,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            color = if (selected) NeonCyan else Color(0xFF8290AA),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                },
                label = { Text(label, fontSize = 9.sp) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent,
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color(0xFF8290AA),
                )
            )
        }
    }
}

@Composable
private fun NeonHomeScreen(
    backend: OnlineGameBackend?,
    onPlay: () -> Unit,
    onShop: () -> Unit,
    onProfile: () -> Unit,
    onHub: () -> Unit,
    onLeaderboard: () -> Unit,
    onVip: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var mode by remember { mutableStateOf(SonHarfGameModeState.mode) }

    LaunchedEffect(Unit) {
        val id = backend?.currentUserId()
        if (id != null) profile = runCatching { backend.getProfile(id) }.getOrNull()
        val remoteMode = runCatching { backend?.getPreferredGameMode() }.getOrNull()
        if (remoteMode != null) {
            mode = remoteMode
            SonHarfGameModeState.mode = remoteMode
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NeonPlayerAvatar(profile?.displayName ?: sh("Oyuncu", "Player"))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            profile?.displayName ?: sh("Oyuncu", "Player"),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                        )
                        Text(
                            if (profile?.isVip == true) "★ VIP" else sh("Düelloya hazır", "Ready to duel"),
                            color = if (profile?.isVip == true) NeonGold else Color(0xFFA3B0C8),
                            fontSize = 10.sp,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    NeonCurrency("🏆", "${profile?.wins ?: 0}", NeonGold)
                    NeonCurrency("💎", "${profile?.diamonds ?: 0}", NeonCyan)
                }
            }
        }

        item { NeonBrandHero() }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { SonHarfSoundFx.tap(); onPlay() },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGold, contentColor = Color(0xFF211500)),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(sh("HEMEN OYNA", "PLAY NOW"), fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text(sh("Rastgele rakip", "Random opponent"), fontSize = 9.sp)
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NeonActionButton(
                        title = sh("ARKADAŞINLA OYNA", "PLAY WITH FRIEND"),
                        subtitle = sh("Davet gönder", "Send invite"),
                        accent = NeonViolet,
                        modifier = Modifier.weight(1f),
                        onClick = onPlay,
                    )
                    NeonActionButton(
                        title = sh("ODA KUR", "CREATE ROOM"),
                        subtitle = sh("Özel eşleşme", "Private match"),
                        accent = NeonBlue,
                        modifier = Modifier.weight(1f),
                        onClick = onPlay,
                    )
                }
            }
        }

        item {
            NeonSectionCard(title = sh("OYUN MODU", "GAME MODE")) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NeonModeChip(
                        selected = mode == "normal",
                        label = sh("NORMAL", "NORMAL"),
                        accent = NeonCyan,
                        modifier = Modifier.weight(1f),
                    ) {
                        mode = "normal"
                        SonHarfGameModeState.mode = "normal"
                        scope.launch { runCatching { backend?.setPreferredGameMode("normal") } }
                    }
                    NeonModeChip(
                        selected = mode == "expert",
                        label = sh("UZMAN", "EXPERT"),
                        accent = NeonPink,
                        modifier = Modifier.weight(1f),
                    ) {
                        mode = "expert"
                        SonHarfGameModeState.mode = "expert"
                        scope.launch { runCatching { backend?.setPreferredGameMode("expert") } }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (mode == "expert") sh("Daha hızlı, daha riskli, daha yüksek ödüllü.", "Faster, riskier and more rewarding.")
                    else sh("Klasik Son Harf düellosu. 3 tur, temiz rekabet.", "Classic Son Harf duel. 3 rounds, clean competition."),
                    color = Color(0xFFA4B2CC),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            NeonSectionCard(title = sh("HIZLI ERİŞİM", "QUICK ACCESS")) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NeonShortcut("🏆", sh("Sıralama", "Ranking"), NeonGold, onLeaderboard, Modifier.weight(1f))
                    NeonShortcut("🛍", sh("Mağaza", "Shop"), NeonCyan, onShop, Modifier.weight(1f))
                    NeonShortcut("♙", sh("Profil", "Profile"), NeonPink, onProfile, Modifier.weight(1f))
                    NeonShortcut("◎", sh("Merkez", "Hub"), NeonViolet, onHub, Modifier.weight(1f))
                }
            }
        }

        item {
            Card(
                onClick = onVip,
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, NeonGold.copy(alpha = .55f)),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(NeonViolet.copy(alpha = .22f), NeonGold.copy(alpha = .16f), NeonPanel)))
                        .padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("♛", color = NeonGold, fontSize = 28.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                if (profile?.isVip == true) sh("VIP AKTİF", "VIP ACTIVE") else sh("VIP ÜYELİK", "VIP MEMBERSHIP"),
                                color = NeonGold,
                                fontWeight = FontWeight.Black,
                            )
                            Text(sh("Özel oda • kozmetik • bonuslar", "Private rooms • cosmetics • bonuses"), color = Color(0xFFA4B2CC), fontSize = 9.sp)
                        }
                    }
                    Text("›", color = NeonGold, fontSize = 26.sp)
                }
            }
        }

        item { Spacer(Modifier.height(6.dp)) }
    }
}

@Composable
private fun NeonBrandHero() {
    val transition = rememberInfiniteTransition(label = "neonHero")
    val pulse by transition.animateFloat(.97f, 1.035f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "pulse")

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(30.dp),
        border = BorderStroke(1.dp, NeonViolet.copy(alpha = .55f)),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.radialGradient(
                        listOf(NeonViolet.copy(alpha = .34f), NeonCyan.copy(alpha = .09f), NeonPanel)
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SON", color = NeonGold, fontSize = 44.sp, fontWeight = FontWeight.Black, modifier = Modifier.scale(pulse))
                Text("HARF", color = NeonCyan, fontSize = 44.sp, fontWeight = FontWeight.Black, modifier = Modifier.scale(pulse))
                Spacer(Modifier.height(2.dp))
                Text(sh("NEON KELİME DÜELLOSU", "NEON WORD DUEL"), color = Color.White, fontSize = 10.sp, letterSpacing = 1.6.sp)
                Spacer(Modifier.height(16.dp))
                Surface(
                    color = NeonInk.copy(alpha = .52f),
                    shape = RoundedCornerShape(99.dp),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = .30f)),
                ) {
                    Text(
                        sh("SON HARFİ YAKALA • SERİYİ BOZMA", "CATCH THE LAST LETTER • KEEP THE STREAK"),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        color = Color(0xFFB8C6DD),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun NeonPlayerAvatar(name: String) {
    Box(
        Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(Brush.sweepGradient(listOf(NeonCyan, NeonViolet, NeonPink, NeonGold, NeonCyan)))
            .padding(3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.fillMaxSize().clip(CircleShape).background(NeonPanel2), contentAlignment = Alignment.Center) {
            Text(name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 19.sp)
        }
    }
}

@Composable
private fun NeonCurrency(icon: String, value: String, accent: Color) {
    Surface(
        color = accent.copy(alpha = .10f),
        shape = RoundedCornerShape(99.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = .28f)),
    ) {
        Text("$icon $value", Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun NeonActionButton(title: String, subtitle: String, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = { SonHarfSoundFx.tap(); onClick() },
        modifier = modifier.height(66.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = .65f)),
        colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = .18f), contentColor = Color.White),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontWeight = FontWeight.Black, fontSize = 10.sp, textAlign = TextAlign.Center)
            Text(subtitle, color = Color(0xFFABB7CC), fontSize = 8.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun NeonSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NeonPanel.copy(alpha = .94f)),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .08f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun NeonModeChip(selected: Boolean, label: String, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (selected) accent else Color.White.copy(alpha = .08f)),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) accent.copy(alpha = .20f) else NeonPanel2,
            contentColor = if (selected) accent else Color(0xFFA4B2CC),
        ),
    ) {
        Text(label, fontWeight = FontWeight.Black, fontSize = 11.sp)
    }
}

@Composable
private fun NeonShortcut(icon: String, label: String, accent: Color, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        onClick = { SonHarfSoundFx.tap(); onClick() },
        modifier = modifier,
        color = accent.copy(alpha = .10f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = .28f)),
    ) {
        Column(Modifier.padding(vertical = 12.dp, horizontal = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 20.sp)
            Spacer(Modifier.height(4.dp))
            Text(label, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}
