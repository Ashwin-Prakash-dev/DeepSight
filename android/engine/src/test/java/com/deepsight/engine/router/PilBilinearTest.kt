package com.deepsight.engine.router

import com.deepsight.engine.quality.PixelImage
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

/**
 * The router was trained on Pillow's BILINEAR resize, so the app must produce the same pixels. The references in
 * ml/router/golden were made with Pillow 12.3.0 from [pattern] (ml/router/README.md shows how).
 */
class PilBilinearTest {
    private val golden = File(requireNotNull(System.getProperty("contractsDir"))).parentFile.resolve("ml/router/golden")

    @Test
    fun largeDownscaleMatchesPillowExactly() {
        // 4.7x, as a phone photo is shrunk to the router's 256-pixel short side.
        assertSamePixels(read("pil_bilinear_1600x1200_to_341x256.png"), PilBilinear.resize(pattern(1600, 1200), 341, 256))
    }

    @Test
    fun upscaleMatchesPillowExactly() {
        assertSamePixels(read("pil_bilinear_120x300_to_256x640.png"), PilBilinear.resize(pattern(120, 300), 256, 640))
    }

    @Test
    fun sameSizeIsAnUnchangedCopy() {
        val image = pattern(13, 7)
        assertSamePixels(image, PilBilinear.resize(image, 13, 7))
    }

    private fun assertSamePixels(expected: PixelImage, actual: PixelImage) {
        assertEquals(expected.width, actual.width)
        assertEquals(expected.height, actual.height)
        assertArrayEquals(rgb(expected), rgb(actual))
    }

    private fun rgb(image: PixelImage) = IntArray(image.argb.size) { image.argb[it] and 0xffffff }

    private fun read(name: String): PixelImage {
        val image = requireNotNull(ImageIO.read(golden.resolve(name))) { "cannot read $name" }
        return PixelImage(image.width, image.height, image.getRGB(0, 0, image.width, image.height, null, 0, image.width))
    }

    companion object {
        /** Integer hash noise in red and green, a diagonal ramp in blue; the same formula as the fixture generator. */
        fun pattern(width: Int, height: Int): PixelImage {
            val mask = 0xFFFFFFFFL
            val pixels = IntArray(width * height) { i ->
                val x = (i % width).toLong()
                val y = (i / width).toLong()
                var v = (x * 374761393L + y * 668265263L) and mask
                v = ((v xor (v ushr 13)) * 1274126177L) and mask
                v = v xor (v ushr 16)
                val r = (v and 255).toInt()
                val g = ((v ushr 8) and 255).toInt()
                val b = ((x + y) * 255 / (width + height - 2)).toInt()
                (0xff shl 24) or (r shl 16) or (g shl 8) or b
            }
            return PixelImage(width, height, pixels)
        }
    }
}
