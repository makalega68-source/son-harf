from pathlib import Path


def replace(path, old, new, count=1):
    p = Path(path)
    s = p.read_text()
    if old not in s:
        raise SystemExit(f"missing pattern in {path}: {old[:160]!r}")
    p.write_text(s.replace(old, new, count))

# Central game-only help route policy.
p = "app/src/main/java/com/sonharf/game/MainExperienceApp.kt"
replace(p,
    "fun SonHarfMainApp(onSignedOut: () -> Unit) {",
    "fun SonHarfMainApp(\n    onSignedOut: () -> Unit,\n    onGameHelpKindChanged: (FirstPlayerTutorialKind?) -> Unit = {},\n) {")
replace(p,
    "    LaunchedEffect(destination) {\n        if (destination != MainDestination.GAME) {",
    "    LaunchedEffect(destination) {\n        onGameHelpKindChanged(\n            when (destination) {\n                MainDestination.GAME -> FirstPlayerTutorialKind.SON_HARF\n                MainDestination.WORD_SIEGE -> FirstPlayerTutorialKind.WORD_SIEGE\n                else -> null\n            },\n        )\n        if (destination != MainDestination.GAME) {")

p = "app/src/main/java/com/sonharf/game/StableV1App.kt"
replace(p,
    "    var showHelpChooser by remember { mutableStateOf(false) }",
    "    var showHelpChooser by remember { mutableStateOf(false) }\n    var gameHelpKind by remember { mutableStateOf<FirstPlayerTutorialKind?>(null) }")
old = '''            SonHarfMainApp(onSignedOut = {
                tutorial = null
                showHelpChooser = false
                showAdminPanel = false
                adminAuthorized = false
                automaticTutorial = false
                authenticated = false
            })

            if (tutorial == null) {
                FloatingActionButton(
                    onClick = { showHelpChooser = true },
                    modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 6.dp, end = 68.dp).size(42.dp),
                    containerColor = MainUi.Surface,
                    contentColor = MainUi.Blue,
                ) {
                    Icon(Icons.Rounded.HelpOutline, contentDescription = sh("Nasıl Oynanır?", "How to Play?"), modifier = Modifier.size(22.dp))
                }

                if (adminAuthorized) {'''
new = '''            SonHarfMainApp(
                onSignedOut = {
                    tutorial = null
                    showHelpChooser = false
                    showAdminPanel = false
                    adminAuthorized = false
                    automaticTutorial = false
                    authenticated = false
                },
                onGameHelpKindChanged = { gameHelpKind = it },
            )

            if (tutorial == null && gameHelpKind != null) {
                FloatingActionButton(
                    onClick = {
                        automaticTutorial = false
                        tutorial = gameHelpKind
                    },
                    modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 6.dp, end = 68.dp).size(42.dp),
                    containerColor = MainUi.Surface,
                    contentColor = MainUi.Blue,
                ) {
                    Icon(Icons.Rounded.HelpOutline, contentDescription = sh("Nasıl Oynanır?", "How to Play?"), modifier = Modifier.size(22.dp))
                }
            }

            if (tutorial == null) {
                if (adminAuthorized) {'''
replace(p, old, new)

# Word Siege game language selection is separate from UI locale.
p = "app/src/main/java/com/sonharf/game/WordSiegeExperience.kt"
replace(p,
    "    var showDurationPicker by remember { mutableStateOf(false) }\n    var practiceActive by remember { mutableStateOf(false) }",
    "    var showDurationPicker by remember { mutableStateOf(false) }\n    var showLanguagePicker by remember { mutableStateOf(false) }\n    var languagePickerForPractice by remember { mutableStateOf(false) }\n    var selectedMatchLanguage by remember { mutableStateOf(\"tr\") }\n    var practiceLanguage by remember { mutableStateOf(\"tr\") }\n    var practiceActive by remember { mutableStateOf(false) }")
replace(p,
    "        WordSiegePracticeScreen(onExit = { practiceActive = false })",
    "        WordSiegePracticeScreen(language = practiceLanguage, onExit = { practiceActive = false })")
replace(p,
    "                onNewGame = { showDurationPicker = true },\n                onPractice = { practiceActive = true },",
    "                onNewGame = { languagePickerForPractice = false; showLanguagePicker = true },\n                onPractice = { languagePickerForPractice = true; showLanguagePicker = true },")
