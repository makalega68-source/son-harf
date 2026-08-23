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
    const val SKELETAL_ASSET_READY = false
    const val MODEL_ASSET = "models/son_harf_white_pet_rigged.glb"
}
