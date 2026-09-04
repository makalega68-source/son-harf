#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def patch(path, pairs):
    p = ROOT / path
    text = p.read_text(encoding='utf-8')
    for old, new in pairs:
        if old in text:
            text = text.replace(old, new)
        elif new not in text:
            raise RuntimeError(f'{path}: missing pattern: {old[:80]}')
    p.write_text(text, encoding='utf-8')

patch('app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt', [
    ('import androidx.compose.runtime.*\n', 'import androidx.compose.runtime.*\nimport androidx.compose.animation.core.animateIntAsState\nimport androidx.compose.animation.core.tween\n'),
    ('    var displayedPlayerScore by remember { mutableIntStateOf(playerTargetScore) }\n    var displayedBotScore by remember { mutableIntStateOf(botTargetScore) }\n    var displayedOwner by remember { mutableIntStateOf(state.currentOwner) }', '    val displayedPlayerScore by animateIntAsState(playerTargetScore, tween(220), label = "practicePlayerScore")\n    val displayedBotScore by animateIntAsState(botTargetScore, tween(220), label = "practiceBotScore")\n    val displayedOwner = state.currentOwner'),
    ('    LaunchedEffect(playerTargetScore, botTargetScore, state.currentOwner) {\n        while (displayedPlayerScore != playerTargetScore || displayedBotScore != botTargetScore) {\n            displayedPlayerScore += (playerTargetScore - displayedPlayerScore).coerceIn(-1, 1)\n            displayedBotScore += (botTargetScore - displayedBotScore).coerceIn(-1, 1)\n            delay(28)\n        }\n        displayedOwner = state.currentOwner\n    }\n\n', ''),
    ('        displayedPlayerScore,\n        displayedBotScore,\n        displayedOwner,\n', ''),
    ('        if (displayedPlayerScore != playerTargetScore || displayedBotScore != botTargetScore || displayedOwner != 2) return@LaunchedEffect\n', ''),
    ('color = MainUi.Blue, fontSize = 8.sp, fontWeight = FontWeight.Black', 'color = MainUi.Blue, fontSize = GameTypography.Metadata, fontWeight = FontWeight.Black'),
    ('                            fontSize = 9.sp,\n                            fontWeight = FontWeight.Black,', '                            fontSize = GameTypography.Action,\n                            fontWeight = FontWeight.Black,'),
    ('Text(readyFeedback.message, color = MainUi.Green, fontSize = 9.sp, fontWeight = FontWeight.Black)', 'Text(readyFeedback.message, color = MainUi.Green, fontSize = GameTypography.Metadata, fontWeight = FontWeight.Black)'),
    ('                                fontSize = 8.sp,\n', '                                fontSize = GameTypography.Metadata,\n'),
    ('Text(sh("Torba ${state.bag.length}", "Bag ${state.bag.length}"), color = MainUi.Muted, fontSize = 8.sp, maxLines = 1)', 'Text(sh("Torba ${state.bag.length}", "Bag ${state.bag.length}"), color = MainUi.Muted, fontSize = GameTypography.Metadata, maxLines = 1)'),
    ('Text(sh("GERİ AL", "UNDO"), fontSize = 8.sp, fontWeight = FontWeight.Black)', 'Text(sh("GERİ AL", "UNDO"), fontSize = GameTypography.Action, fontWeight = FontWeight.Black)'),
    ('Text(sh("KARIŞTIR", "SHUFFLE"), fontSize = 8.sp, fontWeight = FontWeight.Black)', 'Text(sh("KARIŞTIR", "SHUFFLE"), fontSize = GameTypography.Action, fontWeight = FontWeight.Black)'),
    ('Text(sh("PAS", "PASS"), fontSize = 9.sp, fontWeight = FontWeight.Black)', 'Text(sh("PAS", "PASS"), fontSize = GameTypography.Action, fontWeight = FontWeight.Black)'),
    ('Text(sh("DEĞİŞTİR", "EXCHANGE"), color = SiegePurple, fontSize = 8.sp, fontWeight = FontWeight.Black)', 'Text(sh("DEĞİŞTİR", "EXCHANGE"), color = SiegePurple, fontSize = GameTypography.Action, fontWeight = FontWeight.Black)'),
    ('Text(sh("OYNA", "PLAY"), fontSize = 10.sp, fontWeight = FontWeight.Black)', 'Text(sh("OYNA", "PLAY"), fontSize = GameTypography.Action, fontWeight = FontWeight.Black)'),
    ('size = if (compact) 30.dp else 34.dp,', 'size = 50.dp,'),
    ('fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis', 'fontSize = GameTypography.PlayerName, maxLines = 1, overflow = TextOverflow.Ellipsis'),
    ('fontSize = 6.sp, fontWeight = FontWeight.Black', 'fontSize = GameTypography.Metadata, fontWeight = FontWeight.Black'),
    ('fontSize = if (compact) 16.sp else 18.sp', 'fontSize = GameTypography.Score'),
    ('fontSize = 7.sp, maxLines = 1', 'fontSize = GameTypography.Metadata, maxLines = 1'),
])

