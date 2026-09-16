package com.sonharf.game

import androidx.compose.runtime.Composable

/** Stable entry name for the refreshed unified product shell. */
@Composable
fun RefreshedUnifiedProApp(onSignedOut: () -> Unit) {
    VisualRefreshProApp(onSignedOut = onSignedOut)
}
