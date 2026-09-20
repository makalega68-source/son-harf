from pathlib import Path

path = Path("app/src/main/java/com/sonharf/game/WordSiegeExperience.kt")
text = path.read_text(encoding="utf-8")
old = "runCatching { backend.getWordSiegeGame(gameId) }"
new = "runCatching { backend.refreshWordSiegeGame(gameId) }"
count = text.count(old)
if count != 1:
    raise SystemExit(f"expected exactly one classic game poll, found {count}")
path.write_text(text.replace(old, new), encoding="utf-8")
print("classic Word Siege polling now uses server timeout refresh")
