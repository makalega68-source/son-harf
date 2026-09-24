package com.sonharf.game

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sonharf.game.data.OnlineGameBackend

@Composable
internal fun ProfessionalCollectionScreen(
    backend: OnlineGameBackend,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        GameTopBar(
            title = gameText("Envanterim", "My Inventory"),
            subtitle = gameText("Sahip olduğun ve kullandığın ürünler", "Items you own and use"),
            onBack = onBack,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = GameSpacing.ScreenHorizontal, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GameSurface(
                borderColor = GameColors.Lavender.copy(alpha = .35f),
                elevated = true,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.CollectionsBookmark,
                        contentDescription = null,
                        tint = GameColors.Lavender,
                        modifier = Modifier.size(26.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            gameText("Koleksiyon", "Collection"),
                            color = GameColors.TextPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            gameText(
                                "Sahip olduğun profil görünümlerini inceleyip anında aktif edebilirsin.",
                                "Inspect your owned profile looks and equip them instantly.",
                            ),
                            color = GameColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            ProfileOwnedThemesSection(backend = backend)
            Spacer(Modifier.height(12.dp))
        }
    }
}
