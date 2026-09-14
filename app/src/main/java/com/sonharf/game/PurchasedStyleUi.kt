package com.sonharf.game

import android.graphics.BitmapFactory
import android.app.Activity
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
import com.sonharf.game.billing.BillingManager
import com.sonharf.game.billing.PlayPurchaseVerification
import com.sonharf.game.billing.ProductCatalog
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

internal object PurchasedFrameCatalog {
    const val STARTER_BLUE = "frame_round_starter_blue"
    const val STARTER_PINK = "frame_round_starter_pink"
    const val STARTER_NEUTRAL = "frame_round_starter_neutral"
    const val OCEAN = "frame_round_ocean"
    const val BOTANIC = "frame_round_botanic"
    const val LILAC = "frame_round_lilac"
    const val ROSE = "frame_round_rose"
    const val GOLDEN_AVATAR = "frame_round_golden_avatar"

    /** Only these standardized circular IDs can be rendered by the current client. */
    val ids = setOf(STARTER_BLUE, STARTER_PINK, STARTER_NEUTRAL, OCEAN, BOTANIC, LILAC, ROSE, GOLDEN_AVATAR)

    fun drawable(id: String?): Int? = when (id) {
        STARTER_BLUE -> R.drawable.profile_frame_round_starter_blue
        STARTER_PINK -> R.drawable.profile_frame_round_starter_pink
        STARTER_NEUTRAL -> R.drawable.profile_frame_round_starter_neutral
        OCEAN -> R.drawable.profile_frame_round_ocean
        BOTANIC -> R.drawable.profile_frame_round_botanic
        LILAC -> R.drawable.profile_frame_round_lilac
        ROSE -> R.drawable.profile_frame_round_rose
        GOLDEN_AVATAR -> R.drawable.profile_frame_round_golden_avatar
        else -> null
    }

    fun accent(id: String?): Color = when (id) {
        STARTER_BLUE, OCEAN -> Color(0xFF1677FF)
        STARTER_PINK, ROSE -> Color(0xFFE45A91)
        STARTER_NEUTRAL -> Color(0xFF718096)
        BOTANIC -> Color(0xFF2FAE68)
        LILAC -> Color(0xFF7257D8)
        GOLDEN_AVATAR -> Color(0xFFD7A72E)
        else -> Color(0xFF8A97A8)
    }
}

private data class PurchasedFrameSpec(
    val id: String,
    val titleTr: String,
    val titleEn: String,
    val subtitleTr: String,
    val subtitleEn: String,
    @DrawableRes val drawable: Int?,
    val accent: Color,
    val accessTr: String,
    val accessEn: String,
    val sourceIcon: Int,
)

