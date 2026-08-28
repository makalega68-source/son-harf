package com.sonharf.game

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
    ): SealRosterState {
        val mascotId = character.mascotId
        if (mascotId == null) {
            return SealRosterState(
                availability = SealRosterAvailability.AWAITING_3D,
                active = false,
                plannedPrice = plannedPrice(character.key),
            )
        }

        if (mascotId == MascotCatalog.LEGACY_WHITE_ID) {
            return SealRosterState(
                availability = SealRosterAvailability.AWAITING_3D,
                active = false,
                plannedPrice = null,
            )
        }

        val active = (equippedMascotId ?: MascotCatalog.DEFAULT_ID) == mascotId
        if (mascotId == MascotCatalog.DEFAULT_ID) {
            return SealRosterState(
                availability = SealRosterAvailability.FREE,
                active = active,
                plannedPrice = 0,
            )
        }

        val owned = mascotId in ownedItemIds
        return SealRosterState(
            availability = if (owned) SealRosterAvailability.OWNED else SealRosterAvailability.STORE,
            active = active && owned,
            plannedPrice = if (owned) null else 700,
        )
    }

    fun plannedPrice(key: String): Int = when (key) {
        "kael" -> 850
        "ryvan" -> 900
        "mivo" -> 800
        "selen" -> 950
        "neris" -> 0
        else -> 0
    }

    fun canEquip(state: SealRosterState): Boolean =
        state.availability == SealRosterAvailability.FREE ||
            state.availability == SealRosterAvailability.OWNED
}
