package com.sonharf.game.ui.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.SonHarfTheme

/**
 * Premium dark panel with subtle inner glow and optional ribbon title (G5.0).
 * Used for menus, dialog cards, and lobby sections.
 */
@Composable
fun GamePanel(
    modifier: Modifier = Modifier,
    title: String? = null,
    accent: Color = SonHarfTheme.GoldBright,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(accent.copy(alpha = 0.85f), accent, accent.copy(alpha = 0.85f)),
                        ),
                    )
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                    ),
                    color = Color(0xFF1B1440),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            SonHarfTheme.PremiumPanel.copy(alpha = 0.98f),
                            SonHarfTheme.PremiumPanel.copy(alpha = 0.88f),
                        ),
                    ),
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            SonHarfTheme.PremiumPanelBorder,
                            SonHarfTheme.PremiumPanel.copy(alpha = 0.4f),
                        ),
                    ),
                    shape = RoundedCornerShape(18.dp),
                )
                .padding(16.dp),
        ) {
            content()
        }
    }
}
