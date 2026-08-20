package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SupabaseProvider

private val NXBg = Color(0xFF090F1A)
private val NXPanel = Color(0xFF10172B)
private val NXPanel2 = Color(0xFF131D35)
private val NXCyan = Color(0xFF00E5FF)
private val NXPurple = Color(0xFF7B2FFF)
private val NXPink = Color(0xFFFF4D6D)
private val NXGold = Color(0xFFFFC107)
private val NXBlue = Color(0xFF168CFF)
private val NXText = Color(0xFFF6F8FF)
private val NXMuted = Color(0xFF91A1BE)

@Composable
fun TotalNeonSonHarfApp() {
    var screen by remember { mutableStateOf(AppScreen.HOME) }
    var authChecked by remember { mutableStateOf(false) }
    var authenticated by remember { mutableStateOf(false) }
    var showVip by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        authenticated = SupabaseProvider.configured && hasVerifiedMembershipSession()
        authChecked = true
    }

    if (!authChecked) {
        Box(Modifier.fillMaxSize().background(NXBg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = NXCyan)
        }
        return
    }
    if (!authenticated) {
        RequiredAuthGate { authenticated = true; screen = AppScreen.HOME }
        return
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF080D19), Color(0xFF090F1A), Color(0xFF060A13)))
        )
    ) {
        when (screen) {
            AppScreen.HOME -> TotalNeonHome(
                onPlay = { screen = AppScreen.GAME },
                onShop = { screen = AppScreen.SHOP },
                onProfile = { screen = AppScreen.PROFILE },
                onHub = { screen = AppScreen.MORE },
                onLeaderboard = { screen = AppScreen.LEADERBOARD },
                onVip = { showVip = true },
            )
            AppScreen.GAME -> OnlineGameScreenV6()
            AppScreen.SHOP -> TotalNeonShop(onBack = { screen = AppScreen.HOME }, onVip = { showVip = true })
            AppScreen.PROFILE -> TotalNeonProfile(onBack = { screen = AppScreen.HOME })
            AppScreen.MORE -> TotalNeonHub(onBack = { screen = AppScreen.HOME }, onLeaderboard = { screen = AppScreen.LEADERBOARD }, onShop = { screen = AppScreen.SHOP }, onProfile = { screen = AppScreen.PROFILE })
            AppScreen.LEADERBOARD -> TotalNeonLeaderboard(onBack = { screen = AppScreen.HOME })
        }
    }

    if (showVip) VipPurchaseDialog { showVip = false }
}

@Composable
private fun TotalNeonHome(
    onPlay: () -> Unit,
    onShop: () -> Unit,
    onProfile: () -> Unit,
    onHub: () -> Unit,
    onLeaderboard: () -> Unit,
    onVip: () -> Unit,
) {
    val backend = remember { OnlineGameBackend() }
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    LaunchedEffect(Unit) {
        val id = backend.currentUserId()
        if (id != null) profile = runCatching { backend.getProfile(id) }.getOrNull()
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NXAvatar(profile?.displayName ?: "Oyuncu", NXCyan)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(profile?.displayName ?: "Oyuncu", color = NXText, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text(if (profile?.isVip == true) "Usta • VIP" else "Usta", color = NXGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NXStatPill("🏆", "${profile?.wins ?: 0}", NXGold)
                NXStatPill("◆", "${profile?.diamonds ?: 0}", NXCyan)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(30.dp),
            border = BorderStroke(1.dp, NXPurple.copy(alpha = .60f)),
        ) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.radialGradient(listOf(NXPurple.copy(alpha = .30f), Color(0xFF0B1226), NXBg))
                ),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SON", color = NXGold, fontWeight = FontWeight.Black, fontSize = 52.sp, letterSpacing = 2.sp)
                    Text("HARF", color = NXCyan, fontWeight = FontWeight.Black, fontSize = 52.sp, letterSpacing = 2.sp)
                    Text("NEON KELİME DÜELLOSU", color = NXText, fontSize = 11.sp, letterSpacing = 1.4.sp)
                }
            }
        }

        NXPrimaryButton("HEMEN OYNA", "Rastgele Rakip", NXGold, Color(0xFF211500), onPlay)
        NXPrimaryButton("ARKADAŞINLA OYNA", "Davet Gönder", NXPurple, Color.White, onPlay)
        NXPrimaryButton("ODA KUR", "Özel Eşleşme", NXBlue, Color.White, onPlay)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            NXMenuItem("✓", "GÖREVLER", NXCyan, onHub)
            NXMenuItem("♛", "SIRALAMA", NXGold, onLeaderboard)
            NXMenuItem("🛒", "MAĞAZA", NXCyan, onShop)
            NXMenuItem("♙", "PROFİL", NXCyan, onProfile)
        }

        TextButton(onClick = onVip, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("VIP AVANTAJLARI", color = NXGold, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
    }
}

