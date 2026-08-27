package com.sonharf.game

/**
 * Active mascot product policy.
 *
 * Eve is intentionally parked and must not be surfaced by the active UI.
 * The verified white skeletal pet is the standard free/default mascot.
 * Additional mascots may be sold only after both commercial-game license and runtime verification.
 */
internal object MascotPolicy {
    const val ENABLED = true
    const val ALLOW_2D_OR_VIDEO_FALLBACK = false
    const val DEFAULT_MASCOT_ID = MascotCatalog.DEFAULT_ID
    const val EVE_ACTIVE = false

    const val WHITE_MASCOT_ASSET = MascotCatalog.WHITE_ASSET
    const val WHITE_MASCOT_SHA256 = "27779dcb3a201013c47dc4a5099540a17984f805c998984f45ff84cf34426a41"
    const val WHITE_MASCOT_SIZE_BYTES = 781_848

    const val CHIBI_WIZARD_LICENSE_APPROVED = true
    const val CHIBI_WIZARD_ASSET_READY = false
}
