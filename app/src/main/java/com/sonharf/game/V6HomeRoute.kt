package com.sonharf.game

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.sonharf.game.data.*
import com.sonharf.game.ui.home.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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

    V10HomeScreen(
        state = state,
        meta = meta,
        dailyGoals = dailyGoals,
        onStartGameMode = { mode ->
            if (mode == "1v1_RANKED") showGameModePicker = true else onStartGameMode(mode)
        },
        onClaimDailyReward = {
            if (!state.isActionBusy) scope.launch {
                state = state.copy(isActionBusy = true, notice = "")
                runCatching { backend.claimDailyCheckin() }
                    .onSuccess { reward -> state = state.copy(notice = if (reward > 0) "+$reward elmas hesabına işlendi." else "Bugünkü ödül zaten alınmış.") }
                    .onFailure { state = state.copy(notice = "Günlük ödül alınamadı.") }
                reload(false)
                state = state.copy(isActionBusy = false)
            }
        },
        onOpenVipModal = { showVip = true },
        onInviteFriend = { SonHarfShare.challenge(context, state.userName) },
        onOpenFriendsList = { FriendsQuickAccessState.open = true },
        onOpenLeaderboard = onOpenLeague,
        onOpenProfile = onOpenProfile,
        onClaimWeeklyGoal = { goalId ->
            if (state.isActionBusy) return@V10HomeScreen
            scope.launch {
                state = state.copy(isActionBusy = true, notice = "")
                runCatching { backend.claimGoal(goalId) }
                    .onSuccess { state = state.copy(notice = "Haftalık görev ödülü hesabına işlendi.") }
                    .onFailure { e ->
                        state = state.copy(notice = when {
                            "goal_already_claimed" in e.message.orEmpty() -> "Bu görev ödülü zaten alındı."
                            "goal_not_complete" in e.message.orEmpty() -> "Görev henüz tamamlanmadı."
                            else -> "Görev ödülü alınamadı."
                        })
                    }
                reload(false)
                state = state.copy(isActionBusy = false)
            }
        },
        onClaimDailyGoal = { goalId ->
            if (state.isActionBusy) return@V10HomeScreen
            scope.launch {
                state = state.copy(isActionBusy = true, notice = "")
                runCatching { backend.claimDailyGoalV10(goalId) }
                    .onSuccess { state = state.copy(notice = "Günlük görev ödülü hesabına işlendi.") }
                    .onFailure { e ->
                        state = state.copy(notice = when {
                            "goal_already_claimed" in e.message.orEmpty() -> "Bu günlük ödül zaten alındı."
                            "goal_not_complete" in e.message.orEmpty() -> "Günlük görev henüz tamamlanmadı."
                            else -> "Günlük görev ödülü alınamadı."
                        })
                    }
                reload(false)
                state = state.copy(isActionBusy = false)
            }
        },
    )

    if (showGameModePicker) {
        AlertDialog(
            onDismissRequest = { showGameModePicker = false },
            title = { Text("Oyun Modu") },
            text = { Text("Normal mod klasik Son Harf kurallarıyla oynanır. Uzman mod daha kısa süre, daha stratejik bot ve gelişmiş son-harf baskısıyla oynanır.") },
            confirmButton = {
                Button(onClick = { showGameModePicker = false; onStartGameMode("EXPERT_MATCH") }) { Text("UZMAN") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showGameModePicker = false; onStartGameMode("NORMAL_MATCH") }) { Text("NORMAL") }
            },
        )
    }

    if (showVip) {
        VipPurchaseDialog(
            onVerified = { scope.launch { reload(false) } },
            onDismiss = { showVip = false },
        )
    }
}
