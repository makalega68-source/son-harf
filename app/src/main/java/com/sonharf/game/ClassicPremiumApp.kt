package com.sonharf.game

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.GrowthDashboardDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.getAdminDashboard
import com.sonharf.game.data.getLeaderboardV2
import kotlinx.coroutines.launch

private enum class ClassicScreen {
    HOME, PLAY, GAME, BIL_BAKALIM, DAILY_CIPHER, MASTERY, ADMIN, PROFILE, SHOP, HUB, LEAGUE, PROFILE_FULL, SHOP_FULL, HISTORY, MASCOT, MASCOT_ROOM
}

private val ClassicBg = LetharaPalette.Night
private val ClassicBgDeep = LetharaPalette.Night2
private val ClassicPanel = Color(0xFF101D39)
private val ClassicPanel2 = Color(0xFF15284A)
private val ClassicBorder = Color(0xFF29486B)
private val ClassicGold = LetharaPalette.Gold
private val ClassicGoldSoft = Color(0xFFEAB957)
private val ClassicCream = LetharaPalette.Text
private val ClassicText = LetharaPalette.Text
private val ClassicMuted = LetharaPalette.Muted
private val ClassicGreen = LetharaPalette.Green
private val ClassicBlue = LetharaPalette.Cyan
private val ClassicRed = LetharaPalette.Red

/**
 * Premium-casual Son Harf shell designed for a mature audience.
 * The visible home/play/profile/store surfaces follow the approved concept,
 * while existing backend, game, social, profile, shop and league systems remain intact.
 */
