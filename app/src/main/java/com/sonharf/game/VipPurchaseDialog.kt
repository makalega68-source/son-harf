package com.sonharf.game

import android.app.Activity
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.launch

@Composable
fun VipPurchaseDialog(onVerified: () -> Unit = {}, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    var yearly by remember { mutableStateOf(true) }
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
                    notice = "Google Play ürün bilgisi eksik döndü."
                    busy = false
                } else {
                    scope.launch {
                        busy = true
                        runCatching { PlayPurchaseVerification.verify(productId, purchase.purchaseToken) }
                            .onSuccess {
                                notice = "Satın alma Google Play üzerinden doğrulandı. VIP hesabına işlendi."
                                onVerified()
                            }
                            .onFailure { error ->
                                notice = when {
                                    "google_play_not_configured" in error.message.orEmpty() -> "Ödeme doğrulama servisi henüz yayın anahtarıyla yapılandırılmadı. Ücretlendirme tamamlandıysa destek kaydı oluştur."
                                    else -> "Ödeme alındı ancak sunucu doğrulaması tamamlanamadı. Tekrar dene; aynı satın alma ikinci kez ücretlendirilmez."
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
            manager.querySubscriptions(ProductCatalog.subscriptions) { products = it }
        }
        onDispose { manager.close() }
    }

    val transition = rememberInfiniteTransition(label = "vipPulse")
    val pulse by transition.animateFloat(.96f, 1.07f, infiniteRepeatable(tween(950), RepeatMode.Reverse), label = "vipScale")
    val glow by transition.animateFloat(.18f, .54f, infiniteRepeatable(tween(1350), RepeatMode.Reverse), label = "vipGlow")
    val sparkle by transition.animateFloat(.40f, 1f, infiniteRepeatable(tween(820), RepeatMode.Reverse), label = "vipSparkle")
    val panelBase = if (SonHarfUiState.darkMode) Color(0xFF100C06) else Color(0xFFFFFCF3)
    val selectedId = if (yearly) ProductCatalog.VIP_YEARLY else ProductCatalog.VIP_MONTHLY
    val selectedProduct = products[selectedId]

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        containerColor = Color.Transparent,
        shape = RoundedCornerShape(30.dp),
        title = null,
        text = {
            Column(
                Modifier.fillMaxWidth().background(
                    Brush.verticalGradient(
                        listOf(
                            SonHarfGold.copy(alpha = .22f + glow / 5f),
                            panelBase,
                            SonHarfPurple.copy(alpha = .10f),
                        )
                    ),
                    RoundedCornerShape(28.dp),
                ).padding(17.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.fillMaxWidth()) {
                    Text("✦", color = SonHarfGold.copy(alpha = sparkle), fontSize = 23.sp, modifier = Modifier.align(Alignment.TopStart))
                    Text("✧", color = SonHarfGold.copy(alpha = .35f + glow), fontSize = 18.sp, modifier = Modifier.align(Alignment.TopEnd))
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size(88.dp).scale(pulse).background(
                                Brush.radialGradient(listOf(Color(0xFFFFEDB1), SonHarfGold.copy(alpha = .34f))), CircleShape,
                            ), contentAlignment = Alignment.Center,
                        ) { Text("♛", color = Color(0xFF8C5700), fontSize = 48.sp, fontWeight = FontWeight.Black) }
                        Spacer(Modifier.height(7.dp))
                        Surface(color = SonHarfGold.copy(alpha = .14f), shape = RoundedCornerShape(99.dp), border = BorderStroke(1.dp, SonHarfGold.copy(alpha = .45f))) {
                            Text("PREMIUM ÜYELİK", Modifier.padding(horizontal = 12.dp, vertical = 4.dp), color = SonHarfGold, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                        }
                        Spacer(Modifier.height(5.dp))
                        Text("SON HARF VIP", fontSize = 28.sp, fontWeight = FontWeight.Black, color = SonHarfText)
                        Text("Oyunun en ayrıcalıklı tarafına geç.", color = SonHarfMuted, fontSize = 10.sp, textAlign = TextAlign.Center)
                    }
                }

                Surface(
                    color = SonHarfGold.copy(alpha = .08f + glow / 8f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, SonHarfGold.copy(alpha = .50f)),
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            VipBenefit("◇", "400 ELMAS", "Her ay", Modifier.weight(1f))
                            VipBenefit("♛", "ÖZEL ODA", "VIP erişim", Modifier.weight(1f))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            VipBenefit("✦", "KOZMETİK", "Premium stil", Modifier.weight(1f))
                            VipBenefit("↗", "İSTATİSTİK", "Gelişmiş", Modifier.weight(1f))
                        }
                        Surface(color = SonHarfSurface.copy(alpha = .72f), shape = RoundedCornerShape(13.dp)) {
                            Text("✓ Reklamsız deneyim  •  ✓ VIP profil dokunuşları", Modifier.fillMaxWidth().padding(9.dp), color = SonHarfText, fontSize = 8.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VipPlanCard("AYLIK", subscriptionPrice(products[ProductCatalog.VIP_MONTHLY]) ?: "Play fiyatı", !yearly, false, Modifier.weight(1f)) { yearly = false }
                    VipPlanCard("YILLIK", subscriptionPrice(products[ProductCatalog.VIP_YEARLY]) ?: "Play fiyatı", yearly, true, Modifier.weight(1f)) { yearly = true }
                }

                Button(
                    onClick = {
                        if (activity == null) {
                            notice = "Google Play ödeme ekranı bu cihazda açılamadı."
                            return@Button
                        }
                        val product = selectedProduct
                        if (product == null) {
                            notice = if (connected) "Seçilen VIP ürünü Google Play hesabında yayımlı değil veya bu kullanıcı için kullanılamıyor." else "Google Play bağlantısı hazırlanıyor."
                            return@Button
                        }
                        busy = true
                        val result = manager.launchProduct(activity, product)
                        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                            busy = false
                            notice = "Google Play ödeme ekranı açılamadı (${result.responseCode})."
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SonHarfGold, contentColor = Color(0xFF302000)),
                    shape = RoundedCornerShape(18.dp),
                ) { Text(if (busy) "DOĞRULANIYOR…" else if (yearly) "YILLIK VIP İLE DEVAM  ✦" else "AYLIK VIP İLE DEVAM  ✦", fontWeight = FontWeight.Black) }

                Text("Google Play ile güvenli ödeme • Sunucu doğrulaması • İstediğin zaman iptal", color = SonHarfMuted, fontSize = 8.sp, textAlign = TextAlign.Center)
                if (notice.isNotBlank()) {
                    Surface(color = SonHarfSurface.copy(alpha = .76f), shape = RoundedCornerShape(12.dp)) {
                        Text(notice, Modifier.fillMaxWidth().padding(9.dp), color = SonHarfMuted, fontSize = 8.sp, textAlign = TextAlign.Center)
                    }
                }
                TextButton(onClick = onDismiss, enabled = !busy) { Text("ŞİMDİ DEĞİL", color = SonHarfMuted, fontWeight = FontWeight.Bold) }
            }
        },
        confirmButton = {},
        dismissButton = {},
    )
}

