package com.sonharf.game

/**
 * Son Harf uses exactly one mascot: the licensed dark Chibi Cat Wizard.
 * No mascot collection, alternate white mascot, lore character or mascot sales are active.
 */
internal object MascotPolicy {
    const val ENABLED = true
    const val ALLOW_2D_OR_VIDEO_FALLBACK = false
    const val DEFAULT_MASCOT_ID = MascotCatalog.CHIBI_WIZARD_ID
    const val CHIBI_WIZARD_LICENSE_APPROVED = true
    const val CHIBI_WIZARD_ASSET_READY = true
}
