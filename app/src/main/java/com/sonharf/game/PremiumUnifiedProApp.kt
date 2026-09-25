package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SharedDictionaryService
import com.sonharf.game.data.SupabaseProvider
import kotlinx.coroutines.delay

private enum class PremiumDestination {
    HOME, GAMES, CLUB, COMPETE, PROFILE, COLLECTION,
    LAST_LETTER, SIEGE, WORD_WORKSHOP,
    SOCIAL, SETTINGS, ACCOUNT, PROFILE_DETAILS, SHOP, PRO, PRIVATE_ROOM, MASCOT_CHAT
}

@Composable
fun PremiumUnifiedProApp(onSignedOut: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    var destination by remember { mutableStateOf(PremiumDestination.HOME) }
    var isPro by remember { mutableStateOf(false) }
    val homeRequest = SonHarfUiState.homeRequest
    val defaultGameLanguage = SharedDictionaryService.canonicalLanguage(SonHarfUiState.language)
    var siegeLanguage by rememberSaveable { mutableStateOf(defaultGameLanguage) }
    var lastLetterLanguage by rememberSaveable { mutableStateOf(defaultGameLanguage) }
    var workshopLanguage by rememberSaveable { mutableStateOf(defaultGameLanguage) }
    var uiLanguageBeforeGame by rememberSaveable { mutableStateOf<String?>(null) }
    val shellMascotTouches = remember { WordSiegeMascotTouchState() }
    var shellProfile by remember { mutableStateOf<ProfileDto?>(null) }
    var shellMascotAnnouncement by remember { mutableStateOf<Pair<Int, String>?>(null) }
    val shellMascotVisited = remember { mutableSetOf<PremiumDestination>() }
    val shellContext = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        if (!SupabaseProvider.configured) return@LaunchedEffect
        // Owned mascots come from verified purchases on the server.
        WordSiegeMascotOwnership.refresh(shellContext)
        shellProfile = backend.currentUserId()?.let { id -> runCatching { backend.getProfile(id) }.getOrNull() }
    }
    // A short, page-appropriate remark on the first visit of a page in this session.
    LaunchedEffect(destination) {
        if (!shellMascotVisited.add(destination)) return@LaunchedEffect
        val line = when (destination) {
            PremiumDestination.SHOP -> sh("Alışveriş zamanı! 🛍️", "Shopping time! 🛍️")
            PremiumDestination.PROFILE -> sh("Profilin çok havalı!", "Your profile looks great!")
            PremiumDestination.SOCIAL -> sh("Arkadaşlarını çağır, birlikte oynayalım! 👋", "Invite your friends, let's play together! 👋")
            PremiumDestination.COMPETE -> sh("Kupa bizim olacak! 🏆", "That cup will be ours! 🏆")
            PremiumDestination.COLLECTION -> sh("Ne güzel bir koleksiyon ✨", "What a lovely collection ✨")
            else -> null
        } ?: return@LaunchedEffect
        delay(900)
        shellMascotAnnouncement = (shellMascotAnnouncement?.first ?: 0) + 1 to line
    }

    fun openGame(target: PremiumDestination, language: String) {
        if (uiLanguageBeforeGame == null) uiLanguageBeforeGame = SonHarfUiState.language
        SonHarfUiState.language = SharedDictionaryService.canonicalLanguage(language)
        destination = target
    }

    fun leaveGame(target: PremiumDestination = PremiumDestination.HOME) {
        uiLanguageBeforeGame?.let { SonHarfUiState.language = it }
        uiLanguageBeforeGame = null
        destination = target
    }

    LaunchedEffect(Unit) {
        backend.currentUserId()?.let { id ->
            runCatching { backend.getEquippedCosmetics() }.getOrNull()?.let(SonHarfCosmetics::apply)
            isPro = runCatching { backend.getProfile(id).isVip }.getOrDefault(false)
        }
    }
    LaunchedEffect(homeRequest) {
        if (homeRequest > 0) {
            uiLanguageBeforeGame?.let { SonHarfUiState.language = it }
            uiLanguageBeforeGame = null
            destination = PremiumDestination.HOME
        }
    }
    LaunchedEffect(destination) {
        if (destination !in setOf(
                PremiumDestination.LAST_LETTER,
                PremiumDestination.SIEGE,
                PremiumDestination.WORD_WORKSHOP,
            )
        ) {
            while (true) {
                runCatching { backend.setPresence("online") }
                delay(55_000)
            }
        }
    }

    BackHandler(enabled = destination != PremiumDestination.HOME) {
        destination = when (destination) {
            PremiumDestination.SETTINGS, PremiumDestination.PROFILE_DETAILS, PremiumDestination.COLLECTION, PremiumDestination.PRO -> PremiumDestination.PROFILE
            PremiumDestination.PRIVATE_ROOM -> PremiumDestination.PRO
            PremiumDestination.SOCIAL, PremiumDestination.SHOP -> PremiumDestination.HOME
            PremiumDestination.ACCOUNT -> PremiumDestination.SETTINGS
            PremiumDestination.LAST_LETTER, PremiumDestination.SIEGE, PremiumDestination.WORD_WORKSHOP -> {
                uiLanguageBeforeGame?.let { SonHarfUiState.language = it }
                uiLanguageBeforeGame = null
                PremiumDestination.HOME
            }
            else -> PremiumDestination.HOME
        }
    }

    // Top-level navigation: Home, Friends/Social, Store, Profile. Club is hidden.
    val topLevel = destination in setOf(
        PremiumDestination.HOME,
        PremiumDestination.SHOP,
        PremiumDestination.SOCIAL,
        PremiumDestination.COMPETE,
        PremiumDestination.PROFILE,
    )
    val scheme = if (SonHarfTheme.IsDark) {
        darkColorScheme(
            primary = SonHarfTheme.Primary,
            secondary = SonHarfTheme.PremiumGold,
            tertiary = SonHarfTheme.Success,
            background = SonHarfTheme.Background,
            surface = SonHarfTheme.Surface,
            surfaceVariant = SonHarfTheme.SurfaceElevated,
            surfaceContainerLowest = SonHarfTheme.Background,
            surfaceContainerLow = SonHarfTheme.SurfaceSecondary,
            surfaceContainer = SonHarfTheme.Surface,
            surfaceContainerHigh = SonHarfTheme.SurfaceElevated,
            surfaceContainerHighest = SonHarfTheme.SurfaceElevated,
            secondaryContainer = SonHarfTheme.PrimarySoft,
            onSecondaryContainer = SonHarfTheme.TextPrimary,
            onPrimary = SonHarfTheme.OnPrimary,
            onSecondary = SonHarfTheme.OnGold,
            onBackground = SonHarfTheme.TextPrimary,
            onSurface = SonHarfTheme.TextPrimary,
            onSurfaceVariant = SonHarfTheme.TextSecondary,
            outline = SonHarfTheme.Border,
            outlineVariant = SonHarfTheme.Border,
            error = SonHarfTheme.Error,
        )
    } else {
        lightColorScheme(
            primary = SonHarfTheme.Primary,
            secondary = SonHarfTheme.Turquoise,
            tertiary = SonHarfTheme.Success,
            background = SonHarfTheme.Background,
            surface = SonHarfTheme.Surface,
            onPrimary = SonHarfTheme.OnPrimary,
            onBackground = SonHarfTheme.TextPrimary,
            onSurface = SonHarfTheme.TextPrimary,
            error = SonHarfTheme.Error,
        )
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = SonHarfTypography,
        shapes = SonHarfShapes,
    ) {
        Scaffold(
            containerColor = SonHarfTheme.Background,
            topBar = {
                if (destination !in setOf(PremiumDestination.LAST_LETTER, PremiumDestination.SIEGE, PremiumDestination.WORD_WORKSHOP)) SonHarfTopAdBanner(isPremium = isPro)
            },
            bottomBar = {
                if (topLevel) {
                    PremiumBottomBar(
                        destination = destination,
                        onHome = { destination = PremiumDestination.HOME },
                        onShop = { destination = PremiumDestination.SHOP },
                        onSocial = { destination = PremiumDestination.SOCIAL },
                        onCompete = { destination = PremiumDestination.COMPETE },
                        onProfile = { destination = PremiumDestination.PROFILE },
                    )
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).wordSiegeMascotTouchWatcher(shellMascotTouches)) {
                if (!SonHarfTheme.IsDark) SonHarfLeafBackdrop(Modifier.matchParentSize())
                when (destination) {
                    PremiumDestination.HOME -> PremiumHomeScreen(
                        backend = backend,
                        onPrimary = { openGame(PremiumDestination.SIEGE, siegeLanguage) },
                        onCompete = { destination = PremiumDestination.COMPETE },
                        onProfile = { destination = PremiumDestination.PROFILE },
                        onShop = { destination = PremiumDestination.SHOP },
                        onPro = { destination = PremiumDestination.PRO },
                        onSettings = { destination = PremiumDestination.SETTINGS },
                        onLastLetter = { openGame(PremiumDestination.LAST_LETTER, lastLetterLanguage) },
                        onWorkshop = { openGame(PremiumDestination.WORD_WORKSHOP, workshopLanguage) },
                    )
                    PremiumDestination.GAMES -> PremiumGameCenter(
                        siegeLanguage = siegeLanguage,
                        lastLetterLanguage = lastLetterLanguage,
                        workshopLanguage = workshopLanguage,
                        onSiegeLanguage = { siegeLanguage = it },
                        onLastLetterLanguage = { lastLetterLanguage = it },
                        onWorkshopLanguage = { workshopLanguage = it },
                        onSiege = { openGame(PremiumDestination.SIEGE, siegeLanguage) },
                        onLastLetter = { openGame(PremiumDestination.LAST_LETTER, lastLetterLanguage) },
                        onWorkshop = { openGame(PremiumDestination.WORD_WORKSHOP, workshopLanguage) },
                    )
                    PremiumDestination.COMPETE -> CompetitionHubScreen(
                        onBack = { destination = PremiumDestination.HOME },
                    )
                    PremiumDestination.CLUB -> {
                        LaunchedEffect(Unit) { destination = PremiumDestination.HOME }
                    }
                    PremiumDestination.PROFILE -> MainPlayerProfileScreen(
                        backend,
                        { destination = PremiumDestination.PROFILE_DETAILS },
                        { destination = PremiumDestination.PRO },
                        { destination = PremiumDestination.COLLECTION },
                        { destination = PremiumDestination.SETTINGS },
                        { destination = PremiumDestination.SOCIAL },
                    )
                    PremiumDestination.COLLECTION -> PlayerCollectionScreen(backend) { destination = PremiumDestination.PROFILE }
                    PremiumDestination.SHOP -> EconomyShopScreen(
                        onBack = { destination = PremiumDestination.HOME },
                        onMembershipChanged = { isPro = it },
                        onCollection = { destination = PremiumDestination.COLLECTION },
                        onPro = { destination = PremiumDestination.PRO },
                    )
                    PremiumDestination.PRO -> UnifiedProVipScreen(
                        backend = backend,
                        onBack = { destination = PremiumDestination.PROFILE },
                        onPrivateRoom = { destination = PremiumDestination.PRIVATE_ROOM },
                    )
                    PremiumDestination.PRIVATE_ROOM -> PrivateRoomCenterScreen(
                        onBack = { destination = PremiumDestination.PRO },
                        onRoomReady = { language -> openGame(PremiumDestination.LAST_LETTER, language) },
                    )
                    PremiumDestination.LAST_LETTER -> OnlineGameScreenV6()
                    PremiumDestination.SIEGE -> WordSiegeEntryScreen(
                        onExit = { leaveGame() },
                        onOpenStore = { leaveGame(PremiumDestination.SHOP) },
                    )
                    PremiumDestination.WORD_WORKSHOP -> KelimeAtolyesiScreen {
                        leaveGame()
                    }
                    PremiumDestination.SOCIAL -> MainSocialScreen(
                        backend = backend,
                        onPlay = { openGame(PremiumDestination.LAST_LETTER, lastLetterLanguage) },
                        onSiege = { openGame(PremiumDestination.SIEGE, siegeLanguage) },
                    )
                    PremiumDestination.SETTINGS -> MainSettingsScreen(
                        backend,
                        { destination = PremiumDestination.PROFILE },
                        { destination = PremiumDestination.ACCOUNT },
                        onSignedOut,
                    )
                    PremiumDestination.ACCOUNT -> CompleteProfileScreen(1) {
                        destination = PremiumDestination.SETTINGS
                    }
                    PremiumDestination.PROFILE_DETAILS -> CompleteProfileScreen(0) {
                        destination = PremiumDestination.PROFILE
                    }
                    PremiumDestination.MASCOT_CHAT -> MascotChatScreen(
                        onBack = { destination = PremiumDestination.HOME },
                        mascotSkin = chatMascotSkin(shellContext),
                    )
                }
                // Outside the games the mascot keeps the player company from a bottom corner:
                // it mostly watches, says a word on some pages and flies aside when touched.
                if (destination !in setOf(PremiumDestination.LAST_LETTER, PremiumDestination.SIEGE, PremiumDestination.WORD_WORKSHOP, PremiumDestination.MASCOT_CHAT)) {
                    WordSiegeMascotCompanion(
                        anchors = listOf(Offset(.88f, .92f), Offset(.12f, .92f)),
                        mascotSize = 83.dp,
                        moveId = null,
                        lastMoveMine = false,
                        playerTurn = false,
                        modifier = Modifier.matchParentSize(),
                        playerName = shellProfile?.displayName,
                        playerGender = shellProfile?.gender,
                        touches = shellMascotTouches,
                        announcement = shellMascotAnnouncement,
                        stageY = .5f,
                    )
                }
            }
        }
    }
}

