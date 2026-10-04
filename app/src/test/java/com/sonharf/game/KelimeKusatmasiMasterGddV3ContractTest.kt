package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KelimeKusatmasiMasterGddV3ContractTest {
    @Test
    fun canonicalPaletteMatchesCurrentPurchasedThemeDirection() {
        val theme = File("src/main/java/com/sonharf/game/SonHarfTheme.kt").readText()

        listOf(
            "0xFFE6ECF2", // Light blue-grey ground
            "0xFFFFFFFF", // White surface
            "0xFFF7E3A6", // Cream letter tiles
            "0xFFE0A82E", // Gold
            "0xFF3E9F4D", // Player green
            "0xFFD0514A", // Rival red
            "0xFF6B7A8C", // Muted
        ).forEach { token -> assertTrue("Missing current theme palette token $token", theme.contains(token)) }

        assertTrue(theme.contains("val IsDark: Boolean get() = alternateDark"))
        assertTrue(theme.contains("val ActionOrange: Color get()"))
        assertTrue(theme.contains("val HeroStart: Color get()"))
        assertTrue(theme.contains("val HeroMiddle: Color get()"))
        assertTrue(theme.contains("val HeroEnd: Color get()"))
    }

    @Test
    fun modeHierarchyAndLanguageScopeStayFocused() {
        val shell = File("src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt").readText()
        val firstRun = File("src/main/java/com/sonharf/game/IntroWelcome.kt").readText()
        val localization = File("src/main/java/com/sonharf/game/SonHarfUiState.kt").readText()

        assertTrue(shell.contains("title = sh(\"KELİME KUŞATMASI\", \"WORD SIEGE\")"))
        assertTrue(shell.contains("title = sh(\"SON HARF\", \"LAST LETTER\")"))
        assertTrue(shell.contains("title = sh(\"KELİME ATÖLYESİ\", \"WORD WORKSHOP\")"))
        assertTrue(firstRun.contains("\"tr\" to \"TÜRKÇE\""))
        assertTrue(firstRun.contains("selected == \"en\""))
        assertFalse(firstRun.contains("selected == \"es\""))
        assertFalse(firstRun.contains("selected == \"fr\""))
        assertFalse(firstRun.contains("selected == \"de\""))
        // The brand is Kelime Tahtı again: legacy Kuşatma names map to it.
        assertFalse(localization.contains(".replace(\"WORD SIEGE\", \"WORD THRONE\")"))
        assertTrue(localization.contains(".replace(\"KUŞATMA SENİN!\", \"TAHT SENİN!\")"))
        assertTrue(localization.contains(".replace(\"SIEGE WON!\", \"THE THRONE IS YOURS!\")"))
    }

    @Test
    fun homeSurfacesProfileCoinProAndNotificationEntry() {
        val home = File("src/main/java/com/sonharf/game/PremiumHomeV3.kt").readText()

        assertTrue(home.contains("FramedProfilePhotoAvatar("))
        assertTrue(home.contains("profile?.diamonds"))
        assertTrue(home.contains("HfCoin("))
        // Membership access remains in the paired shortcut row, without a duplicate header badge.
        val menu = home.substringAfter("internal fun HomeQuickMenu(").substringBefore("private fun HomeQuickAction(")
        assertTrue(menu.contains("PRO üyelik"))
        assertTrue(menu.contains("PRO üyeliğim"))
        assertTrue(menu.contains("onPro)"))
        assertTrue(menu.contains("onMascots)"))
        assertTrue(menu.contains("onClick = onActivity"))
        assertTrue(home.contains("profile?.isVip == true"))
        assertTrue(home.contains("ratingLeagueProgress(it.rating)"))
        assertTrue(home.contains("\"${'$'}{it.rating} RP\""))
        assertTrue(home.contains("Icons.Rounded.Settings"))
        assertTrue(home.contains("onClick = onSettings"))
        assertTrue(home.contains("sh(\"Ayarlar\", \"Settings\")"))
    }

    @Test
    fun clubSurfaceStaysAuthoredButNeverReachesUsers() {
        val shell = File("src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt").readText()
        val social = File("src/main/java/com/sonharf/game/data/CompetitionSocial.kt").readText()

        // Retired: CLUB destination is intercepted and bounced to HOME, never renders CompetitionHubScreen anymore.
        assertFalse(shell.contains("PremiumDestination.CLUB -> CompetitionHubScreen("))
        assertTrue(shell.contains("PremiumDestination.CLUB -> {"))
        assertTrue(shell.contains("destination = PremiumDestination.HOME"))

        assertTrue(social.contains("\"report_player\""))
        assertTrue(social.contains("\"block_user\""))
        assertTrue(social.contains("\"club_chat_spam_or_abuse\""))
    }

    @Test
    fun gameExitReturnsHomeAndPracticeMoveStatusKeepsFixedHeight() {
        val shell = File("src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt").readText()
        val practice = File("src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt").readText()

        // A game returns to where it was opened from: My Games when started there, otherwise Home.
        assertTrue(shell.contains("var gameReturn by remember { mutableStateOf(PremiumDestination.HOME) }"))
        assertTrue(shell.contains("gameReturn = if (destination == PremiumDestination.MY_GAMES) PremiumDestination.MY_GAMES else PremiumDestination.HOME"))
        assertTrue(shell.contains("fun leaveGame(target: PremiumDestination = gameReturn)"))
        assertTrue(shell.contains("PremiumDestination.LAST_LETTER, PremiumDestination.SIEGE, PremiumDestination.WORD_WORKSHOP ->"))
        assertTrue(shell.contains("gameReturn\n            }"))
        assertTrue(practice.contains("Modifier.fillMaxWidth().height(16.dp)"))
        assertTrue(practice.contains("readyFeedback.message"))
        assertTrue(practice.contains("lineHeight = 12.sp"))
    }

    @Test
    fun clubChatHasServerAuthoritativeAntiSpamAndAbuseGuard() {
        val migration = File("../supabase/migrations/20260915061500_club_chat_server_guard_v2.sql").readText()

        assertTrue(migration.contains("create or replace function private.guard_club_message_insert_v2()"))
        assertTrue(migration.contains("security definer"))
        assertTrue(migration.contains("p.chat_suspended_until > clock_timestamp()"))
        assertTrue(migration.contains("raise exception 'chat_suspended'"))
        assertTrue(migration.contains("pg_advisory_xact_lock"))
        assertTrue(migration.contains("interval '1500 milliseconds'"))
        assertTrue(migration.contains("v_recent_count >= 8"))
        assertTrue(migration.contains("club_chat_duplicate_message"))
        assertTrue(migration.contains("new.body := v_body"))
        assertTrue(migration.contains("new.created_at := clock_timestamp()"))
        assertTrue(migration.contains("create trigger club_messages_server_guard_v2"))
        assertTrue(migration.contains("before insert on public.club_messages"))
        assertTrue(migration.contains("revoke all on function private.guard_club_message_insert_v2()"))
        assertTrue(migration.contains("drop policy if exists club_messages_read_members"))
        assertTrue(migration.contains("from public.user_blocks ub"))
        assertTrue(migration.contains("ub.blocked_id = club_messages.sender_id"))
    }

    @Test
    fun economyKeepsVerifiedFramesAndZeroPayToWinPromise() {
        val economy = File("src/main/java/com/sonharf/game/data/EconomyStore.kt").readText()
        val shop = File("src/main/java/com/sonharf/game/EconomyShopScreen.kt").readText()

        listOf(
            "frame_round_starter_blue",
            "frame_round_starter_pink",
            "frame_round_starter_neutral",
            "frame_round_ocean",
            "frame_round_botanic",
            "frame_round_lilac",
            "frame_round_rose",
        ).forEach { id -> assertTrue("Missing profile frame $id", economy.contains("\"$id\"")) }

        assertTrue(economy.contains("vip_pro_frame_access"))
        assertFalse(shop.contains("ADİL OYUN SÖZÜ"))
        assertFalse(shop.contains("ADİL OYUN SÖZÜ"))
    }

    @Test
    fun territoryScoringRemainsPermanentWordScorePlusTwoPerOwnedCube() {
        val rules = File("src/main/java/com/sonharf/game/WordSiegeFinalRules.kt").readText()

        assertTrue(rules.contains("const val CUBE_TRANSFER_POINTS: Int = 2"))
        assertTrue(rules.contains("wordScore + cubeTransfer(ownedCubes)"))
        assertTrue(rules.contains("Word points are permanent"))
        assertTrue(rules.contains("only a rival capture of one"))
    }

    @Test
    fun normalMatchVictoryDependsOnlyOnCurrentTerritoryControl() {
        val migration = File("../supabase/migrations/20260915060000_word_siege_territory_victory_v10.sql").readText()
        val practice = File("src/main/java/com/sonharf/game/WordSiegePracticeEngine.kt").readText()

        assertTrue(migration.contains("when r.player_one_area > r.player_two_area then r.player_one_id"))
        assertTrue(migration.contains("when r.player_two_area > r.player_one_area then r.player_two_id"))
        assertTrue(migration.contains("if p_forfeit_winner is not null then"))
        assertFalse(migration.contains("v_one_total > v_two_total"))
        assertFalse(migration.contains("v_two_total > v_one_total"))

        assertTrue(practice.contains("state.playerArea > state.botArea -> 1"))
        assertTrue(practice.contains("state.botArea > state.playerArea -> 2"))
        assertFalse(practice.contains("totalScore(state, 1) > totalScore(state, 2) -> 1"))
        assertFalse(practice.contains("totalScore(state, 2) > totalScore(state, 1) -> 2"))
    }
}
