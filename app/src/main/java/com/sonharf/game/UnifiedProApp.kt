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
                    UnifiedDestination.LETTER -> KelimeAtolyesiScreen { destination = UnifiedDestination.HOME }
                    UnifiedDestination.LEAGUE -> LeaderboardExperienceScreen { destination = UnifiedDestination.HOME }
                    UnifiedDestination.COMPETITION -> CompetitionHubScreen(
                        onBack = { destination = UnifiedDestination.HOME },
                    )
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
    var profile by remember { mutableStateOf<ProfileDto?>(OwnProfile.snapshot()) }
    LaunchedEffect(Unit) {
        if (!SupabaseProvider.configured) return@LaunchedEffect
        val id = backend.currentUserId()
        profile = id?.let { runCatching { backend.getProfile(it) }.getOrNull() }
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
            DailyObjectiveCard(onClick = onTasks)
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
private fun DailyObjectiveCard(onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .sonHarfPressScale(pressedScale = .985f)
            .clickable(onClick = onClick),
        shape = shape,
        color = UnifiedUi.Surface.copy(alpha = .96f),
        border = BorderStroke(1.dp, UnifiedUi.Border),
        shadowElevation = 2.dp,
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = UnifiedUi.Purple.copy(alpha = .18f)) {
                Icon(Icons.Rounded.Flag, null, tint = UnifiedUi.Purple, modifier = Modifier.padding(10.dp).size(22.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(sh("BUGÜNÜN HEDEFİ", "TODAY'S OBJECTIVE"), color = UnifiedUi.Text, fontWeight = FontWeight.Black, fontSize = 12.sp)
                Text(sh("Görevini tamamla, XP'ni ve serini büyüt.", "Complete your task to grow XP and your streak."), color = UnifiedUi.Muted, fontSize = 9.sp)
            }
            Surface(shape = RoundedCornerShape(99.dp), color = UnifiedUi.Purple.copy(alpha = .14f)) {
                Text(sh("GÖR", "VIEW"), Modifier.padding(horizontal = 9.dp, vertical = 6.dp), color = UnifiedUi.Purple, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun HomeBrandHeader(onTasks: () -> Unit, onVip: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(82.dp),
        contentAlignment = Alignment.Center,
    ) {
        UnifiedRoundAction(
            icon = Icons.Rounded.Notifications,
            onClick = onTasks,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SonHarfOfficialLogo(
                modifier = Modifier.width(184.dp).height(53.dp),
            )
            Text(
                sh("Kelimeyi kur, alanı kuşat, rakibini geç", "Build words, control territory, beat your rival"),
                color = UnifiedUi.Muted,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
        UnifiedRoundAction(
            icon = Icons.Rounded.WorkspacePremium,
            onClick = onVip,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
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
                    frameId = rememberPlayerFrame(profile?.id),
                    gender = profile?.gender,
                    name = profile?.displayName ?: sh("Oyuncu", "Player"),
                    size = 61.dp,
                    accent = Color(0xFFF4FFF8),
                    visible = profile?.avatarVisibility != "hidden",
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
                            "${goldUnit()} ${profile?.diamonds ?: 0}",
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
    val shape = RoundedCornerShape(26.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(184.dp)
            .shadow(10.dp, shape)
            .sonHarfPressScale(pressedScale = .985f)
            .clickable(onClick = onClick),
        shape = shape,
        color = Color.Transparent,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(UnifiedUi.Forest, UnifiedUi.ForestDeep))),
        ) {
            Icon(
                Icons.Rounded.Shield,
                null,
                tint = Color.White.copy(alpha = .10f),
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(118.dp),
            )
            Column(
                Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = Color.White.copy(alpha = .13f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = .18f)),
                ) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Public, null, tint = Color(0xFFF0D37A), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(sh("TAKTİK ALAN SAVAŞI", "TACTICAL TERRITORY BATTLE"), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(sh("KELİME TAHTI", "WORD THRONE"), color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
                    Text(sh("Kelimeyi kur. Bölgeyi ele geçir. Rakibini geride bırak.", "Build words. Capture territory. Leave your rival behind."), color = Color.White.copy(alpha = .84f), fontSize = 11.sp, lineHeight = 15.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(15.dp), color = Color(0xFFF6D87E), shadowElevation = 3.dp) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.PlayArrow, null, tint = Color(0xFF174837), modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(sh("SAVAŞA GİR", "ENTER BATTLE"), color = Color(0xFF174837), fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(sh("HARİTA KONTROLÜ", "MAP CONTROL"), color = Color.White.copy(alpha = .65f), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        Text(sh("Her hamle bir bölge", "Every move claims ground"), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
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
private fun UnifiedRoundAction(icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .size(48.dp)
            .sonHarfPressScale(pressedScale = 0.94f)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = UnifiedUi.Surface,
        border = BorderStroke(1.2.dp, UnifiedUi.Border),
        shadowElevation = 1.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = UnifiedUi.Gold, modifier = Modifier.size(21.dp))
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
        containerColor = UnifiedUi.Navigation.copy(alpha = .98f),
        tonalElevation = 0.dp,
        modifier = Modifier.border(0.5.dp, UnifiedUi.Border),
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
                    selectedIconColor = UnifiedUi.Gold,
                    selectedTextColor = UnifiedUi.Gold,
                    indicatorColor = UnifiedUi.Surface2,
                    unselectedIconColor = UnifiedUi.Muted,
                    unselectedTextColor = UnifiedUi.Muted,
                ),
            )
        }
    }
}
