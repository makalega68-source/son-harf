package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SharedDictionaryService
import com.sonharf.game.data.getEquippedCosmetics
import com.sonharf.game.data.getInventory
import com.sonharf.game.data.getVipEntitlements
import kotlinx.coroutines.delay

private enum class AdultDestination {
    HOME, SOCIAL, SHOP, PROFILE,
    LAST_LETTER, SIEGE, LETTER_PATH, SERIES,
    COMPETE, COLLECTION, SETTINGS, ACCOUNT, PROFILE_DETAILS,
}

/**
 * Active shell for the verified APK-v2 source line.
 * Server-authoritative game, economy, profile and social flows are preserved; only visual chrome
 * is supplied by the real purchased game-UI asset layer.
 */
@Composable
fun PremiumAdultApp(onSignedOut: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    var destination by remember { mutableStateOf(AdultDestination.HOME) }
    var shopInitialTab by rememberSaveable { mutableIntStateOf(0) }
    var isPro by remember { mutableStateOf(false) }
    var seriesAccess by remember { mutableStateOf(false) }
    val defaultLanguage = SharedDictionaryService.canonicalLanguage(SonHarfUiState.language)
    var siegeLanguage by rememberSaveable { mutableStateOf(defaultLanguage) }
    var lastLetterLanguage by rememberSaveable { mutableStateOf(defaultLanguage) }
    var letterPathLanguage by rememberSaveable { mutableStateOf(defaultLanguage) }
    var seriesLanguage by rememberSaveable { mutableStateOf(defaultLanguage) }
    var uiLanguageBeforeGame by rememberSaveable { mutableStateOf<String?>(null) }
    val homeRequest = SonHarfUiState.homeRequest

    fun openGame(target: AdultDestination, language: String) {
        if (uiLanguageBeforeGame == null) uiLanguageBeforeGame = SonHarfUiState.language
        SonHarfUiState.language = SharedDictionaryService.canonicalLanguage(language)
        destination = target
    }

    fun leaveGame(target: AdultDestination = AdultDestination.HOME) {
        uiLanguageBeforeGame?.let { SonHarfUiState.language = it }
        uiLanguageBeforeGame = null
        destination = target
    }

    fun openStore(tab: Int = 0) {
        shopInitialTab = tab.coerceIn(0, 3)
        destination = AdultDestination.SHOP
    }

    LaunchedEffect(Unit) {
        backend.currentUserId()?.let {
            val owned = runCatching { backend.getInventory() }.getOrDefault(emptySet())
            val equipped = runCatching { backend.getEquippedCosmetics() }.getOrNull()
            SonHarfCosmetics.apply(equipped, owned)
            val premium = runCatching { backend.getVipEntitlements() }.getOrNull()
            isPro = premium?.isPro == true
            seriesAccess = premium?.seriesGameAccess == true
        }
    }

    LaunchedEffect(homeRequest) {
        if (homeRequest > 0) leaveGame()
    }

    LaunchedEffect(destination) {
        if (destination !in setOf(AdultDestination.LAST_LETTER, AdultDestination.SIEGE, AdultDestination.LETTER_PATH, AdultDestination.SERIES)) {
            while (true) {
                runCatching { backend.setPresence("online") }
                delay(55_000)
            }
        }
    }

    BackHandler(enabled = destination != AdultDestination.HOME) {
        destination = when (destination) {
            AdultDestination.SETTINGS,
            AdultDestination.PROFILE_DETAILS,
            AdultDestination.COLLECTION -> AdultDestination.PROFILE
            AdultDestination.ACCOUNT -> AdultDestination.SETTINGS
            AdultDestination.LAST_LETTER,
            AdultDestination.SIEGE,
            AdultDestination.LETTER_PATH,
            AdultDestination.SERIES -> {
                uiLanguageBeforeGame?.let { SonHarfUiState.language = it }
                uiLanguageBeforeGame = null
                AdultDestination.HOME
            }
            else -> AdultDestination.HOME
        }
    }

    val inGame = destination in setOf(AdultDestination.LAST_LETTER, AdultDestination.SIEGE, AdultDestination.LETTER_PATH, AdultDestination.SERIES)
    val topLevel = destination in setOf(AdultDestination.HOME, AdultDestination.SOCIAL, AdultDestination.SHOP, AdultDestination.PROFILE)

    val scheme = lightColorScheme(
        primary = SonHarfTheme.Primary,
        onPrimary = SonHarfTheme.OnPrimary,
        secondary = SonHarfTheme.SoftBlue,
        onSecondary = Color.White,
        tertiary = SonHarfTheme.ActionOrange,
        onTertiary = Color.White,
        background = SonHarfTheme.Background,
        onBackground = SonHarfTheme.TextPrimary,
        surface = SonHarfTheme.Surface,
        onSurface = SonHarfTheme.TextPrimary,
        surfaceVariant = SonHarfTheme.SurfaceSecondary,
        onSurfaceVariant = SonHarfTheme.TextSecondary,
        outline = SonHarfTheme.Border,
        error = SonHarfTheme.Error,
    )

    MaterialTheme(colorScheme = scheme) {
        Scaffold(
            containerColor = SonHarfTheme.Background,
            topBar = { if (!inGame) SonHarfTopAdBanner(isPremium = isPro) },
            bottomBar = {
                if (topLevel) {
                    AdultBottomBar(
                        destination = destination,
                        onHome = { destination = AdultDestination.HOME },
                        onSocial = { destination = AdultDestination.SOCIAL },
                        onShop = { openStore(0) },
                        onProfile = { destination = AdultDestination.PROFILE },
                    )
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                if (!inGame) PremiumScreenBackground(Modifier.matchParentSize())
                when (destination) {
                    AdultDestination.HOME -> AdultHome(
                        backend = backend,
                        siegeLanguage = siegeLanguage,
                        lastLetterLanguage = lastLetterLanguage,
                        letterPathLanguage = letterPathLanguage,
                        seriesLanguage = seriesLanguage,
                        seriesAccess = seriesAccess,
                        onSiegeLanguage = { siegeLanguage = it },
                        onLastLetterLanguage = { lastLetterLanguage = it },
                        onLetterPathLanguage = { letterPathLanguage = it },
                        onSeriesLanguage = { seriesLanguage = it },
                        onSiege = { openGame(AdultDestination.SIEGE, siegeLanguage) },
                        onLastLetter = { openGame(AdultDestination.LAST_LETTER, lastLetterLanguage) },
                        onLetterPath = { openGame(AdultDestination.LETTER_PATH, letterPathLanguage) },
                        onSeries = { if (seriesAccess) openGame(AdultDestination.SERIES, seriesLanguage) else openStore(3) },
                        onCompete = { destination = AdultDestination.COMPETE },
                        onProfile = { destination = AdultDestination.PROFILE },
                        onSocial = { destination = AdultDestination.SOCIAL },
                        onPro = { openStore(3) },
                        onCollection = { destination = AdultDestination.COLLECTION },
                    )
                    AdultDestination.SOCIAL -> MainSocialScreen(
                        backend = backend,
                        onPlay = { openGame(AdultDestination.LAST_LETTER, lastLetterLanguage) },
                        onSiege = { openGame(AdultDestination.SIEGE, siegeLanguage) },
                    )
                    AdultDestination.SHOP -> PremiumStoreScreen(
                        initialTab = shopInitialTab,
                        onBack = { destination = AdultDestination.HOME },
                        onMembershipChanged = { isPro = it },
                        onCollection = { destination = AdultDestination.COLLECTION },
                    )
                    AdultDestination.PROFILE -> MainPlayerProfileScreen(
                        backend,
                        { destination = AdultDestination.PROFILE_DETAILS },
                        { openStore(3) },
                        { destination = AdultDestination.COLLECTION },
                        { destination = AdultDestination.SETTINGS },
                        { destination = AdultDestination.SOCIAL },
                    )
                    AdultDestination.COLLECTION -> PlayerCollectionScreen(backend) { destination = AdultDestination.PROFILE }
                    AdultDestination.SETTINGS -> MainSettingsScreen(
                        backend,
                        { destination = AdultDestination.PROFILE },
                        { destination = AdultDestination.ACCOUNT },
                        onSignedOut,
                    )
                    AdultDestination.ACCOUNT -> CompleteProfileScreen(1) { destination = AdultDestination.SETTINGS }
                    AdultDestination.PROFILE_DETAILS -> CompleteProfileScreen(0) { destination = AdultDestination.PROFILE }
                    AdultDestination.COMPETE -> CompetitionHubScreen(onBack = { destination = AdultDestination.HOME })
                    AdultDestination.LAST_LETTER -> OnlineGameScreenV6()
                    AdultDestination.SIEGE -> WordSiegeExperienceScreen { leaveGame() }
                    AdultDestination.LETTER_PATH -> LetterLadderGameScreen { leaveGame() }
                    AdultDestination.SERIES -> WordSiegeSeriesScreen { leaveGame() }
                }
            }
        }
    }
}

@Composable
private fun AdultHome(
    backend: OnlineGameBackend,
    siegeLanguage: String,
    lastLetterLanguage: String,
    letterPathLanguage: String,
    seriesLanguage: String,
    seriesAccess: Boolean,
    onSiegeLanguage: (String) -> Unit,
    onLastLetterLanguage: (String) -> Unit,
    onLetterPathLanguage: (String) -> Unit,
    onSeriesLanguage: (String) -> Unit,
    onSiege: () -> Unit,
    onLastLetter: () -> Unit,
    onLetterPath: () -> Unit,
    onSeries: () -> Unit,
    onCompete: () -> Unit,
    onProfile: () -> Unit,
    onSocial: () -> Unit,
    onPro: () -> Unit,
    onCollection: () -> Unit,
) {
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    LaunchedEffect(Unit) {
        profile = backend.currentUserId()?.let { id -> runCatching { backend.getProfile(id) }.getOrNull() }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 640.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            PremiumHomeCommandDeckPolished(
                profile = profile,
                onProfile = onProfile,
                onSiege = onSiege,
                onSocial = onSocial,
            )
        }
        item {
            AdultModesSection(
                siegeLanguage = siegeLanguage,
                lastLetterLanguage = lastLetterLanguage,
                letterPathLanguage = letterPathLanguage,
                seriesLanguage = seriesLanguage,
                seriesAccess = seriesAccess,
                onSiegeLanguage = onSiegeLanguage,
                onLastLetterLanguage = onLastLetterLanguage,
                onLetterPathLanguage = onLetterPathLanguage,
                onSeriesLanguage = onSeriesLanguage,
                onSiege = onSiege,
                onLastLetter = onLastLetter,
                onLetterPath = onLetterPath,
                onSeries = onSeries,
            )
        }
        item { PremiumWeeklyBestPolished(onClick = onCompete) }
        item { AdultQuickActions(onCompete, onPro, onCollection) }
        item { Spacer(Modifier.height(4.dp)) }
    }
}

