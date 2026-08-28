package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SealRosterPolicyTest {
    @Test
    fun compatibilityRosterAlwaysReturnsTheSingleFreeMascot() {
        val state = SealRosterPolicy.state(
            character = LetharaLore.character(null),
            ownedItemIds = emptySet(),
            equippedMascotId = null,
        )
        assertEquals(SealRosterAvailability.FREE, state.availability)
        assertTrue(state.active)
        assertEquals(0, state.plannedPrice)
        assertTrue(SealRosterPolicy.canEquip(state))
    }

    @Test
    fun archivedPriceApiCanNeverSellAMascot() {
        listOf("companion", "old", "anything").forEach { key ->
            assertEquals(0, SealRosterPolicy.plannedPrice(key))
        }
    }
}
