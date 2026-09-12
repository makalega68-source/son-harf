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
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
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

    LaunchedEffect(Unit) { runCatching { reload() } }

    LaunchedEffect(adsAllowed) {
        if (adsAllowed) adController.load { adReady = adController.ready }
        else {
            adController.clear()
            adReady = false
        }
    }

    fun showRewarded(rewardType: String, trialItemId: String? = null) {
        val a = activity
        val b = backend
        if (a == null || busy != null) return
        if (b == null) {
            notice = sh("Ödül merkezi şu anda çevrimdışı.", "Reward Center is currently offline.")
            return
        }
        if (rewardType == "trial" && trialItemId.isNullOrBlank()) {
            notice = sh("Şu anda denemeye uygun bir Style ürünü yok.", "No Style item is currently available for trial.")
            return
        }
        busy = rewardType
        adController.show(
            a,
            onEarned = { responseId ->
                scope.launch {
                    runCatching { b.claimRewardedAd(rewardType, responseId, trialItemId) }
                        .onSuccess { claim ->
                            notice = when (rewardType) {
                                "diamonds" -> sh(
                                    "+${claim.diamondsAwarded.takeIf { it > 0 } ?: (status?.coinPerAd ?: 10)} Son Coin hesabına eklendi.",
                                    "+${claim.diamondsAwarded.takeIf { it > 0 } ?: (status?.coinPerAd ?: 10)} Son Coin added.",
                                )
                                else -> sh("Style denemen başladı.", "Your Style trial has started.")
                            }
                            reload()
                        }
                        .onFailure { e ->
                            notice = when {
                                "daily_limit_reached" in e.message.orEmpty() -> sh("Bugünkü kota tamamlandı.", "Today's quota is complete.")
                                "trial_item_unavailable" in e.message.orEmpty() -> sh("Bu deneme ürünü artık kullanılamıyor.", "This trial item is no longer available.")
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
    val activeTrialItem = items.firstOrNull { it.id == s?.trialItemId }
    val trialCandidate = items.firstOrNull { !it.trialMode.isNullOrBlank() && (it.trialValue ?: 0) > 0 }
    val trialCandidateName = trialCandidate?.let { if (SonHarfUiState.isEnglish) it.nameEn else it.nameTr }
    val trialDescription = trialCandidate?.let {
        when (it.trialMode) {
            "minutes" -> sh("${it.trialValue ?: 0} dakika Style denemesi", "${it.trialValue ?: 0}-minute Style trial")
            "match" -> sh("${it.trialValue ?: 1} maçlık Style denemesi", "${it.trialValue ?: 1}-match Style trial")
            else -> sh("Style denemesi", "Style trial")
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(sh("KELİME TAHTI ÖDÜLLERİ", "KELIME TAHTI REWARDS"), fontSize = 27.sp, fontWeight = FontWeight.Black)
            Text(
                sh(
                    "Ödüllü reklamlar isteğe bağlıdır. Maçlarda ve oyun alanında reklam yoktur.",
                    "Rewarded ads are optional. Matches and gameplay remain ad-free.",
                ),
                color = SonHarfMuted,
                fontSize = 10.sp,
            )
        }

        item {
            RewardAdCard(
                icon = "◈",
                title = "SON COIN",
                description = sh(
                    "Her tamamlanan reklam +${s?.coinPerAd ?: 10} Son Coin verir. Günlük kota sunucu tarafından tutulur.",
                    "Each completed ad gives +${s?.coinPerAd ?: 10} Son Coin. The daily quota is enforced by the server.",
                ),
                progress = "${s?.coinAdsUsed ?: 0}/${s?.coinAdsLimit ?: 3}",
                button = sh("REKLAM İZLE", "WATCH AD"),
                enabled = adReady && (s?.coinAdsUsed ?: 0) < (s?.coinAdsLimit ?: 3) && busy == null,
                onClick = { showRewarded("diamonds") },
            )
        }

        item {
            RewardAdCard(
                icon = "✨",
                title = sh("STYLE DENEME", "STYLE TRIAL"),
                description = listOfNotNull(trialCandidateName, trialDescription).joinToString(" • ").ifBlank {
                    sh("Sunucu kataloğundaki uygun bir Style ürününü dene.", "Try an eligible Style item from the server catalog.")
                },
                progress = "${s?.trialAdsUsed ?: 0}/${s?.trialAdsLimit ?: 1}",
                button = sh("DENEMEYİ BAŞLAT", "START TRIAL"),
                enabled = adReady && trialCandidate != null && (s?.trialAdsUsed ?: 0) < (s?.trialAdsLimit ?: 1) && busy == null,
                onClick = { showRewarded("trial", trialCandidate?.id) },
            )
        }

        if (s?.trialItemId != null) item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SonHarfPurple.copy(alpha = .12f)),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, SonHarfPurple.copy(alpha = .45f)),
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(sh("AKTİF DENEME", "ACTIVE TRIAL"), color = SonHarfPurple, fontWeight = FontWeight.Black)
                    Text(
                        if (SonHarfUiState.isEnglish) activeTrialItem?.nameEn ?: s.trialItemId else activeTrialItem?.nameTr ?: s.trialItemId,
                        fontWeight = FontWeight.Bold,
                    )
                    val remaining = when (s.trialMode) {
                        "match" -> sh("Kalan maç: ${s.trialMatchesRemaining ?: 0}", "Matches left: ${s.trialMatchesRemaining ?: 0}")
                        "minutes" -> s.trialExpiresAt.orEmpty()
                        else -> s.trialExpiresAt.orEmpty()
                    }
                    if (remaining.isNotBlank()) Text(remaining, color = SonHarfMuted, fontSize = 9.sp)
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
                    ) { Text(if (busy == "equip_trial") "…" else sh("DENEMEYİ KULLAN", "USE TRIAL"), fontWeight = FontWeight.Black) }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SonHarfSurface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SonHarfGold.copy(alpha = .30f)),
            ) {
                Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(sh("KUMBARA", "PIGGY BANK"), color = LetharaPalette.Gold, fontWeight = FontWeight.Black)
                        Text("${s?.piggyMatchProgress ?: 0}/${s?.piggyMatchTarget ?: 8}", fontWeight = FontWeight.Black)
                    }
                    LinearProgressIndicator(
                        progress = { ((s?.piggyMatchProgress ?: 0).toFloat() / (s?.piggyMatchTarget ?: 8).coerceAtLeast(1)).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        sh(
                            "Tamamlanan maçlarla Kumbara dolar. Hazır olduğunda ${s?.piggyBonusSc ?: 0} Son Coin sunucu tarafından doğrulanarak açılır.",
                            "Completed matches fill the Piggy Bank. When ready, ${s?.piggyBonusSc ?: 0} Son Coin is verified and granted by the server.",
                        ),
                        color = SonHarfMuted,
                        fontSize = 9.sp,
                    )
                    Button(
                        onClick = {
                            val b = backend ?: return@Button
                            scope.launch {
                                busy = "piggy"
                                runCatching { b.openPiggyBank() }
                                    .onSuccess { reward ->
                                        notice = sh(
                                            "Kumbara açıldı: +${reward.bonusSc} Son Coin.",
                                            "Piggy Bank opened: +${reward.bonusSc} Son Coin.",
                                        )
                                        reload()
                                    }
                                    .onFailure { notice = sh("Kumbara henüz hazır değil.", "The Piggy Bank is not ready yet.") }
                                busy = null
                            }
                        },
                        enabled = (s?.piggyBonusSc ?: 0) > 0 && busy == null,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SonHarfGold, contentColor = Color(0xFF211830)),
                    ) { Text(if (busy == "piggy") "…" else sh("KUMBARAYI AÇ", "OPEN PIGGY BANK"), fontWeight = FontWeight.Black) }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface2), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(sh("SUNUCU KONTROLLÜ ÖDÜLLER", "SERVER-CONTROLLED REWARDS"), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    Text(
                        sh(
                            "Günlük kotalar, deneme süresi ve Kumbara ilerlemesi sunucuda tutulur; cihaz saatini değiştirmek veya uygulamayı silmek bunları sıfırlamaz.",
                            "Daily quotas, trial duration and Piggy Bank progress are stored on the server; changing device time or reinstalling the app does not reset them.",
                        ),
                        color = SonHarfMuted,
                        fontSize = 9.sp,
                    )
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
    Card(
        colors = CardDefaults.cardColors(containerColor = SonHarfSurface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SonHarfMuted.copy(alpha = .14f)),
    ) {
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
            Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(button, fontWeight = FontWeight.Black)
            }
        }
    }
}