@Composable
fun ClassicPremiumApp() {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var screen by remember { mutableStateOf(ClassicScreen.HOME) }
    var gameKey by remember { mutableIntStateOf(0) }
    var autoStartMatchmaking by remember { mutableStateOf(false) }
    var authChecked by remember { mutableStateOf(false) }
    var authenticated by remember { mutableStateOf(false) }
    var hubTab by remember { mutableIntStateOf(0) }
    var hubReturnScreen by remember { mutableStateOf(ClassicScreen.HOME) }
    var profileFullTab by remember { mutableIntStateOf(0) }
    var profileFullReturnScreen by remember { mutableStateOf(ClassicScreen.PROFILE) }
    var shopFullTab by remember { mutableIntStateOf(0) }
    var shopFullReturnScreen by remember { mutableStateOf(ClassicScreen.SHOP) }
    val lobbyRequest = SonHarfGameNavigation.lobbyRequest
    val context = LocalContext.current
    var lastHomeBack by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        authenticated = if (BuildConfig.DEBUG) {
            if (backend != null && backend.currentUserId() == null) {
                runCatching { backend.ensurePlayer(sh("Oyuncu", "Player")) }
            }
            true
        } else {
            SupabaseProvider.configured && hasVerifiedMembershipSession()
        }
        authChecked = true
    }
    LaunchedEffect(authenticated) {
        if (!authenticated) return@LaunchedEffect
        runCatching { backend?.getPreferredGameMode() }.getOrNull()?.let { SonHarfGameModeState.mode = it }
        val equipped = runCatching { backend?.getEquippedCosmetics() }.getOrNull()
        SonHarfCosmetics.apply(equipped)
        MascotSelectionRuntime.select(context, equipped?.mascotId ?: MascotCatalog.DEFAULT_ID)
    }
    LaunchedEffect(lobbyRequest) {
        if (authenticated && lobbyRequest > 0) {
            autoStartMatchmaking = false
            gameKey += 1
            screen = ClassicScreen.GAME
        }
    }
    LaunchedEffect(screen, gameKey) {
        if (authenticated && screen == ClassicScreen.GAME) {
            runCatching { backend?.logEvent("son_harf_open") }
        }
    }
    LaunchedEffect(SonHarfUiState.homeRequest) {
        if (SonHarfUiState.homeRequest > 0) screen = ClassicScreen.HOME
    }
    BackHandler(enabled = authenticated) {
        when (screen) {
            ClassicScreen.PROFILE_FULL -> screen = profileFullReturnScreen
            ClassicScreen.SHOP_FULL -> screen = shopFullReturnScreen
            ClassicScreen.MASCOT_ROOM -> screen = ClassicScreen.MASCOT
            ClassicScreen.HUB -> screen = hubReturnScreen
            ClassicScreen.HOME -> {
                val now = System.currentTimeMillis()
                if (now - lastHomeBack < 1800L) (context as? Activity)?.finish() else lastHomeBack = now
            }
            else -> screen = ClassicScreen.HOME
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

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(ClassicBg, ClassicBgDeep)))) {
        MascotBehaviorBridge()
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (screen in setOf(ClassicScreen.HOME, ClassicScreen.PLAY, ClassicScreen.PROFILE, ClassicScreen.SHOP)) {
                    ClassicBottomBar(
                        current = screen,
                        onHome = { screen = ClassicScreen.HOME },
                        onPlay = { screen = ClassicScreen.PLAY },
                        onLeague = { screen = ClassicScreen.LEAGUE },
                        onSocial = { FriendsQuickAccessState.open = true },
                        onProfile = { screen = ClassicScreen.PROFILE },
                    )
                }
            },
        ) { padding ->
            val showTopBanner = screen !in setOf(ClassicScreen.GAME, ClassicScreen.BIL_BAKALIM, ClassicScreen.DAILY_CIPHER)
            Column(Modifier.fillMaxSize().padding(padding)) {
                if (showTopBanner) SonHarfTopAdBanner()
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    when (screen) {
                    ClassicScreen.HOME -> PremiumMasterHome(
                        backend = backend,
                        onPlay = { screen = ClassicScreen.PLAY },
                        onQuickGame = { autoStartMatchmaking = true; gameKey += 1; screen = ClassicScreen.GAME },
                        onBilBakalim = { screen = ClassicScreen.BIL_BAKALIM },
                        onAdmin = { screen = ClassicScreen.ADMIN },
                        onHub = { hubTab = 0; hubReturnScreen = ClassicScreen.HOME; screen = ClassicScreen.HUB },
                        onLeague = { screen = ClassicScreen.LEAGUE },
                        onShop = { screen = ClassicScreen.SHOP },
                        onProfile = { screen = ClassicScreen.PROFILE },
                        onGoals = { hubTab = 2; hubReturnScreen = ClassicScreen.HOME; screen = ClassicScreen.HUB },
                        onSeason = { hubTab = 1; hubReturnScreen = ClassicScreen.HOME; screen = ClassicScreen.HUB },
                        onWardrobe = { shopFullTab = 0; shopFullReturnScreen = ClassicScreen.HOME; screen = ClassicScreen.SHOP_FULL },
                        onNotifications = { profileFullTab = 2; profileFullReturnScreen = ClassicScreen.HOME; screen = ClassicScreen.PROFILE_FULL },
                        onDailyCipher = { screen = ClassicScreen.DAILY_CIPHER },
                        onMastery = { screen = ClassicScreen.MASTERY },
                        onHistory = { screen = ClassicScreen.HISTORY },
                        onMascot = { screen = ClassicScreen.MASCOT },
                        onRoom = { screen = ClassicScreen.MASCOT_ROOM },
                    )
                    ClassicScreen.PLAY -> ClassicPlayScreen(
                        backend = backend,
                        onBack = { screen = ClassicScreen.HOME },
                        onRandom = { autoStartMatchmaking = true; gameKey += 1; screen = ClassicScreen.GAME },
                        onFriend = { FriendsQuickAccessState.open = true },
                    )
                    ClassicScreen.PROFILE -> ClassicProfileScreen(
                        backend = backend,
                        onBack = { screen = ClassicScreen.HOME },
                        onDetails = { profileFullTab = 1; profileFullReturnScreen = ClassicScreen.PROFILE; screen = ClassicScreen.PROFILE_FULL },
                        onCosmetics = { shopFullTab = 0; shopFullReturnScreen = ClassicScreen.PROFILE; screen = ClassicScreen.SHOP_FULL },
                        onAchievements = { hubTab = 0; hubReturnScreen = ClassicScreen.PROFILE; screen = ClassicScreen.HUB },
                        onHistory = { hubTab = 4; hubReturnScreen = ClassicScreen.PROFILE; screen = ClassicScreen.HUB },
                    )
                    ClassicScreen.SHOP -> ClassicStoreScreen(
                        backend = backend,
                        onBack = { screen = ClassicScreen.HOME },
                        onFullShop = { shopFullTab = 0; shopFullReturnScreen = ClassicScreen.SHOP; screen = ClassicScreen.SHOP_FULL },
                    )
                    ClassicScreen.GAME -> key(gameKey) { TargetNeonGameScreen(autoStartMatchmaking = autoStartMatchmaking) }
                    ClassicScreen.BIL_BAKALIM -> TrackedBilBakalimStandaloneScreen { screen = ClassicScreen.HOME }
                    ClassicScreen.DAILY_CIPHER -> DailyCipherScreen { screen = ClassicScreen.HOME }
                    ClassicScreen.MASTERY -> MasteryPathScreen(
                        onBack = { screen = ClassicScreen.HOME },
                        onPlay = { screen = ClassicScreen.PLAY },
                        onLeague = { screen = ClassicScreen.LEAGUE },
                    )
                    ClassicScreen.ADMIN -> AdminConsoleScreen { screen = ClassicScreen.HOME }
                    ClassicScreen.HUB -> MetaHubScreen(
                        initialTab = hubTab,
                        onBack = { screen = hubReturnScreen },
                        onPlay = { screen = ClassicScreen.PLAY },
                    )
                    ClassicScreen.LEAGUE -> LeaderboardExperienceScreen { screen = ClassicScreen.HOME }
                    ClassicScreen.PROFILE_FULL -> ProfileExperienceScreen(
                        initialTab = profileFullTab,
                        onBack = { screen = profileFullReturnScreen },
                    )
                    ClassicScreen.SHOP_FULL -> EconomyShopScreen(
                        initialTab = shopFullTab,
                        onBack = { screen = shopFullReturnScreen },
                    )
                    ClassicScreen.HISTORY -> WizardHistoryScreen(
                        backend = backend,
                        onBack = { screen = ClassicScreen.HOME },
                        onOpenMascot = { screen = ClassicScreen.MASCOT },
                        onOpenShop = {
                            shopFullTab = 0
                            shopFullReturnScreen = ClassicScreen.HISTORY
                            screen = ClassicScreen.SHOP_FULL
                        },
                    )
                    ClassicScreen.MASCOT -> MascotCompanionScreen(
                        backend = backend,
                        onBack = { screen = ClassicScreen.HOME },
                        onOpenHistory = { screen = ClassicScreen.HISTORY },
                        onOpenRoom = { screen = ClassicScreen.MASCOT_ROOM },
                        onOpenShop = {
                            shopFullTab = 1
                            shopFullReturnScreen = ClassicScreen.MASCOT
                            screen = ClassicScreen.SHOP_FULL
                        },
                    )
                    ClassicScreen.MASCOT_ROOM -> MascotRoomScreen(
                        backend = backend,
                        onBack = { screen = ClassicScreen.MASCOT },
                        onOpenCompanion = { screen = ClassicScreen.MASCOT },
                        onOpenHistory = { screen = ClassicScreen.HISTORY },
                    )
                    }
                }
            }
        }

        if (screen == ClassicScreen.GAME) {
            if (SonHarfGameModeState.mode == "expert") ExpertArenaOverlay() else SketchGameOverlayV9()
            ComboOverlayV9()
            BilBakalimBonusOverlay()
        }
    }
}

