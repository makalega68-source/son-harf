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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.auth.auth
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

@Composable
internal fun ProfilePhotoAvatar(
    avatarPath: String?,
    name: String,
    size: Dp,
    visible: Boolean = true,
    accent: Color = SonHarfCyan,
) {
    var bytes by remember(avatarPath) { mutableStateOf<ByteArray?>(null) }
    LaunchedEffect(avatarPath) {
        bytes = if (!avatarPath.isNullOrBlank()) ProfilePhotoRuntime.load(avatarPath) else null
    }
    val bitmap = remember(bytes) { bytes?.let { runCatching { BitmapFactory.decodeByteArray(it, 0, it.size) }.getOrNull() } }
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
}


private fun profileGenderSymbol(gender: String?): String = when (gender?.lowercase()) {
    "kadın", "kadin", "female", "woman" -> "♀"
    "erkek", "male", "man" -> "♂"
    "diğer", "diger", "other" -> "⚧"
    else -> "•"
}

@Composable
internal fun ProfilePhotoAvatarWithGender(
    avatarPath: String?,
    gender: String?,
    name: String,
    size: Dp,
    accent: Color = SonHarfCyan,
) {
    Box(Modifier.size(size + 6.dp), contentAlignment = Alignment.Center) {
        ProfilePhotoAvatar(avatarPath = avatarPath, name = name, size = size, visible = true, accent = accent)
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .size((size.value * .34f).coerceAtLeast(15f).dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                profileGenderSymbol(gender),
                color = accent,
                fontWeight = FontWeight.Black,
                fontSize = (size.value * .20f).coerceAtLeast(9f).sp,
            )
        }
    }
}