/** The mascot the player picked, if they own it, otherwise their first owned character. */
private fun chatMascotSkin(context: android.content.Context): WordSiegeMascotSkin {
    val owned = WordSiegeMascotOwnership.owned
    val picked = WordSiegeMascotBond(context).skinChoice
    return picked?.takeIf { it in owned } ?: owned.minByOrNull { it.ordinal } ?: WordSiegeMascotSkin.ORB
}

@Composable
private fun PremiumHomeScreen(
    backend: OnlineGameBackend,
    onPrimary: () -> Unit,
    onCompete: () -> Unit,
    onProfile: () -> Unit,
    onShop: () -> Unit,
    onPro: () -> Unit,
    onSettings: () -> Unit,
    onLastLetter: () -> Unit,
    onWorkshop: () -> Unit,
) {
    var profile by remember { mutableStateOf<ProfileDto?>(null) }

    LaunchedEffect(Unit) {
        if (!SupabaseProvider.configured) return@LaunchedEffect
        profile = backend.currentUserId()?.let { id ->
            runCatching { backend.getProfile(id) }.getOrNull()
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 600.dp).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "home_hero") {
                PremiumHomeCommandDeck(profile, onProfile, onPrimary, onShop, onPro, onSettings)
            }
            item(key = "home_secondary_modes") {
                PremiumOtherGames(onLastLetter = onLastLetter, onWorkshop = onWorkshop)
            }
            item(key = "home_daily_tasks") {
                PremiumHomeDailyTasks(onClick = onCompete)
            }
            item(key = "invite_friends") {
                InviteFriendsCard(playerName = profile?.displayName)
            }
            item(key = "daily_objective") {
                PremiumDailyObjective(onClick = onCompete)
            }
        }
    }
}

