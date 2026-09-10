package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.LeaderboardV2Row
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.getLeaderboardV2
import kotlinx.coroutines.delay

private enum class UnifiedDestination {
    HOME, GAME, SIEGE, LETTER, LEAGUE, COMPETITION, SOCIAL, SHOP, PROFILE,
    SETTINGS, VIP, ACCOUNT, PROFILE_DETAILS, TASKS, DAILY
}

private data class WeeklyPodiumPlayer(
    val row: LeaderboardV2Row,
    val profile: ProfileDto?,
)

/** Theme-aware shell palette. The equipped profile theme is the only visual source of truth. */
private object UnifiedUi {
    val Background: Color get() = SonHarfTheme.Background
    val Surface: Color get() = SonHarfTheme.Surface
    val Surface2: Color get() = SonHarfTheme.SurfaceSecondary
    val Navigation: Color get() = SonHarfTheme.NavigationSurface
    val Border: Color get() = SonHarfTheme.Border
    val Text: Color get() = SonHarfTheme.TextPrimary
    val Muted: Color get() = SonHarfTheme.TextSecondary
    val Blue: Color get() = SonHarfTheme.Primary
    val Cyan: Color get() = SonHarfTheme.Turquoise
    val Gold: Color get() = SonHarfTheme.Warning
    val Green: Color get() = SonHarfTheme.Success
    val Red: Color get() = SonHarfTheme.Error
    val Purple: Color get() = SonHarfTheme.Lavender
    val OnPrimary: Color get() = SonHarfTheme.OnPrimary
    val OnSecondary: Color get() = SonHarfTheme.OnSecondary
    val OnTertiary: Color get() = SonHarfTheme.OnTertiary

    val HeroStart: Color get() = SonHarfTheme.HeroStart
    val HeroMiddle: Color get() = SonHarfTheme.HeroMiddle
    val HeroEnd: Color get() = SonHarfTheme.HeroEnd
}