@Composable
private fun AdultModesSection(
    siegeLanguage: String,
    lastLetterLanguage: String,
    letterPathLanguage: String,
    seriesLanguage: String,
    seriesAccess: Boolean,
    onSiegeLanguage: (String) -> Unit,
    onLastLetterLanguage: (String) -> Unit,
    onLetterPathLanguage: (String) -> Unit,
    onSeriesLanguage: (String) -> Unit,
    onSiege: () -> Unit,
    onLastLetter: () -> Unit,
    onLetterPath: () -> Unit,
    onSeries: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MainSectionTitle(sh("OYUN MODLARI", "GAME MODES"))
        AdultModeRow(
            asset = PurchasedUiAsset.ICON_SWORDS,
            title = sh("Kelime Kuşatması", "Word Siege"),
            subtitle = sh("Taktik alan savaşı", "Tactical territory battle"),
            language = siegeLanguage,
            onLanguage = onSiegeLanguage,
            primary = true,
            onClick = onSiege,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AdultModeCompact(
                modifier = Modifier.weight(1f),
                asset = PurchasedUiAsset.ICON_REPEAT,
                title = sh("Son Harf", "Last Letter"),
                subtitle = sh("Hızlı 1v1 düello", "Fast 1v1 duel"),
                language = lastLetterLanguage,
                onLanguage = onLastLetterLanguage,
                onClick = onLastLetter,
            )
            AdultModeCompact(
                modifier = Modifier.weight(1f),
                asset = PurchasedUiAsset.ICON_GAMES,
                title = sh("Harf Yolu", "Letter Path"),
                subtitle = sh("5 harfli rota", "Five-letter route"),
                language = letterPathLanguage,
                onLanguage = onLetterPathLanguage,
                onClick = onLetterPath,
            )
        }
        if (seriesAccess) {
            AdultModeRow(
                asset = PurchasedUiAsset.ICON_TROPHY,
                title = sh("Seri Oyun", "Series"),
                subtitle = sh("PRO seri karşılaşmaları", "PRO series matches"),
                language = seriesLanguage,
                onLanguage = onSeriesLanguage,
                primary = false,
                onClick = onSeries,
            )
        }
    }
}

