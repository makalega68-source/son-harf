package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.GrowthDashboardDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SupabaseProvider
import kotlinx.coroutines.launch

private object LightV2 {
    val Bg = Color(0xFFF8FAFC)
    val Surface = Color.White
    val Primary = Color(0xFF0284C7)
    val PrimaryLight = Color(0xFFE0F2FE)
    val PrimaryDark = Color(0xFF0369A1)
    val Text = Color(0xFF0F172A)
    val Muted = Color(0xFF475569)
    val Border = Color(0xFFCBD5E1)
    val Green = Color(0xFF16A34A)
    val Amber = Color(0xFFD97706)
    val Red = Color(0xFFDC2626)
}

private enum class LightScreen { HOME, GAME, STORE, PROFILE, HUB, LEAGUE, PROFILE_FULL, STORE_FULL }

private data class LightStoreItem(
    val title: String,
    val subtitle: String,
    val price: String,
    val icon: ImageVector,
)

@Composable
fun AccessibleLightSonHarfApp() {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var screen by remember { mutableStateOf(LightScreen.HOME) }
    var authChecked by remember { mutableStateOf(false) }
    var authenticated by remember { mutableStateOf(false) }
    var gameKey by remember { mutableIntStateOf(0) }
    val lobbyRequest = SonHarfGameNavigation.lobbyRequest

    LaunchedEffect(Unit) {
        authenticated = SupabaseProvider.configured && hasVerifiedMembershipSession()
        authChecked = true
    }
    LaunchedEffect(authenticated) {
        if (!authenticated) return@LaunchedEffect
        runCatching { backend?.getPreferredGameMode() }.getOrNull()?.let { SonHarfGameModeState.mode = it }
        SonHarfCosmetics.apply(runCatching { backend?.getEquippedCosmetics() }.getOrNull())
    }
    LaunchedEffect(lobbyRequest) {
        if (authenticated && lobbyRequest > 0) {
            gameKey += 1
            screen = LightScreen.GAME
        }
    }

    if (!authChecked) {
        Box(Modifier.fillMaxSize().background(LightV2.Bg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = LightV2.Primary)
        }
        return
    }
    if (!authenticated) {
        RequiredAuthGate { authenticated = true; screen = LightScreen.HOME }
        return
    }

    Scaffold(
        containerColor = LightV2.Bg,
        bottomBar = {
            if (screen in setOf(LightScreen.HOME, LightScreen.STORE, LightScreen.PROFILE)) {
                LightBottomBar(screen) { screen = it }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(LightV2.Bg)) {
            when (screen) {
                LightScreen.HOME -> LightHome(
                    backend = backend,
                    onPlay = { gameKey += 1; screen = LightScreen.GAME },
                    onStore = { screen = LightScreen.STORE },
                    onProfile = { screen = LightScreen.PROFILE },
                    onHub = { screen = LightScreen.HUB },
                    onLeague = { screen = LightScreen.LEAGUE },
                )
                LightScreen.GAME -> key(gameKey) {
                    Box(Modifier.fillMaxSize()) {
                        TargetNeonGameScreen()
                        Surface(
                            onClick = { screen = LightScreen.HOME },
                            shape = CircleShape,
                            color = LightV2.Surface.copy(alpha = .94f),
                            border = BorderStroke(1.dp, LightV2.Border),
                            modifier = Modifier.padding(12.dp).size(46.dp).align(Alignment.TopStart),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Home, contentDescription = "Ana sayfaya dön", tint = LightV2.PrimaryDark)
                            }
                        }
                    }
                }
                LightScreen.STORE -> LightStore(onOpenFullStore = { screen = LightScreen.STORE_FULL })
                LightScreen.PROFILE -> LightProfile(backend = backend, onDetails = { screen = LightScreen.PROFILE_FULL })
                LightScreen.HUB -> MetaHubScreen()
                LightScreen.LEAGUE -> LeaderboardExperienceScreen { screen = LightScreen.HOME }
                LightScreen.PROFILE_FULL -> ProfileExperienceScreen()
                LightScreen.STORE_FULL -> EconomyShopScreen()
            }
        }
    }

    WinnerFireworkOverlay()
    FriendsQuickAccessOverlay()
    GameInviteOverlay()
    FriendRequestOverlay()
}

