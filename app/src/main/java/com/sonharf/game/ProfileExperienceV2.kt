package com.sonharf.game

import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.AchievementProgressDto
import com.sonharf.game.data.CompetitiveSeasonDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.getAchievements
import com.sonharf.game.data.getCompetitiveSeason
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

@Serializable
private data class ProfileV2Dto(
    val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_path") val avatarPath: String? = null,
    @SerialName("avatar_visibility") val avatarVisibility: String = "visible",
    @SerialName("is_vip") val isVip: Boolean = false,
    val diamonds: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    @SerialName("total_matches") val totalMatches: Int = 0,
    @SerialName("total_rounds") val totalRounds: Int = 0,
    @SerialName("rounds_won") val roundsWon: Int = 0,
    @SerialName("valid_words") val validWords: Int = 0,
    @SerialName("best_streak") val bestStreak: Int = 0,
    @SerialName("word_storms") val wordStorms: Int = 0,
    val rating: Int = 1000,
    val gender: String? = null,
    @SerialName("identity_locked") val identityLocked: Boolean = false,
)

private object ProfilePhotoStorageV2 {
    private val http = HttpClient(OkHttp)

    private suspend fun authHeaders(): Pair<String, String> {
        val session = SupabaseProvider.client.auth.currentSessionOrNull() ?: error("not_authenticated")
        return session.accessToken to BuildConfig.SUPABASE_KEY
    }

    suspend fun upload(bytes: ByteArray, contentType: String): String {
        val uid = SupabaseProvider.client.auth.currentUserOrNull()?.id ?: error("not_authenticated")
        val ext = when {
            contentType.contains("png", true) -> "png"
            contentType.contains("webp", true) -> "webp"
            else -> "jpg"
        }
        val path = "$uid/avatar-${UUID.randomUUID()}.$ext"
        val (token, apiKey) = authHeaders()
        val response = http.post("${BuildConfig.SUPABASE_URL}/storage/v1/object/profile-photos/$path") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header("apikey", apiKey)
            header("x-upsert", "true")
            contentType(ContentType.parse(contentType.ifBlank { "image/jpeg" }))
            setBody(bytes)
        }
        if (!response.status.isSuccess()) error("avatar_upload_failed_${response.status.value}")
        return path
    }

    suspend fun download(path: String): ByteArray {
        val (token, apiKey) = authHeaders()
        val response = http.get("${BuildConfig.SUPABASE_URL}/storage/v1/object/authenticated/profile-photos/$path") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header("apikey", apiKey)
        }
        if (!response.status.isSuccess()) error("avatar_download_failed_${response.status.value}")
        return response.bodyAsBytes()
    }
}

private suspend fun loadProfileV2(): ProfileV2Dto? {
    val uid = SupabaseProvider.client.auth.currentUserOrNull()?.id ?: return null
    return SupabaseProvider.client.from("profiles").select { filter { eq("id", uid) } }.decodeList<ProfileV2Dto>().firstOrNull()
}

private suspend fun saveAvatarV2(path: String): ProfileV2Dto =
    SupabaseProvider.client.postgrest.rpc("set_avatar_path", buildJsonObject { put("p_path", path) }).decodeSingle()

private suspend fun setAvatarHiddenV2(hidden: Boolean): ProfileV2Dto =
    SupabaseProvider.client.postgrest.rpc("set_avatar_visibility", buildJsonObject { put("p_hidden", hidden) }).decodeSingle()

private suspend fun completeLegacyIdentityV2(name: String, gender: String): ProfileV2Dto =
    SupabaseProvider.client.postgrest.rpc(
        "complete_profile_identity_v2",
        buildJsonObject { put("p_display_name", name.trim()); put("p_gender", gender) },
    ).decodeSingle()

