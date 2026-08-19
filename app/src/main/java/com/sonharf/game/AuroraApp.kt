package com.sonharf.game

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import kotlinx.coroutines.launch

@Composable
fun AuroraSonHarfApp() {
    var screen by remember { mutableStateOf(AppScreen.HOME) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { if (screen != AppScreen.LEADERBOARD) AuroraBottomBar(screen) { screen = it } }
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).background(
                if (SonHarfPreferences.darkModeEnabled(LocalContext.current))
                    Brush.verticalGradient(listOf(Color(0xFF07111F), SonHarfBg, Color(0xFF02050B)))
                else Brush.verticalGradient(listOf(Color(0xFFF7FAFF), Color(0xFFEAF1FA)))
            )
        ) {
            when (screen) {
                AppScreen.HOME -> AuroraHome({ screen = AppScreen.GAME }, { screen = AppScreen.LEADERBOARD })
                AppScreen.GAME -> OnlineGameScreenV6()
                AppScreen.SHOP -> AuroraShop()
                AppScreen.PROFILE -> AuroraProfile()
                AppScreen.MORE -> AuroraSettings()
                AppScreen.LEADERBOARD -> AuroraLeaderboard { screen = AppScreen.HOME }
            }
        }
    }
}

@Composable
private fun AuroraBottomBar(screen: AppScreen, onChange: (AppScreen) -> Unit) {
    val context = LocalContext.current
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        listOf(
            Triple(AppScreen.HOME, "⌂", "Ana Sayfa"), Triple(AppScreen.GAME, "⚔", "Oyna"),
            Triple(AppScreen.SHOP, "◇", "Mağaza"), Triple(AppScreen.PROFILE, "♙", "Profil"),
            Triple(AppScreen.MORE, "•••", "Daha Fazla")
        ).forEach { (target, icon, label) ->
            NavigationBarItem(
                selected = screen == target,
                onClick = { SonHarfSoundFx.tap(); SonHarfPreferences.hapticTap(context); onChange(target) },
                icon = {
                    Surface(color = if (screen == target) SonHarfPurple.copy(alpha = .22f) else Color.Transparent, shape = RoundedCornerShape(16.dp)) {
                        Text(icon, Modifier.padding(horizontal = 11.dp, vertical = 5.dp), color = if (screen == target) SonHarfCyan else SonHarfMuted, fontWeight = FontWeight.Black, fontSize = 19.sp)
                    }
                },
                label = { Text(label, maxLines = 1, fontSize = 9.sp) },
                colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent, selectedTextColor = MaterialTheme.colorScheme.onSurface, unselectedTextColor = SonHarfMuted)
            )
        }
    }
}

