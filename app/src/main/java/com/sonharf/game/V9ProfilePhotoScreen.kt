package com.sonharf.game

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sonharf.game.data.AvatarSignedUrl
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val P9Bg = Color(0xFFF8FAFC)
private val P9White = Color.White
private val P9Blue = Color(0xFF0284C7)
private val P9BlueLight = Color(0xFFE0F2FE)
private val P9Text = Color(0xFF0F172A)
private val P9Muted = Color(0xFF64748B)
private val P9Border = Color(0xFFCBD5E1)
private val P9Green = Color(0xFF2E6F5E)
private val P9Coral = Color(0xFFE05A47)

@Serializable
private data class V9ProfilePhotoDto(
    val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_path") val avatarPath: String? = null,
    @SerialName("avatar_visibility") val avatarVisibility: String = "hidden",
    val wins: Int = 0,
    val losses: Int = 0,
    val diamonds: Int = 0,
)

private object V9ProfilePhotoStorage {
    private val http = HttpClient(OkHttp)
    private const val MAX_BYTES = 5 * 1024 * 1024

    suspend fun upload(context: Context, uri: Uri): String {
        val session = SupabaseProvider.client.auth.currentSessionOrNull() ?: error("not_authenticated")
        val uid = SupabaseProvider.client.auth.currentUserOrNull()?.id ?: error("not_authenticated")
        val mime = context.contentResolver.getType(uri)?.lowercase() ?: ""
        require(mime in setOf("image/jpeg", "image/png", "image/webp")) { "unsupported_image_type" }
        val bytes = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val data = input.readBytes()
                require(data.isNotEmpty() && data.size <= MAX_BYTES) { "image_too_large" }
                data
            } ?: error("image_read_failed")
        }
        val ext = when (mime) { "image/png" -> "png"; "image/webp" -> "webp"; else -> "jpg" }
        val path = "$uid/avatar-${System.currentTimeMillis()}.$ext"
        val response = http.post("${BuildConfig.SUPABASE_URL}/storage/v1/object/profile-photos/$path") {
            header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
            header("apikey", BuildConfig.SUPABASE_KEY)
            header("x-upsert", "true")
            contentType(ContentType.parse(mime))
            setBody(bytes)
        }
        check(response.status.isSuccess()) { "upload_failed_${response.status.value}" }
        SupabaseProvider.client.postgrest.rpc("set_avatar_path", buildJsonObject { put("p_path", path) })
        SupabaseProvider.client.postgrest.rpc("set_avatar_visibility", buildJsonObject { put("p_hidden", false) })
        return path
    }

    suspend fun publishExisting() {
        SupabaseProvider.client.postgrest.rpc("set_avatar_visibility", buildJsonObject { put("p_hidden", false) })
    }

    suspend fun saveDisplayName(name: String) {
        SupabaseProvider.client.postgrest.rpc("set_display_name", buildJsonObject { put("p_name", name.trim()) })
    }
}

private suspend fun loadV9Profile(): V9ProfilePhotoDto? {
    val uid = SupabaseProvider.client.auth.currentUserOrNull()?.id ?: return null
    return SupabaseProvider.client.from("profiles").select { filter { eq("id", uid) } }.decodeList<V9ProfilePhotoDto>().firstOrNull()
}

