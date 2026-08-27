package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MascotPolicyTest {
    @Test
    fun whiteMascotIsDefaultAndEveIsParked() {
        assertTrue(MascotPolicy.ENABLED)
        assertFalse(MascotPolicy.ALLOW_2D_OR_VIDEO_FALLBACK)
        assertFalse(MascotPolicy.EVE_ACTIVE)
        assertEquals(MascotCatalog.DEFAULT_ID, MascotPolicy.DEFAULT_MASCOT_ID)
        assertTrue(MascotCatalog.item(MascotCatalog.DEFAULT_ID).standard)
        assertTrue(MascotCatalog.item(MascotCatalog.DEFAULT_ID).licensedForCommercialGame)
    }

    @Test
    fun onlyCommerciallyApprovedCatalogEntriesCanBeActivated() {
        assertTrue(MascotCatalog.all.all { it.licensedForCommercialGame })
        assertTrue(MascotPolicy.CHIBI_WIZARD_LICENSE_APPROVED)
        assertFalse(MascotPolicy.CHIBI_WIZARD_ASSET_READY)
    }

    @Test
    fun genericMotionRegistryCoversEveryMotion() {
        assertEquals(MascotMotion.entries.toSet(), MascotAnimationRegistry.all.map { it.motion }.toSet())
    }
}
