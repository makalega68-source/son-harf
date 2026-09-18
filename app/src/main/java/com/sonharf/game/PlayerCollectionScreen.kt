package com.sonharf.game

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend

/** Compact category-first collection screen. */
@Composable
internal fun PlayerCollectionScreen(backend: OnlineGameBackend, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        SonHarfLeafBackdrop(Modifier.matchParentSize())
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                MainScreenHeader(
                    title = sh("Koleksiyonum", "My Collection"),
                    subtitle = sh("Ürünlerini kategorilere göre seç ve kullan", "Choose and equip items by category"),
                    onBack = onBack,
                )
            }
            item {
                Text(
                    sh("Tüm ürünler aşağıda sade kutular halinde sıralanır.", "All products are arranged below in simple tiles."),
                    color = SonHarfTheme.TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            item { ProfileOwnedThemesSection(backend) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
