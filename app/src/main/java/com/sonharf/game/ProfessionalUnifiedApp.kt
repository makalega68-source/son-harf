package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SharedDictionaryService
import kotlinx.coroutines.delay

private enum class ProfessionalDestination {
    HOME,
    COMPETE,
    PROFILE,
    COLLECTION,
    LAST_LETTER,
    SIEGE,
    LETTER_PATH,
    SOCIAL,
    SETTINGS,
    ACCOUNT,
    PROFILE_DETAILS,
    SHOP,
    PRO,
    PRIVATE_ROOM,
}

@Composable
internal fun ProfessionalUnifiedApp(onSignedOut: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    var destination by remember { mutableStateOf(ProfessionalDestination.HOME) }
    var isPro by remember { mutableStateOf(false) }
    val homeRequest = SonHarfUiState.homeRequest
    val defaultGameLanguage = SharedDictionaryService.canonicalLanguage(SonHarfUiState.language)
    var siegeLanguage by rememberSaveable { mutableStateOf(defaultGameLanguage) }
    var lastLetterLanguage by rememberSaveable { mutableStateOf(defaultGameLanguage) }
    var letterPathLanguage by rememberSaveable { mutableStateOf(defaultGameLanguage) }
    var uiLanguageBeforeGame by rememberSaveable { mutableStateOf<String?>(null) }

    fun openGame(target: ProfessionalDestination, language: String) {
        if (uiLanguageBeforeGame == null) uiLanguageBeforeGame = SonHarfUiState.language
        SonHarfUiState.language = SharedDictionaryService.canonicalLanguage(language)
        destination = target
    }

    fun leaveGame(target: ProfessionalDestination = ProfessionalDestination.HOME) {
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
            destination = ProfessionalDestination.HOME
        }
    }

    LaunchedEffect(destination) {
        if (destination !in setOf(
                ProfessionalDestination.LAST_LETTER,
                ProfessionalDestination.SIEGE,
                ProfessionalDestination.LETTER_PATH,
            )
        ) {
            while (true) {
                runCatching { backend.setPresence("online") }
                delay(55_000)
            }
        }
    }

    BackHandler(enabled = destination != ProfessionalDestination.HOME) {
        destination = when (destination) {
            ProfessionalDestination.SETTINGS,
            ProfessionalDestination.PROFILE_DETAILS,
            ProfessionalDestination.COLLECTION,
            ProfessionalDestination.PRO -> ProfessionalDestination.PROFILE
            ProfessionalDestination.PRIVATE_ROOM -> ProfessionalDestination.PRO
            ProfessionalDestination.SOCIAL,
            ProfessionalDestination.SHOP,
            ProfessionalDestination.COMPETE -> ProfessionalDestination.HOME
            ProfessionalDestination.ACCOUNT -> ProfessionalDestination.SETTINGS
            ProfessionalDestination.LAST_LETTER,
            ProfessionalDestination.SIEGE,
            ProfessionalDestination.LETTER_PATH -> {
                uiLanguageBeforeGame?.let { SonHarfUiState.language = it }
                uiLanguageBeforeGame = null
                ProfessionalDestination.HOME
            }
            else -> ProfessionalDestination.HOME
        }
    }

    val topLevel = destination in setOf(
        ProfessionalDestination.HOME,
        ProfessionalDestination.SOCIAL,
        ProfessionalDestination.SHOP,
        ProfessionalDestination.PROFILE,
    )

    val gameplay = destination in setOf(
        ProfessionalDestination.LAST_LETTER,
        ProfessionalDestination.SIEGE,
        ProfessionalDestination.LETTER_PATH,
    )

    GameTheme {
        Scaffold(
            containerColor = GameColors.AppBackground,
            topBar = {
                if (!gameplay) SonHarfTopAdBanner(isPremium = isPro)
            },
            bottomBar = {
                if (topLevel) {
                    val selectedIndex = when (destination) {
                        ProfessionalDestination.HOME -> 0
                        ProfessionalDestination.SOCIAL -> 1
                        ProfessionalDestination.SHOP -> 2
                        ProfessionalDestination.PROFILE -> 3
                        else -> 0
                    }
                    GameBottomNavigation(
                        selectedIndex = selectedIndex,
                        onHome = { destination = ProfessionalDestination.HOME },
                        onSocial = { destination = ProfessionalDestination.SOCIAL },
                        onShop = { destination = ProfessionalDestination.SHOP },
                        onProfile = { destination = ProfessionalDestination.PROFILE },
                    )
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (destination) {
                    ProfessionalDestination.HOME -> ProfessionalHomeScreen(
                        backend = backend,
                        onSiege = { openGame(ProfessionalDestination.SIEGE, siegeLanguage) },
                        onLastLetter = { openGame(ProfessionalDestination.LAST_LETTER, lastLetterLanguage) },
                        onLetterPath = { openGame(ProfessionalDestination.LETTER_PATH, letterPathLanguage) },
                        onProfile = { destination = ProfessionalDestination.PROFILE },
                        onSocial = { destination = ProfessionalDestination.SOCIAL },
                        onLeague = { destination = ProfessionalDestination.COMPETE },
                        onPro = { destination = ProfessionalDestination.PRO },
                        onCollection = { destination = ProfessionalDestination.COLLECTION },
                    )

                    ProfessionalDestination.COMPETE -> ProfessionalCompetitionHubScreen(
                        onBack = { destination = ProfessionalDestination.HOME },
                    )

                    ProfessionalDestination.PROFILE -> ProfessionalProfileScreen(
                        backend = backend,
                        onEdit = { destination = ProfessionalDestination.PROFILE_DETAILS },
                        onPro = { destination = ProfessionalDestination.PRO },
                        onCollection = { destination = ProfessionalDestination.COLLECTION },
                        onSettings = { destination = ProfessionalDestination.SETTINGS },
                        onSocial = { destination = ProfessionalDestination.SOCIAL },
                    )

                    ProfessionalDestination.COLLECTION -> PlayerCollectionScreen(backend) {
                        destination = ProfessionalDestination.PROFILE
                    }

                    ProfessionalDestination.SHOP -> EconomyShopScreen(
                        onBack = { destination = ProfessionalDestination.HOME },
                        onMembershipChanged = { isPro = it },
                        onCollection = { destination = ProfessionalDestination.COLLECTION },
                        onPro = { destination = ProfessionalDestination.PRO },
                    )

                    ProfessionalDestination.PRO -> UnifiedProVipScreen(
                        backend = backend,
                        onBack = { destination = ProfessionalDestination.PROFILE },
                        onPrivateRoom = { destination = ProfessionalDestination.PRIVATE_ROOM },
                    )

                    ProfessionalDestination.PRIVATE_ROOM -> PrivateRoomCenterScreen(
                        onBack = { destination = ProfessionalDestination.PRO },
                        onRoomReady = { language -> openGame(ProfessionalDestination.LAST_LETTER, language) },
                    )

                    ProfessionalDestination.LAST_LETTER -> OnlineGameScreenV6()

                    ProfessionalDestination.SIEGE -> WordSiegeEntryScreen(
                        onExit = { leaveGame() },
                        onOpenStore = { leaveGame(ProfessionalDestination.SHOP) },
                    )

                    ProfessionalDestination.LETTER_PATH -> LetterLadderGameScreen {
                        leaveGame()
                    }

                    ProfessionalDestination.SOCIAL -> ProfessionalSocialScreen(
                        backend = backend,
                        onPlay = { openGame(ProfessionalDestination.LAST_LETTER, lastLetterLanguage) },
                        onSiege = { openGame(ProfessionalDestination.SIEGE, siegeLanguage) },
                    )

                    ProfessionalDestination.SETTINGS -> ProfessionalSettingsScreen(
                        backend,
                        { destination = ProfessionalDestination.PROFILE },
                        { destination = ProfessionalDestination.ACCOUNT },
                        onSignedOut,
                    )

                    ProfessionalDestination.ACCOUNT -> CompleteProfileScreen(1) {
                        destination = ProfessionalDestination.SETTINGS
                    }

                    ProfessionalDestination.PROFILE_DETAILS -> CompleteProfileScreen(0) {
                        destination = ProfessionalDestination.PROFILE
                    }
                }
            }
        }
    }
}
