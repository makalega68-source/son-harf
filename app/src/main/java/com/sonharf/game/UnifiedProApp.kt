package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

private object UnifiedUi {
    val Background = Color(0xFFF6F8F4)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceSoft = Color(0xFFF0F4EF)
    val Border = Color(0xFFDDE5DE)
    val Text = Color(0xFF263A31)
    val Muted = Color(0xFF6F8177)
    val Blue = Color(0xFF527962)
    val BlueSoft = Color(0xFFDCE8E0)
    val Green = Color(0xFF729D8A)
    val Gold = Color(0xFFB68B47)
    val Red = Color(0xFFC45E5E)
    val HeroStart = Color(0xFF527A68)
    val HeroMiddle = Color(0xFF6E8F8E)
    val HeroEnd = Color(0xFF77749C)
}

private enum class UnifiedDestination {
    HOME, GAME, SIEGE, LETTER, LEAGUE, COMPETITION, SOCIAL, SHOP, PROFILE, SETTINGS, VIP, ACCOUNT, PROFILE_DETAILS, TASKS, DAILY
}

private data class WeeklyPodiumPlayer(
    val row: LeaderboardRowV2,
    val profile: ProfileDto?,
)

@Composable
fun UnifiedProApp(onSignedOut: () -> Unit = {}) {
    val backend = remember { OnlineGameBackend() }
    var destination by remember { mutableStateOf(UnifiedDestination.HOME) }

    Scaffold(
        containerColor = UnifiedUi.Background,
        bottomBar = {
            if (destination in setOf(
                    UnifiedDestination.HOME,
                    UnifiedDestination.LEAGUE,
                    UnifiedDestination.SOCIAL,
                    UnifiedDestination.SHOP,
                    UnifiedDestination.PROFILE,
                )
            ) {
                UnifiedBottomBar(
                    current = destination,
                    onSelect = { destination = it },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
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
                Column(Modifier.weight(1f)) {
                    Text("SON HARF", color = UnifiedUi.Text, fontSize = 27.sp, fontWeight = FontWeight.Black)
                    Text("Kelimeyi Sürdür, Rakibini Geç", color = UnifiedUi.Blue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(26.dp))
            .sonHarfPressScale(pressedScale = 0.99f)
            .clickable(onClick = onOpenLeague),
        shape = RoundedCornerShape(26.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color.White.copy(alpha = .20f)),
    ) {
        Column(
            Modifier
                .background(
                    Brush.linearGradient(
                        listOf(UnifiedUi.HeroStart, UnifiedUi.HeroMiddle, UnifiedUi.HeroEnd)
                    )
                )
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = Color.White.copy(alpha = .14f)) {
                    Icon(
                        Icons.Rounded.WorkspacePremium,
                        null,
                        tint = Color(0xFFFFD76A),
                        modifier = Modifier.padding(9.dp).size(20.dp),
                    )
                }
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        sh("HAFTANIN ZİRVESİ", "WEEKLY PODIUM"),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = .5.sp,
                    )
                    Text(
                        sh("Haftanın en iyi 3 oyuncusu", "Top 3 players of the week"),
                        color = Color.White.copy(alpha = .72f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Surface(shape = RoundedCornerShape(99.dp), color = Color.White.copy(alpha = .12f)) {
                    Row(Modifier.padding(horizontal = 9.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(sh("TÜMÜ", "ALL"), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(3.dp))
                        Icon(Icons.Rounded.ChevronRight, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }

            if (loading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = Color(0xFFFFD76A),
                    trackColor = Color.White.copy(alpha = .16f),
                )
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                WeeklyChampionCard(
                    place = 2,
                    player = players.getOrNull(1),
                    modifier = Modifier.weight(1f),
                )
                WeeklyChampionCard(
                    place = 1,
                    player = players.getOrNull(0),
                    modifier = Modifier.weight(1f),
                )
                WeeklyChampionCard(
                    place = 3,
                    player = players.getOrNull(2),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun WeeklyChampionCard(
    place: Int,
    player: WeeklyPodiumPlayer?,
    modifier: Modifier,
) {
    val accent = when (place) {
        1 -> Color(0xFFFFD25A)
        2 -> Color(0xFFC7CED8)
        else -> Color(0xFFC88758)
    }
    val accentDeep = when (place) {
        1 -> Color(0xFF9A6B00)
        2 -> Color(0xFF697586)
        else -> Color(0xFF7C4728)
    }
    val cardHeight = if (place == 1) 178.dp else 160.dp
    val avatarWidth = if (place == 1) 76.dp else 68.dp
    val avatarHeight = if (place == 1) 58.dp else 52.dp
    val name = player?.row?.displayName ?: "—"
    val profile = player?.profile
    val avatarPath = if (profile?.avatarVisibility == "hidden") null else profile?.avatarPath

    Surface(
        modifier = modifier.height(cardHeight),
        shape = RoundedCornerShape(19.dp),
        color = Color.White.copy(alpha = if (place == 1) .18f else .11f),
        border = BorderStroke(if (place == 1) 2.dp else 1.dp, accent.copy(alpha = .88f)),
        shadowElevation = if (place == 1) 8.dp else 3.dp,
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 5.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = RoundedCornerShape(99.dp),
                color = accent,
                border = BorderStroke(1.dp, Color.White.copy(alpha = .55f)),
            ) {
                Row(
                    Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        if (place == 1) Icons.Rounded.EmojiEvents else Icons.Rounded.MilitaryTech,
                        null,
                        tint = accentDeep,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text("#$place", color = accentDeep, fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = accent.copy(alpha = .18f),
                border = BorderStroke(if (place == 1) 3.dp else 2.dp, accent),
                shadowElevation = if (place == 1) 8.dp else 4.dp,
            ) {
                Box(Modifier.padding(3.dp), contentAlignment = Alignment.Center) {
                    ProfilePhotoAvatarRectWithGender(
                        avatarPath = avatarPath,
                        gender = profile?.gender,
                        name = name,
                        width = avatarWidth,
                        height = avatarHeight,
                        accent = accent,
                        showGenderBadge = false,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                name,
                color = Color.White,
                fontSize = if (place == 1) 11.sp else 10.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                player?.row?.let { "${it.rating} RP" } ?: "— RP",
                color = accent,
                fontSize = if (place == 1) 10.sp else 9.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
            if (profile?.isVip == true) {
                Spacer(Modifier.height(3.dp))
                Surface(shape = RoundedCornerShape(99.dp), color = accent.copy(alpha = .20f)) {
                    Text("PRO", Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = accent, fontSize = 6.sp, fontWeight = FontWeight.Black)
                }
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
            .height(88.dp)
            .sonHarfPressScale(pressedScale = 0.98f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = UnifiedUi.Surface,
        border = BorderStroke(1.dp, UnifiedUi.Border),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(27.dp))
            Spacer(Modifier.height(7.dp))
            Text(title, color = UnifiedUi.Text, fontSize = 9.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun UnifiedRoundAction(icon: ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(42.dp)
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
private fun UnifiedBottomBar(current: UnifiedDestination, onSelect: (UnifiedDestination) -> Unit) {
    Surface(color = UnifiedUi.Surface, shadowElevation = 12.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UnifiedBottomItem(Icons.Rounded.GridView, sh("ANA", "HOME"), current == UnifiedDestination.HOME) { onSelect(UnifiedDestination.HOME) }
            UnifiedBottomItem(Icons.Rounded.EmojiEvents, sh("LİG", "LEAGUE"), current == UnifiedDestination.LEAGUE) { onSelect(UnifiedDestination.LEAGUE) }
            UnifiedBottomItem(Icons.Rounded.Groups, sh("SOSYAL", "SOCIAL"), current == UnifiedDestination.SOCIAL) { onSelect(UnifiedDestination.SOCIAL) }
            UnifiedBottomItem(Icons.Rounded.Storefront, sh("MAĞAZA", "SHOP"), current == UnifiedDestination.SHOP) { onSelect(UnifiedDestination.SHOP) }
            UnifiedBottomItem(Icons.Rounded.WorkspacePremium, sh("PROFİL", "PROFILE"), current == UnifiedDestination.PROFILE) { onSelect(UnifiedDestination.PROFILE) }
        }
    }
}

@Composable
private fun UnifiedBottomItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(64.dp)
            .sonHarfPressScale(pressedScale = 0.94f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) UnifiedUi.BlueSoft else Color.Transparent,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, null, tint = if (selected) UnifiedUi.Blue else UnifiedUi.Muted, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(3.dp))
            Text(label, color = if (selected) UnifiedUi.Blue else UnifiedUi.Muted, fontSize = 7.sp, fontWeight = FontWeight.Bold)
        }
    }
}
