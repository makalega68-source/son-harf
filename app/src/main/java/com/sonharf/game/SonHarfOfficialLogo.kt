package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
 * Premium Son Harf wordmark used by the botanical identity.
 * Pure Compose keeps startup safe while reproducing the layered S/H tiles,
 * dark-green lettering, warm gold edging and small botanical flourish.
 */
@Composable
fun SonHarfOfficialLogo(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        val h = maxHeight
        val tile = h * .78f
        val corner = (maxHeight.value * .18f).coerceIn(9f, 20f).dp
        val letterSize = (maxHeight.value * .43f).coerceIn(20f, 54f).sp
        val wordSize = (maxHeight.value * .36f).coerceIn(18f, 43f).sp

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
            // Soft botanical flourish behind the letter tiles.
            Canvas(Modifier.fillMaxHeight().width(tile * 1.70f)) {
                val stem = Path().apply {
                    moveTo(size.width * .08f, size.height * .78f)
                    cubicTo(size.width * .24f, size.height * .52f, size.width * .38f, size.height * .38f, size.width * .61f, size.height * .18f)
                }
                drawPath(stem, Color(0xFF3F7659).copy(alpha = .88f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.2f))
                listOf(
                    Triple(.18f, .62f, -28f), Triple(.29f, .48f, 30f),
                    Triple(.42f, .34f, -30f), Triple(.55f, .21f, 32f),
                ).forEach { (x, y, a) ->
                    val center = Offset(size.width * x, size.height * y)
                    rotate(a, center) {
                        drawOval(
                            brush = Brush.linearGradient(listOf(Color(0xFF2F6A4E), Color(0xFF6EA47D))),
                            topLeft = center - Offset(size.width * .075f, size.height * .055f),
                            size = Size(size.width * .15f, size.height * .11f),
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(tile * 1.58f).fillMaxHeight()) {
                    SonHarfMonogramTile(
                        letter = "H",
                        modifier = Modifier
                            .width(tile)
                            .fillMaxHeight(.74f)
                            .align(Alignment.CenterEnd),
                        corner = corner,
                        letterSize = letterSize.value * .83f,
                        rotation = 4f,
                    )
                    SonHarfMonogramTile(
                        letter = "S",
                        modifier = Modifier
                            .width(tile)
                            .fillMaxHeight(.88f)
                            .align(Alignment.CenterStart),
                        corner = corner,
                        letterSize = letterSize.value,
                        rotation = -3f,
                    )
                }
                Text(
                    text = "Son Harf",
                    modifier = Modifier.padding(start = 4.dp),
                    color = Color(0xFF315C48),
                    fontSize = wordSize,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = (-.7).sp,
                    maxLines = 1,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color(0xFFB08A42).copy(alpha = .44f),
                            offset = Offset(1.1f, 1.3f),
                            blurRadius = 1.1f,
                        ),
                    ),
                )
            }
        }
    }
}

@Composable
private fun SonHarfMonogramTile(
    letter: String,
    modifier: Modifier,
    corner: androidx.compose.ui.unit.Dp,
    letterSize: Float,
    rotation: Float,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(corner),
        color = Color(0xFFFFFBED),
        border = BorderStroke(1.6.dp, Color(0xFFAA8743)),
        shadowElevation = 6.dp,
        tonalElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(3.dp),
            contentAlignment = Alignment.Center,
        ) {
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