replace(p,
    "    if (showDurationPicker) {",
    '''    if (showLanguagePicker) {
        AlertDialog(
            onDismissRequest = { showLanguagePicker = false },
            title = { Text(sh("Oyun dilini seç", "Choose game language"), fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(sh("Arayüz dilinden bağımsızdır ve maç başladıktan sonra değişmez.", "Independent from UI language and locked when the match starts."), color = MainUi.Muted, fontSize = 12.sp)
                    listOf("tr" to "🇹🇷 TÜRKÇE", "en" to "🇬🇧 ENGLISH").forEach { (language, label) ->
                        Button(
                            onClick = {
                                showLanguagePicker = false
                                if (languagePickerForPractice) {
                                    practiceLanguage = language
                                    practiceActive = true
                                } else {
                                    selectedMatchLanguage = language
                                    showDurationPicker = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (language == "tr") MainUi.Blue else SiegePurple),
                        ) { Text(label, fontWeight = FontWeight.Black) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showLanguagePicker = false }) { Text(sh("VAZGEÇ", "CANCEL")) } },
        )
    }

    if (showDurationPicker) {''')
replace(p,
    'backend.findOrCreateWordSiegeGame(if (SonHarfUiState.isEnglish) "en" else "tr", hours)',
    'backend.findOrCreateWordSiegeGame(selectedMatchLanguage, hours)')

# Practice state carries language, language-specific bag/scores, and same server dictionary.
p = "app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt"
replace(p,
    'internal fun WordSiegePracticeScreen(onExit: () -> Unit) {',
    'internal fun WordSiegePracticeScreen(language: String = "tr", onExit: () -> Unit) {')
replace(p,
    '    var state by remember { mutableStateOf(WordSiegePracticeEngine.newGame()) }',
    '    var state by remember(language) { mutableStateOf(WordSiegePracticeEngine.newGame(language)) }')
replace(p, '        state = WordSiegePracticeEngine.newGame()', '        state = WordSiegePracticeEngine.newGame(language)')
replace(p, 'validateWordSiegeDictionaryWord(word, "tr")', 'validateWordSiegeDictionaryWord(word, state.language)')

p = "app/src/main/java/com/sonharf/game/WordSiegePracticeEngine.kt"
replace(p,
    'internal data class WordSiegePracticeState(\n    val board: List<WordSiegeCellDto>,',
    'internal data class WordSiegePracticeState(\n    val board: List<WordSiegeCellDto>,\n    val language: String = "tr",')
replace(p,
    '    fun newGame(): WordSiegePracticeState {\n        val tiles = turkishTileDistribution.toList().shuffled()',
    '    fun newGame(language: String = "tr"): WordSiegePracticeState {\n        val normalizedLanguage = if (language.lowercase() == "en") "en" else "tr"\n        val tiles = (if (normalizedLanguage == "en") englishTileDistribution else turkishTileDistribution).toList().shuffled()')
replace(p,
    '            playerRack = tiles.take(7).joinToString(""),',
    '            language = normalizedLanguage,\n            playerRack = tiles.take(7).joinToString(""),')
old = '''    fun tileValue(letter: Char): Int = when (letter.uppercaseChar()) {
        'A', 'E', 'İ', 'K', 'L', 'N', 'R', 'T' -> 1
        'I', 'M', 'O', 'S', 'U' -> 2
        'B', 'D', 'Ü', 'Y' -> 3
        'C', 'Ç', 'Ş', 'Z' -> 4
        'G', 'H', 'P' -> 5
        'F', 'Ö', 'V' -> 7
        'Ğ' -> 8
        'J' -> 10
        else -> 1
    }'''
new = '''    fun tileValue(letter: Char, language: String = "tr"): Int {
        val upper = letter.uppercaseChar()
        if (language == "en") return when (upper) {
            'A','E','I','L','N','O','R','S','T','U' -> 1
            'D','G' -> 2
            'B','C','M','P' -> 3
            'F','H','V','W','Y' -> 4
            'K' -> 5
            'J','X' -> 8
            'Q','Z' -> 10
            else -> 1
        }
        return when (upper) {
            'A', 'E', 'İ', 'K', 'L', 'N', 'R', 'T' -> 1
            'I', 'M', 'O', 'S', 'U' -> 2
            'B', 'D', 'Ü', 'Y' -> 3
            'C', 'Ç', 'Ş', 'Z' -> 4
            'G', 'H', 'P' -> 5
            'F', 'Ö', 'V' -> 7
            'Ğ' -> 8
            'J' -> 10
            else -> 1
        }
    }'''
replace(p, old, new)
replace(p, '            score += scoreWord(state.board, placements, rack, cells)', '            score += scoreWord(state, placements, rack, cells)')
replace(p,
    '    private fun scoreWord(board: List<WordSiegeCellDto>, placements: Map<Int, Int>, rack: String, cells: List<Int>): Int {',
    '    private fun scoreWord(state: WordSiegePracticeState, placements: Map<Int, Int>, rack: String, cells: List<Int>): Int {\n        val board = state.board')
