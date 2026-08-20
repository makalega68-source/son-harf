package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

private val RefBg = Color(0xFF020711)
private val RefPanel = Color(0xFF07111E)
private val RefPanel2 = Color(0xFF0B1627)
private val RefStroke = Color(0xFF1A2B43)
private val RefText = Color(0xFFF7F9FF)
private val RefMuted = Color(0xFF8995AA)
private val RefCyan = Color(0xFF20C7FF)
private val RefBlue = Color(0xFF2E8BFF)
private val RefPurple = Color(0xFF7B37FF)
private val RefPink = Color(0xFFFF3B7E)
private val RefGold = Color(0xFFFFC247)
private val RefGreen = Color(0xFF2DDB7D)

enum class ReferenceScreen { HOME, GAME, SHOP, PROFILE, MORE, LEADERBOARD }

@Composable
fun ReferenceSonHarfApp() {
    var screen by remember { mutableStateOf(ReferenceScreen.HOME) }
    Scaffold(
        containerColor = RefBg,
        bottomBar = {
            if (screen != ReferenceScreen.LEADERBOARD) ReferenceBottomBar(screen) { screen = it }
        }
    ) { inner ->
        Box(
            Modifier.fillMaxSize().padding(inner).background(
                Brush.verticalGradient(listOf(Color(0xFF06101D), RefBg, Color(0xFF01040A)))
            )
        ) {
            when (screen) {
                ReferenceScreen.HOME -> ReferenceHome(
                    onPlay = { screen = ReferenceScreen.GAME },
                    onLeaderboard = { screen = ReferenceScreen.LEADERBOARD }
                )
                ReferenceScreen.GAME -> OnlineGameScreenV6()
                ReferenceScreen.SHOP -> ReferenceShop()
                ReferenceScreen.PROFILE -> ReferenceProfile()
                ReferenceScreen.MORE -> ReferenceSettings()
                ReferenceScreen.LEADERBOARD -> ReferenceLeaderboard { screen = ReferenceScreen.HOME }
            }
        }
    }
}

