package com.sonharf.game

import org.junit.Assert.assertFalse
import org.junit.Test

class MascotPolicyTest {
    @Test fun mascotIsPermanentlyDisabled() {
        assertFalse(MascotPolicy.ENABLED)
    }
}