replace(p, '            var value = letter?.let(::tileValue) ?: 0', '            var value = letter?.let { tileValue(it, state.language) } ?: 0')
replace(p,
    '    private const val turkishTileDistribution =',
    '    private const val englishTileDistribution =\n        "EEEEEEEEEEEEAAAAAAAAAIIIIIIIIIOOOOOOOONNNNNNRRRRRRTTTTTTLLLLSSSSUUUUDDDDGGGBBCCMMPPFFHHVVWWYYKJXQZ"\n\n    private const val turkishTileDistribution =')

# Bot candidate generation logic is untouched; only locale/dictionary language follows state.
p = "app/src/main/java/com/sonharf/game/WordSiegeBotPlanner.kt"
replace(p,
    '        val rack = state.botRack.uppercase(trLocale)',
    '        val locale = if (state.language == "en") Locale.ENGLISH else trLocale\n        val rack = state.botRack.uppercase(locale)', count=2)
replace(p, 'validateWordSiegeDictionaryWords(it, "tr")', 'validateWordSiegeDictionaryWords(it, state.language)')
replace(p, 'fetchWordSiegeBotLexicon(rack, "tr", 1000)', 'fetchWordSiegeBotLexicon(rack, state.language, 1000)')
replace(p, 'fetchWordSiegeBotLexicon(rack + anchor, "tr", 220)', 'fetchWordSiegeBotLexicon(rack + anchor, state.language, 220)')
replace(p, 'fetchWordSiegeBotLexicon(rack + distinctBoardAlphabet, "tr", 500)', 'fetchWordSiegeBotLexicon(rack + distinctBoardAlphabet, state.language, 500)')
replace(p, '.map { it.uppercase(trLocale) }', '.map { it.uppercase(if (state.language == "en") Locale.ENGLISH else trLocale) }')

Path('app/src/test/java/com/sonharf/game/DictionaryLanguageHelpContractTest.kt').write_text(r'''package com.sonharf.game

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DictionaryLanguageHelpContractTest {
    private fun source(path: String) = File(path).readText()

    @Test fun wordSiegeLanguageSelectorIsIndependentFromUiLocale() {
        val s = source("src/main/java/com/sonharf/game/WordSiegeExperience.kt")
        assertTrue(s.contains("🇹🇷 TÜRKÇE"))
        assertTrue(s.contains("🇬🇧 ENGLISH"))
        assertTrue(s.contains("findOrCreateWordSiegeGame(selectedMatchLanguage, hours)"))
        assertFalse(s.contains("findOrCreateWordSiegeGame(if (SonHarfUiState.isEnglish)"))
    }

    @Test fun practiceBagAndBotFollowMatchLanguage() {
        val engine = source("src/main/java/com/sonharf/game/WordSiegePracticeEngine.kt")
        val planner = source("src/main/java/com/sonharf/game/WordSiegeBotPlanner.kt")
        assertTrue(engine.contains("englishTileDistribution"))
        assertTrue(engine.contains("language = normalizedLanguage"))
        assertTrue(engine.contains("tileValue(it, state.language)"))
        assertTrue(planner.contains("validateWordSiegeDictionaryWords(it, state.language)"))
        assertTrue(planner.contains("fetchWordSiegeBotLexicon(rack, state.language"))
    }

    @Test fun helpVisibilityUsesGameRouteAllowlist() {
        val app = source("src/main/java/com/sonharf/game/MainExperienceApp.kt")
        val shell = source("src/main/java/com/sonharf/game/StableV1App.kt")
        assertTrue(app.contains("MainDestination.GAME -> FirstPlayerTutorialKind.SON_HARF"))
        assertTrue(app.contains("MainDestination.WORD_SIEGE -> FirstPlayerTutorialKind.WORD_SIEGE"))
        assertTrue(app.contains("else -> null"))
        assertTrue(shell.contains("tutorial == null && gameHelpKind != null"))
        assertTrue(shell.contains("Alignment.TopEnd"))
        assertTrue(shell.contains("statusBarsPadding()"))
    }

    @Test fun dictionaryMigrationIsFailClosedAndGameSpecific() {
        val sql = source("../supabase/migrations/20260901074000_dictionary_validation_language_v1.sql")
        assertTrue(sql.contains("validate_dictionary_word_v1"))
        assertTrue(sql.contains("abbreviation_not_allowed"))
        assertTrue(sql.contains("proper_noun_not_allowed"))
        assertTrue(sql.contains("legacy_synthetic_two_letter_fallback"))
        assertTrue(sql.contains("right(v_check.normalized_word, 1) = 'ğ'"))
        assertTrue(sql.contains("word_siege_word_allowed_v1"))
    }
}
''')
