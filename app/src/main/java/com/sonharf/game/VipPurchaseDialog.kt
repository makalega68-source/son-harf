package com.sonharf.game

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

private val VipSurface = Color.White
private val VipSurface2 = Color(0xFFF0F4F8)
private val VipGold = Color(0xFFF3A81A)
private val VipBlue = Color(0xFF1769E0)
private val VipText = Color(0xFF182235)
private val VipMuted = Color(0xFF718096)
private val VipGreen = Color(0xFF22A85A)

private data class VipBenefit(
    val icon: ImageVector,
    val titleTr: String,
    val titleEn: String,
    val bodyTr: String,
    val bodyEn: String,
)

private val primaryVipBenefits = listOf(
    VipBenefit(Icons.Rounded.Group, "Arkadaş Listesi", "Friend List", "Kaydedilebilir arkadaş, favori ve ezeli rakip akışı", "Saved friends, favorites and arch-rival flow"),
    VipBenefit(Icons.Rounded.Calculate, "Otomatik Puan Hesabı", "Automatic Score Breakdown", "Maç sonrası puan kırılımını düzenli ve ayrıntılı gör", "See a clear detailed post-match score breakdown"),
    VipBenefit(Icons.Rounded.ReceiptLong, "Maç Kelimeleri", "Match Words", "Tamamlanan maçların kelime arşivi ve filtreleri", "Word archive and filters for completed matches"),
    VipBenefit(Icons.Rounded.Insights, "Gelişmiş İstatistik", "Advanced Statistics", "Rating grafiği, mod istatistikleri ve karşılaştırmalar", "Rating graph, mode stats and comparisons"),
    VipBenefit(Icons.Rounded.Block, "Reklamsızlık", "Ad-free", "Maç akışına dokunmadan daha temiz deneyim", "A cleaner experience without changing match play"),
    VipBenefit(Icons.Rounded.Palette, "VIP Style", "VIP Style", "Çerçeve, rozet, nameplate, efekt ve prestij görünümü", "Frames, badges, nameplates, effects and prestige looks"),
)

private val moreVipBenefits = listOf(
    VipBenefit(Icons.Rounded.MeetingRoom, "Özel Oda", "Private Room", "Kod ve davetle özel oda oluştur", "Create private rooms with code and invitation"),
    VipBenefit(Icons.Rounded.History, "Uzun Maç Geçmişi", "Extended Match History", "Daha uzun geçmiş, arşiv ve gelişmiş filtreleme", "Longer history, archive and advanced filtering"),
    VipBenefit(Icons.Rounded.PersonSearch, "Rakip Analizi", "Opponent Analysis", "Karşılaşma, seri ve son beş sonuç karşılaştırmaları", "Head-to-head, streak and last-five comparisons"),
    VipBenefit(Icons.Rounded.Analytics, "Maç Sonrası Analiz", "Post-match Analysis", "Hız, kelime, puan ve Kuşatma kırılma noktaları", "Speed, word, score and Siege turning points"),
    VipBenefit(Icons.Rounded.VerifiedUser, "Gizlilik Kontrolü", "Privacy Controls", "Durum ve son görülme yalnız izin verdiğinde paylaşılır", "Status and last seen are shared only with your permission"),
    VipBenefit(Icons.Rounded.AccountBalanceWallet, "Aylık 400 SC", "400 SC Monthly", "Her üyelik ayında koleksiyon için Son Coin başlangıç değeri", "Monthly Son Coin value for your collection"),
)

