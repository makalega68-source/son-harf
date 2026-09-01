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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.painterResource
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
        if (path.startsWith("bot:")) return null
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
        if (path?.startsWith("bot:female") == true) return "female"
        if (path?.startsWith("bot:male") == true) return "male"
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
private data class ProfileFrameVisual(val primary: Color, val secondary: Color, val marker: String?, val outerPadding: Dp)

private fun genderVisual(gender: String?): GenderVisual? = when (gender?.trim()?.lowercase()) {
    "kadın", "kadin", "female", "woman" -> GenderVisual("♀", Color(0xFFFF4F9A))
    "erkek", "male", "man" -> GenderVisual("♂", Color(0xFF238BFF))
    else -> null
}

private fun profileFrameVisual(frameId: String?, fallbackAccent: Color): ProfileFrameVisual = when {
    frameId?.contains("black_gold") == true -> ProfileFrameVisual(Color(0xFF17191F), Color(0xFFD6A84B), "✦", 4.dp)
    frameId?.contains("royal_gold") == true -> ProfileFrameVisual(Color(0xFFD29B2B), Color(0xFFFFE5A3), "♛", 4.dp)
    frameId?.contains("crystal") == true -> ProfileFrameVisual(Color(0xFFBCEBFF), Color(0xFF708BFF), "◇", 4.dp)
    frameId?.contains("purple_prestige") == true -> ProfileFrameVisual(Color(0xFF5E3AB8), Color(0xFFC0A2FF), "✦", 4.dp)
    frameId?.contains("ice") == true -> ProfileFrameVisual(Color(0xFF61B9E8), Color(0xFFD9F5FF), "❄", 4.dp)
    frameId?.contains("gold") == true || frameId?.contains("vip") == true -> ProfileFrameVisual(
        primary = Color(0xFFF4B928), secondary = Color(0xFFFFE59B), marker = "VIP", outerPadding = 4.dp,
    )
    frameId?.contains("neon") == true -> ProfileFrameVisual(
        primary = Color(0xFF22D3EE), secondary = Color(0xFF8B5CF6), marker = "✦", outerPadding = 4.dp,
    )
    frameId?.contains("starter") == true || frameId?.contains("founder") == true || frameId?.contains("light") == true -> ProfileFrameVisual(
        primary = Color(0xFF6D5CE7), secondary = Color(0xFF2D8CFF), marker = "✧", outerPadding = 4.dp,
    )
    else -> ProfileFrameVisual(primary = fallbackAccent, secondary = Color(0xFF57C7F3), marker = null, outerPadding = 3.dp)
}

private fun premiumFrameAsset(frameId: String?): Int? = when {
    frameId?.contains("black_gold") == true -> R.drawable.premium_frame_black_gold_higgsfield_v2
    frameId?.contains("royal_gold") == true -> R.drawable.premium_frame_royal_gold_higgsfield
    frameId?.contains("crystal") == true -> R.drawable.premium_frame_crystal_higgsfield
    frameId?.contains("purple_prestige") == true -> R.drawable.premium_frame_purple_prestige_higgsfield
    else -> null
}

@Composable
private fun PremiumFrameAsset(frameId: String?, modifier: Modifier) {
    val resId = premiumFrameAsset(frameId) ?: return
    Image(
        painter = painterResource(resId),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.FillBounds,
    )
}

@Composable
private fun FramelessGenderSymbol(gender: String?, size: Dp) {
    val visual = genderVisual(gender) ?: return
    Text(
        text = visual.symbol,
        color = visual.color,
        fontWeight = FontWeight.Black,
        fontSize = (size.value * .31f).coerceAtLeast(13f).sp,
        style = TextStyle(shadow = Shadow(color = visual.color.copy(alpha = .28f), blurRadius = (size.value * .12f).coerceAtLeast(3f))),
    )
}

@Composable
private fun ProfileFrameMarker(marker: String?, size: Dp) {
    if (marker.isNullOrBlank()) return
    Text(
        marker,
        color = if (marker == "VIP") Color(0xFFB77800) else Color.White,
        fontWeight = FontWeight.Black,
        fontSize = if (marker == "VIP") (size.value * .12f).coerceAtLeast(7f).sp else (size.value * .22f).coerceAtLeast(11f).sp,
        modifier = Modifier
            .background(
                if (marker == "VIP") Color(0xFFFFE7A6) else Color(0xFF182235).copy(alpha = .78f),
                RoundedCornerShape(99.dp),
            )
            .padding(horizontal = if (marker == "VIP") 4.dp else 3.dp, vertical = 1.dp),
    )
}

