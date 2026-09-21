package com.sonharf.game

import android.app.Activity
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
fun GooglePlayProductsCard(
    onPurchased: () -> Unit = {},
    showPremiumProducts: Boolean = true,
    showCoinPacks: Boolean = true,
) {
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
        scope.launch { entitlements = runCatching { backend.getVipEntitlements() }.getOrDefault(entitlements) }
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
                                    ProductCatalog.PRO_LIFETIME -> sh("PRO kalıcı olarak açıldı. İlk doğrulanan grant'te 100 Son Coin verilir.", "PRO unlocked permanently. 100 Son Coins are granted on the first verified grant.")
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
        manager.connect {
            manager.queryOneTimeProducts(ProductCatalog.oneTimeProducts) { products = it }
            manager.restorePurchases(ProductCatalog.permanentPremiumProducts.toSet())
        }
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

    PurchasedPanel(
        modifier = Modifier.fillMaxWidth(),
        asset = PurchasedUiAsset.PANEL_LARGE,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (showPremiumProducts) {
                PurchasedSectionHeader(sh("PREMİUM ÖZELLİKLER", "PREMIUM FEATURES"))
                Text(
                    sh("Tek ödeme ile kalıcı kullanım. PRO; premium araçları, reklamsız kullanımı, arkadaş listesini, Son Harf tam geçmişini, 50 aktif oyun limitini, PRO çerçevesini ve ilk grant'te bir kez 100 Son Coin'i açar.", "One payment, permanent access. PRO unlocks premium tools, ad-free play, the friends list, full Son Harf history, a 50 active-game limit, the PRO frame and a one-time 100 Son Coins on the first grant."),
                    color = Color(0xFF765746),
                    fontSize = 9.sp,
                )

                PurchasedPremiumProductRow(
                    title = sh("Seri Oyun", "Series Game"),
                    subtitle = sh("3/5/10 dk • ayrı hızlı oyun modu", "3/5/10 min • separate fast game mode"),
                    imageRes = R.drawable.premium_series_game_canva,
                    product = products[ProductCatalog.SERIES_GAME],
                    fallbackPrice = ProductCatalog.SERIES_GAME_FALLBACK_PRICE_TRY,
                    busy = busy != null,
                    directOwned = entitlements.seriesGameDirectOwned,
                    proAccess = entitlements.isPro,
                    onOpen = { showSeriesGame = true },
                ) { buy(ProductCatalog.SERIES_GAME) }
                PurchasedPremiumProductRow(
                    title = sh("Harf Tablosu", "Letter Table"),
                    subtitle = sh("Kalan harfleri rakip elini açmadan gör", "See remaining letters without exposing the opponent rack"),
                    imageRes = R.drawable.premium_letter_table_canva,
                    product = products[ProductCatalog.LETTER_TABLE],
                    fallbackPrice = ProductCatalog.LETTER_TABLE_FALLBACK_PRICE_TRY,
                    busy = busy != null,
                    directOwned = entitlements.letterTableDirectOwned,
                    proAccess = entitlements.isPro,
                ) { buy(ProductCatalog.LETTER_TABLE) }
                PurchasedPremiumProductRow(
                    title = sh("Puan Hesaplayıcı", "Score Calculator"),
                    subtitle = sh("Hamle puanını gerçek sunucu motoruyla önceden gör", "Preview move score with the real server engine"),
                    imageRes = R.drawable.premium_score_calculator_canva,
                    product = products[ProductCatalog.SCORE_CALCULATOR],
                    fallbackPrice = ProductCatalog.SCORE_CALCULATOR_FALLBACK_PRICE_TRY,
                    busy = busy != null,
                    directOwned = entitlements.scoreCalculatorDirectOwned,
                    proAccess = entitlements.isPro,
                ) { buy(ProductCatalog.SCORE_CALCULATOR) }
                PurchasedPremiumProductRow(
                    title = "PRO",
                    subtitle = sh("Premium paket • tek ödeme • kalıcı erişim", "Premium bundle • one payment • lifetime access"),
                    imageRes = R.drawable.premium_pro_canva,
                    product = products[ProductCatalog.PRO_LIFETIME],
                    fallbackPrice = ProductCatalog.PRO_LIFETIME_FALLBACK_PRICE_TRY,
                    busy = busy != null,
                    directOwned = entitlements.isPro,
                    proAccess = false,
                ) { buy(ProductCatalog.PRO_LIFETIME) }
            }

            if (showCoinPacks) {
                PurchasedSectionHeader("SON COIN")
                Text(
                    sh("Coin yalnızca kozmetik ve mağaza ürünlerinde kullanılır; maç gücü satılmaz.", "Coins are only for cosmetics and store items; match power is never sold."),
                    color = Color(0xFF765746),
                    fontSize = 9.sp,
                )
                PurchasedCoinProductRow(500, sh("Mini paket", "Mini pack"), products[ProductCatalog.COINS_500], busy != null) { buy(ProductCatalog.COINS_500) }
                PurchasedCoinProductRow(1500, sh("Standart paket", "Standard pack"), products[ProductCatalog.COINS_1500], busy != null) { buy(ProductCatalog.COINS_1500) }
                PurchasedCoinProductRow(3500, sh("Popüler paket", "Popular pack"), products[ProductCatalog.COINS_3500], busy != null) { buy(ProductCatalog.COINS_3500) }
                PurchasedCoinProductRow(8000, sh("Mega paket", "Mega pack"), products[ProductCatalog.COINS_8000], busy != null) { buy(ProductCatalog.COINS_8000) }
            }

            if (notice.isNotBlank()) {
                PurchasedPanel(
                    modifier = Modifier.fillMaxWidth(),
                    asset = PurchasedUiAsset.PANEL_SMALL,
                    contentPadding = PaddingValues(12.dp),
                ) {
                    Text(notice, color = Color(0xFF654A3D), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showSeriesGame) {
        Dialog(
            onDismissRequest = { showSeriesGame = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            Box(Modifier.fillMaxSize()) {
                PurchasedGameBackdrop(Modifier.matchParentSize())
                WordSiegeSeriesScreen(onExit = { showSeriesGame = false })
            }
        }
    }
}

@Composable
private fun PurchasedPremiumProductRow(
    title: String,
    subtitle: String,
    @DrawableRes imageRes: Int,
    product: ProductDetails?,
    fallbackPrice: String,
    busy: Boolean,
    directOwned: Boolean = false,
    proAccess: Boolean = false,
    onOpen: (() -> Unit)? = null,
    onBuy: () -> Unit,
) {
    val effectiveAccess = directOwned || proAccess
    PurchasedPanel(
        modifier = Modifier.fillMaxWidth(),
        asset = PurchasedUiAsset.PANEL_SMALL,
        contentPadding = PaddingValues(horizontal = 11.dp, vertical = 11.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(Modifier.size(66.dp), contentAlignment = Alignment.Center) {
                PurchasedAsset(PurchasedUiAsset.REWARD_PANEL, Modifier.matchParentSize())
                Image(painter = painterResource(imageRes), contentDescription = null, modifier = Modifier.padding(7.dp).fillMaxSize(), contentScale = ContentScale.Fit)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.Black, color = Color(0xFF4A2D20), fontSize = 13.sp)
                Text(subtitle, color = Color(0xFF765746), fontSize = 9.sp)
                when {
                    directOwned -> Text(sh("SATIN ALINDI", "OWNED"), color = Color(0xFF4D9A4D), fontSize = 8.sp, fontWeight = FontWeight.Black)
                    proAccess -> Text(sh("PRO İLE AÇIK", "UNLOCKED WITH PRO"), color = Color(0xFF6B3CA6), fontSize = 8.sp, fontWeight = FontWeight.Black)
                    product?.oneTimePurchaseOfferDetails != null -> Text(product.oneTimePurchaseOfferDetails!!.formattedPrice, color = Color(0xFF654A3D), fontSize = 8.sp, fontWeight = FontWeight.Black)
                    else -> Text(sh("Hedef fiyat: $fallbackPrice • Google Play fiyatı bekleniyor", "Target price: $fallbackPrice • waiting for Google Play price"), color = Color(0xFF765746), fontSize = 8.sp)
                }
            }
            when {
                effectiveAccess && onOpen != null -> PurchasedButton(
                    text = sh("AÇ", "OPEN"),
                    onClick = onOpen,
                    enabled = !busy,
                    modifier = Modifier.width(96.dp),
                    style = PurchasedButtonStyle.SECONDARY,
                    leadingAsset = PurchasedUiAsset.ICON_GAMES,
                )
                effectiveAccess -> PurchasedAsset(PurchasedUiAsset.ICON_CHECK, Modifier.size(39.dp))
                else -> {
                    val realPrice = product?.oneTimePurchaseOfferDetails?.formattedPrice
                    PurchasedButton(
                        text = realPrice ?: sh("SATIN AL", "BUY"),
                        onClick = onBuy,
                        enabled = !busy && realPrice != null,
                        modifier = Modifier.widthIn(min = 94.dp, max = 130.dp),
                        style = PurchasedButtonStyle.PRIMARY,
                    )
                }
            }
        }
    }
}

@Composable
private fun PurchasedCoinProductRow(
    amount: Int,
    subtitle: String,
    product: ProductDetails?,
    busy: Boolean,
    onBuy: () -> Unit,
) {
    PurchasedPanel(
        modifier = Modifier.fillMaxWidth(),
        asset = PurchasedUiAsset.PANEL_SMALL,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                PurchasedAsset(PurchasedUiAsset.ICON_COIN, Modifier.fillMaxSize())
                Text(if (amount >= 1000) "${amount / 1000}K" else amount.toString(), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.BottomEnd))
            }
            Column(Modifier.weight(1f)) {
                Text("$amount Son Coin", fontWeight = FontWeight.Black, color = Color(0xFF4A2D20), fontSize = 13.sp)
                Text(subtitle, color = Color(0xFF765746), fontSize = 9.sp)
            }
            val realPrice = product?.oneTimePurchaseOfferDetails?.formattedPrice
            PurchasedButton(
                text = when { busy -> "…"; realPrice != null -> realPrice; else -> sh("PLAY'DE YOK", "NOT ON PLAY") },
                onClick = onBuy,
                enabled = !busy && realPrice != null,
                modifier = Modifier.widthIn(min = 100.dp, max = 136.dp),
                style = PurchasedButtonStyle.PRIMARY,
                leadingAsset = PurchasedUiAsset.ICON_COIN,
            )
        }
    }
}
