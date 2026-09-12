package com.sonharf.game

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
                                    else -> sh("Satın alma doğrulandı.", "Purchase verified.")
                                }
                                onPurchased()
                            }
                            .onFailure { error ->
                                notice = when {
                                    "google_play_not_configured" in error.message.orEmpty() -> sh("Google Play sunucu doğrulaması production hesabıyla henüz etkin değil.", "Google Play server verification is not enabled with the production account yet.")
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
            notice = sh("Bu paket Google Play'de henüz satın alınabilir değil.", "This pack is not yet purchasable on Google Play.")
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
        colors = CardDefaults.cardColors(containerColor = SonHarfTheme.Surface),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, SonHarfGold.copy(alpha = .28f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(sh("SON COIN", "SON COIN"), color = SonHarfGold, fontWeight = FontWeight.Black, fontSize = 14.sp)
            Text(
                sh("Paketler Google Play fiyatı yüklendiğinde satın alınabilir. Coin yalnızca kozmetik ve mağaza ürünlerinde kullanılır; maç gücü satılmaz.", "Packs become purchasable when their Google Play price is loaded. Coins are only for cosmetics and store items; match power is never sold."),
                color = SonHarfMuted,
                fontSize = 9.sp,
            )

            CoinProductRow(
                amount = 500,
                subtitle = sh("Mini paket", "Mini pack"),
                product = products[ProductCatalog.COINS_500],
                busy = busy == ProductCatalog.COINS_500,
            ) { buy(ProductCatalog.COINS_500) }
            CoinProductRow(
                amount = 1500,
                subtitle = sh("Standart paket", "Standard pack"),
                product = products[ProductCatalog.COINS_1500],
                busy = busy == ProductCatalog.COINS_1500,
            ) { buy(ProductCatalog.COINS_1500) }
            CoinProductRow(
                amount = 3500,
                subtitle = sh("Popüler paket", "Popular pack"),
                product = products[ProductCatalog.COINS_3500],
                busy = busy == ProductCatalog.COINS_3500,
            ) { buy(ProductCatalog.COINS_3500) }
            CoinProductRow(
                amount = 8000,
                subtitle = sh("Mega paket", "Mega pack"),
                product = products[ProductCatalog.COINS_8000],
                busy = busy == ProductCatalog.COINS_8000,
            ) { buy(ProductCatalog.COINS_8000) }

            if (notice.isNotBlank()) Text(notice, color = SonHarfMuted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun CoinProductRow(
    amount: Int,
    subtitle: String,
    product: ProductDetails?,
    busy: Boolean,
    onBuy: () -> Unit,
) {
    Surface(
        color = SonHarfTheme.SurfaceSecondary,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SonHarfTheme.Border),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(R.drawable.style_icon_coin),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
                Text(
                    if (amount >= 1000) "${amount / 1000}K" else amount.toString(),
                    color = SonHarfText,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.align(Alignment.BottomEnd),
                )
            }
            Column(Modifier.weight(1f)) {
                Text("$amount Son Coin", fontWeight = FontWeight.Black, color = SonHarfText, fontSize = 14.sp)
                Text(subtitle, color = SonHarfMuted, fontSize = 9.sp)
            }
            Button(
                onClick = onBuy,
                enabled = !busy && product != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SonHarfTheme.Primary,
                    disabledContainerColor = SonHarfTheme.DisabledBackground,
                    disabledContentColor = SonHarfTheme.DisabledContent,
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            ) {
                Text(
                    when {
                        busy -> "…"
                        product != null -> product.oneTimePurchaseOfferDetails?.formattedPrice ?: sh("SATIN AL", "BUY")
                        else -> sh("PLAY'DE YOK", "NOT ON PLAY")
                    },
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                )
            }
        }
    }
}
