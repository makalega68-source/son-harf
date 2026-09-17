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
import com.sonharf.game.data.isCurrentUserAdmin
import kotlinx.coroutines.delay

private enum class PremiumDestination {
    HOME, GAMES, CLUB, COMPETE, PROFILE, COLLECTION,
    LAST_LETTER, SIEGE, LETTER_PATH,
    SOCIAL, SETTINGS, ACCOUNT, PROFILE_DETAILS, SHOP, ADMIN
}

@Composable
fun PremiumUnifiedProApp(onSignedOut: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    var destination by remember { mutableStateOf(PremiumDestination.HOME) }
    var isPro by remember { mutableStateOf(false) }
    var isAdmin by remember { mutableStateOf(false) }
    val homeRequest = SonHarfUiState.homeRequest
    val context = androidx.compose.ui.platform.LocalContext.current
    // G5.4: default is the persisted "game language" pref, not the UI
    // language. Kullanıcı UI TR kalırken maçı EN oynayabilir.
    val defaultGameLanguage = SharedDictionaryService.canonicalLanguage(
        SonHarfPreferences.gameLanguage(context),
    )
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
            isAdmin = backend.isCurrentUserAdmin()
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
            PremiumDestination.SOCIAL, PremiumDestination.SHOP, PremiumDestination.ADMIN -> PremiumDestination.HOME
            PremiumDestination.ACCOUNT -> PremiumDestination.SETTINGS
            PremiumDestination.LAST_LETTER, PremiumDestination.SIEGE, PremiumDestination.LETTER_PATH -> {
                uiLanguageBeforeGame?.let { SonHarfUiState.language = it }
                uiLanguageBeforeGame = null
                PremiumDestination.HOME
            }
            else -> PremiumDestination.HOME
        }
    }

    // Top-level navigation: Home, Friends/Social, Store, Profile. Club is hidden.
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
                if (destination !in setOf(PremiumDestination.LAST_LETTER, PremiumDestination.SIEGE, PremiumDestination.LETTER_PATH)) SonHarfTopAdBanner(isPremium = isPro)
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
                        onPrimary = { openGame(PremiumDestination.SIEGE, siegeLanguage) },
                        onCompete = { destination = PremiumDestination.COMPETE },
                        onProfile = { destination = PremiumDestination.PROFILE },
                        onSocial = { destination = PremiumDestination.SOCIAL },
                        onLastLetter = { openGame(PremiumDestination.LAST_LETTER, lastLetterLanguage) },
                        onLetterPath = { openGame(PremiumDestination.LETTER_PATH, letterPathLanguage) },
                        isAdmin = isAdmin,
                        onAdmin = { destination = PremiumDestination.ADMIN },
                    )
                    PremiumDestination.GAMES -> PremiumGameCenter(
                        siegeLanguage = siegeLanguage,
                        lastLetterLanguage = lastLetterLanguage,
                        letterPathLanguage = letterPathLanguage,
                        // G5.4: persist the last picked language per game
                        // so the choice survives app restarts (spec:
                        // "Son seçim cihazda hatırlanır").
                        onSiegeLanguage = {
                            siegeLanguage = it
                            SonHarfPreferences.setGameLanguage(context, it)
                        },
                        onLastLetterLanguage = {
                            lastLetterLanguage = it
                            SonHarfPreferences.setGameLanguage(context, it)
                        },
                        onLetterPathLanguage = {
                            letterPathLanguage = it
                            SonHarfPreferences.setGameLanguage(context, it)
                        },
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
                    PremiumDestination.LAST_LETTER -> Box(Modifier.fillMaxSize()) {
                        OnlineGameScreenV6()
                        GameHelpButton(GameHelpType.LAST_LETTER, Modifier.align(Alignment.TopEnd).padding(8.dp))
                    }
                    PremiumDestination.SIEGE -> Box(Modifier.fillMaxSize()) {
                        WordSiegeExperienceScreen { leaveGame() }
                        GameHelpButton(GameHelpType.SIEGE, Modifier.align(Alignment.TopEnd).padding(8.dp))
                    }
                    PremiumDestination.LETTER_PATH -> Box(Modifier.fillMaxSize()) {
                        LetterLadderGameScreen { leaveGame() }
                        GameHelpButton(GameHelpType.LETTER_PATH, Modifier.align(Alignment.TopEnd).padding(8.dp))
                    }
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
                    PremiumDestination.ADMIN -> AdminConsoleScreen { destination = PremiumDestination.HOME }
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
    isAdmin: Boolean,
    onAdmin: () -> Unit,
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
                PremiumHomeCommandDeck(profile, onProfile, onPrimary, onSocial, isAdmin, onAdmin)
            }
            item(key = "home_secondary_modes") {
                PremiumOtherGames(onLastLetter = onLastLetter, onLetterPath = onLetterPath)
            }
            item(key = "daily_objective") {
                PremiumDailyObjective(onClick = onCompete)
            }
            // G4.3 istemci adopsiyonu — ana ekrandaki 3 günlük görev.
            item(key = "daily_quests_card") {
                com.sonharf.game.ui.premium.DailyQuestsCard(
                    language = SonHarfUiState.language,
                )
            }
            // G4.4 istemci adopsiyonu — streak + kelime koleksiyonu + tema.
            item(key = "retention_card") {
                com.sonharf.game.ui.premium.RetentionCard(
                    language = SonHarfUiState.language,
                )
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
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                sh("OYUN MODLARI", "GAME MODES"),
                color = SonHarfTheme.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                sh(
                    "Modunu seç, dilini ayarla ve doğrudan arenaya gir.",
                    "Pick a mode, set your language, and enter the arena.",
                ),
                color = SonHarfTheme.TextSecondary,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(8.dp))
        }
        item {
            PremiumGameCard(
                icon = Icons.Rounded.GridView,
                title = sh("KELİME KUŞATMASI", "KELİME KUŞATMASI"),
                subtitle = sh("Ana oyun • taktik alan savaşı", "Main game • tactical territory battle"),
                language = siegeLanguage,
                onLanguageChange = onSiegeLanguage,
                primary = true,
                onClick = onSiege,
            )
        }
        // G5.5 adopsiyon: her oyun kartının altına kısa "Nasıl oynanır?"
        // açılır kartı. Kullanıcı ilk açtığında görmezden gelinir; merak
        // ederse tıklar. 3 madde + kısa emoji ikon.
        item {
            com.sonharf.game.ui.premium.HowToPlayCard(
                steps = listOf(
                    com.sonharf.game.ui.premium.HowToStep("🗺️", sh("Hücrelere harf yerleştirerek bölgeni büyüt.", "Place letters on cells to grow your territory.")),
                    com.sonharf.game.ui.premium.HowToStep("💥", sh("Rakip hücrelerini ele geçir; birden fazlasını al 'KUŞATMA' bonusu.", "Capture opponent cells; multiple at once earns 'SIEGE' bonus.")),
                    com.sonharf.game.ui.premium.HowToStep("🏰", sh("Kritik bölgeleri veya kaleyi al, harita %'ini artır.", "Take critical zones and the castle to raise your map %.")),
                ),
                accent = SonHarfTheme.KusatmaPurple,
            )
        }
        item {
            PremiumGameCard(
                icon = Icons.Rounded.Bolt,
                title = sh("SON HARF", "LAST LETTER"),
                subtitle = sh("Hızlı kelime düellosu", "Fast word duel"),
                language = lastLetterLanguage,
                onLanguageChange = onLastLetterLanguage,
                onClick = onLastLetter,
            )
        }
        item {
            com.sonharf.game.ui.premium.HowToPlayCard(
                steps = listOf(
                    com.sonharf.game.ui.premium.HowToStep("🔤", sh("Rakibin bıraktığı son harfle başlayan bir kelime yaz.", "Type a word that starts with the opponent's last letter.")),
                    com.sonharf.game.ui.premium.HowToStep("⏱️", sh("Süre 15sn'den başlar; her hamlede kısalır (min 8sn).", "Turn starts at 15s and shrinks each move (min 8s).")),
                    com.sonharf.game.ui.premium.HowToStep("❤️", sh("3 canın var; geçersiz kelime veya süre bitişi 1 can götürür.", "You get 3 lives; an invalid word or timeout costs one.")),
                ),
                accent = SonHarfTheme.SonHarfOrange,
            )
        }
        item {
            PremiumGameCard(
                icon = Icons.Rounded.Route,
                title = sh("HARF YOLU", "LETTER PATH"),
                subtitle = sh("Kelime rotanı tamamla", "Complete your word path"),
                language = letterPathLanguage,
                onLanguageChange = onLetterPathLanguage,
                onClick = onLetterPath,
            )
        }
        item {
            com.sonharf.game.ui.premium.HowToPlayCard(
                steps = listOf(
                    com.sonharf.game.ui.premium.HowToStep("🧩", sh("Başlangıç ve hedef kelime hazır — sen ara 4 kelimeyi yaz.", "Start and target words are set — you fill the 4 middle words.")),
                    com.sonharf.game.ui.premium.HowToStep("🔁", sh("Her adımda tek harf değiştir; her kelime sözlükte olmalı.", "Change exactly one letter per step; every word must be in the dictionary.")),
                    com.sonharf.game.ui.premium.HowToStep("💡", sh("İpucu 3 aşamalı: kutu → tanım → 3 harf seçeneği.", "Hint is 3 stages: cell → definition → 3 letter choices.")),
                ),
                accent = SonHarfTheme.KelimeYoluTeal,
            )
        }
    }
}

