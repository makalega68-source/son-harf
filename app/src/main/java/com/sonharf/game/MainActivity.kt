package com.sonharf.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.getLeaderboard

internal val SonHarfBg = Color(0xFF050912)
internal val SonHarfSurface = Color(0xFF0D1422)
internal val SonHarfSurface2 = Color(0xFF121C2D)
internal val SonHarfPurple = Color(0xFF8A35FF)
internal val SonHarfCyan = Color(0xFF18BFFF)
internal val SonHarfGold = Color(0xFFFFC857)
internal val SonHarfText = Color(0xFFF7F9FF)
internal val SonHarfMuted = Color(0xFF8B95A8)
internal val SonHarfPink = Color(0xFFFF3D87)

enum class AppScreen { HOME, GAME, SHOP, PROFILE, MORE, LEADERBOARD }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SonHarfPreferences.syncSound(this)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = SonHarfPurple,
                    secondary = SonHarfCyan,
                    tertiary = SonHarfGold,
                    background = SonHarfBg,
                    surface = SonHarfSurface,
                    onBackground = SonHarfText,
                    onSurface = SonHarfText,
                )
            ) { SonHarfApp() }
        }
    }
}

@Composable
private fun SonHarfApp() {
    var screen by remember { mutableStateOf(AppScreen.HOME) }
    val root = screen != AppScreen.LEADERBOARD
    Scaffold(
        containerColor = SonHarfBg,
        bottomBar = {
            if (root) {
                NavigationBar(containerColor = Color(0xFF080E19), tonalElevation = 0.dp) {
                    NavItem("⌂", "Ana Sayfa", screen == AppScreen.HOME) { screen = AppScreen.HOME }
                    NavItem("⚔", "Oyna", screen == AppScreen.GAME) { screen = AppScreen.GAME }
                    NavItem("▱", "Mağaza", screen == AppScreen.SHOP) { screen = AppScreen.SHOP }
                    NavItem("♙", "Profil", screen == AppScreen.PROFILE) { screen = AppScreen.PROFILE }
                    NavItem("•••", "Daha Fazla", screen == AppScreen.MORE) { screen = AppScreen.MORE }
                }
            }
        }
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).background(
                Brush.verticalGradient(listOf(Color(0xFF08101E), SonHarfBg, Color(0xFF03060C)))
            )
        ) {
            when (screen) {
                AppScreen.HOME -> ReferenceHome(
                    onPlay = { screen = AppScreen.GAME },
                    onLeaderboard = { screen = AppScreen.LEADERBOARD },
                )
                AppScreen.GAME -> OnlineGameScreenV4()
                AppScreen.SHOP -> ReferenceShop()
                AppScreen.PROFILE -> ReferenceProfile()
                AppScreen.MORE -> ReferenceSettings()
                AppScreen.LEADERBOARD -> ReferenceLeaderboard { screen = AppScreen.HOME }
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(icon: String, label: String, selected: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    NavigationBarItem(
        selected = selected,
        onClick = { SonHarfSoundFx.tap(); SonHarfPreferences.hapticTap(context); onClick() },
        icon = {
            Surface(
                color = if (selected) SonHarfPurple.copy(alpha = .20f) else Color.Transparent,
                shape = RoundedCornerShape(15.dp),
            ) {
                Text(
                    icon,
                    Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = if (selected) SonHarfCyan else SonHarfMuted,
                    fontSize = if (icon == "•••") 15.sp else 20.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        },
        label = { Text(label, fontSize = 9.sp, maxLines = 1) },
        colors = NavigationBarItemDefaults.colors(
            selectedTextColor = SonHarfText,
            unselectedTextColor = SonHarfMuted,
            indicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun ReferenceHome(onPlay: () -> Unit, onLeaderboard: () -> Unit) {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var top by remember { mutableStateOf<List<ProfileDto>>(emptyList()) }
    LaunchedEffect(Unit) {
        val b = backend ?: return@LaunchedEffect
        if (b.currentUserId() == null) runCatching { b.ensurePlayer("Oyuncu") }
        profile = b.currentUserId()?.let { runCatching { b.getProfile(it) }.getOrNull() }
        top = runCatching { b.getLeaderboard(3).map { it.profile } }.getOrDefault(emptyList())
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarBubble(profile?.displayName ?: "Ayaz", SonHarfGold)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(profile?.displayName ?: "Oyuncu", fontWeight = FontWeight.Black, fontSize = 14.sp)
                        Text("💜 Elmas ${profile?.diamonds ?: 0}  💎", color = SonHarfMuted, fontSize = 10.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeaderIcon("♙")
                    HeaderIcon("🎁")
                    HeaderIcon("⚙")
                }
            }
        }

        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    Surface(
                        modifier = Modifier.size(150.dp, 90.dp),
                        color = SonHarfPurple.copy(alpha = .10f),
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, SonHarfPurple.copy(alpha = .28f)),
                    ) {}
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SON", fontSize = 38.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Text("HARF", fontSize = 38.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = SonHarfText)
                    }
                }
                Text("GERÇEK ZAMANLI KELİME DÜELLOSU", fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
            }
        }

        item {
            Button(
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
            ) {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.horizontalGradient(listOf(Color(0xFF18BFFF), Color(0xFF3E66FF), Color(0xFF8A35FF)))
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("DÜELLOYA GİR", fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text("Rastgele rakip bul", fontSize = 10.sp, color = Color.White.copy(alpha = .8f))
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HomeAction("👥", "ARKADAŞLAR", "Çevrimiçi rakip", Modifier.weight(1f), onPlay)
                HomeAction("♟", "ÖZEL ODA", "Oda oluştur / Katıl", Modifier.weight(1f), onPlay)
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SonHarfSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = .05f)),
            ) {
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    MiniStat("3 × 10", "KELİME", Modifier.weight(1f))
                    MiniStat("45 sn", "SÜRE", Modifier.weight(1f))
                    MiniStat("3", "ROUND", Modifier.weight(1f))
                    MiniStat("TR / EN", "DİL", Modifier.weight(1f))
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1220)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = .06f)),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("HAFTANIN EN İYİLERİ", fontWeight = FontWeight.Black, fontSize = 12.sp)
                        Surface(color = SonHarfSurface2, shape = RoundedCornerShape(9.dp)) { Text("Bu hafta⌄", Modifier.padding(horizontal = 9.dp, vertical = 5.dp), fontSize = 9.sp, color = SonHarfMuted) }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val sample = if (top.isEmpty()) listOf(
                            ProfileDto("1", "Ayaz", wins = 256), ProfileDto("2", "Ece", wins = 193), ProfileDto("3", "Mert", wins = 168)
                        ) else top
                        sample.take(3).forEachIndexed { index, p ->
                            LeaderCard(index, p, Modifier.weight(1f))
                        }
                    }
                    Button(
                        onClick = onLeaderboard,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3944D7)),
                    ) { Text("TÜM LİDERLİK TABLOSU", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                }
            }
        }
    }
}

