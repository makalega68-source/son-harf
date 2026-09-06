from pathlib import Path

root = Path(__file__).resolve().parents[1]
source = root / "app/src/main/java/com/sonharf/game/OnlineGameScreenV6.kt"
text = source.read_text(encoding="utf-8")
old = "latest.playerId == me"
new = "latest.playerId == backend.currentUserId()"
if text.count(old) != 1:
    raise SystemExit(f"expected one observer player-id scope, found {text.count(old)}")
source.write_text(text.replace(old, new, 1), encoding="utf-8")

screenshot_test = root / "app/src/test/java/com/sonharf/game/ScreenshotRegressionFixContractTest.kt"
t = screenshot_test.read_text(encoding="utf-8")
if t.count('s.contains("latest.playerId == me")') != 1:
    raise SystemExit("expected one contract assertion for observer player id")
screenshot_test.write_text(t.replace('s.contains("latest.playerId == me")', 's.contains("latest.playerId == backend.currentUserId()")', 1), encoding="utf-8")

active_test = root / "app/src/test/java/com/sonharf/game/ActiveScreenRegressionContractTest.kt"
a = active_test.read_text(encoding="utf-8")
old_viewport = 'assertTrue(board.contains("WordSiegeBoardViewportMode.FIT"))'
new_viewport = 'assertTrue(board.contains("WordSiegeBoardViewportMode.CLOSE"))'
if a.count(old_viewport) != 1:
    raise SystemExit(f"expected one legacy viewport contract, found {a.count(old_viewport)}")
active_test.write_text(a.replace(old_viewport, new_viewport, 1), encoding="utf-8")

bonus_test = root / "app/src/test/java/com/sonharf/game/BonusFlowReliabilityContractTest.kt"
b = bonus_test.read_text(encoding="utf-8")
old_answer = 'assertTrue(arena.contains("ASIL CEVAP"))'
new_answer = 'assertTrue(arena.contains("DOĞRU CEVAP"))'
if b.count(old_answer) != 1:
    raise SystemExit(f"expected one legacy answer-label contract, found {b.count(old_answer)}")
bonus_test.write_text(b.replace(old_answer, new_answer, 1), encoding="utf-8")

print("observer scope and updated UX contracts fixed")
