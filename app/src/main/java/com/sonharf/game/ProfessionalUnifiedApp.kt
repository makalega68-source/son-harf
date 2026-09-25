package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SharedDictionaryService
import kotlinx.coroutines.delay

private enum class ProfessionalDestination {
    HOME,
    PLAY,
    MATCHES,
    RULES,
    LEADERBOARD,
    COMPETE,
    RETENTION,
    PROFILE,
    PROFILE_PROGRESS,
    COLLECTION,
    LAST_LETTER,
    SIEGE,
    SERIES,
    LETTER_PATH,
    SOCIAL,
    SETTINGS,
    ACCOUNT,
    PROFILE_DETAILS,
    SHOP,
    PRO,
    PRIVATE_ROOM,
    MASCOT_CHAT,
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
    // Where a game or the private room returns to: the tab it was opened from.
    var gameReturn by remember { mutableStateOf(ProfessionalDestination.HOME) }
    var siegeAction by remember { mutableStateOf<WordSiegeEntryAction?>(null) }
    var siegeGameId by remember { mutableStateOf<String?>(null) }
    var matchesReturn by remember { mutableStateOf(ProfessionalDestination.HOME) }
    var privateRoomReturn by remember { mutableStateOf(ProfessionalDestination.PRO) }

    fun openGame(target: ProfessionalDestination, language: String) {
        if (uiLanguageBeforeGame == null) uiLanguageBeforeGame = SonHarfUiState.language
        SonHarfUiState.language = SharedDictionaryService.canonicalLanguage(language)
        siegeAction = null
        siegeGameId = null
        gameReturn = when (destination) {
            ProfessionalDestination.PLAY,
            ProfessionalDestination.MATCHES,
            ProfessionalDestination.SOCIAL,
            ProfessionalDestination.LEADERBOARD,
            ProfessionalDestination.SHOP -> destination
            else -> ProfessionalDestination.HOME
        }
        destination = target
    }

    fun openSiege(action: WordSiegeEntryAction) {
        openGame(ProfessionalDestination.SIEGE, siegeLanguage)
        siegeAction = action
    }

    fun openSiegeMatch(gameId: String) {
        openGame(ProfessionalDestination.SIEGE, siegeLanguage)
        siegeGameId = gameId
    }

    // The private room returns to where it was opened from (PLAY), else to PRO as before.
    fun closePrivateRoom() {
        destination = privateRoomReturn
        privateRoomReturn = ProfessionalDestination.PRO
    }

    fun openMatches() {
        matchesReturn = if (destination == ProfessionalDestination.PLAY) ProfessionalDestination.PLAY else ProfessionalDestination.HOME
        destination = ProfessionalDestination.MATCHES
    }

    fun leaveGame(target: ProfessionalDestination = gameReturn) {
        uiLanguageBeforeGame?.let { SonHarfUiState.language = it }
        uiLanguageBeforeGame = null
        destination = target
    }

