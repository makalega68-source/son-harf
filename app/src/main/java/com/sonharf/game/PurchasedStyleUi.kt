package com.sonharf.game

import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

internal object PurchasedFrameCatalog {
    const val GOLD = "frame_asset_gold"
    const val MINT = "frame_asset_mint"
    const val PURPLE = "frame_asset_purple"
    const val GREEN = "frame_asset_green"
    const val RED = "frame_asset_red"
    const val GOLD_CROWN = "frame_asset_gold_crown"

    val ids = setOf(GOLD, MINT, PURPLE, GREEN, RED, GOLD_CROWN)

    fun drawable(id: String?): Int? = when (id) {
        GOLD -> R.drawable.style_frame_gold
        MINT -> R.drawable.style_frame_mint
        PURPLE -> R.drawable.style_frame_purple
        GREEN -> R.drawable.style_frame_green
        RED -> R.drawable.style_frame_red
        GOLD_CROWN -> R.drawable.style_frame_gold_crown
        else -> null
    }

    fun accent(id: String?): Color = when (id) {
        RED -> Color(0xFFD84C4C)
        GREEN -> Color(0xFF2FAE68)
        MINT -> Color(0xFF32BFB3)
        PURPLE -> Color(0xFF7257D8)
        GOLD -> Color(0xFFD7A72E)
        GOLD_CROWN -> Color(0xFFE0A51C)
        else -> Color(0xFF8A97A8)
    }
}

private data class PurchasedFrameSpec(
    val id: String,
    val titleTr: String,
    val titleEn: String,
    val subtitleTr: String,
    val subtitleEn: String,
    @DrawableRes val drawable: Int,
    val accent: Color,
    val accessTr: String,
    val accessEn: String,
    val sourceIcon: Int,
)

private val purchasedFrameSpecs = listOf(
    PurchasedFrameSpec(PurchasedFrameCatalog.RED, "Kırmızı Hat", "Red Line", "Sade başlangıç ve günlük kullanım çerçevesi", "Clean starter and everyday frame", R.drawable.style_frame_red, Color(0xFFD84C4C), "SIRADAN", "STANDARD", R.drawable.style_icon_user),
    PurchasedFrameSpec(PurchasedFrameCatalog.GREEN, "Zümrüt Hat", "Emerald Line", "Dengeli zümrüt profil çerçevesi", "Balanced emerald profile frame", R.drawable.style_frame_green, Color(0xFF2FAE68), "MAĞAZA", "SHOP", R.drawable.style_icon_coin),
    PurchasedFrameSpec(PurchasedFrameCatalog.MINT, "Buz Mint", "Ice Mint", "Temiz ve modern mint çerçeve", "Clean modern mint frame", R.drawable.style_frame_mint, Color(0xFF32BFB3), "MAĞAZA", "SHOP", R.drawable.style_icon_coin),
    PurchasedFrameSpec(PurchasedFrameCatalog.PURPLE, "Mor Spektrum", "Violet Spectrum", "Premium mor profil vurgusu", "Premium violet profile accent", R.drawable.style_frame_purple, Color(0xFF7257D8), "MAĞAZA", "SHOP", R.drawable.style_icon_coin),
    PurchasedFrameSpec(PurchasedFrameCatalog.GOLD, "Altın Hat", "Gold Line", "VIP ve prestij koleksiyonuna uygun metalik çerçeve", "Metallic frame for VIP and prestige collection", R.drawable.style_frame_gold, Color(0xFFD7A72E), "VIP / PREMIUM", "VIP / PREMIUM", R.drawable.style_icon_trophy),
    PurchasedFrameSpec(PurchasedFrameCatalog.GOLD_CROWN, "Altın Taç", "Gold Crown", "Yüksek lig ve prestij ödülü", "High-league prestige reward", R.drawable.style_frame_gold_crown, Color(0xFFE0A51C), "LİG ÖDÜLÜ", "LEAGUE REWARD", R.drawable.style_icon_trophy),
)

/**
 * Decode drawable-nodpi assets through a raw stream instead of Compose's resource decoder.
 * Some purchased PNGs are accepted by Android packaging but fail through ImageBitmap.imageResource
 * on specific devices. decodeStream is bounded to the local APK resource and cannot trigger network IO.
 */
@Composable
private fun rememberStyleBitmap(@DrawableRes drawable: Int): ImageBitmap? {
    val resources = LocalContext.current.resources
    return remember(resources, drawable) {
        runCatching {
            resources.openRawResource(drawable).use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }.getOrNull()
    }
}

