package com.sonharf.game

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.put
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.billing.ProductCatalog

/**
 * Live profile frames (collection v2). Simple rings are Son Coin items in the shop; the ornate
 * ones are permanent Google Play products, and the ruby-crowned Royal Gold is reserved for PRO:
 * the server grants and equips it the moment a player becomes PRO and takes it back when PRO ends.
 * A Play frame's store id equals its Play product id; the verifier grants it to
 * user_inventory. Every owned frame is equipped through equip_shop_item.
 */
internal object ProfileFrameCollection {
    data class Frame(
        val id: String,
        @DrawableRes val drawable: Int,
        val nameTr: String,
        val nameEn: String,
        /** Non-null for frames sold for real money through Google Play. */
        val playProductId: String? = null,
    )

    val coinFrames = listOf(
        Frame("frame_round_starter_blue", R.drawable.profile_frame_round_starter_blue, "Başlangıç Mavi", "Starter Blue"),
        Frame("frame_round_starter_neutral", R.drawable.profile_frame_round_starter_neutral, "Başlangıç Nötr", "Starter Neutral"),
        Frame("frame_round_pearl", R.drawable.profile_frame_round_pearl, "Beyaz İnci", "White Pearl"),
        Frame("frame_round_ocean", R.drawable.profile_frame_round_ocean, "Okyanus", "Ocean"),
        Frame("frame_round_rose", R.drawable.profile_frame_round_rose, "Gül Pembesi", "Rose Pink"),
        Frame("frame_round_lilac", R.drawable.profile_frame_round_lilac, "Lavanta", "Lavender"),
        Frame("frame_round_botanic", R.drawable.profile_frame_round_botanic, "Botanik Yeşil", "Botanic Green"),
    )

    val premiumFrames = listOf(
        Frame(ProductCatalog.PROFILE_FRAME_GOLD_CREST, R.drawable.profile_frame_premium_gold, "Altın Arma", "Gold Crest", ProductCatalog.PROFILE_FRAME_GOLD_CREST),
        Frame(ProductCatalog.PROFILE_FRAME_EMERALD, R.drawable.profile_frame_premium_emerald, "Zümrüt Arma", "Emerald Crest", ProductCatalog.PROFILE_FRAME_EMERALD),
        Frame(ProductCatalog.PROFILE_FRAME_AMETHYST, R.drawable.profile_frame_premium_amethyst, "Ametist Arma", "Amethyst Crest", ProductCatalog.PROFILE_FRAME_AMETHYST),
        Frame(ProductCatalog.PROFILE_FRAME_SAKURA, R.drawable.profile_frame_premium_sakura, "Sakura Çelengi", "Sakura Wreath", ProductCatalog.PROFILE_FRAME_SAKURA),
        Frame(ProductCatalog.PROFILE_FRAME_SAPPHIRE, R.drawable.profile_frame_premium_sapphire, "Safir Arma", "Sapphire Crest", ProductCatalog.PROFILE_FRAME_SAPPHIRE),
    )

    /** PRO-only frame: never sold, granted with PRO membership. */
    val proFrame = Frame("frame_round_golden_avatar", R.drawable.profile_frame_round_golden_avatar, "PRO Kraliyet Altın", "PRO Royal Gold")

    val throneFrame = Frame("frame_throne_champion", R.drawable.profile_frame_premium_gold, "Taht Birincisi", "Throne Champion")
    val saleFrames = premiumFrames.filter { it.id != ProductCatalog.PROFILE_FRAME_GOLD_CREST }
    val all = coinFrames + premiumFrames + proFrame + throneFrame
    val coinIds = coinFrames.map { it.id }.toSet()
    val allIds = all.map { it.id }.toSet()
    val premiumProductIds = premiumFrames.mapNotNull { it.playProductId }

    fun find(id: String?): Frame? = all.firstOrNull { it.id == id }
}

/**
 * Draws a frame around content of [size] without changing layout: the art is laid out larger
 * than its slot and simply overflows it, so headers and rows keep their measured height.
 * The PRO frame carries a "PRO" plate on its bottom jewel, so everyone sees how it was earned.
 */
@Composable
internal fun ProfileFrameArt(frameId: String?, size: Dp, modifier: Modifier = Modifier) {
    val frame = ProfileFrameCollection.find(frameId) ?: return
    val resources = LocalContext.current.resources
    var bitmap by remember(frame.drawable) { mutableStateOf<android.graphics.Bitmap?>(FrameBitmaps.cached(frame.drawable)) }
    LaunchedEffect(frame.drawable) { bitmap = FrameBitmaps.load(resources, frame.drawable) }
    Box(modifier.requiredSize(size * 1.38f), contentAlignment = Alignment.Center) {
        bitmap?.let { Image(
            bitmap = it.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
        }
        if (frame == ProfileFrameCollection.proFrame) ProFramePlate(size)
        if (frame == ProfileFrameCollection.throneFrame) ProFramePlate(size, "1")
    }
}

/** Ruby-and-gold "PRO" plate sitting on the PRO frame's bottom jewel; scales with the avatar. */
@Composable
private fun ProFramePlate(size: Dp, label: String = "PRO") {
    val scale = size.value / 100f
    Box(
        Modifier
            .offset(y = size * .47f)
            .border(
                (1.5f * scale).coerceAtLeast(1f).dp,
                Brush.verticalGradient(listOf(Color(0xFFFFF1B8), Color(0xFFD4A21F), Color(0xFF8A5A0B))),
                RoundedCornerShape(50),
            )
            .background(
                Brush.verticalGradient(listOf(Color(0xFFD7263D), Color(0xFF8E0F24))),
                RoundedCornerShape(50),
            )
            .padding(horizontal = (9f * scale).dp, vertical = (1.5f * scale).dp),
        contentAlignment = Alignment.Center,
    ) {
        // Tight line box: the default body line height made the plate tall on small avatars.
        val fontSize = (13f * scale).coerceAtLeast(6f).sp
        Text(
            label,
            color = Color(0xFFFFE6A0),
            fontSize = fontSize,
            lineHeight = fontSize,
            fontWeight = FontWeight.Black,
            letterSpacing = (1.2f * scale).sp,
            maxLines = 1,
            style = androidx.compose.ui.text.TextStyle(
                platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
                    alignment = androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
                    trim = androidx.compose.ui.text.style.LineHeightStyle.Trim.Both,
                ),
            ),
        )
    }
}

