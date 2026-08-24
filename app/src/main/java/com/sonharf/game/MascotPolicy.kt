package com.sonharf.game

/**
 * Production mascot gate.
 *
 * A mascot may render only when a real GLB contains a quadruped skin/skeleton and animation clips.
 * PNG, bitmap, VideoView and MP4 fallbacks are intentionally forbidden.
 */
internal object MascotPolicy {
    const val ENABLED = true
    const val ALLOW_2D_OR_VIDEO_FALLBACK = false
    const val SKELETAL_ASSET_READY = true
    const val MODEL_ASSET = "models/son_harf_white_pet_rigged.glb"
    const val MODEL_SHA256 = "27779dcb3a201013c47dc4a5099540a17984f805c998984f45ff84cf34426a41"
    const val MODEL_SIZE_BYTES = 781848
}
