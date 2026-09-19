package com.sonharf.game

import androidx.compose.runtime.Composable

/**
 * Keeps the pre-existing V2 call shape `GooglePlayProductsCard { ... }` source-compatible
 * while the premium/coin visibility flags remain available to the new store surfaces.
 */
@Composable
fun GooglePlayProductsCard(onPurchased: () -> Unit) {
    GooglePlayProductsCard(
        onPurchased = onPurchased,
        showPremiumProducts = true,
        showCoinPacks = true,
    )
}