@Composable
private fun AuroraHome(onPlay: () -> Unit, onLeaderboard: () -> Unit) {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var top by remember { mutableStateOf<List<ProfileDto>>(emptyList()) }
    LaunchedEffect(Unit) {
        val b = backend ?: return@LaunchedEffect
        if (b.currentUserId() == null) runCatching { b.ensurePlayer("Oyuncu") }
        profile = b.currentUserId()?.let { runCatching { b.getProfile(it) }.getOrNull() }
        top = runCatching { b.getLeaderboard(3).map { it.profile } }.getOrDefault(emptyList())
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NeonAvatar(profile?.displayName ?: "O", 40.dp); Spacer(Modifier.width(9.dp))
                    Column { Text(profile?.displayName ?: "Oyuncu", fontWeight = FontWeight.Black, fontSize = 14.sp); Text("Seviye 23", color = SonHarfMuted, fontSize = 10.sp) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Pill("💎 ${profile?.diamonds ?: 0}", SonHarfCyan); RoundIcon("♩") }
            }
        }
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, Color.White.copy(alpha = .05f))) {
                Box(Modifier.fillMaxWidth().height(185.dp).background(Brush.radialGradient(listOf(Color(0xFF28105B), Color(0xFF0B1630), Color(0xFF08101C)))), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("HEMEN OYNA", fontWeight = FontWeight.Black, fontSize = 24.sp)
                        Text("RAKİBİNİ BUL VE DÜELLOYA BAŞLA!", color = SonHarfMuted, fontSize = 10.sp)
                        Spacer(Modifier.height(18.dp))
                        Button(onClick = onPlay, modifier = Modifier.fillMaxWidth(.78f).height(58.dp), shape = RoundedCornerShape(30.dp), colors = ButtonDefaults.buttonColors(containerColor = SonHarfGold, contentColor = Color(0xFF181006))) {
                            Text("DÜELLOYA GİR  ⚡", fontWeight = FontWeight.Black, fontSize = 18.sp)
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                ModeCard("⟳", "RASTGELE", "Hızlı eşleşme", SonHarfCyan, Modifier.weight(1f), onPlay)
                ModeCard("👥", "ARKADAŞLA", "Davet et", SonHarfGreen, Modifier.weight(1f), onPlay)
                ModeCard("♛", "ÖZEL ODA", if (profile?.isVip == true) "Oluştur / Katıl" else "VIP oluşturur", SonHarfPurple, Modifier.weight(1f), onPlay)
            }
        }
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, Color.White.copy(alpha = .05f))) {
                Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("HAFTANIN EN İYİLERİ", fontWeight = FontWeight.Black, fontSize = 12.sp); Text("Bu Hafta  ›", color = SonHarfMuted, fontSize = 10.sp) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) { repeat(3) { index -> TopPlayerCard(index, top.getOrNull(index), Modifier.weight(1f)) } }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.Transparent), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, SonHarfPink.copy(alpha = .45f))) {
                Row(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF3C123A), Color(0xFF4A133E), Color(0xFF1A1238)))).padding(13.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("♛", color = SonHarfGold, fontSize = 26.sp); Spacer(Modifier.width(10.dp)); Column { Text("VIP OL", color = SonHarfGold, fontWeight = FontWeight.Black); Text("Özel oda açma ve premium avantajlar", fontSize = 9.sp, color = SonHarfMuted) } }
                    Surface(shape = RoundedCornerShape(14.dp), color = SonHarfPink) { Text("VIP", Modifier.padding(horizontal = 14.dp, vertical = 9.dp), fontWeight = FontWeight.Black) }
                }
            }
        }
        item { Button(onClick = onLeaderboard, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF233B90))) { Text("LİDERLİK TABLOSUNU AÇ", fontWeight = FontWeight.Bold) } }
    }
}

