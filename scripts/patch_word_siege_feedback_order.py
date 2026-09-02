from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def patch(path, replacements):
    p = ROOT / path
    text = p.read_text()
    for label, old, new in replacements:
        if old not in text:
            raise SystemExit(f"missing {label} in {path}")
        text = text.replace(old, new, 1)
    p.write_text(text)

patch("app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt", [
    ("practice visual owner state",
     "    var displayedBotScore by remember { mutableIntStateOf(botTargetScore) }\n\n    LaunchedEffect(playerTargetScore, botTargetScore) {\n",
     "    var displayedBotScore by remember { mutableIntStateOf(botTargetScore) }\n    var displayedOwner by remember { mutableIntStateOf(state.currentOwner) }\n\n    LaunchedEffect(playerTargetScore, botTargetScore, state.currentOwner) {\n"),
    ("practice owner after score animation",
     "            delay(28)\n        }\n    }\n",
     "            delay(28)\n        }\n        displayedOwner = state.currentOwner\n    }\n"),
    ("practice bot waits score animation",
     "        if (state.status != \"playing\" || state.currentOwner != 2) return@LaunchedEffect\n",
     "        if (state.status != \"playing\" || state.currentOwner != 2) return@LaunchedEffect\n        if (displayedPlayerScore != playerTargetScore || displayedBotScore != botTargetScore || displayedOwner != 2) return@LaunchedEffect\n"),
    ("practice bot effect keys",
     "    LaunchedEffect(state.currentOwner, state.moveCount, state.status) {\n",
     "    LaunchedEffect(state.currentOwner, state.moveCount, state.status, displayedPlayerScore, displayedBotScore, displayedOwner) {\n"),
    ("practice cards visual turn",
     "WordSiegePracticeScoreCard(sh(\"SEN\", \"YOU\"), displayedPlayerScore, state.playerArea, MainUi.Green, active = state.currentOwner == 1, modifier = Modifier.weight(1f))\n                    WordSiegePracticeScoreCard(sh(\"BOT\", \"BOT\"), displayedBotScore, state.botArea, MainUi.Red, active = state.currentOwner == 2, modifier = Modifier.weight(1f))",
     "WordSiegePracticeScoreCard(sh(\"SEN\", \"YOU\"), displayedPlayerScore, state.playerArea, MainUi.Green, active = displayedOwner == 1, modifier = Modifier.weight(1f))\n                    WordSiegePracticeScoreCard(sh(\"BOT\", \"BOT\"), displayedBotScore, state.botArea, MainUi.Red, active = displayedOwner == 2, modifier = Modifier.weight(1f))"),
    ("practice banner background visual turn",
     "                    color = if (state.currentOwner == 1) SiegeBlueSoft else SiegePurpleSoft,\n",
     "                    color = if (displayedOwner == 1) SiegeBlueSoft else SiegePurpleSoft,\n"),
    ("practice banner border visual turn",
     "                    border = BorderStroke(1.dp, if (state.currentOwner == 1) MainUi.Blue.copy(alpha = .25f) else SiegePurple.copy(alpha = .25f)),\n",
     "                    border = BorderStroke(1.dp, if (displayedOwner == 1) MainUi.Green.copy(alpha = .25f) else MainUi.Red.copy(alpha = .25f)),\n"),
    ("practice icon visual turn",
     "else Icon(if (state.currentOwner == 1) Icons.Rounded.TouchApp else Icons.Rounded.SmartToy, null, tint = if (state.currentOwner == 1) MainUi.Blue else SiegePurple, modifier = Modifier.size(17.dp))",
     "else Icon(if (displayedOwner == 1) Icons.Rounded.TouchApp else Icons.Rounded.SmartToy, null, tint = if (displayedOwner == 1) MainUi.Green else MainUi.Red, modifier = Modifier.size(17.dp))"),
    ("practice text visual turn",
     "                                state.currentOwner == 1 -> sh(\"SIRA SENDE • Harf seç, tahtaya bırak, OYNA\", \"YOUR TURN • Select tile, place it, PLAY\")\n",
     "                                displayedOwner == 1 -> sh(\"SIRA SENDE • Harf seç, tahtaya bırak, OYNA\", \"YOUR TURN • Select tile, place it, PLAY\")\n"),
])

patch("app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt", [
    ("pan visual player state",
     "    var displayedRivalScore by remember(game.id) { mutableIntStateOf(rivalTargetScore) }\n\n    LaunchedEffect(myTargetScore, rivalTargetScore) {\n",
     "    var displayedRivalScore by remember(game.id) { mutableIntStateOf(rivalTargetScore) }\n    var displayedCurrentPlayerId by remember(game.id) { mutableStateOf(game.currentPlayerId) }\n    val visualMyTurn = game.status == \"playing\" && displayedCurrentPlayerId == me\n\n    LaunchedEffect(myTargetScore, rivalTargetScore, game.currentPlayerId) {\n"),
    ("pan visual turn after scores",
     "            delay(28)\n        }\n    }\n\n    Column(\n",
     "            delay(28)\n        }\n        displayedCurrentPlayerId = game.currentPlayerId\n    }\n\n    Column(\n"),
    ("pan header visual my turn",
     "                        if (myTurn) sh(\"SIRA SENDE\", \"YOUR TURN\") else sh(\"RAKİPTE\", \"RIVAL'S TURN\")\n",
     "                        if (visualMyTurn) sh(\"SIRA SENDE\", \"YOUR TURN\") else sh(\"RAKİPTE\", \"RIVAL'S TURN\")\n"),
    ("pan header color visual my turn",
     "                    color = if (myTurn) MainUi.Blue else SiegePurple,\n",
     "                    color = if (visualMyTurn) MainUi.Green else MainUi.Red,\n"),
    ("pan my card visual active",
     "                active = game.currentPlayerId == me,\n",
     "                active = displayedCurrentPlayerId == me,\n"),
    ("pan rival card visual active",
     "                active = game.currentPlayerId == opponentId,\n",
     "                active = displayedCurrentPlayerId == opponentId,\n"),
])

print("Word Siege feedback order patched")
