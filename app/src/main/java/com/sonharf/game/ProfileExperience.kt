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
import com.sonharf.game.data.SupabaseProvider
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
private data class EditableProfileDto(
    val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_path") val avatarPath: String? = null,
    @SerialName("avatar_visibility") val avatarVisibility: String = "hidden",
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
    @SerialName("account_email") val accountEmail: String? = null,
    val gender: String? = null,
    @SerialName("identity_locked") val identityLocked: Boolean = false,
)

private object ProfilePhotoStorage {
    private val http = HttpClient(OkHttp)

    private suspend fun authHeaders(): Pair<String, String> {
        val session = SupabaseProvider.client.auth.currentSessionOrNull()
            ?: error("not_authenticated")
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

private suspend fun loadEditableProfile(): EditableProfileDto? {
    val uid = SupabaseProvider.client.auth.currentUserOrNull()?.id ?: return null
    return SupabaseProvider.client.from("profiles")
        .select { filter { eq("id", uid) } }
        .decodeList<EditableProfileDto>()
        .firstOrNull()
}

private suspend fun completeIdentity(displayName: String, gender: String, email: String): EditableProfileDto =
    SupabaseProvider.client.postgrest.rpc(
        "complete_profile_identity",
        buildJsonObject {
            put("p_display_name", displayName.trim())
            put("p_gender", gender)
            put("p_email", email.trim())
        }
    ).decodeSingle()

private suspend fun saveAvatarPath(path: String): EditableProfileDto =
    SupabaseProvider.client.postgrest.rpc(
        "set_avatar_path",
        buildJsonObject { put("p_path", path) }
    ).decodeSingle()

private suspend fun setAvatarHidden(hidden: Boolean): EditableProfileDto =
    SupabaseProvider.client.postgrest.rpc(
        "set_avatar_visibility",
        buildJsonObject { put("p_hidden", hidden) }
    ).decodeSingle()

@Composable
fun ProfileExperienceScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<EditableProfileDto?>(null) }
    var avatarBytes by remember { mutableStateOf<ByteArray?>(null) }
    var loading by remember { mutableStateOf(true) }
    var editOpen by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() {
        loading = true
        profile = runCatching { loadEditableProfile() }.getOrNull()
        avatarBytes = profile?.avatarPath?.let { path -> runCatching { ProfilePhotoStorage.download(path) }.getOrNull() }
        loading = false
    }

    LaunchedEffect(Unit) {
        if (SupabaseProvider.configured) {
            val backend = com.sonharf.game.data.OnlineGameBackend()
            if (backend.currentUserId() == null) runCatching { backend.ensurePlayer(sh("Oyuncu", "Player")) }
            refresh()
        } else loading = false
    }

