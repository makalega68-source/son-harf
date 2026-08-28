package com.sonharf.game

/**
 * Active mascot product policy.
 *
 * Eve is intentionally parked and must not be surfaced by the active UI.
 * Lyra and Neris use the licensed customizable Chibi Cat Wizard asset; Lyra is the free white
 * presentation while Neris keeps the darker Shadow Sage presentation.
 */
internal object MascotPolicy {
    const val ENABLED = true
    const val ALLOW_2D_OR_VIDEO_FALLBACK = false
    const val DEFAULT_MASCOT_ID = MascotCatalog.DEFAULT_ID
    const val EVE_ACTIVE = false

    const val WHITE_MASCOT_ASSET = MascotCatalog.WHITE_ASSET
    const val WHITE_MASCOT_SHA256 = "b321193faea91bf6d75a78fb74f947bc4223892c3f5ede0be0c04a7a2be04db2"
    const val WHITE_MASCOT_SIZE_BYTES = 1_500_344

    const val CHIBI_WIZARD_LICENSE_APPROVED = true
    const val CHIBI_WIZARD_ASSET_READY = true
}