@Composable
private fun ReferenceProfile() {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    LaunchedEffect(Unit) {
        val b = backend ?: return@LaunchedEffect
        if (b.currentUserId() == null) runCatching { b.ensurePlayer("Oyuncu") }
        profile = b.currentUserId()?.let { runCatching { b.getProfile(it) }.getOrNull() }
    }
    val p = profile
    val wins = p?.wins ?: 256
    val losses = p?.losses ?: 142
    val matches = (wins + losses).coerceAtLeast(1)
    val rate = wins * 100 / matches

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { HeaderIcon("‹"); HeaderIcon("✎") }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                AvatarBubble(p?.displayName ?: "Ayaz", SonHarfGold, 92.dp)
                Spacer(Modifier.height(10.dp))
                Text(p?.displayName ?: "Ayaz", fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text(if (p?.isVip == true) "SON HARF USTASI 💜" else "SON HARF OYUNCUSU 💜", color = SonHarfMuted, fontSize = 11.sp)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Seviye 23", fontSize = 11.sp); Spacer(Modifier.width(8.dp)); LinearProgressIndicator(progress = { .72f }, modifier = Modifier.weight(1f).height(6.dp), color = SonHarfPurple, trackColor = SonHarfSurface2); Spacer(Modifier.width(8.dp)); Text("3.250 / 5.000", fontSize = 10.sp, color = SonHarfMuted)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BigMetric(wins.toString(), "Galibiyet", Modifier.weight(1f)); BigMetric(losses.toString(), "Mağlubiyet", Modifier.weight(1f)); BigMetric("%$rate", "Kazanma Oranı", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BigMetric(matches.toString(), "Toplam Maç", Modifier.weight(1f)); BigMetric((matches * 2 + 61).toString(), "Toplam Round", Modifier.weight(1f)); BigMetric("7.431", "Toplam Kelime", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BigMetric("12", "En Uzun Seri", Modifier.weight(1f)); BigMetric("58", "Söz Fırtınası", Modifier.weight(1f)); BigMetric("1.284", "En Yüksek Puan", Modifier.weight(1f))
                }
            }
        }
        item {
            Text("SON BAŞARILAR", fontSize = 11.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                listOf("♛", "★", "✦", "♜", "✪").forEachIndexed { i, s ->
                    Surface(color = if (i < 3) SonHarfGold.copy(alpha = .12f) else SonHarfSurface2, shape = RoundedCornerShape(999.dp), border = BorderStroke(1.dp, if (i < 3) SonHarfGold.copy(alpha = .6f) else Color.White.copy(alpha = .08f))) { Text(s, Modifier.padding(12.dp), color = if (i < 3) SonHarfGold else SonHarfMuted, fontSize = 20.sp) }
                }
            }
        }
    }
}

