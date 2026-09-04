package com.sonharf.game

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll as foundationVerticalScroll
import androidx.compose.ui.Modifier

/**
 * Keeps the production store scroll call bound to Compose Foundation without changing
 * the active store implementation while the store branch is under regression validation.
 */
internal fun Modifier.verticalScroll(state: ScrollState): Modifier =
    this.foundationVerticalScroll(state)
