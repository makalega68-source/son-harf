package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The player-feedback batch: theme, keyboard fit, frames, mascots, hints, PRO preview, store. */
class PlayerFeedbackBatchContractTest {
    private fun source(name: String) = File("src/main/java/com/sonharf/game/$name").readText()

    @Test fun blackThemeReachesSiegeAndSonHarfArenas() {
        assertTrue(source("WordSiegeGameUi.kt").contains("val Background: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFF101419) else if (SonHarfCosmetics.walnutTheme) Color(0xFFEFE3CC) else Color(0xFFE6ECF2)"))
        val duel = source("PremierWordDuelScreen.kt")
        assertTrue(duel.contains("val BackgroundTop: Color get() = if (SonHarfCosmetics.darkArenaTheme)"))
        assertTrue(duel.contains("val Tile: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFF1F2025)"))
        // Matchmaking text follows the theme, so its background must too.
        assertTrue(duel.contains(".background(Brush.verticalGradient(listOf(Hf.Surface, Hf.Ground)))"))
        assertTrue(duel.contains("HfTitleRule(pt(language, \"Son Harf\", \"Last Letter\")"))
    }

    @Test fun paintedKeyboardKeysStayInsideTheirFrame() {
        val runtime = source("CosmeticRuntime.kt")
        assertTrue(runtime.contains("panelSide = .035f, panelTop = .12f, panelBottom = .05f"))
        assertTrue(source("KeyboardSkin.kt").contains("measurable.measure(constraints.offset(-2 * l, -(t + bt)))"))
    }

    @Test fun framesShowOnPlayersEverywhere() {
        assertTrue(source("ProfilePhotoRuntime.kt").contains("if (framed) ProfileFrameArt(frameId, size)"))
        assertTrue(source("ProfileFrameCollection.kt").contains("\"get_public_profile_frame_v1\""))
        assertTrue(source("WordSiegePanMatch.kt").contains("frameId = rememberPlayerFrame(profile?.id)"))
        assertTrue(source("PremierWordDuelScreen.kt").contains("frameId = if (room.isBot) null else rememberPlayerFrame(opponent?.id)"))
        assertTrue(source("CompetitionRankingView.kt").contains("frameId = rememberPlayerFrame(userId)"))
        assertTrue(source("PremiumHomeV3.kt").contains("a clickable Surface would stretch its gold background"))
    }

    @Test fun rivalsMascotIsVisibleWithoutOwningOne() {
        val migration = File("../supabase/migrations/20260928100000_player_mascot_choice_v1.sql").readText()
        assertTrue(migration.contains("raise exception 'mascot_not_owned'"))
        assertTrue(migration.contains("create or replace function public.get_player_mascot_v1(p_user_id uuid)"))
        assertTrue(source("MascotStore.kt").contains("\"get_player_mascot_v1\""))
        assertTrue(source("WordSiegePanMatch.kt").contains("mascot = rememberRivalMascot(opponent?.id)"))
        assertTrue(source("PremierWordDuelScreen.kt").contains("mascot = if (room.isBot) null else rememberRivalMascot(opponent?.id)"))
    }

    @Test fun mascotSpeaksPlainlyAndHintsGiveTheAnswer() {
        val voice = source("MascotVoice.kt")
        assertFalse(voice.contains("Pıt pıt"))
        assertFalse(voice.contains("(◕‿◕)"))
        assertTrue(source("MascotHints.kt").contains("sh(\"Cevap: \$shown — hemen yaz!\""))
        val practice = source("WordSiegePracticeScreen.kt")
        assertTrue(practice.contains("WordSiegePracticeEngine.hintMove(snapshot)"))
        assertTrue(practice.contains("placements = move.placements"))
        assertTrue(source("WordSiegePracticeBoard.kt").contains("key = \"hintmove:\${hint.first}\""))
    }

    @Test fun proSeesTheMoveScoreBeforeConfirming() {
        assertTrue(source("WordSiegePracticeEngine.kt").contains("fun previewScore(board: List<WordSiegeCellDto>, rack: String, placements: Map<Int, Int>): Int?"))
        assertTrue(source("WordSiegePanMatch.kt").contains("if (mine?.isVip == true && placements.isNotEmpty())"))
        assertTrue(source("WordSiegePracticeScreen.kt").contains("if (playerProfile?.isVip == true && placements.isNotEmpty())"))
    }

    @Test fun storeOnlySellsAndTheProfileManages() {
        assertTrue(source("ProfileFrameStore.kt").contains("if (equipped || owned) sh(\"SATIN ALINDI\", \"PURCHASED\") else price"))
        assertTrue(source("MascotStore.kt").contains("Text(sh(\"SATIN ALINDI\", \"PURCHASED\")"))
        assertTrue(source("ProfileOwnedThemesSection.kt").contains("private fun OwnedMascotsPicker()"))
    }

    @Test fun hintAnswerGlowsAndTheMascotFliesOverIt() {
        val board = source("WordSiegePracticeBoard.kt")
        assertTrue(board.contains("kind = WordSiegeMascotVisitKind.ANSWER"))
        assertTrue(board.contains("hintGlow = if (index in hintSet) hintPulse else 0f"))
        val companion = source("WordSiegeMascotCompanion.kt")
        assertTrue(companion.contains("WordSiegeMascotVisitKind.ANSWER -> {"))
        assertTrue(companion.contains("flyBeside(above, 900, onTop = true)"))
    }

    @Test fun victoryCrownSaysWhatItDoesAndCrownsEveryWin() {
        assertTrue(source("StoreProductPreview.kt").contains("item.id == \"victory_crown\" -> sh("))
        assertTrue(source("EconomyShopScreen.kt").contains("storeItemEffect(item)?.let"))
        assertTrue(source("PremierWordDuelScreen.kt").contains("CrownVictoryCelebration(eventKey = \"sonharf:"))
    }

    @Test fun walnutIsAFullTheme() {
        assertTrue(source("CosmeticRuntime.kt").contains("val walnutTheme: Boolean get() = gameThemeId == WALNUT_IVORY_THEME_ID"))
        assertTrue(source("HiggsfieldUi.kt").contains("else if (SonHarfCosmetics.walnutTheme) Color(0xFFFAF3E3) else Color(0xFFFFFFFF)"))
        assertTrue(source("SonHarfTheme.kt").contains("else if (SonHarfCosmetics.walnutTheme) Color(0xFFEFE3CC)"))
        assertTrue(source("PremierWordDuelScreen.kt").contains("else if (SonHarfCosmetics.walnutTheme) Color(0xFFFAF3E3) else Color(0xFFF7E3A6)"))
    }
}
