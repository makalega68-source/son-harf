package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileThemeBackendContractTest {
    @Test fun builtInMainThemeIsNotReintroducedAsPaidCatalogProduct() {
        val economy = File("src/main/java/com/sonharf/game/data/EconomyStore.kt").readText()
        val profileThemes = File("src/main/java/com/sonharf/game/ProfileOwnedThemesSection.kt").readText()

        assertFalse(economy.contains("theme_main_blue_white"))
        assertFalse(economy.contains("theme_monster_blue\")"))
        assertTrue(profileThemes.contains("selectTheme(null)"))
        assertTrue(profileThemes.contains("Ana Mavi Beyaz"))
    }
}
