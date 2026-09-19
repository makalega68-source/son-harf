package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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

private enum class PremiumV2Destination {
    HOME, SOCIAL, GAMES, SHOP, PROFILE,
    LAST_LETTER, SIEGE, LETTER_PATH, SERIES,
    COMPETE, COLLECTION, SETTINGS, ACCOUNT, PROFILE_DETAILS,
}

/**
 * Release-candidate shell for the polished premium UI.
 * Existing game engines, scoring, auth, purchases and server authority stay in their original screens.
 */
@Composable
fun PremiumCanvaAppV2(onSignedOut: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    var destination by remember { mutableStateOf(PremiumV2Destination.HOME) }
    var shopInitialTab by rememberSaveable { mutableIntStateOf(0) }
    var isPro by remember { mutableStateOf(false) }
    val homeRequest = SonHarfUiState.homeRequest
    val defaultGameLanguage = SharedDictionaryService.canonicalLanguage(SonHarfUiState.language)
    var siegeLanguage by rememberSaveable { mutableStateOf(defaultGameLanguage) }
    var lastLetterLanguage by rememberSaveable { mutableStateOf(defaultGameLanguage) }
    var letterPathLanguage by rememberSaveable { mutableStateOf(defaultGameLanguage) }
    var seriesLanguage by rememberSaveable { mutableStateOf(defaultGameLanguage) }
    var seriesAccess by remember { mutableStateOf(false) }
    var uiLanguageBeforeGame by rememberSaveable { mutableStateOf<String?>(null) }

    fun openGame(target: PremiumV2Destination, language: String) {
        if (uiLanguageBeforeGame == null) uiLanguageBeforeGame = SonHarfUiState.language
        SonHarfUiState.language = SharedDictionaryService.canonicalLanguage(language)
        destination = target
    }

    fun leaveGame(target: PremiumV2Destination = PremiumV2Destination.HOME) {
        uiLanguageBeforeGame?.let { SonHarfUiState.language = it }
        uiLanguageBeforeGame = null
        destination = target
    }

    fun openStore(tab: Int = 0) {
        shopInitialTab = tab.coerceIn(0, 3)
        destination = PremiumV2Destination.SHOP
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
        if (homeRequest > 0) leaveGame(PremiumV2Destination.HOME)
    }
    LaunchedEffect(destination) {
        if (destination !in setOf(PremiumV2Destination.LAST_LETTER, PremiumV2Destination.SIEGE, PremiumV2Destination.LETTER_PATH, PremiumV2Destination.SERIES)) {
            while (true) {
                runCatching { backend.setPresence("online") }
                delay(55_000)
            }
        }
    }

    BackHandler(enabled = destination != PremiumV2Destination.HOME) {
        destination = when (destination) {
            PremiumV2Destination.SETTINGS,
            PremiumV2Destination.PROFILE_DETAILS,
            PremiumV2Destination.COLLECTION -> PremiumV2Destination.PROFILE
            PremiumV2Destination.ACCOUNT -> PremiumV2Destination.SETTINGS
            PremiumV2Destination.LAST_LETTER,
            PremiumV2Destination.SIEGE,
            PremiumV2Destination.LETTER_PATH,
            PremiumV2Destination.SERIES -> {
                uiLanguageBeforeGame?.let { SonHarfUiState.language = it }
                uiLanguageBeforeGame = null
                PremiumV2Destination.HOME
            }
            else -> PremiumV2Destination.HOME
        }
    }

    val inGame = destination in setOf(PremiumV2Destination.LAST_LETTER, PremiumV2Destination.SIEGE, PremiumV2Destination.LETTER_PATH, PremiumV2Destination.SERIES)
    val topLevel = destination in setOf(PremiumV2Destination.HOME, PremiumV2Destination.SOCIAL, PremiumV2Destination.GAMES, PremiumV2Destination.SHOP, PremiumV2Destination.PROFILE)

    val scheme = lightColorScheme(
        primary = SonHarfTheme.Primary,
        onPrimary = Color.White,
        secondary = SonHarfTheme.Turquoise,
        onSecondary = Color.White,
        tertiary = SonHarfTheme.Purple,
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
                    PremiumV2BottomBar(
                        destination = destination,
                        onHome = { destination = PremiumV2Destination.HOME },
                        onSocial = { destination = PremiumV2Destination.SOCIAL },
                        onGames = { destination = PremiumV2Destination.GAMES },
                        onShop = { openStore(0) },
                        onProfile = { destination = PremiumV2Destination.PROFILE },
                    )
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                if (!inGame) SonHarfLeafBackdrop(Modifier.matchParentSize())
                when (destination) {
                    PremiumV2Destination.HOME -> PremiumV2Home(
                        backend = backend,
                        onSiege = { openGame(PremiumV2Destination.SIEGE, siegeLanguage) },
                        onLastLetter = { openGame(PremiumV2Destination.LAST_LETTER, lastLetterLanguage) },
                        onLetterPath = { openGame(PremiumV2Destination.LETTER_PATH, letterPathLanguage) },
                        onCompete = { destination = PremiumV2Destination.COMPETE },
                        onProfile = { destination = PremiumV2Destination.PROFILE },
                        onSocial = { destination = PremiumV2Destination.SOCIAL },
                        onPro = { openStore(3) },
                        onCollection = { destination = PremiumV2Destination.COLLECTION },
                    )
                    PremiumV2Destination.GAMES -> PremiumV2GameCenter(
                        siegeLanguage = siegeLanguage,
                        lastLetterLanguage = lastLetterLanguage,
                        letterPathLanguage = letterPathLanguage,
                        seriesLanguage = seriesLanguage,
                        seriesUnlocked = seriesAccess,
                        onSiegeLanguage = { siegeLanguage = it },
                        onLastLetterLanguage = { lastLetterLanguage = it },
                        onLetterPathLanguage = { letterPathLanguage = it },
                        onSeriesLanguage = { seriesLanguage = it },
                        onSiege = { openGame(PremiumV2Destination.SIEGE, siegeLanguage) },
                        onLastLetter = { openGame(PremiumV2Destination.LAST_LETTER, lastLetterLanguage) },
                        onLetterPath = { openGame(PremiumV2Destination.LETTER_PATH, letterPathLanguage) },
                        onSeries = { if (seriesAccess) openGame(PremiumV2Destination.SERIES, seriesLanguage) else openStore(0) },
                    )
                    PremiumV2Destination.SOCIAL -> MainSocialScreen(
                        backend = backend,
                        onPlay = { openGame(PremiumV2Destination.LAST_LETTER, lastLetterLanguage) },
                        onSiege = { openGame(PremiumV2Destination.SIEGE, siegeLanguage) },
                    )
                    PremiumV2Destination.SHOP -> PremiumStoreScreen(
                        initialTab = shopInitialTab,
                        onBack = { destination = PremiumV2Destination.HOME },
                        onMembershipChanged = { isPro = it },
                        onCollection = { destination = PremiumV2Destination.COLLECTION },
                    )
                    PremiumV2Destination.PROFILE -> MainPlayerProfileScreen(
                        backend,
                        { destination = PremiumV2Destination.PROFILE_DETAILS },
                        { openStore(3) },
                        { destination = PremiumV2Destination.COLLECTION },
                        { destination = PremiumV2Destination.SETTINGS },
                        { destination = PremiumV2Destination.SOCIAL },
                    )
                    PremiumV2Destination.COLLECTION -> PlayerCollectionScreen(backend) { destination = PremiumV2Destination.PROFILE }
                    PremiumV2Destination.SETTINGS -> MainSettingsScreen(
                        backend,
                        { destination = PremiumV2Destination.PROFILE },
                        { destination = PremiumV2Destination.ACCOUNT },
                        onSignedOut,
                    )
                    PremiumV2Destination.ACCOUNT -> CompleteProfileScreen(1) { destination = PremiumV2Destination.SETTINGS }
                    PremiumV2Destination.PROFILE_DETAILS -> CompleteProfileScreen(0) { destination = PremiumV2Destination.PROFILE }
                    PremiumV2Destination.COMPETE -> CompetitionHubScreen(onBack = { destination = PremiumV2Destination.HOME })
                    PremiumV2Destination.LAST_LETTER -> OnlineGameScreenV6()
                    PremiumV2Destination.SIEGE -> WordSiegeExperienceScreen { leaveGame() }
                    PremiumV2Destination.LETTER_PATH -> LetterLadderGameScreen { leaveGame() }
                    PremiumV2Destination.SERIES -> WordSiegeSeriesScreen { leaveGame(PremiumV2Destination.GAMES) }
                }
            }
        }
    }
}

