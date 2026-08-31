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
import androidx.compose.ui.graphics.Color
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
fun SeasonPassPurchaseCard(onPurchased: () -> Unit = {}) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    var product by remember { mutableStateOf<ProductDetails?>(null) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }

    val manager = remember {
        BillingManager(
            context = context,
            onPurchase = { purchase ->
                val productId = purchase.products.firstOrNull()
                if (productId != ProductCatalog.SEASON_PASS_MONTHLY) {
                    notice = sh("Sezon bileti ürün bilgisi alınamadı.", "Season pass product information is missing.")
                    busy = false
                } else {
                    scope.launch {
                        busy = true
                        runCatching { PlayPurchaseVerification.verify(productId, purchase.purchaseToken) }
                            .onSuccess {
                                notice = sh("Sezon Bileti etkinleştirildi.", "Season Pass activated.")
                                onPurchased()
                            }
                            .onFailure { error ->
                                notice = when {
                                    "google_play_not_configured" in error.message.orEmpty() ->
                                        sh("Google Play sunucu doğrulaması production hesabıyla yapılandırılmalı.", "Google Play server verification must be configured with the production account.")
                                    else -> sh("Sezon Bileti doğrulanamadı. Tekrar deneyebilirsin.", "Season Pass verification failed. You can retry.")
                                }
                            }
                        busy = false
                    }
                }
            },
            onMessage = { message -> notice = message; busy = false },
        )
    }

    DisposableEffect(manager) {
        manager.connect {
            manager.querySubscriptions(listOf(ProductCatalog.SEASON_PASS_MONTHLY)) {
                product = it[ProductCatalog.SEASON_PASS_MONTHLY]
            }
        }
        onDispose { manager.close() }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MainUi.Surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MainUi.Gold.copy(alpha = .38f)),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(sh("SEZON BİLETİ", "SEASON PASS"), color = MainUi.Text, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text(
                        sh(
                            "Premium ödül yolu • her açık seviyede ek Son Coin",
                            "Premium reward track • extra Son Coin at every unlocked level",
                        ),
                        color = MainUi.Muted,
                        fontSize = 9.sp,
                    )
                }
                Text("◈", color = MainUi.Gold, fontSize = 26.sp, fontWeight = FontWeight.Black)
            }

            Text(
                sh(
                    "Sezon Bileti görünüm ve ilerleme ödülleri verir; maç gücü vermez.",
                    "The Season Pass gives progression and collection rewards; it never gives match power.",
                ),
                color = MainUi.Green,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                sh(
                    "Mevcut sezonda sunucunun gerçekten verdiği premium ödüller Son Coin'dir. Sezonluk çerçeve, unvan veya Style içerikleri backend tarafından tanımlanana kadar satın alma vaadine dahil edilmez.",
                    "In the current season, the server-backed premium rewards are Son Coin. Seasonal frames, titles or Style items are not promised until the backend actually grants them.",
                ),
                color = MainUi.Muted,
                fontSize = 8.5.sp,
            )

            Button(
                onClick = {
                    val details = product
                    if (activity == null || details == null) {
                        notice = sh("Sezon Bileti Google Play'de henüz kullanılamıyor.", "Season Pass is not available on Google Play yet.")
                        return@Button
                    }
                    busy = true
                    val result = manager.launchProduct(activity, details)
                    if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                        busy = false
                        notice = sh("Google Play ödeme ekranı açılamadı.", "Google Play billing could not open.")
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MainUi.Blue, contentColor = Color.White),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    if (busy) "…" else seasonPassPrice(product) ?: sh("PLAY'DE GÖR", "VIEW ON PLAY"),
                    fontWeight = FontWeight.Black,
                )
            }

            if (notice.isNotBlank()) {
                Text(notice, color = MainUi.Muted, fontSize = 9.sp)
            }
        }
    }
}

private fun seasonPassPrice(details: ProductDetails?): String? =
    details?.subscriptionOfferDetails
        ?.firstOrNull()
        ?.pricingPhases
        ?.pricingPhaseList
        ?.lastOrNull()
        ?.formattedPrice