@Composable
private fun ClassicHome(
    backend: OnlineGameBackend?,
    onPlay: () -> Unit,
    onQuickGame: () -> Unit,
    onBilBakalim: () -> Unit,
    onAdmin: () -> Unit,
    onHub: () -> Unit,
    onLeague: () -> Unit,
    onShop: () -> Unit,
    onProfile: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var growth by remember { mutableStateOf<GrowthDashboardDto?>(null) }
    var mode by remember { mutableStateOf(SonHarfGameModeState.mode) }
    var dailyMessage by remember { mutableStateOf("") }
    var isAdmin by remember { mutableStateOf(false) }

    suspend fun reload() {
        val id = backend?.currentUserId()
        if (id != null) profile = runCatching { backend.getProfile(id) }.getOrNull()
        growth = runCatching { backend?.getGrowthDashboard() }.getOrNull()
        isAdmin = if (backend == null) false else runCatching { backend.getAdminDashboard(); true }.getOrDefault(false)
        runCatching { backend?.getPreferredGameMode() }.getOrNull()?.let {
            mode = it
            SonHarfGameModeState.mode = it
        }
        runCatching { backend?.logEvent("home_open_classic_premium") }
    }
    LaunchedEffect(Unit) { reload() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ClassicHeader(profile, growth, onProfile, isAdmin, onAdmin) }
        item { ClassicHero(onQuickGame) }
        item { BilBakalimHomeCard(onBilBakalim) }
        item {
            ClassicModeSelector(mode) { next ->
                mode = next
                SonHarfGameModeState.mode = next
                scope.launch { runCatching { backend?.setPreferredGameMode(next) } }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ClassicShortcut(Icons.Rounded.TrackChanges, sh("GÖREVLER", "GOALS"), 2, Modifier.weight(1f), onHub)
                ClassicShortcut(Icons.Rounded.CardGiftcard, sh("GÜNLÜK ÖDÜL", "DAILY"), if (growth?.dailyClaimed == true) 0 else 1, Modifier.weight(1f)) {
                    val d = growth
                    if (d == null || d.dailyClaimed) return@ClassicShortcut
                    scope.launch {
                        val reward = runCatching { backend?.claimDailyCheckin() ?: 0 }.getOrDefault(0)
                        dailyMessage = if (reward > 0) "+$reward" else sh("Alındı", "Claimed")
                        reload()
                    }
                }
                ClassicShortcut(Icons.Rounded.EmojiEvents, sh("LİGLER", "LEAGUES"), 0, Modifier.weight(1f), onLeague)
                ClassicShortcut(Icons.Rounded.ShoppingCart, sh("MAĞAZA", "SHOP"), 0, Modifier.weight(1f), onShop)
                ClassicShortcut(Icons.Rounded.Checkroom, sh("DOLABIM", "WARDROBE"), 0, Modifier.weight(1f), onProfile)
            }
        }
        item { ClassicDailySeries(growth, dailyMessage) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ClassicSeasonCard(Modifier.weight(1f).aspectRatio(1f), onHub)
                ClassicLeagueCard(growth, Modifier.weight(1f).aspectRatio(1f), onLeague)
            }
        }
        item { ClassicWeeklyTopThree(backend) }
        item { ClassicStats(growth) }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun ClassicHeader(profile: ProfileDto?, growth: GrowthDashboardDto?, onProfile: () -> Unit, isAdmin: Boolean, onAdmin: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.clickable(onClick = onProfile)) {
            ProfilePhotoAvatar(profile?.avatarPath, profile?.displayName ?: "S", 56.dp, visible = true, accent = ClassicBlue)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(profile?.displayName ?: sh("SonHarf Ustası", "SonHarf Master"), color = ClassicText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                if (profile?.isVip == true) {
                    Spacer(Modifier.width(5.dp))
                    Icon(Icons.Rounded.Star, null, tint = ClassicGold, modifier = Modifier.size(17.dp))
                }
            }
            Text("${sh("Seviye", "Level")} ${growth?.level ?: 1}", color = ClassicMuted, fontSize = 11.sp)
            Spacer(Modifier.height(5.dp))
            LinearProgressIndicator(
                progress = { growth?.let { it.levelProgress.toFloat() / it.levelTarget.coerceAtLeast(1) }?.coerceIn(0f, 1f) ?: 0f },
                modifier = Modifier.fillMaxWidth(.66f).height(5.dp).clip(CircleShape),
                color = ClassicGold,
                trackColor = Color.White.copy(alpha = .09f),
            )
            Text("${growth?.xp ?: 0} XP", color = ClassicMuted, fontSize = 9.sp)
        }
        HeaderWallet(Icons.Rounded.Paid, "${(growth?.xp ?: 0) * 2}", ClassicGoldSoft)
        Spacer(Modifier.width(5.dp))
        HeaderWallet(Icons.Rounded.Diamond, "${profile?.diamonds ?: 0}", ClassicBlue)
        Spacer(Modifier.width(5.dp))
        Surface(onClick = if (isAdmin) onAdmin else ({}), shape = CircleShape, color = if (isAdmin) ClassicGold.copy(alpha=.18f) else ClassicPanel, modifier = Modifier.size(38.dp), border = if (isAdmin) BorderStroke(1.dp, ClassicGold) else null) {
            Box(contentAlignment = Alignment.Center) {
                Icon(if (isAdmin) Icons.Rounded.AdminPanelSettings else Icons.Rounded.Notifications, null, tint = if (isAdmin) ClassicGold else ClassicCream, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun HeaderWallet(icon: ImageVector, value: String, tint: Color) {
    Surface(shape = RoundedCornerShape(15.dp), color = ClassicPanel, border = BorderStroke(1.dp, ClassicBorder)) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(4.dp))
            Text(value, color = ClassicText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ClassicHero(onPlay: () -> Unit) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ClassicPanel),
        border = BorderStroke(1.dp, ClassicBorder),
    ) {
        Box(Modifier.fillMaxWidth().height(282.dp)) {
            LibraryScene(Modifier.matchParentSize())
            Box(
                Modifier.matchParentSize().background(
                    Brush.horizontalGradient(listOf(Color(0xF7FFFFFF), Color(0xD9FFFFFF), Color(0x80E8F6FF)))
                )
            )
            Column(
                Modifier.fillMaxHeight().fillMaxWidth(.62f).padding(18.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                ProfessionalLogo(92.dp)
                Spacer(Modifier.height(8.dp))
                Text("SON\nHARF", color = ClassicCream, fontFamily = FontFamily.Serif, fontSize = 39.sp, lineHeight = 35.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text(sh("CANLI KELİME ARENASI", "LIVE WORD ARENA"), color = ClassicGoldSoft, fontSize = 10.sp, letterSpacing = 1.6.sp)
                Spacer(Modifier.height(10.dp))
                Text(sh("Kelimenin son harfiyle zafer senin!", "Win with the last letter of every word."), color = ClassicText, fontSize = 13.sp, lineHeight = 18.sp)
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(containerColor = ClassicGold, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 11.dp),
                ) {
                    Text(sh("HEMEN OYNA", "PLAY NOW"), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(7.dp))
                    Icon(Icons.Rounded.ArrowForward, null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun LibraryScene(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF172B3C), Color(0xFF0B1724))))
        val shelf = Color(0xFF2A211A)
        val wood = Color(0xFF4A3525)
        for (i in 0..4) {
            val x = size.width * (.55f + i * .09f)
            drawRect(shelf, topLeft = Offset(x, size.height * .08f), size = Size(size.width * .06f, size.height * .56f))
            for (j in 0..5) {
                val y = size.height * (.12f + j * .085f)
                drawRect(Color(0xFF6B5742).copy(alpha = .65f), topLeft = Offset(x + 4f, y), size = Size(size.width * .045f, size.height * .05f))
            }
        }
        drawRect(wood, topLeft = Offset(size.width * .42f, size.height * .72f), size = Size(size.width * .58f, size.height * .18f))
        drawRoundRect(Color(0xFFB8945A), topLeft = Offset(size.width * .82f, size.height * .18f), size = Size(size.width * .11f, size.height * .17f), cornerRadius = CornerRadius(24f, 24f))
        drawRect(Color(0xFFC49A58), topLeft = Offset(size.width * .87f, size.height * .35f), size = Size(size.width * .012f, size.height * .30f))
        drawCircle(Color(0xFF6B4D2B), radius = size.width * .035f, center = Offset(size.width * .876f, size.height * .66f))
        val bookY = size.height * .74f
        drawRoundRect(Color(0xFF8B6A43), topLeft = Offset(size.width * .58f, bookY), size = Size(size.width * .23f, size.height * .09f), cornerRadius = CornerRadius(12f, 12f))
        drawRoundRect(Color(0xFFD8C7A0), topLeft = Offset(size.width * .61f, bookY - size.height * .05f), size = Size(size.width * .18f, size.height * .07f), cornerRadius = CornerRadius(16f, 16f))
        drawLine(Color(0xFF6C593F), Offset(size.width * .70f, bookY - size.height * .05f), Offset(size.width * .70f, bookY + size.height * .02f), 2f)
    }
}

@Composable
private fun ProfessionalLogo(size: Dp) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(ClassicGold.copy(alpha = .12f))
            drawCircle(ClassicGold, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx()))
            drawCircle(ClassicCream.copy(alpha = .95f), radius = this.size.minDimension * .23f)
            drawCircle(ClassicBg, radius = this.size.minDimension * .18f)
            val crownY = this.size.height * .18f
            val cx = this.size.width / 2f
            drawLine(ClassicGoldSoft, Offset(cx - 18f, crownY + 16f), Offset(cx - 9f, crownY), 4f)
            drawLine(ClassicGoldSoft, Offset(cx - 9f, crownY), Offset(cx, crownY + 12f), 4f)
            drawLine(ClassicGoldSoft, Offset(cx, crownY + 12f), Offset(cx + 9f, crownY), 4f)
            drawLine(ClassicGoldSoft, Offset(cx + 9f, crownY), Offset(cx + 18f, crownY + 16f), 4f)
            drawLine(ClassicGoldSoft, Offset(cx - 18f, crownY + 16f), Offset(cx + 18f, crownY + 16f), 4f)
        }
        Text("S·H", color = ClassicCream, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = (size.value * .22f).sp)
    }
}

