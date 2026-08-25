package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * Root shell. The bottom companion dock is deliberately outside the gameplay area:
 * Eve never needs to sit on top of a word field, timer, or gameplay button.
 */
@Composable
fun GamePortalApp() {
    val context = LocalContext.current
    val store = remember { EveCompanionStore(context) }
    var eveOpen by remember { mutableStateOf(false) }
    var parkedRight by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) { RemoteExperience.refresh(context.applicationContext) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().navigationBarsPadding()) {
            Box(Modifier.weight(1f).fillMaxWidth()) { ClassicPremiumApp() }
            EveSafeDock(
                name = store.name,
                giftReady = store.giftAvailable(),
                parkedRight = parkedRight,
                onMascotTap = {
                    EveMascotRuntime.play(EveAnimationCue.IDLE_BREATHE, "Buradayım. 🌿")
                    parkedRight = !parkedRight
                },
                onOpen = { eveOpen = true },
            )
        }
        if (eveOpen) EveMascotScreen(onClose = { eveOpen = false })
    }
}

@Composable
private fun EveSafeDock(
    name: String,
    giftReady: Boolean,
    parkedRight: Boolean,
    onMascotTap: () -> Unit,
    onOpen: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(76.dp),
        color = Color(0xFFF6FFF9),
        border = BorderStroke(1.dp, Color(0xFFC6E9D7)),
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(listOf(Color(0xFFE7F8EF), Color.White, Color(0xFFEAF8FF))),
            ).padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Row(
                modifier = Modifier.align(if (parkedRight) Alignment.CenterEnd else Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (parkedRight) DockText(name, giftReady)
                Box(
                    Modifier.size(58.dp).background(Color.White, RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Eve3DStage(Modifier.fillMaxSize(), compact = true)
                    Box(Modifier.fillMaxSize().clickable(onClick = onMascotTap))
                }
                if (!parkedRight) DockText(name, giftReady)
                Button(
                    onClick = onOpen,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PortalBlue),
                    shape = RoundedCornerShape(16.dp),
                ) { Text(sh("Yanına git", "Visit"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
            }
        }
    }
}

@Composable
private fun DockText(name: String, giftReady: Boolean) {
    Column(horizontalAlignment = Alignment.End) {
        Text(name, color = PortalText, fontWeight = FontWeight.Black, fontSize = 12.sp)
        Text(if (giftReady) "🎁 ${sh("Hediye hazır", "Gift ready")}" else sh("Güvenli alanında", "In safe spot"), color = if (giftReady) Color(0xFFB57900) else PortalMuted, fontSize = 9.sp)
    }
}
