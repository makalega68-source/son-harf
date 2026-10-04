package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.sonharf.game.data.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal enum class HomeTurn { YOURS, RIVAL, WAITING }

internal fun homeTurn(game: WordSiegeGameDto, me: String?): HomeTurn? {
    if (me == null || me !in listOf(game.playerOneId, game.playerTwoId)) return null
    return when (game.status) {
        "waiting" -> HomeTurn.WAITING
        "playing" -> if (game.currentPlayerId == me) HomeTurn.YOURS else HomeTurn.RIVAL
        else -> null
    }
}

internal fun homeGames(games: List<WordSiegeGameDto>, me: String?): List<WordSiegeGameDto> =
    games.distinctBy { it.id }.filter { homeTurn(it, me) != null }
        .sortedWith(compareBy<WordSiegeGameDto> { homeTurn(it, me)?.ordinal }
            .thenBy { it.turnDeadline ?: "9999" }.thenByDescending { it.updatedAt })

@Composable
internal fun rememberAppForeground(): Boolean {
    val lifecycle = LocalView.current.findViewTreeLifecycleOwner()?.lifecycle
    var active by remember(lifecycle) { mutableStateOf(lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) != false) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> active = lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) != false }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }
    return active
}

internal fun socialDate(value: String): String = runCatching {
    DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(ZoneId.of("Europe/Istanbul")).format(Instant.parse(value))
}.getOrDefault("—")

internal data class UnifiedHomeMatch(
    val id: String, val kind: String, val rivalId: String?, val turn: HomeTurn,
    val createdAt: String, val deadline: String?, val siege: WordSiegeGameDto?=null, val duel: GameRoomDto?=null,
)
internal fun unifiedHomeMatches(games: List<WordSiegeGameDto>, rooms: List<GameRoomDto>, me: String?): List<UnifiedHomeMatch> {
    if(me==null) return emptyList()
    val siege=homeGames(games,me).map { g -> UnifiedHomeMatch(g.id,if(g.gameMode=="series") "series" else "siege",
        if(g.playerOneId==me) g.playerTwoId else g.playerOneId,requireNotNull(homeTurn(g,me)),g.createdAt,g.turnDeadline,siege=g) }
    val duels=rooms.distinctBy { it.id }.filter { (it.hostId==me || it.guestId==me) && it.status in setOf("waiting","playing","quiz","final","sudden_death","paused") }
        .map { g -> UnifiedHomeMatch(g.id,"son_harf",if(g.hostId==me) g.guestId else g.hostId,
            if(g.status=="waiting" || g.status=="paused") HomeTurn.WAITING else if(g.currentPlayerId==me && !g.botTurn) HomeTurn.YOURS else HomeTurn.RIVAL,
            g.createdAt,g.turnDeadline,duel=g) }
    return (siege+duels).sortedWith(compareBy<UnifiedHomeMatch> { if(it.turn==HomeTurn.YOURS) 0 else 1 }.thenByDescending { it.createdAt })
}

@Composable
internal fun HomeSessions(backend: OnlineGameBackend, onOpen: (WordSiegeGameDto) -> Unit, onAll: () -> Unit,
    onLastLetter: (GameRoomDto) -> Unit) {
    var games by remember { mutableStateOf<List<WordSiegeGameDto>>(emptyList()) }
    var rooms by remember { mutableStateOf<List<GameRoomDto>>(emptyList()) }
    var profiles by remember { mutableStateOf<Map<String, ProfileDto>>(emptyMap()) }
    var loaded by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var filter by remember { mutableStateOf<HomeTurn?>(null) }
    var expanded by remember { mutableStateOf(false) }
    val me=backend.currentUserId()
    val foreground=rememberAppForeground()
    LaunchedEffect(me,foreground,retry) {
        if(me==null) { loaded=true; return@LaunchedEffect }
        if(!foreground) return@LaunchedEffect
        while(true) {
            coroutineScope {
                val classic=async { gameRequestResult { backend.getWordSiegeGameSummaries("classic", finishedLimit = 0) } }
                val series=async { gameRequestResult { backend.getWordSiegeGameSummaries("series", finishedLimit = 0) } }
                val last=async { gameRequestResult { backend.getLastLetterRooms() } }
                val a=classic.await(); val b=series.await(); val c=last.await()
                failed=a.isFailure || b.isFailure || c.isFailure
                games=homeGames(a.getOrElse { games.filter { it.gameMode=="classic" } }+b.getOrElse { games.filter { it.gameMode=="series" } },me)
                c.onSuccess { rooms=it }
                loaded=true
            }
            delay(15_000)
        }
    }
    val matches=unifiedHomeMatches(games,rooms,me)
    Surface(onClick=onAll, shape=RoundedCornerShape(20.dp), color=LobbyPalette.Soft,
        border=BorderStroke(1.dp,LobbyPalette.Line), modifier=Modifier.fillMaxWidth()) {
        Row(Modifier.padding(18.dp), verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Rounded.SportsEsports,null,tint=LobbyPalette.Accent,modifier=Modifier.size(32.dp))
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(sh("Oyunlarım","My games"),color=LobbyPalette.Ink,fontSize=18.sp,fontWeight=FontWeight.Bold)
                Text(when { !loaded -> sh("Oyunların yükleniyor…","Loading games…")
                    failed -> sh("Yenilemek için aç","Open to refresh")
                    matches.isEmpty() -> sh("Devam eden oyunun yok","No active games")
                    else -> sh("${matches.size} oyun · ${matches.count { it.turn==HomeTurn.YOURS }} sıra sende",
                        "${matches.size} games · ${matches.count { it.turn==HomeTurn.YOURS }} your turn") },
                    color=LobbyPalette.Muted,fontSize=12.sp,maxLines=2)
            }
            Icon(Icons.Rounded.ChevronRight,null,tint=LobbyPalette.Accent)
        }
    }
}

