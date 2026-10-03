package com.deepsight.engine.router

import com.deepsight.engine.contract.RouterVerdict
import com.deepsight.engine.quality.PixelImage
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Loading and validating ml/router, and the guard each pack gets. ONNX Runtime is never started here. */
class RouterModelTest {
    private val dir = File(requireNotNull(System.getProperty("contractsDir"))).parentFile.resolve("ml/router")
    private val noOnnx: (ByteArray) -> Nothing = { error("ONNX Runtime must not start while loading") }
    private val field = PixelImage(300, 260, IntArray(300 * 260) { 0xff808080.toInt() })

    private fun files(override: Map<String, ByteArray> = emptyMap()): (String) -> ByteArray =
        { name -> override[name] ?: dir.resolve(name).readBytes() }

    @Test
    fun loadsTheShippedRouterWithoutStartingOnnxRuntime() {
        val router = RouterModel.load(files(), noOnnx)
        assertEquals(listOf("malaria_thin", "fungal", "leukaemia_wbc", "breast_breakhis", "reject"), router.labels)
        assertEquals(256, router.shortSide)
        assertEquals(224, router.inputSize)
    }

    @Test
    fun modelThatDiffersFromItsChecksumIsRefused() {
        val tampered = dir.resolve("router.onnx").readBytes().also { it[it.size / 2] = (it[it.size / 2] + 1).toByte() }
        val error = runCatching { RouterModel.load(files(mapOf("router.onnx" to tampered)), noOnnx) }.exceptionOrNull()
        assertTrue("$error", error is RouterLoadException && "sha256" in error.message.orEmpty())
    }

    @Test
    fun labelsThatDisagreeWithTheMetadataAreRefused() {
        val swapped = """["fungal", "malaria_thin", "leukaemia_wbc", "breast_breakhis", "reject"]""".toByteArray()
        val error = runCatching { RouterModel.load(files(mapOf("labels.json" to swapped)), noOnnx) }.exceptionOrNull()
        assertTrue("$error", error is RouterLoadException && "labels" in error.message.orEmpty())
    }

    @Test
    fun knownPackGetsTheScoringGuard() {
        var shape: LongArray? = null
        val router = RouterModel(LABELS, shortSide = 256, inputSize = 224, runModel = { _, s -> shape = s; floatArrayOf(.1f, .7f, .1f, .05f, .05f) })

        val result = router.guardFor("malaria_thin").evaluate(field, "malaria_thin")

        assertEquals(RouterVerdict.MISMATCH, result.verdict)
        assertEquals("fungal", result.predicted)
        assertEquals(0.7, result.score, 1e-6)
        assertArrayEquals(longArrayOf(1, 3, 224, 224), shape)
    }

    @Test
    fun packTheRouterDoesNotKnowKeepsTheAlwaysMatchGuard() {
        val router = RouterModel(LABELS, 256, 224, runModel = { _, _ -> error("must not run") })
        assertSame(AlwaysMatchRouterGuard, router.guardFor("smoke"))
        assertSame(AlwaysMatchRouterGuard, router.guardFor("reject"))
    }

    @Test(expected = IllegalStateException::class)
    fun outputOfTheWrongSizeFails() {
        RouterModel(LABELS, 256, 224, runModel = { _, _ -> floatArrayOf(1f) }).probabilities(field)
    }

    private companion object {
        val LABELS = listOf("malaria_thin", "fungal", "leukaemia_wbc", "breast_breakhis", "reject")
    }
}
