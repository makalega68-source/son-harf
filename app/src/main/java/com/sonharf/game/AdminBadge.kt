package com.sonharf.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlin.math.cos
import kotlin.math.sin

/** Who the game's admins are (from the server's admin list), refreshed every few minutes. */
internal object AdminRoster {
    var ids by mutableStateOf<Set<String>>(emptySet())
        private set
    private var loadedAt = 0L

    /** After an admin change: read the list again right away. */
    suspend fun forceRefresh() { loadedAt = 0L; refresh() }

    suspend fun refresh() {
        if (!SupabaseProvider.configured || System.currentTimeMillis() - loadedAt < 5 * 60_000L) return
        runCatching { SupabaseProvider.client.postgrest.rpc("get_admin_ids_v1").decodeList<String>() }
            .onSuccess { ids = it.toSet(); loadedAt = System.currentTimeMillis() }
    }
}

/** The small admin seal shown beside an admin's name, everywhere names appear. */
@Composable
internal fun AdminBadge(userId: String?, size: Dp = 16.dp, modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) { AdminRoster.refresh() }
    if (userId.isNullOrBlank() || userId !in AdminRoster.ids) return
    Box(
        modifier.size(size).semantics { contentDescription = sh("Yönetici", "Admin") }
            .background(Brush.verticalGradient(listOf(Color(0xFFFFE7A3), Color(0xFFC8962F))), CircleShape)
            .border(1.dp, Color(0xFF0C3540), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.VerifiedUser, null, tint = Color(0xFF0C3540), modifier = Modifier.size(size * .68f))
    }
}

/**
 * The admin crest: a gold ring with teal gems, a gold shield on top and a "YÖNETİCİ" plate at
 * the bottom. Laid out like the bitmap frames (1.38 × the avatar) so it overflows its slot.
 */
@Composable
internal fun AdminFrameArt(size: Dp, modifier: Modifier = Modifier) {
    Box(modifier.requiredSize(size * 1.38f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(this.size.width / 2, this.size.height / 2)
            val avatar = this.size.minDimension / 1.38f / 2f
            val ring = avatar * .14f
            val mid = avatar + ring / 2
            drawCircle(Brush.radialGradient(listOf(Color(0x5540F0E0), Color.Transparent), c, mid + ring * 2.2f), mid + ring * 2.2f, c)
            drawCircle(Brush.sweepGradient(listOf(Color(0xFFFFF3C4), Color(0xFFE0A82E), Color(0xFF8C5E0E), Color(0xFFFFD86A),
                Color(0xFFFFF3C4), Color(0xFFC8962F), Color(0xFFFFF3C4)), c), mid, c, style = Stroke(ring))
            drawCircle(Color(0xFF0C3540), avatar + ring * .08f, c, style = Stroke(ring * .22f))
            drawCircle(Color(0xFF2FD3C8).copy(alpha = .8f), avatar + ring * .08f, c, style = Stroke(ring * .07f))
            drawCircle(Color(0xFFFFF8E0).copy(alpha = .7f), mid + ring * .5f, c, style = Stroke(ring * .08f))
            // Teal gems around the ring (the top and bottom carry the shield and the plate).
            for (k in 0 until 8) {
                if (k == 0 || k == 4) continue
                val a = Math.toRadians(-90.0 + k * 45.0)
                val p = Offset(c.x + (mid * cos(a)).toFloat(), c.y + (mid * sin(a)).toFloat())
                val r = ring * .42f
                drawCircle(Brush.radialGradient(listOf(Color(0xFFE0FFFB), Color(0xFF2FD3C8), Color(0xFF0B5E66)), p - Offset(r * .3f, r * .3f), r * 1.4f), r, p)
                drawCircle(Color(0xFF5A3A04), r, p, style = Stroke(ring * .08f))
            }
            // Gold shield on top with a teal heart and a white star.
            val sw = avatar * .46f
            val top = c.y - mid - sw * .52f
            val shield = Path().apply {
                moveTo(c.x - sw / 2, top)
                lineTo(c.x + sw / 2, top)
                lineTo(c.x + sw / 2, top + sw * .5f)
                quadraticTo(c.x + sw / 2, top + sw * .95f, c.x, top + sw * 1.15f)
                quadraticTo(c.x - sw / 2, top + sw * .95f, c.x - sw / 2, top + sw * .5f)
                close()
            }
            drawPath(shield, Brush.verticalGradient(listOf(Color(0xFFFFF3C4), Color(0xFFE0A82E), Color(0xFF8C5E0E)), top, top + sw * 1.15f))
            drawPath(shield, Color(0xFF5A3A04), style = Stroke(ring * .12f))
            val inner = sw * .62f
            val it0 = top + sw * .14f
            val heart = Path().apply {
                moveTo(c.x - inner / 2, it0)
                lineTo(c.x + inner / 2, it0)
                lineTo(c.x + inner / 2, it0 + inner * .48f)
                quadraticTo(c.x + inner / 2, it0 + inner * .9f, c.x, it0 + inner * 1.08f)
                quadraticTo(c.x - inner / 2, it0 + inner * .9f, c.x - inner / 2, it0 + inner * .48f)
                close()
            }
            drawPath(heart, Brush.verticalGradient(listOf(Color(0xFF2FD3C8), Color(0xFF0C3540)), it0, it0 + inner * 1.08f))
            val sc = Offset(c.x, it0 + inner * .48f)
            val arm = inner * .36f
            val w = arm * .25f
            val star = Path().apply {
                moveTo(sc.x, sc.y - arm)
                quadraticTo(sc.x + w, sc.y - w, sc.x + arm, sc.y)
                quadraticTo(sc.x + w, sc.y + w, sc.x, sc.y + arm)
                quadraticTo(sc.x - w, sc.y + w, sc.x - arm, sc.y)
                quadraticTo(sc.x - w, sc.y - w, sc.x, sc.y - arm)
                close()
            }
            drawPath(star, Color.White)
        }
        AdminFramePlate(size)
    }
}

@Composable
private fun AdminFramePlate(size: Dp) {
    val scale = size.value / 100f
    Box(
        Modifier.offset(y = size * .5f)
            .border((1.5f * scale).coerceAtLeast(1f).dp,
                Brush.verticalGradient(listOf(Color(0xFFFFF1B8), Color(0xFFD4A21F), Color(0xFF8A5A0B))), RoundedCornerShape(50))
            .background(Brush.verticalGradient(listOf(Color(0xFF1E7F86), Color(0xFF0C3540))), RoundedCornerShape(50))
            .padding(horizontal = (8f * scale).dp, vertical = (1.5f * scale).dp),
        contentAlignment = Alignment.Center,
    ) {
        val fontSize = (11f * scale).coerceAtLeast(5.5f).sp
        Text(sh("YÖNETİCİ", "ADMIN"), color = Color(0xFFFFE6A0), fontSize = fontSize, lineHeight = fontSize,
            fontWeight = FontWeight.Black, letterSpacing = (1f * scale).sp, maxLines = 1,
            style = androidx.compose.ui.text.TextStyle(
                platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
            ))
    }
}
