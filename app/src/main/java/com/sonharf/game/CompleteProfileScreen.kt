package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CompleteProfileScreen(
    initialTab: Int = 0,
    onBack: (() -> Unit)? = null,
) {
    var tab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 2)) }

    Column(Modifier.fillMaxSize()) {
        if (onBack != null) {
            GameTopBar(
                title = gameText("Oyuncu Profili", "Player Profile"),
                subtitle = gameText(
                    "Kimlik, gizlilik ve oyun tercihleri",
                    "Identity, privacy and game preferences",
                ),
                onBack = onBack,
            )
        }

        SegmentedGameTabs(
            labels = listOf(
                gameText("KİMLİK", "IDENTITY"),
                gameText("GİZLİLİK", "PRIVACY"),
                gameText("TERCİHLER", "PREFERENCES"),
            ),
            selectedIndex = tab,
            onSelected = { tab = it },
            modifier = Modifier.padding(horizontal = GameSpacing.ScreenHorizontal, vertical = 6.dp),
        )

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
        contentPadding = PaddingValues(horizontal = GameSpacing.ScreenHorizontal, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            GameSectionHeader(gameText("Uygulama Tercihleri", "App Preferences"))
            Spacer(Modifier.height(4.dp))
            Text(
                gameText(
                    "Dil, bildirim ve reklam gizliliği seçeneklerini yönet.",
                    "Manage language, notifications and ad privacy choices.",
                ),
                color = GameColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        item {
            GameSurface(borderColor = GameColors.PrimaryBlue.copy(alpha = .26f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Language,
                        contentDescription = null,
                        tint = GameColors.PrimaryBlue,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            gameText("Uygulama dili", "App language"),
                            color = GameColors.TextPrimary,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            gameText(
                                "Arayüz dilini seç. Oyun sözlüğü maç dilinden bağımsız yönetilir.",
                                "Choose the interface language. Match dictionary language is managed separately.",
                            ),
                            color = GameColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = language == "tr",
                        onClick = {
                            language = "tr"
                            SonHarfPreferences.setLanguage(context, "tr")
                        },
                        label = { Text("TÜRKÇE", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = language == "en",
                        onClick = {
                            language = "en"
                            SonHarfPreferences.setLanguage(context, "en")
                        },
                        label = { Text("ENGLISH", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item {
            NotificationToggleCard(
                icon = Icons.Rounded.NotificationsActive,
                title = gameText("Oyun davetleri", "Game invitations"),
                description = gameText(
                    "Arkadaşların seni düelloya çağırdığında uyar.",
                    "Alerts when friends invite you to a duel.",
                ),
                checked = gameInvites,
            ) {
                gameInvites = it
                SonHarfPreferences.setGameInviteNotificationsEnabled(context, it)
            }
        }

        item {
            NotificationToggleCard(
                icon = Icons.Rounded.GroupAdd,
                title = gameText("Arkadaşlık istekleri", "Friend requests"),
                description = gameText(
                    "Yeni arkadaşlık isteği geldiğinde uyar.",
                    "Alerts when a new friend request arrives.",
                ),
                checked = friendRequests,
            ) {
                friendRequests = it
                SonHarfPreferences.setFriendRequestNotificationsEnabled(context, it)
            }
        }

        item {
            NotificationToggleCard(
                icon = Icons.Rounded.Campaign,
                title = gameText("Sistem duyuruları", "System announcements"),
                description = gameText(
                    "Ödül, bakım ve önemli oyun duyuruları.",
                    "Rewards, maintenance and important game announcements.",
                ),
                checked = system,
            ) {
                system = it
                SonHarfPreferences.setSystemNotificationsEnabled(context, it)
            }
        }

        if (privacyOptionsRequired) {
            item {
                GameSurface(borderColor = GameColors.TacticalTurquoise.copy(alpha = .30f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.PrivacyTip,
                            contentDescription = null,
                            tint = GameColors.TacticalTurquoise,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(9.dp))
                        Text(
                            gameText("Reklam gizlilik seçenekleri", "Ad privacy options"),
                            color = GameColors.TextPrimary,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        gameText(
                            "Google reklam gizliliği tercihlerini görüntüle veya değiştir. Bu seçenek yalnız bölgen ve mevcut mesaj ayarları gerektirdiğinde görünür.",
                            "View or change your Google ad privacy choices. This option appears only when required for your region and current message settings.",
                        ),
                        color = GameColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(12.dp))
                    GameSecondaryButton(
                        text = gameText("GİZLİLİK SEÇENEKLERİNİ AÇ", "OPEN PRIVACY OPTIONS"),
                        onClick = {
                            val activity = AdPrivacyManager.findActivity(context)
                            if (activity == null) {
                                privacyNotice = gameText(
                                    "Gizlilik formu açılamadı.",
                                    "Privacy form could not be opened.",
                                )
                            } else {
                                AdPrivacyManager.showPrivacyOptions(activity) { success ->
                                    privacyNotice = if (success) {
                                        gameText(
                                            "Reklam gizliliği tercihleri güncellendi.",
                                            "Ad privacy choices updated.",
                                        )
                                    } else {
                                        gameText(
                                            "Gizlilik formu tamamlanamadı.",
                                            "Privacy form could not be completed.",
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Rounded.PrivacyTip,
                    )
                    privacyNotice?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            it,
                            color = GameColors.TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
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
    GameSurface(
        borderColor = if (checked) {
            GameColors.TacticalTurquoise.copy(alpha = .40f)
        } else {
            GameColors.Border
        },
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = GameShapes.Medium,
                color = if (checked) {
                    GameColors.TacticalTurquoise.copy(alpha = .13f)
                } else {
                    GameColors.ElevatedBackground
                },
                border = BorderStroke(1.dp, GameColors.Divider),
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (checked) GameColors.TacticalTurquoise else GameColors.TextTertiary,
                    modifier = Modifier.padding(9.dp).size(21.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = GameColors.TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    description,
                    color = GameColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}
