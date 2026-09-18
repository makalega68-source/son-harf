package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SharedDictionaryService
import com.sonharf.game.data.SupabaseProvider
import kotlinx.coroutines.delay

private enum class PremiumCanvaDestination {
    HOME, SOCIAL, GAMES, SHOP, PROFILE,
    LAST_LETTER, SIEGE, LETTER_PATH,
    COMPETE, COLLECTION, SETTINGS, ACCOUNT, PROFILE_DETAILS,
}

/**
 * Publish-candidate application shell for the approved Canva direction.
 *
 * Only presentation and navigation composition live here. Existing game engines, backend calls,
 * scoring, purchases and account behavior remain in their established screens.
 */
@Composable
fun PremiumCanvaApp(onSignedOut: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    var destination by remember { mutableStateOf(PremiumCanvaDestination.HOME) }
    var isPro by remember { mutableStateOf(false) }
    val homeRequest = SonHarfUiState.homeRequest
    val defaultGameLanguage = SharedDictionaryService.canonicalLanguage(SonHarfUiState.language)
    var siegeLanguage by rememberSaveable { mutableStateOf(defaultGameLanguage) }
    var lastLetterLanguage by rememberSaveable { mutableStateOf(defaultGameLanguage) }
    var letterPathLanguage by rememberSaveable { mutableStateOf(defaultGameLanguage) }
    var uiLanguageBeforeGame by rememberSaveable { mutableStateOf<String?>(null) }

    fun openGame(target: PremiumCanvaDestination, language: String) {
        if (uiLanguageBeforeGame == null) uiLanguageBeforeGame = SonHarfUiState.language
        SonHarfUiState.language = SharedDictionaryService.canonicalLanguage(language)
        destination = target
    }

    fun leaveGame(target: PremiumCanvaDestination = PremiumCanvaDestination.HOME) {
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
        if (homeRequest > 0) leaveGame(PremiumCanvaDestination.HOME)
    }
    LaunchedEffect(destination) {
        if (destination !in setOf(
                PremiumCanvaDestination.LAST_LETTER,
                PremiumCanvaDestination.SIEGE,
                PremiumCanvaDestination.LETTER_PATH,
            )
        ) {
            while (true) {
                runCatching { backend.setPresence("online") }
                delay(55_000)
            }
        }
    }

    BackHandler(enabled = destination != PremiumCanvaDestination.HOME) {
        destination = when (destination) {
            PremiumCanvaDestination.SETTINGS,
            PremiumCanvaDestination.PROFILE_DETAILS,
            PremiumCanvaDestination.COLLECTION -> PremiumCanvaDestination.PROFILE
            PremiumCanvaDestination.ACCOUNT -> PremiumCanvaDestination.SETTINGS
            PremiumCanvaDestination.LAST_LETTER,
            PremiumCanvaDestination.SIEGE,
            PremiumCanvaDestination.LETTER_PATH -> {
                uiLanguageBeforeGame?.let { SonHarfUiState.language = it }
                uiLanguageBeforeGame = null
                PremiumCanvaDestination.HOME
            }
            else -> PremiumCanvaDestination.HOME
        }
    }

    val inGame = destination in setOf(
        PremiumCanvaDestination.LAST_LETTER,
        PremiumCanvaDestination.SIEGE,
        PremiumCanvaDestination.LETTER_PATH,
    )
    val topLevel = destination in setOf(
        PremiumCanvaDestination.HOME,
        PremiumCanvaDestination.SOCIAL,
        PremiumCanvaDestination.GAMES,
        PremiumCanvaDestination.SHOP,
        PremiumCanvaDestination.PROFILE,
    )

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
                    PremiumCanvaBottomBar(
                        destination = destination,
                        onHome = { destination = PremiumCanvaDestination.HOME },
                        onSocial = { destination = PremiumCanvaDestination.SOCIAL },
                        onGames = { destination = PremiumCanvaDestination.GAMES },
                        onShop = { destination = PremiumCanvaDestination.SHOP },
                        onProfile = { destination = PremiumCanvaDestination.PROFILE },
                    )
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                if (!inGame) SonHarfLeafBackdrop(Modifier.matchParentSize())
                when (destination) {
                    PremiumCanvaDestination.HOME -> PremiumCanvaHome(
                        backend = backend,
                        onSiege = { openGame(PremiumCanvaDestination.SIEGE, siegeLanguage) },
                        onLastLetter = { openGame(PremiumCanvaDestination.LAST_LETTER, lastLetterLanguage) },
                        onLetterPath = { openGame(PremiumCanvaDestination.LETTER_PATH, letterPathLanguage) },
                        onCompete = { destination = PremiumCanvaDestination.COMPETE },
                        onProfile = { destination = PremiumCanvaDestination.PROFILE },
                        onSocial = { destination = PremiumCanvaDestination.SOCIAL },
                        onShop = { destination = PremiumCanvaDestination.SHOP },
                    )
                    PremiumCanvaDestination.GAMES -> PremiumCanvaGameCenter(
                        siegeLanguage = siegeLanguage,
                        lastLetterLanguage = lastLetterLanguage,
                        letterPathLanguage = letterPathLanguage,
                        onSiegeLanguage = { siegeLanguage = it },
                        onLastLetterLanguage = { lastLetterLanguage = it },
                        onLetterPathLanguage = { letterPathLanguage = it },
                        onSiege = { openGame(PremiumCanvaDestination.SIEGE, siegeLanguage) },
                        onLastLetter = { openGame(PremiumCanvaDestination.LAST_LETTER, lastLetterLanguage) },
                        onLetterPath = { openGame(PremiumCanvaDestination.LETTER_PATH, letterPathLanguage) },
                    )
                    PremiumCanvaDestination.SOCIAL -> MainSocialScreen(
                        backend = backend,
                        onPlay = { openGame(PremiumCanvaDestination.LAST_LETTER, lastLetterLanguage) },
                        onSiege = { openGame(PremiumCanvaDestination.SIEGE, siegeLanguage) },
                    )
                    PremiumCanvaDestination.SHOP -> EconomyShopScreen(
                        onBack = { destination = PremiumCanvaDestination.HOME },
                        onMembershipChanged = { isPro = it },
                        onCollection = { destination = PremiumCanvaDestination.COLLECTION },
                    )
                    PremiumCanvaDestination.PROFILE -> MainPlayerProfileScreen(
                        backend,
                        { destination = PremiumCanvaDestination.PROFILE_DETAILS },
                        { destination = PremiumCanvaDestination.SHOP },
                        { destination = PremiumCanvaDestination.COLLECTION },
                        { destination = PremiumCanvaDestination.SETTINGS },
                        { destination = PremiumCanvaDestination.SOCIAL },
                    )
                    PremiumCanvaDestination.COLLECTION -> PlayerCollectionScreen(backend) {
                        destination = PremiumCanvaDestination.PROFILE
                    }
                    PremiumCanvaDestination.SETTINGS -> MainSettingsScreen(
                        backend,
                        { destination = PremiumCanvaDestination.PROFILE },
                        { destination = PremiumCanvaDestination.ACCOUNT },
                        onSignedOut,
                    )
                    PremiumCanvaDestination.ACCOUNT -> CompleteProfileScreen(1) {
                        destination = PremiumCanvaDestination.SETTINGS
                    }
                    PremiumCanvaDestination.PROFILE_DETAILS -> CompleteProfileScreen(0) {
                        destination = PremiumCanvaDestination.PROFILE
                    }
                    PremiumCanvaDestination.COMPETE -> CompetitionHubScreen(
                        onBack = { destination = PremiumCanvaDestination.HOME },
                    )
                    PremiumCanvaDestination.LAST_LETTER -> OnlineGameScreenV6()
                    PremiumCanvaDestination.SIEGE -> WordSiegeExperienceScreen { leaveGame() }
                    PremiumCanvaDestination.LETTER_PATH -> LetterLadderGameScreen { leaveGame() }
                }
            }
        }
    }
}