@Composable
private fun PremiumV2Home(
    backend: OnlineGameBackend,
    onSiege: () -> Unit,
    onLastLetter: () -> Unit,
    onLetterPath: () -> Unit,
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
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            PremiumHomeCommandDeckPolished(
                profile = profile,
                onProfile = onProfile,
                onSiege = onSiege,
                onSocial = onSocial,
            )
        }
        item { PremiumV2SecondaryModes(onLastLetter, onLetterPath) }
        item { PremiumWeeklyBestPolished(onClick = onCompete) }
        item { PremiumV2QuickActions(onCompete, onPro, onCollection) }
    }
}

@Composable
private fun PremiumV2SecondaryModes(onLastLetter: () -> Unit, onLetterPath: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MainSectionTitle(sh("DİĞER OYUNLAR", "MORE GAMES"))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PremiumV2ModeTile(
                modifier = Modifier.weight(1f),
                iconRes = R.drawable.son_harf_app_icon_master,
                title = sh("SON HARF", "LAST LETTER"),
                subtitle = sh("Hızlı kelime düellosu", "Fast word duel"),
                accent = SonHarfTheme.ActionOrange,
                onClick = onLastLetter,
            )
            PremiumV2ModeTile(
                modifier = Modifier.weight(1f),
                iconRes = R.drawable.harf_yolu_logo,
                title = sh("HARF YOLU", "LETTER PATH"),
                subtitle = sh("Kelime rotanı tamamla", "Complete your word path"),
                accent = SonHarfTheme.Purple,
                onClick = onLetterPath,
            )
        }
    }
}

