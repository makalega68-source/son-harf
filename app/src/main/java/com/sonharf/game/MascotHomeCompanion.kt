package com.sonharf.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun MascotHomeCompanion(
    modifier: Modifier = Modifier,
    playerName: String? = null,
    mascotId: String? = null,
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { MascotSelectionRuntime.load(context) }
    val selected = MascotCatalog.item(mascotId ?: MascotSelectionRuntime.selectedId)

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            MascotLive3DStage(
                modifier = Modifier.fillMaxSize(),
                mascotId = selected.id,
                displayScale = 1.75f,
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        MascotRuntime.react(MascotMotion.GREETING)
                    },
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = if (SonHarfUiState.isEnglish) selected.nameEn else selected.nameTr,
            color = LetharaPalette.Text,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
            maxLines = 1,
        )
    }
}
