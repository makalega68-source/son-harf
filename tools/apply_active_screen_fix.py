from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]


def read(path):
    return (ROOT / path).read_text()


def write(path, text):
    (ROOT / path).write_text(text)


def one(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one match, got {count}")
    return text.replace(old, new, 1)


def regex_one(text, pattern, repl, label, flags=re.S):
    new, count = re.subn(pattern, repl, text, count=1, flags=flags)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one regex match, got {count}")
    return new

# ---- Classic active UI: LightDuelArena ----
p = "app/src/main/java/com/sonharf/game/LightDuelUi.kt"
t = read(p)
t = one(t, "import android.provider.Settings\n", "import android.provider.Settings\nimport android.os.SystemClock\n", "classic SystemClock import")
t = one(t, "import java.time.Instant\n", "", "classic remove wall-clock Instant")

t = one(t,
'''    var seconds by remember(deadline, room.status) { mutableIntStateOf(if (deadline == null) 0 else 10) }\n    var lastSignalledSecond by remember(deadline) { mutableIntStateOf(Int.MIN_VALUE) }''',
'''    var seconds by remember(deadline, room.status) { mutableIntStateOf(0) }\n    var timerSynchronizing by remember(room.id) { mutableStateOf(false) }\n    var timeoutSignalKey by remember(room.id) { mutableStateOf<String?>(null) }\n    var lastSignalledSecond by remember(deadline) { mutableIntStateOf(Int.MIN_VALUE) }''',
"classic timer state")

t = regex_one(t,
r'''    LaunchedEffect\(deadline, room\.currentPlayerId, room\.status\) \{\n        val endMs = runCatching \{ deadline\?\.let \{ Instant\.parse\(it\)\.toEpochMilli\(\) \} \}\.getOrNull\(\) \?: return@LaunchedEffect\n        while \(true\) \{\n            val remaining = endMs - Instant\.now\(\)\.toEpochMilli\(\)\n            if \(remaining <= 0L\) \{\n                seconds = 0\n                if \(quizActive && triviaRound\?\.resolvedAt == null\) onTriviaTimeout\(\) else if \(!quizActive\) onTimeout\(\)\n                break\n            \}\n            val shown = ceil\(remaining / 1000\.0\)\.toInt\(\)\.coerceAtLeast\(1\)\n            seconds = shown\n            if \(!quizActive && shown <= ClassicCompetitionRules\.URGENT_SECONDS && shown != lastSignalledSecond\) \{\n                lastSignalledSecond = shown\n                if \(SonHarfPreferences\.soundEnabled\(context\)\) \{\n                    if \(shown <= ClassicCompetitionRules\.HAPTIC_SECONDS\) SonHarfSoundFx\.heartbeat\(\) else SonHarfSoundFx\.countdown\(\)\n                \}\n                if \(ClassicCompetitionRules\.shouldHaptic\(shown\) && SonHarfPreferences\.vibrationEnabled\(context\)\) \{\n                    haptics\.performHapticFeedback\(HapticFeedbackType\.TextHandleMove\)\n                \}\n            \}\n            delay\(minOf\(100L, remaining\)\)\n        \}\n    \}''',
'''    LaunchedEffect(deadline, room.currentPlayerId, room.status, triviaRound?.id) {
        val endMs = classicDeadlineEpochMs(deadline) ?: run {
            seconds = 0
            timerSynchronizing = false
            return@LaunchedEffect
        }
        val anchor = ClassicMonotonicDeadlineAnchor(
            serverDeadlineEpochMs = endMs,
            wallEpochMsAtAnchor = System.currentTimeMillis(),
            elapsedRealtimeMsAtAnchor = SystemClock.elapsedRealtime(),
        )
        val eventKey = if (quizActive) "quiz:${triviaRound?.id}:$deadline" else classicDeadlineEventKey(room)
        if (timeoutSignalKey != eventKey) timerSynchronizing = false
        while (true) {
            val remaining = anchor.remainingMs(SystemClock.elapsedRealtime())
            val shown = anchor.displaySeconds(SystemClock.elapsedRealtime())
            seconds = shown
            if (remaining <= 0L) {
                timerSynchronizing = true
                if (timeoutSignalKey != eventKey) {
                    timeoutSignalKey = eventKey
                    if (quizActive && triviaRound?.resolvedAt == null) onTriviaTimeout() else if (!quizActive) onTimeout()
                }
                break
            }
            if (!quizActive && shown <= ClassicCompetitionRules.URGENT_SECONDS && shown != lastSignalledSecond) {
                lastSignalledSecond = shown
                if (SonHarfPreferences.soundEnabled(context)) {
                    if (shown <= ClassicCompetitionRules.HAPTIC_SECONDS) SonHarfSoundFx.heartbeat() else SonHarfSoundFx.countdown()
                }
                if (ClassicCompetitionRules.shouldHaptic(shown) && SonHarfPreferences.vibrationEnabled(context)) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
            delay(minOf(225L, remaining.coerceAtLeast(1L)))
        }
    }

    LaunchedEffect(room.turnDeadline, room.currentPlayerId, room.validWordCount, room.roundNo, room.status) {
        val currentKey = classicDeadlineEventKey(room)
        if (timeoutSignalKey != null && timeoutSignalKey != currentKey) {
            timerSynchronizing = false
        }
    }''',
"classic monotonic timer")

t = one(t,
'''                            myTurn -> sh("● SIRA SENDE", "● YOUR TURN")\n                            room.isBot && room.botTurn -> sh("● BOT DÜŞÜNÜYOR", "● BOT THINKING")''',
'''                            timerSynchronizing && !quizActive -> sh("Senkronize ediliyor…", "Synchronizing…")\n                            myTurn -> sh("● SIRA SENDE", "● YOUR TURN")\n                            room.isBot && room.botTurn -> sh("● BOT DÜŞÜNÜYOR", "● BOT THINKING")''',
"classic sync label")

t = one(t, "                            items(words.takeLast(5)) { word ->", "                            items(words.takeLast(5), key = { it.id }) { word ->", "classic stable word keys")
t = one(t, '                KeyButton("⌫", enabled && value.isNotEmpty(), Modifier.weight(1f)) { onValueChange(value.dropLast(1)) }', '                KeyButton(sh("SİL", "DELETE"), enabled && value.isNotEmpty(), Modifier.weight(1f)) { onValueChange(value.dropLast(1)) }', "classic delete label")

# Accessibility/readability while retaining symmetric cards.
t = one(t, "        modifier = modifier.height(108.dp),", "        modifier = modifier.heightIn(min = 112.dp),", "classic card height")
t = one(t, "                Text(name, color = LText, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)", "                Text(name, color = LText, fontSize = 15.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)", "classic name size")
t = one(t, "                Text(\"$league • $rating\", color = LMuted, fontSize = 7.sp, fontWeight = FontWeight.Bold, maxLines = 1)", "                Text(\"$league • $rating\", color = LMuted, fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)", "classic metadata size")
t = one(t, "                    fontSize = if (value.isBlank()) 13.sp else 20.sp,", "                    fontSize = if (value.isBlank()) 16.sp else 20.sp,", "classic input size")
t = one(t, "            Text(statusText, color = statusColor, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, maxLines = 1)", "            Text(statusText, color = statusColor, fontSize = 14.sp, lineHeight = 16.sp, fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)", "classic status size")
t = one(t, '                    Text(sh("GÖNDER", "SEND"), fontSize = 10.sp, fontWeight = FontWeight.Black)', '                    Text(sh("GÖNDER", "SEND"), fontSize = 14.sp, fontWeight = FontWeight.Black)', "classic send size")
t = one(t, "                fontSize = if (label.length > 5) 9.sp else 13.sp,", "                fontSize = if (label.length > 5) 14.sp else 14.sp,", "classic key/action size")
# Avoid old lead/trail text surviving a new round.
t = one(t, "    LaunchedEffect(myScore, oppScore, lastItem?.id) {", "    LaunchedEffect(room.roundNo) { actionOverlay = null }\n\n    LaunchedEffect(myScore, oppScore, lastItem?.id) {", "classic overlay reset")
write(p, t)

# ---- Classic active state collector/actions: OnlineGameScreenV6 ----
p = "app/src/main/java/com/sonharf/game/OnlineGameScreenV6.kt"
t = read(p)
t = one(t, "    var busy by remember { mutableStateOf(false) }\n", "    var busy by remember { mutableStateOf(false) }\n    var submitInFlightKey by remember { mutableStateOf<String?>(null) }\n    var timeoutClaimKey by remember { mutableStateOf<String?>(null) }\n", "classic in-flight states")
t = one(t,
'''    fun failedEvent(e: String?) = e in setOf("word_already_used", "wrong_start_letter", "not_in_dictionary", "invalid_word", "ends_with_soft_g", "turn_expired")''',
'''    fun acceptServerRoom(next: GameRoomDto) {
        val current = room
        if (current == null || shouldAcceptClassicSnapshot(current, next)) room = next
    }
    fun failedEvent(e: String?) = e in setOf("word_already_used", "wrong_start_letter", "not_in_dictionary", "invalid_word", "ends_with_soft_g", "turn_expired")''',
"classic acceptance helper")
t = one(t, "                    room = it\n                    refreshQuiz(it)", "                    acceptServerRoom(it)\n                    refreshQuiz(it)", "classic collector stale guard")
# Submit dedupe + guarded result acceptance.
t = one(t,
'''                    val submitted = wordInput.trim()\n                    if (submitted.isBlank()) return@launch''',
'''                    val submitted = wordInput.trim()
                    if (submitted.isBlank()) return@launch
                    val submitKey = "${active.id}|${active.roundNo}|${active.validWordCount}|${active.currentPlayerId}|${active.turnDeadline}|$submitted"
                    if (submitInFlightKey == submitKey || busy) return@launch
                    submitInFlightKey = submitKey''',
"classic submit dedupe")
t = one(t, "                            room = result\n                            if (voiceToken != null) {", "                            acceptServerRoom(result)\n                            if (voiceToken != null) {", "classic submit stale guard")
t = one(t, "                    busy = false\n                }\n            },\n            onTimeout = {", "                    busy = false\n                    submitInFlightKey = null\n                }\n            },\n            onTimeout = {", "classic submit release")
# Replace retrying timeout loop with one server-validated claim per event key.
t = regex_one(t,
r'''            onTimeout = \{\n                scope\.launch \{\n                    val expectedDeadline = active\.turnDeadline\n                    val expectedPlayer = active\.currentPlayerId\n                    val expectedBotTurn = active\.botTurn\n                    while \(true\) \{.*?\n                    \}\n                \}\n            \},''',
'''            onTimeout = {
                scope.launch {
                    val claimKey = classicDeadlineEventKey(active)
                    if (timeoutClaimKey == claimKey) return@launch
                    timeoutClaimKey = claimKey
                    notice = sh("Senkronize ediliyor…", "Synchronizing…")
                    runCatching { backend.claimTurnTimeout(active.id) }
                        .onSuccess { acceptServerRoom(it) }
                        .onFailure { notice = friendly(it.message.orEmpty()) }
                }
            },''',
"classic timeout dedupe")
# Reset dedupe only when authoritative event key advances.
t = one(t,
'''        LaunchedEffect(active.currentPlayerId, active.validWordCount, active.roundNo) {\n            wordInput = ""\n            voiceRequestId = null\n        }''',
'''        LaunchedEffect(active.currentPlayerId, active.validWordCount, active.roundNo, active.turnDeadline) {
            wordInput = ""
            voiceRequestId = null
            val eventKey = classicDeadlineEventKey(active)
            if (timeoutClaimKey != null && timeoutClaimKey != eventKey) timeoutClaimKey = null
            if (submitInFlightKey != null && !busy) submitInFlightKey = null
        }''',
"classic event-key reset")
write(p, t)

# ---- Practice score performance/readability ----
p = "app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt"
t = read(p)
t = one(t, "import androidx.compose.foundation.BorderStroke\n", "import androidx.compose.animation.core.animateIntAsState\nimport androidx.compose.animation.core.tween\nimport androidx.compose.foundation.BorderStroke\n", "practice animation imports")
t = one(t,
'''    var displayedPlayerScore by remember { mutableIntStateOf(playerTargetScore) }\n    var displayedBotScore by remember { mutableIntStateOf(botTargetScore) }\n    var displayedOwner by remember { mutableIntStateOf(state.currentOwner) }''',
'''    val displayedPlayerScore by animateIntAsState(playerTargetScore, tween(260), label = "practice-player-score")
    val displayedBotScore by animateIntAsState(botTargetScore, tween(260), label = "practice-bot-score")
    val displayedOwner = state.currentOwner''',
"practice local score animation")
t = regex_one(t,
r'''\n    LaunchedEffect\(playerTargetScore, botTargetScore, state\.currentOwner\) \{\n        while \(displayedPlayerScore != playerTargetScore \|\| displayedBotScore != botTargetScore\) \{\n            displayedPlayerScore \+= \(playerTargetScore - displayedPlayerScore\)\.coerceIn\(-1, 1\)\n            displayedBotScore \+= \(botTargetScore - displayedBotScore\)\.coerceIn\(-1, 1\)\n            delay\(28\)\n        \}\n        displayedOwner = state\.currentOwner\n    \}\n''', "\n", "practice remove delay28 score loop")
t = one(t,
'''        displayedPlayerScore,\n        displayedBotScore,\n        displayedOwner,\n        dictionaryReady,''',
'''        dictionaryReady,''',
"practice bot effect stable deps")
t = one(t, "        if (displayedPlayerScore != playerTargetScore || displayedBotScore != botTargetScore || displayedOwner != 2) return@LaunchedEffect\n", "", "practice bot no animation gate")
# Readability target: symmetric 50dp avatar, 15sp name, 30sp score, >=12 metadata.
t = one(t, "                size = if (compact) 30.dp else 34.dp,", "                size = 50.dp,", "practice avatar size")
t = one(t, "                    Text(name, color = MainUi.Text, fontWeight = FontWeight.Black, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))", "                    Text(name, color = MainUi.Text, fontWeight = FontWeight.Black, fontSize = 15.sp, lineHeight = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))", "practice name size")
t = one(t, '                            Text("BOT", Modifier.padding(horizontal = 4.dp, vertical = 1.dp), color = accent, fontSize = 6.sp, fontWeight = FontWeight.Black)', '                            Text("BOT", Modifier.padding(horizontal = 5.dp, vertical = 2.dp), color = accent, fontSize = 12.sp, fontWeight = FontWeight.Black)', "practice bot metadata size")
t = one(t, '                    Text("$score", color = accent, fontWeight = FontWeight.Black, fontSize = if (compact) 16.sp else 18.sp)', '                    Text("$score", color = accent, fontWeight = FontWeight.Black, fontSize = 30.sp, lineHeight = 32.sp, maxLines = 1)', "practice score size")
t = one(t, '                    Text(sh("Alan $area", "Area $area"), color = MainUi.Muted, fontSize = 7.sp, maxLines = 1)', '                    Text(sh("Alan $area", "Area $area"), color = MainUi.Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)', "practice area metadata size")
# Status/action labels in active bot practice.
t = t.replace("fontSize = 9.sp,\n                            fontWeight = FontWeight.Black,\n                            maxLines = 1,", "fontSize = 14.sp,\n                            lineHeight = 16.sp,\n                            fontWeight = FontWeight.Black,\n                            maxLines = 2,", 1)
t = one(t, 'Text(sh("PAS", "PASS"), fontSize = 9.sp, fontWeight = FontWeight.Black)', 'Text(sh("PAS", "PASS"), fontSize = 14.sp, fontWeight = FontWeight.Black)', "practice pass size")
t = one(t, 'Text(sh("DEĞİŞTİR", "EXCHANGE"), color = SiegePurple, fontSize = 8.sp, fontWeight = FontWeight.Black)', 'Text(sh("DEĞİŞTİR", "EXCHANGE"), color = SiegePurple, fontSize = 14.sp, fontWeight = FontWeight.Black)', "practice exchange size")
t = one(t, 'Text(sh("OYNA", "PLAY"), fontSize = 10.sp, fontWeight = FontWeight.Black)', 'Text(sh("OYNA", "PLAY"), fontSize = 14.sp, fontWeight = FontWeight.Black)', "practice play size")
write(p, t)

# ---- Practice board camera + one-shot last move highlight ----
p = "app/src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt"
t = read(p)
t = one(t, "import androidx.compose.foundation.background\n", "import androidx.compose.animation.core.Animatable\nimport androidx.compose.animation.core.tween\nimport androidx.compose.foundation.background\nimport androidx.compose.foundation.border\n", "practice board highlight imports")
t = one(t, "import androidx.compose.foundation.gestures.detectDragGestures\n", "import androidx.compose.foundation.gestures.detectTransformGestures\n", "practice pinch import")
t = one(t, "    var closePan by remember { mutableStateOf(Offset.Zero) }\n", "    var closePan by remember { mutableStateOf(Offset.Zero) }\n    var closeScale by remember { mutableFloatStateOf(WORD_SIEGE_PRACTICE_CLOSE_SCALE) }\n", "practice scale state")
t = one(t, "    val transform by remember(mode, viewport, boardPx, closePan) {", "    val transform by remember(mode, viewport, boardPx, closePan, closeScale) {", "practice transform deps")
t = one(t, "                boardWidthPx = boardPx,\n                closePan = closePan,", "                boardWidthPx = boardPx,\n                closeScale = closeScale,\n                closePan = closePan,", "practice transform scale")
t = one(t, "        boardPx,\n        1f,\n    )", "        boardPx,\n        closeScale,\n    )", "practice clamp scale")
t = one(t, "            cellSizePx = tilePx,\n        )", "            cellSizePx = tilePx,\n            scale = closeScale,\n        )", "practice center scale")
# Insert highlight state before actionVfxEvents.
t = one(t, "    val actionVfxEvents = remember(placements, moveEventKey, resolvedIndices) {", '''    var consumedHighlightKey by remember { mutableStateOf(moveEventKey) }
    var highlightedIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    val highlightAlpha = remember { Animatable(0f) }
    LaunchedEffect(moveEventKey, resolvedIndices) {
        val key = moveEventKey
        if (key != null && key != consumedHighlightKey) {
            consumedHighlightKey = key
            highlightedIndices = resolvedIndices.filter(WordSiegeBoardSpec::isValidIndex).toSet()
            highlightAlpha.snapTo(0f)
            highlightAlpha.animateTo(1f, tween(WORD_SIEGE_LAST_MOVE_ENTER_MS))
            delay(WORD_SIEGE_LAST_MOVE_HOLD_MS.toLong())
            highlightAlpha.animateTo(0f, tween(WORD_SIEGE_LAST_MOVE_EXIT_MS))
            highlightedIndices = emptySet()
        }
    }

    val actionVfxEvents = remember(placements, moveEventKey, resolvedIndices) {''', "practice highlight state")
# transform gesture block.
t = regex_one(t,
r'''                \.pointerInput\(mode, viewport, boardPx\) \{\n                    if \(mode == WordSiegeBoardViewportMode\.CLOSE\) \{\n                        detectDragGestures \{ change, dragAmount ->\n                            change\.consume\(\)\n                            closePan = clampClosePan\(closePan \+ dragAmount\)\n                        \}\n                    \}\n                \},''',
'''                .pointerInput(mode, viewport, boardPx, closeScale) {
                    if (mode == WordSiegeBoardViewportMode.CLOSE) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            val oldScale = closeScale
                            val newScale = (oldScale * zoom).coerceIn(WORD_SIEGE_PRACTICE_MIN_SCALE, WORD_SIEGE_PRACTICE_MAX_SCALE)
                            val ratio = if (oldScale > 0f) newScale / oldScale else 1f
                            val candidate = centroid + (closePan - centroid) * ratio + pan
                            closeScale = newScale
                            closePan = clampWordSiegeBoardPan(
                                candidate,
                                viewport.width.toFloat(),
                                viewport.height.toFloat(),
                                boardPx,
                                newScale,
                            )
                        }
                    }
                },''',
"practice transform gestures")
t = one(t, "                                enabled = enabled,\n                                onClick = { onCell(index) },", "                                enabled = enabled,\n                                lastMoveHighlight = if (index in highlightedIndices) highlightAlpha.value else 0f,\n                                onClick = { onCell(index) },", "practice highlight pass")
t = one(t, "                    mode = WordSiegeBoardViewportMode.CLOSE\n                    closePan = centerClose()", "                    mode = WordSiegeBoardViewportMode.CLOSE\n                    closeScale = WORD_SIEGE_PRACTICE_CLOSE_SCALE\n                    closePan = centerClose()", "practice reset scale")
t = one(t, "    enabled: Boolean,\n    onClick: () -> Unit,", "    enabled: Boolean,\n    lastMoveHighlight: Float,\n    onClick: () -> Unit,", "practice cell highlight arg")
t = one(t, "            .background(cellColor)\n            .combinedClickable(", "            .background(cellColor)\n            .border(\n                width = if (lastMoveHighlight > 0f) 1.75.dp else 0.dp,\n                color = Color.White.copy(alpha = .25f + .65f * lastMoveHighlight),\n                shape = RoundedCornerShape(7.dp),\n            )\n            .combinedClickable(", "practice highlight border")
t = one(t, "    ) {\n        if (letter != null) {", "    ) {\n        if (lastMoveHighlight > 0f) Box(Modifier.matchParentSize().background(Color.White.copy(alpha = .06f * lastMoveHighlight)))\n        if (letter != null) {", "practice highlight brightness")
write(p, t)

# ---- Viewport constants ----
p = "app/src/main/java/com/sonharf/game/WordSiegeBoardViewport.kt"
t = read(p)
t = one(t, "internal const val WORD_SIEGE_MIN_SCREEN_BORDER_DP = 1f\n", "internal const val WORD_SIEGE_MIN_SCREEN_BORDER_DP = 1f\ninternal const val WORD_SIEGE_PRACTICE_CLOSE_SCALE = 0.86f\ninternal const val WORD_SIEGE_PRACTICE_MIN_SCALE = 0.78f\ninternal const val WORD_SIEGE_PRACTICE_MAX_SCALE = 1.24f\ninternal const val WORD_SIEGE_LAST_MOVE_ENTER_MS = 180\ninternal const val WORD_SIEGE_LAST_MOVE_HOLD_MS = 1_200\ninternal const val WORD_SIEGE_LAST_MOVE_EXIT_MS = 200\n", "viewport/highlight constants")
write(p, t)

# ---- Active Siege match: lightweight score + one-shot last move highlight ----
p = "app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt"
t = read(p)
t = one(t, "import androidx.compose.foundation.BorderStroke\n", "import androidx.compose.animation.core.Animatable\nimport androidx.compose.animation.core.animateIntAsState\nimport androidx.compose.animation.core.tween\nimport androidx.compose.foundation.BorderStroke\nimport androidx.compose.foundation.border\n", "siege match animation imports")
t = one(t,
'''    var displayedMyScore by remember(game.id) { mutableIntStateOf(myTargetScore) }\n    var displayedRivalScore by remember(game.id) { mutableIntStateOf(rivalTargetScore) }\n    var displayedCurrentPlayerId by remember(game.id) { mutableStateOf(game.currentPlayerId) }''',
'''    val displayedMyScore by animateIntAsState(myTargetScore, tween(260), label = "siege-my-score")
    val displayedRivalScore by animateIntAsState(rivalTargetScore, tween(260), label = "siege-rival-score")
    val displayedCurrentPlayerId = game.currentPlayerId''',
"siege lightweight scores")
t = regex_one(t,
r'''\n    LaunchedEffect\(myTargetScore, rivalTargetScore, game\.currentPlayerId\) \{\n        while \(displayedMyScore != myTargetScore \|\| displayedRivalScore != rivalTargetScore\) \{\n            displayedMyScore \+= \(myTargetScore - displayedMyScore\)\.coerceIn\(-1, 1\)\n            displayedRivalScore \+= \(rivalTargetScore - displayedRivalScore\)\.coerceIn\(-1, 1\)\n            delay\(28\)\n        \}\n        displayedCurrentPlayerId = game\.currentPlayerId\n    \}\n''', "\n", "siege remove score loop")
# Board highlight uses existing actionVfxMoveId dedupe/placedTiles source.
t = one(t, "    var actionVfxMoveId by remember(gameId) { mutableStateOf<Long?>(null) }\n", "    var actionVfxMoveId by remember(gameId) { mutableStateOf<Long?>(null) }\n    var highlightedIndices by remember(gameId) { mutableStateOf<Set<Int>>(emptySet()) }\n    val highlightAlpha = remember(gameId) { Animatable(0f) }\n", "siege highlight state")
t = one(t,
'''            actionVfxMoveId = moveId\n            if (!dragging && viewportMode == WordSiegeBoardViewportMode.CLOSE) {''',
'''            actionVfxMoveId = moveId
            highlightedIndices = lastMove.placedTiles.map { it.index }.filter(WordSiegeBoardSpec::isValidIndex).toSet()
            highlightAlpha.snapTo(0f)
            launch {
                highlightAlpha.animateTo(1f, tween(WORD_SIEGE_LAST_MOVE_ENTER_MS))
                delay(WORD_SIEGE_LAST_MOVE_HOLD_MS.toLong())
                highlightAlpha.animateTo(0f, tween(WORD_SIEGE_LAST_MOVE_EXIT_MS))
                highlightedIndices = emptySet()
            }
            if (!dragging && viewportMode == WordSiegeBoardViewportMode.CLOSE) {''',
"siege highlight animation")
# launch extension requires coroutineScope inside LaunchedEffect; launch is available receiver. Import exists via runtime? add kotlinx.coroutines.launch.
t = one(t, "import kotlinx.coroutines.delay\n", "import kotlinx.coroutines.delay\nimport kotlinx.coroutines.launch\n", "siege launch import")
t = one(t, "                                borderWidth = boardBorderWidth,\n                                onClick = { onCell(index) },", "                                borderWidth = boardBorderWidth,\n                                lastMoveHighlight = if (index in highlightedIndices) highlightAlpha.value else 0f,\n                                onClick = { onCell(index) },", "siege highlight pass")
t = one(t, "    borderWidth: Dp,\n    onClick: () -> Unit,", "    borderWidth: Dp,\n    lastMoveHighlight: Float,\n    onClick: () -> Unit,", "siege cell highlight arg")
t = one(t, "            .background(baseColor)\n            .combinedClickable(", "            .background(baseColor)\n            .border(\n                width = if (lastMoveHighlight > 0f) 1.75.dp else 0.dp,\n                color = Color.White.copy(alpha = .25f + .65f * lastMoveHighlight),\n                shape = RoundedCornerShape(7.dp),\n            )\n            .combinedClickable(", "siege highlight border")
t = one(t, "    ) {\n        Surface(\n            modifier = Modifier.fillMaxSize(),", "    ) {\n        Surface(\n            modifier = Modifier.fillMaxSize(),", "siege cell surface anchor")
# add slight brightness inside inner cell box
needle = "            Box(contentAlignment = Alignment.Center) {"
if needle not in t:
    raise SystemExit("siege inner box missing")
t = t.replace(needle, "            Box(contentAlignment = Alignment.Center) {\n                if (lastMoveHighlight > 0f) Box(Modifier.matchParentSize().background(Color.White.copy(alpha = .06f * lastMoveHighlight)))", 1)
write(p, t)

# ---- Contract tests for routes, practice perf/camera/highlight, keyboard/readability ----
p = "app/src/test/java/com/sonharf/game/ActiveScreenRegressionContractTest.kt"
write(p, r'''package com.sonharf.game

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveScreenRegressionContractTest {
    private fun src(name: String): String = File("src/main/java/com/sonharf/game/$name").readText()

    @Test fun activeRoutesRemainAuthoritative() {
        val portal = src("GamePortalApp.kt")
        val stable = src("StableV1App.kt")
        val monster = src("MonsterExperienceApp.kt")
        val online = src("OnlineGameScreenV6.kt")
        val siege = src("WordSiegePanMatch.kt")
        assertTrue(portal.contains("StableV1App"))
        assertTrue(stable.contains("MonsterExperienceApp"))
        assertTrue(monster.contains("OnlineGameScreenV6"))
        assertTrue(online.contains("LightDuelArena("))
        assertTrue(siege.contains("WordSiegePracticeScreen("))
        assertFalse(online.contains("TargetNeonGameScreen("))
    }

    @Test fun practiceHasNoLegacy28msScoreLoop() {
        val practice = src("WordSiegePracticeScreen.kt")
        assertFalse(practice.contains("delay(28)"))
        assertTrue(practice.contains("animateIntAsState"))
    }

    @Test fun practiceViewportContract() {
        val viewport = src("WordSiegeBoardViewport.kt")
        val board = src("WordSiegePracticeBoard.kt")
        assertTrue(viewport.contains("WORD_SIEGE_PRACTICE_CLOSE_SCALE = 0.86f"))
        assertTrue(viewport.contains("WORD_SIEGE_PRACTICE_MIN_SCALE = 0.78f"))
        assertTrue(viewport.contains("WORD_SIEGE_PRACTICE_MAX_SCALE = 1.24f"))
        assertTrue(board.contains("detectTransformGestures"))
        assertTrue(board.contains("closeScale = WORD_SIEGE_PRACTICE_CLOSE_SCALE"))
        assertTrue(board.contains("clampWordSiegeBoardPan"))
        assertTrue(board.contains("clipToBounds"))
    }

    @Test fun lastMoveHighlightIsOneShotAndDeduped() {
        val viewport = src("WordSiegeBoardViewport.kt")
        val practice = src("WordSiegePracticeBoard.kt")
        val match = src("WordSiegePanMatch.kt")
        assertTrue(viewport.contains("WORD_SIEGE_LAST_MOVE_ENTER_MS = 180"))
        assertTrue(viewport.contains("WORD_SIEGE_LAST_MOVE_HOLD_MS = 1_200"))
        assertTrue(viewport.contains("WORD_SIEGE_LAST_MOVE_EXIT_MS = 200"))
        assertTrue(practice.contains("consumedHighlightKey"))
        assertTrue(match.contains("observedMoveId"))
        assertFalse(practice.contains("infiniteRepeatable"))
    }

    @Test fun exactTurkishKeyboardAndActionsRemain() {
        val classic = src("LightDuelUi.kt")
        val expectedRows = listOf(
            "listOf(\"Q\", \"W\", \"E\", \"R\", \"T\", \"Y\", \"U\", \"I\", \"O\", \"P\", \"Ğ\", \"Ü\")",
            "listOf(\"A\", \"S\", \"D\", \"F\", \"G\", \"H\", \"J\", \"K\", \"L\", \"Ş\", \"İ\")",
            "listOf(\"Z\", \"X\", \"C\", \"V\", \"B\", \"N\", \"M\", \"Ö\", \"Ç\")",
        )
        expectedRows.forEach { assertTrue(classic.contains(it)) }
        assertTrue(classic.contains("sh(\"SİL\", \"DELETE\")"))
        assertTrue(classic.contains("sh(\"TEMİZLE\", \"CLEAR\")"))
        assertTrue(classic.contains("sh(\"GÖNDER\", \"SEND\")"))
        assertTrue(classic.contains("● SOHBET"))
        assertTrue(classic.contains("★ BONUS"))
        assertFalse(classic.contains("BasicTextField"))
    }

    @Test fun typographyContractTargetsArePresent() {
        val classic = src("LightDuelUi.kt")
        val practice = src("WordSiegePracticeScreen.kt")
        assertTrue(classic.contains("fontSize = 15.sp"))
        assertTrue(classic.contains("fontSize = 12.sp"))
        assertTrue(classic.contains("fontSize = if (value.isBlank()) 16.sp else 20.sp"))
        assertTrue(practice.contains("size = 50.dp"))
        assertTrue(practice.contains("fontSize = 30.sp"))
        assertTrue(practice.contains("fontSize = 15.sp"))
    }

    @Test fun scoringStillUsesTwoPointTransferWithoutWordRollback() {
        assertEquals(2, WordSiegeFinalRules.CUBE_TRANSFER_POINTS)
        assertEquals(24, WordSiegeFinalRules.netScore(wordScore = 20, earnedCubePoints = 4, ignoredOpponentEarnedCubePoints = 99))
        assertEquals(16, WordSiegeFinalRules.currentTerritoryScore(wordScore = 10, ownedCubes = 3))
    }
}
''')

# ---- CI provenance gate: source must be committed before build; emit SHA/size/hash/signature ----
p = ".github/workflows/android.yml"
t = read(p)
t = one(t,
'''      - name: Build debug APK with tests\n        run: gradle :app:testDebugUnitTest :app:assembleDebug --stacktrace --warning-mode all''',
'''      - name: Verify committed source state
        run: |
          echo "SOURCE_SHA=$(git rev-parse HEAD)"
          git diff --exit-code
          test -z "$(git status --porcelain --untracked-files=no)"

      - name: Build debug APK with tests
        run: gradle clean :app:testDebugUnitTest :app:assembleDebug --stacktrace --warning-mode all''',
"CI clean provenance gate")
t = one(t,
'''      - name: Prepare test APK artifact\n        run: cp app/build/outputs/apk/debug/app-debug.apk SonHarf-Final-Test.apk''',
'''      - name: Prepare test APK artifact
        run: cp app/build/outputs/apk/debug/app-debug.apk SonHarf-Final-Test.apk

      - name: Verify APK provenance and integrity
        run: |
          echo "SOURCE_SHA=$(git rev-parse HEAD)"
          echo "APK_BYTES=$(stat -c%s SonHarf-Final-Test.apk)"
          sha256sum SonHarf-Final-Test.apk | tee SonHarf-Final-Test.sha256
          APKSIGNER="$(find "$ANDROID_HOME/build-tools" -name apksigner -type f | sort -V | tail -1)"
          "$APKSIGNER" verify --verbose --print-certs SonHarf-Final-Test.apk''',
"CI apk integrity gate")
write(p, t)

print("Guarded active-screen patch applied successfully")
