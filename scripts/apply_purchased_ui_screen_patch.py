from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected 1 exact match, found {count}")
    return text.replace(old, new, 1)


def slice_replace(text: str, start_marker: str, end_marker: str, new: str, label: str) -> str:
    start = text.find(start_marker)
    if start < 0:
        raise RuntimeError(f"{label}: start marker missing")
    end = text.find(end_marker, start + len(start_marker))
    if end < 0:
        raise RuntimeError(f"{label}: end marker missing")
    return text[:start] + new + text[end:]


def patch_siege() -> None:
    path = Path("app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt")
    text = path.read_text()

    header_start = "        Row(verticalAlignment = Alignment.CenterVertically) {\n            IconButton(onClick = onBack)"
    header_end = "\n\n        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {"
    header_new = '''        PurchasedPanel(
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
                    Text(sh("KELİME KUŞATMASI", "WORD SIEGE"), color = Color(0xFF4A2D20), fontSize = 15.sp, lineHeight = 19.sp, fontWeight = FontWeight.Black, maxLines = 1)
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
        }'''
    text = slice_replace(text, header_start, header_end, header_new, "siege header")

    waiting_start = '        if (game.status == "waiting") {'
    waiting_end = '        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {'
    waiting_new = '''        if (game.status == "waiting") {
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

'''
    text = slice_replace(text, waiting_start, waiting_end, waiting_new, "siege waiting")

    old_submit = '''            Row(Modifier.fillMaxWidth()) {
                Button(
                    onClick = onSubmit,
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                    enabled = canAct && placements.isNotEmpty(),
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PanSiegeMineBorder,
                        contentColor = Color.White,
                        disabledContainerColor = WordSiegeGameUi.DisabledBackground,
                        disabledContentColor = WordSiegeGameUi.DisabledContent,
                    ),
                    contentPadding = PaddingValues(horizontal = 5.dp),
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text(sh("HAMLEYİ ONAYLA", "CONFIRM MOVE"), fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
            }
'''
    new_submit = '''            PurchasedButton(
                text = if (busy) "…" else sh("HAMLEYİ ONAYLA", "CONFIRM MOVE"),
                onClick = onSubmit,
                enabled = canAct && placements.isNotEmpty() && !busy,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                style = PurchasedButtonStyle.PRIMARY,
                leadingAsset = PurchasedUiAsset.ICON_CHECK,
            )
'''
    text = replace_once(text, old_submit, new_submit, "siege submit")

    old_focus = '''            SmallFloatingActionButton(
                onClick = {
                    viewportMode = WordSiegeBoardViewportMode.CLOSE
                    closePan = centerCloseOn(WordSiegeBoardSpec.CenterIndex)
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(7.dp).size(36.dp),
                shape = CircleShape,
                containerColor = Color.White.copy(alpha = .94f),
                contentColor = WordSiegeGameUi.Blue,
            ) {
                Icon(Icons.Rounded.CenterFocusStrong, sh("Merkeze dön", "Center board"), Modifier.size(19.dp))
            }
'''
    new_focus = '''            PurchasedIconButton(
                asset = PurchasedUiAsset.ICON_GAMES,
                onClick = {
                    viewportMode = WordSiegeBoardViewportMode.CLOSE
                    closePan = centerCloseOn(WordSiegeBoardSpec.CenterIndex)
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).size(44.dp),
                contentDescription = sh("Merkeze dön", "Center board"),
            )
'''
    text = replace_once(text, old_focus, new_focus, "siege focus")

    old_chat = '''            SmallFloatingActionButton(
                onClick = onChat,
                modifier = Modifier.align(Alignment.BottomEnd).padding(7.dp).size(42.dp),
                shape = CircleShape,
                containerColor = Color.White.copy(alpha = .96f),
                contentColor = WordSiegeGameUi.Muted,
            ) {
                Icon(Icons.Rounded.Chat, sh("Oyun içi sohbet", "In-game chat"), Modifier.size(20.dp))
            }
'''
    new_chat = '''            PurchasedIconButton(
                asset = PurchasedUiAsset.ICON_CHAT,
                onClick = onChat,
                modifier = Modifier.align(Alignment.BottomEnd).padding(5.dp).size(50.dp),
                contentDescription = sh("Oyun içi sohbet", "In-game chat"),
            )
'''
    text = replace_once(text, old_chat, new_chat, "siege chat")

    old_last = '''    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = WordSiegeGameUi.Surface,
        border = BorderStroke(1.dp, WordSiegeGameUi.Border),
    ) {
'''
    new_last = '''    PurchasedPanel(
        modifier = Modifier.fillMaxWidth(),
        asset = PurchasedUiAsset.PANEL_SMALL,
        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 5.dp),
    ) {
'''
    text = replace_once(text, old_last, new_last, "siege last move panel")
    text = text.replace('            Modifier.padding(horizontal = 9.dp, vertical = 5.dp),\n', '            Modifier.fillMaxWidth(),\n', 1)

    path.write_text(text)


