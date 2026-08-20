package com.sonharf.game

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val PBg = Color(0xFF020711)
private val PPanel = Color(0xFF07111E)
private val PPanel2 = Color(0xFF0B1627)
private val PStroke = Color(0xFF1A2B43)
private val PText = Color(0xFFF7F9FF)
private val PMuted = Color(0xFF8995AA)
private val PCyan = Color(0xFF20C7FF)
private val PPurple = Color(0xFF7B37FF)
private val PGold = Color(0xFFFFC247)
private val PGreen = Color(0xFF2DDB7D)
private val PPink = Color(0xFFFF3B7E)

@Composable
fun ProductionProfileScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gameBackend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val service = remember { if (SupabaseProvider.configured) ProductionProfileService() else null }

    var profile by remember { mutableStateOf<ProductionProfileDto?>(null) }
    var avatarBytes by remember { mutableStateOf<ByteArray?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() {
        val b = gameBackend ?: return
        if (b.currentUserId() == null) runCatching { b.ensurePlayer("Oyuncu") }
        val p = runCatching { service?.getMe() }.getOrNull() ?: return
        profile = p
        avatarBytes = p.avatarPath?.let { runCatching { service?.downloadOwnAvatar(it) }.getOrNull() }
    }

    LaunchedEffect(Unit) { refresh() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && service != null) {
            scope.launch {
                busy = true
                notice = "Fotoğraf hazırlanıyor…"
                runCatching {
                    val bytes = withContext(Dispatchers.Default) { optimizeAvatar(context, uri) }
                    require(bytes.size <= 512 * 1024) { "avatar_too_large" }
                    service.uploadAvatar(bytes)
                    avatarBytes = bytes
                    refresh()
                }.onSuccess {
                    notice = "Fotoğraf yüksek kalite korunarak optimize edildi ve yüklendi."
                    SonHarfSoundFx.softNotify()
                }.onFailure {
                    notice = "Fotoğraf yüklenemedi. Yeniden deneyin."
                    SonHarfSoundFx.warning()
                }
                busy = false
            }
        }
    }

    val p = profile
    val wins = p?.wins ?: 0
    val losses = p?.losses ?: 0
    val matches = if ((p?.totalMatches ?: 0) > 0) p?.totalMatches ?: 0 else wins + losses
    val rate = if (matches == 0) 0 else wins * 100 / matches
    val avatarBitmap = remember(avatarBytes) { avatarBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(PBg),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Surface(shape = CircleShape, color = PPanel2, border = BorderStroke(1.dp, PStroke)) {
                    Text("‹", Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontSize = 24.sp, color = PText)
                }
                Surface(
                    modifier = Modifier.clickable { showEditor = true },
                    shape = CircleShape,
                    color = PPanel2,
                    border = BorderStroke(1.dp, PStroke)
                ) {
                    Text("✎", Modifier.padding(horizontal = 13.dp, vertical = 9.dp), fontSize = 18.sp, color = PCyan)
                }
            }

            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Surface(
                        modifier = Modifier.size(94.dp),
                        shape = CircleShape,
                        color = PPanel2,
                        border = BorderStroke(2.dp, if (p?.isVip == true) PGold else PPurple)
                    ) {
                        if (avatarBitmap != null) {
                            Image(avatarBitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text((p?.displayName ?: "O").take(1).uppercase(), fontSize = 38.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    Surface(
                        modifier = Modifier.size(30.dp).clickable(enabled = !busy) { picker.launch("image/*") },
                        shape = CircleShape,
                        color = Color(0xFF14223A),
                        border = BorderStroke(1.dp, PCyan)
                    ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("📷", fontSize = 13.sp) } }
                }
                Spacer(Modifier.height(8.dp))
                Text(p?.displayName ?: "Oyuncu", fontSize = 28.sp, fontWeight = FontWeight.Black, color = PText)
                Text(if (p?.isVip == true) "SON HARF USTASI  ♥" else "SON HARF OYUNCUSU  ♥", color = PMuted, fontSize = 10.sp)
                if (p?.identityLocked == true) {
                    Text("🔒 İsim ve cinsiyet sabit", color = PGold, fontSize = 9.sp)
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Seviye 23", fontSize = 9.sp, color = PText)
                    Spacer(Modifier.width(8.dp))
                    LinearProgressIndicator(progress = { .64f }, modifier = Modifier.weight(1f).height(5.dp), color = PPurple, trackColor = PPanel2)
                    Spacer(Modifier.width(8.dp))
                    Text("3.250 / 5.000", fontSize = 9.sp, color = PMuted)
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    ProfileMetric(wins.toString(), "Galibiyet", Modifier.weight(1f))
                    ProfileMetric(losses.toString(), "Mağlubiyet", Modifier.weight(1f))
                    ProfileMetric("%$rate", "Kazanma Oranı", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    ProfileMetric(matches.toString(), "Toplam Maç", Modifier.weight(1f))
                    ProfileMetric((p?.totalRounds ?: 0).toString(), "Toplam Round", Modifier.weight(1f))
                    ProfileMetric((p?.validWords ?: 0).toString(), "Toplam Kelime", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    ProfileMetric((p?.bestStreak ?: 0).toString(), "En Uzun Seri", Modifier.weight(1f))
                    ProfileMetric((p?.wordStorms ?: 0).toString(), "Söz Fırtınası", Modifier.weight(1f))
                    ProfileMetric((p?.rating ?: 1000).toString(), "Puan", Modifier.weight(1f))
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = PPanel), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, PStroke)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("PROFİL FOTOĞRAFINI GİZLE", color = PText, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Text("Açık olduğunda fotoğrafın diğer oyunculara gösterilmez.", color = PMuted, fontSize = 9.sp)
                    }
                    Switch(
                        checked = p?.avatarVisibility != "public",
                        onCheckedChange = { hidden ->
                            val s = service ?: return@Switch
                            scope.launch {
                                busy = true
                                runCatching { s.setPhotoHidden(hidden) }.onSuccess { profile = it }
                                    .onFailure { notice = "Gizlilik ayarı değiştirilemedi." }
                                busy = false
                            }
                        },
                        enabled = !busy
                    )
                }
            }
        }

        if (!notice.isNullOrBlank()) item {
            Surface(color = PPanel2, shape = RoundedCornerShape(12.dp)) {
                Text(notice!!, Modifier.fillMaxWidth().padding(10.dp), textAlign = TextAlign.Center, color = if (notice!!.contains("yüklenemedi")) PPink else PMuted, fontSize = 10.sp)
            }
        }

        item {
            Text("SON BAŞARILAR", fontSize = 11.sp, fontWeight = FontWeight.Black, color = PText)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("♛", "★", "✦", "♜", "✪").forEachIndexed { i, icon ->
                    Surface(modifier = Modifier.size(52.dp), shape = CircleShape, color = if (i < 3) Color(0xFF2B2112) else PPanel2, border = BorderStroke(1.dp, if (i < 3) PGold.copy(alpha=.65f) else PStroke)) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(icon, fontSize = 20.sp, color = if (i < 3) PGold else PMuted) }
                    }
                }
            }
        }
    }

    if (showEditor && p != null) {
        ProfileEditorDialog(
            profile = p,
            busy = busy,
            onDismiss = { showEditor = false },
            onChoosePhoto = { picker.launch("image/*") },
            onSaveIdentity = { name, gender, email ->
                val s = service ?: return@ProfileEditorDialog
                scope.launch {
                    busy = true
                    notice = null
                    runCatching { s.completeIdentity(name, gender, email) }
                        .onSuccess { profile = it; showEditor = false; notice = "Profil kimliği kaydedildi. İsim ve cinsiyet artık değiştirilemez." }
                        .onFailure { e ->
                            notice = when {
                                "email_already_used" in e.message.orEmpty() -> "Bu e-posta başka bir üyelikte kullanılıyor."
                                "identity_locked" in e.message.orEmpty() -> "İsim ve cinsiyet daha önce kilitlenmiş."
                                else -> "Profil bilgileri kaydedilemedi."
                            }
                        }
                    busy = false
                }
            }
        )
    }
}

