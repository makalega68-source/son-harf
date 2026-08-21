package com.sonharf.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import coil.compose.AsyncImage
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal object V6Light {
    val bg = Color(0xFFF8FAFC)
    val white = Color.White
    val blue = Color(0xFF0284C7)
    val blueDark = Color(0xFF0369A1)
    val blueLight = Color(0xFFE0F2FE)
    val text = Color(0xFF0F172A)
    val muted = Color(0xFF64748B)
    val border = Color(0xFFCBD5E1)
    val amber = Color(0xFFD97706)
    val green = Color(0xFF16A34A)
    val red = Color(0xFFDC2626)
    val fire = Color(0xFFEA580C)
}

@Serializable
internal data class V6ProfileDto(
    val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_path") val avatarPath: String? = null,
    @SerialName("allow_match_chat") val allowMatchChat: Boolean = true,
    val diamonds: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
)

internal suspend fun v6LoadProfile(id: String): V6ProfileDto? =
    SupabaseProvider.client.from("profiles")
        .select { filter { eq("id", id) } }
        .decodeList<V6ProfileDto>()
        .firstOrNull()

@Composable
internal fun V6Avatar(url: String?, name: String, size: Int) {
    if (!url.isNullOrBlank()) {
        AsyncImage(
            model = url,
            contentDescription = "$name profil fotoğrafı",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size.dp).clip(CircleShape).background(V6Light.blueLight),
        )
    } else {
        Box(
            Modifier.size(size.dp).clip(CircleShape).background(V6Light.blueLight),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                name.take(1).uppercase(),
                color = V6Light.blueDark,
                fontWeight = FontWeight.Black,
                fontSize = (size / 2.2).sp,
            )
        }
    }
}
