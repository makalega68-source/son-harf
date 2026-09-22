package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonharf.game.data.AchievementProgressDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.getAchievements

@Composable
internal fun ProfessionalProfileProgressScreen(
    backend: OnlineGameBackend,
    onBack: () -> Unit,
) {
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var achievements by remember { mutableStateOf<List<AchievementProgressDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        loading = true
        val id = backend.currentUserId()
        profile = id?.let { runCatching { backend.getProfile(it) }.getOrNull() }
        achievements = runCatching { backend.getAchievements() }.getOrDefault(emptyList())
        loading = false
    }

    val rating = profile?.rating ?: 1000
    val league = ratingLeagueProgress(rating)
    val unlocked = achievements.count { it.unlocked }

    Column(Modifier.fillMaxSize()) {
        GameTopBar(
            title = gameText("Başarımlar ve Lig", "Achievements & League"),
            subtitle = gameText("Prestij, rating ve başarı ilerlemen", "Your prestige, rating and achievement progress"),
            onBack = onBack,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = GameSpacing.ScreenHorizontal, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (loading) {
                item {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = GameColors.RewardAmber,
                        trackColor = GameColors.SecondarySurface,
                    )
                }
            }

            item {
                GameSurface(
                    elevated = true,
                    borderColor = GameColors.RewardAmber.copy(alpha = .40f),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = GameColors.RewardAmber.copy(alpha = .13f),
                        ) {
                            Icon(
                                Icons.Rounded.MilitaryTech,
                                contentDescription = null,
                                tint = GameColors.RewardAmber,
                                modifier = Modifier.padding(11.dp).size(26.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                league.leagueName,
                                color = GameColors.TextPrimary,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                            )
                            Text(
                                "$rating RP",
                                color = GameColors.RewardAmber,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                            )
                        }
                        LeagueBadge(league.leagueName)
                    }
                    Spacer(Modifier.height(12.dp))
                    LeagueProgress(league.progress)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (league.nextThreshold == null) {
                            gameText("En yüksek lige ulaştın.", "You reached the highest league.")
                        } else {
                            gameText(
                                "${league.nextThreshold - rating} RP sonra ${league.nextLeague}",
                                "${league.nextThreshold - rating} RP to ${league.nextLeague}",
                            )
                        },
                        color = GameColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ProgressMetric(
                        modifier = Modifier.weight(1f),
                        value = unlocked.toString(),
                        label = gameText("Açılan", "Unlocked"),
                        accent = GameColors.PlayGreen,
                    )
                    ProgressMetric(
                        modifier = Modifier.weight(1f),
                        value = achievements.size.toString(),
                        label = gameText("Toplam", "Total"),
                        accent = GameColors.PrimaryBlue,
                    )
                }
            }

            item { GameSectionHeader(gameText("Başarımlar", "Achievements")) }

            if (!loading && achievements.isEmpty()) {
                item {
                    GameEmptyState(
                        icon = Icons.Rounded.EmojiEvents,
                        title = gameText("Başarımlar hazırlanıyor", "Achievements are being prepared"),
                        body = gameText(
                            "Sunucudaki başarımlar hazır olduğunda burada görünecek.",
                            "Server achievements will appear here when available.",
                        ),
                    )
                }
            } else {
                items(achievements, key = { it.code }) { achievement ->
                    ProfessionalAchievementCard(achievement)
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun ProfessionalAchievementCard(achievement: AchievementProgressDto) {
    val accent = if (achievement.unlocked) GameColors.PlayGreen else GameColors.Lavender
    val current = achievement.currentValue.coerceAtMost(achievement.target)
    val progress = current.toFloat() / achievement.target.coerceAtLeast(1)

    GameSurface(borderColor = accent.copy(alpha = if (achievement.unlocked) .42f else .22f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = GameShapes.Medium, color = accent.copy(alpha = .12f)) {
                Box(
                    modifier = Modifier.padding(9.dp).size(22.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        achievement.icon.ifBlank { "★" },
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (SonHarfUiState.isEnglish) achievement.titleEn else achievement.titleTr,
                    color = GameColors.TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (SonHarfUiState.isEnglish) achievement.descriptionEn else achievement.descriptionTr,
                    color = GameColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (achievement.unlocked) {
                Icon(Icons.Rounded.Star, null, tint = GameColors.RewardAmber, modifier = Modifier.size(21.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        GameProgress(progress = progress, color = accent)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$current/${achievement.target}",
                color = GameColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
            Spacer(Modifier.weight(1f))
            if (achievement.rewardCoin > 0) {
                Text(
                    "+${achievement.rewardCoin} SC",
                    color = GameColors.RewardAmber,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@Composable
private fun ProgressMetric(
    modifier: Modifier,
    value: String,
    label: String,
    accent: Color,
) {
    Surface(
        modifier = modifier.heightIn(min = 82.dp),
        shape = GameShapes.Large,
        color = GameColors.PrimarySurface,
        border = BorderStroke(1.dp, GameColors.Border),
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(value, color = accent, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(label, color = GameColors.TextSecondary, style = MaterialTheme.typography.labelSmall)
        }
    }
}
