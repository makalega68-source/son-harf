package com.sonharf.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * High-fidelity approval candidate recreated from the supplied home reference.
 * It is intentionally real Compose code in main source and is only routed by the
 * debug-only preview Activity until explicit approval.
 */
@Composable
fun ReferenceHomeV2Screen(modifier: Modifier = Modifier) {
    val forest = Color(0xFF115A43)
    val text = Color(0xFF173A2E)
    val muted = Color(0xFF6A7E73)
    val gold = Color(0xFFE1B857)

    Box(modifier.fillMaxSize().background(Color(0xFFF9FAF5))) {
        ReferenceBotanicalV2(Modifier.matchParentSize())
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 17.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            // Header: use the exact system-loaded logo asset, enlarged.
            Row(Modifier.fillMaxWidth().height(92.dp), verticalAlignment = Alignment.Top) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.son_harf_app_icon_master),
                        contentDescription = "Son Harf",
                        modifier = Modifier.size(82.dp),
                        contentScale = ContentScale.Fit,
                    )
                    Column(Modifier.padding(start = 2.dp, top = 4.dp)) {
                        Text(
                            "Son Harf",
                            color = Color(0xFF285C46),
                            fontSize = 29.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Serif,
                            letterSpacing = (-1.0).sp,
                        )
                        Text(
                            "Kelimeyi Sürdür, Rakibini Geç",
                            color = Color(0xFF4C7864),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        RefCircleAction(Icons.Rounded.Notifications)
                        RefCircleAction(Icons.Rounded.WorkspacePremium)
                    }
                    Text(
                        "Her kelime\nyeni bir meydan okuma!",
                        color = Color(0xFF315D49),
                        fontSize = 8.sp,
                        lineHeight = 8.7.sp,
                        textAlign = TextAlign.End,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }

            // Player card.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(145.dp)
                    .shadow(11.dp, RoundedCornerShape(25.dp))
                    .clip(RoundedCornerShape(25.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF4D806B), Color(0xFF4E7580), Color(0xFF6B6C8C))))
                    .border(1.dp, Color.White.copy(alpha = .20f), RoundedCornerShape(25.dp)),
            ) {
                Canvas(Modifier.matchParentSize()) {
                    drawCircle(Color.White.copy(alpha = .035f), size.minDimension * .52f, Offset(size.width * .94f, size.height * .78f))
                    drawCircle(Color(0xFF153A2F).copy(alpha = .06f), size.minDimension * .37f, Offset(size.width * .05f, size.height * .94f))
                    // faint crown watermark, like the reference card.
                    val crownY = size.height * .70f
                    val crownX = size.width * .88f
                    val p = Path().apply {
                        moveTo(crownX - 28f, crownY + 18f)
                        lineTo(crownX - 20f, crownY - 4f)
                        lineTo(crownX - 8f, crownY + 9f)
                        lineTo(crownX + 2f, crownY - 11f)
                        lineTo(crownX + 14f, crownY + 9f)
                        lineTo(crownX + 27f, crownY - 3f)
                        lineTo(crownX + 31f, crownY + 18f)
                        close()
                    }
                    drawPath(p, Color.White.copy(alpha = .045f))
                }
                Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = Color(0xFFDFF8EF), border = androidx.compose.foundation.BorderStroke(2.5.dp, Color(0xFF9AF2DE)), shadowElevation = 4.dp) {
                            Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
                                Text("Ü", color = Color(0xFF285E4D), fontSize = 25.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Ümit", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.EmojiEvents, null, tint = Color(0xFFF5C84F), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("612 RP", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Surface(shape = RoundedCornerShape(99.dp), color = Color(0xFFFFFCF3), shadowElevation = 2.dp) {
                            Row(Modifier.padding(horizontal = 13.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.WorkspacePremium, null, tint = Color(0xFF8A6430), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("SC 1110", color = Color(0xFF795628), fontSize = 15.sp, fontWeight = FontWeight.Black)
                                Spacer(Modifier.width(2.dp))
                                Icon(Icons.Rounded.ChevronRight, null, tint = Color(0xFF90744B), modifier = Modifier.size(17.dp))
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        RefMetric("1", "GALİBİYET", Modifier.weight(1f)); RefMetricDivider()
                        RefMetric("29", "MAĞLUBİYET", Modifier.weight(1f)); RefMetricDivider()
                        RefMetric("PRO", "ÜYELİK", Modifier.weight(1f))
                    }
                }
            }

            // Primary CTA.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(66.dp)
                    .shadow(8.dp, RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF1A684F), forest))),
                contentAlignment = Alignment.Center,
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(34.dp))
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("OYNA", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
                        Text("Premier 1v1 kelime düellosu", color = Color.White.copy(alpha = .88f), fontSize = 9.2.sp)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = Color.White.copy(alpha = .90f), modifier = Modifier.size(27.dp))
                }
            }

            // Rich weekly podium.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(257.dp)
                    .shadow(10.dp, RoundedCornerShape(25.dp))
                    .clip(RoundedCornerShape(25.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF103C33), Color(0xFF154B40), Color(0xFF173A40))))
                    .border(1.dp, gold.copy(alpha = .70f), RoundedCornerShape(25.dp)),
            ) {
                RefPodiumAtmosphere(Modifier.matchParentSize())
                Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(13.dp), color = gold.copy(alpha = .14f), border = androidx.compose.foundation.BorderStroke(1.dp, gold.copy(alpha = .38f))) {
                            Icon(Icons.Rounded.EmojiEvents, null, tint = Color(0xFFF3D271), modifier = Modifier.padding(8.dp).size(21.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("HAFTANIN ZİRVESİ", color = Color.White, fontSize = 17.5.sp, fontWeight = FontWeight.Black)
                            Text("Bu haftanın en güçlü oyuncuları", color = Color.White.copy(alpha = .70f), fontSize = 8.6.sp)
                        }
                        Surface(shape = RoundedCornerShape(99.dp), color = Color.White.copy(alpha = .055f), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .17f))) {
                            Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("TÜMÜ", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Icon(Icons.Rounded.ChevronRight, null, tint = gold, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.Bottom) {
                        RefStage(2, "KaanY", "498 RP", Color(0xFFDCE8F0), Color(0xFF8FA7B6), 116.dp, Modifier.weight(.93f))
                        RefStage(1, "WordMaster", "732 RP", Color(0xFFFFD96B), Color(0xFFA56B10), 143.dp, Modifier.weight(1.16f), true)
                        RefStage(3, "LaleS", "410 RP", Color(0xFFE6A47A), Color(0xFF8B4D2C), 108.dp, Modifier.weight(.93f))
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Text("DİĞER OYUNLAR", color = text, fontSize = 16.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                Text("Daha fazla kelime, daha fazla eğlence!", color = muted, fontSize = 8.sp)
            }
            Row(Modifier.fillMaxWidth().height(103.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                RefGameCard(R.drawable.kelime_kusatma_logo_hd, "KELİME\nKUŞATMASI", "Alanı ele geçir,\nküpleri koru", listOf(Color(0xFF9EDAC3), Color(0xFFF0EBCB)), Modifier.weight(1f))
                RefGameCard(R.drawable.harf_yolu_logo, "KELİME YOLU", "Her kelime\nseni hedefe yaklaştırır", listOf(Color(0xFF96D7EE), Color(0xFFDDF2DE)), Modifier.weight(1f))
            }

            Spacer(Modifier.weight(1f))
            RefBottomNav()
        }
    }
}

@Composable private fun RefCircleAction(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(shape = CircleShape, color = Color(0xFFFBFCF7), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBCDC1))) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color(0xFF224C3B), modifier = Modifier.size(21.dp)) }
    }
}