@Composable
private fun SafeStyleDrawable(
    @DrawableRes drawable: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    tint: Color? = null,
) {
    val bitmap = rememberStyleBitmap(drawable)
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale,
            colorFilter = tint?.let { ColorFilter.tint(it) },
        )
    } else {
        // Never paint a broken-image glyph over user content. A missing decorative icon is harmless.
        Spacer(modifier)
    }
}

@Composable
private fun SafeFrameArtwork(
    @DrawableRes drawable: Int,
    frameId: String,
    modifier: Modifier,
): Boolean {
    val bitmap = rememberStyleBitmap(drawable)
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.FillBounds,
        )
        return true
    }

    // Fail-safe visual frame: avatar stays visible and usable even if the packaged PNG cannot decode.
    val accent = PurchasedFrameCatalog.accent(frameId)
    Box(
        modifier = modifier
            .padding(5.dp)
            .border(4.dp, accent, RoundedCornerShape(18.dp))
            .padding(4.dp)
            .border(2.dp, accent.copy(alpha = .45f), RoundedCornerShape(14.dp)),
    )
    return false
}

@Composable
internal fun PurchasedProfileFrameOverlay(frameId: String?, modifier: Modifier = Modifier) {
    val drawable = PurchasedFrameCatalog.drawable(frameId) ?: return
    SafeFrameArtwork(
        drawable = drawable,
        frameId = frameId.orEmpty(),
        modifier = modifier,
    )
}

