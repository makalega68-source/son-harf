package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LetharaThemeContractTest {
    @Test
    fun remoteExperienceDefaultsToCanonicalLetharaV3() {
        val config = RemoteExperienceConfig()

        assertTrue(config.version >= 3)
        assertEquals("#071229", config.backgroundColor)
        assertEquals("#101D39", config.surfaceColor)
        assertEquals("#15284A", config.surfaceVariantColor)
        assertEquals("#F4F0FF", config.textColor)
        assertEquals("#FFD36A", config.primaryColor)
        assertEquals("#56D6FF", config.secondaryColor)
        assertEquals("SÖZ DOKUSU DÜELLOSU", config.homeWordArenaBadgeTr)
        assertEquals("WORD WEAVE DUEL", config.homeWordArenaBadgeEn)
    }

    @Test
    fun appStartsInDarkLetharaMode() {
        assertTrue(SonHarfUiState.darkMode)
    }
}
