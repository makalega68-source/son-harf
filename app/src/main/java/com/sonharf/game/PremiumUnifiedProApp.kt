package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
    LAST_LETTER, SIEGE, LETTER_PATH,
    SOCIAL, SETTINGS, ACCOUNT, PROFILE_DETAILS, SHOP, PRO, PRIVATE_ROOM
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
    var letterPathLanguage by rememberSaveable { mutableStateOf(defaultGameLanguage) }
    var uiLanguageBeforeGame by rememberSaveable { mutableStateOf<String?>(null) }

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
                PremiumDestination.LETTER_PATH,
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
            PremiumDestination.SETTINGS,
            PremiumDestination.PROFILE_DETAILS,
            PremiumDestination.COLLECTION,
            PremiumDestination.PRO -> PremiumDestination.PROFILE
            PremiumDestination.PRIVATE_ROOM -> PremiumDestination.PRO
            PremiumDestination.SOCIAL, PremiumDestination.SHOP -> PremiumDestination.HOME
            PremiumDestination.ACCOUNT -> PremiumDestination.SETTINGS
            PremiumDestination.LAST_LETTER,
            PremiumDestination.SIEGE,
            PremiumDestination.LETTER_PATH -> {
                uiLanguageBeforeGame?.let { SonHarfUiState.language = it }
                uiLanguageBeforeGame = null
                PremiumDestination.HOME
            }
            else -> PremiumDestination.HOME
        }
    }

    val topLevel = destination in setOf(
        PremiumDestination.HOME,
        PremiumDestination.SOCIAL,
        PremiumDestination.SHOP,
        PremiumDestination.PROFILE,
    )
    val inGameplay = destination in setOf(
        PremiumDestination.LAST_LETTER,
        PremiumDestination.SIEGE,
        PremiumDestination.LETTER_PATH,
    )
    val scheme = if (SonHarfTheme.IsDark) {
        darkColorScheme(
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
    } else {
        lightColorScheme(
            primary = SonHarfTheme.Primary,
            secondary = SonHarfTheme.Turquoise,
            tertiary = SonHarfTheme.Success,
            background = SonHarfTheme.Background,
            surface = SonHarfTheme.Surface,
            surfaceVariant = SonHarfTheme.SurfaceSecondary,
            onPrimary = SonHarfTheme.OnPrimary,
            onBackground = SonHarfTheme.TextPrimary,
            onSurface = SonHarfTheme.TextPrimary,
            onSurfaceVariant = SonHarfTheme.TextSecondary,
            outline = SonHarfTheme.Border,
            error = SonHarfTheme.Error,
        )
    }

    MaterialTheme(
        colorScheme = scheme,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(22.dp),
            extraLarge = RoundedCornerShape(28.dp),
        ),
    ) {
        Scaffold(
            containerColor = SonHarfTheme.Background,
            topBar = {
                if (!inGameplay) SonHarfTopAdBanner(isPremium = isPro)
            },
            bottomBar = {
                if (topLevel) {
                    PremiumBottomBar(
                        destination = destination,
                        onHome = { destination = PremiumDestination.HOME },
                        onSocial = { destination = PremiumDestination.SOCIAL },
                        onShop = { destination = PremiumDestination.SHOP },
                        onProfile = { destination = PremiumDestination.PROFILE },
                    )
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                if (!inGameplay) PurchasedGameBackdrop(Modifier.matchParentSize())
                when (destination) {
                    PremiumDestination.HOME -> PremiumHomeScreen(
                        backend = backend,
                        onPrimary = { openGame(PremiumDestination.SIEGE, siegeLanguage) },
                        onCompete = { destination = PremiumDestination.COMPETE },
                        onProfile = { destination = PremiumDestination.PROFILE },
                        onSocial = { destination = PremiumDestination.SOCIAL },
                        onShop = { destination = PremiumDestination.SHOP },
                        onLastLetter = { openGame(PremiumDestination.LAST_LETTER, lastLetterLanguage) },
                        onLetterPath = { openGame(PremiumDestination.LETTER_PATH, letterPathLanguage) },
                    )
                    PremiumDestination.GAMES -> PremiumGameCenter(
                        siegeLanguage = siegeLanguage,
                        lastLetterLanguage = lastLetterLanguage,
                        letterPathLanguage = letterPathLanguage,
                        onSiegeLanguage = { siegeLanguage = it },
                        onLastLetterLanguage = { lastLetterLanguage = it },
                        onLetterPathLanguage = { letterPathLanguage = it },
                        onSiege = { openGame(PremiumDestination.SIEGE, siegeLanguage) },
                        onLastLetter = { openGame(PremiumDestination.LAST_LETTER, lastLetterLanguage) },
                        onLetterPath = { openGame(PremiumDestination.LETTER_PATH, letterPathLanguage) },
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
                    PremiumDestination.LETTER_PATH -> LetterLadderGameScreen { leaveGame() }
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
                }
            }
        }
    }
}

@Composable
private fun PremiumHomeScreen(
    backend: OnlineGameBackend,
    onPrimary: () -> Unit,
    onCompete: () -> Unit,
    onProfile: () -> Unit,
    onSocial: () -> Unit,
    onShop: () -> Unit,
    onLastLetter: () -> Unit,
    onLetterPath: () -> Unit,
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
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "home_hero") {
                PremiumHomeCommandDeck(profile, onProfile, onPrimary, onSocial)
            }
            item(key = "home_secondary_modes") {
                PremiumOtherGames(onLastLetter = onLastLetter, onLetterPath = onLetterPath)
            }
            item(key = "home_league") {
                PremiumLeagueProgress(profile = profile, onLeague = onCompete)
            }
            item(key = "weekly_top") {
                PremiumDailyObjective(onClick = onCompete)
            }
            item(key = "home_extras") {
                PremiumHomeExtras(profile = profile, onShop = onShop, onSocial = onSocial)
            }
            item { Spacer(Modifier.height(4.dp)) }
        }
    }
}

