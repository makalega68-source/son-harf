package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.launch

@Composable
fun SonHarfIntegratedApp() {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var screen by remember { mutableStateOf(AppScreen.HOME) }
    LaunchedEffect(Unit) {
        if (backend?.currentUserId() == null) runCatching { backend?.ensurePlayer(sh("Oyuncu", "Player")) }
        val mode = runCatching { backend?.getPreferredGameMode() }.getOrNull()
        if (mode != null) SonHarfGameModeState.mode = mode
    }
    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = SonHarfBg,
            bottomBar = {
                if (screen != AppScreen.LEADERBOARD) IntegratedBottomBar(screen) { screen = it }
            },
        ) { pad ->
            Box(Modifier.fillMaxSize().padding(pad).background(Brush.verticalGradient(listOf(SonHarfSurface2, SonHarfBg, SonHarfSurface)))) {
                when (screen) {
                    AppScreen.HOME -> IntegratedHomeScreen(
                        onPlay = { screen = AppScreen.GAME },
                        onShop = { screen = AppScreen.SHOP },
                        onHub = { screen = AppScreen.MORE },
                        onLeaderboard = { screen = AppScreen.LEADERBOARD },
                    )
                    AppScreen.GAME -> OnlineGameScreenV6()
                    AppScreen.SHOP -> EconomyShopScreen()
                    AppScreen.PROFILE -> ProfileExperienceScreen()
                    AppScreen.MORE -> MetaHubScreen()
                    AppScreen.LEADERBOARD -> LeaderboardExperienceScreen { screen = AppScreen.HOME }
                }
            }
        }
        if (SonHarfGameModeState.mode == "expert") ExpertArenaOverlay()
        else SketchGameOverlayV9()
        ComboOverlayV9()
    }
}

@Composable
private fun IntegratedBottomBar(screen: AppScreen, onChange: (AppScreen) -> Unit) {
    NavigationBar(containerColor = SonHarfSurface, tonalElevation = 0.dp) {
        listOf(
            Triple(AppScreen.HOME, "⌂", sh("Ana Sayfa", "Home")),
            Triple(AppScreen.GAME, "⚔", sh("Oyna", "Play")),
            Triple(AppScreen.SHOP, "◇", sh("Mağaza", "Shop")),
            Triple(AppScreen.PROFILE, "♙", sh("Profil", "Profile")),
            Triple(AppScreen.MORE, "◎", sh("Merkez", "Hub")),
        ).forEach { (target, icon, label) ->
            NavigationBarItem(
                selected = screen == target,
                onClick = { SonHarfSoundFx.tap(); onChange(target) },
                icon = { Text(icon, color = if (screen == target) SonHarfCyan else SonHarfMuted, fontSize = 20.sp, fontWeight = FontWeight.Black) },
                label = { Text(label, fontSize = 9.sp) },
                colors = NavigationBarItemDefaults.colors(indicatorColor = SonHarfPurple.copy(alpha = .18f)),
            )
        }
    }
}

