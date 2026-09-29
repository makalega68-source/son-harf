package com.sonharf.game

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.billing.ProductCatalog
import com.sonharf.game.data.*
import kotlinx.coroutines.launch

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun ShopHubScreen() { EconomyShopScreen() }

/**
 * Watch-and-earn: every reward is optional, shown up front, capped per day or week and granted by
 * the server only after AdMob has verified the video. PRO players never see or load an ad.
 */
@Composable
fun RewardCenterScreen() {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val adController = remember { RewardedAdController(context) }
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<RewardCenterStatusDto?>(null) }
    var items by remember { mutableStateOf<List<ShopItemDto>>(emptyList()) }
    var owned by remember { mutableStateOf<Set<String>>(emptySet()) }
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var storefront by remember { mutableStateOf<StorefrontDto?>(null) }
    var seriesAccess by remember { mutableStateOf(false) }
    var adReady by remember { mutableStateOf(false) }
    val isPro = profile?.isVip == true
    // Do not even request a rewarded ad until account entitlement is known and non-PRO.
    val adsAllowed = profile?.isVip == false && AdPrivacyManager.adsAllowed
    var busy by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val passes = RewardPassState.passes
    val mascotOwner = WordSiegeMascotOwnership.hasAny

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
            owned = b.getInventory()
            profile = b.currentUserId()?.let { b.getProfile(it) }
            storefront = runCatching { b.getStorefront() }.getOrNull()
            seriesAccess = runCatching { b.getVipEntitlements().seriesGameAccess }.getOrDefault(false)
            RewardPassState.refresh()
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

    fun itemName(id: String?): String {
        val item = items.firstOrNull { it.id == id } ?: return id.orEmpty()
        return if (SonHarfUiState.isEnglish) item.nameEn else item.nameTr
    }

    fun successMessage(rewardType: String, claim: RewardClaimDto): String = when (rewardType) {
        RewardKeys.COINS -> sh(
            "+${claim.diamondsAwarded.takeIf { it > 0 } ?: (status?.coinPerAd ?: 10)} Son Coin hesabına eklendi.",
            "+${claim.diamondsAwarded.takeIf { it > 0 } ?: (status?.coinPerAd ?: 10)} Son Coin added.",
        )
        RewardKeys.DAILY_DOUBLE -> sh("Günlük hediyen ikiye katlandı: +${claim.diamondsAwarded} Son Coin!", "Daily gift doubled: +${claim.diamondsAwarded} Son Coin!")
        RewardKeys.KEYBOARD_DAY -> sh("${itemName(claim.trialItemId)} 24 saat senin! Tüm oyunlarda kullanılıyor.", "${itemName(claim.trialItemId)} is yours for 24 hours, in every game.")
        RewardKeys.THEME_DAY -> sh("${itemName(claim.trialItemId)} 24 saat senin!", "${itemName(claim.trialItemId)} is yours for 24 hours!")
        RewardKeys.QUICK_GAMES -> sh("${claim.amount} Hızlı Düello hakkı açıldı! Kuşatma > Hızlı Düello.", "${claim.amount} Quick Duels unlocked! Siege > Quick Duel.")
        else -> sh("+${claim.amount} ipucu bankana eklendi.", "+${claim.amount} hints added to your bank.")
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
        busy = rewardType
        scope.launch {
            val intentId = runCatching { b.prepareStoreReward(rewardType, trialItemId) }.getOrElse {
                notice = rewardErrorMessage(it.message.orEmpty())
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
                                notice = successMessage(rewardType, claim)
                                SonHarfSoundFx.bonus()
                                reload()
                            }
                            .onFailure { e -> notice = rewardErrorMessage(e.message.orEmpty()) }
                        busy = null
                        adReady = adController.ready
                    }
                },
                onUnavailable = {
                    notice = sh("Video şu an hazır değil. Biraz sonra tekrar dene.", "The video isn't ready. Try again shortly.")
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
    val keyboards = items.filter { it.kind == "keyboard_theme" && it.id in ProductCatalog.keyboardProducts && it.id !in owned }
    val themes = items.filter { it.kind == "game_theme" && it.active && it.id !in owned }
    var keyboardPick by remember { mutableStateOf<String?>(null) }
    var themePick by remember { mutableStateOf<String?>(null) }
    val chosenKeyboard = keyboardPick?.takeIf { id -> keyboards.any { it.id == id } } ?: keyboards.firstOrNull()?.id
    val chosenTheme = themePick?.takeIf { id -> themes.any { it.id == id } } ?: themes.firstOrNull()?.id

    fun periodLabel(key: String): String {
        val usage = passes?.usage(key) ?: return ""
        val period = if (usage.periodDays == 1) sh("Bugün", "Today") else sh("Bu hafta", "This week")
        return "$period ${usage.used}/${usage.max}"
    }
    fun canWatch(key: String): Boolean = adReady && busy == null && (passes?.left(key) ?: 1) > 0

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(sh("İZLE, KAZAN", "WATCH & WIN"), color = Hf.Gold, fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (isPro) {
                        sh(
                            "PRO hesabında reklam gösterilmez. Kumbara ve sunucu kontrollü ilerleme sistemleri normal şekilde devam eder.",
                            "Ads are disabled on PRO. Piggy Bank and server-controlled progression continue normally.",
                        )
                    } else {
                        sh(
                            "Kısa bir video izle, ödülünü hemen al. Tamamen isteğe bağlı; maçların içinde reklam yok.",
                            "Watch a short video, get your reward right away. Always optional; no ads inside matches.",
                        )
                    },
                    color = Hf.TextMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }

        if (!notice.isNullOrBlank()) item {
            Surface(color = Hf.Gold.copy(alpha = .14f), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Hf.Gold.copy(alpha = .6f))) {
                Text(notice!!, Modifier.fillMaxWidth().padding(12.dp), color = Hf.Text, textAlign = TextAlign.Center, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // What the player already has from videos.
        val bankLines = buildList {
            passes?.keyboard?.itemId?.let { add("⌨ " + sh("${itemName(it)} aktif (24 saat)", "${itemName(it)} active (24 h)")) }
            passes?.theme?.itemId?.let { add("🎨 " + sh("${itemName(it)} aktif (24 saat)", "${itemName(it)} active (24 h)")) }
            passes?.quickGamesLeft?.takeIf { it > 0 }?.let { add("⚡ " + sh("$it Hızlı Düello hakkın var", "$it Quick Duels left")) }
            val hints = listOf(
                sh("Son Harf", "Last Letter") to (passes?.hintsSonHarf ?: 0),
                sh("Kuşatma", "Siege") to (passes?.hintsSiege ?: 0),
                sh("Atölye", "Workshop") to (passes?.hintsWorkshop ?: 0),
            ).filter { it.second > 0 }
            if (hints.isNotEmpty()) add("💡 " + hints.joinToString(" • ") { "${it.first} ${it.second}" })
        }
        if (bankLines.isNotEmpty()) item {
            HfCard(modifier = Modifier.fillMaxWidth(), borderColor = Hf.Green) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(sh("KAZANDIKLARIN", "YOUR REWARDS"), color = Hf.Green, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    bankLines.forEach { Text(it, color = Hf.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                }
            }
        }

        if (!isPro) {
            if (storefront?.rewardedEnabled == false) item {
                Text(
                    sh("Video ödülleri çok yakında açılıyor. Hepsi aşağıda hazır.", "Video rewards open very soon. Everything is ready below."),
                    color = Hf.GoldDeep,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            item {
                Text(
                    sh("PRO hesabında reklam yok; video ödülleri PRO olmayan oyunculara açıktır. Önizleme:", "No ads on PRO; video rewards are for non-PRO players. Preview:"),
                    color = Hf.TextMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        // Every reward is listed for everyone; only non-PRO players can start a video.
        run {
            item {
                RewardVideoCard(
                    icon = "🪙",
                    title = sh("+${s?.coinPerAd ?: 10} SON COIN", "+${s?.coinPerAd ?: 10} SON COIN"),
                    description = sh("Her video Son Coin verir.", "Each video gives Son Coin."),
                    progress = sh("Bugün ${s?.coinAdsUsed ?: 0}/${s?.coinAdsLimit ?: 3}", "Today ${s?.coinAdsUsed ?: 0}/${s?.coinAdsLimit ?: 3}"),
                    enabled = !isPro && adReady && (s?.coinAdsUsed ?: 0) < (s?.coinAdsLimit ?: 3) && busy == null,
                    busy = busy == RewardKeys.COINS,
                    onClick = { showRewarded(RewardKeys.COINS) },
                )
            }
            item {
                val claimed = storefront?.dailyClaimed == true
                RewardVideoCard(
                    icon = "🎁",
                    title = sh("GÜNLÜK HEDİYE x2", "DAILY GIFT x2"),
                    description = if (claimed) sh("Bugünkü giriş hediyeni bir kez daha al.", "Get today's login gift once more.")
                        else sh("Önce bugünkü giriş hediyeni al, sonra ikiye katla.", "Claim today's login gift first, then double it."),
                    progress = periodLabel(RewardKeys.DAILY_DOUBLE),
                    enabled = !isPro && claimed && canWatch(RewardKeys.DAILY_DOUBLE),
                    busy = busy == RewardKeys.DAILY_DOUBLE,
                    onClick = { showRewarded(RewardKeys.DAILY_DOUBLE) },
                )
            }
            if (keyboards.isNotEmpty()) item {
                RewardVideoCard(
                    icon = "⌨",
                    title = sh("PREMIUM KLAVYE • 1 GÜN", "PREMIUM KEYBOARD • 1 DAY"),
                    description = sh("Seçtiğin klavye 24 saat boyunca tüm oyunlarda senin.", "The keyboard you pick is yours in every game for 24 hours."),
                    progress = periodLabel(RewardKeys.KEYBOARD_DAY),
                    enabled = !isPro && chosenKeyboard != null && passes?.keyboard == null && canWatch(RewardKeys.KEYBOARD_DAY),
                    busy = busy == RewardKeys.KEYBOARD_DAY,
                    onClick = { showRewarded(RewardKeys.KEYBOARD_DAY, chosenKeyboard) },
                ) {
                    RewardPicker(keyboards.map { it.id to itemName(it.id) }, chosenKeyboard) { keyboardPick = it }
                }
            }
            if (themes.isNotEmpty()) item {
                RewardVideoCard(
                    icon = "🎨",
                    title = sh("OYUN TEMASI • 1 GÜN", "GAME THEME • 1 DAY"),
                    description = sh("Seçtiğin tema 24 saat boyunca senin.", "The theme you pick is yours for 24 hours."),
                    progress = periodLabel(RewardKeys.THEME_DAY),
                    enabled = !isPro && chosenTheme != null && passes?.theme == null && canWatch(RewardKeys.THEME_DAY),
                    busy = busy == RewardKeys.THEME_DAY,
                    onClick = { showRewarded(RewardKeys.THEME_DAY, chosenTheme) },
                ) {
                    RewardPicker(themes.map { it.id to itemName(it.id) }, chosenTheme) { themePick = it }
                }
            }
            // Quick Duel is a paid mode: a video opens five games to try it.
            if (!seriesAccess || (passes?.quickGamesLeft ?: 0) > 0) item {
                RewardVideoCard(
                    icon = "⚡",
                    title = sh("5 HIZLI DÜELLO", "5 QUICK DUELS"),
                    description = sh("Kuşatma'nın Hızlı Düello modunu 5 maç boyunca aç (3 gün geçerli).", "Unlock Siege Quick Duel for 5 matches (valid 3 days)."),
                    progress = periodLabel(RewardKeys.QUICK_GAMES),
                    enabled = !isPro && !seriesAccess && canWatch(RewardKeys.QUICK_GAMES),
                    busy = busy == RewardKeys.QUICK_GAMES,
                    onClick = { showRewarded(RewardKeys.QUICK_GAMES) },
                )
            }
            // Mascot owners already get three hints every match.
            if (!mascotOwner) {
                listOf(
                    Triple(RewardKeys.HINTS_SON_HARF, "🔤", sh("SON HARF • +2 İPUCU", "LAST LETTER • +2 HINTS")),
                    Triple(RewardKeys.HINTS_SIEGE, "🏰", sh("KUŞATMA • +2 İPUCU", "SIEGE • +2 HINTS")),
                    Triple(RewardKeys.HINTS_WORKSHOP, "🛠", sh("KELİME ATÖLYESİ • +2 İPUCU", "WORD WORKSHOP • +2 HINTS")),
                ).forEach { (key, icon, title) ->
                    item(key = key) {
                        RewardVideoCard(
                            icon = icon,
                            title = title,
                            description = sh("İpuçları bankana eklenir, maçta ipucu düğmesiyle kullanırsın (7 gün geçerli).", "Hints go to your bank; use them with the hint button in a match (valid 7 days)."),
                            progress = periodLabel(key),
                            enabled = !isPro && canWatch(key),
                            busy = busy == key,
                            onClick = { showRewarded(key) },
                        )
                    }
                }
            }
        }

        item {
            HfCard(modifier = Modifier.fillMaxWidth(), borderColor = Hf.Gold.copy(alpha = .6f)) {
                Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(sh("🐷 KUMBARA", "🐷 PIGGY BANK"), color = Hf.GoldDeep, fontWeight = FontWeight.Black)
                        Text("${s?.piggyMatchProgress ?: 0}/${s?.piggyMatchTarget ?: 8}", color = Hf.Text, fontWeight = FontWeight.Black)
                    }
                    LinearProgressIndicator(
                        progress = { ((s?.piggyMatchProgress ?: 0).toFloat() / (s?.piggyMatchTarget ?: 8).coerceAtLeast(1)).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = Hf.Gold,
                        trackColor = Hf.Border,
                    )
                    Text(
                        sh(
                            "Tamamlanan maçlarla Kumbara dolar. Hazır olduğunda ${s?.piggyBonusSc ?: 0} Son Coin açılır.",
                            "Completed matches fill the Piggy Bank. When ready, ${s?.piggyBonusSc ?: 0} Son Coin opens.",
                        ),
                        color = Hf.TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    )
                    Button(
                        onClick = {
                            val b = backend ?: return@Button
                            scope.launch {
                                busy = "piggy"
                                runCatching { b.openPiggyBank() }
                                    .onSuccess { reward ->
                                        notice = sh("Kumbara açıldı: +${reward.bonusSc} Son Coin.", "Piggy Bank opened: +${reward.bonusSc} Son Coin.")
                                        reload()
                                    }
                                    .onFailure { notice = sh("Kumbara henüz hazır değil.", "The Piggy Bank is not ready yet.") }
                                busy = null
                            }
                        },
                        enabled = (s?.piggyBonusSc ?: 0) > 0 && busy == null,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Hf.Gold, contentColor = Color(0xFF3A2400)),
                    ) { Text(if (busy == "piggy") "…" else sh("KUMBARAYI AÇ", "OPEN PIGGY BANK"), fontWeight = FontWeight.Black) }
                }
            }
        }

        item {
            Text(
                sh(
                    "Hakların ve süreler sunucuda tutulur; cihaz saatini değiştirmek veya uygulamayı silmek bunları sıfırlamaz. Bir video yarım kalırsa ödül verilmez.",
                    "Limits and durations live on the server; changing the device clock or reinstalling does not reset them. An unfinished video gives no reward.",
                ),
                color = Hf.TextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun RewardVideoCard(
    icon: String,
    title: String,
    description: String,
    progress: String,
    enabled: Boolean,
    busy: Boolean,
    onClick: () -> Unit,
    extra: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, Hf.Gold.copy(alpha = .7f)),
        shadowElevation = 3.dp,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF16223D), Color(0xFF223457))))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 26.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = Color(0xFFFFF6E0), fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(description, color = Color(0xFFC9D3E3), fontSize = 12.sp, lineHeight = 16.sp)
                }
            }
            extra?.invoke()
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(progress, color = Color(0xFFF2C14E), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Button(
                    onClick = onClick,
                    enabled = enabled,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF2C14E),
                        contentColor = Color(0xFF3A2400),
                        disabledContainerColor = Color.White.copy(alpha = .12f),
                        disabledContentColor = Color.White.copy(alpha = .5f),
                    ),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                ) {
                    Text(if (busy) "…" else sh("▶ İZLE", "▶ WATCH"), fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun RewardPicker(options: List<Pair<String, String>>, selected: String?, onPick: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (id, name) ->
            val on = id == selected
            Surface(
                onClick = { onPick(id) },
                shape = RoundedCornerShape(50),
                color = if (on) Color(0xFFF2C14E) else Color.White.copy(alpha = .08f),
                border = BorderStroke(1.dp, if (on) Color(0xFFF2C14E) else Color.White.copy(alpha = .25f)),
            ) {
                Text(
                    name,
                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = if (on) Color(0xFF3A2400) else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
    }
}
