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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.WordSiegeGameDto
import com.sonharf.game.data.WordSiegeInviteDto
import com.sonharf.game.data.getIncomingWordSiegeInvites
import com.sonharf.game.data.getWordSiegeGames
import com.sonharf.game.data.inviteFriendToWordSiege
import com.sonharf.game.data.respondWordSiegeInvite
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.launch

internal enum class MatchCenterTab { ACTIVE, FINISHED, INVITES }

/** One Kelime Kuşatması match as the match center shows it; every value comes from the game row. */
internal data class SiegeMatchCard(
    val gameId: String,
    val rivalId: String?,
    val rivalName: String,
    val rivalRating: Int?,
    val rivalAvatar: String?,
    val waitingForRival: Boolean,
    val myTurn: Boolean,
    val myWordScore: Int,
    val myAreaScore: Int,
    val rivalWordScore: Int,
    val rivalAreaScore: Int,
    val myMapControl: Int,
    val rivalMapControl: Int,
    val lastMoveAt: String?,
    val result: String?,
) {
    val myTotal: Int get() = myWordScore + myAreaScore
    val rivalTotal: Int get() = rivalWordScore + rivalAreaScore
}

internal data class SiegeInviteCard(val inviteId: String, val senderName: String, val language: String)

/**
 * Builds a match card from the server's game row. Area points are the server's own
 * (cells x 2); map control is the share of the board's cells a player holds.
 */
internal fun siegeMatchCard(game: WordSiegeGameDto, me: String?, profiles: Map<String, ProfileDto>): SiegeMatchCard {
    val iAmOne = game.playerOneId == me
    val rivalId = if (iAmOne) game.playerTwoId else game.playerOneId
    val rival = rivalId?.let(profiles::get)
    fun control(cells: Int) = ((cells * 100f) / WordSiegeBoardSpec.CellCount).toInt().coerceIn(0, 100)
    return SiegeMatchCard(
        gameId = game.id,
        rivalId = rivalId,
        rivalName = rival?.displayName ?: gameText("Rakip", "Rival"),
        rivalRating = rival?.rating,
        rivalAvatar = rival?.avatarPath,
        waitingForRival = game.status == "waiting",
        myTurn = game.status == "playing" && game.currentPlayerId == me,
        myWordScore = if (iAmOne) game.playerOneWordScore else game.playerTwoWordScore,
        myAreaScore = if (iAmOne) game.playerOneAreaScore else game.playerTwoAreaScore,
        rivalWordScore = if (iAmOne) game.playerTwoWordScore else game.playerOneWordScore,
        rivalAreaScore = if (iAmOne) game.playerTwoAreaScore else game.playerOneAreaScore,
        myMapControl = control(if (iAmOne) game.playerOneArea else game.playerTwoArea),
        rivalMapControl = control(if (iAmOne) game.playerTwoArea else game.playerOneArea),
        lastMoveAt = game.lastMoveAt ?: game.updatedAt.ifBlank { null },
        result = when {
            game.status != "finished" -> null
            game.winnerId == null -> "draw"
            game.winnerId == me -> "win"
            else -> "loss"
        },
    )
}

/** "3 dk", "5 sa", "2 gün" since an ISO timestamp; null when it cannot be read. */
internal fun siegeElapsedLabel(iso: String?, now: Instant = Instant.now()): String? {
    val then = iso?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
    val minutes = Duration.between(then, now).toMinutes().coerceAtLeast(0)
    return when {
        minutes < 1 -> gameText("az önce", "just now")
        minutes < 60 -> gameText("$minutes dk önce", "${minutes}m ago")
        minutes < 60 * 24 -> gameText("${minutes / 60} sa önce", "${minutes / 60}h ago")
        else -> gameText("${minutes / (60 * 24)} gün önce", "${minutes / (60 * 24)}d ago")
    }
}