@Composable
private fun PremiumGameCenter(
    siegeLanguage: String,
    lastLetterLanguage: String,
    workshopLanguage: String,
    onSiegeLanguage: (String) -> Unit,
    onLastLetterLanguage: (String) -> Unit,
    onWorkshopLanguage: (String) -> Unit,
    onSiege: () -> Unit,
    onLastLetter: () -> Unit,
    onWorkshop: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    sh("OYUN MODLARI", "GAME MODES"),
                    color = SonHarfTheme.TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Text(
                    sh(
                        "Modunu seç, dilini ayarla ve doğrudan arenaya gir.",
                        "Pick a mode, set your language, and enter the arena.",
                    ),
                    color = SonHarfTheme.TextSecondary,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        item {
            PremiumGameCard(
                icon = Icons.Rounded.GridView,
                title = sh("KELİME KUŞATMASI", "KELİME KUŞATMASI"),
                subtitle = sh("Ana oyun • taktik alan savaşı", "Main game • tactical territory battle"),
                language = siegeLanguage,
                onLanguageChange = onSiegeLanguage,
                primary = true,
                onClick = onSiege,
            )
        }
        item {
            PremiumGameCard(
                icon = Icons.Rounded.Bolt,
                title = sh("SON HARF", "LAST LETTER"),
                subtitle = sh("Hızlı kelime düellosu", "Fast word duel"),
                language = lastLetterLanguage,
                onLanguageChange = onLastLetterLanguage,
                onClick = onLastLetter,
            )
        }
        item {
            PremiumGameCard(
                icon = Icons.Rounded.Route,
                title = sh("KELİME ATÖLYESİ", "WORD WORKSHOP"),
                subtitle = sh("7 harf, 3 görev, 60 saniye", "7 letters, 3 tasks, 60 seconds"),
                language = workshopLanguage,
                onLanguageChange = onWorkshopLanguage,
                onClick = onWorkshop,
            )
        }
    }
}