@Composable
private fun BotAvatar(path: String, modifier: Modifier) {
    val res = if (path == "bot:female") R.drawable.bot_avatar_female_higgsfield else R.drawable.bot_avatar_male_higgsfield
    Image(painterResource(res), null, modifier, contentScale = ContentScale.Crop)
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
    val isBot = avatarPath?.startsWith("bot:") == true
    LaunchedEffect(avatarPath, visible) {
        bytes = if (isBot) null else if (visible && !avatarPath.isNullOrBlank()) ProfilePhotoRuntime.load(avatarPath) else null
        gender = ProfilePhotoRuntime.genderForAvatar(avatarPath)
    }
    val bitmap = remember(bytes) { bytes?.let { runCatching { BitmapFactory.decodeByteArray(it, 0, it.size) }.getOrNull() } }
    val frameId = SonHarfCosmetics.profileFrameId
    val frame = profileFrameVisual(frameId, accent)
    Box(Modifier.size(size + 8.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(Brush.sweepGradient(listOf(frame.secondary, frame.primary, frame.secondary, frame.primary)))
                .padding(frame.outerPadding),
            contentAlignment = Alignment.Center,
        ) {
            when {
                isBot && visible -> BotAvatar(requireNotNull(avatarPath), Modifier.fillMaxSize().clip(CircleShape))
                bitmap != null -> Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                else -> Box(Modifier.fillMaxSize().clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                    Text(name.take(1).uppercase(), color = Color(0xFF16324A), fontWeight = FontWeight.Black, fontSize = (size.value * .38f).sp)
                }
            }
        }
        PremiumFrameAsset(frameId, Modifier.fillMaxSize())
        Box(Modifier.align(Alignment.TopEnd)) { ProfileFrameMarker(frame.marker, size) }
        Box(Modifier.align(Alignment.BottomEnd)) { FramelessGenderSymbol(gender, size) }
    }
}

@Composable
internal fun ProfilePhotoAvatarWithGender(
    avatarPath: String?,
    gender: String?,
    name: String,
    size: Dp,
    accent: Color = SonHarfCyan,
    visible: Boolean = true,
) {
    val width = if (size < 56.dp) 56.dp else size
    ProfilePhotoAvatarRectWithGender(
        avatarPath = avatarPath,
        gender = gender,
        name = name,
        width = width,
        height = width * (74f / 56f),
        accent = accent,
        visible = visible,
    )
}

@Composable
internal fun ProfilePhotoAvatarRectWithGender(
    avatarPath: String?,
    gender: String?,
    name: String,
    width: Dp,
    height: Dp,
    accent: Color = SonHarfCyan,
    visible: Boolean = true,
) {
    var bytes by remember(avatarPath) { mutableStateOf<ByteArray?>(null) }
    val isBot = avatarPath?.startsWith("bot:") == true
    LaunchedEffect(avatarPath, visible) {
        bytes = if (isBot) null else if (visible && !avatarPath.isNullOrBlank()) ProfilePhotoRuntime.load(avatarPath) else null
    }
    val bitmap = remember(bytes) { bytes?.let { runCatching { BitmapFactory.decodeByteArray(it, 0, it.size) }.getOrNull() } }
    val shape = RoundedCornerShape(14.dp)
    val frameId = SonHarfCosmetics.profileFrameId
    val frame = profileFrameVisual(frameId, accent)
    Box(Modifier.size(width + 4.dp, height + 8.dp), contentAlignment = Alignment.TopCenter) {
        Box(
            Modifier
                .size(width, height)
                .clip(shape)
                .background(Brush.linearGradient(listOf(frame.primary, frame.secondary, frame.primary)))
                .padding(frame.outerPadding),
            contentAlignment = Alignment.Center,
        ) {
            when {
                isBot && visible -> BotAvatar(requireNotNull(avatarPath), Modifier.fillMaxSize().clip(shape))
                bitmap != null -> Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize().clip(shape), contentScale = ContentScale.Crop)
                else -> Box(Modifier.fillMaxSize().clip(shape).background(Color.White), contentAlignment = Alignment.Center) {
                    Text(name.take(1).uppercase(), color = Color(0xFF16324A), fontWeight = FontWeight.Black, fontSize = (height.value * .32f).coerceAtLeast(14f).sp)
                }
            }
        }
        PremiumFrameAsset(frameId, Modifier.size(width, height).align(Alignment.TopCenter))
        Box(Modifier.align(Alignment.TopEnd)) { ProfileFrameMarker(frame.marker, width) }
        Box(Modifier.align(Alignment.BottomEnd)) { FramelessGenderSymbol(gender, height) }
    }
}