@Composable
private fun AuroraProfile() {
    val context = LocalContext.current
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var editing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var photoBytes by remember { mutableStateOf<ByteArray?>(null) }
    var localBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var status by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@runCatching
                photoBytes = bytes
                localBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.onFailure { status = "Fotoğraf okunamadı." }
        }
    }
    suspend fun reload() {
        val b = backend ?: return
        if (b.currentUserId() == null) runCatching { b.ensurePlayer("Oyuncu") }
        profile = b.currentUserId()?.let { runCatching { b.getProfile(it) }.getOrNull() }
    }
    LaunchedEffect(Unit) { reload() }
    val p = profile
    val matches = p?.totalMatches?.takeIf { it > 0 } ?: ((p?.wins ?: 0) + (p?.losses ?: 0))
    val rate = if (matches == 0) 0 else (p?.wins ?: 0) * 100 / matches
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                RoundIcon("‹")
                TextButton(onClick = { editName = p?.displayName ?: "Oyuncu"; editing = true }) { Text("DÜZENLE", color = SonHarfCyan, fontWeight = FontWeight.Black) }
            }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (localBitmap != null) {
                    Image(localBitmap!!.asImageBitmap(), null, Modifier.size(92.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                } else NeonAvatar(p?.displayName ?: "O", 92.dp)
                Spacer(Modifier.height(9.dp)); Text(p?.displayName ?: "Oyuncu", fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text(if (p?.isVip == true) "SON HARF USTASI  ◆" else "SON HARF OYUNCUSU", color = SonHarfMuted, fontSize = 10.sp)
                if (p?.avatarPath != null) Text("Profil fotoğrafı kayıtlı", color = SonHarfGreen, fontSize = 9.sp)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Seviye 23", fontSize = 10.sp); Spacer(Modifier.width(8.dp)); LinearProgressIndicator(progress = { .64f }, modifier = Modifier.weight(1f).height(6.dp), color = SonHarfPurple, trackColor = SonHarfSurface2); Spacer(Modifier.width(8.dp)); Text("3.250 / 5.000", fontSize = 9.sp, color = SonHarfMuted) }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { MetricCard((p?.wins ?: 0).toString(), "Galibiyet", Modifier.weight(1f)); MetricCard((p?.losses ?: 0).toString(), "Mağlubiyet", Modifier.weight(1f)); MetricCard("%$rate", "Kazanma Oranı", Modifier.weight(1f)) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { MetricCard(matches.toString(), "Toplam Maç", Modifier.weight(1f)); MetricCard((p?.totalRounds ?: 0).toString(), "Toplam Round", Modifier.weight(1f)); MetricCard((p?.validWords ?: 0).toString(), "Toplam Kelime", Modifier.weight(1f)) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { MetricCard((p?.bestStreak ?: 0).toString(), "En Uzun Seri", Modifier.weight(1f)); MetricCard((p?.wordStorms ?: 0).toString(), "Söz Fırtınası", Modifier.weight(1f)); MetricCard((p?.rating ?: 1000).toString(), "Puan", Modifier.weight(1f)) }
            }
        }
        if (status.isNotBlank()) item { Text(status, color = SonHarfCyan, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 10.sp) }
        item { Text("SON BAŞARILAR", fontWeight = FontWeight.Black, fontSize = 11.sp); Spacer(Modifier.height(8.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { listOf("🏆", "★", "✦", "♜", "✪").forEachIndexed { i, s -> Surface(shape = CircleShape, color = if (i < 3) SonHarfGold.copy(alpha = .11f) else SonHarfSurface2, border = BorderStroke(1.dp, if (i < 3) SonHarfGold.copy(alpha = .55f) else Color.White.copy(alpha = .08f))) { Text(s, Modifier.padding(12.dp), fontSize = 20.sp, color = if (i < 3) SonHarfGold else SonHarfMuted) } } } }
    }
    if (editing) {
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("PROFİLİ DÜZENLE") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(editName, { editName = it.take(24) }, label = { Text("Oyuncu adı") }, singleLine = true)
                    OutlinedButton(onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) { Text(if (photoBytes == null) "FOTOĞRAF SEÇ" else "FOTOĞRAF SEÇİLDİ ✓") }
                    Text("Fotoğraf Supabase profil alanına kaydedilir.", color = SonHarfMuted, fontSize = 9.sp)
                }
            },
            confirmButton = { Button(onClick = { scope.launch { status = "Kaydediliyor…"; runCatching { backend?.updateMyProfile(editName, photoBytes) }.onSuccess { if (it != null) profile = it; photoBytes = null; status = "Profil kaydedildi."; editing = false }.onFailure { status = "Profil kaydedilemedi: ${it.message.orEmpty()}" } } }) { Text("KAYDET") } },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("VAZGEÇ") } }
        )
    }
}

@Composable
private fun AuroraLeaderboard(onBack: () -> Unit) {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var rows by remember { mutableStateOf<List<ProfileDto>>(emptyList()) }; var tab by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { rows = runCatching { backend?.getLeaderboard(50)?.map { it.profile } ?: emptyList() }.getOrDefault(emptyList()) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { TextButton(onClick = onBack) { Text("‹", fontSize = 30.sp, color = SonHarfPurple) }; Text("LİDERLİK TABLOSU", fontWeight = FontWeight.Black, fontSize = 22.sp) } }
        item { Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(SonHarfSurface2).padding(4.dp)) { listOf("TOPLAM", "BU HAFTA", "BU AY").forEachIndexed { i, t -> Button(onClick = { tab = i }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (tab == i) Color(0xFF3944D7) else Color.Transparent), shape = RoundedCornerShape(12.dp)) { Text(t, fontSize = 10.sp) } } } }
        item { Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) { Text("SIRA", Modifier.width(42.dp), color = SonHarfMuted, fontSize = 9.sp); Text("OYUNCU", Modifier.weight(1f), color = SonHarfMuted, fontSize = 9.sp); Text("GALİBİYET", Modifier.width(70.dp), color = SonHarfMuted, fontSize = 9.sp); Text("KAZANMA %", Modifier.width(72.dp), color = SonHarfMuted, fontSize = 9.sp) } }
        itemsIndexed(rows) { index, p ->
            val matches = p.totalMatches.takeIf { it > 0 } ?: (p.wins + p.losses); val wr = if (matches == 0) 0 else p.wins * 100 / matches
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(15.dp), border = BorderStroke(1.dp, if (index == 0) SonHarfGold.copy(alpha = .35f) else Color.White.copy(alpha = .05f))) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (index < 3) listOf("♛", "♜", "♝")[index] else "${index + 1}", Modifier.width(42.dp), color = if (index == 0) SonHarfGold else SonHarfMuted, textAlign = TextAlign.Center)
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) { NeonAvatar(p.displayName, 34.dp); Spacer(Modifier.width(9.dp)); Text(p.displayName, fontWeight = FontWeight.Bold) }
                    Text(p.wins.toString(), Modifier.width(70.dp), textAlign = TextAlign.Center); Text("%$wr", Modifier.width(72.dp), textAlign = TextAlign.Center)
                }
            }
        }
        if (rows.isEmpty()) item { Text("Sıralama verisi bekleniyor.", color = SonHarfMuted, modifier = Modifier.fillMaxWidth().padding(24.dp), textAlign = TextAlign.Center) }
    }
}

