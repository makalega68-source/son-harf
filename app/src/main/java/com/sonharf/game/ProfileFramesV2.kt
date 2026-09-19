package com.sonharf.game

import android.app.Activity
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.sonharf.game.billing.BillingManager
import com.sonharf.game.billing.PlayPurchaseVerification
import com.sonharf.game.billing.ProductCatalog
import com.sonharf.game.data.EquippedCosmeticsDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.equipShopItem
import com.sonharf.game.data.getEquippedCosmetics
import com.sonharf.game.data.getInventory
import kotlinx.coroutines.launch

/**
 * Profile Frames V2 is deliberately separate from the retired historical frame renderer.
 * Default/PRO frames are automatic presentation; paid frames require verified server ownership.
 */
internal object ProfileFrameV2Catalog {
    const val PINK_BLOSSOM = ProductCatalog.PROFILE_FRAME_PINK_BLOSSOM
    const val BLUE_ROYAL = ProductCatalog.PROFILE_FRAME_BLUE_ROYAL
    const val AMETHYST = ProductCatalog.PROFILE_FRAME_AMETHYST
    const val EMERALD = ProductCatalog.PROFILE_FRAME_EMERALD

    val paidIds: Set<String> = setOf(PINK_BLOSSOM, BLUE_ROYAL, AMETHYST, EMERALD)

    data class Visual(@DrawableRes val drawable: Int, val photoRatio: Float)

    // Photo diameters are alpha-geometry validated against the 512x512 transparent artwork.
    // Ornate frames intentionally overlap only the outer rim, never the readable avatar area.
    private val defaultVisual = Visual(R.drawable.profile_frame_default_gray, 301f / 512f)
    private val proVisual = Visual(R.drawable.profile_frame_pro_gold, 308f / 512f)
    private val paidVisuals = mapOf(
        PINK_BLOSSOM to Visual(R.drawable.profile_frame_shop_pink_blossom, 287f / 512f),
        BLUE_ROYAL to Visual(R.drawable.profile_frame_shop_blue_royal, 321f / 512f),
        AMETHYST to Visual(R.drawable.profile_frame_shop_amethyst, 293f / 512f),
        EMERALD to Visual(R.drawable.profile_frame_shop_emerald, 293f / 512f),
    )

    fun ownedPaidFrame(equippedId: String?, ownedIds: Set<String>): String? =
        equippedId?.takeIf { it in paidIds && it in ownedIds }

    fun visual(equippedPaidFrameId: String?, isPro: Boolean): Visual =
        paidVisuals[equippedPaidFrameId] ?: if (isPro) proVisual else defaultVisual
}

