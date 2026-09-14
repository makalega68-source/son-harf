package com.sonharf.game

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector
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
                    notice = sh("Google Play ürün bilgisi alınamadı.", "Google Play product information is missing.")
                    busy = false
                } else {
                    scope.launch {
                        busy = true
                        runCatching { PlayPurchaseVerification.verify(productId, purchase.purchaseToken) }
                            .onSuccess {
                                notice = sh("PRO üyeliğin sunucuda doğrulandı.", "Your PRO membership was verified on the server.")
                                onVerified()
                            }
                            .onFailure { error ->
                                notice = if ("google_play_not_configured" in error.message.orEmpty()) {
                                    sh("Ödeme doğrulama servisi henüz yayın anahtarıyla yapılandırılmadı.", "Purchase verification is not configured for release yet.")
                                } else {
                                    sh("Ödeme alındı ancak sunucu doğrulaması tamamlanamadı. Tekrar denemek ikinci kez ücretlendirmez.", "Payment was received but server verification is pending. Retrying will not charge twice.")
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
            manager.querySubscriptions(listOf(ProductCatalog.VIP_MONTHLY, ProductCatalog.VIP_YEARLY)) { products = it }
        }
        onDispose { manager.close() }
    }

    val selectedId = if (yearly) ProductCatalog.VIP_YEARLY else ProductCatalog.VIP_MONTHLY
    val selectedProduct = products[selectedId]
    val monthlyPrice = subscriptionPrice(products[ProductCatalog.VIP_MONTHLY]) ?: sh("Play fiyatı", "Play price")
    val yearlyPrice = subscriptionPrice(products[ProductCatalog.VIP_YEARLY]) ?: sh("Play fiyatı", "Play price")

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
                        Text(sh("Kelime Kuşatması PRO", "Word Siege PRO"), color = ProText, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text(sh("Adil premium üyelik", "Fair-play premium membership"), color = ProBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onDismiss, enabled = !busy) { Text("✕", color = ProText, fontSize = 18.sp) }
                }

                ProBenefit(Icons.Rounded.Block, sh("REKLAMSIZ", "AD-FREE"), sh("Menü ve mağazada reklamsız deneyim", "Ad-free menus and shop"))
                ProBenefit(Icons.Rounded.Palette, sh("PRO STYLE", "PRO STYLE"), sh("Özel görünüm ve profil ayrıcalıkları", "Exclusive appearance and profile benefits"))
                ProBenefit(Icons.Rounded.MeetingRoom, sh("ÖZEL ODALAR", "PRIVATE ROOMS"), sh("Arkadaşlarınla özel oyun alanları", "Private play spaces with friends"))
                ProBenefit(Icons.Rounded.Insights, sh("GELİŞMİŞ İSTATİSTİK", "ADVANCED STATS"), sh("Detaylı maç analizi ve performans", "Detailed match analysis and performance"))
                ProBenefit(Icons.Rounded.Groups, sh("SOSYAL AYRICALIKLAR", "SOCIAL BENEFITS"), sh("Kaydedilmiş arkadaş listesi ve PRO profil değeri", "Saved friend list and PRO profile value"))

                Surface(shape = RoundedCornerShape(14.dp), color = ProGreen.copy(alpha = .10f), border = BorderStroke(1.dp, ProGreen.copy(alpha = .35f))) {
                    Text(
                        sh(
                            "ADİL REKABET: PRO, dereceli Premier maçlarda skor, hedef harf veya kelime avantajı vermez.",
                            "FAIR PLAY: PRO gives no score, target-letter, or word advantage in ranked Premier matches.",
                        ),
                        Modifier.fillMaxWidth().padding(11.dp), color = ProGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    )
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    ProPlan(sh("AYLIK", "MONTHLY"), monthlyPrice, !yearly, Modifier.weight(1f)) { yearly = false }
                    ProPlan(sh("YILLIK", "YEARLY"), yearlyPrice, yearly, Modifier.weight(1f)) { yearly = true }
                }

                Button(
                    onClick = {
                        if (activity == null) {
                            notice = sh("Google Play ödeme ekranı açılamadı.", "Google Play billing could not be opened.")
                            return@Button
                        }
                        val product = selectedProduct
                        if (product == null) {
                            notice = if (connected) sh("Seçilen PRO ürünü bu hesap için kullanılamıyor.", "The selected PRO product is unavailable for this account.")
                            else sh("Google Play bağlantısı hazırlanıyor.", "Connecting to Google Play.")
                            return@Button
                        }
                        busy = true
                        val result = manager.launchProduct(activity, product)
                        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                            busy = false
                            notice = sh("Google Play ödeme ekranı açılamadı (${result.responseCode}).", "Google Play billing could not open (${result.responseCode}).")
                        }
                    },
                    enabled = !busy && selectedProduct != null,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ProBlue),
                ) {
                    Text(if (busy) sh("DOĞRULANIYOR…", "VERIFYING…") else sh("PRO'YA GEÇ", "GET PRO"), fontWeight = FontWeight.Black, fontSize = 15.sp)
                }

                TextButton(enabled = !busy, onClick = { manager.restorePurchases(setOf(ProductCatalog.VIP_MONTHLY, ProductCatalog.VIP_YEARLY)) }) {
                    Text(sh("Satın almaları geri yükle", "Restore purchases"))
                }
                if (notice.isNotBlank()) Text(notice, Modifier.fillMaxWidth(), color = ProMuted, fontSize = 9.sp, textAlign = TextAlign.Center)
                Text(sh("Google Play ile güvenli ödeme • İstediğin zaman iptal", "Secure Google Play billing • Cancel anytime"), Modifier.fillMaxWidth(), color = ProMuted, fontSize = 8.sp, textAlign = TextAlign.Center)
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

@Composable
private fun ProPlan(title: String, price: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) ProBlue.copy(alpha = .16f) else ProSurface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) ProBlue else ProBorder),
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = if (selected) ProBlue else ProText, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Text(price, color = ProMuted, fontSize = 9.sp, textAlign = TextAlign.Center)
        }
    }
}

private fun subscriptionPrice(details: ProductDetails?): String? =
    details?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
