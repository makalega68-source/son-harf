package com.sonharf.game

import android.app.Activity
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.sonharf.game.billing.BillingManager
import com.sonharf.game.billing.PlayPurchaseVerification
import com.sonharf.game.billing.ProductCatalog
import com.sonharf.game.data.OnlineGameBackend
import kotlinx.coroutines.launch

/** One Google Play gold pack: product id, gold amount, label and artwork. */
private data class GoldPack(val productId: String, val amount: Int, val label: String, @DrawableRes val art: Int, val badge: String? = null)

/**
 * The store's Gold tab: real-money gold packs through Google Play. The server verifies each
 * purchase, adds the gold once and consumes it, so a pack can be bought again.
 */
@Composable
internal fun GoldStoreSection(onBalance: (Int?) -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val backend = remember { OnlineGameBackend() }
    var products by remember { mutableStateOf<Map<String, ProductDetails>>(emptyMap()) }
    var busy by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf("") }
    val packs = remember {
        listOf(
            GoldPack(ProductCatalog.COINS_500, 500, sh("Kese", "Pouch"), R.drawable.premium_coin_500),
            GoldPack(ProductCatalog.COINS_1500, 1500, sh("Sandık", "Chest"), R.drawable.premium_coin_1500),
            GoldPack(ProductCatalog.COINS_3500, 3500, sh("Hazine", "Treasure"), R.drawable.premium_coin_3500, sh("EN POPÜLER", "MOST POPULAR")),
            GoldPack(ProductCatalog.COINS_8000, 8000, sh("Taht Hazinesi", "Throne Hoard"), R.drawable.premium_coin_8000, sh("EN AVANTAJLI", "BEST VALUE")),
        )
    }

    fun refreshBalance() {
        scope.launch {
            backend.currentUserId()?.let { id -> onBalance(runCatching { backend.getProfile(id).diamonds }.getOrNull()) }
        }
    }

    val manager = remember {
        BillingManager(
            context = context,
            onPurchase = { purchase ->
                val productId = purchase.products.firstOrNull()
                if (productId != null && productId in ProductCatalog.consumableProducts) scope.launch {
                    busy = productId
                    runCatching { PlayPurchaseVerification.verify(productId, purchase.purchaseToken) }
                        .onSuccess {
                            val amount = packs.firstOrNull { it.productId == productId }?.amount
                            notice = if (amount != null) sh("$amount Altın hesabına eklendi.", "$amount Gold added to your account.")
                            else sh("Satın alma doğrulandı.", "Purchase verified.")
                            refreshBalance()
                        }
                        .onFailure { error ->
                            notice = if ("google_play_not_configured" in error.message.orEmpty())
                                sh("Google Play doğrulaması henüz etkin değil.", "Google Play verification is not enabled yet.")
                            else sh("Ödeme doğrulanamadı. Aynı ödeme iki kez sayılmaz; tekrar deneyebilirsin.",
                                "Purchase verification failed. The same purchase never counts twice; you can retry.")
                        }
                    busy = null
                }
            },
            onMessage = { message -> notice = message; busy = null },
        )
    }

    DisposableEffect(manager) {
        manager.connect {
            manager.queryOneTimeProducts(ProductCatalog.consumableProducts.toList()) { products = it }
            // A pack paid for but not yet delivered (app closed mid-purchase) is delivered now.
            manager.restorePurchases(ProductCatalog.consumableProducts)
        }
        onDispose { manager.close() }
    }

    fun buy(productId: String) {
        val product = products[productId]
        if (activity == null || product?.oneTimePurchaseOfferDetails == null) {
            notice = sh("Bu paket Google Play'de henüz satışta değil.", "This pack is not on sale on Google Play yet.")
            return
        }
        busy = productId
        val result = manager.launchProduct(activity, product)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            busy = null
            notice = sh("Google Play ödeme ekranı açılamadı (${result.responseCode}).", "Google Play billing could not open (${result.responseCode}).")
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(sh("Altın Paketleri", "Gold Packs"), color = LobbyPalette.Ink, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(sh("Altınla mağazadan tema, klavye, çerçeve ve ipucu alabilirsin.",
            "Spend gold in the store on themes, keyboards, frames and hints."), color = LobbyPalette.Muted, fontSize = 12.sp)
        packs.forEach { pack ->
            GoldPackRow(pack, products[pack.productId], busy != null) { buy(pack.productId) }
        }
        if (notice.isNotBlank()) {
            Text(notice, Modifier.fillMaxWidth(), color = LobbyPalette.Ink, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
        Text(sh("Ödeme Google Play üzerinden alınır. Altın, ödeme doğrulandıktan sonra hesabına eklenir.",
            "Payment is taken by Google Play. Gold is added once the payment is verified."),
            color = LobbyPalette.Muted, fontSize = 10.sp, lineHeight = 14.sp)
    }
}

@Composable
private fun GoldPackRow(pack: GoldPack, product: ProductDetails?, busy: Boolean, onBuy: () -> Unit) {
    val offer = product?.oneTimePurchaseOfferDetails
    Surface(
        color = LobbyPalette.Paper,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(if (pack.badge != null) 2.dp else 1.dp, if (pack.badge != null) LobbyPalette.Gold else LobbyPalette.Line),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 84.dp).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(rememberArtPainter(pack.art, 300), null, Modifier.size(62.dp), contentScale = ContentScale.Fit)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                pack.badge?.let {
                    Text(it, color = LobbyPalette.Gold, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = .6.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HfCoin(18.dp)
                    Spacer(Modifier.width(5.dp))
                    Text(goldAmount(pack.amount), color = LobbyPalette.Ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
                Text(pack.label, color = LobbyPalette.Muted, fontSize = 11.sp)
            }
            Button(
                onClick = onBuy,
                enabled = !busy && offer != null,
                modifier = Modifier.defaultMinSize(minWidth = 86.dp, minHeight = 42.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LobbyPalette.Green, disabledContainerColor = LobbyPalette.Line),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(offer?.formattedPrice ?: sh("YAKINDA", "SOON"), fontWeight = FontWeight.Black, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

/** "1.500 Altın" / "1,500 Gold". */
internal fun goldAmount(amount: Int): String {
    val grouped = "%,d".format(amount).let { if (SonHarfUiState.language == "en") it else it.replace(',', '.') }
    return sh("$grouped Altın", "$grouped Gold")
}

/** The currency's name in the current language: "Altın" / "Gold". */
internal fun goldUnit(): String = sh("Altın", "Gold")
