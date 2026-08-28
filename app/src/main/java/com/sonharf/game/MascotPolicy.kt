package com.sonharf.game

/**
 * Active mascot product policy.
 *
 * Eve is intentionally parked and must not be surfaced by the active UI.
 * Neris is the single active Son Harf mascot. Legacy Lyra assets stay packaged only for
 * migration/rollback compatibility and must not be selected by the active UI.
 */
internal object MascotPolicy {
    const val ENABLED = true
    const val ALLOW_2D_OR_VIDEO_FALLBACK = false
    const val ACTIVE_MASCOT_ID = MascotCatalog.CHIBI_WIZARD_ID
    const val DEFAULT_MASCOT_ID = ACTIVE_MASCOT_ID
    const val EVE_ACTIVE = false

    const val WHITE_MASCOT_ASSET = MascotCatalog.WHITE_ASSET
    const val WHITE_MASCOT_SHA256 = "ac07c6833b101b2ef228aa6039ec951841a77c5939cfd5e102a27446c333c104"
    const val WHITE_MASCOT_SIZE_BYTES = 1_540_504

    const val CHIBI_WIZARD_LICENSE_APPROVED = true
    const val CHIBI_WIZARD_ASSET_READY = true
}
