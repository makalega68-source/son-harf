package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegePanAreaContractTest {
    @Test fun boardIsLargeBoundedTwoDimensionalAndCentered() {
        val pan = projectFile("app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt").readText()
        val viewport = projectFile("app/src/main/java/com/sonharf/game/WordSiegeBoardViewport.kt").readText()
        val experience = projectFile("app/src/main/java/com/sonharf/game/WordSiegeExperience.kt").readText()

        assertTrue(experience.contains("WordSiegePanMatch("))
        assertTrue(pan.contains("PanSiegeCellSize = 52.dp"))
        assertTrue(pan.contains("WordSiegeBoardSpec.Size"))
        assertTrue(pan.contains("detectDragGestures"))
        assertTrue(pan.contains("combinedClickable"))
        assertTrue(pan.contains("onDoubleClick = { toggleViewport(index) }"))
        assertTrue(pan.contains("wordSiegeOnlineCloseScale"))
        assertTrue(pan.contains("closeScale = closeScale"))
        assertTrue(pan.contains("WordSiegeBoardViewportMode.CLOSE"))
        assertTrue(viewport.contains("WordSiegeBoardViewportMode.FIT"))
        assertTrue(viewport.contains("WORD_SIEGE_ONLINE_ZOOM_FACTOR = 2.00f"))
        assertTrue(viewport.contains("wordSiegeFitScale"))
        assertTrue(pan.contains("translationX = transform.pan.x"))
        assertTrue(pan.contains("translationY = transform.pan.y"))
        assertTrue(pan.contains("scaleX = transform.scale"))
        assertTrue(pan.contains("scaleY = transform.scale"))
        assertTrue(pan.contains("clampWordSiegeBoardPan"))
        assertTrue(viewport.contains("value.coerceIn(viewportPx - renderedPx, 0f)"))
        assertTrue(pan.contains("centerCloseOn(WordSiegeBoardSpec.CenterIndex)"))
        assertTrue(pan.contains("clipToBounds"))
        assertTrue(pan.contains("onGloballyPositioned"))
        assertTrue(pan.contains("CenterFocusStrong"))
        assertTrue(pan.contains("Modifier.fillMaxWidth().weight(1f)"))
        assertFalse(pan.contains("detectTapGestures"))
        assertFalse(pan.contains("Çift dokun:"))
        assertFalse(pan.contains("LazyColumn("))
        assertFalse(pan.contains("verticalScroll("))
    }

    @Test fun boardKeepsReadableOwnershipAndTapPlacementContract() {
        val pan = projectFile("app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt").readText()
        val experience = projectFile("app/src/main/java/com/sonharf/game/WordSiegeExperience.kt").readText()
        val accessibility = projectFile("app/src/main/java/com/sonharf/game/WordSiegeBoardAccessibility.kt").readText()

        assertTrue(pan.contains("PanSiegeMine = WordSiegeWalnutIvory.mine"))
        assertTrue(pan.contains("PanSiegeRival = WordSiegeWalnutIvory.rival"))
        assertTrue(pan.contains("fontSize = 22.sp"))
        assertTrue(pan.contains("WordSiegeBoardAccessibility.BoardLetterPoint"))
        assertTrue(pan.contains("WordSiegeBoardAccessibility.BoardBonus"))
        assertTrue(accessibility.contains("BoardLetterPoint: TextUnit = 14.sp"))
        assertTrue(accessibility.contains("BoardBonus: TextUnit = 17.sp"))
        assertTrue(pan.contains("Color(0xFF4A3217)"))
        assertTrue(pan.contains("FontFamily.SansSerif"))
        assertTrue(pan.contains("Color(0xFFF2D680)"))
        assertTrue(pan.contains("val canPlace = enabled && (cell.letter == null || pending)"))
        assertTrue(pan.contains("WordSiegeBoardTapAction.PLACE"))
        assertTrue(pan.contains("WordSiegeBoardTapAction.TOGGLE_VIEWPORT"))
        assertTrue(experience.contains("if (placements.containsKey(boardIndex))"))
        assertTrue(experience.contains("game.board.getOrNull(boardIndex)?.letter == null"))
        assertTrue(experience.contains("owner == myOwner -> WordSiegeWalnutIvory.mine"))
        assertTrue(experience.contains("else -> WordSiegeWalnutIvory.rival"))
    }

    @Test fun areaPointsUseAuthoritativePlusTwoGainAndMinusOneRivalLossLedger() {
        val backend = projectFile("app/src/main/java/com/sonharf/game/data/WordSiegeBackend.kt").readText()
        val pan = projectFile("app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt").readText()
        val baseMigration = projectFile("supabase/migrations/20260901060000_word_siege_area_score_v1.sql").readText()
        val finalMigration = projectFile("supabase/migrations/20260902090000_word_siege_final_transfer_v2.sql").readText()
        val currentTerritoryMigration = projectFile("supabase/migrations/20260909090000_word_siege_current_territory_score_v5.sql").readText()
        val captureLossMigration = projectFile("supabase/migrations/20260923131500_word_siege_capture_loss_penalty_v8.sql").readText()

        assertTrue(backend.contains("player_one_area_score"))
        assertTrue(backend.contains("neutral_captured"))
        assertTrue(backend.contains("opponent_captured"))
        assertTrue(backend.contains("area_score"))
        assertTrue(backend.contains("total_score"))
        assertTrue(pan.contains("WordSiegeFinalRules.scoreWithTerritoryLedger"))
        assertTrue(pan.contains("playerOneAreaScore"))
        assertTrue(pan.contains("playerTwoAreaScore"))
        assertTrue(pan.contains("val myAreaCount = panSiegeAreaCount(game, myOwner)"))

        // Preserve the original transactional/ownership pipeline.
        assertTrue(baseMigration.contains("before_owner = 0 and after_owner = p_owner"))
        assertTrue(baseMigration.contains("before_owner not in (0, p_owner) and after_owner = p_owner"))
        assertTrue(baseMigration.contains("player_one_area_score = player_one_area_score +"))
        assertTrue(baseMigration.contains("player_two_area_score = player_two_area_score +"))
        assertTrue(baseMigration.contains("player_one_area = v_one_area"))
        assertTrue(baseMigration.contains("player_two_area = v_two_area"))

        // Historical migrations remain immutable for provenance. v8 changes only the
        // territory score ledger: +2 per newly won cube, -1 per rival-owned cube lost.
        assertTrue(finalMigration.contains("(neutral_count + opponent_count) * 2"))
        assertTrue(currentTerritoryMigration.contains("r.player_one_word_score + (r.player_one_area * 2)"))
        assertTrue(currentTerritoryMigration.contains("r.player_two_word_score + (r.player_two_area * 2)"))
        assertTrue(captureLossMigration.contains("-v_opponent_captured"))
        assertTrue(captureLossMigration.contains("greatest(0, player_one_area_score"))
        assertTrue(captureLossMigration.contains("greatest(0, player_two_area_score"))
    }

    @Test fun duplicateProtectionAndExistingValidationPipelineStayIntact() {
        val migration = projectFile("supabase/migrations/20260901060000_word_siege_area_score_v1.sql").readText()

        assertTrue(migration.contains("word_siege_moves_game_move_number_uidx"))
        assertTrue(migration.contains("request_fingerprint"))
        assertTrue(migration.contains("r.move_count = v_existing_move_number"))
        assertTrue(migration.contains("r.last_action_player_id = v_uid"))
        assertTrue(migration.contains("private.word_siege_prepare_turn_v2"))
        assertTrue(migration.contains("private.word_siege_prevalidate_move_v2"))
        assertTrue(migration.contains("private.word_siege_word_allowed_v1"))
        assertTrue(migration.contains("private.word_siege_score_word_v1"))
        assertFalse(migration.contains("create or replace function private.word_siege_word_allowed_v1"))
        assertFalse(migration.contains("create or replace function private.word_siege_score_word_v1"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
