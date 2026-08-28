package com.sonharf.game

/**
 * Compatibility policy for archived callers.
 * Son Harf now has one permanent mascot. It is always available, never sold and never collected.
 */
internal enum class SealRosterAvailability {
    FREE,
    OWNED,
    STORE,
    AWAITING_3D,
}

internal data class SealRosterState(
    val availability: SealRosterAvailability,
    val active: Boolean,
    val plannedPrice: Int?,
)

internal object SealRosterPolicy {
    fun state(
        character: WizardLoreCharacter,
        ownedItemIds: Set<String>,
        equippedMascotId: String?,
    ): SealRosterState = SealRosterState(
        availability = SealRosterAvailability.FREE,
        active = true,
        plannedPrice = 0,
    )

    fun plannedPrice(key: String): Int = 0

    fun canEquip(state: SealRosterState): Boolean = true
}
