package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
        Modifier.fillMaxSize(),
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
            item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = MainUi.Blue, trackColor = MainUi.BlueSoft) }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MainUi.BlueSoft,
            ) {
                Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = Color.White) {
                            Icon(Icons.Rounded.Bolt, null, tint = MainUi.Blue, modifier = Modifier.padding(9.dp).size(23.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${sh("SEVİYE", "LEVEL")} ${g?.level ?: 1}", color = MainUi.Text, fontSize = 16.sp, fontWeight = FontWeight.Black)
                            Text("${g?.xp ?: 0} XP • ${m?.selectedTitle ?: g?.nextTitle.orEmpty()}", color = MainUi.Muted, fontSize = 9.sp)
                        }
                        Text("${m?.dailyPlayStreak ?: 0} 🔥", color = MainUi.Text, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                    LinearProgressIndicator(
                        progress = { xpProgress },
                        modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                        color = MainUi.Blue,
                        trackColor = Color.White,
                    )
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
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MainUi.Surface,
                border = BorderStroke(1.dp, MainUi.Border),
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MainSectionTitle(sh("BUGÜN", "TODAY"))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Button(
                            onClick = {
                                if (busyKey != null || g?.dailyClaimed == true) return@Button
                                scope.launch {
                                    busyKey = "checkin"
                                    val reward = runCatching { backend.claimDailyCheckin() }.getOrDefault(0)
                                    notice = if (reward > 0) sh("+$reward Son Coin kazandın.", "You earned +$reward Son Coins.")
                                    else sh("Günlük ödül daha önce alındı.", "Daily reward was already claimed.")
                                    reload()
                                    busyKey = null
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = busyKey == null && g?.dailyClaimed != true,
                            colors = ButtonDefaults.buttonColors(containerColor = MainUi.Gold, contentColor = Color(0xFF3C2700)),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text(if (g?.dailyClaimed == true) "✓ ${sh("ALINDI", "CLAIMED")}" else "🎁 +${g?.dailyReward ?: 40} SC", fontWeight = FontWeight.Black, fontSize = 10.sp)
                        }
                        OutlinedButton(
                            onClick = onDailyChallenge,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MainUi.Blue.copy(alpha = .38f)),
                        ) {
                            Text(sh("GÜNÜN KELİMESİ", "DAILY WORD"), color = MainUi.Blue, fontWeight = FontWeight.Black, fontSize = 9.sp)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(sh("3 düello tamamla", "Complete 3 duels"), color = MainUi.Text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("${(g?.matchesToday ?: 0).coerceAtMost(3)}/3", color = MainUi.Blue, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                        LinearProgressIndicator(
                            progress = { ((g?.matchesToday ?: 0).coerceAtMost(3) / 3f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = MainUi.Green,
                            trackColor = MainUi.SurfaceSoft,
                        )
                        Button(
                            onClick = {
                                if (busyKey != null) return@Button
                                scope.launch {
                                    busyKey = "daily_challenge"
                                    val reward = runCatching { backend.claimDailyChallenge() }.getOrDefault(0)
                                    notice = if (reward > 0) sh("+$reward Son Coin kazandın.", "You earned +$reward Son Coins.")
                                    else sh("Görev henüz tamamlanmadı veya ödül alındı.", "Mission is incomplete or already claimed.")
                                    reload()
                                    busyKey = null
                                }
                            },
                            enabled = busyKey == null && (g?.matchesToday ?: 0) >= 3 && g?.dailyChallengeClaimed != true,
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.buttonColors(containerColor = MainUi.Green),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                        ) {
                            Text(if (g?.dailyChallengeClaimed == true) sh("ALINDI", "CLAIMED") else "+30 SC", fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onPlay),
                shape = RoundedCornerShape(20.dp),
                color = MainUi.BlueSoft,
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(sh("YAKIN HEDEF", "NEARBY GOAL"), color = MainUi.Blue, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (league.nextAt == null) sh("En üst ligdesin", "You are in the top league")
                        else sh("${league.nextLeagueName} için ${league.pointsToNext} puan", "${league.pointsToNext} points to ${league.nextLeagueName}"),
                        color = MainUi.Text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                    )
                    LinearProgressIndicator(
                        progress = { league.progress },
                        modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                        color = MainUi.Blue,
                        trackColor = Color.White,
                    )
                    Text(sh("Bir maç daha oynayarak hedefe yaklaş", "Play one more match to move closer"), color = MainUi.Muted, fontSize = 9.sp)
                }
            }
        }

        item { MainSectionTitle(sh("GÖREV ROTASI", "MISSION ROUTE")) }

        if (missions.isEmpty() && goals.isEmpty() && !loading) {
            item {
                Text(
                    sh("Yeni görevler sunucuda hazırlanıyor.", "New missions are being prepared on the server."),
                    Modifier.fillMaxWidth().padding(vertical = 18.dp),
                    color = MainUi.Muted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }

        items(missions, key = { it.missionId }) { mission ->
            MainUnifiedMissionCard(
                mission = mission,
                busy = busyKey == mission.missionId,
                onPlay = onPlay,
                onClaim = {
                    if (busyKey != null) return@MainUnifiedMissionCard
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
                        if (busyKey != null) return@MainLegacyGoalCard
                        scope.launch {
                            busyKey = goal.id
                            val reward = runCatching { backend.claimGoal(goal.id) }.getOrDefault(0)
                            notice = if (reward > 0) sh("+$reward Son Coin alındı.", "+$reward Son Coins claimed.")
                            else sh("Görev ödülü alınamadı.", "Mission reward could not be claimed.")
                            reload()
                            busyKey = null
                        }
                    },
                )
            }
        }

        item { MainSectionTitle(sh("KELİME USTALIĞI", "WORD MASTERY")) }

        if (mastery.isEmpty() && !loading) {
            item { Text(sh("Ustalık yolu ilk maçlarınla açılır.", "The mastery path unlocks with your first matches."), color = MainUi.Muted, fontSize = 10.sp) }
        }

        items(mastery.take(6), key = { it.id }) { milestone ->
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MainUi.Surface,
                border = BorderStroke(1.dp, if (milestone.unlocked) MainUi.Gold.copy(alpha = .45f) else MainUi.Border),
            ) {
                Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = if (milestone.unlocked) MainUi.Gold.copy(alpha = .12f) else MainUi.SurfaceSoft) {
                            Icon(Icons.Rounded.AutoAwesome, null, tint = if (milestone.unlocked) MainUi.Gold else MainUi.Muted, modifier = Modifier.padding(8.dp).size(19.dp))
                        }
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (SonHarfUiState.isEnglish) milestone.titleEn else milestone.titleTr, color = MainUi.Text, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            Text(if (SonHarfUiState.isEnglish) milestone.descriptionEn else milestone.descriptionTr, color = MainUi.Muted, fontSize = 9.sp)
                        }
                        Text("+${milestone.rewardCoins} SC", color = MainUi.Gold, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                    LinearProgressIndicator(
                        progress = { (milestone.progress.toFloat() / milestone.target.coerceAtLeast(1)).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                        color = if (milestone.unlocked) MainUi.Gold else MainUi.Blue,
                        trackColor = MainUi.SurfaceSoft,
                    )
                    if (milestone.unlocked && !milestone.claimed) {
                        TextButton(
                            onClick = {
                                if (busyKey != null) return@TextButton
                                scope.launch {
                                    busyKey = milestone.id
                                    val reward = runCatching { backend.claimMasteryReward(milestone.id) }.getOrDefault(0)
                                    notice = if (reward > 0) sh("+$reward Son Coin alındı.", "+$reward Son Coins claimed.") else sh("Ödül alınamadı.", "Reward could not be claimed.")
                                    reload()
                                    busyKey = null
                                }
                            },
                            modifier = Modifier.align(Alignment.End),
                        ) { Text(sh("ÖDÜLÜ AL", "CLAIM"), color = MainUi.Blue, fontSize = 9.sp, fontWeight = FontWeight.Black) }
                    }
                }
            }
        }

        item {
            MainSectionTitle(sh("BAŞARIMLAR", "ACHIEVEMENTS"))
            Spacer(Modifier.height(8.dp))
            val unlocked = achievements.count { it.unlocked }
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MainUi.Surface,
                border = BorderStroke(1.dp, MainUi.Border),
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(sh("Açılan başarımlar", "Unlocked achievements"), color = MainUi.Text, fontWeight = FontWeight.Bold)
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
                            Text(if (achievement.unlocked) "✓" else "${achievement.currentValue}/${achievement.target}", color = if (achievement.unlocked) MainUi.Green else MainUi.Muted, fontSize = 9.sp)
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
                Surface(shape = RoundedCornerShape(14.dp), color = MainUi.BlueSoft) {
                    Text(message, Modifier.fillMaxWidth().padding(11.dp), color = MainUi.Text, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
        }

        item {
            Button(
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(17.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MainUi.Blue),
            ) {
                Text(sh("BİR MAÇ DAHA OYNA", "PLAY ONE MORE MATCH"), fontWeight = FontWeight.Black)
                Spacer(Modifier.width(7.dp))
                Icon(Icons.Rounded.ArrowForward, null)
            }
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
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MainUi.Surface,
        border = BorderStroke(1.dp, if (complete) MainUi.Green.copy(alpha = .42f) else MainUi.Border),
    ) {
        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (complete) Icons.Rounded.CheckCircle else Icons.Rounded.TrackChanges, null, tint = if (complete) MainUi.Green else MainUi.Blue)
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (SonHarfUiState.isEnglish) mission.titleEn else mission.titleTr, color = MainUi.Text, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Text(mission.scope.uppercase(), color = MainUi.Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
                Text("+${mission.rewardCoins} SC", color = MainUi.Gold, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
            LinearProgressIndicator(
                progress = { (mission.progress.toFloat() / mission.target.coerceAtLeast(1)).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                color = if (complete) MainUi.Green else MainUi.Blue,
                trackColor = MainUi.SurfaceSoft,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${mission.progress.coerceAtMost(mission.target)}/${mission.target}", color = MainUi.Muted, fontSize = 9.sp)
                when {
                    mission.claimed -> Text(sh("ALINDI", "CLAIMED"), color = MainUi.Green, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    complete -> Button(onClick = onClaim, enabled = !busy, contentPadding = PaddingValues(horizontal = 13.dp, vertical = 6.dp), shape = RoundedCornerShape(11.dp)) { Text(if (busy) "…" else sh("ÖDÜLÜ AL", "CLAIM"), fontSize = 8.sp, fontWeight = FontWeight.Black) }
                    else -> TextButton(onClick = onPlay, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) { Text(sh("OYNA", "PLAY"), color = MainUi.Blue, fontSize = 9.sp, fontWeight = FontWeight.Black) }
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
    Surface(shape = RoundedCornerShape(18.dp), color = MainUi.Surface, border = BorderStroke(1.dp, MainUi.Border)) {
        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (SonHarfUiState.isEnglish) goal.titleEn else goal.titleTr, color = MainUi.Text, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                Text("+${goal.rewardDiamonds} SC", color = MainUi.Gold, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
            Text(if (SonHarfUiState.isEnglish) goal.descriptionEn else goal.descriptionTr, color = MainUi.Muted, fontSize = 9.sp)
            LinearProgressIndicator(
                progress = { (goal.progress.toFloat() / goal.target.coerceAtLeast(1)).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                color = if (complete) MainUi.Green else MainUi.Blue,
                trackColor = MainUi.SurfaceSoft,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${goal.progress.coerceAtMost(goal.target)}/${goal.target}", color = MainUi.Muted, fontSize = 9.sp)
                when {
                    goal.claimed -> Text(sh("ALINDI", "CLAIMED"), color = MainUi.Green, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    complete -> TextButton(onClick = onClaim, enabled = !busy) { Text(if (busy) "…" else sh("ÖDÜLÜ AL", "CLAIM"), fontSize = 9.sp, fontWeight = FontWeight.Black) }
                    else -> TextButton(onClick = onPlay) { Text(sh("OYNA", "PLAY"), fontSize = 9.sp, fontWeight = FontWeight.Black) }
                }
            }
        }
    }
}
