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

@Composable
internal fun HomeSessions(backend: OnlineGameBackend, onOpen: (WordSiegeGameDto) -> Unit, onAll: () -> Unit) {
    var games by remember { mutableStateOf<List<WordSiegeGameDto>>(emptyList()) }
    var profiles by remember { mutableStateOf<Map<String, ProfileDto>>(emptyMap()) }
    var loaded by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var filter by remember { mutableStateOf<HomeTurn?>(null) }
    val me = backend.currentUserId()
    val foreground = rememberAppForeground()
    LaunchedEffect(me, foreground, retry) {
        if (me == null || !foreground) return@LaunchedEffect
        while (true) {
            coroutineScope {
                val classic = async { gameRequestResult { backend.getWordSiegeGames() } }
                val series = async { gameRequestResult { backend.getWordSiegeSeriesGames() } }
                val a = classic.await(); val b = series.await()
                failed = a.isFailure || b.isFailure
                // Do not silently erase the other pool after a partial connection failure.
                games = homeGames(a.getOrElse { games.filter { it.gameMode == "classic" } } +
                    b.getOrElse { games.filter { it.gameMode == "series" } }, me)
                games.mapNotNull { if (it.playerOneId == me) it.playerTwoId else it.playerOneId }.distinct()
                    .filterNot(profiles::containsKey).map { id -> async {
                        gameRequestResult { backend.getProfile(id) }.getOrNull()?.let { id to it }
                    } }.forEach { it.await()?.let { row -> profiles = profiles + row } }
                loaded = true
            }
            delay(15_000)
        }
    }
    Surface(shape = RoundedCornerShape(20.dp), color = Hf.Surface, border = BorderStroke(1.dp, Hf.Border)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(sh("OYUNLARIM", "MY GAMES"), Modifier.weight(1f), color = Hf.Text, fontWeight = FontWeight.Black, fontSize = 16.sp)
                TextButton(onClick = onAll) { Text(sh("TÜMÜ", "ALL"), color = Hf.Green, fontSize = 11.sp) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(null, HomeTurn.YOURS, HomeTurn.RIVAL).forEach { turn ->
                    val label = when (turn) { HomeTurn.YOURS -> sh("Sıra sende", "Your turn"); HomeTurn.RIVAL -> sh("Rakibin sırası", "Their turn"); else -> sh("Tümü", "All") }
                    FilterChip(selected = filter == turn, onClick = { filter = turn }, label = {
                        Text("$label ${games.count { turn == null || homeTurn(it, me) == turn }}", fontSize = 10.sp)
                    })
                }
            }
            if (!loaded) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Hf.Green)
            if (failed) Row(verticalAlignment = Alignment.CenterVertically) {
                Text(sh("Maçlar güncellenemedi", "Games could not be refreshed"), Modifier.weight(1f), color = Hf.Red, fontSize = 11.sp)
                TextButton(onClick = { retry++ }) { Text(sh("YENİLE", "RETRY"), fontSize = 10.sp) }
            }
            val shown = games.filter { filter == null || homeTurn(it, me) == filter }
            if (loaded && shown.isEmpty() && !failed) Text(sh("Bu bölümde bekleyen maç yok", "No games in this section"), color = Hf.TextMuted, fontSize = 12.sp)
            shown.take(5).forEach { game ->
                val id = if (game.playerOneId == me) game.playerTwoId else game.playerOneId
                val rival = id?.let(profiles::get)
                val turn = homeTurn(game, me)
                Surface(Modifier.fillMaxWidth().clickable { onOpen(game) }, shape = RoundedCornerShape(13.dp),
                    color = Hf.Ground, border = BorderStroke(1.dp, if (turn == HomeTurn.YOURS) Hf.Green.copy(alpha = .5f) else Hf.Border)) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (turn == HomeTurn.YOURS) Icons.Rounded.PlayArrow else Icons.Rounded.Schedule, null, tint = Hf.Green)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(rival?.displayName ?: if (turn == HomeTurn.WAITING) sh("Rakip aranıyor", "Finding rival") else sh("Rakip", "Rival"), color = Hf.Text, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text((if (game.gameMode == "series") sh("Hızlı Düello", "Quick Duel") else sh("Kelime Kuşatması", "Word Siege")) + " · " + when (turn) {
                                HomeTurn.YOURS -> sh("Sıra sende", "Your turn"); HomeTurn.RIVAL -> sh("Rakibin sırası", "Their turn"); else -> sh("Eşleşme", "Matchmaking")
                            }, color = Hf.TextMuted, fontSize = 10.sp)
                            game.turnDeadline?.let { Text(sh("Son hamle: ", "Turn deadline: ") + socialDate(it), color = Hf.TextMuted, fontSize = 10.sp) }
                        }
                        Icon(Icons.Rounded.ChevronRight, null, tint = Hf.Gold)
                    }
                }
            }
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
                    else sh("Sezon ödülü final sıralamasına göre belirlenir", "Season reward follows your final ranking"), color = Hf.Gold, fontSize = 11.sp)
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
