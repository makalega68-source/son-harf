from pathlib import Path

path = Path("app/src/main/java/com/sonharf/game/MonsterExperienceApp.kt")
text = path.read_text(encoding="utf-8")

replacements = [
    (
        "val isGameplay = destination in setOf(MonsterDestination.GAME, MonsterDestination.WORD_SIEGE, MonsterDestination.LETTER_LADDER, MonsterDestination.DAILY_CHALLENGE)",
        "val isGameplay = destination in setOf(MonsterDestination.GAME, MonsterDestination.WORD_SIEGE, MonsterDestination.DAILY_CHALLENGE, MonsterDestination.LETTER_LADDER)",
    ),
    (
        'Text(sh("OYUNUNU SEÇ", "CHOOSE YOUR GAME"), color = MonsterUi.Text, fontWeight = FontWeight.Black, fontSize = 14.sp)',
        'Text(sh("ARENANI SEÇ", "CHOOSE YOUR ARENA"), color = MonsterUi.Text, fontWeight = FontWeight.Black, fontSize = 14.sp)',
    ),
]

for old, new in replacements:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Expected one anchor, found {count}: {old}")
    text = text.replace(old, new, 1)

path.write_text(text, encoding="utf-8")
