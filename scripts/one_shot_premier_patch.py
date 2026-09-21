from pathlib import Path

BRANCH_SOURCE = Path('app/src/main/java/com/sonharf/game/PremierWordDuelScreen.kt')
TEST_SOURCE = Path('app/src/test/java/com/sonharf/game/PremierDuelUxRegressionTest.kt')

s = BRANCH_SOURCE.read_text()
before = s


def replace_once(old: str, new: str, label: str) -> None:
    global s
    if old not in s:
        raise SystemExit(f'{label} missing; refusing partial patch')
    s = s.replace(old, new, 1)

replace_once(
'''            Surface(shape = CircleShape, color = PremierUi.Surface, border = BorderStroke(2.dp, PremierUi.Ocean)) {
                Icon(Icons.Rounded.Groups, null, tint = PremierUi.Ocean, modifier = Modifier.padding(27.dp).size(42.dp))
            }''',
'''            PurchasedPanel(
                modifier = Modifier.size(104.dp),
                asset = PurchasedUiAsset.PANEL_SMALL,
                contentPadding = PaddingValues(18.dp),
                contentAlignment = Alignment.Center,
            ) {
                PurchasedAsset(PurchasedUiAsset.ICON_GAMES, Modifier.size(64.dp))
            }''',
'Searching center',
)

replace_once(
'''        Surface(shape = RoundedCornerShape(99.dp), color = Color.Transparent) {
            Box(Modifier.background(Brush.horizontalGradient(listOf(PremierUi.Ocean, PremierUi.OceanDeep))).padding(horizontal = 27.dp, vertical = 10.dp)) {
                Text("VS", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
        }''',
'''        PurchasedPanel(
            modifier = Modifier.width(132.dp),
            asset = PurchasedUiAsset.PANEL_SMALL,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                PurchasedAsset(PurchasedUiAsset.ICON_SWORDS, Modifier.size(34.dp))
                Spacer(Modifier.width(7.dp))
                Text("VS", color = Color(0xFF4A2D20), fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
        }''',
'VS badge',
)

replace_once(
'''    val latestPlayedWord = words.lastOrNull()?.let { premierUpper(it.normalizedWord.ifBlank { it.word }, language) }.orEmpty()

    BoxWithConstraints(Modifier.fillMaxSize()) {''',
'''    val latestPlayedWord = words.lastOrNull()?.let { premierUpper(it.normalizedWord.ifBlank { it.word }, language) }.orEmpty()

    BoxWithConstraints(Modifier.fillMaxSize()) {
        PurchasedGameBackdrop(Modifier.matchParentSize())''',
'Arena backdrop',
)

replace_once(
'''            Surface(shape = RoundedCornerShape(16.dp), color = PremierUi.Surface, border = BorderStroke(1.dp, PremierUi.Sky.copy(alpha = .45f)), shadowElevation = 9.dp) {
                Text(floatingMessage?.body.orEmpty(), Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = PremierUi.OceanDeep, fontWeight = FontWeight.Black, fontSize = 13.sp)
            }''',
'''            PurchasedPanel(
                asset = PurchasedUiAsset.PANEL_SMALL,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PurchasedAsset(PurchasedUiAsset.ICON_CHAT, Modifier.size(28.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(floatingMessage?.body.orEmpty(), color = Color(0xFF4A2D20), fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            }''',
'Floating chat',
)

replace_once(
'''                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (feedback.accepted) PremierUi.GreenSoft else PremierUi.RedSoft,
                    border = BorderStroke(2.dp, accent),
                    shadowElevation = 12.dp,
                ) {
                    Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (feedback.accepted) Icons.Rounded.CheckCircle else Icons.Rounded.Close, null, tint = accent, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(9.dp))
                        Text(feedback.message, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                }''',
'''                PurchasedPanel(
                    asset = if (feedback.accepted) PurchasedUiAsset.REWARD_PANEL else PurchasedUiAsset.PANEL_SMALL,
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PurchasedAsset(
                            if (feedback.accepted) PurchasedUiAsset.ICON_CHECK else PurchasedUiAsset.ICON_CLOSE,
                            Modifier.size(30.dp),
                        )
                        Spacer(Modifier.width(9.dp))
                        Text(feedback.message, color = if (feedback.accepted) Color(0xFF4A2D20) else PremierUi.Red, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                }''',
'Move feedback',
)

