package com.sonharf.game

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal const val WORD_SIEGE_BOT_FALLBACK_DELAY_MS = 15_000L

@Composable
internal fun WordSiegePanMatch(
    game: WordSiegeGameDto,
    me: String?,
    profiles: Map<String, ProfileDto>,
    moves: List<WordSiegeMoveDto>,
    placements: Map<Int, Int>,
    selectedRackIndex: Int?,
    busy: Boolean,
    notice: String?,
    onBack: () -> Unit,
    onBoardCell: (Int) -> Unit,
    onRackTile: (Int) -> Unit,
    onSubmit: () -> Unit,
    onPass: () -> Unit,
    onExchange: () -> Unit,
    onChat: () -> Unit,
    onForfeit: () -> Unit,
    onCancelWaiting: () -> Unit,
) {
    val mine = me?.let(profiles::get)
    val opponentId = if (me == game.playerOneId) game.playerTwoId else game.playerOneId
    val opponent = opponentId?.let(profiles::get)
    val myOwner = if (me == game.playerOneId) 1 else 2
    val rivalOwner = if (myOwner == 1) 2 else 1
    val myTurn = game.status == "playing" && game.currentPlayerId == me
    val rack = if (me == game.playerOneId) game.playerOneRack else game.playerTwoRack.orEmpty()
    val canAct = myTurn && !busy
    val lastMove = moves.lastOrNull()
    val myZoneScore = panSiegeZoneScore(game, myOwner)
    val rivalZoneScore = panSiegeZoneScore(game, rivalOwner)
    val myTargetScore = WordSiegeZoneRules.totalScore(panSiegeWordScore(game, myOwner), myZoneScore)
    val rivalTargetScore = WordSiegeZoneRules.totalScore(panSiegeWordScore(game, rivalOwner), rivalZoneScore)
    val displayedMyScore by animateIntAsState(myTargetScore, tween(260), label = "siege-my-score")
    val displayedRivalScore by animateIntAsState(rivalTargetScore, tween(260), label = "siege-rival-score")
    val myMeter = panSiegeConquestMeter(game, myOwner)
    val rivalMeter = panSiegeConquestMeter(game, rivalOwner)
    val myOnslaught = panSiegeOnslaught(game, myOwner)
    val rivalOnslaught = panSiegeOnslaught(game, rivalOwner)
    var fallbackPracticeActive by remember(game.id) { mutableStateOf(false) }
    var shuffleSeed by remember(game.id) { mutableIntStateOf(0) }
    var turnSecondsLeft by remember(game.id) { mutableIntStateOf(WordSiegeZoneRules.TurnSeconds) }
    var timeoutKeyDispatched by remember(game.id) { mutableStateOf<String?>(null) }
    var urgentKeyNotified by remember(game.id) { mutableStateOf<String?>(null) }
    var actionVfxEvent by remember(game.id) { mutableIntStateOf(0) }
    var observedMoveId by remember(game.id) { mutableStateOf(lastMove?.id) }
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val timeoutBackend = remember { OnlineGameBackend() }
    val rackOrder = remember(rack, shuffleSeed) {
        if (shuffleSeed == 0) rack.indices.toList() else wordSiegeShuffledRackIndices(rack.length, shuffleSeed)
    }
    val readyFeedback = wordSiegeValidationFeedback(
        placementsCount = placements.size,
        turkish = !SonHarfUiState.isEnglish,
    )
    val previewZoneIds = remember(game.board, placements, myOwner) {
        WordSiegeZoneRules.touchedZoneIds(placements.keys)
            .filterTo(linkedSetOf()) { WordSiegeZoneRules.zoneOwner(game.board, it) != myOwner }
    }
    val previewZoneScore = remember(game.board, placements, myOwner) {
        WordSiegeZoneRules.previewZoneGain(game.board, myOwner, placements.keys)
    }

    LaunchedEffect(game.id, game.status) {
        if (game.status == "waiting") {
            delay(WORD_SIEGE_BOT_FALLBACK_DELAY_MS)
            fallbackPracticeActive = true
        } else {
            fallbackPracticeActive = false
        }
    }

    LaunchedEffect(lastMove?.id) {
        if (lastMove?.id != null && lastMove.id != observedMoveId) {
            observedMoveId = lastMove.id
            actionVfxEvent += 1
        }
    }

    LaunchedEffect(game.turnDeadline, game.currentPlayerId, game.status, game.moveCount) {
        if (game.status != "playing" || game.currentPlayerId == null) {
            turnSecondsLeft = 0
            return@LaunchedEffect
        }
        val deadline = panSiegeDeadline(game)
        while (true) {
            val left = if (deadline != null) {
                Duration.between(Instant.now(), deadline).seconds.coerceAtLeast(0).toInt()
            } else {
                WordSiegeZoneRules.TurnSeconds
            }
            turnSecondsLeft = left.coerceIn(0, WordSiegeZoneRules.TurnSeconds)
            val turnKey = "${game.moveCount}:${game.currentPlayerId}"
            if (myTurn && turnSecondsLeft <= WordSiegeZoneRules.UrgentTurnSeconds && urgentKeyNotified != turnKey) {
                urgentKeyNotified = turnKey
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                SonHarfSoundFx.warning()
            }
            if (turnSecondsLeft <= 0) {
                if (timeoutKeyDispatched != turnKey) {
                    timeoutKeyDispatched = turnKey
                    scope.launch { runCatching { timeoutBackend.expireWordSiegeTurn(game.id) } }
                }
                break
            }
            delay(500)
        }
    }

    if (fallbackPracticeActive && game.status == "waiting") {
        WordSiegePracticeScreen(onExit = { fallbackPracticeActive = false })
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(40.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.ArrowBack, sh("Oyunlar", "Games"), tint = MainUi.Text, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(sh("KELİME KUŞATMASI", "WORD SIEGE"), color = MainUi.Text, fontSize = 14.sp, lineHeight = 15.sp, fontWeight = FontWeight.Black, maxLines = 1)
                Text(
                    if (game.status == "playing") sh("25 BÖLGE • 5 KALE • 45 SN", "25 ZONES • 5 FORTRESSES • 45 S") else panSiegeStatusLabel(game, me),
                    color = if (myTurn) MainUi.Green else MainUi.Blue,
                    fontSize = 7.sp,
                    lineHeight = 8.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                )
            }
            IconButton(onClick = onChat, enabled = game.playerTwoId != null, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Rounded.Chat, sh("Sohbet", "Chat"), tint = MainUi.Blue, modifier = Modifier.size(18.dp))
            }
            IconButton(
                onClick = { shuffleSeed = if (shuffleSeed == Int.MAX_VALUE) 1 else shuffleSeed + 1 },
                enabled = canAct && rack.length > 1,
                modifier = Modifier.size(34.dp),
            ) {
                Icon(Icons.Rounded.Shuffle, sh("Karıştır", "Shuffle"), tint = MainUi.Blue, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onForfeit, enabled = game.status == "playing" && !busy, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Rounded.Flag, sh("Pes et", "Forfeit"), tint = MainUi.Red, modifier = Modifier.size(18.dp))
            }
        }

        WordSiegeLiveRivalryBar(
            myScore = displayedMyScore,
            opponentScore = displayedRivalScore,
            modifier = Modifier.fillMaxWidth().height(24.dp).padding(horizontal = 6.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            PanSiegePlayerCard(
                profile = mine,
                fallbackName = sh("Sen", "You"),
                score = displayedMyScore,
                zoneScore = myZoneScore,
                areaCount = panSiegeAreaCount(game, myOwner),
                conquestMeter = myMeter,
                onslaughtActive = myOnslaught,
                accent = MainUi.Green,
                active = game.currentPlayerId == me,
                modifier = Modifier.weight(1f),
            )
            PanSiegePlayerCard(
                profile = opponent,
                fallbackName = if (game.status == "waiting") sh("Rakip aranıyor", "Finding rival") else sh("Rakip", "Rival"),
                score = displayedRivalScore,
                zoneScore = rivalZoneScore,
                areaCount = panSiegeAreaCount(game, rivalOwner),
                conquestMeter = rivalMeter,
                onslaughtActive = rivalOnslaught,
                accent = MainUi.Red,
                active = game.currentPlayerId == opponentId,
                modifier = Modifier.weight(1f),
            )
        }

        if (game.status == "waiting") {
            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                color = MainUi.Surface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MainUi.Border),
            ) {
                Column(
                    Modifier.fillMaxSize().padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(color = SiegePurple)
                    Spacer(Modifier.height(10.dp))
                    Text(sh("RAKİP ARANIYOR", "FINDING A RIVAL"), color = MainUi.Text, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(7.dp))
                    Text(
                        sh(
                            "15 saniye içinde rakip bulunmazsa botla hemen başlayacaksın. Gerçek rakip araması arka planda sürecek.",
                            "If no rival is found within 15 seconds, practice starts immediately with a bot while real matchmaking continues in the background.",
                        ),
                        color = MainUi.Muted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = onCancelWaiting, enabled = !busy, border = BorderStroke(1.dp, MainUi.Red)) {
                        Text(sh("ARAMAYI İPTAL ET", "CANCEL SEARCH"), color = MainUi.Red, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                    }
                }
            }
            notice?.let { PanSiegeNotice(it) }
            return@Column
        }

        WordSiegeTempoBanner(
            isMyTurn = myTurn,
            secondsLeft = turnSecondsLeft,
            opponentLabel = opponent?.displayName ?: sh("Rakip", "Rival"),
            modifier = Modifier.fillMaxWidth().height(28.dp).padding(horizontal = 6.dp),
        )

        WordSiegePracticeBoard(
            board = game.board,
            rack = rack,
            placements = placements,
            myOwner = myOwner,
            enabled = canAct,
            moveEventKey = actionVfxEvent.takeIf { it > 0 },
            resolvedIndices = lastMove?.placedTiles?.map { it.index }?.toSet() ?: emptySet(),
            modifier = Modifier.fillMaxWidth().weight(1f),
            onCell = onBoardCell,
        )

        if (game.status == "playing") {
            Row(
                modifier = Modifier.fillMaxWidth().height(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (placements.isNotEmpty()) {
                    Text(readyFeedback.message, color = MainUi.Green, fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        sh("${previewZoneIds.size} bölge • +$previewZoneScore", "${previewZoneIds.size} zones • +$previewZoneScore"),
                        color = MainUi.Gold,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(sh("Torba ${game.bag.length}", "Bag ${game.bag.length}"), color = MainUi.Muted, fontSize = 7.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                rackOrder.forEach { rackIndex ->
                    val letter = rack.getOrNull(rackIndex) ?: return@forEach
                    WordSiegePracticeRackTile(
                        letter = letter,
                        selected = selectedRackIndex == rackIndex,
                        used = rackIndex in placements.values,
                        enabled = canAct,
                        modifier = Modifier.weight(1f),
                        onClick = { onRackTile(rackIndex) },
                    )
                }
                repeat((7 - rack.length).coerceAtLeast(0)) { Spacer(Modifier.weight(1f).fillMaxHeight()) }
            }

            Row(
                modifier = Modifier.fillMaxWidth().height(44.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                OutlinedButton(
                    onClick = { placements.keys.lastOrNull()?.let(onBoardCell) },
                    enabled = canAct && placements.isNotEmpty(),
                    modifier = Modifier.weight(.85f).fillMaxHeight(),
                    contentPadding = PaddingValues(horizontal = 2.dp),
                ) {
                    Icon(Icons.Rounded.Undo, null, Modifier.size(12.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(sh("GERİ", "UNDO"), fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
                OutlinedButton(
                    onClick = onPass,
                    enabled = canAct,
                    modifier = Modifier.weight(.70f).fillMaxHeight(),
                    contentPadding = PaddingValues(horizontal = 2.dp),
                ) { Text(sh("PAS", "PASS"), fontSize = 9.sp, fontWeight = FontWeight.Black) }
                OutlinedButton(
                    onClick = onExchange,
                    enabled = canAct && game.bag.isNotEmpty(),
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    border = BorderStroke(1.dp, SiegePurple),
                    contentPadding = PaddingValues(horizontal = 2.dp),
                ) { Text(sh("DEĞİŞTİR", "EXCHANGE"), color = SiegePurple, fontSize = 8.sp, fontWeight = FontWeight.Black) }
                Button(
                    onClick = onSubmit,
                    enabled = canAct && placements.isNotEmpty(),
                    modifier = Modifier.weight(1.10f).fillMaxHeight(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MainUi.Blue,
                        contentColor = Color.White,
                        disabledContainerColor = SonHarfTheme.DisabledBackground,
                        disabledContentColor = SonHarfTheme.DisabledContent,
                    ),
                    contentPadding = PaddingValues(horizontal = 3.dp),
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text(sh("OYNA", "PLAY"), fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        } else {
            PanSiegeFinishedCard(game, me)
        }

        Box(
            modifier = Modifier.fillMaxWidth().height(30.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                notice != null -> PanSiegeNotice(notice)
                lastMove != null -> PanSiegeLastMoveInfo(lastMove)
            }
        }
    }
}

@Composable
private fun PanSiegeLastMoveInfo(move: WordSiegeMoveDto) {
    val fire = when {
        move.onslaughtTriggered -> sh(" • 🔥 HAZIR", " • 🔥 READY")
        move.onslaughtConsumed -> " • 🔥 ×2"
        else -> ""
    }
    Text(
        sh(
            "Kelime +${move.wordScore} • ${move.zonesFlipped.size} bölge +${move.areaScore}$fire",
            "Word +${move.wordScore} • ${move.zonesFlipped.size} zones +${move.areaScore}$fire",
        ),
        color = MainUi.Text,
        fontSize = 8.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun PanSiegeNotice(message: String) {
    Text(
        message,
        color = MainUi.Text,
        fontSize = 8.sp,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PanSiegePlayerCard(
    profile: ProfileDto?,
    fallbackName: String,
    score: Int,
    zoneScore: Int,
    areaCount: Int,
    conquestMeter: Int,
    onslaughtActive: Boolean,
    accent: Color,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(11.dp),
        color = if (active) accent.copy(alpha = .10f) else MainUi.Surface,
        border = BorderStroke(if (active) 1.5.dp else 1.dp, if (active) accent else MainUi.Border),
    ) {
        Row(Modifier.padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            ProfilePhotoAvatarWithGender(
                avatarPath = profile?.avatarPath,
                gender = profile?.gender,
                name = profile?.displayName ?: fallbackName,
                size = 30.dp,
                accent = accent,
                visible = profile?.avatarVisibility != "hidden",
            )
            Spacer(Modifier.width(5.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(profile?.displayName ?: fallbackName, color = MainUi.Text, fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$score", color = accent, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(5.dp))
                    Text(sh("$areaCount bölge • +$zoneScore", "$areaCount zones • +$zoneScore"), color = MainUi.Muted, fontSize = 7.sp, maxLines = 1)
                }
                WordSiegeConquestMeter(conquestMeter, onslaughtActive)
            }
        }
    }
}

@Composable
private fun PanSiegeFinishedCard(game: WordSiegeGameDto, me: String?) {
    val won = game.winnerId == me
    val draw = game.winnerId == null
    val accent = when { draw -> MainUi.Gold; won -> MainUi.Green; else -> MainUi.Red }
    Surface(
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = .08f),
        border = BorderStroke(1.dp, accent.copy(alpha = .45f)),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 9.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                when { draw -> sh("BERABERE", "DRAW"); won -> sh("KUŞATMA SENİN!", "SIEGE WON!"); else -> sh("OYUN BİTTİ", "GAME OVER") },
                color = accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
            )
            Text(sh("Sonuç = kalıcı kelime puanı + mevcut bölge puanı", "Result = permanent word score + current zone score"), color = MainUi.Muted, fontSize = 8.sp)
        }
    }
}

private fun panSiegeWordScore(game: WordSiegeGameDto, owner: Int): Int =
    if (owner == 1) game.playerOneWordScore else game.playerTwoWordScore

private fun panSiegeZoneScore(game: WordSiegeGameDto, owner: Int): Int =
    if (owner == 1) game.playerOneAreaScore else game.playerTwoAreaScore

private fun panSiegeAreaCount(game: WordSiegeGameDto, owner: Int): Int =
    if (owner == 1) game.playerOneArea else game.playerTwoArea

private fun panSiegeConquestMeter(game: WordSiegeGameDto, owner: Int): Int =
    if (owner == 1) game.playerOneConquestMeter else game.playerTwoConquestMeter

private fun panSiegeOnslaught(game: WordSiegeGameDto, owner: Int): Boolean =
    if (owner == 1) game.playerOneOnslaughtActive else game.playerTwoOnslaughtActive

private fun panSiegeDeadline(game: WordSiegeGameDto): Instant? {
    fun parse(raw: String?): Instant? {
        if (raw.isNullOrBlank()) return null
        return runCatching { Instant.parse(raw) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(raw).toInstant() }.getOrNull()
    }
    parse(game.turnDeadline)?.let { return it }
    val started = parse(game.turnStartedAt) ?: parse(game.lastMoveAt) ?: return null
    return started.plusSeconds(WordSiegeZoneRules.TurnSeconds.toLong())
}

@Composable
private fun panSiegeStatusLabel(game: WordSiegeGameDto, me: String?): String = when {
    game.status == "cancelled" -> sh("İptal edildi", "Cancelled")
    game.status != "finished" -> sh("Devam ediyor", "In progress")
    game.winnerId == null -> sh("Berabere", "Draw")
    game.winnerId == me -> sh("Kazandın", "You won")
    else -> sh("Rakip kazandı", "Rival won")
}
