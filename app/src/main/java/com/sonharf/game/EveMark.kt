package com.sonharf.game

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
internal fun EveMark(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val face = Path().apply {
            moveTo(w * .18f, h * .28f)
            lineTo(w * .08f, h * .05f)
            lineTo(w * .35f, h * .18f)
            quadraticBezierTo(w * .50f, h * .10f, w * .65f, h * .18f)
            lineTo(w * .92f, h * .05f)
            lineTo(w * .82f, h * .28f)
            quadraticBezierTo(w * .90f, h * .62f, w * .50f, h * .92f)
            quadraticBezierTo(w * .10f, h * .62f, w * .18f, h * .28f)
            close()
        }
        drawPath(face, Color(0xFFFFD28B))
        drawPath(face, Color(0xFF173B57), style = Stroke(width = w * .055f))
        drawCircle(Color(0xFF173B57), w * .045f, Offset(w * .36f, h * .48f))
        drawCircle(Color(0xFF173B57), w * .045f, Offset(w * .64f, h * .48f))
        val muzzle = Path().apply {
            moveTo(w * .35f, h * .62f)
            quadraticBezierTo(w * .50f, h * .76f, w * .65f, h * .62f)
            quadraticBezierTo(w * .50f, h * .88f, w * .35f, h * .62f)
        }
        drawPath(muzzle, Color.White.copy(alpha = .9f))
        drawCircle(Color(0xFF173B57), w * .045f, Offset(w * .50f, h * .67f))
        drawCircle(Color(0xFF8CF7FF), w * .035f, Offset(w * .50f, h * .22f))
    }
}
