package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
    HOME, CLUB, COMPETE, PROFILE, COLLECTION,
    SIEGE_ENTRY, LAST_LETTER_ENTRY, LETTER_PATH_ENTRY,
    LAST_LETTER, SIEGE, LETTER_PATH,
    SOCIAL, SETTINGS, ACCOUNT, PROFILE_DETAILS, SHOP
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
            PremiumDestination.SETTINGS, PremiumDestination.PROFILE_DETAILS, PremiumDestination.COLLECTION -> PremiumDestination.PROFILE
            PremiumDestination.SOCIAL, PremiumDestination.SHOP,
            PremiumDestination.SIEGE_ENTRY, PremiumDestination.LAST_LETTER_ENTRY, PremiumDestination.LETTER_PATH_ENTRY -> PremiumDestination.HOME
            PremiumDestination.ACCOUNT -> PremiumDestination.SETTINGS
            PremiumDestination.LAST_LETTER -> {
                uiLanguageBeforeGame?.let { SonHarfUiState.language = it }
                uiLanguageBeforeGame = null
                PremiumDestination.LAST_LETTER_ENTRY
            }
            PremiumDestination.SIEGE -> {
                uiLanguageBeforeGame?.let { SonHarfUiState.language = it }
                uiLanguageBeforeGame = null
                PremiumDestination.SIEGE_ENTRY
            }
            PremiumDestination.LETTER_PATH -> {
                uiLanguageBeforeGame?.let { SonHarfUiState.language = it }
                uiLanguageBeforeGame = null
                PremiumDestination.LETTER_PATH_ENTRY
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
    val immersive = destination in setOf(
        PremiumDestination.SIEGE_ENTRY,
        PremiumDestination.LAST_LETTER_ENTRY,
        PremiumDestination.LETTER_PATH_ENTRY,
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
            onPrimary = SonHarfTheme.OnPrimary,
            onBackground = SonHarfTheme.TextPrimary,
            onSurface = SonHarfTheme.TextPrimary,
            error = SonHarfTheme.Error,
        )
    }

    MaterialTheme(colorScheme = scheme) {
        Scaffold(
            containerColor = SonHarfTheme.Background,
            topBar = {
                if (!immersive) SonHarfTopAdBanner(isPremium = isPro)
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
                if (!SonHarfTheme.IsDark) SonHarfLeafBackdrop(Modifier.matchParentSize())
                when (destination) {
                    PremiumDestination.HOME -> PremiumHomeScreen(
                        backend = backend,
                        onPrimary = { destination = PremiumDestination.SIEGE_ENTRY },
                        onCompete = { destination = PremiumDestination.COMPETE },
                        onProfile = { destination = PremiumDestination.PROFILE },
                        onSocial = { destination = PremiumDestination.SOCIAL },
                        onLastLetter = { destination = PremiumDestination.LAST_LETTER_ENTRY },
                        onLetterPath = { destination = PremiumDestination.LETTER_PATH_ENTRY },
                    )
                    PremiumDestination.SIEGE_ENTRY -> PremiumSiegeEntryScreen(
                        language = siegeLanguage,
                        onLanguageChange = { siegeLanguage = it },
                        onPlay = { openGame(PremiumDestination.SIEGE, siegeLanguage) },
                        onBack = { destination = PremiumDestination.HOME },
                    )
                    PremiumDestination.LAST_LETTER_ENTRY -> PremiumLastLetterEntryScreen(
                        language = lastLetterLanguage,
                        onLanguageChange = { lastLetterLanguage = it },
                        onPlay = { openGame(PremiumDestination.LAST_LETTER, lastLetterLanguage) },
                        onBack = { destination = PremiumDestination.HOME },
                    )
                    PremiumDestination.LETTER_PATH_ENTRY -> PremiumLetterPathEntryScreen(
                        language = letterPathLanguage,
                        onLanguageChange = { letterPathLanguage = it },
                        onPlay = { openGame(PremiumDestination.LETTER_PATH, letterPathLanguage) },
                        onBack = { destination = PremiumDestination.HOME },
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
                        { destination = PremiumDestination.SHOP },
                        { destination = PremiumDestination.COLLECTION },
                        { destination = PremiumDestination.SETTINGS },
                        { destination = PremiumDestination.SOCIAL },
                    )
                    PremiumDestination.COLLECTION -> PlayerCollectionScreen(backend) { destination = PremiumDestination.PROFILE }
                    PremiumDestination.SHOP -> EconomyShopScreen(
                        onBack = { destination = PremiumDestination.HOME },
                        onMembershipChanged = { isPro = it },
                        onCollection = { destination = PremiumDestination.COLLECTION },
                    )
                    PremiumDestination.LAST_LETTER -> OnlineGameScreenV6()
                    PremiumDestination.SIEGE -> WordSiegeExperienceScreen {
                        leaveGame(PremiumDestination.SIEGE_ENTRY)
                    }
                    PremiumDestination.LETTER_PATH -> LetterLadderGameScreen {
                        leaveGame(PremiumDestination.LETTER_PATH_ENTRY)
                    }
                    PremiumDestination.SOCIAL -> MainSocialScreen(
                        backend = backend,
                        onPlay = { destination = PremiumDestination.LAST_LETTER_ENTRY },
                        onSiege = { destination = PremiumDestination.SIEGE_ENTRY },
                    )
                    PremiumDestination.SETTINGS -> AdminAwareSettingsScreen(
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item(key = "home_hero") {
                PremiumHomeCommandDeck(profile, onProfile, onPrimary, onSocial)
            }
            item(key = "home_secondary_modes") {
                PremiumOtherGames(onLastLetter = onLastLetter, onLetterPath = onLetterPath)
            }
            item(key = "daily_objective") {
                PremiumDailyObjective(onClick = onCompete)
            }
        }
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
        Triple(PremiumDestination.SOCIAL, Icons.Rounded.People, sh("ARKADAŞLAR", "FRIENDS")) to onSocial,
        Triple(PremiumDestination.SHOP, Icons.Rounded.Storefront, sh("MAĞAZA", "STORE")) to onShop,
        Triple(PremiumDestination.PROFILE, Icons.Rounded.Person, sh("PROFİL", "PROFILE")) to onProfile,
    )
    NavigationBar(containerColor = SonHarfTheme.NavigationSurface, tonalElevation = 0.dp) {
        items.forEach { (item, onClick) ->
            NavigationBarItem(
                selected = destination == item.first,
                onClick = onClick,
                icon = { Icon(item.second, null) },
                label = {
                    Text(
                        item.third,
                        fontSize = 8.sp,
                        fontWeight = if (destination == item.first) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = SonHarfTheme.Primary,
                    selectedTextColor = SonHarfTheme.Primary,
                    indicatorColor = SonHarfTheme.Primary.copy(alpha = .12f),
                    unselectedIconColor = SonHarfTheme.TextSecondary,
                    unselectedTextColor = SonHarfTheme.TextSecondary,
                ),
            )
        }
    }
}
