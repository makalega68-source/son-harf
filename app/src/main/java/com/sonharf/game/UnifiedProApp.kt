package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
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

internal data class WeeklyPodiumPlayer(
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
    val Gold: Color get() = SonHarfTheme.PremiumGold
    val Green: Color get() = SonHarfTheme.Success
    val Red: Color get() = SonHarfTheme.Error
    val Purple: Color get() = SonHarfTheme.Lavender
    val OnPrimary: Color get() = SonHarfTheme.OnPrimary
    val OnSecondary: Color get() = SonHarfTheme.OnSecondary
    val OnTertiary: Color get() = SonHarfTheme.OnTertiary
    val HeroStart: Color get() = SonHarfTheme.HeroStart
    val HeroMiddle: Color get() = SonHarfTheme.HeroMiddle
    val HeroEnd: Color get() = SonHarfTheme.HeroEnd
    val Forest: Color get() = SonHarfTheme.Forest
    val ForestDeep: Color get() = SonHarfTheme.ForestDeep
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
                if (topLevel) {
                    UnifiedBottomBar(
                        destination = destination,
                        onHome = { destination = UnifiedDestination.HOME },
                        onLeague = { destination = UnifiedDestination.LEAGUE },
                        onSocial = { destination = UnifiedDestination.SOCIAL },
                        onShop = { destination = UnifiedDestination.SHOP },
                        onProfile = { destination = UnifiedDestination.PROFILE },
                    )
                }
            },
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                if (!SonHarfTheme.IsDark) {
                    SonHarfLeafBackdrop(Modifier.matchParentSize())
                }
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
                    UnifiedDestination.SOCIAL -> MainSocialScreen(
                        backend = backend,
                        onPlay = { destination = UnifiedDestination.GAME },
                        onSiege = { destination = UnifiedDestination.SIEGE },
                    )
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
                        { destination = UnifiedDestination.SIEGE },
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        item {
            HomeBrandHeader(onTasks = onTasks, onVip = onVip)
        }

        item {
            PremiumProfileHero(profile = profile, onProfile = onProfile)
        }

        item {
            PremiumPlayButton(onClick = onSiege)
        }

        item {
            WeeklyPodiumCardV210(
                players = weeklyTop,
                loading = weeklyTopLoading,
                onOpenLeague = onLeague,
            )
        }

        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Text(
                    sh("DİĞER OYUNLAR", "OTHER GAMES"),
                    color = UnifiedUi.Text,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    sh("Kısa oyunlar ve farklı meydan okumalar", "Quick games and other challenges"),
                    color = UnifiedUi.Muted,
                    fontSize = 8.sp,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PremiumModeCard(
                    modifier = Modifier.weight(1f),
                    logoRes = R.drawable.son_harf_app_icon_master,
                    title = sh("SON HARF", "LAST LETTER"),
                    subtitle = sh("Kısa ve hızlı\nkelime düellosu", "Fast, short\nword duel"),
                    colors = listOf(Color(0xFFE5EFE8), Color(0xFFF5F0E6)),
                    onClick = onPlay,
                )
                PremiumModeCard(
                    modifier = Modifier.weight(1f),
                    logoRes = R.drawable.harf_yolu_logo,
                    title = sh("HARF YOLU", "LETTER PATH"),
                    subtitle = sh("Her kelime seni\nhedefe yaklaştırır", "Every word moves\nyou closer"),
                    colors = listOf(Color(0xFFCBE7EE), Color(0xFFE8F4E7)),
                    onClick = onLetter,
                )
            }
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
                color = UnifiedUi.Surface.copy(alpha = .96f),
                border = BorderStroke(1.dp, UnifiedUi.Border),
                shadowElevation = 2.dp,
            ) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = UnifiedUi.Gold.copy(alpha = .16f)) {
                        Icon(Icons.Rounded.Bolt, null, tint = Color(0xFF9A7131), modifier = Modifier.padding(10.dp).size(23.dp))
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(sh("REKABET MERKEZİ", "COMPETITION HUB"), color = UnifiedUi.Text, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Text(sh("Turnuvalar • ezeli rakip • haftalık hedefler", "Tournaments • arch rival • weekly goals"), color = UnifiedUi.Muted, fontSize = 9.sp)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = UnifiedUi.Blue)
                }
            }
        }

        item { Spacer(Modifier.height(6.dp)) }
    }
}

