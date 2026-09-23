from pathlib import Path

ROOT = Path('.')

def replace_once(path, old, new):
    p = ROOT / path
    text = p.read_text()
    if old not in text:
        raise SystemExit(f'missing pattern in {path}: {old[:120]!r}')
    text = text.replace(old, new, 1)
    p.write_text(text)

# Capture batch: remember which newly won cubes came from the rival.
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegeCaptureEffect.kt',
    '''internal const val WORD_SIEGE_CAPTURE_POINTS_PER_CUBE = 2\n\ninternal data class WordSiegeCaptureBatch(\n    val updateKey: String,\n    val owner: Int,\n    val indices: List<Int>,\n) {''',
    '''internal const val WORD_SIEGE_CAPTURE_POINTS_PER_CUBE = 2\ninternal const val WORD_SIEGE_OPPONENT_LOSS_PER_CUBE = 1\n\ninternal data class WordSiegeCaptureBatch(\n    val updateKey: String,\n    val owner: Int,\n    val indices: List<Int>,\n    val opponentIndices: Set<Int> = emptySet(),\n) {'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegeCaptureEffect.kt',
    '''    val points: Int\n        get() = indices.size * WORD_SIEGE_CAPTURE_POINTS_PER_CUBE\n}''',
    '''    val points: Int\n        get() = indices.size * WORD_SIEGE_CAPTURE_POINTS_PER_CUBE\n\n    val opponentLossPoints: Int\n        get() = opponentIndices.size * WORD_SIEGE_OPPONENT_LOSS_PER_CUBE\n}'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegeCaptureEffect.kt',
    '''    return captured.takeIf { it.isNotEmpty() }?.let {\n        WordSiegeCaptureBatch(updateKey = updateKey, owner = capturingOwner, indices = it)\n    }\n}\n\ninternal fun wordSiegeDisplayedScore(actualScore: Int, pendingCapturePoints: Int): Int =\n    (actualScore - pendingCapturePoints.coerceAtLeast(0)).coerceAtLeast(0)''',
    '''    return captured.takeIf { it.isNotEmpty() }?.let { indices ->\n        val opponentIndices = indices\n            .filter { index -> previousOwners[index] != 0 && previousOwners[index] != capturingOwner }\n            .toSet()\n        WordSiegeCaptureBatch(\n            updateKey = updateKey,\n            owner = capturingOwner,\n            indices = indices,\n            opponentIndices = opponentIndices,\n        )\n    }\n}\n\ninternal fun wordSiegeDisplayedScore(\n    actualScore: Int,\n    pendingCapturePoints: Int,\n    pendingLossPoints: Int = 0,\n): Int = (actualScore - pendingCapturePoints.coerceAtLeast(0) + pendingLossPoints.coerceAtLeast(0)).coerceAtLeast(0)'''
)

# The flight callback reports the exact cube, so a rival-owned cube can decrement the rival score at arrival.
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegeCaptureMotion.kt',
    '    val onCubeArrived: () -> Unit,',
    '    val onCubeArrived: (Int) -> Unit,'
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegeCaptureMotion.kt',
    '                    onCubeArrived()\n',
    '                    onCubeArrived(effect.batch.indices[ordinal])\n'
)

