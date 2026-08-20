package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.getLeaderboard

@Composable
fun AuroraSonHarfApp() {
    var screen by remember { mutableStateOf(AppScreen.HOME) }
    Scaffold(
        containerColor = SonHarfBg,
        bottomBar = { if (screen != AppScreen.LEADERBOARD) AuroraBottomBar(screen) { screen = it } },
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).background(
                Brush.verticalGradient(listOf(SonHarfSurface2, SonHarfBg, SonHarfSurface)),
            ),
        ) {
            when (screen) {
                AppScreen.HOME -> AuroraHome({ screen = AppScreen.GAME }, { screen = AppScreen.LEADERBOARD }, { screen = AppScreen.SHOP })
                AppScreen.GAME -> OnlineGameScreenV6()
                AppScreen.SHOP -> EconomyShopScreen()
                AppScreen.PROFILE -> ProfileExperienceScreen()
                AppScreen.MORE -> AuroraSettings()
                AppScreen.LEADERBOARD -> LeaderboardExperienceScreen { screen = AppScreen.HOME }
            }
        }
    }
}

@Composable
private fun AuroraBottomBar(screen: AppScreen, onChange: (AppScreen) -> Unit) {
    val context = LocalContext.current
    NavigationBar(containerColor = SonHarfSurface, tonalElevation = 0.dp) {
        listOf(
            Triple(AppScreen.HOME, "⌂", sh("Ana Sayfa", "Home")),
            Triple(AppScreen.GAME, "⚔", sh("Oyna", "Play")),
            Triple(AppScreen.SHOP, "◇", sh("Mağaza", "Shop")),
            Triple(AppScreen.PROFILE, "♙", sh("Profil", "Profile")),
            Triple(AppScreen.MORE, "•••", sh("Daha Fazla", "More")),
        ).forEach { (target, icon, label) ->
            NavigationBarItem(
                selected = screen == target,
                onClick = { SonHarfSoundFx.tap(); SonHarfPreferences.hapticTap(context); onChange(target) },
                icon = {
                    Surface(
                        color = if (screen == target) SonHarfPurple.copy(alpha = .22f) else Color.Transparent,
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(icon, Modifier.padding(horizontal = 11.dp, vertical = 5.dp), color = if (screen == target) SonHarfCyan else SonHarfMuted, fontWeight = FontWeight.Black, fontSize = 19.sp)
                    }
                },
                label = { Text(label, maxLines = 1, fontSize = 9.sp) },
                colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent, selectedTextColor = SonHarfText, unselectedTextColor = SonHarfMuted),
            )
        }
    }
}

