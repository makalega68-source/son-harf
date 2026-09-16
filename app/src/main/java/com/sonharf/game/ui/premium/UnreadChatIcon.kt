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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.SonHarfTheme
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.getMyUnreadChatCount
import com.sonharf.game.data.markMyChatRead
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * G4.6 — Ana ekran üst çubuğuna konan sohbet ikonu.
 *
 * Existing callers may still pass a positive [unreadCount]. A zero value is
 * treated as "use the server-owned unread cursor", which removes the old
 * hard-coded-zero behavior without changing the home-screen API.
 */
@Composable
fun UnreadChatIcon(
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = SonHarfTheme.PremiumTextPrimary,
) {
    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var serverUnreadCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(unreadCount) {
        if (unreadCount > 0 || !SupabaseProvider.configured) return@LaunchedEffect
        while (true) {
            runCatching { backend.getMyUnreadChatCount() }
                .onSuccess { serverUnreadCount = it.coerceIn(0, 999) }
            delay(2_500)
        }
    }

    val displayCount = if (unreadCount > 0) unreadCount else serverUnreadCount
    val openChat = {
        serverUnreadCount = 0
        if (SupabaseProvider.configured) {
            scope.launch { runCatching { backend.markMyChatRead() } }
        }
        onClick()
    }

    Box(modifier = modifier.size(44.dp)) {
        Icon(
            imageVector = Icons.Rounded.ChatBubbleOutline,
            contentDescription = "Sohbet",
            tint = tint,
            modifier = Modifier
                .align(Alignment.Center)
                .size(24.dp)
                .clickable(onClick = openChat),
        )
        if (displayCount > 0) {
            val label = if (displayCount > 99) "99+" else displayCount.toString()
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
