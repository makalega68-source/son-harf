package com.sonharf.game

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SemanticPathEngineTest {
    @Test fun seedToCityHasValidShortRoute() {
        assertTrue(SemanticPathEngine.canConnect("TOHUM", "TARLA"))
        assertTrue(SemanticPathEngine.canConnect("TARLA", "KÖY"))
        assertTrue(SemanticPathEngine.canConnect("KÖY", "ŞEHİR"))
    }

    @Test fun unrelatedJumpIsRejected() {
        assertFalse(SemanticPathEngine.canConnect("TOHUM", "TELEFON"))
    }

    @Test fun normalizationIsTurkishSafe() {
        assertTrue(SemanticPathEngine.canConnect("KÖY", "şehir"))
    }
}