@Composable
private fun AdultModeRow(
    asset: PurchasedUiAsset,
    title: String,
    subtitle: String,
    language: String,
    onLanguage: (String) -> Unit,
    primary: Boolean,
    onClick: () -> Unit,
) {
    PurchasedPanel(
        modifier = Modifier.fillMaxWidth().heightIn(min = if (primary) 150.dp else 126.dp),
        asset = if (primary) PurchasedUiAsset.PANEL_LARGE else PurchasedUiAsset.PANEL_MEDIUM,
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 20.dp),
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PurchasedAsset(asset, Modifier.size(if (primary) 64.dp else 52.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        color = Color(0xFF563A2A),
                        fontSize = if (primary) 20.sp else 16.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(subtitle, color = Color(0xFF7D5C47), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                AdultLanguageSwitch(language, onLanguage)
            }
            PurchasedButton(
                text = sh("OYNA", "PLAY"),
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                style = if (primary) PurchasedButtonStyle.PRIMARY else PurchasedButtonStyle.SECONDARY,
                leadingAsset = if (primary) PurchasedUiAsset.ICON_SWORDS else null,
            )
        }
    }
}

@Composable
private fun AdultModeCompact(
    modifier: Modifier,
    asset: PurchasedUiAsset,
    title: String,
    subtitle: String,
    language: String,
    onLanguage: (String) -> Unit,
    onClick: () -> Unit,
) {
    PurchasedPanel(
        modifier = modifier.heightIn(min = 190.dp),
        asset = PurchasedUiAsset.PANEL_SMALL,
        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 16.dp),
    ) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            PurchasedAsset(asset, Modifier.size(48.dp))
            Text(
                title,
                color = Color(0xFF563A2A),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                subtitle,
                color = Color(0xFF7D5C47),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            AdultLanguageSwitch(language, onLanguage)
            PurchasedButton(
                text = sh("OYNA", "PLAY"),
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                style = PurchasedButtonStyle.SECONDARY,
            )
        }
    }
}