@Composable
internal fun MatchCenterScreen(
    backend: OnlineGameBackend,
    onBack: () -> Unit,
    onOpenMatch: (String) -> Unit,
    onQuickMatch: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val me = remember { backend.currentUserId() }
    var tab by remember { mutableStateOf(MatchCenterTab.ACTIVE) }
    var games by remember { mutableStateOf<List<WordSiegeGameDto>>(emptyList()) }
    var invites by remember { mutableStateOf<List<WordSiegeInviteDto>>(emptyList()) }
    var profiles by remember { mutableStateOf<Map<String, ProfileDto>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(reload) {
        loading = true
        games = runCatching { backend.getWordSiegeGames() }.getOrDefault(games)
        invites = runCatching { backend.getIncomingWordSiegeInvites() }.getOrDefault(invites)
        val ids = (games.flatMap { listOf(it.playerOneId, it.playerTwoId) } + invites.map { it.senderId })
            .filterNotNull().filter { it != me }.distinct().filterNot(profiles::containsKey)
        val loaded = ids.mapNotNull { id -> runCatching { id to backend.getProfile(id) }.getOrNull() }
        profiles = profiles + loaded
        loading = false
    }

    val cards = games.map { siegeMatchCard(it, me, profiles) }
    MatchCenterContent(
        tab = tab,
        onTab = { tab = it },
        active = cards.filter { it.result == null }.sortedByDescending { it.myTurn },
        finished = cards.filter { it.result != null },
        invites = invites.map { invite ->
            SiegeInviteCard(invite.id, profiles[invite.senderId]?.displayName ?: gameText("Oyuncu", "Player"), invite.language)
        },
        loading = loading,
        busy = busy,
        notice = notice,
        onBack = onBack,
        onRefresh = { reload++ },
        onOpenMatch = onOpenMatch,
        onQuickMatch = onQuickMatch,
        onRematch = { card ->
            val rivalId = card.rivalId ?: return@MatchCenterContent
            busy = true
            scope.launch {
                notice = runCatching { backend.inviteFriendToWordSiege(rivalId, if (SonHarfUiState.isEnglish) "en" else "tr") }
                    .fold(
                        onSuccess = { gameText("Rövanş daveti gönderildi.", "Rematch invite sent.") },
                        onFailure = { error ->
                            val raw = error.message.orEmpty()
                            if ("not_friends" in raw) {
                                gameText("Rövanş daveti için önce arkadaş olmalısınız.", "You need to be friends to send a rematch.")
                            } else wordSiegeFriendlyError(raw)
                        },
                    )
                busy = false
            }
        },
        onRespond = { invite, accept ->
            busy = true
            scope.launch {
                runCatching { backend.respondWordSiegeInvite(invite.inviteId, accept) }
                    .onSuccess { game ->
                        invites = invites.filterNot { it.id == invite.inviteId }
                        if (game != null) onOpenMatch(game.id) else reload++
                    }
                    .onFailure { notice = wordSiegeFriendlyError(it.message.orEmpty()) }
                busy = false
            }
        },
    )
}

@Composable
internal fun MatchCenterContent(
    tab: MatchCenterTab,
    onTab: (MatchCenterTab) -> Unit,
    active: List<SiegeMatchCard>,
    finished: List<SiegeMatchCard>,
    invites: List<SiegeInviteCard>,
    loading: Boolean,
    busy: Boolean,
    notice: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenMatch: (String) -> Unit,
    onQuickMatch: () -> Unit,
    onRematch: (SiegeMatchCard) -> Unit,
    onRespond: (SiegeInviteCard, Boolean) -> Unit,
    now: Instant = Instant.now(),
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = GameSpacing.ScreenHorizontal, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "mc-top") {
            GameTopBar(
                title = gameText("Maçlarım", "My Matches"),
                subtitle = gameText("Kelime Kuşatması maç merkezi", "Kelime Kuşatması match center"),
                onBack = onBack,
                trailing = {
                    GameIconButton(icon = Icons.Rounded.Refresh, description = gameText("Yenile", "Refresh"), onClick = onRefresh, enabled = !loading)
                },
            )
        }
        item(key = "mc-tabs") {
            SegmentedGameTabs(
                labels = listOf(
                    gameText("AKTİF", "ACTIVE") + if (active.isNotEmpty()) " ${active.size}" else "",
                    gameText("TAMAMLANAN", "FINISHED"),
                    gameText("DAVETLER", "INVITES") + if (invites.isNotEmpty()) " ${invites.size}" else "",
                ),
                selectedIndex = tab.ordinal,
                onSelected = { onTab(MatchCenterTab.entries[it]) },
            )
        }
        if (loading) {
            item(key = "mc-loading") {
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = GameColors.PrimaryBlue, trackColor = GameColors.SecondarySurface)
            }
        }
        notice?.let { message ->
            item(key = "mc-notice") {
                Text(message, color = GameColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
        when (tab) {
            MatchCenterTab.ACTIVE -> {
                if (active.isEmpty() && !loading) {
                    item(key = "mc-active-empty") {
                        GameEmptyState(
                            icon = Icons.Rounded.GridView,
                            title = gameText("Aktif maçın yok", "No active matches"),
                            body = gameText("Hızlı eşleşme ile yeni bir kuşatma başlat.", "Start a new siege with quick match."),
                            actionText = gameText("HIZLI EŞLEŞME", "QUICK MATCH"),
                            onAction = onQuickMatch,
                        )
                    }
                }
                items(active, key = { "a-" + it.gameId }) { card -> ActiveMatchCard(card, now) { onOpenMatch(card.gameId) } }
            }
            MatchCenterTab.FINISHED -> {
                if (finished.isEmpty() && !loading) {
                    item(key = "mc-finished-empty") {
                        GameEmptyState(
                            icon = Icons.Rounded.EmojiEvents,
                            title = gameText("Henüz biten maç yok", "No finished matches yet"),
                            body = gameText("Tamamlanan kuşatmalar burada özetlenir.", "Finished sieges are summarised here."),
                        )
                    }
                }
                items(finished, key = { "f-" + it.gameId }) { card ->
                    FinishedMatchCard(card, busy, onSummary = { onOpenMatch(card.gameId) }, onRematch = { onRematch(card) })
                }
            }
            MatchCenterTab.INVITES -> {
                if (invites.isEmpty() && !loading) {
                    item(key = "mc-invites-empty") {
                        GameEmptyState(
                            icon = Icons.Rounded.MarkEmailRead,
                            title = gameText("Bekleyen davet yok", "No pending invites"),
                            body = gameText("Arkadaşların seni kuşatmaya davet ettiğinde burada görünür.", "Siege invites from friends appear here."),
                        )
                    }
                }
                items(invites, key = { "i-" + it.inviteId }) { invite -> InviteCard(invite, busy, onRespond) }
            }
        }
        item { Spacer(Modifier.height(6.dp)) }
    }
}

@Composable
private fun MatchRivalHeader(card: SiegeMatchCard, trailing: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FramedProfilePhotoAvatar(
            avatarPath = card.rivalAvatar,
            gender = null,
            name = card.rivalName,
            size = 42.dp,
            frameId = null,
            accent = GameColors.PrimaryBlue,
            showGenderBadge = false,
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(card.rivalName, color = GameColors.TextPrimary, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            card.rivalRating?.let { rating ->
                Text(
                    "${ratingLeagueProgress(rating).leagueName} • $rating RP",
                    color = GameColors.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        trailing()
    }
}

@Composable
private fun ScoreLine(label: String, mine: String, theirs: String, accent: Color = GameColors.TextPrimary) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = GameColors.TextSecondary, style = MaterialTheme.typography.labelSmall)
        Text(mine, color = accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text("  –  ", color = GameColors.TextTertiary, fontSize = 12.sp)
        Text(theirs, color = GameColors.TextSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun MapControlBar(mine: Int, theirs: Int) {
    Row(
        Modifier.fillMaxWidth().height(8.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        val neutral = (100 - mine - theirs).coerceAtLeast(0)
        if (mine > 0) Surface(Modifier.weight(mine.toFloat()).fillMaxHeight(), shape = GameShapes.Pill, color = GameColors.TacticalTurquoise) {}
        if (neutral > 0) Surface(Modifier.weight(neutral.toFloat()).fillMaxHeight(), shape = GameShapes.Pill, color = GameColors.SecondarySurface) {}
        if (theirs > 0) Surface(Modifier.weight(theirs.toFloat()).fillMaxHeight(), shape = GameShapes.Pill, color = GameColors.Danger.copy(alpha = .8f)) {}
    }
}

@Composable
private fun ActiveMatchCard(card: SiegeMatchCard, now: Instant, onOpen: () -> Unit) {
    val accent = when {
        card.waitingForRival -> GameColors.RewardAmber
        card.myTurn -> GameColors.PlayGreen
        else -> GameColors.PrimaryBlue
    }
    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = GameShapes.Large,
        color = GameColors.PrimarySurface,
        border = BorderStroke(1.dp, accent.copy(alpha = if (card.myTurn) .6f else .3f)),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            if (card.waitingForRival) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.HourglassTop, null, tint = accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(gameText("Rakip aranıyor", "Finding a rival"), Modifier.weight(1f), color = GameColors.TextPrimary, style = MaterialTheme.typography.titleSmall)
                    Text(gameText("AÇ", "OPEN"), color = accent, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
                return@Column
            }
            MatchRivalHeader(card) {
                Surface(shape = GameShapes.Pill, color = accent.copy(alpha = .15f)) {
                    Text(
                        if (card.myTurn) gameText("SIRA SENDE", "YOUR TURN") else gameText("RAKİPTE", "THEIR TURN"),
                        Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        color = accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            ScoreLine(gameText("Toplam skor", "Total score"), card.myTotal.toString(), card.rivalTotal.toString(), GameColors.TacticalTurquoise)
            MapControlBar(card.myMapControl, card.rivalMapControl)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    gameText("Harita %${card.myMapControl} – %${card.rivalMapControl}", "Map ${card.myMapControl}% – ${card.rivalMapControl}%"),
                    Modifier.weight(1f),
                    color = GameColors.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
                siegeElapsedLabel(card.lastMoveAt, now)?.let {
                    Text(gameText("Son hamle $it", "Last move $it"), color = GameColors.TextTertiary, style = MaterialTheme.typography.labelSmall)
                }
            }
            GamePrimaryButton(
                text = if (card.myTurn) gameText("DEVAM ET", "CONTINUE") else gameText("MAÇI AÇ", "OPEN MATCH"),
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun FinishedMatchCard(card: SiegeMatchCard, busy: Boolean, onSummary: () -> Unit, onRematch: () -> Unit) {
    val (label, accent) = when (card.result) {
        "win" -> gameText("KAZANDIN", "YOU WON") to GameColors.PlayGreen
        "loss" -> gameText("KAYBETTİN", "YOU LOST") to GameColors.Danger
        else -> gameText("BERABERE", "DRAW") to GameColors.RewardAmber
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = GameShapes.Large,
        color = GameColors.PrimarySurface,
        border = BorderStroke(1.dp, accent.copy(alpha = .35f)),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MatchRivalHeader(card) {
                Text(label, color = accent, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
            ScoreLine(gameText("Kelime puanı", "Word points"), card.myWordScore.toString(), card.rivalWordScore.toString())
            ScoreLine(gameText("Bölge puanı", "Territory points"), card.myAreaScore.toString(), card.rivalAreaScore.toString())
            ScoreLine(gameText("Toplam skor", "Total score"), card.myTotal.toString(), card.rivalTotal.toString(), accent)
            MapControlBar(card.myMapControl, card.rivalMapControl)
            Text(
                gameText("Harita kontrolü %${card.myMapControl} – %${card.rivalMapControl}", "Map control ${card.myMapControl}% – ${card.rivalMapControl}%"),
                color = GameColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GameSecondaryButton(
                    text = gameText("MAÇ ÖZETİ", "SUMMARY"),
                    onClick = onSummary,
                    modifier = Modifier.weight(1f),
                )
                GamePrimaryButton(
                    text = gameText("RÖVANŞ", "REMATCH"),
                    onClick = onRematch,
                    modifier = Modifier.weight(1f),
                    enabled = !busy && card.rivalId != null,
                )
            }
        }
    }
}

@Composable
private fun InviteCard(invite: SiegeInviteCard, busy: Boolean, onRespond: (SiegeInviteCard, Boolean) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = GameShapes.Large,
        color = GameColors.PrimarySurface,
        border = BorderStroke(1.dp, GameColors.TacticalTurquoise.copy(alpha = .35f)),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = GameColors.TacticalTurquoise.copy(alpha = .15f)) {
                    Icon(Icons.Rounded.GridView, null, tint = GameColors.TacticalTurquoise, modifier = Modifier.padding(9.dp).size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(invite.senderName, color = GameColors.TextPrimary, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        gameText("Seni Kelime Kuşatması'na davet etti • ${invite.language.uppercase()}", "Invited you to a siege • ${invite.language.uppercase()}"),
                        color = GameColors.TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GameTertiaryButton(gameText("REDDET", "DECLINE"), { onRespond(invite, false) }, Modifier.weight(1f), enabled = !busy)
                GamePrimaryButton(gameText("KABUL ET", "ACCEPT"), { onRespond(invite, true) }, Modifier.weight(1f), enabled = !busy)
            }
        }
    }
}
