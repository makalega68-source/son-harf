from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected 1 match, got {count}: {old[:140]!r}")
    p.write_text(text.replace(old, new, 1))


gdd = "app/src/test/java/com/sonharf/game/KelimeKusatmasiMasterGddV3ContractTest.kt"
replace_once(
    gdd,
    '''        val shell = File("src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt").readText()
        val entries = File("src/main/java/com/sonharf/game/PremiumGameEntryScreens.kt").readText()
        val firstRun = File("src/main/java/com/sonharf/game/StableV1App.kt").readText()
        val localization = File("src/main/java/com/sonharf/game/SonHarfUiState.kt").readText()

        assertTrue(shell.contains("SIEGE_ENTRY, LAST_LETTER_ENTRY, LETTER_PATH_ENTRY"))
        assertTrue(entries.contains("PremiumSiegeEntryScreen"))
        assertTrue(entries.contains("PremiumLastLetterEntryScreen"))
        assertTrue(entries.contains("PremiumLetterPathEntryScreen"))
        assertTrue(entries.contains("LanguageOption(\\\"tr\\\""))
        assertTrue(entries.contains("LanguageOption(\\\"en\\\""))
        assertFalse(entries.contains("R.drawable"))''',
    '''        val shell = File("src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt").readText()
        val siege = File("src/main/java/com/sonharf/game/WordSiegeExperience.kt").readText()
        val duel = File("src/main/java/com/sonharf/game/PremierWordDuelScreen.kt").readText()
        val ladder = File("src/main/java/com/sonharf/game/LetterLadderGame.kt").readText()
        val firstRun = File("src/main/java/com/sonharf/game/StableV1App.kt").readText()
        val localization = File("src/main/java/com/sonharf/game/SonHarfUiState.kt").readText()

        assertFalse(shell.contains("_ENTRY"))
        assertTrue(shell.contains("openGame(PremiumDestination.SIEGE)"))
        assertTrue(shell.contains("openGame(PremiumDestination.LAST_LETTER)"))
        assertTrue(shell.contains("openGame(PremiumDestination.LETTER_PATH)"))
        assertTrue(siege.contains("onLanguageChange = { SonHarfUiState.language = it }"))
        assertTrue(duel.contains("PremierLanguageSwitch(language, onLanguage)"))
        assertTrue(ladder.contains("SonHarfUiState.language = code"))
        assertFalse(File("src/main/java/com/sonharf/game/PremiumGameEntryScreens.kt").exists())''',
)
replace_once(
    gdd,
    '''        assertTrue(shell.contains("fun leaveGame(target: PremiumDestination = PremiumDestination.HOME)"))
        assertTrue(shell.contains("PremiumDestination.LAST_LETTER_ENTRY"))
        assertTrue(shell.contains("PremiumDestination.SIEGE_ENTRY"))
        assertTrue(shell.contains("PremiumDestination.LETTER_PATH_ENTRY"))
        assertTrue(shell.contains("leaveGame(PremiumDestination.SIEGE_ENTRY)"))
        assertTrue(shell.contains("leaveGame(PremiumDestination.LETTER_PATH_ENTRY)"))''',
    '''        assertTrue(shell.contains("fun leaveGame(target: PremiumDestination = PremiumDestination.HOME)"))
        assertFalse(shell.contains("PremiumDestination.LAST_LETTER_ENTRY"))
        assertFalse(shell.contains("PremiumDestination.SIEGE_ENTRY"))
        assertFalse(shell.contains("PremiumDestination.LETTER_PATH_ENTRY"))
        assertTrue(shell.contains("PremiumDestination.SIEGE -> WordSiegeExperienceScreen"))
        assertTrue(shell.contains("PremiumDestination.LETTER_PATH -> LetterLadderGameScreen"))
        assertTrue(shell.contains("leaveGame()"))''',
)

