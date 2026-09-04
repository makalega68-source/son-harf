#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]


def patch(path: str, fn):
    p = ROOT / path
    before = p.read_text(encoding="utf-8")
    after = fn(before)
    if after != before:
        p.write_text(after, encoding="utf-8")
        print(f"patched {path}")
    else:
        print(f"unchanged {path}")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        return text
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one old block, found {count}")
    return text.replace(old, new, 1)


def replace_all(text: str, old: str, new: str, label: str, minimum: int = 1) -> str:
    if old not in text:
        if new in text:
            return text
        raise RuntimeError(f"{label}: old block not found")
    count = text.count(old)
    if count < minimum:
        raise RuntimeError(f"{label}: expected at least {minimum}, found {count}")
    return text.replace(old, new)


def patch_target(text: str) -> str:
    text = replace_once(
        text,
        "import androidx.activity.compose.BackHandler\n",
        "import android.os.SystemClock\nimport androidx.activity.compose.BackHandler\n",
        "Target import SystemClock",
    )
    text = replace_once(
        text,
        "    var matchJob by remember { mutableStateOf<Job?>(null) }\n    var autoStartConsumed by remember(autoStartMatchmaking) { mutableStateOf(false) }",
        "    var matchJob by remember { mutableStateOf<Job?>(null) }\n    var submittingTurnToken by remember { mutableStateOf<ClassicTurnToken?>(null) }\n    var timeoutRequestToken by remember { mutableStateOf<ClassicTurnToken?>(null) }\n    var autoStartConsumed by remember(autoStartMatchmaking) { mutableStateOf(false) }",
        "Target turn latches",
    )
    text = replace_once(
        text,
        "        roomJob = scope.launch { backend.observeRoom(r.id).catch { notice = friendly(it.message.orEmpty()) }.collect { room = it; refreshOpponent(it) } }",
        "        roomJob = scope.launch {\n            backend.observeRoom(r.id)\n                .catch { notice = friendly(it.message.orEmpty()) }\n                .collect { next ->\n                    if (classicShouldAcceptRoom(room, next)) {\n                        room = next\n                        refreshOpponent(next)\n                    }\n                }\n        }",
        "Target stale room observer",
    )
    text = replace_once(
        text,
        "                        runCatching { backend.heartbeatRoom(current.id) }\n                            .onSuccess { room = it }\n                            .onFailure { notice = friendly(it.message.orEmpty()) }",
        "                        runCatching { backend.heartbeatRoom(current.id) }\n                            .onSuccess { next -> if (classicShouldAcceptRoom(room, next)) room = next }\n                            .onFailure { notice = friendly(it.message.orEmpty()) }",
        "Target heartbeat stale filter",
    )
    text = replace_once(
        text,
        "                    runCatching { backend.botTakeTurn(active.id) }.onSuccess { room = it }.onFailure { notice = friendly(it.message.orEmpty()) }",
        "                    runCatching { backend.botTakeTurn(active.id) }\n                        .onSuccess { next -> if (classicShouldAcceptRoom(room, next)) room = next }\n                        .onFailure { notice = friendly(it.message.orEmpty()) }",
        "Target bot stale filter",
    )
    old_submit = '''                onSubmit = {\n                    scope.launch {\n                        val submitted = wordInput.trim(); if (submitted.isBlank()) return@launch\n                        wordInput = ""\n                        busy = true\n                        runCatching { backend.submitWord(active.id, submitted) }\n                            .onSuccess { updated ->\n                                room = updated\n                                val rejected = updated.lastEventPlayerId == me && updated.lastEvent in setOf(\n                                    "word_already_used", "wrong_start_letter", "not_in_dictionary", "invalid_word", "turn_expired"\n                                )\n                                notice = if (rejected) friendly(updated.lastEvent.orEmpty()) else "${submitted.uppercase()} kabul edildi"\n                            }\n                            .onFailure { notice = friendly(it.message.orEmpty()) }\n                        busy = false\n                    }\n                },\n                onTimeout = { scope.launch { runCatching { backend.claimTurnTimeout(active.id) }.onSuccess { room = it } } },'''
    new_submit = '''                submittingTurnToken = submittingTurnToken,\n                onSubmit = submit@{\n                    val submitted = wordInput.trim()\n                    val token = classicTurnToken(active)\n                    if (submitted.isBlank() || busy || submittingTurnToken == token || active.currentPlayerId != me) return@submit\n                    submittingTurnToken = token\n                    scope.launch {\n                        wordInput = ""\n                        busy = true\n                        runCatching { backend.submitWord(active.id, submitted) }\n                            .onSuccess { updated ->\n                                if (classicShouldAcceptRoom(room, updated)) room = updated\n                                val rejected = updated.lastEventPlayerId == me && updated.lastEvent in setOf(\n                                    "word_already_used", "wrong_start_letter", "not_in_dictionary", "invalid_word", "turn_expired"\n                                )\n                                notice = if (rejected) friendly(updated.lastEvent.orEmpty()) else "${submitted.uppercase()} kabul edildi"\n                            }\n                            .onFailure { error ->\n                                if (room?.let(::classicTurnToken) == token) notice = friendly(error.message.orEmpty())\n                            }\n                        if (submittingTurnToken == token) submittingTurnToken = null\n                        busy = false\n                    }\n                },\n                onTimeout = timeout@{\n                    val token = classicTurnToken(active)\n                    if (timeoutRequestToken == token) return@timeout\n                    timeoutRequestToken = token\n                    scope.launch {\n                        runCatching { backend.claimTurnTimeout(active.id) }\n                            .onSuccess { updated ->\n                                if (classicShouldAcceptRoom(room, updated)) room = updated\n                            }\n                            .onFailure {\n                                if (room?.let(::classicTurnToken) == token) {\n                                    timeoutRequestToken = null\n                                    notice = sh("Senkronize ediliyor…", "Synchronizing…")\n                                }\n                            }\n                    }\n                },'''
    text = replace_once(text, old_submit, new_submit, "Target submit/timeout state machine")
    text = replace_once(
        text,
        "    busy: Boolean,\n    onSubmit: () -> Unit,",
        "    busy: Boolean,\n    submittingTurnToken: ClassicTurnToken?,\n    onSubmit: () -> Unit,",
        "Target arena submitting parameter",
    )
    old_turn_timer = '''    val myTurn = room.currentPlayerId == me && room.status in listOf("playing", "final", "sudden_death")\n    var seconds by remember(room.turnDeadline) { mutableStateOf(45) }\n    var showChat by remember { mutableStateOf(false) }'''
    new_turn_timer = '''    val turnPhase = classicTurnPhase(room, me, submittingTurnToken)\n    val myTurn = turnPhase == ClassicTurnPhase.MY_TURN\n    var seconds by remember(room.id, room.turnDeadline, room.currentPlayerId) { mutableIntStateOf(0) }\n    var synchronizing by remember(room.id, room.turnDeadline, room.currentPlayerId) { mutableStateOf(false) }\n    var showChat by remember { mutableStateOf(false) }'''
    text = replace_once(text, old_turn_timer, new_turn_timer, "Target turn phase")
    old_timer_effect = '''    LaunchedEffect(room.turnDeadline, room.currentPlayerId, room.status) {\n        while (room.turnDeadline != null && room.status in listOf("playing", "final", "sudden_death")) {\n            seconds = runCatching {\n                (Instant.parse(room.turnDeadline).epochSecond - Instant.now().epochSecond).toInt().coerceAtLeast(0)\n            }.getOrDefault(45)\n            if (seconds <= 0) {\n                onTimeout()\n                break\n            }\n            delay(1000)\n        }\n    }'''
    new_timer_effect = '''    LaunchedEffect(room.id, room.turnDeadline, room.currentPlayerId, room.status) {\n        val anchor = classicDeadlineAnchor(\n            serverEndsAt = room.turnDeadline,\n            wallEpochMsNow = System.currentTimeMillis(),\n            elapsedRealtimeMsNow = SystemClock.elapsedRealtime(),\n        )\n        if (anchor == null || room.status !in listOf("playing", "final", "sudden_death")) {\n            seconds = 0\n            synchronizing = false\n            return@LaunchedEffect\n        }\n        var timeoutRequested = false\n        while (true) {\n            val remainingMs = classicRemainingMs(anchor, SystemClock.elapsedRealtime())\n            seconds = classicShownSeconds(remainingMs)\n            if (remainingMs <= 0L) {\n                synchronizing = true\n                if (!timeoutRequested) {\n                    timeoutRequested = true\n                    // This only asks the server to resolve an expired turn. The UI\n                    // does not switch players until an accepted server snapshot arrives.\n                    onTimeout()\n                }\n                break\n            }\n            synchronizing = false\n            delay(minOf(250L, remainingMs))\n        }\n    }'''
    text = replace_once(text, old_timer_effect, new_timer_effect, "Target monotonic timer")
    text = replace_once(
        text,
        '''                    if (myTurn) sh("SIRA SENDE", "YOUR TURN") else sh("SIRA RAKİPTE", "RIVAL'S TURN"),''',
        '''                    when {\n                        synchronizing -> sh("SENKRONİZE EDİLİYOR…", "SYNCHRONIZING…")\n                        turnPhase == ClassicTurnPhase.SUBMITTING -> sh("GÖNDERİLİYOR…", "SUBMITTING…")\n                        myTurn -> sh("SIRA SENDE", "YOUR TURN")\n                        else -> sh("SIRA RAKİPTE", "RIVAL'S TURN")\n                    },''',
        "Target status text",
    )
    text = replace_once(
        text,
        "                    fontSize = 9.sp,\n                    fontWeight = FontWeight.Black,",
        "                    fontSize = GameTypography.Action,\n                    fontWeight = FontWeight.Black,",
        "Target turn pill typography",
    )
    # Top player cards and timer readability without changing their symmetry.
    text = replace_all(text, "                        size = 36.dp,", "                        size = 50.dp,", "Target arena avatar sizes", minimum=2)
    text = replace_all(text, "fontWeight = FontWeight.Black, fontSize = 11.sp, maxLines = 1", "fontWeight = FontWeight.SemiBold, fontSize = GameTypography.PlayerName, maxLines = 1", "Target player names", minimum=2)
    text = replace_all(text, "color = TGmuted, fontSize = 8.sp, maxLines = 1", "color = TGmuted, fontSize = GameTypography.Metadata, maxLines = 1", "Target score metadata", minimum=2)
    text = replace_once(text, 'Text(sh("sn", "sec"), color = TGmuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)', 'Text(sh("sn", "sec"), color = TGmuted, fontSize = GameTypography.Metadata, fontWeight = FontWeight.Bold)', "Target timer unit")
    text = replace_once(text, 'Text("TUR ${room.roundNo}/3", color = TGmuted, fontWeight = FontWeight.Black, fontSize = 10.sp)', 'Text("TUR ${room.roundNo}/3", color = TGmuted, fontWeight = FontWeight.Black, fontSize = GameTypography.Secondary)', "Target round text")
    text = replace_once(text, 'Text(sh("SON KELİME", "CURRENT WORD"), color = TGmuted, fontSize = 8.sp, fontWeight = FontWeight.Black)', 'Text(sh("SON KELİME", "CURRENT WORD"), color = TGmuted, fontSize = GameTypography.Metadata, fontWeight = FontWeight.Black)', "Target current word label")
    text = replace_once(text, 'Text(sh("SON HARF", "LAST LETTER"), color = TGmuted, fontSize = 7.sp, fontWeight = FontWeight.Black)', 'Text(sh("SON HARF", "LAST LETTER"), color = TGmuted, fontSize = GameTypography.Metadata, fontWeight = FontWeight.Black)', "Target last letter label")
    text = replace_once(text, 'Text(sh("Sıradaki kelime bununla başlar", "Next word starts here"), color = TGmuted, fontSize = 7.sp)', 'Text(sh("Sıradaki kelime bununla başlar", "Next word starts here"), color = TGmuted, fontSize = GameTypography.Metadata)', "Target last letter help")
    text = replace_all(text, "fontWeight = FontWeight.Bold, fontSize = 11.sp", "fontWeight = FontWeight.Bold, fontSize = GameTypography.Action", "Target visible arena actions", minimum=2)
    text = replace_once(text, 'textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)', 'textStyle = LocalTextStyle.current.copy(fontSize = GameTypography.Input, fontWeight = FontWeight.SemiBold)', "Target input typography")
    text = replace_once(text, 'label = { Text(sh("Kelimen", "Your word"), fontSize = 11.sp) }', 'label = { Text(sh("Kelimen", "Your word"), fontSize = GameTypography.Metadata) }', "Target input label")
    text = replace_once(text, ') { Text("⚑ PES ET", color = TGpink, fontSize = 10.sp, fontWeight = FontWeight.Bold) }', ') { Text("⚑ PES ET", color = TGpink, fontSize = GameTypography.Action, fontWeight = FontWeight.Bold) }', "Target forfeit typography")
    text = replace_once(text, 'Text(notice, color = TGmuted, fontSize = 9.sp, lineHeight = 11.sp, textAlign = TextAlign.Center, maxLines = 2)', 'Text(notice, color = TGmuted, fontSize = GameTypography.Metadata, lineHeight = 16.sp, textAlign = TextAlign.Center, maxLines = 2)', "Target notice typography")
    text = replace_once(text, 'items(words.takeLast(30)) { w ->', 'items(items = words.takeLast(30), key = { it.id }) { w ->', "Target stable word keys")
    text = replace_once(text, 'items(chatMessages.takeLast(40)) { message ->', 'items(items = chatMessages.takeLast(40), key = { it.id }) { message ->', "Target stable chat keys")
    return text


