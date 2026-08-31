package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BilBakalimNumericEstimateContractTest {
    @Test
    fun activeBonusUiUsesNumericInputAndNoMultipleChoiceOptions() {
        val ui = projectFile("app/src/main/java/com/sonharf/game/LightDuelUi.kt").readText()
        val bonus = ui.substringAfter("private fun LightBonusCard(").substringBefore("private fun LightInputBar")
        assertTrue(bonus.contains("TAHMİNİNİ YAZ"))
        assertTrue(bonus.contains("KeyboardType.Number"))
        assertTrue(bonus.contains("TAHMİNİ KİLİTLE"))
        assertFalse(bonus.contains("question.optionA"))
        assertFalse(bonus.contains("question.optionB"))
        assertFalse(bonus.contains("question.optionC"))
        assertFalse(bonus.contains("question.optionD"))
    }

    @Test
    fun uiDisclosesOpponentOnlyInsideResolvedBranchAndShowsResultSemantics() {
        val ui = projectFile("app/src/main/java/com/sonharf/game/LightDuelUi.kt").readText()
        val bonus = ui.substringAfter("private fun LightBonusCard(").substringBefore("private fun LightInputBar")
        assertTrue(bonus.contains("!resolved -> null"))
        assertTrue(bonus.contains("DOĞRU CEVAP"))
        assertTrue(bonus.contains("CEVAP YOK"))
        assertTrue(bonus.contains("BERABERE • PUAN YOK"))
        assertTrue(bonus.contains("+10 PUAN"))
        assertTrue(bonus.contains("Sonuç 3 saniye sonra otomatik kapanır"))
        assertTrue(bonus.contains("won -> LGreen"))
        assertTrue(bonus.contains("else -> LRed"))
    }

    @Test
    fun backendContractIsTenSecondsNearestAnswerTenPointsAndIdempotent() {
        val sql = projectFile("supabase/migrations/20260831233000_bilbakalim_numeric_estimate_v2.sql").readText()
        assertTrue(sql.contains("interval '10 seconds'"))
        assertTrue(sql.contains("award int:=10"))
        assertTrue(sql.contains("host_dist:=abs(host_est-v_correct)"))
        assertTrue(sql.contains("guest_dist:=abs(guest_est-v_correct)"))
        assertTrue(sql.contains("side:='tie'"))
        assertTrue(sql.contains("side text:='none'"))
        assertTrue(sql.contains("on conflict(round_id,player_id) do nothing"))
        assertTrue(sql.contains("if q.resolved_at is not null then return r"))
        assertTrue(sql.contains("interval '3 seconds'"))
        val scoring = sql.substringAfter("-- tie and double no-answer deliberately award zero.").substringBefore("update public.trivia_rounds")
        assertFalse(scoring.contains("side='tie'"))
    }

    @Test
    fun serverPrivacyKeepsOpponentEstimateHiddenUntilResolved() {
        val sql = projectFile("supabase/migrations/20260831233000_bilbakalim_numeric_estimate_v2.sql").readText()
        assertTrue(sql.contains("player_id=auth.uid()"))
        assertTrue(sql.contains("q.resolved_at is not null"))
        assertTrue(sql.contains("revoke all on function public.resolve_bilbakalim_round_v1(uuid,boolean) from public,anon,authenticated"))
        assertFalse(sql.contains("grant execute on function public.resolve_bilbakalim_round_v1"))
    }

    @Test
    fun questionDtoExposesUnitButNotCorrectValue() {
        val backend = projectFile("app/src/main/java/com/sonharf/game/data/OnlineGameBackend.kt").readText()
        val questionDto = backend.substringAfter("data class TriviaQuestionDto(").substringBefore("data class TriviaRoundDto")
        assertTrue(questionDto.contains("answer_unit"))
        assertTrue(questionDto.contains("question_kind"))
        assertFalse(questionDto.contains("correct_value"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::isFile)
        assertNotNull("Project file missing: $path", file)
        return requireNotNull(file)
    }
}