@Composable
fun V9ProfilePhotoScreen(onOpenPreferences: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<V9ProfilePhotoDto?>(null) }
    var avatarUrl by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    var editingName by remember { mutableStateOf(false) }
    var nameDraft by remember { mutableStateOf("") }

    suspend fun reload() {
        loading = true
        val p = runCatching { loadV9Profile() }.getOrNull()
        profile = p
        if (!editingName) nameDraft = p?.displayName.orEmpty()
        avatarUrl = runCatching { AvatarSignedUrl.resolve(p?.avatarPath) }.getOrNull()
        loading = false
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null || busy) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            notice = "Fotoğraf yükleniyor…"
            runCatching { V9ProfilePhotoStorage.upload(context, uri) }
                .onSuccess {
                    reload()
                    notice = if (avatarUrl.isNullOrBlank()) "Fotoğraf kaydedildi ancak görüntü adresi alınamadı. Tekrar deneyin." else "Profil fotoğrafı yüklendi ve tüm profil alanlarında görünür."
                }
                .onFailure { error ->
                    notice = when {
                        "image_too_large" in error.message.orEmpty() -> "Fotoğraf en fazla 5 MB olabilir."
                        "unsupported_image_type" in error.message.orEmpty() -> "JPG, PNG veya WEBP fotoğraf seçin."
                        else -> "Fotoğraf yüklenemedi: ${error.message.orEmpty().take(80)}"
                    }
                }
            busy = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    if (loading && profile == null) {
        Box(Modifier.fillMaxSize().background(P9Bg), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = P9Blue) }
        return
    }

    val p = profile
    LazyColumn(
        Modifier.fillMaxSize().background(P9Bg),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("PROFİL", Modifier.weight(1f), fontWeight = FontWeight.Black, fontSize = 22.sp, color = P9Text)
                TextButton(onClick = onOpenPreferences) { Text("Gizlilik & Tercihler") }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(22.dp), color = P9White, border = BorderStroke(1.dp, P9Border)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    V9Avatar(avatarUrl, p?.displayName ?: "Oyuncu", 112)
                    if (editingName) {
                        OutlinedTextField(
                            value = nameDraft,
                            onValueChange = { nameDraft = it.take(24) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Oyuncu adı") },
                            supportingText = { Text("2–24 karakter") },
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { nameDraft = p?.displayName.orEmpty(); editingName = false }, modifier = Modifier.weight(1f)) { Text("VAZGEÇ") }
                            Button(
                                onClick = {
                                    if (!busy) scope.launch {
                                        busy = true
                                        runCatching { V9ProfilePhotoStorage.saveDisplayName(nameDraft) }
                                            .onSuccess { editingName = false; notice = "Oyuncu adı güncellendi."; reload() }
                                            .onFailure { notice = if ("invalid_display_name" in it.message.orEmpty()) "Oyuncu adı 2–24 karakter olmalı." else "Oyuncu adı güncellenemedi." }
                                        busy = false
                                    }
                                },
                                enabled = !busy && nameDraft.trim().length in 2..24,
                                modifier = Modifier.weight(1f),
                            ) { Icon(Icons.Rounded.Save, null); Spacer(Modifier.width(4.dp)); Text("KAYDET") }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(p?.displayName ?: "Oyuncu", fontWeight = FontWeight.Black, fontSize = 22.sp, color = P9Text)
                            IconButton(onClick = { nameDraft = p?.displayName.orEmpty(); editingName = true }) { Icon(Icons.Rounded.Edit, "Oyuncu adını düzenle", tint = P9Blue) }
                        }
                    }
                    Text(if (p?.avatarPath.isNullOrBlank()) "Henüz profil fotoğrafı yok" else if (avatarUrl.isNullOrBlank()) "Fotoğraf kayıtlı • görüntü bağlantısı yenileniyor" else "Profil fotoğrafın aktif", color = P9Muted, fontSize = 12.sp)
                    Button(
                        onClick = { picker.launch("image/*") },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = P9Blue),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Rounded.CameraAlt, null); Spacer(Modifier.width(8.dp))
                        Text(if (p?.avatarPath.isNullOrBlank()) "PROFİL FOTOĞRAFI YÜKLE" else "PROFİL FOTOĞRAFINI DEĞİŞTİR", fontWeight = FontWeight.Black)
                    }
                    if (!p?.avatarPath.isNullOrBlank() && p?.avatarVisibility != "public") {
                        OutlinedButton(
                            onClick = {
                                if (!busy) scope.launch {
                                    busy = true
                                    runCatching { V9ProfilePhotoStorage.publishExisting() }
                                        .onSuccess { notice = "Profil fotoğrafın diğer oyunculara görünür yapıldı."; reload() }
                                        .onFailure { notice = "Fotoğraf görünürlüğü güncellenemedi." }
                                    busy = false
                                }
                            },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                        ) { Icon(Icons.Rounded.Visibility, null); Spacer(Modifier.width(8.dp)); Text("FOTOĞRAFI TÜM OYUNDA GÖRÜNÜR YAP", fontWeight = FontWeight.Bold) }
                    }
                    Text("Desteklenen: JPG, PNG, WEBP • En fazla 5 MB", color = P9Muted, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            }
        }
        if (notice.isNotBlank()) {
            item {
                val failed = notice.contains("yüklenemedi") || notice.contains("güncellenemedi") || notice.contains("alınamadı")
                Surface(shape = RoundedCornerShape(14.dp), color = if (failed) Color(0xFFFDECEC) else P9BlueLight) {
                    Text(notice, Modifier.fillMaxWidth().padding(12.dp), color = if (failed) P9Coral else P9Text, textAlign = TextAlign.Center, fontSize = 12.sp)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                V9Metric("${p?.wins ?: 0}", "Galibiyet", Modifier.weight(1f)); V9Metric("${p?.losses ?: 0}", "Mağlubiyet", Modifier.weight(1f)); V9Metric("${p?.diamonds ?: 0}", "Elmas", Modifier.weight(1f))
            }
        }
        item {
            Surface(shape = RoundedCornerShape(16.dp), color = P9White, border = BorderStroke(1.dp, P9Border)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("FOTOĞRAF NEREDE GÖRÜNÜR?", color = P9Green, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Text("Ana sayfa profil kartı • Profil ekranı • Maç lobisi • 1v1 oyun ekranı • Arkadaş ve liderlik yüzeyleri", color = P9Text, fontSize = 12.sp, lineHeight = 18.sp)
                    Text("Yeni yüklenen fotoğraf otomatik olarak görünür moda alınır.", color = P9Muted, fontSize = 11.sp)
                }
            }
        }
        if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = P9Blue) }
    }
}

@Composable
private fun V9Avatar(url: String?, name: String, size: Int) {
    var failed by remember(url) { mutableStateOf(false) }
    if (!url.isNullOrBlank() && !failed) {
        AsyncImage(model = url, contentDescription = "$name profil fotoğrafı", contentScale = ContentScale.Crop, modifier = Modifier.size(size.dp).clip(CircleShape).background(P9BlueLight), onError = { failed = true })
    } else {
        Box(Modifier.size(size.dp).clip(CircleShape).background(P9BlueLight), contentAlignment = Alignment.Center) { Text(name.take(1).uppercase(), fontWeight = FontWeight.Black, fontSize = (size / 2.3).sp, color = P9Blue) }
    }
}

@Composable
private fun V9Metric(value: String, label: String, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = P9White, border = BorderStroke(1.dp, P9Border)) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, fontWeight = FontWeight.Black, fontSize = 20.sp, color = P9Blue); Text(label, fontSize = 11.sp, color = P9Muted) }
    }
}
