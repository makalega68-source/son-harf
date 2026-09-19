package com.sonharf.game

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/**
 * Purchase surface for permanent premium tools.
 *
 * The UI never grants access locally. A completed Google Play purchase is first verified by the
 * server, then [onVerified] lets the caller re-read the authoritative entitlement RPC.
 */
@Composable
internal fun PremiumProductPurchaseDialog(
    productId: String,
    onVerified: () -> Unit,
    onDismiss: () -> Unit,
) {
    require(productId == ProductCatalog.LETTER_TABLE || productId == ProductCatalog.SCORE_CALCULATOR)

    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    var product by remember(productId) { mutableStateOf<ProductDetails?>(null) }
    var connected by remember(productId) { mutableStateOf(false) }
    var busy by remember(productId) { mutableStateOf(false) }
    var notice by remember(productId) { mutableStateOf("") }

    val manager = remember(productId) {
        BillingManager(
            context = context,
            onPurchase = { purchase ->
                if (purchase.products.contains(productId)) {
                    scope.launch {
                        busy = true
                        runCatching { PlayPurchaseVerification.verify(productId, purchase.purchaseToken) }
                            .onSuccess { onVerified() }
                            .onFailure {
                                notice = sh(
                                    "Ödeme alındı ancak sunucu doğrulaması tamamlanamadı. Yeniden doğrulama ikinci kez ücretlendirmez.",
                                    "Payment was received but server verification is pending. Re-verification will not charge twice.",
                                )
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

    DisposableEffect(manager, productId) {
        manager.connect {
            connected = true
            manager.queryOneTimeProducts(listOf(productId)) { details -> product = details[productId] }
            // Re-deliver a completed permanent purchase after an interrupted server verification.
            manager.restorePurchases(setOf(productId))
        }
        onDispose { manager.close() }
    }

    val isLetterTable = productId == ProductCatalog.LETTER_TABLE
    val title = if (isLetterTable) sh("Harf Tablosu", "Letter Table") else sh("Puan Hesaplayıcı", "Score Calculator")
    val description = if (isLetterTable) {
        sh(
            "Kelime Kuşatması sırasında kalan harf adetlerini server doğrulamasıyla gösterir. Tek ödeme ile kalıcı erişim.",
            "Shows remaining letter counts during Word Siege with server validation. One payment for permanent access.",
        )
    } else {
        sh(
            "Hamleni göndermeden önce kelime ve bölge puanı ön izlemesini server doğrulamasıyla gösterir. Tek ödeme ile kalıcı erişim.",
            "Shows a server-validated word and territory score preview before submitting your move. One payment for permanent access.",
        )
    }
    val price = product?.oneTimePurchaseOfferDetails?.formattedPrice
        ?: if (isLetterTable) ProductCatalog.LETTER_TABLE_FALLBACK_PRICE_TRY else ProductCatalog.SCORE_CALCULATOR_FALLBACK_PRICE_TRY

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        icon = {
            Icon(
                if (isLetterTable) Icons.Rounded.GridView else Icons.Rounded.Calculate,
                contentDescription = null,
                tint = WordSiegeGameUi.Blue,
            )
        },
        title = { Text(title, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(description, fontSize = 11.sp, color = WordSiegeGameUi.Muted)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(sh("Kalıcı lisans", "Permanent license"), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text(price, color = WordSiegeGameUi.Blue, fontWeight = FontWeight.Black)
                }
                if (notice.isNotBlank()) {
                    Text(notice, fontSize = 9.sp, color = WordSiegeGameUi.Muted)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val selected = product
                    if (activity == null) {
                        notice = sh("Google Play ödeme ekranı açılamadı.", "Google Play billing could not be opened.")
                        return@Button
                    }
                    if (selected?.oneTimePurchaseOfferDetails == null) {
                        notice = if (connected) {
                            sh("Ürün bu hesap için henüz Google Play'de satın alınabilir değil.", "This product is not yet purchasable on Google Play for this account.")
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
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(if (busy) sh("DOĞRULANIYOR…", "VERIFYING…") else sh("SATIN AL", "BUY"), fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text(sh("İPTAL", "CANCEL"), fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(22.dp),
        containerColor = SonHarfTheme.Surface,
        tonalElevation = 0.dp,
    )
}
