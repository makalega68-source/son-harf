package com.sonharf.game

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.sonharf.game.data.*
import kotlinx.coroutines.launch

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun ShopHubScreen() {
    var tab by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text(sh("MAĞAZA", "SHOP")) }, modifier = Modifier.weight(1f))
            FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text(sh("ÖDÜLLER", "REWARDS")) }, modifier = Modifier.weight(1f))
        }
        Box(Modifier.weight(1f)) {
            if (tab == 0) EconomyShopScreen() else RewardCenterScreen()
        }
    }
}

@Composable
fun RewardCenterScreen() {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val adController = remember { RewardedAdController(context) }
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<RewardCenterStatusDto?>(null) }
    var items by remember { mutableStateOf<List<ShopItemDto>>(emptyList()) }
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var adReady by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        val b = backend
        if (b == null) {
            notice = sh("Ödül merkezi sunucu bağlantısı olmadan kullanılamaz.", "Reward Center requires a server connection.")
            return
        }
        if (b.currentUserId() == null) {
            runCatching { b.ensurePlayer(sh("Oyuncu", "Player")) }
                .onFailure {
                    notice = sh("Oyuncu oturumu hazırlanamadı.", "Player session is not ready.")
                    return
                }
        }
        runCatching {
            status = b.getRewardCenterStatus()
            items = b.getShopItems()
            profile = b.currentUserId()?.let { b.getProfile(it) }
        }.onFailure {
            notice = sh("Ödül verileri yüklenemedi.", "Reward data could not be loaded.")
        }
    }

    LaunchedEffect(Unit) {
        runCatching { reload() }
        adController.load { adReady = adController.ready }
    }

    fun showRewarded(rewardType: String) {
        val a = activity
        val b = backend
        if (a == null || busy != null) return
        if (b == null) {
            notice = sh("Ödül merkezi şu anda çevrimdışı.", "Reward Center is currently offline.")
            return
        }
        busy = rewardType
        adController.show(
            a,
            onEarned = { responseId ->
                scope.launch {
                    runCatching { b.claimRewardedAd(rewardType, responseId) }
                        .onSuccess { claim ->
                            notice = when (rewardType) {
                                "diamonds" -> sh("+${claim?.diamondsAwarded ?: 10} Son Coin hesabına eklendi.", "+${claim?.diamondsAwarded ?: 10} Son Coin added.")
                                "chest" -> sh("1 ödül sandığı kazandın.", "You earned 1 reward chest.")
                                else -> sh("24 saatlik premium kozmetik denemen başladı.", "Your 24-hour premium cosmetic trial has started.")
                            }
                            reload()
                        }
                        .onFailure { e ->
                            notice = if ("daily_limit_reached" in e.message.orEmpty()) sh("Bugünkü kota tamamlandı.", "Today's quota is complete.") else sh("Ödül işlenemedi.", "Reward could not be processed.")
                        }
                    busy = null
                    adReady = adController.ready
                }
            },
            onUnavailable = {
                notice = sh("Reklam şu an hazır değil. Biraz sonra tekrar dene.", "The ad is not ready yet. Try again shortly.")
                busy = null
                adReady = false
            },
            onClosed = { adController.load { adReady = adController.ready } },
        )
    }

    val s = status
    val trialItem = items.firstOrNull { it.id == s?.trialItemId }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(sh("SON HARF ÖDÜLLERİ", "SON HARF REWARDS"), fontSize = 27.sp, fontWeight = FontWeight.Black)
            Text(sh("Ödüllü reklamlar isteğe bağlıdır. İnce banner yalnızca oyun dışı menülerde gösterilir; maçlarda ve oyun alanlarında reklam yoktur.", "Rewarded ads are optional. A thin banner appears only on non-game menus; matches and gameplay areas remain ad-free."), color = SonHarfMuted, fontSize = 10.sp)
        }

        item {
            RewardAdCard(
                icon = "◈", title = sh("SON COIN", "DIAMONDS"),
                description = sh("Her tamamlanan reklam +10 Son Coin verir. Son Coinları Mağaza'daki kozmetiklerde kullan.", "Each completed ad gives +10 diamonds. Spend Son Coin on Style items in the Shop."),
                progress = "${s?.diamondAdsUsed ?: 0}/${s?.diamondAdsLimit ?: 3}",
                button = sh("REKLAM İZLE  +10", "WATCH AD  +10"),
                enabled = adReady && (s?.diamondAdsUsed ?: 0) < (s?.diamondAdsLimit ?: 3) && busy == null,
                onClick = { showRewarded("diamonds") },
            )
        }

        item {
            RewardAdCard(
                icon = "🎁", title = sh("ÖDÜL SANDIĞI", "REWARD CHEST"),
                description = sh("Reklam başına 1 sandık hakkı. Sandık açıldığında 15, 25 veya 40 Son Coin çıkar.", "Earn 1 chest per ad. Opening a chest awards 15, 25, or 40 diamonds."),
                progress = "${s?.chestAdsUsed ?: 0}/${s?.chestAdsLimit ?: 2}",
                button = sh("REKLAM İZLE  +1 SANDIK", "WATCH AD  +1 CHEST"),
                enabled = adReady && (s?.chestAdsUsed ?: 0) < (s?.chestAdsLimit ?: 2) && busy == null,
                onClick = { showRewarded("chest") },
            )
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, SonHarfGold.copy(alpha = .30f))) {
                Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(sh("ÖDÜL SANDIKLARIM", "MY REWARD CHESTS"), color = LetharaPalette.Gold, fontWeight = FontWeight.Black)
                        Text("🎁 ${s?.chestKeys ?: 0}", fontWeight = FontWeight.Black)
                    }
                    Text(sh("Topladığın ödül sandıklarını aç. Çıkan Son Coin doğrudan cüzdanına eklenir ve yalnızca güç vermeyen içeriklerde kullanılır.", "Open collected reward chests. Son Coin goes directly to your wallet and is used only for non-power content."), color = SonHarfMuted, fontSize = 9.sp)
                    Button(
                        onClick = {
                            val b = backend
                            if (b == null) {
                                notice = sh("Ödül merkezi şu anda çevrimdışı.", "Reward Center is currently offline.")
                                return@Button
                            }
                            scope.launch {
                                busy = "open_chest"
                                runCatching { b.openRewardChest() }
                                    .onSuccess { reward -> notice = sh("Sandıktan ${reward?.diamondsAwarded ?: 0} Son Coin çıktı!", "Chest awarded ${reward?.diamondsAwarded ?: 0} diamonds!"); reload() }
                                    .onFailure { notice = sh("Açılacak sandığın yok.", "You do not have a chest to open.") }
                                busy = null
                            }
                        },
                        enabled = (s?.chestKeys ?: 0) > 0 && busy == null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SonHarfGold, contentColor = Color(0xFF211830)),
                    ) { Text(sh("SANDIĞI AÇ", "OPEN CHEST"), fontWeight = FontWeight.Black) }
                }
            }
        }

        item {
            RewardAdCard(
                icon = "✨", title = sh("PREMIUM DENEME", "PREMIUM TRIAL"),
                description = sh("Günde 1 reklamla rastgele bir VIP kozmetiğini 24 saat deneyebilirsin.", "Watch 1 ad per day to try a random VIP cosmetic for 24 hours."),
                progress = "${s?.trialAdsUsed ?: 0}/${s?.trialAdsLimit ?: 1}",
                button = sh("24 SAAT DENEME", "24-HOUR TRIAL"),
                enabled = adReady && (s?.trialAdsUsed ?: 0) < (s?.trialAdsLimit ?: 1) && busy == null,
                onClick = { showRewarded("trial") },
            )
        }

        if (s?.trialItemId != null) item {
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfPurple.copy(alpha = .12f)), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, SonHarfPurple.copy(alpha = .45f))) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(sh("AKTİF DENEME", "ACTIVE TRIAL"), color = SonHarfPurple, fontWeight = FontWeight.Black)
                    Text(if (SonHarfUiState.isEnglish) trialItem?.nameEn ?: s.trialItemId else trialItem?.nameTr ?: s.trialItemId, fontWeight = FontWeight.Bold)
                    Text(sh("Bu premium kozmetiği 24 saat boyunca kullanabilirsin. Süre dolunca, ürünü satın almadıysan otomatik olarak çıkarılır.", "You can use this premium cosmetic for 24 hours. When the trial expires, it is automatically unequipped unless you own it."), color = SonHarfMuted, fontSize = 9.sp)
                    Text(s.trialExpiresAt.orEmpty(), color = SonHarfMuted, fontSize = 8.sp)
                    Button(
                        onClick = {
                            val b = backend
                            if (b == null) {
                                notice = sh("Ödül merkezi şu anda çevrimdışı.", "Reward Center is currently offline.")
                                return@Button
                            }
                            scope.launch {
                                busy = "equip_trial"
                                runCatching { b.equipRewardTrial() }
                                    .onSuccess { notice = sh("Deneme kozmetiği etkinleştirildi.", "Trial cosmetic equipped."); reload() }
                                    .onFailure { notice = sh("Deneme artık aktif değil.", "The trial is no longer active."); reload() }
                                busy = null
                            }
                        },
                        enabled = busy == null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SonHarfPurple),
                    ) { Text(if (busy == "equip_trial") "…" else sh("DENEMEYİ KULLAN", "USE TRIAL"), fontWeight = FontWeight.Black) }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface2), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(sh("GÜNLÜK YENİLENME", "DAILY RESET"), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    Text(sh("Kotalar her gün UTC gün değişiminde sunucuda yenilenir. Cihaz saatini değiştirmek veya uygulamayı silmek kotayı sıfırlamaz.", "Quotas reset on the server each UTC day. Changing device time or reinstalling the app does not reset them."), color = SonHarfMuted, fontSize = 9.sp)
                    Text("◈ ${profile?.diamonds ?: 0}", color = SonHarfCyan, fontWeight = FontWeight.Black)
                }
            }
        }

        if (!notice.isNullOrBlank()) item {
            Surface(color = SonHarfSurface2, shape = RoundedCornerShape(14.dp)) {
                Text(notice!!, Modifier.fillMaxWidth().padding(12.dp), color = SonHarfMuted, textAlign = TextAlign.Center, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun RewardAdCard(
    icon: String,
    title: String,
    description: String,
    progress: String,
    button: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, SonHarfMuted.copy(alpha = .14f))) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(icon, fontSize = 24.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(title, fontWeight = FontWeight.Black)
                }
                Text(progress, color = SonHarfCyan, fontWeight = FontWeight.Black)
            }
            Text(description, color = SonHarfMuted, fontSize = 9.sp)
            Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text(button, fontWeight = FontWeight.Black) }
        }
    }
}
