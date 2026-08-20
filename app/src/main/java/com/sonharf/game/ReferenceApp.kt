package com.sonharf.game

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.sonharf.game.data.LeaderboardEntry
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.getLanguageLeaderboard
import kotlinx.coroutines.launch

private val RefBg = Color(0xFF020713)
private val RefPanel = Color(0xFF08111F)
private val RefPanel2 = Color(0xFF0C1627)
private val RefStroke = Color(0xFF21324A)
private val RefBlue = Color(0xFF18B8FF)
private val RefPurple = Color(0xFF6A42F4)
private val RefMagenta = Color(0xFFB51FE8)
private val RefGold = Color(0xFFF5C04D)
private val RefGreen = Color(0xFF4BC765)
private val RefRed = Color(0xFFF25B75)
private val RefMuted = Color(0xFF8C98AD)
private val RefText = Color(0xFFF3F6FF)

private enum class RefScreen { HOME, GAME, SHOP, PROFILE, MORE, LEADERBOARD }

@Composable
fun ReferenceSonHarfApp() {
    var screen by remember { mutableStateOf(RefScreen.HOME) }
    val dark = SonHarfPreferences.darkModeEnabled(LocalContext.current)
    val bg = if (dark) RefBg else MaterialTheme.colorScheme.background
    Scaffold(
        containerColor = bg,
        bottomBar = {
            if (screen != RefScreen.LEADERBOARD) RefBottomBar(screen) { screen = it }
        }
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).background(
                if (dark) Brush.verticalGradient(listOf(Color(0xFF030916), RefBg, Color(0xFF01040A)))
                else Brush.verticalGradient(listOf(Color(0xFFF8FAFE), Color(0xFFEDF3FA)))
            )
        ) {
            when (screen) {
                RefScreen.HOME -> RefHome(
                    onPlay = { screen = RefScreen.GAME },
                    onProfile = { screen = RefScreen.PROFILE },
                    onSettings = { screen = RefScreen.MORE },
                    onLeaderboard = { screen = RefScreen.LEADERBOARD }
                )
                RefScreen.GAME -> OnlineGameScreenV7()
                RefScreen.SHOP -> RefShop()
                RefScreen.PROFILE -> RefProfile(onBack = { screen = RefScreen.HOME })
                RefScreen.MORE -> RefSettings(onBack = { screen = RefScreen.HOME })
                RefScreen.LEADERBOARD -> RefLeaderboard(onBack = { screen = RefScreen.HOME })
            }
        }
    }
}

