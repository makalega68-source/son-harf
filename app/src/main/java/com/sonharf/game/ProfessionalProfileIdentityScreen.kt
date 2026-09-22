package com.sonharf.game

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
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
import java.util.UUID

@Serializable
private data class ProfessionalIdentityStateDto(
    val id: String,
    @SerialName("display_name") val displayName: String,
    val gender: String? = null,
    @SerialName("identity_locked") val identityLocked: Boolean = false,
)

private object ProfessionalProfilePhotoStorage {
    private val http = HttpClient(OkHttp)

    suspend fun upload(bytes: ByteArray): String {
        val session = SupabaseProvider.client.auth.currentSessionOrNull() ?: error("not_authenticated")
        val uid = SupabaseProvider.client.auth.currentUserOrNull()?.id ?: error("not_authenticated")
        val path = "$uid/avatar-${UUID.randomUUID()}.webp"
        val response = http.post("${BuildConfig.SUPABASE_URL}/storage/v1/object/profile-photos/$path") {
            header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
            header("apikey", BuildConfig.SUPABASE_KEY)
            header("x-upsert", "true")
            contentType(ContentType.Image.WebP)
            setBody(bytes)
        }
        if (!response.status.isSuccess()) error("avatar_upload_failed_${response.status.value}")
        return path
    }
}

