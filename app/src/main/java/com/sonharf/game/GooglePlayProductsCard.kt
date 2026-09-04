package com.sonharf.game

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.sonharf.game.billing.BillingManager
import com.sonharf.game.billing.PlayPurchaseVerification
import com.sonharf.game.billing.ProductCatalog
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.StoreCatalogConfigDto
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.getStoreCatalogConfig
import com.sonharf.game.data.trackStoreEvent
import kotlinx.coroutines.launch

private enum class PlayStoreSection { COINS, PACKS }

@Composable
fun GooglePlayProductsCard(onPurchased: () -> Unit = {}) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GooglePlayCoinProductsCard(onPurchased)
        GooglePlayPackProductsCard(onPurchased)
    }
}

@Composable
fun GooglePlayCoinProductsCard(onPurchased: () -> Unit = {}) =
    GooglePlaySectionCard(PlayStoreSection.COINS, onPurchased)

@Composable
fun GooglePlayPackProductsCard(onPurchased: () -> Unit = {}) =
    GooglePlaySectionCard(PlayStoreSection.PACKS, onPurchased)

@Composable
private fun GooglePlaySectionCard(section: PlayStoreSection, onPurchased: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val backend = remember { if (SupabaseProvider.configured) runCatching { OnlineGameBackend() }.getOrNull() else null }
    var products by remember { mutableStateOf<Map<String, ProductDetails>>(emptyMap()) }
    var config by remember { mutableStateOf<Map<String, StoreCatalogConfigDto>>(emptyMap()) }
    var busy by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf("") }
    var connected by remember { mutableStateOf(false) }

    val sectionIds = when (section) {
        PlayStoreSection.COINS -> listOf(ProductCatalog.COINS_500, ProductCatalog.COINS_1500, ProductCatalog.COINS_3500, ProductCatalog.COINS_8000)
        PlayStoreSection.PACKS -> listOf(ProductCatalog.STARTER_STYLE_PACK, ProductCatalog.PREMIUM_STYLE_PACK, ProductCatalog.SEASON_PACK, ProductCatalog.VIP_WELCOME_PACK)
    }

    LaunchedEffect(backend) {
        config = runCatching { backend?.getStoreCatalogConfig().orEmpty().associateBy { it.productId } }.getOrDefault(emptyMap())
    }

    fun enabled(productId: String): Boolean = config[productId]?.enabled ?: (productId == ProductCatalog.STARTER_STYLE_PACK || productId in setOf(
        ProductCatalog.COINS_500, ProductCatalog.COINS_1500, ProductCatalog.COINS_3500, ProductCatalog.COINS_8000,
    ))

    fun badge(productId: String, fallback: String?): String? {
        val remote = config[productId]
        return if (SonHarfUiState.isEnglish) remote?.badgeEn ?: fallback else remote?.badgeTr ?: fallback
    }

    fun verify(productId: String, token: String) {
        if (!ProductCatalog.isKnown(productId) || !enabled(productId)) {
            notice = sh("Satışa kapalı veya bilinmeyen Google Play ürünü reddedildi.", "Disabled or unknown Google Play product rejected.")
            busy = null
            return
        }
        scope.launch {
            busy = productId
            runCatching { PlayPurchaseVerification.verify(productId, token) }
                .onSuccess {
                    notice = when (productId) {
                        ProductCatalog.COINS_500 -> sh("500 Son Coin hesabına eklendi.", "500 Son Coins added to your account.")
                        ProductCatalog.COINS_1500 -> sh("1500 Son Coin hesabına eklendi.", "1500 Son Coins added to your account.")
                        ProductCatalog.COINS_3500 -> sh("3500 Son Coin hesabına eklendi.", "3500 Son Coins added to your account.")
                        ProductCatalog.COINS_8000 -> sh("8000 Son Coin hesabına eklendi.", "8000 Son Coins added to your account.")
                        ProductCatalog.STARTER_STYLE_PACK -> sh("Başlangıç Style Paketi hesabına eklendi.", "Starter Style Pack added to your account.")
                        else -> sh("Satın alma Google Play ile doğrulandı.", "Purchase verified with Google Play.")
                    }
                    runCatching { backend?.trackStoreEvent("purchase_success", productId) }
                    onPurchased()
                }
                .onFailure { error ->
                    runCatching { backend?.trackStoreEvent("purchase_failure", productId) }
                    notice = when {
                        "google_play_not_configured" in error.message.orEmpty() -> sh(
                            "Google Play sunucu doğrulaması production hesabıyla yapılandırılmamış.",
                            "Google Play server verification is not configured for production.",
                        )
                        else -> sh(
                            "Doğrulama tamamlanamadı. Aynı satın alma ikinci kez ödül vermez; Geri Yükle ile tekrar deneyebilirsin.",
                            "Verification failed. The same purchase cannot grant twice; use Restore to retry.",
                        )
                    }
                }
            busy = null
        }
    }

    val manager = remember {
        BillingManager(
            context = context,
            onPurchase = { purchase ->
                val productId = purchase.products.firstOrNull()
                if (productId.isNullOrBlank()) {
                    notice = sh("Google Play ürün bilgisi alınamadı.", "Google Play product information is missing.")
                    busy = null
                } else {
                    verify(productId, purchase.purchaseToken)
                }
            },
            onPending = { purchase ->
                runCatching { backend?.let { b -> scope.launch { b.trackStoreEvent("purchase_pending", purchase.products.firstOrNull()) } } }
                notice = sh(
                    "Ödeme beklemede. Son Coin veya Style yalnız ödeme tamamlandıktan sonra verilir.",
                    "Payment is pending. Son Coin or Style is granted only after completion.",
                )
                busy = null
            },
            onMessage = { message -> notice = message; busy = null },
        )
    }

    DisposableEffect(manager) {
        manager.connect {
            connected = true
            manager.queryOneTimeProducts(ProductCatalog.activeOneTimeProducts) { products = it }
        }
        onDispose { manager.close() }
    }

    fun buy(productId: String) {
        val product = products[productId]
        if (!enabled(productId) || activity == null || product == null) {
            notice = sh("Ürün Google Play'de bu hesap için kullanılamıyor.", "Product is unavailable for this Google Play account.")
            return
        }
        scope.launch { runCatching { backend?.trackStoreEvent("checkout_start", productId) } }
        busy = productId
        val result = manager.launchProduct(activity, product)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            busy = null
            notice = sh("Google Play ödeme ekranı açılamadı (${result.responseCode}).", "Google Play billing could not open (${result.responseCode}).")
        }
    }

    val visibleIds = sectionIds.filter(::enabled)
    Card(
        colors = CardDefaults.cardColors(containerColor = SonHarfSurface.copy(alpha = .96f)),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, SonHarfGold.copy(alpha = .28f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (section == PlayStoreSection.COINS) "SON COIN" else sh("PAKETLER", "PACKS"), color = SonHarfGold, fontWeight = FontWeight.Black, fontSize = 13.sp)
            Text(
                sh(
                    "Fiyatlar Google Play'den yerelleştirilmiş gelir. Hak yalnız sunucu doğrulamasından sonra verilir; para ile maç gücü satın alınamaz.",
                    "Prices are localized by Google Play. Entitlements are granted only after server verification; money can never buy match power.",
                ),
                color = SonHarfMuted,
                fontSize = 9.sp,
            )

            if (section == PlayStoreSection.COINS) {
                PlayProductRow(badge(ProductCatalog.COINS_500, "MINI"), sh("500 Son Coin", "500 Son Coins"), sh("Style koleksiyonu için", "For your Style collection"), products[ProductCatalog.COINS_500], busy == ProductCatalog.COINS_500) { buy(ProductCatalog.COINS_500) }
                PlayProductRow(badge(ProductCatalog.COINS_1500, sh("EN POPÜLER", "POPULAR")), sh("1500 Son Coin", "1500 Son Coins"), sh("Style koleksiyonu için", "For your Style collection"), products[ProductCatalog.COINS_1500], busy == ProductCatalog.COINS_1500) { buy(ProductCatalog.COINS_1500) }
                PlayProductRow(badge(ProductCatalog.COINS_3500, sh("EN İYİ DEĞER", "BEST VALUE")), sh("3500 Son Coin", "3500 Son Coins"), sh("Style koleksiyonu için", "For your Style collection"), products[ProductCatalog.COINS_3500], busy == ProductCatalog.COINS_3500) { buy(ProductCatalog.COINS_3500) }
                PlayProductRow(badge(ProductCatalog.COINS_8000, "MEGA"), sh("8000 Son Coin", "8000 Son Coins"), sh("Style koleksiyonu için", "For your Style collection"), products[ProductCatalog.COINS_8000], busy == ProductCatalog.COINS_8000) { buy(ProductCatalog.COINS_8000) }
            } else if (visibleIds.isEmpty()) {
                Text(sh("Şu anda doğrulanmış aktif paket yok.", "There are no verified active packs right now."), color = SonHarfMuted, fontSize = 10.sp)
            } else {
                if (ProductCatalog.STARTER_STYLE_PACK in visibleIds) {
                    PlayProductRow(badge(ProductCatalog.STARTER_STYLE_PACK, null), sh("Başlangıç Style Paketi", "Starter Style Pack"), sh("800 SC + Kurucu Işık Çerçevesi", "800 SC + Founder Glow Frame"), products[ProductCatalog.STARTER_STYLE_PACK], busy == ProductCatalog.STARTER_STYLE_PACK) { buy(ProductCatalog.STARTER_STYLE_PACK) }
                }
                if (ProductCatalog.PREMIUM_STYLE_PACK in visibleIds) PlayProductRow(badge(ProductCatalog.PREMIUM_STYLE_PACK, null), sh("Premium Style Paketi", "Premium Style Pack"), sh("Yalnız Style ve koleksiyon", "Style and collection only"), products[ProductCatalog.PREMIUM_STYLE_PACK], busy == ProductCatalog.PREMIUM_STYLE_PACK) { buy(ProductCatalog.PREMIUM_STYLE_PACK) }
                if (ProductCatalog.SEASON_PACK in visibleIds) PlayProductRow(badge(ProductCatalog.SEASON_PACK, null), sh("Sezon Paketi", "Season Pack"), sh("Sezon Style ve Son Coin", "Season Style and Son Coin"), products[ProductCatalog.SEASON_PACK], busy == ProductCatalog.SEASON_PACK) { buy(ProductCatalog.SEASON_PACK) }
                if (ProductCatalog.VIP_WELCOME_PACK in visibleIds) PlayProductRow(badge(ProductCatalog.VIP_WELCOME_PACK, null), sh("VIP Welcome Pack", "VIP Welcome Pack"), sh("Güç vermeyen başlangıç koleksiyonu", "Non-power starter collection"), products[ProductCatalog.VIP_WELCOME_PACK], busy == ProductCatalog.VIP_WELCOME_PACK) { buy(ProductCatalog.VIP_WELCOME_PACK) }
            }

            OutlinedButton(
                onClick = {
                    busy = "restore"
                    manager.restorePurchases { count ->
                        scope.launch { runCatching { backend?.trackStoreEvent("restore") } }
                        notice = if (count > 0) sh("Satın almalar yeniden doğrulanıyor.", "Purchases are being re-verified.")
                        else sh("Geri yüklenecek satın alma bulunamadı.", "No purchases were found to restore.")
                        busy = null
                    }
                },
                enabled = connected && busy == null,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text(sh("SATIN ALMALARI GERİ YÜKLE", "RESTORE PURCHASES"), fontWeight = FontWeight.Black)
            }

            if (notice.isNotBlank()) Text(notice, color = SonHarfMuted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun PlayProductRow(
    badge: String?,
    title: String,
    subtitle: String,
    product: ProductDetails?,
    busy: Boolean,
    onBuy: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            if (!badge.isNullOrBlank()) Text(badge, color = SonHarfGold, fontSize = 8.sp, fontWeight = FontWeight.Black)
            Text(title, fontWeight = FontWeight.Black, color = SonHarfText)
            Text(subtitle, color = SonHarfMuted, fontSize = 9.sp)
        }
        Button(
            onClick = onBuy,
            enabled = !busy && product != null,
            modifier = Modifier.heightIn(min = 48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SonHarfPurple),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(
                when {
                    busy -> sh("İŞLENİYOR", "PROCESSING")
                    product == null -> sh("YOK", "N/A")
                    else -> product.oneTimePurchaseOfferDetails?.formattedPrice ?: sh("FİYAT YOK", "NO PRICE")
                },
                fontWeight = FontWeight.Black,
            )
        }
    }
}
