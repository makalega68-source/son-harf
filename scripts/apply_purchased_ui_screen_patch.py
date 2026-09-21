from pathlib import Path

path = Path("app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt")
text = path.read_text()

start_marker = "    LaunchedEffect(game.id, game.status) {"
end_marker = "\n@Composable\nprivate fun PanSiegeBoard("
start = text.find(start_marker)
end = text.find(end_marker, start)
if start < 0 or end < 0:
    raise RuntimeError("siege repair markers missing")

replacement = r'''    LaunchedEffect(game.id, game.status) {
        if (game.status == "waiting") {
            delay(WORD_SIEGE_BOT_FALLBACK_DELAY_MS)
            fallbackPracticeActive = true
        } else {
            fallbackPracticeActive = false
        }
    }

    if (fallbackPracticeActive && game.status == "waiting") {
        WordSiegePracticeScreen(
            onExit = { fallbackPracticeActive = false },
            matchmakingFallback = true,
        )
        return
    }

    WordSiegeGameTheme {
        Column(
            Modifier
                .fillMaxSize()
                .background(WordSiegeGameUi.Background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PurchasedPanel(
                modifier = Modifier.fillMaxWidth(),
                asset = PurchasedUiAsset.PANEL_SMALL,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    PurchasedIconButton(
                        asset = PurchasedUiAsset.NAV_HOME,
                        onClick = onBack,
                        modifier = Modifier.size(46.dp),
                        contentDescription = sh("Oyunlar", "Games"),
                    )
                    Spacer(Modifier.width(6.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            sh("KELİME KUŞATMASI", "WORD SIEGE"),
                            color = Color(0xFF4A2D20),
                            fontSize = 15.sp,
                            lineHeight = 19.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                        )
                        Text(
                            if (game.status == "playing") {
                                if (visualMyTurn) sh("SIRA SENDE", "YOUR TURN") else sh("RAKİPTE", "RIVAL'S TURN")
                            } else panSiegeStatusLabel(game, me),
                            color = if (visualMyTurn) PanSiegeMineBorder else PanSiegeRivalBorder,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    PurchasedButton(
                        text = sh("PES", "FORFEIT"),
                        onClick = onForfeit,
                        enabled = game.status == "playing" && !busy,
                        modifier = Modifier.width(92.dp).height(48.dp),
                        style = PurchasedButtonStyle.DANGER,
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                PanSiegePlayerCard(
                    profile = mine,
                    fallbackName = sh("Sen", "You"),
                    score = displayedMyScore,
                    wordPoints = myWordPoints,
                    territoryPoints = myTerritoryPoints,
                    areaCount = myAreaCount,
                    accent = PanSiegeMineBorder,
                    active = displayedCurrentPlayerId == me,
                    leading = myTargetScore > rivalTargetScore,
                    modifier = Modifier.weight(1f),
                )
                PanSiegePlayerCard(
                    profile = opponent,
                    fallbackName = if (game.status == "waiting") sh("Rakip aranıyor", "Finding rival") else sh("Rakip", "Rival"),
                    score = displayedRivalScore,
                    wordPoints = rivalWordPoints,
                    territoryPoints = rivalTerritoryPoints,
                    areaCount = rivalAreaCount,
                    accent = PanSiegeRivalBorder,
                    active = displayedCurrentPlayerId == opponentId,
                    leading = rivalTargetScore > myTargetScore,
                    modifier = Modifier.weight(1f),
                )
            }

            WordSiegeOwnershipLegend()

            if (game.status == "waiting") {
                PurchasedPanel(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    asset = PurchasedUiAsset.PANEL_LARGE,
                    contentPadding = PaddingValues(24.dp),
                ) {
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        PurchasedAsset(PurchasedUiAsset.ICON_SWORDS, Modifier.size(72.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(sh("RAKİP ARANIYOR", "FINDING A RIVAL"), color = Color(0xFF4A2D20), fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            sh(
                                "15 saniye içinde rakip bulunmazsa geçici bot maçı hemen başlayacak. Gerçek rakip araması arka planda sürecek.",
                                "If no rival is found within 15 seconds, a temporary bot match starts immediately while real matchmaking continues in the background.",
                            ),
                            color = Color(0xFF765746),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(16.dp))
                        PurchasedButton(
                            text = sh("ARAMAYI İPTAL ET", "CANCEL SEARCH"),
                            onClick = onCancelWaiting,
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth(),
                            style = PurchasedButtonStyle.DANGER,
                            leadingAsset = PurchasedUiAsset.ICON_CLOSE,
                        )
                    }
                }
                notice?.let { PanSiegeNotice(it) }
                return@Column
            }

            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                PanSiegeBoard(
                    gameId = game.id,
                    board = game.board,
                    rack = rack,
                    placements = placements,
                    myOwner = myOwner,
                    enabled = canAct,
                    lastMove = lastMove,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    onCell = onBoardCell,
                    onChat = onChat,
                )
            }

            if (game.status == "playing") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (placements.isNotEmpty()) {
                        Text(readyFeedback.message, color = PanSiegeMineBorder, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    } else Spacer(Modifier.weight(1f))
                    Spacer(Modifier.weight(1f))
                    Text(sh("Torba ${game.bag.length}", "Bag ${game.bag.length}"), color = WordSiegeGameUi.Muted, fontSize = 8.sp)
                }

                WordSiegePremiumPanel(
                    game = game,
                    placements = placements,
                    canAct = canAct,
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    rackOrder.forEach { rackIndex ->
                        val letter = rack.getOrNull(rackIndex) ?: return@forEach
                        PanSiegeRackTile(
                            letter = letter,
                            selected = selectedRackIndex == rackIndex,
                            used = rackIndex in placements.values,
                            enabled = canAct,
                            modifier = Modifier.weight(1f),
                            onClick = { onRackTile(rackIndex) },
                        )
                    }
                    repeat((7 - rack.length).coerceAtLeast(0)) { Spacer(Modifier.weight(1f).height(48.dp)) }
                }

                Row(Modifier.fillMaxWidth()) {
                    WordSiegeCompactAction(sh("GERİ AL", "UNDO"), Icons.Rounded.Undo,
                        canAct && placements.isNotEmpty(), Modifier.weight(1f)) {
                        placements.keys.lastOrNull()?.let(onBoardCell)
                    }
                    WordSiegeCompactAction(sh("KARIŞTIR", "SHUFFLE"), Icons.Rounded.Shuffle,
                        canAct && rack.length > 1, Modifier.weight(1f)) {
                        shuffleSeed = if (shuffleSeed == Int.MAX_VALUE) 1 else shuffleSeed + 1
                    }
                    WordSiegeCompactAction(sh("PAS", "PASS"), Icons.Rounded.SkipNext,
                        canAct, Modifier.weight(1f), onPass)
                    WordSiegeCompactAction(sh("DEĞİŞTİR", "EXCHANGE"), Icons.Rounded.SwapHoriz,
                        canAct && game.bag.isNotEmpty(), Modifier.weight(1f), onExchange)
                }
                PurchasedButton(
                    text = if (busy) "…" else sh("HAMLEYİ ONAYLA", "CONFIRM MOVE"),
                    onClick = onSubmit,
                    enabled = canAct && placements.isNotEmpty() && !busy,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    style = PurchasedButtonStyle.PRIMARY,
                    leadingAsset = PurchasedUiAsset.ICON_CHECK,
                )
            } else {
                PanSiegeFinishedCard(game, me)
            }

            notice?.let { PanSiegeNotice(it) }
            lastMove?.let { PanSiegeLastMoveInfo(it) }
        }
    }
}
'''

repaired = text[:start] + replacement + text[end:]

required = [
    "delay(WORD_SIEGE_BOT_FALLBACK_DELAY_MS)",
    "WordSiegePracticeScreen(",
    "PurchasedUiAsset.PANEL_SMALL",
    "PurchasedUiAsset.ICON_SWORDS",
    "WordSiegeFinalRules.currentTerritoryScore",
    "WordSiegeBoardSpec.Size",
    "HAMLEYİ ONAYLA",
]
for token in required:
    if token not in repaired:
        raise RuntimeError(f"required siege contract missing: {token}")

if repaired.count("@Composable\nprivate fun PanSiegeBoard(") != 1:
    raise RuntimeError("PanSiegeBoard declaration count changed")

path.write_text(repaired)
print("WordSiegePanMatch composable structure repaired; gameplay contracts preserved")