@Composable
internal fun PurchasedProfileFramesStoreRow(backend: OnlineGameBackend?) {
    val scope = rememberCoroutineScope()
    var shopItems by remember { mutableStateOf<Map<String, ShopItemDto>>(emptyMap()) }
    var inventory by remember { mutableStateOf<Set<String>>(emptySet()) }
    var equippedId by remember { mutableStateOf<String?>(SonHarfCosmetics.profileFrameId) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var notice by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        loading = true
        val b = backend
        if (b == null) {
            notice = sh("Profil Style sunucusu kullanılamıyor; çerçeveler güvenli önizleme modunda gösteriliyor.", "Profile Style server is unavailable; frames are shown in safe preview mode.")
            loading = false
            return
        }
        runCatching {
            withTimeout(12_000L) {
                val shop = b.getShopItems().filter { it.kind == "profile_frame" && it.id in PurchasedFrameCatalog.ids }
                val owned = b.getInventory().toSet()
                val equipped = b.getEquippedCosmetics()
                shopItems = shop.associateBy { it.id }
                inventory = owned
                equippedId = equipped?.profileFrameId
                SonHarfCosmetics.apply(equipped)
            }
        }.onFailure {
            notice = sh("Profil Style verileri alınamadı; bölüm açık kalacak ve daha sonra yeniden denenebilir.", "Profile Style data could not be loaded; the section remains open and can be retried later.")
        }
        loading = false
    }

    LaunchedEffect(backend) {
        runCatching { reload() }.onFailure {
            loading = false
            notice = sh("Profil Style geçici olarak kullanılamıyor.", "Profile Style is temporarily unavailable.")
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        if (loading) {
            Text(sh("Çerçeveler yükleniyor…", "Loading frames…"), color = Color(0xFF6F7C8D), fontSize = 9.sp)
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(purchasedFrameSpecs, key = { it.id }) { spec ->
                val item = shopItems[spec.id]
                val owned = spec.id in inventory
                val equipped = equippedId == spec.id
                val frameBitmap = rememberStyleBitmap(spec.drawable)
                val assetReady = frameBitmap != null
                Surface(
                    modifier = Modifier.width(164.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = BorderStroke(if (equipped) 2.dp else 1.dp, if (equipped) spec.accent else Color(0xFFD5E0EA)),
                ) {
                    Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            Modifier.fillMaxWidth().height(102.dp).clip(RoundedCornerShape(13.dp)).background(spec.accent.copy(alpha = .08f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Surface(modifier = Modifier.size(62.dp), shape = CircleShape, color = Color(0xFFF5F8FC)) {
                                    SafeStyleDrawable(
                                        drawable = R.drawable.style_icon_user,
                                        modifier = Modifier.padding(15.dp),
                                        tint = Color(0xFF142033),
                                    )
                                }
                                if (frameBitmap != null) {
                                    Image(
                                        bitmap = frameBitmap,
                                        contentDescription = null,
                                        modifier = Modifier.size(86.dp),
                                        contentScale = ContentScale.FillBounds,
                                    )
                                } else {
                                    Box(
                                        Modifier.size(82.dp)
                                            .border(4.dp, spec.accent, RoundedCornerShape(17.dp))
                                            .padding(4.dp)
                                            .border(2.dp, spec.accent.copy(alpha = .45f), RoundedCornerShape(13.dp)),
                                    )
                                }
                            }
                            if (equipped) {
                                Surface(
                                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                                    shape = RoundedCornerShape(99.dp),
                                    color = Color(0xFF2FAE68),
                                ) {
                                    Text(sh("AKTİF", "ACTIVE"), Modifier.padding(horizontal = 7.dp, vertical = 4.dp), color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                        Text(sh(spec.titleTr, spec.titleEn), color = Color(0xFF142033), fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
                        Text(sh(spec.subtitleTr, spec.subtitleEn), color = Color(0xFF6F7C8D), fontSize = 8.sp, minLines = 2, maxLines = 3)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            SafeStyleDrawable(drawable = spec.sourceIcon, modifier = Modifier.size(12.dp), tint = spec.accent)
                            Text(sh(spec.accessTr, spec.accessEn), color = spec.accent, fontSize = 7.sp, fontWeight = FontWeight.Black)
                        }
                        if (!assetReady) {
                            Text(
                                sh("Görsel doğrulanamadı", "Artwork unavailable"),
                                color = Color(0xFF8A97A8),
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            if (!owned && !equipped && assetReady) {
                                SafeStyleDrawable(drawable = R.drawable.style_icon_coin, modifier = Modifier.size(15.dp), tint = spec.accent)
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                when {
                                    equipped -> sh("AKTİF", "ACTIVE")
                                    owned -> sh("SAHİPSİN", "OWNED")
                                    item != null -> "${item.diamondPrice} SC"
                                    else -> sh(spec.accessTr, spec.accessEn)
                                },
                                modifier = Modifier.weight(1f),
                                color = if (owned || equipped) Color(0xFF2FAE68) else spec.accent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                            )
                            when {
                                equipped -> Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF2FAE68), modifier = Modifier.size(22.dp))
                                !assetReady -> Text(sh("KAPALI", "LOCKED"), color = Color(0xFF8A97A8), fontSize = 7.sp, fontWeight = FontWeight.Black)
                                else -> Button(
                                    onClick = {
                                        val b = backend ?: return@Button
                                        scope.launch {
                                            busyId = spec.id
                                            try {
                                                runCatching {
                                                    withTimeout(12_000L) {
                                                        if (!owned) b.purchaseShopItem(spec.id)
                                                        b.equipShopItem(spec.id)
                                                    }
                                                }.onSuccess {
                                                    notice = sh("${spec.titleTr} kullanılıyor.", "${spec.titleEn} equipped.")
                                                    reload()
                                                }.onFailure { error ->
                                                    notice = if ("insufficient_diamonds" in error.message.orEmpty()) {
                                                        sh("Yeterli Son Coin'in yok.", "Not enough Son Coin.")
                                                    } else {
                                                        sh("Style işlemi tamamlanamadı. Daha sonra tekrar dene.", "Style action could not be completed. Try again later.")
                                                    }
                                                }
                                            } finally {
                                                busyId = null
                                            }
                                        }
                                    },
                                    enabled = backend != null && (owned || item != null) && busyId == null,
                                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 3.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = spec.accent, contentColor = Color.White),
                                    shape = RoundedCornerShape(10.dp),
                                ) {
                                    Text(if (busyId == spec.id) "…" else if (owned) sh("KULLAN", "EQUIP") else sh("AL", "BUY"), fontSize = 8.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }
        }
        notice?.let { Text(it, color = Color(0xFF6F7C8D), fontSize = 9.sp) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            SafeStyleDrawable(drawable = R.drawable.style_icon_palette, modifier = Modifier.size(16.dp), tint = Color(0xFF1677FF))
            Text(
                sh("Satın alınan çerçeveler yalnızca görünümü değiştirir; oyun gücü vermez.", "Purchased frames only change appearance; they grant no gameplay power."),
                color = Color(0xFF6F7C8D),
                fontSize = 8.sp,
            )
        }
    }
}
