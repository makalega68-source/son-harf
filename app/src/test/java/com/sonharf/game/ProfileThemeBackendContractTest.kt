package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileThemeBackendContractTest {
    @Test fun builtInMainThemeRemainsFreeWhileBlackThemeIsASeparateOwnedStyle() {
        val economy = File("src/main/java/com/sonharf/game/data/EconomyStore.kt").readText()
        val profileThemes = File("src/main/java/com/sonharf/game/ProfileOwnedThemesSection.kt").readText()

        assertFalse(economy.contains("theme_main_blue_white"))
        assertFalse(economy.contains("theme_monster_blue"))
        assertTrue(economy.contains("theme_black"))
        assertTrue(profileThemes.contains("backend.equipDefaultGameTheme()"))
        assertTrue(profileThemes.contains("title = sh(\"Ana Tema\", \"Main Theme\")"))
        assertTrue(profileThemes.contains("Varsayılan görünüm • Ücretsiz"))
        assertTrue(profileThemes.contains("title = \"Black Theme\""))
        assertTrue(profileThemes.contains("BlackThemeId in owned -> BlackThemeId"))
        assertFalse(profileThemes.contains("purchaseShopItem"))
    }
}
