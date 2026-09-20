package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

    Column(Modifier.fillMaxSize().background(MainUi.Background)) {
        if (onBack != null) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                MainScreenHeader(
                    title = sh("Oyuncu Profili", "Player Profile"),
                    subtitle = sh("Kimlik, gizlilik ve oyun ayarların", "Identity, privacy and game settings"),
                    onBack = onBack,
                )
            }
        }

        ScrollableTabRow(
            selectedTabIndex = tab,
            edgePadding = 16.dp,
            containerColor = Color.Transparent,
            contentColor = MainUi.Blue,
            divider = { HorizontalDivider(color = MainUi.Border) },
            indicator = { positions ->
                if (tab < positions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(positions[tab]),
                        color = MainUi.Blue,
                    )
                }
            },
        ) {
            listOf(
                sh("KİMLİK", "IDENTITY"),
                sh("GİZLİLİK", "PRIVACY"),
                sh("TERCİHLER", "PREFERENCES"),
            ).forEachIndexed { index, title ->
                Tab(
                    selected = tab == index,
                    onClick = { tab = index },
                    text = {
                        Text(
                            title,
                            color = if (tab == index) MainUi.Blue else MainUi.Muted,
                            fontWeight = if (tab == index) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp,
                        )
                    },
                )
            }
        }

        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> ProfileExperienceV2Screen()
                1 -> FinalProfileScreen()
                else -> DetailedPreferencesSettings()
            }
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

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            MainSectionTitle(sh("Uygulama Tercihleri", "App Preferences"))
            Spacer(Modifier.height(4.dp))
            Text(
                sh("Dil ve bildirim ayarlarını buradan yönet.", "Manage language and notification settings here."),
                color = MainUi.Muted,
                fontSize = 12.sp,
            )
        }

        item {
            MainGameCard {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = MainUiShape.Control, color = MainUi.BlueSoft) {
                            Icon(
                                Icons.Rounded.Language,
                                contentDescription = null,
                                tint = MainUi.Blue,
                                modifier = Modifier.padding(9.dp).size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(sh("Uygulama dili", "App language"), color = MainUi.Text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(sh("Şimdilik Türkçe ve İngilizce", "Turkish and English for now"), color = MainUi.Muted, fontSize = 10.sp)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = language == "tr",
                            onClick = {
                                language = "tr"
                                SonHarfPreferences.setLanguage(context, "tr")
                            },
                            label = { Text("TÜRKÇE", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f).heightIn(min = 46.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MainUi.BlueSoft,
                                selectedLabelColor = MainUi.Blue,
                                containerColor = MainUi.Surface,
                                labelColor = MainUi.Text,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = language == "tr",
                                borderColor = MainUi.Border,
                                selectedBorderColor = MainUi.Blue.copy(alpha = .55f),
                            ),
                        )
                        FilterChip(
                            selected = language == "en",
                            onClick = {
                                language = "en"
                                SonHarfPreferences.setLanguage(context, "en")
                            },
                            label = { Text("ENGLISH", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f).heightIn(min = 46.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MainUi.BlueSoft,
                                selectedLabelColor = MainUi.Blue,
                                containerColor = MainUi.Surface,
                                labelColor = MainUi.Text,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = language == "en",
                                borderColor = MainUi.Border,
                                selectedBorderColor = MainUi.Blue.copy(alpha = .55f),
                            ),
                        )
                    }
                }
            }
        }

        item {
            NotificationToggleCard(
                icon = Icons.Rounded.SportsEsports,
                title = sh("Oyun davetleri", "Game invitations"),
                description = sh("Arkadaşların seni oyuna çağırdığında uyar.", "Alerts when friends invite you to play."),
                checked = gameInvites,
            ) {
                gameInvites = it
                SonHarfPreferences.setGameInviteNotificationsEnabled(context, it)
            }
        }
        item {
            NotificationToggleCard(
                icon = Icons.Rounded.Groups,
                title = sh("Arkadaşlık istekleri", "Friend requests"),
                description = sh("Yeni arkadaşlık isteği geldiğinde uyar.", "Alerts when a new friend request arrives."),
                checked = friendRequests,
            ) {
                friendRequests = it
                SonHarfPreferences.setFriendRequestNotificationsEnabled(context, it)
            }
        }
        item {
            NotificationToggleCard(
                icon = Icons.Rounded.Campaign,
                title = sh("Sistem duyuruları", "System announcements"),
                description = sh("Ödül, bakım ve önemli oyun duyuruları.", "Rewards, maintenance and important game announcements."),
                checked = system,
            ) {
                system = it
                SonHarfPreferences.setSystemNotificationsEnabled(context, it)
            }
        }

        if (privacyOptionsRequired) {
            item {
                MainGameCard {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = MainUiShape.Control, color = MainUi.BlueSoft) {
                                Icon(
                                    Icons.Rounded.Security,
                                    contentDescription = null,
                                    tint = MainUi.Blue,
                                    modifier = Modifier.padding(9.dp).size(20.dp),
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                sh("Reklam gizlilik seçenekleri", "Ad privacy options"),
                                color = MainUi.Text,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                            )
                        }
                        Text(
                            sh(
                                "Google reklam gizliliği tercihlerini görüntüle veya değiştir. Bu seçenek yalnız bölgen ve mevcut mesaj ayarları gerektirdiğinde görünür.",
                                "View or change your Google ad privacy choices. This option appears only when required for your region and current message settings.",
                            ),
                            color = MainUi.Muted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                        )
                        MainSecondaryButton(
                            text = sh("GİZLİLİK SEÇENEKLERİNİ AÇ", "OPEN PRIVACY OPTIONS"),
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
                            modifier = Modifier.fillMaxWidth(),
                        )
                        privacyNotice?.let {
                            Text(it, color = MainUi.Muted, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationToggleCard(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    MainGameCard {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = MainUiShape.Control,
                color = if (checked) MainUi.BlueSoft else MainUi.SurfaceSoft,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (checked) MainUi.Blue else MainUi.Muted,
                    modifier = Modifier.padding(9.dp).size(20.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = MainUi.Text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(2.dp))
                Text(description, color = MainUi.Muted, fontSize = 11.sp, lineHeight = 15.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MainUi.Blue,
                    uncheckedThumbColor = MainUi.Muted,
                    uncheckedTrackColor = MainUi.SurfaceRaised,
                    uncheckedBorderColor = MainUi.Border,
                ),
            )
        }
    }
}