@Composable private fun AuroraShop() { LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { item { Text("MAĞAZA", fontSize = 27.sp, fontWeight = FontWeight.Black); Text("Kozmetik ve premium içerikler", color = SonHarfMuted, fontSize = 11.sp) }; item { ShopCard("◆", "VIP", "Özel oda açma, özel temalar ve gelişmiş istatistikler", SonHarfGold) }; item { ShopCard("💎", "ELMAS", "Google Play Billing bağlandığında satın alma aktif olacak", SonHarfCyan) }; item { ShopCard("✦", "TEMALAR", "Profil ve oyun görünümünü kişiselleştir", SonHarfPurple) }; item { ShopCard("☺", "EMOJİ PAKETLERİ", "Sohbette kullanabileceğin kozmetik paketler", SonHarfGreen) } } }

@Composable
private fun AuroraSettings() {
    val context = LocalContext.current
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    var sound by remember { mutableStateOf(SonHarfPreferences.soundEnabled(context)) }
    var music by remember { mutableStateOf(SonHarfPreferences.musicEnabled(context)) }
    var vibration by remember { mutableStateOf(SonHarfPreferences.vibrationEnabled(context)) }
    var gameInvites by remember { mutableStateOf(SonHarfPreferences.gameInvitesEnabled(context)) }
    var friendRequestsEnabled by remember { mutableStateOf(SonHarfPreferences.friendRequestsEnabled(context)) }
    var systemNotifications by remember { mutableStateOf(SonHarfPreferences.systemNotificationsEnabled(context)) }
    var darkMode by remember { mutableStateOf(SonHarfPreferences.darkModeEnabled(context)) }
    var language by remember { mutableStateOf(SonHarfPreferences.language(context)) }
    var showBlocks by remember { mutableStateOf(false) }
    var showSocial by remember { mutableStateOf(false) }
    var blocked by remember { mutableStateOf<List<ProfileDto>>(emptyList()) }
    var friends by remember { mutableStateOf<List<Pair<com.sonharf.game.data.FriendshipDto, ProfileDto>>>(emptyList()) }
    var requests by remember { mutableStateOf<List<Pair<com.sonharf.game.data.FriendshipDto, ProfileDto>>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<ProfileDto>>(emptyList()) }
    var socialNotice by remember { mutableStateOf("") }

    fun loadSocial() { scope.launch { friends = runCatching { backend?.getFriends() ?: emptyList() }.getOrDefault(emptyList()); requests = runCatching { backend?.getIncomingFriendRequests() ?: emptyList() }.getOrDefault(emptyList()) } }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("AYARLAR", fontSize = 27.sp, fontWeight = FontWeight.Black) }
        item { SettingsGroup("SES & TİTREŞİM") {
            SettingSwitch("Ses Efektleri", sound) { sound = it; SonHarfPreferences.setSoundEnabled(context, it) }
            SettingSwitch("Müzik", music) { music = it; SonHarfPreferences.setMusicEnabled(context, it) }
            SettingSwitch("Titreşim", vibration) { vibration = it; SonHarfPreferences.setVibrationEnabled(context, it) }
        } }
        item { SettingsGroup("BİLDİRİMLER") {
            SettingSwitch("Oyun Davetleri", gameInvites) { gameInvites = it; SonHarfPreferences.setGameInvitesEnabled(context, it) }
            SettingSwitch("Arkadaşlık İstekleri", friendRequestsEnabled) { friendRequestsEnabled = it; SonHarfPreferences.setFriendRequestsEnabled(context, it) }
            SettingSwitch("Sistem Bildirimleri", systemNotifications) { systemNotifications = it; SonHarfPreferences.setSystemNotificationsEnabled(context, it) }
        } }
        item { SettingsGroup("SOSYAL") {
            SettingLine("Arkadaşlar ve İstekler", "›") { showSocial = true; loadSocial() }
            SettingLine("Engellenenler", "›") { scope.launch { blocked = runCatching { backend?.getBlockedUsers() ?: emptyList() }.getOrDefault(emptyList()); showBlocks = true } }
        } }
        item { SettingsGroup("DİĞER") {
            SettingLine("Dil", if (language == "tr") "Türkçe  ›" else "English  ›") { language = if (language == "tr") "en" else "tr"; SonHarfPreferences.setLanguage(context, language) }
            SettingSwitch("Karanlık Mod", darkMode) { darkMode = it; SonHarfPreferences.setDarkModeEnabled(context, it); (context as? Activity)?.recreate() }
        } }
    }

    if (showBlocks) AlertDialog(onDismissRequest = { showBlocks = false }, title = { Text("ENGELLENENLER") }, text = {
        LazyColumn(Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (blocked.isEmpty()) item { Text("Engellenen oyuncu yok.", color = SonHarfMuted) }
            items(blocked) { p -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(p.displayName); TextButton(onClick = { scope.launch { runCatching { backend?.unblockUser(p.id) }; blocked = blocked.filterNot { it.id == p.id } } }) { Text("ENGELİ KALDIR") } } }
        }
    }, confirmButton = { TextButton(onClick = { showBlocks = false }) { Text("KAPAT") } })

    if (showSocial) AlertDialog(onDismissRequest = { showSocial = false }, title = { Text("ARKADAŞLAR") }, text = {
        Column(Modifier.heightIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(query, { query = it.take(24) }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Oyuncu ara") }, trailingIcon = { TextButton(onClick = { scope.launch { results = runCatching { backend?.searchPlayers(query) ?: emptyList() }.getOrDefault(emptyList()) } }) { Text("ARA") } })
            if (requests.isNotEmpty()) Text("GELEN İSTEKLER", color = SonHarfCyan, fontSize = 10.sp, fontWeight = FontWeight.Black)
            requests.forEach { (_, p) -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(p.displayName); Row { TextButton(onClick = { scope.launch { backend?.respondFriendRequest(p.id, true); loadSocial() } }) { Text("KABUL") }; TextButton(onClick = { scope.launch { backend?.respondFriendRequest(p.id, false); loadSocial() } }) { Text("RED") } } } }
            if (results.isNotEmpty()) Text("ARAMA", color = SonHarfCyan, fontSize = 10.sp, fontWeight = FontWeight.Black)
            results.forEach { p -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(p.displayName); TextButton(onClick = { scope.launch { runCatching { backend?.sendFriendRequest(p.id) }.onSuccess { socialNotice = "İstek gönderildi." }.onFailure { socialNotice = it.message.orEmpty() } } }) { Text("EKLE") } } }
            Text("ARKADAŞ LİSTESİ", color = SonHarfCyan, fontSize = 10.sp, fontWeight = FontWeight.Black)
            if (friends.isEmpty()) Text("Henüz arkadaş yok.", color = SonHarfMuted, fontSize = 10.sp)
            friends.forEach { (_, p) -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(p.displayName); Text(if (p.presenceStatus == "online") "Çevrimiçi" else "Çevrimdışı", color = if (p.presenceStatus == "online") SonHarfGreen else SonHarfMuted, fontSize = 9.sp) } }
            if (socialNotice.isNotBlank()) Text(socialNotice, color = SonHarfCyan, fontSize = 9.sp)
        }
    }, confirmButton = { TextButton(onClick = { showSocial = false }) { Text("KAPAT") } })
}

