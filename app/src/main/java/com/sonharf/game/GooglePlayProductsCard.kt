package com.sonharf.game

import android.app.Activity
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.sonharf.game.billing.BillingManager
import com.sonharf.game.billing.PlayPurchaseVerification
import com.sonharf.game.billing.ProductCatalog
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.VipEntitlementsDto
import com.sonharf.game.data.getVipEntitlements
import kotlinx.coroutines.launch

@Composable
fun GooglePlayProductsCard(onPurchased: () -> Unit = {}) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val backend = remember { OnlineGameBackend() }
    var products by remember { mutableStateOf<Map<String, ProductDetails>>(emptyMap()) }
    var busy by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf("") }
    var entitlements by remember { mutableStateOf(VipEntitlementsDto()) }
    var showSeriesGame by remember { mutableStateOf(false) }

    fun refreshEntitlements() {
        scope.launch {
            entitlements = runCatching { backend.getVipEntitlements() }.getOrDefault(entitlements)
        }
    }

    LaunchedEffect(Unit) { refreshEntitlements() }

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
                                    ProductCatalog.SERIES_GAME -> sh("Seri Oyun kalıcı olarak açıldı.", "Series Game unlocked permanently.")
                                    ProductCatalog.LETTER_TABLE -> sh("Harf Tablosu kalıcı olarak açıldı.", "Letter Table unlocked permanently.")
                                    ProductCatalog.SCORE_CALCULATOR -> sh("Puan Hesaplayıcı kalıcı olarak açıldı.", "Score Calculator unlocked permanently.")
                                    ProductCatalog.PRO_LIFETIME -> sh("PRO kalıcı olarak açıldı. 100 Son Coin hesabına eklendi.", "PRO unlocked permanently. 100 Son Coins were added.")
                                    ProductCatalog.COINS_500 -> sh("500 Son Coin hesabına eklendi.", "500 Son Coins added to your account.")
                                    ProductCatalog.COINS_1500 -> sh("1500 Son Coin hesabına eklendi.", "1500 Son Coins added to your account.")
                                    ProductCatalog.COINS_3500 -> sh("3500 Son Coin hesabına eklendi.", "3500 Son Coins added to your account.")
                                    ProductCatalog.COINS_8000 -> sh("8000 Son Coin hesabına eklendi.", "8000 Son Coins added to your account.")
                                    else -> sh("Satın alma doğrulandı.", "Purchase verified.")
                                }
                                entitlements = runCatching { backend.getVipEntitlements() }.getOrDefault(entitlements)
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
        if (activity == null || product?.oneTimePurchaseOfferDetails == null) {
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
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(sh("PREMİUM ÖZELLİKLER", "PREMIUM FEATURES"), color = SonHarfGold, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text(
                sh("Tek ödeme ile kalıcı kullanım. PRO; tüm premium özellikleri, reklamsız kullanımı, arkadaş listesini, Son Harf kelime geçmişini, 50 aktif oyun limitini ve 100 Son Coin'i açar.", "One payment, permanent access. PRO unlocks all premium features, ad-free play, friends list, Son Harf word history, a 50 active-game limit, and 100 Son Coins."),
                color = SonHarfMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )

            PremiumProductRow(
                title = sh("Seri Oyun", "Series Game"),
                subtitle = sh("3/5/10 dk tur • otomatik pas • 3 kaçırma = mağlubiyet", "3/5/10 min turns • auto-pass • 3 misses = defeat"),
                imageRes = R.drawable.premium_series_game,
                product = products[ProductCatalog.SERIES_GAME],
                busy = busy != null,
                owned = entitlements.seriesGameAccess,
                onOpen = { showSeriesGame = true },
            ) { buy(ProductCatalog.SERIES_GAME) }
            PremiumProductRow(
                title = sh("Harf Tablosu", "Letter Table"),
                subtitle = sh("Kalan harfleri gör", "See remaining letters"),
                imageRes = R.drawable.premium_letter_table,
                product = products[ProductCatalog.LETTER_TABLE],
                busy = busy != null,
                owned = entitlements.letterTableAccess,
            ) { buy(ProductCatalog.LETTER_TABLE) }
            PremiumProductRow(
                title = sh("Puan Hesaplayıcı", "Score Calculator"),
                subtitle = sh("Hamle puanını önceden gör", "Preview move score"),
                imageRes = R.drawable.premium_score_calculator,
                product = products[ProductCatalog.SCORE_CALCULATOR],
                busy = busy != null,
                owned = entitlements.scoreCalculatorAccess,
            ) { buy(ProductCatalog.SCORE_CALCULATOR) }
            PremiumProductRow(
                title = "PRO",
                subtitle = sh("Tüm premium özellikler", "All premium features"),
                imageRes = R.drawable.premium_pro,
                product = products[ProductCatalog.PRO_LIFETIME],
                busy = busy != null,
                owned = entitlements.isPro,
            ) { buy(ProductCatalog.PRO_LIFETIME) }

            HorizontalDivider(color = SonHarfTheme.Border)
            Text("SON COIN", color = SonHarfGold, fontWeight = FontWeight.Black, fontSize = 14.sp)
            Text(
                sh("Coin yalnızca kozmetik ve mağaza ürünlerinde kullanılır; maç gücü satılmaz.", "Coins are only for cosmetics and store items; match power is never sold."),
                color = SonHarfMuted,
                fontSize = 9.sp,
            )

            CoinProductRow(
                amount = 500,
                subtitle = sh("Mini paket", "Mini pack"),
                imageRes = R.drawable.premium_coin_500,
                product = products[ProductCatalog.COINS_500],
                busy = busy != null,
            ) { buy(ProductCatalog.COINS_500) }
            CoinProductRow(
                amount = 1500,
                subtitle = sh("Standart paket", "Standard pack"),
                imageRes = R.drawable.premium_coin_1500,
                product = products[ProductCatalog.COINS_1500],
                busy = busy != null,
            ) { buy(ProductCatalog.COINS_1500) }
            CoinProductRow(
                amount = 3500,
                subtitle = sh("Popüler paket", "Popular pack"),
                imageRes = R.drawable.premium_coin_3500,
                product = products[ProductCatalog.COINS_3500],
                busy = busy != null,
            ) { buy(ProductCatalog.COINS_3500) }
            CoinProductRow(
                amount = 8000,
                subtitle = sh("Mega paket", "Mega pack"),
                imageRes = R.drawable.premium_coin_8000,
                product = products[ProductCatalog.COINS_8000],
                busy = busy != null,
            ) { buy(ProductCatalog.COINS_8000) }

            if (notice.isNotBlank()) Text(notice, color = SonHarfMuted, fontSize = 9.sp)
        }
    }

    if (showSeriesGame) {
        Dialog(
            onDismissRequest = { showSeriesGame = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            Surface(Modifier.fillMaxSize(), color = SonHarfTheme.Background) {
                WordSiegeSeriesScreen(verifiedAccess = true, onExit = { showSeriesGame = false })
            }
        }
    }
}