@Composable
private fun RefBottomBar(screen: RefScreen, onChange: (RefScreen) -> Unit) {
    val context = LocalContext.current
    NavigationBar(containerColor = if (SonHarfPreferences.darkModeEnabled(context)) Color(0xFF040B17) else MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        listOf(
            Triple(RefScreen.HOME, "⌂", "Ana Sayfa"),
            Triple(RefScreen.GAME, "⚔", "Oyna"),
            Triple(RefScreen.SHOP, "▣", "Mağaza"),
            Triple(RefScreen.PROFILE, "♙", "Profil"),
            Triple(RefScreen.MORE, "⋮", "Daha Fazla")
        ).forEach { (target, icon, label) ->
            val selected = screen == target
            NavigationBarItem(
                selected = selected,
                onClick = { SonHarfSoundFx.tap(); SonHarfPreferences.hapticTap(context); onChange(target) },
                icon = {
                    Box(
                        Modifier.size(42.dp).clip(RoundedCornerShape(15.dp))
                            .background(if (selected) RefPurple.copy(alpha = .22f) else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) { Text(icon, color = if (selected) RefBlue else RefMuted, fontSize = 20.sp, fontWeight = FontWeight.Black) }
                },
                label = { Text(label, fontSize = 8.sp) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent,
                    selectedTextColor = RefText,
                    unselectedTextColor = RefMuted
                )
            )
        }
    }
}

@Composable
private fun RefHome(onPlay: () -> Unit, onProfile: () -> Unit, onSettings: () -> Unit, onLeaderboard: () -> Unit) {
    val context = LocalContext.current
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var top by remember { mutableStateOf<List<ProfileDto>>(emptyList()) }
    var leaderLanguage by remember { mutableStateOf(SonHarfPreferences.language(context)) }
    LaunchedEffect(Unit) {
        val b = backend ?: return@LaunchedEffect
        if (b.currentUserId() == null) runCatching { b.ensurePlayer("Oyuncu") }
        profile = b.currentUserId()?.let { runCatching { b.getProfile(it) }.getOrNull() }
    }
    LaunchedEffect(leaderLanguage) {
        val b = backend ?: return@LaunchedEffect
        top = runCatching { b.getLanguageLeaderboard(leaderLanguage, "week", 3).map { it.profile } }.getOrDefault(emptyList())
    }
    val p = profile
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.clickable(onClick = onProfile), verticalAlignment = Alignment.CenterVertically) {
                    RefAvatar(p?.displayName ?: "O", 40.dp, RefGold)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(p?.displayName ?: "Oyuncu", color = RefText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("♥", color = RefPurple, fontSize = 9.sp)
                            Text("  Elmas: ${p?.diamonds ?: 0}", color = RefMuted, fontSize = 8.sp)
                            Text("  ◆", color = RefBlue, fontSize = 8.sp)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    RefIconButton("▣", {})
                    RefIconButton("♙", onProfile)
                    RefIconButton("⚙", onSettings)
                }
            }
        }
        item {
            Box(
                Modifier.fillMaxWidth().height(175.dp).clip(RoundedCornerShape(22.dp))
                    .background(Brush.radialGradient(listOf(Color(0xFF231049), Color(0xFF07152A), Color(0xFF050B16)))),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SON", color = RefText, fontSize = 37.sp, fontWeight = FontWeight.Black, letterSpacing = (-2).sp)
                    Text("HARF", color = RefText, fontSize = 37.sp, fontWeight = FontWeight.Black, letterSpacing = (-2).sp)
                    Text("GERÇEK ZAMANLI KELİME DÜELLOSU", color = RefText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        item { RefGradientButton("DÜELLOYA GİR", "Rastgele rakip bul", onPlay) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                RefModeButton("♟", "ARKADAŞLAR", "Çevrimiçi: —", RefBlue, Modifier.weight(1f), onPlay)
                RefModeButton("♛", "ÖZEL ODA", if (p?.isVip == true) "Oda oluştur / Katıl" else "VIP oluşturur", RefPurple, Modifier.weight(1f), onPlay)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RefPanel), border = BorderStroke(1.dp, RefStroke), shape = RoundedCornerShape(15.dp)) {
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    RefRule("3 × 10", "KELİME"); RefDivider(); RefRule("45 sn", "SÜRE"); RefDivider(); RefRule("3", "ROUND"); RefDivider(); RefRule("TR / EN", "DİL")
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("HAFTANIN EN İYİLERİ", color = RefText, fontWeight = FontWeight.Black, fontSize = 11.sp)
                Row(Modifier.clip(RoundedCornerShape(8.dp)).background(RefPanel2).padding(2.dp)) {
                    listOf("tr" to "🇹🇷 TR", "en" to "🇬🇧 EN").forEach { (code, label) ->
                        Surface(
                            modifier = Modifier.clickable { leaderLanguage = code },
                            color = if (leaderLanguage == code) RefPurple.copy(alpha = .55f) else Color.Transparent,
                            shape = RoundedCornerShape(7.dp)
                        ) { Text(label, Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = if (leaderLanguage == code) RefText else RefMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { i -> RefTopCard(i, top.getOrNull(i), Modifier.weight(1f)) }
            }
        }
        item {
            Button(onClick = onLeaderboard, modifier = Modifier.fillMaxWidth().height(42.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3438C8))) {
                Text("TÜM LİDERLİK TABLOSU", fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun RefProfile(onBack: () -> Unit) {
    val context = LocalContext.current
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var editing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var photoBytes by remember { mutableStateOf<ByteArray?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var status by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@runCatching
            photoBytes = bytes
            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.onFailure { status = "Fotoğraf okunamadı." }
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
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                RefIconButton("‹", onBack)
                RefIconButton("✎") { editName = p?.displayName ?: "Oyuncu"; editing = true }
            }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(108.dp).clip(CircleShape).background(Brush.sweepGradient(listOf(RefGold, RefBlue, RefPurple, RefGold))).padding(3.dp), contentAlignment = Alignment.Center) {
                    if (bitmap != null) Image(bitmap!!.asImageBitmap(), null, Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                    else RefAvatar(p?.displayName ?: "O", 102.dp, RefGold)
                }
                Spacer(Modifier.height(8.dp))
                Text(p?.displayName ?: "Oyuncu", color = RefText, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text(if (p?.isVip == true) "SON HARF USTASI  ♥" else "SON HARF OYUNCUSU", color = RefMuted, fontSize = 9.sp)
                Spacer(Modifier.height(15.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Seviye 23", color = RefText, fontSize = 9.sp); Spacer(Modifier.width(8.dp)); LinearProgressIndicator(progress = { .64f }, modifier = Modifier.weight(1f).height(5.dp), color = RefPurple, trackColor = RefPanel2); Spacer(Modifier.width(8.dp)); Text("3.250 / 5.000", color = RefMuted, fontSize = 8.sp)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { RefMetric((p?.wins ?: 0).toString(), "Galibiyet", Modifier.weight(1f)); RefMetric((p?.losses ?: 0).toString(), "Mağlubiyet", Modifier.weight(1f)); RefMetric("%$rate", "Kazanma Oranı", Modifier.weight(1f)) }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { RefMetric(matches.toString(), "Toplam Maç", Modifier.weight(1f)); RefMetric((p?.totalRounds ?: 0).toString(), "Toplam Round", Modifier.weight(1f)); RefMetric((p?.validWords ?: 0).toString(), "Toplam Kelime", Modifier.weight(1f)) }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { RefMetric((p?.bestStreak ?: 0).toString(), "En Uzun Seri", Modifier.weight(1f)); RefMetric((p?.wordStorms ?: 0).toString(), "Söz Fırtınası", Modifier.weight(1f)); RefMetric((p?.rating ?: 1000).toString(), "En Yüksek Puan", Modifier.weight(1f)) }
            }
        }
        item {
            Text("SON BAŞARILAR", color = RefText, fontWeight = FontWeight.Black, fontSize = 10.sp); Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("♛", "★", "✦", "♟", "✪").forEachIndexed { i, icon ->
                    Surface(shape = CircleShape, color = if (i < 3) RefGold.copy(alpha = .12f) else RefPanel2, border = BorderStroke(1.dp, if (i < 3) RefGold.copy(alpha = .7f) else RefStroke)) { Text(icon, Modifier.padding(13.dp), color = if (i < 3) RefGold else RefMuted, fontSize = 18.sp) }
                }
            }
        }
        if (status.isNotBlank()) item { Text(status, color = RefBlue, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 9.sp) }
    }
    if (editing) AlertDialog(
        onDismissRequest = { editing = false },
        title = { Text("PROFİLİ DÜZENLE") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(editName, { editName = it.take(24) }, label = { Text("Oyuncu adı") }, singleLine = true)
                OutlinedButton(onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) { Text(if (photoBytes == null) "PROFİL FOTOĞRAFI SEÇ" else "FOTOĞRAF SEÇİLDİ ✓") }
            }
        },
        confirmButton = {
            Button(onClick = {
                scope.launch {
                    status = "Kaydediliyor…"
                    runCatching { backend?.updateMyProfile(editName, photoBytes) }
                        .onSuccess { if (it != null) profile = it; photoBytes = null; editing = false; status = "Profil güncellendi." }
                        .onFailure { status = "Profil kaydedilemedi." }
                }
            }) { Text("KAYDET") }
        },
        dismissButton = { TextButton(onClick = { editing = false }) { Text("VAZGEÇ") } }
    )
}

@Composable
private fun RefLeaderboard(onBack: () -> Unit) {
    val context = LocalContext.current
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var rows by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var tab by remember { mutableStateOf(0) }
    var language by remember { mutableStateOf(SonHarfPreferences.language(context)) }
    var me by remember { mutableStateOf<String?>(null) }
    val period = when (tab) { 1 -> "week"; 2 -> "month"; else -> "total" }
    LaunchedEffect(language, tab) {
        me = backend?.currentUserId()
        rows = runCatching { backend?.getLanguageLeaderboard(language, period, 50) ?: emptyList() }.getOrDefault(emptyList())
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { RefIconButton("‹", onBack); Spacer(Modifier.width(9.dp)); Text("LİDERLİK TABLOSU", color = RefText, fontSize = 20.sp, fontWeight = FontWeight.Black) } }
        item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(RefPanel).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("tr" to "🇹🇷 TÜRKÇE", "en" to "🇬🇧 ENGLISH").forEach { (code, label) ->
                    Button(
                        onClick = { language = code },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = if (language == code) RefPurple else Color.Transparent),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text(label, fontSize = 9.sp, fontWeight = FontWeight.Black) }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(RefPanel).padding(4.dp)) {
                listOf("Toplam", "Bu Hafta", "Bu Ay").forEachIndexed { i, t ->
                    Button(onClick = { tab = i }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (tab == i) Color(0xFF3535BB) else Color.Transparent), shape = RoundedCornerShape(10.dp)) { Text(t, fontSize = 9.sp) }
                }
            }
        }
        item {
            Text(if (language == "tr") "Türkçe maçların sıralaması" else "English match rankings", color = RefBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                Text("#", Modifier.width(32.dp), color = RefMuted, fontSize = 8.sp)
                Text("Oyuncu", Modifier.weight(1f), color = RefMuted, fontSize = 8.sp)
                Text("Galibiyet", Modifier.width(64.dp), color = RefMuted, fontSize = 8.sp, textAlign = TextAlign.Center)
                Text("Kazanma %", Modifier.width(70.dp), color = RefMuted, fontSize = 8.sp, textAlign = TextAlign.Center)
            }
        }
        itemsIndexed(rows) { index, entry ->
            val p = entry.profile
            val mine = p.id == me
            Card(colors = CardDefaults.cardColors(containerColor = if (mine) RefPurple.copy(alpha = .26f) else RefPanel), border = BorderStroke(1.dp, if (mine) RefPurple.copy(alpha = .7f) else RefStroke), shape = RoundedCornerShape(10.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (index < 3) listOf("♛", "♜", "♝")[index] else "${index + 1}", Modifier.width(32.dp), color = if (index == 0) RefGold else RefMuted, textAlign = TextAlign.Center)
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) { RefAvatar(p.displayName, 28.dp, if (index == 0) RefGold else RefBlue); Spacer(Modifier.width(7.dp)); Text(if (mine) "Sen (${p.displayName})" else p.displayName, color = RefText, fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1) }
                    Text(p.wins.toString(), Modifier.width(64.dp), color = RefText, textAlign = TextAlign.Center, fontSize = 10.sp)
                    Text("%${entry.winRate}", Modifier.width(70.dp), color = RefText, textAlign = TextAlign.Center, fontSize = 10.sp)
                }
            }
        }
        if (rows.isEmpty()) item { Text(if (language == "tr") "Bu dönem için Türkçe maç verisi yok." else "No English match data for this period.", color = RefMuted, modifier = Modifier.fillMaxWidth().padding(22.dp), textAlign = TextAlign.Center, fontSize = 10.sp) }
    }
}

