package com.sonharf.game

import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileOwnedStyleCollectionContractTest {
    @Test fun ownedCollectionIsCategorizedAndSurvivesStoreRotation() {
        val profile = projectFile("app/src/main/java/com/sonharf/game/ProfileOwnedThemesSection.kt").readText()
        val economy = projectFile("app/src/main/java/com/sonharf/game/data/EconomyStore.kt").readText()
        val migration = projectFile("supabase/migrations/20260908120313_permanent_style_ownership.sql").readText()
        val blackMigration = projectFile("supabase/migrations/20260918213000_black_theme_store_v1.sql").readText()
        val databaseTest = projectFile("supabase/tests/permanent_style_ownership.sql").readText()

        assertTrue(profile.contains("backend.getOwnedShopItems(nextOwned)"))
        assertTrue(profile.contains("BlackThemeId = \"theme_dark_arena\""))
        assertTrue(profile.contains("CollectionCategoryBlock"))
        assertTrue(profile.contains("\"Temalar\""))
        assertTrue(profile.contains("\"Profil Çerçeveleri\""))
        assertTrue(profile.contains("\"Tuş Stilleri\""))
        assertTrue(profile.contains("items.chunked(2)"))
        assertTrue(economy.contains("filter { it.id in owned }"))
        assertTrue(blackMigration.contains("on conflict do nothing"))
        assertTrue(blackMigration.contains("game_theme_id = 'theme_dark_arena'"))

        assertTrue(migration.contains("create policy shop_items_owned_read"))
        assertTrue(migration.contains("revoke execute on function public.equip_shop_item(text) from public, anon"))
        assertTrue(databaseTest.contains("owner_cannot_read_retired_product"))
        assertTrue(databaseTest.contains("retired_product_leaked_to_non_owner"))
        assertTrue(databaseTest.contains("rollback;"))
    }

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
