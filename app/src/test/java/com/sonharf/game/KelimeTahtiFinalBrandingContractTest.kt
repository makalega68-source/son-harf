package com.sonharf.game

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class KelimeKusatmasiFinalBrandingContractTest {
    private fun source(path: String): String = File(path).readText()

    @Test fun flagshipHomeCtaIsExplicitlyBattleAndCanonicalBrandIsPreserved() {
        val state = source("src/main/java/com/sonharf/game/SonHarfUiState.kt")
        val home = source("src/main/java/com/sonharf/game/PremiumHomeV3.kt")

        assertTrue(home.contains("onClick = onSiege"))
        assertTrue(home.contains("sh(\"OYNA\", \"PLAY\")"))
        assertTrue(home.contains("HfTitleRule(sh(\"Ana Oyun\", \"Main Game\")"))
        assertTrue(state.contains("OYNA • Harflerini yerleştir, kelimeni oluştur"))
        assertTrue(state.contains("PLAY • Place your tiles, build your word"))
        assertTrue(state.contains(".replace(\"KELİME KUŞATMASI\", \"KELİME TAHTI\")"))
        assertTrue(state.contains(".replace(\"Kelime Kuşatması\", \"Kelime Tahtı\")"))
        assertFalse(state.contains(".replace(\"KELİME TAHTI\", \"KELİME KUŞATMASI\")"))
        assertTrue(state.contains(".replace(\"WORD SIEGE\", \"WORD THRONE\")"))
        assertTrue(
            home.contains("\"KELİME TAHTI\"") ||
                home.contains("\"KELİME\\nKUŞATMASI\"")
        )
    }

    @Test fun onlineMatchExposesSignatureSiegeFeedbackWithoutChangingAuthoritativeScoring() {
        val state = source("src/main/java/com/sonharf/game/SonHarfUiState.kt")
        val online = source("src/main/java/com/sonharf/game/WordSiegePanMatch.kt")

        assertTrue(online.contains("Kelime +${'$'}{move.wordScore}"))
        assertTrue(online.contains("Bölge +${'$'}{move.areaScore}"))
        assertFalse(state.contains("Regex(\"Bölge \\\\+(\\\\d+)\")"))
        assertFalse(state.contains("KUŞATMA +${'$'}territoryScore"))
        assertFalse(state.contains("SIEGE +${'$'}territoryScore"))
        assertFalse(state.contains(".replace(\"TAHT SENİN!\", \"KUŞATMA SENİN!\")"))
        assertFalse(state.contains(".replace(\"THE THRONE IS YOURS!\", \"SIEGE WON!\")"))
        assertTrue(online.contains("TAHT SENİN!"))
        assertTrue(online.contains("THE THRONE IS YOURS!"))
        assertTrue(online.contains("WordSiegeFinalRules.scoreWithTerritoryLedger"))
        assertTrue(online.contains("playerOneAreaScore"))
        assertTrue(online.contains("playerTwoAreaScore"))
    }

    @Test fun onlineBoardKeepsTerritoryOwnershipAndStrategicZonePresentation() {
        val online = source("src/main/java/com/sonharf/game/WordSiegePanMatch.kt")

        assertTrue(online.contains("PanSiegeMine = Color(0xFF5FAF73)"))
        assertTrue(online.contains("PanSiegeRival = Color(0xFFD9776F)"))
        assertTrue(online.contains("PanSiegeMineBorder = Color(0xFF7FC391)"))
        assertTrue(online.contains("PanSiegeRivalBorder = Color(0xFFEB9E97)"))
        assertTrue(online.contains("WordSiegeBoardSpec.displayBonusLabel(activeBonus, !SonHarfUiState.isEnglish)"))
        assertTrue(online.contains("val regionGap = 1.25.dp"))
        assertTrue(online.contains("HAMLEYİ ONAYLA"))
    }
}