@Composable
private fun ClassicModeSelector(mode: String, onMode: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ModeCard(Icons.Rounded.Group, sh("NORMAL", "NORMAL"), sh("Rastgele Rakipler", "Random rivals"), mode == "normal", Modifier.weight(1f)) { onMode("normal") }
        ModeCard(Icons.Rounded.EmojiEvents, sh("UZMAN", "EXPERT"), sh("Daha Zor Rakipler", "Harder rivals"), mode == "expert", Modifier.weight(1f)) { onMode("expert") }
    }
}

@Composable
private fun ModeCard(icon: ImageVector, title: String, subtitle: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) ClassicPanel2 else ClassicPanel,
        border = BorderStroke(1.dp, if (selected) ClassicGold else ClassicBorder),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (selected) ClassicGold else ClassicCream, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, color = ClassicText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = ClassicMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun ClassicShortcut(icon: ImageVector, label: String, badge: Int, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(96.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        color = ClassicPanel,
        border = BorderStroke(1.dp, ClassicBorder),
    ) {
        Box(Modifier.fillMaxSize().padding(8.dp)) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, null, tint = ClassicGoldSoft, modifier = Modifier.size(24.dp))
                Spacer(Modifier.height(7.dp))
                Text(label, color = ClassicText, fontSize = 7.5.sp, lineHeight = 10.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 2)
            }
            if (badge > 0) {
                Surface(Modifier.align(Alignment.TopEnd).size(20.dp), shape = CircleShape, color = ClassicRed) {
                    Box(contentAlignment = Alignment.Center) { Text("$badge", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun ClassicDailySeries(growth: GrowthDashboardDto?, message: String) {
    Surface(shape = RoundedCornerShape(18.dp), color = ClassicPanel, border = BorderStroke(1.dp, ClassicBorder)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text(sh("GÜNLÜK SERİ", "DAILY STREAK"), color = ClassicGoldSoft, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(sh("Her gün oyna, ödülleri kaçırma!", "Play every day and keep your rewards."), color = ClassicMuted, fontSize = 10.sp)
                }
                if (message.isNotBlank()) Text(message, color = ClassicGold, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                val rewards = listOf("100", "150", "200", "5", "300", "10", "500")
                rewards.forEachIndexed { index, reward ->
                    val day = index + 1
                    val active = day == 3
                    Surface(
                        Modifier.weight(1f).height(84.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = if (active) Color(0xFF243242) else ClassicPanel2,
                        border = BorderStroke(1.dp, if (active) ClassicGold else ClassicBorder),
                    ) {
                        Column(Modifier.fillMaxSize().padding(horizontal = 3.dp, vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
                            Text("${sh("Gün", "Day")} $day", color = if (active) ClassicGoldSoft else ClassicMuted, fontSize = 8.sp, maxLines = 1)
                            Icon(if (day <= 2) Icons.Rounded.CheckCircle else if (day == 7) Icons.Rounded.CardGiftcard else Icons.Rounded.Star, null, tint = if (day <= 2) ClassicGreen else ClassicGold, modifier = Modifier.size(20.dp))
                            Text(reward, color = if (active) Color.White else ClassicText, fontSize = 9.sp, lineHeight = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
            if (growth?.dailyClaimed == true) {
                Spacer(Modifier.height(6.dp))
                Text(sh("Bugünün ödülü alındı.", "Today's reward is claimed."), color = ClassicGreen, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun ClassicSeasonCard(modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), color = ClassicPanel, border = BorderStroke(1.dp, ClassicBorder)) {
        Column(Modifier.fillMaxSize().padding(11.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(sh("SEZON 12", "SEASON 12"), color = ClassicGoldSoft, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
            Text(sh("ARENA ŞAMPİYONASI", "ARENA CHAMPIONSHIP"), color = ClassicText, fontSize = 8.5.sp, lineHeight = 11.sp, maxLines = 2)
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfessionalLogo(45.dp)
                Spacer(Modifier.width(7.dp))
                Column(Modifier.weight(1f)) {
                    Text(sh("24 gün 18 saat kaldı", "24 days 18 hours left"), color = ClassicMuted, fontSize = 7.5.sp, lineHeight = 10.sp, maxLines = 2)
                    Spacer(Modifier.height(5.dp))
                    LinearProgressIndicator(progress = { .72f }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape), color = ClassicGold, trackColor = Color.White.copy(alpha = .08f))
                    Text("7.250 / 10.000", color = ClassicMuted, fontSize = 7.5.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun ClassicLeagueCard(growth: GrowthDashboardDto?, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), color = ClassicPanel, border = BorderStroke(1.dp, ClassicBorder)) {
        Column(Modifier.fillMaxSize().padding(11.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(sh("LİGİN", "YOUR LEAGUE"), color = ClassicGoldSoft, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.EmojiEvents, null, tint = ClassicGold, modifier = Modifier.size(39.dp))
                Spacer(Modifier.width(7.dp))
                Column(Modifier.weight(1f)) {
                    Text(growth?.leagueName ?: "ALTIN I", color = ClassicText, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                    Text("2.150 / 3.000", color = ClassicMuted, fontSize = 8.sp, maxLines = 1)
                }
            }
            LinearProgressIndicator(progress = { .72f }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape), color = ClassicGold, trackColor = Color.White.copy(alpha = .08f))
            Text(sh("Sıralamanı yükselt", "Climb the ranking"), color = ClassicMuted, fontSize = 7.5.sp, maxLines = 1)
        }
    }
}

@Composable
private fun ClassicStats(growth: GrowthDashboardDto?) {
    Surface(shape = RoundedCornerShape(18.dp), color = ClassicPanel, border = BorderStroke(1.dp, ClassicBorder)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(sh("ARENA İSTATİSTİKLERİ", "ARENA STATISTICS"), color = ClassicMuted, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatCell(sh("Maçlar", "Matches"), "${growth?.totalMatches ?: 0}")
                StatCell(sh("Kazanma", "Win rate"), growth?.let { if (it.totalMatches > 0) "%${(it.wins * 100 / it.totalMatches)}" else "%0" } ?: "%0")
                StatCell(sh("En Uzun Seri", "Best streak"), "${growth?.bestStreak ?: 0}")
                StatCell(sh("Toplam XP", "Total XP"), "${growth?.xp ?: 0}")
                StatCell(sh("Kelime Bilgisi", "Word skill"), sh("USTA", "MASTER"))
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = ClassicMuted, fontSize = 8.sp, textAlign = TextAlign.Center)
        Text(value, color = ClassicText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ClassicPlayScreen(backend: OnlineGameBackend?, onBack: () -> Unit, onRandom: () -> Unit, onFriend: () -> Unit) {
    var mode by remember { mutableStateOf(SonHarfGameModeState.mode) }
    val scope = rememberCoroutineScope()
    ClassicPageScaffold(sh("OYNA", "PLAY"), onBack) {
        PlayOption(
            icon = Icons.Rounded.Group,
            title = sh("Rastgele Rakip", "Random Rival"),
            subtitle = sh("Hızlı maç, rastgele eşleşme", "Quick match with a random player"),
            button = sh("EŞLEŞME BUL", "FIND MATCH"),
            onClick = onRandom,
        )
        PlayOption(
            icon = Icons.Rounded.Person,
            title = sh("Arkadaşınla Oyna", "Play With a Friend"),
            subtitle = sh("Arkadaşını davet et, beraber oynayın", "Invite a friend and play together"),
            button = sh("DAVET GÖNDER", "SEND INVITE"),
            onClick = onFriend,
        )
        Surface(shape = RoundedCornerShape(16.dp), color = ClassicPanel, border = BorderStroke(1.dp, ClassicBorder)) {
            Column(Modifier.padding(14.dp)) {
                Text(sh("Oyun Modu", "Game Mode"), color = ClassicText, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                ClassicModeSelector(mode) { next ->
                    mode = next
                    SonHarfGameModeState.mode = next
                    scope.launch { runCatching { backend?.setPreferredGameMode(next) } }
                }
            }
        }
    }
}

@Composable
private fun PlayOption(icon: ImageVector, title: String, subtitle: String, button: String, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = ClassicPanel, border = BorderStroke(1.dp, ClassicBorder)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(70.dp), shape = CircleShape, color = ClassicGold.copy(alpha = .12f)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = ClassicGoldSoft, modifier = Modifier.size(38.dp)) }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = ClassicText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, color = ClassicMuted, fontSize = 11.sp, lineHeight = 16.sp)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = ClassicGold, contentColor = Color(0xFF2B1E0C)), shape = RoundedCornerShape(10.dp)) {
                    Text(button, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ClassicProfileScreen(
    backend: OnlineGameBackend?,
    onBack: () -> Unit,
    onDetails: () -> Unit,
    onCosmetics: () -> Unit,
    onAchievements: () -> Unit,
    onHistory: () -> Unit,
) {
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var growth by remember { mutableStateOf<GrowthDashboardDto?>(null) }
    LaunchedEffect(Unit) {
        val id = backend?.currentUserId()
        if (id != null) profile = runCatching { backend.getProfile(id) }.getOrNull()
        growth = runCatching { backend?.getGrowthDashboard() }.getOrNull()
    }
    ClassicPageScaffold(sh("PROFİL", "PROFILE"), onBack, trailing = {
        IconButton(onClick = onDetails) { Icon(Icons.Rounded.Settings, null, tint = ClassicCream) }
    }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(78.dp), shape = CircleShape, color = ClassicPanel2, border = BorderStroke(2.dp, ClassicGold)) {
                Box(contentAlignment = Alignment.Center) { Text((profile?.displayName ?: "S").take(1).uppercase(), color = ClassicCream, fontSize = 29.sp, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(profile?.displayName ?: sh("SonHarf Ustası", "SonHarf Master"), color = ClassicText, fontFamily = FontFamily.Serif, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("ID: SH-${profile?.id?.takeLast(8)?.uppercase() ?: "----"}", color = ClassicMuted, fontSize = 10.sp)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Rounded.Lock, null, tint = ClassicGold, modifier = Modifier.size(12.dp))
                }
                Text("🇹🇷 Türkiye", color = ClassicMuted, fontSize = 10.sp)
            }
        }
        ProfileStatsRow(growth)
        Surface(shape = RoundedCornerShape(18.dp), color = ClassicPanel, border = BorderStroke(1.dp, ClassicBorder)) {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Text("${sh("Seviye", "Level")} ${growth?.level ?: 1}", color = ClassicText, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(7.dp))
                LinearProgressIndicator(
                    progress = { growth?.let { it.levelProgress.toFloat() / it.levelTarget.coerceAtLeast(1) } ?: 0f },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = ClassicGold,
                    trackColor = Color.White.copy(alpha = .08f),
                )
                Text("${growth?.xp ?: 0} XP", color = ClassicMuted, fontSize = 9.sp)
            }
        }
        ProfileMenuRow(Icons.Rounded.Checkroom, sh("Kozmetikler", "Cosmetics"), onCosmetics)
        ProfileMenuRow(Icons.Rounded.EmojiEvents, sh("Başarımlar", "Achievements"), onAchievements)
        ProfileMenuRow(Icons.Rounded.History, sh("Maç Geçmişi", "Match History"), onHistory)
        OutlinedButton(onClick = onDetails, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, ClassicGold), colors = ButtonDefaults.outlinedButtonColors(contentColor = ClassicGoldSoft)) {
            Text(sh("PROFİL AYARLARI VE GİZLİLİK", "PROFILE SETTINGS & PRIVACY"))
        }
    }
}

@Composable
private fun ProfileStatsRow(growth: GrowthDashboardDto?) {
    Surface(shape = RoundedCornerShape(16.dp), color = ClassicPanel, border = BorderStroke(1.dp, ClassicBorder)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatCell(sh("Maçlar", "Matches"), "${growth?.totalMatches ?: 0}")
            StatCell(sh("Kazanma", "Win rate"), growth?.let { if (it.totalMatches > 0) "%${it.wins * 100 / it.totalMatches}" else "%0" } ?: "%0")
            StatCell(sh("En Uzun Seri", "Best streak"), "${growth?.bestStreak ?: 0}")
        }
    }
}

@Composable
private fun ProfileMenuRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(13.dp), color = ClassicPanel, border = BorderStroke(1.dp, ClassicBorder)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = ClassicGoldSoft, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(title, color = ClassicText, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Icon(Icons.Rounded.ArrowForward, null, tint = ClassicMuted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ClassicStoreScreen(backend: OnlineGameBackend?, onBack: () -> Unit, onFullShop: () -> Unit) {
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var tab by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        val id = backend?.currentUserId()
        if (id != null) profile = runCatching { backend.getProfile(id) }.getOrNull()
    }
    ClassicPageScaffold(sh("MAĞAZA", "SHOP"), onBack) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(sh("Öne Çıkan", "Featured"), sh("Kozmetikler", "Cosmetics"), sh("Elmas", "Diamonds"), sh("Özel", "Special")).forEachIndexed { index, label ->
                FilterChip(
                    selected = tab == index,
                    onClick = {
                        if (index == 0) tab = 0 else {
                            tab = index
                            onFullShop()
                        }
                    },
                    label = { Text(label, fontSize = 9.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ClassicGold.copy(alpha = .22f), selectedLabelColor = ClassicGoldSoft, containerColor = ClassicPanel, labelColor = ClassicMuted),
                    border = FilterChipDefaults.filterChipBorder(enabled = true, selected = tab == index, borderColor = ClassicBorder, selectedBorderColor = ClassicGold),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StoreOffer(sh("Altın Paketi", "Gold Pack"), Icons.Rounded.Paid, "1.000", "₺49,99", ClassicGold, Modifier.weight(1f), onFullShop)
            StoreOffer(sh("Elmas Paketi", "Diamond Pack"), Icons.Rounded.Diamond, "500", "₺99,99", ClassicBlue, Modifier.weight(1f), onFullShop)
            StoreOffer(sh("VIP Üyelik", "VIP Membership"), Icons.Rounded.Star, sh("30 Gün", "30 Days"), "₺59,99", ClassicGoldSoft, Modifier.weight(1f), onFullShop)
        }
        Surface(shape = RoundedCornerShape(16.dp), color = ClassicPanel, border = BorderStroke(1.dp, ClassicBorder)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(sh("Mevcut Bakiyen", "Your Balance"), color = ClassicMuted, fontSize = 10.sp)
                    Text("${profile?.diamonds ?: 0} 💎", color = ClassicText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Button(onClick = onFullShop, colors = ButtonDefaults.buttonColors(containerColor = ClassicGold, contentColor = Color(0xFF2B1E0C))) {
                    Text(sh("TÜM MAĞAZAYI AÇ", "OPEN FULL SHOP"), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StoreOffer(title: String, icon: ImageVector, amount: String, price: String, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), color = ClassicPanel2, border = BorderStroke(1.dp, ClassicBorder)) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = ClassicText, fontSize = 9.sp, textAlign = TextAlign.Center, minLines = 2)
            Spacer(Modifier.height(7.dp))
            Icon(icon, null, tint = tint, modifier = Modifier.size(40.dp))
            Text(amount, color = ClassicCream, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(8.dp), color = ClassicGold) {
                Text(price, Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = Color(0xFF2B1E0C), fontWeight = FontWeight.Bold, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun ClassicPageScaffold(title: String, onBack: () -> Unit, trailing: @Composable (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null, tint = ClassicCream) }
                Text(title, color = ClassicText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                if (trailing != null) trailing() else Spacer(Modifier.size(48.dp))
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun ClassicBottomBar(current: ClassicScreen, onHome: () -> Unit, onPlay: () -> Unit, onLeague: () -> Unit, onSocial: () -> Unit, onProfile: () -> Unit) {
    Surface(color = ClassicBgDeep, shadowElevation = 18.dp, border = BorderStroke(1.dp, ClassicBorder)) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 6.dp, vertical = 7.dp), horizontalArrangement = Arrangement.SpaceAround) {
            BottomItem(Icons.Rounded.Home, sh("Ana Sayfa", "Home"), current == ClassicScreen.HOME, Modifier.weight(1f), onHome)
            BottomItem(Icons.Rounded.PlayArrow, sh("Oyna", "Play"), current == ClassicScreen.PLAY, Modifier.weight(1f), onPlay)
            BottomItem(Icons.Rounded.EmojiEvents, sh("Lig", "League"), current == ClassicScreen.LEAGUE, Modifier.weight(1f), onLeague)
            BottomItem(Icons.Rounded.Group, sh("Sosyal", "Social"), false, Modifier.weight(1f), onSocial)
            BottomItem(Icons.Rounded.Person, sh("Profil", "Profile"), current == ClassicScreen.PROFILE, Modifier.weight(1f), onProfile)
        }
    }
}

@Composable
private fun BottomItem(icon: ImageVector, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(13.dp)).clickable(onClick = onClick).padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = if (selected) ClassicGold else ClassicMuted, modifier = Modifier.size(23.dp))
        Text(label, color = if (selected) ClassicGoldSoft else ClassicMuted, fontSize = 8.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}


@Composable
private fun ClassicWeeklyTopThree(backend: OnlineGameBackend?) {
    var leaders by remember { mutableStateOf<List<com.sonharf.game.data.LeaderboardV2Row>>(emptyList()) }
    LaunchedEffect(backend) {
        leaders = runCatching { backend?.getLeaderboardV2(SonHarfUiState.language, "week", 3).orEmpty() }.getOrDefault(emptyList())
    }
    if (leaders.isEmpty()) return
    Card(
        colors = CardDefaults.cardColors(containerColor = ClassicPanel),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, ClassicBorder),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(sh("🏆 HAFTANIN EN İYİ 3 OYUNCUSU", "🏆 TOP 3 THIS WEEK"), color = ClassicText, fontWeight = FontWeight.Black, fontSize = 12.sp)
                Text(sh("CANLI", "LIVE"), color = ClassicGreen, fontWeight = FontWeight.Black, fontSize = 9.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                leaders.take(3).forEachIndexed { index, row ->
                    Surface(Modifier.weight(1f), shape = RoundedCornerShape(14.dp), color = ClassicPanel2) {
                        Column(Modifier.padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(listOf("🥇", "🥈", "🥉").getOrElse(index) { "#${index + 1}" }, fontSize = 20.sp)
                            Text(row.displayName, color = ClassicText, fontWeight = FontWeight.Black, fontSize = 10.sp, maxLines = 1)
                            Text("${row.wins}W • ${row.winRate.toInt()}%", color = ClassicMuted, fontSize = 8.sp)
                        }
                    }
                }
            }
        }
    }
}