@Composable
private fun TotalNeonShop(onBack: () -> Unit, onVip: () -> Unit) {
    NXScreenScaffold("MAĞAZA", onBack) {
        NXHeroCard("KOZMETİK & JETON", "Düellonu kişiselleştir", "🛍", NXPurple)
        NXFeatureCard("VIP ÜYELİK", "Özel oda, özel görünüm ve bonuslar", "♛", NXGold, onVip)
        NXFeatureCard("JETON PAKETLERİ", "Oyun içi avantajlar ve kozmetikler", "◆", NXCyan) { }
        NXFeatureCard("NEON TEMALAR", "Arena efektlerini değiştir", "✦", NXPink) { }
    }
}

@Composable
private fun TotalNeonProfile(onBack: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    LaunchedEffect(Unit) {
        val id = backend.currentUserId(); if (id != null) profile = runCatching { backend.getProfile(id) }.getOrNull()
    }
    NXScreenScaffold("PROFİL", onBack) {
        Card(colors = CardDefaults.cardColors(containerColor = NXPanel), shape = RoundedCornerShape(28.dp), border = BorderStroke(1.dp, NXCyan.copy(alpha = .35f))) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                NXAvatar(profile?.displayName ?: "Oyuncu", NXPink, 92.dp)
                Spacer(Modifier.height(14.dp))
                Text(profile?.displayName ?: "Oyuncu", color = NXText, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text("SON HARF OYUNCUSU", color = NXMuted, fontSize = 11.sp, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NXMiniStat("${profile?.wins ?: 0}", "Galibiyet", Modifier.weight(1f))
                    NXMiniStat("${profile?.losses ?: 0}", "Mağlubiyet", Modifier.weight(1f))
                    val total = (profile?.wins ?: 0) + (profile?.losses ?: 0)
                    val rate = if (total == 0) 0 else ((profile?.wins ?: 0) * 100 / total)
                    NXMiniStat("%$rate", "Kazanma", Modifier.weight(1f))
                }
            }
        }
        NXFeatureCard("BAŞARILAR", "Seriler, kilometre taşları ve rozetler", "🏆", NXGold) { }
        NXFeatureCard("ARKADAŞLAR", "Çevrimiçi oyuncular ve davetler", "👥", NXCyan) { }
    }
}

@Composable
private fun TotalNeonHub(onBack: () -> Unit, onLeaderboard: () -> Unit, onShop: () -> Unit, onProfile: () -> Unit) {
    NXScreenScaffold("OYUNCU MERKEZİ", onBack) {
        NXHeroCard("KARİYER MERKEZİ", "Hedeflerini büyüt, serini koru", "🚀", NXCyan)
        NXFeatureCard("SIRALAMA", "Haftalık ve genel lig", "🏆", NXGold, onLeaderboard)
        NXFeatureCard("GÖREVLER", "Günlük ve haftalık meydan okumalar", "🎯", NXPink) { }
        NXFeatureCard("MAĞAZA", "Jetonlar ve görsel öğeler", "🛍", NXCyan, onShop)
        NXFeatureCard("PROFİL", "İstatistikler ve kimlik", "♙", NXPurple, onProfile)
    }
}

