package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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

    Box(Modifier.fillMaxSize()) {
        SonHarfLeafBackdrop(Modifier.matchParentSize())
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                MainScreenHeader(
                    title = sh("Oyuncu Profili", "Player Profile"),
                    subtitle = sh("Kimlik, gizlilik ve kişisel tercihler", "Identity, privacy and personal preferences"),
                    onBack = onBack,
                )
            }

            ScrollableTabRow(
                selectedTabIndex = tab,
                edgePadding = 12.dp,
                containerColor = SonHarfTheme.Surface.copy(alpha = .92f),
                contentColor = SonHarfTheme.Primary,
                divider = { HorizontalDivider(color = SonHarfTheme.Border) },
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
                                color = if (tab == index) SonHarfTheme.Primary else SonHarfTheme.TextSecondary,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            PremiumCard(modifier = Modifier.fillMaxWidth(), accent = SonHarfTheme.Purple) {
                Text(
                    sh("UYGULAMA TERCİHLERİ", "APP PREFERENCES"),
                    color = SonHarfTheme.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    sh("Dil ve bildirim ayarlarını buradan yönet.", "Manage language and notification settings here."),
                    color = SonHarfTheme.TextSecondary,
                    fontSize = 11.sp,
                )
            }
        }

        item {
            PremiumCard(modifier = Modifier.fillMaxWidth(), accent = SonHarfTheme.Primary) {
                Text(
                    sh("Uygulama dili", "App language"),
                    color = SonHarfTheme.TextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PremiumLanguageChip(
                        selected = language == "tr",
                        label = "🇹🇷 TÜRKÇE",
                        accent = SonHarfTheme.Primary,
                        modifier = Modifier.weight(1f),
                    ) {
                        language = "tr"
                        SonHarfPreferences.setLanguage(context, "tr")
                    }
                    PremiumLanguageChip(
                        selected = language == "en",
                        label = "🇬🇧 ENGLISH",
                        accent = SonHarfTheme.Purple,
                        modifier = Modifier.weight(1f),
                    ) {
                        language = "en"
                        SonHarfPreferences.setLanguage(context, "en")
                    }
                }
            }
        }

        item {
            NotificationToggleCard(
                icon = "⚔",
                title = sh("Oyun davetleri", "Game invitations"),
                description = sh("Arkadaşların seni düelloya çağırdığında uyar.", "Alerts when friends invite you to a duel."),
                checked = gameInvites,
                accent = SonHarfTheme.Primary,
            ) {
                gameInvites = it
                SonHarfPreferences.setGameInviteNotificationsEnabled(context, it)
            }
        }
        item {
            NotificationToggleCard(
                icon = "👥",
                title = sh("Arkadaşlık istekleri", "Friend requests"),
                description = sh("Yeni arkadaşlık isteği geldiğinde uyar.", "Alerts when a new friend request arrives."),
                checked = friendRequests,
                accent = SonHarfTheme.Turquoise,
            ) {
                friendRequests = it
                SonHarfPreferences.setFriendRequestNotificationsEnabled(context, it)
            }
        }
        item {
            NotificationToggleCard(
                icon = "✦",
                title = sh("Sistem duyuruları", "System announcements"),
                description = sh("Ödül, bakım ve önemli oyun duyuruları.", "Rewards, maintenance and important game announcements."),
                checked = system,
                accent = SonHarfTheme.ActionOrange,
            ) {
                system = it
                SonHarfPreferences.setSystemNotificationsEnabled(context, it)
            }
        }

        if (privacyOptionsRequired) {
            item {
                PremiumCard(modifier = Modifier.fillMaxWidth(), accent = SonHarfTheme.Purple) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SonHarfTheme.Purple.copy(alpha = .10f),
                        ) {
                            androidx.compose.material3.Icon(
                                Icons.Rounded.PrivacyTip,
                                contentDescription = null,
                                tint = SonHarfTheme.Purple,
                                modifier = Modifier.padding(10.dp).size(22.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                sh("Reklam gizlilik seçenekleri", "Ad privacy options"),
                                color = SonHarfTheme.TextPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                            )
                            Text(
                                sh(
                                    "Google reklam gizliliği tercihlerini görüntüle veya değiştir.",
                                    "View or change your Google ad privacy choices.",
                                ),
                                color = SonHarfTheme.TextSecondary,
                                fontSize = 10.sp,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    PremiumPrimaryButton(
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
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = SonHarfTheme.TextSecondary, fontSize = 10.sp)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun PremiumLanguageChip(
    selected: Boolean,
    label: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontWeight = FontWeight.Black, fontSize = 10.sp) },
        modifier = modifier.height(48.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = SonHarfTheme.SurfaceSecondary,
            labelColor = SonHarfTheme.TextPrimary,
            selectedContainerColor = accent.copy(alpha = .12f),
            selectedLabelColor = accent,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = SonHarfTheme.Border,
            selectedBorderColor = accent.copy(alpha = .55f),
        ),
    )
}

@Composable
private fun NotificationToggleCard(
    icon: String,
    title: String,
    description: String,
    checked: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    onChange: (Boolean) -> Unit,
) {
    Surface(
        shape = MainUiShape.Card,
        color = SonHarfTheme.Surface,
        border = BorderStroke(1.dp, if (checked) accent.copy(alpha = .34f) else SonHarfTheme.Border),
        shadowElevation = 2.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(15.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = accent.copy(alpha = .10f),
            ) {
                Box(Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                    Text(icon, fontSize = 22.sp)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = SonHarfTheme.TextPrimary, fontWeight = FontWeight.Black, fontSize = 13.sp)
                Spacer(Modifier.height(2.dp))
                Text(description, color = SonHarfTheme.TextSecondary, fontSize = 10.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                    checkedTrackColor = accent,
                    uncheckedThumbColor = SonHarfTheme.TextSecondary,
                    uncheckedTrackColor = SonHarfTheme.SurfaceSecondary,
                    uncheckedBorderColor = SonHarfTheme.Border,
                ),
            )
        }
    }
}
