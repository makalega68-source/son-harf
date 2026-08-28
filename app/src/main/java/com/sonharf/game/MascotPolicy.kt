package com.sonharf.game

/**
 * Product contract: Son Harf has exactly one mascot, Chibi.
 * No Eve project, no white mascot, no mascot collection and no mascot sales.
 */
internal object MascotPolicy {
    const val ENABLED = true
    const val ALLOW_2D_OR_VIDEO_FALLBACK = false
    const val DEFAULT_MASCOT_ID = MascotCatalog.CHIBI_WIZARD_ID
    const val CHIBI_WIZARD_LICENSE_APPROVED = true
    const val CHIBI_WIZARD_ASSET_READY = true
}
