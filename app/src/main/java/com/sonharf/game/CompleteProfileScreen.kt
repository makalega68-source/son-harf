package com.sonharf.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Keeps both profile generations: the V2 identity/photo/statistics experience and
 * the mature privacy/settings/account-deletion controls that were added later.
 */
@Composable
fun CompleteProfileScreen() {
    var tab by remember { mutableIntStateOf(0) }
    Column(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF040717), SonHarfBg, Color(0xFF060A18)))
        )
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = tab == 0,
                onClick = { tab = 0 },
                label = { Text(sh("OYUNCU KARTI", "PLAYER CARD"), fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                modifier = Modifier.weight(1f),
            )
            FilterChip(
                selected = tab == 1,
                onClick = { tab = 1 },
                label = { Text(sh("GİZLİLİK & AYARLAR", "PRIVACY & SETTINGS"), fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                modifier = Modifier.weight(1f),
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth().height(1.dp),
            color = SonHarfPurple.copy(alpha = .22f),
            shape = RoundedCornerShape(999.dp),
        ) {}
        Box(Modifier.weight(1f)) {
            if (tab == 0) ProfileExperienceV2Screen() else FinalProfileScreen()
        }
    }
}
