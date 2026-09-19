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
                        runCatching { PlayPurchaseVerification.verify(productId, purchase.purchaseToken) }
                            .onSuccess {
                                notice = sh(
                                    "PRO kalıcı olarak açıldı. İlk doğrulanan grant'te 100 Son Coin hesabına eklenir.",
                                    "PRO is unlocked permanently. 100 Son Coins are added on the first verified grant.",
                                )
                                onVerified()
                            }
                            .onFailure { error ->
                                notice = if ("google_play_not_configured" in error.message.orEmpty()) {
                                    sh(
                                        "Ödeme doğrulama servisi henüz yayın anahtarıyla yapılandırılmadı.",
                                        "Purchase verification is not configured for release yet.",
                                    )
                                } else {
                                    sh(
                                        "Ödeme alındı ancak sunucu doğrulaması tamamlanamadı. Yeniden doğrulama ikinci kez ücretlendirmez.",
                                        "Payment was received but server verification is pending. Re-verification will not charge twice.",
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
            manager.queryOneTimeProducts(listOf(ProductCatalog.PRO_LIFETIME)) {
                product = it[ProductCatalog.PRO_LIFETIME]
            }
            // Recover a completed lifetime purchase after an interrupted verification without
            // exposing a legacy restore action in the UI.
            manager.restorePurchases(setOf(ProductCatalog.PRO_LIFETIME))
        }
        onDispose { manager.close() }
    }

    val price = product?.oneTimePurchaseOfferDetails?.formattedPrice
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
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Kelime Kuşatması PRO", color = ProText, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text(
                            sh("Tek ödeme • kalıcı erişim", "One payment • lifetime access"),
                            color = ProBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    TextButton(onClick = onDismiss, enabled = !busy) { Text("✕", color = ProText, fontSize = 18.sp) }
                }

                ProBenefit(Icons.Rounded.Block, sh("REKLAMSIZ", "AD-FREE"), sh("Menü ve mağazada reklamsız deneyim", "Ad-free menus and shop"))
                ProBenefit(Icons.Rounded.Calculate, sh("PREMİUM ARAÇLAR", "PREMIUM TOOLS"), sh("Harf Tablosu ve Puan Hesaplayıcı erişimi", "Letter Table and Score Calculator access"))
                ProBenefit(Icons.Rounded.PlayArrow, sh("SERİ OYUN", "SERIES GAME"), sh("3/5/10 dakikalık hızlı oyun modu", "3/5/10-minute fast game mode"))
                ProBenefit(Icons.Rounded.Groups, sh("SOSYAL AYRICALIKLAR", "SOCIAL BENEFITS"), sh("Kaydedilmiş arkadaş listesi ve PRO profil çerçevesi", "Saved friends list and PRO profile frame"))
                ProBenefit(Icons.Rounded.History, sh("TAM GEÇMİŞ", "FULL HISTORY"), sh("Son Harf kelime geçmişine tam erişim", "Full Son Harf word-history access"))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ProSurface,
                    border = BorderStroke(1.dp, ProGold.copy(alpha = .45f)),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(sh("PRO KALICI LİSANS", "PRO LIFETIME"), color = ProText, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            Text(sh("Abonelik yok • tekrar eden ödeme yok", "No subscription • no recurring charge"), color = ProMuted, fontSize = 9.sp)
                        }
                        Text(price, color = ProGold, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                }

                Button(
                    onClick = {
                        if (activity == null) {
                            notice = sh("Google Play ödeme ekranı açılamadı.", "Google Play billing could not be opened.")
                            return@Button
                        }
                        val selected = product
                        if (selected?.oneTimePurchaseOfferDetails == null) {
                            notice = if (connected) {
                                sh("PRO ürünü bu hesap için henüz Google Play'de satın alınabilir değil.", "The PRO product is not yet purchasable on Google Play for this account.")
                            } else {
                                sh("Google Play bağlantısı hazırlanıyor.", "Connecting to Google Play.")
                            }
                            return@Button
                        }
                        busy = true
                        val result = manager.launchProduct(activity, selected)
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
                    colors = ButtonDefaults.buttonColors(containerColor = ProBlue),
                ) {
                    Text(
                        if (busy) sh("DOĞRULANIYOR…", "VERIFYING…") else sh("PRO'YU KALICI AÇ", "UNLOCK PRO FOREVER"),
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                    )
                }

                if (notice.isNotBlank()) {
                    Text(notice, Modifier.fillMaxWidth(), color = ProMuted, fontSize = 9.sp, textAlign = TextAlign.Center)
                }
                Text(
                    sh("Google Play ile güvenli tek ödeme", "Secure one-time payment with Google Play"),
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
private fun ProBenefit(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
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