private val purchasedFrameSpecs = listOf(
    PurchasedFrameSpec(PurchasedFrameCatalog.STARTER_BLUE, "Başlangıç Mavi", "Starter Blue", "Yeni hesaplar için ücretsiz yuvarlak çerçeve", "Free round frame for new accounts", R.drawable.profile_frame_round_starter_blue, Color(0xFF1677FF), "ÜCRETSİZ", "FREE", R.drawable.style_icon_user),
    PurchasedFrameSpec(PurchasedFrameCatalog.STARTER_PINK, "Başlangıç Pembe", "Starter Pink", "Yeni hesaplar için ücretsiz yuvarlak çerçeve", "Free round frame for new accounts", R.drawable.profile_frame_round_starter_pink, Color(0xFFE45A91), "ÜCRETSİZ", "FREE", R.drawable.style_icon_user),
    PurchasedFrameSpec(PurchasedFrameCatalog.STARTER_NEUTRAL, "Başlangıç Nötr", "Starter Neutral", "Yeni hesaplar için ücretsiz yuvarlak çerçeve", "Free round frame for new accounts", R.drawable.profile_frame_round_starter_neutral, Color(0xFF718096), "ÜCRETSİZ", "FREE", R.drawable.style_icon_user),
    PurchasedFrameSpec(PurchasedFrameCatalog.OCEAN, "Okyanus Halkası", "Ocean Ring", "Sadece görünüm için mavi yuvarlak çerçeve", "Blue round frame, visual only", R.drawable.profile_frame_round_ocean, Color(0xFF1677FF), "GOOGLE PLAY", "GOOGLE PLAY", R.drawable.style_icon_coin),
    PurchasedFrameSpec(PurchasedFrameCatalog.BOTANIC, "Botanik Halka", "Botanic Ring", "Sadece görünüm için yeşil yuvarlak çerçeve", "Green round frame, visual only", R.drawable.profile_frame_round_botanic, Color(0xFF2FAE68), "GOOGLE PLAY", "GOOGLE PLAY", R.drawable.style_icon_coin),
    PurchasedFrameSpec(PurchasedFrameCatalog.LILAC, "Lila Halo", "Lilac Halo", "Sadece görünüm için lila yuvarlak çerçeve", "Lilac round frame, visual only", R.drawable.profile_frame_round_lilac, Color(0xFF7257D8), "GOOGLE PLAY", "GOOGLE PLAY", R.drawable.style_icon_coin),
    PurchasedFrameSpec(PurchasedFrameCatalog.ROSE, "Gül Işığı", "Rose Glow", "Sadece görünüm için pembe yuvarlak çerçeve", "Rose round frame, visual only", R.drawable.profile_frame_round_rose, Color(0xFFE45A91), "GOOGLE PLAY", "GOOGLE PLAY", R.drawable.style_icon_coin),
    PurchasedFrameSpec(PurchasedFrameCatalog.GOLDEN_AVATAR, "Golden Avatar", "Golden Avatar", "Yalnızca aktif VIP / PRO oyuncular için", "Only for active VIP / PRO players", R.drawable.profile_frame_round_golden_avatar, Color(0xFFD7A72E), "VIP / PRO", "VIP / PRO", R.drawable.style_icon_trophy),
)

/**
 * Decode drawable-nodpi assets through a raw stream instead of Compose's resource decoder.
 * Some purchased PNGs are accepted by Android packaging but fail through ImageBitmap.imageResource
 * on specific devices. decodeStream is bounded to the local APK resource and cannot trigger network IO.
 */
