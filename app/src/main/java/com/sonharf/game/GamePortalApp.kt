package com.sonharf.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Legacy-compatible shared palette.
// Kept in sync with the active premium dark theme so rollback/legacy screens
// do not flash back to the old light visual language.
internal val PortalBg = Color(0xFF0B0D12)
internal val PortalCard = Color(0xFF141821)
internal val PortalText = Color(0xFFF4F6FA)
internal val PortalMuted = Color(0xFF9299A8)
internal val PortalBlue = Color(0xFFB9F227)
internal val PortalGold = Color(0xFFFFC857)
internal val PortalGreen = Color(0xFF61D68A)
internal val PortalRed = Color(0xFFFF6B72)

/**
 * Active Son Harf V1 shell.
 *
 * The legacy/experimental screens remain available in source control as
 * rollback material, but the shipped V1 path is intentionally focused on the
 * verified core duel flow.
 */
@Composable
fun GamePortalApp() {
    StableV1App()
}
