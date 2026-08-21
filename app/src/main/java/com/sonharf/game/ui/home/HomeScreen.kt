package com.sonharf.game.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.sonharf.game.FriendsQuickAccessState
import com.sonharf.game.SonHarfShare
import com.sonharf.game.VipPurchaseDialog
import com.sonharf.game.claimDailyCheckin
import com.sonharf.game.getGrowthDashboard
import com.sonharf.game.data.GoalRowDto
import com.sonharf.game.data.LeaderboardV2Row
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.claimGoal
import com.sonharf.game.data.getGoals
import com.sonharf.game.data.getLeaderboardV2
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object SonHarfHomePalette {
    val ScreenBackground = Color(0xFFF8FAFC)
    val SurfaceWhite = Color(0xFFFFFFFF)
    val SkyBluePrimary = Color(0xFF0284C7)
    val SkyBlueLight = Color(0xFFE0F2FE)
    val SkyBlueDark = Color(0xFF0369A1)
    val TextDark = Color(0xFF0F172A)
    val TextMuted = Color(0xFF475569)
    val BorderSubtle = Color(0xFFCBD5E1)
    val GoldCrown = Color(0xFFEAB308)
    val SilverMedal = Color(0xFF94A3B8)
    val BronzeMedal = Color(0xFFB45309)
    val VipPurple = Color(0xFF7C3AED)
    val VipPurpleLight = Color(0xFFEDE9FE)
    val SuccessGreen = Color(0xFF16A34A)
}

data class TopPlayerUiModel(val rank: Int, val name: String, val score: Int, val photoUrl: String?)
data class DailyTaskUiModel(
    val id: String,
    val title: String,
    val current: Int,
    val target: Int,
    val rewardDiamonds: Int,
    val isClaimed: Boolean,
)

data class FullHomeUiState(
    val userName: String = "Son Harf Oyuncusu",
    val userPhotoUrl: String? = null,
    val level: Int = 1,
    val diamonds: Int = 0,
    val league: String = "BRONZ Lig",
    val topPlayers: List<TopPlayerUiModel> = emptyList(),
    val isDailyRewardAvailable: Boolean = false,
    val dailyRewardDiamonds: Int = 40,
    val onlineFriendsCount: Int = 0,
    val tasks: List<DailyTaskUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val isActionBusy: Boolean = false,
    val notice: String = "",
)

class HomeViewModel : ViewModel() {
    private val backend = if (SupabaseProvider.configured) OnlineGameBackend() else null
    var uiState by mutableStateOf(FullHomeUiState())
        private set

    init {
        refresh()
        viewModelScope.launch {
            while (isActive) {
                delay(15_000)
                reload(false)
            }
        }
    }

    fun refresh() = viewModelScope.launch { reload(true) }

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
        val board = runCatching { service.getLeaderboardV2("tr", "week", 3) }.getOrDefault(emptyList())
        val top = board.mapIndexed { index, row ->
            val p = runCatching { service.getProfile(row.userId) }.getOrNull()
            row.toUi(index + 1, p)
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
            tasks = goals.map { it.toUi() },
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
                .onSuccess { reward -> uiState = uiState.copy(notice = if (reward > 0) "+$reward elmas hesabına işlendi." else "Günlük ödül daha önce alınmış.") }
                .onFailure { uiState = uiState.copy(notice = "Günlük ödül alınamadı.") }
            reload(false)
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
                .onFailure { e -> uiState = uiState.copy(notice = if ("goal_already_claimed" in e.message.orEmpty()) "Bu görev ödülü daha önce alınmış." else "Görev ödülü alınamadı.") }
            reload(false)
            uiState = uiState.copy(isActionBusy = false)
        }
    }

    private fun LeaderboardV2Row.toUi(rank: Int, profile: ProfileDto?) = TopPlayerUiModel(rank, displayName, wins, profile?.avatarUrl)
    private fun GoalRowDto.toUi() = DailyTaskUiModel(id, titleTr, progress, target, rewardDiamonds, claimed)
}

