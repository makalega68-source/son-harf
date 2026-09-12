package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopSaleWindowContractTest {
    @Test
    fun `purchase rpc enforces catalog sale window without regressing production privileges`() {
        val migration = repoFile("supabase/migrations/20260912103000_shop_sale_window_purchase_enforcement.sql")
            .readText()
            .replace(Regex("\\s+"), "")

        assertTrue(migration.contains("active=true"))
        assertTrue(migration.contains("available_from<=now()"))
        assertTrue(migration.contains("(available_untilisnulloravailable_until>now())"))

        assertTrue(migration.contains("v_owner_unlimitedboolean:=false"))
        assertTrue(migration.contains("o.activeando.unlimited_diamonds"))
        assertTrue(migration.contains("'owner_unlimited_purchase'"))
        assertTrue(migration.contains("v_admin_free:=coalesce(v_admin_free,false)andpublic.is_admin()"))
        assertTrue(migration.contains("updated_at=now()whereid=v_uid"))
        assertTrue(migration.contains("ifv_item.vip_onlyandnotcoalesce(v_vip,false)thenraiseexception'vip_required'"))

        assertTrue(migration.contains("revokeallonfunctionpublic.purchase_shop_item(text)frompublic,anon"))
        assertTrue(migration.contains("grantexecuteonfunctionpublic.purchase_shop_item(text)toauthenticated,service_role"))
        assertFalse(migration.contains("grantexecuteonfunctionpublic.purchase_shop_item(text)toanon"))
    }

    private fun repoFile(path: String): File = sequenceOf(File(path), File("../$path"))
        .firstOrNull { it.exists() }
        ?: error("Missing repository file: $path")
}
