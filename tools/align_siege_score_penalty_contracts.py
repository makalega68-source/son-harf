from pathlib import Path

ROOT = Path('.')

def replace_once(path, old, new):
    p = ROOT / path
    t = p.read_text()
    if old not in t:
        raise SystemExit(f'missing pattern in {path}: {old[:120]!r}')
    p.write_text(t.replace(old, new, 1))

# Help remains user-invoked from ?, but explains the newly requested +2/-1 territory ledger.
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegePracticeGuidance.kt',
    '"Harf bonusu yalnız harfi, kelime bonusu tüm kelimeyi çarpar. +25 ödülü hamlene eklenir. Skordaki bölge puanı mevcut maç kuralıdır: sahip olduğun hücre başına 2 puan; kelime puanı kalıcıdır."',
    '"Harf bonusu yalnız harfi, kelime bonusu tüm kelimeyi çarpar. +25 ödülü hamlene eklenir. Kazandığın her küp +2 bölge puanı getirir; rakipten aldığın her küp rakibin bölge puanından -1 düşürür. Kelime puanı kalıcıdır."',
)
replace_once(
    'app/src/main/java/com/sonharf/game/WordSiegePracticeGuidance.kt',
    '"Letter bonuses multiply one letter; word bonuses multiply the word. +25 adds to the move. Existing matches also award 2 points per owned cell; word points are permanent."',
    '"Letter bonuses multiply one letter; word bonuses multiply the word. +25 adds to the move. Every cube you win adds +2 territory points; every rival cube you take removes -1 territory point from the rival. Word points are permanent."',
)

replace_once(
    'app/src/test/java/com/sonharf/game/KelimeTahtiFinalBrandingContractTest.kt',
    '''        assertTrue(online.contains("WordSiegeFinalRules.currentTerritoryScore"))\n        assertTrue(online.contains("WordSiegeFinalRules.cubeTransfer"))''',
    '''        assertTrue(online.contains("WordSiegeFinalRules.scoreWithTerritoryLedger"))\n        assertTrue(online.contains("playerOneAreaScore"))\n        assertTrue(online.contains("playerTwoAreaScore"))''',
)

replace_once(
    'app/src/test/java/com/sonharf/game/WordSiegeOnlineBoardVisualContractTest.kt',
    '        assertTrue(source.contains("fontWeight = FontWeight.Light"))',
    '        assertTrue(source.contains("fontWeight = if (overview) FontWeight.SemiBold else FontWeight.Medium"))',
)

replace_once(
    'app/src/test/java/com/sonharf/game/WordSiegeReadabilityContractTest.kt',
    '            assertTrue(board.contains("fontWeight = FontWeight.Light"))',
    '            assertTrue(board.contains("fontWeight = if (overview) FontWeight.SemiBold else FontWeight.Medium"))',
)

replace_once(
    'app/src/test/java/com/sonharf/game/WordSiegePracticeUxAndBotRegressionTest.kt',
    '        assertTrue(screen.contains("WordSiegePracticeTutorialPrefs.isCompleted(context)"))',
    '        assertFalse(screen.contains("WordSiegePracticeTutorialPrefs.isCompleted(context)"))',
)
replace_once(
    'app/src/test/java/com/sonharf/game/WordSiegePracticeUxAndBotRegressionTest.kt',
    '        assertTrue(guidance.contains("sahip olduğun hücre başına 2 puan"))',
    '        assertTrue(guidance.contains("Kazandığın her küp +2 bölge puanı"))',
)

replace_once(
    'app/src/test/java/com/sonharf/game/WordSiegeStableRegressionTest.kt',
    '''        assertTrue(rules.contains("CUBE_TRANSFER_POINTS: Int = 2"))\n        assertTrue(rules.contains("wordScore + earnedCubePoints.coerceAtLeast(0)"))\n        assertTrue(rules.contains("ownedCubes -= move.opponentCaptured"))''',
    '''        assertTrue(rules.contains("CUBE_TRANSFER_POINTS: Int = 2"))\n        assertTrue(rules.contains("OPPONENT_CAPTURE_LOSS_POINTS: Int = 1"))\n        assertTrue(rules.contains("wordScore + earnedCubePoints.coerceAtLeast(0)"))\n        assertTrue(rules.contains("territoryScore -= opponentCaptureLoss(move.opponentCaptured)"))''',
)

p = ROOT / 'app/src/test/java/com/sonharf/game/WordSiegePanAreaContractTest.kt'
t = p.read_text()
t = t.replace(
    'fun areaPointsAreServerCalculatedAsTwoPointsPerGainedCubeAndCurrentTerritoryDrivesVisibleScore()',
    'fun areaPointsUseAuthoritativePlusTwoGainAndMinusOneRivalLossLedger()',
)
t = t.replace(
    '        val currentTerritoryMigration = projectFile("supabase/migrations/20260909090000_word_siege_current_territory_score_v5.sql").readText()\n',
    '        val currentTerritoryMigration = projectFile("supabase/migrations/20260909090000_word_siege_current_territory_score_v5.sql").readText()\n        val captureLossMigration = projectFile("supabase/migrations/20260923131500_word_siege_capture_loss_penalty_v8.sql").readText()\n',
)
t = t.replace(
    '        assertTrue(pan.contains("WordSiegeFinalRules.currentTerritoryScore"))\n',
    '        assertTrue(pan.contains("WordSiegeFinalRules.scoreWithTerritoryLedger"))\n        assertTrue(pan.contains("playerOneAreaScore"))\n        assertTrue(pan.contains("playerTwoAreaScore"))\n',
)
t = t.replace(
    '''        // Historical migrations remain immutable for provenance. v5 overrides final winner scoring\n        // with permanent word points + cubes currently owned * 2.\n        assertTrue(finalMigration.contains("(neutral_count + opponent_count) * 2"))\n        assertTrue(currentTerritoryMigration.contains("r.player_one_word_score + (r.player_one_area * 2)"))\n        assertTrue(currentTerritoryMigration.contains("r.player_two_word_score + (r.player_two_area * 2)"))\n        assertFalse(currentTerritoryMigration.contains("player_one_area_score -"))\n        assertFalse(currentTerritoryMigration.contains("player_two_area_score -"))''',
    '''        // Historical migrations remain immutable for provenance. v8 changes only the\n        // territory score ledger: +2 per newly won cube, -1 per rival-owned cube lost.\n        assertTrue(finalMigration.contains("(neutral_count + opponent_count) * 2"))\n        assertTrue(currentTerritoryMigration.contains("r.player_one_word_score + (r.player_one_area * 2)"))\n        assertTrue(currentTerritoryMigration.contains("r.player_two_word_score + (r.player_two_area * 2)"))\n        assertTrue(captureLossMigration.contains("-v_opponent_captured"))\n        assertTrue(captureLossMigration.contains("greatest(0, player_one_area_score"))\n        assertTrue(captureLossMigration.contains("greatest(0, player_two_area_score"))''',
)
p.write_text(t)
