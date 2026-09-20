package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SeasonRewardDto
import com.sonharf.game.data.StoreSeasonDto
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.claimStoreSeasonReward
import com.sonharf.game.data.getStoreSeason
import kotlinx.coroutines.launch

@Composable
fun SeasonCenterContent() {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    var season by remember { mutableStateOf<StoreSeasonDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var busyReward by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf("") }
    var rewardVfxKey by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        val b = backend
        if (b == null) {
            season = null
            notice = sh("Sezon Merkezi sunucu bağlantısı olmadan kullanılamaz.", "Season Center requires a server connection.")
            loading = false
            return
        }
        runCatching { b.getStoreSeason() }
            .onSuccess { season = it }
            .onFailure { notice = sh("Sezon bilgileri yüklenemedi.", "Season information could not be loaded.") }
        loading = false
    }

    LaunchedEffect(Unit) { reload() }

    Box(Modifier.fillMaxSize().background(MainUi.Background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SeasonCenterHero(season) }

            if (loading) {
                item {
                    LinearProgressIndicator(
                        Modifier.fillMaxWidth(),
                        color = MainUi.Blue,
                        trackColor = MainUi.SurfaceRaised,
                    )
                }
            } else {
                val currentSeason = season
                if (currentSeason == null || !currentSeason.active) {
                    item {
                        MainGameCard {
                            Text(
                                sh("Şu anda aktif sezon bulunmuyor.", "There is no active season right now."),
                                modifier = Modifier.fillMaxWidth().padding(22.dp),
                                color = MainUi.Muted,
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp,
                            )
                        }
                    }
                } else {
                    if (!currentSeason.premiumActive) {
                        item {
                            SeasonPassPurchaseCard {
                                scope.launch {
                                    loading = true
                                    reload()
                                }
                            }
                        }
                    } else {
                        item {
                            Surface(
                                color = MainUi.GoldSoft,
                                shape = MainUiShape.Control,
                                border = BorderStroke(1.dp, MainUi.Gold.copy(alpha = .32f)),
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Rounded.Verified, null, tint = MainUi.Gold, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(7.dp))
                                    Text(
                                        sh("PREMIUM ÖDÜL YOLU AKTİF", "PREMIUM REWARD TRACK ACTIVE"),
                                        color = MainUi.Text,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Surface(
                            shape = MainUiShape.Control,
                            color = MainUi.GreenSoft,
                            border = BorderStroke(1.dp, MainUi.Green.copy(alpha = .18f)),
                        ) {
                            Text(
                                sh(
                                    "Premium ödüller kozmetik, prestij ve ilerleme içindir; maç gücü, rating, süre veya rekabet avantajı vermez.",
                                    "Premium rewards are cosmetic, prestige and progression only; they never grant match power, rating, time or competitive advantages.",
                                ),
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                                color = MainUi.Green,
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    items(
                        items = currentSeason.rewards,
                        key = { reward -> rewardIdentity(reward) },
                    ) { reward ->
                        SeasonRewardCard(
                            reward = reward,
                            playerLevel = currentSeason.level,
                            busy = busyReward == rewardIdentity(reward),
                            anotherBusy = busyReward != null && busyReward != rewardIdentity(reward),
                            onClaim = {
                                val b = backend ?: return@SeasonRewardCard
                                val identity = rewardIdentity(reward)
                                scope.launch {
                                    busyReward = identity
                                    notice = ""
                                    val result = runCatching { b.claimStoreSeasonReward(reward) }
                                    if (result.isSuccess) {
                                        rewardVfxKey = "$identity:${System.nanoTime()}"
                                        SonHarfSoundFx.reward()
                                        notice = if (reward.rewardType == "son_coin") {
                                            sh("Sezon ödülü Son Coin bakiyene eklendi.", "Season reward was added to your Son Coin balance.")
                                        } else {
                                            sh(
                                                "Sezon Style ödülü kalıcı koleksiyonuna eklendi; Profil > Style alanından kullanabilirsin.",
                                                "Season Style reward was added to your permanent collection; equip it from Profile > Style.",
                                            )
                                        }
                                        reload()
                                    } else {
                                        notice = seasonClaimError(result.exceptionOrNull())
                                        SonHarfSoundFx.warning()
                                    }
                                    busyReward = null
                                }
                            },
                        )
                    }
                }
            }

            if (notice.isNotBlank()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MainUiShape.Control,
                        color = MainUi.Surface,
                        border = BorderStroke(1.dp, MainUi.Border),
                    ) {
                        Text(
                            notice,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            color = MainUi.Muted,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        rewardVfxKey?.let { key ->
            PurchasedMomentVfx(
                eventKey = "season-reward:$key",
                kind = PurchasedMomentVfxKind.REWARD,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun SeasonCenterHero(season: StoreSeasonDto?) {
    MainGameCard(elevated = true) {
        Column(
            Modifier.fillMaxWidth().padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(sh("SEZON MERKEZİ", "SEASON CENTER"), color = MainUi.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        sh("Ücretsiz ve Premium ödül yolu", "Free and Premium reward track"),
                        color = MainUi.Muted,
                        fontSize = 11.sp,
                    )
                }
                Surface(shape = MainUiShape.Control, color = MainUi.GoldSoft) {
                    Icon(
                        Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = MainUi.Gold,
                        modifier = Modifier.padding(11.dp).size(25.dp),
                    )
                }
            }
            if (season?.active == true) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SeasonStatChip(sh("SEVİYE", "LEVEL"), season.level.toString(), Modifier.weight(1f))
                    SeasonStatChip(sh("SÜRE", "DAYS"), season.durationDays.toString(), Modifier.weight(1f))
                    SeasonStatChip(sh("ÖDÜL", "REWARDS"), season.rewards.size.toString(), Modifier.weight(1f))
                }
                val meta = listOfNotNull(
                    season.seasonId.takeIf { it.isNotBlank() },
                    season.endsAt?.take(10)?.let { sh("Bitiş $it", "Ends $it") },
                ).joinToString(" • ")
                if (meta.isNotBlank()) Text(meta, color = MainUi.Muted, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun SeasonStatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MainUi.BlueSoft,
        shape = MainUiShape.Control,
        border = BorderStroke(1.dp, MainUi.Blue.copy(alpha = .14f)),
    ) {
        Column(
            Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, color = MainUi.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(label, color = MainUi.Muted, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SeasonRewardCard(
    reward: SeasonRewardDto,
    playerLevel: Int,
    busy: Boolean,
    anotherBusy: Boolean,
    onClaim: () -> Unit,
) {
    val premium = reward.track.equals("premium", ignoreCase = true)
    val accent = if (premium) MainUi.Gold else MainUi.Blue
    val accentSoft = if (premium) MainUi.GoldSoft else MainUi.BlueSoft
    val canClaim = reward.unlocked && reward.premiumAccess && !reward.claimed
    val buttonText = when {
        reward.claimed -> sh("ALINDI", "CLAIMED")
        !reward.unlocked -> sh("SV. ${reward.level}", "LV. ${reward.level}")
        premium && !reward.premiumAccess -> "PREMIUM"
        busy -> "…"
        else -> sh("AL", "CLAIM")
    }

    MainGameCard {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = accentSoft, shape = MainUiShape.Control) {
                        Icon(
                            seasonRewardIcon(reward.rewardType),
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.padding(10.dp).size(22.dp),
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(rewardTitle(reward), color = MainUi.Text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            if (premium) sh("Premium yol • Seviye ${reward.level}", "Premium track • Level ${reward.level}")
                            else sh("Ücretsiz yol • Seviye ${reward.level}", "Free track • Level ${reward.level}"),
                            color = accent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (!reward.unlocked) {
                            Text(
                                sh("Mevcut seviyen: $playerLevel", "Your current level: $playerLevel"),
                                color = MainUi.Muted,
                                fontSize = 8.sp,
                            )
                        }
                    }
                }
                if (reward.claimed) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = MainUi.Green, modifier = Modifier.size(21.dp))
                }
            }

            Button(
                onClick = onClaim,
                enabled = canClaim && !busy && !anotherBusy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = if (premium) Color(0xFF2B2418) else Color.White,
                    disabledContainerColor = MainUi.SurfaceRaised,
                    disabledContentColor = MainUi.Muted,
                ),
                shape = MainUiShape.Control,
            ) {
                Text(buttonText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun rewardIdentity(reward: SeasonRewardDto): String =
    "${reward.level}:${reward.track}:${reward.rewardType}:${reward.rewardKey}"

private fun rewardTitle(reward: SeasonRewardDto): String = when (reward.rewardType) {
    "son_coin" -> sh("${reward.amount} SON COIN", "${reward.amount} SON COINS")
    "profile_frame" -> sh("PROFİL ÇERÇEVESİ", "PROFILE FRAME")
    "badge" -> sh("SEZON ROZETİ", "SEASON BADGE")
    "title" -> sh("SEZON UNVANI", "SEASON TITLE")
    "nameplate" -> sh("PROFİL PLAKASI", "PROFILE NAMEPLATE")
    "victory_effect" -> sh("ZAFER EFEKTİ", "VICTORY EFFECT")
    "word_effect" -> sh("KELİME EFEKTİ", "WORD EFFECT")
    "vs_intro" -> sh("VS GİRİŞİ", "VS INTRO")
    "final_style" -> sh("SEZON FİNAL STYLE", "SEASON FINAL STYLE")
    else -> sh("SEZON ÖDÜLÜ", "SEASON REWARD")
}

private fun seasonRewardIcon(type: String): ImageVector = when (type) {
    "son_coin" -> Icons.Rounded.Paid
    "badge" -> Icons.Rounded.MilitaryTech
    "title", "nameplate" -> Icons.Rounded.WorkspacePremium
    "victory_effect", "word_effect", "final_style" -> Icons.Rounded.AutoAwesome
    "vs_intro" -> Icons.Rounded.SportsScore
    else -> Icons.Rounded.CardGiftcard
}

private fun seasonClaimError(error: Throwable?): String {
    val message = error?.message.orEmpty()
    return when {
        "already_claimed" in message -> sh("Bu ödül zaten alındı.", "This reward has already been claimed.")
        "reward_locked" in message -> sh("Bu ödül için gereken seviyeye henüz ulaşmadın.", "You have not reached the required level yet.")
        "season_pass_required" in message -> sh("Bu ödül için aktif Sezon Bileti gerekiyor.", "An active Season Pass is required for this reward.")
        "season_inactive" in message -> sh("Aktif sezon sona ermiş olabilir.", "The active season may have ended.")
        else -> sh("Sezon ödülü alınamadı. Tekrar deneyebilirsin.", "Season reward could not be claimed. You can retry.")
    }
}
