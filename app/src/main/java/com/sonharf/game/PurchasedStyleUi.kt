package com.sonharf.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Profile/avatar frames have been retired from the product.
 * These compatibility entry points intentionally render no frame UI so stale
 * ownership/equipped values cannot surface in shop, collection, profile or game.
 */
internal object PurchasedFrameCatalog {
    val ids: Set<String> = emptySet()
    fun drawable(id: String?): Int? = null
}

@Composable
internal fun PurchasedProfileFrameOverlay(
    frameId: String?,
    modifier: Modifier = Modifier,
) = Unit

@Composable
internal fun PurchasedProfileFramesSection(
    modifier: Modifier = Modifier,
    onFrameEquipped: ((String?) -> Unit)? = null,
) = Unit
