package com.sonharf.game

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Savings
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
import com.sonharf.game.data.*
import kotlinx.coroutines.launch

private tailrec fun Context.findRewardActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findRewardActivity()
    else -> null
}

@Composable
fun ShopHubScreen() {
    // Compatibility entrypoint. The production category shell lives in EconomyShopScreen.
    EconomyShopScreen()
}

@Composable
fun RewardCenterScreen(showKasaOnly: Boolean = false) {
    val context = LocalContext.current
    val activity = remember(context) { context.findRewardActivity() }
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val adController = remember { RewardedAdController(context) }
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<RewardCenterStatusDto?>(null) }
    var items by remember { mutableStateOf<List<ShopItemDto>>(emptyList()) }
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var adReady by remember { mutableStateOf(false) }
    val adsAllowed = AdPrivacyManager.adsAllowed
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
    }

    LaunchedEffect(adsAllowed, showKasaOnly) {
        if (adsAllowed && !showKasaOnly) {
            adController.load { adReady = adController.ready }
        } else {
            adController.clear()
            adReady = false
        }
    }

    val trialItem = items.firstOrNull { it.active && it.trialMode == "minutes" && (it.trialValue ?: 0) > 0 }
        ?: items.firstOrNull { it.active && it.trialMode == "match" && (it.trialValue ?: 0) > 0 }

    fun showRewarded(rewardType: String) {
        val a = activity
        val b = backend
        if (a == null || busy != null) return
        if (b == null) {
            notice = sh("Ödül merkezi şu anda çevrimdışı.", "Reward Center is currently offline.")
            return
        }
        if (rewardType == "trial" && trialItem == null) {
            notice = sh("Şu anda güvenli denemeye açık Style ürünü yok.", "No Style item is currently enabled for a secure trial.")
            return
        }
        busy = rewardType
        adController.show(
            a,
            onEarned = { responseId ->
                scope.launch {
                    runCatching { b.claimRewardedAd(rewardType, responseId, trialItem?.id) }
                        .onSuccess { claim ->
                            notice = when (rewardType) {
                                "diamonds" -> sh(
                                    "+${claim.diamondsAwarded.takeIf { it > 0 } ?: 10} Son Coin hesabına eklendi.",
                                    "+${claim.diamondsAwarded.takeIf { it > 0 } ?: 10} Son Coin added.",
                                )
                                else -> if (claim.trialMode == "match") {
                                    sh("1 maçlık Style denemen başladı.", "Your 1-match Style trial started.")
                                } else {
                                    sh("30 dakikalık Style denemen başladı.", "Your 30-minute Style trial started.")
                                }
                            }
                            runCatching { b.trackStoreEvent("rewarded_ad_complete", trialItem?.id) }
                            reload()
                        }
                        .onFailure { e ->
                            notice = when {
                                "daily_limit_reached" in e.message.orEmpty() -> sh("Bugünkü kota tamamlandı.", "Today's quota is complete.")
                                "ad_already_claimed" in e.message.orEmpty() -> sh("Bu reklam ödülü daha önce işlendi.", "This ad reward was already processed.")
                                else -> sh("Ödül işlenemedi.", "Reward could not be processed.")
                            }
                        }
                    busy = null
                    adReady = adController.ready
                }
            },
            onUnavailable = {
                notice = sh("Reklam şu an hazır değil. Daha sonra tekrar dene.", "The ad is not ready. Try again later.")
                busy = null
                adReady = false
            },
            onClosed = { adController.load { adReady = adController.ready } },
        )
    }

    val s = status
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(if (showKasaOnly) sh("KASA", "PIGGY BANK") else sh("SON HARF ÖDÜLLERİ", "SON HARF REWARDS"), fontSize = 27.sp, fontWeight = FontWeight.Black)
            Text(
                if (showKasaOnly) sh(
                    "Kasa normal maç ödüllerini azaltmaz. Tamamlanan maçlarla dolar ve bonusu her zaman önceden bellidir.",
                    "The Piggy Bank never reduces normal match rewards. Completed matches fill it and its bonus is always known in advance.",
                ) else sh(
                    "Ödüllü reklamlar tamamen isteğe bağlıdır. Maç sırasında reklam yoktur ve hiçbir reklam süre, hamle, skor veya rating avantajı vermez.",
                    "Rewarded ads are fully optional. No ads appear during matches and ads never grant time, moves, score or rating advantages.",
                ),
                color = SonHarfMuted,
                fontSize = 10.sp,
            )
        }

        item {
            KasaCard(
                tier = s?.piggyTier ?: 0,
                bonus = s?.piggyBonusSc ?: 0,
                progress = s?.piggyMatchProgress ?: 0,
                target = s?.piggyMatchTarget ?: 8,
                busy = busy == "piggy",
                onOpen = {
                    val b = backend ?: return@KasaCard
                    scope.launch {
                        busy = "piggy"
                        runCatching { b.openPiggyBank() }
                            .onSuccess { result ->
                                notice = sh("Kasa açıldı: +${result.bonusSc} Son Coin.", "Piggy Bank opened: +${result.bonusSc} Son Coin.")
                                runCatching { b.trackStoreEvent("piggy_open") }
                                reload()
                            }
                            .onFailure { notice = sh("Kasa henüz açılmaya hazır değil.", "The Piggy Bank is not ready to open yet.") }
                        busy = null
                    }
                },
            )
        }

        if (!showKasaOnly) {
            item {
                RewardAdCard(
                    icon = Icons.Rounded.Paid,
                    title = sh("SON COIN", "SON COIN"),
                    description = sh(
                        "Tamamlanan reklam +10 Son Coin verir. Günlük sınır sunucuda tutulur ve aynı reklam iki kez ödül veremez.",
                        "A completed ad gives +10 Son Coin. The daily cap is server-side and the same ad cannot grant twice.",
                    ),
                    progress = "${s?.coinAdsUsed ?: 0}/${s?.coinAdsLimit ?: 3}",
                    button = sh("REKLAM İZLE  +10 SC", "WATCH AD  +10 SC"),
                    enabled = adReady && (s?.coinAdsUsed ?: 0) < (s?.coinAdsLimit ?: 3) && busy == null,
                    onClick = { showRewarded("diamonds") },
                )
            }

            item {
                val trialName = trialItem?.let { if (SonHarfUiState.isEnglish) it.nameEn else it.nameTr }
                val trialLabel = when (trialItem?.trialMode) {
                    "match" -> sh("1 MAÇ DENE", "TRY 1 MATCH")
                    "minutes" -> sh("30 DAKİKA DENE", "TRY 30 MINUTES")
                    else -> sh("DENEME YOK", "NO TRIAL")
                }
                RewardAdCard(
                    icon = Icons.Rounded.Palette,
                    title = sh("STYLE DENEMESİ", "STYLE TRIAL"),
                    description = if (trialName != null) sh(
                        "$trialName ürününü güvenli ve süreli olarak dene. Deneme sunucu saatine/maç sayısına bağlıdır; ranked gücü vermez.",
                        "Try $trialName for a secure limited period. The trial is server-timed/match-counted and grants no ranked power.",
                    ) else sh("Şu anda denemeye açık Style ürünü yok.", "No Style item is currently enabled for trial."),
                    progress = "${s?.trialAdsUsed ?: 0}/${s?.trialAdsLimit ?: 1}",
                    button = trialLabel,
                    enabled = adReady && trialItem != null && (s?.trialAdsUsed ?: 0) < (s?.trialAdsLimit ?: 1) && busy == null,
                    onClick = { showRewarded("trial") },
                )
            }

            if (s?.trialItemId != null) item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SonHarfPurple.copy(alpha = .10f)),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, SonHarfPurple.copy(alpha = .35f)),
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = SonHarfGreen)
                            Text(sh("AKTİF STYLE DENEMESİ", "ACTIVE STYLE TRIAL"), color = SonHarfPurple, fontWeight = FontWeight.Black)
                        }
                        val activeTrial = items.firstOrNull { it.id == s.trialItemId }
                        Text(if (SonHarfUiState.isEnglish) activeTrial?.nameEn ?: s.trialItemId else activeTrial?.nameTr ?: s.trialItemId, fontWeight = FontWeight.Bold)
                        Text(
                            when (s.trialMode) {
                                "match" -> sh("Kalan: ${s.trialMatchesRemaining ?: 0} maç", "Remaining: ${s.trialMatchesRemaining ?: 0} match")
                                else -> sh("Bitiş: ${s.trialExpiresAt.orEmpty()}", "Ends: ${s.trialExpiresAt.orEmpty()}")
                            },
                            color = SonHarfMuted,
                            fontSize = 9.sp,
                        )
                        Button(
                            onClick = {
                                val b = backend ?: return@Button
                                scope.launch {
                                    busy = "equip_trial"
                                    runCatching { b.equipRewardTrial() }
                                        .onSuccess { notice = sh("Deneme Style ürünü etkinleştirildi.", "Trial Style item equipped."); reload() }
                                        .onFailure { notice = sh("Deneme artık aktif değil.", "The trial is no longer active."); reload() }
                                    busy = null
                                }
                            },
                            enabled = busy == null,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SonHarfPurple),
                        ) { Text(if (busy == "equip_trial") sh("İŞLENİYOR", "PROCESSING") else sh("DENEMEYİ KULLAN", "USE TRIAL"), fontWeight = FontWeight.Black) }
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface2), shape = RoundedCornerShape(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Info, contentDescription = null, tint = SonHarfCyan)
                        Column(Modifier.weight(1f)) {
                            Text(sh("SUNUCU KORUMALI", "SERVER PROTECTED"), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            Text(sh("Kotalar sunucuda tutulur. Cihaz saatini değiştirmek veya uygulamayı silmek hak kazandırmaz.", "Caps are server-side. Changing device time or reinstalling does not grant rewards."), color = SonHarfMuted, fontSize = 9.sp)
                            Text(sh("Bakiye: ${profile?.diamonds ?: 0} SC", "Balance: ${profile?.diamonds ?: 0} SC"), color = SonHarfCyan, fontWeight = FontWeight.Black)
                        }
                    }
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
private fun KasaCard(
    tier: Int,
    bonus: Int,
    progress: Int,
    target: Int,
    busy: Boolean,
    onOpen: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SonHarfSurface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SonHarfGold.copy(alpha = .32f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.Savings, contentDescription = null, tint = SonHarfGold, modifier = Modifier.size(30.dp))
                    Column {
                        Text(sh("KASA", "PIGGY BANK"), color = SonHarfGold, fontWeight = FontWeight.Black)
                        Text(sh("Sabit bonus • rastgele ödül yok", "Fixed bonus • no random rewards"), color = SonHarfMuted, fontSize = 9.sp)
                    }
                }
                Text(if (bonus > 0) "+$bonus SC" else sh("DOLUYOR", "FILLING"), color = SonHarfCyan, fontWeight = FontWeight.Black)
            }
            LinearProgressIndicator(
                progress = { (progress.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = SonHarfGold,
                trackColor = SonHarfSurface2,
            )
            Text(sh("İlerleme: $progress/$target tamamlanmış maç • Seviye $tier/4", "Progress: $progress/$target completed matches • Tier $tier/4"), color = SonHarfMuted, fontSize = 9.sp)
            Text(sh("Bonus basamakları: 200 / 400 / 600 / 800 SC", "Bonus tiers: 200 / 400 / 600 / 800 SC"), color = SonHarfText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Button(
                onClick = onOpen,
                enabled = bonus in setOf(200, 400, 600, 800) && !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SonHarfGold, contentColor = Color(0xFF211830)),
            ) { Text(if (busy) sh("İŞLENİYOR", "PROCESSING") else sh("KASAYI AÇ", "OPEN PIGGY BANK"), fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
private fun RewardAdCard(
    icon: ImageVector,
    title: String,
    description: String,
    progress: String,
    button: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SonHarfSurface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SonHarfMuted.copy(alpha = .14f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(icon, contentDescription = null, tint = SonHarfCyan, modifier = Modifier.size(28.dp))
                    Text(title, fontWeight = FontWeight.Black)
                }
                Text(progress, color = SonHarfCyan, fontWeight = FontWeight.Black)
            }
            Text(description, color = SonHarfMuted, fontSize = 9.sp)
            Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(button, fontWeight = FontWeight.Black)
            }
        }
    }
}
