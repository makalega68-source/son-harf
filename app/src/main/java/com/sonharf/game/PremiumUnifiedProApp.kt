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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SupabaseProvider
import kotlinx.coroutines.delay

private enum class PremiumDestination {
    HOME, GAMES, COMPETE, PROFILE,
    LAST_LETTER, SIEGE, LETTER_PATH,
    SOCIAL, SETTINGS, ACCOUNT, PROFILE_DETAILS
}

@Composable
fun PremiumUnifiedProApp(onSignedOut: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    var destination by remember { mutableStateOf(PremiumDestination.HOME) }
    var isPro by remember { mutableStateOf(false) }
    val homeRequest = SonHarfUiState.homeRequest

    LaunchedEffect(Unit) {
        backend.currentUserId()?.let { id ->
            runCatching { backend.getEquippedCosmetics() }.getOrNull()?.let(SonHarfCosmetics::apply)
            isPro = runCatching { backend.getProfile(id).isVip }.getOrDefault(false)
        }
    }
    LaunchedEffect(homeRequest) {
        if (homeRequest > 0) destination = PremiumDestination.HOME
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
            PremiumDestination.SETTINGS, PremiumDestination.PROFILE_DETAILS, PremiumDestination.SOCIAL -> PremiumDestination.PROFILE
            PremiumDestination.ACCOUNT -> PremiumDestination.SETTINGS
            PremiumDestination.LAST_LETTER, PremiumDestination.SIEGE, PremiumDestination.LETTER_PATH -> PremiumDestination.GAMES
            else -> PremiumDestination.HOME
        }
    }

    val topLevel = destination in setOf(
        PremiumDestination.HOME,
        PremiumDestination.GAMES,
        PremiumDestination.COMPETE,
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
            topBar = { SonHarfTopAdBanner(isPremium = isPro) },
            bottomBar = {
                if (topLevel) {
                    PremiumBottomBar(
                        destination = destination,
                        onHome = { destination = PremiumDestination.HOME },
                        onGames = { destination = PremiumDestination.GAMES },
                        onCompete = { destination = PremiumDestination.COMPETE },
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
                        onPrimary = { destination = PremiumDestination.SIEGE },
                        onGames = { destination = PremiumDestination.GAMES },
                        onCompete = { destination = PremiumDestination.COMPETE },
                        onProfile = { destination = PremiumDestination.PROFILE },
                    )
                    PremiumDestination.GAMES -> PremiumGameCenter(
                        onSiege = { destination = PremiumDestination.SIEGE },
                        onLastLetter = { destination = PremiumDestination.LAST_LETTER },
                        onLetterPath = { destination = PremiumDestination.LETTER_PATH },
                    )
                    PremiumDestination.COMPETE -> LeaderboardExperienceScreen {
                        destination = PremiumDestination.HOME
                    }
                    PremiumDestination.PROFILE -> MainPlayerProfileScreen(
                        backend,
                        { destination = PremiumDestination.PROFILE_DETAILS },
                        { destination = PremiumDestination.PROFILE },
                        { destination = PremiumDestination.SETTINGS },
                        { destination = PremiumDestination.SOCIAL },
                    )
                    PremiumDestination.LAST_LETTER -> OnlineGameScreenV6()
                    PremiumDestination.SIEGE -> WordSiegeExperienceScreen {
                        destination = PremiumDestination.GAMES
                    }
                    PremiumDestination.LETTER_PATH -> LetterLadderGameScreen {
                        destination = PremiumDestination.GAMES
                    }
                    PremiumDestination.SOCIAL -> MainSocialScreen(
                        backend = backend,
                        onPlay = { destination = PremiumDestination.LAST_LETTER },
                        onSiege = { destination = PremiumDestination.SIEGE },
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
    onGames: () -> Unit,
    onCompete: () -> Unit,
    onProfile: () -> Unit,
) {
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    LaunchedEffect(Unit) {
        if (SupabaseProvider.configured) {
            profile = backend.currentUserId()?.let { id ->
                runCatching { backend.getProfile(id) }.getOrNull()
            }
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            PremiumHomeCommandDeck(
                profile = profile,
                onProfile = onProfile,
                onSiege = onPrimary,
                onGames = onGames,
                onCompete = onCompete,
            )
        }
        item { Spacer(Modifier.height(4.dp)) }
    }
}

@Composable
private fun PremiumGameCenter(
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
                sh("OYUNLAR", "GAMES"),
                color = SonHarfTheme.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                sh("Bir oyun seç ve başla.", "Choose a game and start."),
                color = SonHarfTheme.TextSecondary,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(8.dp))
        }
        item {
            PremiumGameCard(
                icon = Icons.Rounded.GridView,
                title = sh("KELİME TAHTI", "WORD THRONE"),
                subtitle = sh("Ana oyun • taktik alan savaşı", "Main game • tactical territory battle"),
                primary = true,
                onClick = onSiege,
            )
        }
        item {
            PremiumGameCard(
                icon = Icons.Rounded.Bolt,
                title = sh("SON HARF", "LAST LETTER"),
                subtitle = sh("Hızlı kelime düellosu", "Fast word duel"),
                onClick = onLastLetter,
            )
        }
        item {
            PremiumGameCard(
                icon = Icons.Rounded.Route,
                title = sh("HARF YOLU", "LETTER PATH"),
                subtitle = sh("Kelime rotanı tamamla", "Complete your word path"),
                onClick = onLetterPath,
            )
        }
    }
}

@Composable
private fun PremiumGameCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(if (primary) 124.dp else 94.dp),
        shape = RoundedCornerShape(24.dp),
        color = if (primary) SonHarfTheme.Primary.copy(alpha = .12f) else SonHarfTheme.Surface.copy(alpha = .98f),
        border = BorderStroke(1.dp, if (primary) SonHarfTheme.Primary.copy(alpha = .34f) else SonHarfTheme.Border),
        shadowElevation = if (primary) 5.dp else 1.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SonHarfTheme.Primary.copy(alpha = .13f),
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = SonHarfTheme.Primary,
                    modifier = Modifier.padding(12.dp).size(26.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = SonHarfTheme.TextPrimary, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = SonHarfTheme.TextSecondary, fontSize = 10.sp)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = SonHarfTheme.Primary)
        }
    }
}

@Composable
private fun PremiumBottomBar(
    destination: PremiumDestination,
    onHome: () -> Unit,
    onGames: () -> Unit,
    onCompete: () -> Unit,
    onProfile: () -> Unit,
) {
    val items = listOf(
        Triple(PremiumDestination.HOME, Icons.Rounded.Home, sh("ANA", "HOME")) to onHome,
        Triple(PremiumDestination.GAMES, Icons.Rounded.SportsEsports, sh("OYUNLAR", "GAMES")) to onGames,
        Triple(PremiumDestination.COMPETE, Icons.Rounded.EmojiEvents, sh("REKABET", "COMPETE")) to onCompete,
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
