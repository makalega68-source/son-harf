package com.sonharf.game

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.setAvatarVisibility
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

@Composable
internal fun MainSettingsScreen(
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            MainScreenHeader(
                title = sh("Ayarlar", "Settings"),
                subtitle = sh("Ses, dil, bildirim, gizlilik ve hesap", "Audio, language, notifications, privacy and account"),
                onBack = onBack,
            )
        }

        item {
            MainSettingsGroup(sh("SES VE DOKUNUŞ", "AUDIO & HAPTICS")) {
                MainToggleSetting(
                    title = sh("Müzik", "Music"),
                    subtitle = "Warm Beginnings",
                    checked = music,
                ) {
                    music = it
                    SonHarfPreferences.setMusicEnabled(context, it)
                }
                MainToggleSetting(
                    title = sh("Ses efektleri", "Sound effects"),
                    subtitle = sh("Butonlar ve oyun geri bildirimleri", "Buttons and game feedback"),
                    checked = sound,
                ) {
                    sound = it
                    SonHarfPreferences.setSoundEnabled(context, it)
                    if (it) SonHarfSoundFx.tap()
                }
                MainToggleSetting(
                    title = sh("Titreşim", "Vibration"),
                    subtitle = sh("Kısa ve hafif dokunsal geri bildirim", "Short and light haptic feedback"),
                    checked = vibration,
                ) {
                    vibration = it
                    SonHarfPreferences.setVibrationEnabled(context, it)
                    if (it) SonHarfPreferences.hapticTap(context)
                }
            }
        }

        item { ProfileOwnedThemesSection(backend) }

        item {
            MainSettingsGroup(sh("BİLDİRİMLER", "NOTIFICATIONS")) {
                MainToggleSetting(sh("Oyun davetleri", "Game invitations"), sh("Arkadaşların düelloya çağırdığında", "When friends invite you to a duel"), gameInvites) {
                    gameInvites = it
                    SonHarfPreferences.setGameInviteNotificationsEnabled(context, it)
                }
                MainToggleSetting(sh("Arkadaşlık istekleri", "Friend requests"), sh("Yeni arkadaşlık isteği geldiğinde", "When a new friend request arrives"), friendRequests) {
                    friendRequests = it
                    SonHarfPreferences.setFriendRequestNotificationsEnabled(context, it)
                }
                MainToggleSetting(sh("Sistem duyuruları", "System announcements"), sh("Ödül, bakım ve önemli haberler", "Rewards, maintenance and important news"), systemNotifications) {
                    systemNotifications = it
                    SonHarfPreferences.setSystemNotificationsEnabled(context, it)
                }
            }
        }

        item {
            MainSettingsGroup(sh("PROFİL GÖRÜNÜRLÜĞÜ", "PROFILE VISIBILITY")) {
                MainToggleSetting(
                    title = sh("Profil fotoğrafını göster", "Show profile photo"),
                    subtitle = sh("Arkadaşlar, lig ve maç yüzeylerinde", "On friends, league and match surfaces"),
                    checked = profileVisible,
                    enabled = !visibilityBusy,
                ) { visible ->
                    if (!visibilityBusy) {
                        scope.launch {
                            visibilityBusy = true
                            runCatching { backend.setAvatarVisibility(hidden = !visible) }
                                .onSuccess {
                                    profile = it
                                    profileVisible = it.avatarVisibility != "hidden"
                                    notice = sh("Profil görünürlüğü güncellendi.", "Profile visibility updated.")
                                }
                                .onFailure { notice = sh("Görünürlük güncellenemedi.", "Visibility could not be updated.") }
                            visibilityBusy = false
                        }
                    }
                }
                Text(
                    sh("Gizli olduğunda fotoğraf yerine adının baş harfi görünür.", "When hidden, your initial appears instead of the photo."),
                    color = MainUi.Muted,
                    fontSize = 9.sp,
                )
            }
        }

        item {
            MainSettingsGroup(sh("GİZLİLİK VE DESTEK", "PRIVACY & SUPPORT")) {
                MainSettingsLink(
                    asset = PurchasedUiAsset.ICON_SETTINGS,
                    title = sh("Reklam gizlilik seçenekleri", "Ad privacy options"),
                    subtitle = sh("Google UMP tercihlerini yönet", "Manage Google UMP choices"),
                ) {
                    val activity = AdPrivacyManager.findActivity(context)
                    if (activity == null || !AdPrivacyManager.privacyOptionsRequired) {
                        notice = sh("Bölgen için ayrıca bir reklam gizlilik formu gerekmiyor.", "No additional ad privacy form is required for your region.")
                    } else {
                        AdPrivacyManager.showPrivacyOptions(activity) { success ->
                            notice = if (success) sh("Gizlilik tercihleri güncellendi.", "Privacy choices updated.")
                            else sh("Gizlilik formu açılamadı.", "Privacy form could not be opened.")
                        }
                    }
                }
                MainSettingsLink(
                    asset = PurchasedUiAsset.ICON_CHAT,
                    title = sh("Yardım", "Help"),
                    subtitle = sh("Oyun ve hesap yardımı", "Game and account help"),
                ) { helpDialog = true }
            }
        }

        item {
            MainSettingsGroup(sh("HESAP", "ACCOUNT")) {
                val email = runCatching { com.sonharf.game.data.SupabaseProvider.client.auth.currentUserOrNull()?.email }.getOrNull().orEmpty()
                if (email.isNotBlank()) {
                    Text(email, color = MainUi.Text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(3.dp))
                }
                MainSettingsLink(
                    asset = PurchasedUiAsset.NAV_PROFILE,
                    title = sh("Hesap ve gizlilik", "Account & privacy"),
                    subtitle = sh("Engellenenler ve hesap silme", "Blocked users and account deletion"),
                    onClick = onAccount,
                )
                MainSettingsLink(
                    asset = PurchasedUiAsset.ICON_CLOSE,
                    title = sh("Çıkış yap", "Sign out"),
                    subtitle = sh("Bu cihazdaki oturumu kapat", "End the session on this device"),
                ) { logoutDialog = true }
            }
        }

        notice?.let { message ->
            item {
                PurchasedPanel(
                    modifier = Modifier.fillMaxWidth(),
                    asset = PurchasedUiAsset.REWARD_PANEL,
                    contentPadding = PaddingValues(13.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        PurchasedAsset(PurchasedUiAsset.ICON_CHECK, Modifier.size(32.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(message, Modifier.weight(1f), color = MainUi.Text, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        item {
            Text(
                "Kelime Kuşatması ${BuildConfig.VERSION_NAME} • Android",
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                color = MainUi.Muted,
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
            )
        }
    }

    if (helpDialog) {
        PurchasedMessageDialog(
            title = sh("Yardım", "Help"),
            message = sh(
                "Bağlantı veya ödeme sorunu yaşarsan önce internet bağlantını ve Google Play hesabını kontrol et. Satın almalar sunucuda doğrulanır ve aynı işlem ikinci kez ödül vermez. Hesap ve gizlilik bölümünden profil verilerini yönetebilirsin.",
                "For connection or payment issues, first check your internet connection and Google Play account. Purchases are verified on the server and the same transaction cannot grant twice. Manage profile data from Account & privacy.",
            ),
            confirmText = sh("TAMAM", "OK"),
            onConfirm = { helpDialog = false },
            onDismiss = { helpDialog = false },
        )
    }

    if (logoutDialog) {
        Dialog(onDismissRequest = { logoutDialog = false }) {
            PurchasedPanel(
                modifier = Modifier.fillMaxWidth(),
                asset = PurchasedUiAsset.PANEL_MEDIUM,
                contentPadding = PaddingValues(20.dp),
            ) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PurchasedAsset(PurchasedUiAsset.ICON_CLOSE, Modifier.size(48.dp))
                    Text(sh("Çıkış yapılsın mı?", "Sign out?"), color = MainUi.Text, fontSize = 18.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Text(sh("Bu cihazdaki Kelime Kuşatması oturumu kapatılacak.", "Your Kelime Kuşatması session on this device will end."), color = MainUi.Muted, fontSize = 10.sp, textAlign = TextAlign.Center)
                    PurchasedButton(
                        text = sh("ÇIKIŞ YAP", "SIGN OUT"),
                        onClick = {
                            scope.launch {
                                runCatching { backend.setPresence("offline") }
                                runCatching { com.sonharf.game.data.SupabaseProvider.client.auth.signOut() }
                                SonHarfPreferences.setRememberLogin(context, false)
                                logoutDialog = false
                                onSignedOut()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        style = PurchasedButtonStyle.DANGER,
                    )
                    PurchasedButton(
                        text = sh("VAZGEÇ", "CANCEL"),
                        onClick = { logoutDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        style = PurchasedButtonStyle.SECONDARY,
                    )
                }
            }
        }
    }
}

@Composable
internal fun MainVipScreen(
    backend: OnlineGameBackend,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var showPurchase by remember { mutableStateOf(false) }

    suspend fun reload() {
        loading = true
        val id = backend.currentUserId()
        profile = id?.let { runCatching { backend.getProfile(it) }.getOrNull() }
        loading = false
    }

    LaunchedEffect(Unit) { reload() }
    val active = profile?.isVip == true

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            MainScreenHeader(
                title = "Kelime Kuşatması VIP",
                subtitle = sh("Reklamsız deneyim, PRO hakları ve kişiselleştirme", "Ad-free experience, PRO benefits and personalization"),
                onBack = onBack,
            )
        }

        if (loading) {
            item {
                PurchasedPanel(modifier = Modifier.fillMaxWidth(), asset = PurchasedUiAsset.PANEL_SMALL) {
                    Text(sh("VIP durumu kontrol ediliyor…", "Checking VIP status…"), color = MainUi.Text, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Box(Modifier.fillMaxWidth().heightIn(min = 190.dp), contentAlignment = Alignment.Center) {
                PurchasedAsset(PurchasedUiAsset.SEASON_BANNER, Modifier.matchParentSize())
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    PurchasedAsset(PurchasedUiAsset.ICON_CROWN, Modifier.size(58.dp))
                    Text("VIP", color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (active) sh("Üyeliğin aktif", "Your membership is active") else sh("Kelime Kuşatması deneyimini kişiselleştir", "Personalize your Kelime Kuşatması experience"),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        if (active) profile?.displayName.orEmpty() else sh("Google Play ile güvenli üyelik", "Secure membership through Google Play"),
                        color = Color.White.copy(alpha = .82f),
                        fontSize = 10.sp,
                    )
                }
            }
        }

        item { MainSectionTitle(sh("VIP AYRICALIKLARI", "VIP BENEFITS")) }

        item {
            PurchasedPanel(
                modifier = Modifier.fillMaxWidth(),
                asset = PurchasedUiAsset.PANEL_LARGE,
                contentPadding = PaddingValues(16.dp),
            ) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    MainVipBenefit(PurchasedUiAsset.ICON_CLOSE, sh("Reklamsız deneyim", "Ad-free experience"), sh("Maç dışında da sade ve kesintisiz", "Clean and uninterrupted outside matches"))
                    MainVipBenefit(PurchasedUiAsset.ICON_CROWN, sh("VIP profil rozeti", "VIP profile badge"), sh("Profil ve sosyal alanlarda görünür", "Visible on profile and social surfaces"))
                    MainVipBenefit(PurchasedUiAsset.NAV_PROFILE, sh("Özel Style içerikleri", "Exclusive Style content"), sh("Profil çerçevesi ve kişiselleştirme", "Profile frames and personalization"))
                    MainVipBenefit(PurchasedUiAsset.ICON_REPEAT, sh("Kelime geçmişi", "Word history"), sh("Düelloda son kelimeleri gör", "See recent words during a duel"))
                    MainVipBenefit(PurchasedUiAsset.ICON_RANKING, sh("Gelişmiş istatistikler", "Advanced statistics"), sh("Performansını daha ayrıntılı incele", "Review performance in more detail"))
                    MainVipBenefit(PurchasedUiAsset.ICON_GAMES, sh("Özel oda oluşturma", "Create private rooms"), sh("Arkadaşlarınla kodlu oda aç", "Open coded rooms with friends"))
                    MainVipBenefit(PurchasedUiAsset.NAV_SHOP, sh("VIP Style görünümü", "VIP Style view"), sh("Üyelere özel ürünleri keşfet", "Discover member-only items"))
                }
            }
        }

        item {
            PurchasedPanel(
                modifier = Modifier.fillMaxWidth(),
                asset = PurchasedUiAsset.PANEL_SMALL,
                contentPadding = PaddingValues(15.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    PurchasedAsset(PurchasedUiAsset.ICON_GAMES, Modifier.size(46.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(sh("PRO OYUN YARDIMLARI", "PRO GAME HELPERS"), color = MainUi.Green, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Text(
                            sh(
                                "PRO; İpucu, Harf Değiştirici ve 2x Skor dahil sunucu doğrulamalı oyun yardımcıları sunar.",
                                "PRO includes server-validated gameplay helpers such as Hint, Letter Swap and 2x Score.",
                            ),
                            color = MainUi.Text,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        item {
            if (active) {
                PurchasedButton(
                    text = sh("GOOGLE PLAY'DE YÖNET", "MANAGE ON GOOGLE PLAY"),
                    onClick = {
                        val url = "https://play.google.com/store/account/subscriptions?package=${BuildConfig.APPLICATION_ID}"
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    style = PurchasedButtonStyle.SECONDARY,
                    leadingAsset = PurchasedUiAsset.ICON_SETTINGS,
                )
            } else {
                PurchasedButton(
                    text = sh("VIP PLANLARINI GÖR", "VIEW VIP PLANS"),
                    onClick = { showPurchase = true },
                    modifier = Modifier.fillMaxWidth(),
                    style = PurchasedButtonStyle.PRIMARY,
                    leadingAsset = PurchasedUiAsset.ICON_CROWN,
                )
            }
        }

        item {
            Text(
                sh("Ödeme ve iptal Google Play hesabın üzerinden yönetilir.", "Payment and cancellation are managed through your Google Play account."),
                Modifier.fillMaxWidth(),
                color = MainUi.Muted,
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
            )
        }
        item { Spacer(Modifier.height(8.dp)) }
    }

    if (showPurchase) {
        VipPurchaseDialog(
            onVerified = { scope.launch { reload() } },
            onDismiss = { showPurchase = false },
        )
    }
}

@Composable
private fun MainSettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    PurchasedPanel(
        modifier = Modifier.fillMaxWidth(),
        asset = PurchasedUiAsset.OLD_SETTINGS_PANEL,
        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 16.dp),
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PurchasedSectionHeader(title = title)
            content()
        }
    }
}

@Composable
private fun MainToggleSetting(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled) { onChange(!checked) }.padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PurchasedAsset(PurchasedUiAsset.ICON_SETTINGS, Modifier.size(38.dp), alpha = if (enabled) 1f else .45f)
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = MainUi.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MainUi.Muted, fontSize = 8.5.sp)
        }
        PurchasedAsset(
            if (checked) PurchasedUiAsset.TOGGLE_ON else PurchasedUiAsset.TOGGLE_OFF,
            Modifier.size(width = 68.dp, height = 28.dp),
            alpha = if (enabled) 1f else .45f,
        )
    }
}

@Composable
private fun MainSettingsLink(
    asset: PurchasedUiAsset,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PurchasedAsset(asset, Modifier.size(38.dp))
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = MainUi.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MainUi.Muted, fontSize = 8.5.sp)
        }
        PurchasedAsset(PurchasedUiAsset.BUTTON_BLUE, Modifier.size(24.dp), alpha = .65f)
    }
}

@Composable
private fun MainVipBenefit(asset: PurchasedUiAsset, title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        PurchasedAsset(asset, Modifier.size(38.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = MainUi.Text, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = MainUi.Muted, fontSize = 9.sp)
        }
        PurchasedAsset(PurchasedUiAsset.ICON_CHECK, Modifier.size(28.dp))
    }
}

@Composable
private fun PurchasedMessageDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        PurchasedPanel(
            modifier = Modifier.fillMaxWidth(),
            asset = PurchasedUiAsset.PANEL_MEDIUM,
            contentPadding = PaddingValues(20.dp),
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PurchasedAsset(PurchasedUiAsset.ICON_CHAT, Modifier.size(48.dp))
                Text(title, color = MainUi.Text, fontSize = 18.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Text(message, color = MainUi.Muted, fontSize = 10.sp, textAlign = TextAlign.Center)
                PurchasedButton(
                    text = confirmText,
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    style = PurchasedButtonStyle.PRIMARY,
                )
            }
        }
    }
}
