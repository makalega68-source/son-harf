package com.sonharf.game

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class KelimeTahtiFinalBrandingContractTest {
    private fun source(path: String): String = File(path).readText()

    @Test fun flagshipHomeCtaIsExplicitlyPlayAndCurrentBrandIsPreserved() {
        val state = source("src/main/java/com/sonharf/game/SonHarfUiState.kt")
        val home = source("src/main/java/com/sonharf/game/PremiumHomeV3.kt")

        assertTrue(home.contains("PremiumSiegeHero(onPlay = onSiege)"))
        assertTrue(home.contains("sh(\"OYNA\", \"PLAY\")"))
        assertTrue(home.contains("Kelime kur. Bölge kazan. Haritanın kontrolünü ele geçir."))
        assertTrue(state.contains("OYNA • Harflerini yerleştir, kelimeni oluştur"))
        assertTrue(state.contains("PLAY • Place your tiles, build your word"))
        assertFalse(state.contains(".replace(\"KELİME KUŞATMASI\", \"KELİME TAHTI\")"))
        assertFalse(state.contains(".replace(\"WORD SIEGE\", \"WORD THRONE\")"))
        assertTrue(home.contains("sh(\"KELİME\\nKUŞATMASI\", \"WORD\\nSIEGE\")"))
    }

    @Test fun onlineMatchExposesSignatureSiegeFeedbackWithoutChangingAuthoritativeScoring() {
        val state = source("src/main/java/com/sonharf/game/SonHarfUiState.kt")
        val online = source("src/main/java/com/sonharf/game/WordSiegePanMatch.kt")

        assertTrue(online.contains("Kelime +${'$'}{move.wordScore}"))
        assertTrue(online.contains("Bölge +${'$'}{move.areaScore}"))
        assertFalse(state.contains("Regex(\"Bölge \\\\+(\\\\d+)\")"))
        assertFalse(state.contains("KUŞATMA +${'$'}territoryScore"))
        assertFalse(state.contains("SIEGE +${'$'}territoryScore"))
        assertFalse(state.contains(".replace(\"KUŞATMA SENİN!\", \"TAHT SENİN!\")"))
        assertFalse(state.contains(".replace(\"SIEGE WON!\", \"THE THRONE IS YOURS!\")"))
        assertTrue(online.contains("KUŞATMA SENİN!"))
        assertTrue(online.contains("SIEGE WON!"))
        assertTrue(online.contains("WordSiegeFinalRules.currentTerritoryScore"))
        assertTrue(online.contains("WordSiegeFinalRules.cubeTransfer"))
    }

    @Test fun onlineBoardKeepsTerritoryOwnershipAndStrategicZonePresentation() {
        val online = source("src/main/java/com/sonharf/game/WordSiegePanMatch.kt")

        assertTrue(online.contains("PanSiegeMine = Color(0xFFA8D5B5)"))
        assertTrue(online.contains("PanSiegeRival = Color(0xFFE4AEAA)"))
        assertTrue(online.contains("PanSiegeMineBorder = Color(0xFF3F7C53)"))
        assertTrue(online.contains("PanSiegeRivalBorder = Color(0xFF9B4D4A)"))
        assertTrue(online.contains("WordSiegeBoardSpec.displayBonusLabel(activeBonus, !SonHarfUiState.isEnglish)"))
        assertTrue(online.contains("val regionGap = 1.25.dp"))
        assertTrue(online.contains("HAMLEYİ ONAYLA"))
    }
}
