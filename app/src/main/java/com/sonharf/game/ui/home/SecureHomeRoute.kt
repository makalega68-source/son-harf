package com.sonharf.game.ui.home

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sonharf.game.FriendsQuickAccessState
import com.sonharf.game.SonHarfShare
import com.sonharf.game.VipPurchaseDialog
import com.sonharf.game.claimDailyCheckin
import com.sonharf.game.getGrowthDashboard
import com.sonharf.game.data.GoalRowDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.claimGoal
import com.sonharf.game.data.getGoals
import com.sonharf.game.data.getLeaderboardV3
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * V3 home route backed only by server-authoritative RPCs.
 * Leaderboard avatars come from get_leaderboard_v3, which enforces
 * can_view_profile_photo() server-side before returning avatar_url.
 */
class SecureHomeViewModel : ViewModel() {
    private val backend = if (SupabaseProvider.configured) OnlineGameBackend() else null

    var uiState by mutableStateOf(FullHomeUiState())
        private set

    init {
        refresh()
        viewModelScope.launch {
            while (isActive) {
                delay(15_000)
                reload(showSpinner = false)
            }
        }
    }

    fun refresh() = viewModelScope.launch { reload(showSpinner = true) }

    private suspend fun reload(showSpinner: Boolean) {
        val service = backend ?: run {
            uiState = uiState.copy(isLoading = false, notice = "Sunucu bağlantısı yok.")
            return
        }
        if (showSpinner) uiState = uiState.copy(isLoading = true)

        val userId = service.currentUserId() ?: run {
            uiState = uiState.copy(isLoading = false, notice = "Oturum bilgisi bulunamadı.")
            return
        }

        val profile = runCatching { service.getProfile(userId) }.getOrNull()
        val growth = runCatching { service.getGrowthDashboard() }.getOrNull()
        val top = runCatching { service.getLeaderboardV3("tr", "week", 3) }
            .getOrDefault(emptyList())
            .mapIndexed { index, row ->
                TopPlayerUiModel(
                    rank = index + 1,
                    name = row.displayName,
                    score = row.wins,
                    photoUrl = row.avatarUrl,
                )
            }
        val friends = runCatching { service.getFriends() }.getOrDefault(emptyList())
        val goals = runCatching { service.getGoals() }.getOrDefault(emptyList())

        uiState = FullHomeUiState(
            userName = profile?.displayName ?: growth?.displayName ?: "Son Harf Oyuncusu",
            userPhotoUrl = profile?.avatarUrl,
            level = growth?.level ?: 1,
            diamonds = profile?.diamonds ?: 0,
            league = "${growth?.leagueName ?: "BRONZ"} Lig",
            topPlayers = top,
            isDailyRewardAvailable = growth?.dailyClaimed == false,
            dailyRewardDiamonds = growth?.dailyReward ?: 40,
            onlineFriendsCount = friends.count { it.second.presenceStatus == "online" },
            tasks = goals.map { it.toSecureUi() },
            isLoading = false,
            isActionBusy = uiState.isActionBusy,
            notice = uiState.notice,
        )
    }

    fun claimDailyReward() {
        val service = backend ?: return
        if (uiState.isActionBusy || !uiState.isDailyRewardAvailable) return
        viewModelScope.launch {
            uiState = uiState.copy(isActionBusy = true, notice = "")
            runCatching { service.claimDailyCheckin() }
                .onSuccess { reward ->
                    uiState = uiState.copy(
                        notice = if (reward > 0) "+$reward elmas hesabına işlendi." else "Günlük ödül daha önce alınmış."
                    )
                }
                .onFailure { uiState = uiState.copy(notice = "Günlük ödül alınamadı.") }
            reload(showSpinner = false)
            uiState = uiState.copy(isActionBusy = false)
        }
    }

    fun claimTaskReward(goalId: String) {
        val service = backend ?: return
        val task = uiState.tasks.firstOrNull { it.id == goalId } ?: return
        if (uiState.isActionBusy || task.current < task.target || task.isClaimed) return
        viewModelScope.launch {
            uiState = uiState.copy(isActionBusy = true, notice = "")
            runCatching { service.claimGoal(goalId) }
                .onSuccess { uiState = uiState.copy(notice = "Görev ödülü hesabına işlendi.") }
                .onFailure { e ->
                    uiState = uiState.copy(
                        notice = if ("goal_already_claimed" in e.message.orEmpty())
                            "Bu görev ödülü daha önce alınmış."
                        else "Görev ödülü alınamadı."
                    )
                }
            reload(showSpinner = false)
            uiState = uiState.copy(isActionBusy = false)
        }
    }

    private fun GoalRowDto.toSecureUi() = DailyTaskUiModel(
        id = id,
        title = titleTr,
        current = progress,
        target = target,
        rewardDiamonds = rewardDiamonds,
        isClaimed = claimed,
    )
}

@Composable
fun SecureV3HomeRoute(
    onStartGameMode: (String) -> Unit,
    onOpenLeague: () -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: SecureHomeViewModel = viewModel(),
) {
    val state = viewModel.uiState
    val context = LocalContext.current
    var showVip by remember { mutableStateOf(false) }

    FullHomeScreen(
        state = state,
        onStartGameMode = onStartGameMode,
        onClaimDailyReward = viewModel::claimDailyReward,
        onOpenVipModal = { showVip = true },
        onInviteFriend = { SonHarfShare.challenge(context, state.userName) },
        onOpenFriendsList = { FriendsQuickAccessState.open = true },
        onOpenLeaderboard = onOpenLeague,
        onOpenProfile = onOpenProfile,
        onClaimTaskReward = viewModel::claimTaskReward,
    )

    if (showVip) {
        VipPurchaseDialog(
            onVerified = { viewModel.refresh() },
            onDismiss = { showVip = false },
        )
    }
}