@Composable
private fun PremiumProductRow(
    title: String,
    subtitle: String,
    @DrawableRes imageRes: Int,
    product: ProductDetails?,
    busy: Boolean,
    owned: Boolean = false,
    onOpen: (() -> Unit)? = null,
    onBuy: () -> Unit,
) {
    val offer = product?.oneTimePurchaseOfferDetails
    Surface(
        color = SonHarfTheme.SurfaceSecondary,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, SonHarfTheme.Border),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 104.dp).padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.size(78.dp),
                shape = RoundedCornerShape(18.dp),
                color = SonHarfTheme.Surface,
                border = BorderStroke(1.dp, SonHarfTheme.Primary.copy(alpha = .16f)),
            ) {
                Image(
                    painter = painterResource(imageRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(6.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(title, fontWeight = FontWeight.Black, color = SonHarfText, fontSize = 16.sp, lineHeight = 19.sp)
                Text(subtitle, color = SonHarfMuted, fontSize = 11.sp, lineHeight = 15.sp)
                when {
                    owned -> Text(sh("SATIN ALINDI", "OWNED"), color = SonHarfTheme.Success, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    offer == null -> Text(sh("Google Play teklifi kullanılabilir olduğunda satış açılır", "Sale opens when the Google Play offer is available"), color = SonHarfMuted, fontSize = 9.sp, lineHeight = 12.sp)
                }
            }
            if (owned && onOpen != null) {
                Button(
                    onClick = onOpen,
                    enabled = !busy,
                    modifier = Modifier.defaultMinSize(minWidth = 74.dp, minHeight = 40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SonHarfTheme.Primary),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 13.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(sh("AÇ", "OPEN"), fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            } else if (owned) {
                Surface(shape = RoundedCornerShape(12.dp), color = SonHarfTheme.Success.copy(alpha = .12f)) {
                    Text("✓", Modifier.padding(horizontal = 15.dp, vertical = 9.dp), color = SonHarfTheme.Success, fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
            } else {
                Button(
                    onClick = onBuy,
                    enabled = !busy && offer != null,
                    modifier = Modifier.defaultMinSize(minWidth = 78.dp, minHeight = 40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SonHarfTheme.Primary, disabledContainerColor = SonHarfTheme.DisabledBackground, disabledContentColor = SonHarfTheme.DisabledContent),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 13.dp, vertical = 8.dp),
                ) {
                    Text(offer?.formattedPrice ?: sh("PLAY'DE YOK", "NOT ON PLAY"), fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun CoinProductRow(
    amount: Int,
    subtitle: String,
    @DrawableRes imageRes: Int,
    product: ProductDetails?,
    busy: Boolean,
    onBuy: () -> Unit,
) {
    val offer = product?.oneTimePurchaseOfferDetails
    Surface(color = SonHarfTheme.SurfaceSecondary, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, SonHarfTheme.Border)) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Image(
                painter = painterResource(imageRes),
                contentDescription = null,
                modifier = Modifier.size(54.dp),
                contentScale = ContentScale.Fit,
            )
            Column(Modifier.weight(1f)) {
                Text("$amount Son Coin", fontWeight = FontWeight.Black, color = SonHarfText, fontSize = 14.sp)
                Text(subtitle, color = SonHarfMuted, fontSize = 9.sp)
            }
            Button(
                onClick = onBuy,
                enabled = !busy && offer != null,
                colors = ButtonDefaults.buttonColors(containerColor = SonHarfTheme.Primary, disabledContainerColor = SonHarfTheme.DisabledBackground, disabledContentColor = SonHarfTheme.DisabledContent),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            ) {
                Text(when { busy -> "…"; offer != null -> offer.formattedPrice; else -> sh("PLAY'DE YOK", "NOT ON PLAY") }, fontWeight = FontWeight.Black, fontSize = 10.sp)
            }
        }
    }
}