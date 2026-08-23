package com.sonharf.game

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

internal object ProfilePhotoRuntime {
    private val http = HttpClient(OkHttp)
    private val cache = LinkedHashMap<String, ByteArray>()
    private val genderCache = LinkedHashMap<String, String?>()

    suspend fun load(path: String): ByteArray? {
        if (path.isBlank() || !SupabaseProvider.configured) return null
        synchronized(cache) { cache[path] }?.let { return it }
        val session = SupabaseProvider.client.auth.currentSessionOrNull() ?: return null
        val response = runCatching {
            http.get("${BuildConfig.SUPABASE_URL}/storage/v1/object/authenticated/profile-photos/$path") {
                header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
                header("apikey", BuildConfig.SUPABASE_KEY)
            }
        }.getOrNull() ?: return null
        if (!response.status.isSuccess()) return null
        val bytes = response.bodyAsBytes()
        if (bytes.isNotEmpty()) synchronized(cache) {
            cache[path] = bytes
            while (cache.size > 40) cache.remove(cache.keys.first())
        }
        return bytes
    }

    suspend fun genderForAvatar(path: String?): String? {
        if (path.isNullOrBlank() || !SupabaseProvider.configured) return null
        val ownerId = path.substringBefore('/').takeIf { it.isNotBlank() } ?: return null
        synchronized(genderCache) { if (genderCache.containsKey(ownerId)) return genderCache[ownerId] }
        val gender = runCatching {
            SupabaseProvider.client.from("profiles").select { filter { eq("id", ownerId) } }
                .decodeList<ProfileDto>().firstOrNull()?.gender
        }.getOrNull()
        synchronized(genderCache) {
            genderCache[ownerId] = gender
            while (genderCache.size > 80) genderCache.remove(genderCache.keys.first())
        }
        return gender
    }

    suspend fun compactForUpload(source: ByteArray, maxSide: Int = 720, maxBytes: Int = 420_000): ByteArray = withContext(Dispatchers.Default) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(source, 0, source.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) error("invalid_image")
        var sample = 1
        while (bounds.outWidth / sample > maxSide * 2 || bounds.outHeight / sample > maxSide * 2) sample *= 2
        val bitmap = BitmapFactory.decodeByteArray(source, 0, source.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: error("invalid_image")
        val ratio = minOf(maxSide.toFloat() / bitmap.width, maxSide.toFloat() / bitmap.height, 1f)
        val resized = if (ratio < 1f) Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt().coerceAtLeast(1), (bitmap.height * ratio).toInt().coerceAtLeast(1), true) else bitmap
        var quality = 88
        var out: ByteArray
        do {
            val stream = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, stream)
            out = stream.toByteArray()
            quality -= 6
        } while (out.size > maxBytes && quality >= 58)
        if (resized !== bitmap) resized.recycle()
        bitmap.recycle()
        out
    }
}

private data class GenderVisual(val symbol: String, val color: Color)

private fun genderVisual(gender: String?): GenderVisual? = when (gender?.trim()?.lowercase()) {
    "kadın", "kadin", "female", "woman" -> GenderVisual("♀", Color(0xFFFF4F9A))
    "erkek", "male", "man" -> GenderVisual("♂", Color(0xFF238BFF))
    else -> null
}

@Composable
private fun FramelessGenderSymbol(gender: String?, size: Dp) {
    val visual = genderVisual(gender) ?: return
    Text(
        text = visual.symbol,
        color = visual.color,
        fontWeight = FontWeight.Black,
        fontSize = (size.value * .31f).coerceAtLeast(13f).sp,
        style = TextStyle(
            shadow = Shadow(
                color = visual.color.copy(alpha = .28f),
                blurRadius = (size.value * .12f).coerceAtLeast(3f),
            )
        ),
    )
}

@Composable
internal fun ProfilePhotoAvatar(
    avatarPath: String?,
    name: String,
    size: Dp,
    visible: Boolean = true,
    accent: Color = SonHarfCyan,
) {
    var bytes by remember(avatarPath) { mutableStateOf<ByteArray?>(null) }
    var gender by remember(avatarPath) { mutableStateOf<String?>(null) }
    LaunchedEffect(avatarPath) {
        bytes = if (!avatarPath.isNullOrBlank()) ProfilePhotoRuntime.load(avatarPath) else null
        gender = ProfilePhotoRuntime.genderForAvatar(avatarPath)
    }
    val bitmap = remember(bytes) { bytes?.let { runCatching { BitmapFactory.decodeByteArray(it, 0, it.size) }.getOrNull() } }
    Box(Modifier.size(size + 5.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(size).clip(CircleShape).background(Brush.sweepGradient(listOf(Color.White, accent, Color(0xFF57C7F3), Color.White))).padding(3.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxSize().clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                    Text(name.take(1).uppercase(), color = Color(0xFF16324A), fontWeight = FontWeight.Black, fontSize = (size.value * .38f).sp)
                }
            }
        }
        Box(Modifier.align(Alignment.BottomEnd)) {
            FramelessGenderSymbol(gender, size)
        }
    }
}

@Composable
internal fun ProfilePhotoAvatarWithGender(
    avatarPath: String?,
    gender: String?,
    name: String,
    size: Dp,
    accent: Color = SonHarfCyan,
) {
    var bytes by remember(avatarPath) { mutableStateOf<ByteArray?>(null) }
    LaunchedEffect(avatarPath) {
        bytes = if (!avatarPath.isNullOrBlank()) ProfilePhotoRuntime.load(avatarPath) else null
    }
    val bitmap = remember(bytes) { bytes?.let { runCatching { BitmapFactory.decodeByteArray(it, 0, it.size) }.getOrNull() } }
    Box(Modifier.size(size + 5.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(size).clip(CircleShape).background(Brush.sweepGradient(listOf(Color.White, accent, Color(0xFF57C7F3), Color.White))).padding(3.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxSize().clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                    Text(name.take(1).uppercase(), color = Color(0xFF16324A), fontWeight = FontWeight.Black, fontSize = (size.value * .38f).sp)
                }
            }
        }
        Box(Modifier.align(Alignment.BottomEnd)) {
            FramelessGenderSymbol(gender, size)
        }
    }
}
