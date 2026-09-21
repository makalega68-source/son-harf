package com.sonharf.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun MainRetentionScreen(
    backend: OnlineGameBackend,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onDailyChallenge: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var growth by remember { mutableStateOf<GrowthDashboardDto?>(null) }
    var meta by remember { mutableStateOf<MetaProgressV2Dto?>(null) }
    var missions by remember { mutableStateOf<List<UnifiedMissionDto>>(emptyList()) }
    var goals by remember { mutableStateOf<List<GoalRowDto>>(emptyList()) }
    var achievements by remember { mutableStateOf<List<AchievementProgressDto>>(emptyList()) }
    var mastery by remember { mutableStateOf<List<MasteryMilestoneDto>>(emptyList()) }
    var records by remember { mutableStateOf<PersonalRecordsDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var busyKey by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    suspend fun reload() = coroutineScope {
        loading = true
        val id = backend.currentUserId()
        val profileTask = async { id?.let { runCatching { backend.getProfile(it) }.getOrNull() } }
        val growthTask = async { runCatching { backend.getGrowthDashboard() }.getOrNull() }
        val metaTask = async { runCatching { backend.getMetaProgressV2() }.getOrNull() }
        val missionTask = async { runCatching { backend.getUnifiedMissions() }.getOrDefault(emptyList()) }
        val goalTask = async { runCatching { backend.getGoals() }.getOrDefault(emptyList()) }
        val achievementTask = async { runCatching { backend.getAchievements() }.getOrDefault(emptyList()) }
        val masteryTask = async { runCatching { backend.getMasteryPath() }.getOrDefault(emptyList()) }
        val recordTask = async { runCatching { backend.getPersonalRecords() }.getOrNull() }
        profile = profileTask.await()
        growth = growthTask.await()
        meta = metaTask.await()
        missions = missionTask.await()
        goals = goalTask.await()
        achievements = achievementTask.await()
        mastery = masteryTask.await()
        records = recordTask.await()
        loading = false
    }

    LaunchedEffect(Unit) { reload() }

    val g = growth
    val m = meta
    val league = ratingLeagueProgress(profile?.rating ?: 1000)
    val xpProgress = g?.let {
        (it.levelProgress.toFloat() / it.levelTarget.coerceAtLeast(1)).coerceIn(0f, 1f)
    } ?: 0f

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            MainScreenHeader(
                title = sh("Görevler ve İlerleme", "Missions & Progress"),
                subtitle = sh("Oyna, ödül kazan ve sıradaki hedefi gör", "Play, earn rewards and see the next goal"),
                onBack = onBack,
            )
        }

        if (loading) {
            item {
                PurchasedPanel(
                    modifier = Modifier.fillMaxWidth(),
                    asset = PurchasedUiAsset.PANEL_SMALL,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PurchasedAsset(PurchasedUiAsset.ICON_GAMES, Modifier.size(30.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(sh("İlerleme yükleniyor…", "Loading progress…"), color = MainUi.Text, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            PurchasedPanel(
                modifier = Modifier.fillMaxWidth(),
                asset = PurchasedUiAsset.PANEL_LARGE,
                contentPadding = PaddingValues(18.dp),
            ) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        PurchasedAsset(PurchasedUiAsset.ICON_TROPHY, Modifier.size(46.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${sh("SEVİYE", "LEVEL")} ${g?.level ?: 1}",
                                color = MainUi.Text,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                            )
                            Text(
                                "${g?.xp ?: 0} XP • ${m?.selectedTitle ?: g?.nextTitle.orEmpty()}",
                                color = MainUi.Muted,
                                fontSize = 9.sp,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            PurchasedAsset(PurchasedUiAsset.ICON_CROWN, Modifier.size(32.dp))
                            Text("${m?.dailyPlayStreak ?: 0} 🔥", color = MainUi.Text, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    PurchasedProgress(progress = xpProgress, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${g?.levelProgress ?: 0}/${g?.levelTarget ?: 500}", color = MainUi.Muted, fontSize = 9.sp)
                        Text(
                            sh("En iyi günlük seri: ${m?.bestDailyPlayStreak ?: 0}", "Best daily streak: ${m?.bestDailyPlayStreak ?: 0}"),
                            color = MainUi.Muted,
                            fontSize = 9.sp,
                        )
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MainMetricCard("${g?.currentWinStreak ?: 0}", sh("Galibiyet serisi", "Win streak"), Modifier.weight(1f))
                MainMetricCard("${g?.bestStreak ?: 0}", sh("En uzun seri", "Best streak"), Modifier.weight(1f))
                MainMetricCard("${m?.uniqueWords ?: g?.validWords ?: 0}", sh("Usta kelime", "Unique words"), Modifier.weight(1f))
            }
        }

        item {
            MainSectionTitle(sh("BUGÜN", "TODAY"))
            Spacer(Modifier.height(7.dp))
            PurchasedPanel(
                modifier = Modifier.fillMaxWidth(),
                asset = PurchasedUiAsset.PANEL_MEDIUM,
                contentPadding = PaddingValues(16.dp),
            ) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        PurchasedAsset(PurchasedUiAsset.OLD_DAILY_REWARD, Modifier.size(width = 62.dp, height = 78.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(sh("Günlük ödül", "Daily reward"), color = MainUi.Text, fontSize = 13.sp, fontWeight = FontWeight.Black)
                            Text(
                                if (g?.dailyClaimed == true) sh("Bugünün ödülü alındı", "Today's reward claimed")
                                else sh("Bugünkü Son Coin ödülünü al", "Claim today's Son Coin reward"),
                                color = MainUi.Muted,
                                fontSize = 9.sp,
                            )
                            PurchasedButton(
                                text = if (g?.dailyClaimed == true) "✓ ${sh("ALINDI", "CLAIMED")}" else "+${g?.dailyReward ?: 40} SC",
                                onClick = {
                                    if (busyKey == null && g?.dailyClaimed != true) {
                                        scope.launch {
                                            busyKey = "checkin"
                                            val reward = runCatching { backend.claimDailyCheckin() }.getOrDefault(0)
                                            notice = if (reward > 0) sh("+$reward Son Coin kazandın.", "You earned +$reward Son Coins.")
                                            else sh("Günlük ödül daha önce alındı.", "Daily reward was already claimed.")
                                            reload()
                                            busyKey = null
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                style = PurchasedButtonStyle.WARNING,
                                enabled = busyKey == null && g?.dailyClaimed != true,
                                leadingAsset = PurchasedUiAsset.ICON_GIFT,
                            )
                        }
                    }

                    PurchasedButton(
                        text = sh("GÜNÜN KELİMESİ", "DAILY WORD"),
                        onClick = onDailyChallenge,
                        modifier = Modifier.fillMaxWidth(),
                        style = PurchasedButtonStyle.SECONDARY,
                        leadingAsset = PurchasedUiAsset.ICON_GAMES,
                    )

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(sh("3 düello tamamla", "Complete 3 duels"), color = MainUi.Text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("${(g?.matchesToday ?: 0).coerceAtMost(3)}/3", color = MainUi.Blue, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                    PurchasedProgress(
                        progress = ((g?.matchesToday ?: 0).coerceAtMost(3) / 3f),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PurchasedButton(
                        text = if (g?.dailyChallengeClaimed == true) sh("ALINDI", "CLAIMED") else "+30 SC",
                        onClick = {
                            if (busyKey == null) {
                                scope.launch {
                                    busyKey = "daily_challenge"
                                    val reward = runCatching { backend.claimDailyChallenge() }.getOrDefault(0)
                                    notice = if (reward > 0) sh("+$reward Son Coin kazandın.", "You earned +$reward Son Coins.")
                                    else sh("Görev henüz tamamlanmadı veya ödül alındı.", "Mission is incomplete or already claimed.")
                                    reload()
                                    busyKey = null
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = busyKey == null && (g?.matchesToday ?: 0) >= 3 && g?.dailyChallengeClaimed != true,
                        style = PurchasedButtonStyle.PRIMARY,
                        leadingAsset = PurchasedUiAsset.ICON_CHECK,
                    )
                }
            }
        }

        item {
            PurchasedPanel(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onPlay),
                asset = PurchasedUiAsset.PANEL_SMALL,
                contentPadding = PaddingValues(15.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    PurchasedAsset(PurchasedUiAsset.ICON_RANKING, Modifier.size(46.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(sh("YAKIN HEDEF", "NEARBY GOAL"), color = MainUi.Blue, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        Text(
                            if (league.nextAt == null) sh("En üst ligdesin", "You are in the top league")
                            else sh("${league.nextLeagueName} için ${league.pointsToNext} puan", "${league.pointsToNext} points to ${league.nextLeagueName}"),
                            color = MainUi.Text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                        )
                        PurchasedProgress(progress = league.progress, modifier = Modifier.fillMaxWidth())
                        Text(sh("Bir maç daha oynayarak hedefe yaklaş", "Play one more match to move closer"), color = MainUi.Muted, fontSize = 9.sp)
                    }
                }
            }
        }

        item { MainSectionTitle(sh("GÖREV ROTASI", "MISSION ROUTE")) }

        if (missions.isEmpty() && goals.isEmpty() && !loading) {
            item {
                PurchasedPanel(
                    modifier = Modifier.fillMaxWidth(),
                    asset = PurchasedUiAsset.OLD_MISSION_ROW,
                    contentPadding = PaddingValues(14.dp),
                ) {
                    Text(
                        sh("Yeni görevler sunucuda hazırlanıyor.", "New missions are being prepared on the server."),
                        color = MainUi.Muted,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        items(missions, key = { it.missionId }) { mission ->
            MainUnifiedMissionCard(
                mission = mission,
                busy = busyKey == mission.missionId,
                onPlay = onPlay,
                onClaim = {
                    if (busyKey == null) {
                        scope.launch {
                            busyKey = mission.missionId
                            runCatching { backend.claimUnifiedMission(mission.missionId) }
                                .onSuccess {
                                    notice = sh("+${it.rewardCoins} Son Coin alındı.", "+${it.rewardCoins} Son Coins claimed.")
                                    SonHarfSoundFx.missionComplete()
                                    reload()
                                }
                                .onFailure { notice = sh("Görev ödülü alınamadı.", "Mission reward could not be claimed.") }
                            busyKey = null
                        }
                    }
                },
            )
        }

        if (missions.isEmpty()) {
            items(goals, key = { it.id }) { goal ->
                MainLegacyGoalCard(
                    goal = goal,
                    busy = busyKey == goal.id,
                    onPlay = onPlay,
                    onClaim = {
                        if (busyKey == null) {
                            scope.launch {
                                busyKey = goal.id
                                val reward = runCatching { backend.claimGoal(goal.id) }.getOrDefault(0)
                                notice = if (reward > 0) sh("+$reward Son Coin alındı.", "+$reward Son Coins claimed.")
                                else sh("Görev ödülü alınamadı.", "Mission reward could not be claimed.")
                                reload()
                                busyKey = null
                            }
                        }
                    },
                )
            }
        }

        item { MainSectionTitle(sh("KELİME USTALIĞI", "WORD MASTERY")) }

        if (mastery.isEmpty() && !loading) {
            item {
                Text(
                    sh("Ustalık yolu ilk maçlarınla açılır.", "The mastery path unlocks with your first matches."),
                    color = MainUi.Muted,
                    fontSize = 10.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }

        items(mastery.take(6), key = { it.id }) { milestone ->
            PurchasedPanel(
                modifier = Modifier.fillMaxWidth(),
                asset = PurchasedUiAsset.PANEL_SMALL,
                contentPadding = PaddingValues(14.dp),
            ) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PurchasedAsset(
                            if (milestone.unlocked) PurchasedUiAsset.ICON_TROPHY else PurchasedUiAsset.ICON_RANKING,
                            Modifier.size(38.dp),
                            alpha = if (milestone.unlocked) 1f else .55f,
                        )
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (SonHarfUiState.isEnglish) milestone.titleEn else milestone.titleTr, color = MainUi.Text, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            Text(if (SonHarfUiState.isEnglish) milestone.descriptionEn else milestone.descriptionTr, color = MainUi.Muted, fontSize = 9.sp)
                        }
                        Text("+${milestone.rewardCoins} SC", color = MainUi.Gold, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                    PurchasedProgress(
                        progress = (milestone.progress.toFloat() / milestone.target.coerceAtLeast(1)).coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (milestone.unlocked && !milestone.claimed) {
                        PurchasedButton(
                            text = sh("ÖDÜLÜ AL", "CLAIM"),
                            onClick = {
                                if (busyKey == null) {
                                    scope.launch {
                                        busyKey = milestone.id
                                        val reward = runCatching { backend.claimMasteryReward(milestone.id) }.getOrDefault(0)
                                        notice = if (reward > 0) sh("+$reward Son Coin alındı.", "+$reward Son Coins claimed.")
                                        else sh("Ödül alınamadı.", "Reward could not be claimed.")
                                        reload()
                                        busyKey = null
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = busyKey == null,
                            style = PurchasedButtonStyle.WARNING,
                            leadingAsset = PurchasedUiAsset.ICON_GIFT,
                        )
                    }
                }
            }
        }

        item {
            MainSectionTitle(sh("BAŞARIMLAR", "ACHIEVEMENTS"))
            Spacer(Modifier.height(8.dp))
            val unlocked = achievements.count { it.unlocked }
            PurchasedPanel(
                modifier = Modifier.fillMaxWidth(),
                asset = PurchasedUiAsset.PANEL_MEDIUM,
                contentPadding = PaddingValues(14.dp),
            ) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        PurchasedAsset(PurchasedUiAsset.ICON_TROPHY, Modifier.size(42.dp))
                        Spacer(Modifier.width(9.dp))
                        Text(sh("Açılan başarımlar", "Unlocked achievements"), color = MainUi.Text, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("$unlocked/${achievements.size}", color = MainUi.Blue, fontWeight = FontWeight.Black)
                    }
                    achievements.take(5).forEach { achievement ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(achievement.icon, fontSize = 18.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (SonHarfUiState.isEnglish) achievement.titleEn else achievement.titleTr,
                                color = if (achievement.unlocked) MainUi.Text else MainUi.Muted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                if (achievement.unlocked) "✓" else "${achievement.currentValue}/${achievement.target}",
                                color = if (achievement.unlocked) MainUi.Green else MainUi.Muted,
                                fontSize = 9.sp,
                            )
                        }
                    }
                }
            }
        }

        records?.let { r ->
            item {
                MainSectionTitle(sh("KİŞİSEL REKORLAR", "PERSONAL RECORDS"))
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MainMetricCard(r.longestWord.ifBlank { "—" }.uppercase(), sh("En uzun kelime", "Longest word"), Modifier.weight(1f))
                    MainMetricCard(r.bestClassicScore.toString(), sh("En iyi skor", "Best score"), Modifier.weight(1f))
                    MainMetricCard(r.realPvpMatches.toString(), sh("Gerçek PvP", "Real PvP"), Modifier.weight(1f))
                }
            }
        }

        notice?.let { message ->
            item {
                PurchasedPanel(
                    modifier = Modifier.fillMaxWidth(),
                    asset = PurchasedUiAsset.REWARD_PANEL,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 13.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        PurchasedAsset(PurchasedUiAsset.ICON_GIFT, Modifier.size(32.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(message, modifier = Modifier.weight(1f), color = MainUi.Text, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        item {
            PurchasedButton(
                text = sh("BİR MAÇ DAHA OYNA", "PLAY ONE MORE MATCH"),
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth(),
                style = PurchasedButtonStyle.PRIMARY,
                leadingAsset = PurchasedUiAsset.ICON_SWORDS,
            )
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun MainUnifiedMissionCard(
    mission: UnifiedMissionDto,
    busy: Boolean,
    onPlay: () -> Unit,
    onClaim: () -> Unit,
) {
    val complete = mission.completed || mission.progress >= mission.target
    PurchasedPanel(
        modifier = Modifier.fillMaxWidth(),
        asset = PurchasedUiAsset.OLD_MISSION_ROW,
        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 13.dp),
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PurchasedAsset(
                    if (complete) PurchasedUiAsset.ICON_CHECK else PurchasedUiAsset.ICON_GAMES,
                    Modifier.size(36.dp),
                    alpha = if (complete) 1f else .9f,
                )
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (SonHarfUiState.isEnglish) mission.titleEn else mission.titleTr, color = MainUi.Text, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Text(mission.scope.uppercase(), color = MainUi.Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
                PurchasedCurrencyBar(amount = "+${mission.rewardCoins}", modifier = Modifier.widthIn(min = 92.dp))
            }
            PurchasedProgress(
                progress = (mission.progress.toFloat() / mission.target.coerceAtLeast(1)).coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${mission.progress.coerceAtMost(mission.target)}/${mission.target}", color = MainUi.Muted, fontSize = 9.sp)
                when {
                    mission.claimed -> Text(sh("ALINDI", "CLAIMED"), color = MainUi.Green, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    complete -> PurchasedButton(
                        text = if (busy) "…" else sh("ÖDÜLÜ AL", "CLAIM"),
                        onClick = onClaim,
                        modifier = Modifier.widthIn(min = 120.dp),
                        enabled = !busy,
                        style = PurchasedButtonStyle.WARNING,
                        leadingAsset = PurchasedUiAsset.ICON_GIFT,
                    )
                    else -> PurchasedButton(
                        text = sh("OYNA", "PLAY"),
                        onClick = onPlay,
                        modifier = Modifier.widthIn(min = 104.dp),
                        style = PurchasedButtonStyle.SECONDARY,
                    )
                }
            }
        }
    }
}

@Composable
private fun MainLegacyGoalCard(
    goal: GoalRowDto,
    busy: Boolean,
    onPlay: () -> Unit,
    onClaim: () -> Unit,
) {
    val complete = goal.progress >= goal.target
    PurchasedPanel(
        modifier = Modifier.fillMaxWidth(),
        asset = PurchasedUiAsset.OLD_MISSION_ROW,
        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 13.dp),
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PurchasedAsset(PurchasedUiAsset.ICON_GAMES, Modifier.size(34.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (SonHarfUiState.isEnglish) goal.titleEn else goal.titleTr, color = MainUi.Text, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                PurchasedCurrencyBar(amount = "+${goal.rewardDiamonds}", modifier = Modifier.widthIn(min = 92.dp))
            }
            Text(if (SonHarfUiState.isEnglish) goal.descriptionEn else goal.descriptionTr, color = MainUi.Muted, fontSize = 9.sp)
            PurchasedProgress(
                progress = (goal.progress.toFloat() / goal.target.coerceAtLeast(1)).coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${goal.progress.coerceAtMost(goal.target)}/${goal.target}", color = MainUi.Muted, fontSize = 9.sp)
                when {
                    goal.claimed -> Text(sh("ALINDI", "CLAIMED"), color = MainUi.Green, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    complete -> PurchasedButton(
                        text = if (busy) "…" else sh("ÖDÜLÜ AL", "CLAIM"),
                        onClick = onClaim,
                        modifier = Modifier.widthIn(min = 120.dp),
                        enabled = !busy,
                        style = PurchasedButtonStyle.WARNING,
                    )
                    else -> PurchasedButton(
                        text = sh("OYNA", "PLAY"),
                        onClick = onPlay,
                        modifier = Modifier.widthIn(min = 104.dp),
                        style = PurchasedButtonStyle.SECONDARY,
                    )
                }
            }
        }
    }
}
