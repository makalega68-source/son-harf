package com.sonharf.game

/**
 * Single accepted Eve asset contract. A build must fail before packaging when this exact
 * rigged GLB is absent or changed; runtime is not allowed to substitute a 2D/video mascot.
 */
internal object MascotPolicy {
    const val ENABLED = true
    const val ALLOW_2D_OR_VIDEO_FALLBACK = false
    const val SKELETAL_ASSET_READY = true
    const val MODEL_ASSET = EveAssetPolicy.MODEL_ASSET
    const val MODEL_SHA256 = "0c68ac4c4f5475332fac77ccb9bda4bb08bd202a5d596114552e37ab27d6c39e"
    const val MODEL_SIZE_BYTES = 4_870_220L
    const val JOINT_COUNT = 38
    const val ANIMATION_COUNT = 16
}