@Composable
fun V3HomeRoute(
    onStartGameMode: (String) -> Unit,
    onOpenLeague: () -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
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
    if (showVip) VipPurchaseDialog(onVerified = { viewModel.refresh() }, onDismiss = { showVip = false })
}

@Composable
fun FullHomeScreen(
    state: FullHomeUiState,
    onStartGameMode: (String) -> Unit,
    onClaimDailyReward: () -> Unit,
    onOpenVipModal: () -> Unit,
    onInviteFriend: () -> Unit,
    onOpenFriendsList: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenProfile: () -> Unit,
    onClaimTaskReward: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        Box(modifier.fillMaxSize().background(SonHarfHomePalette.ScreenBackground), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SonHarfHomePalette.SkyBluePrimary)
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize().background(SonHarfHomePalette.ScreenBackground),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp),
    ) {
        item { ModernBrandLogo() }
        item { UserTopBar(state, onOpenProfile) }
        item { TopThreePodiumCard(state.topPlayers, onOpenLeaderboard) }
        item { RewardAndVipRow(state, onClaimDailyReward, onOpenVipModal) }
        if (state.notice.isNotBlank()) item { NoticeCard(state.notice) }
        item { GameModesSection(onStartGameMode) }
        item { FriendsActionRow(state.onlineFriendsCount, onInviteFriend, onOpenFriendsList) }
        item { DailyTasksSection(state.tasks, state.isActionBusy, onClaimTaskReward) }
    }
}

@Composable
private fun ModernBrandLogo() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        BrandTile("S", SonHarfHomePalette.SkyBlueLight, SonHarfHomePalette.SkyBluePrimary)
        Spacer(Modifier.width(7.dp))
        Text("SON HARF", fontSize = 22.sp, fontWeight = FontWeight.Black, color = SonHarfHomePalette.TextDark, letterSpacing = 2.sp)
        Spacer(Modifier.width(7.dp))
        BrandTile("F", SonHarfHomePalette.GoldCrown.copy(alpha = .14f), SonHarfHomePalette.GoldCrown)
    }
}

@Composable
private fun BrandTile(letter: String, background: Color, border: Color) {
    Box(Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(background).border(2.dp, border, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
        Text(letter, color = border, fontWeight = FontWeight.Black, fontSize = 18.sp)
    }
}

@Composable
private fun UserTopBar(state: FullHomeUiState, onProfile: () -> Unit) {
    Surface(onClick = onProfile, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Color.White, border = BorderStroke(1.dp, SonHarfHomePalette.BorderSubtle)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RealAvatarImage(state.userPhotoUrl, state.userName, 50)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(state.userName, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = SonHarfHomePalette.TextDark)
                Text("Seviye ${state.level} • ${state.league}", fontSize = 13.sp, color = SonHarfHomePalette.TextMuted)
            }
            Surface(shape = RoundedCornerShape(20.dp), color = SonHarfHomePalette.SkyBlueLight) {
                Row(Modifier.padding(horizontal = 11.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Diamond, "Elmas", tint = SonHarfHomePalette.SkyBluePrimary, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(4.dp)); Text("${state.diamonds}", fontWeight = FontWeight.ExtraBold, color = SonHarfHomePalette.SkyBlueDark)
                }
            }
        }
    }
}

@Composable
private fun TopThreePodiumCard(players: List<TopPlayerUiModel>, onOpenLeaderboard: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Color.White, border = BorderStroke(1.dp, SonHarfHomePalette.BorderSubtle)) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Haftanın En İyi 3 Oyuncusu", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = SonHarfHomePalette.TextDark)
                TextButton(onClick = onOpenLeaderboard) { Text("Tüm Liste") }
            }
            if (players.isEmpty()) Text("Bu hafta sıralama henüz oluşmadı.", color = SonHarfHomePalette.TextMuted)
            else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                listOfNotNull(players.find { it.rank == 2 }, players.find { it.rank == 1 }, players.find { it.rank == 3 }).forEach { p ->
                    val c = when (p.rank) { 1 -> SonHarfHomePalette.GoldCrown; 2 -> SonHarfHomePalette.SilverMedal; else -> SonHarfHomePalette.BronzeMedal }
                    PodiumRankItem(p, c, if (p.rank == 1) 56 else 46)
                }
            }
        }
    }
}