    val p = profile
    val wins = p?.wins ?: 0
    val losses = p?.losses ?: 0
    val totalMatches = p?.totalMatches?.takeIf { it > 0 } ?: (wins + losses)
    val winRate = if (totalMatches <= 0) 0 else wins * 100 / totalMatches

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Surface(
                    modifier = Modifier.clickable { editOpen = true },
                    shape = CircleShape,
                    color = SonHarfSurface2,
                    border = BorderStroke(1.dp, SonHarfMuted.copy(alpha = .18f))
                ) {
                    Text("✎", Modifier.padding(horizontal = 14.dp, vertical = 10.dp), color = SonHarfCyan, fontSize = 18.sp)
                }
            }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                ProfileAvatar(avatarBytes, p?.displayName ?: "O", 100)
                Spacer(Modifier.height(10.dp))
                Text(p?.displayName ?: sh("Oyuncu", "Player"), fontSize = 27.sp, fontWeight = FontWeight.Black)
                Text(
                    if (p?.isVip == true) sh("SON HARF USTASI  ◆", "SON HARF MASTER  ◆") else sh("SON HARF OYUNCUSU", "SON HARF PLAYER"),
                    color = SonHarfMuted,
                    fontSize = 10.sp
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    if (p?.identityLocked == true) sh("Kimlik bilgileri doğrulandı", "Identity details locked") else sh("Profili tamamlamak için düzenle", "Edit to complete profile"),
                    color = if (p?.identityLocked == true) SonHarfGreen else SonHarfGold,
                    fontSize = 10.sp
                )
            }
        }

        if (!notice.isNullOrBlank()) item {
            Surface(color = SonHarfSurface2, shape = RoundedCornerShape(12.dp)) {
                Text(notice!!, Modifier.fillMaxWidth().padding(10.dp), color = SonHarfMuted, fontSize = 10.sp, textAlign = TextAlign.Center)
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProfileMetric(wins.toString(), sh("Galibiyet", "Wins"), Modifier.weight(1f))
                    ProfileMetric(losses.toString(), sh("Mağlubiyet", "Losses"), Modifier.weight(1f))
                    ProfileMetric("%$winRate", sh("Kazanma", "Win Rate"), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProfileMetric(totalMatches.toString(), sh("Toplam Maç", "Matches"), Modifier.weight(1f))
                    ProfileMetric((p?.totalRounds ?: 0).toString(), sh("Toplam Round", "Rounds"), Modifier.weight(1f))
                    ProfileMetric((p?.validWords ?: 0).toString(), sh("Toplam Kelime", "Words"), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProfileMetric((p?.bestStreak ?: 0).toString(), sh("En Uzun Seri", "Best Streak"), Modifier.weight(1f))
                    ProfileMetric((p?.wordStorms ?: 0).toString(), sh("Söz Fırtınası", "Word Storms"), Modifier.weight(1f))
                    ProfileMetric((p?.rating ?: 1000).toString(), sh("En Yüksek Puan", "Rating"), Modifier.weight(1f))
                }
            }
        }

        item {
            Text(sh("SON BAŞARILAR", "LATEST ACHIEVEMENTS"), fontWeight = FontWeight.Black, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AchievementBadge("♛", sh("Usta", "Master"), wins >= 10, Modifier.weight(1f))
                AchievementBadge("★", sh("Seri", "Streak"), (p?.bestStreak ?: 0) >= 5, Modifier.weight(1f))
                AchievementBadge("✦", sh("Fırtına", "Storm"), (p?.wordStorms ?: 0) >= 1, Modifier.weight(1f))
                AchievementBadge("♜", sh("Düello", "Duel"), totalMatches >= 25, Modifier.weight(1f))
                AchievementBadge("✪", sh("Elit", "Elite"), (p?.rating ?: 1000) >= 1200, Modifier.weight(1f))
            }
        }

        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
    }

    if (editOpen && p != null) {
        ProfileEditDialog(
            initial = p,
            initialAvatar = avatarBytes,
            busy = busy,
            onDismiss = { if (!busy) editOpen = false },
            onSaveIdentity = { name, gender, email ->
                scope.launch {
                    busy = true
                    runCatching { completeIdentity(name, gender, email) }
                        .onSuccess {
                            profile = it
                            notice = sh("Profil bilgileri kaydedildi ve kimlik alanları kilitlendi.", "Profile identity saved and locked.")
                        }
                        .onFailure { notice = identityError(it.message.orEmpty()) }
                    busy = false
                }
            },
            onPhotoSelected = { uri ->
                scope.launch {
                    busy = true
                    runCatching {
                        val bytes = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("photo_read_failed") }
                        require(bytes.isNotEmpty() && bytes.size <= 8 * 1024 * 1024) { "photo_size" }
                        val type = context.contentResolver.getType(uri) ?: "image/jpeg"
                        require(type.startsWith("image/")) { "photo_type" }
                        val path = ProfilePhotoStorage.upload(bytes, type)
                        profile = saveAvatarPath(path)
                        avatarBytes = bytes
                    }.onSuccess {
                        notice = sh("Profil fotoğrafı yüklendi.", "Profile photo uploaded.")
                    }.onFailure {
                        notice = photoError(it.message.orEmpty())
                    }
                    busy = false
                }
            },
            onPrivacyChange = { hidden ->
                scope.launch {
                    busy = true
                    runCatching { setAvatarHidden(hidden) }
                        .onSuccess { profile = it; notice = if (hidden) sh("Fotoğraf gizlendi.", "Photo hidden.") else sh("Fotoğraf görünürlüğü açıldı.", "Photo visibility enabled.") }
                        .onFailure { notice = sh("Gizlilik ayarı kaydedilemedi.", "Privacy setting could not be saved.") }
                    busy = false
                }
            }
        )
    }
}