# Shared score/profile cards: borderless premium surface, larger avatar, fixed score geometry, loss pulse.
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegeGameUi.kt',
    '''    modifier: Modifier = Modifier,\n    scoreArrivalTick: Int = 0,\n    onScoreCenterChanged: (Offset) -> Unit = {},\n) {\n    val edge = if (active) accent else WordSiegeGameUi.Border\n    val scoreScale = remember { Animatable(1f) }\n    val scoreGlow = remember { Animatable(0f) }''',
    '''    modifier: Modifier = Modifier,\n    scoreArrivalTick: Int = 0,\n    scoreLossTick: Int = 0,\n    onScoreCenterChanged: (Offset) -> Unit = {},\n) {\n    val scoreScale = remember { Animatable(1f) }\n    val scoreGlow = remember { Animatable(0f) }\n    val lossScale = remember { Animatable(1f) }\n    val lossGlow = remember { Animatable(0f) }'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegeGameUi.kt',
    '''    LaunchedEffect(scoreArrivalTick) {\n        if (scoreArrivalTick <= 0) return@LaunchedEffect\n        scoreScale.snapTo(1f)\n        scoreGlow.snapTo(0f)\n        coroutineScope {\n            launch {\n                scoreScale.animateTo(1.14f, tween(90))\n                scoreScale.animateTo(1f, tween(170))\n            }\n            launch {\n                scoreGlow.animateTo(1f, tween(70))\n                scoreGlow.animateTo(0f, tween(260))\n            }\n        }\n    }\n    Surface(\n        modifier = modifier,\n        color = lerp(WordSiegeGameUi.Surface, accent, if (active) .065f else .025f),\n        shape = RoundedCornerShape(16.dp),\n        border = BorderStroke(if (active) 1.5.dp else 1.dp, edge.copy(alpha = if (active) .72f else .72f)),\n        shadowElevation = if (active) 4.dp else 1.dp,\n    ) {\n        Column(Modifier.padding(horizontal = 9.dp, vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {''',
    '''    LaunchedEffect(scoreArrivalTick) {\n        if (scoreArrivalTick <= 0) return@LaunchedEffect\n        scoreScale.snapTo(1f)\n        scoreGlow.snapTo(0f)\n        coroutineScope {\n            launch {\n                scoreScale.animateTo(1.14f, tween(90))\n                scoreScale.animateTo(1f, tween(170))\n            }\n            launch {\n                scoreGlow.animateTo(1f, tween(70))\n                scoreGlow.animateTo(0f, tween(260))\n            }\n        }\n    }\n    LaunchedEffect(scoreLossTick) {\n        if (scoreLossTick <= 0) return@LaunchedEffect\n        lossScale.snapTo(1f)\n        lossGlow.snapTo(0f)\n        coroutineScope {\n            launch {\n                lossScale.animateTo(.91f, tween(70))\n                lossScale.animateTo(1f, tween(130))\n            }\n            launch {\n                lossGlow.animateTo(1f, tween(55))\n                lossGlow.animateTo(0f, tween(210))\n            }\n        }\n    }\n    Surface(\n        modifier = modifier.height(96.dp),\n        color = lerp(WordSiegeGameUi.Surface, accent, if (active) .055f else .018f),\n        shape = RoundedCornerShape(18.dp),\n        shadowElevation = if (active) 3.dp else 1.dp,\n    ) {\n        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegeGameUi.kt',
    '''                Surface(\n                    shape = RoundedCornerShape(13.dp),\n                    color = accent.copy(alpha = .09f),\n                    border = BorderStroke(1.dp, accent.copy(alpha = .22f)),\n                ) {\n                    Box(Modifier.padding(2.dp)) {\n                        ProfilePhotoAvatarWithGender(\n                            avatarPath = avatarPath, gender = gender, name = name,\n                            size = 42.dp, accent = accent, visible = avatarVisible,\n                        )\n                    }\n                }\n                Spacer(Modifier.width(7.dp))''',
    '''                Surface(\n                    shape = RoundedCornerShape(15.dp),\n                    color = accent.copy(alpha = .075f),\n                ) {\n                    Box(Modifier.padding(2.dp)) {\n                        ProfilePhotoAvatarWithGender(\n                            avatarPath = avatarPath, gender = gender, name = name,\n                            size = 50.dp, accent = accent, visible = avatarVisible,\n                        )\n                    }\n                }\n                Spacer(Modifier.width(6.dp))'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegeGameUi.kt',
    '''                        Text(sh("$area küp", "$area cubes"), color = WordSiegeGameUi.Muted, fontSize = 9.sp, lineHeight = 11.sp)''',
    '''                        Text(sh("$area küp", "$area cubes"), color = WordSiegeGameUi.Muted, fontSize = 9.sp, lineHeight = 11.sp, maxLines = 1)'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegeGameUi.kt',
    '''                            Surface(shape = RoundedCornerShape(99.dp), color = accent.copy(alpha = .12f)) {\n                                Text("BOT", Modifier.padding(horizontal = 5.dp, vertical = 1.dp), color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black)\n                            }''',
    '''                            Surface(shape = RoundedCornerShape(99.dp), color = accent.copy(alpha = .12f)) {\n                                Text("BOT", Modifier.padding(horizontal = 5.dp, vertical = 1.dp), color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1)\n                            }'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegeGameUi.kt',
    '''                        .graphicsLayer {\n                            scaleX = scoreScale.value\n                            scaleY = scoreScale.value\n                        }\n                        .drawBehind {\n                            if (scoreGlow.value > 0f) {\n                                drawCircle(\n                                    color = accent.copy(alpha = .55f * scoreGlow.value),\n                                    radius = size.maxDimension * (.58f + .10f * scoreGlow.value),\n                                    style = Stroke(width = 2.dp.toPx()),\n                                )\n                            }\n                        }''',
    '''                        .width(58.dp)\n                        .height(48.dp)\n                        .graphicsLayer {\n                            val combinedScale = scoreScale.value * lossScale.value\n                            scaleX = combinedScale\n                            scaleY = combinedScale\n                        }\n                        .drawBehind {\n                            if (scoreGlow.value > 0f) {\n                                drawCircle(\n                                    color = accent.copy(alpha = .55f * scoreGlow.value),\n                                    radius = size.maxDimension * (.58f + .10f * scoreGlow.value),\n                                    style = Stroke(width = 2.dp.toPx()),\n                                )\n                            }\n                            if (lossGlow.value > 0f) {\n                                drawCircle(\n                                    color = Color(0xFFB94B4B).copy(alpha = .62f * lossGlow.value),\n                                    radius = size.maxDimension * (.58f + .08f * lossGlow.value),\n                                    style = Stroke(width = 2.dp.toPx()),\n                                )\n                            }\n                        }'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegeGameUi.kt',
    '''                    Surface(\n                        shape = RoundedCornerShape(12.dp),\n                        color = accent.copy(alpha = .10f),\n                        border = BorderStroke(1.dp, accent.copy(alpha = .18f)),\n                    ) {\n                        Text(\n                            "$score",\n                            Modifier.padding(horizontal = 8.dp, vertical = 4.dp).semantics { contentDescription = totalDescription },\n                            color = accent,\n                            fontSize = 21.sp,\n                            lineHeight = 24.sp,\n                            fontWeight = FontWeight.Black,\n                            maxLines = 1,\n                        )\n                    }''',
    '''                    val scoreFontSize = when {\n                        score >= 10_000 -> 14.sp\n                        score >= 1_000 -> 17.sp\n                        else -> 21.sp\n                    }\n                    Surface(\n                        modifier = Modifier.fillMaxSize(),\n                        shape = RoundedCornerShape(13.dp),\n                        color = accent.copy(alpha = .10f),\n                    ) {\n                        Box(contentAlignment = Alignment.Center) {\n                            Text(\n                                "$score",\n                                Modifier.semantics { contentDescription = totalDescription },\n                                color = if (lossGlow.value > 0f) lerp(accent, Color(0xFFB94B4B), lossGlow.value) else accent,\n                                fontSize = scoreFontSize,\n                                lineHeight = 24.sp,\n                                fontWeight = FontWeight.Black,\n                                maxLines = 1,\n                            )\n                        }\n                    }'''
)

# Practice scoring becomes a territory ledger: +2 per won cube, -1 to rival for each rival cube taken.
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegePracticeEngine.kt',
    '''    val wordScore: Int,\n    val capturedCells: Int,\n)''',
    '''    val wordScore: Int,\n    val capturedCells: Int,\n    val opponentCaptured: Int = 0,\n)'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegePracticeEngine.kt',
    '''        val gainedCells = board.indices.count { index -> state.board[index].owner != owner && board[index].owner == owner }\n        val playerArea = board.count { it.owner == 1 }\n        val botArea = board.count { it.owner == 2 }\n        val next = state.copy(''',
    '''        val gainedCells = board.indices.count { index -> state.board[index].owner != owner && board[index].owner == owner }\n        val opponentCaptured = board.indices.count { index ->\n            state.board[index].owner == other(owner) && board[index].owner == owner\n        }\n        val playerArea = board.count { it.owner == 1 }\n        val botArea = board.count { it.owner == 2 }\n        val gainedPoints = WordSiegeFinalRules.cubeTransfer(gainedCells)\n        val rivalLoss = WordSiegeFinalRules.opponentCaptureLoss(opponentCaptured)\n        val next = state.copy('''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegePracticeEngine.kt',
    '''            playerAreaScore = WordSiegeFinalRules.cubeTransfer(playerArea),\n            botAreaScore = WordSiegeFinalRules.cubeTransfer(botArea),''',
    '''            playerAreaScore = if (owner == 1) {\n                state.playerAreaScore + gainedPoints\n            } else {\n                (state.playerAreaScore - rivalLoss).coerceAtLeast(0)\n            },\n            botAreaScore = if (owner == 2) {\n                state.botAreaScore + gainedPoints\n            } else {\n                (state.botAreaScore - rivalLoss).coerceAtLeast(0)\n            },'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegePracticeEngine.kt',
    '''        return finished to WordSiegePracticeMove(placements, horizontal, primary.orEmpty(), words, score, gainedCells)''',
    '''        return finished to WordSiegePracticeMove(\n            placements, horizontal, primary.orEmpty(), words, score, gainedCells, opponentCaptured,\n        )'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegePracticeEngine.kt',
    '''        val winner = forcedWinner ?: when {\n            state.playerArea > state.botArea -> 1\n            state.botArea > state.playerArea -> 2\n            else -> null\n        }''',
    '''        val winner = forcedWinner ?: when {\n            totalScore(state, 1) > totalScore(state, 2) -> 1\n            totalScore(state, 2) > totalScore(state, 1) -> 2\n            state.playerArea > state.botArea -> 1\n            state.botArea > state.playerArea -> 2\n            else -> null\n        }'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegePracticeEngine.kt',
    '''    fun totalScore(state: WordSiegePracticeState, owner: Int): Int = if (owner == 1) {\n        WordSiegeFinalRules.currentTerritoryScore(state.playerWordScore, state.playerArea)\n    } else {\n        WordSiegeFinalRules.currentTerritoryScore(state.botWordScore, state.botArea)\n    }''',
    '''    fun totalScore(state: WordSiegePracticeState, owner: Int): Int = if (owner == 1) {\n        WordSiegeFinalRules.scoreWithTerritoryLedger(state.playerWordScore, state.playerAreaScore)\n    } else {\n        WordSiegeFinalRules.scoreWithTerritoryLedger(state.botWordScore, state.botAreaScore)\n    }'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegePracticeEngine.kt',
    '''            WordSiegeFinalRules.cubeTransfer(move.capturedCells) +\n            opponentTakeovers * WordSiegeFinalRules.CUBE_TRANSFER_POINTS +''',
    '''            WordSiegeFinalRules.cubeTransfer(move.capturedCells) +\n            opponentTakeovers * WordSiegeFinalRules.OPPONENT_CAPTURE_LOSS_POINTS +'''
)

# Shared final-rule helpers for the new +2/-1 territory ledger.
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegeFinalRules.kt',
    '''    const val CUBE_TRANSFER_POINTS: Int = 2''',
    '''    const val CUBE_TRANSFER_POINTS: Int = 2\n    const val OPPONENT_CAPTURE_LOSS_POINTS: Int = 1'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegeFinalRules.kt',
    '''    fun currentTerritoryScore(wordScore: Int, ownedCubes: Int): Int =\n        wordScore + cubeTransfer(ownedCubes)''',
    '''    fun currentTerritoryScore(wordScore: Int, ownedCubes: Int): Int =\n        wordScore + cubeTransfer(ownedCubes)\n\n    fun opponentCaptureLoss(cubesLost: Int): Int =\n        cubesLost.coerceAtLeast(0) * OPPONENT_CAPTURE_LOSS_POINTS\n\n    fun scoreWithTerritoryLedger(wordScore: Int, territoryScore: Int): Int =\n        wordScore.coerceAtLeast(0) + territoryScore.coerceAtLeast(0)'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegeFinalRules.kt',
    '''        var ownedCubes = 0\n        moves.forEach { move ->\n            if (move.playerId == playerId) {\n                ownedCubes += move.neutralCaptured + move.opponentCaptured\n            } else {\n                ownedCubes -= move.opponentCaptured\n            }\n        }\n        return cubeTransfer(ownedCubes)''',
    '''        var territoryScore = 0\n        moves.forEach { move ->\n            if (move.playerId == playerId) {\n                territoryScore += cubeTransfer(move.neutralCaptured + move.opponentCaptured)\n            } else {\n                territoryScore -= opponentCaptureLoss(move.opponentCaptured)\n            }\n        }\n        return territoryScore.coerceAtLeast(0)'''
)

# Practice screen: no automatic tutorial, no ownership legend, territory ledger and synchronized loss countdown.
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt',
    '''    var pendingPlayerCapturePoints by remember { mutableIntStateOf(0) }\n    var pendingBotCapturePoints by remember { mutableIntStateOf(0) }\n    var playerScoreArrivalTick by remember { mutableIntStateOf(0) }\n    var botScoreArrivalTick by remember { mutableIntStateOf(0) }''',
    '''    var pendingPlayerCapturePoints by remember { mutableIntStateOf(0) }\n    var pendingBotCapturePoints by remember { mutableIntStateOf(0) }\n    var pendingPlayerLossPoints by remember { mutableIntStateOf(0) }\n    var pendingBotLossPoints by remember { mutableIntStateOf(0) }\n    var playerScoreArrivalTick by remember { mutableIntStateOf(0) }\n    var botScoreArrivalTick by remember { mutableIntStateOf(0) }\n    var playerScoreLossTick by remember { mutableIntStateOf(0) }\n    var botScoreLossTick by remember { mutableIntStateOf(0) }'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt',
    '''    var tutorialStep by remember {\n        mutableIntStateOf(\n            if (!matchmakingFallback && !WordSiegePracticeTutorialPrefs.isCompleted(context)) 0 else -1,\n        )\n    }''',
    '''    var tutorialStep by remember { mutableIntStateOf(-1) }'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt',
    '''    val displayedPlayerScore = wordSiegeDisplayedScore(playerTargetScore, pendingPlayerCapturePoints)\n    val displayedBotScore = wordSiegeDisplayedScore(botTargetScore, pendingBotCapturePoints)''',
    '''    val displayedPlayerScore = wordSiegeDisplayedScore(\n        playerTargetScore, pendingPlayerCapturePoints, pendingPlayerLossPoints,\n    )\n    val displayedBotScore = wordSiegeDisplayedScore(\n        botTargetScore, pendingBotCapturePoints, pendingBotLossPoints,\n    )'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt',
    '''    val playerTerritoryPoints = WordSiegeFinalRules.cubeTransfer(state.playerArea)\n    val botTerritoryPoints = WordSiegeFinalRules.cubeTransfer(state.botArea)''',
    '''    val playerTerritoryPoints = state.playerAreaScore\n    val botTerritoryPoints = state.botAreaScore'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt',
    '''        if (owner == 1) pendingPlayerCapturePoints += batch.points\n        else pendingBotCapturePoints += batch.points''',
    '''        if (owner == 1) {\n            pendingPlayerCapturePoints += batch.points\n            pendingBotLossPoints += batch.opponentLossPoints\n        } else {\n            pendingBotCapturePoints += batch.points\n            pendingPlayerLossPoints += batch.opponentLossPoints\n        }'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt',
    '''        pendingPlayerCapturePoints = 0\n        pendingBotCapturePoints = 0\n        playerScoreArrivalTick = 0\n        botScoreArrivalTick = 0''',
    '''        pendingPlayerCapturePoints = 0\n        pendingBotCapturePoints = 0\n        pendingPlayerLossPoints = 0\n        pendingBotLossPoints = 0\n        playerScoreArrivalTick = 0\n        botScoreArrivalTick = 0\n        playerScoreLossTick = 0\n        botScoreLossTick = 0'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt',
    '''            onCubeArrived = {\n                if (batch.owner == 1) {\n                    pendingPlayerCapturePoints =\n                        (pendingPlayerCapturePoints - WORD_SIEGE_CAPTURE_POINTS_PER_CUBE).coerceAtLeast(0)\n                    playerScoreArrivalTick += 1\n                } else {\n                    pendingBotCapturePoints =\n                        (pendingBotCapturePoints - WORD_SIEGE_CAPTURE_POINTS_PER_CUBE).coerceAtLeast(0)\n                    botScoreArrivalTick += 1\n                }\n            },''',
    '''            onCubeArrived = { index ->\n                if (batch.owner == 1) {\n                    pendingPlayerCapturePoints =\n                        (pendingPlayerCapturePoints - WORD_SIEGE_CAPTURE_POINTS_PER_CUBE).coerceAtLeast(0)\n                    playerScoreArrivalTick += 1\n                    if (index in batch.opponentIndices) {\n                        pendingBotLossPoints =\n                            (pendingBotLossPoints - WORD_SIEGE_OPPONENT_LOSS_PER_CUBE).coerceAtLeast(0)\n                        botScoreLossTick += 1\n                    }\n                } else {\n                    pendingBotCapturePoints =\n                        (pendingBotCapturePoints - WORD_SIEGE_CAPTURE_POINTS_PER_CUBE).coerceAtLeast(0)\n                    botScoreArrivalTick += 1\n                    if (index in batch.opponentIndices) {\n                        pendingPlayerLossPoints =\n                            (pendingPlayerLossPoints - WORD_SIEGE_OPPONENT_LOSS_PER_CUBE).coerceAtLeast(0)\n                        playerScoreLossTick += 1\n                    }\n                }\n            },'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt',
    '''                        scoreArrivalTick = playerScoreArrivalTick,\n                        onScoreCenterChanged = { playerScoreTargetInWindow = it },''',
    '''                        scoreArrivalTick = playerScoreArrivalTick,\n                        scoreLossTick = playerScoreLossTick,\n                        onScoreCenterChanged = { playerScoreTargetInWindow = it },'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt',
    '''                        scoreArrivalTick = botScoreArrivalTick,\n                        onScoreCenterChanged = { botScoreTargetInWindow = it },''',
    '''                        scoreArrivalTick = botScoreArrivalTick,\n                        scoreLossTick = botScoreLossTick,\n                        onScoreCenterChanged = { botScoreTargetInWindow = it },'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt',
    '''\n                if (boardViewportMode == WordSiegeBoardViewportMode.FIT) WordSiegeOwnershipLegend()\n''',
    '''\n'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt',
    '''    modifier: Modifier = Modifier,\n    scoreArrivalTick: Int = 0,\n    onScoreCenterChanged: (Offset) -> Unit = {},''',
    '''    modifier: Modifier = Modifier,\n    scoreArrivalTick: Int = 0,\n    scoreLossTick: Int = 0,\n    onScoreCenterChanged: (Offset) -> Unit = {},'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt',
    '''        scoreArrivalTick = scoreArrivalTick,\n        onScoreCenterChanged = onScoreCenterChanged,''',
    '''        scoreArrivalTick = scoreArrivalTick,\n        scoreLossTick = scoreLossTick,\n        onScoreCenterChanged = onScoreCenterChanged,'''
)

# Practice board: very thin black cell outline and stronger overview typography.
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt',
    '''            .border(\n                width = when {\n                    lastMoveHighlight > 0f -> 1.7.dp\n                    threatened && owner != 0 -> 1.5.dp\n                    else -> .55.dp\n                },\n                color = if (lastMoveHighlight > 0f) {\n                    PracticeLastMove.copy(alpha = 0.45f + .45f * lastMoveHighlight)\n                } else borderColor,\n                shape = RoundedCornerShape(8.dp),\n            ),''',
    '''            .border(\n                width = if (lastMoveHighlight > 0f) 1.7.dp else .45.dp,\n                color = if (lastMoveHighlight > 0f) {\n                    PracticeLastMove.copy(alpha = 0.45f + .45f * lastMoveHighlight)\n                } else Color.Black.copy(alpha = .52f),\n                shape = RoundedCornerShape(8.dp),\n            ),'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt',
    '''                fontSize = 22.sp,\n                fontFamily = FontFamily.Serif,\n                fontWeight = FontWeight.Black,\n                letterSpacing = .35.sp,''',
    '''                fontSize = if (overview) 24.sp else 22.sp,\n                fontFamily = FontFamily.Serif,\n                fontWeight = FontWeight.Black,\n                letterSpacing = if (overview) .10.sp else .25.sp,'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt',
    '''                fontWeight = FontWeight.Light,\n            )''',
    '''                fontWeight = if (overview) FontWeight.SemiBold else FontWeight.Medium,\n            )'''
)

# Live match: use authoritative area-score ledger, synchronized rival loss countdown, no ownership legend.
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt',
    '''    val myTerritoryPoints = WordSiegeFinalRules.cubeTransfer(myAreaCount)\n    val rivalTerritoryPoints = WordSiegeFinalRules.cubeTransfer(rivalAreaCount)\n    val myTargetScore = WordSiegeFinalRules.currentTerritoryScore(myWordPoints, myAreaCount)\n    val rivalTargetScore = WordSiegeFinalRules.currentTerritoryScore(rivalWordPoints, rivalAreaCount)''',
    '''    val myTerritoryPoints = if (myOwner == 1) game.playerOneAreaScore else game.playerTwoAreaScore\n    val rivalTerritoryPoints = if (rivalOwner == 1) game.playerOneAreaScore else game.playerTwoAreaScore\n    val myTargetScore = WordSiegeFinalRules.scoreWithTerritoryLedger(myWordPoints, myTerritoryPoints)\n    val rivalTargetScore = WordSiegeFinalRules.scoreWithTerritoryLedger(rivalWordPoints, rivalTerritoryPoints)'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt',
    '''    var pendingMyCapturePoints by remember(game.id) { mutableIntStateOf(0) }\n    var pendingRivalCapturePoints by remember(game.id) { mutableIntStateOf(0) }\n    var myScoreArrivalTick by remember(game.id) { mutableIntStateOf(0) }\n    var rivalScoreArrivalTick by remember(game.id) { mutableIntStateOf(0) }''',
    '''    var pendingMyCapturePoints by remember(game.id) { mutableIntStateOf(0) }\n    var pendingRivalCapturePoints by remember(game.id) { mutableIntStateOf(0) }\n    var pendingMyLossPoints by remember(game.id) { mutableIntStateOf(0) }\n    var pendingRivalLossPoints by remember(game.id) { mutableIntStateOf(0) }\n    var myScoreArrivalTick by remember(game.id) { mutableIntStateOf(0) }\n    var rivalScoreArrivalTick by remember(game.id) { mutableIntStateOf(0) }\n    var myScoreLossTick by remember(game.id) { mutableIntStateOf(0) }\n    var rivalScoreLossTick by remember(game.id) { mutableIntStateOf(0) }'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt',
    '''    val candidateRivalPoints =\n        if (!candidateAlreadyQueued && captureCandidate?.owner == rivalOwner) captureCandidate.points else 0\n    val displayedMyScore =\n        wordSiegeDisplayedScore(myTargetScore, pendingMyCapturePoints + candidateMyPoints)\n    val displayedRivalScore =\n        wordSiegeDisplayedScore(rivalTargetScore, pendingRivalCapturePoints + candidateRivalPoints)''',
    '''    val candidateRivalPoints =\n        if (!candidateAlreadyQueued && captureCandidate?.owner == rivalOwner) captureCandidate.points else 0\n    val candidateMyLoss =\n        if (!candidateAlreadyQueued && captureCandidate?.owner == rivalOwner) captureCandidate.opponentLossPoints else 0\n    val candidateRivalLoss =\n        if (!candidateAlreadyQueued && captureCandidate?.owner == myOwner) captureCandidate.opponentLossPoints else 0\n    val displayedMyScore = wordSiegeDisplayedScore(\n        myTargetScore, pendingMyCapturePoints + candidateMyPoints, pendingMyLossPoints + candidateMyLoss,\n    )\n    val displayedRivalScore = wordSiegeDisplayedScore(\n        rivalTargetScore, pendingRivalCapturePoints + candidateRivalPoints, pendingRivalLossPoints + candidateRivalLoss,\n    )'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt',
    '''            if (batch.owner == myOwner) {\n                pendingMyCapturePoints += batch.points\n            } else if (batch.owner == rivalOwner) {\n                pendingRivalCapturePoints += batch.points\n            }''',
    '''            if (batch.owner == myOwner) {\n                pendingMyCapturePoints += batch.points\n                pendingRivalLossPoints += batch.opponentLossPoints\n            } else if (batch.owner == rivalOwner) {\n                pendingRivalCapturePoints += batch.points\n                pendingMyLossPoints += batch.opponentLossPoints\n            }'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt',
    '''            onCubeArrived = {\n                if (batch.owner == myOwner) {\n                    pendingMyCapturePoints =\n                        (pendingMyCapturePoints - WORD_SIEGE_CAPTURE_POINTS_PER_CUBE).coerceAtLeast(0)\n                    myScoreArrivalTick += 1\n                } else {\n                    pendingRivalCapturePoints =\n                        (pendingRivalCapturePoints - WORD_SIEGE_CAPTURE_POINTS_PER_CUBE).coerceAtLeast(0)\n                    rivalScoreArrivalTick += 1\n                }\n            },''',
    '''            onCubeArrived = { index ->\n                if (batch.owner == myOwner) {\n                    pendingMyCapturePoints =\n                        (pendingMyCapturePoints - WORD_SIEGE_CAPTURE_POINTS_PER_CUBE).coerceAtLeast(0)\n                    myScoreArrivalTick += 1\n                    if (index in batch.opponentIndices) {\n                        pendingRivalLossPoints =\n                            (pendingRivalLossPoints - WORD_SIEGE_OPPONENT_LOSS_PER_CUBE).coerceAtLeast(0)\n                        rivalScoreLossTick += 1\n                    }\n                } else {\n                    pendingRivalCapturePoints =\n                        (pendingRivalCapturePoints - WORD_SIEGE_CAPTURE_POINTS_PER_CUBE).coerceAtLeast(0)\n                    rivalScoreArrivalTick += 1\n                    if (index in batch.opponentIndices) {\n                        pendingMyLossPoints =\n                            (pendingMyLossPoints - WORD_SIEGE_OPPONENT_LOSS_PER_CUBE).coerceAtLeast(0)\n                        myScoreLossTick += 1\n                    }\n                }\n            },'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt',
    '''                scoreArrivalTick = myScoreArrivalTick,\n                onScoreCenterChanged = { myScoreTargetInWindow = it },''',
    '''                scoreArrivalTick = myScoreArrivalTick,\n                scoreLossTick = myScoreLossTick,\n                onScoreCenterChanged = { myScoreTargetInWindow = it },'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt',
    '''                scoreArrivalTick = rivalScoreArrivalTick,\n                onScoreCenterChanged = { rivalScoreTargetInWindow = it },''',
    '''                scoreArrivalTick = rivalScoreArrivalTick,\n                scoreLossTick = rivalScoreLossTick,\n                onScoreCenterChanged = { rivalScoreTargetInWindow = it },'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt',
    '''\n        if (boardViewportMode == WordSiegeBoardViewportMode.FIT) WordSiegeOwnershipLegend()\n''',
    '''\n'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt',
    '''    modifier: Modifier = Modifier,\n    scoreArrivalTick: Int = 0,\n    onScoreCenterChanged: (Offset) -> Unit = {},''',
    '''    modifier: Modifier = Modifier,\n    scoreArrivalTick: Int = 0,\n    scoreLossTick: Int = 0,\n    onScoreCenterChanged: (Offset) -> Unit = {},'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt',
    '''        scoreArrivalTick = scoreArrivalTick,\n        onScoreCenterChanged = onScoreCenterChanged,''',
    '''        scoreArrivalTick = scoreArrivalTick,\n        scoreLossTick = scoreLossTick,\n        onScoreCenterChanged = onScoreCenterChanged,'''
)
# Live board thin black outline and stronger overview typography.
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt',
    '''            border = BorderStroke(if (pending) maxOf(2.dp, borderWidth) else borderWidth, border.copy(alpha = .92f)),''',
    '''            border = BorderStroke(\n                if (pending) maxOf(1.4.dp, borderWidth) else .45.dp,\n                if (pending) border.copy(alpha = .92f) else Color.Black.copy(alpha = .52f),\n            ),'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt',
    '''                        fontSize = 22.sp,\n                        fontFamily = FontFamily.Serif,\n                        fontWeight = FontWeight.Black,\n                        letterSpacing = .35.sp,''',
    '''                        fontSize = if (overview) 24.sp else 22.sp,\n                        fontFamily = FontFamily.Serif,\n                        fontWeight = FontWeight.Black,\n                        letterSpacing = if (overview) .10.sp else .25.sp,'''
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt',
    '''                        fontWeight = FontWeight.Light,\n                    )''',
    '''                        fontWeight = if (overview) FontWeight.SemiBold else FontWeight.Medium,\n                    )'''
)

# Tests: capture-loss semantics and new authoritative territory ledger.
replace_once(
    'app/src/test/java/com/sonharf/game/WordSiegeCaptureEffectTest.kt',
    '''        assertEquals(listOf(0, 1), batch!!.indices)\n        assertEquals(4, batch.points)''',
    '''        assertEquals(listOf(0, 1), batch!!.indices)\n        assertEquals(setOf(1), batch.opponentIndices)\n        assertEquals(4, batch.points)\n        assertEquals(1, batch.opponentLossPoints)'''
)
replace_once(
    'app/src/test/java/com/sonharf/game/WordSiegeCaptureEffectTest.kt',
    '''        assertEquals(0, wordSiegeDisplayedScore(actualScore = 0, pendingCapturePoints = 4))''',
    '''        assertEquals(0, wordSiegeDisplayedScore(actualScore = 0, pendingCapturePoints = 4))\n        assertEquals(43, wordSiegeDisplayedScore(actualScore = 42, pendingCapturePoints = 0, pendingLossPoints = 1))'''
)
replace_once(
    'app/src/test/java/com/sonharf/game/WordSiegeFinalRulesTest.kt',
    '''        assertEquals(4, WordSiegeFinalRules.earnedCubePoints(listOf(first, rivalNeutral, rivalTakesMine), "me"))''',
    '''        assertEquals(5, WordSiegeFinalRules.earnedCubePoints(listOf(first, rivalNeutral, rivalTakesMine), "me"))'''
)
replace_once(
    'app/src/test/java/com/sonharf/game/WordSiegeFinalRulesTest.kt',
    '''        assertTrue(pan.contains("WordSiegeFinalRules.currentTerritoryScore"))\n        assertFalse(pan.contains("playerOneAreaScore"))\n        assertFalse(pan.contains("playerTwoAreaScore"))''',
    '''        assertTrue(pan.contains("WordSiegeFinalRules.scoreWithTerritoryLedger"))\n        assertTrue(pan.contains("playerOneAreaScore"))\n        assertTrue(pan.contains("playerTwoAreaScore"))'''
)

# Forward-only server migration. Existing ledgers are rebuilt from authoritative move history;
# future opponent captures subtract exactly one point from the rival territory ledger.
migration = ROOT / 'supabase/migrations/20260923131500_word_siege_capture_loss_penalty_v8.sql'
migration.write_text(r'''-- Word Siege v8: +2 to the capturer per newly won cube; -1 to the rival per rival-owned cube lost.
-- Word points remain permanent. Ownership counts remain unchanged and continue to drive territory control.

-- Rebuild the territory score ledger for existing games from authoritative move history.
update public.word_siege_games g
set player_one_area_score = greatest(0, coalesce((
      select sum(case
        when m.player_id = g.player_one_id then coalesce(m.area_score, 0)
        else -coalesce(m.opponent_captured, 0)
      end)::integer
      from public.word_siege_moves m
      where m.game_id = g.id
    ), 0)),
    player_two_area_score = greatest(0, coalesce((
      select sum(case
        when m.player_id = g.player_two_id then coalesce(m.area_score, 0)
        else -coalesce(m.opponent_captured, 0)
      end)::integer
      from public.word_siege_moves m
      where m.game_id = g.id
    ), 0));

do $patch_submit$
declare
  v_def text;
  v_src text;
begin
  select pg_get_functiondef(
    'private.submit_word_siege_move_v1(uuid,jsonb,boolean)'::regprocedure
  ) into v_def;
  if v_def is null then
    raise exception 'word_siege_submit_function_missing';
  end if;

  v_def := replace(
    v_def,
    'player_one_area_score = player_one_area_score + case when v_owner = 1 then v_area_score else 0 end,',
    'player_one_area_score = greatest(0, player_one_area_score + case when v_owner = 1 then v_area_score else -v_opponent_captured end),'
  );
  v_def := replace(
    v_def,
    'player_two_area_score = player_two_area_score + case when v_owner = 2 then v_area_score else 0 end,',
    'player_two_area_score = greatest(0, player_two_area_score + case when v_owner = 2 then v_area_score else -v_opponent_captured end),'
  );
  execute v_def;

  select p.prosrc into v_src
  from pg_proc p
  join pg_namespace n on n.oid = p.pronamespace
  where n.nspname = 'private'
    and p.proname = 'submit_word_siege_move_v1'
    and p.oid = 'private.submit_word_siege_move_v1(uuid,jsonb,boolean)'::regprocedure;

  if v_src not like '%player_one_area_score = greatest(0, player_one_area_score + case when v_owner = 1 then v_area_score else -v_opponent_captured end)%'
     or v_src not like '%player_two_area_score = greatest(0, player_two_area_score + case when v_owner = 2 then v_area_score else -v_opponent_captured end)%' then
    raise exception 'word_siege_capture_loss_patch_failed';
  end if;
end
$patch_submit$;

revoke all on function private.submit_word_siege_move_v1(uuid, jsonb, boolean) from public, anon, authenticated;
select pg_notify('pgrst', 'reload schema');
''')

# Source contract for the requested scope.
contract = ROOT / 'app/src/test/java/com/sonharf/game/WordSiegePremiumScorePenaltyContractTest.kt'
contract.write_text(r'''package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegePremiumScorePenaltyContractTest {
    @Test fun requestedSiegePresentationAndPenaltyAreLocked() {
        val ui = projectFile("app/src/main/java/com/sonharf/game/WordSiegeGameUi.kt").readText()
        val practice = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt").readText()
        val practiceBoard = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt").readText()
        val pan = projectFile("app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt").readText()
        val migration = projectFile("supabase/migrations/20260923131500_word_siege_capture_loss_penalty_v8.sql").readText()

        assertTrue(ui.contains("size = 50.dp"))
        assertTrue(ui.contains("modifier = modifier.height(96.dp)"))
        assertFalse(practice.contains("WordSiegeOwnershipLegend()"))
        assertFalse(pan.contains("WordSiegeOwnershipLegend()"))
        assertTrue(practice.contains("mutableIntStateOf(-1)"))
        assertTrue(practiceBoard.contains("Color.Black.copy(alpha = .52f)"))
        assertTrue(pan.contains("Color.Black.copy(alpha = .52f)"))
        assertTrue(practice.contains("pendingBotLossPoints"))
        assertTrue(pan.contains("pendingRivalLossPoints"))
        assertTrue(migration.contains("-v_opponent_captured"))
    }

    private fun projectFile(path: String): File = listOf(File(path), File("../$path"))
        .firstOrNull(File::exists) ?: error("Project path missing: $path")
}
''')
