package com.sonharf.game

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

class WordSiegeCaptureMotionTest {
    @Test fun transformedCubeCenterUsesRealZoomPanAndViewportOrigin() {
        val transform = WordSiegeBoardTransform(
            scale = 2f,
            pan = Offset(10f, 20f),
            renderedWidthPx = 1_500f,
            renderedHeightPx = 1_500f,
        )
        val index = WordSiegeBoardSpec.index(1, 2)

        val center = wordSiegeCaptureCellCenterInWindow(
            index = index,
            transform = transform,
            cellSizePx = 50f,
            viewportOriginInWindow = Offset(100f, 200f),
        )

        assertEquals(360f, center.x, .001f)
        assertEquals(370f, center.y, .001f)
    }

    @Test fun arcStartsAndEndsExactlyAtRequestedCoordinates() {
        val start = Offset(25f, 400f)
        val end = Offset(180f, 40f)

        assertOffsetEquals(start, wordSiegeCaptureArcPoint(start, end, 0f))
        assertOffsetEquals(end, wordSiegeCaptureArcPoint(start, end, 1f))
    }

    @Test fun multipleCubesUseShortSequentialStagger() {
        assertEquals(0L, wordSiegeCaptureStaggerDelayMs(0))
        assertEquals(50L, wordSiegeCaptureStaggerDelayMs(1))
        assertEquals(150L, wordSiegeCaptureStaggerDelayMs(3))
    }

    private fun assertOffsetEquals(expected: Offset, actual: Offset) {
        assertEquals(expected.x, actual.x, .001f)
        assertEquals(expected.y, actual.y, .001f)
    }
}