@Composable private fun RefMetric(value: String, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Text(label, color = Color.White.copy(alpha = .77f), fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
    }
}
@Composable private fun RefMetricDivider() { Box(Modifier.width(1.dp).height(35.dp).background(Color.White.copy(alpha = .17f))) }

@Composable private fun RefPodiumAtmosphere(modifier: Modifier) {
    Canvas(modifier) {
        // Central warm spotlight and floor glow.
        drawCircle(Brush.radialGradient(listOf(Color(0x66FFD96D), Color(0x162F765F), Color.Transparent)), size.minDimension * .48f, Offset(size.width * .50f, size.height * .72f))
        drawOval(Brush.radialGradient(listOf(Color(0x66FFC94D), Color.Transparent)), Offset(size.width * .27f, size.height * .77f), Size(size.width * .46f, size.height * .18f))
        // Top spotlights.
        drawRect(Brush.verticalGradient(listOf(Color(0x38FFE6A2), Color.Transparent)), Offset(size.width * .20f, size.height * .18f), Size(size.width * .04f, size.height * .30f))
        drawRect(Brush.verticalGradient(listOf(Color(0x38FFE6A2), Color.Transparent)), Offset(size.width * .76f, size.height * .18f), Size(size.width * .04f, size.height * .30f))
        // Confetti rectangles/diamonds.
        val pts = listOf(.08f to .36f, .16f to .47f, .27f to .31f, .35f to .42f, .62f to .29f, .71f to .39f, .82f to .33f, .91f to .46f, .56f to .35f)
        pts.forEachIndexed { i, p ->
            val c = if (i % 2 == 0) Color(0xFFFFD65D) else Color(0xFFF1B94B)
            rotate(if (i % 2 == 0) 28f else -24f, Offset(size.width * p.first, size.height * p.second)) {
                drawRoundRect(c.copy(alpha = .92f), Offset(size.width * p.first - 4f, size.height * p.second - 7f), Size(8f, 14f), androidx.compose.ui.geometry.CornerRadius(2f, 2f))
            }
        }
    }
}

