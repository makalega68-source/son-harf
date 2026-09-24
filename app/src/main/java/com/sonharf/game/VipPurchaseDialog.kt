package com.sonharf.game

import android.app.Activity
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.sonharf.game.billing.BillingManager
import com.sonharf.game.billing.PlayPurchaseVerification
import com.sonharf.game.billing.ProductCatalog
import kotlinx.coroutines.launch

@Composable
fun VipPurchaseDialog(
    onVerified: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    var yearly by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var connected by remember { mutableStateOf(false) }
    var products by remember { mutableStateOf<Map<String, ProductDetails>>(emptyMap()) }

    val manager = remember {
        BillingManager(
            context = context,
            onPurchase = { purchase ->
                val productId = purchase.products.firstOrNull()
                if (productId.isNullOrBlank()) {
                    notice = sh(
                        "Google Play ürün bilgisi alınamadı.",
                        "Google Play product information is missing.",
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
                                    "PRO üyeliğin sunucuda doğrulandı.",
                                    "Your PRO membership was verified on the server.",
                                )
                                onVerified()
                            }
                            .onFailure { error ->
                                notice = if (
                                    "google_play_not_configured" in error.message.orEmpty()
                                ) {
                                    sh(
                                        "Ödeme doğrulama servisi henüz yayın anahtarıyla yapılandırılmadı.",
                                        "Purchase verification is not configured for release yet.",
                                    )
                                } else {
                                    sh(
                                        "Ödeme alındı ancak sunucu doğrulaması tamamlanamadı. Tekrar denemek ikinci kez ücretlendirmez.",
                                        "Payment was received but server verification is pending. Retrying will not charge twice.",
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
            connected = true
            manager.querySubscriptions(
                listOf(ProductCatalog.VIP_MONTHLY, ProductCatalog.VIP_YEARLY),
            ) { products = it }
        }
        onDispose { manager.close() }
    }

    val selectedId = if (yearly) ProductCatalog.VIP_YEARLY else ProductCatalog.VIP_MONTHLY
    val selectedProduct = products[selectedId]
    val selectedPurchasable = BillingManager.hasPurchasableOffer(selectedProduct)
    val monthlyPrice = subscriptionPrice(products[ProductCatalog.VIP_MONTHLY])
        ?: sh("PLAY'DE YOK", "NOT ON PLAY")
    val yearlyPrice = subscriptionPrice(products[ProductCatalog.VIP_YEARLY])
        ?: sh("PLAY'DE YOK", "NOT ON PLAY")

    Dialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .widthIn(max = 430.dp),
            shape = GameShapes.Hero,
            color = GameColors.AppBackground,
            border = BorderStroke(1.dp, GameColors.Border),
            shadowElevation = GameElevation.High,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = GameShapes.Medium,
                        color = GameColors.RewardAmber.copy(alpha = .12f),
                    ) {
                        Icon(
                            Icons.Rounded.WorkspacePremium,
                            contentDescription = null,
                            tint = GameColors.RewardAmber,
                            modifier = Modifier.padding(9.dp).size(22.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "KELİME KUŞATMASI PRO",
                            color = GameColors.TextPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            sh("Adil premium üyelik", "Fair-play premium membership"),
                            color = GameColors.PrimaryBlue,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        enabled = !busy,
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = sh("Kapat", "Close"),
                            tint = GameColors.TextSecondary,
                        )
                    }
                }

                ProBenefit(
                    Icons.Rounded.Block,
                    sh("REKLAMSIZ", "AD-FREE"),
                    sh("Menü ve mağazada reklamsız deneyim", "Ad-free menus and shop"),
                )
                ProBenefit(
                    Icons.Rounded.Badge,
                    sh("PRO PROFİL", "PRO PROFILE"),
                    sh("PRO rozeti ve profil ayrıcalıkları", "PRO badge and profile benefits"),
                )
                ProBenefit(
                    Icons.Rounded.MeetingRoom,
                    sh("ÖZEL ODALAR", "PRIVATE ROOMS"),
                    sh("Arkadaşlarınla özel oyun alanları", "Private play spaces with friends"),
                )
                ProBenefit(
                    Icons.Rounded.Insights,
                    sh("GELİŞMİŞ İSTATİSTİK", "ADVANCED STATS"),
                    sh("Detaylı maç analizi ve performans", "Detailed match analysis and performance"),
                )
                ProBenefit(
                    Icons.Rounded.Groups,
                    sh("SOSYAL AYRICALIKLAR", "SOCIAL BENEFITS"),
                    sh(
                        "Kaydedilmiş arkadaş listesi ve PRO profil değeri",
                        "Saved friend list and PRO profile value",
                    ),
                )

                Surface(
                    shape = GameShapes.Medium,
                    color = GameColors.PlayGreen.copy(alpha = .09f),
                    border = BorderStroke(1.dp, GameColors.PlayGreen.copy(alpha = .32f)),
                ) {
                    Text(
                        sh(
                            "ADİL REKABET: PRO, dereceli maçlarda skor, hedef harf veya kelime avantajı vermez.",
                            "FAIR PLAY: PRO gives no score, target-letter, or word advantage in ranked matches.",
                        ),
                        modifier = Modifier.fillMaxWidth().padding(11.dp),
                        color = GameColors.PlayGreen,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    ProPlan(
                        title = sh("AYLIK", "MONTHLY"),
                        price = monthlyPrice,
                        imageRes = StoreProductArtwork.forProduct(ProductCatalog.VIP_MONTHLY, R.drawable.premium_vip_monthly),
                        selected = !yearly,
                        modifier = Modifier.weight(1f),
                    ) { yearly = false }
                    ProPlan(
                        title = sh("YILLIK", "YEARLY"),
                        price = yearlyPrice,
                        imageRes = StoreProductArtwork.forProduct(ProductCatalog.VIP_YEARLY, R.drawable.premium_vip_yearly),
                        selected = yearly,
                        modifier = Modifier.weight(1f),
                    ) { yearly = true }
                }

                Button(
                    onClick = {
                        if (activity == null) {
                            notice = sh(
                                "Google Play ödeme ekranı açılamadı.",
                                "Google Play billing could not be opened.",
                            )
                            return@Button
                        }
                        val product = selectedProduct
                        if (product == null || !BillingManager.hasPurchasableOffer(product)) {
                            notice = if (connected) {
                                sh(
                                    "Seçilen PRO teklifi bu hesap için kullanılamıyor.",
                                    "The selected PRO offer is unavailable for this account.",
                                )
                            } else {
                                sh(
                                    "Google Play bağlantısı hazırlanıyor.",
                                    "Connecting to Google Play.",
                                )
                            }
                            return@Button
                        }
                        busy = true
                        val result = manager.launchProduct(activity, product)
                        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                            busy = false
                            notice = sh(
                                "Google Play ödeme ekranı açılamadı (${result.responseCode}).",
                                "Google Play billing could not open (${result.responseCode}).",
                            )
                        }
                    },
                    enabled = !busy && selectedPurchasable,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = GameShapes.Medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GameColors.PrimaryBlue,
                        contentColor = GameColors.TextPrimary,
                        disabledContainerColor = GameColors.Disabled,
                        disabledContentColor = GameColors.DisabledContent,
                    ),
                ) {
                    Text(
                        if (busy) {
                            sh("DOĞRULANIYOR…", "VERIFYING…")
                        } else {
                            sh("PRO'YA GEÇ", "GET PRO")
                        },
                        fontWeight = FontWeight.Black,
                    )
                }

                TextButton(
                    enabled = !busy,
                    onClick = {
                        manager.restorePurchases(
                            setOf(ProductCatalog.VIP_MONTHLY, ProductCatalog.VIP_YEARLY),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        sh("Satın almaları geri yükle", "Restore purchases"),
                        color = GameColors.PrimaryBlue,
                    )
                }

                if (notice.isNotBlank()) {
                    Surface(
                        shape = GameShapes.Small,
                        color = GameColors.SecondarySurface,
                    ) {
                        Text(
                            notice,
                            modifier = Modifier.fillMaxWidth().padding(9.dp),
                            color = GameColors.TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Text(
                    sh(
                        "Google Play ile güvenli ödeme • İstediğin zaman iptal",
                        "Secure Google Play billing • Cancel anytime",
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    color = GameColors.TextTertiary,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ProBenefit(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = GameShapes.Medium,
            color = GameColors.PrimaryBlue.copy(alpha = .12f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = GameColors.PrimaryBlue,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = GameColors.TextPrimary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                color = GameColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Icon(
            Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = GameColors.PlayGreen,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun ProPlan(
    title: String,
    price: String,
    @DrawableRes imageRes: Int,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = GameShapes.Medium,
        color = if (selected) {
            GameColors.PrimaryBlue.copy(alpha = .13f)
        } else {
            GameColors.PrimarySurface
        },
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) GameColors.PrimaryBlue else GameColors.Border,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Image(
                painter = painterResource(imageRes),
                contentDescription = null,
                modifier = Modifier.size(58.dp),
                contentScale = ContentScale.Fit,
            )
            Text(
                title,
                color = if (selected) GameColors.PrimaryBlue else GameColors.TextPrimary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
            )
            Text(
                price,
                color = GameColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun subscriptionPrice(details: ProductDetails?): String? =
    details
        ?.subscriptionOfferDetails
        ?.firstOrNull()
        ?.pricingPhases
        ?.pricingPhaseList
        ?.lastOrNull()
        ?.formattedPrice
