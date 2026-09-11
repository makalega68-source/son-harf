package com.sonharf.game

import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileOwnedStyleCollectionContractTest {
    @Test fun ownedCollectionSurvivesStoreRotationAndRefreshFailures() {
        val profile = projectFile("app/src/main/java/com/sonharf/game/ProfileOwnedThemesSection.kt").readText()
        val economy = projectFile("app/src/main/java/com/sonharf/game/data/EconomyStore.kt").readText()
        val migration = projectFile("supabase/migrations/20260908120313_permanent_style_ownership.sql").readText()
        val databaseTest = projectFile("supabase/tests/permanent_style_ownership.sql").readText()

        assertTrue(profile.contains("backend.getOwnedShopItems(nextOwned)"))
        assertTrue(profile.contains("SonHarfCosmetics.gameThemeId == DarkArenaThemeId"))
        assertTrue(profile.contains("A failed request must not erase cached UI"))
        assertTrue(profile.contains("enabled = enabled && supported && !active"))
        assertTrue(economy.contains("filter { it.id in owned }"))

        assertTrue(migration.contains("create policy shop_items_owned_read"))
        assertTrue(migration.contains("revoke execute on function public.equip_shop_item(text) from public, anon"))
        assertTrue(migration.contains("grant execute on function public.equip_shop_item(text) to authenticated"))
        assertTrue(databaseTest.contains("owner_cannot_read_retired_product"))
        assertTrue(databaseTest.contains("retired_product_leaked_to_non_owner"))
        assertTrue(databaseTest.contains("anonymous_equip_rpc_exposed"))
        assertTrue(databaseTest.contains("rollback;"))
    }

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
