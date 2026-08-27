package com.sonharf.game

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Fixed home companion slot. It deliberately has no card, border or background: only the
 * transparent compact 3D Eve viewport and the player's saved companion name are rendered.
 */
@Composable
internal fun EveHomeFixedCompanion(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val store = remember { EveCompanionStore(context) }

    DisposableEffect(Unit) {
        // Home must be visually stable: no autonomous digging/grazing/sleep loop and no drift.
        EveMascotRuntime.stopLivingBehavior()
        EveMascotRuntime.calm()
        onDispose { EveMascotRuntime.stopLivingBehavior() }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EveLive3DStage(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            compact = true,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = store.name,
            color = Color(0xFF163B58),
            fontWeight = FontWeight.Black,
            fontSize = 12.sp,
            maxLines = 1,
        )
    }
}
