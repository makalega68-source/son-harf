package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KelimeKusatmasiPrimaryProductContractTest {
    @Test
    fun kelimeKusatmasiIsThePrimaryProductAndSonHarfIsSecondary() {
        val shell = File("src/main/java/com/sonharf/game/UnifiedProApp.kt").readText()
        val manifest = File("src/main/AndroidManifest.xml").readText()

        assertTrue(manifest.contains("android:label=\"Kelime Kuşatması\""))
        assertTrue(shell.contains("PremiumPlayButton(onClick = onSiege)"))
        assertTrue(shell.contains("sh(\"KELİME KUŞATMASI\", \"WORD SIEGE\")"))
        assertTrue(shell.contains("title = sh(\"SON HARF\", \"LAST LETTER\")"))
        assertTrue(shell.contains("onClick = onPlay"))
    }

    @Test
    fun siegeBoardNoLongerUsesTheClassicCornerAndDiagonalBonusTopology() {
        val spec = File("src/main/java/com/sonharf/game/WordSiegeBoardSpec.kt").readText()

        assertTrue(spec.contains("SiegeMajorZones"))
        assertTrue(spec.contains("SiegeWatchZones"))
        assertTrue(spec.contains("SiegeFortZones"))
        assertTrue(spec.contains("SiegeTacticalZones"))
        assertFalse(spec.contains("private val TripleWord"))
        assertFalse(spec.contains("private val DoubleWord"))
    }

    @Test
    fun matchScreenKeepsWordAndTerritoryScoresSeparate() {
        val match = File("src/main/java/com/sonharf/game/WordSiegePanMatch.kt").readText()

        assertTrue(match.contains("wordPoints = myWordPoints"))
        assertTrue(match.contains("territoryPoints = myTerritoryPoints"))
        assertTrue(match.contains("HARİTA KONTROLÜ"))
        assertTrue(match.contains("HAMLEYİ ONAYLA"))
    }
}
