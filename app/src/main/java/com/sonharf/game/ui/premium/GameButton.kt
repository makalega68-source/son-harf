package com.sonharf.game.ui.premium

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.SonHarfTheme
import java.util.Locale

/**
 * Premium 3D game button (G5.0).
 * - Rounded 18dp, 4-5dp dark shoulder underneath.
 * - On press: sinks 4dp + shrinks to 96%, springs back on release.
 * - Text is BOLD UPPERCASE (Turkish-aware).
 * - Primary buttons min 64dp; secondary min 48dp.
 * - Locked state shows a lock icon and is not clickable.
 *
 * Effects are visual only; game state and server calls MUST NOT be gated by
 * animations. The button reports enabled clicks the moment they occur.
 */
@Composable
fun GameButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = SonHarfTheme.SonHarfOrange,
    primary: Boolean = false,
    enabled: Boolean = true,
    locked: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val minHeight: Dp = if (primary) 64.dp else 48.dp
    val cornerRadius = 18.dp
    val shoulder = 5.dp

    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && !locked) 0.96f else 1f,
        animationSpec = spring(),
        label = "gb-scale",
    )
    val sinkDp by animateFloatAsState(
        targetValue = if (pressed && enabled && !locked) 4f else 0f,
        animationSpec = spring(),
        label = "gb-sink",
    )

    val actualAccent = when {
        locked || !enabled -> Color(0xFF4A4258)
        else -> accent
    }
    val shoulderColor = actualAccent.copy(alpha = 0.55f).let { base ->
        Color(
            red = (base.red * 0.35f).coerceIn(0f, 1f),
            green = (base.green * 0.35f).coerceIn(0f, 1f),
            blue = (base.blue * 0.35f).coerceIn(0f, 1f),
            alpha = 1f,
        )
    }
    val topBrush = Brush.verticalGradient(
        listOf(
            actualAccent.copy(alpha = 1f),
            actualAccent.copy(alpha = 0.86f),
        ),
    )
    Box(modifier = modifier.heightIn(min = minHeight + shoulder)) {
        // Shoulder (drawn behind).
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = shoulder)
                .clip(RoundedCornerShape(cornerRadius))
                .background(shoulderColor),
        )
        // Face.
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = sinkDp.dp)
                .clip(RoundedCornerShape(cornerRadius))
                .background(topBrush)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled && !locked,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                if (locked) {
                    Text(
                        text = "🔒 ",
                        style = TextStyle(fontSize = 16.sp),
                        color = SonHarfTheme.PremiumTextPrimary,
                    )
                }
                Text(
                    text = text.uppercase(Locale("tr")),
                    style = TextStyle(
                        fontSize = if (primary) 18.sp else 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Center,
                    ),
                    color = if (locked || !enabled) Color.White.copy(alpha = 0.55f) else Color.White,
                )
            }
        }
    }
}
