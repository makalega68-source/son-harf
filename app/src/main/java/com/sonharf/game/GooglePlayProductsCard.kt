package com.sonharf.game

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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

    val manager = remember {
        BillingManager(
            context = context,
            onPurchase = { purchase ->
                val productId = purchase.products.firstOrNull()
                if (productId.isNullOrBlank()) {
                    notice = sh("Google Play ürün bilgisi alınamadı.", "Google Play product information is missing.")
                    busy = null
                } else {
                    scope.launch {
                        busy = productId
                        runCatching { PlayPurchaseVerification.verify(productId, purchase.purchaseToken) }
                            .onSuccess {
                                notice = when (productId) {
                                    ProductCatalog.COINS_500 -> sh("500 Son Coin hesabına eklendi.", "500 Son Coins added to your account.")
                                    ProductCatalog.COINS_1500 -> sh("1500 Son Coin hesabına eklendi.", "1500 Son Coins added to your account.")
                                    ProductCatalog.COINS_3500 -> sh("3500 Son Coin hesabına eklendi.", "3500 Son Coins added to your account.")
                                    ProductCatalog.COINS_8000 -> sh("8000 Son Coin hesabına eklendi.", "8000 Son Coins added to your account.")
                                    ProductCatalog.STARTER_STYLE_PACK -> sh("Başlangıç Style Paketi hesabına eklendi.", "Starter Style Pack added to your account.")
                                    else -> sh("Satın alma doğrulandı.", "Purchase verified.")
                                }
                                onPurchased()
                            }
                            .onFailure { error ->
                                notice = when {
                                    "google_play_not_configured" in error.message.orEmpty() -> sh("Google Play sunucu doğrulaması henüz production hesabıyla yapılandırılmadı.", "Google Play server verification is not configured with the production account yet.")
                                    else -> sh("Ödeme doğrulaması tamamlanamadı. Aynı satın alma tekrar ödül vermez; yeniden deneyebilirsin.", "Purchase verification failed. The same purchase cannot grant twice; you can retry.")
                                }
                            }
                        busy = null
                    }
                }
            },
            onMessage = { message -> notice = message; busy = null },
        )
    }

    DisposableEffect(manager) {
        manager.connect {
            manager.queryOneTimeProducts(ProductCatalog.oneTimeProducts) { products = it }
        }
        onDispose { manager.close() }
    }

    fun buy(productId: String) {
        val product = products[productId]
        if (activity == null || product == null) {
            notice = sh("Ürün henüz Google Play'de kullanılabilir değil.", "Product is not available on Google Play yet.")
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
            Text(sh("SON COIN & STYLE MARKET", "SON COIN & STYLE MARKET"), color = SonHarfGold, fontWeight = FontWeight.Black, fontSize = 13.sp)
            Text(sh("Ödeme Google Play üzerinden yapılır; satın alınan ürün yalnızca sunucu doğrulamasından sonra hesaba eklenir. Para ile maç gücü satın alınamaz.", "Payment is handled by Google Play; purchases are granted only after server verification. Money can never buy match power."), color = SonHarfMuted, fontSize = 9.sp)

            PlayProductRow(
                title = sh("500 Son Coin", "500 Son Coins"),
                subtitle = sh("Style mağazası için", "For the Style shop"),
                product = products[ProductCatalog.COINS_500],
                busy = busy == ProductCatalog.COINS_500,
            ) { buy(ProductCatalog.COINS_500) }

            PlayProductRow(
                title = sh("1500 Son Coin", "1500 Son Coins"),
                subtitle = sh("Orta Son Coin paketi", "Medium Son Coin pack"),
                product = products[ProductCatalog.COINS_1500],
                busy = busy == ProductCatalog.COINS_1500,
            ) { buy(ProductCatalog.COINS_1500) }

            PlayProductRow(
                title = sh("3500 Son Coin", "3500 Son Coins"),
                subtitle = sh("Popüler paket • Style alışverişleri için", "Popular pack • for Style purchases"),
                product = products[ProductCatalog.COINS_3500],
                busy = busy == ProductCatalog.COINS_3500,
            ) { buy(ProductCatalog.COINS_3500) }

            PlayProductRow(
                title = sh("8000 Son Coin", "8000 Son Coins"),
                subtitle = sh("En büyük Son Coin paketi", "Largest Son Coin pack"),
                product = products[ProductCatalog.COINS_8000],
                busy = busy == ProductCatalog.COINS_8000,
            ) { buy(ProductCatalog.COINS_8000) }

            PlayProductRow(
                title = sh("Başlangıç Style Paketi", "Starter Style Pack"),
                subtitle = sh("800 Son Coin + özel Kurucu Işık Çerçevesi", "800 Son Coins + exclusive Founder Glow Frame"),
                product = products[ProductCatalog.STARTER_STYLE_PACK],
                busy = busy == ProductCatalog.STARTER_STYLE_PACK,
            ) { buy(ProductCatalog.STARTER_STYLE_PACK) }

            if (notice.isNotBlank()) Text(notice, color = SonHarfMuted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun PlayProductRow(
    title: String,
    subtitle: String,
    product: ProductDetails?,
    busy: Boolean,
    onBuy: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Black, color = SonHarfText)
            Text(subtitle, color = SonHarfMuted, fontSize = 9.sp)
        }
        Button(
            onClick = onBuy,
            enabled = !busy,
            colors = ButtonDefaults.buttonColors(containerColor = SonHarfPurple),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(if (busy) "…" else product?.oneTimePurchaseOfferDetails?.formattedPrice ?: "PLAY", fontWeight = FontWeight.Black)
        }
    }
}