@Composable
internal fun ProfessionalProfileIdentityScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var identity by remember { mutableStateOf<ProfessionalIdentityStateDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var showIdentityDialog by remember { mutableStateOf(false) }

    suspend fun reload() {
        val b = backend ?: run {
            loading = false
            return
        }
        loading = true
        val uid = b.currentUserId()
        profile = uid?.let { runCatching { b.getProfile(it) }.getOrNull() }
        identity = uid?.let {
            runCatching {
                SupabaseProvider.client.from("profiles")
                    .select { filter { eq("id", it) } }
                    .decodeList<ProfessionalIdentityStateDto>()
                    .firstOrNull()
            }.getOrNull()
        }
        loading = false
    }

    LaunchedEffect(Unit) { reload() }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null || busy) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            runCatching {
                val raw = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: error("photo_read_failed")
                }
                require(raw.isNotEmpty())
                require((context.contentResolver.getType(uri) ?: "image/jpeg").startsWith("image/"))
                val compact = ProfilePhotoRuntime.compactForUpload(raw)
                val path = ProfessionalProfilePhotoStorage.upload(compact)
                SupabaseProvider.client.postgrest.rpc(
                    "set_avatar_path",
                    buildJsonObject { put("p_path", path) },
                )
                SupabaseProvider.client.postgrest.rpc(
                    "set_avatar_visibility",
                    buildJsonObject { put("p_hidden", false) },
                )
            }.onSuccess {
                notice = gameText("Profil fotoğrafı güncellendi.", "Profile photo updated.")
                reload()
            }.onFailure {
                notice = gameText(
                    "Fotoğraf yüklenemedi. JPG, PNG veya WEBP dosyasını tekrar dene.",
                    "Photo could not be uploaded. Try a JPG, PNG or WEBP file again.",
                )
            }
            busy = false
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = GameSpacing.ScreenHorizontal, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (loading) {
            item {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = GameColors.PrimaryBlue,
                    trackColor = GameColors.SecondarySurface,
                )
            }
        }

        item {
            GameSurface(
                elevated = true,
                borderColor = if (profile?.isVip == true) GameColors.PrestigeGold.copy(alpha = .42f) else GameColors.PrimaryBlue.copy(alpha = .30f),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        FramedProfilePhotoAvatar(
                            avatarPath = profile?.avatarPath,
                            gender = profile?.gender,
                            name = profile?.displayName ?: gameText("Oyuncu", "Player"),
                            size = 92.dp,
                            frameId = SonHarfCosmetics.profileFrameId,
                            accent = if (profile?.isVip == true) GameColors.PrestigeGold else GameColors.PrimaryBlue,
                            visible = profile?.avatarVisibility != "hidden",
                            isPro = profile?.isVip == true,
                        )
                        Surface(
                            onClick = { if (!busy) photoPicker.launch("image/*") },
                            shape = CircleShape,
                            color = GameColors.PrimaryBlue,
                            border = BorderStroke(2.dp, GameColors.PrimarySurface),
                        ) {
                            Icon(
                                Icons.Rounded.PhotoCamera,
                                contentDescription = gameText("Fotoğrafı değiştir", "Change photo"),
                                tint = Color.White,
                                modifier = Modifier.padding(8.dp).size(18.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            profile?.displayName ?: gameText("Oyuncu", "Player"),
                            color = GameColors.TextPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (profile?.isVip == true) "KELİME KUŞATMASI PRO" else gameText("KELİME KUŞATMASI OYUNCUSU", "KELIME KUSATMASI PLAYER"),
                            color = if (profile?.isVip == true) GameColors.PrestigeGold else GameColors.TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        profile?.gender?.let {
                            Spacer(Modifier.height(3.dp))
                            Text(
                                professionalGenderLabel(it),
                                color = GameColors.TextTertiary,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProfileIdentityMetric(
                        value = profile?.rating?.toString() ?: "—",
                        label = "Rating",
                        modifier = Modifier.weight(1f),
                    )
                    ProfileIdentityMetric(
                        value = profile?.wins?.toString() ?: "0",
                        label = gameText("Galibiyet", "Wins"),
                        modifier = Modifier.weight(1f),
                    )
                    ProfileIdentityMetric(
                        value = profile?.losses?.toString() ?: "0",
                        label = gameText("Mağlubiyet", "Losses"),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item {
            GameSectionHeader(gameText("Profil Fotoğrafı", "Profile Photo"))
            GameSurface(borderColor = GameColors.TacticalTurquoise.copy(alpha = .28f)) {
                Text(
                    gameText(
                        "Fotoğrafın otomatik olarak optimize edilir. Görünürlüğü gizlilik sekmesinden veya aşağıdaki anahtardan yönetebilirsin.",
                        "Your photo is optimized automatically. Manage visibility from the privacy tab or the switch below.",
                    ),
                    color = GameColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(10.dp))
                GameSecondaryButton(
                    text = gameText("FOTOĞRAFI DEĞİŞTİR", "CHANGE PHOTO"),
                    onClick = { photoPicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                    icon = Icons.Rounded.PhotoCamera,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            gameText("Fotoğrafı gizle", "Hide photo"),
                            color = GameColors.TextPrimary,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            gameText("Diğer oyuncular fotoğrafını görmez.", "Other players will not see your photo."),
                            color = GameColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(
                        checked = profile?.avatarVisibility == "hidden",
                        enabled = !busy && profile != null,
                        onCheckedChange = { hidden ->
                            scope.launch {
                                busy = true
                                runCatching {
                                    SupabaseProvider.client.postgrest.rpc(
                                        "set_avatar_visibility",
                                        buildJsonObject { put("p_hidden", hidden) },
                                    )
                                }.onSuccess {
                                    notice = if (hidden) {
                                        gameText("Profil fotoğrafın gizlendi.", "Your profile photo is hidden.")
                                    } else {
                                        gameText("Profil fotoğrafın görünür.", "Your profile photo is visible.")
                                    }
                                    reload()
                                }.onFailure {
                                    notice = gameText("Fotoğraf görünürlüğü kaydedilemedi.", "Photo visibility could not be saved.")
                                }
                                busy = false
                            }
                        },
                    )
                }
            }
        }

        item {
            GameSectionHeader(gameText("Oyuncu Kimliği", "Player Identity"))
            GameSurface(borderColor = GameColors.Lavender.copy(alpha = .28f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (identity?.identityLocked == true) Icons.Rounded.Lock else Icons.Rounded.Badge,
                        contentDescription = null,
                        tint = if (identity?.identityLocked == true) GameColors.TextTertiary else GameColors.Lavender,
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (identity?.identityLocked == true) gameText("Kimlik doğrulandı", "Identity verified") else gameText("Kimliği tamamla", "Complete identity"),
                            color = GameColors.TextPrimary,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            if (identity?.identityLocked == true) {
                                gameText("Oyuncu adı ve cinsiyet güvenlik nedeniyle kilitlidir.", "Player name and gender are locked for account integrity.")
                            } else {
                                gameText("Eski hesaplarda oyuncu adı ve cinsiyet yalnız bir kez kaydedilir.", "Legacy accounts can save player name and gender once.")
                            },
                            color = GameColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                if (identity?.identityLocked == false) {
                    Spacer(Modifier.height(12.dp))
                    GamePrimaryButton(
                        text = gameText("KİMLİĞİ TAMAMLA", "COMPLETE IDENTITY"),
                        onClick = { showIdentityDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy,
                        icon = Icons.Rounded.Badge,
                    )
                }
            }
        }

        notice?.let {
            item {
                Surface(
                    shape = GameShapes.Medium,
                    color = GameColors.PrimaryBlue.copy(alpha = .10f),
                    border = BorderStroke(1.dp, GameColors.PrimaryBlue.copy(alpha = .24f)),
                ) {
                    Text(
                        it,
                        modifier = Modifier.fillMaxWidth().padding(11.dp),
                        color = GameColors.TextPrimary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }

    if (showIdentityDialog && identity?.identityLocked == false) {
        ProfessionalIdentityDialog(
            initialName = identity?.displayName.orEmpty().takeUnless { it.startsWith("Oyuncu-") }.orEmpty(),
            busy = busy,
            onDismiss = { if (!busy) showIdentityDialog = false },
            onSave = { name, gender ->
                scope.launch {
                    busy = true
                    runCatching {
                        SupabaseProvider.client.postgrest.rpc(
                            "complete_profile_identity_v2",
                            buildJsonObject {
                                put("p_display_name", name.trim())
                                put("p_gender", gender)
                            },
                        )
                    }.onSuccess {
                        showIdentityDialog = false
                        notice = gameText("Oyuncu kimliği kalıcı olarak kaydedildi.", "Player identity was saved permanently.")
                        reload()
                    }.onFailure {
                        notice = gameText("Kimlik kaydedilemedi. Oyuncu adı 2-24 karakter olmalı.", "Identity could not be saved. Player name must be 2-24 characters.")
                    }
                    busy = false
                }
            },
        )
    }
}

@Composable
private fun ProfileIdentityMetric(value: String, label: String, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = GameShapes.Medium,
        color = GameColors.ElevatedBackground,
        border = BorderStroke(1.dp, GameColors.Divider),
    ) {
        Column(
            Modifier.padding(horizontal = 6.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, color = GameColors.TextPrimary, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleSmall)
            Text(label, color = GameColors.TextTertiary, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
private fun ProfessionalIdentityDialog(
    initialName: String,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var gender by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GameColors.PrimarySurface,
        title = {
            Text(
                gameText("Kimliği Bir Kez Tamamla", "Complete Identity Once"),
                color = GameColors.TextPrimary,
                fontWeight = FontWeight.Black,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    gameText(
                        "Bu işlem eski hesaplar içindir. Kaydettikten sonra oyuncu adı ve cinsiyet değiştirilemez.",
                        "This is for legacy accounts. Player name and gender cannot be changed after saving.",
                    ),
                    color = GameColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(24) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(gameText("Oyuncu adı", "Player name")) },
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "erkek" to gameText("Erkek", "Male"),
                        "kadın" to gameText("Kadın", "Female"),
                        "diğer" to gameText("Diğer", "Other"),
                    ).forEach { (value, label) ->
                        FilterChip(
                            selected = gender == value,
                            onClick = { gender = value },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, gender) },
                enabled = !busy && name.trim().length in 2..24 && gender.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = GameColors.PrimaryBlue),
            ) {
                Text(gameText("KALICI KAYDET", "SAVE PERMANENTLY"), fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text(gameText("ŞİMDİ DEĞİL", "NOT NOW"), color = GameColors.TextSecondary)
            }
        },
    )
}

private fun professionalGenderLabel(value: String): String = when (value.trim().lowercase()) {
    "kadın", "kadin", "female", "woman" -> gameText("Kadın", "Female")
    "erkek", "male", "man" -> gameText("Erkek", "Male")
    else -> gameText("Diğer", "Other")
}