def patch_siege_viewport(text: str) -> str:
    text = replace_once(
        text,
        "internal const val WORD_SIEGE_MIN_SCREEN_BORDER_DP = 1f\n",
        "internal const val WORD_SIEGE_MIN_SCREEN_BORDER_DP = 1f\ninternal const val WORD_SIEGE_DEFAULT_CLOSE_SCALE = 0.86f\ninternal const val WORD_SIEGE_MIN_CLOSE_SCALE = 0.78f\ninternal const val WORD_SIEGE_MAX_CLOSE_SCALE = 1.24f\n",
        "Siege viewport constants",
    )
    text = replace_once(text, "    closeScale: Float = 1f,", "    closeScale: Float = WORD_SIEGE_DEFAULT_CLOSE_SCALE,", "Siege transform default scale")
    text = replace_once(text, "    scale: Float = 1f,", "    scale: Float = WORD_SIEGE_DEFAULT_CLOSE_SCALE,", "Siege center default scale")
    return text


def patch_siege(text: str) -> str:
    text = replace_once(
        text,
        "import androidx.compose.foundation.BorderStroke\n",
        "import androidx.compose.animation.core.Animatable\nimport androidx.compose.animation.core.animateIntAsState\nimport androidx.compose.animation.core.tween\nimport androidx.compose.foundation.BorderStroke\n",
        "Siege animation imports",
    )
    text = replace_once(text, "import androidx.compose.foundation.gestures.detectDragGestures\n", "import androidx.compose.foundation.gestures.detectTransformGestures\n", "Siege transform gesture import")
    old_score_block = '''    val lastMove = moves.lastOrNull()\n    val myEarnedCubePoints = WordSiegeFinalRules.earnedCubePoints(moves, me)\n    val rivalEarnedCubePoints = WordSiegeFinalRules.earnedCubePoints(moves, opponentId)\n    val myTargetScore = WordSiegeFinalRules.netScore(panSiegeWordScore(game, myOwner), myEarnedCubePoints, rivalEarnedCubePoints)\n    val rivalTargetScore = WordSiegeFinalRules.netScore(panSiegeWordScore(game, rivalOwner), rivalEarnedCubePoints, myEarnedCubePoints)\n    var displayedMyScore by remember(game.id) { mutableIntStateOf(myTargetScore) }\n    var displayedRivalScore by remember(game.id) { mutableIntStateOf(rivalTargetScore) }\n    var displayedCurrentPlayerId by remember(game.id) { mutableStateOf(game.currentPlayerId) }\n    var fallbackPracticeActive by remember(game.id) { mutableStateOf(false) }'''
    new_score_block = '''    val lastMove = moves.lastOrNull()\n    val myEarnedCubePoints = remember(moves, me) { WordSiegeFinalRules.earnedCubePoints(moves, me) }\n    val rivalEarnedCubePoints = remember(moves, opponentId) { WordSiegeFinalRules.earnedCubePoints(moves, opponentId) }\n    val myTargetScore = remember(game.playerOneWordScore, game.playerTwoWordScore, myOwner, myEarnedCubePoints, rivalEarnedCubePoints) {\n        WordSiegeFinalRules.netScore(panSiegeWordScore(game, myOwner), myEarnedCubePoints, rivalEarnedCubePoints)\n    }\n    val rivalTargetScore = remember(game.playerOneWordScore, game.playerTwoWordScore, rivalOwner, rivalEarnedCubePoints, myEarnedCubePoints) {\n        WordSiegeFinalRules.netScore(panSiegeWordScore(game, rivalOwner), rivalEarnedCubePoints, myEarnedCubePoints)\n    }\n    var fallbackPracticeActive by remember(game.id) { mutableStateOf(false) }'''
    text = replace_once(text, old_score_block, new_score_block, "Siege score memoization")
    text = replace_once(text, "    val visualMyTurn = game.status == \"playing\" && displayedCurrentPlayerId == me\n", "    val visualMyTurn = myTurn\n", "Siege accurate turn visual")
    score_effect = '''    LaunchedEffect(myTargetScore, rivalTargetScore, game.currentPlayerId) {\n        while (displayedMyScore != myTargetScore || displayedRivalScore != rivalTargetScore) {\n            displayedMyScore += (myTargetScore - displayedMyScore).coerceIn(-1, 1)\n            displayedRivalScore += (rivalTargetScore - displayedRivalScore).coerceIn(-1, 1)\n            delay(28)\n        }\n        displayedCurrentPlayerId = game.currentPlayerId\n    }\n\n'''
    text = replace_once(text, score_effect, "", "Siege remove parent score loop")
    text = replace_all(text, "score = displayedMyScore,", "score = myTargetScore,", "Siege my score target")
    text = replace_all(text, "score = displayedRivalScore,", "score = rivalTargetScore,", "Siege rival score target")
    text = replace_all(text, "active = displayedCurrentPlayerId == me,", "active = game.currentPlayerId == me,", "Siege my active")
    text = replace_all(text, "active = displayedCurrentPlayerId == opponentId,", "active = game.currentPlayerId == opponentId,", "Siege rival active")

    # Camera state and one shared last-move pulse, not one animation per cube.
    text = replace_once(
        text,
        "    var closePan by remember(gameId) { mutableStateOf(Offset.Zero) }\n    var dragging by remember(gameId) { mutableStateOf(false) }",
        "    var closePan by remember(gameId) { mutableStateOf(Offset.Zero) }\n    var closeScale by remember(gameId) { mutableFloatStateOf(WORD_SIEGE_DEFAULT_CLOSE_SCALE) }\n    var dragging by remember(gameId) { mutableStateOf(false) }",
        "Siege close scale state",
    )
    text = replace_once(
        text,
        "    var viewportMode by remember(gameId) { mutableStateOf(WordSiegeBoardViewportMode.CLOSE) }\n\n    val transform by remember(viewportMode, viewport, boardPx, closePan) {",
        "    var viewportMode by remember(gameId) { mutableStateOf(WordSiegeBoardViewportMode.CLOSE) }\n    val lastMoveHighlightAlpha = remember(gameId) { Animatable(0f) }\n\n    val transform by remember(viewportMode, viewport, boardPx, closePan, closeScale) {",
        "Siege shared highlight animatable",
    )
    text = replace_once(text, "                closePan = closePan,\n", "                closeScale = closeScale,\n                closePan = closePan,\n", "Siege transform close scale")
    text = replace_once(
        text,
        "    val boardBorderWidth = wordSiegeBoardBorderWidthDp(transform.scale).dp\n",
        "    val boardBorderWidth = wordSiegeBoardBorderWidthDp(transform.scale).dp\n    val highlightBorderWidth = wordSiegeBoardBorderWidthDp(transform.scale, desiredScreenWidthDp = 1.8f).dp\n",
        "Siege highlight border width",
    )
    old_helpers = '''    fun clampClosePan(candidate: Offset): Offset = clampWordSiegeBoardPan(\n        candidate,\n        viewport.width.toFloat(),\n        viewport.height.toFloat(),\n        boardPx,\n        1f,\n    )\n\n    fun centerCloseOn(index: Int): Offset =\n        wordSiegeCenteredClosePan(\n            index = index,\n            viewportWidthPx = viewport.width.toFloat(),\n            viewportHeightPx = viewport.height.toFloat(),\n            boardWidthPx = boardPx,\n            cellSizePx = tilePx,\n        )'''
    new_helpers = '''    fun clampClosePan(candidate: Offset, scale: Float = closeScale): Offset = clampWordSiegeBoardPan(\n        candidate,\n        viewport.width.toFloat(),\n        viewport.height.toFloat(),\n        boardPx,\n        scale,\n    )\n\n    fun centerCloseOn(index: Int, scale: Float = closeScale): Offset =\n        wordSiegeCenteredClosePan(\n            index = index,\n            viewportWidthPx = viewport.width.toFloat(),\n            viewportHeightPx = viewport.height.toFloat(),\n            boardWidthPx = boardPx,\n            cellSizePx = tilePx,\n            scale = scale,\n        )'''
    text = replace_once(text, old_helpers, new_helpers, "Siege scaled pan helpers")
    text = replace_once(
        text,
        '''        if (nextMode == WordSiegeBoardViewportMode.CLOSE) {\n            closePan = centerCloseOn(WordSiegeBoardSpec.CenterIndex)\n        }''',
        '''        if (nextMode == WordSiegeBoardViewportMode.CLOSE) {\n            closeScale = WORD_SIEGE_DEFAULT_CLOSE_SCALE\n            closePan = centerCloseOn(WordSiegeBoardSpec.CenterIndex, WORD_SIEGE_DEFAULT_CLOSE_SCALE)\n        }''',
        "Siege toggle reset scale",
    )
    text = replace_once(
        text,
        "            closePan = centerCloseOn(WordSiegeBoardSpec.CenterIndex)\n            initialized = true",
        "            closeScale = WORD_SIEGE_DEFAULT_CLOSE_SCALE\n            closePan = centerCloseOn(WordSiegeBoardSpec.CenterIndex, WORD_SIEGE_DEFAULT_CLOSE_SCALE)\n            initialized = true",
        "Siege initial scale",
    )
    text = replace_once(
        text,
        "            actionVfxMoveId = moveId\n            if (!dragging && viewportMode == WordSiegeBoardViewportMode.CLOSE) {",
        "            actionVfxMoveId = moveId\n            lastMoveHighlightAlpha.snapTo(0f)\n            lastMoveHighlightAlpha.animateTo(1f, animationSpec = tween(180))\n            if (!dragging && viewportMode == WordSiegeBoardViewportMode.CLOSE) {",
        "Siege highlight enter",
    )
    text = replace_once(
        text,
        '''                    closePan = centerCloseOn(targetIndex)\n                }\n            }\n        }\n    }\n\n    val resolvedIndices''',
        '''                    closePan = centerCloseOn(targetIndex)\n                }\n            }\n            delay(1_200)\n            lastMoveHighlightAlpha.animateTo(0f, animationSpec = tween(200))\n        }\n    }\n\n    val resolvedIndices''',
        "Siege highlight hold/exit",
    )
    old_pointer = '''                .pointerInput(gameId, viewportMode, viewport, boardPx) {\n                    if (viewportMode == WordSiegeBoardViewportMode.CLOSE) {\n                        detectDragGestures(\n                            onDragStart = { dragging = true },\n                            onDragCancel = { dragging = false },\n                            onDragEnd = { dragging = false },\n                        ) { change, dragAmount ->\n                            change.consume()\n                            closePan = clampClosePan(closePan + dragAmount)\n                        }\n                    }\n                },'''
    new_pointer = '''                .pointerInput(gameId, viewportMode, viewport, boardPx) {\n                    if (viewportMode == WordSiegeBoardViewportMode.CLOSE) {\n                        detectTransformGestures { centroid, pan, zoom, _ ->\n                            dragging = true\n                            val previousScale = closeScale\n                            val nextScale = (previousScale * zoom).coerceIn(WORD_SIEGE_MIN_CLOSE_SCALE, WORD_SIEGE_MAX_CLOSE_SCALE)\n                            val scaleRatio = nextScale / previousScale\n                            val zoomAdjustedPan = (closePan - centroid) * scaleRatio + centroid + pan\n                            closeScale = nextScale\n                            closePan = clampClosePan(zoomAdjustedPan, nextScale)\n                        }\n                    }\n                },'''
    text = replace_once(text, old_pointer, new_pointer, "Siege pinch pan gesture")
    text = replace_once(
        text,
        "    val actionVfxEvents = remember(gameId, placements, actionVfxMoveId, resolvedIndices) {",
        "    LaunchedEffect(closePan, closeScale) {\n        if (dragging) {\n            delay(120)\n            dragging = false\n        }\n    }\n\n    val stableBoard = remember(board) {\n        List(WordSiegeBoardSpec.Size * WordSiegeBoardSpec.Size) { index ->\n            board.getOrElse(index) { WordSiegeCellDto(bonus = WordSiegeBoardSpec.bonusAt(index)) }\n        }\n    }\n\n    val actionVfxEvents = remember(gameId, placements, actionVfxMoveId, resolvedIndices) {",
        "Siege gesture settle and stable board",
    )
    old_cell_call = '''                            PanSiegeBoardCell(\n                                cell = board.getOrElse(index) { WordSiegeCellDto(bonus = WordSiegeBoardSpec.bonusAt(index)) },\n                                pendingLetter = pendingRackIndex?.let(rack::getOrNull),\n                                pending = pending,\n                                myOwner = myOwner,\n                                enabled = enabled,\n                                size = PanSiegeCellSize,\n                                borderWidth = boardBorderWidth,\n                                onClick = { onCell(index) },\n                                onDoubleClick = ::toggleViewport,\n                            )'''
    new_cell_call = '''                            key(index) {\n                                PanSiegeBoardCell(\n                                    cell = stableBoard[index],\n                                    pendingLetter = pendingRackIndex?.let(rack::getOrNull),\n                                    pending = pending,\n                                    myOwner = myOwner,\n                                    enabled = enabled,\n                                    size = PanSiegeCellSize,\n                                    borderWidth = boardBorderWidth,\n                                    highlightBorderWidth = highlightBorderWidth,\n                                    lastMoveHighlightAlpha = if (index in resolvedIndices) lastMoveHighlightAlpha.value else 0f,\n                                    onClick = { onCell(index) },\n                                    onDoubleClick = ::toggleViewport,\n                                )\n                            }'''
    text = replace_once(text, old_cell_call, new_cell_call, "Siege stable cell keys/highlight")
    text = replace_once(
        text,
        '''                    viewportMode = WordSiegeBoardViewportMode.CLOSE\n                    closePan = centerCloseOn(WordSiegeBoardSpec.CenterIndex)''',
        '''                    viewportMode = WordSiegeBoardViewportMode.CLOSE\n                    closeScale = WORD_SIEGE_DEFAULT_CLOSE_SCALE\n                    closePan = centerCloseOn(WordSiegeBoardSpec.CenterIndex, WORD_SIEGE_DEFAULT_CLOSE_SCALE)''',
        "Siege center button default scale",
    )
    text = replace_once(
        text,
        "    borderWidth: Dp,\n    onClick: () -> Unit,",
        "    borderWidth: Dp,\n    highlightBorderWidth: Dp,\n    lastMoveHighlightAlpha: Float,\n    onClick: () -> Unit,",
        "Siege cell highlight params",
    )
    text = replace_once(
        text,
        '''            color = if (pending) PanSiegeTile.copy(alpha = .76f) else Color.Transparent,\n            shape = RoundedCornerShape(7.dp),\n            border = BorderStroke(if (pending) maxOf(2.dp, borderWidth) else borderWidth, border.copy(alpha = .96f)),''',
        '''            color = when {\n                pending -> PanSiegeTile.copy(alpha = .76f)\n                lastMoveHighlightAlpha > 0f -> Color.White.copy(alpha = .07f * lastMoveHighlightAlpha)\n                else -> Color.Transparent\n            },\n            shape = RoundedCornerShape(7.dp),\n            border = BorderStroke(\n                when {\n                    pending -> maxOf(2.dp, borderWidth)\n                    lastMoveHighlightAlpha > 0f -> highlightBorderWidth\n                    else -> borderWidth\n                },\n                border.copy(alpha = (.96f + .04f * lastMoveHighlightAlpha).coerceAtMost(1f)),\n            ),''',
        "Siege minimal last move highlight",
    )
    # Readability tokens on gameplay controls.
    replacements = {
        'fontSize = 10.sp,\n                    fontWeight = FontWeight.Black,': 'fontSize = GameTypography.Secondary,\n                    fontWeight = FontWeight.Black,',
        'fontSize = 8.sp, fontWeight = FontWeight.Black)': 'fontSize = GameTypography.Metadata, fontWeight = FontWeight.Black)',
        'fontSize = 9.sp, fontWeight = FontWeight.Black)': 'fontSize = GameTypography.Action, fontWeight = FontWeight.Black)',
        'fontSize = 8.sp,\n                        modifier = Modifier.weight(1f),': 'fontSize = GameTypography.Metadata,\n                        modifier = Modifier.weight(1f),',
        'color = MainUi.Muted, fontSize = 8.sp)': 'color = MainUi.Muted, fontSize = GameTypography.Metadata)',
        'fontSize = 8.sp, fontWeight = FontWeight.Black)': 'fontSize = GameTypography.Metadata, fontWeight = FontWeight.Black)',
        'fontSize = 10.sp, fontWeight = FontWeight.Black)': 'fontSize = GameTypography.Action, fontWeight = FontWeight.Black)',
        'fontSize = 9.sp, fontWeight = FontWeight.Black)': 'fontSize = GameTypography.Action, fontWeight = FontWeight.Black)',
        'fontSize = 12.sp, fontWeight = FontWeight.Black)': 'fontSize = GameTypography.Action, fontWeight = FontWeight.Black)',
        'fontSize = 9.sp,\n            fontWeight = FontWeight.Bold,': 'fontSize = GameTypography.Metadata,\n            fontWeight = FontWeight.Bold,',
        'fontSize = 9.sp, maxLines = 2': 'fontSize = GameTypography.Metadata, maxLines = 2',
    }
    for old, new in replacements.items():
        if old in text:
            text = text.replace(old, new)
    text = replace_once(text, "                size = 36.dp,", "                size = 50.dp,", "Siege profile avatar")
    text = replace_once(text, "fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis", "fontSize = GameTypography.PlayerName, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis", "Siege profile name")
    text = replace_once(text, 'Text("$score", color = accent, fontSize = 18.sp, fontWeight = FontWeight.Black)', 'val displayedScore by animateIntAsState(score, animationSpec = tween(220), label = "siege-score")\n                Text("$displayedScore", color = accent, fontSize = GameTypography.Score, fontWeight = FontWeight.Black)', "Siege isolated score animation")
    text = replace_once(text, "                    fontSize = 7.sp,\n                    maxLines = 1,", "                    fontSize = GameTypography.Metadata,\n                    maxLines = 1,", "Siege profile metadata")
    text = replace_once(text, "                fontSize = 9.sp,\n            )", "                fontSize = GameTypography.Metadata,\n            )", "Siege finished metadata")
    return text


def patch_siege_dto(text: str) -> str:
    text = replace_once(text, "package com.sonharf.game.data\n\n", "package com.sonharf.game.data\n\nimport androidx.compose.runtime.Immutable\n", "Siege immutable import")
    for name in ["WordSiegeCellDto", "WordSiegeGameDto", "WordSiegeMoveDto", "WordSiegePlacedTileDto", "WordSiegeMessageDto"]:
        marker = f"@Serializable\ndata class {name}("
        replacement = f"@Immutable\n@Serializable\ndata class {name}("
        if replacement not in text:
            if marker not in text:
                raise RuntimeError(f"Siege immutable {name}: marker not found")
            text = text.replace(marker, replacement, 1)
    return text


patch("app/src/main/java/com/sonharf/game/TargetNeonGameScreen.kt", patch_target)
patch("app/src/main/java/com/sonharf/game/WordSiegeBoardViewport.kt", patch_siege_viewport)
patch("app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt", patch_siege)
patch("app/src/main/java/com/sonharf/game/data/WordSiegeBackend.kt", patch_siege_dto)
