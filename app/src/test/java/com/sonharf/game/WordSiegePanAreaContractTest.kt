package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegePanAreaContractTest {
    @Test fun boardIsLargeBoundedTwoDimensionalAndCentered() {
        val pan = projectFile("app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt").readText()
        val experience = projectFile("app/src/main/java/com/sonharf/game/WordSiegeExperience.kt").readText()

        assertTrue(experience.contains("WordSiegePanMatch("))
        assertTrue(pan.contains("PanSiegeCellSize = 52.dp"))
        assertTrue(pan.contains("detectDragGestures"))
        assertTrue(pan.contains("translationX = pan.x"))
        assertTrue(pan.contains("translationY = pan.y"))
        assertTrue(pan.contains("candidate.x.coerceIn(viewport.width - boardPx, 0f)"))
        assertTrue(pan.contains("candidate.y.coerceIn(viewport.height - boardPx, 0f)"))
        assertTrue(pan.contains("pan = centerOn(40)"))
        assertTrue(pan.contains("CenterFocusStrong"))
        assertTrue(pan.contains("Modifier.fillMaxWidth().weight(1f)"))
        assertFalse(pan.contains("LazyColumn("))
        assertFalse(pan.contains("verticalScroll("))
    }

    @Test fun boardKeepsReadableOwnershipAndTapPlacementContract() {
        val pan = projectFile("app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt").readText()
        val experience = projectFile("app/src/main/java/com/sonharf/game/WordSiegeExperience.kt").readText()

        assertTrue(pan.contains("PanSiegeMine = Color(0xFF35C878)"))
        assertTrue(pan.contains("PanSiegeRival = Color(0xFFFF5F57)"))
        assertTrue(pan.contains("fontSize = 21.sp"))
        assertTrue(pan.contains("fontSize = 10.sp"))
        assertTrue(pan.contains("color = Color.Black"))
        assertTrue(pan.contains(".clickable(enabled = enabled && (cell.letter == null || pending), onClick = onClick)"))
        assertTrue(experience.contains("if (placements.containsKey(boardIndex))"))
        assertTrue(experience.contains("game.board.getOrNull(boardIndex)?.letter == null"))
        assertTrue(experience.contains("0xFF35C878"))
        assertTrue(experience.contains("0xFFFF5F57"))
    }

    @Test fun areaPointsAreServerCalculatedAsTwoPointsPerGainedCubeAndTransferredInNetScore() {
        val backend = projectFile("app/src/main/java/com/sonharf/game/data/WordSiegeBackend.kt").readText()
        val baseMigration = projectFile("supabase/migrations/20260901060000_word_siege_area_score_v1.sql").readText()
        val finalMigration = projectFile("supabase/migrations/20260902090000_word_siege_final_transfer_v2.sql").readText()

        assertTrue(backend.contains("player_one_area_score"))
        assertTrue(backend.contains("neutral_captured"))
        assertTrue(backend.contains("opponent_captured"))
        assertTrue(backend.contains("area_score"))
        assertTrue(backend.contains("total_score"))

        // Preserve the original transactional/ownership pipeline.
        assertTrue(baseMigration.contains("before_owner = 0 and after_owner = p_owner"))
        assertTrue(baseMigration.contains("before_owner not in (0, p_owner) and after_owner = p_owner"))
        assertTrue(baseMigration.contains("player_one_area_score = player_one_area_score +"))
        assertTrue(baseMigration.contains("player_two_area_score = player_two_area_score +"))
        assertTrue(baseMigration.contains("player_one_area = v_one_area"))
        assertTrue(baseMigration.contains("player_two_area = v_two_area"))

        // Final rule: every gained cube is worth two transferred points.
        assertTrue(finalMigration.contains("(neutral_count + opponent_count) * 2"))
        assertTrue(finalMigration.contains("r.player_one_word_score + r.player_one_area_score - r.player_two_area_score"))
        assertTrue(finalMigration.contains("r.player_two_word_score + r.player_two_area_score - r.player_one_area_score"))
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
