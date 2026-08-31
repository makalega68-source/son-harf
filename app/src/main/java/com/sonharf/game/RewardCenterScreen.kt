package com.sonharf.game

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private tailrec fun Context.findRewardActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findRewardActivity()
    else -> null
}

@Composable
fun ShopHubScreen(onOpenProfileAppearance: (() -> Unit)? = null) {
    EconomyShopScreen(onOpenProfileAppearance = onOpenProfileAppearance)
}

@Composable
fun RewardCenterScreen(
    onOpenTasks: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findRewardActivity() }
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val adController = remember { RewardedAdController(context) }
    val scope = rememberCoroutineScope()

    var status by remember { mutableStateOf<RewardCenterStatusDto?>(null) }
    var items by remember { mutableStateOf<List<ShopItemDto>>(emptyList()) }
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var growth by remember { mutableStateOf<GrowthDashboardDto?>(null) }
    var meta by remember { mutableStateOf<MetaProgressV2Dto?>(null) }
    var missions by remember { mutableStateOf<List<UnifiedMissionDto>>(emptyList()) }
    var adReady by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val adsAllowed = AdPrivacyManager.adsAllowed

    suspend fun reload() = coroutineScope {
        val b = backend
        if (b == null) {
            notice = sh("Ödül merkezi sunucu bağlantısı olmadan kullanılamaz.", "Reward Center requires a server connection.")
            return@coroutineScope
        }
        if (b.currentUserId() == null) {
            runCatching { b.ensurePlayer(sh("Oyuncu", "Player")) }.getOrElse {
                notice = sh("Oyuncu oturumu hazırlanamadı.", "Player session is not ready.")
                return@coroutineScope
            }
        }
        val statusTask = async { runCatching { b.getRewardCenterStatus() }.getOrNull() }
        val itemTask = async { runCatching { b.getShopItems() }.getOrDefault(emptyList()) }
        val profileTask = async { b.currentUserId()?.let { runCatching { b.getProfile(it) }.getOrNull() } }
        val growthTask = async { runCatching { b.getGrowthDashboard() }.getOrNull() }
        val metaTask = async { runCatching { b.getMetaProgressV2() }.getOrNull() }
        val missionTask = async { runCatching { b.getUnifiedMissions() }.getOrDefault(emptyList()) }
        status = statusTask.await()
        items = itemTask.await()
        profile = profileTask.await()
        growth = growthTask.await()
        meta = metaTask.await()
        missions = missionTask.await()
    }

    LaunchedEffect(Unit) { reload() }

    LaunchedEffect(adsAllowed) {
        if (adsAllowed) adController.load { adReady = adController.ready }
        else {
            adController.clear()
            adReady = false
        }
    }

    fun showRewarded(rewardType: String) {
        val a = activity
        val b = backend
        if (a == null || b == null || busy != null) return
        busy = rewardType
        adController.show(
            a,
            onEarned = { responseId ->
                scope.launch {
                    runCatching { b.claimRewardedAd(rewardType, responseId) }
                        .onSuccess { claim ->
                            notice = when (rewardType) {
                                "diamonds" -> sh("+${claim.diamondsAwarded} Son Coin cüzdanına eklendi.", "+${claim.diamondsAwarded} Son Coin added to your wallet.")
                                "chest" -> sh("1 ödül sandığı kazandın.", "You earned 1 reward chest.")
                                else -> sh("24 saatlik Style denemen başladı.", "Your 24-hour Style trial has started.")
                            }
                            reload()
                        }
                        .onFailure { e ->
                            notice = if ("daily_limit_reached" in e.message.orEmpty()) sh("Bugünkü reklam kotan tamamlandı.", "Today's ad quota is complete.") else sh("Ödül işlenemedi.", "Reward could not be processed.")
                        }
                    busy = null
                    adReady = adController.ready
                }
            },
            onUnavailable = {
                notice = sh("Ödüllü reklam şu an hazır değil.", "The rewarded ad is not ready yet.")
                busy = null
                adReady = false
            },
            onClosed = { adController.load { adReady = adController.ready } },
        )
    }

    val s = status
    val g = growth
    val dailyMissions = missions.filter { it.scope == "daily" }
    val remainingCoinAds = ((s?.diamondAdsLimit ?: 3) - (s?.diamondAdsUsed ?: 0)).coerceAtLeast(0)
    val remainingChestAds = ((s?.chestAdsLimit ?: 2) - (s?.chestAdsUsed ?: 0)).coerceAtLeast(0)
    val remainingTrialAds = ((s?.trialAdsLimit ?: 1) - (s?.trialAdsUsed ?: 0)).coerceAtLeast(0)
    val trialItem = items.firstOrNull { it.id == s?.trialItemId }
    val rootModifier = if (onOpenTasks != null) {
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
    } else {
        Modifier.fillMaxSize()
    }

    LazyColumn(
        modifier = rootModifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(sh("Ödüller", "Rewards"), color = MainUi.Text, fontSize = 27.sp, fontWeight = FontWeight.Black)
            Text(
                sh("Günlük ödüller, sandıklar ve isteğe bağlı reklamlar. Maç sırasında reklam gösterilmez.", "Daily rewards, chests and optional rewarded ads. Ads never appear during matches."),
                color = MainUi.Muted,
                fontSize = 10.sp,
            )
        }

        item {
            Surface(color = MainUi.BlueSoft, shape = RoundedCornerShape(18.dp)) {
                Text(
                    sh("Ödüller maç avantajı vermez; yalnızca Son Coin ve güç vermeyen Style içindir.", "Rewards never give match advantages; they are only for Son Coin and non-power Style."),
                    Modifier.fillMaxWidth().padding(12.dp),
                    color = MainUi.Text,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        item {
            Surface(color = MainUi.Surface, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MainUi.Border)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = MainUi.Gold.copy(alpha = .14f)) {
                            Icon(Icons.Rounded.LocalFireDepartment, null, tint = MainUi.Gold, modifier = Modifier.padding(8.dp).size(20.dp))
                        }
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(sh("GÜNLÜK SERİ VE GİRİŞ", "DAILY STREAK & CHECK-IN"), color = MainUi.Text, fontWeight = FontWeight.Black, fontSize = 11.sp)
                            Text(sh("${meta?.dailyPlayStreak ?: 0} günlük seri • en iyi ${meta?.bestDailyPlayStreak ?: 0}", "${meta?.dailyPlayStreak ?: 0}-day streak • best ${meta?.bestDailyPlayStreak ?: 0}"), color = MainUi.Muted, fontSize = 9.sp)
                        }
                        Text("+${g?.dailyReward ?: 40} SC", color = MainUi.Gold, fontWeight = FontWeight.Black, fontSize = 10.sp)
                    }
                    Button(
                        onClick = {
                            val b = backend ?: return@Button
                            if (busy != null || g?.dailyClaimed == true) return@Button
                            scope.launch {
                                busy = "checkin"
                                val reward = runCatching { b.claimDailyCheckin() }.getOrDefault(0)
                                notice = if (reward > 0) sh("+$reward Son Coin cüzdanına eklendi.", "+$reward Son Coin added to your wallet.") else sh("Bugünün giriş ödülü daha önce alındı.", "Today's check-in reward was already claimed.")
                                reload()
                                busy = null
                            }
                        },
                        enabled = busy == null && g?.dailyClaimed != true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MainUi.Gold,
                            contentColor = Color(0xFF3C2700),
                            disabledContainerColor = MainUi.Green.copy(alpha = .14f),
                            disabledContentColor = MainUi.Green,
                        ),
                    ) {
                        Text(if (g?.dailyClaimed == true) sh("✓ ALINDI", "✓ CLAIMED") else sh("GÜNLÜK ÖDÜLÜ AL", "CLAIM DAILY REWARD"), fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        item {
            Surface(color = MainUi.Surface, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MainUi.Border)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = MainUi.Green.copy(alpha = .10f)) {
                            Icon(Icons.Rounded.TaskAlt, null, tint = MainUi.Green, modifier = Modifier.padding(8.dp).size(20.dp))
                        }
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(sh("GÜNLÜK GÖREV ÖDÜLLERİ", "DAILY MISSION REWARDS"), color = MainUi.Text, fontWeight = FontWeight.Black, fontSize = 11.sp)
                            Text(sh("Görev XP'si sezon ilerlemesini destekler; Son Coin ödülleri cüzdana işlenir.", "Mission progress supports your season; Son Coin rewards go to your wallet."), color = MainUi.Muted, fontSize = 9.sp)
                        }
                    }
                    if (dailyMissions.isEmpty()) {
                        Text(sh("Bugün için açık günlük görev bulunmuyor.", "No active daily missions are available today."), color = MainUi.Muted, fontSize = 9.sp)
                    } else {
                        dailyMissions.take(3).forEach { mission ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(if (SonHarfUiState.isEnglish) mission.titleEn else mission.titleTr, color = MainUi.Text, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text(if (mission.claimed) "✓" else "+${mission.rewardCoins} SC", color = if (mission.claimed) MainUi.Green else MainUi.Gold, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                                LinearProgressIndicator(
                                    progress = { (mission.progress.toFloat() / mission.target.coerceAtLeast(1)).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                                    color = if (mission.claimed) MainUi.Green else MainUi.Blue,
                                    trackColor = MainUi.SurfaceSoft,
                                )
                            }
                        }
                    }
                    if (onOpenTasks != null) {
                        OutlinedButton(onClick = onOpenTasks, modifier = Modifier.fillMaxWidth()) {
                            Text(sh("GÖREVLERİ AÇ", "OPEN MISSIONS"), color = MainUi.Blue, fontWeight = FontWeight.Black, fontSize = 9.sp)
                        }
                    }
                }
            }
        }

        item {
            RewardAdCard(
                title = "SON COIN",
                description = sh("Tamamlanan her isteğe bağlı reklam +${s?.diamondPerAd ?: 10} Son Coin verir.", "Each completed optional ad gives +${s?.diamondPerAd ?: 10} Son Coin."),
                progress = sh("Bugün $remainingCoinAds reklam hakkı kaldı", "$remainingCoinAds ad rewards left today"),
                button = sh("REKLAM İZLE  +${s?.diamondPerAd ?: 10} SC", "WATCH AD  +${s?.diamondPerAd ?: 10} SC"),
                enabled = adsAllowed && adReady && remainingCoinAds > 0 && busy == null,
                onClick = { showRewarded("diamonds") },
            )
        }

        item {
            RewardAdCard(
                title = sh("ÖDÜL SANDIĞI", "REWARD CHEST"),
                description = sh("Reklam başına 1 sandık hakkı kazanırsın. Sandık Son Coin verir.", "Earn 1 chest per ad. Chests grant Son Coin."),
                progress = sh("Bugün $remainingChestAds sandık reklamı kaldı", "$remainingChestAds chest ads left today"),
                button = sh("REKLAM İZLE  +1 SANDIK", "WATCH AD  +1 CHEST"),
                enabled = adsAllowed && adReady && remainingChestAds > 0 && busy == null,
                onClick = { showRewarded("chest") },
            )
        }

        item {
            Surface(color = MainUi.Surface, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MainUi.Gold.copy(alpha = .30f))) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(sh("SANDIKLARIM", "MY CHESTS"), color = MainUi.Text, fontWeight = FontWeight.Black)
                        Text("${s?.chestKeys ?: 0}", color = MainUi.Gold, fontWeight = FontWeight.Black)
                    }
                    Text(sh("Sandıktan çıkan Son Coin doğrudan cüzdanına eklenir.", "Son Coin from a chest is added directly to your wallet."), color = MainUi.Muted, fontSize = 9.sp)
                    Button(
                        onClick = {
                            val b = backend ?: return@Button
                            scope.launch {
                                busy = "open_chest"
                                runCatching { b.openRewardChest() }
                                    .onSuccess { reward -> notice = sh("Sandıktan ${reward.diamondsAwarded} Son Coin çıktı ve cüzdanına eklendi.", "Chest awarded ${reward.diamondsAwarded} Son Coin and added it to your wallet."); reload() }
                                    .onFailure { notice = sh("Açılacak sandığın yok.", "You do not have a chest to open.") }
                                busy = null
                            }
                        },
                        enabled = (s?.chestKeys ?: 0) > 0 && busy == null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MainUi.Gold, contentColor = Color(0xFF3C2700)),
                    ) { Text(sh("SANDIĞI AÇ", "OPEN CHEST"), fontWeight = FontWeight.Black) }
                }
            }
        }

        item {
            RewardAdCard(
                title = sh("STYLE DENEME", "STYLE TRIAL"),
                description = sh("Günde 1 isteğe bağlı reklamla uygun bir VIP Style öğesini 24 saat deneyebilirsin.", "Watch 1 optional ad per day to try an eligible VIP Style item for 24 hours."),
                progress = sh("Bugün $remainingTrialAds deneme hakkı kaldı", "$remainingTrialAds trial reward left today"),
                button = sh("24 SAAT DENEME", "24-HOUR TRIAL"),
                enabled = adsAllowed && adReady && remainingTrialAds > 0 && busy == null,
                onClick = { showRewarded("trial") },
            )
        }

        if (s?.trialItemId != null) item {
            Surface(color = MainUi.BlueSoft, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, MainUi.Blue.copy(alpha = .25f))) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(sh("AKTİF STYLE DENEME", "ACTIVE STYLE TRIAL"), color = MainUi.Blue, fontWeight = FontWeight.Black)
                    Text(if (SonHarfUiState.isEnglish) trialItem?.nameEn ?: s.trialItemId else trialItem?.nameTr ?: s.trialItemId, color = MainUi.Text, fontWeight = FontWeight.Bold)
                    Text(s.trialExpiresAt.orEmpty(), color = MainUi.Muted, fontSize = 8.sp)
                    Button(
                        onClick = {
                            val b = backend ?: return@Button
                            scope.launch {
                                busy = "equip_trial"
                                runCatching { b.equipRewardTrial() }
                                    .onSuccess { notice = sh("Style denemesi etkinleştirildi.", "Style trial activated."); reload() }
                                    .onFailure { notice = sh("Deneme artık aktif değil.", "The trial is no longer active."); reload() }
                                busy = null
                            }
                        },
                        enabled = busy == null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MainUi.Blue),
                    ) { Text(if (busy == "equip_trial") "…" else sh("DENEMEYİ KULLAN", "USE TRIAL"), fontWeight = FontWeight.Black) }
                }
            }
        }

        item {
            Surface(color = MainUi.SurfaceSoft, shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(sh("GÜNLÜK KOTALAR", "DAILY QUOTAS"), color = MainUi.Text, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    Text(sh("Reklam kotaları sunucuda yenilenir. Reklam izlemek tamamen isteğe bağlıdır.", "Ad quotas reset on the server. Watching rewarded ads is always optional."), color = MainUi.Muted, fontSize = 9.sp)
                    Text("◈ ${profile?.diamonds ?: 0} SC", color = MainUi.Gold, fontWeight = FontWeight.Black)
                }
            }
        }

        if (!notice.isNullOrBlank()) item {
            Surface(color = MainUi.SurfaceSoft, shape = RoundedCornerShape(14.dp)) {
                Text(notice!!, Modifier.fillMaxWidth().padding(12.dp), color = MainUi.Text, textAlign = TextAlign.Center, fontSize = 10.sp)
            }
        }
        item { Spacer(Modifier.height(6.dp)) }
    }
}

@Composable
private fun RewardAdCard(
    title: String,
    description: String,
    progress: String,
    button: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(color = MainUi.Surface, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MainUi.Border)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = MainUi.Text, fontWeight = FontWeight.Black)
                Text(progress, color = MainUi.Muted, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
            }
            Text(description, color = MainUi.Muted, fontSize = 9.sp)
            Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MainUi.Blue)) {
                Text(button, fontWeight = FontWeight.Black)
            }
        }
    }
}
