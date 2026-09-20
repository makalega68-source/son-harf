package com.sonharf.game

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayCoinRefundContractTest {
    private val migrationPath =
        "supabase/migrations/20260920123000_son_coin_refund_clawback_v1.sql"

    @Test
    fun refundDebtIsServerOnlyAndCannotGoNegative() {
        val migration = projectFile(migrationPath).readText()

        assertTrue(migration.contains("create table if not exists public.son_coin_refund_debts"))
        assertTrue(migration.contains("amount integer not null check (amount > 0)"))
        assertTrue(migration.contains("alter table public.son_coin_refund_debts enable row level security"))
        assertTrue(migration.contains("revoke all on table public.son_coin_refund_debts from anon, authenticated"))
    }

    @Test
    fun firstRefundClawsBackExactTokenScopedCoinGrantOnlyOnce() {
        val migration = projectFile(migrationPath).readText()

        assertTrue(migration.contains("v_was_revoked := v_purchase.status = 'revoked' or v_purchase.revoked_at is not null"))
        assertTrue(migration.contains("if p_revoke and not v_was_revoked then"))
        assertTrue(migration.contains("'google_play_purchase:' || trim(p_purchase_token)"))
        assertTrue(migration.contains("'google_play_pro_lifetime:' || trim(p_purchase_token)"))
        assertTrue(migration.contains("l.user_id = v_purchase.user_id"))
        assertTrue(migration.contains("l.delta > 0"))
        assertTrue(migration.contains("v_clawback := least(greatest(v_balance, 0), v_coin_grant)"))
        assertTrue(migration.contains("'google_play_refund_clawback:' || trim(p_purchase_token)"))
        assertTrue(migration.contains("revoked_at = case when p_revoke then coalesce(revoked_at, now())"))
    }

    @Test
    fun spentRefundedCoinsBecomeDebtInsteadOfNegativeBalance() {
        val migration = projectFile(migrationPath).readText()

        assertTrue(migration.contains("v_debt_added := v_coin_grant - v_clawback"))
        assertTrue(migration.contains("insert into public.son_coin_refund_debts(user_id, amount, updated_at)"))
        assertTrue(migration.contains("amount = public.son_coin_refund_debts.amount + excluded.amount"))
        assertTrue(migration.contains("update public.profiles"))
        assertTrue(migration.contains("set diamonds = diamonds - v_clawback"))
    }

    @Test
    fun futureCoinIncomeAutomaticallyRepaysRefundDebt() {
        val migration = projectFile(migrationPath).readText()

        assertTrue(migration.contains("create or replace function private.absorb_son_coin_refund_debt_v1()"))
        assertTrue(migration.contains("if new.diamonds <= old.diamonds then"))
        assertTrue(migration.contains("v_increase := new.diamonds - old.diamonds"))
        assertTrue(migration.contains("v_absorbed := least(v_increase, v_debt)"))
        assertTrue(migration.contains("new.diamonds := new.diamonds - v_absorbed"))
        assertTrue(migration.contains("'google_play_refund_debt_repayment'"))
        assertTrue(migration.contains("before update of diamonds on public.profiles"))
        assertTrue(migration.contains("execute function private.absorb_son_coin_refund_debt_v1()"))
    }

    @Test
    fun entitlementRefundProtectionFromPreviousHardeningRemainsPresent() {
        val migration = projectFile(migrationPath).readText()

        listOf("series_game", "letter_table", "score_calculator", "pro_lifetime")
            .forEach { productId -> assertTrue(migration.contains("'$productId'")) }
        assertTrue(migration.contains("set status = 'revoked'"))
        assertTrue(migration.contains("source_id = trim(p_purchase_token)"))
        assertTrue(migration.contains("source_type = 'pro_bundle'"))
        assertTrue(migration.contains("public.has_permanent_entitlement_v1(v_purchase.user_id, 'pro_lifetime')"))
    }

    private fun projectFile(path: String): File =
        sequenceOf(File(path), File("../$path"))
            .firstOrNull { it.exists() }
            ?: error("Missing project file: $path")
}