@Composable
private fun LightHome(
    backend: OnlineGameBackend?,
    onPlay: () -> Unit,
    onStore: () -> Unit,
    onProfile: () -> Unit,
    onHub: () -> Unit,
    onLeague: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var growth by remember { mutableStateOf<GrowthDashboardDto?>(null) }
    var dailyMessage by remember { mutableStateOf("") }

    suspend fun reload() {
        val id = backend?.currentUserId()
        if (id != null) profile = runCatching { backend.getProfile(id) }.getOrNull()
        growth = runCatching { backend?.getGrowthDashboard() }.getOrNull()
        runCatching { backend?.logEvent("home_open_accessible_light_v2") }
    }

    LaunchedEffect(Unit) { reload() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            LightProfileHeader(
                name = profile?.displayName ?: "Son Harf Oyuncusu",
                level = growth?.level ?: 1,
                coins = (growth?.xp ?: 0) * 2,
                onProfile = onProfile,
            )
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = LightV2.Surface,
                border = BorderStroke(1.dp, LightV2.Border),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(14.dp), color = LightV2.PrimaryLight, modifier = Modifier.size(52.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.AutoStories, null, tint = LightV2.PrimaryDark, modifier = Modifier.size(28.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Günün Kelime Egzersizi", color = LightV2.Text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (growth?.dailyClaimed == true) "Bugünün ödülü alındı. Yeni bir düelloyla serini sürdür."
                                else "Bugün oyununu tamamla, kelime hızını geliştir ve günlük ödülünü al.",
                                color = LightV2.Muted,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                            )
                        }
                    }
                    if (dailyMessage.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(dailyMessage, color = LightV2.Green, fontWeight = FontWeight.Bold)
                    }
                    if (growth?.dailyClaimed == false) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val reward = runCatching { backend?.claimDailyCheckin() ?: 0 }.getOrDefault(0)
                                    dailyMessage = if (reward > 0) "+$reward günlük ödül" else "Günlük ödül kontrol edildi"
                                    reload()
                                }
                            },
                            border = BorderStroke(1.dp, LightV2.Primary),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("GÜNLÜK ÖDÜLÜ AL", color = LightV2.PrimaryDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth().height(68.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LightV2.Primary),
            ) {
                Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(8.dp))
                Text("HEMEN OYNA (1v1)", fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
        }

        item {
            Text("Kısayollar", color = LightV2.Text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LightShortcut(Icons.Rounded.EmojiEvents, "Ligler", Modifier.weight(1f), onLeague)
                LightShortcut(Icons.Rounded.Groups, "Arkadaşlar", Modifier.weight(1f)) { FriendsQuickAccessState.open = true }
                LightShortcut(Icons.Rounded.Flag, "Görevler", Modifier.weight(1f), onHub)
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LightInfoCard("Seviye", "${growth?.level ?: 1}", Icons.Rounded.TrendingUp, Modifier.weight(1f))
                LightInfoCard("XP", "${growth?.xp ?: 0}", Icons.Rounded.Bolt, Modifier.weight(1f))
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onStore, modifier = Modifier.weight(1f).height(54.dp), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Rounded.ShoppingCart, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Mağaza")
                }
                OutlinedButton(onClick = onProfile, modifier = Modifier.weight(1f).height(54.dp), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Rounded.Person, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Profil")
                }
            }
        }
    }
}