@Composable
private fun ReferenceLeaderboard(onBack: () -> Unit) {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var rows by remember { mutableStateOf<List<ProfileDto>>(emptyList()) }
    LaunchedEffect(Unit) { rows = runCatching { backend?.getLeaderboard(50)?.map { it.profile } ?: emptyList() }.getOrDefault(emptyList()) }
    val fallback = listOf("Ayaz" to 256, "Ece" to 193, "Mert" to 168, "Can" to 142, "Zeynep" to 121, "Kerem" to 98, "Elif" to 87)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) { TextButton(onClick = onBack) { Text("‹", fontSize = 28.sp) }; Text("LİDERLİK TABLOSU", fontSize = 20.sp, fontWeight = FontWeight.Black) }
            Row(Modifier.fillMaxWidth().background(SonHarfSurface, RoundedCornerShape(14.dp)).padding(5.dp)) {
                listOf("Toplam", "Bu Hafta", "Bu Ay").forEachIndexed { i, t -> Surface(Modifier.weight(1f), color = if (i == 0) Color(0xFF343DCB) else Color.Transparent, shape = RoundedCornerShape(10.dp)) { Text(t, Modifier.padding(10.dp), textAlign = TextAlign.Center, fontSize = 11.sp) } }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) { Text("#", Modifier.width(28.dp), color = SonHarfMuted, fontSize = 10.sp); Text("Oyuncu", Modifier.weight(1f), color = SonHarfMuted, fontSize = 10.sp); Text("Galibiyet", Modifier.width(70.dp), color = SonHarfMuted, fontSize = 10.sp); Text("Kazanma %", Modifier.width(72.dp), color = SonHarfMuted, fontSize = 10.sp) }
        }
        if (rows.isEmpty()) {
            items(fallback.size) { i -> LeaderRow(i + 1, fallback[i].first, fallback[i].second, listOf(64,61,59,57,55,53,52)[i], i == 0) }
        } else {
            items(rows.take(20).withIndex().toList()) { indexed ->
                val p = indexed.value; val total = (p.wins + p.losses).coerceAtLeast(1); LeaderRow(indexed.index + 1, p.displayName, p.wins, p.wins * 100 / total, false)
            }
        }
    }
}

@Composable
private fun ReferenceShop() {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("MAĞAZA", fontSize = 28.sp, fontWeight = FontWeight.Black); Text("Elmas, VIP ve kişiselleştirme", color = SonHarfMuted) }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
                Box(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFFFFB547), Color(0xFFFF7A59)))).padding(18.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("♛  VIP", color = Color(0xFF20120C), fontSize = 28.sp, fontWeight = FontWeight.Black); Text("Reklamsız deneyim • özel rozet • profil ayrıcalıkları", color = Color(0xFF261711)); Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("Google Play ile yakında") } }
                }
            }
        }
        items(listOf("💎 100 Elmas", "💎 500 Elmas", "🎨 Premium Tema", "😊 Emoji Paketi")) { title ->
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = .05f))) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(title, fontWeight = FontWeight.Bold); Surface(color = SonHarfSurface2, shape = RoundedCornerShape(999.dp)) { Text("Yakında", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = SonHarfMuted, fontSize = 10.sp) } }
            }
        }
    }
}

@Composable
private fun ReferenceSettings() {
    val context = LocalContext.current
    var sound by remember { mutableStateOf(SonHarfPreferences.soundEnabled(context)) }
    var vibration by remember { mutableStateOf(SonHarfPreferences.vibrationEnabled(context)) }
    var notifications by remember { mutableStateOf(SonHarfPreferences.notificationsEnabled(context)) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("AYARLAR", fontSize = 26.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
        item {
            SettingsCard("SES AYARLARI") {
                ToggleLine("Ses Efektleri", sound) { sound = it; SonHarfPreferences.setSoundEnabled(context, it) }
                ToggleLine("Müzik", sound) { sound = it; SonHarfPreferences.setSoundEnabled(context, it) }
                ToggleLine("Titreşim", vibration) { vibration = it; SonHarfPreferences.setVibrationEnabled(context, it) }
            }
        }
        item {
            SettingsCard("BİLDİRİMLER") {
                ToggleLine("Oyun Davetleri", notifications) { notifications = it; SonHarfPreferences.setNotificationsEnabled(context, it) }
                ToggleLine("Arkadaşlık İstekleri", notifications) { notifications = it; SonHarfPreferences.setNotificationsEnabled(context, it) }
                ToggleLine("Sistem Bildirimleri", notifications) { notifications = it; SonHarfPreferences.setNotificationsEnabled(context, it) }
            }
        }
        item {
            SettingsCard("DİĞER") {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("Dil"); Text("Türkçe⌄", color = SonHarfMuted) }
                ToggleLine("Karanlık Mod", true) {}
            }
        }
    }
}