replace_once(
'''                    Surface(modifier = Modifier.clickable(onClick = onForfeit), shape = RoundedCornerShape(12.dp), color = PremierUi.RedSoft) {
                        Row(Modifier.padding(horizontal = 9.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Flag, null, tint = PremierUi.Red, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(pt(language, "PES ET", "SURRENDER"), color = PremierUi.Red, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }''',
'''                    PurchasedButton(
                        text = pt(language, "PES ET", "SURRENDER"),
                        onClick = onForfeit,
                        modifier = Modifier.width(92.dp).height(48.dp),
                        style = PurchasedButtonStyle.DANGER,
                    )''',
'Forfeit control',
)

replace_once(
'''                    Surface(shape = RoundedCornerShape(13.dp), color = PremierUi.Ice) {
                        Text("$myScore  —  $rivalScore", Modifier.padding(horizontal = 13.dp, vertical = 6.dp), color = PremierUi.OceanDeep, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }''',
'''                    PurchasedPanel(
                        asset = PurchasedUiAsset.PANEL_SMALL,
                        contentPadding = PaddingValues(horizontal = 13.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("$myScore  —  $rivalScore", color = Color(0xFF4A2D20), fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }''',
'Score center',
)

replace_once(
'''                        Surface(
                            modifier = Modifier.clickable(onClick = onQuickChat),
                            shape = RoundedCornerShape(12.dp),
                            color = PremierUi.Ice,
                            border = BorderStroke(1.dp, PremierUi.Border),
                        ) {
                            Row(Modifier.padding(horizontal = 9.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.ChatBubbleOutline, pt(language, "Sohbet", "Chat"), tint = PremierUi.Ocean, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(pt(language, "SOHBET", "CHAT"), color = PremierUi.OceanDeep, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        }''',
'''                        PurchasedButton(
                            text = pt(language, "SOHBET", "CHAT"),
                            onClick = onQuickChat,
                            modifier = Modifier.width(96.dp).height(48.dp),
                            style = PurchasedButtonStyle.SECONDARY,
                            leadingAsset = PurchasedUiAsset.ICON_CHAT,
                        )''',
'Chat control',
)

replace_once(
'''                Surface(shape = CircleShape, color = Color.Transparent) {
                    Box(Modifier.size(60.dp).background(Brush.radialGradient(listOf(timerStart, if (danger) PremierUi.Red else PremierUi.Ocean, timerEnd)), CircleShape), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(seconds.toString().padStart(2, '0'), color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black)
                            Text("SEC", color = Color.White.copy(alpha = .75f), fontSize = 6.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }''',
'''                PurchasedPanel(
                    modifier = Modifier.size(66.dp),
                    asset = PurchasedUiAsset.PANEL_SMALL,
                    contentPadding = PaddingValues(6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(seconds.toString().padStart(2, '0'), color = if (danger) PremierUi.Red else Color(0xFF4A2D20), fontSize = 19.sp, fontWeight = FontWeight.Black)
                        Text("SEC", color = Color(0xFF765746), fontSize = 6.sp, fontWeight = FontWeight.Black)
                    }
                }''',
'Timer',
)

