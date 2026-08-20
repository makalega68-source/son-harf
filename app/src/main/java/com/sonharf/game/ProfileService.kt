package com.sonharf.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.ByteArrayOutputStream

@Serializable
data class ProductionProfileDto(
    val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("account_email") val accountEmail: String? = null,
    val gender: String? = null,
    @SerialName("identity_locked") val identityLocked: Boolean = false,
    @SerialName("avatar_visibility") val avatarVisibility: String = "hidden",
    @SerialName("avatar_path") val avatarPath: String? = null,
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
)

class ProductionProfileService {
    private val supabase = SupabaseProvider.client
    private val http = HttpClient(OkHttp)

    suspend fun getMe(): ProductionProfileDto {
        val uid = requireNotNull(supabase.auth.currentUserOrNull()?.id)
        return supabase.from("profiles").select { filter { eq("id", uid) } }.decodeSingle()
    }

    suspend fun completeIdentity(name: String, gender: String, email: String): ProductionProfileDto =
        supabase.postgrest.rpc("complete_profile_identity", buildJsonObject {
            put("p_display_name", name.trim())
            put("p_gender", gender.trim())
            put("p_email", email.trim().lowercase())
        }).decodeSingle()

    suspend fun setPhotoHidden(hidden: Boolean): ProductionProfileDto =
        supabase.postgrest.rpc("set_avatar_visibility", buildJsonObject { put("p_hidden", hidden) }).decodeSingle()

    suspend fun uploadAvatar(bytes: ByteArray): ProductionProfileDto {
        val uid = requireNotNull(supabase.auth.currentUserOrNull()?.id)
        val token = requireNotNull(supabase.auth.currentSessionOrNull()?.accessToken)
        val path = "$uid/avatar.webp"
        val response = http.put("${BuildConfig.SUPABASE_URL}/storage/v1/object/avatars/$path") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header("apikey", BuildConfig.SUPABASE_KEY)
            header("x-upsert", "true")
            contentType(ContentType.parse("image/webp"))
            setBody(bytes)
        }
        if (!response.status.isSuccess()) error("avatar_upload_failed")
        return supabase.postgrest.rpc("set_avatar_path", buildJsonObject { put("p_path", path) }).decodeSingle()
    }

    suspend fun downloadOwnAvatar(path: String): ByteArray? {
        val uid = supabase.auth.currentUserOrNull()?.id ?: return null
        if (!path.startsWith("$uid/")) return null
        val token = supabase.auth.currentSessionOrNull()?.accessToken ?: return null
        val response = http.get("${BuildConfig.SUPABASE_URL}/storage/v1/object/authenticated/avatars/$path") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header("apikey", BuildConfig.SUPABASE_KEY)
        }
        return if (response.status.isSuccess()) response.bodyAsBytes() else null
    }
}

fun optimizeAvatar(context: Context, uri: Uri): ByteArray {
    val source = if (Build.VERSION.SDK_INT >= 28) {
        val src = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(src) { decoder, _, _ -> decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE }
    } else {
        context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it) }
            ?: error("image_decode_failed")
    }

    val side = minOf(source.width, source.height)
    val x = (source.width - side) / 2
    val y = (source.height - side) / 2
    val cropped = Bitmap.createBitmap(source, x, y, side, side)
    val target = minOf(512, side)
    val scaled = if (cropped.width == target) cropped else Bitmap.createScaledBitmap(cropped, target, target, true)

    var result = ByteArray(0)
    val qualities = intArrayOf(92, 88, 84, 80, 76, 72)
    for (q in qualities) {
        val out = ByteArrayOutputStream()
        @Suppress("DEPRECATION")
        val format = if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
        scaled.compress(format, q, out)
        result = out.toByteArray()
        if (result.size <= 180 * 1024) break
    }
    if (scaled !== cropped) scaled.recycle()
    if (cropped !== source) cropped.recycle()
    source.recycle()
    return result
}
