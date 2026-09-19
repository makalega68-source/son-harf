package com.sonharf.game

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider

/**
 * Exact one-argument overload used by the shop bottom sheet.
 * It preserves the existing Google Play catalogue and appends Profile Frames V2.
 */
@Composable
fun GooglePlayProductsCard(onPurchased: () -> Unit) {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    Column {
        GooglePlayProductsCard(
            onPurchased = onPurchased,
            showPremiumProducts = true,
            showCoinPacks = true,
        )
        Spacer(Modifier.height(12.dp))
        ProfileFramesV2StoreRow(backend = backend, onChanged = onPurchased)
    }
}
