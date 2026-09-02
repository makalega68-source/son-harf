package com.sonharf.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Legacy-compatible shared palette.
// These constants stay because older source files still compile against them,
// even though those screens are not part of the active V1 navigation path.
internal val PortalBg = Color(0xFFF7F9FC)
internal val PortalCard = Color.White
internal val PortalText = Color(0xFF182235)
internal val PortalMuted = Color(0xFF718096)
internal val PortalBlue = Color(0xFF1769E0)
internal val PortalGold = Color(0xFFF3A81A)
internal val PortalGreen = Color(0xFF22B95F)
internal val PortalRed = Color(0xFFE64B55)

/**
 * Active Son Harf V1 shell.
 *
 * The legacy/experimental screens remain available in source control as
 * rollback material, but the shipped V1 path is intentionally focused on the
 * verified core duel flow. The active shell includes the licensed-safe Action
 * UI adaptation applied in MonsterExperienceApp.
 */
@Composable
fun GamePortalApp() {
    StableV1App()
}