@Composable
private fun PremiumGameCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    language: String,
    onLanguageChange: (String) -> Unit,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = if (primary) SonHarfTheme.Primary.copy(alpha = .12f) else SonHarfTheme.Surface.copy(alpha = .98f),
        border = BorderStroke(1.dp, if (primary) SonHarfTheme.Primary.copy(alpha = .34f) else SonHarfTheme.Border),
        shadowElevation = if (primary) 5.dp else 1.dp,
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            if (primary) {
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = SonHarfTheme.PremiumGold.copy(alpha = .16f),
                ) {
                    Text(
                        sh("ANA ARENA", "MAIN ARENA"),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = SonHarfTheme.TextPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                Spacer(Modifier.height(10.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                HfGameIconSlot(50.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = SonHarfTheme.TextPrimary, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(subtitle, color = SonHarfTheme.TextSecondary, fontSize = 10.sp)
                }
            }
            Spacer(Modifier.height(13.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PremiumLanguageChoice(
                    language = language,
                    onLanguageChange = onLanguageChange,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (primary) SonHarfTheme.Primary else SonHarfTheme.Forest,
                        contentColor = SonHarfTheme.OnPrimary,
                    ),
                ) {
                    Text(sh("ARENA'YA GİR", "ENTER"), fontWeight = FontWeight.Black, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun PremiumLanguageChoice(
    language: String,
    onLanguageChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = language == "tr",
            onClick = { onLanguageChange("tr") },
            label = { Text("TR", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
            leadingIcon = if (language == "tr") {
                { Icon(Icons.Rounded.Check, null, Modifier.size(15.dp)) }
            } else null,
        )
        FilterChip(
            selected = language == "en",
            onClick = { onLanguageChange("en") },
            label = { Text("EN", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
            leadingIcon = if (language == "en") {
                { Icon(Icons.Rounded.Check, null, Modifier.size(15.dp)) }
            } else null,
        )
    }
}

@Composable
private fun PremiumBottomBar(
    destination: PremiumDestination,
    onHome: () -> Unit,
    onShop: () -> Unit,
    onSocial: () -> Unit,
    onCompete: () -> Unit,
    onProfile: () -> Unit,
) {
    val items = listOf(
        Triple(PremiumDestination.HOME, R.drawable.hf_ic_home, sh("Ana Sayfa", "Home")) to onHome,
        Triple(PremiumDestination.SHOP, R.drawable.hf_ic_store, sh("Mağaza", "Store")) to onShop,
        Triple(PremiumDestination.SOCIAL, R.drawable.hf_ic_club, sh("Arkadaşlar", "Friends")) to onSocial,
        Triple(PremiumDestination.COMPETE, R.drawable.hf_ic_compete, sh("Rekabet", "Compete")) to onCompete,
        Triple(PremiumDestination.PROFILE, R.drawable.hf_ic_profile, sh("Profil", "Profile")) to onProfile,
    )
    Surface(color = SonHarfTheme.NavigationSurface) {
        Column(Modifier.navigationBarsPadding()) {
            HorizontalDivider(thickness = 1.dp, color = Hf.Gold.copy(alpha = .22f))
            Row(Modifier.fillMaxWidth().height(72.dp), verticalAlignment = Alignment.CenterVertically) {
                items.forEachIndexed { index, (item, onClick) ->
                    val selected = destination == item.first
                    if (index > 0) Box(Modifier.width(1.dp).height(34.dp).background(Hf.Gold.copy(alpha = .16f)))
                    Column(
                        Modifier.weight(1f).fillMaxHeight().clickable(onClick = onClick),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            painterResource(item.second),
                            null,
                            tint = if (selected) Hf.Gold else Hf.Ivory.copy(alpha = .78f),
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            item.third,
                            color = if (selected) Hf.Gold else Hf.Ivory.copy(alpha = .78f),
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier.width(44.dp).height(3.dp).background(
                                if (selected) Hf.Gold else Color.Transparent,
                                RoundedCornerShape(99.dp),
                            ),
                        )
                    }
                }
            }
        }
    }
}
