package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private enum class ProfessionalRetentionTab { MISSIONS, DAILY_REWARD }

@Composable
internal fun ProfessionalRetentionScreen(
    backend: OnlineGameBackend,
    onBack: () -> Unit,
    onPlay: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(ProfessionalRetentionTab.MISSIONS) }
    var growth by remember { mutableStateOf<GrowthDashboardDto?>(null) }
    var meta by remember { mutableStateOf<MetaProgressV2Dto?>(null) }
    var missions by remember { mutableStateOf<List<UnifiedMissionDto>>(emptyList()) }
    var goals by remember { mutableStateOf<List<GoalRowDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busyKey by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    suspend fun reload() = coroutineScope {
        loading = true
        val growthTask = async { runCatching { backend.getGrowthDashboard() }.getOrNull() }
        val metaTask = async { runCatching { backend.getMetaProgressV2() }.getOrNull() }
        val missionTask = async { runCatching { backend.getUnifiedMissions() }.getOrDefault(emptyList()) }
        val goalTask = async { runCatching { backend.getGoals() }.getOrDefault(emptyList()) }
        growth = growthTask.await()
        meta = metaTask.await()
        missions = missionTask.await()
        goals = goalTask.await()
        loading = false
    }

    LaunchedEffect(Unit) { reload() }

    Column(Modifier.fillMaxSize()) {
        GameTopBar(
            title = gameText("Günlük Merkez", "Daily Center"),
            subtitle = gameText("Görevlerini tamamla, serini koru ve ödülünü al", "Complete missions, keep your streak and claim rewards"),
            onBack = onBack,
        )

        SegmentedGameTabs(
            labels = listOf(
                gameText("GÖREVLER", "MISSIONS"),
                gameText("GÜNLÜK ÖDÜL", "DAILY REWARD"),
            ),
            selectedIndex = tab.ordinal,
            onSelected = { tab = ProfessionalRetentionTab.entries[it] },
            modifier = Modifier.padding(horizontal = GameSpacing.ScreenHorizontal, vertical = 6.dp),
        )

        Box(Modifier.weight(1f)) {
            when (tab) {
                ProfessionalRetentionTab.MISSIONS -> ProfessionalMissionTab(
                    growth = growth,
                    missions = missions,
                    goals = goals,
                    loading = loading,
                    busyKey = busyKey,
                    notice = notice,
                    onPlay = onPlay,
                    onClaimDailyChallenge = {
                        if (busyKey != null) return@ProfessionalMissionTab
                        scope.launch {
                            busyKey = "daily_challenge"
                            val reward = runCatching { backend.claimDailyChallenge() }.getOrDefault(0)
                            notice = if (reward > 0) {
                                gameText("+$reward Son Coin kazandın.", "You earned +$reward Son Coins.")
                            } else {
                                gameText("Görev henüz tamamlanmadı veya ödül alındı.", "Mission is incomplete or already claimed.")
                            }
                            reload()
                            busyKey = null
                        }
                    },
                    onClaimMission = { mission ->
                        if (busyKey != null) return@ProfessionalMissionTab
                        scope.launch {
                            busyKey = mission.missionId
                            runCatching { backend.claimUnifiedMission(mission.missionId) }
                                .onSuccess {
                                    notice = gameText("+${it.rewardCoins} Son Coin alındı.", "+${it.rewardCoins} Son Coins claimed.")
                                    SonHarfSoundFx.missionComplete()
                                    reload()
                                }
                                .onFailure {
                                    notice = gameText("Görev ödülü alınamadı.", "Mission reward could not be claimed.")
                                }
                            busyKey = null
                        }
                    },
                    onClaimGoal = { goal ->
                        if (busyKey != null) return@ProfessionalMissionTab
                        scope.launch {
                            busyKey = goal.id
                            val reward = runCatching { backend.claimGoal(goal.id) }.getOrDefault(0)
                            notice = if (reward > 0) {
                                gameText("+$reward Son Coin alındı.", "+$reward Son Coins claimed.")
                            } else {
                                gameText("Görev ödülü alınamadı.", "Mission reward could not be claimed.")
                            }
                            reload()
                            busyKey = null
                        }
                    },
                )

                ProfessionalRetentionTab.DAILY_REWARD -> ProfessionalDailyRewardTab(
                    growth = growth,
                    meta = meta,
                    loading = loading,
                    busy = busyKey != null,
                    notice = notice,
                    onClaim = {
                        if (busyKey != null || growth?.dailyClaimed == true) return@ProfessionalDailyRewardTab
                        scope.launch {
                            busyKey = "checkin"
                            val reward = runCatching { backend.claimDailyCheckin() }.getOrDefault(0)
                            notice = if (reward > 0) {
                                gameText("+$reward Son Coin günlük ödülün hesabına eklendi.", "+$reward Son Coins added to your account.")
                            } else {
                                gameText("Günlük ödül daha önce alındı.", "Daily reward was already claimed.")
                            }
                            reload()
                            busyKey = null
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ProfessionalMissionTab(
    growth: GrowthDashboardDto?,
    missions: List<UnifiedMissionDto>,
    goals: List<GoalRowDto>,
    loading: Boolean,
    busyKey: String?,
    notice: String?,
    onPlay: () -> Unit,
    onClaimDailyChallenge: () -> Unit,
    onClaimMission: (UnifiedMissionDto) -> Unit,
    onClaimGoal: (GoalRowDto) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = GameSpacing.ScreenHorizontal, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (loading) {
            item {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = GameColors.PlayGreen,
                    trackColor = GameColors.SecondarySurface,
                )
            }
        }

        item {
            ProfessionalDailyChallengeCard(
                growth = growth,
                busy = busyKey == "daily_challenge",
                onPlay = onPlay,
                onClaim = onClaimDailyChallenge,
            )
        }

        item { GameSectionHeader(gameText("Günlük Görevler", "Daily Missions")) }

        if (!loading && missions.isEmpty() && goals.isEmpty()) {
            item {
                GameEmptyState(
                    icon = Icons.Rounded.AssignmentTurnedIn,
                    title = gameText("Yeni görevler hazırlanıyor", "New missions are being prepared"),
                    body = gameText("Sunucu yeni görev rotasını hazırladığında burada görünecek.", "The next mission route will appear here when the server prepares it."),
                    actionText = gameText("OYNA", "PLAY"),
                    onAction = onPlay,
                )
            }
        }

        items(missions, key = { it.missionId }) { mission ->
            ProfessionalMissionCard(
                mission = mission,
                busy = busyKey == mission.missionId,
                onPlay = onPlay,
                onClaim = { onClaimMission(mission) },
            )
        }

        if (missions.isEmpty()) {
            items(goals, key = { it.id }) { goal ->
                ProfessionalLegacyGoalCard(
                    goal = goal,
                    busy = busyKey == goal.id,
                    onPlay = onPlay,
                    onClaim = { onClaimGoal(goal) },
                )
            }
        }

        notice?.let { item { ProfessionalRetentionNotice(it) } }

        item {
            GamePrimaryButton(
                text = gameText("BİR MAÇ DAHA OYNA", "PLAY ONE MORE MATCH"),
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Rounded.PlayArrow,
            )
        }
        item { Spacer(Modifier.height(6.dp)) }
    }
}

@Composable
private fun ProfessionalDailyChallengeCard(
    growth: GrowthDashboardDto?,
    busy: Boolean,
    onPlay: () -> Unit,
    onClaim: () -> Unit,
) {
    val matches = (growth?.matchesToday ?: 0).coerceAtMost(3)
    val complete = matches >= 3
    val claimed = growth?.dailyChallengeClaimed == true

    GameSurface(
        elevated = true,
        borderColor = if (complete) GameColors.PlayGreen.copy(alpha = .52f) else GameColors.PrimaryBlue.copy(alpha = .28f),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = (if (complete) GameColors.PlayGreen else GameColors.PrimaryBlue).copy(alpha = .14f),
            ) {
                Icon(
                    if (complete) Icons.Rounded.CheckCircle else Icons.Rounded.SportsEsports,
                    contentDescription = null,
                    tint = if (complete) GameColors.PlayGreen else GameColors.PrimaryBlue,
                    modifier = Modifier.padding(10.dp).size(24.dp),
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    gameText("3 düello tamamla", "Complete 3 duels"),
                    color = GameColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    gameText("Bugünün hızlı rekabet hedefi", "Today's quick competitive goal"),
                    color = GameColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Surface(shape = GameShapes.Pill, color = GameColors.RewardAmber.copy(alpha = .13f)) {
                Text(
                    "+30 SC",
                    Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    color = GameColors.RewardAmber,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        MissionProgress(matches / 3f)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("$matches/3", color = GameColors.TextSecondary, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.weight(1f))
            when {
                claimed -> Text(gameText("✓ ALINDI", "✓ CLAIMED"), color = GameColors.PlayGreen, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelMedium)
                complete -> GamePrimaryButton(
                    text = if (busy) "…" else gameText("ÖDÜLÜ AL", "CLAIM"),
                    onClick = onClaim,
                    enabled = !busy,
                    modifier = Modifier.widthIn(min = 126.dp),
                )
                else -> GameSecondaryButton(
                    text = gameText("OYNA", "PLAY"),
                    onClick = onPlay,
                    modifier = Modifier.widthIn(min = 108.dp),
                    icon = Icons.Rounded.PlayArrow,
                )
            }
        }
    }
}

@Composable
private fun ProfessionalMissionCard(
    mission: UnifiedMissionDto,
    busy: Boolean,
    onPlay: () -> Unit,
    onClaim: () -> Unit,
) {
    val complete = mission.completed || mission.progress >= mission.target
    val accent = if (complete) GameColors.PlayGreen else GameColors.PrimaryBlue

    GameSurface(borderColor = accent.copy(alpha = if (complete) .45f else .20f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = GameShapes.Medium, color = accent.copy(alpha = .13f)) {
                Icon(
                    if (complete) Icons.Rounded.CheckCircle else Icons.Rounded.TrackChanges,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.padding(9.dp).size(21.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (SonHarfUiState.isEnglish) mission.titleEn else mission.titleTr,
                    color = GameColors.TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    mission.scope.uppercase(),
                    color = GameColors.TextTertiary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Text(
                "+${mission.rewardCoins} SC",
                color = GameColors.RewardAmber,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
            )
        }
        Spacer(Modifier.height(10.dp))
        GameProgress(
            progress = mission.progress.toFloat() / mission.target.coerceAtLeast(1),
            color = accent,
        )
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${mission.progress.coerceAtMost(mission.target)}/${mission.target}",
                color = GameColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
            Spacer(Modifier.weight(1f))
            when {
                mission.claimed -> Text(gameText("✓ ALINDI", "✓ CLAIMED"), color = GameColors.PlayGreen, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall)
                complete -> GamePrimaryButton(
                    text = if (busy) "…" else gameText("ÖDÜLÜ AL", "CLAIM"),
                    onClick = onClaim,
                    enabled = !busy,
                    modifier = Modifier.widthIn(min = 122.dp),
                )
                else -> GameTertiaryButton(
                    text = gameText("OYNA", "PLAY"),
                    onClick = onPlay,
                    modifier = Modifier.widthIn(min = 100.dp),
                    icon = Icons.Rounded.PlayArrow,
                )
            }
        }
    }
}

@Composable
private fun ProfessionalLegacyGoalCard(
    goal: GoalRowDto,
    busy: Boolean,
    onPlay: () -> Unit,
    onClaim: () -> Unit,
) {
    val complete = goal.progress >= goal.target
    val accent = if (complete) GameColors.PlayGreen else GameColors.Lavender

    GameSurface(borderColor = accent.copy(alpha = .28f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (SonHarfUiState.isEnglish) goal.titleEn else goal.titleTr,
                    color = GameColors.TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (SonHarfUiState.isEnglish) goal.descriptionEn else goal.descriptionTr,
                    color = GameColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text("+${goal.rewardDiamonds} SC", color = GameColors.RewardAmber, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(9.dp))
        GameProgress(goal.progress.toFloat() / goal.target.coerceAtLeast(1), accent)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("${goal.progress.coerceAtMost(goal.target)}/${goal.target}", color = GameColors.TextSecondary, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.weight(1f))
            when {
                goal.claimed -> Text(gameText("✓ ALINDI", "✓ CLAIMED"), color = GameColors.PlayGreen, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall)
                complete -> GamePrimaryButton(
                    text = if (busy) "…" else gameText("ÖDÜLÜ AL", "CLAIM"),
                    onClick = onClaim,
                    enabled = !busy,
                    modifier = Modifier.widthIn(min = 122.dp),
                )
                else -> GameTertiaryButton(gameText("OYNA", "PLAY"), onPlay, modifier = Modifier.widthIn(min = 100.dp))
            }
        }
    }
}

@Composable
private fun ProfessionalDailyRewardTab(
    growth: GrowthDashboardDto?,
    meta: MetaProgressV2Dto?,
    loading: Boolean,
    busy: Boolean,
    notice: String?,
    onClaim: () -> Unit,
) {
    val claimed = growth?.dailyClaimed == true
    val reward = growth?.dailyReward ?: 40
    val streak = meta?.dailyPlayStreak ?: 0
    val bestStreak = meta?.bestDailyPlayStreak ?: streak
    val level = growth?.level ?: 1
    val levelProgress = growth?.levelProgress ?: 0
    val levelTarget = growth?.levelTarget?.coerceAtLeast(1) ?: 500

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = GameSpacing.ScreenHorizontal, vertical = 10.dp),
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
                borderColor = GameColors.RewardAmber.copy(alpha = .55f),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = GameColors.RewardAmber.copy(alpha = .14f)) {
                        Icon(
                            Icons.Rounded.CardGiftcard,
                            contentDescription = null,
                            tint = GameColors.RewardAmber,
                            modifier = Modifier.padding(12.dp).size(29.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (claimed) gameText("Bugünün ödülü alındı", "Today's reward claimed") else gameText("Bugünün ödülü hazır", "Today's reward is ready"),
                            color = GameColors.TextPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            gameText("Her gün geri dönerek oyun serini koru.", "Return each day to keep your play streak."),
                            color = GameColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(
                        "+$reward SC",
                        color = GameColors.RewardAmber,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                Spacer(Modifier.height(14.dp))
                GamePrimaryButton(
                    text = if (claimed) gameText("✓ ALINDI", "✓ CLAIMED") else if (busy) "…" else gameText("ÖDÜLÜ AL", "CLAIM REWARD"),
                    onClick = onClaim,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !claimed && !busy,
                    icon = if (claimed) Icons.Rounded.Check else Icons.Rounded.Redeem,
                )
            }
        }

        item {
            DailyStreakWeekStrip(
                streak = streak,
                claimedToday = claimed,
            )
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RetentionMetric(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.LocalFireDepartment,
                    value = streak.toString(),
                    label = gameText("Günlük Seri", "Daily Streak"),
                    accent = GameColors.RewardAmber,
                )
                RetentionMetric(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.EmojiEvents,
                    value = bestStreak.toString(),
                    label = gameText("En İyi Seri", "Best Streak"),
                    accent = GameColors.PrestigeGold,
                )
            }
        }

        item {
            GameSurface(borderColor = GameColors.PrimaryBlue.copy(alpha = .25f)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(gameText("Seviye $level", "Level $level"), color = GameColors.TextPrimary, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.weight(1f))
                    Text("${growth?.xp ?: 0} XP", color = GameColors.PrimaryBlue, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(9.dp))
                XPProgress(levelProgress.toFloat() / levelTarget)
                Spacer(Modifier.height(5.dp))
                Text(
                    "$levelProgress / $levelTarget ${gameText("sonraki seviyeye", "to next level")}",
                    color = GameColors.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }

        notice?.let { item { ProfessionalRetentionNotice(it) } }
        item { Spacer(Modifier.height(6.dp)) }
    }
}

@Composable
private fun DailyStreakWeekStrip(
    streak: Int,
    claimedToday: Boolean,
) {
    val cycleDay = if (streak <= 0) 1 else ((streak - 1) % 7) + 1

    GameSurface(
        borderColor = GameColors.RewardAmber.copy(alpha = .28f),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    gameText("7 Günlük Seri", "7-Day Streak"),
                    color = GameColors.TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    gameText("Bugün: Gün $cycleDay", "Today: Day $cycleDay"),
                    color = GameColors.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Icon(
                Icons.Rounded.LocalFireDepartment,
                contentDescription = null,
                tint = GameColors.RewardAmber,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(Modifier.height(11.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            (1..7).forEach { day ->
                val isCurrent = day == cycleDay
                val isPast = day < cycleDay
                val isDone = isPast || (isCurrent && claimedToday)
                val accent = when {
                    isCurrent -> GameColors.RewardAmber
                    isDone -> GameColors.PlayGreen
                    else -> GameColors.Border
                }
                val background = when {
                    isCurrent -> GameColors.RewardAmber.copy(alpha = .14f)
                    isDone -> GameColors.PlayGreen.copy(alpha = .10f)
                    else -> GameColors.ElevatedBackground
                }

                Surface(
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    shape = GameShapes.Medium,
                    color = background,
                    border = BorderStroke(
                        if (isCurrent) 1.5.dp else 1.dp,
                        accent.copy(alpha = if (isCurrent) .90f else .55f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 2.dp, vertical = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        if (isDone) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                tint = if (isCurrent) GameColors.RewardAmber else GameColors.PlayGreen,
                                modifier = Modifier.size(15.dp),
                            )
                        } else {
                            Text(
                                day.toString(),
                                color = if (isCurrent) GameColors.RewardAmber else GameColors.TextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 15.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                        Text(
                            gameText("G$day", "D$day"),
                            color = if (isCurrent) GameColors.RewardAmber else GameColors.TextTertiary,
                            fontSize = 9.sp,
                            lineHeight = 11.sp,
                            fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Medium,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RetentionMetric(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    accent: Color,
) {
    Surface(
        modifier = modifier.heightIn(min = 88.dp),
        shape = GameShapes.Large,
        color = GameColors.PrimarySurface,
        border = BorderStroke(1.dp, GameColors.Border),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = accent.copy(alpha = .13f)) {
                Icon(icon, null, tint = accent, modifier = Modifier.padding(8.dp).size(20.dp))
            }
            Spacer(Modifier.width(9.dp))
            Column {
                Text(value, color = GameColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text(label, color = GameColors.TextSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ProfessionalRetentionNotice(message: String) {
    Surface(
        shape = GameShapes.Medium,
        color = GameColors.PrimaryBlue.copy(alpha = .11f),
        border = BorderStroke(1.dp, GameColors.PrimaryBlue.copy(alpha = .26f)),
    ) {
        Text(
            message,
            modifier = Modifier.fillMaxWidth().padding(11.dp),
            color = GameColors.TextPrimary,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}