@Composable
private fun PremiumGameCenter(
    siegeLanguage: String,
    lastLetterLanguage: String,
    letterPathLanguage: String,
    onSiegeLanguage: (String) -> Unit,
    onLastLetterLanguage: (String) -> Unit,
    onLetterPathLanguage: (String) -> Unit,
    onSiege: () -> Unit,
    onLastLetter: () -> Unit,
    onLetterPath: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            PurchasedSectionHeader(
                title = sh("OYUN MODLARI", "GAME MODES"),
                icon = Icons.Rounded.SportsEsports,
                accent = PurchasedCasualUi2.Blue,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                sh(
                    "Modunu seç, dilini ayarla ve doğrudan arenaya gir.",
                    "Pick a mode, set your language, and enter the arena.",
                ),
                color = SonHarfTheme.TextSecondary,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(6.dp))
        }
        item {
            PremiumGameCard(
                icon = Icons.Rounded.GridView,
                title = sh("KELİME KUŞATMASI", "WORD SIEGE"),
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
                title = sh("HARF YOLU", "LETTER PATH"),
                subtitle = sh("Beş harfli rotanı tamamla", "Complete your five-letter path"),
                language = letterPathLanguage,
                onLanguageChange = onLetterPathLanguage,
                onClick = onLetterPath,
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
    val accent = if (primary) PurchasedCasualUi2.Green else PurchasedCasualUi2.Blue
    PurchasedGamePanel(
        modifier = Modifier.fillMaxWidth(),
        accent = accent,
        emphasized = primary,
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            if (primary) {
                Surface(shape = RoundedCornerShape(99.dp), color = SonHarfTheme.PremiumGold.copy(alpha = .14f)) {
                    Text(
                        sh("ANA ARENA", "MAIN ARENA"),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = SonHarfTheme.PremiumGold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                Spacer(Modifier.height(10.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = accent.copy(alpha = .12f),
                    border = BorderStroke(1.dp, accent.copy(alpha = .16f)),
                ) {
                    Icon(icon, null, tint = accent, modifier = Modifier.padding(12.dp).size(27.dp))
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = SonHarfTheme.TextPrimary, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    Spacer(Modifier.height(3.dp))
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
                Spacer(Modifier.width(10.dp))
                PurchasedGlossButton(
                    text = sh("OYNA", "PLAY"),
                    onClick = onClick,
                    modifier = Modifier.widthIn(min = 112.dp),
                    color = accent,
                    icon = Icons.Rounded.PlayArrow,
                    minHeight = 46.dp,
                )
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
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        FilterChip(
            selected = language == "tr",
            onClick = { onLanguageChange("tr") },
            label = { Text("TR", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
            leadingIcon = if (language == "tr") {
                { Icon(Icons.Rounded.Check, null, Modifier.size(14.dp)) }
            } else null,
        )
        FilterChip(
            selected = language == "en",
            onClick = { onLanguageChange("en") },
            label = { Text("EN", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
            leadingIcon = if (language == "en") {
                { Icon(Icons.Rounded.Check, null, Modifier.size(14.dp)) }
            } else null,
        )
    }
}

@Composable
private fun PremiumBottomBar(
    destination: PremiumDestination,
    onHome: () -> Unit,
    onSocial: () -> Unit,
    onShop: () -> Unit,
    onProfile: () -> Unit,
) {
    val items = listOf(
        Triple(PremiumDestination.HOME, Icons.Rounded.Home, sh("ANA SAYFA", "HOME")) to onHome,
        Triple(PremiumDestination.SOCIAL, Icons.Rounded.People, sh("SOSYAL", "SOCIAL")) to onSocial,
        Triple(PremiumDestination.SHOP, Icons.Rounded.Storefront, sh("MAĞAZA", "STORE")) to onShop,
        Triple(PremiumDestination.PROFILE, Icons.Rounded.Person, sh("PROFİL", "PROFILE")) to onProfile,
    )
    Surface(
        color = SonHarfTheme.NavigationSurface,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, SonHarfTheme.Border.copy(alpha = .7f)),
    ) {
        NavigationBar(containerColor = SonHarfTheme.NavigationSurface, tonalElevation = 0.dp) {
            items.forEach { (item, onClick) ->
                val selected = destination == item.first
                NavigationBarItem(
                    selected = selected,
                    onClick = onClick,
                    icon = { Icon(item.second, null, modifier = Modifier.size(if (selected) 25.dp else 23.dp)) },
                    label = {
                        Text(
                            item.third,
                            fontSize = 8.sp,
                            fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SonHarfTheme.Primary,
                        selectedTextColor = SonHarfTheme.Primary,
                        indicatorColor = SonHarfTheme.Primary.copy(alpha = .12f),
                        unselectedIconColor = SonHarfTheme.TextSecondary.copy(alpha = .72f),
                        unselectedTextColor = SonHarfTheme.TextSecondary,
                    ),
                )
            }
        }
    }
}
