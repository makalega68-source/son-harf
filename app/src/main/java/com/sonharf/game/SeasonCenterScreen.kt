package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            .onFailure {
                notice = sh("Sezon bilgileri yüklenemedi.", "Season information could not be loaded.")
            }
        loading = false
    }

    LaunchedEffect(Unit) { reload() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(SonHarfBg),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SeasonCenterHero(season)
        }

        if (loading) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = SonHarfCyan) }
        } else {
            val currentSeason = season
            if (currentSeason == null || !currentSeason.active) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SonHarfSurface),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, SonHarfMuted.copy(alpha = .14f)),
                    ) {
                        Text(
                            sh("Şu anda aktif sezon bulunmuyor.", "There is no active season right now."),
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            color = SonHarfMuted,
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
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
                            color = SonHarfPurple.copy(alpha = .10f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, SonHarfPurple.copy(alpha = .28f)),
                        ) {
                            Text(
                                sh("✓ PREMIUM ÖDÜL YOLU AKTİF", "✓ PREMIUM REWARD TRACK ACTIVE"),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
                                color = SonHarfPurple,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }

                item {
                    Text(
                        sh(
                            "Premium ödüller yalnız kozmetik, prestij ve ilerleme içindir; maç gücü, rating, süre veya rekabet avantajı vermez.",
                            "Premium rewards are cosmetic, prestige and progression only; they never grant match power, rating, time or competitive advantages.",
                        ),
                        color = SonHarfGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
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
                Text(
                    notice,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    color = SonHarfMuted,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun SeasonCenterHero(season: StoreSeasonDto?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SonHarfSurface.copy(alpha = .97f)),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.2.dp, SonHarfCyan.copy(alpha = .26f)),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(sh("SEZON MERKEZİ", "SEASON CENTER"), color = SonHarfCyan, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text(
                        sh("Sunucu tarafından yönetilen Free + Premium ödül yolu", "Server-driven Free + Premium reward track"),
                        color = SonHarfMuted,
                        fontSize = 9.sp,
                    )
                }
                Text("🏆", fontSize = 32.sp)
            }
            if (season?.active == true) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SeasonStatChip(sh("SEVİYE", "LEVEL"), season.level.toString(), Modifier.weight(1f))
                    SeasonStatChip(sh("SÜRE", "DAYS"), season.durationDays.toString(), Modifier.weight(1f))
                    SeasonStatChip(sh("ÖDÜL", "REWARDS"), season.rewards.size.toString(), Modifier.weight(1f))
                }
                Text(
                    listOfNotNull(
                        season.seasonId.takeIf { it.isNotBlank() },
                        season.endsAt?.take(10)?.let { sh("Bitiş $it", "Ends $it") },
                    ).joinToString(" • "),
                    color = SonHarfMuted,
                    fontSize = 9.sp,
                )
            }
        }
    }
}

@Composable
private fun SeasonStatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = SonHarfCyan.copy(alpha = .09f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, SonHarfCyan.copy(alpha = .16f)),
    ) {
        Column(
            Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, color = SonHarfText, fontSize = 17.sp, fontWeight = FontWeight.Black)
            Text(label, color = SonHarfMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
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
    val accent = if (premium) SonHarfPurple else SonHarfCyan
    val canClaim = reward.unlocked && reward.premiumAccess && !reward.claimed
    val buttonText = when {
        reward.claimed -> sh("ALINDI", "CLAIMED")
        !reward.unlocked -> sh("SV. ${reward.level}", "LV. ${reward.level}")
        premium && !reward.premiumAccess -> "PREMIUM"
        busy -> "…"
        else -> sh("AL", "CLAIM")
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SonHarfSurface.copy(alpha = .97f)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = if (reward.claimed) .15f else .30f)),
    ) {
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
                    Surface(color = accent.copy(alpha = .12f), shape = RoundedCornerShape(14.dp)) {
                        Text(rewardIcon(reward.rewardType), Modifier.padding(10.dp), fontSize = 22.sp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(rewardTitle(reward), color = SonHarfText, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        Text(
                            if (premium) sh("Premium yol • Seviye ${reward.level}", "Premium track • Level ${reward.level}")
                            else sh("Ücretsiz yol • Seviye ${reward.level}", "Free track • Level ${reward.level}"),
                            color = accent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        if (!reward.unlocked) {
                            Text(
                                sh("Mevcut seviyen: $playerLevel", "Your current level: $playerLevel"),
                                color = SonHarfMuted,
                                fontSize = 8.sp,
                            )
                        }
                    }
                }
                if (reward.claimed) {
                    Text("✓", color = SonHarfGreen, fontWeight = FontWeight.Black, fontSize = 20.sp)
                }
            }

            Button(
                onClick = onClaim,
                enabled = canClaim && !busy && !anotherBusy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(buttonText, fontWeight = FontWeight.Black)
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

private fun rewardIcon(type: String): String = when (type) {
    "son_coin" -> "◈"
    "badge" -> "🏅"
    "title" -> "👑"
    "victory_effect", "word_effect" -> "✨"
    "vs_intro" -> "⚔"
    else -> "🎁"
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