@Composable
private fun HomeBrandHeader(onTasks: () -> Unit, onVip: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            SonHarfOfficialLogo(
                modifier = Modifier.width(205.dp).height(61.dp),
            )
            Text(
                sh("Kelimeyi kur, alanı kuşat, rakibini geç", "Build words, control territory, beat your rival"),
                color = Color(0xFF4F7964),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 9.dp, top = 1.dp),
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UnifiedRoundAction(Icons.Rounded.Notifications, onTasks)
                UnifiedRoundAction(Icons.Rounded.WorkspacePremium, onVip)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                sh("Kelime oyunu +\ntaktik alan savaşı", "Word game +\ntactical territory battle"),
                color = Color(0xFF436C59),
                fontSize = 7.5.sp,
                lineHeight = 8.5.sp,
                textAlign = TextAlign.End,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun PremiumProfileHero(profile: ProfileDto?, onProfile: () -> Unit) {
    val shape = RoundedCornerShape(27.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(13.dp, shape)
            .clip(shape)
            .background(Brush.linearGradient(listOf(UnifiedUi.HeroStart, UnifiedUi.HeroMiddle, UnifiedUi.HeroEnd)))
            .border(1.dp, Color.White.copy(alpha = .22f), shape)
            .clickable(onClick = onProfile)
            .padding(horizontal = 17.dp, vertical = 16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfilePhotoAvatarWithGender(
                    avatarPath = profile?.avatarPath,
                    gender = profile?.gender,
                    name = profile?.displayName ?: sh("Oyuncu", "Player"),
                    size = 61.dp,
                    accent = Color(0xFFF4FFF8),
                    visible = profile?.avatarVisibility != "hidden",
                    showGenderBadge = false,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        profile?.displayName ?: sh("OYUNCU", "PLAYER"),
                        color = Color.White,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.EmojiEvents, null, tint = Color(0xFFF0C557), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "${profile?.rating ?: 1000} RP",
                            color = Color.White.copy(alpha = .90f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = Color(0xFFFFFCF2),
                    shadowElevation = 3.dp,
                ) {
                    Row(
                        Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.WorkspacePremium, null, tint = Color(0xFF8C6834), modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "SC ${profile?.diamonds ?: 0}",
                            color = Color(0xFF795A2E),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Spacer(Modifier.width(3.dp))
                        Icon(Icons.Rounded.ChevronRight, null, tint = Color(0xFF8B7450), modifier = Modifier.size(17.dp))
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                UnifiedHeroMetric("${profile?.wins ?: 0}", sh("GALİBİYET", "WINS"), Modifier.weight(1f))
                HeroDivider()
                UnifiedHeroMetric("${profile?.losses ?: 0}", sh("MAĞLUBİYET", "LOSSES"), Modifier.weight(1f))
                HeroDivider()
                UnifiedHeroMetric(if (profile?.isVip == true) "PRO" else "FREE", sh("ÜYELİK", "PLAN"), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HeroDivider() {
    Box(Modifier.width(1.dp).height(39.dp).background(Color.White.copy(alpha = .16f)))
}

@Composable
private fun UnifiedHeroMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(3.dp))
        Text(label, color = Color.White.copy(alpha = .75f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PremiumPlayButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
            .shadow(8.dp, shape)
            .sonHarfPressScale(pressedScale = .985f)
            .clickable(onClick = onClick),
        shape = shape,
        color = Color.Transparent,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(listOf(UnifiedUi.Forest, UnifiedUi.ForestDeep))),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Shield, null, tint = Color.White, modifier = Modifier.size(35.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(sh("KELİME KUŞATMASI", "WORD SIEGE"), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text(sh("Kelime kur • alanı ele geçir • haritayı kontrol et", "Build words • capture territory • control the map"), color = Color.White.copy(alpha = .82f), fontSize = 9.sp)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = Color.White.copy(alpha = .88f), modifier = Modifier.size(27.dp))
            }
        }
    }
}

@Composable
private fun WeeklyChampionPodium(
    players: List<WeeklyPodiumPlayer>,
    loading: Boolean,
    onOpenLeague: () -> Unit,
) {
    val gold = Color(0xFFF1CF70)
    val silver = Color(0xFFD7E0E6)
    val bronze = Color(0xFFD49A70)
    val shape = RoundedCornerShape(27.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(13.dp, shape)
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF153A32), Color(0xFF1C4C43), Color(0xFF243F48))))
            .border(1.dp, gold.copy(alpha = .55f), shape)
            .sonHarfPressScale(pressedScale = .99f)
            .clickable(onClick = onOpenLeague),
    ) {
        PodiumAmbientDecor(Modifier.matchParentSize())
        Column(Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = gold.copy(alpha = .14f),
                    border = BorderStroke(1.dp, gold.copy(alpha = .28f)),
                ) {
                    Icon(Icons.Rounded.EmojiEvents, null, tint = gold, modifier = Modifier.padding(8.dp).size(22.dp))
                }
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(sh("HAFTANIN ZİRVESİ", "WEEKLY PODIUM"), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    Text(sh("Bu haftanın en güçlü oyuncuları", "This week's strongest players"), color = Color.White.copy(alpha = .68f), fontSize = 9.sp)
                }
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = Color.White.copy(alpha = .07f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = .15f)),
                ) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(sh("TÜMÜ", "ALL"), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        Icon(Icons.Rounded.ChevronRight, null, tint = gold, modifier = Modifier.size(14.dp))
                    }
                }
            }

            if (loading) {
                Spacer(Modifier.height(7.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = gold,
                    trackColor = Color.White.copy(alpha = .08f),
                )
            }

            Spacer(Modifier.height(13.dp))
            Row(
                Modifier.fillMaxWidth().height(185.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                PodiumColumn(
                    place = 2,
                    player = players.getOrNull(1),
                    accent = silver,
                    modifier = Modifier.weight(.92f),
                    podiumHeight = 118.dp,
                )
                PodiumColumn(
                    place = 1,
                    player = players.getOrNull(0),
                    accent = gold,
                    modifier = Modifier.weight(1.16f),
                    podiumHeight = 145.dp,
                )
                PodiumColumn(
                    place = 3,
                    player = players.getOrNull(2),
                    accent = bronze,
                    modifier = Modifier.weight(.92f),
                    podiumHeight = 108.dp,
                )
            }

            if (players.isEmpty() && !loading) {
                Text(
                    sh("İlk sonuçlarla birlikte gerçek oyuncular bu podyumda görünecek.", "Real players will appear on this podium when the first results arrive."),
                    color = Color.White.copy(alpha = .56f),
                    fontSize = 8.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun PodiumAmbientDecor(modifier: Modifier) {
    Canvas(modifier) {
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0x55F6D375), Color.Transparent)),
            radius = size.minDimension * .38f,
            center = Offset(size.width * .50f, size.height * .66f),
        )
        val confetti = listOf(
            .09f to .40f, .17f to .53f, .28f to .34f, .72f to .36f,
            .82f to .49f, .91f to .38f, .60f to .29f, .40f to .31f,
        )
        confetti.forEachIndexed { index, point ->
            val c = if (index % 2 == 0) Color(0xFFEBC85C) else Color(0xFFF4E3A1)
            drawCircle(c.copy(alpha = .66f), radius = if (index % 3 == 0) 2.6f else 1.8f, center = Offset(size.width * point.first, size.height * point.second))
        }
    }
}

