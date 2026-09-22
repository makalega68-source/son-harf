package com.sonharf.game

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
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
import com.sonharf.game.data.*
import kotlinx.coroutines.launch

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun ShopHubScreen() {
    EconomyShopScreen()
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
    val isPro = profile?.isVip == true
    val adsAllowed = profile?.isVip == false && AdPrivacyManager.adsAllowed
    var busy by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        val b = backend
        if (b == null) {
            notice = sh(
                "Ödül merkezi sunucu bağlantısı olmadan kullanılamaz.",
                "Reward Center requires a server connection.",
            )
            return
        }
        if (b.currentUserId() == null) {
            runCatching { b.ensurePlayer(sh("Oyuncu", "Player")) }
                .onFailure {
                    notice = sh(
                        "Oyuncu oturumu hazırlanamadı.",
                        "Player session is not ready.",
                    )
                    return
                }
        }
        runCatching {
            status = b.getRewardCenterStatus()
            items = b.getShopItems()
            profile = b.currentUserId()?.let { b.getProfile(it) }
        }.onFailure {
            notice = sh(
                "Ödül verileri yüklenemedi.",
                "Reward data could not be loaded.",
            )
        }
    }

    LaunchedEffect(Unit) { runCatching { reload() } }

    LaunchedEffect(adsAllowed) {
        if (adsAllowed) {
            adController.load { adReady = adController.ready }
        } else {
            adController.clear()
            adReady = false
        }
    }

    fun showRewarded(rewardType: String, trialItemId: String? = null) {
        if (isPro) {
            notice = sh(
                "PRO hesabında reklam gösterilmez.",
                "Ads are disabled on PRO accounts.",
            )
            return
        }
        val a = activity
        val b = backend
        if (a == null || busy != null) return
        if (b == null) {
            notice = sh(
                "Ödül merkezi şu anda çevrimdışı.",
                "Reward Center is currently offline.",
            )
            return
        }
        if (rewardType == "trial" && trialItemId.isNullOrBlank()) {
            notice = sh(
                "Şu anda denemeye uygun bir Style ürünü yok.",
                "No Style item is currently available for trial.",
            )
            return
        }

        busy = rewardType
        scope.launch {
            val intentId = runCatching {
                b.prepareStoreReward(rewardType, trialItemId)
            }.getOrElse {
                notice = sh(
                    "Ödüllü reklam şu anda kullanılamıyor.",
                    "Rewarded ads are currently unavailable.",
                )
                busy = null
                return@launch
            }

            adController.show(
                a,
                verificationUserId = b.currentUserId(),
                verificationData = intentId,
                onEarned = { responseId ->
                    scope.launch {
                        runCatching {
                            b.awaitVerifiedStoreReward(
                                rewardType,
                                responseId,
                                trialItemId,
                            )
                        }
                            .onSuccess { claim ->
                                notice = when (rewardType) {
                                    "diamonds" -> sh(
                                        "+${claim.diamondsAwarded.takeIf { it > 0 } ?: (status?.coinPerAd ?: 10)} Son Coin hesabına eklendi.",
                                        "+${claim.diamondsAwarded.takeIf { it > 0 } ?: (status?.coinPerAd ?: 10)} Son Coin added.",
                                    )
                                    else -> sh(
                                        "Style denemen başladı.",
                                        "Your Style trial has started.",
                                    )
                                }
                                reload()
                            }
                            .onFailure { error ->
                                notice = when {
                                    "daily_limit_reached" in error.message.orEmpty() -> sh(
                                        "Bugünkü kota tamamlandı.",
                                        "Today's quota is complete.",
                                    )
                                    "ad_verification_pending" in error.message.orEmpty() -> sh(
                                        "Ödül doğrulandığında hesabına eklenecek.",
                                        "Your reward will be added after verification.",
                                    )
                                    "trial_item_unavailable" in error.message.orEmpty() -> sh(
                                        "Bu deneme ürünü artık kullanılamıyor.",
                                        "This trial item is no longer available.",
                                    )
                                    else -> sh(
                                        "Ödül işlenemedi.",
                                        "Reward could not be processed.",
                                    )
                                }
                            }
                        busy = null
                        adReady = adController.ready
                    }
                },
                onUnavailable = {
                    notice = sh(
                        "Reklam şu an hazır değil. Daha sonra tekrar dene.",
                        "The ad is not ready. Try again later.",
                    )
                    busy = null
                    adReady = false
                },
                onClosed = {
                    busy = null
                    if (adsAllowed) {
                        adController.load { adReady = adController.ready }
                    } else {
                        adController.clear()
                        adReady = false
                    }
                },
            )
        }
    }

    val s = status
    val activeTrialItem = items.firstOrNull { it.id == s?.trialItemId }
    val trialCandidate = items.firstOrNull {
        !it.trialMode.isNullOrBlank() && (it.trialValue ?: 0) > 0
    }
    val trialCandidateName = trialCandidate?.let {
        if (SonHarfUiState.isEnglish) it.nameEn else it.nameTr
    }
    val trialDescription = trialCandidate?.let {
        when (it.trialMode) {
            "minutes" -> sh(
                "${it.trialValue ?: 0} dakika Style denemesi",
                "${it.trialValue ?: 0}-minute Style trial",
            )
            "match" -> sh(
                "${it.trialValue ?: 1} maçlık Style denemesi",
                "${it.trialValue ?: 1}-match Style trial",
            )
            else -> sh("Style denemesi", "Style trial")
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = GameSpacing.ScreenHorizontal,
            vertical = 12.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            GameSurface(
                elevated = true,
                borderColor = GameColors.PrimaryBlue.copy(alpha = .32f),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        shape = GameShapes.Medium,
                        color = GameColors.RewardAmber.copy(alpha = .12f),
                    ) {
                        Icon(
                            Icons.Rounded.CardGiftcard,
                            contentDescription = null,
                            tint = GameColors.RewardAmber,
                            modifier = Modifier.padding(9.dp).size(23.dp),
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            sh("KELİME KUŞATMASI ÖDÜLLERİ", "WORD SIEGE REWARDS"),
                            color = GameColors.TextPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            if (isPro) {
                                sh(
                                    "PRO hesabında reklam gösterilmez. Kumbara ve sunucu kontrollü ilerleme sistemleri devam eder.",
                                    "Ads are disabled on PRO. Piggy Bank and server-controlled progression continue normally.",
                                )
                            } else {
                                sh(
                                    "Ödüllü reklamlar isteğe bağlıdır. Maçlarda ve oyun alanında reklam yoktur.",
                                    "Rewarded ads are optional. Matches and gameplay remain ad-free.",
                                )
                            },
                            color = GameColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        if (!isPro) {
            item {
                RewardAdCard(
                    icon = Icons.Rounded.Toll,
                    title = "SON COIN",
                    description = sh(
                        "Her tamamlanan reklam +${s?.coinPerAd ?: 10} Son Coin verir. Günlük kota sunucu tarafından tutulur.",
                        "Each completed ad gives +${s?.coinPerAd ?: 10} Son Coin. The daily quota is enforced by the server.",
                    ),
                    progress = "${s?.coinAdsUsed ?: 0}/${s?.coinAdsLimit ?: 3}",
                    button = sh("REKLAM İZLE", "WATCH AD"),
                    accent = GameColors.RewardAmber,
                    enabled = adReady &&
                        (s?.coinAdsUsed ?: 0) < (s?.coinAdsLimit ?: 3) &&
                        busy == null,
                    onClick = { showRewarded("diamonds") },
                )
            }

            item {
                RewardAdCard(
                    icon = Icons.Rounded.AutoAwesome,
                    title = sh("STYLE DENEME", "STYLE TRIAL"),
                    description = listOfNotNull(
                        trialCandidateName,
                        trialDescription,
                    ).joinToString(" • ").ifBlank {
                        sh(
                            "Sunucu kataloğundaki uygun bir Style ürününü dene.",
                            "Try an eligible Style item from the server catalog.",
                        )
                    },
                    progress = "${s?.trialAdsUsed ?: 0}/${s?.trialAdsLimit ?: 1}",
                    button = sh("DENEMEYİ BAŞLAT", "START TRIAL"),
                    accent = GameColors.Lavender,
                    enabled = adReady &&
                        trialCandidate != null &&
                        (s?.trialAdsUsed ?: 0) < (s?.trialAdsLimit ?: 1) &&
                        busy == null,
                    onClick = { showRewarded("trial", trialCandidate?.id) },
                )
            }
        }

        if (s?.trialItemId != null) {
            item {
                GameSurface(
                    borderColor = GameColors.Lavender.copy(alpha = .38f),
                ) {
                    Text(
                        sh("AKTİF DENEME", "ACTIVE TRIAL"),
                        color = GameColors.Lavender,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        if (SonHarfUiState.isEnglish) {
                            activeTrialItem?.nameEn ?: s.trialItemId
                        } else {
                            activeTrialItem?.nameTr ?: s.trialItemId
                        },
                        color = GameColors.TextPrimary,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    val remaining = when (s.trialMode) {
                        "match" -> sh(
                            "Kalan maç: ${s.trialMatchesRemaining ?: 0}",
                            "Matches left: ${s.trialMatchesRemaining ?: 0}",
                        )
                        "minutes" -> s.trialExpiresAt.orEmpty()
                        else -> s.trialExpiresAt.orEmpty()
                    }
                    if (remaining.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            remaining,
                            color = GameColors.TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val b = backend ?: return@Button
                            scope.launch {
                                busy = "equip_trial"
                                runCatching { b.equipRewardTrial() }
                                    .onSuccess {
                                        notice = sh(
                                            "Deneme Style ürünü etkinleştirildi.",
                                            "Trial Style item equipped.",
                                        )
                                        reload()
                                    }
                                    .onFailure {
                                        notice = sh(
                                            "Deneme artık aktif değil.",
                                            "The trial is no longer active.",
                                        )
                                        reload()
                                    }
                                busy = null
                            }
                        },
                        enabled = busy == null,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GameColors.Lavender,
                            contentColor = GameColors.TextPrimary,
                            disabledContainerColor = GameColors.Disabled,
                            disabledContentColor = GameColors.DisabledContent,
                        ),
                        shape = GameShapes.Medium,
                    ) {
                        Text(
                            if (busy == "equip_trial") {
                                "…"
                            } else {
                                sh("DENEMEYİ KULLAN", "USE TRIAL")
                            },
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        }

        item {
            GameSurface(
                borderColor = GameColors.RewardAmber.copy(alpha = .34f),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            shape = GameShapes.Small,
                            color = GameColors.RewardAmber.copy(alpha = .11f),
                        ) {
                            Icon(
                                Icons.Rounded.Savings,
                                contentDescription = null,
                                tint = GameColors.RewardAmber,
                                modifier = Modifier.padding(7.dp).size(18.dp),
                            )
                        }
                        Text(
                            sh("KUMBARA", "PIGGY BANK"),
                            color = GameColors.TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    Text(
                        "${s?.piggyMatchProgress ?: 0}/${s?.piggyMatchTarget ?: 8}",
                        color = GameColors.RewardAmber,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                    )
                }

                Spacer(Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = {
                        ((s?.piggyMatchProgress ?: 0).toFloat() /
                            (s?.piggyMatchTarget ?: 8).coerceAtLeast(1))
                            .coerceIn(0f, 1f)
                    },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = GameColors.RewardAmber,
                    trackColor = GameColors.SecondarySurface,
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    sh(
                        "Tamamlanan maçlarla Kumbara dolar. Hazır olduğunda ${s?.piggyBonusSc ?: 0} Son Coin sunucu tarafından doğrulanarak açılır.",
                        "Completed matches fill the Piggy Bank. When ready, ${s?.piggyBonusSc ?: 0} Son Coin is verified and granted by the server.",
                    ),
                    color = GameColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )

                Spacer(Modifier.height(10.dp))

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
                                .onFailure {
                                    notice = sh(
                                        "Kumbara henüz hazır değil.",
                                        "The Piggy Bank is not ready yet.",
                                    )
                                }
                            busy = null
                        }
                    },
                    enabled = (s?.piggyBonusSc ?: 0) > 0 && busy == null,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GameColors.RewardAmber,
                        contentColor = GameColors.AppBackground,
                        disabledContainerColor = GameColors.Disabled,
                        disabledContentColor = GameColors.DisabledContent,
                    ),
                    shape = GameShapes.Medium,
                ) {
                    Text(
                        if (busy == "piggy") {
                            "…"
                        } else {
                            sh("KUMBARAYI AÇ", "OPEN PIGGY BANK")
                        },
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }

        item {
            GameSurface(borderColor = GameColors.Border) {
                Text(
                    sh("SUNUCU KONTROLLÜ ÖDÜLLER", "SERVER-CONTROLLED REWARDS"),
                    color = GameColors.PrimaryBlue,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    sh(
                        "Günlük kotalar, deneme süresi ve Kumbara ilerlemesi sunucuda tutulur; cihaz saatini değiştirmek veya uygulamayı silmek bunları sıfırlamaz.",
                        "Daily quotas, trial duration and Piggy Bank progress are stored on the server; changing device time or reinstalling the app does not reset them.",
                    ),
                    color = GameColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                CurrencyChip(profile?.diamonds ?: 0)
            }
        }

        if (!notice.isNullOrBlank()) {
            item {
                Surface(
                    color = GameColors.SecondarySurface,
                    shape = GameShapes.Medium,
                    border = BorderStroke(1.dp, GameColors.Border),
                ) {
                    Text(
                        notice!!,
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        color = GameColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun RewardAdCard(
    icon: ImageVector,
    title: String,
    description: String,
    progress: String,
    button: String,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    GameSurface(
        borderColor = accent.copy(alpha = .30f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = GameShapes.Medium,
                    color = accent.copy(alpha = .11f),
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.padding(9.dp).size(22.dp),
                    )
                }
                Text(
                    title,
                    color = GameColors.TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                )
            }
            Text(
                progress,
                color = accent,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            description,
            color = GameColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accent,
                contentColor = GameColors.TextPrimary,
                disabledContainerColor = GameColors.Disabled,
                disabledContentColor = GameColors.DisabledContent,
            ),
            shape = GameShapes.Medium,
        ) {
            Text(button, fontWeight = FontWeight.Black)
        }
    }
}