@Composable
private fun TotalNeonLeaderboard(onBack: () -> Unit) {
    NXScreenScaffold("SIRALAMA", onBack) {
        NXHeroCard("NEON LİG", "En iyi kelime düellocuları", "🏆", NXGold)
        listOf("1  KAĞAN", "2  ZEYNEP", "3  OYUNCU-4555", "4  ÜMİT", "5  KELİME USTASI").forEachIndexed { i, name ->
            Card(colors = CardDefaults.cardColors(containerColor = NXPanel), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, if (i == 0) NXGold.copy(alpha = .55f) else Color.White.copy(alpha = .08f))) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(name, color = NXText, fontWeight = FontWeight.Black)
                    Text("${1250 - i * 70}", color = if (i == 0) NXGold else NXCyan, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun NXScreenScaffold(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onBack, shape = CircleShape, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(42.dp), border = BorderStroke(1.dp, NXCyan.copy(alpha = .35f))) { Text("‹", color = NXCyan, fontSize = 24.sp) }
                Spacer(Modifier.width(14.dp))
                Text(title, color = NXText, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
        }
        item { Column(verticalArrangement = Arrangement.spacedBy(14.dp), content = content) }
    }
}

@Composable private fun NXHeroCard(title: String, subtitle: String, icon: String, accent: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.Transparent), shape = RoundedCornerShape(28.dp), border = BorderStroke(1.dp, accent.copy(alpha = .45f))) {
        Row(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(accent.copy(alpha = .22f), NXPanel))).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 42.sp); Spacer(Modifier.width(14.dp)); Column { Text(title, color = NXText, fontWeight = FontWeight.Black, fontSize = 20.sp); Text(subtitle, color = NXMuted, fontSize = 11.sp) }
        }
    }
}

@Composable private fun NXFeatureCard(title: String, subtitle: String, icon: String, accent: Color, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = NXPanel), shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, accent.copy(alpha = .28f))) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(accent.copy(alpha = .15f)), contentAlignment = Alignment.Center) { Text(icon, fontSize = 24.sp) }
            Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(title, color = NXText, fontWeight = FontWeight.Black); Text(subtitle, color = NXMuted, fontSize = 10.sp) }; Text("›", color = accent, fontSize = 28.sp)
        }
    }
}

@Composable private fun NXPrimaryButton(title: String, subtitle: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(66.dp), colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = fg), shape = RoundedCornerShape(18.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(title, fontWeight = FontWeight.Black, fontSize = 17.sp); Text(subtitle, fontSize = 9.sp) }
    }
}

@Composable private fun NXMenuItem(icon: String, label: String, accent: Color, onClick: () -> Unit) {
    TextButton(onClick = onClick, contentPadding = PaddingValues(4.dp)) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(icon, color = accent, fontSize = 21.sp); Text(label, color = NXMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold) } }
}

@Composable private fun NXStatPill(icon: String, value: String, accent: Color) {
    Surface(color = NXPanel, shape = RoundedCornerShape(99.dp), border = BorderStroke(1.dp, accent.copy(alpha = .25f))) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { Text(icon, fontSize = 12.sp); Spacer(Modifier.width(4.dp)); Text(value, color = NXText, fontSize = 11.sp, fontWeight = FontWeight.Black) }
    }
}

@Composable private fun NXAvatar(name: String, accent: Color, size: androidx.compose.ui.unit.Dp = 46.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(Brush.sweepGradient(listOf(NXCyan, NXPurple, NXPink, NXGold, NXCyan))).padding(3.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().clip(CircleShape).background(NXPanel2), contentAlignment = Alignment.Center) { Text(name.take(1).uppercase(), color = NXText, fontWeight = FontWeight.Black, fontSize = (size.value * .40f).sp) }
    }
}

@Composable private fun NXMiniStat(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = NXPanel2, shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, color = NXText, fontWeight = FontWeight.Black, fontSize = 20.sp); Text(label, color = NXMuted, fontSize = 9.sp) } }
}