@Composable
private fun ReferenceBottomBar(screen: ReferenceScreen, onChange: (ReferenceScreen) -> Unit) {
    val context = LocalContext.current
    NavigationBar(containerColor = Color(0xFF06101C), tonalElevation = 0.dp, modifier = Modifier.height(66.dp)) {
        val items = listOf(
            Triple(ReferenceScreen.HOME, "⌂", "Ana Sayfa"),
            Triple(ReferenceScreen.GAME, "⚔", "Oyna"),
            Triple(ReferenceScreen.SHOP, "▱", "Mağaza"),
            Triple(ReferenceScreen.PROFILE, "♙", "Profil"),
            Triple(ReferenceScreen.MORE, "•••", "Daha Fazla")
        )
        items.forEach { (target, icon, label) ->
            NavigationBarItem(
                selected = screen == target,
                onClick = {
                    SonHarfSoundFx.tap()
                    SonHarfPreferences.hapticTap(context)
                    onChange(target)
                },
                icon = {
                    Box(
                        Modifier.size(42.dp).clip(RoundedCornerShape(15.dp))
                            .background(if (screen == target) RefPurple.copy(alpha = .20f) else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) { Text(icon, color = if (screen == target) RefCyan else RefMuted, fontSize = 20.sp, fontWeight = FontWeight.Black) }
                },
                label = { Text(label, fontSize = 8.sp, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent,
                    selectedTextColor = RefText,
                    unselectedTextColor = RefMuted
                )
            )
        }
    }
}

@Composable
private fun ReferenceHome(onPlay: () -> Unit, onLeaderboard: () -> Unit) {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var leaders by remember { mutableStateOf<List<ProfileDto>>(emptyList()) }
    var weekLanguage by remember { mutableStateOf("tr") }

    LaunchedEffect(Unit) {
        val b = backend ?: return@LaunchedEffect
        if (b.currentUserId() == null) runCatching { b.ensurePlayer("Oyuncu") }
        profile = b.currentUserId()?.let { runCatching { b.getProfile(it) }.getOrNull() }
        leaders = runCatching { b.getLeaderboard(3).map { it.profile } }.getOrDefault(emptyList())
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RefAvatar(profile?.displayName ?: "O", 36.dp, RefGold)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(profile?.displayName ?: "Oyuncu", color = RefText, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Text("♥  Elmas: ${profile?.diamonds ?: 0}  💎", color = RefMuted, fontSize = 8.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RefRoundIcon("♙")
                    RefRoundIcon("♜")
                    RefRoundIcon("⚙")
                }
            }
        }

        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(4.dp))
                Text("SON", color = RefText, fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
                Text("HARF", color = RefText, fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
                Box(
                    Modifier.offset(y = (-54).dp).width(160.dp).height(82.dp)
                        .background(Brush.radialGradient(listOf(RefPink.copy(alpha = .20f), Color.Transparent)))
                )
                Text("GERÇEK ZAMANLI KELİME DÜELLOSU", color = RefText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }

        item {
            Button(
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.horizontalGradient(listOf(Color(0xFF28A8FF), Color(0xFF4A54FF), Color(0xFF8F23E8)))
                    ), contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("DÜELLOYA GİR", fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text("Rastgele rakip bul", fontSize = 8.sp, color = RefText.copy(alpha = .8f))
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RefActionCard("👥", "ARKADAŞLAR", "Çevrimiçi: 0", RefCyan, Modifier.weight(1f), onPlay)
                RefActionCard("♛", "ÖZEL ODA", "Oda oluştur / Katıl", RefPurple, Modifier.weight(1f), onPlay)
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = RefPanel), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, RefStroke)) {
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    RefMiniStat("3 × 10", "KELİME")
                    RefMiniStat("45 sn", "SÜRE")
                    RefMiniStat("3", "ROUND")
                    RefMiniStat("TR / EN", "DİL")
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = RefPanel), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, RefStroke)) {
                Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("HAFTANIN EN İYİLERİ", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            RefLanguagePill("🇹🇷 TR", weekLanguage == "tr") { weekLanguage = "tr" }
                            RefLanguagePill("🇬🇧 EN", weekLanguage == "en") { weekLanguage = "en" }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        repeat(3) { index -> RefTopCard(index, leaders.getOrNull(index), Modifier.weight(1f)) }
                    }
                    Button(
                        onClick = onLeaderboard,
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263ACB)),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("TÜM LİDERLİK TABLOSU", fontSize = 10.sp, fontWeight = FontWeight.Black) }
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
    val wins = profile?.wins ?: 0
    val losses = profile?.losses ?: 0
    val matches = wins + losses
    val rate = if (matches == 0) 0 else wins * 100 / matches

    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                RefRoundIcon("‹")
                RefRoundIcon("✎")
            }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                RefAvatar(profile?.displayName ?: "O", 88.dp, RefGold)
                Spacer(Modifier.height(8.dp))
                Text(profile?.displayName ?: "Oyuncu", fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text(if (profile?.isVip == true) "SON HARF USTASI  ♥" else "SON HARF OYUNCUSU  ♥", color = RefMuted, fontSize = 10.sp)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Seviye 23", fontSize = 9.sp)
                    Spacer(Modifier.width(8.dp))
                    LinearProgressIndicator(progress = { .64f }, modifier = Modifier.weight(1f).height(5.dp), color = RefPurple, trackColor = RefPanel2)
                    Spacer(Modifier.width(8.dp))
                    Text("3.250 / 5.000", fontSize = 9.sp, color = RefMuted)
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    RefMetric(wins.toString(), "Galibiyet", Modifier.weight(1f))
                    RefMetric(losses.toString(), "Mağlubiyet", Modifier.weight(1f))
                    RefMetric("%$rate", "Kazanma Oranı", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    RefMetric(matches.toString(), "Toplam Maç", Modifier.weight(1f))
                    RefMetric("—", "Toplam Round", Modifier.weight(1f))
                    RefMetric("—", "Toplam Kelime", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    RefMetric("—", "En Uzun Seri", Modifier.weight(1f))
                    RefMetric("—", "Söz Fırtınası", Modifier.weight(1f))
                    RefMetric("—", "En Yüksek Puan", Modifier.weight(1f))
                }
            }
        }

        item {
            Text("SON BAŞARILAR", fontSize = 11.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("♛", "★", "✦", "♜", "✪").forEachIndexed { i, icon ->
                    Surface(
                        modifier = Modifier.size(52.dp), shape = CircleShape,
                        color = if (i < 3) Color(0xFF2B2112) else RefPanel2,
                        border = BorderStroke(1.dp, if (i < 3) RefGold.copy(alpha = .65f) else RefStroke)
                    ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(icon, fontSize = 20.sp, color = if (i < 3) RefGold else RefMuted) } }
                }
            }
        }
    }
}

