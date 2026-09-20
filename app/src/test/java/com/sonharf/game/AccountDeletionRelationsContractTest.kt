package com.sonharf.game

import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountDeletionRelationsContractTest {
    @Test fun accountDeletionCannotBeBlockedBySharedHistoryOrClubOwnership() {
        val migration = projectFile("supabase/migrations/20260920135500_account_deletion_relations_v1.sql").readText()
        val edge = projectFile("supabase/functions/delete-account/index.ts").readText()

        assertTrue(migration.contains("club_challenges_created_by_fkey"))
        assertTrue(migration.contains("on delete set null"))
        assertTrue(migration.contains("trivia_rounds_winner_id_fkey"))
        assertTrue(migration.contains("profiles_prepare_delete_relations_v1"))
        assertTrue(migration.contains("where c.owner_id = old.id"))
        assertTrue(migration.contains("order by cm.joined_at, cm.user_id"))
        assertTrue(migration.contains("delete from public.clubs where id = v_club_id"))
        assertTrue(migration.contains("set owner_id = v_successor"))

        // The Edge Function may delete only the user represented by the verified bearer token.
        assertTrue(edge.contains("userClient.auth.getUser()"))
        assertTrue(edge.contains("admin.auth.admin.deleteUser(userData.user.id)"))
    }

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
