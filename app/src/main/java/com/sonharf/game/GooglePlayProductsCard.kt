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
import kotlinx.coroutines.launch

@Composable
fun GooglePlayProductsCard(onPurchased: () -> Unit = {}) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    var products by remember { mutableStateOf<Map<String, ProductDetails>>(emptyMap()) }
    var busy by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf("") }
    var connected by remember { mutableStateOf(false) }

    fun verify(productId: String, token: String) {
        if (!ProductCatalog.isKnown(productId)) {
            notice = sh("Bilinmeyen Google Play ürünü reddedildi.", "Unknown Google Play product rejected.")
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
                    onPurchased()
                }
                .onFailure { error ->
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
            onPending = {
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
        if (activity == null || product == null) {
            notice = sh("Ürün Google Play'de bu hesap için kullanılamıyor.", "Product is unavailable for this Google Play account.")
            return
        }
        busy = productId
        val result = manager.launchProduct(activity, product)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            busy = null
            notice = sh("Google Play ödeme ekranı açılamadı (${result.responseCode}).", "Google Play billing could not open (${result.responseCode}).")
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SonHarfSurface.copy(alpha = .96f)),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, SonHarfGold.copy(alpha = .28f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("SON COIN", color = SonHarfGold, fontWeight = FontWeight.Black, fontSize = 13.sp)
            Text(
                sh(
                    "Fiyatlar Google Play'den yerelleştirilmiş olarak gelir. Ödeme yalnız sunucu doğrulamasından sonra hesaba işlenir; para ile maç gücü satın alınamaz.",
                    "Prices are localized by Google Play. Purchases are granted only after server verification; money can never buy match power.",
                ),
                color = SonHarfMuted,
                fontSize = 9.sp,
            )

            PlayProductRow("MINI", sh("500 Son Coin", "500 Son Coins"), sh("Style koleksiyonu için", "For your Style collection"), products[ProductCatalog.COINS_500], busy == ProductCatalog.COINS_500) { buy(ProductCatalog.COINS_500) }
            PlayProductRow(sh("EN POPÜLER", "POPULAR"), sh("1500 Son Coin", "1500 Son Coins"), sh("Style koleksiyonu için", "For your Style collection"), products[ProductCatalog.COINS_1500], busy == ProductCatalog.COINS_1500) { buy(ProductCatalog.COINS_1500) }
            PlayProductRow(sh("EN İYİ DEĞER", "BEST VALUE"), sh("3500 Son Coin", "3500 Son Coins"), sh("Style koleksiyonu için", "For your Style collection"), products[ProductCatalog.COINS_3500], busy == ProductCatalog.COINS_3500) { buy(ProductCatalog.COINS_3500) }
            PlayProductRow("MEGA", sh("8000 Son Coin", "8000 Son Coins"), sh("Style koleksiyonu için", "For your Style collection"), products[ProductCatalog.COINS_8000], busy == ProductCatalog.COINS_8000) { buy(ProductCatalog.COINS_8000) }
            PlayProductRow(null, sh("Başlangıç Style Paketi", "Starter Style Pack"), sh("800 SC + Kurucu Işık Çerçevesi", "800 SC + Founder Glow Frame"), products[ProductCatalog.STARTER_STYLE_PACK], busy == ProductCatalog.STARTER_STYLE_PACK) { buy(ProductCatalog.STARTER_STYLE_PACK) }

            OutlinedButton(
                onClick = {
                    busy = "restore"
                    manager.restorePurchases { count ->
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
