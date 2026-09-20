package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KelimeKusatmasiMasterGddV3ContractTest {
    @Test
    fun canonicalPaletteMatchesApprovedAdultWordGameDirection() {
        val theme = File("src/main/java/com/sonharf/game/SonHarfTheme.kt").readText()

        listOf(
            "0xFF365F53",
            "0xFF23443B",
            "0xFF4F7B6E",
            "0xFF6E7F8C",
            "0xFFAD6A57",
            "0xFFF4F2EC",
            "0xFFFFFEFA",
            "0xFF202A28",
            "0xFF69736F",
            "0xFFD5DAD4",
        ).forEach { token -> assertTrue("Missing approved adult palette token $token", theme.contains(token)) }

        assertTrue(theme.contains("val IsDark: Boolean get() = SonHarfCosmetics.blackThemeActive"))
        assertTrue(theme.contains("internal object BlackThemePalette"))
        assertTrue(theme.contains("val ActionOrange: Color get()"))
        assertTrue(theme.contains("val HeroStart: Color get()"))
        assertTrue(theme.contains("val HeroMiddle: Color get()"))
        assertTrue(theme.contains("val HeroEnd: Color get()"))
        assertFalse(theme.contains("0xFF7C3AED"))
        assertFalse(theme.contains("0xFFF97316"))
        assertFalse(theme.contains("MonsterLime"))
    }

    @Test
    fun modeHierarchyAndLanguageScopeStayFocused() {
        val shell = File("src/main/java/com/sonharf/game/PremiumCanvaApp.kt").readText()
        val firstRun = File("src/main/java/com/sonharf/game/StableV1App.kt").readText()
        val localization = File("src/main/java/com/sonharf/game/SonHarfUiState.kt").readText()

        assertTrue(shell.contains("title = \"KELİME KUŞATMASI\""))
        assertTrue(shell.contains("title = sh(\"SON HARF\", \"LAST LETTER\")"))
        assertTrue(shell.contains("title = sh(\"HARF YOLU\", \"LETTER PATH\")"))
        assertTrue(firstRun.contains("selected == \"tr\""))
        assertTrue(firstRun.contains("selected == \"en\""))
        assertFalse(firstRun.contains("selected == \"es\""))
        assertFalse(firstRun.contains("selected == \"fr\""))
        assertFalse(firstRun.contains("selected == \"de\""))
        assertTrue(localization.contains(".replace(\"WORD THRONE\", \"KELİME KUŞATMASI\")"))
        assertTrue(localization.contains(".replace(\"TAHT SENİN!\", \"KUŞATMA SENİN!\")"))
        assertTrue(localization.contains(".replace(\"THE THRONE IS YOURS!\", \"SIEGE WON!\")"))
    }

    @Test
    fun homeSurfacesProfileCoinProAndNotificationEntry() {
        val home = File("src/main/java/com/sonharf/game/PremiumHomeV3.kt").readText()
        assertTrue(home.contains("FramedProfilePhotoAvatar("))
        assertTrue(home.contains("profile?.diamonds"))
        assertTrue(home.contains("} Coin"))
        assertTrue(home.contains("\"PRO\""))
        assertTrue(home.contains("profile?.isVip == true"))
        assertTrue(home.contains("ratingLeagueProgress(it.rating)"))
        assertTrue(home.contains("\"${'$'}{it.rating} RP\""))
        assertTrue(home.contains("Icons.Rounded.Notifications"))
        assertTrue(home.contains("onClick = onSocial"))
        assertTrue(home.contains("Bildirimler ve davetler"))
    }

    @Test
    fun legacyClubDestinationDoesNotReenterTheNewRuntimeShell() {
        val shell = File("src/main/java/com/sonharf/game/PremiumCanvaApp.kt").readText()
        val club = File("src/main/java/com/sonharf/game/KelimeKusatmasiClubScreen.kt").readText()
        val social = File("src/main/java/com/sonharf/game/data/CompetitionSocial.kt").readText()
        assertFalse(shell.contains("CLUB"))
        assertFalse(shell.contains("KelimeKusatmasiClubScreen("))
        assertTrue(club.contains("Text(sh(\"KULÜP SOHBETİ\", \"CLUB CHAT\")"))
        assertTrue(club.contains("b.getClubMessages(current.clubId)"))
        assertTrue(club.contains("b.sendClubMessage(current.clubId, outgoing)"))
        assertTrue(social.contains("\"report_player\""))
        assertTrue(social.contains("\"block_user\""))
        assertTrue(social.contains("\"club_chat_spam_or_abuse\""))
    }

    @Test
    fun gameExitReturnsHomeAndPracticeMoveStatusKeepsFixedHeight() {
        val shell = File("src/main/java/com/sonharf/game/PremiumCanvaApp.kt").readText()
        val practice = File("src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt").readText()
        assertTrue(shell.contains("fun leaveGame(target: PremiumCanvaDestination = PremiumCanvaDestination.HOME)"))
        assertTrue(shell.contains("PremiumCanvaDestination.LAST_LETTER,"))
        assertTrue(shell.contains("PremiumCanvaDestination.SIEGE,"))
        assertTrue(shell.contains("PremiumCanvaDestination.LETTER_PATH ->"))
        assertTrue(shell.contains("PremiumCanvaDestination.HOME"))
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
        listOf("frame_round_starter_blue","frame_round_starter_pink","frame_round_starter_neutral","frame_round_ocean","frame_round_botanic","frame_round_lilac","frame_round_rose")
            .forEach { id -> assertTrue("Missing profile frame $id", economy.contains("\"$id\"")) }
        assertTrue(economy.contains("vip_pro_frame_access"))
        assertTrue(shop.contains("ADİL OYUN SÖZÜ"))
        assertTrue(shop.contains("Mağaza ürünleri maç gücü, skor veya rating avantajı sağlamaz."))
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
