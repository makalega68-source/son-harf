package com.sonharf.game

import org.junit.Assert.assertTrue
import org.junit.Test

class MascotPolicyTest {
    @Test fun approvedMascotIsEnabled() {
        assertTrue(MascotPolicy.ENABLED)
    }
}
