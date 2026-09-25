package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
            notice = sh(
                "Sezon Merkezi sunucu bağlantısı olmadan kullanılamaz.",
                "Season Center requires a server connection.",
            )
            loading = false
            return
        }
        runCatching { b.getStoreSeason() }
            .onSuccess { season = it }
            .onFailure {
                notice = sh(
                    "Sezon bilgileri yüklenemedi.",
                    "Season information could not be loaded.",
                )
            }
        loading = false
    }

    LaunchedEffect(Unit) { reload() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = GameSpacing.ScreenHorizontal,
            vertical = 10.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SeasonCenterHero(season) }

        if (loading) {
            item {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(5.dp),
                    color = GameColors.PrimaryBlue,
                    trackColor = GameColors.SecondarySurface,
                )
            }
        } else {
            val currentSeason = season
            if (currentSeason == null || !currentSeason.active) {
                item {
                    GameEmptyState(
                        icon = Icons.Rounded.EventBusy,
                        title = sh("Aktif sezon yok", "No active season"),
                        body = sh(
                            "Yeni sezon başladığında ödül yolu burada görünecek.",
                            "The reward track will appear here when a new season begins.",
                        ),
                    )
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
                            color = GameColors.Lavender.copy(alpha = .10f),
                            shape = GameShapes.Medium,
                            border = BorderStroke(
                                1.dp,
                                GameColors.Lavender.copy(alpha = .30f),
                            ),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = GameColors.Lavender,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(7.dp))
                                Text(
                                    sh(
                                        "PREMIUM ÖDÜL YOLU AKTİF",
                                        "PREMIUM REWARD TRACK ACTIVE",
                                    ),
                                    color = GameColors.Lavender,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                )
                            }
                        }
                    }
                }

                item {
                    Surface(
                        shape = GameShapes.Medium,
                        color = GameColors.PlayGreen.copy(alpha = .08f),
                        border = BorderStroke(
                            1.dp,
                            GameColors.PlayGreen.copy(alpha = .28f),
                        ),
                    ) {
                        Text(
                            sh(
                                "Premium ödüller yalnız kozmetik, prestij ve ilerleme içindir; maç gücü, rating, süre veya rekabet avantajı vermez.",
                                "Premium rewards are cosmetic, prestige and progression only; they never grant match power, rating, time or competitive advantages.",
                            ),
                            modifier = Modifier.fillMaxWidth().padding(11.dp),
                            color = GameColors.PlayGreen,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
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
                                    notice = if (reward.rewardType == "son_coin") {
                                        sh(
                                            "Sezon ödülü Son Coin bakiyene eklendi.",
                                            "Season reward was added to your Son Coin balance.",
                                        )
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
                Surface(
                    shape = GameShapes.Small,
                    color = GameColors.SecondarySurface,
                    border = BorderStroke(1.dp, GameColors.Border),
                ) {
                    Text(
                        notice,
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
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
private fun SeasonCenterHero(season: StoreSeasonDto?) {
    GameSurface(
        elevated = true,
        borderColor = GameColors.PrimaryBlue.copy(alpha = .34f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    sh("SEZON MERKEZİ", "SEASON CENTER"),
                    color = GameColors.TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    sh(
                        "Free + Premium ödül yolu",
                        "Free + Premium reward track",
                    ),
                    color = GameColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Surface(
                shape = GameShapes.Medium,
                color = GameColors.RewardAmber.copy(alpha = .12f),
            ) {
                Icon(
                    Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = GameColors.RewardAmber,
                    modifier = Modifier.padding(10.dp).size(28.dp),
                )
            }
        }

        if (season?.active == true) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SeasonStatChip(
                    sh("SEVİYE", "LEVEL"),
                    season.level.toString(),
                    Modifier.weight(1f),
                )
                SeasonStatChip(
                    sh("SÜRE", "DAYS"),
                    season.durationDays.toString(),
                    Modifier.weight(1f),
                )
                SeasonStatChip(
                    sh("ÖDÜL", "REWARDS"),
                    season.rewards.size.toString(),
                    Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                listOfNotNull(
                    season.seasonId.takeIf { it.isNotBlank() },
                    season.endsAt?.take(10)?.let { sh("Bitiş $it", "Ends $it") },
                ).joinToString(" • "),
                color = GameColors.TextTertiary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun SeasonStatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = GameColors.PrimaryBlue.copy(alpha = .09f),
        shape = GameShapes.Medium,
        border = BorderStroke(
            1.dp,
            GameColors.PrimaryBlue.copy(alpha = .18f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                value,
                color = GameColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
            )
            Text(
                label,
                color = GameColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
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
    val accent = if (premium) GameColors.Lavender else GameColors.PrimaryBlue
    val canClaim = reward.unlocked && reward.premiumAccess && !reward.claimed
    val buttonText = when {
        reward.claimed -> sh("ALINDI", "CLAIMED")
        !reward.unlocked -> sh("SV. ${reward.level}", "LV. ${reward.level}")
        premium && !reward.premiumAccess -> "PREMIUM"
        busy -> "…"
        else -> sh("AL", "CLAIM")
    }

    GameSurface(
        borderColor = accent.copy(alpha = if (reward.claimed) .18f else .34f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = accent.copy(alpha = .12f),
                    shape = GameShapes.Medium,
                ) {
                    Icon(
                        rewardIcon(reward.rewardType),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.padding(10.dp).size(24.dp),
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        rewardTitle(reward),
                        color = GameColors.TextPrimary,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (premium) {
                            sh(
                                "Premium yol • Seviye ${reward.level}",
                                "Premium track • Level ${reward.level}",
                            )
                        } else {
                            sh(
                                "Ücretsiz yol • Seviye ${reward.level}",
                                "Free track • Level ${reward.level}",
                            )
                        },
                        color = accent,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    if (!reward.unlocked) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            sh(
                                "Mevcut seviyen: $playerLevel",
                                "Your current level: $playerLevel",
                            ),
                            color = GameColors.TextTertiary,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
            if (reward.claimed) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = GameColors.PlayGreen,
                    modifier = Modifier.size(21.dp),
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = onClaim,
            enabled = canClaim && !busy && !anotherBusy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accent,
                contentColor = GameColors.TextPrimary,
                disabledContainerColor = GameColors.Disabled,
                disabledContentColor = GameColors.DisabledContent,
            ),
            shape = GameShapes.Medium,
        ) {
            Text(buttonText, fontWeight = FontWeight.Black)
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

private fun rewardIcon(type: String): ImageVector = when (type) {
    "son_coin" -> Icons.Rounded.Toll
    "badge" -> Icons.Rounded.MilitaryTech
    "title" -> Icons.Rounded.WorkspacePremium
    "victory_effect", "word_effect" -> Icons.Rounded.AutoAwesome
    "vs_intro" -> Icons.Rounded.SportsEsports
    "profile_frame", "nameplate" -> Icons.Rounded.AccountBox
    else -> Icons.Rounded.CardGiftcard
}

private fun seasonClaimError(error: Throwable?): String {
    val message = error?.message.orEmpty()
    return when {
        "already_claimed" in message -> sh(
            "Bu ödül zaten alındı.",
            "This reward has already been claimed.",
        )
        "reward_locked" in message -> sh(
            "Bu ödül için gereken seviyeye henüz ulaşmadın.",
            "You have not reached the required level yet.",
        )
        "season_pass_required" in message -> sh(
            "Bu ödül için aktif Sezon Bileti gerekiyor.",
            "An active Season Pass is required for this reward.",
        )
        "season_inactive" in message -> sh(
            "Aktif sezon sona ermiş olabilir.",
            "The active season may have ended.",
        )
        else -> sh(
            "Sezon ödülü alınamadı. Tekrar deneyebilirsin.",
            "Season reward could not be claimed. You can retry.",
        )
    }
}
