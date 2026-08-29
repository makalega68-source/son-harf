package com.sonharf.game

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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

private val VipNavy = Color(0xFF111827)
private val VipNavy2 = Color(0xFF192235)
private val VipGold = Color(0xFFFFB31A)
private val VipText = Color(0xFFF8FAFC)
private val VipMuted = Color(0xFFAAB4C4)
private val VipGreen = Color(0xFF35C47C)

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
                                notice = "VIP hesabına işlendi."
                                onVerified()
                            }
                            .onFailure { error ->
                                notice = when {
                                    "google_play_not_configured" in error.message.orEmpty() ->
                                        "Ödeme doğrulama servisi henüz yayın anahtarıyla yapılandırılmadı."
                                    else ->
                                        "Ödeme alındı fakat sunucu doğrulaması tamamlanamadı. Tekrar denediğinde ikinci kez ücret alınmaz."
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
            manager.querySubscriptions(ProductCatalog.subscriptions) { products = it }
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .widthIn(max = 430.dp),
            shape = RoundedCornerShape(28.dp),
            color = VipNavy,
            shadowElevation = 20.dp,
        ) {
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFFFFD76A),
                                    VipGold,
                                    Color(0xFFFF9F12),
                                )
                            )
                        )
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = Color.White.copy(alpha = .22f)) {
                            Text("♛", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = Color(0xFF4A2B00), fontSize = 27.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text("SON HARF VIP", color = Color(0xFF2C1A00), fontSize = 23.sp, fontWeight = FontWeight.Black)
                            Text(sh("Daha temiz. Daha kişisel.", "Cleaner. More personal."), color = Color(0xFF5A3900), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = onDismiss, enabled = !busy) {
                            Text("✕", color = Color(0xFF2C1A00), fontSize = 18.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                Column(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        sh("VIP ayrıcalıkları", "VIP benefits"),
                        color = VipText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                    )

                    VipModernBenefit("◆", "400 SON COIN", sh("Her ay hesabına", "Added every month"))
                    VipModernBenefit("♛", sh("ÖZEL ODA", "PRIVATE ROOM"), sh("Arkadaşlarınla özel eşleşme", "Private matches with friends"))
                    VipModernBenefit("✦", "STYLE", sh("VIP profil ve görünüm seçenekleri", "VIP profile and appearance options"))
                    VipModernBenefit("↗", sh("GELİŞMİŞ İSTATİSTİK", "ADVANCED STATS"), sh("Daha ayrıntılı performans görünümü", "More detailed performance view"))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = VipGreen.copy(alpha = .10f),
                    ) {
                        Text(
                            sh("✓ Reklamsız deneyim   •   ✓ Rekabet gücü vermez", "✓ Ad-free   •   ✓ No competitive power"),
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                            color = VipGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                    }

                    Text(sh("Planını seç", "Choose your plan"), color = VipMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        VipModernPlan(
                            title = sh("AYLIK", "MONTHLY"),
                            price = monthlyPrice,
                            selected = !yearly,
                            badge = null,
                            modifier = Modifier.weight(1f),
                        ) { yearly = false }
                        VipModernPlan(
                            title = sh("YILLIK", "YEARLY"),
                            price = yearlyPrice,
                            selected = yearly,
                            badge = sh("EN AVANTAJLI", "BEST VALUE"),
                            modifier = Modifier.weight(1f),
                        ) { yearly = true }
                    }

                    Button(
                        onClick = {
                            if (activity == null) {
                                notice = "Google Play ödeme ekranı bu cihazda açılamadı."
                                return@Button
                            }
                            val product = selectedProduct
                            if (product == null) {
                                notice = if (connected) {
                                    "Seçilen VIP ürünü Google Play hesabında yayımlı değil veya bu kullanıcı için kullanılamıyor."
                                } else {
                                    "Google Play bağlantısı hazırlanıyor."
                                }
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
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VipGold,
                            contentColor = Color(0xFF2C1A00),
                            disabledContainerColor = VipGold.copy(alpha = .45f),
                            disabledContentColor = Color(0xFF2C1A00).copy(alpha = .6f),
                        ),
                    ) {
                        Text(
                            if (busy) sh("DOĞRULANIYOR…", "VERIFYING…")
                            else if (yearly) sh("YILLIK VIP'E GEÇ", "GET YEARLY VIP")
                            else sh("AYLIK VIP'E GEÇ", "GET MONTHLY VIP"),
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                        )
                    }

                    if (notice.isNotBlank()) {
                        Text(
                            notice,
                            modifier = Modifier.fillMaxWidth(),
                            color = VipMuted,
                            fontSize = 9.sp,
                            lineHeight = 12.sp,
                            textAlign = TextAlign.Center,
                        )
                    }

                    Text(
                        sh("Google Play ile güvenli ödeme • İstediğin zaman iptal", "Secure Google Play billing • Cancel anytime"),
                        modifier = Modifier.fillMaxWidth(),
                        color = VipMuted.copy(alpha = .8f),
                        fontSize = 8.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun VipModernBenefit(icon: String, title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(38.dp),
            shape = RoundedCornerShape(12.dp),
            color = VipGold.copy(alpha = .12f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(icon, color = VipGold, fontSize = 17.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = VipText, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = VipMuted, fontSize = 9.sp)
        }
        Text("✓", color = VipGreen, fontSize = 15.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun VipModernPlan(
    title: String,
    price: String,
    selected: Boolean,
    badge: String?,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) VipGold.copy(alpha = .14f) else VipNavy2,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) VipGold else Color.White.copy(alpha = .08f)),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            if (badge != null) {
                Text(badge, color = VipGold, fontSize = 6.sp, fontWeight = FontWeight.Black)
            } else {
                Spacer(Modifier.height(7.dp))
            }
            Text(title, color = if (selected) VipGold else VipText, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Text(price, color = VipMuted, fontSize = 8.sp, textAlign = TextAlign.Center)
        }
    }
}

private fun subscriptionPrice(details: ProductDetails?): String? =
    details?.subscriptionOfferDetails
        ?.firstOrNull()
        ?.pricingPhases
        ?.pricingPhaseList
        ?.lastOrNull()
        ?.formattedPrice
