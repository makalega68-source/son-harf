package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SealRosterPolicyTest {
    @Test
    fun lyraIsAlwaysFreeAndCanBeEquipped() {
        val state = SealRosterPolicy.state(
            character = LetharaLore.character("lyra"),
            ownedItemIds = emptySet(),
            equippedMascotId = MascotCatalog.DEFAULT_ID,
        )
        assertEquals(SealRosterAvailability.FREE, state.availability)
        assertTrue(state.active)
        assertTrue(SealRosterPolicy.canEquip(state))
        assertEquals(0, state.plannedPrice)
    }

    @Test
    fun nerisRequiresOwnershipBeforeEquip() {
        val locked = SealRosterPolicy.state(
            character = LetharaLore.character("neris"),
            ownedItemIds = emptySet(),
            equippedMascotId = MascotCatalog.DEFAULT_ID,
        )
        assertEquals(SealRosterAvailability.STORE, locked.availability)
        assertFalse(locked.active)
        assertFalse(SealRosterPolicy.canEquip(locked))
        assertEquals(700, locked.plannedPrice)

        val owned = SealRosterPolicy.state(
            character = LetharaLore.character("neris"),
            ownedItemIds = setOf(MascotCatalog.CHIBI_WIZARD_ID),
            equippedMascotId = MascotCatalog.CHIBI_WIZARD_ID,
        )
        assertEquals(SealRosterAvailability.OWNED, owned.availability)
        assertTrue(owned.active)
        assertTrue(SealRosterPolicy.canEquip(owned))
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