@Composable private fun ModeCard(icon:String,title:String,sub:String,color:Color,modifier:Modifier,onClick:()->Unit){Card(onClick=onClick,modifier=modifier.height(96.dp),colors=CardDefaults.cardColors(containerColor=color.copy(alpha=.10f)),shape=RoundedCornerShape(17.dp),border=BorderStroke(1.dp,color.copy(alpha=.45f))){Column(Modifier.fillMaxSize().padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Text(icon,fontSize=24.sp,color=color);Text(title,fontWeight=FontWeight.Black,fontSize=10.sp);Text(sub,color=SonHarfMuted,fontSize=8.sp,textAlign=TextAlign.Center)}}}
@Composable private fun TopPlayerCard(index:Int,p:ProfileDto?,modifier:Modifier){val c=listOf(SonHarfGold,Color(0xFFB7C1D8),Color(0xFFFF8C4A))[index];Card(modifier=modifier,colors=CardDefaults.cardColors(containerColor=c.copy(alpha=.08f)),shape=RoundedCornerShape(15.dp),border=BorderStroke(1.dp,c.copy(alpha=.42f))){Column(Modifier.padding(9.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("${index+1}",color=c,fontWeight=FontWeight.Black);Spacer(Modifier.height(5.dp));NeonAvatar(p?.displayName?:"—",38.dp);Spacer(Modifier.height(5.dp));Text(p?.displayName?:"Bekleniyor",fontWeight=FontWeight.Bold,fontSize=10.sp,maxLines=1);Text(if(p==null)"—" else "${p.wins} GALİBİYET",color=SonHarfMuted,fontSize=7.sp)}}}
@Composable private fun NeonAvatar(name:String,size:Dp){Box(Modifier.size(size).clip(CircleShape).background(Brush.linearGradient(listOf(SonHarfCyan,SonHarfPurple,SonHarfPink))).padding(2.dp),contentAlignment=Alignment.Center){Box(Modifier.fillMaxSize().clip(CircleShape).background(MaterialTheme.colorScheme.surface),contentAlignment=Alignment.Center){Text(name.take(1).uppercase(),fontWeight=FontWeight.Black,fontSize=(size.value*.38f).sp)}}}
@Composable private fun Pill(text:String,color:Color){Surface(color=color.copy(alpha=.12f),shape=RoundedCornerShape(99.dp),border=BorderStroke(1.dp,color.copy(alpha=.3f))){Text(text,Modifier.padding(horizontal=12.dp,vertical=7.dp),color=color,fontWeight=FontWeight.Bold,fontSize=10.sp)}}
@Composable private fun RoundIcon(text:String){Surface(shape=CircleShape,color=MaterialTheme.colorScheme.surface,border=BorderStroke(1.dp,Color.White.copy(alpha=.06f))){Text(text,Modifier.padding(9.dp),color=SonHarfMuted,fontSize=16.sp)}}
@Composable private fun MetricCard(value:String,label:String,modifier:Modifier){Card(modifier=modifier,colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),shape=RoundedCornerShape(16.dp),border=BorderStroke(1.dp,Color.White.copy(alpha=.05f))){Column(Modifier.padding(vertical=15.dp,horizontal=6.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(value,fontSize=22.sp,fontWeight=FontWeight.Black);Text(label,color=SonHarfMuted,fontSize=8.sp,textAlign=TextAlign.Center)}}}
@Composable private fun ShopCard(icon:String,title:String,sub:String,color:Color){Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,color.copy(alpha=.25f))){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Text(icon,fontSize=32.sp,color=color);Spacer(Modifier.width(13.dp));Column(Modifier.weight(1f)){Text(title,fontWeight=FontWeight.Black,color=color);Text(sub,color=SonHarfMuted,fontSize=10.sp)};Text("›",fontSize=24.sp,color=SonHarfMuted)}}}
@Composable private fun SettingsGroup(title:String,content:@Composable ColumnScope.()->Unit){Column(verticalArrangement=Arrangement.spacedBy(6.dp)){Text(title,color=SonHarfCyan,fontSize=10.sp,fontWeight=FontWeight.Black);Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,Color.White.copy(alpha=.05f))){Column(Modifier.padding(horizontal=13.dp,vertical=4.dp),content=content)}}}
@Composable private fun SettingSwitch(label:String,checked:Boolean,onChange:(Boolean)->Unit){Row(Modifier.fillMaxWidth().height(48.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text(label,fontSize=12.sp);Switch(checked,onChange)}}
@Composable private fun SettingLine(label:String,value:String,onClick:()->Unit={}){TextButton(onClick=onClick,modifier=Modifier.fillMaxWidth().height(48.dp),contentPadding=PaddingValues(0.dp)){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text(label,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurface);Text(value,color=SonHarfMuted,fontSize=11.sp)}}}
