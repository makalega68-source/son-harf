package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.StoreBundleDto
import com.sonharf.game.data.StorefrontDto

@Composable
internal fun StoreDailyRewardCard(state: StorefrontDto?, busy: Boolean, onClaim: () -> Unit) {
    Surface(color = SonHarfTheme.PrimarySoft, shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CardGiftcard, null, Modifier.size(26.dp), tint = SonHarfTheme.Primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(sh("Günlük hediyen", "Your daily gift"), fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 19.sp)
                Text(state?.let { "${it.dailyReward} Son Coin" } ?: sh("Yükleniyor…", "Loading…"), color = SonHarfMuted, fontSize = 12.sp, lineHeight = 17.sp)
            }
            TextButton(onClick = onClaim, enabled = state != null && !state.dailyClaimed && !busy) {
                Text(if (state?.dailyClaimed == true) sh("Alındı", "Claimed") else sh("Ücretsiz al", "Claim free"))
            }
        }
    }
}
@Composable
internal fun StorePromoCard(title: String, subtitle: String, action: String, onClick: () -> Unit) {
    OutlinedCard(shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, SonHarfTheme.Border)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = SonHarfMuted, fontSize = 12.sp, lineHeight = 17.sp)
            }
            TextButton(onClick = onClick) { Text(action) }
        }
    }
}
@Composable
internal fun StoreBundleCard(bundle: StoreBundleDto, ownedItems: Set<String>, busy: Boolean, onBuy: () -> Unit) {
    val complete = bundle.owned || bundle.items.all { it.id in ownedItems }
    OutlinedCard(shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, SonHarfTheme.Border)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(sh(bundle.nameTr, bundle.nameEn), fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
                    bundle.availableUntil?.take(10)?.let { end ->
                        Text(sh("$end tarihine kadar", "Available until $end"), color = SonHarfMuted, fontSize = 11.sp, lineHeight = 15.sp)
                    }
                }
                Text("${bundle.diamondPrice} SC", color = SonHarfTheme.Primary, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                bundle.items.forEach { product ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        StoreProductPreview(product, Modifier.fillMaxWidth().height(90.dp))
                        Text(sh(product.nameTr, product.nameEn), color = SonHarfText, fontSize = 11.sp, lineHeight = 15.sp)
                    }
                }
            }
            OutlinedButton(onClick = onBuy, enabled = !busy && !complete, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Text(if (complete) sh("Koleksiyonunda", "In your collection") else sh("Paketi incele", "View bundle"))
            }
        }
    }
}
@Composable
internal fun StoreProBenefits() {
    val uri = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(8.dp)) {
        listOf(
            sh("Zorunlu reklamsız kullanım", "No mandatory ads"),
            sh("PRO rozeti ve özel profil görünümleri", "PRO badge and exclusive profile styles"),
            sh("Gelişmiş maç analizi", "Advanced match analysis"),
            sh("Özel odalar ve kayıtlı arkadaş listesi", "Private rooms and saved friends"),
        ).forEach { benefit ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Check, null, Modifier.size(18.dp), tint = SonHarfTheme.Primary)
                Spacer(Modifier.width(10.dp))
                Text(benefit, fontSize = 13.sp, lineHeight = 18.sp, color = SonHarfText)
            }
        }
        TextButton(onClick = { uri.openUri("https://play.google.com/store/account/subscriptions?package=${BuildConfig.APPLICATION_ID}") }) {
            Text(sh("Aboneliği yönet", "Manage subscription"))
        }
    }
}
