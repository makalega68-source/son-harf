from pathlib import Path

ROOT = Path('.')
GAME = ROOT / 'app/src/main/java/com/sonharf/game'
TEST = ROOT / 'app/src/test/java/com/sonharf/game'


def replace_once(path, old, new, label):
    path = Path(path)
    text = path.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected exactly 1 match in {path}, found {count}')
    path.write_text(text.replace(old, new, 1))


# Preserve the established Kelime Kusatmasi winner contract: normal matches are decided
# by current territory control. The v34 work changes only early-finish cadence, not victory rules.
replace_once(
    GAME / 'WordSiegePracticeEngine.kt',
    '''    private fun finish(state: WordSiegePracticeState, reason: String, forcedWinner: Int? = null): WordSiegePracticeState {
        val playerScore = totalScore(state, 1)
        val botScore = totalScore(state, 2)
        val winner = forcedWinner ?: when {
            playerScore > botScore -> 1
            botScore > playerScore -> 2
            else -> null
        }
        return state.copy(status = "finished", winnerOwner = winner, lastAction = reason)
    }
''',
    '''    private fun finish(state: WordSiegePracticeState, reason: String, forcedWinner: Int? = null): WordSiegePracticeState {
        val winner = forcedWinner ?: when {
            state.playerArea > state.botArea -> 1
            state.botArea > state.playerArea -> 2
            else -> null
        }
        return state.copy(status = "finished", winnerOwner = winner, lastAction = reason)
    }
''',
    'restore territory victory contract',
)

# Visual contracts now intentionally lock the more vivid ownership/bonus palette requested by user.
replace_once(
    TEST / 'AssetIntegrationContractTest.kt',
    '''        listOf(
            "0xFF8EA697", "0xFFA8D5B5", "0xFFE4AEAA", "0xFF3F7C53", "0xFF9B4D4A",
            "0xFFDCEAF2", "0xFFDCEAF2", "0xFFEAE2F0", "0xFFEAE2F0", "0xFFE7DDBB", "0xFFEAD59B",
        ).forEach { assertTrue(online.contains(it)) }
''',
    '''        listOf(
            "0xFF8EA697", "0xFF8BD8AA", "0xFFEDA09B", "0xFF2D7B4C", "0xFFA33F3B",
            "0xFFC8E7F4", "0xFF9CCFEA", "0xFFDEC8EF", "0xFFC5A3E2", "0xFFF1B95E", "0xFFF4C95F",
        ).forEach { assertTrue(online.contains(it)) }
''',
    'online vivid palette contract',
)
replace_once(TEST / 'AssetIntegrationContractTest.kt', 'PracticeSiegeMine = Color(0xFFA8D5B5)', 'PracticeSiegeMine = Color(0xFF8BD8AA)', 'asset practice green')
replace_once(TEST / 'AssetIntegrationContractTest.kt', 'PracticeSiegeRival = Color(0xFFE4AEAA)', 'PracticeSiegeRival = Color(0xFFEDA09B)', 'asset practice red')

for path_name in ['KelimeTahtiFinalBrandingContractTest.kt', 'WordSiegeEntryModesContractTest.kt', 'WordSiegePanAreaContractTest.kt']:
    path = TEST / path_name
    replace_once(path, 'PanSiegeMine = Color(0xFFA8D5B5)', 'PanSiegeMine = Color(0xFF8BD8AA)', f'{path_name} green')
    replace_once(path, 'PanSiegeRival = Color(0xFFE4AEAA)', 'PanSiegeRival = Color(0xFFEDA09B)', f'{path_name} red')

replace_once(TEST / 'KelimeTahtiFinalBrandingContractTest.kt', 'PanSiegeMineBorder = Color(0xFF3F7C53)', 'PanSiegeMineBorder = Color(0xFF2D7B4C)', 'branding green border')
replace_once(TEST / 'KelimeTahtiFinalBrandingContractTest.kt', 'PanSiegeRivalBorder = Color(0xFF9B4D4A)', 'PanSiegeRivalBorder = Color(0xFFA33F3B)', 'branding red border')

path = TEST / 'KelimeTahtiDifferentiationContractTest.kt'
replace_once(path, 'PracticePlayerAccent = Color(0xFF567A64)', 'PracticePlayerAccent = Color(0xFF3C8E62)', 'practice player accent')
replace_once(path, 'PracticeRivalAccent = Color(0xFF9B4D4A)', 'PracticeRivalAccent = Color(0xFFA84642)', 'practice rival accent')
replace_once(path, 'PracticeSiegeMine = Color(0xFFA8D5B5)', 'PracticeSiegeMine = Color(0xFF8BD8AA)', 'practice board green')
replace_once(path, 'PracticeSiegeRival = Color(0xFFE4AEAA)', 'PracticeSiegeRival = Color(0xFFEDA09B)', 'practice board red')

replace_once(TEST / 'WordSiegeOnlineBoardVisualContractTest.kt', 'PanSiegeBonusLabel = Color(0xFF68716D)', 'PanSiegeBonusLabel = Color(0xFF33445A)', 'online bonus label contrast')
replace_once(TEST / 'WordSiegeReadabilityContractTest.kt', 'PanSiegeBonusLabel = Color(0xFF68716D)', 'PanSiegeBonusLabel = Color(0xFF33445A)', 'readability online label')
replace_once(TEST / 'WordSiegeReadabilityContractTest.kt', 'PracticeSiegeBonusLabel = Color(0xFF68716D)', 'PracticeSiegeBonusLabel = Color(0xFF33445A)', 'readability practice label')

path = TEST / 'WordSiegePracticeVisualContractTest.kt'
replace_once(path, 'PracticeSiegeMine = Color(0xFFA8D5B5)', 'PracticeSiegeMine = Color(0xFF8BD8AA)', 'practice visual green')
replace_once(path, 'PracticeSiegeRival = Color(0xFFE4AEAA)', 'PracticeSiegeRival = Color(0xFFEDA09B)', 'practice visual red')

# Seven-tile rack is now a named contract rather than repeated magic numbers.
replace_once(TEST / 'WordSiegePracticeBoardContractTest.kt', 'playerRack = shuffled.take(7)', 'playerRack = shuffled.take(WordSiegeBoardSpec.RackSize)', 'player rack named size')
replace_once(TEST / 'WordSiegePracticeBoardContractTest.kt', 'botRack = shuffled.drop(7).take(7)', 'botRack = shuffled.drop(WordSiegeBoardSpec.RackSize).take(WordSiegeBoardSpec.RackSize)', 'bot rack named size')
replace_once(
    TEST / 'WordSiegePracticeBoardContractTest.kt',
    'assertTrue(spec.contains("canonicalBag"))',
    'assertTrue(spec.contains("canonicalBag"))\n        assertTrue(spec.contains("const val RackSize = 7"))',
    'explicit seven tile test',
)

# BOT status is deliberately integrated into the secondary line so it cannot clip down to a stray B.
replace_once(TEST / 'WordSiegeProfileRegressionTest.kt', 'assertTrue(scoreCard.contains("Text(\\"BOT\\""))', 'assertTrue(scoreCard.contains("• BOT"))', 'integrated bot label contract')

print('Remaining v34 contracts aligned without changing the territory-victory rule.')