@Composable
fun VipPurchaseDialog(onVerified: () -> Unit = {}, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    var yearly by remember { mutableStateOf(true) }
    var expanded by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var connected by remember { mutableStateOf(false) }
    var products by remember { mutableStateOf<Map<String, ProductDetails>>(emptyMap()) }

    fun verifyPurchase(productId: String, token: String) {
        scope.launch {
            busy = true
            runCatching { PlayPurchaseVerification.verify(productId, token) }
                .onSuccess {
                    notice = sh("VIP hesabın Google Play ile doğrulandı.", "Your VIP was verified with Google Play.")
                    onVerified()
                }
                .onFailure { error ->
                    notice = when {
                        "google_play_not_configured" in error.message.orEmpty() -> sh(
                            "Sunucu Google Play production doğrulaması için henüz yapılandırılmamış.",
                            "Server-side Google Play production verification is not configured yet.",
                        )
                        else -> sh(
                            "Satın alma doğrulanamadı. Aynı token ikinci kez hak vermez; Geri Yükle ile tekrar deneyebilirsin.",
                            "Purchase verification failed. The same token cannot grant twice; use Restore to retry.",
                        )
                    }
                }
            busy = false
        }
    }

    val manager = remember {
        BillingManager(
            context = context,
            onPurchase = { purchase ->
                val productId = purchase.products.firstOrNull()
                if (productId.isNullOrBlank() || !ProductCatalog.isVip(productId)) {
                    notice = sh("VIP ürün bilgisi alınamadı.", "VIP product information is missing.")
                    busy = false
                } else {
                    verifyPurchase(productId, purchase.purchaseToken)
                }
            },
            onPending = {
                notice = sh(
                    "Ödeme beklemede. VIP yalnız Google Play işlemi tamamlandığında açılır.",
                    "Payment is pending. VIP activates only after Google Play completes it.",
                )
                busy = false
            },
            onMessage = { message -> notice = message; busy = false },
        )
    }

    DisposableEffect(manager) {
        manager.connect {
            connected = true
            manager.querySubscriptions(ProductCatalog.activeSubscriptions) { products = it }
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).widthIn(max = 460.dp),
            shape = RoundedCornerShape(28.dp),
            color = VipSurface,
            border = BorderStroke(1.dp, Color(0xFFDDE5EE)),
            shadowElevation = 12.dp,
        ) {
            Column(Modifier.heightIn(max = 760.dp).verticalScroll(rememberScrollState())) {
                Box(
                    Modifier.fillMaxWidth().background(Color(0xFFEAF3FF)).padding(horizontal = 18.dp, vertical = 16.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(14.dp), color = Color.White) {
                            Icon(
                                Icons.Rounded.WorkspacePremium,
                                contentDescription = null,
                                tint = VipGold,
                                modifier = Modifier.padding(10.dp).size(30.dp),
                            )
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text("SON HARF VIP", color = VipBlue, fontSize = 23.sp, fontWeight = FontWeight.Black)
                            Text(
                                sh("Daha kolay. Daha detaylı. Daha kişisel.", "Easier. More detailed. More personal."),
                                color = VipMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        IconButton(onClick = onDismiss, enabled = !busy, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = sh("Kapat", "Close"), tint = VipText)
                        }
                    }
                }

                Column(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    primaryVipBenefits.forEach { VipBenefitRow(it) }

                    TextButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text(
                            if (expanded) sh("DAHA AZ GÖSTER", "SHOW LESS") else sh("TÜM VIP ÖZELLİKLERİNİ GÖR", "SHOW ALL VIP FEATURES"),
                            color = VipBlue,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    if (expanded) moreVipBenefits.forEach { VipBenefitRow(it) }

                    Surface(shape = RoundedCornerShape(14.dp), color = VipGreen.copy(alpha = .10f)) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.GppGood, contentDescription = null, tint = VipGreen, modifier = Modifier.size(20.dp))
                            Text(
                                sh(
                                    "VIP ranked maçta süre, puan, hamle, rating veya canlı karar avantajı vermez.",
                                    "VIP never grants time, score, moves, rating or live decision advantages in ranked play.",
                                ),
                                color = VipGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Text(sh("Planını seç", "Choose your plan"), color = VipMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        VipPlanCard(sh("AYLIK", "MONTHLY"), monthlyPrice, !yearly, null, Modifier.weight(1f)) { yearly = false }
                        VipPlanCard(sh("YILLIK", "YEARLY"), yearlyPrice, yearly, sh("EN AVANTAJLI", "BEST VALUE"), Modifier.weight(1f)) { yearly = true }
                    }

                    Button(
                        onClick = {
                            if (activity == null) {
                                notice = sh("Google Play ödeme ekranı bu cihazda açılamadı.", "Google Play billing could not open on this device.")
                                return@Button
                            }
                            val product = selectedProduct
                            if (product == null) {
                                notice = if (connected) sh(
                                    "Seçilen VIP ürünü Google Play'de bu hesap için kullanılamıyor.",
                                    "The selected VIP product is unavailable for this Play account.",
                                ) else sh("Google Play bağlantısı hazırlanıyor.", "Preparing Google Play connection.")
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
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VipBlue, contentColor = Color.White),
                    ) {
                        Text(
                            if (busy) sh("DOĞRULANIYOR…", "VERIFYING…")
                            else if (yearly) sh("YILLIK VIP'E GEÇ", "GET YEARLY VIP")
                            else sh("AYLIK VIP'E GEÇ", "GET MONTHLY VIP"),
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            busy = true
                            manager.restorePurchases { count ->
                                if (count == 0) notice = sh("Geri yüklenecek aktif VIP bulunamadı.", "No active VIP purchase was found to restore.")
                                busy = false
                            }
                        },
                        enabled = !busy && connected,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(sh("SATIN ALMALARI GERİ YÜKLE", "RESTORE PURCHASES"), fontWeight = FontWeight.Black)
                    }

                    if (notice.isNotBlank()) {
                        Text(notice, Modifier.fillMaxWidth(), color = VipMuted, fontSize = 9.sp, lineHeight = 12.sp, textAlign = TextAlign.Center)
                    }
                    Text(
                        sh("Google Play ile güvenli ödeme • İstediğin zaman iptal", "Secure Google Play billing • Cancel anytime"),
                        Modifier.fillMaxWidth(), color = VipMuted.copy(alpha = .8f), fontSize = 8.sp, textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun VipBenefitRow(benefit: VipBenefit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(12.dp), color = VipGold.copy(alpha = .12f)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(benefit.icon, contentDescription = null, tint = VipGold, modifier = Modifier.size(21.dp))
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(sh(benefit.titleTr, benefit.titleEn), color = VipText, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text(sh(benefit.bodyTr, benefit.bodyEn), color = VipMuted, fontSize = 9.sp)
        }
        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = VipGreen, modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun VipPlanCard(
    title: String,
    price: String,
    selected: Boolean,
    badge: String?,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick).heightIn(min = 78.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) Color(0xFFEAF3FF) else VipSurface2,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) VipBlue else Color(0xFFDDE5EE)),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            if (badge != null) Text(badge, color = VipBlue, fontSize = 7.sp, fontWeight = FontWeight.Black)
            else Spacer(Modifier.height(9.dp))
            Text(title, color = if (selected) VipBlue else VipText, fontSize = 12.sp, fontWeight = FontWeight.Black)
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
