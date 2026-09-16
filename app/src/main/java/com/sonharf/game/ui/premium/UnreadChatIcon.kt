package com.sonharf.game.ui.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.SonHarfTheme

/**
 * G4.6 — Ana ekran üst çubuğuna konan sohbet ikonu.
 * [unreadCount] > 0 ise sağ üst köşede kırmızı rozet + sayı gösterir.
 * Sayı 99'u aşarsa "99+".
 */
@Composable
fun UnreadChatIcon(
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = SonHarfTheme.PremiumTextPrimary,
) {
    Box(modifier = modifier.size(44.dp)) {
        Icon(
            imageVector = Icons.Rounded.ChatBubbleOutline,
            contentDescription = "Sohbet",
            tint = tint,
            modifier = Modifier
                .align(Alignment.Center)
                .size(24.dp)
                .clickable(onClick = onClick),
        )
        if (unreadCount > 0) {
            val label = if (unreadCount > 99) "99+" else unreadCount.toString()
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-6).dp, y = 4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE85555))
                    .padding(horizontal = 5.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = TextStyle(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                    ),
                    color = Color.White,
                )
            }
        }
    }
}
