package com.deepsight.engine.router

import com.deepsight.engine.quality.PixelImage
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

/** The router's input as router_meta.json's resize_rule defines it, checked against the Colab notebook's tensors. */
class RouterInputTest {
    private val golden = File(requireNotNull(System.getProperty("contractsDir"))).parentFile.resolve("ml/router/golden")

    @Test
    fun resizedSizeKeepsTheShortSideAndFloorsTheLongSide() {
        assertEquals(341 to 256, RouterInput.resizedSize(1600, 1200, 256))
        assertEquals(256 to 455, RouterInput.resizedSize(2988, 5312, 256)) // NLM field in portrait: floor(455.1)
        assertEquals(367 to 256, RouterInput.resizedSize(389, 271, 256))
        assertEquals(339 to 256, RouterInput.resizedSize(317, 239, 256))
        assertEquals(256 to 256, RouterInput.resizedSize(100, 100, 256))
        assertEquals(640 to 256, RouterInput.resizedSize(300, 120, 256)) // upscale
    }

    @Test
    fun tensorIsNchwRgbScaledToOne() {
        val image = PixelImage(300, 260, IntArray(300 * 260) { (0xff shl 24) or (255 shl 16) or (0 shl 8) or 51 })
        val tensor = RouterInput.tensor(image, shortSide = 256, size = 224)
        val plane = 224 * 224
        assertEquals(3 * plane, tensor.size)
        assertEquals(1f, tensor[0], 0f)
        assertEquals(0f, tensor[plane], 0f)
        assertEquals(51f / 255f, tensor[2 * plane], 0f)
    }

    @Test
    fun blankAndNoiseMatchTheNotebookTensorsExactly() {
        val expected = golden.resolve("inputs_u8.bin").readBytes()
        val size = 3 * 224 * 224
        // Cases 5 and 6 of expected.json are blank.png and noise.png.
        for ((case, name) in listOf(5 to "blank.png", 6 to "noise.png")) {
            val want = FloatArray(size) { (expected[case * size + it].toInt() and 0xff) / 255f }
            assertArrayEquals(name, want, RouterInput.tensor(read(name), shortSide = 256, size = 224), 0f)
        }
    }

    private fun read(name: String): PixelImage {
        val image = requireNotNull(ImageIO.read(golden.resolve(name))) { "cannot read $name" }
        return PixelImage(image.width, image.height, image.getRGB(0, 0, image.width, image.height, null, 0, image.width))
    }
}
