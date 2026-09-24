package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*

@Composable
internal fun ProfessionalLeaderboardScreen(
    backend: OnlineGameBackend,
    onBack: (() -> Unit)? = null,
    onCompetition: (() -> Unit)? = null,
) {
    var season by remember { mutableStateOf<CompetitiveSeasonDto?>(null) }
    var rows by remember { mutableStateOf<List<SeasonLeaderboardRowDto>>(emptyList()) }
    var profiles by remember { mutableStateOf<Map<String, ProfileDto?>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    val me = remember { backend.currentUserId() }
    var filter by remember { mutableIntStateOf(0) }
    var weekly by remember { mutableStateOf<List<WeeklyTopPlayerV210>>(emptyList()) }
    var friendRanking by remember { mutableStateOf<List<ProfileDto>>(emptyList()) }

    suspend fun reload() {
        loading = true
        error = false
        runCatching {
            season = backend.getCompetitiveSeason()
            val next = backend.getSeasonLeaderboard(50)
            rows = next
            val mapped = linkedMapOf<String, ProfileDto?>()
            next.take(20).forEach { row ->
                mapped[row.userId] = runCatching { backend.getProfile(row.userId) }.getOrNull()
            }
            profiles = mapped
        }.onFailure { error = true }
        weekly = runCatching { backend.getWeeklyTopV210(limit = 50) }.getOrDefault(emptyList())
        // Friends ranking: accepted friends plus the player, ordered by their real rating.
        friendRanking = runCatching {
            val mine = me?.let { id -> runCatching { backend.getProfile(id) }.getOrNull() }
            (backend.getFriends().map { it.second } + listOfNotNull(mine)).distinctBy { it.id }.sortedByDescending { it.rating }
        }.getOrDefault(emptyList())
        loading = false
    }

    LaunchedEffect(Unit) { reload() }

    Column(Modifier.fillMaxSize()) {
        GameTopBar(
            title = gameText("Lig", "League"),
            subtitle = gameText("Liderlik Tablosu • sezon, haftalık ve arkadaşlar", "Leaderboard • season, weekly and friends"),
            onBack = onBack,
        )

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

            season?.let { current ->
                item {
                    GameSurface(
                        elevated = true,
                        borderColor = GameColors.RewardAmber.copy(alpha = .45f),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = GameColors.RewardAmber.copy(alpha = .14f)) {
                                Icon(
                                    Icons.Rounded.EmojiEvents,
                                    contentDescription = null,
                                    tint = GameColors.RewardAmber,
                                    modifier = Modifier.padding(11.dp).size(28.dp),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (SonHarfUiState.isEnglish) current.nameEn else current.nameTr,
                                    color = GameColors.TextPrimary,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                )
                                Text(
                                    gameText("${current.playerCount} aktif oyuncu", "${current.playerCount} active players"),
                                    color = GameColors.TextSecondary,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    if (current.seasonRank > 0) "#${current.seasonRank}" else "—",
                                    color = GameColors.RewardAmber,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                )
                                Text(
                                    current.leagueName,
                                    color = GameColors.TextSecondary,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LeaderboardMetric(Modifier.weight(1f), current.rating.toString(), "RP", GameColors.PrimaryBlue)
                            LeaderboardMetric(Modifier.weight(1f), current.wins.toString(), gameText("Galibiyet", "Wins"), GameColors.PlayGreen)
                            LeaderboardMetric(Modifier.weight(1f), current.matches.toString(), gameText("Maç", "Matches"), GameColors.Lavender)
                        }
                        // Next target from the same thresholds as the server's league_for_rating_v1.
                        val progress = ratingLeagueProgress(current.rating)
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                current.leagueName,
                                Modifier.weight(1f),
                                color = GameColors.PrestigeGold,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                            )
                            Text(
                                progress.nextAt?.let { gameText("Sonraki hedef: $it", "Next target: $it") } ?: gameText("En yüksek lig", "Top league"),
                                color = GameColors.TextSecondary,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        LeagueProgress(progress.progress)
                        progress.nextAt?.let {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                gameText("${progress.nextLeagueName} için ${progress.pointsToNext} RP", "${progress.pointsToNext} RP to ${progress.nextLeagueName}"),
                                color = GameColors.TextTertiary,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }

            if (onCompetition != null) {
                item {
                    GameSecondaryButton(
                        text = gameText("HAFTALIK KUPA VE RAKİPLER", "WEEKLY CUP & RIVALS"),
                        onClick = onCompetition,
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Rounded.SportsEsports,
                    )
                }
            }

            item(key = "league-filter") {
                SegmentedGameTabs(
                    labels = listOf(gameText("Genel", "Overall"), gameText("Haftalık", "Weekly"), gameText("Arkadaşlar", "Friends")),
                    selectedIndex = filter,
                    onSelected = { filter = it },
                )
            }

            if (filter == 1) {
                if (!loading && weekly.isEmpty()) item {
                    GameEmptyState(
                        icon = Icons.Rounded.Leaderboard,
                        title = gameText("Haftalık sıralama henüz yok", "No weekly ranking yet"),
                        body = gameText("Bu hafta oynanan dereceli maçlar burada sıralanır.", "Ranked matches played this week are ranked here."),
                    )
                }
                itemsIndexed(weekly, key = { _, row -> "w-" + row.userId }) { index, row ->
                    LeagueRankRow(index + 1, row.username, gameText("Bu hafta", "This week"), "${row.rp} RP", row.userId == me)
                }
            }

            if (filter == 2) {
                if (!loading && friendRanking.size <= 1) item {
                    GameEmptyState(
                        icon = Icons.Rounded.Groups,
                        title = gameText("Arkadaş sıralaması boş", "No friends to rank yet"),
                        body = gameText("Arkadaş ekledikçe aranızdaki lig sıralaması burada görünür.", "Add friends to see how you rank against them."),
                    )
                }
                itemsIndexed(friendRanking, key = { _, p -> "f-" + p.id }) { index, player ->
                    LeagueRankRow(
                        index + 1,
                        player.displayName,
                        ratingLeagueProgress(player.rating).leagueName,
                        "${player.rating} RP",
                        player.id == me,
                    )
                }
            }

            if (filter == 0 && !loading && rows.isEmpty()) {
                item {
                    GameEmptyState(
                        icon = Icons.Rounded.Leaderboard,
                        title = gameText("Sıralama henüz oluşmadı", "Ranking is not available yet"),
                        body = if (error) {
                            gameText("Sıralama şu anda yenilenemedi. Daha sonra tekrar dene.", "The ranking could not be refreshed. Try again later.")
                        } else {
                            gameText("İlk dereceli maçlar oynandıkça sezon sıralaması burada oluşacak.", "The season ranking will appear as ranked matches are played.")
                        },
                    )
                }
            }

            if (filter == 0 && rows.isNotEmpty()) {
                item { ProfessionalPodium(rows.take(3), profiles, me) }
            }

            if (filter == 0 && rows.size > 3) {
                item { GameSectionHeader(gameText("Sezon Sıralaması", "Season Ranking")) }
                items(rows.drop(3), key = { it.userId }) { row ->
                    LeaderboardCompactRow(
                        row = row,
                        profile = profiles[row.userId],
                        isMe = row.userId == me,
                    )
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun ProfessionalPodium(
    rows: List<SeasonLeaderboardRowDto>,
    profiles: Map<String, ProfileDto?>,
    me: String?,
) {
    GameSurface(borderColor = GameColors.RewardAmber.copy(alpha = .28f)) {
        Text(
            gameText("İLK 3", "TOP 3"),
            color = GameColors.RewardAmber,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            val podium = listOf(2 to rows.getOrNull(1), 1 to rows.getOrNull(0), 3 to rows.getOrNull(2))
            podium.forEach { (place, row) ->
                if (row != null) {
                    val first = place == 1
                    val isMe = row.userId == me
                    val accent = when (place) {
                        1 -> GameColors.PrestigeGold
                        2 -> GameColors.TextSecondary
                        else -> GameColors.RewardAmber
                    }
                    Surface(
                        modifier = Modifier.weight(1f).then(if (first) Modifier.padding(bottom = 10.dp) else Modifier),
                        shape = GameShapes.Large,
                        color = if (isMe) GameColors.PrimaryBlue.copy(alpha = .12f) else GameColors.SecondarySurface,
                        border = BorderStroke(1.dp, if (isMe) GameColors.PrimaryBlue else accent.copy(alpha = .36f)),
                    ) {
                        Column(
                            Modifier.padding(horizontal = 6.dp, vertical = if (first) 14.dp else 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("#$place", color = accent, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            Spacer(Modifier.height(6.dp))
                            ProfilePhotoAvatarWithGender(
                                avatarPath = profiles[row.userId]?.avatarPath,
                                gender = profiles[row.userId]?.gender,
                                name = row.displayName,
                                size = if (first) 52.dp else 44.dp,
                                accent = accent,
                                visible = profiles[row.userId]?.avatarVisibility != "hidden",
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                row.displayName,
                                color = GameColors.TextPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text("${row.rating} RP", color = GameColors.TextSecondary, fontSize = 9.sp)
                            Text(row.leagueName, color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardCompactRow(
    row: SeasonLeaderboardRowDto,
    profile: ProfileDto?,
    isMe: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = GameShapes.Medium,
        color = if (isMe) GameColors.PrimaryBlue.copy(alpha = .10f) else GameColors.PrimarySurface,
        border = BorderStroke(1.dp, if (isMe) GameColors.PrimaryBlue.copy(alpha = .55f) else GameColors.Border),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "#${row.rankNo}",
                modifier = Modifier.width(38.dp),
                color = if (isMe) GameColors.PrimaryBlue else GameColors.TextSecondary,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
            )
            ProfilePhotoAvatarWithGender(
                avatarPath = profile?.avatarPath,
                gender = profile?.gender,
                name = row.displayName,
                size = 38.dp,
                accent = if (isMe) GameColors.PrimaryBlue else GameColors.TacticalTurquoise,
                visible = profile?.avatarVisibility != "hidden",
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (isMe) gameText("${row.displayName} • SEN", "${row.displayName} • YOU") else row.displayName,
                    color = GameColors.TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${row.leagueName} • ${row.wins}W/${row.losses}L",
                    color = GameColors.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Text(
                "${row.rating} RP",
                color = if (isMe) GameColors.PrimaryBlue else GameColors.TextPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun LeaderboardMetric(
    modifier: Modifier,
    value: String,
    label: String,
    accent: Color,
) {
    Surface(
        modifier = modifier,
        shape = GameShapes.Medium,
        color = GameColors.ElevatedBackground,
        border = BorderStroke(1.dp, GameColors.Divider),
    ) {
        Column(Modifier.padding(vertical = 9.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = accent, fontWeight = FontWeight.Black, fontSize = 15.sp)
            Text(label, color = GameColors.TextSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun LeagueRankRow(rank: Int, name: String, detail: String, value: String, isMe: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = GameShapes.Medium,
        color = if (isMe) GameColors.PrimaryBlue.copy(alpha = .10f) else GameColors.PrimarySurface,
        border = BorderStroke(1.dp, if (isMe) GameColors.PrimaryBlue.copy(alpha = .55f) else GameColors.Border),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "#$rank",
                modifier = Modifier.width(38.dp),
                color = when (rank) { 1 -> GameColors.PrestigeGold; 2, 3 -> GameColors.RewardAmber; else -> GameColors.TextSecondary },
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    if (isMe) gameText("$name • SEN", "$name • YOU") else name,
                    color = GameColors.TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(detail, color = GameColors.TextSecondary, style = MaterialTheme.typography.labelSmall)
            }
            Text(value, color = if (isMe) GameColors.PrimaryBlue else GameColors.TextPrimary, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }
    }
}