@Composable private fun RefStage(
    place: Int,
    name: String,
    rp: String,
    accent: Color,
    deep: Color,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier,
    champion: Boolean = false,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
        if (champion) {
            Icon(Icons.Rounded.Crown, null, tint = Color(0xFFFFD857), modifier = Modifier.size(29.dp))
            Spacer(Modifier.height((-2).dp))
        }
        Surface(shape = CircleShape, color = Color(0xFF153A32), border = androidx.compose.foundation.BorderStroke(if (champion) 3.dp else 2.2.dp, accent), shadowElevation = if (champion) 9.dp else 5.dp) {
            Box(Modifier.size(if (champion) 49.dp else 40.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Person, null, tint = Color.White.copy(alpha = .78f), modifier = Modifier.size(if (champion) 28.dp else 23.dp))
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(name, color = Color.White, fontSize = if (champion) 9.7.sp else 8.sp, fontWeight = FontWeight.Black)
        Text(rp, color = accent, fontSize = if (champion) 9.2.sp else 7.6.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(3.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(height)
                .shadow(if (champion) 8.dp else 4.dp, RoundedCornerShape(topStart = 17.dp, topEnd = 17.dp))
                .clip(RoundedCornerShape(topStart = 17.dp, topEnd = 17.dp))
                .background(Brush.verticalGradient(listOf(accent.copy(alpha = .66f), deep.copy(alpha = .48f), Color(0xFF102F2A).copy(alpha = .92f))))
                .border(1.2.dp, accent.copy(alpha = .86f), RoundedCornerShape(topStart = 17.dp, topEnd = 17.dp)),
            contentAlignment = Alignment.TopCenter,
        ) {
            Canvas(Modifier.matchParentSize()) {
                drawOval(Brush.radialGradient(listOf(accent.copy(alpha = .40f), Color.Transparent)), Offset(size.width * .05f, -size.height * .04f), Size(size.width * .90f, size.height * .28f))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 7.dp)) {
                Text("$place", color = accent, fontSize = if (champion) 21.sp else 17.sp, fontWeight = FontWeight.Black)
                if (champion) {
                    Spacer(Modifier.height(2.dp))
                    Surface(shape = RoundedCornerShape(99.dp), color = Color(0xFF5B3C0E).copy(alpha = .66f), border = androidx.compose.foundation.BorderStroke(.7.dp, accent.copy(alpha = .75f))) {
                        Text("★ ŞAMPİYON ★", color = Color(0xFFFFE293), fontSize = 5.8.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
        }
    }
}

@Composable private fun RefGameCard(res: Int, title: String, subtitle: String, colors: List<Color>, modifier: Modifier) {
    Row(
        modifier
            .shadow(3.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(colors))
            .border(1.dp, Color.White.copy(alpha = .78f), RoundedCornerShape(18.dp))
            .padding(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1.02f).fillMaxHeight().clip(RoundedCornerShape(13.dp)).background(Color.White.copy(alpha = .24f)), contentAlignment = Alignment.Center) {
            Image(painterResource(res), null, Modifier.fillMaxSize().padding(0.dp), contentScale = ContentScale.Fit)
        }
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(.98f)) {
            Text(title, color = Color(0xFF173A2E), fontSize = 10.2.sp, lineHeight = 10.7.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = Color(0xFF566D62), fontSize = 6.6.sp, lineHeight = 8.2.sp)
            Spacer(Modifier.height(6.dp))
            Surface(shape = RoundedCornerShape(99.dp), color = Color(0xFFFFFDF6).copy(alpha = .96f)) {
                Row(Modifier.padding(horizontal = 8.dp, vertical = 4.5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Hemen Oyna", color = Color(0xFF355D4D), fontSize = 6.2.sp, fontWeight = FontWeight.Black)
                    Icon(Icons.Rounded.ChevronRight, null, tint = Color(0xFF8A6D38), modifier = Modifier.size(10.dp))
                }
            }
        }
    }
}

@Composable private fun RefBottomNav() {
    Surface(shape = RoundedCornerShape(23.dp), color = Color(0xFFF2F6F0).copy(alpha = .99f), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDE5DF))) {
        Row(Modifier.fillMaxWidth().height(61.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(Icons.Rounded.Home to "ANA", Icons.Rounded.EmojiEvents to "LİG", Icons.Rounded.Groups to "SOSYAL", Icons.Rounded.Storefront to "MAĞAZA", Icons.Rounded.Person to "PROFİL").forEachIndexed { index, item ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (index == 0) Surface(shape = RoundedCornerShape(99.dp), color = Color(0xFFDDE9E1)) {
                        Icon(item.first, null, tint = Color(0xFF3F745E), modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp).size(21.dp))
                    } else Icon(item.first, null, tint = Color(0xFF657A70), modifier = Modifier.size(20.dp))
                    Text(item.second, color = if (index == 0) Color(0xFF3F745E) else Color(0xFF657A70), fontSize = 7.sp, fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable private fun ReferenceBotanicalV2(modifier: Modifier) {
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFFFFFEF8), Color(0xFFFBFCF6), Color(0xFFE2F1E5))))
        val wave = Path().apply {
            moveTo(0f, size.height * .68f)
            cubicTo(size.width * .18f, size.height * .60f, size.width * .58f, size.height * .86f, size.width, size.height * .71f)
            lineTo(size.width, size.height); lineTo(0f, size.height); close()
        }
        drawPath(wave, Brush.verticalGradient(listOf(Color(0x1878BB98), Color(0x4A7FC3A0)), startY = size.height * .63f, endY = size.height))
        fun leaf(cx: Float, cy: Float, w: Float, h: Float, a: Float, angle: Float) {
            val center = Offset(size.width * cx, size.height * cy)
            rotate(angle, center) {
                drawOval(Brush.linearGradient(listOf(Color(0xFF4F8B6F).copy(alpha = a), Color(0xFFB3D6BC).copy(alpha = a * .8f))), center - Offset(w / 2, h / 2), Size(w, h))
            }
        }
        leaf(.03f, .13f, 54f, 22f, .12f, 24f); leaf(.08f, .17f, 68f, 25f, .10f, -22f)
        leaf(.73f, .022f, 72f, 27f, .12f, 16f); leaf(.94f, .16f, 58f, 22f, .10f, -30f)
        leaf(.03f, .56f, 72f, 25f, .10f, 22f); leaf(.93f, .60f, 78f, 27f, .11f, -24f)
        leaf(.17f, .92f, 63f, 22f, .10f, 18f); leaf(.77f, .90f, 66f, 23f, .08f, -24f)
    }
}
