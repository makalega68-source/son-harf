package com.sonharf.game

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import com.sonharf.game.ui.home.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private data class RewardVisual(val title: String, val diamonds: Int)

@Composable
fun V6HomeRoute(
    onStartGameMode: (String) -> Unit,
    onOpenLeague: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val backend = remember { OnlineGameBackend() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(FullHomeUiState()) }
    var meta by remember { mutableStateOf<MetaDashboardV10Dto?>(null) }
    var dailyGoals by remember { mutableStateOf<List<DailyGoalV10Dto>>(emptyList()) }
    var showVip by remember { mutableStateOf(false) }
    var showGameModePicker by remember { mutableStateOf(false) }
    var rewardVisual by remember { mutableStateOf<RewardVisual?>(null) }

    suspend fun reload(showSpinner: Boolean) {
        if (showSpinner) state = state.copy(isLoading = true)
        val uid = backend.currentUserId() ?: run {
            state = state.copy(isLoading = false, notice = "Oturum bulunamadı.")
            return
        }
        val profile = runCatching { v6LoadProfile(uid) }.getOrNull()
        val growth = runCatching { backend.getGrowthDashboard() }.getOrNull()
        val ownAvatar = runCatching { AvatarSignedUrl.resolve(profile?.avatarPath) }.getOrNull()
        val leaders = runCatching { backend.getLeaderboardV3("tr", "week", 3) }.getOrDefault(emptyList())
        val top = leaders.mapIndexed { index, row ->
            TopPlayerUiModel(
                rank = index + 1,
                name = row.displayName,
                score = row.wins,
                photoUrl = runCatching { AvatarSignedUrl.resolve(row.avatarUrl) }.getOrNull(),
            )
        }
        val friends = runCatching { backend.getFriends() }.getOrDefault(emptyList())
        val goals = runCatching { backend.getGoals() }.getOrDefault(emptyList())
        meta = runCatching { backend.getMetaDashboardV10() }.getOrNull()
        dailyGoals = runCatching { backend.getDailyGoalsV10() }.getOrDefault(emptyList())
        state = FullHomeUiState(
            userName = profile?.displayName ?: growth?.displayName ?: "Son Harf Oyuncusu",
            userPhotoUrl = ownAvatar,
            level = growth?.level ?: 1,
            diamonds = profile?.diamonds ?: 0,
            league = "${meta?.seasonLeague ?: growth?.leagueName ?: "BRONZ"} Lig",
            topPlayers = top,
            isDailyRewardAvailable = growth?.dailyClaimed == false,
            dailyRewardDiamonds = growth?.dailyReward ?: 40,
            onlineFriendsCount = friends.count { it.second.presenceStatus == "online" },
            tasks = goals.map { DailyTaskUiModel(it.id, it.titleTr, it.progress, it.target, it.rewardDiamonds, it.claimed) },
            isLoading = false,
            isActionBusy = state.isActionBusy,
            notice = state.notice,
        )
    }

    LaunchedEffect(Unit) {
        reload(true)
        while (isActive) {
            delay(15_000)
            reload(false)
        }
    }
    LaunchedEffect("dictionary-bootstrap") {
        // Does not block the home screen. Once the server reports both dictionaries ready,
        // word validation automatically switches from bootstrap mode to strict dictionary mode.
        runCatching { GameDictionaryBootstrap.syncIfNeeded() }
    }

    V10HomeScreen(
        state = state,
        meta = meta,
        dailyGoals = dailyGoals,
        onStartGameMode = { mode -> if (mode == "1v1_RANKED") showGameModePicker = true else onStartGameMode(mode) },
        onClaimDailyReward = {
            if (!state.isActionBusy) scope.launch {
                state = state.copy(isActionBusy = true, notice = "Ödül alınıyor…")
                runCatching { backend.claimDailyCheckin() }
                    .onSuccess { reward ->
                        if (reward > 0) {
                            state = state.copy(diamonds = state.diamonds + reward, isDailyRewardAvailable = false, notice = "+$reward elmas hesabına işlendi.")
                            rewardVisual = RewardVisual("Günlük Ödül", reward)
                        } else state = state.copy(isDailyRewardAvailable = false, notice = "Bugünkü ödül zaten alınmış.")
                    }
                    .onFailure { state = state.copy(notice = "Günlük ödül alınamadı.") }
                state = state.copy(isActionBusy = false)
                reload(false)
            }
        },
        onOpenVipModal = { showVip = true },
        onInviteFriend = { SonHarfShare.challenge(context, state.userName) },
        onOpenFriendsList = { FriendsQuickAccessState.open = true },
        onOpenLeaderboard = onOpenLeague,
        onOpenProfile = onOpenProfile,
        onClaimWeeklyGoal = { goalId ->
            if (state.isActionBusy) return@V10HomeScreen
            val goal = state.tasks.firstOrNull { it.id == goalId }
            scope.launch {
                val oldBalance = state.diamonds
                state = state.copy(isActionBusy = true, notice = "Ödül alınıyor…")
                runCatching { backend.claimGoal(goalId) }
                    .onSuccess { newBalance ->
                        val delta = (newBalance - oldBalance).coerceAtLeast(goal?.rewardDiamonds ?: 0)
                        state = state.copy(diamonds = newBalance, notice = "Haftalık görev ödülü hesabına işlendi.")
                        rewardVisual = RewardVisual(goal?.title ?: "Haftalık Görev", delta)
                    }
                    .onFailure { e ->
                        state = state.copy(notice = when {
                            "goal_already_claimed" in e.message.orEmpty() -> "Bu görev ödülü zaten alındı."
                            "goal_not_complete" in e.message.orEmpty() -> "Görev henüz tamamlanmadı."
                            else -> "Görev ödülü alınamadı."
                        })
                    }
                state = state.copy(isActionBusy = false)
                reload(false)
            }
        },
        onClaimDailyGoal = { goalId ->
            if (state.isActionBusy) return@V10HomeScreen
            val goal = dailyGoals.firstOrNull { it.id == goalId }
            scope.launch {
                val oldBalance = state.diamonds
                state = state.copy(isActionBusy = true, notice = "Ödül alınıyor…")
                runCatching { backend.claimDailyGoalV10(goalId) }
                    .onSuccess { newBalance ->
                        val delta = (newBalance - oldBalance).coerceAtLeast(goal?.rewardDiamonds ?: 0)
                        state = state.copy(diamonds = newBalance, notice = "Günlük görev ödülü hesabına işlendi.")
                        rewardVisual = RewardVisual(goal?.titleTr ?: "Günlük Görev", delta)
                    }
                    .onFailure { e ->
                        state = state.copy(notice = when {
                            "goal_already_claimed" in e.message.orEmpty() -> "Bu günlük ödül zaten alındı."
                            "goal_not_complete" in e.message.orEmpty() -> "Günlük görev henüz tamamlanmadı."
                            else -> "Günlük görev ödülü alınamadı."
                        })
                    }
                state = state.copy(isActionBusy = false)
                reload(false)
            }
        },
    )

    rewardVisual?.let { reward ->
        AlertDialog(
            onDismissRequest = { rewardVisual = null },
            title = { Text("🎉 ÖDÜL ALINDI", fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💎", fontSize = 68.sp)
                    Text("+${reward.diamonds}", fontSize = 32.sp, fontWeight = FontWeight.Black)
                    Text(reward.title, textAlign = TextAlign.Center)
                    Text("Elmaslar hesabına eklendi.", textAlign = TextAlign.Center)
                }
            },
            confirmButton = { Button(onClick = { rewardVisual = null }, modifier = Modifier.fillMaxWidth()) { Text("DEVAM") } },
        )
    }

    if (showGameModePicker) {
        AlertDialog(
            onDismissRequest = { showGameModePicker = false },
            title = { Text("Oyun Modu") },
            text = { Text("Normal mod klasik Son Harf kurallarıyla oynanır. Uzman mod daha kısa süre ve gelişmiş son-harf baskısıyla oynanır.") },
            confirmButton = { Button(onClick = { showGameModePicker = false; onStartGameMode("EXPERT_MATCH") }) { Text("UZMAN") } },
            dismissButton = { OutlinedButton(onClick = { showGameModePicker = false; onStartGameMode("NORMAL_MATCH") }) { Text("NORMAL") } },
        )
    }

    if (showVip) {
        VipPurchaseDialog(onVerified = { scope.launch { reload(false) } }, onDismiss = { showVip = false })
    }
}