@Composable
fun UnifiedProApp(onSignedOut: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    var destination by remember { mutableStateOf(UnifiedDestination.HOME) }
    var isPro by remember { mutableStateOf(false) }
    val homeRequest = SonHarfUiState.homeRequest

    LaunchedEffect(Unit) {
        val id = backend.currentUserId()
        if (id != null) {
            runCatching { backend.getEquippedCosmetics() }.getOrNull()?.let(SonHarfCosmetics::apply)
            isPro = runCatching { backend.getProfile(id).isVip }.getOrDefault(false)
        }
    }
    LaunchedEffect(homeRequest) {
        if (homeRequest > 0) destination = UnifiedDestination.HOME
    }
    LaunchedEffect(destination) {
        if (destination !in setOf(UnifiedDestination.GAME, UnifiedDestination.SIEGE, UnifiedDestination.LETTER, UnifiedDestination.DAILY)) {
            while (true) {
                runCatching { backend.setPresence("online") }
                delay(55_000)
            }
        }
    }

    BackHandler(enabled = destination != UnifiedDestination.HOME) {
        destination = when (destination) {
            UnifiedDestination.SETTINGS, UnifiedDestination.VIP, UnifiedDestination.PROFILE_DETAILS -> UnifiedDestination.PROFILE
            UnifiedDestination.ACCOUNT -> UnifiedDestination.SETTINGS
            UnifiedDestination.DAILY -> UnifiedDestination.TASKS
            else -> UnifiedDestination.HOME
        }
    }

    val topLevel = destination in setOf(
        UnifiedDestination.HOME,
        UnifiedDestination.LEAGUE,
        UnifiedDestination.SOCIAL,
        UnifiedDestination.SHOP,
        UnifiedDestination.PROFILE,
    )
    val scheme = if (SonHarfTheme.IsDark) {
        darkColorScheme(
            primary = UnifiedUi.Blue,
            secondary = UnifiedUi.Cyan,
            tertiary = UnifiedUi.Green,
            background = UnifiedUi.Background,
            surface = UnifiedUi.Surface,
            surfaceVariant = UnifiedUi.Surface2,
            onPrimary = UnifiedUi.OnPrimary,
            onSecondary = UnifiedUi.OnSecondary,
            onTertiary = UnifiedUi.OnTertiary,
            onBackground = UnifiedUi.Text,
            onSurface = UnifiedUi.Text,
            onSurfaceVariant = UnifiedUi.Text,
            error = UnifiedUi.Red,
        )
    } else {
        lightColorScheme(
            primary = UnifiedUi.Blue,
            secondary = UnifiedUi.Cyan,
            tertiary = UnifiedUi.Green,
            background = UnifiedUi.Background,
            surface = UnifiedUi.Surface,
            surfaceVariant = UnifiedUi.Surface2,
            onPrimary = UnifiedUi.OnPrimary,
            onSecondary = UnifiedUi.OnSecondary,
            onTertiary = UnifiedUi.OnTertiary,
            onBackground = UnifiedUi.Text,
            onSurface = UnifiedUi.Text,
            onSurfaceVariant = UnifiedUi.Text,
            error = UnifiedUi.Red,
        )
    }

    MaterialTheme(colorScheme = scheme) {
        Scaffold(
            containerColor = UnifiedUi.Background,
            topBar = { SonHarfTopAdBanner(isPremium = isPro) },
            bottomBar = {
                if (topLevel) UnifiedBottomBar(
                    destination = destination,
                    onHome = { destination = UnifiedDestination.HOME },
                    onLeague = { destination = UnifiedDestination.LEAGUE },
                    onSocial = { destination = UnifiedDestination.SOCIAL },
                    onShop = { destination = UnifiedDestination.SHOP },
                    onProfile = { destination = UnifiedDestination.PROFILE },
                )
            },
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(UnifiedUi.Background)
            ) {
                when (destination) {
                    UnifiedDestination.HOME -> UnifiedHomeScreen(
                        backend = backend,
                        onPlay = { destination = UnifiedDestination.GAME },
                        onSiege = { destination = UnifiedDestination.SIEGE },
                        onLetter = { destination = UnifiedDestination.LETTER },
                        onLeague = { destination = UnifiedDestination.LEAGUE },
                        onCompetition = { destination = UnifiedDestination.COMPETITION },
                        onSocial = { destination = UnifiedDestination.SOCIAL },
                        onShop = { destination = UnifiedDestination.SHOP },
                        onProfile = { destination = UnifiedDestination.PROFILE },
                        onTasks = { destination = UnifiedDestination.TASKS },
                        onVip = { destination = UnifiedDestination.VIP },
                    )
                    UnifiedDestination.GAME -> OnlineGameScreenV6()
                    UnifiedDestination.SIEGE -> WordSiegeExperienceScreen { destination = UnifiedDestination.HOME }
                    UnifiedDestination.LETTER -> LetterLadderGameScreen { destination = UnifiedDestination.HOME }
                    UnifiedDestination.LEAGUE -> LeaderboardExperienceScreen { destination = UnifiedDestination.HOME }
                    UnifiedDestination.COMPETITION -> CompetitionHubScreen { destination = UnifiedDestination.HOME }
                    UnifiedDestination.SOCIAL -> MainSocialScreen(backend = backend, onPlay = { destination = UnifiedDestination.GAME })
                    UnifiedDestination.SHOP -> ShopHubScreen()
                    UnifiedDestination.PROFILE -> MainPlayerProfileScreen(
                        backend,
                        { destination = UnifiedDestination.PROFILE_DETAILS },
                        { destination = UnifiedDestination.VIP },
                        { destination = UnifiedDestination.SETTINGS },
                        { destination = UnifiedDestination.SOCIAL },
                    )
                    UnifiedDestination.SETTINGS -> MainSettingsScreen(
                        backend,
                        { destination = UnifiedDestination.PROFILE },
                        { destination = UnifiedDestination.ACCOUNT },
                        onSignedOut,
                    )
                    UnifiedDestination.VIP -> UnifiedProVipScreen(backend) { destination = UnifiedDestination.PROFILE }
                    UnifiedDestination.ACCOUNT -> CompleteProfileScreen(1) { destination = UnifiedDestination.SETTINGS }
                    UnifiedDestination.PROFILE_DETAILS -> CompleteProfileScreen(0) { destination = UnifiedDestination.PROFILE }
                    UnifiedDestination.TASKS -> MainRetentionScreen(
                        backend,
                        { destination = UnifiedDestination.HOME },
                        { destination = UnifiedDestination.GAME },
                        { destination = UnifiedDestination.DAILY },
                    )
                    UnifiedDestination.DAILY -> DailyCipherScreen { destination = UnifiedDestination.TASKS }
                }
            }
        }
    }
}

