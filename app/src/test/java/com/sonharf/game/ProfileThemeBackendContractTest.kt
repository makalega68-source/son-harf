package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileThemeBackendContractTest {
    @Test fun builtInPremiumThemeIsFreeAndBlackThemeIsTheOnlySellableRuntimeTheme() {
        val economy = File("src/main/java/com/sonharf/game/data/EconomyStore.kt").readText()
        val profileThemes = File("src/main/java/com/sonharf/game/ProfileOwnedThemesSection.kt").readText()
        val migration = File("../supabase/migrations/20260918213000_black_theme_store_v1.sql").readText()

        assertFalse(economy.contains("theme_main_blue_white"))
        assertFalse(economy.contains("theme_dark_arena\")"))
        assertTrue(economy.contains("supportedGameThemeIds = setOf(\"theme_black\")"))
        assertTrue(profileThemes.contains("backend.equipDefaultGameTheme()"))
        assertTrue(profileThemes.contains("Black Theme"))
        assertFalse(profileThemes.contains("Ana Yeşil Beyaz"))
        assertFalse(profileThemes.contains("Gece Arenası"))
        assertFalse(profileThemes.contains("purchaseShopItem"))
        assertTrue(migration.contains("'theme_black'"))
        assertTrue(migration.contains("600"))
        assertTrue(migration.contains("set active = false"))
    }
}
