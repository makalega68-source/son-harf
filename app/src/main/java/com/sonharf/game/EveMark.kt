package com.sonharf.game

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap

@Composable
internal fun EveMark(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val ink = Color(0xFF173B57)
        val fur = Color(0xFFFFD28B)
        val cream = Color.White.copy(alpha = .92f)
        val stroke = w * .055f

        drawCircle(fur, w * .34f, Offset(w * .50f, h * .53f))
        drawCircle(fur, w * .17f, Offset(w * .23f, h * .25f))
        drawCircle(fur, w * .17f, Offset(w * .77f, h * .25f))
        drawCircle(ink, w * .045f, Offset(w * .36f, h * .48f))
        drawCircle(ink, w * .045f, Offset(w * .64f, h * .48f))
        drawCircle(cream, w * .16f, Offset(w * .50f, h * .67f))
        drawCircle(ink, w * .045f, Offset(w * .50f, h * .64f))
        drawCircle(Color(0xFF8CF7FF), w * .035f, Offset(w * .50f, h * .24f))

        drawLine(ink, Offset(w * .09f, h * .07f), Offset(w * .27f, h * .35f), stroke, StrokeCap.Round)
        drawLine(ink, Offset(w * .91f, h * .07f), Offset(w * .73f, h * .35f), stroke, StrokeCap.Round)
        drawLine(ink, Offset(w * .31f, h * .78f), Offset(w * .50f, h * .91f), stroke, StrokeCap.Round)
        drawLine(ink, Offset(w * .69f, h * .78f), Offset(w * .50f, h * .91f), stroke, StrokeCap.Round)
    }
}