@Composable
private fun rememberStyleBitmap(@DrawableRes drawable: Int?): ImageBitmap? {
    val resources = LocalContext.current.resources
    return remember(resources, drawable) {
        if (drawable == null) null else runCatching {
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
    @DrawableRes drawable: Int?,
    frameId: String,
    modifier: Modifier,
): Boolean {
    val bitmap = rememberStyleBitmap(drawable)
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
        return true
    }

    // Fail-safe visual frame: avatar stays visible and usable even if the packaged PNG cannot decode.
    val accent = PurchasedFrameCatalog.accent(frameId)
    Box(
        modifier = modifier
            .padding(5.dp)
            .clip(CircleShape)
            .border(4.dp, accent, CircleShape)
            .padding(4.dp)
            .border(2.dp, accent.copy(alpha = .45f), CircleShape),
    )
    return false
}

@Composable
internal fun PurchasedProfileFrameOverlay(frameId: String?, modifier: Modifier = Modifier) {
    if (frameId.isNullOrBlank()) return
    SafeFrameArtwork(
        drawable = PurchasedFrameCatalog.drawable(frameId),
        frameId = frameId,
        modifier = modifier,
    )
}

private fun legacyFrameSpec(item: ShopItemDto): PurchasedFrameSpec = PurchasedFrameSpec(
    id = item.id,
    titleTr = item.nameTr,
    titleEn = item.nameEn,
    subtitleTr = item.descriptionTr.ifBlank { "Satın alınmış profil çerçevesi" },
    subtitleEn = item.descriptionEn.ifBlank { "Purchased profile frame" },
    drawable = null,
    accent = PurchasedFrameCatalog.accent(item.id),
    accessTr = if (item.vipOnly) "VIP / PREMIUM" else "MAĞAZA",
    accessEn = if (item.vipOnly) "VIP / PREMIUM" else "SHOP",
    sourceIcon = if (item.vipOnly) R.drawable.style_icon_trophy else R.drawable.style_icon_coin,
)

@Composable
internal fun PurchasedProfileFramesStoreRow(backend: OnlineGameBackend?) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    var shopItems by remember { mutableStateOf<Map<String, ShopItemDto>>(emptyMap()) }
    var inventory by remember { mutableStateOf<Set<String>>(emptySet()) }
    var equippedId by remember { mutableStateOf<String?>(SonHarfCosmetics.profileFrameId) }
    var vipFrameAccess by remember { mutableStateOf(false) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var notice by remember { mutableStateOf<String?>(null) }
    var playProducts by remember { mutableStateOf<Map<String, ProductDetails>>(emptyMap()) }
    val playProductByFrame = remember {
        mapOf(
            PurchasedFrameCatalog.OCEAN to ProductCatalog.PROFILE_FRAME_OCEAN,
            PurchasedFrameCatalog.BOTANIC to ProductCatalog.PROFILE_FRAME_BOTANIC,
            PurchasedFrameCatalog.LILAC to ProductCatalog.PROFILE_FRAME_LILAC,
            PurchasedFrameCatalog.ROSE to ProductCatalog.PROFILE_FRAME_ROSE,
        )
    }

    val refreshPurchasedFrames: suspend () -> Unit = {
        loading = true
        val b = backend
        if (b == null) {
            notice = sh("Profil Style sunucusu kullanılamıyor; çerçeveler güvenli önizleme modunda gösteriliyor.", "Profile Style server is unavailable; frames are shown in safe preview mode.")
            loading = false
            return
        }
        runCatching {
            withTimeout(12_000L) {
                val shop = b.getShopItems().filter { it.kind == "profile_frame" }
                val state = b.getProfileFrameState()
                val owned = state?.ownedProfileFrames?.toSet() ?: b.getInventory()
                val equipped = b.getEquippedCosmetics()
                shopItems = shop.associateBy { it.id }
                inventory = owned
                equippedId = state?.equippedProfileFrame ?: equipped?.profileFrameId
                vipFrameAccess = state?.vipProFrameAccess == true
                SonHarfCosmetics.apply(equipped)
            }
        }.onFailure {
            notice = sh("Profil Style verileri alınamadı; bölüm açık kalacak ve daha sonra yeniden denenebilir.", "Profile Style data could not be loaded; the section remains open and can be retried later.")
        }
        loading = false\n    }
    val billing = remember {
        BillingManager(
            context = context,
            onPurchase = { purchase ->
                val productId = purchase.products.firstOrNull()
                if (productId != null) {
                    scope.launch {
                        busyId = productId
                        runCatching { PlayPurchaseVerification.verify(productId, purchase.purchaseToken) }
                            .onSuccess { refreshPurchasedFrames() }
                            .onFailure { notice = sh("Google Play satın alması doğrulanamadı.", "Google Play purchase could not be verified.") }
                        busyId = null
                    }
                }
            },
            onMessage = { notice = it },
        )
    }

    DisposableEffect(billing) {
        billing.connect { billing.queryOneTimeProducts(playProductByFrame.values.toList()) { playProducts = it } }
        onDispose { billing.close() }
    }



    LaunchedEffect(backend) {
        runCatching { refreshPurchasedFrames() }.onFailure {
            loading = false
            notice = sh("Profil Style geçici olarak kullanılamıyor.", "Profile Style is temporarily unavailable.")
        }
    }

    val displaySpecs = remember(shopItems, inventory, equippedId, vipFrameAccess) {
        val knownById = purchasedFrameSpecs.associateBy { it.id }
        // Backend active shop_items is authoritative for discovery and pricing. Known purchased
        // package IDs receive their verified artwork; unknown/legacy IDs keep procedural rendering.
        val activeCatalog = shopItems.values
            .sortedBy { it.sortOrder }
            .map { item -> knownById[item.id] ?: legacyFrameSpec(item) }
        // Inactive reward/event items must not appear for sale, but an already-owned or equipped
        // item remains renderable even when it is absent from the active backend catalogue.
        val recoveryOwned = purchasedFrameSpecs.filter { spec ->
            spec.id !in shopItems && (spec.id in inventory || equippedId == spec.id)
        }
        val playCatalog = purchasedFrameSpecs.filter { it.id in playProductByFrame || it.id == PurchasedFrameCatalog.GOLDEN_AVATAR }
        (activeCatalog + recoveryOwned + playCatalog).distinctBy { it.id }
    }

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        if (loading) {
            Text(sh("Çerçeveler yükleniyor…", "Loading frames…"), color = Color(0xFF6F7C8D), fontSize = 9.sp)
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(displaySpecs, key = { it.id }) { spec ->
                val item = shopItems[spec.id]
                val playProduct = playProductByFrame[spec.id]?.let(playProducts::get)
                val owned = spec.id in inventory
                val equipped = equippedId == spec.id
                val vipLocked = spec.id == PurchasedFrameCatalog.GOLDEN_AVATAR && !vipFrameAccess
                val frameBitmap = rememberStyleBitmap(spec.drawable)
                val assetReady = spec.drawable == null || frameBitmap != null
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
                                        contentScale = ContentScale.Fit,
                                    )
                                } else {
                                    Box(
                                        Modifier.size(82.dp)
                                            .border(4.dp, spec.accent, CircleShape)
                                            .padding(4.dp)
                                            .border(2.dp, spec.accent.copy(alpha = .45f), CircleShape),
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
                                sh("Orijinal görsel onarılıyor", "Original artwork is being repaired"),
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
                                    equipped -> sh("KULLANIMDA", "EQUIPPED")
                                    owned -> sh("SAHİPSİN", "OWNED")
                                    vipLocked -> sh("VIP / PRO KİLİTLİ", "VIP / PRO LOCKED")
                                    playProduct?.oneTimePurchaseOfferDetails != null -> playProduct.oneTimePurchaseOfferDetails?.formattedPrice.orEmpty()
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
                                vipLocked -> Text(sh("VIP", "VIP"), color = spec.accent, fontSize = 7.sp, fontWeight = FontWeight.Black)
                                !assetReady && !owned -> Text(sh("KAPALI", "LOCKED"), color = Color(0xFF8A97A8), fontSize = 7.sp, fontWeight = FontWeight.Black)
                                else -> Button(
                                    onClick = {
                                        val b = backend ?: return@Button
                                        scope.launch {
                                            busyId = spec.id
                                            try {
                                                runCatching {
                                                    withTimeout(12_000L) {
                                                        if (spec.id == PurchasedFrameCatalog.GOLDEN_AVATAR) {
                                                            b.equipShopItem(spec.id)
                                                        } else if (!owned && playProduct != null) {
                                                            val host = activity ?: error("activity_required")
                                                            val result = billing.launchProduct(host, playProduct)
                                                            if (result.responseCode != BillingClient.BillingResponseCode.OK) error("billing_unavailable")
                                                        } else {
                                                            b.equipShopItem(spec.id)
                                                        }
                                                    }
                                                }.onSuccess {
                                                    notice = sh("${spec.titleTr} kullanılıyor.", "${spec.titleEn} equipped.")
                                                    refreshPurchasedFrames()
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
                                    enabled = backend != null && !vipLocked && (spec.id == PurchasedFrameCatalog.GOLDEN_AVATAR || owned || (playProduct?.oneTimePurchaseOfferDetails != null && assetReady)) && busyId == null,
                                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 3.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = spec.accent, contentColor = Color.White),
                                    shape = RoundedCornerShape(10.dp),
                                ) {
                                    Text(if (busyId == spec.id) "…" else if (owned || spec.id == PurchasedFrameCatalog.GOLDEN_AVATAR) sh("KULLAN", "EQUIP") else sh("SATIN AL", "BUY"), fontSize = 8.sp, fontWeight = FontWeight.Black)
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
