package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SharedDictionaryService
import kotlinx.coroutines.delay

private enum class RefreshDestination {
    HOME, CLUB, COMPETE, SOCIAL, SHOP, PROFILE, COLLECTION,
    LAST_LETTER, SIEGE, LETTER_PATH,
    SETTINGS, ACCOUNT, PROFILE_DETAILS,
}

/**
 * Current production shell for the visual refresh.
 *
 * Navigation and the existing game/profile/shop implementations are preserved; only
 * the home presentation and the shared background layer are replaced.
 */
@Composable
fun VisualRefreshProApp(onSignedOut: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    var destination by remember { mutableStateOf(RefreshDestination.HOME) }
    var isPro by remember { mutableStateOf(false) }
    val homeRequest = SonHarfUiState.homeRequest
    val defaultGameLanguage = SharedDictionaryService.canonicalLanguage(SonHarfUiState.language)
    var siegeLanguage by rememberSaveable { mutableStateOf(defaultGameLanguage) }
    var lastLetterLanguage by rememberSaveable { mutableStateOf(defaultGameLanguage) }
    var letterPathLanguage by rememberSaveable { mutableStateOf(defaultGameLanguage) }
    var uiLanguageBeforeGame by rememberSaveable { mutableStateOf<String?>(null) }

    fun openGame(target: RefreshDestination, language: String) {
        if (uiLanguageBeforeGame == null) uiLanguageBeforeGame = SonHarfUiState.language
        SonHarfUiState.language = SharedDictionaryService.canonicalLanguage(language)
        destination = target
    }

    fun leaveGame(target: RefreshDestination = RefreshDestination.HOME) {
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
                RefreshDestination.LAST_LETTER,
                RefreshDestination.SIEGE,
                RefreshDestination.LETTER_PATH,
            )
        ) {
            while (true) {
                runCatching { backend.setPresence("online") }
                delay(55_000)
            }
        }
    }

    BackHandler(enabled = destination != RefreshDestination.HOME) {
        destination = when (destination) {
            RefreshDestination.SETTINGS,
            RefreshDestination.PROFILE_DETAILS,
            RefreshDestination.COLLECTION -> RefreshDestination.PROFILE

            RefreshDestination.ACCOUNT -> RefreshDestination.SETTINGS
            RefreshDestination.LAST_LETTER,
            RefreshDestination.SIEGE,
            RefreshDestination.LETTER_PATH -> {
                uiLanguageBeforeGame?.let { SonHarfUiState.language = it }
                uiLanguageBeforeGame = null
                RefreshDestination.HOME
            }

            else -> RefreshDestination.HOME
        }
    }

    val topLevel = destination in setOf(
        RefreshDestination.HOME,
        RefreshDestination.CLUB,
        RefreshDestination.SOCIAL,
        RefreshDestination.SHOP,
        RefreshDestination.PROFILE,
    )
    val inGame = destination in setOf(
        RefreshDestination.LAST_LETTER,
        RefreshDestination.SIEGE,
        RefreshDestination.LETTER_PATH,
    )

    val scheme = lightColorScheme(
        primary = SonHarfTheme.Primary,
        secondary = SonHarfTheme.Turquoise,
        tertiary = SonHarfTheme.Success,
        background = Color.Transparent,
        surface = SonHarfTheme.Surface,
        onPrimary = SonHarfTheme.OnPrimary,
        onBackground = SonHarfTheme.TextPrimary,
        onSurface = SonHarfTheme.TextPrimary,
        error = SonHarfTheme.Error,
    )

    MaterialTheme(colorScheme = scheme) {
        AppBackground {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                topBar = {
                    if (!inGame) SonHarfTopAdBanner(isPremium = isPro)
                },
                bottomBar = {
                    if (topLevel) {
                        RefreshBottomBar(
                            destination = destination,
                            onHome = { destination = RefreshDestination.HOME },
                            onClub = { destination = RefreshDestination.CLUB },
                            onSocial = { destination = RefreshDestination.SOCIAL },
                            onShop = { destination = RefreshDestination.SHOP },
                            onProfile = { destination = RefreshDestination.PROFILE },
                        )
                    }
                },
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    // Existing screens keep their proven layouts. A tiny layer transparency on
                    // non-home pages lets the shared artwork remain perceptible even where an
                    // older screen still paints an opaque root surface.
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer(alpha = if (destination == RefreshDestination.HOME) 1f else .96f)
                    ) {
                        when (destination) {
                            RefreshDestination.HOME -> RefreshHomeScreen(
                                onSiege = { openGame(RefreshDestination.SIEGE, siegeLanguage) },
                                onLastLetter = { openGame(RefreshDestination.LAST_LETTER, lastLetterLanguage) },
                                onLetterPath = { openGame(RefreshDestination.LETTER_PATH, letterPathLanguage) },
                                onCompete = { destination = RefreshDestination.COMPETE },
                            )

                            RefreshDestination.CLUB -> CompetitionHubScreen(
                                onBack = { destination = RefreshDestination.HOME },
                                clubEntry = true,
                            )

                            RefreshDestination.COMPETE -> CompetitionHubScreen(
                                onBack = { destination = RefreshDestination.HOME },
                            )

                            RefreshDestination.SOCIAL -> MainSocialScreen(
                                backend = backend,
                                onPlay = { openGame(RefreshDestination.LAST_LETTER, lastLetterLanguage) },
                                onSiege = { openGame(RefreshDestination.SIEGE, siegeLanguage) },
                            )

                            RefreshDestination.SHOP -> EconomyShopScreen(
                                onBack = { destination = RefreshDestination.HOME },
                                onMembershipChanged = { isPro = it },
                                onCollection = { destination = RefreshDestination.COLLECTION },
                            )

                            RefreshDestination.PROFILE -> MainPlayerProfileScreen(
                                backend,
                                { destination = RefreshDestination.PROFILE_DETAILS },
                                { destination = RefreshDestination.SHOP },
                                { destination = RefreshDestination.COLLECTION },
                                { destination = RefreshDestination.SETTINGS },
                                { destination = RefreshDestination.SOCIAL },
                            )

                            RefreshDestination.COLLECTION -> PlayerCollectionScreen(backend) {
                                destination = RefreshDestination.PROFILE
                            }

                            RefreshDestination.LAST_LETTER -> OnlineGameScreenV6()
                            RefreshDestination.SIEGE -> WordSiegeExperienceScreen { leaveGame() }
                            RefreshDestination.LETTER_PATH -> LetterLadderGameScreen { leaveGame() }

                            RefreshDestination.SETTINGS -> MainSettingsScreen(
                                backend,
                                { destination = RefreshDestination.PROFILE },
                                { destination = RefreshDestination.ACCOUNT },
                                onSignedOut,
                            )

                            RefreshDestination.ACCOUNT -> CompleteProfileScreen(1) {
                                destination = RefreshDestination.SETTINGS
                            }

                            RefreshDestination.PROFILE_DETAILS -> CompleteProfileScreen(0) {
                                destination = RefreshDestination.PROFILE
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RefreshHomeScreen(
    onSiege: () -> Unit,
    onLastLetter: () -> Unit,
    onLetterPath: () -> Unit,
    onCompete: () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 620.dp).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "app_logo") {
                Image(
                    painter = painterResource(R.drawable.app_logo),
                    contentDescription = sh("Kelime Kuşatması logosu", "Kelime Kuşatması logo"),
                    modifier = Modifier.size(88.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            item(key = "kelime_kusatmasi") {
                RefreshModeBanner(
                    drawable = R.drawable.mode_kelime_kusatmasi,
                    description = sh("Kelime Kuşatması oyna", "Play Kelime Kuşatması"),
                    aspectRatio = 8f / 3f,
                    onClick = onSiege,
                )
            }
            item(key = "son_harf") {
                RefreshModeBanner(
                    drawable = R.drawable.mode_son_harf,
                    description = sh("Son Harf oyna", "Play Last Letter"),
                    aspectRatio = 3f,
                    onClick = onLastLetter,
                )
            }
            item(key = "kelime_yolu") {
                RefreshModeBanner(
                    drawable = R.drawable.mode_kelime_yolu,
                    description = sh("Kelime Yolu oyna", "Play Word Path"),
                    aspectRatio = 3f,
                    onClick = onLetterPath,
                )
            }
            item(key = "competition") {
                OutlinedButton(
                    onClick = onCompete,
                    modifier = Modifier.fillMaxWidth(.72f).height(44.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White.copy(alpha = .80f),
                        contentColor = SonHarfTheme.Primary,
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(SonHarfTheme.Border)
                    ),
                ) {
                    Icon(Icons.Rounded.EmojiEvents, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(sh("Rekabet Merkezi", "Competition Hub"), fontWeight = FontWeight.Bold)
                }
            }
            item { Spacer(Modifier.height(4.dp)) }
        }
    }
}

@Composable
private fun RefreshModeBanner(
    drawable: Int,
    description: String,
    aspectRatio: Float,
    onClick: () -> Unit,
) {
    Image(
        painter = painterResource(drawable),
        contentDescription = description,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(18.dp))
            .semantics { contentDescription = description }
            .clickable(onClick = onClick),
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun RefreshBottomBar(
    destination: RefreshDestination,
    onHome: () -> Unit,
    onClub: () -> Unit,
    onSocial: () -> Unit,
    onShop: () -> Unit,
    onProfile: () -> Unit,
) {
    NavigationBar(containerColor = Color.White.copy(alpha = .94f), tonalElevation = 4.dp) {
        NavigationBarItem(
            selected = destination == RefreshDestination.HOME,
            onClick = onHome,
            icon = { Icon(Icons.Rounded.Home, null) },
            label = { Text(sh("Ana Sayfa", "Home")) },
        )
        NavigationBarItem(
            selected = destination == RefreshDestination.CLUB,
            onClick = onClub,
            icon = { Icon(Icons.Rounded.Groups, null) },
            label = { Text(sh("Kulüp", "Club")) },
        )
        NavigationBarItem(
            selected = destination == RefreshDestination.SOCIAL,
            onClick = onSocial,
            icon = { Icon(Icons.Rounded.People, null) },
            label = { Text(sh("Sosyal", "Social")) },
        )
        NavigationBarItem(
            selected = destination == RefreshDestination.SHOP,
            onClick = onShop,
            icon = { Icon(Icons.Rounded.Storefront, null) },
            label = { Text(sh("Mağaza", "Store")) },
        )
        NavigationBarItem(
            selected = destination == RefreshDestination.PROFILE,
            onClick = onProfile,
            icon = { Icon(Icons.Rounded.Person, null) },
            label = { Text(sh("Profil", "Profile")) },
        )
    }
}
