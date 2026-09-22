package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.setAvatarVisibility
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

@Composable
internal fun ProfessionalSettingsScreen(
    backend: OnlineGameBackend,
    onBack: () -> Unit,
    onAccount: () -> Unit,
    onSignedOut: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var music by remember { mutableStateOf(SonHarfPreferences.musicEnabled(context)) }
    var sound by remember { mutableStateOf(SonHarfPreferences.soundEnabled(context)) }
    var vibration by remember { mutableStateOf(SonHarfPreferences.vibrationEnabled(context)) }
    var gameInvites by remember { mutableStateOf(SonHarfPreferences.gameInviteNotificationsEnabled(context)) }
    var friendRequests by remember { mutableStateOf(SonHarfPreferences.friendRequestNotificationsEnabled(context)) }
    var systemNotifications by remember { mutableStateOf(SonHarfPreferences.systemNotificationsEnabled(context)) }
    var profileVisible by remember { mutableStateOf(true) }
    var visibilityBusy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var helpDialog by remember { mutableStateOf(false) }
    var logoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val id = backend.currentUserId()
        profile = id?.let { runCatching { backend.getProfile(it) }.getOrNull() }
        profileVisible = profile?.avatarVisibility != "hidden"
    }

    Column(Modifier.fillMaxSize()) {
        GameTopBar(
            title = gameText("Ayarlar", "Settings"),
            subtitle = gameText(
                "Ses, bildirim, gizlilik ve hesap",
                "Audio, notifications, privacy and account",
            ),
            onBack = onBack,
        )

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ProfessionalSettingsGroup(gameText("SES VE DOKUNUŞ", "AUDIO & HAPTICS")) {
                    ProfessionalToggleSetting(
                        icon = Icons.Rounded.MusicNote,
                        title = gameText("Müzik", "Music"),
                        subtitle = "Warm Beginnings",
                        checked = music,
                    ) {
                        music = it
                        SonHarfPreferences.setMusicEnabled(context, it)
                    }
                    SettingsDivider()
                    ProfessionalToggleSetting(
                        icon = Icons.Rounded.VolumeUp,
                        title = gameText("Ses efektleri", "Sound effects"),
                        subtitle = gameText("Butonlar ve oyun geri bildirimleri", "Buttons and game feedback"),
                        checked = sound,
                    ) {
                        sound = it
                        SonHarfPreferences.setSoundEnabled(context, it)
                        if (it) SonHarfSoundFx.tap()
                    }
                    SettingsDivider()
                    ProfessionalToggleSetting(
                        icon = Icons.Rounded.Vibration,
                        title = gameText("Titreşim", "Vibration"),
                        subtitle = gameText("Kısa ve hafif dokunsal geri bildirim", "Short and light haptic feedback"),
                        checked = vibration,
                    ) {
                        vibration = it
                        SonHarfPreferences.setVibrationEnabled(context, it)
                        if (it) SonHarfPreferences.hapticTap(context)
                    }
                }
            }

            item {
                GameSurface(borderColor = GameColors.TacticalTurquoise.copy(alpha = .28f)) {
                    GameSectionHeader(gameText("KİŞİSELLEŞTİRME", "PERSONALIZATION"))
                    Spacer(Modifier.height(4.dp))
                    ProfileOwnedThemesSection(backend)
                }
            }

            item {
                ProfessionalSettingsGroup(gameText("BİLDİRİMLER", "NOTIFICATIONS")) {
                    ProfessionalToggleSetting(
                        Icons.Rounded.SportsEsports,
                        gameText("Oyun davetleri", "Game invitations"),
                        gameText("Arkadaşların düelloya çağırdığında", "When friends invite you to a duel"),
                        gameInvites,
                    ) {
                        gameInvites = it
                        SonHarfPreferences.setGameInviteNotificationsEnabled(context, it)
                    }
                    SettingsDivider()
                    ProfessionalToggleSetting(
                        Icons.Rounded.GroupAdd,
                        gameText("Arkadaşlık istekleri", "Friend requests"),
                        gameText("Yeni arkadaşlık isteği geldiğinde", "When a new friend request arrives"),
                        friendRequests,
                    ) {
                        friendRequests = it
                        SonHarfPreferences.setFriendRequestNotificationsEnabled(context, it)
                    }
                    SettingsDivider()
                    ProfessionalToggleSetting(
                        Icons.Rounded.Notifications,
                        gameText("Sistem duyuruları", "System announcements"),
                        gameText("Ödül, bakım ve önemli haberler", "Rewards, maintenance and important news"),
                        systemNotifications,
                    ) {
                        systemNotifications = it
                        SonHarfPreferences.setSystemNotificationsEnabled(context, it)
                    }
                }
            }

            item {
                ProfessionalSettingsGroup(gameText("PROFİL GÖRÜNÜRLÜĞÜ", "PROFILE VISIBILITY")) {
                    ProfessionalToggleSetting(
                        icon = Icons.Rounded.Visibility,
                        title = gameText("Profil fotoğrafını göster", "Show profile photo"),
                        subtitle = gameText("Arkadaşlar, lig ve maç yüzeylerinde", "On friends, league and match surfaces"),
                        checked = profileVisible,
                        enabled = !visibilityBusy,
                    ) { visible ->
                        if (!visibilityBusy) scope.launch {
                            visibilityBusy = true
                            runCatching { backend.setAvatarVisibility(hidden = !visible) }
                                .onSuccess {
                                    profile = it
                                    profileVisible = it.avatarVisibility != "hidden"
                                    notice = gameText("Profil görünürlüğü güncellendi.", "Profile visibility updated.")
                                }
                                .onFailure {
                                    notice = gameText("Görünürlük güncellenemedi.", "Visibility could not be updated.")
                                }
                            visibilityBusy = false
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        gameText(
                            "Gizli olduğunda fotoğraf yerine adının baş harfi görünür.",
                            "When hidden, your initial appears instead of the photo.",
                        ),
                        color = GameColors.TextTertiary,
                        fontSize = 10.sp,
                    )
                }
            }

            item {
                ProfessionalSettingsGroup(gameText("GİZLİLİK VE DESTEK", "PRIVACY & SUPPORT")) {
                    ProfessionalSettingsLink(
                        Icons.Rounded.PrivacyTip,
                        gameText("Reklam gizlilik seçenekleri", "Ad privacy options"),
                        gameText("Google UMP tercihlerini yönet", "Manage Google UMP choices"),
                    ) {
                        val activity = AdPrivacyManager.findActivity(context)
                        if (activity == null || !AdPrivacyManager.privacyOptionsRequired) {
                            notice = gameText(
                                "Bölgen için ayrıca bir reklam gizlilik formu gerekmiyor.",
                                "No additional ad privacy form is required for your region.",
                            )
                        } else {
                            AdPrivacyManager.showPrivacyOptions(activity) { success ->
                                notice = if (success) {
                                    gameText("Gizlilik tercihleri güncellendi.", "Privacy choices updated.")
                                } else {
                                    gameText("Gizlilik formu açılamadı.", "Privacy form could not be opened.")
                                }
                            }
                        }
                    }
                    SettingsDivider()
                    ProfessionalSettingsLink(
                        Icons.Rounded.Help,
                        gameText("Yardım", "Help"),
                        gameText("Oyun ve hesap yardımı", "Game and account help"),
                    ) { helpDialog = true }
                }
            }

            item {
                ProfessionalSettingsGroup(gameText("HESAP", "ACCOUNT")) {
                    val email = runCatching { SupabaseProvider.client.auth.currentUserOrNull()?.email }
                        .getOrNull()
                        .orEmpty()
                    if (email.isNotBlank()) {
                        Text(
                            email,
                            color = GameColors.TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    ProfessionalSettingsLink(
                        Icons.Rounded.ManageAccounts,
                        gameText("Hesap ve gizlilik", "Account & privacy"),
                        gameText("Engellenenler ve hesap silme", "Blocked users and account deletion"),
                        onAccount,
                    )
                    SettingsDivider()
                    ProfessionalSettingsLink(
                        Icons.Rounded.Logout,
                        gameText("Çıkış yap", "Sign out"),
                        gameText("Bu cihazdaki oturumu kapat", "End the session on this device"),
                        danger = true,
                    ) { logoutDialog = true }
                }
            }

            notice?.let { message ->
                item {
                    Surface(
                        shape = GameShapes.Medium,
                        color = GameColors.PrimaryBlue.copy(alpha = .12f),
                        border = BorderStroke(1.dp, GameColors.PrimaryBlue.copy(alpha = .25f)),
                    ) {
                        Text(
                            message,
                            Modifier.fillMaxWidth().padding(12.dp),
                            color = GameColors.TextPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            item {
                Text(
                    "Kelime Kuşatması ${BuildConfig.VERSION_NAME} • Android",
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    color = GameColors.TextTertiary,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    if (helpDialog) {
        AlertDialog(
            onDismissRequest = { helpDialog = false },
            containerColor = GameColors.PrimarySurface,
            titleContentColor = GameColors.TextPrimary,
            textContentColor = GameColors.TextSecondary,
            title = { Text(gameText("Yardım", "Help"), fontWeight = FontWeight.Black) },
            text = {
                Text(
                    gameText(
                        "Bağlantı veya ödeme sorunu yaşarsan önce internet bağlantını ve Google Play hesabını kontrol et. Satın almalar sunucuda doğrulanır ve aynı işlem ikinci kez ödül vermez. Hesap ve gizlilik bölümünden profil verilerini yönetebilirsin.",
                        "For connection or payment issues, first check your internet connection and Google Play account. Purchases are verified on the server and the same transaction cannot grant twice. Manage profile data from Account & privacy.",
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = { helpDialog = false }) {
                    Text(gameText("TAMAM", "OK"), color = GameColors.PrimaryBlue)
                }
            },
        )
    }

    if (logoutDialog) {
        AlertDialog(
            onDismissRequest = { logoutDialog = false },
            containerColor = GameColors.PrimarySurface,
            titleContentColor = GameColors.TextPrimary,
            textContentColor = GameColors.TextSecondary,
            title = { Text(gameText("Çıkış yapılsın mı?", "Sign out?"), fontWeight = FontWeight.Black) },
            text = {
                Text(gameText(
                    "Bu cihazdaki Kelime Kuşatması oturumu kapatılacak.",
                    "Your Word Siege session on this device will end.",
                ))
            },
            dismissButton = {
                TextButton(onClick = { logoutDialog = false }) {
                    Text(gameText("VAZGEÇ", "CANCEL"), color = GameColors.TextSecondary)
                }
            },
            confirmButton = {
                GameDangerButton(
                    text = gameText("ÇIKIŞ YAP", "SIGN OUT"),
                    onClick = {
                        scope.launch {
                            runCatching { backend.setPresence("offline") }
                            runCatching { SupabaseProvider.client.auth.signOut() }
                            RememberedCredentialVault.clear(context)
                            SonHarfPreferences.setRememberLogin(context, false)
                            logoutDialog = false
                            onSignedOut()
                        }
                    },
                )
            },
        )
    }
}

@Composable
private fun ProfessionalSettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    GameSurface {
        Text(
            title,
            color = GameColors.TextTertiary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = .5.sp,
        )
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun ProfessionalToggleSetting(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = CircleShape, color = GameColors.SecondarySurface) {
            Icon(icon, null, tint = GameColors.PrimaryBlue, modifier = Modifier.padding(9.dp).size(21.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = GameColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, color = GameColors.TextSecondary, fontSize = 10.sp, maxLines = 2)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = GameColors.TextPrimary,
                checkedTrackColor = GameColors.PlayGreen,
                uncheckedThumbColor = GameColors.TextSecondary,
                uncheckedTrackColor = GameColors.SecondarySurface,
                uncheckedBorderColor = GameColors.Border,
            ),
        )
    }
}

@Composable
private fun ProfessionalSettingsLink(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) = ProfessionalSettingsLink(icon, title, subtitle, false, onClick)

@Composable
private fun ProfessionalSettingsLink(
    icon: ImageVector,
    title: String,
    subtitle: String,
    danger: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = GameShapes.Small,
        color = androidx.compose.ui.graphics.Color.Transparent,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val accent = if (danger) GameColors.Danger else GameColors.PrimaryBlue
            Surface(shape = CircleShape, color = accent.copy(alpha = .12f)) {
                Icon(icon, null, tint = accent, modifier = Modifier.padding(9.dp).size(21.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = if (danger) GameColors.Danger else GameColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(subtitle, color = GameColors.TextSecondary, fontSize = 10.sp, maxLines = 2)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = GameColors.TextTertiary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(color = GameColors.Divider)
}
