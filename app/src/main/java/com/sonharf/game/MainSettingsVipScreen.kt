package com.sonharf.game

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var language by remember { mutableStateOf(SonHarfPreferences.language(context)) }
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
        Modifier.fillMaxSize(),
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
                    icon = Icons.Rounded.MusicNote,
                    title = sh("Müzik", "Music"),
                    subtitle = "Warm Beginnings",
                    checked = music,
                ) {
                    music = it
                    SonHarfPreferences.setMusicEnabled(context, it)
                }
                HorizontalDivider(color = MainUi.Border)
                MainToggleSetting(
                    icon = Icons.Rounded.VolumeUp,
                    title = sh("Ses efektleri", "Sound effects"),
                    subtitle = sh("Butonlar ve oyun geri bildirimleri", "Buttons and game feedback"),
                    checked = sound,
                ) {
                    sound = it
                    SonHarfPreferences.setSoundEnabled(context, it)
                    if (it) SonHarfSoundFx.tap()
                }
                HorizontalDivider(color = MainUi.Border)
                MainToggleSetting(
                    icon = Icons.Rounded.Vibration,
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

        item {
            MainSettingsGroup(sh("DİL", "LANGUAGE")) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = language == "tr",
                        onClick = { language = "tr"; SonHarfPreferences.setLanguage(context, "tr") },
                        label = { Text("🇹🇷 TÜRKÇE", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = language == "en",
                        onClick = { language = "en"; SonHarfPreferences.setLanguage(context, "en") },
                        label = { Text("🇬🇧 ENGLISH", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    sh("Dil değişikliği açık ekranlarda hemen uygulanır.", "Language changes apply immediately to open screens."),
                    color = MainUi.Muted,
                    fontSize = 13.sp,
                )
            }
        }

        item {
            MainSettingsGroup(sh("BİLDİRİMLER", "NOTIFICATIONS")) {
                MainToggleSetting(Icons.Rounded.SportsEsports, sh("Oyun davetleri", "Game invitations"), sh("Arkadaşların düelloya çağırdığında", "When friends invite you to a duel"), gameInvites) {
                    gameInvites = it
                    SonHarfPreferences.setGameInviteNotificationsEnabled(context, it)
                }
                HorizontalDivider(color = MainUi.Border)
                MainToggleSetting(Icons.Rounded.GroupAdd, sh("Arkadaşlık istekleri", "Friend requests"), sh("Yeni arkadaşlık isteği geldiğinde", "When a new friend request arrives"), friendRequests) {
                    friendRequests = it
                    SonHarfPreferences.setFriendRequestNotificationsEnabled(context, it)
                }
                HorizontalDivider(color = MainUi.Border)
                MainToggleSetting(Icons.Rounded.Notifications, sh("Sistem duyuruları", "System announcements"), sh("Ödül, bakım ve önemli haberler", "Rewards, maintenance and important news"), systemNotifications) {
                    systemNotifications = it
                    SonHarfPreferences.setSystemNotificationsEnabled(context, it)
                }
            }
        }

        item {
            MainSettingsGroup(sh("PROFİL GÖRÜNÜRLÜĞÜ", "PROFILE VISIBILITY")) {
                MainToggleSetting(
                    Icons.Rounded.Visibility,
                    sh("Profil fotoğrafını göster", "Show profile photo"),
                    sh("Arkadaşlar, lig ve maç yüzeylerinde", "On friends, league and match surfaces"),
                    profileVisible,
                    enabled = !visibilityBusy,
                ) { visible ->
                    if (visibilityBusy) return@MainToggleSetting
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
                Text(
                    sh("Gizli olduğunda fotoğraf yerine adının baş harfi görünür.", "When hidden, your initial appears instead of the photo."),
                    color = MainUi.Muted,
                    fontSize = 13.sp,
                )
            }
        }

        item {
            MainSettingsGroup(sh("GİZLİLİK VE DESTEK", "PRIVACY & SUPPORT")) {
                MainSettingsLink(Icons.Rounded.PrivacyTip, sh("Reklam gizlilik seçenekleri", "Ad privacy options"), sh("Google UMP tercihlerini yönet", "Manage Google UMP choices")) {
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
                HorizontalDivider(color = MainUi.Border)
                MainSettingsLink(Icons.Rounded.Help, sh("Yardım", "Help"), sh("Oyun ve hesap yardımı", "Game and account help")) { helpDialog = true }
            }
        }

        item {
            MainSettingsGroup(sh("HESAP", "ACCOUNT")) {
                val email = runCatching { com.sonharf.game.data.SupabaseProvider.client.auth.currentUserOrNull()?.email }.getOrNull().orEmpty()
                if (email.isNotBlank()) {
                    Text(email, color = MainUi.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(3.dp))
                }
                MainSettingsLink(Icons.Rounded.ManageAccounts, sh("Hesap ve gizlilik", "Account & privacy"), sh("Engellenenler ve hesap silme", "Blocked users and account deletion"), onAccount)
                HorizontalDivider(color = MainUi.Border)
                MainSettingsLink(Icons.Rounded.Logout, sh("Çıkış yap", "Sign out"), sh("Bu cihazdaki oturumu kapat", "End the session on this device")) { logoutDialog = true }
            }
        }

        notice?.let { message ->
            item {
                Surface(shape = RoundedCornerShape(14.dp), color = MainUi.BlueSoft) {
                    Text(message, Modifier.fillMaxWidth().padding(11.dp), color = MainUi.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
        }

        item {
            Text(
                "Son Harf ${BuildConfig.VERSION_NAME} • Android",
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                color = MainUi.Muted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
        }
    }

    if (helpDialog) {
        AlertDialog(
            onDismissRequest = { helpDialog = false },
            title = { Text(sh("Yardım", "Help"), fontWeight = FontWeight.Black) },
            text = {
                Text(
                    sh(
                        "Bağlantı veya ödeme sorunu yaşarsan önce internet bağlantını ve Google Play hesabını kontrol et. Satın almalar sunucuda doğrulanır ve aynı işlem ikinci kez ödül vermez. Hesap ve gizlilik bölümünden profil verilerini yönetebilirsin.",
                        "For connection or payment issues, first check your internet connection and Google Play account. Purchases are verified on the server and the same transaction cannot grant twice. Manage profile data from Account & privacy.",
                    ),
                    color = MainUi.Muted,
                )
            },
            confirmButton = { TextButton(onClick = { helpDialog = false }) { Text(sh("TAMAM", "OK")) } },
        )
    }

    if (logoutDialog) {
        AlertDialog(
            onDismissRequest = { logoutDialog = false },
            title = { Text(sh("Çıkış yapılsın mı?", "Sign out?"), fontWeight = FontWeight.Black) },
            text = { Text(sh("Bu cihazdaki Son Harf oturumu kapatılacak.", "Your Son Harf session on this device will end.")) },
            dismissButton = { TextButton(onClick = { logoutDialog = false }) { Text(sh("VAZGEÇ", "CANCEL")) } },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            runCatching { backend.setPresence("offline") }
                            runCatching { com.sonharf.game.data.SupabaseProvider.client.auth.signOut() }
                            SonHarfPreferences.setRememberLogin(context, false)
                            logoutDialog = false
                            onSignedOut()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MainUi.Red),
                ) { Text(sh("ÇIKIŞ YAP", "SIGN OUT"), fontWeight = FontWeight.Black) }
            },
        )
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
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            MainScreenHeader(
                title = "Son Harf VIP",
                subtitle = sh("Daha temiz, daha kişisel; rekabet daima adil", "Cleaner and more personal; competition stays fair"),
                onBack = onBack,
            )
        }

        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = MainUi.Blue, trackColor = MainUi.BlueSoft) }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MainUi.Blue,
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Surface(shape = CircleShape, color = Color.White.copy(alpha = .14f)) {
                        Icon(Icons.Rounded.WorkspacePremium, null, tint = Color.White, modifier = Modifier.padding(13.dp).size(34.dp))
                    }
                    Text("VIP", color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (active) sh("Üyeliğin aktif", "Your membership is active") else sh("Son Harf deneyimini kişiselleştir", "Personalize your Son Harf experience"),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (active) profile?.displayName.orEmpty() else sh("Google Play ile güvenli üyelik", "Secure membership through Google Play"),
                        color = Color.White.copy(alpha = .75f),
                        fontSize = 13.sp,
                    )
                }
            }
        }

        item { MainSectionTitle(sh("VIP AYRICALIKLARI", "VIP BENEFITS")) }

        item {
            Surface(shape = RoundedCornerShape(20.dp), color = MainUi.Surface, border = BorderStroke(1.dp, MainUi.Border)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    MainVipBenefit(Icons.Rounded.Block, sh("Reklamsız deneyim", "Ad-free experience"), sh("Maç dışında da sade ve kesintisiz", "Clean and uninterrupted outside matches"))
                    MainVipBenefit(Icons.Rounded.Verified, sh("VIP profil rozeti", "VIP profile badge"), sh("Profil ve sosyal alanlarda görünür", "Visible on profile and social surfaces"))
                    MainVipBenefit(Icons.Rounded.Checkroom, sh("Özel Style içerikleri", "Exclusive Style content"), sh("Profil çerçevesi ve kişiselleştirme", "Profile frames and personalization"))
                    MainVipBenefit(Icons.Rounded.History, sh("Kelime geçmişi", "Word history"), sh("Düelloda son kelimeleri gör", "See recent words during a duel"))
                    MainVipBenefit(Icons.Rounded.Insights, sh("Gelişmiş istatistikler", "Advanced statistics"), sh("Performansını daha ayrıntılı incele", "Review performance in more detail"))
                    MainVipBenefit(Icons.Rounded.Lock, sh("Özel oda oluşturma", "Create private rooms"), sh("Arkadaşlarınla kodlu oda aç", "Open coded rooms with friends"))
                    MainVipBenefit(Icons.Rounded.Storefront, sh("VIP Style görünümü", "VIP Style view"), sh("Üyelere özel ürünleri keşfet", "Discover member-only items"))
                }
            }
        }

        item {
            Surface(shape = RoundedCornerShape(20.dp), color = MainUi.Green.copy(alpha = .08f), border = BorderStroke(1.dp, MainUi.Green.copy(alpha = .30f))) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(sh("REKABET ADALETİ", "COMPETITIVE FAIRNESS"), color = MainUi.Green, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    Text(
                        sh(
                            "VIP; ekstra süre, ek puan, güçlü joker, rating koruması veya kazanma avantajı vermez.",
                            "VIP never grants extra time, points, stronger jokers, rating protection or a winning advantage.",
                        ),
                        color = MainUi.Text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        item {
            if (active) {
                OutlinedButton(
                    onClick = {
                        val url = "https://play.google.com/store/account/subscriptions?package=${BuildConfig.APPLICATION_ID}"
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MainUi.Blue.copy(alpha = .40f)),
                ) {
                    Icon(Icons.Rounded.OpenInNew, null, tint = MainUi.Blue)
                    Spacer(Modifier.width(7.dp))
                    Text(sh("GOOGLE PLAY'DE YÖNET", "MANAGE ON GOOGLE PLAY"), color = MainUi.Blue, fontWeight = FontWeight.Black)
                }
            } else {
                Button(
                    onClick = { showPurchase = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(17.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MainUi.Blue),
                ) {
                    Text(sh("VIP PLANLARINI GÖR", "VIEW VIP PLANS"), fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(7.dp))
                    Icon(Icons.Rounded.ArrowForward, null)
                }
            }
        }

        item {
            Text(
                sh("Ödeme ve iptal Google Play hesabın üzerinden yönetilir.", "Payment and cancellation are managed through your Google Play account."),
                Modifier.fillMaxWidth(),
                color = MainUi.Muted,
                fontSize = 13.sp,
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
    Surface(shape = RoundedCornerShape(20.dp), color = MainUi.Surface, border = BorderStroke(1.dp, MainUi.Border)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = MainUi.Blue, fontSize = 13.sp, fontWeight = FontWeight.Black)
            content()
        }
    }
}

@Composable
private fun MainToggleSetting(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(12.dp), color = MainUi.BlueSoft) {
            Icon(icon, null, tint = MainUi.Blue, modifier = Modifier.padding(8.dp).size(19.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = MainUi.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MainUi.Muted, fontSize = 8.5.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun MainSettingsLink(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(12.dp), color = MainUi.BlueSoft) {
            Icon(icon, null, tint = MainUi.Blue, modifier = Modifier.padding(8.dp).size(19.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = MainUi.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MainUi.Muted, fontSize = 8.5.sp)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = MainUi.Muted)
    }
}

@Composable
private fun MainVipBenefit(icon: ImageVector, title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(12.dp), color = MainUi.BlueSoft) {
            Icon(icon, null, tint = MainUi.Blue, modifier = Modifier.padding(9.dp).size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = MainUi.Text, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = MainUi.Muted, fontSize = 13.sp)
        }
        Icon(Icons.Rounded.CheckCircle, null, tint = MainUi.Green, modifier = Modifier.size(18.dp))
    }
}