    val shellContext = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        // Owned mascots come from verified purchases on the server; without this the in-game
        // mascot never appears for a player who owns one.
        WordSiegeMascotOwnership.refresh(shellContext)
        backend.currentUserId()?.let { id ->
            runCatching { backend.getEquippedCosmetics() }.getOrNull()?.let(SonHarfCosmetics::apply)
            isPro = runCatching { backend.getProfile(id).isVip }.getOrDefault(false)
        }
    }

    LaunchedEffect(homeRequest) {
        if (homeRequest > 0) {
            uiLanguageBeforeGame?.let { SonHarfUiState.language = it }
            uiLanguageBeforeGame = null
            // Leaving Son Harf returns to where the player opened it (Play tab, matches...).
            destination = if (destination == ProfessionalDestination.LAST_LETTER) gameReturn else ProfessionalDestination.HOME
        }
    }

    LaunchedEffect(destination) {
        if (destination !in setOf(
                ProfessionalDestination.LAST_LETTER,
                ProfessionalDestination.SIEGE,
                ProfessionalDestination.SERIES,
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
            ProfessionalDestination.PROFILE_PROGRESS,
            ProfessionalDestination.COLLECTION,
            ProfessionalDestination.PRO -> ProfessionalDestination.PROFILE
            ProfessionalDestination.PRIVATE_ROOM -> privateRoomReturn.also {
                privateRoomReturn = ProfessionalDestination.PRO
            }
            ProfessionalDestination.SOCIAL,
            ProfessionalDestination.PLAY,
            ProfessionalDestination.SHOP,
            ProfessionalDestination.LEADERBOARD,
            ProfessionalDestination.PROFILE,
            ProfessionalDestination.RETENTION,
            ProfessionalDestination.MASCOT_CHAT -> ProfessionalDestination.HOME
            ProfessionalDestination.COMPETE -> ProfessionalDestination.LEADERBOARD
            ProfessionalDestination.MATCHES -> matchesReturn
            ProfessionalDestination.RULES -> ProfessionalDestination.PLAY
            ProfessionalDestination.ACCOUNT -> ProfessionalDestination.SETTINGS
            ProfessionalDestination.SERIES -> {
                uiLanguageBeforeGame?.let { SonHarfUiState.language = it }
                uiLanguageBeforeGame = null
                ProfessionalDestination.PRO
            }
            ProfessionalDestination.LAST_LETTER,
            ProfessionalDestination.SIEGE,
            ProfessionalDestination.LETTER_PATH -> {
                uiLanguageBeforeGame?.let { SonHarfUiState.language = it }
                uiLanguageBeforeGame = null
                gameReturn
            }
            else -> ProfessionalDestination.HOME
        }
    }

    val selectedTab = when (destination) {
        ProfessionalDestination.HOME -> GameMainTab.HOME
        ProfessionalDestination.SOCIAL -> GameMainTab.SOCIAL
        ProfessionalDestination.PLAY -> GameMainTab.PLAY
        ProfessionalDestination.LEADERBOARD -> GameMainTab.LEAGUE
        ProfessionalDestination.SHOP -> GameMainTab.SHOP
        else -> null
    }
    val topLevel = selectedTab != null

    val gameplay = destination in setOf(
        ProfessionalDestination.LAST_LETTER,
        ProfessionalDestination.SIEGE,
        ProfessionalDestination.SERIES,
        ProfessionalDestination.LETTER_PATH,
        ProfessionalDestination.MASCOT_CHAT,
    )

    GameTheme {
        Scaffold(
            containerColor = GameColors.AppBackground,
            topBar = {
                if (!gameplay) SonHarfTopAdBanner(isPremium = isPro)
            },
            bottomBar = {
                if (topLevel) {
                    GameBottomNavigation(
                        selected = selectedTab,
                        onSelect = { tab ->
                            destination = when (tab) {
                                GameMainTab.HOME -> ProfessionalDestination.HOME
                                GameMainTab.SOCIAL -> ProfessionalDestination.SOCIAL
                                GameMainTab.PLAY -> ProfessionalDestination.PLAY
                                GameMainTab.LEAGUE -> ProfessionalDestination.LEADERBOARD
                                GameMainTab.SHOP -> ProfessionalDestination.SHOP
                            }
                        },
                    )
                }
            },
            floatingActionButton = {
                if (destination == ProfessionalDestination.HOME) {
                    ExtendedFloatingActionButton(
                        onClick = { destination = ProfessionalDestination.MASCOT_CHAT },
                        icon = { Icon(Icons.Rounded.Forum, null) },
                        text = { Text(gameText("Maskotla Sohbet", "Mascot Chat")) },
                        containerColor = GameColors.Lavender,
                        contentColor = GameColors.TextPrimary,
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
                        onLeague = { destination = ProfessionalDestination.LEADERBOARD },
                        onPro = { destination = ProfessionalDestination.PRO },
                        onCollection = { destination = ProfessionalDestination.COLLECTION },
                        onRetention = { destination = ProfessionalDestination.RETENTION },
                        onContinueMatch = { gameId -> openSiegeMatch(gameId) },
                        onMatches = { openMatches() },
                        onTournament = { destination = ProfessionalDestination.COMPETE },
                    )

                    ProfessionalDestination.MATCHES -> MatchCenterScreen(
                        backend = backend,
                        onBack = { destination = matchesReturn },
                        onOpenMatch = { gameId -> openSiegeMatch(gameId) },
                        onQuickMatch = { openSiege(WordSiegeEntryAction.QUICK_MATCH) },
                    )

                    ProfessionalDestination.PLAY -> PlayHubScreen(
                        backend = backend,
                        onQuickMatch = { openSiege(WordSiegeEntryAction.QUICK_MATCH) },
                        onPractice = { openSiege(WordSiegeEntryAction.PRACTICE) },
                        onMyGames = { openMatches() },
                        onFriends = { destination = ProfessionalDestination.SOCIAL },
                        onPrivateRoom = {
                            privateRoomReturn = ProfessionalDestination.PLAY
                            destination = ProfessionalDestination.PRIVATE_ROOM
                        },
                        onLastLetter = { openGame(ProfessionalDestination.LAST_LETTER, lastLetterLanguage) },
                        onLetterPath = { openGame(ProfessionalDestination.LETTER_PATH, letterPathLanguage) },
                        onRules = { destination = ProfessionalDestination.RULES },
                    )

                    ProfessionalDestination.RULES -> RulesScreen(onBack = { destination = ProfessionalDestination.PLAY })

                    ProfessionalDestination.LEADERBOARD -> ProfessionalLeaderboardScreen(
                        backend = backend,
                        onBack = null,
                        onCompetition = { destination = ProfessionalDestination.COMPETE },
                    )

                    ProfessionalDestination.COMPETE -> ProfessionalCompetitionHubScreen(
                        onBack = { destination = ProfessionalDestination.LEADERBOARD },
                    )

                    ProfessionalDestination.RETENTION -> ProfessionalRetentionScreen(
                        backend = backend,
                        onBack = { destination = ProfessionalDestination.HOME },
                        onPlay = { openGame(ProfessionalDestination.SIEGE, siegeLanguage) },
                    )

                    ProfessionalDestination.PROFILE -> ProfessionalProfileScreen(
                        backend = backend,
                        onBack = { destination = ProfessionalDestination.HOME },
                        onEdit = { destination = ProfessionalDestination.PROFILE_DETAILS },
                        onProgress = { destination = ProfessionalDestination.PROFILE_PROGRESS },
                        onPro = { destination = ProfessionalDestination.PRO },
                        onCollection = { destination = ProfessionalDestination.COLLECTION },
                        onSettings = { destination = ProfessionalDestination.SETTINGS },
                        onSocial = { destination = ProfessionalDestination.SOCIAL },
                    )

                    ProfessionalDestination.PROFILE_PROGRESS -> ProfessionalProfileProgressScreen(
                        backend = backend,
                        onBack = { destination = ProfessionalDestination.PROFILE },
                    )

                    ProfessionalDestination.COLLECTION -> ProfessionalCollectionScreen(backend) {
                        destination = ProfessionalDestination.PROFILE
                    }

                    ProfessionalDestination.SHOP -> EconomyShopScreen(
                        onBack = null,
                        onMembershipChanged = { isPro = it },
                        onCollection = { destination = ProfessionalDestination.COLLECTION },
                        onPro = { destination = ProfessionalDestination.PRO },
                    )

                    ProfessionalDestination.PRO -> UnifiedProVipScreen(
                        backend = backend,
                        onBack = { destination = ProfessionalDestination.PROFILE },
                        onPrivateRoom = { destination = ProfessionalDestination.PRIVATE_ROOM },
                        onSeries = { openGame(ProfessionalDestination.SERIES, siegeLanguage) },
                    )

                    ProfessionalDestination.PRIVATE_ROOM -> PrivateRoomCenterScreen(
                        onBack = { closePrivateRoom() },
                        onRoomReady = { language ->
                            privateRoomReturn = ProfessionalDestination.PRO
                            openGame(ProfessionalDestination.LAST_LETTER, language)
                        },
                    )

                    ProfessionalDestination.MASCOT_CHAT -> MascotChatScreen(
                        onBack = { destination = ProfessionalDestination.HOME },
                    )

                    ProfessionalDestination.LAST_LETTER -> OnlineGameScreenV6()

                    ProfessionalDestination.SIEGE -> WordSiegeEntryScreen(
                        onExit = { leaveGame() },
                        onOpenStore = { leaveGame(ProfessionalDestination.SHOP) },
                        initialAction = siegeAction,
                        initialGameId = siegeGameId,
                    )

                    ProfessionalDestination.SERIES -> WordSiegeSeriesScreen(
                        verifiedAccess = true,
                        onExit = { leaveGame(ProfessionalDestination.PRO) },
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
