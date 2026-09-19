package com.sonharf.game

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.sonharf.game.billing.BillingManager
import com.sonharf.game.billing.PlayPurchaseVerification
import com.sonharf.game.billing.ProductCatalog
import kotlinx.coroutines.launch

private val ProBg: Color get() = SonHarfTheme.Background
private val ProSurface: Color get() = SonHarfTheme.Surface
private val ProBorder: Color get() = SonHarfTheme.Border
private val ProText: Color get() = SonHarfTheme.TextPrimary
private val ProMuted: Color get() = SonHarfTheme.TextSecondary
private val ProBlue: Color get() = SonHarfTheme.Primary
private val ProGold: Color get() = SonHarfTheme.Warning
private val ProGreen: Color get() = SonHarfTheme.Success

@Composable
fun VipPurchaseDialog(onVerified: () -> Unit = {}, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    var notice by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var connected by remember { mutableStateOf(false) }
    var product by remember { mutableStateOf<ProductDetails?>(null) }

    val manager = remember {
        BillingManager(
            context = context,
            onPurchase = { purchase ->
                val productId = purchase.products.firstOrNull()
                if (productId != ProductCatalog.PRO_LIFETIME) {
                    notice = sh("Google Play PRO ürün bilgisi doğrulanamadı.", "Google Play PRO product information could not be verified.")
                    busy = false
                } else {
                    scope.launch {
                        busy = true
                        runCatching { PlayPurchaseVerification.verify(ProductCatalog.PRO_LIFETIME, purchase.purchaseToken) }
                            .onSuccess {
                                notice = sh(
                                    "PRO kalıcı olarak açıldı. İlk doğrulamada 100 Son Coin hesabına eklenir.",
                                    "PRO is permanently unlocked. 100 Son Coins are granted on the first verified purchase.",
                                )
                                onVerified()
                            }
                            .onFailure { error ->
                                notice = if ("google_play_not_configured" in error.message.orEmpty()) {
                                    sh(
                                        "Google Play sunucu doğrulaması production hesabıyla henüz etkin değil.",
                                        "Google Play server verification is not enabled with the production account yet.",
                                    )
                                } else {
                                    sh(
                                        "Ödeme alındı ancak sunucu doğrulaması tamamlanamadı. Aynı token ikinci kez hak veya Coin vermez.",
                                        "Payment was received but server verification is pending. The same token cannot grant entitlement or Coins twice.",
                                    )
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
            connected = true
            manager.queryOneTimeProducts(listOf(ProductCatalog.PRO_LIFETIME)) { details ->
                product = details[ProductCatalog.PRO_LIFETIME]
                // Silent account recovery only: there is intentionally no visible Restore Purchases button.
                manager.restorePurchases(setOf(ProductCatalog.PRO_LIFETIME))
            }
        }
        onDispose { manager.close() }
    }

    val playPrice = product?.oneTimePurchaseOfferDetails?.formattedPrice
        ?: ProductCatalog.PRO_LIFETIME_FALLBACK_PRICE_TRY

    Dialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).widthIn(max = 430.dp),
            shape = RoundedCornerShape(28.dp),
            color = ProBg,
            border = BorderStroke(1.dp, ProBorder),
            shadowElevation = 16.dp,
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Kelime Kuşatması PRO", color = ProText, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text(sh("Tek ödeme • Kalıcı kullanım", "One payment • Permanent access"), color = ProBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onDismiss, enabled = !busy) { Text("✕", color = ProText, fontSize = 18.sp) }
                }

                ProBenefit(Icons.Rounded.Block, sh("REKLAMSIZ", "AD-FREE"), sh("Uygulamadaki reklam alanları kaldırılır", "Ad placements are removed from the app"))
                ProBenefit(Icons.Rounded.Calculate, sh("PUAN HESAPLAYICI", "SCORE CALCULATOR"), sh("Kuşatma hamlesini onaylamadan önce gerçek motorla önizle", "Preview Siege scoring with the real engine before confirming"))
                ProBenefit(Icons.Rounded.GridView, sh("HARF TABLOSU", "LETTER TABLE"), sh("Rakip elini açmadan matematiksel kalan harfleri gör", "See mathematically remaining letters without exposing the opponent rack"))
                ProBenefit(Icons.Rounded.Timer, sh("SERİ OYUN", "SERIES GAME"), sh("3 / 5 / 10 dakikalık ayrı hızlı oyun sistemi", "Separate 3 / 5 / 10 minute fast-game system"))
                ProBenefit(Icons.Rounded.Groups, sh("ARKADAŞ LİSTESİ", "FRIENDS LIST"), sh("Arkadaş yönetimi ve arkadaş üzerinden oyun daveti", "Friend management and friend-based game invites"))
                ProBenefit(Icons.Rounded.History, sh("SON HARF TAM GEÇMİŞ", "FULL SON HARF HISTORY"), sh("Maçta oynanan tüm kelimeleri görüntüle", "View every word played in the match"))
                ProBenefit(Icons.Rounded.SportsEsports, sh("50 AKTİF OYUN", "50 ACTIVE GAMES"), sh("Aynı anda en fazla 50 aktif oyun", "Up to 50 active games at once"))
                ProBenefit(Icons.Rounded.WorkspacePremium, sh("PRO PROFİL", "PRO PROFILE"), sh("Özel profil çerçevesi ve PRO rozeti", "Exclusive profile frame and PRO badge"))
                ProBenefit(Icons.Rounded.Stars, "100 SON COIN", sh("İlk başarılı PRO grant'inde tek sefer", "Granted once on the first successful PRO grant"))

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ProSurface,
                    border = BorderStroke(2.dp, ProBlue.copy(alpha = .55f)),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("PRO LIFETIME", color = ProBlue, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            Text(sh("Kalıcı erişim", "Permanent access"), color = ProMuted, fontSize = 9.sp)
                        }
                        Text(playPrice, color = ProText, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                }

                Button(
                    onClick = {
                        if (activity == null) {
                            notice = sh("Google Play ödeme ekranı açılamadı.", "Google Play billing could not be opened.")
                            return@Button
                        }
                        val details = product
                        if (details == null) {
                            notice = if (connected) {
                                sh("PRO ürünü Google Play'de bu hesap için henüz satışa açık değil.", "The PRO product is not yet available for this account on Google Play.")
                            } else {
                                sh("Google Play bağlantısı hazırlanıyor.", "Connecting to Google Play.")
                            }
                            return@Button
                        }
                        busy = true
                        val result = manager.launchProduct(activity, details)
                        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                            busy = false
                            notice = sh(
                                "Google Play ödeme ekranı açılamadı (${result.responseCode}).",
                                "Google Play billing could not open (${result.responseCode}).",
                            )
                        }
                    },
                    enabled = !busy && product?.oneTimePurchaseOfferDetails != null,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ProBlue,
                        disabledContainerColor = SonHarfTheme.DisabledBackground,
                        disabledContentColor = SonHarfTheme.DisabledContent,
                    ),
                ) {
                    Text(
                        if (busy) sh("DOĞRULANIYOR…", "VERIFYING…") else sh("PRO'YU SATIN AL", "BUY PRO"),
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                    )
                }

                if (product == null) {
                    Text(
                        sh(
                            "Satın alma yalnızca Google Play gerçek ürün bilgisini döndürdüğünde açılır.",
                            "Purchase is enabled only when Google Play returns real product details.",
                        ),
                        Modifier.fillMaxWidth(),
                        color = ProMuted,
                        fontSize = 8.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                if (notice.isNotBlank()) {
                    Text(notice, Modifier.fillMaxWidth(), color = ProMuted, fontSize = 9.sp, textAlign = TextAlign.Center)
                }
                Text(
                    sh("Google Play ile güvenli tek ödeme", "Secure one-time Google Play purchase"),
                    Modifier.fillMaxWidth(),
                    color = ProMuted,
                    fontSize = 8.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ProBenefit(icon: ImageVector, title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(12.dp), color = ProBlue.copy(alpha = .13f)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(20.dp), tint = ProBlue) }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = ProText, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = ProMuted, fontSize = 9.sp)
        }
        Text("✓", color = ProGreen, fontWeight = FontWeight.Black)
    }
}