def patch_letter_ladder() -> None:
    path = Path("app/src/main/java/com/sonharf/game/LetterLadderGame.kt")
    text = path.read_text()

    old_retry = '''                    Button(onClick = { puzzleNonce++ }) { Text(sh("TEKRAR DENE", "TRY AGAIN"), fontWeight = FontWeight.Black) }
                    TextButton(onClick = onExit) { Text(sh("Geri dön", "Go back")) }
'''
    new_retry = '''                    PurchasedButton(
                        text = sh("TEKRAR DENE", "TRY AGAIN"),
                        onClick = { puzzleNonce++ },
                        modifier = Modifier.fillMaxWidth(),
                        style = PurchasedButtonStyle.PRIMARY,
                        leadingAsset = PurchasedUiAsset.ICON_REPEAT,
                    )
                    PurchasedButton(
                        text = sh("GERİ DÖN", "GO BACK"),
                        onClick = onExit,
                        modifier = Modifier.fillMaxWidth(),
                        style = PurchasedButtonStyle.SECONDARY,
                        leadingAsset = PurchasedUiAsset.NAV_HOME,
                    )
'''
    text = replace_once(text, old_retry, new_retry, "letter retry")

    old_board_open = '''                Surface(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    color = LetterLadderUi.SurfaceRaised.copy(alpha = .94f),
                    border = BorderStroke(1.dp, LetterLadderUi.Border),
                ) {
'''
    new_board_open = '''                PurchasedPanel(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    asset = PurchasedUiAsset.PANEL_LARGE,
                    contentPadding = PaddingValues(0.dp),
                ) {
'''
    text = replace_once(text, old_board_open, new_board_open, "letter main panel")

    old_new_game = '''                    Button(
                        onClick = { puzzleNonce++ },
                        modifier = Modifier.fillMaxWidth().height(46.dp).sonHarfPressScale(pressedScale = 0.94f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LetterLadderUi.Purple,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(sh("YENİ OYUN", "NEW GAME"), fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Rounded.ChevronRight, null)
                    }
'''
    new_new_game = '''                    PurchasedButton(
                        text = sh("YENİ OYUN", "NEW GAME"),
                        onClick = { puzzleNonce++ },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        style = PurchasedButtonStyle.PURPLE,
                        leadingAsset = PurchasedUiAsset.ICON_REPEAT,
                    )
'''
    text = replace_once(text, old_new_game, new_new_game, "letter new game")

    # Preserve the strict visible row contract: START + 1/2/3/4 + TARGET.
    if 'for (move in 1 until LetterLadderEngine.MOVE_COUNT)' not in text:
        raise RuntimeError("letter row contract missing")
    if 'EmbeddedWordKeyboard(' not in text or 'maxLength = LetterLadderEngine.WORD_LENGTH' not in text:
        raise RuntimeError("letter keyboard contract missing")

    path.write_text(text)


patch_siege()
patch_letter_ladder()
print("Purchased UI screen patch applied safely")
