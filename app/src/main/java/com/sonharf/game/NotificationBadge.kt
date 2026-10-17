package com.sonharf.game

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The one notification badge used everywhere (My Games, chat, friends): a small ruby pill with a
 * fine white rim and a soft shadow that pops in when a count appears. Large and small sizes share
 * the same look.
 */
@Composable
internal fun NotificationBadge(count: Int, modifier: Modifier = Modifier, small: Boolean = false) {
    if (count <= 0) return
    val pop = remember { Animatable(.4f) }
    LaunchedEffect(count) {
        pop.snapTo(.6f)
        pop.animateTo(1f, spring(dampingRatio = .45f, stiffness = 520f))
    }
    val height: Dp = if (small) 16.dp else 22.dp
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
            .shadow(if (small) 2.dp else 4.dp, shape, clip = false, ambientColor = NotificationBadgeColors.Shadow, spotColor = NotificationBadgeColors.Shadow)
            .defaultMinSize(minWidth = height, minHeight = height)
            .background(Brush.verticalGradient(listOf(NotificationBadgeColors.Top, NotificationBadgeColors.Bottom)), shape)
            .border(if (small) 1.dp else 1.5.dp, Color.White, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (count > 9) "9+" else "$count",
            color = Color.White,
            fontSize = if (small) 9.sp else 12.sp,
            lineHeight = if (small) 10.sp else 13.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = if (small) 4.dp else 6.dp),
        )
    }
}

/** Places the shared badge on the top-right corner of its parent box. */
@Composable
internal fun BoxScope.CornerNotificationBadge(count: Int, small: Boolean = false, inset: Dp = 0.dp) {
    NotificationBadge(
        count,
        Modifier.align(Alignment.TopEnd).offset(x = if (small) inset else 6.dp + inset, y = if (small) -inset else (-6).dp - inset),
        small = small,
    )
}

private object NotificationBadgeColors {
    val Top = Color(0xFFFF5A6E)
    val Bottom = Color(0xFFD81B3C)
    val Shadow = Color(0xFF7A0018)
}

/**
 * "Seen" memory for list badges. A badge counts only items the player has not looked at yet:
 * opening the list marks everything in it as seen, and the badge comes back only for something
 * new (a new turn, a new invitation). Kept per user on the device.
 */
internal object SeenBadges {
    private var preferences: android.content.SharedPreferences? = null
    private val seen = mutableMapOf<String, Set<String>>()
    private val current = mutableMapOf<String, Set<String>>()
    var version by mutableStateOf(0)
        private set

    fun init(context: Context) {
        preferences = context.applicationContext.getSharedPreferences("seen_badges", Context.MODE_PRIVATE)
    }

    private fun stored(key: String): Set<String> = seen.getOrPut(key) {
        preferences?.getStringSet(key, emptySet())?.toSet().orEmpty()
    }

    /** The items waiting now; returns how many of them are still unseen. */
    fun report(key: String, items: Set<String>): Int {
        current[key] = items
        version++
        return unseen(key)
    }

    fun unseen(key: String): Int {
        val now = current[key].orEmpty()
        val old = stored(key)
        return now.count { it !in old }
    }

    /** Everything waiting now counts as seen. */
    fun markSeen(key: String) {
        val now = current[key].orEmpty()
        seen[key] = now
        preferences?.edit()?.putStringSet(key, now)?.apply()
        version++
    }
}