@Composable
private fun ProfileEditDialog(
    initial: EditableProfileDto,
    initialAvatar: ByteArray?,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSaveIdentity: (String, String, String) -> Unit,
    onPhotoSelected: (Uri) -> Unit,
    onPrivacyChange: (Boolean) -> Unit,
) {
    var name by remember(initial.id) { mutableStateOf(initial.displayName) }
    var email by remember(initial.id) { mutableStateOf(initial.accountEmail.orEmpty()) }
    var gender by remember(initial.id) { mutableStateOf(initial.gender ?: "erkek") }
    var genderMenu by remember { mutableStateOf(false) }
    var hidden by remember(initial.avatarVisibility) { mutableStateOf(initial.avatarVisibility == "hidden") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let(onPhotoSelected) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(sh("PROFİLİ DÜZENLE", "EDIT PROFILE"), fontWeight = FontWeight.Black) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.heightIn(max = 520.dp)) {
                item {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        ProfileAvatar(initialAvatar, initial.displayName, 88)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { picker.launch("image/*") }, enabled = !busy) {
                            Text(sh("FOTOĞRAF SEÇ / DEĞİŞTİR", "CHOOSE / CHANGE PHOTO"))
                        }
                        Text(sh("JPG, PNG veya WEBP • en fazla 8 MB", "JPG, PNG or WEBP • max 8 MB"), color = SonHarfMuted, fontSize = 9.sp)
                    }
                }
                item {
                    if (!initial.identityLocked) {
                        OutlinedTextField(name, { name = it.take(24) }, label = { Text(sh("Kullanıcı adı", "Display name")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(email, { email = it.take(120) }, label = { Text("E-mail") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = { genderMenu = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(sh("Cinsiyet: ${genderLabel(gender)}", "Gender: ${genderLabelEn(gender)}"))
                            }
                            DropdownMenu(expanded = genderMenu, onDismissRequest = { genderMenu = false }) {
                                listOf("erkek", "kadın", "diğer").forEach { value ->
                                    DropdownMenuItem(text = { Text(if (SonHarfUiState.isEnglish) genderLabelEn(value) else genderLabel(value)) }, onClick = { gender = value; genderMenu = false })
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(sh("Bu bilgiler ilk kayıttan sonra değiştirilemez.", "These identity fields cannot be changed after first save."), color = SonHarfGold, fontSize = 10.sp)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { onSaveIdentity(name.trim(), gender, email.trim()) },
                            enabled = !busy && name.trim().length in 2..24 && email.contains("@"),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(sh("KİMLİK BİLGİLERİNİ KAYDET", "SAVE IDENTITY")) }
                    } else {
                        Surface(color = SonHarfSurface2, shape = RoundedCornerShape(12.dp)) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(initial.displayName, fontWeight = FontWeight.Bold)
                                Text(initial.accountEmail ?: "—", color = SonHarfMuted, fontSize = 11.sp)
                                Text(if (SonHarfUiState.isEnglish) genderLabelEn(initial.gender.orEmpty()) else genderLabel(initial.gender.orEmpty()), color = SonHarfMuted, fontSize = 11.sp)
                                Text(sh("Kimlik alanları kilitli; fotoğraf ve gizlilik ayarları değiştirilebilir.", "Identity fields are locked; photo and privacy can still be changed."), color = SonHarfGold, fontSize = 9.sp)
                            }
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(sh("Fotoğrafı gizle", "Hide profile photo"), fontWeight = FontWeight.SemiBold)
                            Text(sh("Açıkken fotoğraf yalnızca izin verilen erişim kurallarına göre gösterilir.", "When hidden, your photo is not shown to other players."), color = SonHarfMuted, fontSize = 9.sp)
                        }
                        Switch(checked = hidden, onCheckedChange = { value -> hidden = value; onPrivacyChange(value) }, enabled = !busy)
                    }
                }
                if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text(sh("BİTTİ", "DONE")) } }
    )
}