@Composable
private fun LightProfileHeader(name: String, level: Int, coins: Int, onProfile: () -> Unit) {
    Surface(
        onClick = onProfile,
        shape = RoundedCornerShape(18.dp),
        color = LightV2.Surface,
        border = BorderStroke(1.dp, LightV2.Border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = LightV2.PrimaryLight, border = BorderStroke(2.dp, LightV2.Primary), modifier = Modifier.size(58.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(name.take(1).uppercase(), color = LightV2.PrimaryDark, fontWeight = FontWeight.Black, fontSize = 24.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, color = LightV2.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Seviye $level • ${leagueFor(level)}", color = LightV2.Muted, fontSize = 14.sp)
            }
            Surface(shape = RoundedCornerShape(22.dp), color = LightV2.PrimaryLight) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.MonetizationOn, null, tint = LightV2.Amber, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("$coins", color = LightV2.PrimaryDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LightShortcut(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(88.dp),
        shape = RoundedCornerShape(16.dp),
        color = LightV2.Surface,
        border = BorderStroke(1.dp, LightV2.Border),
    ) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = LightV2.PrimaryDark, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, color = LightV2.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun LightInfoCard(title: String, value: String, icon: ImageVector, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = LightV2.Surface, border = BorderStroke(1.dp, LightV2.Border)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = LightV2.PrimaryLight, modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = LightV2.PrimaryDark) }
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, color = LightV2.Muted, fontSize = 12.sp)
                Text(value, color = LightV2.Text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LightStore(onOpenFullStore: () -> Unit) {
    val items = listOf(
        LightStoreItem("500 Altın Paketi", "İpuçları ve jokerler için", "19,99 ₺", Icons.Rounded.MonetizationOn),
        LightStoreItem("2.000 Altın Paketi", "Popüler başlangıç paketi", "49,99 ₺", Icons.Rounded.Paid),
        LightStoreItem("Zaman Dondurucu (x5)", "Zorlu turlarda ek süre desteği", "250 Altın", Icons.Rounded.HourglassTop),
        LightStoreItem("Harf İpucu (x5)", "Kelime üretirken yardım al", "300 Altın", Icons.Rounded.Lightbulb),
    )

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Oyun Mağazası", color = LightV2.Text, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text("Büyük butonlar ve sade seçimlerle güvenli alışveriş.", color = LightV2.Muted, fontSize = 14.sp)
        }
        items(items) { item ->
            Surface(shape = RoundedCornerShape(16.dp), color = LightV2.Surface, border = BorderStroke(1.dp, LightV2.Border)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(12.dp), color = LightV2.PrimaryLight, modifier = Modifier.size(50.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(item.icon, null, tint = LightV2.PrimaryDark) }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title, color = LightV2.Text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(item.subtitle, color = LightV2.Muted, fontSize = 12.sp)
                    }
                    Text(item.price, color = LightV2.PrimaryDark, fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            }
        }
        item {
            Button(
                onClick = onOpenFullStore,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LightV2.Primary),
            ) {
                Icon(Icons.Rounded.ShoppingBag, null)
                Spacer(Modifier.width(8.dp))
                Text("GERÇEK MAĞAZAYI AÇ", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Text("Gerçek satın alma işlemleri mevcut Google Play / oyun ekonomisi akışında tamamlanır.", color = LightV2.Muted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun LightProfile(backend: OnlineGameBackend?, onDetails: () -> Unit) {
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var growth by remember { mutableStateOf<GrowthDashboardDto?>(null) }

    LaunchedEffect(Unit) {
        val id = backend?.currentUserId()
        if (id != null) profile = runCatching { backend.getProfile(id) }.getOrNull()
        growth = runCatching { backend?.getGrowthDashboard() }.getOrNull()
    }

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Oyuncu Profili", color = LightV2.Text, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Surface(shape = CircleShape, color = LightV2.PrimaryLight, border = BorderStroke(3.dp, LightV2.Primary), modifier = Modifier.size(104.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text((profile?.displayName ?: "S").take(1).uppercase(), color = LightV2.PrimaryDark, fontSize = 42.sp, fontWeight = FontWeight.Black)
            }
        }
        Text(profile?.displayName ?: "Son Harf Oyuncusu", color = LightV2.Text, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Text("${leagueFor(growth?.level ?: 1)} • Seviye ${growth?.level ?: 1}", color = LightV2.Muted, fontSize = 15.sp)

        Surface(shape = RoundedCornerShape(18.dp), color = LightV2.Surface, border = BorderStroke(1.dp, LightV2.Border), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.SpaceAround) {
                ProfileStat("XP", "${growth?.xp ?: 0}", LightV2.PrimaryDark)
                VerticalDivider(Modifier.height(46.dp), color = LightV2.Border)
                ProfileStat("Seviye", "${growth?.level ?: 1}", LightV2.Green)
            }
        }

        Button(
            onClick = onDetails,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LightV2.Primary),
        ) {
            Icon(Icons.Rounded.ManageAccounts, null)
            Spacer(Modifier.width(8.dp))
            Text("PROFİL AYARLARI VE İSTATİSTİKLER", fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = { FriendsQuickAccessState.open = true },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, LightV2.Border),
        ) {
            Icon(Icons.Rounded.Groups, null, tint = LightV2.PrimaryDark)
            Spacer(Modifier.width(8.dp))
            Text("Arkadaşlar ve Düellolar", color = LightV2.Text)
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = LightV2.Muted, fontSize = 13.sp)
        Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun LightBottomBar(current: LightScreen, onSelect: (LightScreen) -> Unit) {
    NavigationBar(containerColor = LightV2.Surface, tonalElevation = 6.dp) {
        NavigationBarItem(
            selected = current == LightScreen.HOME,
            onClick = { onSelect(LightScreen.HOME) },
            icon = { Icon(Icons.Rounded.Home, contentDescription = "Ana Sayfa") },
            label = { Text("Ana Sayfa", fontWeight = FontWeight.SemiBold) },
            colors = lightNavColors(),
        )
        NavigationBarItem(
            selected = current == LightScreen.STORE,
            onClick = { onSelect(LightScreen.STORE) },
            icon = { Icon(Icons.Rounded.ShoppingCart, contentDescription = "Mağaza") },
            label = { Text("Mağaza", fontWeight = FontWeight.SemiBold) },
            colors = lightNavColors(),
        )
        NavigationBarItem(
            selected = current == LightScreen.PROFILE,
            onClick = { onSelect(LightScreen.PROFILE) },
            icon = { Icon(Icons.Rounded.Person, contentDescription = "Profil") },
            label = { Text("Profil", fontWeight = FontWeight.SemiBold) },
            colors = lightNavColors(),
        )
    }
}

@Composable
private fun lightNavColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = LightV2.PrimaryDark,
    selectedTextColor = LightV2.PrimaryDark,
    indicatorColor = LightV2.PrimaryLight,
    unselectedIconColor = LightV2.Muted,
    unselectedTextColor = LightV2.Muted,
)

private fun leagueFor(level: Int): String = when {
    level >= 40 -> "Elmas Lig"
    level >= 25 -> "Altın Lig"
    level >= 10 -> "Gümüş Lig"
    else -> "Bronz Lig"
}
