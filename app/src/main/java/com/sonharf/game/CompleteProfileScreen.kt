package com.sonharf.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CompleteProfileScreen(
    initialTab: Int = 0,
    onBack: (() -> Unit)? = null,
) {
    var tab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(SonHarfBg, SonHarfSurface2, SonHarfBg)))) {
        if (onBack != null) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = sh("Geri", "Back"), tint = SonHarfText)
                }
                Text(sh("PROFİL", "PROFILE"), color = SonHarfText, fontWeight = FontWeight.Black, fontSize = 21.sp)
            }
        }
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 10.dp, containerColor = SonHarfBg, divider = {}) {
            listOf(sh("OYUNCU KARTI", "PLAYER CARD"), sh("GİZLİLİK & AYARLAR", "PRIVACY & SETTINGS"), sh("TERCİHLER", "PREFERENCES")).forEachIndexed { index, title ->
                Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title, color = if (tab == index) SonHarfCyan else SonHarfMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp) })
            }
        }
        Surface(Modifier.fillMaxWidth().height(1.dp), color = SonHarfCyan.copy(alpha = .30f), shape = RoundedCornerShape(999.dp)) {}
        Box(Modifier.weight(1f)) {
            when (tab) { 0 -> ProfileExperienceV2Screen(); 1 -> FinalProfileScreen(); else -> DetailedPreferencesSettings() }
        }
    }
}

@Composable
private fun DetailedPreferencesSettings() {
    val context = LocalContext.current
    var language by remember { mutableStateOf(SonHarfPreferences.language(context)) }
    var gameInvites by remember { mutableStateOf(SonHarfPreferences.gameInviteNotificationsEnabled(context)) }
    var friendRequests by remember { mutableStateOf(SonHarfPreferences.friendRequestNotificationsEnabled(context)) }
    var system by remember { mutableStateOf(SonHarfPreferences.systemNotificationsEnabled(context)) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(sh("UYGULAMA TERCİHLERİ", "APP PREFERENCES"), color = SonHarfText, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(sh("Dil ve bildirim ayarlarını ayrı ayrı yönet.", "Manage language and notification settings separately."), color = SonHarfMuted, fontSize = 13.sp)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(sh("Uygulama dili", "App language"), color = SonHarfText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = language == "tr", onClick = { language = "tr"; SonHarfPreferences.setLanguage(context, "tr") }, label = { Text("🇹🇷 TÜRKÇE") }, modifier = Modifier.weight(1f))
                        FilterChip(selected = language == "en", onClick = { language = "en"; SonHarfPreferences.setLanguage(context, "en") }, label = { Text("🇬🇧 ENGLISH") }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        item { NotificationToggleCard("⚔", sh("Oyun davetleri", "Game invitations"), sh("Arkadaşların seni düelloya çağırdığında uyar.", "Alerts when friends invite you to a duel."), gameInvites) { gameInvites = it; SonHarfPreferences.setGameInviteNotificationsEnabled(context, it) } }
        item { NotificationToggleCard("👥", sh("Arkadaşlık istekleri", "Friend requests"), sh("Yeni arkadaşlık isteği geldiğinde uyar.", "Alerts when a new friend request arrives."), friendRequests) { friendRequests = it; SonHarfPreferences.setFriendRequestNotificationsEnabled(context, it) } }
        item { NotificationToggleCard("✦", sh("Sistem duyuruları", "System announcements"), sh("Ödül, bakım ve önemli oyun duyuruları.", "Rewards, maintenance and important game announcements."), system) { system = it; SonHarfPreferences.setSystemNotificationsEnabled(context, it) } }
    }
}

@Composable
private fun NotificationToggleCard(icon: String, title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, if (checked) SonHarfCyan.copy(alpha = .38f) else SonHarfMuted.copy(alpha = .10f))) {
        Row(Modifier.fillMaxWidth().padding(15.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(icon, fontSize = 25.sp)
            Column(Modifier.weight(1f)) { Text(title, color = SonHarfText, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text(description, color = SonHarfMuted, fontSize = 12.sp) }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}