@Composable
private fun PremiumV2ModeTile(
    modifier: Modifier,
    iconRes: Int,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(176.dp),
        shape = MainUiShape.Card,
        color = SonHarfTheme.Surface,
        border = BorderStroke(1.dp, accent.copy(alpha = .22f)),
        shadowElevation = 4.dp,
    ) {
        Column(
            Modifier.fillMaxSize().padding(13.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = MainUiShape.Control, color = accent.copy(alpha = .10f)) {
                    Image(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier.padding(7.dp).size(44.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(9.dp).background(accent, CircleShape))
            }
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, color = SonHarfTheme.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = SonHarfTheme.TextSecondary, fontSize = 9.sp, lineHeight = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = accent.copy(alpha = .12f),
                    border = BorderStroke(1.dp, accent.copy(alpha = .16f)),
                ) {
                    Row(
                        Modifier.heightIn(min = 30.dp).padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(sh("OYNA", "PLAY"), color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black, maxLines = 1)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Rounded.PlayArrow, null, tint = accent, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumV2QuickActions(onCompete: () -> Unit, onPro: () -> Unit, onCollection: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MainSectionTitle(sh("HIZLI ERİŞİM", "QUICK ACCESS"))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            PremiumV2QuickAction(Modifier.weight(1f), Icons.Rounded.EmojiEvents, sh("Lig & Turnuva", "League & Events"), SonHarfTheme.Purple, onCompete)
            PremiumV2QuickAction(Modifier.weight(1f), Icons.Rounded.WorkspacePremium, sh("PRO Üyelik", "PRO Membership"), SonHarfTheme.Primary, onPro)
            PremiumV2QuickAction(Modifier.weight(1f), Icons.Rounded.Palette, sh("Koleksiyonum", "My Collection"), SonHarfTheme.Turquoise, onCollection)
        }
    }
}

@Composable
private fun PremiumV2QuickAction(modifier: Modifier, icon: ImageVector, label: String, accent: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 88.dp),
        shape = MainUiShape.Control,
        color = SonHarfTheme.Surface,
        border = BorderStroke(1.dp, accent.copy(alpha = .16f)),
        shadowElevation = 2.dp,
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(shape = CircleShape, color = accent.copy(alpha = .10f)) {
                Icon(icon, null, tint = accent, modifier = Modifier.padding(7.dp).size(21.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(label, color = SonHarfTheme.TextPrimary, fontSize = 8.sp, lineHeight = 11.sp, fontWeight = FontWeight.Black, maxLines = 2, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun PremiumV2GameCenter(
    siegeLanguage: String,
    lastLetterLanguage: String,
    letterPathLanguage: String,
    seriesLanguage: String,
    seriesUnlocked: Boolean,
    onSiegeLanguage: (String) -> Unit,
    onLastLetterLanguage: (String) -> Unit,
    onLetterPathLanguage: (String) -> Unit,
    onSeriesLanguage: (String) -> Unit,
    onSiege: () -> Unit,
    onLastLetter: () -> Unit,
    onLetterPath: () -> Unit,
    onSeries: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 640.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            MainScreenHeader(
                title = sh("Oyunlar", "Games"),
                subtitle = sh("Modunu ve dilini seç, doğrudan oyuna gir", "Choose your mode and language, then play"),
            )
        }
        item {
            PremiumV2GameCard(
                icon = Icons.Rounded.GridView,
                title = "KELİME KUŞATMASI",
                subtitle = sh("Ana oyun • taktik alan savaşı", "Main game • tactical territory battle"),
                language = siegeLanguage,
                onLanguageChange = onSiegeLanguage,
                accent = SonHarfTheme.Primary,
                primary = true,
                onClick = onSiege,
            )
        }
        item {
            PremiumV2GameCard(
                icon = Icons.Rounded.Bolt,
                title = sh("SON HARF", "LAST LETTER"),
                subtitle = sh("Hızlı ve rekabetçi kelime düellosu", "Fast competitive word duel"),
                language = lastLetterLanguage,
                onLanguageChange = onLastLetterLanguage,
                accent = SonHarfTheme.ActionOrange,
                onClick = onLastLetter,
            )
        }
        item {
            PremiumV2GameCard(
                icon = Icons.Rounded.Route,
                title = sh("HARF YOLU", "LETTER PATH"),
                subtitle = sh("Beş adımda kelime rotanı tamamla", "Complete your word route in five moves"),
                language = letterPathLanguage,
                onLanguageChange = onLetterPathLanguage,
                accent = SonHarfTheme.Purple,
                onClick = onLetterPath,
            )
        }
        item {
            PremiumV2GameCard(
                icon = if (seriesUnlocked) Icons.Rounded.Timer else Icons.Rounded.Lock,
                title = sh("SERİ OYUN", "SERIES GAME"),
                subtitle = if (seriesUnlocked) sh("3 / 5 / 10 dakikalık premium hızlı mod", "Premium fast mode with 3 / 5 / 10 minute turns") else sh("Premium mod • satın al veya PRO ile aç", "Premium mode • buy it or unlock with PRO"),
                language = seriesLanguage,
                onLanguageChange = onSeriesLanguage,
                accent = SonHarfTheme.ActionOrange,
                onClick = onSeries,
            )
        }
    }
}

@Composable
private fun PremiumV2GameCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    language: String,
    onLanguageChange: (String) -> Unit,
    accent: Color,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        shape = MainUiShape.Hero,
        color = SonHarfTheme.Surface,
        border = BorderStroke(if (primary) 1.5.dp else 1.dp, accent.copy(alpha = if (primary) .42f else .20f)),
        shadowElevation = if (primary) 7.dp else 4.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = MainUiShape.Card, color = accent.copy(alpha = .11f)) {
                    Icon(icon, null, tint = accent, modifier = Modifier.padding(13.dp).size(28.dp))
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    if (primary) {
                        PremiumAccentPill(sh("ANA OYUN", "MAIN GAME"), SonHarfTheme.Turquoise)
                        Spacer(Modifier.height(6.dp))
                    }
                    Text(title, color = SonHarfTheme.TextPrimary, fontSize = if (primary) 18.sp else 16.sp, fontWeight = FontWeight.Black)
                    Text(subtitle, color = SonHarfTheme.TextSecondary, fontSize = 10.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                PremiumV2LanguageChoice(language, onLanguageChange, Modifier.weight(1f), accent)
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = onClick,
                    modifier = Modifier.heightIn(min = 44.dp),
                    shape = MainUiShape.Control,
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(sh("OYNA", "PLAY"), fontWeight = FontWeight.Black, fontSize = 10.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun PremiumV2LanguageChoice(language: String, onLanguageChange: (String) -> Unit, modifier: Modifier = Modifier, accent: Color) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        listOf("tr" to "TR", "en" to "EN").forEach { (code, label) ->
            FilterChip(
                selected = language == code,
                onClick = { onLanguageChange(code) },
                label = { Text(label, fontWeight = FontWeight.Black, fontSize = 9.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accent.copy(alpha = .12f),
                    selectedLabelColor = accent,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = language == code,
                    borderColor = SonHarfTheme.Border,
                    selectedBorderColor = accent.copy(alpha = .55f),
                ),
            )
        }
    }
}

@Composable
private fun PremiumV2BottomBar(
    destination: PremiumV2Destination,
    onHome: () -> Unit,
    onSocial: () -> Unit,
    onGames: () -> Unit,
    onShop: () -> Unit,
    onProfile: () -> Unit,
) {
    Surface(color = SonHarfTheme.NavigationSurface, shadowElevation = 10.dp) {
        NavigationBar(containerColor = Color.Transparent, tonalElevation = 0.dp) {
            PremiumV2NavItem(destination == PremiumV2Destination.HOME, Icons.Rounded.Home, sh("ANA SAYFA", "HOME"), onHome)
            PremiumV2NavItem(destination == PremiumV2Destination.SOCIAL, Icons.Rounded.Groups, sh("SOSYAL", "SOCIAL"), onSocial)
            PremiumV2NavItem(destination == PremiumV2Destination.GAMES, Icons.Rounded.PlayCircle, sh("OYNA", "PLAY"), onGames, emphasized = true)
            PremiumV2NavItem(destination == PremiumV2Destination.SHOP, Icons.Rounded.Storefront, sh("MAĞAZA", "STORE"), onShop)
            PremiumV2NavItem(destination == PremiumV2Destination.PROFILE, Icons.Rounded.Person, sh("PROFİL", "PROFILE"), onProfile)
        }
    }
}

@Composable
private fun RowScope.PremiumV2NavItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    emphasized: Boolean = false,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            if (emphasized) {
                Surface(shape = CircleShape, color = SonHarfTheme.Primary, shadowElevation = 4.dp) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.padding(8.dp).size(23.dp))
                }
            } else {
                Icon(icon, null)
            }
        },
        label = { Text(label, fontSize = 8.sp, fontWeight = if (selected || emphasized) FontWeight.Black else FontWeight.Medium) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = SonHarfTheme.Primary,
            selectedTextColor = SonHarfTheme.Primary,
            indicatorColor = if (emphasized) Color.Transparent else SonHarfTheme.PrimarySoft,
            unselectedIconColor = SonHarfTheme.TextSecondary,
            unselectedTextColor = SonHarfTheme.TextSecondary,
        ),
    )
}