@Composable
private fun PremiumCanvaHome(
    backend: OnlineGameBackend,
    onSiege: () -> Unit,
    onLastLetter: () -> Unit,
    onLetterPath: () -> Unit,
    onCompete: () -> Unit,
    onProfile: () -> Unit,
    onSocial: () -> Unit,
    onShop: () -> Unit,
) {
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    LaunchedEffect(Unit) {
        if (!SupabaseProvider.configured) return@LaunchedEffect
        profile = backend.currentUserId()?.let { id -> runCatching { backend.getProfile(id) }.getOrNull() }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 640.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            PremiumHomeCommandDeck(
                profile = profile,
                onProfile = onProfile,
                onSiege = onSiege,
                onSocial = onSocial,
            )
        }
        item {
            PremiumCanvaSecondaryModes(onLastLetter, onLetterPath)
        }
        item {
            PremiumDailyObjective(onClick = onCompete)
        }
        item {
            PremiumCanvaQuickActions(onCompete, onSocial, onShop)
        }
    }
}

@Composable
private fun PremiumCanvaSecondaryModes(onLastLetter: () -> Unit, onLetterPath: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MainSectionTitle(sh("DİĞER OYUNLAR", "MORE GAMES"))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PremiumCanvaModeTile(
                modifier = Modifier.weight(1f),
                iconRes = R.drawable.son_harf_app_icon_master,
                title = sh("SON HARF", "LAST LETTER"),
                subtitle = sh("Hızlı kelime düellosu", "Fast word duel"),
                accent = SonHarfTheme.ActionOrange,
                onClick = onLastLetter,
            )
            PremiumCanvaModeTile(
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
private fun PremiumCanvaModeTile(
    modifier: Modifier,
    iconRes: Int,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(166.dp),
        shape = MainUiShape.Card,
        color = SonHarfTheme.Surface,
        border = BorderStroke(1.dp, accent.copy(alpha = .22f)),
        shadowElevation = 4.dp,
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
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
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = SonHarfTheme.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = SonHarfTheme.TextSecondary, fontSize = 9.sp, lineHeight = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(5.dp))
                PremiumAccentPill(sh("OYNA", "PLAY"), accent)
            }
        }
    }
}