@Composable
private fun IntegratedHomeScreen(onPlay: () -> Unit, onShop: () -> Unit, onHub: () -> Unit, onLeaderboard: () -> Unit) {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var goals by remember { mutableStateOf<List<GoalRowDto>>(emptyList()) }
    var mode by remember { mutableStateOf(SonHarfGameModeState.mode) }
    LaunchedEffect(Unit) {
        val id = backend?.currentUserId()
        if (id != null) profile = runCatching { backend.getProfile(id) }.getOrNull()
        goals = runCatching { backend?.getGoals().orEmpty() }.getOrDefault(emptyList())
        val remoteMode = runCatching { backend?.getPreferredGameMode() }.getOrNull()
        if (remoteMode != null) mode = remoteMode
        SonHarfGameModeState.mode = mode
    }
    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(profile?.displayName ?: sh("Oyuncu", "Player"), fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text(if (profile?.isVip == true) "VIP • 💎 ${profile?.diamonds ?: 0}" else "💎 ${profile?.diamonds ?: 0}", color = if (profile?.isVip == true) SonHarfGold else SonHarfCyan, fontSize = 11.sp)
            }
            Surface(onClick = onHub, shape = RoundedCornerShape(99.dp), color = SonHarfSurface) { Text("🎯 ${goals.count { it.progress >= it.target && !it.claimed }}", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontWeight = FontWeight.Black) }
        }

        Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, SonHarfMuted.copy(alpha=.14f))) {
            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(sh("HEMEN OYNA", "PLAY NOW"), fontSize = 25.sp, fontWeight = FontWeight.Black)
                Text(if (mode == "expert") sh("UZMAN MODU • 15/15/15 • ×1/×2/×3", "EXPERT MODE • 15/15/15 • ×1/×2/×3") else sh("NORMAL MOD • 3 × 10", "NORMAL MODE • 3 × 10"), color = if (mode == "expert") SonHarfGold else SonHarfCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = mode == "normal", onClick = {
                        mode = "normal"; SonHarfGameModeState.mode = "normal"
                        scope.launch { runCatching { backend?.setPreferredGameMode("normal") } }
                    }, label = { Text(sh("NORMAL", "NORMAL")) })
                    FilterChip(selected = mode == "expert", onClick = {
                        mode = "expert"; SonHarfGameModeState.mode = "expert"
                        scope.launch { runCatching { backend?.setPreferredGameMode("expert") } }
                    }, label = { Text(sh("UZMAN", "EXPERT")) })
                }
                Button(onClick = onPlay, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = SonHarfGold, contentColor = Color(0xFF171008))) {
                    Text(sh("DÜELLOYA GİR ⚡", "ENTER DUEL ⚡"), fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
                if (mode == "expert") Text(sh("1. round son 1 harf • 2. round son 2 harf ×2 • 3. round son 3 harf ×3", "Round 1 last 1 letter • round 2 last 2 letters ×2 • round 3 last 3 letters ×3"), color = SonHarfMuted, fontSize = 9.sp, textAlign = TextAlign.Center)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickHomeCard("🎯", sh("Hedefler", "Goals"), onHub, Modifier.weight(1f))
            QuickHomeCard("🏆", sh("Haftalık Lig", "Weekly League"), onLeaderboard, Modifier.weight(1f))
            QuickHomeCard("🛍", sh("Mağaza", "Shop"), onShop, Modifier.weight(1f))
        }

        Card(onClick = onHub, colors = CardDefaults.cardColors(containerColor = SonHarfBlue.copy(alpha=.10f)), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(sh("OYUNCU MERKEZİ", "PLAYER HUB"), color = SonHarfCyan, fontWeight = FontWeight.Black)
                Text(sh("Hedefler • Lig • Oyun geçmişi • Haberler • Kurallar • Ayarlar", "Goals • League • Game history • News • Rules • Settings"), color = SonHarfMuted, fontSize = 10.sp)
            }
        }

        if (goals.isNotEmpty()) {
            Text(sh("BU HAFTANIN HEDEFLERİ", "THIS WEEK'S GOALS"), fontWeight = FontWeight.Black, fontSize = 12.sp)
            goals.take(2).forEach { g ->
                val title = if (SonHarfUiState.isEnglish) g.titleEn else g.titleTr
                Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(15.dp)) {
                    Row(Modifier.fillMaxWidth().padding(11.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold, fontSize = 11.sp); Text("${g.progress.coerceAtMost(g.target)}/${g.target}", color = SonHarfMuted, fontSize = 9.sp) }
                        Text("💎 ${g.rewardDiamonds}", color = SonHarfCyan, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable private fun QuickHomeCard(icon: String, title: String, onClick: () -> Unit, modifier: Modifier) {
    Card(onClick = onClick, modifier = modifier.height(88.dp), colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(icon, fontSize = 24.sp); Text(title, fontWeight = FontWeight.Bold, fontSize = 9.sp, textAlign = TextAlign.Center)
        }
    }
}