@Composable
private fun AuroraHome(onPlay: () -> Unit, onLeaderboard: () -> Unit, onShop: () -> Unit) {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var top by remember { mutableStateOf<List<ProfileDto>>(emptyList()) }
    LaunchedEffect(Unit) {
        val b = backend ?: return@LaunchedEffect
        if (b.currentUserId() == null) runCatching { b.ensurePlayer(sh("Oyuncu", "Player")) }
        profile = b.currentUserId()?.let { runCatching { b.getProfile(it) }.getOrNull() }
        top = runCatching { b.getLeaderboard(3).map { it.profile } }.getOrDefault(emptyList())
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NeonAvatar(profile?.displayName ?: "O", 40.dp)
                    Spacer(Modifier.width(9.dp))
                    Column {
                        Text(profile?.displayName ?: sh("Oyuncu", "Player"), fontWeight = FontWeight.Black, fontSize = 14.sp)
                        Text(if (profile?.isVip == true) "VIP • ${sh("Seviye 23", "Level 23")}" else sh("Seviye 23", "Level 23"), color = if (profile?.isVip == true) SonHarfGold else SonHarfMuted, fontSize = 10.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(onClick = onShop, color = SonHarfCyan.copy(alpha = .12f), shape = RoundedCornerShape(99.dp), border = BorderStroke(1.dp, SonHarfCyan.copy(alpha = .3f))) {
                        Text("💎 ${profile?.diamonds ?: 0}", Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = SonHarfCyan, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                    RoundIcon("♩")
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = SonHarfSurface), border = BorderStroke(1.dp, SonHarfMuted.copy(alpha = .12f))) {
                Box(Modifier.fillMaxWidth().height(185.dp).background(Brush.radialGradient(listOf(SonHarfPurple.copy(alpha = .24f), SonHarfSurface2, SonHarfSurface))), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(sh("HEMEN OYNA", "PLAY NOW"), fontWeight = FontWeight.Black, fontSize = 24.sp)
                        Text(sh("RAKİBİNİ BUL VE DÜELLOYA BAŞLA!", "FIND AN OPPONENT AND START THE DUEL!"), color = SonHarfMuted, fontSize = 10.sp)
                        Spacer(Modifier.height(18.dp))
                        Button(onClick = onPlay, modifier = Modifier.fillMaxWidth(.78f).height(58.dp), shape = RoundedCornerShape(30.dp), colors = ButtonDefaults.buttonColors(containerColor = SonHarfGold, contentColor = Color(0xFF181006))) {
                            Text(sh("DÜELLOYA GİR  ⚡", "ENTER DUEL  ⚡"), fontWeight = FontWeight.Black, fontSize = 18.sp)
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                ModeCard("⟳", sh("RASTGELE", "RANDOM"), sh("Hızlı eşleşme", "Quick match"), SonHarfCyan, Modifier.weight(1f), onPlay)
                ModeCard("👥", sh("ARKADAŞLA", "WITH FRIEND"), sh("Davet et", "Invite"), SonHarfGreen, Modifier.weight(1f), onPlay)
                ModeCard("♛", sh("ÖZEL ODA", "PRIVATE ROOM"), sh("VIP oluşturur", "VIP creates"), SonHarfPurple, Modifier.weight(1f), onPlay)
            }
        }
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = SonHarfSurface), border = BorderStroke(1.dp, SonHarfMuted.copy(alpha = .12f))) {
                Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(sh("HAFTANIN EN İYİLERİ", "BEST OF THE WEEK"), fontWeight = FontWeight.Black, fontSize = 12.sp)
                        Text(sh("Bu Hafta  ›", "This Week  ›"), color = SonHarfMuted, fontSize = 10.sp)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        repeat(3) { index -> TopPlayerCard(index, top.getOrNull(index), Modifier.weight(1f)) }
                    }
                }
            }
        }
        item {
            Card(onClick = onShop, colors = CardDefaults.cardColors(containerColor = Color.Transparent), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, SonHarfGold.copy(alpha = .45f))) {
                Row(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(SonHarfPurple.copy(alpha = .20f), SonHarfGold.copy(alpha = .14f), SonHarfSurface))).padding(13.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("♛", color = SonHarfGold, fontSize = 26.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(if (profile?.isVip == true) sh("VIP AKTİF", "VIP ACTIVE") else sh("VIP ÜYELİK", "VIP MEMBERSHIP"), color = SonHarfGold, fontWeight = FontWeight.Black)
                            Text(sh("Özel oda • kozmetik • aylık 400 elmas", "Private rooms • cosmetics • 400 monthly diamonds"), fontSize = 9.sp, color = SonHarfMuted)
                        }
                    }
                    Text("›", color = SonHarfGold, fontSize = 24.sp)
                }
            }
        }
        item {
            Button(onClick = onLeaderboard, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = SonHarfBlue)) {
                Text(sh("LİDERLİK TABLOSUNU AÇ", "OPEN LEADERBOARD"), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AuroraSettings() {
    val context = LocalContext.current
    var sound by remember { mutableStateOf(SonHarfPreferences.soundEnabled(context)) }
    var vibration by remember { mutableStateOf(SonHarfPreferences.vibrationEnabled(context)) }
    var gameInvites by remember { mutableStateOf(SonHarfPreferences.gameInviteNotificationsEnabled(context)) }
    var friendRequests by remember { mutableStateOf(SonHarfPreferences.friendRequestNotificationsEnabled(context)) }
    var systemNotifications by remember { mutableStateOf(SonHarfPreferences.systemNotificationsEnabled(context)) }
    var darkMode by remember { mutableStateOf(SonHarfPreferences.darkModeEnabled(context)) }
    var languageMenu by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(sh("AYARLAR", "SETTINGS"), fontSize = 27.sp, fontWeight = FontWeight.Black) }
        item {
            SettingsGroup(sh("SES & TİTREŞİM", "SOUND & VIBRATION")) {
                SettingSwitch(sh("Ses Efektleri", "Sound Effects"), sound) { sound = it; SonHarfPreferences.setSoundEnabled(context, it) }
                SettingSwitch(sh("Titreşim", "Vibration"), vibration) { vibration = it; SonHarfPreferences.setVibrationEnabled(context, it) }
            }
        }
        item {
            SettingsGroup(sh("BİLDİRİMLER", "NOTIFICATIONS")) {
                SettingSwitch(sh("Oyun Davetleri", "Game Invites"), gameInvites) { gameInvites = it; SonHarfPreferences.setGameInviteNotificationsEnabled(context, it) }
                SettingSwitch(sh("Arkadaşlık İstekleri", "Friend Requests"), friendRequests) { friendRequests = it; SonHarfPreferences.setFriendRequestNotificationsEnabled(context, it) }
                SettingSwitch(sh("Sistem Bildirimleri", "System Notifications"), systemNotifications) { systemNotifications = it; SonHarfPreferences.setSystemNotificationsEnabled(context, it) }
            }
        }
        item {
            SettingsGroup(sh("DİĞER", "OTHER")) {
                Box(Modifier.fillMaxWidth()) {
                    SettingActionLine(sh("Dil", "Language"), if (SonHarfUiState.isEnglish) "English  ›" else "Türkçe  ›") { languageMenu = true }
                    DropdownMenu(expanded = languageMenu, onDismissRequest = { languageMenu = false }) {
                        DropdownMenuItem(text = { Text("Türkçe") }, onClick = { SonHarfPreferences.setLanguage(context, "tr"); languageMenu = false })
                        DropdownMenuItem(text = { Text("English") }, onClick = { SonHarfPreferences.setLanguage(context, "en"); languageMenu = false })
                    }
                }
                SettingSwitch(sh("Karanlık Mod", "Dark Mode"), darkMode) { darkMode = it; SonHarfPreferences.setDarkModeEnabled(context, it) }
                SettingLine(sh("Engellenenler", "Blocked Users"), "›")
            }
        }
    }
}

