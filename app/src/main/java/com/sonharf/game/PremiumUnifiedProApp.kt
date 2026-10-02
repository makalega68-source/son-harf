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
import androidx.compose.ui.draw.clipToBounds
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
import com.sonharf.game.data.getWordSiegeGame
import com.sonharf.game.data.useInviteCode
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

private enum class PremiumDestination {
    HOME, GAMES, CLUB, COMPETE, PROFILE, COLLECTION,
    LAST_LETTER, SIEGE, WORD_WORKSHOP,
    ACTIVITY, EVENTS, RIVALS, SOCIAL, SETTINGS, ACCOUNT, PROFILE_DETAILS, SHOP, PRO, PRIVATE_ROOM, MASCOT_CHAT
}

@Composable
fun PremiumUnifiedProApp(onSignedOut: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    var destination by remember { mutableStateOf(PremiumDestination.HOME) }
    var isPro by remember { mutableStateOf(false) }
    var startQuickDuel by remember { mutableStateOf(false) }
    var gameLaunchRevision by remember { mutableIntStateOf(0) }
    val homeRequest = SonHarfUiState.homeRequest
    val defaultGameLanguage = SharedDictionaryService.canonicalLanguage(SonHarfUiState.language)
    var siegeLanguage by rememberSaveable { mutableStateOf(defaultGameLanguage) }
    var lastLetterLanguage by rememberSaveable { mutableStateOf(defaultGameLanguage) }
    var workshopLanguage by rememberSaveable { mutableStateOf(defaultGameLanguage) }
    var uiLanguageBeforeGame by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val shellContext = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        if (!SupabaseProvider.configured) return@LaunchedEffect
        // Owned mascots come from verified purchases on the server.
        WordSiegeMascotOwnership.refresh(shellContext)
        // Rewarded-video passes: banked hints and a day's keyboard or theme.
        RewardPassState.refresh()
    }
    fun openGame(target: PremiumDestination, language: String, quickDuel: Boolean = false) {
        gameLaunchRevision++
        startQuickDuel = quickDuel
        if (uiLanguageBeforeGame == null) uiLanguageBeforeGame = SonHarfUiState.language
        SonHarfUiState.language = SharedDictionaryService.canonicalLanguage(language)
        destination = target
    }

    fun leaveGame(target: PremiumDestination = PremiumDestination.HOME) {
        uiLanguageBeforeGame?.let { SonHarfUiState.language = it }
        uiLanguageBeforeGame = null
        destination = target
    }

    fun openPlayerTarget(kind: String, id: String?) {
        scope.launch {
            try {
                when (kind) {
                    "invite" -> {
                        backend.useInviteCode(requireNotNull(id))
                        destination = PremiumDestination.SOCIAL
                        android.widget.Toast.makeText(shellContext, sh("Arkadaşlık isteği gönderildi", "Friend request sent"), android.widget.Toast.LENGTH_SHORT).show()
                    }
                    "social" -> destination = PremiumDestination.SOCIAL
                    "activity" -> destination = PremiumDestination.ACTIVITY
                    "siege", "series" -> {
                        val game = backend.getWordSiegeGame(requireNotNull(id))
                        val me = backend.currentUserId()
                        check(me != null && (me == game.playerOneId || me == game.playerTwoId))
                        com.sonharf.game.data.WordSiegeLaunchConfig.open(game)
                        openGame(PremiumDestination.SIEGE, game.language)
                    }
                    "son_harf" -> {
                        val room = backend.getRoom(requireNotNull(id))
                        val me = backend.currentUserId()
                        check(me != null && (me == room.hostId || me == room.guestId))
                        SonHarfLaunchConfig.pendingRoomId = room.id
                        openGame(PremiumDestination.LAST_LETTER, room.language)
                    }
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) {
                android.widget.Toast.makeText(shellContext, sh("Bağlantı açılamadı. Aktivite ekranından tekrar dene.", "Could not open. Try again from Activity."), android.widget.Toast.LENGTH_LONG).show()
                destination = PremiumDestination.ACTIVITY
            }
        }
    }
    LaunchedEffect(PlayerLinks.pending) {
        PlayerLinks.take(shellContext)?.let { openPlayerTarget(it.kind, it.id) }
    }
    LaunchedEffect(Unit) {
        MatchNotifications.schedule(shellContext)
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
            PremiumDestination.RIVALS -> PremiumDestination.PROFILE
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
    var incomingSocialCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(destination) { if (destination == PremiumDestination.SOCIAL) incomingSocialCount = 0 }
    val scheme = if (SonHarfTheme.IsDark || SonHarfCosmetics.darkArenaTheme) {
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
                        socialBadge = incomingSocialCount,
                    )
                }
            },
        ) { padding ->
            // consumeWindowInsets: the Scaffold padding already contains the status bar, so screens that
            // add statusBarsPadding() themselves (the game arenas) no longer get a second, empty band on top.
            Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
                if (!SonHarfTheme.IsDark && !SonHarfCosmetics.darkArenaTheme && !SonHarfCosmetics.walnutTheme) SonHarfLeafBackdrop(Modifier.matchParentSize())
                key(destination, gameLaunchRevision) {
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
                        onSocial = { destination = PremiumDestination.SOCIAL },
                        onActivity = { destination = PremiumDestination.ACTIVITY },
                        onEvents = { destination = PremiumDestination.EVENTS },
                        onResume = { game ->
                            com.sonharf.game.data.WordSiegeLaunchConfig.open(game)
                            openGame(PremiumDestination.SIEGE, game.language)
                        },
                        onLastLetterResume = { room -> openPlayerTarget("son_harf", room.id) },
                        incomingCount = incomingSocialCount,
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
                        onRivals = { destination = PremiumDestination.RIVALS },
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
                        onFriends = { destination = PremiumDestination.SOCIAL },
                        onQuickDuel = { openGame(PremiumDestination.SIEGE, siegeLanguage, quickDuel = true) },
                    )
                    PremiumDestination.PRIVATE_ROOM -> PrivateRoomCenterScreen(
                        onBack = { destination = PremiumDestination.PRO },
                        onRoomReady = { language -> openGame(PremiumDestination.LAST_LETTER, language) },
                    )
                    PremiumDestination.LAST_LETTER -> OnlineGameScreenV6()
                    PremiumDestination.SIEGE -> WordSiegeEntryScreen(
                        startQuickDuel = startQuickDuel,
                        onExit = { leaveGame() },
                        onOpenStore = { leaveGame(PremiumDestination.SHOP) },
                    )
                    PremiumDestination.WORD_WORKSHOP -> KelimeAtolyesiScreen {
                        leaveGame()
                    }
                    PremiumDestination.ACTIVITY -> SocialActivityScreen(backend,
                        onOpenTarget = ::openPlayerTarget,
                        onBack = { destination = PremiumDestination.HOME },
                        onFriends = { destination = PremiumDestination.SOCIAL },
                        onOpenGame = { game ->
                            com.sonharf.game.data.WordSiegeLaunchConfig.open(game)
                            openGame(PremiumDestination.SIEGE, game.language)
                        })
                    PremiumDestination.EVENTS -> EventsCalendarScreen(
                        onBack = { destination = PremiumDestination.HOME },
                        onAtelier = { openGame(PremiumDestination.WORD_WORKSHOP, workshopLanguage) },
                        onThrone = { destination = PremiumDestination.COMPETE })
                    PremiumDestination.SOCIAL, PremiumDestination.RIVALS -> MainSocialScreen(
                        backend = backend,
                        initialTab = if (destination == PremiumDestination.RIVALS) 2 else 0,
                        onOpenSiege = { game ->
                            com.sonharf.game.data.WordSiegeLaunchConfig.open(game)
                            openGame(PremiumDestination.SIEGE, game.language)
                        },
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
                }
                // Friend requests and game invitations are announced on every main page,
                // not only inside the Friends page, so they never wait unseen.
                IncomingSocialWatcher(
                    backend = backend,
                    enabled = topLevel,
                    onCount = { incomingSocialCount = it },
                    onOpen = { destination = PremiumDestination.ACTIVITY },
                    modifier = Modifier.align(Alignment.TopCenter),
                    onAcceptedSiege = { game ->
                        com.sonharf.game.data.WordSiegeLaunchConfig.open(game)
                        openGame(PremiumDestination.SIEGE, game.language)
                    },
                )
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
    onSocial: () -> Unit,
    onActivity: () -> Unit,
    onEvents: () -> Unit,
    onResume: (com.sonharf.game.data.WordSiegeGameDto) -> Unit,
    onLastLetterResume: (com.sonharf.game.data.GameRoomDto) -> Unit,
    incomingCount: Int,
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
            item(key = "ongoing_games") { HomeSessions(backend, onResume, onPrimary, onLastLetterResume) }
            item(key = "home_secondary_modes") {
                PremiumOtherGames(onLastLetter = onLastLetter, onWorkshop = onWorkshop)
            }
            item(key = "league_progress") { HomeLeague(backend, onCompete) }
            item(key = "activity_events") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onActivity, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Notifications, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(sh("AKTİVİTE", "ACTIVITY"), fontSize = 11.sp)
                    }
                    OutlinedButton(onClick = onEvents, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Event, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(sh("ETKİNLİKLER", "EVENTS"), fontSize = 11.sp)
                    }
                }
            }
            item(key = "atelier_tournament") { HomeTournamentCard(onOpen = onWorkshop) }
            item(key = "social_arena") { HomeSocialArena(backend, incomingCount, onSocial, isPro = profile?.isVip == true) }
            item(key = "home_daily_tasks") {
                PremiumHomeDailyTasks(onClick = onCompete)
            }

        }
        // The original floating companion consumes no row or empty space in the home feed.
        WordSiegeMascotCompanion(
            anchors = listOf(Offset(.88f, .92f), Offset(.12f, .92f)),
            mascotSize = 83.dp,
            moveId = null, lastMoveMine = false, playerTurn = false,
            modifier = Modifier.matchParentSize(),
            playerName = profile?.displayName, playerGender = profile?.gender,
            stageY = .5f,
        )
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
                title = sh("KELİME KUŞATMASI", "WORD SIEGE"),
                artRes = R.drawable.kelime_tahti_game_icon,
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
                artRes = R.drawable.son_harf_game_icon,
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
                artRes = R.drawable.kelime_atolyesi_game_icon,
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
    artRes: Int? = null,
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
                if (artRes != null) HfGameArt(artRes, 58.dp, 58.dp) else HfGameIconSlot(50.dp)
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
    socialBadge: Int = 0,
) {
    val items = listOf(
        Triple(PremiumDestination.HOME, R.drawable.hf_ic_home, sh("Ana Sayfa", "Home")) to onHome,
        Triple(PremiumDestination.SHOP, R.drawable.hf_ic_store, sh("Mağaza", "Store")) to onShop,
        Triple(PremiumDestination.SOCIAL, R.drawable.hf_ic_club, sh("Arkadaşlar", "Friends")) to onSocial,
        Triple(PremiumDestination.COMPETE, R.drawable.hf_ic_compete, sh("Taht", "Throne")) to onCompete,
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
                        Box {
                            Icon(
                                painterResource(item.second),
                                null,
                                tint = if (selected) Hf.Green else Hf.Text.copy(alpha = .78f),
                                modifier = Modifier.size(28.dp),
                            )
                            if (item.first == PremiumDestination.SOCIAL && socialBadge > 0) {
                                Surface(
                                    shape = RoundedCornerShape(99.dp),
                                    color = Color(0xFFD64541),
                                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-4).dp),
                                ) {
                                    Text(
                                        if (socialBadge > 9) "9+" else socialBadge.toString(),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            item.third,
                            color = if (selected) Hf.Green else Hf.Text.copy(alpha = .78f),
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier.width(44.dp).height(3.dp).background(
                                if (selected) Hf.Green else Color.Transparent,
                                RoundedCornerShape(99.dp),
                            ),
                        )
                    }
                }
            }
        }
    }
}
