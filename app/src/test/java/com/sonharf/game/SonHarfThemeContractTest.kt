package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SonHarfThemeContractTest {
    @Test
    fun remoteExperienceDefaultsToCurrentSonHarfV4() {
        val config = RemoteExperienceConfig()

        assertTrue(config.version >= 4)
        assertEquals("KELİME DÜELLOSU", config.homeWordArenaBadgeTr)
        assertEquals("WORD DUEL", config.homeWordArenaBadgeEn)
        assertEquals("Kelimeyi sürdür, rakibini geç.", config.homeWordArenaSubtitleTr)
        assertEquals("Keep the word going, beat your rival.", config.homeWordArenaSubtitleEn)
        assertTrue(config.brandLogoBase64Url.isBlank())
    }
}
