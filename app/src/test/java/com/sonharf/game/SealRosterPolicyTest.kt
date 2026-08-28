package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SealRosterPolicyTest {
    @Test
    fun nerisIsAlwaysFreeAndCanBeEquipped() {
        val state = SealRosterPolicy.state(
            character = LetharaLore.character("neris"),
            ownedItemIds = emptySet(),
            equippedMascotId = MascotCatalog.DEFAULT_ID,
        )
        assertEquals(SealRosterAvailability.FREE, state.availability)
        assertTrue(state.active)
        assertTrue(SealRosterPolicy.canEquip(state))
        assertEquals(0, state.plannedPrice)
    }

    @Test
    fun legacyLyraIsParkedAndCannotBeEquipped() {
        val state = SealRosterPolicy.state(
            character = LetharaLore.character("lyra"),
            ownedItemIds = setOf(MascotCatalog.LEGACY_WHITE_ID),
            equippedMascotId = MascotCatalog.LEGACY_WHITE_ID,
        )
        assertEquals(SealRosterAvailability.AWAITING_3D, state.availability)
        assertFalse(state.active)
        assertFalse(SealRosterPolicy.canEquip(state))
        assertEquals(null, state.plannedPrice)
    }

    @Test
    fun futureSealsNeverPretendToBePlayable() {
        listOf("kael", "ryvan", "mivo", "selen").forEach { key ->
            val state = SealRosterPolicy.state(
                character = LetharaLore.character(key),
                ownedItemIds = setOf("mascot_$key"),
                equippedMascotId = "mascot_$key",
            )
            assertEquals(SealRosterAvailability.AWAITING_3D, state.availability)
            assertFalse(state.active)
            assertFalse(SealRosterPolicy.canEquip(state))
        }
    }

    @Test
    fun plannedFuturePricesStayStable() {
        assertEquals(850, SealRosterPolicy.plannedPrice("kael"))
        assertEquals(900, SealRosterPolicy.plannedPrice("ryvan"))
        assertEquals(800, SealRosterPolicy.plannedPrice("mivo"))
        assertEquals(950, SealRosterPolicy.plannedPrice("selen"))
    }
}
