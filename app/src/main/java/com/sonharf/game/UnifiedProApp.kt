package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.LeaderboardV2Row
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.getLeaderboardV2
import com.sonharf.game.mascot.MageCatCompanion
import com.sonharf.game.mascot.MageCatDirector
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
    val Border: Color get() = SonHarfTheme.Border
    val Text: Color get() = SonHarfTheme.TextPrimary
    val Muted: Color get() = SonHarfTheme.TextSecondary
    val Blue: Color get() = SonHarfTheme.PrimaryBlue
    val Cyan: Color get() = SonHarfTheme.SecondaryAccent
    val Gold: Color get() = SonHarfTheme.Warning
    val Green: Color get() = SonHarfTheme.Success
    val Red: Color get() = SonHarfTheme.Error
    val Purple: Color get() = SonHarfTheme.Purple

    val HeroStart: Color get() = if (SonHarfTheme.IsDark) Color(0xFF173E35) else Color(0xFF347B68)
    val HeroMiddle: Color get() = if (SonHarfTheme.IsDark) Color(0xFF25483F) else Color(0xFF28705F)
    val HeroEnd: Color get() = if (SonHarfTheme.IsDark) Color(0xFF3A3320) else Color(0xFF465C7A)
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
    val gameplay = destination in setOf(UnifiedDestination.GAME, UnifiedDestination.SIEGE, UnifiedDestination.LETTER, UnifiedDestination.DAILY)
    val scheme = if (SonHarfTheme.IsDark) {
        darkColorScheme(
            primary = UnifiedUi.Blue,
            secondary = UnifiedUi.Cyan,
            tertiary = UnifiedUi.Green,
            background = UnifiedUi.Background,
            surface = UnifiedUi.Surface,
            surfaceVariant = UnifiedUi.Surface2,
            onPrimary = Color(0xFF211700),
            onSecondary = Color(0xFF211700),
            onTertiary = Color(0xFF05251B),
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
            onPrimary = Color.White,
            onSecondary = Color.White,
            onTertiary = Color.White,
            onBackground = UnifiedUi.Text,
            onSurface = UnifiedUi.Text,
            onSurfaceVariant = UnifiedUi.Text,
            error = UnifiedUi.Red,
        )
    }

    MaterialTheme(colorScheme = scheme) {
        Scaffold(
            containerColor = UnifiedUi.Background,
            topBar = { SonHarfTopAdBanner(visible = !gameplay, isPremium = isPro) },
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
            ) {
                if (!SonHarfTheme.IsDark) SonHarfLeafBackdrop(Modifier.matchParentSize())
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
    var weeklyLeaders by remember { mutableStateOf<List<LeaderboardV2Row>>(emptyList()) }

    LaunchedEffect(Unit) {
        MageCatDirector.onLobbyGreet()
        if (!SupabaseProvider.configured) return@LaunchedEffect
        val id = backend.currentUserId()
        profile = id?.let { runCatching { backend.getProfile(it) }.getOrNull() }
        weeklyLeaders = runCatching {
            backend.getLeaderboardV2(if (SonHarfUiState.isEnglish) "en" else "tr", "week", 3)
        }.getOrDefault(emptyList())
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SonHarfBrandLogo(modifier = Modifier.weight(1f).height(102.dp), size = null)
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
                        Surface(shape = RoundedCornerShape(99.dp), color = UnifiedUi.Gold) {
                            Text("SC ${profile?.diamonds ?: 0}", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = Color(0xFF201600), fontWeight = FontWeight.Black)
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
                modifier = Modifier.fillMaxWidth().height(70.dp),
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
            UnifiedWeeklyPodium(weeklyLeaders, onLeague)
        }

        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Text(sh("DİĞER OYUNLAR", "MORE GAMES"), color = UnifiedUi.Text, fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.weight(1f))
                Text(sh("Daha fazla kelime, daha fazla eğlence!", "More words, more fun!"), color = UnifiedUi.Muted, fontSize = 8.sp)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                UnifiedBrandModeCard(
                    image = R.drawable.kelime_kusatmasi_brand,
                    title = sh("KELİME KUŞATMASI", "WORD SIEGE"),
                    subtitle = sh("Alanı ele geçir, küpleri koru", "Capture territory and protect cubes"),
                    modifier = Modifier.weight(1f),
                    onClick = onSiege,
                )
                UnifiedBrandModeCard(
                    image = R.drawable.kelime_yolu_brand,
                    title = sh("KELİME YOLU", "WORD ROAD"),
                    subtitle = sh("Her kelime seni hedefe yaklaştırır", "Every word moves you closer"),
                    modifier = Modifier.weight(1f),
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
                modifier = Modifier.fillMaxWidth().clickable(onClick = onCompetition),
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

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = UnifiedUi.Surface,
                border = BorderStroke(1.dp, UnifiedUi.Border),
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    MageCatCompanion(size = 58.dp, onClick = { MageCatDirector.onLobbyGreet() })
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(sh("Kelime yoldaşın hazır", "Your word companion is ready"), color = UnifiedUi.Text, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Text(sh("Her kelime yeni bir meydan okuma!", "Every word is a new challenge!"), color = UnifiedUi.Muted, fontSize = 9.sp)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
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
private fun UnifiedWeeklyPodium(leaders: List<LeaderboardV2Row>, onAll: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF123E35),
        border = BorderStroke(1.dp, UnifiedUi.Gold.copy(alpha = .38f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = UnifiedUi.Gold.copy(alpha = .15f)) {
                    Icon(Icons.Rounded.EmojiEvents, null, tint = UnifiedUi.Gold, modifier = Modifier.padding(9.dp).size(23.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(sh("HAFTANIN ZİRVESİ", "WEEKLY LEADERS"), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text(sh("Bu haftanın en güçlü oyuncuları", "This week's strongest players"), color = Color(0xFFBDD4CB), fontSize = 10.sp)
                }
                TextButton(onClick = onAll) { Text(sh("TÜMÜ  ›", "ALL  ›"), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black) }
            }
            Spacer(Modifier.height(15.dp))
            val order = listOf(1, 0, 2)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                order.forEach { index ->
                    val row = leaders.getOrNull(index)
                    UnifiedPodiumPlayer(
                        place = index + 1,
                        name = row?.displayName ?: "—",
                        rating = row?.rating,
                        modifier = Modifier.weight(if (index == 0) 1.15f else 1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun UnifiedPodiumPlayer(place: Int, name: String, rating: Int?, modifier: Modifier = Modifier) {
    val accent = when (place) {
        1 -> Color(0xFFF2C44F)
        2 -> Color(0xFFB9CAD6)
        else -> Color(0xFFD08B60)
    }
    val avatarSize = if (place == 1) 62.dp else 50.dp
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (place == 1) "♛" else "◆", color = accent, fontSize = if (place == 1) 27.sp else 17.sp)
        Surface(shape = CircleShape, color = accent.copy(alpha = .18f), border = BorderStroke(3.dp, accent), modifier = Modifier.size(avatarSize)) {
            Box(contentAlignment = Alignment.Center) {
                Text(name.take(1).uppercase().ifBlank { place.toString() }, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1)
        Text(rating?.let { "$it RP" } ?: "— RP", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(if (place == 1) 12.dp else 7.dp))
        Box(
            Modifier
                .width(if (place == 1) 82.dp else 68.dp)
                .height(if (place == 1) 25.dp else 18.dp)
                .background(accent.copy(alpha = .28f), RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(place.toString(), color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun UnifiedBrandModeCard(
    image: Int,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.height(164.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = UnifiedUi.Surface,
        border = BorderStroke(1.dp, UnifiedUi.Border),
    ) {
        Column {
            Image(
                painter = painterResource(image),
                contentDescription = title,
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentScale = ContentScale.Crop,
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, color = UnifiedUi.Text, fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    Text(subtitle, color = UnifiedUi.Muted, fontSize = 7.sp, maxLines = 2)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = UnifiedUi.Blue, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun UnifiedModeCard(icon: ImageVector, title: String, subtitle: String, accent: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
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
        modifier = modifier.height(90.dp).clickable(onClick = onClick),
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
        modifier = Modifier.size(44.dp).clickable(onClick = onClick),
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
    NavigationBar(containerColor = UnifiedUi.Surface, tonalElevation = 0.dp) {
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
