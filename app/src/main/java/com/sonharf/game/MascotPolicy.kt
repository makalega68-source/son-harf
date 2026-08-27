package com.sonharf.game

/**
 * Active mascot product policy.
 *
 * Eve is intentionally parked and must not be surfaced by the active UI. The bundled white
 * companion is the default free mascot. Additional mascots may be sold only after both their
 * commercial-game license and runtime asset pass verification.
 */
internal object MascotPolicy {
    const val ENABLED = true
    const val ALLOW_2D_OR_VIDEO_FALLBACK = false
    const val DEFAULT_MASCOT_ID = MascotCatalog.DEFAULT_ID
    const val EVE_ACTIVE = false
    const val CHIBI_WIZARD_LICENSE_APPROVED = true
    const val CHIBI_WIZARD_ASSET_READY = false
}