@Composable private fun HeaderIcon(text: String) { Surface(color = SonHarfSurface2, shape = RoundedCornerShape(999.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = .05f))) { Text(text, Modifier.padding(9.dp), fontSize = 14.sp) } }

@Composable
private fun AvatarBubble(name: String, ring: Color, size: androidx.compose.ui.unit.Dp = 38.dp) {
    Box(Modifier.size(size).background(Brush.radialGradient(listOf(ring.copy(alpha = .45f), SonHarfSurface)), RoundedCornerShape(999.dp)), contentAlignment = Alignment.Center) {
        Surface(Modifier.size(size - 5.dp), color = Color(0xFF182238), shape = RoundedCornerShape(999.dp), border = BorderStroke(1.dp, ring.copy(alpha = .8f))) { Box(contentAlignment = Alignment.Center) { Text(name.take(1).uppercase(), fontWeight = FontWeight.Black, fontSize = (size.value * .34f).sp) } }
    }
}

@Composable
private fun HomeAction(icon: String, title: String, sub: String, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier.height(62.dp), shape = RoundedCornerShape(15.dp), border = BorderStroke(1.dp, Color(0xFF31415E))) {
        Row(verticalAlignment = Alignment.CenterVertically) { Text(icon, fontSize = 20.sp); Spacer(Modifier.width(8.dp)); Column { Text(title, fontWeight = FontWeight.Black, fontSize = 11.sp); Text(sub, color = SonHarfMuted, fontSize = 8.sp) } }
    }
}

@Composable private fun MiniStat(value: String, label: String, modifier: Modifier) { Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) { Text(value, fontWeight = FontWeight.Black, fontSize = 12.sp); Text(label, color = SonHarfMuted, fontSize = 8.sp) } }

@Composable
private fun LeaderCard(index: Int, p: ProfileDto, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = if (index == 0) SonHarfGold.copy(alpha = .10f) else SonHarfSurface2), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, if (index == 0) SonHarfGold.copy(alpha = .45f) else SonHarfPurple.copy(alpha = .20f))) {
        Column(Modifier.fillMaxWidth().padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(listOf("♛", "♜", "♝")[index], color = if (index == 0) SonHarfGold else SonHarfMuted); AvatarBubble(p.displayName, if (index == 0) SonHarfGold else SonHarfPurple, 42.dp); Text(p.displayName, fontWeight = FontWeight.Bold, fontSize = 11.sp); Text("${p.wins} GALİBİYET", color = SonHarfMuted, fontSize = 8.sp) }
    }
}

@Composable private fun BigMetric(value: String, label: String, modifier: Modifier) { Surface(modifier, color = SonHarfSurface, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = .04f))) { Column(Modifier.padding(vertical = 13.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black); Text(label, fontSize = 9.sp, color = SonHarfMuted, textAlign = TextAlign.Center) } } }

@Composable
private fun LeaderRow(rank: Int, name: String, wins: Int, rate: Int, highlight: Boolean) {
    Surface(color = if (highlight) SonHarfPurple.copy(alpha = .18f) else SonHarfSurface, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, if (highlight) SonHarfPurple.copy(alpha = .35f) else Color.White.copy(alpha = .04f))) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (rank <= 3) listOf("♛", "♜", "♝")[rank - 1] else rank.toString(), Modifier.width(28.dp), textAlign = TextAlign.Center, color = if (rank == 1) SonHarfGold else SonHarfMuted)
            AvatarBubble(name, if (rank == 1) SonHarfGold else SonHarfPurple, 30.dp); Spacer(Modifier.width(8.dp)); Text(name, Modifier.weight(1f), fontWeight = if (highlight) FontWeight.Black else FontWeight.Medium, fontSize = 12.sp)
            Text(wins.toString(), Modifier.width(70.dp), textAlign = TextAlign.Center, fontSize = 12.sp); Text("%$rate", Modifier.width(72.dp), textAlign = TextAlign.Center, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = .05f))) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(title, color = SonHarfCyan, fontSize = 10.sp, fontWeight = FontWeight.Black); content() }
    }
}

@Composable private fun ToggleLine(label: String, checked: Boolean, onChange: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth().height(42.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(label, fontSize = 12.sp); Switch(checked = checked, onCheckedChange = onChange) } }