@Composable
private fun PodiumRankItem(player: TopPlayerUiModel, rankColor: Color, avatarSize: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(max = 105.dp)) {
        Box(contentAlignment = Alignment.BottomCenter) {
            RealAvatarImage(player.photoUrl, player.name, avatarSize)
            Surface(shape = RoundedCornerShape(8.dp), color = rankColor, modifier = Modifier.offset(y = 6.dp)) {
                Text("${player.rank}.", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp))
            }
        }
        Spacer(Modifier.height(9.dp)); Text(player.name, maxLines = 1, textAlign = TextAlign.Center, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SonHarfHomePalette.TextDark)
        Text("${player.score} galibiyet", fontSize = 10.sp, color = SonHarfHomePalette.TextMuted)
    }
}

@Composable
private fun RewardAndVipRow(state: FullHomeUiState, onDaily: () -> Unit, onVip: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ActionCard("Günlük Ödül", if (state.isDailyRewardAvailable) "+${state.dailyRewardDiamonds} elmas • Şimdi al" else "Bugün alındı", Icons.Rounded.CardGiftcard, SonHarfHomePalette.SkyBluePrimary, Color.White, Modifier.weight(1f), state.isDailyRewardAvailable && !state.isActionBusy, onDaily)
        ActionCard("VIP Teklif", "Reklamsız + avantajlar", Icons.Rounded.WorkspacePremium, SonHarfHomePalette.VipPurple, SonHarfHomePalette.VipPurpleLight, Modifier.weight(1f), true, onVip)
    }
}

@Composable
private fun ActionCard(title: String, subtitle: String, icon: ImageVector, iconColor: Color, bg: Color, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, enabled = enabled, modifier = modifier.height(92.dp), shape = RoundedCornerShape(16.dp), color = bg, border = BorderStroke(1.dp, SonHarfHomePalette.BorderSubtle)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = iconColor.copy(alpha = .12f), modifier = Modifier.size(44.dp)) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = iconColor) } }
            Spacer(Modifier.width(9.dp)); Column { Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SonHarfHomePalette.TextDark); Text(subtitle, fontSize = 11.sp, color = SonHarfHomePalette.TextMuted) }
        }
    }
}

@Composable
private fun NoticeCard(message: String) { Surface(shape = RoundedCornerShape(12.dp), color = SonHarfHomePalette.SkyBlueLight) { Text(message, color = SonHarfHomePalette.SkyBlueDark, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.SemiBold) } }

@Composable
private fun GameModesSection(onSelectMode: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Oyun Seçenekleri", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = SonHarfHomePalette.TextDark)
        Button(onClick = { onSelectMode("1v1_RANKED") }, modifier = Modifier.fillMaxWidth().height(66.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = SonHarfHomePalette.SkyBluePrimary)) {
            Icon(Icons.Rounded.Bolt, null, modifier = Modifier.size(28.dp)); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text("1v1 Hızlı Karşılaşma", fontWeight = FontWeight.Bold, fontSize = 16.sp); Text("Gerçek oyuncu ile sıra tabanlı", fontSize = 12.sp, color = Color.White.copy(alpha = .88f)) }; Icon(Icons.Rounded.ChevronRight, null)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GameModeSmallCard("Lig Arenası", "Puan kazan, yüksel", Icons.Rounded.Shield, SonHarfHomePalette.GoldCrown, Modifier.weight(1f)) { onSelectMode("LEAGUE") }
            GameModeSmallCard("Bot ile Pratik", "Kelime hızını geliştir", Icons.Rounded.SmartToy, SonHarfHomePalette.SkyBluePrimary, Modifier.weight(1f)) { onSelectMode("PRACTICE_BOT") }
        }
    }
}

