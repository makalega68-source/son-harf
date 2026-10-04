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
    Column(Modifier.fillMaxSize().menuGround()) {
        Box(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            MainScreenHeader(sh("Oyuncu profili", "Player profile"),
                sh("Kimliğin, gizliliğin ve tercihlerin", "Your identity, privacy and preferences"), onBack = onBack)
        }
        LobbyTabs(listOf(sh("Kimlik", "Identity"), sh("Gizlilik", "Privacy"), sh("Tercihler", "Preferences")),
            selected = tab, onSelect = { tab = it }, modifier = Modifier.padding(horizontal = 16.dp))
        Surface(Modifier.fillMaxWidth().height(1.dp), color = SonHarfBlue.copy(alpha = .18f), shape = RoundedCornerShape(999.dp)) {}
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
    val privacyOptionsRequired = AdPrivacyManager.privacyOptionsRequired
    var privacyNotice by remember { mutableStateOf<String?>(null) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(sh("UYGULAMA TERCİHLERİ", "APP PREFERENCES"), color = SonHarfGold, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(sh("Dil ve bildirim ayarlarını buradan yönet.", "Manage language and notification settings here."), color = LobbyPalette.Muted, fontSize = 12.sp)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = LobbyPalette.Paper), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(sh("Uygulama dili", "App language"), color = LobbyPalette.Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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

        if (privacyOptionsRequired) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = LobbyPalette.Paper),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SonHarfBlue.copy(alpha = .22f)),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(15.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        Text(
                            sh("Reklam gizlilik seçenekleri", "Ad privacy options"),
                            color = LobbyPalette.Ink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                        Text(
                            sh(
                                "Google reklam gizliliği tercihlerini görüntüle veya değiştir. Bu seçenek yalnız bölgen ve mevcut mesaj ayarları gerektirdiğinde görünür.",
                                "View or change your Google ad privacy choices. This option appears only when required for your region and current message settings.",
                            ),
                            color = LobbyPalette.Muted,
                            fontSize = 12.sp,
                        )
                        Button(
                            onClick = {
                                val activity = AdPrivacyManager.findActivity(context)
                                if (activity == null) {
                                    privacyNotice = sh("Gizlilik formu açılamadı.", "Privacy form could not be opened.")
                                } else {
                                    AdPrivacyManager.showPrivacyOptions(activity) { success ->
                                        privacyNotice = if (success) {
                                            sh("Reklam gizliliği tercihleri güncellendi.", "Ad privacy choices updated.")
                                        } else {
                                            sh("Gizlilik formu tamamlanamadı.", "Privacy form could not be completed.")
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SonHarfBlue, contentColor = androidx.compose.ui.graphics.Color.White),
                        ) {
                            Text(
                                sh("GİZLİLİK SEÇENEKLERİNİ AÇ", "OPEN PRIVACY OPTIONS"),
                                fontWeight = FontWeight.Black,
                            )
                        }
                        privacyNotice?.let {
                            Text(it, color = LobbyPalette.Muted, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationToggleCard(icon: String, title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = LobbyPalette.Paper), shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, if (checked) SonHarfCyan.copy(alpha = .38f) else LobbyPalette.Muted.copy(alpha = .10f))) {
        Row(Modifier.fillMaxWidth().padding(15.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(icon, fontSize = 25.sp)
            Column(Modifier.weight(1f)) { Text(title, color = LobbyPalette.Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text(description, color = LobbyPalette.Muted, fontSize = 12.sp) }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}
