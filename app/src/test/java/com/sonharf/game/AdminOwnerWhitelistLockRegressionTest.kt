package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminOwnerWhitelistLockRegressionTest {
    @Test
    fun adminAuthorizationIsLockedToExactlyTheTwoOwnerEmails() {
        val migration = projectFile(
            "supabase/migrations/20260917142000_lock_admin_to_owner_accounts.sql",
        ).readText().lowercase()

        val allowed = setOf("makalega58@gmail.com", "makalega68@gmail.com")
        allowed.forEach { email -> assertTrue(migration.contains("'$email'")) }

        assertTrue(migration.contains("delete from public.admin_users"))
        assertTrue(migration.contains("join auth.users"))
        assertTrue(migration.contains("a.user_id = auth.uid()"))
        assertTrue(migration.contains("a.role = 'admin'"))
        assertTrue(migration.contains("create or replace function public.is_admin()"))

        val emails = Regex("'[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}'")
            .findAll(migration)
            .map { it.value.removeSurrounding("'") }
            .toSet()
        assertTrue(emails.containsAll(allowed))
        assertFalse(emails.any { it !in allowed })
    }

    private fun projectFile(path: String): File =
        listOf(File(path), File("../$path")).firstOrNull(File::exists)
            ?: error("Project path missing: $path")
}
