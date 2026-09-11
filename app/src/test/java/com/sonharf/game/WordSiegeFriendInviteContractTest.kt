package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeFriendInviteContractTest {
    @Test
    fun friendInvitesAreServerAuthoritativeAndParticipantScoped() {
        val migration = projectFile(
            "supabase/migrations/20260911173000_word_siege_friend_invites_v9.sql",
        ).readText()

        assertTrue(migration.contains("create table if not exists public.word_siege_invites"))
        assertTrue(migration.contains("alter table public.word_siege_invites enable row level security"))
        assertTrue(migration.contains("using ((select auth.uid()) in (sender_id, receiver_id))"))
        assertTrue(migration.contains("revoke all on public.word_siege_invites from anon, authenticated"))
        assertTrue(migration.contains("grant select on public.word_siege_invites to authenticated"))
        assertFalse(migration.contains("grant insert on public.word_siege_invites to authenticated"))

        assertTrue(migration.contains("f.status = 'accepted'"))
        assertTrue(migration.contains("public.user_blocks"))
        assertTrue(migration.contains("word_siege_active_limit"))
        assertTrue(migration.contains("private.word_siege_new_bag_v1(inv.language)"))
        assertTrue(migration.contains("private.word_siege_new_board_v1()"))
        assertTrue(migration.contains("'friend_game_started'"))

        assertTrue(migration.contains("revoke all on function public.invite_friend_to_word_siege_v1(uuid,text) from public, anon, authenticated"))
        assertTrue(migration.contains("revoke all on function public.respond_word_siege_invite_v1(uuid,boolean) from public, anon, authenticated"))
        assertTrue(migration.contains("grant execute on function public.invite_friend_to_word_siege_v1(uuid,text) to authenticated"))
        assertTrue(migration.contains("grant execute on function public.respond_word_siege_invite_v1(uuid,boolean) to authenticated"))
    }

    @Test
    fun clientAndSocialUiKeepWordSiegePrimaryWhilePreservingLastLetterInvites() {
        val backend = projectFile(
            "app/src/main/java/com/sonharf/game/data/WordSiegeBackend.kt",
        ).readText()
        val social = projectFile(
            "app/src/main/java/com/sonharf/game/MainSocialScreen.kt",
        ).readText()
        val shell = projectFile(
            "app/src/main/java/com/sonharf/game/UnifiedProApp.kt",
        ).readText()

        assertTrue(backend.contains("getIncomingWordSiegeInvites"))
        assertTrue(backend.contains("inviteFriendToWordSiege"))
        assertTrue(backend.contains("respondWordSiegeInvite"))
        assertTrue(backend.contains("invite_friend_to_word_siege_v1"))
        assertTrue(backend.contains("respond_word_siege_invite_v1"))

        assertTrue(social.contains("KELİME KUŞATMASI DAVETLERİ"))
        assertTrue(social.contains("SON HARF DAVETLERİ"))
        assertTrue(social.contains("backend.inviteFriendToWordSiege(friend.id"))
        assertTrue(social.contains("backend.inviteFriendToWordSiege(rival.opponentId"))
        assertTrue(social.contains("backend.respondWordSiegeInvite(invite.id, true)"))
        assertTrue(social.contains("backend.respondGameInvite(invite.id, true)"))

        assertTrue(shell.contains("onPlay = { destination = UnifiedDestination.GAME }"))
        assertTrue(shell.contains("onSiege = { destination = UnifiedDestination.SIEGE }"))
    }

    private fun projectFile(path: String): File =
        listOf(File(path), File("../$path")).firstOrNull(File::exists)
            ?: error("Project path missing: $path")
}
