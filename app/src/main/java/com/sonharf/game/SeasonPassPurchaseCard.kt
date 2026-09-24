package com.sonharf.game

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                    notice = sh(
                        "Sezon bileti ürün bilgisi alınamadı.",
                        "Season pass product information is missing.",
                    )
                    busy = false
                } else {
                    scope.launch {
                        busy = true
                        runCatching {
                            PlayPurchaseVerification.verify(productId, purchase.purchaseToken)
                        }
                            .onSuccess {
                                notice = sh(
                                    "Sezon Bileti etkinleştirildi.",
                                    "Season Pass activated.",
                                )
                                onPurchased()
                            }
                            .onFailure { error ->
                                notice = when {
                                    "google_play_not_configured" in error.message.orEmpty() -> sh(
                                        "Google Play sunucu doğrulaması production hesabıyla yapılandırılmalı.",
                                        "Google Play server verification must be configured with the production account.",
                                    )
                                    else -> sh(
                                        "Sezon Bileti doğrulanamadı. Tekrar deneyebilirsin.",
                                        "Season Pass verification failed. You can retry.",
                                    )
                                }
                            }
                        busy = false
                    }
                }
            },
            onMessage = { message ->
                notice = message
                busy = false
            },
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

    GameSurface(
        elevated = true,
        borderColor = GameColors.Lavender.copy(alpha = .42f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = GameShapes.Medium,
                color = GameColors.Lavender.copy(alpha = .10f),
                border = BorderStroke(
                    1.dp,
                    GameColors.Lavender.copy(alpha = .20f),
                ),
            ) {
                Image(
                    painter = painterResource(StoreProductArtwork.forProduct(ProductCatalog.SEASON_PASS_MONTHLY, R.drawable.premium_season_pass)),
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp).size(66.dp),
                    contentScale = ContentScale.Fit,
                )
            }

            Column(Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        sh("SEZON BİLETİ", "SEASON PASS"),
                        color = GameColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                    )
                    Surface(
                        shape = GameShapes.Pill,
                        color = GameColors.Lavender.copy(alpha = .12f),
                    ) {
                        Text(
                            sh("AYLIK", "MONTHLY"),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = GameColors.Lavender,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    sh(
                        "Premium ödül yolu • daha fazla Son Coin • ilerleme ödülleri",
                        "Premium reward track • more Son Coins • progression rewards",
                    ),
                    color = GameColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Surface(
            shape = GameShapes.Small,
            color = GameColors.PlayGreen.copy(alpha = .08f),
            border = BorderStroke(
                1.dp,
                GameColors.PlayGreen.copy(alpha = .25f),
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Rounded.WorkspacePremium,
                    contentDescription = null,
                    tint = GameColors.PlayGreen,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    sh(
                        "Yalnız ilerleme ve ekonomi ödülleri verir; rating, süre, joker gücü veya maç avantajı vermez.",
                        "Grants progression and economy rewards only; no rating, time, joker power or match advantages.",
                    ),
                    modifier = Modifier.weight(1f),
                    color = GameColors.PlayGreen,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = {
                val details = product
                if (activity == null || !BillingManager.hasPurchasableOffer(details)) {
                    notice = sh(
                        "Sezon Bileti Google Play'de henüz satın alınabilir değil.",
                        "Season Pass is not yet purchasable on Google Play.",
                    )
                    return@Button
                }
                busy = true
                val result = manager.launchProduct(activity, requireNotNull(details))
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    busy = false
                    notice = sh(
                        "Google Play ödeme ekranı açılamadı.",
                        "Google Play billing could not open.",
                    )
                }
            },
            enabled = !busy && BillingManager.hasPurchasableOffer(product),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GameColors.Lavender,
                contentColor = GameColors.TextPrimary,
                disabledContainerColor = GameColors.Disabled,
                disabledContentColor = GameColors.DisabledContent,
            ),
            shape = GameShapes.Medium,
        ) {
            Text(
                if (busy) {
                    "…"
                } else {
                    seasonPassPrice(product) ?: sh("PLAY'DE YOK", "NOT ON PLAY")
                },
                fontWeight = FontWeight.Black,
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            sh(
                "Aylık abonelik · Google Play üzerinden iptal edilebilir.",
                "Monthly subscription · Cancel through Google Play.",
            ),
            color = GameColors.TextTertiary,
            style = MaterialTheme.typography.labelSmall,
        )

        TextButton(
            enabled = !busy,
            onClick = {
                manager.restorePurchases(setOf(ProductCatalog.SEASON_PASS_MONTHLY))
            },
        ) {
            Text(
                sh("Satın almayı geri yükle", "Restore purchase"),
                color = GameColors.PrimaryBlue,
            )
        }

        if (notice.isNotBlank()) {
            Surface(
                shape = GameShapes.Small,
                color = GameColors.SecondarySurface,
                border = BorderStroke(1.dp, GameColors.Border),
            ) {
                Text(
                    notice,
                    modifier = Modifier.fillMaxWidth().padding(9.dp),
                    color = GameColors.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

private fun seasonPassPrice(details: ProductDetails?): String? =
    details
        ?.subscriptionOfferDetails
        ?.firstOrNull()
        ?.pricingPhases
        ?.pricingPhaseList
        ?.lastOrNull()
        ?.formattedPrice