@Composable private fun ModeCard(icon: String, title: String, sub: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier.height(96.dp), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = .10f)), shape = RoundedCornerShape(17.dp), border = BorderStroke(1.dp, color.copy(alpha = .45f))) {
        Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(icon, fontSize = 24.sp, color = color); Text(title, fontWeight = FontWeight.Black, fontSize = 10.sp); Text(sub, color = SonHarfMuted, fontSize = 8.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable private fun TopPlayerCard(index: Int, p: ProfileDto?, modifier: Modifier) {
    val c = listOf(SonHarfGold, Color(0xFFB7C1D8), Color(0xFFFF8C4A))[index]
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = c.copy(alpha = .08f)), shape = RoundedCornerShape(15.dp), border = BorderStroke(1.dp, c.copy(alpha = .42f))) {
        Column(Modifier.padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${index + 1}", color = c, fontWeight = FontWeight.Black); Spacer(Modifier.height(5.dp)); NeonAvatar(p?.displayName ?: "—", 38.dp); Spacer(Modifier.height(5.dp)); Text(p?.displayName ?: sh("Bekleniyor", "Waiting"), fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1); Text(if (p == null) "—" else "${p.wins} ${sh("GALİBİYET", "WINS")}", color = SonHarfMuted, fontSize = 7.sp)
        }
    }
}

@Composable private fun NeonAvatar(name: String, size: Dp) {
    Box(Modifier.size(size).clip(CircleShape).background(Brush.linearGradient(listOf(SonHarfCyan, SonHarfPurple, SonHarfPink))).padding(2.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().clip(CircleShape).background(SonHarfSurface2), contentAlignment = Alignment.Center) { Text(name.take(1).uppercase(), fontWeight = FontWeight.Black, fontSize = (size.value * .38f).sp) }
    }
}

@Composable private fun RoundIcon(text: String) {
    Surface(shape = CircleShape, color = SonHarfSurface2, border = BorderStroke(1.dp, SonHarfMuted.copy(alpha = .12f))) { Text(text, Modifier.padding(9.dp), color = SonHarfMuted, fontSize = 16.sp) }
}

@Composable private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = SonHarfCyan, fontSize = 10.sp, fontWeight = FontWeight.Black)
        Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, SonHarfMuted.copy(alpha = .12f))) { Column(Modifier.padding(horizontal = 13.dp, vertical = 4.dp), content = content) }
    }
}

@Composable private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(label, fontSize = 12.sp); Switch(checked, onChange) }
}

@Composable private fun SettingLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(label, fontSize = 12.sp); Text(value, color = SonHarfMuted, fontSize = 11.sp) }
}

@Composable private fun SettingActionLine(label: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(48.dp).clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(label, fontSize = 12.sp); Text(value, color = SonHarfMuted, fontSize = 11.sp) }
}