@Composable
private fun UnifiedHomeScreen(
    backend: OnlineGameBackend,
    onPlay: () -> Unit,
    onSiege: () -> Unit,
    onLetter: () -> Unit,
    onLeague: () -> Unit,
    onCompetition: () -> Unit,
    onSocial: () -> Unit,
    onShop: () -> Unit,
    onProfile: () -> Unit,
    onTasks: () -> Unit,
    onVip: () -> Unit,
) {
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var weeklyTop by remember { mutableStateOf<List<WeeklyPodiumPlayer>>(emptyList()) }
    var weeklyTopLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!SupabaseProvider.configured) return@LaunchedEffect
        val id = backend.currentUserId()
        profile = id?.let { runCatching { backend.getProfile(it) }.getOrNull() }

        weeklyTopLoading = true
        val language = if (SonHarfUiState.language == "en") "en" else "tr"
        weeklyTop = runCatching {
            backend.getLeaderboardV2(language, "week", 3).map { row ->
                WeeklyPodiumPlayer(
                    row = row,
                    profile = runCatching { backend.getProfile(row.userId) }.getOrNull(),
                )
            }
        }.getOrDefault(emptyList())
        weeklyTopLoading = false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    SonHarfOfficialLogo(
                        modifier = Modifier.width(158.dp).height(44.dp),
                    )
                    Text(
                        sh("Kelimeyi Sürdür, Rakibini Geç", "Keep the word going, beat your rival"),
                        color = UnifiedUi.Blue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                UnifiedRoundAction(Icons.Rounded.Notifications, onTasks)
                Spacer(Modifier.width(8.dp))
                UnifiedRoundAction(Icons.Rounded.WorkspacePremium, onVip)
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth().shadow(14.dp, RoundedCornerShape(26.dp)),
                shape = RoundedCornerShape(26.dp),
                color = Color.Transparent,
                border = BorderStroke(1.dp, UnifiedUi.Blue.copy(alpha = .25f)),
            ) {
                Column(
                    Modifier
                        .background(Brush.linearGradient(listOf(UnifiedUi.HeroStart, UnifiedUi.HeroMiddle, UnifiedUi.HeroEnd)))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(15.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProfilePhotoAvatarWithGender(
                            avatarPath = profile?.avatarPath,
                            gender = profile?.gender,
                            name = profile?.displayName ?: sh("Oyuncu", "Player"),
                            size = 58.dp,
                            accent = Color.White,
                            visible = profile?.avatarVisibility != "hidden",
                            showGenderBadge = false,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(profile?.displayName ?: sh("OYUNCU", "PLAYER"), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                            Text("🏆 ${profile?.rating ?: 1000} RP", color = Color.White.copy(alpha = .82f), fontWeight = FontWeight.Bold)
                        }
                        Surface(shape = RoundedCornerShape(99.dp), color = UnifiedUi.Surface) {
                            Text("SC ${profile?.diamonds ?: 0}", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = UnifiedUi.Gold, fontWeight = FontWeight.Black)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        UnifiedHeroMetric("${profile?.wins ?: 0}", sh("GALİBİYET", "WINS"))
                        UnifiedHeroMetric("${profile?.losses ?: 0}", sh("MAĞLUBİYET", "LOSSES"))
                        UnifiedHeroMetric(if (profile?.isVip == true) "PRO" else "FREE", sh("ÜYELİK", "PLAN"))
                    }
                }
            }
        }

        item {
            Button(
                onClick = onPlay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .sonHarfPressScale(pressedScale = 0.985f),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(containerColor = UnifiedUi.Blue),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
            ) {
                Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(31.dp))
                Spacer(Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(sh("OYNA", "PLAY"), fontSize = 23.sp, fontWeight = FontWeight.Black)
                    Text(sh("Premier 1v1 kelime düellosu", "Premier 1v1 word duel"), fontSize = 9.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .80f))
                }
            }
        }

        item {
            WeeklyChampionPodium(
                players = weeklyTop,
                loading = weeklyTopLoading,
                onOpenLeague = onLeague,
            )
        }

        item {
            Text(sh("DİĞER OYUNLAR", "OTHER GAMES"), color = UnifiedUi.Text, fontWeight = FontWeight.Black, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            UnifiedModeCard(
                icon = Icons.Rounded.GridView,
                title = sh("KELİME KUŞATMASI", "WORD SIEGE"),
                subtitle = sh("Alanı ele geçir, küpleri koru", "Capture territory and protect cubes"),
                accent = UnifiedUi.Gold,
                onClick = onSiege,
            )
            Spacer(Modifier.height(9.dp))
            UnifiedModeCard(
                icon = Icons.Rounded.Route,
                title = sh("HARF YOLU", "LETTER PATH"),
                subtitle = sh("Hedef kelimeye ulaş", "Reach the target word"),
                accent = UnifiedUi.Green,
                onClick = onLetter,
            )
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                UnifiedQuickTile(Icons.Rounded.EmojiEvents, sh("LİG", "LEAGUE"), UnifiedUi.Gold, Modifier.weight(1f), onLeague)
                UnifiedQuickTile(Icons.Rounded.Groups, sh("SOSYAL", "SOCIAL"), UnifiedUi.Green, Modifier.weight(1f), onSocial)
                UnifiedQuickTile(Icons.Rounded.Storefront, sh("MAĞAZA", "SHOP"), UnifiedUi.Blue, Modifier.weight(1f), onShop)
            }
        }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .sonHarfPressScale(pressedScale = 0.985f)
                    .clickable(onClick = onCompetition),
                shape = RoundedCornerShape(20.dp),
                color = UnifiedUi.Surface,
                border = BorderStroke(1.dp, UnifiedUi.Border),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = UnifiedUi.Red.copy(alpha = .14f)) {
                        Icon(Icons.Rounded.Bolt, null, tint = UnifiedUi.Red, modifier = Modifier.padding(11.dp).size(24.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(sh("REKABET MERKEZİ", "COMPETITION HUB"), color = UnifiedUi.Text, fontWeight = FontWeight.Black)
                        Text(sh("Turnuvalar • ezeli rakip • haftalık hedefler", "Tournaments • arch rival • weekly goals"), color = UnifiedUi.Muted, fontSize = 10.sp)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = UnifiedUi.Muted)
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun WeeklyChampionPodium(
    players: List<WeeklyPodiumPlayer>,
    loading: Boolean,
    onOpenLeague: () -> Unit,
) {
    val premiumGold = Color(0xFFF0CF75)
    val deepForest = Color(0xFF18342E)
    val deepBlue = Color(0xFF263E47)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(28.dp))
            .sonHarfPressScale(pressedScale = 0.99f)
            .clickable(onClick = onOpenLeague),
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, premiumGold.copy(alpha = .52f)),
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(deepForest, deepBlue)))
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(15.dp),
                    color = premiumGold.copy(alpha = .14f),
                    border = BorderStroke(1.dp, premiumGold.copy(alpha = .28f)),
                ) {
                    Icon(
                        Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = premiumGold,
                        modifier = Modifier.padding(9.dp).size(22.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        sh("HAFTANIN ZİRVESİ", "WEEKLY PODIUM"),
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = .6.sp,
                    )
                    Text(
                        sh("Haftanın en güçlü 3 oyuncusu", "The week's top 3 players"),
                        color = Color.White.copy(alpha = .68f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = Color.White.copy(alpha = .08f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = .10f)),
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(sh("TÜMÜ", "ALL"), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(3.dp))
                        Icon(Icons.Rounded.ChevronRight, null, tint = premiumGold, modifier = Modifier.size(14.dp))
                    }
                }
            }

            if (loading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = premiumGold,
                    trackColor = Color.White.copy(alpha = .10f),
                )
            }

            if (players.isEmpty()) {
                WeeklyPodiumEmptyState(loading = loading, gold = premiumGold)
            } else {
                WeeklyChampionHero(player = players.first(), gold = premiumGold)

                val second = players.getOrNull(1)
                val third = players.getOrNull(2)
                if (second != null || third != null) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (second != null) {
                            WeeklyRunnerCard(
                                place = 2,
                                player = second,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (third != null) {
                            WeeklyRunnerCard(
                                place = 3,
                                player = third,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (second != null && third == null) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyChampionHero(
    player: WeeklyPodiumPlayer,
    gold: Color,
) {
    val profile = player.profile
    val avatarPath = if (profile?.avatarVisibility == "hidden") null else profile?.avatarPath
    val name = player.row.displayName.ifBlank { sh("Oyuncu", "Player") }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = .085f),
        border = BorderStroke(1.5.dp, gold.copy(alpha = .70f)),
        shadowElevation = 5.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfilePhotoAvatarWithGender(
                avatarPath = avatarPath,
                gender = profile?.gender,
                name = name,
                size = 64.dp,
                accent = gold,
                visible = profile?.avatarVisibility != "hidden",
                showGenderBadge = false,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    sh("HAFTA ŞAMPİYONU", "WEEKLY CHAMPION"),
                    color = gold,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = .9.sp,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    name,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${player.row.rating} RP",
                        color = Color.White.copy(alpha = .76f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (profile?.isVip == true) {
                        Spacer(Modifier.width(7.dp))
                        Surface(shape = RoundedCornerShape(99.dp), color = gold.copy(alpha = .16f)) {
                            Text(
                                "PRO",
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                color = gold,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }
            }
            Surface(
                shape = RoundedCornerShape(17.dp),
                color = gold,
                shadowElevation = 4.dp,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Rounded.EmojiEvents, null, tint = Color(0xFF725200), modifier = Modifier.size(17.dp))
                    Text("#1", color = Color(0xFF725200), fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun WeeklyRunnerCard(
    place: Int,
    player: WeeklyPodiumPlayer,
    modifier: Modifier,
) {
    val accent = if (place == 2) Color(0xFFC8D0D6) else Color(0xFFC98B62)
    val profile = player.profile
    val avatarPath = if (profile?.avatarVisibility == "hidden") null else profile?.avatarPath
    val name = player.row.displayName.ifBlank { sh("Oyuncu", "Player") }

    Surface(
        modifier = modifier.height(108.dp),
        shape = RoundedCornerShape(19.dp),
        color = Color.White.copy(alpha = .065f),
        border = BorderStroke(1.dp, accent.copy(alpha = .54f)),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.MilitaryTech,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    sh("$place. SIRA", "#$place PLACE"),
                    color = accent,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = .5.sp,
                )
                Spacer(Modifier.weight(1f))
                if (profile?.isVip == true) {
                    Text("PRO", color = accent, fontSize = 6.sp, fontWeight = FontWeight.Black)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfilePhotoAvatarWithGender(
                    avatarPath = avatarPath,
                    gender = profile?.gender,
                    name = name,
                    size = 40.dp,
                    accent = accent,
                    visible = profile?.avatarVisibility != "hidden",
                    showGenderBadge = false,
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        name,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${player.row.rating} RP",
                        color = Color.White.copy(alpha = .62f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyPodiumEmptyState(
    loading: Boolean,
    gold: Color,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = .06f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .10f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = gold.copy(alpha = .12f)) {
                Icon(
                    Icons.Rounded.EmojiEvents,
                    null,
                    tint = gold.copy(alpha = .82f),
                    modifier = Modifier.padding(9.dp).size(18.dp),
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (loading) sh("Sıralama yükleniyor", "Loading rankings") else sh("Haftalık sıralama hazırlanıyor", "Weekly ranking is taking shape"),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (loading) sh("Güncel ilk 3 oyuncu getiriliyor…", "Fetching the current top three…") else sh("İlk sonuçlar geldiğinde şampiyonlar burada görünecek.", "Champions will appear here as soon as results arrive."),
                    color = Color.White.copy(alpha = .58f),
                    fontSize = 8.sp,
                )
            }
        }
    }
}

@Composable
private fun UnifiedHeroMetric(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
        Text(label, color = Color.White.copy(alpha = .72f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun UnifiedModeCard(icon: ImageVector, title: String, subtitle: String, accent: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .sonHarfPressScale(pressedScale = 0.985f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = UnifiedUi.Surface,
        border = BorderStroke(1.dp, accent.copy(alpha = .32f)),
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(15.dp), color = accent.copy(alpha = .14f)) {
                Icon(icon, null, tint = accent, modifier = Modifier.padding(12.dp).size(29.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = UnifiedUi.Text, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Text(subtitle, color = UnifiedUi.Muted, fontSize = 10.sp)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = UnifiedUi.Muted)
        }
    }
}

@Composable
private fun UnifiedQuickTile(icon: ImageVector, title: String, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier
            .height(90.dp)
            .sonHarfPressScale(pressedScale = 0.96f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = accent.copy(alpha = .09f),
        border = BorderStroke(1.dp, accent.copy(alpha = .28f)),
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(7.dp))
            Text(title, color = UnifiedUi.Text, fontWeight = FontWeight.Black, fontSize = 10.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun UnifiedRoundAction(icon: ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(44.dp)
            .sonHarfPressScale(pressedScale = 0.94f)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = UnifiedUi.Surface,
        border = BorderStroke(1.dp, UnifiedUi.Border),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = UnifiedUi.Text, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun UnifiedBottomBar(
    destination: UnifiedDestination,
    onHome: () -> Unit,
    onLeague: () -> Unit,
    onSocial: () -> Unit,
    onShop: () -> Unit,
    onProfile: () -> Unit,
) {
    NavigationBar(containerColor = UnifiedUi.Navigation, tonalElevation = 0.dp) {
        listOf(
            Triple(UnifiedDestination.HOME, Icons.Rounded.Home, sh("ANA", "HOME")) to onHome,
            Triple(UnifiedDestination.LEAGUE, Icons.Rounded.EmojiEvents, sh("LİG", "LEAGUE")) to onLeague,
            Triple(UnifiedDestination.SOCIAL, Icons.Rounded.Groups, sh("SOSYAL", "SOCIAL")) to onSocial,
            Triple(UnifiedDestination.SHOP, Icons.Rounded.Storefront, sh("MAĞAZA", "SHOP")) to onShop,
            Triple(UnifiedDestination.PROFILE, Icons.Rounded.Person, sh("PROFİL", "PROFILE")) to onProfile,
        ).forEach { pair ->
            val item = pair.first
            val action = pair.second
            NavigationBarItem(
                selected = destination == item.first,
                onClick = action,
                icon = { Icon(item.second, null) },
                label = { Text(item.third, fontSize = 8.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = UnifiedUi.Blue,
                    selectedTextColor = UnifiedUi.Blue,
                    indicatorColor = UnifiedUi.Blue.copy(alpha = .12f),
                    unselectedIconColor = UnifiedUi.Muted,
                    unselectedTextColor = UnifiedUi.Muted,
                )
            )
        }
    }
}
