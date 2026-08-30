package com.sonharf.game

import androidx.compose.runtime.Composable

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