@Composable
private fun PremiumCanvaQuickActions(
    onCompete: () -> Unit,
    onSocial: () -> Unit,
    onShop: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MainSectionTitle(sh("HIZLI ERİŞİM", "QUICK ACCESS"))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            PremiumCanvaQuickAction(Modifier.weight(1f), Icons.Rounded.EmojiEvents, sh("Lig & Turnuva", "League & Events"), SonHarfTheme.Purple, onCompete)
            PremiumCanvaQuickAction(Modifier.weight(1f), Icons.Rounded.Groups, sh("Arkadaşlar", "Friends"), SonHarfTheme.Turquoise, onSocial)
            PremiumCanvaQuickAction(Modifier.weight(1f), Icons.Rounded.Storefront, sh("Mağaza", "Store"), SonHarfTheme.ActionOrange, onShop)
        }
    }
}

@Composable
private fun PremiumCanvaQuickAction(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 82.dp),
        shape = MainUiShape.Control,
        color = SonHarfTheme.Surface,
        border = BorderStroke(1.dp, SonHarfTheme.Border),
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp))
            Text(label, color = SonHarfTheme.TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Black, maxLines = 2)
        }
    }
}

@Composable
private fun PremiumCanvaGameCenter(
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
            PremiumCanvaGameCard(
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
            PremiumCanvaGameCard(
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
            PremiumCanvaGameCard(
                icon = Icons.Rounded.Route,
                title = sh("HARF YOLU", "LETTER PATH"),
                subtitle = sh("Beş adımda kelime rotanı tamamla", "Complete your word route in five moves"),
                language = letterPathLanguage,
                onLanguageChange = onLetterPathLanguage,
                accent = SonHarfTheme.Purple,
                onClick = onLetterPath,
            )
        }
    }
}

@Composable
private fun PremiumCanvaGameCard(
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
        border = BorderStroke(if (primary) 1.5.dp else 1.dp, accent.copy(alpha = if (primary) .45f else .22f)),
        shadowElevation = if (primary) 7.dp else 4.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = MainUiShape.Card, color = accent.copy(alpha = .11f)) {
                    Icon(icon, null, tint = accent, modifier = Modifier.padding(13.dp).size(28.dp))
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    if (primary) PremiumAccentPill(sh("ANA OYUN", "MAIN GAME"), SonHarfTheme.Turquoise)
                    if (primary) Spacer(Modifier.height(6.dp))
                    Text(title, color = SonHarfTheme.TextPrimary, fontSize = if (primary) 18.sp else 16.sp, fontWeight = FontWeight.Black)
                    Text(subtitle, color = SonHarfTheme.TextSecondary, fontSize = 10.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                PremiumCanvaLanguageChoice(language, onLanguageChange, Modifier.weight(1f), accent)
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = onClick,
                    shape = MainUiShape.Control,
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(sh("OYNA", "PLAY"), fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun PremiumCanvaLanguageChoice(
    language: String,
    onLanguageChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = SonHarfTheme.Primary,
) {
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
private fun PremiumCanvaBottomBar(
    destination: PremiumCanvaDestination,
    onHome: () -> Unit,
    onSocial: () -> Unit,
    onGames: () -> Unit,
    onShop: () -> Unit,
    onProfile: () -> Unit,
) {
    Surface(color = SonHarfTheme.NavigationSurface, shadowElevation = 10.dp) {
        NavigationBar(containerColor = Color.Transparent, tonalElevation = 0.dp) {
            PremiumCanvaNavItem(destination == PremiumCanvaDestination.HOME, Icons.Rounded.Home, sh("ANA SAYFA", "HOME"), onHome)
            PremiumCanvaNavItem(destination == PremiumCanvaDestination.SOCIAL, Icons.Rounded.Groups, sh("SOSYAL", "SOCIAL"), onSocial)
            PremiumCanvaNavItem(destination == PremiumCanvaDestination.GAMES, Icons.Rounded.PlayCircle, sh("OYNA", "PLAY"), onGames, emphasized = true)
            PremiumCanvaNavItem(destination == PremiumCanvaDestination.SHOP, Icons.Rounded.Storefront, sh("MAĞAZA", "STORE"), onShop)
            PremiumCanvaNavItem(destination == PremiumCanvaDestination.PROFILE, Icons.Rounded.Person, sh("PROFİL", "PROFILE"), onProfile)
        }
    }
}

@Composable
private fun RowScope.PremiumCanvaNavItem(
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
