from pathlib import Path

root = Path(__file__).resolve().parents[1]
source = root / "app/src/main/java/com/sonharf/game/OnlineGameScreenV6.kt"
text = source.read_text(encoding="utf-8")
old = "latest.playerId == me"
new = "latest.playerId == backend.currentUserId()"
if text.count(old) != 1:
    raise SystemExit(f"expected one observer player-id scope, found {text.count(old)}")
source.write_text(text.replace(old, new, 1), encoding="utf-8")

test = root / "app/src/test/java/com/sonharf/game/ScreenshotRegressionFixContractTest.kt"
t = test.read_text(encoding="utf-8")
if t.count('s.contains("latest.playerId == me")') != 1:
    raise SystemExit("expected one contract assertion for observer player id")
test.write_text(t.replace('s.contains("latest.playerId == me")', 's.contains("latest.playerId == backend.currentUserId()")', 1), encoding="utf-8")

print("observer player-id scope fixed")
