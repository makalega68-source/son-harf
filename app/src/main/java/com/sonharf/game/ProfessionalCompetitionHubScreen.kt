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
import com.sonharf.game.data.MatchHistoryDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.RivalHistoryDto
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.WeeklyTournamentDto
import com.sonharf.game.data.WeeklyTournamentHistoryDto
import com.sonharf.game.data.WeeklyTournamentLeaderboardRowDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun ProfessionalCompetitionHubScreen(onBack: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        GameTopBar(
            title = gameText("Rekabet Merkezi", "Competition Hub"),
            subtitle = gameText(
                "Haftalık Kupa, sıralama ve rövanşlar",
                "Weekly Cup, ranking and rematches",
            ),
            onBack = onBack,
        )

        SegmentedGameTabs(
            labels = listOf(
                gameText("KUPA", "CUP"),
                gameText("RAKİPLER", "RIVALS"),
            ),
            selectedIndex = selectedTab,
            onSelected = { selectedTab = it },
            modifier = Modifier.padding(horizontal = GameSpacing.ScreenHorizontal, vertical = 6.dp),
        )

        Box(Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> ProfessionalWeeklyTournamentTab()
                else -> ProfessionalRivalHistoryTab()
            }
        }
    }
}

@Composable
private fun ProfessionalWeeklyTournamentTab() {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    var tournament by remember { mutableStateOf<WeeklyTournamentDto?>(null) }
    var leaderboard by remember { mutableStateOf<List<WeeklyTournamentLeaderboardRowDto>>(emptyList()) }
    var leaderboardProfiles by remember { mutableStateOf<Map<String, ProfileDto?>>(emptyMap()) }
    var history by remember { mutableStateOf<List<WeeklyTournamentHistoryDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }

    suspend fun reload() {
        val b = backend ?: run {
            loading = false
            return
        }
        loading = true
        runCatching {
            tournament = b.getWeeklyTournament()
            val nextLeaderboard = b.getWeeklyTournamentLeaderboard(50)
            leaderboard = nextLeaderboard
            val nextProfiles = leaderboardProfiles.toMutableMap()
            for (row in nextLeaderboard) {
                if (!nextProfiles.containsKey(row.userId)) {
                    nextProfiles[row.userId] = runCatching { b.getProfile(row.userId) }.getOrNull()
                }
            }
            val activeIds = nextLeaderboard.mapTo(mutableSetOf()) { it.userId }
            leaderboardProfiles = nextProfiles.filterKeys { it in activeIds }
            history = b.getWeeklyTournamentHistory(12)
        }.onFailure {
            notice = professionalCompetitionError()
        }
        loading = false
    }

    LaunchedEffect(Unit) { reload() }

    LazyColumn(
        Modifier.fillMaxSize(),
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

        tournament?.let { current ->
            item {
                ProfessionalCupHero(
                    tournament = current,
                    busy = busy,
                    onJoin = {
                        scope.launch {
                            busy = true
                            runCatching { backend?.joinWeeklyTournament() }
                                .onSuccess {
                                    notice = gameText(
                                        "Haftalık Kupaya katıldın. Bundan sonraki PvP maçların puan kazandırır.",
                                        "You joined the Weekly Cup. Your next PvP matches earn points.",
                                    )
                                    reload()
                                }
                                .onFailure { notice = professionalCompetitionError() }
                            busy = false
                        }
                    },
                )
            }

            item { ProfessionalCupRewards() }

            item {
                val canClaim = history.any { it.rewardEligible }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            busy = true
                            runCatching { backend?.claimPreviousWeeklyTournamentReward() }
                                .onSuccess { reward ->
                                    notice = if (reward != null) {
                                        gameText(
                                            "Ödül: #${reward.rank} • +${reward.rewardCoins} Son Coin",
                                            "Reward: #${reward.rank} • +${reward.rewardCoins} Son Coin",
                                        )
                                    } else {
                                        gameText("Alınabilir kupa ödülü yok.", "No cup reward is available.")
                                    }
                                    reload()
                                }
                                .onFailure { notice = professionalCompetitionError() }
                            busy = false
                        }
                    },
                    enabled = canClaim && !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = GameShapes.Medium,
                    border = BorderStroke(
                        1.dp,
                        if (canClaim) GameColors.RewardAmber else GameColors.Border,
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (canClaim) GameColors.RewardAmber else GameColors.DisabledContent,
                    ),
                ) {
                    Icon(Icons.Rounded.Redeem, null, Modifier.size(19.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        if (canClaim) gameText("KUPA ÖDÜLÜNÜ AL", "CLAIM CUP REWARD")
                        else gameText("ALINABİLİR ÖDÜL YOK", "NO REWARD TO CLAIM"),
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }

        if (notice.isNotBlank()) {
            item { CompetitionNotice(notice) }
        }

        item { GameSectionHeader(gameText("Canlı Sıralama", "Live Ranking")) }

        if (!loading && leaderboard.isEmpty()) {
            item {
                GameEmptyState(
                    icon = Icons.Rounded.Leaderboard,
                    title = gameText("Sıralama henüz oluşmadı", "Ranking is not available yet"),
                    body = gameText(
                        "İlk gerçek PvP sonuçları geldikçe sıralama burada görünecek.",
                        "The ranking will appear here as real PvP results arrive.",
                    ),
                )
            }
        } else {
            items(leaderboard, key = { it.userId }) { row ->
                ProfessionalLeaderboardRow(row, leaderboardProfiles[row.userId])
            }
        }

        item { GameSectionHeader(gameText("Kupa Geçmişim", "My Cup History")) }

        if (!loading && history.isEmpty()) {
            item {
                GameEmptyState(
                    icon = Icons.Rounded.History,
                    title = gameText("Tamamlanmış kupa yok", "No completed cups yet"),
                    body = gameText(
                        "Katıldığın haftalık kupalar tamamlandıkça burada listelenecek.",
                        "Completed Weekly Cups you joined will be listed here.",
                    ),
                )
            }
        } else {
            items(history, key = { it.tournamentId }) { item ->
                ProfessionalCupHistoryRow(item)
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun ProfessionalCupHero(
    tournament: WeeklyTournamentDto,
    busy: Boolean,
    onJoin: () -> Unit,
) {
    GameSurface(borderColor = GameColors.RewardAmber.copy(alpha = .55f), elevated = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(58.dp),
                shape = CircleShape,
                color = GameColors.RewardAmber.copy(alpha = .14f),
                border = BorderStroke(1.dp, GameColors.RewardAmber.copy(alpha = .35f)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = GameColors.RewardAmber,
                        modifier = Modifier.size(31.dp),
                    )
                }
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    tournament.name,
                    color = GameColors.TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    gameText(
                        "Ücretsiz katılım • PvP galibiyet +3 • mağlubiyet +1",
                        "Free entry • PvP win +3 • loss +1",
                    ),
                    color = GameColors.PlayGreen,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${tournament.weekStart} • ${tournament.playerCount} ${gameText("oyuncu", "players")}",
                    color = GameColors.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        if (!tournament.joined) {
            GamePrimaryButton(
                text = gameText("ÜCRETSİZ KATIL", "JOIN FREE"),
                onClick = onJoin,
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
                icon = Icons.Rounded.AddTask,
            )
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompetitionMetric(
                    value = tournament.myPoints.toString(),
                    label = gameText("PUAN", "POINTS"),
                    modifier = Modifier.weight(1f),
                )
                CompetitionMetric(
                    value = if (tournament.myRank == 0L) "—" else "#${tournament.myRank}",
                    label = gameText("SIRAN", "RANK"),
                    modifier = Modifier.weight(1f),
                )
                CompetitionMetric(
                    value = "${tournament.myWins}-${tournament.myLosses}",
                    label = "W-L",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ProfessionalCupRewards() {
    GameSurface(borderColor = GameColors.RewardAmber.copy(alpha = .28f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Redeem, null, tint = GameColors.RewardAmber, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                gameText("Kupa Ödülleri", "Cup Rewards"),
                color = GameColors.TextPrimary,
                style = MaterialTheme.typography.titleSmall,
            )
        }
        Spacer(Modifier.height(9.dp))
        Text(
            "🥇 1.000 SC   •   🥈 600 SC   •   🥉 400 SC",
            color = GameColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            gameText(
                "4–10: 150 SC • En az 1 maç oynayan diğer oyuncular: 50 SC",
                "4–10: 150 SC • Other players with at least 1 match: 50 SC",
            ),
            color = GameColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            gameText(
                "Maç oynamadan sıralama ve ödül kazanılmaz.",
                "No ranking or reward is earned without playing a match.",
            ),
            color = GameColors.TextTertiary,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun ProfessionalLeaderboardRow(
    row: WeeklyTournamentLeaderboardRowDto,
    profile: ProfileDto?,
) {
    val rankAccent = if (row.rank <= 3L) GameColors.RewardAmber else GameColors.PrimaryBlue
    GameSurface(borderColor = rankAccent.copy(alpha = if (row.rank <= 3L) .34f else .18f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = GameShapes.Pill, color = rankAccent.copy(alpha = .12f)) {
                Text(
                    "#${row.rank}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = rankAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Spacer(Modifier.width(10.dp))
            ProfilePhotoAvatar(
                avatarPath = profile?.avatarPath,
                name = row.displayName,
                size = 38.dp,
                accent = rankAccent,
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    row.displayName,
                    color = GameColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${row.leagueName} • ${row.rating} RP • ${row.wins}W/${row.losses}L",
                    color = GameColors.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                "${row.points}",
                color = GameColors.PrimaryBlue,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun ProfessionalCupHistoryRow(history: WeeklyTournamentHistoryDto) {
    val played = history.matches > 0
    val accent = when {
        history.rewardEligible -> GameColors.RewardAmber
        history.rewardClaimed -> GameColors.PlayGreen
        else -> GameColors.Border
    }

    GameSurface(borderColor = accent.copy(alpha = if (accent == GameColors.Border) 1f else .42f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = GameColors.SecondarySurface) {
                Icon(
                    Icons.Rounded.EmojiEvents,
                    null,
                    tint = if (played) GameColors.RewardAmber else GameColors.TextTertiary,
                    modifier = Modifier.padding(9.dp).size(21.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    history.name,
                    color = GameColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${history.weekStart} • ${history.participantCount} ${gameText("aktif oyuncu", "active players")}",
                    color = GameColors.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Text(
                if (history.finalRank > 0) "#${history.finalRank}" else "—",
                color = if (played) GameColors.RewardAmber else GameColors.TextTertiary,
                fontWeight = FontWeight.Black,
                fontSize = 17.sp,
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CompetitionMetric(history.points.toString(), gameText("PUAN", "POINTS"), Modifier.weight(1f))
            CompetitionMetric("${history.wins}-${history.losses}", "W-L", Modifier.weight(1f))
            CompetitionMetric(history.matches.toString(), gameText("MAÇ", "MATCHES"), Modifier.weight(1f))
        }

        Spacer(Modifier.height(8.dp))
        Text(
            when {
                !played -> gameText(
                    "Maç oynamadığın için sıralama ve ödül oluşmadı.",
                    "No ranking or reward because no match was played.",
                )
                history.rewardClaimed -> gameText(
                    "✓ +${history.rewardCoins} Son Coin alındı",
                    "✓ +${history.rewardCoins} Son Coin claimed",
                )
                history.rewardEligible -> gameText(
                    "+${history.rewardCoins} Son Coin alınabilir",
                    "+${history.rewardCoins} Son Coin available",
                )
                else -> gameText("Ödül durumu kapalı.", "Reward unavailable.")
            },
            color = when {
                history.rewardEligible -> GameColors.RewardAmber
                history.rewardClaimed -> GameColors.PlayGreen
                else -> GameColors.TextTertiary
            },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ProfessionalRivalHistoryTab() {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    var rivals by remember { mutableStateOf<List<RivalHistoryDto>>(emptyList()) }
    var matchHistory by remember { mutableStateOf<List<MatchHistoryDto>>(emptyList()) }
    var playerProfiles by remember { mutableStateOf<Map<String, ProfileDto?>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var busyOpponent by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf("") }

    suspend fun reload(showLoading: Boolean = true) {
        val b = backend ?: run {
            if (showLoading) loading = false
            return
        }
        if (showLoading) loading = true
        runCatching {
            val nextRivals = b.getRivalHistory(30)
            val nextMatches = b.getMatchHistory(30)
            rivals = nextRivals
            matchHistory = nextMatches
            val nextProfiles = playerProfiles.toMutableMap()
            val activeIds = (nextRivals.map { it.opponentId } + nextMatches.map { it.opponentId }).toSet()
            for (userId in activeIds) {
                if (!nextProfiles.containsKey(userId)) {
                    nextProfiles[userId] = runCatching { b.getProfile(userId) }.getOrNull()
                }
            }
            playerProfiles = nextProfiles.filterKeys { it in activeIds }
        }.onFailure { notice = professionalCompetitionError() }
        if (showLoading) loading = false
    }

    LaunchedEffect(Unit) {
        reload()
        while (true) {
            delay(12_000)
            reload(showLoading = false)
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = GameSpacing.ScreenHorizontal, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (loading) {
            item {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = GameColors.PrimaryBlue,
                    trackColor = GameColors.SecondarySurface,
                )
            }
        }

        item {
            GameSurface(borderColor = GameColors.PrimaryBlue.copy(alpha = .26f), elevated = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = GameColors.PrimaryBlue.copy(alpha = .14f)) {
                        Icon(
                            Icons.Rounded.Replay,
                            null,
                            tint = GameColors.PrimaryBlue,
                            modifier = Modifier.padding(11.dp).size(25.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            gameText("Rövanş Hattı", "Rematch Line"),
                            color = GameColors.TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            gameText(
                                "Gerçek PvP rakip geçmişin ve çevrimiçi arkadaş meydan okumaların.",
                                "Your real PvP rival history and online friend challenges.",
                            ),
                            color = GameColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        if (notice.isNotBlank()) item { CompetitionNotice(notice) }

        if (!loading && rivals.isEmpty()) {
            item {
                GameEmptyState(
                    icon = Icons.Rounded.SportsEsports,
                    title = gameText("Henüz rakip geçmişin yok", "No rival history yet"),
                    body = gameText(
                        "Gerçek PvP maçların tamamlandıkça rakiplerin burada görünecek.",
                        "Your rivals will appear here as real PvP matches are completed.",
                    ),
                )
            }
        } else {
            items(rivals, key = { it.opponentId }) { rival ->
                ProfessionalRivalCard(
                    rival = rival,
                    profile = playerProfiles[rival.opponentId],
                    busy = busyOpponent != null,
                    onLastLetter = {
                        scope.launch {
                            busyOpponent = rival.opponentId
                            runCatching { backend?.inviteFriend(rival.opponentId, SonHarfUiState.language) }
                                .onSuccess {
                                    notice = gameText(
                                        "${rival.displayName}: Son Harf daveti gönderildi.",
                                        "${rival.displayName}: Son Harf invite sent.",
                                    )
                                    SonHarfSoundFx.softNotify()
                                }
                                .onFailure { notice = professionalCompetitionError() }
                            busyOpponent = null
                        }
                    },
                    onArena = {
                        scope.launch {
                            busyOpponent = rival.opponentId
                            runCatching { backend?.inviteFriendToWordArena(rival.opponentId, SonHarfUiState.language) }
                                .onSuccess {
                                    notice = gameText(
                                        "${rival.displayName}: Arena daveti gönderildi.",
                                        "${rival.displayName}: Arena invite sent.",
                                    )
                                    SonHarfSoundFx.softNotify()
                                }
                                .onFailure { notice = professionalCompetitionError() }
                            busyOpponent = null
                        }
                    },
                )
            }
        }

        item { GameSectionHeader(gameText("Son Maçlar", "Recent Matches")) }

        if (!loading && matchHistory.isEmpty()) {
            item {
                GameEmptyState(
                    icon = Icons.Rounded.History,
                    title = gameText("Tamamlanmış maç yok", "No completed matches"),
                    body = gameText(
                        "Gerçek PvP maçların burada sonuç ve rating değişimiyle listelenecek.",
                        "Your real PvP matches will be listed here with result and rating change.",
                    ),
                )
            }
        } else {
            items(matchHistory, key = { "${it.mode}-${it.matchId}" }) { match ->
                ProfessionalMatchHistoryRow(match, playerProfiles[match.opponentId])
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun ProfessionalRivalCard(
    rival: RivalHistoryDto,
    profile: ProfileDto?,
    busy: Boolean,
    onLastLetter: () -> Unit,
    onArena: () -> Unit,
) {
    val online = rival.presenceStatus == "online"
    val accent = if (rival.canChallenge) GameColors.PrimaryBlue else GameColors.Border

    GameSurface(borderColor = accent.copy(alpha = if (rival.canChallenge) .45f else 1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProfilePhotoAvatar(
                avatarPath = profile?.avatarPath,
                name = rival.displayName,
                size = 46.dp,
                accent = if (online) GameColors.PlayGreen else GameColors.TextTertiary,
            )
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    rival.displayName,
                    color = GameColors.TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${rival.matches} ${gameText("maç", "matches")} • ${rival.wins}W/${rival.losses}L" +
                        if (rival.draws > 0) "/${rival.draws}D" else "",
                    color = GameColors.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${rival.myPoints}:${rival.theirPoints}",
                    color = GameColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    if (online) "● ${gameText("Çevrimiçi", "Online")}" else gameText("Çevrimdışı", "Offline"),
                    color = if (online) GameColors.PlayGreen else GameColors.TextTertiary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CompetitionMetric(
                "${rival.classicMatches}",
                "Son Harf",
                Modifier.weight(1f),
            )
            CompetitionMetric(
                "${rival.arenaMatches}",
                gameText("Arena", "Arena"),
                Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(10.dp))
        if (rival.canChallenge) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GameSecondaryButton(
                    text = "SON HARF",
                    onClick = onLastLetter,
                    modifier = Modifier.weight(1f),
                    enabled = !busy,
                    icon = Icons.Rounded.SportsEsports,
                )
                GameTertiaryButton(
                    text = "ARENA",
                    onClick = onArena,
                    modifier = Modifier.weight(1f),
                    enabled = !busy,
                    icon = Icons.Rounded.Bolt,
                )
            }
        } else {
            Text(
                when {
                    !rival.isFriend -> gameText(
                        "Canlı meydan okuma için önce arkadaş olmalısınız.",
                        "Become friends first to send a live challenge.",
                    )
                    !online -> gameText(
                        "Arkadaşın çevrimiçi olduğunda meydan okuyabilirsin.",
                        "You can challenge this friend when they are online.",
                    )
                    else -> gameText(
                        "Bu rakibe şu anda meydan okunamıyor.",
                        "This rival cannot be challenged right now.",
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                color = GameColors.TextTertiary,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ProfessionalMatchHistoryRow(match: MatchHistoryDto, profile: ProfileDto?) {
    val accent = when (match.result) {
        "win" -> GameColors.PlayGreen
        "loss" -> GameColors.Danger
        else -> GameColors.RewardAmber
    }

    GameSurface(borderColor = accent.copy(alpha = .28f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProfilePhotoAvatar(
                avatarPath = profile?.avatarPath,
                name = match.displayName,
                size = 40.dp,
                accent = accent,
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    match.displayName,
                    color = GameColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${match.playedAt.take(10)} • ${match.language.uppercase()} • " +
                        if (match.mode == "arena") "Arena" else "Son Harf",
                    color = GameColors.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    when (match.result) {
                        "win" -> gameText("GALİBİYET", "WIN")
                        "loss" -> gameText("MAĞLUBİYET", "LOSS")
                        else -> gameText("BERABERE", "DRAW")
                    },
                    color = accent,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${match.myScore}:${match.theirScore}",
                    color = GameColors.TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    (if (match.ratingDelta > 0) "+" else "") + match.ratingDelta + " RP",
                    color = if (match.ratingDelta >= 0) GameColors.PlayGreen else GameColors.Danger,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun CompetitionMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = GameShapes.Medium,
        color = GameColors.ElevatedBackground,
        border = BorderStroke(1.dp, GameColors.Divider),
    ) {
        Column(
            Modifier.padding(horizontal = 6.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                value,
                color = GameColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
            Text(
                label,
                color = GameColors.TextTertiary,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CompetitionNotice(message: String) {
    Surface(
        shape = GameShapes.Medium,
        color = GameColors.PrimaryBlue.copy(alpha = .11f),
        border = BorderStroke(1.dp, GameColors.PrimaryBlue.copy(alpha = .24f)),
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

private fun professionalCompetitionError(): String = gameText(
    "İşlem şu anda tamamlanamadı. Bağlantını kontrol edip tekrar dene.",
    "The action could not be completed. Check your connection and try again.",
)