@Composable
private fun ReferenceLeaderboard(onBack: () -> Unit) {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var rows by remember { mutableStateOf<List<ProfileDto>>(emptyList()) }
    var period by remember { mutableStateOf(0) }
    var language by remember { mutableStateOf("tr") }
    LaunchedEffect(Unit) {
        rows = runCatching { backend?.getLeaderboard(50)?.map { it.profile } ?: emptyList() }.getOrDefault(emptyList())
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("‹", color = RefPurple, fontSize = 30.sp, fontWeight = FontWeight.Black) }
                Text("LİDERLİK TABLOSU", fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
        }
        item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(RefPanel2).padding(4.dp)) {
                RefTab("🇹🇷 TÜRKÇE", language == "tr", Modifier.weight(1f)) { language = "tr" }
                RefTab("🇬🇧 ENGLISH", language == "en", Modifier.weight(1f)) { language = "en" }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(RefPanel2).padding(4.dp)) {
                listOf("Toplam", "Bu Hafta", "Bu Ay").forEachIndexed { i, t ->
                    RefTab(t, period == i, Modifier.weight(1f)) { period = i }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                Text("#", Modifier.width(40.dp), color = RefMuted, fontSize = 9.sp)
                Text("Oyuncu", Modifier.weight(1f), color = RefMuted, fontSize = 9.sp)
                Text("Galibiyet", Modifier.width(72.dp), color = RefMuted, fontSize = 9.sp, textAlign = TextAlign.Center)
                Text("Kazanma %", Modifier.width(72.dp), color = RefMuted, fontSize = 9.sp, textAlign = TextAlign.Center)
            }
        }
        itemsIndexed(rows) { index, p ->
            val m = p.wins + p.losses
            val wr = if (m == 0) 0 else p.wins * 100 / m
            Card(colors = CardDefaults.cardColors(containerColor = RefPanel), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, if (index == 0) RefGold.copy(alpha = .45f) else RefStroke)) {
                Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (index < 3) listOf("♛", "♜", "♝")[index] else "${index + 1}", Modifier.width(40.dp), color = if (index == 0) RefGold else RefMuted, textAlign = TextAlign.Center)
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        RefAvatar(p.displayName, 30.dp, if (index == 0) RefGold else RefPurple)
                        Spacer(Modifier.width(8.dp))
                        Text(p.displayName, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                    }
                    Text(p.wins.toString(), Modifier.width(72.dp), textAlign = TextAlign.Center, fontSize = 12.sp)
                    Text("%$wr", Modifier.width(72.dp), textAlign = TextAlign.Center, fontSize = 12.sp)
                }
            }
        }
        if (rows.isEmpty()) item { Text("Sıralama verisi bekleniyor.", modifier = Modifier.fillMaxWidth().padding(24.dp), color = RefMuted, textAlign = TextAlign.Center) }
    }
}

@Composable
private fun ReferenceSettings() {
    val context = LocalContext.current
    var sound by remember { mutableStateOf(SonHarfPreferences.soundEnabled(context)) }
    var vibration by remember { mutableStateOf(SonHarfPreferences.vibrationEnabled(context)) }
    var notifications by remember { mutableStateOf(SonHarfPreferences.notificationsEnabled(context)) }
    var music by remember { mutableStateOf(false) }
    var gameInvites by remember { mutableStateOf(true) }
    var friendInvites by remember { mutableStateOf(true) }
    var systemNotifications by remember { mutableStateOf(true) }
    var darkMode by remember { mutableStateOf(true) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("AYARLAR", fontSize = 28.sp, fontWeight = FontWeight.Black) }
        item { RefSectionTitle("SES AYARLARI") }
        item {
            RefSettingsCard {
                RefSwitchRow("Ses Efektleri", sound) { sound = it; SonHarfPreferences.setSoundEnabled(context, it) }
                RefSwitchRow("Müzik", music) { music = it }
                RefSwitchRow("Titreşim", vibration) { vibration = it; SonHarfPreferences.setVibrationEnabled(context, it) }
            }
        }
        item { RefSectionTitle("BİLDİRİMLER") }
        item {
            RefSettingsCard {
                RefSwitchRow("Oyun Davetleri", gameInvites) { gameInvites = it }
                RefSwitchRow("Arkadaşlık İstekleri", friendInvites) { friendInvites = it }
                RefSwitchRow("Sistem Bildirimleri", systemNotifications && notifications) {
                    systemNotifications = it
                    notifications = it
                    SonHarfPreferences.setNotificationsEnabled(context, it)
                }
            }
        }
        item { RefSectionTitle("DİĞER") }
        item {
            RefSettingsCard {
                RefChevronRow("Dil", "Türkçe")
                RefSwitchRow("Karanlık Mod", darkMode) { darkMode = it }
                RefChevronRow("Engellenenler", "")
            }
        }
    }
}

@Composable
private fun ReferenceShop() {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("MAĞAZA", fontSize = 28.sp, fontWeight = FontWeight.Black) }
        item { Text("Kozmetik ve premium içerikler", color = RefMuted, fontSize = 11.sp) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RefPanel), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, RefStroke)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("♛ VIP", color = RefGold, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text("Reklamsız deneyim • özel temalar • gelişmiş istatistikler • özel oda oluşturma", color = RefMuted, fontSize = 11.sp, lineHeight = 17.sp)
                    OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("GOOGLE PLAY BILLING HAZIR DEĞİL") }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RefStoreCard("💎", "ELMAS", "Yakında", Modifier.weight(1f))
                RefStoreCard("🎨", "TEMALAR", "Yakında", Modifier.weight(1f))
                RefStoreCard("☺", "KOZMETİK", "Yakında", Modifier.weight(1f))
            }
        }
    }
}