@Composable
fun ProfileExperienceV2Screen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var profile by remember { mutableStateOf<ProfileV2Dto?>(null) }
    var season by remember { mutableStateOf<CompetitiveSeasonDto?>(null) }
    var achievements by remember { mutableStateOf<List<AchievementProgressDto>>(emptyList()) }
    var avatarBytes by remember { mutableStateOf<ByteArray?>(null) }
    var loading by remember { mutableStateOf(true) }
    var photoEditor by remember { mutableStateOf(false) }
    var legacyIdentity by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() {
        loading = true
        profile = runCatching { loadProfileV2() }.getOrNull()
        avatarBytes = profile?.avatarPath?.let { runCatching { ProfilePhotoStorageV2.download(it) }.getOrNull() }
        season = runCatching { backend?.getCompetitiveSeason() }.getOrNull()
        achievements = runCatching { backend?.getAchievements().orEmpty() }.getOrDefault(emptyList())
        loading = false
    }

    LaunchedEffect(Unit) { if (SupabaseProvider.configured) refresh() else loading = false }

    val p = profile
    val wins = p?.wins ?: 0
    val losses = p?.losses ?: 0
    val totalMatches = p?.totalMatches?.takeIf { it > 0 } ?: wins + losses
    val winRate = if (totalMatches == 0) 0 else wins * 100 / totalMatches

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    ProfileAvatarV2(avatarBytes, p?.displayName ?: "O", 112)
                    Surface(
                        modifier = Modifier.size(42.dp).clickable { photoEditor = true },
                        shape = CircleShape,
                        color = SonHarfPurple,
                        border = BorderStroke(2.dp, SonHarfSurface),
                    ) { Box(contentAlignment = Alignment.Center) { Text("✎", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black) } }
                    p?.gender?.let { gender ->
                        val female = gender.trim().lowercase() in setOf("kadın", "kadin", "female", "woman")
                        val male = gender.trim().lowercase() in setOf("erkek", "male", "man")
                        if (female || male) {
                            Surface(
                                modifier = Modifier.align(Alignment.BottomStart).size(42.dp),
                                shape = CircleShape,
                                color = if (female) Color(0xFFFF76A8) else Color(0xFF439EF2),
                                border = BorderStroke(2.dp, Color.White),
                                shadowElevation = 3.dp,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(if (female) "♀" else "♂", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(p?.displayName ?: sh("Oyuncu", "Player"), fontSize = 29.sp, fontWeight = FontWeight.Black)
                Text(
                    if (p?.isVip == true) sh("SON HARF VIP", "SON HARF VIP") else sh("SON HARF OYUNCUSU", "SON HARF PLAYER"),
                    color = if (p?.isVip == true) SonHarfGold else SonHarfMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                p?.gender?.let { Text(genderLabelV2(it), color = SonHarfMuted, fontSize = 14.sp) }
                if (p != null && !p.identityLocked) {
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { legacyIdentity = true }, shape = RoundedCornerShape(16.dp)) {
                        Text(sh("BİR KEZLİK KİMLİĞİ TAMAMLA", "COMPLETE IDENTITY ONCE"), fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                    Text(sh("Eski hesaplarda yalnız bir kez oyuncu adı ve cinsiyet doğrulanır; e-posta tekrar istenmez.", "Legacy accounts confirm name and gender once; email is not requested again."), color = SonHarfMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            }
        }

        if (!notice.isNullOrBlank()) item {
            Surface(color = SonHarfSurface2, shape = RoundedCornerShape(14.dp)) {
                Text(notice!!, Modifier.fillMaxWidth().padding(12.dp), color = SonHarfText, fontSize = 14.sp, textAlign = TextAlign.Center)
            }
        }

        item {
            Text(sh("İSTATİSTİKLERİM", "MY STATS"), fontSize = 17.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProfileMetricV2(wins.toString(), sh("Galibiyet", "Wins"), Modifier.weight(1f))
                    ProfileMetricV2(losses.toString(), sh("Mağlubiyet", "Losses"), Modifier.weight(1f))
                    ProfileMetricV2("%$winRate", sh("Kazanma", "Win Rate"), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProfileMetricV2(totalMatches.toString(), sh("Maç", "Matches"), Modifier.weight(1f))
                    ProfileMetricV2((p?.validWords ?: 0).toString(), sh("Kelime", "Words"), Modifier.weight(1f))
                    ProfileMetricV2((p?.bestStreak ?: 0).toString(), sh("En iyi seri", "Best streak"), Modifier.weight(1f))
                }
            }
        }

        season?.let { s ->
            item {
                val seasonLeague = ratingLeagueProgress(s.rating)
                val daysLeft = runCatching {
                    kotlin.math.ceil(
                        ((java.time.Instant.parse(s.endsAt).toEpochMilli() - System.currentTimeMillis())
                            .coerceAtLeast(0L)) / 86_400_000.0
                    ).toInt()
                }.getOrDefault(0)

                Text(sh("REKABET SEZONU", "COMPETITIVE SEASON"), fontSize = 18.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(10.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = SonHarfSurface),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, SonHarfGold.copy(alpha = .34f)),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(15.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(if (SonHarfUiState.isEnglish) s.nameEn else s.nameTr, fontWeight = FontWeight.Black, fontSize = 18.sp)
                                Text(
                                    if (s.seasonRank > 0)
                                        sh("#${s.seasonRank} • ${s.playerCount} oyuncu", "#${s.seasonRank} • ${s.playerCount} players")
                                    else
                                        sh("İlk PvP maçınla sıralamaya gir", "Enter the ranking with your first PvP match"),
                                    color = SonHarfMuted,
                                    fontSize = 11.sp,
                                )
                            }
                            Surface(shape = RoundedCornerShape(13.dp), color = SonHarfGold.copy(alpha = .13f)) {
                                Text("${s.leagueName} • ${s.rating}", Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = SonHarfGold, fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                        }
                        LinearProgressIndicator(
                            progress = { seasonLeague.progress },
                            modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                            color = SonHarfGold,
                            trackColor = SonHarfSurface2,
                        )
                        Text(
                            if (s.nextRating == null)
                                sh("En üst sezon ligi • $daysLeft gün kaldı", "Top season league • $daysLeft days left")
                            else
                                sh("Sonraki lige ${s.pointsToNext} puan • $daysLeft gün kaldı", "${s.pointsToNext} points to next league • $daysLeft days left"),
                            color = SonHarfMuted,
                            fontSize = 10.sp,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            ProfileMetricV2(s.wins.toString(), sh("Sezon G", "Season W"), Modifier.weight(1f))
                            ProfileMetricV2(s.losses.toString(), sh("Sezon M", "Season L"), Modifier.weight(1f))
                            ProfileMetricV2(s.peakRating.toString(), sh("Zirve", "Peak"), Modifier.weight(1f))
                        }
                        val honor = if (SonHarfUiState.isEnglish) s.latestHonorEn else s.latestHonorTr
                        if (!honor.isNullOrBlank()) {
                            Surface(shape = RoundedCornerShape(13.dp), color = SonHarfPurple.copy(alpha = .11f)) {
                                Text("🏅 $honor", Modifier.fillMaxWidth().padding(9.dp), color = SonHarfPurple, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }

        item {
            val unlockedCount = achievements.count { it.unlocked }
            Text(sh("BAŞARILARIM", "MY ACHIEVEMENTS") + " $unlockedCount/${achievements.size}", fontSize = 18.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            if (achievements.isEmpty()) {
                Text(sh("Başarımlar yükleniyor…", "Loading achievements…"), color = SonHarfMuted, fontSize = 13.sp)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    achievements.forEach { a ->
                        AchievementLineV2(
                            icon = a.icon,
                            title = if (SonHarfUiState.isEnglish) a.titleEn else a.titleTr,
                            unlocked = a.unlocked,
                            progress = "${a.currentValue.coerceAtMost(a.target)} / ${a.target}",
                            description = if (SonHarfUiState.isEnglish) a.descriptionEn else a.descriptionTr,
                            rewardCoin = a.rewardCoin,
                        )
                    }
                }
            }
        }

        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
    }

    if (photoEditor && p != null) {
        PhotoEditorDialogV2(
            avatar = avatarBytes,
            name = p.displayName,
            hidden = p.avatarVisibility == "hidden",
            busy = busy,
            onDismiss = { if (!busy) photoEditor = false },
            onPhoto = { uri ->
                scope.launch {
                    busy = true
                    runCatching {
                        val bytes = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("photo_read_failed") }
                        require(bytes.isNotEmpty()) { "photo_size" }
                        val type = context.contentResolver.getType(uri) ?: "image/jpeg"
                        require(type.startsWith("image/")) { "photo_type" }
                        val compact = ProfilePhotoRuntime.compactForUpload(bytes)
                        val path = ProfilePhotoStorageV2.upload(compact, "image/webp")
                        saveAvatarV2(path)
                        profile = setAvatarHiddenV2(false)
                        avatarBytes = compact
                    }.onSuccess { notice = sh("Profil fotoğrafı güncellendi.", "Profile photo updated.") }
                        .onFailure { notice = sh("Fotoğraf yüklenemedi. JPG/PNG/WEBP ve 8 MB sınırını kontrol et.", "Photo could not be uploaded. Check JPG/PNG/WEBP and the 8 MB limit.") }
                    busy = false
                }
            },
            onHidden = { hidden ->
                scope.launch {
                    busy = true
                    runCatching { setAvatarHiddenV2(hidden) }
                        .onSuccess { profile = it; notice = if (hidden) sh("Fotoğraf gizlendi.", "Photo hidden.") else sh("Fotoğraf görünür yapıldı.", "Photo made visible.") }
                        .onFailure { notice = sh("Fotoğraf gizlilik ayarı kaydedilemedi.", "Photo privacy setting could not be saved.") }
                    busy = false
                }
            },
        )
    }

    if (legacyIdentity && p != null && !p.identityLocked) {
        LegacyIdentityDialogV2(
            initialName = p.displayName.takeUnless { it.startsWith("Oyuncu-") }.orEmpty(),
            busy = busy,
            onDismiss = { if (!busy) legacyIdentity = false },
            onSave = { name, gender ->
                scope.launch {
                    busy = true
                    runCatching { completeLegacyIdentityV2(name, gender) }
                        .onSuccess { profile = it; legacyIdentity = false; notice = sh("Oyuncu adı ve cinsiyet kalıcı olarak kaydedildi.", "Player name and gender were saved permanently.") }
                        .onFailure { notice = sh("Bilgiler kaydedilemedi. Oyuncu adı 2-24 karakter olmalı.", "Could not save. Player name must be 2-24 characters.") }
                    busy = false
                }
            },
        )
    }
}

@Composable
private fun PhotoEditorDialogV2(
    avatar: ByteArray?, name: String, hidden: Boolean, busy: Boolean,
    onDismiss: () -> Unit, onPhoto: (Uri) -> Unit, onHidden: (Boolean) -> Unit,
) {
    var hide by remember(hidden) { mutableStateOf(hidden) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { it?.let(onPhoto) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(sh("FOTOĞRAFI DÜZENLE", "EDIT PHOTO"), fontWeight = FontWeight.Black, fontSize = 23.sp) },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    ProfileAvatarV2(avatar, name, 118)
                    Surface(modifier = Modifier.size(42.dp), shape = CircleShape, color = SonHarfPurple) { Box(contentAlignment = Alignment.Center) { Text("✎", color = Color.White, fontSize = 21.sp) } }
                }
                OutlinedButton(onClick = { picker.launch("image/*") }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Text(sh("✎  FOTOĞRAF EKLE / DEĞİŞTİR", "✎  ADD / CHANGE PHOTO"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(sh("Fotoğrafı gizle", "Hide photo"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(sh("Açıkken fotoğraf diğer oyunculara gösterilmez.", "When enabled, other players cannot see the photo."), color = SonHarfMuted, fontSize = 13.sp)
                    }
                    Switch(checked = hide, onCheckedChange = { hide = it; onHidden(it) }, enabled = !busy)
                }
                Text(sh("Her boyut kabul edilir • otomatik yüksek kaliteli WEBP küçültme", "Any image size • automatic high-quality WEBP compression"), color = SonHarfMuted, fontSize = 13.sp)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text(sh("BİTTİ", "DONE"), fontSize = 17.sp, fontWeight = FontWeight.Black) } },
    )
}

@Composable
private fun LegacyIdentityDialogV2(initialName: String, busy: Boolean, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    var gender by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(sh("KİMLİĞİ BİR KEZ TAMAMLA", "COMPLETE IDENTITY ONCE"), fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(sh("Bu ekran yalnız eski hesaplar içindir. E-posta tekrar istenmez; kayıt sonrası bu bilgiler değiştirilemez.", "This is only for legacy accounts. Email is not requested; these values are locked after saving."), fontSize = 14.sp)
                OutlinedTextField(name, { name = it.take(24) }, label = { Text(sh("Oyuncu adı", "Player name")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("erkek" to "Erkek", "kadın" to "Kadın", "diğer" to "Diğer").forEach { (value, label) ->
                        FilterChip(selected = gender == value, onClick = { gender = value }, label = { Text(label) }, modifier = Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(name, gender) }, enabled = !busy && name.trim().length >= 2 && gender.isNotBlank()) { Text(sh("KALICI KAYDET", "SAVE PERMANENTLY")) } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text(sh("ŞİMDİ DEĞİL", "NOT NOW")) } },
    )
}

@Composable
private fun ProfileAvatarV2(bytes: ByteArray?, name: String, size: Int) {
    val bitmap = remember(bytes) { bytes?.let { runCatching { BitmapFactory.decodeByteArray(it, 0, it.size) }.getOrNull() } }
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(Brush.sweepGradient(listOf(SonHarfPurple, SonHarfCyan, SonHarfPink, SonHarfPurple))).padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
        else Box(Modifier.fillMaxSize().clip(CircleShape).background(SonHarfSurface), contentAlignment = Alignment.Center) { Text(name.take(1).uppercase(), fontSize = (size / 3).sp, fontWeight = FontWeight.Black) }
    }
}

@Composable
private fun ProfileMetricV2(value: String, label: String, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = SonHarfSurface2) {
        Column(Modifier.padding(vertical = 12.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 21.sp, fontWeight = FontWeight.Black)
            Text(label, color = SonHarfMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun AchievementLineV2(
    icon: String,
    title: String,
    unlocked: Boolean,
    progress: String,
    description: String,
    rewardCoin: Int,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (unlocked) SonHarfGold.copy(alpha = .13f) else SonHarfSurface2,
        border = BorderStroke(1.dp, if (unlocked) SonHarfGold.copy(alpha=.45f) else SonHarfMuted.copy(alpha=.12f)),
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 25.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(description, color = SonHarfMuted, fontSize = 11.sp)
                Text(if (unlocked) sh("Tamamlandı", "Completed") else progress, color = if (unlocked) SonHarfGold else SonHarfMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (unlocked) "✓" else "○", color = if (unlocked) SonHarfGreen else SonHarfMuted, fontSize = 22.sp, fontWeight = FontWeight.Black)
                if (rewardCoin > 0) Text("+$rewardCoin SC", color = SonHarfGold, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

private fun genderLabelV2(value: String): String = when (value.lowercase()) {
    "erkek" -> "Erkek"
    "kadın", "kadin" -> "Kadın"
    "diğer", "diger" -> "Diğer"
    else -> value
}
