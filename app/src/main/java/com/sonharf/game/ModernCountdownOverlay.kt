package com.sonharf.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ModernCountdownOverlay(
    value: Int,
    visible: Boolean = value >= 0,
) {
    if (!visible) return
    Box(
        Modifier.fillMaxSize().background(Color.White.copy(alpha = .34f)),
        contentAlignment = Alignment.Center,
    ) {
        key(value) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + scaleIn(initialScale = .55f),
                exit = fadeOut() + scaleOut(targetScale = 1.35f),
            ) {
                Surface(
                    modifier = Modifier.size(if (value > 0) 126.dp else 148.dp),
                    shape = CircleShape,
                    color = if (value > 0) PortalBlue else PortalGreen,
                    shadowElevation = 18.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            if (value > 0) value.toString() else "BAŞLA",
                            color = Color.White,
                            fontSize = if (value > 0) 58.sp else 25.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        }
    }
}