@Composable
private fun RefSettings(onBack: () -> Unit) {
    val context = LocalContext.current
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    var sound by remember { mutableStateOf(SonHarfPreferences.soundEnabled(context)) }
    var music by remember { mutableStateOf(SonHarfPreferences.musicEnabled(context)) }
    var vibration by remember { mutableStateOf(SonHarfPreferences.vibrationEnabled(context)) }
    var gameInvites by remember { mutableStateOf(SonHarfPreferences.gameInvitesEnabled(context)) }
    var friendRequests by remember { mutableStateOf(SonHarfPreferences.friendRequestsEnabled(context)) }
    var systemNotes by remember { mutableStateOf(SonHarfPreferences.systemNotificationsEnabled(context)) }
    var dark by remember { mutableStateOf(SonHarfPreferences.darkModeEnabled(context)) }
    var language by remember { mutableStateOf(SonHarfPreferences.language(context)) }
    var showBlocked by remember { mutableStateOf(false) }
    var blocked by remember { mutableStateOf<List<ProfileDto>>(emptyList()) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { RefIconButton("‹", onBack); Spacer(Modifier.width(10.dp)); Text("AYARLAR", color = RefText, fontSize = 22.sp, fontWeight = FontWeight.Black) } }
        item { RefSettingsGroup("SES AYARLARI") {
            RefSwitchLine("Ses Efektleri", sound) { sound = it; SonHarfPreferences.setSoundEnabled(context, it) }
            RefSwitchLine("Müzik", music) { music = it; SonHarfPreferences.setMusicEnabled(context, it) }
            RefSwitchLine("Titreşim", vibration) { vibration = it; SonHarfPreferences.setVibrationEnabled(context, it) }
        } }
        item { RefSettingsGroup("BİLDİRİMLER") {
            RefSwitchLine("Oyun Davetleri", gameInvites) { gameInvites = it; SonHarfPreferences.setGameInvitesEnabled(context, it) }
            RefSwitchLine("Arkadaşlık İstekleri", friendRequests) { friendRequests = it; SonHarfPreferences.setFriendRequestsEnabled(context, it) }
            RefSwitchLine("Sistem Bildirimleri", systemNotes) { systemNotes = it; SonHarfPreferences.setSystemNotificationsEnabled(context, it) }
        } }
        item { RefSettingsGroup("DİĞER") {
            Row(Modifier.fillMaxWidth().height(46.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Dil", color = RefText, fontSize = 11.sp)
                Row { TextButton(onClick = { language = "tr"; SonHarfPreferences.setLanguage(context, "tr") }) { Text("TR", color = if (language == "tr") RefBlue else RefMuted) }; TextButton(onClick = { language = "en"; SonHarfPreferences.setLanguage(context, "en") }) { Text("EN", color = if (language == "en") RefBlue else RefMuted) } }
            }
            RefSwitchLine("Karanlık Mod", dark) { dark = it; SonHarfPreferences.setDarkModeEnabled(context, it) }
            Row(Modifier.fillMaxWidth().height(46.dp).clickable { scope.launch { blocked = runCatching { backend?.getBlockedUsers() ?: emptyList() }.getOrDefault(emptyList()); showBlocked = true } }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text("Engellenenler", color = RefText, fontSize = 11.sp); Text("›", color = RefMuted) }
        } }
    }
    if (showBlocked) AlertDialog(
        onDismissRequest = { showBlocked = false },
        title = { Text("ENGELLENENLER") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                if (blocked.isEmpty()) Text("Engellenen oyuncu yok.", color = RefMuted)
                blocked.forEach { p -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(p.displayName); TextButton(onClick = { scope.launch { runCatching { backend?.unblockUser(p.id) }; blocked = blocked.filterNot { it.id == p.id } } }) { Text("ENGELİ KALDIR") } } }
            }
        },
        confirmButton = { TextButton(onClick = { showBlocked = false }) { Text("KAPAT") } }
    )
}