@Composable
internal fun ProfileFrameAvatarBytesV2(
    avatarBytes: ByteArray?,
    name: String,
    outerSize: Dp,
    equippedPaidFrameId: String?,
    isPro: Boolean,
) {
    val visual = remember(equippedPaidFrameId, isPro) {
        ProfileFrameV2Catalog.visual(equippedPaidFrameId, isPro)
    }
    val bitmap = remember(avatarBytes) {
        avatarBytes?.let { bytes -> runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }.getOrNull() }
    }
    Box(modifier = Modifier.size(outerSize), contentAlignment = Alignment.Center) {
        val photoSize = outerSize * visual.photoRatio
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.size(photoSize).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Surface(modifier = Modifier.size(photoSize), shape = CircleShape, color = SonHarfTheme.PrimarySoft) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        name.trim().firstOrNull()?.uppercase() ?: "O",
                        color = SonHarfTheme.Primary,
                        fontSize = (photoSize.value * .38f).sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
        Image(
            painter = painterResource(visual.drawable),
            contentDescription = null,
            modifier = Modifier.size(outerSize),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
internal fun ProfileFrameAvatarPathV2(
    avatarPath: String?,
    gender: String?,
    name: String,
    outerSize: Dp,
    equippedPaidFrameId: String?,
    isPro: Boolean,
    accent: Color = SonHarfCyan,
    visible: Boolean = true,
    showGenderBadge: Boolean = true,
) {
    val visual = remember(equippedPaidFrameId, isPro) {
        ProfileFrameV2Catalog.visual(equippedPaidFrameId, isPro)
    }
    Box(modifier = Modifier.size(outerSize), contentAlignment = Alignment.Center) {
        ProfilePhotoAvatarWithGender(
            avatarPath = avatarPath,
            gender = gender,
            name = name,
            size = outerSize * visual.photoRatio,
            accent = accent,
            visible = visible,
            showGenderBadge = showGenderBadge,
        )
        Image(
            painter = painterResource(visual.drawable),
            contentDescription = null,
            modifier = Modifier.size(outerSize),
            contentScale = ContentScale.Fit,
        )
    }
}

private data class ProfileFrameStoreSpec(
    val productId: String,
    val titleTr: String,
    val titleEn: String,
    val subtitleTr: String,
    val subtitleEn: String,
    val accent: Color,
)

private val profileFrameStoreSpecs = listOf(
    ProfileFrameStoreSpec(
        ProfileFrameV2Catalog.PINK_BLOSSOM,
        "Pembe Çiçek Premium",
        "Pink Blossom Premium",
        "Çiçek detaylı premium profil çerçevesi",
        "Premium floral profile frame",
        Color(0xFFE96FA5),
    ),
    ProfileFrameStoreSpec(
        ProfileFrameV2Catalog.BLUE_ROYAL,
        "Mavi Royal Premium",
        "Blue Royal Premium",
        "Mavi desenli premium profil çerçevesi",
        "Premium blue patterned profile frame",
        Color(0xFF3979D8),
    ),
    ProfileFrameStoreSpec(
        ProfileFrameV2Catalog.AMETHYST,
        "Ametist Fantastik",
        "Amethyst Fantasy",
        "Mor fantastik premium profil çerçevesi",
        "Premium amethyst fantasy profile frame",
        Color(0xFF8A5BD6),
    ),
    ProfileFrameStoreSpec(
        ProfileFrameV2Catalog.EMERALD,
        "Zümrüt Fantastik",
        "Emerald Fantasy",
        "Zümrüt fantastik premium profil çerçevesi",
        "Premium emerald fantasy profile frame",
        Color(0xFF2D9D78),
    ),
)

/** Google Play one-time purchase/equip surface for the four active paid frames. */
@Composable
internal fun ProfileFramesV2StoreRow(
    backend: OnlineGameBackend?,
    onChanged: () -> Unit = {},
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    var owned by remember { mutableStateOf<Set<String>>(emptySet()) }
    var equipped by remember { mutableStateOf<EquippedCosmeticsDto?>(null) }
    var products by remember { mutableStateOf<Map<String, ProductDetails>>(emptyMap()) }
    var busy by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() {
        val b = backend ?: return
        owned = b.getInventory()
        equipped = b.getEquippedCosmetics()
        SonHarfCosmetics.apply(equipped, owned)
        onChanged()
    }

    val billing = remember {
        BillingManager(
            context = context,
            onPurchase = { purchase ->
                val productId = purchase.products.firstOrNull()
                if (productId != null && productId in ProfileFrameV2Catalog.paidIds) {
                    scope.launch {
                        busy = productId
                        runCatching { PlayPurchaseVerification.verify(productId, purchase.purchaseToken) }
                            .onSuccess {
                                refresh()
                                notice = sh("Çerçeve hesabına eklendi.", "Frame added to your account.")
                            }
                            .onFailure {
                                notice = sh("Google Play satın alması doğrulanamadı.", "Google Play purchase could not be verified.")
                            }
                        busy = null
                    }
                }
            },
            onMessage = { notice = it; busy = null },
        )
    }

    DisposableEffect(billing) {
        billing.connect {
            billing.queryOneTimeProducts(ProductCatalog.profileFrameProducts) { products = it }
            billing.restorePurchases(ProductCatalog.profileFrameProducts.toSet())
        }
        onDispose { billing.close() }
    }

    LaunchedEffect(backend) { runCatching { refresh() } }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            sh("PREMIUM PROFİL ÇERÇEVELERİ", "PREMIUM PROFILE FRAMES"),
            color = SonHarfText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            sh("Tek ödeme • kalıcı • yalnızca kozmetik", "One-time purchase • permanent • cosmetic only"),
            color = SonHarfMuted,
            fontSize = 9.sp,
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 1.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(profileFrameStoreSpecs, key = { it.productId }) { spec ->
                val mine = spec.productId in owned
                val active = equipped?.profileFrameId == spec.productId && mine
                val product = products[spec.productId]
                val realPrice = product?.oneTimePurchaseOfferDetails?.formattedPrice
                val previewVisual = remember(spec.productId) { ProfileFrameV2Catalog.visual(spec.productId, false) }
                Surface(
                    modifier = Modifier.width(170.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = SonHarfSurface,
                    border = BorderStroke(if (active) 2.dp else 1.dp, if (active) spec.accent else SonHarfTheme.Border),
                ) {
                    Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(14.dp)).background(spec.accent.copy(alpha = .08f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Surface(
                                modifier = Modifier.size(104.dp * previewVisual.photoRatio),
                                shape = CircleShape,
                                color = spec.accent.copy(alpha = .14f),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        "A",
                                        color = spec.accent,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                    )
                                }
                            }
                            Image(
                                painter = painterResource(previewVisual.drawable),
                                contentDescription = null,
                                modifier = Modifier.size(104.dp),
                                contentScale = ContentScale.Fit,
                            )
                            if (active) Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2FAE68),
                                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp),
                            )
                        }
                        Text(sh(spec.titleTr, spec.titleEn), color = SonHarfText, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Text(sh(spec.subtitleTr, spec.subtitleEn), color = SonHarfMuted, fontSize = 8.sp, minLines = 2)
                        Text(
                            when {
                                active -> sh("KULLANILIYOR", "EQUIPPED")
                                mine -> sh("SATIN ALINDI", "OWNED")
                                realPrice != null -> realPrice
                                else -> ProductCatalog.PROFILE_FRAME_FALLBACK_PRICE_TRY
                            },
                            color = if (mine) Color(0xFF2FAE68) else spec.accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Button(
                            onClick = {
                                val b = backend ?: return@Button
                                scope.launch {
                                    busy = spec.productId
                                    if (mine) {
                                        runCatching { b.equipShopItem(spec.productId) }
                                            .onSuccess {
                                                refresh()
                                                notice = sh("Çerçeve kullanılıyor.", "Frame equipped.")
                                            }
                                            .onFailure { notice = sh("Çerçeve uygulanamadı.", "Frame could not be equipped.") }
                                    } else {
                                        val host = activity
                                        if (host == null || product == null || product.oneTimePurchaseOfferDetails == null) {
                                            notice = sh("Google Play ürünü henüz hazır değil.", "Google Play product is not ready yet.")
                                        } else {
                                            val result = billing.launchProduct(host, product)
                                            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                                                notice = sh("Google Play ödeme ekranı açılamadı.", "Google Play billing could not open.")
                                            }
                                        }
                                    }
                                    busy = null
                                }
                            },
                            enabled = backend != null && !active && busy == null && (mine || realPrice != null),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = spec.accent),
                            contentPadding = PaddingValues(vertical = 7.dp),
                        ) {
                            Text(
                                if (busy == spec.productId) "…" else if (mine) sh("KULLAN", "EQUIP") else sh("SATIN AL", "BUY"),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }
            }
        }
        notice?.let {
            Text(it, modifier = Modifier.fillMaxWidth(), color = SonHarfMuted, fontSize = 9.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(2.dp))
    }
}
