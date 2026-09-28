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
        assertTrue(source("PremiumHomeV3.kt").contains("PRO: a compact gold badge when active, an invitation otherwise."))
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
        // The score floats above the word on the board, only for PRO.
        assertTrue(source("WordSiegePanMatch.kt").contains("pendingScore = pendingScore.takeIf { mine?.isVip == true }"))
        assertTrue(source("WordSiegePracticeScreen.kt").contains("pendingScore = pendingMove?.takeIf { playerProfile?.isVip == true && it.placements == placements }?.wordScore"))
        assertFalse(source("WordSiegePracticeScreen.kt").contains("\" • +\$it\""))
    }

    @Test fun tilesAreCarriedWithTheFingerAndValidWordsGetAGreenCheck() {
        val drag = source("WordSiegeTileDrag.kt")
        assertTrue(drag.contains("detectDragGestures("))
        assertTrue(drag.contains("drag.start(currentRack, currentFrom, currentLetter, c.localToWindow(local), 46.dp.toPx())"))
        assertTrue(drag.contains("fun WordSiegePendingMoveBadges("))
        assertTrue(drag.contains("Icons.Rounded.Check"))
        // The score bubble sits above the word.
        assertTrue(drag.contains("(top - bubbleH - 4f)"))
        for (screen in listOf("WordSiegePracticeScreen.kt", "WordSiegePanMatch.kt")) {
            val s = source(screen)
            assertTrue(screen, s.contains("WordSiegeTileDragOverlay(tileDrag)"))
            assertTrue(screen, s.contains(".wordSiegeTileDragSource("))
            assertTrue(screen, s.contains("wordSiegeDropTile(placements,"))
        }
        assertTrue(source("WordSiegePracticeEngine.kt").contains("fun previewValidScore("))
        assertTrue(source("WordSiegeExperience.kt").contains("onPlacementsChange = { next ->"))
        assertTrue(source("WordSiegeSeriesScreen.kt").contains("onPlacementsChange = { next ->"))
    }

    @Test fun weeklyPodiumUsesTheNewArtWithFramelessPhotos() {
        val art = source("WeeklyPodiumArt.kt")
        assertTrue(art.contains("art = R.drawable.weekly_podium_blue"))
        assertTrue(art.contains("art = R.drawable.weekly_podium_gold"))
        // Photos fill the ring exactly: clipped to the circle, cropped, no frame composable.
        assertTrue(art.contains("Image(bitmap.asImageBitmap(), seat.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)"))
        assertFalse(art.contains("ProfileFrameArt("))
        // Photos sit behind the art (wells are cut out), so rings and the 1-2-3 badges stay on top.
        assertTrue(art.indexOf("if (seat != null) PodiumPhoto(seat, size) else EmptyWell()") < art.indexOf("Image(painterResource(style.art)"))
        assertTrue(art.contains("(labelH * (if (compete) .46f else .36f)).toSp()"))
        assertFalse(art.contains("ProfilePhotoAvatarWithGender("))
        for (name in listOf("weekly_podium_blue.png", "weekly_podium_gold.png")) {
            val f = listOf(File("src/main/res/drawable-nodpi/$name"), File("app/src/main/res/drawable-nodpi/$name")).first { it.exists() }
            assertTrue(name, f.length() > 100_000L)
        }
        val home = source("PremiumHomeV3.kt")
        assertTrue(home.contains("style = WeeklyPodiumStyle.HOME"))
        assertFalse(home.contains("HomePodiumSpot("))
        assertFalse(home.contains("PremiumWeeklyPodium("))
        assertTrue(home.contains("delay(WEEKLY_PODIUM_REFRESH_MS)"))
        val compete = source("CompetitionRankingView.kt")
        assertTrue(compete.contains("style = WeeklyPodiumStyle.COMPETE"))
        assertFalse(compete.contains("RankingPodiumStep("))
        assertTrue(compete.contains("kotlinx.coroutines.delay(WEEKLY_PODIUM_REFRESH_MS)"))
        assertFalse(source("UnifiedProApp.kt").contains("WeeklyChampionPodium("))
    }

    @Test fun carriedBoardTilesKeepTheirGestureAndTheBoardStaysFast() {
        for (screen in listOf("WordSiegePracticeBoard.kt", "WordSiegePanMatch.kt")) {
            val s = source(screen)
            // The drag source follows the placed tile, not the (hidden while carried) shown one.
            assertTrue(screen, s.contains("dragSource = if (placedRackIndex != null) {"))
            assertTrue(screen, s.contains("derivedStateOf { tileDrag?.hoverCell }"))
        }
        val board = source("WordSiegePracticeBoard.kt")
        assertFalse(board.contains("rememberInfiniteTransition(label = \"hint-glow\")"))
        assertTrue(board.contains("hintGlow: (() -> Float)? = null"))
        val duel = source("PremierWordDuelScreen.kt")
        assertTrue(duel.contains("PREMIER_WORD_TILE_STAGGER_MS = 40L"))
        assertTrue(duel.contains("PREMIER_WORD_TILE_DROP_MS = 200L"))
        assertTrue(source("MascotHatPainter.kt").contains("Placement(R.drawable.hat_beret_worn,"))
    }

    @Test fun mascotOwnersWinWithTheVictoryClipAndOnlyObiIsOnSale() {
        val video = source("ChromaKeyVideo.kt")
        assertTrue(video.contains("smoothstep(0.10, 0.30, distance(c, uKey))"))
        assertTrue(video.contains("isOpaque = false"))
        assertFalse(video.contains("EGL14.eglTerminate("))
        val victory = source("MascotVictoryScreen.kt")
        assertTrue(victory.contains("raw = R.raw.mascot_victory"))
        assertTrue(victory.contains("usePlatformDefaultWidth = false"))
        for (screen in listOf("WordSiegePracticeScreen.kt", "PremierWordDuelScreen.kt", "WordSiegePanMatch.kt")) {
            assertTrue(screen, source(screen).contains("MascotVictoryScreen("))
        }
        val raw = listOf(File("src/main/res/raw/mascot_victory.mp4"), File("app/src/main/res/raw/mascot_victory.mp4")).first { it.exists() }
        assertTrue(raw.length() > 1_000_000L)
        val skins = source("WordSiegeMascotSkins.kt")
        assertTrue(skins.contains("val onSale: Boolean get() = this == ORB"))
        for (name in listOf("\"Pofi\"", "\"Buzo\"", "\"Zıpo\"", "\"Mino\"", "\"Bibo\"", "\"Novi\"")) assertTrue(name, skins.contains(name))
        val store = source("MascotStore.kt")
        assertTrue(store.contains("val comingSoon = !owned && !skin.onSale"))
        assertTrue(store.contains("sh(\"ÇOK YAKINDA\", \"COMING SOON\")"))
        assertTrue(store.contains("if (!skin.onSale) return"))
    }

    @Test fun hintLettersFlyOntoTheBoard() {
        val practice = source("WordSiegePracticeScreen.kt")
        assertTrue(practice.contains("tileDrag.launchFlights("))
        assertTrue(practice.contains("WordSiegeTileFlight(\"hint\$key:\$cell\", cell, letter, from, to, order * 140)"))
        assertTrue(source("WordSiegeTileDrag.kt").contains("sin(PI * t.value)"))
    }


    @Test fun storeOnlySellsAndTheProfileManages() {
        assertTrue(source("ProfileFrameStore.kt").contains("if (equipped || owned) sh(\"SATIN ALINDI\", \"PURCHASED\") else price"))
        assertTrue(source("MascotStore.kt").contains("Text(sh(\"SATIN ALINDI\", \"PURCHASED\")"))
        assertTrue(source("ProfileOwnedThemesSection.kt").contains("private fun OwnedMascotsPicker()"))
    }

    @Test fun hintAnswerGlowsAndTheMascotFliesOverIt() {
        val board = source("WordSiegePracticeBoard.kt")
        assertTrue(board.contains("kind = WordSiegeMascotVisitKind.ANSWER"))
        assertTrue(board.contains("hintGlow = if (index in hintSet) ({ hintPulse.value }) else null"))
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
