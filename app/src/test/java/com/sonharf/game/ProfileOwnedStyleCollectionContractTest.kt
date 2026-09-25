package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileOwnedStyleCollectionContractTest {
    @Test fun ownedCollectionSurvivesStoreRotationAndRefreshFailures() {
        val profile = projectFile("app/src/main/java/com/sonharf/game/ProfileOwnedThemesSection.kt").readText()
        val economy = projectFile("app/src/main/java/com/sonharf/game/data/EconomyStore.kt").readText()
        val migration = projectFile("supabase/migrations/20260920150000_store_runtime_equip_parity.sql").readText()
        val databaseTest = projectFile("supabase/tests/permanent_style_ownership.sql").readText()

        assertTrue(profile.contains("backend.getOwnedShopItems(nextOwned)"))
        assertTrue(profile.contains("backend.getInventory()"))
        assertTrue(profile.contains("backend.getEquippedCosmetics()"))
        assertTrue(profile.contains("backend.equipDefaultGameTheme()"))
        assertTrue(profile.contains("backend.equipShopItem(itemId)"))
        assertTrue(profile.contains("A failed request must not erase cached UI"))
        assertTrue(profile.contains("SonHarfCosmetics.applyAndPersist(context, nextEquipped)"))
        assertTrue(profile.contains("enabled = enabled && supported && !active"))
        assertTrue(profile.contains("GameColors.PrimarySurface"))
        assertTrue(profile.contains("GameColors.PrimaryBlue"))
        assertTrue(profile.contains("GameShapes.Large"))
        assertFalse(profile.contains("MainUi."))
        assertFalse(profile.contains("SonHarfTheme."))
        assertFalse(profile.contains("Monster"))
        assertTrue(economy.contains("filter { it.id in owned }"))

        assertTrue(migration.contains("Do not require active=true here"))
        assertTrue(migration.contains("if not v_owned then raise exception 'not_owned'; end if;"))
        assertTrue(migration.contains("revoke all on function public.equip_shop_item(text) from public,anon"))
        assertTrue(migration.contains("grant execute on function public.equip_shop_item(text) to authenticated,service_role"))
        assertTrue(databaseTest.contains("store_rotation_still_revokes_owned_runtime_style"))
        assertTrue(databaseTest.contains("equip_ownership_gate_missing"))
        assertTrue(databaseTest.contains("anonymous_equip_rpc_exposed"))
        assertTrue(databaseTest.contains("rollback;"))
    }

    @Test fun currentBlackThemeAndLegacyDarkOwnershipShareOneTruthfulCard() {
        val profile = projectFile("app/src/main/java/com/sonharf/game/ProfileOwnedThemesSection.kt").readText()

        assertTrue(profile.contains("private const val BlackThemeId = \"theme_black\""))
        assertTrue(profile.contains("private const val LegacyDarkArenaThemeId = \"theme_dark_arena\""))
        assertTrue(profile.contains("private val DarkThemeIds = setOf(BlackThemeId, LegacyDarkArenaThemeId)"))
        assertTrue(profile.contains("SonHarfCosmetics.gameThemeId?.takeIf { it in DarkThemeIds }"))
        assertTrue(profile.contains("BlackThemeId in owned -> BlackThemeId"))
        assertTrue(profile.contains("title = \"Black Theme\""))
        assertTrue(profile.contains("onClick = { equipStyle(ownedDarkThemeId ?: BlackThemeId) }"))
        assertTrue(profile.contains("collection.filter { it.id !in DarkThemeIds && it.isSupportedOwnedStyle() }"))
        assertFalse(profile.contains("title = sh(\"Gece Arenası\", \"Night Arena\")"))
        assertFalse(profile.contains("SonHarfCosmetics.gameThemeId == DarkArenaThemeId"))
    }

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
