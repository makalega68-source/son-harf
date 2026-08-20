package com.sonharf.game

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VipPurchaseDialog(onDismiss: () -> Unit) {
    var yearly by remember { mutableStateOf(true) }
    var notice by remember { mutableStateOf("") }
    val transition = rememberInfiniteTransition(label = "vipPulse")
    val pulse by transition.animateFloat(.94f, 1.08f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "vipScale")
    val glow by transition.animateFloat(.18f, .42f, infiniteRepeatable(tween(1250), RepeatMode.Reverse), label = "vipGlow")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SonHarfSurface,
        shape = RoundedCornerShape(28.dp),
        title = null,
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(86.dp).scale(pulse).background(
                        Brush.radialGradient(listOf(SonHarfGold, Color(0xFFFFE39B))), CircleShape,
                    ), contentAlignment = Alignment.Center,
                ) { Text("♛", color = Color(0xFF5D3A00), fontSize = 46.sp, fontWeight = FontWeight.Black) }
                Text("SON HARF VIP", fontSize = 25.sp, fontWeight = FontWeight.Black, color = SonHarfGold)
                Text("Daha güçlü görünüm, daha temiz deneyim.", color = SonHarfMuted, fontSize = 11.sp, textAlign = TextAlign.Center)

                Surface(
                    color = SonHarfGold.copy(alpha = glow),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, SonHarfGold.copy(alpha = .55f)),
                ) {
                    Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("✓ Reklamsız deneyim", fontWeight = FontWeight.Bold)
                        Text("✓ Özel oda oluşturma", fontWeight = FontWeight.Bold)
                        Text("✓ Altın çerçeve + özel kozmetikler", fontWeight = FontWeight.Bold)
                        Text("✓ Gelişmiş istatistikler", fontWeight = FontWeight.Bold)
                        Text("✓ Her ay 400 elmas", fontWeight = FontWeight.Bold)
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VipPlanCard("AYLIK", "VIP Monthly", !yearly, Modifier.weight(1f)) { yearly = false }
                    VipPlanCard("YILLIK", "VIP Yearly", yearly, Modifier.weight(1f)) { yearly = true }
                }

                Button(
                    onClick = {
                        notice = "Google Play ürünleri mağaza konsolunda yayımlandığında seçtiğin plan doğrudan ödeme ekranına bağlanır. VIP durumu sunucuda doğrulanır."
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SonHarfGold, contentColor = Color(0xFF3A2400)),
                    shape = RoundedCornerShape(17.dp),
                ) { Text("GOOGLE PLAY İLE DEVAM", fontWeight = FontWeight.Black) }
                if (notice.isNotBlank()) Text(notice, color = SonHarfMuted, fontSize = 9.sp, textAlign = TextAlign.Center)
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("KAPAT") } },
    )
}

@Composable
private fun VipPlanCard(title: String, subtitle: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = if (selected) SonHarfGold.copy(alpha = .16f) else SonHarfSurface2),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) SonHarfGold else SonHarfMuted.copy(alpha = .18f)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontWeight = FontWeight.Black, color = if (selected) SonHarfGold else SonHarfText)
            Text(subtitle, color = SonHarfMuted, fontSize = 8.sp)
        }
    }
}