@Composable
private fun GameModeSmallCard(title: String, subtitle: String, icon: ImageVector, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.height(88.dp), shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, SonHarfHomePalette.BorderSubtle)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) { Icon(icon, null, tint = accent); Column { Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SonHarfHomePalette.TextDark); Text(subtitle, fontSize = 11.sp, color = SonHarfHomePalette.TextMuted) } }
    }
}

@Composable
private fun FriendsActionRow(onlineCount: Int, onInvite: () -> Unit, onFriends: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onInvite, modifier = Modifier.weight(1f).height(54.dp), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, SonHarfHomePalette.SkyBluePrimary)) { Icon(Icons.Rounded.PersonAdd, null); Spacer(Modifier.width(6.dp)); Text("Davet Et", fontWeight = FontWeight.Bold) }
        Button(onClick = onFriends, modifier = Modifier.weight(1f).height(54.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = SonHarfHomePalette.SkyBlueLight)) { Icon(Icons.Rounded.Groups, null, tint = SonHarfHomePalette.SkyBlueDark); Spacer(Modifier.width(6.dp)); Text("Arkadaşlar ($onlineCount)", color = SonHarfHomePalette.SkyBlueDark, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
    }
}

@Composable
private fun DailyTasksSection(tasks: List<DailyTaskUiModel>, busy: Boolean, onClaim: (String) -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Color.White, border = BorderStroke(1.dp, SonHarfHomePalette.BorderSubtle)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Günlük / Haftalık Görevler", fontWeight = FontWeight.Bold, color = SonHarfHomePalette.TextDark)
            if (tasks.isEmpty()) Text("Şu anda aktif görev bulunmuyor.", color = SonHarfHomePalette.TextMuted)
            else tasks.forEach { DailyTaskItem(it, busy) { onClaim(it.id) } }
        }
    }
}

@Composable
private fun DailyTaskItem(task: DailyTaskUiModel, busy: Boolean, onClaim: () -> Unit) {
    val progress = if (task.target <= 0) 0f else (task.current.toFloat() / task.target).coerceIn(0f, 1f)
    val completed = task.current >= task.target
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SonHarfHomePalette.ScreenBackground).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(task.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SonHarfHomePalette.TextDark); Spacer(Modifier.height(5.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(.9f).height(6.dp).clip(CircleShape), color = if (completed) SonHarfHomePalette.SuccessGreen else SonHarfHomePalette.SkyBluePrimary, trackColor = SonHarfHomePalette.BorderSubtle)
            Spacer(Modifier.height(3.dp)); Text("${task.current}/${task.target} • +${task.rewardDiamonds} elmas", fontSize = 11.sp, color = SonHarfHomePalette.TextMuted)
        }
        if (completed) Button(onClick = onClaim, enabled = !task.isClaimed && !busy, shape = RoundedCornerShape(9.dp), colors = ButtonDefaults.buttonColors(containerColor = SonHarfHomePalette.SuccessGreen, disabledContainerColor = SonHarfHomePalette.BorderSubtle), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)) { Text(if (task.isClaimed) "Alındı" else "Ödülü Al", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun RealAvatarImage(photoUrl: String?, name: String, size: Int) {
    if (!photoUrl.isNullOrBlank()) AsyncImage(model = photoUrl, contentDescription = "$name profil fotoğrafı", contentScale = ContentScale.Crop, modifier = Modifier.size(size.dp).clip(CircleShape).border(2.dp, SonHarfHomePalette.SkyBluePrimary, CircleShape))
    else Box(Modifier.size(size.dp).clip(CircleShape).background(SonHarfHomePalette.SkyBlueLight).border(2.dp, SonHarfHomePalette.SkyBluePrimary, CircleShape), contentAlignment = Alignment.Center) { Text(name.take(1).uppercase(), color = SonHarfHomePalette.SkyBlueDark, fontWeight = FontWeight.Bold, fontSize = (size / 2.2).sp) }
}
