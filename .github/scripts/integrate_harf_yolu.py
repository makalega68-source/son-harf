from pathlib import Path

path = Path("app/src/main/java/com/sonharf/game/MonsterExperienceApp.kt")
text = path.read_text(encoding="utf-8")

replacements = [
    (
        "private enum class MonsterDestination { HOME, GAME, WORD_SIEGE, LEAGUE, COMPETITION, SOCIAL, STYLE, PROFILE, TASKS, VIP, SETTINGS, PROFILE_DETAILS, ACCOUNT, DAILY_CHALLENGE }",
        "private enum class MonsterDestination { HOME, GAME, WORD_SIEGE, LETTER_LADDER, LEAGUE, COMPETITION, SOCIAL, STYLE, PROFILE, TASKS, VIP, SETTINGS, PROFILE_DETAILS, ACCOUNT, DAILY_CHALLENGE }",
    ),
    (
        "val isGameplay = destination in setOf(MonsterDestination.GAME, MonsterDestination.WORD_SIEGE, MonsterDestination.DAILY_CHALLENGE)",
        "val isGameplay = destination in setOf(MonsterDestination.GAME, MonsterDestination.WORD_SIEGE, MonsterDestination.LETTER_LADDER, MonsterDestination.DAILY_CHALLENGE)",
    ),
    (
        "                    { destination = MonsterDestination.GAME },\n                    { destination = MonsterDestination.WORD_SIEGE },\n                    { destination = MonsterDestination.LEAGUE },",
        "                    { destination = MonsterDestination.GAME },\n                    { destination = MonsterDestination.WORD_SIEGE },\n                    { destination = MonsterDestination.LETTER_LADDER },\n                    { destination = MonsterDestination.LEAGUE },",
    ),
    (
        "                MonsterDestination.GAME -> OnlineGameScreenV6()\n                MonsterDestination.WORD_SIEGE -> WordSiegeExperienceScreen { destination = MonsterDestination.HOME }\n                MonsterDestination.LEAGUE -> LeaderboardExperienceScreen { destination = MonsterDestination.HOME }",
        "                MonsterDestination.GAME -> OnlineGameScreenV6()\n                MonsterDestination.WORD_SIEGE -> WordSiegeExperienceScreen { destination = MonsterDestination.HOME }\n                MonsterDestination.LETTER_LADDER -> LetterLadderGameScreen { destination = MonsterDestination.HOME }\n                MonsterDestination.LEAGUE -> LeaderboardExperienceScreen { destination = MonsterDestination.HOME }",
    ),
    (
        "    onPlay: () -> Unit,\n    onSiege: () -> Unit,\n    onLeague: () -> Unit,",
        "    onPlay: () -> Unit,\n    onSiege: () -> Unit,\n    onLetterLadder: () -> Unit,\n    onLeague: () -> Unit,",
    ),
    (
        "            Text(sh(\"ARENANI SEÇ\", \"CHOOSE YOUR ARENA\"), color = MonsterUi.Text, fontWeight = FontWeight.Black, fontSize = 14.sp)\n            Spacer(Modifier.height(7.dp))\n            MonsterSiegeQuickCard(Modifier.fillMaxWidth(), onSiege)\n            Spacer(Modifier.height(8.dp))\n            MonsterLeagueCard(stats.rating, onLeague)",
        "            Text(sh(\"OYUNUNU SEÇ\", \"CHOOSE YOUR GAME\"), color = MonsterUi.Text, fontWeight = FontWeight.Black, fontSize = 14.sp)\n            Spacer(Modifier.height(7.dp))\n            MonsterSiegeQuickCard(Modifier.fillMaxWidth(), onSiege)\n            Spacer(Modifier.height(8.dp))\n            MonsterLetterLadderQuickCard(Modifier.fillMaxWidth(), onLetterLadder)\n            Spacer(Modifier.height(8.dp))\n            MonsterLeagueCard(stats.rating, onLeague)",
    ),
]

for old, new in replacements:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one routing anchor, found {count}: {old[:90]!r}")
    text = text.replace(old, new, 1)

path.write_text(text, encoding="utf-8")
