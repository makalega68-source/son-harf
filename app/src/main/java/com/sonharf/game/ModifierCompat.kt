package com.sonharf.game

import androidx.compose.ui.Modifier

/**
 * Fallback for composables that need a non-scoped layout modifier.
 * Scoped Column/Row weight extensions still take precedence inside their scopes.
 */
internal fun Modifier.weight(@Suppress("UNUSED_PARAMETER") value: Float): Modifier = this