patch('app/src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt', [
    ('import androidx.compose.foundation.gestures.detectDragGestures', 'import androidx.compose.foundation.gestures.detectTransformGestures'),
    ('import androidx.compose.runtime.*\n', 'import androidx.compose.runtime.*\nimport androidx.compose.animation.core.Animatable\nimport androidx.compose.animation.core.tween\n'),
    ('    var closePan by remember { mutableStateOf(Offset.Zero) }', '    var closePan by remember { mutableStateOf(Offset.Zero) }\n    var closeScale by remember { mutableFloatStateOf(WORD_SIEGE_DEFAULT_CLOSE_SCALE) }'),
    ('    val transform by remember(mode, viewport, boardPx, closePan) {', '    val transform by remember(mode, viewport, boardPx, closePan, closeScale) {'),
    ('                closePan = closePan,\n', '                closeScale = closeScale,\n                closePan = closePan,\n'),
    ('        1f,\n    )', '        closeScale,\n    )'),
    ('            cellSizePx = tilePx,\n        )', '            cellSizePx = tilePx,\n            scale = closeScale,\n        )'),
    ('        if (nextMode == WordSiegeBoardViewportMode.CLOSE) closePan = centerClose()', '        if (nextMode == WordSiegeBoardViewportMode.CLOSE) {\n            closeScale = WORD_SIEGE_DEFAULT_CLOSE_SCALE\n            closePan = centerClose()\n        }'),
    ('.pointerInput(mode, viewport, boardPx) {\n                    if (mode == WordSiegeBoardViewportMode.CLOSE) {\n                        detectDragGestures { change, dragAmount ->\n                            change.consume()\n                            closePan = clampClosePan(closePan + dragAmount)\n                        }\n                    }\n                }', '.pointerInput(mode, viewport, boardPx, closeScale) {\n                    if (mode == WordSiegeBoardViewportMode.CLOSE) {\n                        detectTransformGestures { centroid, pan, zoom, _ ->\n                            val oldScale = closeScale\n                            val newScale = (oldScale * zoom).coerceIn(WORD_SIEGE_MIN_CLOSE_SCALE, WORD_SIEGE_MAX_CLOSE_SCALE)\n                            val boardPoint = (centroid - closePan) / oldScale\n                            closeScale = newScale\n                            closePan = clampClosePan(centroid - boardPoint * newScale + pan)\n                        }\n                    }\n                }'),
    ('                    mode = WordSiegeBoardViewportMode.CLOSE\n                    closePan = centerClose()', '                    mode = WordSiegeBoardViewportMode.CLOSE\n                    closeScale = WORD_SIEGE_DEFAULT_CLOSE_SCALE\n                    closePan = centerClose()'),
])

print('practice screen fixes applied')