@Composable
private fun ProfileEditorDialog(
    profile: ProductionProfileDto,
    busy: Boolean,
    onDismiss: () -> Unit,
    onChoosePhoto: () -> Unit,
    onSaveIdentity: (String, String, String) -> Unit,
) {
    var name by remember(profile.id) { mutableStateOf(if (profile.identityLocked) profile.displayName else "") }
    var email by remember(profile.id) { mutableStateOf(profile.accountEmail.orEmpty()) }
    var gender by remember(profile.id) { mutableStateOf(profile.gender.orEmpty()) }
    val locked = profile.identityLocked

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PROFİLİ DÜZENLE", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedButton(onClick = onChoosePhoto, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("📷 FOTOĞRAF SEÇ / DEĞİŞTİR") }
                OutlinedTextField(name, { name = it.take(24) }, label = { Text("Oyuncu adı") }, singleLine = true, enabled = !locked && !busy, modifier = Modifier.fillMaxWidth(), supportingText = { Text(if (locked) "🔒 Oyuncu adı değiştirilemez" else "Bir kez belirlenir ve sonra kilitlenir") })
                Text("Cinsiyet", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("erkek", "kadın", "diğer").forEach { value ->
                        FilterChip(selected = gender == value, onClick = { if (!locked) gender = value }, enabled = !locked && !busy, label = { Text(value.replaceFirstChar { it.uppercase() }) })
                    }
                }
                if (locked) Text("🔒 Cinsiyet değiştirilemez", color = PMuted, fontSize = 9.sp)
                OutlinedTextField(email, { email = it.trim().take(120) }, label = { Text("E-posta") }, singleLine = true, enabled = !locked && !busy, modifier = Modifier.fillMaxWidth(), supportingText = { Text(if (locked) "🔒 Üyelik e-postası sabit" else "Bir e-posta yalnızca bir üyelikte kullanılabilir") })
                Text("Fotoğraf: merkezden kare kırpılır, oran bozulmaz, en fazla 512×512 WebP olarak yüksek görsel kaliteyle optimize edilir.", color = PMuted, fontSize = 9.sp)
            }
        },
        confirmButton = {
            if (!locked) TextButton(onClick = { onSaveIdentity(name, gender, email) }, enabled = !busy && name.length >= 2 && gender.isNotBlank() && email.contains("@")) { Text("KAYDET VE KİLİTLE") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("KAPAT") } }
    )
}

@Composable
private fun ProfileMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier.height(76.dp), colors = CardDefaults.cardColors(containerColor = PPanel), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, PStroke)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = PText)
            Text(label, color = PMuted, fontSize = 8.sp, textAlign = TextAlign.Center)
        }
    }
}