@Composable
private fun PremiumGameCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    language: String,
    onLanguageChange: (String) -> Unit,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = if (primary) SonHarfTheme.Primary.copy(alpha = .12f) else SonHarfTheme.Surface.copy(alpha = .98f),
        border = BorderStroke(1.dp, if (primary) SonHarfTheme.Primary.copy(alpha = .34f) else SonHarfTheme.Border),
        shadowElevation = if (primary) 5.dp else 1.dp,
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            if (primary) {
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = SonHarfTheme.PremiumGold.copy(alpha = .16f),
                ) {
                    Text(
                        sh("ANA ARENA", "MAIN ARENA"),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = SonHarfTheme.PremiumGold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                Spacer(Modifier.height(10.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (primary) SonHarfTheme.Primary.copy(alpha = .18f) else SonHarfTheme.Turquoise.copy(alpha = .12f),
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (primary) SonHarfTheme.Primary else SonHarfTheme.Turquoise,
                        modifier = Modifier.padding(12.dp).size(26.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = SonHarfTheme.TextPrimary, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(subtitle, color = SonHarfTheme.TextSecondary, fontSize = 10.sp)
                }
            }
            Spacer(Modifier.height(13.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PremiumLanguageChoice(
                    language = language,
                    onLanguageChange = onLanguageChange,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (primary) SonHarfTheme.Primary else SonHarfTheme.Forest,
                        contentColor = SonHarfTheme.OnPrimary,
                    ),
                ) {
                    Text(sh("ARENA'YA GİR", "ENTER"), fontWeight = FontWeight.Black, fontSize = 10.sp)
                }
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
            label = { Text("TR", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
            leadingIcon = if (language == "tr") {
                { Icon(Icons.Rounded.Check, null, Modifier.size(15.dp)) }
            } else null,
        )
        FilterChip(
            selected = language == "en",
            onClick = { onLanguageChange("en") },
            label = { Text("EN", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
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
