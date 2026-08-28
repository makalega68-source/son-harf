package com.sonharf.game

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.launch

@Composable
fun FinalProfileScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var blocked by remember { mutableStateOf<List<ProfileDto>>(emptyList()) }
    var leaders by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var sound by remember { mutableStateOf(SonHarfPreferences.soundEnabled(context)) }
    var vibration by remember { mutableStateOf(SonHarfPreferences.vibrationEnabled(context)) }
    var notifications by remember { mutableStateOf(SonHarfPreferences.notificationsEnabled(context)) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteBusy by remember { mutableStateOf(false) }
    var deleteNotice by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() {
        loading = true
        val b = backend
        if (b != null && b.currentUserId() != null) {
            profile = runCatching { b.getProfile(requireNotNull(b.currentUserId())) }.getOrNull()
            blocked = runCatching { b.getBlockedUsers() }.getOrDefault(emptyList())
            leaders = runCatching { b.getLeaderboard(20) }.getOrDefault(emptyList())
        }
        loading = false
    }

    LaunchedEffect(Unit) {
        SonHarfPreferences.syncSound(context)
        refresh()
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("OYUNCU PROFİLİ", fontSize = 26.sp, fontWeight = FontWeight.Black)
            Text("Kimliğin, gizliliğin ve oyun ayarların", color = SonHarfMuted)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SonHarfSurface),
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = .05f)),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ProfilePhotoAvatar(
                        avatarPath = profile?.avatarPath,
                        name = profile?.displayName ?: "Oyuncu",
                        size = 112.dp,
                        visible = true,
                        accent = SonHarfCyan,
                    )
                    Text(profile?.displayName ?: "OYUNCU PROFİLİ", fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (profile == null) "İlk maça girdiğinde oyuncu profilin hazırlanır." else if (profile?.isVip == true) "VIP OYUNCU" else "OYUNCU",
                        color = if (profile?.isVip == true) SonHarfGold else SonHarfMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        item {
            val wins = profile?.wins ?: 0
            val losses = profile?.losses ?: 0
            val matches = wins + losses
            val rate = if (matches == 0) 0 else wins * 100 / matches
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                FinalMetric(wins.toString(), "Galibiyet", Modifier.weight(1f))
                FinalMetric(losses.toString(), "Mağlubiyet", Modifier.weight(1f))
                FinalMetric("%$rate", "Kazanma", Modifier.weight(1f))
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("SES & BİLDİRİM", color = SonHarfCyan, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    SettingSwitch("Ses efektleri", "Yumuşak kalem ucu dokunuşu ve oyun sesleri", sound) {
                        sound = it
                        SonHarfPreferences.setSoundEnabled(context, it)
                        if (it) SonHarfSoundFx.tap()
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = .06f))
                    SettingSwitch("Titreşim", "Kısa ve hafif dokunsal geri bildirim", vibration) {
                        vibration = it
                        SonHarfPreferences.setVibrationEnabled(context, it)
                        if (it) SonHarfPreferences.hapticTap(context)
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = .06f))
                    SettingSwitch("Bildirimler", "Oyun daveti ve eşleşme bildirimlerine izin ver", notifications) {
                        notifications = it
                        SonHarfPreferences.setNotificationsEnabled(context, it)
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("PROFİL FOTOĞRAFI & GİZLİLİK", color = SonHarfCyan, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Text("Profil fotoğrafın oyun içinde oyuncu kimliğinin göründüğü alanlarda kullanılır. Engellediğin oyuncularla etkileşim sınırlandırılır.", color = SonHarfMuted, lineHeight = 20.sp)
                }
            }
        }

        if (blocked.isNotEmpty()) item {
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ENGELLENENLER", color = SonHarfCyan, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    blocked.forEach { p ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ProfilePhotoAvatar(p.avatarPath, p.displayName, 34.dp, visible = true, accent = SonHarfCyan)
                                Text(p.displayName, fontWeight = FontWeight.SemiBold)
                            }
                            TextButton(onClick = {
                                val b = backend ?: return@TextButton
                                scope.launch {
                                    runCatching { b.unblockUser(p.id) }
                                    refresh()
                                }
                            }) { Text("Engeli kaldır") }
                        }
                    }
                }
            }
        }

        if (leaders.isNotEmpty()) item {
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(sh("OYUNCU SIRALAMASI", "PLAYER RANKING"), color = SonHarfGold, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    leaders.take(10).forEachIndexed { index, row ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${index + 1}.", modifier = Modifier.width(24.dp), color = SonHarfMuted)
                            ProfilePhotoAvatar(
                                avatarPath = row.profile.avatarPath,
                                name = row.profile.displayName,
                                size = 34.dp,
                                visible = true,
                                accent = if (row.profile.id == profile?.id) SonHarfCyan else SonHarfPurple,
                            )
                            Text(
                                row.profile.displayName,
                                modifier = Modifier.weight(1f),
                                fontWeight = if (row.profile.id == profile?.id) FontWeight.Black else FontWeight.Medium,
                            )
                            Text("${row.profile.wins} G • %${row.winRate}", color = SonHarfMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("GİZLİLİK & HAKKINDA", color = SonHarfCyan, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Text("Son Harf; maç, sohbet, arkadaşlık, raporlama ve profil verilerini çevrimiçi oyun özelliklerini çalıştırmak için kullanır. Kritik skor ve oyun kuralları sunucuda doğrulanır.", color = SonHarfMuted, lineHeight = 20.sp)
                    Text("Sürüm ${BuildConfig.VERSION_NAME} • Android", color = SonHarfMuted, fontSize = 11.sp)
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1116)), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Color(0xFF7A2634))) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("HESAP YÖNETİMİ", color = Color(0xFFFF8394), fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Text("Hesabını silersen üyeliğin ve hesabına bağlı oyun verileri kalıcı olarak silinir. Bu işlem geri alınamaz.", color = SonHarfMuted, fontSize = 11.sp, lineHeight = 18.sp)
                    OutlinedButton(
                        onClick = { showDeleteDialog = true; deleteNotice = null },
                        enabled = !deleteBusy,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF8394)),
                        border = BorderStroke(1.dp, Color(0xFF7A2634)),
                    ) { Text("HESABIMI SİL", fontWeight = FontWeight.Black) }
                    if (!deleteNotice.isNullOrBlank()) Text(deleteNotice!!, color = Color(0xFFFFB4BE), fontSize = 10.sp)
                }
            }
        }

        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!deleteBusy) showDeleteDialog = false },
            title = { Text("Hesabı kalıcı olarak sil?") },
            text = { Text("Profilin, ilerlemen ve hesabına bağlı veriler silinecek. Bu işlemi geri alamazsın.") },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }, enabled = !deleteBusy) { Text("VAZGEÇ") }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            deleteBusy = true
                            runCatching { AccountDeletion.deleteCurrentAccount() }
                                .onSuccess {
                                    showDeleteDialog = false
                                    (context as? Activity)?.recreate()
                                }
                                .onFailure {
                                    deleteNotice = "Hesap silinemedi. Bağlantını kontrol edip tekrar dene."
                                    showDeleteDialog = false
                                }
                            deleteBusy = false
                        }
                    },
                    enabled = !deleteBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3263B)),
                ) { Text(if (deleteBusy) "SİLİNİYOR…" else "KALICI OLARAK SİL") }
            },
        )
    }
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = SonHarfMuted, fontSize = 11.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun FinalMetric(value: String, label: String, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Black, fontSize = 20.sp)
            Text(label, color = SonHarfMuted, fontSize = 10.sp)
        }
    }
}
