package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend

/** Profile-owned cosmetics live here, presented with the shared premium Canva design system. */
@Composable
internal fun PlayerCollectionScreen(backend: OnlineGameBackend, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        SonHarfLeafBackdrop(Modifier.matchParentSize())
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                MainScreenHeader(
                    title = sh("Koleksiyonum", "My Collection"),
                    subtitle = sh("Sahip olduğun görünüm ve prestij öğelerini yönet", "Manage your owned cosmetic and prestige items"),
                    onBack = onBack,
                )
            }
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MainUiShape.Hero,
                    color = SonHarfTheme.Surface,
                    border = BorderStroke(1.dp, SonHarfTheme.Purple.copy(alpha = .25f)),
                    shadowElevation = 5.dp,
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SonHarfTheme.Purple.copy(alpha = .10f),
                        ) {
                            Icon(
                                Icons.Rounded.Palette,
                                contentDescription = null,
                                tint = SonHarfTheme.Purple,
                                modifier = Modifier.padding(12.dp).size(26.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                sh("Tarzını tek yerden yönet", "Manage your style in one place"),
                                color = SonHarfTheme.TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                sh("Satın aldığın ürünleri seç, önizle ve aktif hale getir.", "Select, preview and equip your purchased items."),
                                color = SonHarfTheme.TextSecondary,
                                fontSize = 10.sp,
                            )
                        }
                    }
                }
            }
            item {
                PremiumCard(modifier = Modifier.fillMaxWidth(), accent = SonHarfTheme.Turquoise) {
                    ProfileOwnedThemesSection(backend)
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}
