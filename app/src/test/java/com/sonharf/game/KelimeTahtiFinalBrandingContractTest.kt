package com.sonharf.game

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class KelimeTahtiFinalBrandingContractTest {
    private fun source(path: String): String = File(path).readText()

    @Test fun flagshipHomeCtaIsExplicitlyPlayAndLegacyBrandIsTranslatedAtTheUiBoundary() {
        val state = source("src/main/java/com/sonharf/game/SonHarfUiState.kt")
        val home = source("src/main/java/com/sonharf/game/UnifiedProApp.kt")

        assertTrue(home.contains("PremiumPlayButton(onClick = onSiege)"))
        assertTrue(home.contains("Kelime kur • alanı ele geçir • haritayı kontrol et"))
        assertTrue(state.contains("OYNA • Harflerini yerleştir, kelimeni oluştur"))
        assertTrue(state.contains("PLAY • Place your tiles, build your word"))
        assertTrue(state.contains(".replace(\"KELİME KUŞATMASI\", \"KELİME TAHTI\")"))
        assertTrue(state.contains(".replace(\"WORD SIEGE\", \"WORD THRONE\")"))
    }

    @Test fun onlineMatchExposesSignatureSiegeFeedbackWithoutChangingAuthoritativeScoring() {
        val state = source("src/main/java/com/sonharf/game/SonHarfUiState.kt")
        val online = source("src/main/java/com/sonharf/game/WordSiegePanMatch.kt")

        assertTrue(online.contains("Kelime +${'$'}{move.wordScore}"))
        assertTrue(online.contains("Bölge +${'$'}{move.areaScore}"))
        assertFalse(state.contains("Regex(\"Bölge \\\\+(\\\\d+)\")"))
        assertFalse(state.contains("KUŞATMA +${'$'}territoryScore"))
        assertFalse(state.contains("SIEGE +${'$'}territoryScore"))
        assertTrue(state.contains(".replace(\"KUŞATMA SENİN!\", \"TAHT SENİN!\")"))
        assertTrue(state.contains(".replace(\"SIEGE WON!\", \"THE THRONE IS YOURS!\")"))
        assertTrue(online.contains("WordSiegeFinalRules.currentTerritoryScore"))
        assertTrue(online.contains("WordSiegeFinalRules.cubeTransfer"))
    }

    @Test fun onlineBoardKeepsTerritoryOwnershipAndStrategicZonePresentation() {
        val online = source("src/main/java/com/sonharf/game/WordSiegePanMatch.kt")

        assertTrue(online.contains("PanSiegeMine = Color(0xFFE3EDE5)"))
        assertTrue(online.contains("PanSiegeRival = Color(0xFFE4EDF3)"))
        assertTrue(online.contains("PanSiegeMineBorder = Color(0xFF567A64)"))
        assertTrue(online.contains("PanSiegeRivalBorder = Color(0xFF5C8299)"))
        assertTrue(online.contains("WordSiegeBoardSpec.displayBonusLabel(activeBonus, !SonHarfUiState.isEnglish)"))
        assertTrue(online.contains("val regionGap = 1.25.dp"))
        assertTrue(online.contains("HAMLEYİ ONAYLA"))
    }
}
