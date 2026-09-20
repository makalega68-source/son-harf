package com.sonharf.game

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.VerifiedUser
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

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun ShopHubScreen() { EconomyShopScreen() }

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
    val isPro = profile?.isVip == true
    val adsAllowed = profile?.isVip == false && AdPrivacyManager.adsAllowed
    var busy by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var rewardVfxKey by remember { mutableStateOf<String?>(null) }

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
        if (isPro) {
            notice = sh("PRO hesabında reklam gösterilmez.", "Ads are disabled on PRO accounts.")
            return
        }
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
        scope.launch {
            val intentId = runCatching { b.prepareStoreReward(rewardType, trialItemId) }.getOrElse {
                notice = sh("Ödüllü reklam şu anda kullanılamıyor.", "Rewarded ads are currently unavailable.")
                busy = null
                return@launch
            }
            adController.show(
                a,
                verificationUserId = b.currentUserId(),
                verificationData = intentId,
                onEarned = { responseId ->
                    scope.launch {
                        runCatching { b.awaitVerifiedStoreReward(rewardType, responseId, trialItemId) }
                            .onSuccess { claim ->
                                notice = when (rewardType) {
                                    "diamonds" -> sh(
                                        "+${claim.diamondsAwarded.takeIf { it > 0 } ?: (status?.coinPerAd ?: 10)} Son Coin hesabına eklendi.",
                                        "+${claim.diamondsAwarded.takeIf { it > 0 } ?: (status?.coinPerAd ?: 10)} Son Coin added.",
                                    )
                                    else -> sh("Style denemen başladı.", "Your Style trial has started.")
                                }
                                rewardVfxKey = "reward:$rewardType:$responseId"
                                SonHarfSoundFx.reward()
                                reload()
                            }
                            .onFailure { e ->
                                notice = when {
                                    "daily_limit_reached" in e.message.orEmpty() -> sh("Bugünkü kota tamamlandı.", "Today's quota is complete.")
                                    "ad_verification_pending" in e.message.orEmpty() -> sh("Ödül doğrulandığında hesabına eklenecek.", "Your reward will be added after verification.")
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
                onClosed = {
                    busy = null
                    if (adsAllowed) adController.load { adReady = adController.ready }
                    else {
                        adController.clear()
                        adReady = false
                    }
                },
            )
        }
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

    Box(Modifier.fillMaxSize()) {
        androidx.compose.foundation.lazy.LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                MainScreenHeader(
                    title = sh("Kelime Kuşatması Ödülleri", "Word Siege Rewards"),
                    subtitle = if (isPro) {
                        sh(
                            "PRO hesabında reklam yok; sunucu kontrollü ödüller devam eder.",
                            "PRO is ad-free; server-controlled rewards remain available.",
                        )
                    } else {
                        sh(
                            "Ödüllü reklamlar isteğe bağlıdır; oyun ekranlarında reklam yoktur.",
                            "Rewarded ads are optional; gameplay screens remain ad-free.",
                        )
                    },
                )
            }

            if (!isPro) {
                item {
                    RewardAdCard(
                        icon = Icons.Rounded.Paid,
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
                        icon = Icons.Rounded.AutoAwesome,
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
            }

            if (s?.trialItemId != null) item {
                MainGameCard {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = MainUiShape.Control, color = MainUi.BlueSoft) {
                                Icon(Icons.Rounded.AutoAwesome, null, tint = MainUi.Blue, modifier = Modifier.padding(9.dp).size(20.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(sh("AKTİF DENEME", "ACTIVE TRIAL"), color = MainUi.Blue, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text(
                                    if (SonHarfUiState.isEnglish) activeTrialItem?.nameEn ?: s.trialItemId else activeTrialItem?.nameTr ?: s.trialItemId,
                                    color = MainUi.Text,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                )
                            }
                        }
                        val remaining = when (s.trialMode) {
                            "match" -> sh("Kalan maç: ${s.trialMatchesRemaining ?: 0}", "Matches left: ${s.trialMatchesRemaining ?: 0}")
                            "minutes" -> s.trialExpiresAt.orEmpty()
                            else -> s.trialExpiresAt.orEmpty()
                        }
                        if (remaining.isNotBlank()) Text(remaining, color = MainUi.Muted, fontSize = 10.sp)
                        MainGameButton(
                            text = if (busy == "equip_trial") "…" else sh("DENEMEYİ KULLAN", "USE TRIAL"),
                            onClick = {
                                val b = backend ?: return@MainGameButton
                                scope.launch {
                                    busy = "equip_trial"
                                    runCatching { b.equipRewardTrial() }
                                        .onSuccess {
                                            notice = sh("Deneme Style ürünü etkinleştirildi.", "Trial Style item equipped.")
                                            rewardVfxKey = "reward:trial-equip:${s.trialItemId}"
                                            SonHarfSoundFx.reward()
                                            reload()
                                        }
                                        .onFailure {
                                            notice = sh("Deneme artık aktif değil.", "The trial is no longer active.")
                                            reload()
                                        }
                                    busy = null
                                }
                            },
                            enabled = busy == null,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            item {
                MainGameCard {
                    Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = MainUiShape.Control, color = MainUi.GoldSoft) {
                                    Icon(Icons.Rounded.Savings, null, tint = MainUi.Gold, modifier = Modifier.padding(9.dp).size(20.dp))
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(sh("KUMBARA", "PIGGY BANK"), color = MainUi.Text, fontWeight = FontWeight.Bold)
                            }
                            Text("${s?.piggyMatchProgress ?: 0}/${s?.piggyMatchTarget ?: 8}", color = MainUi.Gold, fontWeight = FontWeight.Bold)
                        }
                        MainProgress(
                            progress = ((s?.piggyMatchProgress ?: 0).toFloat() / (s?.piggyMatchTarget ?: 8).coerceAtLeast(1)).coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxWidth(),
                            accent = MainUi.Gold,
                        )
                        Text(
                            sh(
                                "Tamamlanan maçlarla Kumbara dolar. Hazır olduğunda ${s?.piggyBonusSc ?: 0} Son Coin sunucu tarafından doğrulanarak açılır.",
                                "Completed matches fill the Piggy Bank. When ready, ${s?.piggyBonusSc ?: 0} Son Coin is verified and granted by the server.",
                            ),
                            color = MainUi.Muted,
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
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
                                            rewardVfxKey = "reward:piggy:${reward.bonusSc}:${s?.piggyMatchProgress}"
                                            SonHarfSoundFx.reward()
                                            reload()
                                        }
                                        .onFailure { notice = sh("Kumbara henüz hazır değil.", "The Piggy Bank is not ready yet.") }
                                    busy = null
                                }
                            },
                            enabled = (s?.piggyBonusSc ?: 0) > 0 && busy == null,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            shape = MainUiShape.Control,
                            colors = ButtonDefaults.buttonColors(containerColor = MainUi.Gold, contentColor = Color(0xFF2B2418)),
                        ) {
                            Text(if (busy == "piggy") "…" else sh("KUMBARAYI AÇ", "OPEN PIGGY BANK"), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                MainGameCard {
                    Row(
                        Modifier.fillMaxWidth().padding(13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(shape = MainUiShape.Control, color = MainUi.GreenSoft) {
                            Icon(Icons.Rounded.VerifiedUser, null, tint = MainUi.Green, modifier = Modifier.padding(8.dp).size(19.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(sh("SUNUCU KONTROLLÜ ÖDÜLLER", "SERVER-CONTROLLED REWARDS"), color = MainUi.Text, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            Text(
                                sh(
                                    "Günlük kotalar, deneme süresi ve Kumbara ilerlemesi sunucuda tutulur; cihaz saati veya yeniden kurulum bunları sıfırlamaz.",
                                    "Daily quotas, trial duration and Piggy Bank progress are stored on the server; device time or reinstalling does not reset them.",
                                ),
                                color = MainUi.Muted,
                                fontSize = 9.sp,
                                lineHeight = 13.sp,
                            )
                        }
                        Text("${profile?.diamonds ?: 0}", color = MainUi.Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            if (!notice.isNullOrBlank()) item {
                Surface(
                    color = MainUi.Surface,
                    shape = MainUiShape.Control,
                    border = BorderStroke(1.dp, MainUi.Border),
                ) {
                    Text(
                        notice!!,
                        Modifier.fillMaxWidth().padding(12.dp),
                        color = MainUi.Muted,
                        textAlign = TextAlign.Center,
                        fontSize = 10.sp,
                    )
                }
            }
        }

        rewardVfxKey?.let { key ->
            PurchasedMomentVfx(
                eventKey = key,
                kind = PurchasedMomentVfxKind.REWARD,
                modifier = Modifier.fillMaxSize(),
            )
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
    MainGameCard {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = MainUiShape.Control, color = MainUi.BlueSoft) {
                        Icon(icon, null, tint = MainUi.Blue, modifier = Modifier.padding(9.dp).size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(title, color = MainUi.Text, fontWeight = FontWeight.Bold)
                }
                MainBadge(progress, accent = MainUi.Blue)
            }
            Text(description, color = MainUi.Muted, fontSize = 10.sp, lineHeight = 14.sp)
            MainGameButton(
                text = button,
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