private fun subscriptionPrice(details: ProductDetails?): String? =
    details?.subscriptionOfferDetails
        ?.firstOrNull()
        ?.pricingPhases
        ?.pricingPhaseList
        ?.lastOrNull()
        ?.formattedPrice

@Composable
private fun VipBenefit(icon: String, title: String, subtitle: String, modifier: Modifier) {
    Surface(modifier = modifier, color = SonHarfSurface.copy(alpha = .76f), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, SonHarfGold.copy(alpha = .18f))) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, color = SonHarfGold, fontSize = 19.sp, fontWeight = FontWeight.Black)
            Text(title, color = SonHarfText, fontSize = 9.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = SonHarfMuted, fontSize = 7.sp)
        }
    }
}

@Composable
private fun VipPlanCard(title: String, subtitle: String, selected: Boolean, best: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = if (selected) SonHarfGold.copy(alpha = .18f) else SonHarfSurface.copy(alpha = .78f)),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) SonHarfGold else SonHarfMuted.copy(alpha = .18f)),
        shape = RoundedCornerShape(17.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (best) Text("EN AVANTAJLI", color = SonHarfGold, fontSize = 6.sp, fontWeight = FontWeight.Black, letterSpacing = .5.sp)
            Text(title, fontWeight = FontWeight.Black, color = if (selected) SonHarfGold else SonHarfText, fontSize = 13.sp)
            Text(subtitle, color = SonHarfMuted, fontSize = 7.sp, textAlign = TextAlign.Center)
        }
    }
}