@kotlinx.serialization.Serializable
private data class PublicFrameDto(
    @kotlinx.serialization.SerialName("profile_frame_id") val profileFrameId: String? = null,
    @kotlinx.serialization.SerialName("expires_at") val expiresAt: String? = null,
    @kotlinx.serialization.SerialName("server_time") val serverTime: String? = null,
)

/** Effective frames with bounded cache and a server-derived reward expiry. */
internal object PublicFrames {
    private data class Entry(val frame: String?, val savedAt: Long, val week: String, val expiresAt: Long?)
    private val cache = java.util.concurrent.ConcurrentHashMap<String, Entry>()
    private fun weekKey(): String = java.time.LocalDate.now(java.time.ZoneId.of("Europe/Istanbul"))
        .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).toString()

    fun invalidate(userId: String) { cache.remove(userId) }

    fun rewardDeadline(userId: String): Long? = cache[userId]?.expiresAt

    suspend fun get(userId: String): String? {
        val now = System.currentTimeMillis()
        cache[userId]?.takeIf { it.week == weekKey() && now - it.savedAt < 30_000L && (it.expiresAt == null || now < it.expiresAt) }?.let { return it.frame }
        val result = com.sonharf.game.data.SupabaseProvider.client.postgrest.rpc(
            "get_public_profile_frame_v2",
            kotlinx.serialization.json.buildJsonObject { put("p_user_id", userId) },
        ).decodeList<PublicFrameDto>().firstOrNull()
        val deadline = throneRewardDeadline(result?.serverTime, result?.expiresAt, now)
        val frame = result?.profileFrameId?.takeIf { deadline == null || deadline > now }
        cache[userId] = Entry(frame, now, weekKey(), deadline)
        return frame
    }
}

/** The frame a player wears: yours instantly from the local cosmetics, others' from the server. */
@Composable
internal fun rememberPlayerFrame(userId: String?): String? {
    if (userId.isNullOrBlank()) return null
    val me = remember { runCatching { com.sonharf.game.data.SupabaseProvider.client.auth.currentUserOrNull()?.id }.getOrNull() }
    val localFrame = if (userId == me) SonHarfCosmetics.profileFrameId else null
    var frame by remember(userId) { mutableStateOf<String?>(localFrame) }
    val foreground = rememberAppForeground()
    LaunchedEffect(userId, localFrame, foreground) {
        if (!foreground) return@LaunchedEffect
        if (!com.sonharf.game.data.SupabaseProvider.configured) return@LaunchedEffect
        if (userId == me) {
            frame = localFrame
        }
        while (true) {
            // Keep a valid reward visible during refresh; revoke on expiry or transport failure.
            if (frame == ProfileFrameCollection.throneFrame.id &&
                (PublicFrames.rewardDeadline(userId) ?: 0L) <= System.currentTimeMillis()) frame = localFrame
            runCatching { PublicFrames.get(userId) }
                .onSuccess { frame = it }
                .onFailure { if (frame == ProfileFrameCollection.throneFrame.id) frame = localFrame }
            val zone = java.time.ZoneId.of("Europe/Istanbul")
            val boundary = java.time.LocalDate.now(zone)
                .with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.MONDAY))
                .atStartOfDay(zone).toInstant().toEpochMilli()
            kotlinx.coroutines.delay((minOf(boundary, PublicFrames.rewardDeadline(userId)?.takeIf { it > System.currentTimeMillis() } ?: Long.MAX_VALUE) - System.currentTimeMillis()).coerceIn(100L, 30_000L))
        }
    }
    return frame
}

/** Convert the server's remaining reward time to a client deadline, even with clock skew. */
internal fun throneRewardDeadline(serverTime: String?, expiresAt: String?, receivedAt: Long): Long? {
    if (expiresAt == null) return null
    return runCatching {
        receivedAt + (java.time.Instant.parse(expiresAt).toEpochMilli() - java.time.Instant.parse(serverTime).toEpochMilli())
    }.getOrDefault(receivedAt)
}

private object FrameBitmaps {
    private val cache = android.util.LruCache<Int, android.graphics.Bitmap>(12)
    fun cached(id: Int): android.graphics.Bitmap? = cache.get(id)
    suspend fun load(resources: android.content.res.Resources, id: Int): android.graphics.Bitmap? =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            cached(id) ?: android.graphics.BitmapFactory.decodeResource(resources,id,
                android.graphics.BitmapFactory.Options().apply { inScaled=false; inSampleSize=2 })?.also { cache.put(id,it) }
        }
}
