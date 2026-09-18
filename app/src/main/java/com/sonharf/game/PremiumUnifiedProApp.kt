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
            PremiumDestination.SOCIAL, PremiumDestination.SHOP -> PremiumDestination.HOME
            PremiumDestination.ACCOUNT -> PremiumDestination.SETTINGS
            PremiumDestination.LAST_LETTER, PremiumDestination.SIEGE, PremiumDestination.LETTER_PATH -> {
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
                if (destination !in setOf(PremiumDestination.LAST_LETTER, PremiumDestination.SIEGE, PremiumDestination.LETTER_PATH)) {
                    SonHarfTopAdBanner(isPremium = isPro)
                }
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
                        onPrimary = { destination = PremiumDestination.GAMES },
                        onCompete = { destination = PremiumDestination.COMPETE },
                        onProfile = { destination = PremiumDestination.PROFILE },
                        onSocial = { destination = PremiumDestination.SOCIAL },
                        onLastLetter = { destination = PremiumDestination.GAMES },
                        onLetterPath = { destination = PremiumDestination.GAMES },
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
                        leaveGame()
                    }
                    PremiumDestination.LETTER_PATH -> LetterLadderGameScreen {
                        leaveGame()
                    }
                    PremiumDestination.SOCIAL -> MainSocialScreen(
                        backend = backend,
                        onPlay = { destination = PremiumDestination.GAMES },
                        onSiege = { destination = PremiumDestination.GAMES },
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = SonHarfTheme.Primary.copy(alpha = .11f),
                border = BorderStroke(1.dp, SonHarfTheme.Primary.copy(alpha = .30f)),
                shadowElevation = 3.dp,
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(16.dp), color = SonHarfTheme.Primary) {
                            Icon(
                                Icons.Rounded.GridView,
                                contentDescription = null,
                                tint = SonHarfTheme.OnPrimary,
                                modifier = Modifier.padding(11.dp).size(27.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                sh("ARENA MERKEZİ", "ARENA CENTER"),
                                color = SonHarfTheme.TextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                            )
                            Text(
                                sh(
                                    "Oyununu seç • dilini belirle • mücadeleyi başlat",
                                    "Choose your game • set the language • start the battle",
                                ),
                                color = SonHarfTheme.TextSecondary,
                                fontSize = 11.sp,
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        PremiumArenaPill(Icons.Rounded.Language, "TR + EN", Modifier.weight(1f))
                        PremiumArenaPill(Icons.Rounded.Verified, sh("ADİL OYUN", "FAIR PLAY"), Modifier.weight(1f))
                        PremiumArenaPill(Icons.Rounded.Groups, "1v1", Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            Text(
                sh("OYUNUNU VE OYUN DİLİNİ SEÇ", "CHOOSE GAME & GAME LANGUAGE"),
                color = SonHarfTheme.PremiumGold,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = .8.sp,
            )
        }
        item {
            PremiumGameCard(
                icon = Icons.Rounded.GridView,
                eyebrow = sh("ANA OYUN • TAKTİK ARENA", "MAIN GAME • TACTICAL ARENA"),
                title = sh("KELİME KUŞATMASI", "KELİME KUŞATMASI"),
                subtitle = sh("Kelimelerle alan fethet, harita hâkimiyetini ele geçir.", "Capture territory with words and control the map."),
                facts = listOf(
                    sh("KELİME + BÖLGE PUANI", "WORD + TERRITORY SCORE"),
                    sh("ALAN ELE GEÇİRME", "TERRITORY CAPTURE"),
                    sh("1v1 + BOT ANTRENMANI", "1v1 + BOT PRACTICE"),
                ),
                language = siegeLanguage,
                onLanguageChange = onSiegeLanguage,
                primary = true,
                onClick = onSiege,
            )
        }
        item {
            PremiumGameCard(
                icon = Icons.Rounded.Bolt,
                eyebrow = sh("HIZLI DÜELLO", "QUICK DUEL"),
                title = sh("SON HARF", "LAST LETTER"),
                subtitle = sh("Son harften kelimeyi sürdür; üç round boyunca rakibini geç.", "Continue from the last letter and outscore your rival across three rounds."),
                facts = listOf(
                    sh("3 ROUND", "3 ROUNDS"),
                    sh("10 + 10 KELİME / ROUND", "10 + 10 WORDS / ROUND"),
                    "15 → 13 → 11 ${sh("SN", "SEC")}",
                ),
                language = lastLetterLanguage,
                onLanguageChange = onLastLetterLanguage,
                onClick = onLastLetter,
            )
        }
        item {
            PremiumGameCard(
                icon = Icons.Rounded.Route,
                eyebrow = sh("KISA OTURUM", "QUICK SESSION"),
                title = sh("HARF YOLU", "LETTER PATH"),
                subtitle = sh("Kelime rotanı tamamla ve yeni hedefleri aç.", "Complete your word route and unlock new targets."),
                facts = listOf(
                    sh("KELİME ROTASI", "WORD ROUTE"),
                    sh("HIZLI OYUN", "QUICK PLAY"),
                    "TR + EN",
                ),
                language = letterPathLanguage,
                onLanguageChange = onLetterPathLanguage,
                onClick = onLetterPath,
            )
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = SonHarfTheme.Surface.copy(alpha = .96f),
                border = BorderStroke(1.dp, SonHarfTheme.Border),
            ) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Verified, null, tint = SonHarfTheme.Success, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(sh("REKABET GÜÇ SATIN ALMAZ", "COMPETITION IS SKILL-BASED"), color = SonHarfTheme.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Text(
                            sh("Dil yalnızca sözlüğü ve eşleşmeyi belirler; Premium rekabet avantajı sağlamaz.", "Language only sets dictionary and matchmaking; Premium gives no competitive advantage."),
                            color = SonHarfTheme.TextSecondary,
                            fontSize = 9.sp,
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun PremiumArenaPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = SonHarfTheme.Surface.copy(alpha = .92f),
        border = BorderStroke(1.dp, SonHarfTheme.Border),
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(icon, null, tint = SonHarfTheme.Primary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
            Text(label, color = SonHarfTheme.TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}

@Composable
private fun PremiumGameCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    eyebrow: String,
    title: String,
    subtitle: String,
    facts: List<String>,
    language: String,
    onLanguageChange: (String) -> Unit,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    val accent = if (primary) SonHarfTheme.Primary else SonHarfTheme.Turquoise
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = if (primary) SonHarfTheme.Primary.copy(alpha = .10f) else SonHarfTheme.Surface.copy(alpha = .98f),
        border = BorderStroke(1.dp, accent.copy(alpha = if (primary) .38f else .24f)),
        shadowElevation = if (primary) 6.dp else 2.dp,
    ) {
        Column(Modifier.padding(horizontal = 17.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(17.dp), color = accent.copy(alpha = .16f)) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.padding(12.dp).size(28.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(eyebrow, color = if (primary) SonHarfTheme.PremiumGold else accent, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = .5.sp)
                    Text(title, color = SonHarfTheme.TextPrimary, fontWeight = FontWeight.Black, fontSize = 17.sp)
                    Text(subtitle, color = SonHarfTheme.TextSecondary, fontSize = 10.sp, lineHeight = 13.sp)
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                facts.take(3).forEach { fact ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = SonHarfTheme.Surface.copy(alpha = .88f),
                        border = BorderStroke(1.dp, SonHarfTheme.Border),
                    ) {
                        Text(
                            fact,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp),
                            color = SonHarfTheme.TextSecondary,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Language, null, tint = accent, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(sh("OYUN DİLİ", "GAME LANGUAGE"), color = SonHarfTheme.TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.weight(1f))
                    Text(if (language == "tr") "TÜRKÇE" else "ENGLISH", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
                PremiumLanguageChoice(
                    language = language,
                    onLanguageChange = onLanguageChange,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(17.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (primary) SonHarfTheme.Primary else SonHarfTheme.Forest,
                    contentColor = SonHarfTheme.OnPrimary,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
            ) {
                Icon(Icons.Rounded.Bolt, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text(
                    if (language == "tr") "TR • ${sh("OYNA", "PLAY")}" else "EN • ${sh("OYNA", "PLAY")}",
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    letterSpacing = .4.sp,
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
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = language == "tr",
            onClick = { onLanguageChange("tr") },
            modifier = Modifier.weight(1f),
            label = {
                Text(
                    "TR  TÜRKÇE",
                    modifier = Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                )
            },
            leadingIcon = if (language == "tr") {
                { Icon(Icons.Rounded.Check, null, Modifier.size(15.dp)) }
            } else null,
        )
        FilterChip(
            selected = language == "en",
            onClick = { onLanguageChange("en") },
            modifier = Modifier.weight(1f),
            label = {
                Text(
                    "EN  ENGLISH",
                    modifier = Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                )
            },
            leadingIcon = if (language == "en") {
                { Icon(Icons.Rounded.Check, null, Modifier.size(15.dp)) }
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
