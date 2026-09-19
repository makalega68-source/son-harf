package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.hypot

class ProfileFrameVisualGeometryContractTest {
    private data class FrameCase(
        val fileName: String,
        val configuredPhotoDiameterPx: Int,
    )

    private val frames = listOf(
        FrameCase("profile_frame_default_gray.png", 301),
        FrameCase("profile_frame_pro_gold.png", 312),
        FrameCase("profile_frame_shop_pink_blossom.png", 316),
        FrameCase("profile_frame_shop_blue_royal.png", 333),
        FrameCase("profile_frame_shop_amethyst.png", 316),
        FrameCase("profile_frame_shop_emerald.png", 324),
    )

    @Test
    fun allFramePngsAreTrue512SquareAssetsWithTransparentCanvas() {
        frames.forEach { frame ->
            val image = ImageIO.read(File("src/main/res/drawable/${frame.fileName}"))
            assertEquals("${frame.fileName} width", 512, image.width)
            assertEquals("${frame.fileName} height", 512, image.height)
            assertTrue("${frame.fileName} must preserve alpha", image.colorModel.hasAlpha())

            val corners = listOf(
                0 to 0,
                511 to 0,
                0 to 511,
                511 to 511,
            )
            corners.forEach { (x, y) ->
                assertTrue(
                    "${frame.fileName} corner must stay transparent at $x,$y",
                    alpha(image.getRGB(x, y)) <= 8,
                )
            }
            assertTrue(
                "${frame.fileName} center must remain transparent for the avatar",
                alpha(image.getRGB(256, 256)) <= 8,
            )
        }
    }

    @Test
    fun configuredAvatarCircleFitsInsideTransparentOpeningWithoutDirtyOverlay() {
        frames.forEach { frame ->
            val image = ImageIO.read(File("src/main/res/drawable/${frame.fileName}"))
            val radius = frame.configuredPhotoDiameterPx / 2.0
            var pixels = 0
            var contaminated = 0
            var strongContamination = 0

            for (y in 0 until 512) {
                for (x in 0 until 512) {
                    // Keep a 3 px anti-alias safety inset from the intended photo edge.
                    if (hypot(x - 255.5, y - 255.5) <= radius - 3.0) {
                        val a = alpha(image.getRGB(x, y))
                        pixels++
                        if (a > 24) contaminated++
                        if (a > 96) strongContamination++
                    }
                }
            }

            val contaminationRate = contaminated.toDouble() / pixels
            val strongRate = strongContamination.toDouble() / pixels
            assertTrue(
                "${frame.fileName} overlays too much of the profile photo: ${(contaminationRate * 100).format2()}%",
                contaminationRate <= 0.015,
            )
            assertTrue(
                "${frame.fileName} has strong opaque intrusion over the profile photo: ${(strongRate * 100).format2()}%",
                strongRate <= 0.003,
            )
        }
    }

    @Test
    fun artworkHasOuterPaddingSoDecorationsAreNotVisiblyClipped() {
        frames.forEach { frame ->
            val image = ImageIO.read(File("src/main/res/drawable/${frame.fileName}"))
            var edgePixels = 0
            var visibleEdgePixels = 0
            val border = 2

            for (y in 0 until 512) {
                for (x in 0 until 512) {
                    if (x < border || y < border || x >= 512 - border || y >= 512 - border) {
                        edgePixels++
                        if (alpha(image.getRGB(x, y)) > 24) visibleEdgePixels++
                    }
                }
            }

            val edgeOccupancy = visibleEdgePixels.toDouble() / edgePixels
            assertTrue(
                "${frame.fileName} artwork reaches the canvas edge and may look clipped: ${(edgeOccupancy * 100).format2()}%",
                edgeOccupancy <= 0.01,
            )
        }
    }

    @Test
    fun eachAssetActuallyContainsVisibleFrameArtwork() {
        frames.forEach { frame ->
            val image = ImageIO.read(File("src/main/res/drawable/${frame.fileName}"))
            var visible = 0
            var total = 0
            for (y in 0 until 512 step 2) {
                for (x in 0 until 512 step 2) {
                    total++
                    if (alpha(image.getRGB(x, y)) > 64) visible++
                }
            }
            val visibleRate = visible.toDouble() / total
            assertTrue(
                "${frame.fileName} does not contain enough visible frame artwork: ${(visibleRate * 100).format2()}%",
                visibleRate >= 0.03,
            )
        }
    }

    private fun alpha(argb: Int): Int = (argb ushr 24) and 0xFF

    private fun Double.format2(): String = String.format(java.util.Locale.US, "%.2f", this)
}
