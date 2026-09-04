#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def patch(path,pairs):
 p=ROOT/path; t=p.read_text()
 for a,b in pairs:
  if a in t: t=t.replace(a,b)
  elif b not in t: raise RuntimeError(f'{path}: missing {a}')
 p.write_text(t)
patch('app/src/test/java/com/sonharf/game/WordSiegePracticeBoardContractTest.kt',[
 ('assertTrue(board.contains("detectDragGestures"))','assertTrue(board.contains("detectTransformGestures"))\n        assertTrue(board.contains("WORD_SIEGE_DEFAULT_CLOSE_SCALE"))\n        assertTrue(board.contains("WORD_SIEGE_MIN_CLOSE_SCALE"))\n        assertTrue(board.contains("WORD_SIEGE_MAX_CLOSE_SCALE"))')])
patch('app/src/test/java/com/sonharf/game/WordSiegePracticeVisualContractTest.kt',[
 ('assertTrue(text.contains("detectDragGestures"))','assertTrue(text.contains("detectTransformGestures"))')])
patch('app/src/test/java/com/sonharf/game/WordSiegeFinalRulesTest.kt',[
 ('assertTrue(practice.contains("delay(28)"))','assertTrue(practice.contains("animateIntAsState"))\n        assertTrue(!practice.contains("while (displayedPlayerScore != playerTargetScore"))')])
print('practice test contracts updated')
