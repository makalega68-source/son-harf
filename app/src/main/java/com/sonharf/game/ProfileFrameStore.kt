package com.sonharf.game

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.sonharf.game.billing.BillingManager
import com.sonharf.game.billing.PlayPurchaseVerification
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ShopItemDto
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.getEquippedCosmetics
import com.sonharf.game.data.getInventory
import com.sonharf.game.data.getShopItems
import com.sonharf.game.data.purchaseShopItem
import kotlinx.coroutines.launch

/**
 * Store tab for profile frames: simple rings for Son Coin, ornate crests as permanent Google Play
 * products. Buying equips the frame right away; any owned frame can be re-equipped here or in Profile.
 */
@Composable
internal fun ProfileFrameStoreSection(onBalance: (Int?) -> Unit, onPro: () -> Unit = {}) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var coinItems by remember { mutableStateOf<Map<String, ShopItemDto>>(emptyMap()) }
    var owned by remember { mutableStateOf<Set<String>>(emptySet()) }
    var equipped by remember { mutableStateOf(SonHarfCosmetics.profileFrameId) }
    var products by remember { mutableStateOf<Map<String, ProductDetails>>(emptyMap()) }
    var busy by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf("") }

    suspend fun reload() {
        val b = backend ?: return
        runCatching {
            coinItems = b.getShopItems().filter { it.kind == "profile_frame" }.associateBy { it.id }
            owned = b.getInventory()
            val e = b.getEquippedCosmetics()
            SonHarfCosmetics.applyAndPersist(context, e)
            equipped = e?.profileFrameId
            b.currentUserId()?.let { id -> onBalance(runCatching { b.getProfile(id).diamonds }.getOrNull()) }
        }.onFailure { notice = sh("Çerçeveler yüklenemedi.", "Frames could not be loaded.") }
    }

    val manager = remember {
        BillingManager(
            context = context,
            onPurchase = { purchase ->
                val productId = purchase.products.firstOrNull()
                if (productId != null && productId in ProfileFrameCollection.premiumProductIds && productId !in owned) {
                    scope.launch {
                        busy = productId
                        runCatching { PlayPurchaseVerification.verify(productId, purchase.purchaseToken) }
                            .onSuccess {
                                reload()
                                notice = sh("Çerçeve satın alındı! Profil > Çerçeve'den takabilirsin.", "Frame purchased! Wear it from Profile > Frame.")
                            }
                            .onFailure { error ->
                                notice = when {
                                    "google_play_not_configured" in error.message.orEmpty() ->
                                        sh("Google Play sunucu doğrulaması henüz etkin değil.", "Google Play server verification is not enabled yet.")
                                    "product_disabled" in error.message.orEmpty() ->
                                        sh("Bu çerçeve henüz satışa açılmadı.", "This frame is not on sale yet.")
                                    else -> sh("Ödeme doğrulaması tamamlanamadı; yeniden deneyebilirsin.", "Purchase verification failed; you can retry.")
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
            manager.queryOneTimeProducts(ProfileFrameCollection.premiumProductIds) { products = it }
            manager.restorePurchases(ProfileFrameCollection.premiumProductIds.toSet())
        }
        onDispose { manager.close() }
    }
    LaunchedEffect(Unit) { reload() }

    fun onFrame(frame: ProfileFrameCollection.Frame) {
        if (busy != null) return
        val b = backend ?: return
        when {
            // Owned frames are worn, changed and removed from the profile.
            frame.id in owned -> notice = sh("Bu çerçeve sende var. Profil > Çerçeve'den takabilirsin.", "You own this frame. Wear it from Profile > Frame.")
            // The PRO frame comes with PRO membership; the button leads to PRO.
            frame == ProfileFrameCollection.proFrame -> onPro()
            frame.playProductId != null -> {
                val product = products[frame.playProductId]
                if (activity == null || product?.oneTimePurchaseOfferDetails == null) {
                    notice = sh("Bu çerçeve Google Play'de henüz satışta değil.", "This frame is not on Google Play yet.")
                    return
                }
                busy = frame.id
                val result = manager.launchProduct(activity, product)
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    busy = null
                    notice = sh("Google Play ödeme ekranı açılamadı (${result.responseCode}).", "Google Play billing could not open (${result.responseCode}).")
                }
            }
            else -> scope.launch {
                busy = frame.id
                runCatching { b.purchaseShopItem(frame.id) }
                    .onSuccess {
                        reload()
                        notice = sh("Çerçeve satın alındı! Profil > Çerçeve'den takabilirsin.", "Frame purchased! Wear it from Profile > Frame.")
                    }
                    .onFailure {
                        notice = if ("insufficient_diamonds" in it.message.orEmpty()) sh("Yeterli Son Coin'in yok.", "Not enough Son Coin.")
                            else sh("Satın alma tamamlanamadı.", "Purchase failed.")
                    }
                busy = null
            }
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (notice.isNotBlank()) {
            Text(notice, color = Hf.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
        FrameSectionTitle(sh("PRO ÜYELERE ÖZEL", "PRO MEMBERS ONLY"), sh("PRO olunca otomatik takılır", "Equipped automatically when you go PRO"))
        FrameGrid(listOf(ProfileFrameCollection.proFrame)) { frame ->
            FrameCard(frame, owned = frame.id in owned, equipped = equipped == frame.id, busy = busy != null,
                price = sh("PRO ol", "Go PRO"), premium = true) { onFrame(frame) }
        }
        FrameSectionTitle(sh("PREMIUM ÇERÇEVELER", "PREMIUM FRAMES"), sh("Kalıcı • Google Play ile", "Permanent • via Google Play"))
        FrameGrid(ProfileFrameCollection.premiumFrames) { frame ->
            val offer = frame.playProductId?.let { products[it]?.oneTimePurchaseOfferDetails }
            FrameCard(frame, owned = frame.id in owned, equipped = equipped == frame.id, busy = busy != null,
                price = offer?.formattedPrice ?: sh("Yakında", "Soon"), premium = true) { onFrame(frame) }
        }
        FrameSectionTitle(sh("SON COIN ÇERÇEVELERİ", "SON COIN FRAMES"), sh("Sade halkalar • Son Coin ile", "Simple rings • for Son Coin"))
        FrameGrid(ProfileFrameCollection.coinFrames.filter { it.id in coinItems || it.id in owned }) { frame ->
            FrameCard(frame, owned = frame.id in owned, equipped = equipped == frame.id, busy = busy != null,
                price = coinItems[frame.id]?.let { "${it.diamondPrice} SC" } ?: "—", premium = false) { onFrame(frame) }
        }
        Text(
            sh("Çerçeveler yalnızca görünümdür; oyun gücü vermez. Çerçevesiz görünüme Profil > Çerçeve'den dönebilirsin.",
                "Frames are purely cosmetic. Go back to no frame from Profile > Frame."),
            color = Hf.TextMuted, fontSize = 11.sp, lineHeight = 15.sp,
        )
    }
}

@Composable
private fun FrameSectionTitle(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text(title, color = Hf.Gold, fontSize = 15.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Text(subtitle, color = Hf.TextMuted, fontSize = 12.sp)
    }
}

@Composable
private fun FrameGrid(frames: List<ProfileFrameCollection.Frame>, card: @Composable (ProfileFrameCollection.Frame) -> Unit) {
    frames.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { frame -> Box(Modifier.weight(1f)) { card(frame) } }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun FrameCard(
    frame: ProfileFrameCollection.Frame,
    owned: Boolean,
    equipped: Boolean,
    busy: Boolean,
    price: String,
    premium: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = Hf.CardShape,
        color = Hf.Surface,
        border = BorderStroke(if (equipped) 2.dp else 1.dp, if (equipped) Hf.Green else Hf.Gold.copy(alpha = if (premium) .9f else .45f)),
        shadowElevation = 3.dp,
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.fillMaxWidth().height(118.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(70.dp).background(Brush.linearGradient(listOf(Hf.Ground, Hf.Surface)), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Person, null, tint = Hf.TextMuted, modifier = Modifier.size(38.dp)) }
                ProfileFrameArt(frame.id, 70.dp)
            }
            Text(sh(frame.nameTr, frame.nameEn), color = Hf.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Surface(
                onClick = onClick,
                enabled = !busy && !equipped && !owned,
                modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp),
                shape = Hf.PillShape,
                color = when {
                    equipped || owned -> Hf.Green
                    premium -> Hf.Gold
                    else -> Hf.Ivory
                },
            ) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 9.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    if (!owned && !premium) { HfCoin(16.dp); Spacer(Modifier.width(6.dp)) }
                    Text(
                        if (equipped || owned) sh("SATIN ALINDI", "PURCHASED") else price,
                        color = if (equipped || owned) Hf.OnAccent else Hf.Text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}
