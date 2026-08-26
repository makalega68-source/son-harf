package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val PortalBg = Color(0xFFF4FBFF)
internal val PortalCard = Color.White
internal val PortalText = Color(0xFF173B57)
internal val PortalMuted = Color(0xFF6D879A)
internal val PortalBlue = Color(0xFF24AEE4)
internal val PortalGold = Color(0xFFFFC857)
internal val PortalGreen = Color(0xFF32C985)
internal val PortalRed = Color(0xFFFF7891)

/**
 * Root shell. Eve's companion dock stays outside gameplay so it never covers the word field,
 * timer or match actions. The dock renders the same real GLB as the room and deliberately avoids
 * a second navigation track: Eve should read as a living companion, not as another nav icon.
 */
@Composable
fun GamePortalApp() {
    val context = LocalContext.current
    val store = remember { EveCompanionStore(context) }

    DisposableEffect(Unit) {
        EveMascotRuntime.startLivingBehavior()
        onDispose { EveMascotRuntime.stopLivingBehavior() }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        RemoteExperience.refresh(context.applicationContext)
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier.weight(1f).fillMaxWidth()
                    .consumeWindowInsets(androidx.compose.foundation.layout.WindowInsets.navigationBars),
            ) {
                ClassicPremiumApp()
            }
            if (!EveLivingRoomRuntime.open) {
                EveTravelDock(
                    store = store,
                    onOpen = { EveLivingRoomRuntime.show() },
                )
            }
        }

        if (EveLivingRoomRuntime.open) {
            Box(Modifier.fillMaxSize()) {
                EveForestScreen(onNavigateBack = { EveLivingRoomRuntime.hide() })
            }
        }
    }
}

@Composable
private fun EveTravelDock(store: EveCompanionStore, onOpen: () -> Unit) {
    val target = store.xpToNextLevel.coerceAtLeast(1)
    Surface(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
        color = Color(0xFFF8FFFA),
        border = BorderStroke(1.dp, Color(0xFFBEE8CF)),
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(94.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFFEAF9F0), Color.White, Color(0xFFEAF8FF)),
                    ),
                )
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier
                    .size(80.dp)
                    .clickable(onClick = onOpen),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFFE4F7EA),
                border = BorderStroke(2.dp, PortalGreen),
                shadowElevation = 4.dp,
            ) {
                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp))) {
                    EveLive3DStage(
                        modifier = Modifier.fillMaxSize(),
                        compact = true,
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    "${store.name} ${sh("seninle", "is with you")} 🐾",
                    color = PortalText,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.5.sp,
                )
                Text(
                    "${sh("Dostluk Sv.", "Friendship Lv.")} ${store.friendshipLevel}  •  ${store.affection}/$target XP",
                    color = PortalMuted,
                    fontSize = 8.5.sp,
                )
                Spacer(Modifier.height(5.dp))
                LinearProgressIndicator(
                    progress = { (store.affection / target.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                    color = PortalGreen,
                    trackColor = Color(0xFFD9F1E2),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    sh("Canlı • seni izliyor", "Live • watching with you"),
                    color = PortalGreen,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.width(8.dp))

            Button(
                onClick = onOpen,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PortalBlue),
                shape = RoundedCornerShape(15.dp),
            ) {
                Text(
                    sh("Yanına git", "Visit"),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.5.sp,
                )
            }
        }
    }
}