@Composable
private fun ProfileAvatar(bytes: ByteArray?, name: String, sizeDp: Int) {
    val bitmap = remember(bytes) {
        bytes?.let { runCatching { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }.getOrNull() }
    }
    Box(
        Modifier.size(sizeDp.dp).clip(CircleShape)
            .background(Brush.linearGradient(listOf(SonHarfGold, SonHarfPurple, SonHarfCyan)))
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = sh("Profil fotoğrafı", "Profile photo"), modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
        } else {
            Box(Modifier.fillMaxSize().clip(CircleShape).background(SonHarfSurface2), contentAlignment = Alignment.Center) {
                Text(name.take(1).uppercase(), fontWeight = FontWeight.Black, fontSize = (sizeDp * .40f).sp)
            }
        }
    }
}

@Composable
private fun ProfileMetric(value: String, label: String, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, SonHarfMuted.copy(alpha = .12f))) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 21.sp, fontWeight = FontWeight.Black)
            Text(label, color = SonHarfMuted, fontSize = 8.sp, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

@Composable
private fun AchievementBadge(icon: String, label: String, unlocked: Boolean, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(58.dp),
            shape = CircleShape,
            color = if (unlocked) SonHarfGold.copy(alpha = .14f) else SonHarfSurface2,
            border = BorderStroke(1.dp, if (unlocked) SonHarfGold.copy(alpha = .7f) else SonHarfMuted.copy(alpha = .16f))
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(icon, color = if (unlocked) SonHarfGold else SonHarfMuted, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(label, color = if (unlocked) SonHarfText else SonHarfMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

private fun genderLabel(value: String) = when (value.lowercase()) {
    "erkek" -> "Erkek"
    "kadın", "kadin" -> "Kadın"
    else -> "Diğer"
}

private fun genderLabelEn(value: String) = when (value.lowercase()) {
    "erkek" -> "Male"
    "kadın", "kadin" -> "Female"
    else -> "Other"
}

private fun identityError(raw: String): String = when {
    "identity_locked" in raw -> sh("Kimlik bilgileri daha önce kaydedilmiş; değiştirilemez.", "Identity fields were already saved and cannot be changed.")
    "invalid_display_name" in raw -> sh("Kullanıcı adı 2–24 karakter olmalı.", "Display name must be 2–24 characters.")
    "invalid_email" in raw -> sh("Geçerli bir e-mail adresi girin.", "Enter a valid email address.")
    "email_already_used" in raw -> sh("Bu e-mail başka hesapta kullanılıyor.", "This email is already in use.")
    else -> sh("Profil kaydedilemedi.", "Profile could not be saved.")
}

private fun photoError(raw: String): String = when {
    "photo_size" in raw -> sh("Fotoğraf en fazla 8 MB olabilir.", "Photo must be 8 MB or smaller.")
    "photo_type" in raw -> sh("Geçerli bir görsel dosyası seçin.", "Choose a valid image file.")
    "avatar_upload_failed" in raw -> sh("Fotoğraf sunucuya yüklenemedi.", "Photo could not be uploaded to the server.")
    else -> sh("Fotoğraf yüklenemedi.", "Photo upload failed.")
}