replace_once(
'''    Surface(shape = RoundedCornerShape(99.dp), color = if (myTurn) PremierUi.GreenSoft else PremierUi.GoldSoft, border = BorderStroke(1.dp, accent.copy(alpha = .25f))) {
        Text(
            when {
                !active -> pt(language, "MAÇ SENKRONİZE EDİLİYOR", "SYNCING MATCH")
                myTurn -> pt(language, "⚡ SENİN SIRAN", "⚡ YOUR TURN")
                else -> pt(language, "⏳ RAKİP DÜŞÜNÜYOR", "⏳ RIVAL IS THINKING")
            },
            Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
            color = accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = .4.sp,
        )
    }''',
'''    PurchasedPanel(
        asset = PurchasedUiAsset.PANEL_SMALL,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            when {
                !active -> pt(language, "MAÇ SENKRONİZE EDİLİYOR", "SYNCING MATCH")
                myTurn -> pt(language, "⚡ SENİN SIRAN", "⚡ YOUR TURN")
                else -> pt(language, "⏳ RAKİP DÜŞÜNÜYOR", "⏳ RIVAL IS THINKING")
            },
            color = accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = .4.sp,
        )
    }''',
'Turn badge',
)

replace_once(
'''    Box(
        Modifier.size(size).shadow(16.dp, RoundedCornerShape(26.dp)).clip(RoundedCornerShape(26.dp))
            .background(Brush.radialGradient(listOf(PremierUi.Sky, PremierUi.Ocean, PremierUi.OceanDeep))),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.matchParentSize().background(Color.White.copy(alpha = glow * .13f)))''',
'''    Box(
        Modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        PurchasedAsset(PurchasedUiAsset.PANEL_SMALL, Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(Color.White.copy(alpha = glow * .05f)))''',
'Target card',
)

replace_once(
'''        Surface(shape = CircleShape, color = if (won) PremierUi.GreenSoft else PremierUi.RedSoft) {
            Icon(if (won) Icons.Rounded.EmojiEvents else Icons.Rounded.SportsEsports, null, tint = if (won) PremierUi.Green else PremierUi.Red, modifier = Modifier.padding(22.dp).size(48.dp))
        }''',
'''        PurchasedPanel(
            modifier = Modifier.size(104.dp),
            asset = PurchasedUiAsset.REWARD_PANEL,
            contentPadding = PaddingValues(18.dp),
            contentAlignment = Alignment.Center,
        ) {
            PurchasedAsset(if (won) PurchasedUiAsset.ICON_TROPHY else PurchasedUiAsset.ICON_GAMES, Modifier.size(66.dp))
        }''',
'Result emblem',
)

replace_once(
'''        Button(onClick = onAction, colors = ButtonDefaults.buttonColors(containerColor = PremierUi.Ocean)) { Text(action, fontWeight = FontWeight.Black) }''',
'''        PurchasedButton(
            text = action,
            onClick = onAction,
            modifier = Modifier.widthIn(min = 180.dp),
            style = PurchasedButtonStyle.SECONDARY,
            leadingAsset = PurchasedUiAsset.NAV_HOME,
        )''',
'Centered fallback action',
)

required = [
    'backend.submitPremierWord(active.id, candidate)',
    'backend.requestRematch(active.id)',
    'backend.sendChat(active.id, message)',
    'backend.botTakeTurn(active.id)',
    'backend.claimTurnTimeout(active.id)',
    'EmbeddedWordKeyboard(',
    'PurchasedGameBackdrop(Modifier.matchParentSize())',
    'PurchasedUiAsset.ICON_SWORDS',
    'PurchasedUiAsset.ICON_CHAT',
]
for token in required:
    if token not in s:
        raise SystemExit('Behavior/UI contract missing after patch: ' + token)
if s == before:
    raise SystemExit('No Premier changes produced')
BRANCH_SOURCE.write_text(s)

ts = TEST_SOURCE.read_text()
old_test = 'assertTrue(screen.contains("Text(pt(language, \\\"SOHBET\\\", \\\"CHAT\\\")"))'
new_test = 'assertTrue(screen.contains("text = pt(language, \\\"SOHBET\\\", \\\"CHAT\\\")"))'
if old_test not in ts:
    raise SystemExit('Premier chat regression assertion missing')
TEST_SOURCE.write_text(ts.replace(old_test, new_test, 1))
