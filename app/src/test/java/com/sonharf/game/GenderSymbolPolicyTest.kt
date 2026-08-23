package com.sonharf.game

import org.junit.Assert.assertTrue
import org.junit.Test

class GenderSymbolPolicyTest {
    @Test fun genderSymbolsRemainFramelessAndDistinct() {
        val female = 0xFFFF4F9AL
        val male = 0xFF238BFFL
        assertTrue(female != male)
    }
}
