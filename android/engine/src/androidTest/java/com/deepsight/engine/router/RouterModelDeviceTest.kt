package com.deepsight.engine.router

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.deepsight.engine.contract.Contracts
import com.deepsight.engine.contract.RouterVerdict
import com.deepsight.engine.onnx.OnnxModel
import com.deepsight.engine.pack.PackLoader
import com.deepsight.engine.pipeline.CellFinders
import com.deepsight.engine.pipeline.FieldPipeline
import com.deepsight.engine.quality.PixelImage
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** ml/router on the phone: PyTorch's golden outputs, then real fields through FieldPipeline. Logs under DeepSightRouter. */
@RunWith(AndroidJUnit4::class)
class RouterModelDeviceTest {
    private val assets = InstrumentationRegistry.getInstrumentation().context.assets
    private val expected = Contracts.json.parseToJsonElement(assets.open("router/golden/expected.json").use { it.readBytes().decodeToString() }).jsonObject
    private val cases = expected.getValue("cases").jsonArray.map { it.jsonObject }

    private fun probs(case: Int) = cases[case].getValue("probs").jsonArray.map { it.jsonPrimitive.float }

    @Test
    fun goldenTensorsMatchPyTorchOnCpuAndXnnpack() {
        val inputs = assets.open("router/golden/inputs_u8.bin").use { it.readBytes() }
        for (accelerator in OnnxModel.Accelerator.entries) {
            RouterModel.fromAssets(assets, accelerator = accelerator).use { router ->
                val size = 3 * router.inputSize * router.inputSize
                cases.indices.forEach { c ->
                    val got = router.probabilitiesOf(FloatArray(size) { (inputs[c * size + it].toInt() and 0xff) / 255f })
                    val want = probs(c)
                    val diff = got.indices.maxOf { kotlin.math.abs(got[it] - want[it]) }
                    Log.i(TAG, "$accelerator ${cases[c]["name"]}: max abs diff $diff")
                    assertTrue("$accelerator case $c: diff $diff", diff < 1e-4f)
                    assertEquals(cases[c].getValue("argmax").jsonPrimitive.content, router.labels[got.indices.maxBy { got[it] }])
                }
            }
        }
    }

    @Test
    fun blankAndNoisePngsGiveTheGoldenProbabilitiesEndToEnd() {
        RouterModel.fromAssets(assets).use { router ->
            for ((case, name) in listOf(5 to "blank.png", 6 to "noise.png")) {
                val got = router.probabilities(decode("router/golden/$name"))
                val diff = got.indices.maxOf { kotlin.math.abs(got[it] - probs(case)[it]) }
                Log.i(TAG, "$name end to end: max abs diff $diff, reject ${got.last()}")
                assertTrue("$name: diff $diff", diff < 1e-4f)
            }
        }
    }

    @Test
    fun realMalariaFieldPassesOnMalariaAndIsBlockedOnBreast() {
        assumeTrue("ml/data/android_parity is not in this build", FIELD in assets.list("").orEmpty())
        val field = decode(FIELD)
        val packs = PackLoader.fromAssets(assets, packsRoot = "packs")
        RouterModel.fromAssets(assets).use { router ->
            val malaria = packs.load("malaria_thin")
            FieldPipeline(malaria, cellFinder = CellFinders.forPack(malaria), routerGuard = router.guardFor("malaria_thin")).use { pipeline ->
                val result = pipeline.analyze("case-r", "f1", field)
                Log.i(TAG, "malaria field on malaria_thin: ${result.router} timing ${result.timingMs}")
                assertTrue(result.quality.pass)
                assertEquals(RouterVerdict.MATCH, requireNotNull(result.router).verdict)
                assertTrue("the pack model must run after a match", "pack" in result.timingMs)
            }
            val breast = packs.load("breast_breakhis")
            FieldPipeline(breast, routerGuard = router.guardFor("breast_breakhis")).use { pipeline ->
                val result = pipeline.analyze("case-r", "f1", field)
                Log.i(TAG, "malaria field on breast_breakhis: ${result.router} timing ${result.timingMs}")
                val router = requireNotNull(result.router)
                assertEquals(RouterVerdict.MISMATCH, router.verdict)
                assertEquals("malaria_thin", router.predicted)
                assertTrue(result.objects.isEmpty())
                assertEquals(Contracts.RULE_ROUTER_MISMATCH, pipeline.closeCase("case-r", listOf(result)).triage.ruleId)
            }
        }
    }

    private fun decode(asset: String): PixelImage {
        val bitmap = requireNotNull(assets.open(asset).use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }) }) { "cannot decode $asset" }
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return PixelImage(bitmap.width, bitmap.height, pixels).also { bitmap.recycle() }
    }

    private companion object {
        const val TAG = "DeepSightRouter"
        const val FIELD = "234C92P53ThinF_IMG_20150821_150718.jpg" // RBCNet positive patient (ml/data/android_parity)
    }
}