@Composable
private fun PodiumColumn(
    place: Int,
    player: WeeklyPodiumPlayer?,
    accent: Color,
    modifier: Modifier,
    podiumHeight: androidx.compose.ui.unit.Dp,
) {
    val profile = player?.profile
    val displayName = player?.row?.displayName?.ifBlank { sh("Oyuncu", "Player") } ?: "—"
    val rating = player?.row?.rating
    val avatarSize = if (place == 1) 55.dp else 45.dp

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        if (place == 1) {
            Icon(Icons.Rounded.WorkspacePremium, null, tint = accent, modifier = Modifier.size(27.dp))
            Spacer(Modifier.height(1.dp))
        }
        Surface(
            shape = CircleShape,
            color = Color(0xFF173A34),
            border = BorderStroke(if (place == 1) 2.5.dp else 2.dp, accent),
            shadowElevation = if (place == 1) 9.dp else 5.dp,
        ) {
            Box(Modifier.size(avatarSize), contentAlignment = Alignment.Center) {
                if (player != null) {
                    ProfilePhotoAvatarWithGender(
                        avatarPath = if (profile?.avatarVisibility == "hidden") null else profile?.avatarPath,
                        gender = profile?.gender,
                        name = displayName,
                        size = avatarSize,
                        accent = accent,
                        visible = profile?.avatarVisibility != "hidden",
                        showGenderBadge = false,
                    )
                } else {
                    Icon(Icons.Rounded.Person, null, tint = Color.White.copy(alpha = .42f), modifier = Modifier.size(avatarSize * .52f))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(displayName, color = Color.White, fontSize = if (place == 1) 11.sp else 9.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(if (rating != null) "$rating RP" else "— RP", color = accent, fontSize = if (place == 1) 10.sp else 8.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(podiumHeight)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            accent.copy(alpha = .38f),
                            accent.copy(alpha = .19f),
                            Color.White.copy(alpha = .045f),
                        ),
                    ),
                )
                .border(1.dp, accent.copy(alpha = .58f), RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 7.dp)) {
                Text("$place", color = accent, fontSize = if (place == 1) 22.sp else 18.sp, fontWeight = FontWeight.Black)
                if (place == 1) {
                    Text(sh("ŞAMPİYON", "CHAMPION"), color = Color.White.copy(alpha = .78f), fontSize = 6.5.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun PremiumModeCard(
    modifier: Modifier,
    logoRes: Int,
    title: String,
    subtitle: String,
    colors: List<Color>,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .height(128.dp)
            .shadow(4.dp, shape)
            .clip(shape)
            .background(Brush.linearGradient(colors))
            .border(1.dp, Color.White.copy(alpha = .74f), shape)
            .sonHarfPressScale(pressedScale = .98f)
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .weight(.93f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(15.dp))
                    .background(Color.White.copy(alpha = .34f)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(logoRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(2.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            Spacer(Modifier.width(7.dp))
            Column(Modifier.weight(1.07f), verticalArrangement = Arrangement.Center) {
                Text(title, color = Color(0xFF1E3B31), fontSize = 11.sp, lineHeight = 12.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(5.dp))
                Text(subtitle, color = Color(0xFF60746A), fontSize = 7.5.sp, lineHeight = 9.5.sp)
                Spacer(Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(99.dp), color = Color(0xFFFFFDF6).copy(alpha = .92f)) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(sh("Hemen Oyna", "Play Now"), color = Color(0xFF355D4D), fontSize = 7.sp, fontWeight = FontWeight.Black)
                        Icon(Icons.Rounded.ChevronRight, null, tint = Color(0xFF8A6D38), modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun UnifiedQuickTile(
    icon: ImageVector,
    title: String,
    accent: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .height(82.dp)
            .sonHarfPressScale(pressedScale = 0.96f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = UnifiedUi.Surface.copy(alpha = .91f),
        border = BorderStroke(1.dp, accent.copy(alpha = .24f)),
    ) {
        Column(
            Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(23.dp))
            Spacer(Modifier.height(6.dp))
            Text(title, color = UnifiedUi.Text, fontWeight = FontWeight.Black, fontSize = 9.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun UnifiedRoundAction(icon: ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(48.dp)
            .sonHarfPressScale(pressedScale = 0.94f)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = Color(0xFFFBFCF7).copy(alpha = .94f),
        border = BorderStroke(1.2.dp, Color(0xFFB9CCC0)),
        shadowElevation = 1.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color(0xFF244D3D), modifier = Modifier.size(21.dp))
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
    NavigationBar(
        containerColor = Color(0xFFF2F6F0).copy(alpha = .98f),
        tonalElevation = 0.dp,
        modifier = Modifier.border(0.5.dp, Color(0xFFDDE6DF)),
    ) {
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
                label = { Text(item.third, fontSize = 8.sp, fontWeight = if (destination == item.first) FontWeight.Bold else FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF3F745E),
                    selectedTextColor = Color(0xFF3F745E),
                    indicatorColor = Color(0xFFDDE9E1),
                    unselectedIconColor = Color(0xFF6A8075),
                    unselectedTextColor = Color(0xFF6A8075),
                ),
            )
        }
    }
}
