package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegePanAreaContractTest {
    @Test fun onlineAndPracticeUseTheSameLarge15x15BoardSurface() {
        val pan = projectFile("app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt").readText()
        val practiceBoard = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt").readText()
        val viewport = projectFile("app/src/main/java/com/sonharf/game/WordSiegeBoardViewport.kt").readText()
        val experience = projectFile("app/src/main/java/com/sonharf/game/WordSiegeExperience.kt").readText()

        assertTrue(experience.contains("WordSiegePanMatch("))
        assertTrue(pan.contains("WordSiegePracticeBoard("))
        assertTrue(pan.contains("Modifier.fillMaxWidth().weight(1f)"))
        assertTrue(practiceBoard.contains("PracticeSiegeCellSize = 52.dp"))
        assertTrue(practiceBoard.contains("WordSiegeBoardSpec.Size"))
        assertTrue(practiceBoard.contains("detectTransformGestures"))
        assertTrue(practiceBoard.contains("combinedClickable"))
        assertTrue(practiceBoard.contains("onDoubleClick = ::toggleMode"))
        assertTrue(practiceBoard.contains("WordSiegeBoardViewportMode.CLOSE"))
        assertTrue(viewport.contains("WordSiegeBoardViewportMode.FIT"))
        assertTrue(viewport.contains("wordSiegeFitScale"))
        assertTrue(practiceBoard.contains("translationX = transform.pan.x"))
        assertTrue(practiceBoard.contains("translationY = transform.pan.y"))
        assertTrue(practiceBoard.contains("scaleX = transform.scale"))
        assertTrue(practiceBoard.contains("scaleY = transform.scale"))
        assertTrue(practiceBoard.contains("clampWordSiegeBoardPan"))
        assertTrue(viewport.contains("value.coerceIn(viewportPx - renderedPx, 0f)"))
        assertTrue(practiceBoard.contains("clipToBounds"))
        assertTrue(practiceBoard.contains("onGloballyPositioned"))
        assertFalse(pan.contains("LazyColumn("))
        assertFalse(pan.contains("verticalScroll("))
    }

    @Test fun serverBridgeLocksCanonical15x15GeometryBeforeZones() {
        val bridge = v5BridgeMigrations()

        assertTrue(bridge.contains("generate_series(0, 224)"))
        assertTrue(bridge.contains("x not between 0 and 224"))
        assertTrue(bridge.contains("x / 15 <> v_anchor / 15"))
        assertTrue(bridge.contains("x % 15 <> v_anchor % 15"))
        assertTrue(bridge.contains("case when p_horizontal then 1 else 15 end"))
        assertTrue(bridge.contains("not (112 = any(v_indices))"))
        assertTrue(bridge.contains("private.word_siege_prevalidate_move_v2"))
        assertTrue(bridge.contains("private.word_siege_word_allowed_v1"))
        assertTrue(bridge.contains("private.word_siege_score_word_v1"))
        assertTrue(bridge.indexOf("drop constraint") < bridge.indexOf("set board = ("))
    }

    @Test fun zonesFortressesAndPermanentWordScoreReplaceCellLedger() {
        val backend = projectFile("app/src/main/java/com/sonharf/game/data/WordSiegeBackend.kt").readText()
        val zoneRules = projectFile("app/src/main/java/com/sonharf/game/WordSiegeZoneRules.kt").readText()
        val migrations = v5ZoneMigrations()
        val practice = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeEngine.kt").readText()

        assertTrue(backend.contains("player_one_conquest_meter"))
        assertTrue(backend.contains("player_one_onslaught_active"))
        assertTrue(backend.contains("zones_flipped"))
        assertTrue(backend.contains("raw_word_score"))

        assertTrue(zoneRules.contains("const val ZoneSize = 3"))
        assertTrue(zoneRules.contains("const val NormalZonePoints = 2"))
        assertTrue(zoneRules.contains("const val FortressZonePoints = 4"))
        assertTrue(zoneRules.contains("zoneScore"))
        assertTrue(zoneRules.contains("claimZones"))

        assertTrue(practice.contains("playerWordScore = state.playerWordScore +"))
        assertTrue(practice.contains("playerAreaScore = playerAreaScore"))
        assertTrue(practice.contains("WordSiegeZoneRules.zoneScore"))
        assertFalse(practice.contains("WordSiegeFinalRules.cubeTransfer(move.capturedCells)"))

        assertTrue(migrations.contains("private.word_siege_claim_zones_v5"))
        assertTrue(migrations.contains("private.word_siege_zone_score_v5"))
        assertTrue(migrations.contains("create or replace function private.word_siege_sync_area_scores_v1()"))
        assertTrue(migrations.contains("v_final_score := v_raw_score * case when v_before_onslaught then 2 else 1 end"))
        assertTrue(migrations.contains("player_one_word_score = case"))
        assertTrue(migrations.contains("player_one_area_score = v_one_zone_score"))
        assertTrue(migrations.contains("player_two_area_score = v_two_zone_score"))
    }

    @Test fun duplicateProtectionAndCanonicalValidationPipelineStayIntact() {
        val legacyValidatedCore = projectFile("supabase/migrations/20260901060000_word_siege_area_score_v1.sql").readText()
        val zoneMigrations = v5ZoneMigrations()

        assertTrue(legacyValidatedCore.contains("word_siege_moves_game_move_number_uidx"))
        assertTrue(legacyValidatedCore.contains("request_fingerprint"))
        assertTrue(legacyValidatedCore.contains("private.word_siege_word_allowed_v1"))
        assertTrue(legacyValidatedCore.contains("private.word_siege_score_word_v1"))

        assertTrue(zoneMigrations.contains("rename to submit_word_siege_move_cell_core_v5"))
        assertTrue(zoneMigrations.contains("private.submit_word_siege_move_cell_core_v5(p_game_id, p_placements, p_horizontal)"))
        assertFalse(zoneMigrations.contains("create or replace function private.word_siege_word_allowed_v1"))
        assertFalse(zoneMigrations.contains("create or replace function private.word_siege_score_word_v1"))
    }

    private fun v5BridgeMigrations(): String = listOf(
        "supabase/migrations/20260907232000_word_siege_15x15_bridge_geometry_v5.sql",
        "supabase/migrations/20260907232100_word_siege_15x15_bridge_submit_v5.sql",
        "supabase/migrations/20260907232200_word_siege_15x15_bridge_prevalidate_v5.sql",
    ).joinToString("\n") { projectFile(it).readText() }

    private fun v5ZoneMigrations(): String = listOf(
        "supabase/migrations/20260907233000_word_siege_zone_schema_v5.sql",
        "supabase/migrations/20260907233100_word_siege_zone_helpers_v5.sql",
        "supabase/migrations/20260907233150_word_siege_zone_score_trigger_v5.sql",
        "supabase/migrations/20260907233200_word_siege_zone_claim_v5.sql",
        "supabase/migrations/20260907233300_word_siege_zone_normalize_v5.sql",
        "supabase/migrations/20260907233400_word_siege_zone_submit_v5.sql",
        "supabase/migrations/20260907233500_word_siege_zone_finish_timer_v5.sql",
    ).joinToString("\n") { projectFile(it).readText() }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