@Composable private fun RefShop() {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("MAĞAZA", color = RefText, fontSize = 23.sp, fontWeight = FontWeight.Black); Text("Premium ve kozmetik içerikler", color = RefMuted, fontSize = 9.sp) }
        item { RefShopCard("♛", "VIP", "Özel oda oluşturma, gelişmiş profil ve premium avantajlar", RefGold) }
        item { RefShopCard("◆", "ELMAS", "Oyun içi premium para birimi", RefBlue) }
        item { RefShopCard("✦", "TEMALAR", "Profil ve oyun görünümünü kişiselleştir", RefPurple) }
    }
}

@Composable private fun RefGradientButton(title: String, sub: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(62.dp).clip(RoundedCornerShape(15.dp)).background(Brush.horizontalGradient(listOf(RefBlue, Color(0xFF3662F2), RefMagenta))).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp); Text(sub, color = Color.White.copy(alpha = .8f), fontSize = 8.sp) }
    }
}
@Composable private fun RefModeButton(icon: String, title: String, sub: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier.height(58.dp), colors = CardDefaults.cardColors(containerColor = RefPanel), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, RefStroke)) {
        Row(Modifier.fillMaxSize().padding(9.dp), verticalAlignment = Alignment.CenterVertically) { Text(icon, color = color, fontSize = 20.sp); Spacer(Modifier.width(8.dp)); Column { Text(title, color = RefText, fontSize = 9.sp, fontWeight = FontWeight.Black); Text(sub, color = RefMuted, fontSize = 7.sp) } }
    }
}
@Composable private fun RefIconButton(icon: String, onClick: () -> Unit) { Surface(modifier = Modifier.size(36.dp).clickable(onClick = onClick), shape = CircleShape, color = RefPanel2, border = BorderStroke(1.dp, RefStroke)) { Box(contentAlignment = Alignment.Center) { Text(icon, color = RefText, fontSize = 16.sp) } } }
@Composable private fun RefRule(value: String, label: String) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(value, color = RefText, fontSize = 11.sp, fontWeight = FontWeight.Black); Text(label, color = RefMuted, fontSize = 6.sp) } }
@Composable private fun RefDivider() { Box(Modifier.width(1.dp).height(23.dp).background(RefStroke)) }
@Composable private fun RefAvatar(name: String, size: Dp, accent: Color) { Box(Modifier.size(size).clip(CircleShape).background(Brush.sweepGradient(listOf(accent, RefBlue, RefPurple, accent))).padding(2.dp), contentAlignment = Alignment.Center) { Box(Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFF152033)), contentAlignment = Alignment.Center) { Text(name.take(1).uppercase(), color = RefText, fontSize = (size.value * .37f).sp, fontWeight = FontWeight.Black) } } }
@Composable private fun RefTopCard(index: Int, p: ProfileDto?, modifier: Modifier) { val accent = listOf(RefGold, Color(0xFFA6B2C7), Color(0xFFD68450))[index]; Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = RefPanel), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, accent.copy(alpha = .45f))) { Column(Modifier.padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(if (index == 0) "♛" else "♜", color = accent, fontSize = 14.sp); RefAvatar(p?.displayName ?: "—", 48.dp, accent); Spacer(Modifier.height(5.dp)); Text(p?.displayName ?: "Bekleniyor", color = RefText, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1); Text(if (p == null) "—" else "${p.wins} GALİBİYET", color = RefMuted, fontSize = 6.sp) } } }
@Composable private fun RefMetric(value: String, label: String, modifier: Modifier) { Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = RefPanel), border = BorderStroke(1.dp, RefStroke), shape = RoundedCornerShape(10.dp)) { Column(Modifier.fillMaxWidth().padding(vertical = 11.dp, horizontal = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, color = RefText, fontSize = 19.sp, fontWeight = FontWeight.Black); Text(label, color = RefMuted, fontSize = 7.sp, textAlign = TextAlign.Center) } } }
@Composable private fun RefSettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) { Column(verticalArrangement = Arrangement.spacedBy(5.dp)) { Text(title, color = RefBlue, fontSize = 9.sp, fontWeight = FontWeight.Black); Card(colors = CardDefaults.cardColors(containerColor = RefPanel), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, RefStroke)) { Column(Modifier.padding(horizontal = 12.dp, vertical = 3.dp), content = content) } } }
@Composable private fun RefSwitchLine(label: String, checked: Boolean, onChange: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = RefText, fontSize = 10.sp); Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = RefGreen, uncheckedTrackColor = RefPanel2)) } }
@Composable private fun RefShopCard(icon: String, title: String, sub: String, accent: Color) { Card(colors = CardDefaults.cardColors(containerColor = RefPanel), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, RefStroke)) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Text(icon, color = accent, fontSize = 28.sp); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(title, color = accent, fontWeight = FontWeight.Black); Text(sub, color = RefMuted, fontSize = 9.sp) }; Text("›", color = RefMuted, fontSize = 22.sp) } } }
