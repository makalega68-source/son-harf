from pathlib import Path

path = Path("app/src/main/java/com/sonharf/game/PremierWordDuelScreen.kt")
text = path.read_text(encoding="utf-8")
old = "verticalAlignment = Alignment.Stretch,"
new = "verticalAlignment = Alignment.CenterVertically,"
if old not in text:
    raise SystemExit("Expected Son Harf alignment marker was not found")
path.write_text(text.replace(old, new, 1), encoding="utf-8")
print("Fixed Son Harf Row alignment")
