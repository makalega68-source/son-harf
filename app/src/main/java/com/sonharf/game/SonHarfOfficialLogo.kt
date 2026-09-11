package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Main Kelime Kuşatması wordmark. The legacy function name is intentionally retained
 * so existing startup/navigation call sites remain binary- and source-compatible.
 */
@Composable
fun SonHarfOfficialLogo(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        val h = maxHeight
        val tile = h * .78f
        val corner = (maxHeight.value * .18f).coerceIn(9f, 20f).dp
        val letterSize = (maxHeight.value * .37f).coerceIn(18f, 48f).sp
        val wordSize = (maxHeight.value * .24f).coerceIn(13f, 29f).sp

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
            Canvas(Modifier.fillMaxHeight().width(tile * 1.85f)) {
                val stem = Path().apply {
                    moveTo(size.width * .08f, size.height * .80f)
                    cubicTo(size.width * .26f, size.height * .56f, size.width * .42f, size.height * .36f, size.width * .68f, size.height * .14f)
                }
                drawPath(stem, Color(0xFF4E8069).copy(alpha = .82f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.2f))
                listOf(
                    Triple(.19f, .64f, -28f), Triple(.31f, .49f, 30f),
                    Triple(.46f, .32f, -30f), Triple(.61f, .19f, 32f),
                ).forEach { (x, y, a) ->
                    val center = Offset(size.width * x, size.height * y)
                    rotate(a, center) {
                        drawOval(
                            brush = Brush.linearGradient(listOf(Color(0xFF477B64), Color(0xFF86B19A))),
                            topLeft = center - Offset(size.width * .075f, size.height * .055f),
                            size = Size(size.width * .15f, size.height * .11f),
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(tile * 1.58f).fillMaxHeight()) {
                    SiegeMonogramTile(
                        letter = "K",
                        modifier = Modifier.width(tile).fillMaxHeight(.74f).align(Alignment.CenterEnd),
                        corner = corner,
                        letterSize = letterSize.value * .83f,
                    )
                    SiegeMonogramTile(
                        letter = "K",
                        modifier = Modifier.width(tile).fillMaxHeight(.88f).align(Alignment.CenterStart),
                        corner = corner,
                        letterSize = letterSize.value,
                    )
                }
                Column(Modifier.padding(start = 4.dp)) {
                    Text(
                        text = "Kelime",
                        color = Color(0xFF315C48),
                        fontSize = wordSize,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Serif,
                        letterSpacing = (-.5).sp,
                        maxLines = 1,
                    )
                    Text(
                        text = "Kuşatması",
                        color = Color(0xFF315C48),
                        fontSize = wordSize,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Serif,
                        letterSpacing = (-.5).sp,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun SiegeMonogramTile(
    letter: String,
    modifier: Modifier,
    corner: androidx.compose.ui.unit.Dp,
    letterSize: Float,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(corner),
        color = Color(0xFFF8F3E7),
        border = BorderStroke(1.6.dp, Color(0xFF71947F)),
        shadowElevation = 5.dp,
        tonalElevation = 0.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(3.dp), contentAlignment = Alignment.Center) {
            Text(
                text = letter,
                color = Color(0xFF285B45),
                fontSize = letterSize.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Serif,
            )
        }
    }
}