primary = "app/src/test/java/com/sonharf/game/KelimeKusatmasiPrimaryProductContractTest.kt"
replace_once(primary, '        val entries = File("src/main/java/com/sonharf/game/PremiumGameEntryScreens.kt").readText()\n', '')
replace_once(
    primary,
    '''        assertTrue(entries.contains("PremiumSiegeEntryScreen"))
        assertTrue(entries.contains("PremiumLastLetterEntryScreen"))
        assertTrue(entries.contains("PremiumLetterPathEntryScreen"))
        assertFalse(entries.contains("R.drawable"))''',
    '''        assertFalse(File("src/main/java/com/sonharf/game/PremiumGameEntryScreens.kt").exists())
        assertTrue(shell.contains("onPrimary = { openGame(PremiumDestination.SIEGE) }"))
        assertTrue(shell.contains("onLastLetter = { openGame(PremiumDestination.LAST_LETTER) }"))
        assertTrue(shell.contains("onLetterPath = { openGame(PremiumDestination.LETTER_PATH) }"))''',
)
replace_once(
    primary,
    '''        assertTrue(shell.contains("PremiumDestination.SIEGE_ENTRY"))
        assertTrue(shell.contains("PremiumDestination.LAST_LETTER_ENTRY"))
        assertTrue(shell.contains("PremiumDestination.LETTER_PATH_ENTRY"))''',
    '''        assertTrue(shell.contains("PremiumDestination.SIEGE"))
        assertTrue(shell.contains("PremiumDestination.LAST_LETTER"))
        assertTrue(shell.contains("PremiumDestination.LETTER_PATH"))
        assertFalse(shell.contains("SIEGE_ENTRY"))
        assertFalse(shell.contains("LAST_LETTER_ENTRY"))
        assertFalse(shell.contains("LETTER_PATH_ENTRY"))''',
)
replace_once(
    primary,
    '''        assertFalse(topLevel.contains("PremiumDestination.SIEGE_ENTRY"))
        assertFalse(topLevel.contains("PremiumDestination.LAST_LETTER_ENTRY"))
        assertFalse(topLevel.contains("PremiumDestination.LETTER_PATH_ENTRY"))''',
    '''        assertFalse(topLevel.contains("PremiumDestination.SIEGE"))
        assertFalse(topLevel.contains("PremiumDestination.LAST_LETTER"))
        assertFalse(topLevel.contains("PremiumDestination.LETTER_PATH"))''',
)

visual = "app/src/test/java/com/sonharf/game/KelimeTahtiVisualPlacementContractTest.kt"
replace_once(visual, '        val entries = File("src/main/java/com/sonharf/game/PremiumGameEntryScreens.kt").readText()\n', '')
replace_once(
    visual,
    '''        assertTrue(screen.contains("TAKTİK ALAN SAVAŞI"))
        assertTrue(screen.contains("Icons.Rounded.GridView"))
        assertFalse(entries.contains("painterResource"))
        assertFalse(entries.contains("R.drawable"))''',
    '''        assertTrue(screen.contains("ALAN SAVAŞI"))
        assertTrue(screen.contains("Kelime kur • alan ele geçir"))
        assertTrue(screen.contains("Icons.Rounded.GridView"))
        assertTrue(screen.contains("onLanguageChange = { SonHarfUiState.language = it }"))
        assertFalse(File("src/main/java/com/sonharf/game/PremiumGameEntryScreens.kt").exists())''',
)

premier = "app/src/test/java/com/sonharf/game/PremierDuelUxRegressionTest.kt"
replace_once(premier, '        assertTrue(screen.contains("\\\"15→13→11 sn\\\""))', '        assertTrue(screen.contains("\\\"15→13→11\\\""))')

rounds = "app/src/test/java/com/sonharf/game/SonHarfThreeRoundContractTest.kt"
replace_once(rounds, '        assertTrue(screen.contains("15→13→11 sn"))', '        assertTrue(screen.contains("15→13→11"))')
