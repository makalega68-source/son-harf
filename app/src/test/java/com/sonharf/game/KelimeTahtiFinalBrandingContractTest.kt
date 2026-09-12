package com.sonharf.game

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class KelimeTahtiFinalBrandingContractTest {
    private fun source(path: String): String = File(path).readText()

    @Test fun flagshipHomeCtaIsExplicitlyPlayAndLegacyBrandIsTranslatedAtTheUiBoundary() {
        val state = source("src/main/java/com/sonharf/game/SonHarfUiState.kt")
        val home = source("src/main/java/com/sonharf/game/UnifiedProApp.kt")

        assertTrue(home.contains("PremiumPlayButton(onClick = onSiege)"))
        assertTrue(home.contains("Kelime kur • alanı ele geçir • haritayı kontrol et"))
        assertTrue(state.contains("OYNA • Kelime kur • alanı ele geçir • haritayı kontrol et"))
        assertTrue(state.contains("PLAY • Build words • capture territory • control the map"))
        assertTrue(state.contains(".replace(\"KELİME KUŞATMASI\", \"KELİME TAHTI\")"))
        assertTrue(state.contains(".replace(\"WORD SIEGE\", \"WORD THRONE\")"))
    }

    @Test fun onlineMatchExposesSignatureSiegeFeedbackWithoutChangingAuthoritativeScoring() {
        val state = source("src/main/java/com/sonharf/game/SonHarfUiState.kt")
        val online = source("src/main/java/com/sonharf/game/WordSiegePanMatch.kt")

        assertTrue(online.contains("Kelime +${'$'}{move.wordScore}"))
        assertTrue(online.contains("Bölge +${'$'}{move.areaScore}"))
        assertTrue(state.contains("Regex(\"Bölge \\\\+(\\\\d+)\")"))
        assertTrue(state.contains("KUŞATMA +${'$'}territoryScore"))
        assertTrue(state.contains("SIEGE +${'$'}territoryScore"))
        assertTrue(state.contains(".replace(\"KUŞATMA SENİN!\", \"TAHT SENİN!\")"))
        assertTrue(state.contains(".replace(\"SIEGE WON!\", \"THE THRONE IS YOURS!\")"))
        assertTrue(online.contains("WordSiegeFinalRules.currentTerritoryScore"))
        assertTrue(online.contains("WordSiegeFinalRules.cubeTransfer"))
    }

    @Test fun onlineBoardUsesGreenOwnTerritoryAndRedRivalTerritory() {
        val online = source("src/main/java/com/sonharf/game/WordSiegePanMatch.kt")

        assertTrue(online.contains("PanSiegeMine = Color(0xFFA8D5B5)"))
        assertTrue(online.contains("PanSiegeRival = Color(0xFFE4AEAA)"))
        assertTrue(online.contains("PanSiegeMineBorder = Color(0xFF3F7C53)"))
        assertTrue(online.contains("PanSiegeRivalBorder = Color(0xFF9B4D4A)"))
        assertTrue(online.contains("WordSiegeBoardSpec.displayBonusLabel(activeBonus)"))
        assertTrue(online.contains("val regionGap = if (letter != null && owner != 0) .55.dp else 1.25.dp"))
        assertTrue(online.contains("HAMLEYİ ONAYLA"))
    }
}