@Composable
private fun AdultLanguageSwitch(language: String, onLanguage: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf("tr" to "TR", "en" to "EN").forEach { (code, label) ->
            PurchasedButton(
                text = label,
                onClick = { onLanguage(code) },
                modifier = Modifier.width(48.dp).height(38.dp),
                style = if (language == code) PurchasedButtonStyle.PURPLE else PurchasedButtonStyle.SECONDARY,
            )
        }
    }
}

@Composable
private fun AdultQuickActions(onCompete: () -> Unit, onPro: () -> Unit, onCollection: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MainSectionTitle(sh("HIZLI ERİŞİM", "QUICK ACCESS"))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdultQuickAction(Modifier.weight(1f), PurchasedUiAsset.ICON_RANKING, sh("Lig", "League"), onCompete)
            AdultQuickAction(Modifier.weight(1f), PurchasedUiAsset.ICON_CROWN, "PRO", onPro)
            AdultQuickAction(Modifier.weight(1f), PurchasedUiAsset.ICON_GIFT, sh("Koleksiyon", "Collection"), onCollection)
        }
    }
}

@Composable
private fun AdultQuickAction(
    modifier: Modifier,
    asset: PurchasedUiAsset,
    label: String,
    onClick: () -> Unit,
) {
    PurchasedPanel(
        modifier = modifier.heightIn(min = 116.dp),
        asset = PurchasedUiAsset.PANEL_SMALL,
        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 12.dp),
    ) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            PurchasedAsset(asset, Modifier.size(44.dp))
            Text(label, color = Color(0xFF563A2A), fontSize = 10.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = 1)
            PurchasedButton(
                text = sh("AÇ", "OPEN"),
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().height(42.dp),
                style = PurchasedButtonStyle.PURPLE,
            )
        }
    }
}

@Composable
private fun AdultBottomBar(
    destination: AdultDestination,
    onHome: () -> Unit,
    onSocial: () -> Unit,
    onShop: () -> Unit,
    onProfile: () -> Unit,
) {
    PurchasedPanel(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .heightIn(min = 88.dp),
        asset = PurchasedUiAsset.PANEL_MEDIUM,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PurchasedNavItem(
                label = sh("Ana Sayfa", "Home"),
                icon = PurchasedUiAsset.NAV_HOME,
                selected = destination == AdultDestination.HOME,
                onClick = onHome,
                modifier = Modifier.weight(1f),
            )
            PurchasedNavItem(
                label = sh("Sosyal", "Social"),
                icon = PurchasedUiAsset.NAV_SOCIAL,
                selected = destination == AdultDestination.SOCIAL,
                onClick = onSocial,
                modifier = Modifier.weight(1f),
            )
            PurchasedNavItem(
                label = sh("Mağaza", "Shop"),
                icon = PurchasedUiAsset.NAV_SHOP,
                selected = destination == AdultDestination.SHOP,
                onClick = onShop,
                modifier = Modifier.weight(1f),
            )
            PurchasedNavItem(
                label = sh("Profil", "Profile"),
                icon = PurchasedUiAsset.NAV_PROFILE,
                selected = destination == AdultDestination.PROFILE,
                onClick = onProfile,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