@Composable private fun RefAvatar(name: String, size: androidx.compose.ui.unit.Dp, accent: Color) {
    Box(Modifier.size(size).clip(CircleShape).background(accent.copy(alpha = .16f)).padding(2.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFF172136)), contentAlignment = Alignment.Center) {
            Text(name.take(1).uppercase(), color = RefText, fontWeight = FontWeight.Black, fontSize = (size.value * .34f).sp)
        }
    }
}

@Composable private fun RefRoundIcon(text: String) {
    Surface(modifier = Modifier.size(34.dp), shape = CircleShape, color = RefPanel2, border = BorderStroke(1.dp, RefStroke)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text, color = RefText, fontSize = 13.sp) }
    }
}

@Composable private fun RefActionCard(icon: String, title: String, subtitle: String, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = RefPanel), shape = RoundedCornerShape(13.dp), border = BorderStroke(1.dp, accent.copy(alpha = .35f))) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 19.sp); Spacer(Modifier.width(8.dp)); Column { Text(title, fontSize = 10.sp, fontWeight = FontWeight.Black); Text(subtitle, color = RefMuted, fontSize = 7.sp) }
        }
    }
}

@Composable private fun RefMiniStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(value, fontSize = 11.sp, fontWeight = FontWeight.Black); Text(label, color = RefMuted, fontSize = 6.sp) }
}

@Composable private fun RefLanguagePill(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(modifier = Modifier.clickable(onClick = onClick), shape = RoundedCornerShape(8.dp), color = if (selected) RefPurple.copy(alpha = .22f) else RefPanel2, border = BorderStroke(1.dp, if (selected) RefPurple else RefStroke)) {
        Text(text, Modifier.padding(horizontal = 7.dp, vertical = 4.dp), fontSize = 7.sp, color = if (selected) RefText else RefMuted)
    }
}

@Composable private fun RefTopCard(index: Int, p: ProfileDto?, modifier: Modifier) {
    val accent = listOf(RefGold, Color(0xFF8A9AB7), Color(0xFFD98248))[index]
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1423)), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, accent.copy(alpha = .45f))) {
        Column(Modifier.fillMaxWidth().padding(7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(listOf("♛", "♜", "♝")[index], color = accent, fontSize = 14.sp)
            RefAvatar(p?.displayName ?: "?", 36.dp, accent)
            Spacer(Modifier.height(4.dp))
            Text(p?.displayName ?: "—", fontSize = 9.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text("${p?.wins ?: 0} GALİBİYET", color = RefMuted, fontSize = 6.sp)
        }
    }
}

@Composable private fun RefMetric(value: String, label: String, modifier: Modifier) {
    Card(modifier = modifier.height(76.dp), colors = CardDefaults.cardColors(containerColor = RefPanel), shape = RoundedCornerShape(13.dp), border = BorderStroke(1.dp, RefStroke)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text(label, color = RefMuted, fontSize = 7.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable private fun RefTab(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier.height(38.dp), colors = ButtonDefaults.buttonColors(containerColor = if (selected) Color(0xFF3B43D8) else Color.Transparent), shape = RoundedCornerShape(11.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
        Text(text, fontSize = 9.sp, color = if (selected) RefText else RefMuted)
    }
}

@Composable private fun RefSectionTitle(text: String) { Text(text, color = RefCyan, fontSize = 11.sp, fontWeight = FontWeight.Black) }

@Composable private fun RefSettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = RefPanel), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, RefStroke)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), content = content)
    }
}

@Composable private fun RefSwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, fontSize = 12.sp)
        Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = RefGreen, uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFF3A3A45)))
    }
}

@Composable private fun RefChevronRow(title: String, value: String) {
    Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, fontSize = 12.sp)
        Row(verticalAlignment = Alignment.CenterVertically) { if (value.isNotBlank()) Text(value, color = RefMuted, fontSize = 10.sp); Spacer(Modifier.width(8.dp)); Text("›", color = RefMuted, fontSize = 18.sp) }
    }
}

@Composable private fun RefStoreCard(icon: String, title: String, subtitle: String, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = RefPanel), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, RefStroke)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(icon, fontSize = 24.sp); Text(title, fontSize = 10.sp, fontWeight = FontWeight.Black); Text(subtitle, color = RefMuted, fontSize = 8.sp) }
    }
}
