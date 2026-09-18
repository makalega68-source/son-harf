package com.sonharf.game

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
                                    ProductCatalog.SERIES_GAME -> sh("Seri Oyun kalıcı olarak açıldı.", "Fast Game unlocked permanently.")
                                    ProductCatalog.LETTER_TABLE -> sh("Harf Tablosu kalıcı olarak açıldı.", "Letter Table unlocked permanently.")
                                    ProductCatalog.SCORE_CALCULATOR -> sh("Puan Hesaplayıcı kalıcı olarak açıldı.", "Score Calculator unlocked permanently.")
                                    ProductCatalog.PRO_LIFETIME -> sh("PRO kalıcı olarak açıldı. 100 Son Coin hesabına eklendi.", "PRO unlocked permanently. 100 Son Coins were added.")
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
        manager.connect { manager.queryOneTimeProducts(ProductCatalog.oneTimeProducts) { products = it } }
        onDispose { manager.close() }
    }

    fun buy(productId: String) {
        val product = products[productId]
        if (activity == null || product == null) {
            notice = sh("Bu ürün Google Play'de henüz satın alınabilir değil.", "This product is not yet purchasable on Google Play.")
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
            Text(sh("PREMİUM ÖZELLİKLER", "PREMIUM FEATURES"), color = SonHarfGold, fontWeight = FontWeight.Black, fontSize = 14.sp)
            Text(
                sh("Tek ödeme ile kalıcı kullanım. PRO; tüm premium özellikleri, reklamsız kullanımı, arkadaş listesini, Son Harf kelime geçmişini, 50 aktif oyun limitini, PRO çerçevesini ve 100 Son Coin'i açar.", "One payment, permanent access. PRO unlocks all premium features, ad-free play, friends list, Son Harf word history, a 50 active-game limit, the PRO frame and 100 Son Coins."),
                color = SonHarfMuted,
                fontSize = 9.sp,
            )

            PremiumProductRow(
                title = sh("Seri Oyun", "Fast Game"),
                subtitle = sh("Hızlı oyun erişimi", "Fast-game access"),
                icon = Icons.Rounded.Timer,
                product = products[ProductCatalog.SERIES_GAME],
                fallbackPrice = ProductCatalog.SERIES_GAME_FALLBACK_PRICE_TRY,
                busy = busy != null,
            ) { buy(ProductCatalog.SERIES_GAME) }
            PremiumProductRow(
                title = sh("Harf Tablosu", "Letter Table"),
                subtitle = sh("Kalan harfleri gör", "See remaining letters"),
                icon = Icons.Rounded.GridView,
                product = products[ProductCatalog.LETTER_TABLE],
                fallbackPrice = ProductCatalog.LETTER_TABLE_FALLBACK_PRICE_TRY,
                busy = busy != null,
            ) { buy(ProductCatalog.LETTER_TABLE) }
            PremiumProductRow(
                title = sh("Puan Hesaplayıcı", "Score Calculator"),
                subtitle = sh("Hamle puanını önceden gör", "Preview move score"),
                icon = Icons.Rounded.Calculate,
                product = products[ProductCatalog.SCORE_CALCULATOR],
                fallbackPrice = ProductCatalog.SCORE_CALCULATOR_FALLBACK_PRICE_TRY,
                busy = busy != null,
            ) { buy(ProductCatalog.SCORE_CALCULATOR) }
            PremiumProductRow(
                title = "PRO",
                subtitle = sh("Tüm premium özellikler", "All premium features"),
                icon = Icons.Rounded.WorkspacePremium,
                product = products[ProductCatalog.PRO_LIFETIME],
                fallbackPrice = ProductCatalog.PRO_LIFETIME_FALLBACK_PRICE_TRY,
                busy = busy != null,
            ) { buy(ProductCatalog.PRO_LIFETIME) }

            HorizontalDivider(color = SonHarfTheme.Border)
            Text("SON COIN", color = SonHarfGold, fontWeight = FontWeight.Black, fontSize = 14.sp)
            Text(
                sh("Coin yalnızca kozmetik ve mağaza ürünlerinde kullanılır; maç gücü satılmaz.", "Coins are only for cosmetics and store items; match power is never sold."),
                color = SonHarfMuted,
                fontSize = 9.sp,
            )

            CoinProductRow(500, sh("Mini paket", "Mini pack"), products[ProductCatalog.COINS_500], busy != null) { buy(ProductCatalog.COINS_500) }
            CoinProductRow(1500, sh("Standart paket", "Standard pack"), products[ProductCatalog.COINS_1500], busy != null) { buy(ProductCatalog.COINS_1500) }
            CoinProductRow(3500, sh("Popüler paket", "Popular pack"), products[ProductCatalog.COINS_3500], busy != null) { buy(ProductCatalog.COINS_3500) }
            CoinProductRow(8000, sh("Mega paket", "Mega pack"), products[ProductCatalog.COINS_8000], busy != null) { buy(ProductCatalog.COINS_8000) }

            if (notice.isNotBlank()) Text(notice, color = SonHarfMuted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun PremiumProductRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    product: ProductDetails?,
    fallbackPrice: String,
    busy: Boolean,
    onBuy: () -> Unit,
) {
    Surface(color = SonHarfTheme.SurfaceSecondary, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, SonHarfTheme.Border)) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = RoundedCornerShape(14.dp), color = SonHarfTheme.Primary.copy(alpha = .10f)) {
                Icon(icon, null, tint = SonHarfTheme.Primary, modifier = Modifier.padding(12.dp).size(24.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Black, color = SonHarfText, fontSize = 14.sp)
                Text(subtitle, color = SonHarfMuted, fontSize = 9.sp)
                if (product == null) Text(sh("Google Play fiyatı bağlanınca satış açılır", "Sale opens when the Google Play price is linked"), color = SonHarfMuted, fontSize = 8.sp)
            }
            Button(
                onClick = onBuy,
                enabled = !busy && product?.oneTimePurchaseOfferDetails != null,
                colors = ButtonDefaults.buttonColors(containerColor = SonHarfTheme.Primary, disabledContainerColor = SonHarfTheme.DisabledBackground, disabledContentColor = SonHarfTheme.DisabledContent),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            ) {
                Text(if (product != null) product.oneTimePurchaseOfferDetails?.formattedPrice ?: fallbackPrice else fallbackPrice, fontWeight = FontWeight.Black, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun CoinProductRow(amount: Int, subtitle: String, product: ProductDetails?, busy: Boolean, onBuy: () -> Unit) {
    Surface(color = SonHarfTheme.SurfaceSecondary, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, SonHarfTheme.Border)) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Image(painter = painterResource(R.drawable.style_icon_coin), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                Text(if (amount >= 1000) "${amount / 1000}K" else amount.toString(), color = SonHarfText, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.BottomEnd))
            }
            Column(Modifier.weight(1f)) {
                Text("$amount Son Coin", fontWeight = FontWeight.Black, color = SonHarfText, fontSize = 14.sp)
                Text(subtitle, color = SonHarfMuted, fontSize = 9.sp)
            }
            Button(
                onClick = onBuy,
                enabled = !busy && product?.oneTimePurchaseOfferDetails != null,
                colors = ButtonDefaults.buttonColors(containerColor = SonHarfTheme.Primary, disabledContainerColor = SonHarfTheme.DisabledBackground, disabledContentColor = SonHarfTheme.DisabledContent),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            ) {
                Text(when { busy -> "…"; product != null -> product.oneTimePurchaseOfferDetails?.formattedPrice ?: sh("SATIN AL", "BUY"); else -> sh("PLAY'DE YOK", "NOT ON PLAY") }, fontWeight = FontWeight.Black, fontSize = 10.sp)
            }
        }
    }
}
