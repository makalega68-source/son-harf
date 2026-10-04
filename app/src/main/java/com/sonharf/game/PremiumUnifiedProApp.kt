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
    HOME, MY_GAMES, GAMES, CLUB, COMPETE, PROFILE, COLLECTION,
    LAST_LETTER, SIEGE, WORD_WORKSHOP,
    ACTIVITY, EVENTS, RIVALS, SOCIAL, SETTINGS, ACCOUNT, PROFILE_DETAILS, SHOP, PRO, PRIVATE_ROOM, MASCOT_CHAT
}

@Composable
fun PremiumUnifiedProApp(onSignedOut: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    var destination by remember { mutableStateOf(PremiumDestination.HOME) }
    var gameReturn by remember { mutableStateOf(PremiumDestination.HOME) }
    var gamesTab by remember { mutableIntStateOf(0) }
    var shopInitialTab by rememberSaveable { mutableIntStateOf(0) }
    var proReturn by remember { mutableStateOf(PremiumDestination.PROFILE) }
    var settingsReturn by remember { mutableStateOf(PremiumDestination.HOME) }
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
        gameReturn = if (destination == PremiumDestination.MY_GAMES) PremiumDestination.MY_GAMES else PremiumDestination.HOME
        gameLaunchRevision++
        startQuickDuel = quickDuel
        if (uiLanguageBeforeGame == null) uiLanguageBeforeGame = SonHarfUiState.language
        SonHarfUiState.language = SharedDictionaryService.canonicalLanguage(language)
        destination = target
    }

    fun leaveGame(target: PremiumDestination = gameReturn) {
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
            destination = gameReturn
        }
    }
    AppPresence(backend, destination in setOf(PremiumDestination.LAST_LETTER, PremiumDestination.SIEGE, PremiumDestination.WORD_WORKSHOP))

    BackHandler(enabled = destination != PremiumDestination.HOME) {
        destination = when (destination) {
            PremiumDestination.SETTINGS -> settingsReturn
            PremiumDestination.PROFILE_DETAILS, PremiumDestination.COLLECTION -> PremiumDestination.PROFILE
            PremiumDestination.PRO -> proReturn
            PremiumDestination.PRIVATE_ROOM -> PremiumDestination.PRO
            PremiumDestination.RIVALS -> PremiumDestination.PROFILE
            PremiumDestination.SOCIAL, PremiumDestination.SHOP -> PremiumDestination.HOME
            PremiumDestination.ACCOUNT -> PremiumDestination.SETTINGS
            PremiumDestination.LAST_LETTER, PremiumDestination.SIEGE, PremiumDestination.WORD_WORKSHOP -> {
                uiLanguageBeforeGame?.let { SonHarfUiState.language = it }
                uiLanguageBeforeGame = null
                gameReturn
            }
            else -> PremiumDestination.HOME
        }
    }

    // Lobby home with a five-tab bar; other pages open from the lobby and
    // the back button always returns there. topLevel only decides where invitation toasts show.
    val lobbyTabs = listOf(
        PremiumDestination.SHOP, PremiumDestination.COMPETE, PremiumDestination.HOME,
        PremiumDestination.GAMES, PremiumDestination.PROFILE,
    )
    val topLevel = destination in setOf(
        PremiumDestination.HOME, PremiumDestination.MY_GAMES, PremiumDestination.EVENTS,
        PremiumDestination.SHOP, PremiumDestination.PROFILE,
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

    val inGame = destination in setOf(PremiumDestination.LAST_LETTER, PremiumDestination.SIEGE, PremiumDestination.WORD_WORKSHOP)
    val pageScheme = if (inGame) scheme else scheme.copy(
        primary = LobbyPalette.Accent, onPrimary = if (SonHarfTheme.IsDark) Color(0xFF193523) else Color.White,
        primaryContainer = LobbyPalette.Soft, onPrimaryContainer = LobbyPalette.Ink,
        secondary = LobbyPalette.Gold, secondaryContainer = LobbyPalette.Soft,
        onSecondaryContainer = LobbyPalette.Ink, background = LobbyPalette.Ground,
        surface = LobbyPalette.Paper, surfaceVariant = LobbyPalette.Soft,
        surfaceContainer = LobbyPalette.Paper, surfaceContainerHigh = LobbyPalette.Paper,
        surfaceContainerHighest = LobbyPalette.Soft, surfaceContainerLow = LobbyPalette.Paper,
        surfaceContainerLowest = LobbyPalette.Ground,
        onBackground = LobbyPalette.Ink, onSurface = LobbyPalette.Ink,
        onSurfaceVariant = LobbyPalette.Muted, outline = LobbyPalette.Line, outlineVariant = LobbyPalette.Line,
    )
    MaterialTheme(
        colorScheme = pageScheme,
        typography = SonHarfTypography,
        shapes = SonHarfShapes,
    ) {
        Scaffold(
            containerColor = if (inGame) SonHarfTheme.Background else LobbyPalette.Ground,
            topBar = {
                if (destination !in setOf(PremiumDestination.LAST_LETTER, PremiumDestination.SIEGE, PremiumDestination.WORD_WORKSHOP)) SonHarfTopAdBanner(isPremium = isPro)
            },
            // Five-tab bar on the main pages: Mağaza · Taht · Oyna · Oyunlar · Profil.
            bottomBar = {
                val tab = lobbyTabs.indexOf(destination)
                if (tab >= 0) LobbyBottomBar(selected = tab) { index ->
                    if (index == 0) shopInitialTab = 0
                    destination = lobbyTabs[index]
                }
            },
        ) { padding ->
            // consumeWindowInsets: the Scaffold padding already contains the status bar, so screens that
            // add statusBarsPadding() themselves (the game arenas) no longer get a second, empty band on top.
            Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
                // Clean paper menus; the game arenas retain their own surfaces.
                key(destination, gameLaunchRevision) {
                when (destination) {
                    PremiumDestination.HOME -> HomeLobbyScreen(
                        backend = backend,
                        isPro = isPro,
                        onProfile = { destination = PremiumDestination.PROFILE },
                        onShop = { shopInitialTab = 0; destination = PremiumDestination.SHOP },
                        onPro = { proReturn = PremiumDestination.HOME; destination = PremiumDestination.PRO },
                        onSettings = { settingsReturn = PremiumDestination.HOME; destination = PremiumDestination.SETTINGS },
                        onNewGame = { openGame(PremiumDestination.SIEGE, siegeLanguage) },
                        onMyGames = { gamesTab = 0; destination = PremiumDestination.MY_GAMES },
                        onFriends = { destination = PremiumDestination.SOCIAL },
                        onEvents = { destination = PremiumDestination.EVENTS },
                        onWorkshop = { openGame(PremiumDestination.WORD_WORKSHOP, workshopLanguage) },
                    )
                    PremiumDestination.MY_GAMES -> MyGamesScreen(backend, onOpen = { kind, id -> openPlayerTarget(kind, id) }, initialTab = gamesTab)
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
                        { proReturn = PremiumDestination.PROFILE; destination = PremiumDestination.PRO },
                        { destination = PremiumDestination.COLLECTION },
                        { settingsReturn = PremiumDestination.PROFILE; destination = PremiumDestination.SETTINGS },
                        { destination = PremiumDestination.SOCIAL },
                        onRivals = { destination = PremiumDestination.RIVALS },
                        onCompete = { destination = PremiumDestination.COMPETE },
                    )
                    PremiumDestination.COLLECTION -> PlayerCollectionScreen(backend) { destination = PremiumDestination.PROFILE }
                    PremiumDestination.SHOP -> EconomyShopScreen(
                        initialTab = shopInitialTab,
                        onBack = { destination = PremiumDestination.HOME },
                        onMembershipChanged = { isPro = it },
                        onCollection = { destination = PremiumDestination.COLLECTION },
                        onPro = { proReturn = PremiumDestination.SHOP; destination = PremiumDestination.PRO },
                    )
                    PremiumDestination.PRO -> UnifiedProVipScreen(
                        backend = backend,
                        onBack = { destination = proReturn },
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
                        onOpenStore = { shopInitialTab = 0; leaveGame(PremiumDestination.SHOP) },
                        onFriends = { leaveGame(PremiumDestination.SOCIAL) },
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
                        isPro = isPro,
                        onPro = { proReturn = PremiumDestination.SOCIAL; destination = PremiumDestination.PRO },
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
                        { destination = settingsReturn },
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