@Composable
internal fun HomeLeague(backend: OnlineGameBackend, onOpen: (() -> Unit)? = null) {
    var season by remember { mutableStateOf<CompetitiveSeasonDto?>(null) }
    var rewards by remember { mutableStateOf<List<CompetitiveSeasonHistoryDto>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    val foreground = rememberAppForeground()
    val scope = rememberCoroutineScope()
    var claiming by remember { mutableStateOf(false) }
    var claimNotice by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(foreground, retry) {
        if (!foreground) return@LaunchedEffect
        gameRequestResult { backend.getCompetitiveSeason() }.onSuccess { season = it; failed = false }.onFailure { failed = true }
        rewards = gameRequestResult { backend.getCompetitiveSeasonHistory() }.getOrNull()
    }
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = Hf.Surface, border = BorderStroke(1.dp, Hf.Gold)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.WorkspacePremium, null, tint = Hf.Gold)
                Spacer(Modifier.width(8.dp))
                Text(season?.leagueName ?: sh("LİG", "LEAGUE"), Modifier.weight(1f), color = Hf.Text, fontWeight = FontWeight.Black)
                if (onOpen != null) TextButton(onClick = onOpen) { Text(sh("REKABET", "COMPETE"), color = Hf.Green, fontSize = 10.sp) }
            }
            season?.let { s ->
                val league = ratingLeagueProgress(s.rating)
                Text("${s.rating} RP · " + if (s.nextRating == null) sh("En üst lig", "Top league") else sh("${league.nextLeagueName} için ${s.pointsToNext} RP", "${s.pointsToNext} RP to ${league.nextLeagueName}"), color = Hf.Text, fontSize = 12.sp)
                LinearProgressIndicator(progress = { league.progress }, modifier = Modifier.fillMaxWidth().height(6.dp), color = Hf.Green, trackColor = Hf.Ground)
                Text(sh("Sezon bitişi: ", "Season ends: ") + socialDate(s.endsAt), color = Hf.TextMuted, fontSize = 11.sp)
                val available = rewards?.filter { it.rewardEligible && !it.rewardClaimed }.orEmpty()
                Text(if (available.isNotEmpty()) sh("Sezon ödülü: ${available.sumOf { it.rewardCoins }} Son Coin alınabilir", "Season reward: ${available.sumOf { it.rewardCoins }} Son Coins available")
                    else sh("Ödül: sezon sıralaması", "Reward: season ranking"), color = Hf.Gold, fontSize = 11.sp)
            }
            rewards.orEmpty().filter { it.rewardEligible && !it.rewardClaimed }.forEach { reward ->
                TextButton(enabled = !claiming, onClick = {
                    claiming = true
                    scope.launch {
                        try {
                            gameRequestResult { backend.claimCompetitiveSeasonReward(reward.seasonId) }
                                .onSuccess { result ->
                                    claimNotice = if (result.success) sh("+${result.rewardCoins} Son Coin alındı", "+${result.rewardCoins} Son Coins claimed")
                                        else sh("Ödül artık alınabilir değil", "Reward is no longer claimable")
                                    retry++
                                }.onFailure { claimNotice = sh("Ödül alınamadı; tekrar dene", "Reward could not be claimed; retry") }
                        } finally { claiming = false }
                    }
                }) { Text(sh("SEZON ÖDÜLÜNÜ AL", "CLAIM SEASON REWARD"), color = Hf.Green) }
            }
            claimNotice?.let { Text(it, color = Hf.TextMuted, fontSize = 11.sp) }
            if (failed) TextButton(onClick = { retry++ }) { Text(sh("Lig yüklenemedi · Yenile", "League unavailable · Retry"), color = Hf.Red) }
            else if (season == null) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Hf.Green)
        }
    }
}

@Composable
internal fun HomeSessionFilter(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick,
        colors = FilterChipDefaults.filterChipColors(containerColor = Hf.Surface,
            labelColor = Hf.TextMuted, selectedContainerColor = SonHarfTheme.PrimarySoft,
            selectedLabelColor = Hf.Text),
        label = { Text(label, fontSize = 11.sp) })
}
