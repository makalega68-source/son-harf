package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
 * Active shell for the APK-v2 line. It keeps the original server-authoritative game, economy,
 * profile and social screens, but replaces the casual/rainbow navigation layer with a restrained
 * four-tab word-game shell.
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

    val scheme = darkColorScheme(
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

    MaterialTheme(colorScheme = scheme, typography = AppTypography) {
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
            Box(Modifier.fillMaxSize().padding(padding).imePadding()) {
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
            icon = Icons.Rounded.GridView,
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
                icon = Icons.Rounded.Bolt,
                title = sh("Son Harf", "Last Letter"),
                subtitle = sh("Hızlı 1v1 düello", "Fast 1v1 duel"),
                language = lastLetterLanguage,
                onLanguage = onLastLetterLanguage,
                onClick = onLastLetter,
            )
            AdultModeCompact(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Abc,
                title = sh("Harf Yolu", "Letter Path"),
                subtitle = sh("5 harfli rota", "Five-letter route"),
                language = letterPathLanguage,
                onLanguage = onLetterPathLanguage,
                onClick = onLetterPath,
            )
        }
        if (seriesAccess) {
            AdultModeRow(
                icon = Icons.Rounded.MilitaryTech,
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
    icon: ImageVector,
    title: String,
    subtitle: String,
    language: String,
    onLanguage: (String) -> Unit,
    primary: Boolean,
    onClick: () -> Unit,
) {
    val accent = if (primary) SonHarfTheme.Primary else SonHarfTheme.SoftBlue
    Surface(
        onClick = onClick,
        shape = MainUiShape.Card,
        color = SonHarfTheme.Surface,
        border = BorderStroke(1.dp, if (primary) accent.copy(alpha = .32f) else SonHarfTheme.Border),
        shadowElevation = 0.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = RoundedCornerShape(12.dp), color = accent.copy(alpha = .10f)) {
                Icon(icon, null, tint = accent, modifier = Modifier.padding(10.dp).size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = SonHarfTheme.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Text(subtitle, color = SonHarfTheme.TextSecondary, fontSize = 10.sp)
            }
            AdultLanguageSwitch(language, onLanguage)
            Spacer(Modifier.width(7.dp))
            Icon(Icons.Rounded.PlayArrow, null, tint = accent, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun AdultModeCompact(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    language: String,
    onLanguage: (String) -> Unit,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 138.dp),
        shape = MainUiShape.Card,
        color = SonHarfTheme.Surface,
        border = BorderStroke(1.dp, SonHarfTheme.Border),
        shadowElevation = 0.dp,
    ) {
        Column(
            Modifier.fillMaxSize().padding(13.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(10.dp), color = SonHarfTheme.SurfaceSecondary) {
                    Icon(icon, null, tint = SonHarfTheme.SoftBlue, modifier = Modifier.padding(8.dp).size(20.dp))
                }
                Spacer(Modifier.weight(1f))
                AdultLanguageSwitch(language, onLanguage)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = SonHarfTheme.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = SonHarfTheme.TextSecondary, fontSize = 9.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(sh("OYNA", "PLAY"), color = SonHarfTheme.Primary, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(3.dp))
                    Icon(Icons.Rounded.PlayArrow, null, tint = SonHarfTheme.Primary, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
private fun AdultLanguageSwitch(language: String, onLanguage: (String) -> Unit) {
    Surface(shape = RoundedCornerShape(99.dp), color = SonHarfTheme.SurfaceSecondary) {
        Row(Modifier.padding(2.dp)) {
            listOf("tr" to "TR", "en" to "EN").forEach { (code, label) ->
                Surface(
                    onClick = { onLanguage(code) },
                    shape = RoundedCornerShape(99.dp),
                    color = if (language == code) SonHarfTheme.Primary else Color.Transparent,
                ) {
                    Text(
                        label,
                        Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                        color = if (language == code) Color.White else SonHarfTheme.TextSecondary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

@Composable
private fun AdultQuickActions(onCompete: () -> Unit, onPro: () -> Unit, onCollection: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MainSectionTitle(sh("HIZLI ERİŞİM", "QUICK ACCESS"))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdultQuickAction(Modifier.weight(1f), Icons.Rounded.EmojiEvents, sh("Lig", "League"), onCompete)
            AdultQuickAction(Modifier.weight(1f), Icons.Rounded.WorkspacePremium, "PRO", onPro)
            AdultQuickAction(Modifier.weight(1f), Icons.Rounded.CollectionsBookmark, sh("Koleksiyon", "Collection"), onCollection)
        }
    }
}

@Composable
private fun AdultQuickAction(modifier: Modifier, icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(13.dp),
        color = SonHarfTheme.Surface,
        border = BorderStroke(1.dp, SonHarfTheme.Border),
        shadowElevation = 0.dp,
    ) {
        Column(
            Modifier.padding(horizontal = 8.dp, vertical = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (label == "Lig" || label == "League") {
                PackageLeagueBadge(Modifier.size(22.dp))
            } else {
                Icon(icon, null, tint = SonHarfTheme.Primary, modifier = Modifier.size(20.dp))
            }
            Text(label, color = SonHarfTheme.TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
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
    NavigationBar(
        modifier = Modifier.height(64.dp),
        containerColor = SonHarfTheme.NavigationSurface,
        tonalElevation = 0.dp,
        windowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        AdultNavItem(destination == AdultDestination.HOME, Icons.Rounded.Home, sh("Ana Sayfa", "Home"), onHome)
        AdultNavItem(destination == AdultDestination.SOCIAL, Icons.Rounded.Groups, sh("Sosyal", "Social"), onSocial)
        AdultNavItem(destination == AdultDestination.SHOP, Icons.Rounded.Storefront, sh("Mağaza", "Shop"), onShop)
        AdultNavItem(destination == AdultDestination.PROFILE, Icons.Rounded.Person, sh("Profil", "Profile"), onProfile)
    }
}

@Composable
private fun RowScope.AdultNavItem(selected: Boolean, icon: ImageVector, label: String, onClick: () -> Unit) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, null, modifier = Modifier.size(21.dp)) },
        label = { Text(label, fontSize = 9.sp, fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = SonHarfTheme.Primary,
            selectedTextColor = SonHarfTheme.Primary,
            indicatorColor = SonHarfTheme.PrimarySoft,
            unselectedIconColor = SonHarfTheme.TextSecondary,
            unselectedTextColor = SonHarfTheme.TextSecondary,
        ),
    )
}
