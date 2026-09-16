package com.sonharf.game.ui.premium

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Kept as a source-compatible no-op after the visual PRO badge was retired.
 * Existing call sites may remain while no badge is rendered anywhere.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun ProBadge(
    modifier: Modifier = Modifier,
    sizeDp: Int = 18,
) = Unit
