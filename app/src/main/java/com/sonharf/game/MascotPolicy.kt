package com.sonharf.game

/**
 * Compatibility facade for the old mascot policy.
 * The former white-pet GLB is retired; Eve is the only mascot asset contract.
 */
internal object MascotPolicy {
    const val ENABLED = true
    const val ALLOW_2D_OR_VIDEO_FALLBACK = false
    const val SKELETAL_ASSET_READY = true
    const val MODEL_ASSET = EveAssetPolicy.MODEL_ASSET

    // Filled only after the final Eve GLB is produced and accepted.
    const val MODEL_SHA256 = ""
    const val MODEL_SIZE_BYTES = 0
}
