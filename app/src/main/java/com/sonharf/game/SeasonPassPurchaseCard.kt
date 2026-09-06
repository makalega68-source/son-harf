package com.sonharf.game

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Toll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.sonharf.game.billing.BillingManager
import com.sonharf.game.billing.PlayPurchaseVerification
import com.sonharf.game.billing.ProductCatalog
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.StoreSeasonDto
import com.sonharf.game.data.StoreSeasonRewardDto
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.claimStoreSeasonReward
import com.sonharf.game.data.equipShopItem
import com.sonharf.game.data.getStoreSeason
import com.sonharf.game.data.trackStoreEvent
import kotlinx.coroutines.launch

@Composable
fun SeasonPassPurchaseCard(onPurchased: () -> Unit = {}) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val backend = remember { if (SupabaseProvider.configured) runCatching { OnlineGameBackend() }.getOrNull() else null }
    var product by remember { mutableStateOf<ProductDetails?>(null) }
    var season by remember { mutableStateOf<StoreSeasonDto?>(null) }
    var busy by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf("") }

    suspend fun reloadSeason() {
        val b = backend ?: return
        runCatching { b.getStoreSeason() }
            .onSuccess { season = it }
            .onFailure { season = null }
    }

    val manager = remember {
        BillingManager(
            context = context,
            onPurchase = { purchase ->
                val productId = purchase.products.firstOrNull()
                if (productId != ProductCatalog.SEASON_PASS_MONTHLY) {
                    notice = sh("Sezon bileti ürün bilgisi alınamadı.", "Season pass product information is missing.")
                    busy = null
                } else {
                    scope.launch {
                        busy = "purchase"
                        runCatching { PlayPurchaseVerification.verify(productId, purchase.purchaseToken) }
                            .onSuccess {
                                notice = sh("Sezon Bileti etkinleştirildi.", "Season Pass activated.")
                                reloadSeason()
                                onPurchased()
                            }
                            .onFailure { error ->
                                notice = when {
                                    "google_play_not_configured" in error.message.orEmpty() ->
                                        sh("Google Play sunucu doğrulaması production hesabıyla yapılandırılmalı.", "Google Play server verification must be configured with the production account.")
                                    else -> sh("Sezon Bileti doğrulanamadı. Tekrar deneyebilirsin.", "Season Pass verification failed. You can retry.")
                                }
                            }
                        busy = null
                    }
                }
            },
            onMessage = { message -> notice = message; busy = null },
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

    LaunchedEffect(backend) { reloadSeason() }

    Card(
        colors = CardDefaults.cardColors(containerColor = SonHarfSurface.copy(alpha = .96f)),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.2.dp, SonHarfPurple.copy(alpha = .55f)),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(sh("SEZON BİLETİ", "SEASON PASS"), color = SonHarfPurple, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text(
                        sh("FREE + PREMIUM ödül yolu • Style • unvan • Son Coin", "FREE + PREMIUM reward track • Style • titles • Son Coins"),
                        color = SonHarfMuted,
                        fontSize = 9.sp,
                    )
                }
                Icon(Icons.Rounded.EmojiEvents, sh("Sezon ödülleri", "Season rewards"), tint = SonHarfGold, modifier = Modifier.size(30.dp))
            }

            Text(
                sh(
                    "Sezon Bileti yalnızca koleksiyon, görünüm ve ilerleme ödülleri verir; rating, süre, joker gücü veya maç avantajı vermez.",
                    "Season Pass only grants collection, appearance and progression rewards; it never grants rating, time, joker power or match advantages.",
                ),
                color = SonHarfGreen,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            )

            season?.takeIf { it.active }?.let { activeSeason ->
                SeasonProgressHeader(activeSeason)
                activeSeason.rewards.take(14).forEach { reward ->
                    SeasonRewardRow(
                        reward = reward,
                        busy = busy == seasonRewardBusyKey(reward),
                        onAction = {
                            val b = backend ?: return@SeasonRewardRow
                            scope.launch {
                                busy = seasonRewardBusyKey(reward)
                                if (!reward.claimed) {
                                    runCatching { b.claimStoreSeasonReward(reward) }
                                        .onSuccess {
                                            notice = sh("Sezon ödülü alındı.", "Season reward claimed.")
                                            runCatching { b.trackStoreEvent("season_upgrade", reward.rewardKey.ifBlank { reward.rewardType }) }
                                            reloadSeason()
                                            onPurchased()
                                        }
                                        .onFailure { error ->
                                            notice = when {
                                                "already_claimed" in error.message.orEmpty() -> sh("Bu ödül daha önce alındı.", "This reward was already claimed.")
                                                "season_pass_required" in error.message.orEmpty() -> sh("Bu ödül Premium yoluna ait.", "This reward belongs to the Premium track.")
                                                "reward_locked" in error.message.orEmpty() -> sh("Bu seviye henüz açılmadı.", "This level is not unlocked yet.")
                                                else -> sh("Sezon ödülü alınamadı.", "Season reward could not be claimed.")
                                            }
                                            reloadSeason()
                                        }
                                } else if (reward.rewardType != "son_coin" && reward.rewardKey.isNotBlank()) {
                                    runCatching { b.equipShopItem(reward.rewardKey) }
                                        .onSuccess { notice = sh("Sezon Style ödülü kullanılıyor.", "Season Style reward equipped.") }
                                        .onFailure { notice = sh("Style ödülü şu anda kullanılamadı.", "Style reward could not be equipped right now.") }
                                }
                                busy = null
                            }
                        },
                    )
                }
            } ?: Text(
                sh("Sezon ödül yolu sunucudan hazırlanıyor.", "The season reward track is being prepared by the server."),
                color = SonHarfMuted,
                fontSize = 9.sp,
            )

            val premiumActive = season?.premiumActive == true
            if (!premiumActive) {
                Button(
                    onClick = {
                        val details = product
                        if (activity == null || details == null) {
                            notice = sh("Sezon Bileti Google Play'de henüz kullanılabilir değil.", "Season Pass is not available on Google Play yet.")
                            return@Button
                        }
                        busy = "purchase"
                        val result = manager.launchProduct(activity, details)
                        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                            busy = null
                            notice = sh("Google Play ödeme ekranı açılamadı.", "Google Play billing could not open.")
                        }
                    },
                    enabled = busy == null,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SonHarfPurple),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        if (busy == "purchase") "…" else seasonPassPrice(product) ?: sh("PLAY'DE GÖR", "VIEW ON PLAY"),
                        fontWeight = FontWeight.Black,
                    )
                }
            } else {
                Surface(color = SonHarfGreen.copy(alpha = .10f), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = SonHarfGreen)
                        Spacer(Modifier.width(8.dp))
                        Text(sh("PREMIUM YOL AKTİF", "PREMIUM TRACK ACTIVE"), color = SonHarfGreen, fontWeight = FontWeight.Black, fontSize = 10.sp)
                    }
                }
            }

            if (notice.isNotBlank()) Text(notice, color = SonHarfMuted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun SeasonProgressHeader(season: StoreSeasonDto) {
    Surface(color = SonHarfPurple.copy(alpha = .08f), shape = RoundedCornerShape(15.dp)) {
        Column(Modifier.fillMaxWidth().padding(11.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${sh("SEVİYE", "LEVEL")} ${season.level}", color = SonHarfPurple, fontWeight = FontWeight.Black, fontSize = 11.sp)
                Text("${season.durationDays} ${sh("günlük sezon", "day season")}", color = SonHarfMuted, fontSize = 9.sp)
            }
            Text(
                sh("FREE ödüller herkese açık; PREMIUM ödüller Sezon Bileti ile açılır.", "FREE rewards are available to everyone; PREMIUM rewards unlock with Season Pass."),
                color = SonHarfMuted,
                fontSize = 8.5.sp,
            )
        }
    }
}

@Composable
private fun SeasonRewardRow(reward: StoreSeasonRewardDto, busy: Boolean, onAction: () -> Unit) {
    val isPremium = reward.track == "premium"
    val hasAccess = !isPremium || reward.premiumAccess
    val actionable = reward.unlocked && hasAccess && (!reward.claimed || reward.rewardType != "son_coin")
    Surface(
        color = if (isPremium) SonHarfGold.copy(alpha = .07f) else SonHarfSurface2,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (isPremium) SonHarfGold.copy(alpha = .26f) else SonHarfTheme.Border),
    ) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (!reward.unlocked || !hasAccess) Icons.Rounded.Lock else if (reward.rewardType == "son_coin") Icons.Rounded.Toll else Icons.Rounded.Palette,
                contentDescription = null,
                tint = if (isPremium) SonHarfGold else SonHarfTheme.PrimaryBlue,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${sh("SV.", "LV.")} ${reward.level} • ${reward.track.uppercase()}",
                    color = SonHarfTheme.TextPrimary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(seasonRewardLabel(reward), color = SonHarfTheme.TextSecondary, fontSize = 8.5.sp)
            }
            when {
                reward.claimed && reward.rewardType == "son_coin" -> Icon(Icons.Rounded.CheckCircle, null, tint = SonHarfGreen, modifier = Modifier.size(20.dp))
                actionable -> TextButton(onClick = onAction, enabled = !busy) {
                    Text(
                        if (busy) "…" else if (reward.claimed) sh("KULLAN", "EQUIP") else sh("AL", "CLAIM"),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                else -> Text(if (!reward.unlocked) sh("KİLİTLİ", "LOCKED") else sh("PREMIUM", "PREMIUM"), color = SonHarfMuted, fontSize = 7.5.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

private fun seasonRewardBusyKey(reward: StoreSeasonRewardDto): String =
    "${reward.level}:${reward.track}:${reward.rewardType}:${reward.rewardKey}"

private fun seasonRewardLabel(reward: StoreSeasonRewardDto): String = when (reward.rewardType) {
    "son_coin" -> "+${reward.amount} Son Coin"
    "profile_frame" -> sh("Profil çerçevesi", "Profile frame")
    "badge" -> sh("Sezon rozeti", "Season badge")
    "title" -> sh("Sezon unvanı", "Season title")
    "nameplate" -> sh("Profil nameplate", "Profile nameplate")
    "victory_effect" -> sh("Zafer efekti", "Victory effect")
    "word_effect" -> sh("Kelime efekti", "Word effect")
    "vs_intro" -> sh("VS intro", "VS intro")
    "final_style" -> sh("Final Style", "Final Style")
    else -> sh("Style ödülü", "Style reward")
}

private fun seasonPassPrice(details: ProductDetails?): String? =
    details?.subscriptionOfferDetails
        ?.firstOrNull()
        ?.pricingPhases
        ?.pricingPhaseList
        ?.lastOrNull()
        ?.formattedPrice
