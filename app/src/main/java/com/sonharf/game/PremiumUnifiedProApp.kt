package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SharedDictionaryService
import com.sonharf.game.data.SupabaseProvider
import kotlinx.coroutines.delay

/**
 * Test-release shell.
 *
 * The underlying store, league and competition code is intentionally kept in the project,
 * but removed from the visible top-level experience. This keeps rollback cheap while the
 * premium visual system and the three core word modes are validated.
 */
private enum class PremiumDestination {
    HOME,
    PROFILE,
    PREMIUM,
    LAST_LETTER,
    SIEGE,
    LETTER_PATH,
    SOCIAL,
    SETTINGS,
    ACCOUNT,
    PROFILE_DETAILS,
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
        if (homeRequest > 0) leaveGame()
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
            PremiumDestination.PROFILE_DETAILS -> PremiumDestination.PROFILE
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
        PremiumDestination.PROFILE,
    )

    val scheme = darkColorScheme(
        primary = SonHarfTheme.Primary,
        secondary = SonHarfTheme.SoftBlue,
        tertiary = SonHarfTheme.PremiumGold,
        background = SonHarfTheme.Background,
        surface = SonHarfTheme.Surface,
        onPrimary = SonHarfTheme.OnPrimary,
        onBackground = SonHarfTheme.TextPrimary,
        onSurface = SonHarfTheme.TextPrimary,
        error = SonHarfTheme.Error,
    )

    MaterialTheme(colorScheme = scheme) {
        Scaffold(
            containerColor = SonHarfTheme.Background,
            topBar = {
                // Real banner component; gameplay stays ad-free.
                if (destination !in setOf(
                        PremiumDestination.LAST_LETTER,
                        PremiumDestination.SIEGE,
                        PremiumDestination.LETTER_PATH,
                    )
                ) {
                    SonHarfTopAdBanner(isPremium = isPro)
                }
            },
            bottomBar = {
                if (topLevel) {
                    PremiumTestBottomBar(
                        destination = destination,
                        onHome = { destination = PremiumDestination.HOME },
                        onSocial = { destination = PremiumDestination.SOCIAL },
                        onProfile = { destination = PremiumDestination.PROFILE },
                    )
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (destination) {
                    PremiumDestination.HOME -> PremiumHomeScreen(
                        backend = backend,
                        onPrimary = { openGame(PremiumDestination.SIEGE, siegeLanguage) },
                        onProfile = { destination = PremiumDestination.PROFILE },
                        onSocial = { destination = PremiumDestination.SOCIAL },
                        onPremium = { destination = PremiumDestination.PREMIUM },
                        onLastLetter = { openGame(PremiumDestination.LAST_LETTER, lastLetterLanguage) },
                        onLetterPath = { openGame(PremiumDestination.LETTER_PATH, letterPathLanguage) },
                    )
                    PremiumDestination.PROFILE -> MainPlayerProfileScreen(
                        backend,
                        { destination = PremiumDestination.PROFILE_DETAILS },
                        { destination = PremiumDestination.PREMIUM },
                        { destination = PremiumDestination.PROFILE_DETAILS },
                        { destination = PremiumDestination.SETTINGS },
                        { destination = PremiumDestination.SOCIAL },
                    )
                    PremiumDestination.PREMIUM -> UnifiedProVipScreen(backend) {
                        destination = PremiumDestination.HOME
                    }
                    PremiumDestination.LAST_LETTER -> OnlineGameScreenV6()
                    PremiumDestination.SIEGE -> WordSiegeExperienceScreen { leaveGame() }
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
    onProfile: () -> Unit,
    onSocial: () -> Unit,
    onPremium: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "home_hero") {
                PremiumHomeCommandDeck(
                    profile = profile,
                    onProfile = onProfile,
                    onSiege = onPrimary,
                    onPremium = onPremium,
                )
            }
            item(key = "home_secondary_modes") {
                PremiumOtherGames(
                    onLastLetter = onLastLetter,
                    onLetterPath = onLetterPath,
                )
            }
            item(key = "daily_objective") {
                PremiumDailyObjective()
            }
        }
    }
}

@Composable
private fun PremiumTestBottomBar(
    destination: PremiumDestination,
    onHome: () -> Unit,
    onSocial: () -> Unit,
    onProfile: () -> Unit,
) {
    NavigationBar(
        containerColor = SonHarfTheme.NavigationSurface,
        tonalElevation = 0.dp,
    ) {
        PremiumTestNavItem(
            selected = destination == PremiumDestination.HOME,
            icon = Icons.Rounded.Home,
            label = sh("Ana Sayfa", "Home"),
            onClick = onHome,
        )
        PremiumTestNavItem(
            selected = destination == PremiumDestination.SOCIAL,
            icon = Icons.Rounded.People,
            label = sh("Sosyal", "Social"),
            onClick = onSocial,
        )
        PremiumTestNavItem(
            selected = destination == PremiumDestination.PROFILE,
            icon = Icons.Rounded.Person,
            label = sh("Profil", "Profile"),
            onClick = onProfile,
        )
    }
}

@Composable
private fun RowScope.PremiumTestNavItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = SonHarfTheme.Primary,
            selectedTextColor = SonHarfTheme.TextPrimary,
            indicatorColor = SonHarfTheme.PrimarySoft,
            unselectedIconColor = SonHarfTheme.TextSecondary,
            unselectedTextColor = SonHarfTheme.TextSecondary,
        ),
    )
}
