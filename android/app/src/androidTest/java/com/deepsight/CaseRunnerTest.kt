package com.deepsight

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.deepsight.capture.CaseStore
import com.deepsight.engine.contract.Contracts
import com.deepsight.engine.contract.RouterVerdict
import com.deepsight.engine.pack.PackLoader
import com.deepsight.engine.router.RouterModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** The app's case run on the engine's smoke pack (random weights): real ONNX Runtime, real files, real triage. */
@RunWith(AndroidJUnit4::class)
class CaseRunnerTest {
    private val testAssets = InstrumentationRegistry.getInstrumentation().context.assets
    private val root = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "case-runner-test").apply { deleteRecursively() }
    private val store = CaseStore(root)
    private val runner = CaseRunner(PackLoader.fromAssets(testAssets, packsRoot = "testpacks"))

    private fun addSmokeField(caseId: String) = testAssets.open("testpacks/smoke/golden/case1.png").use { store.import(caseId, it, "png") }

    @Test
    fun runsEveryStoredFieldThenClosesTheCase() = runBlocking {
        addSmokeField("c1")
        addSmokeField("c1")
        store.fields("c1").first().file.delete() // Delete leaves a gap: ids follow the file index, not the position

        val (fields, case) = runner.run("smoke", "c1", store.fields("c1"))

        assertEquals(listOf(fieldId("c1", 2)), fields.map { it.fieldId })
        assertTrue(fields.single().quality.pass)
        assertNotNull(fields.single().imageScore)
        assertEquals("c1", case.caseId)
        assertEquals(listOf(fieldId("c1", 2)), case.fieldIds)
        assertEquals(1, case.fieldsPassed)
    }

    @Test
    fun analysisTimeIsTakenWhenTheRunFinishes() = runBlocking {
        val times = ArrayDeque(listOf(1_000L, 2_000L))
        val timed = CaseRunner(PackLoader.fromAssets(testAssets, packsRoot = "testpacks")) { times.removeFirst() }
        addSmokeField("c1")
        addSmokeField("c2")
        assertEquals(1_000L, timed.run("smoke", "c1", store.fields("c1")).analysedAt)
        assertEquals(2_000L, timed.run("smoke", "c2", store.fields("c2")).analysedAt) // every analysis gets its own time
    }

    @Test
    fun secondCaseReusesTheLoadedPack() = runBlocking {
        addSmokeField("c1")
        addSmokeField("c2")
        val first = runner.run("smoke", "c1", store.fields("c1")).fields.single()
        val second = runner.run("smoke", "c2", store.fields("c2")).fields.single()
        assertEquals(first.imageScore, second.imageScore)
        assertEquals(1, runner.loads)
    }

    @Test
    fun undecodableImageFailsWithItsFileName() = runBlocking {
        val bad = store.nextFile("c1", "jpg").apply { writeText("not an image") }
        assertEquals(null, BitmapFactory.decodeFile(bad.path))
        val error = runCatching { runner.run("smoke", "c1", store.fields("c1")) }.exceptionOrNull()
        assertTrue(error?.message.orEmpty(), error?.message.orEmpty().contains(bad.name))
    }

    /** The shipped router and malaria pack, as the app runs them: a photo of noise never reaches the pack model. */
    @Test
    fun shippedRouterRejectsAFieldThatIsNotASlide() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val shipped = CaseRunner(PackLoader.fromAssets(app.assets), routerFactory = { RouterModel.fromAssets(app.assets) })
        val random = java.util.Random(7)
        val noise = Bitmap.createBitmap(IntArray(400 * 300) { 0xff000000.toInt() or random.nextInt(0x1000000) }, 400, 300, Bitmap.Config.ARGB_8888)
        store.nextFile("r1", "png").outputStream().use { noise.compress(Bitmap.CompressFormat.PNG, 100, it) }

        val (fields, case) = shipped.run("malaria_thin", "r1", store.fields("r1"))

        assertTrue(fields.single().quality.pass)
        assertEquals(RouterVerdict.REJECT, fields.single().router?.verdict)
        assertTrue("pack model ran after a reject: ${fields.single().timingMs}", "pack" !in fields.single().timingMs)
        assertEquals(Contracts.RULE_ROUTER_REJECT, case.triage.ruleId)
    }

    /** Batch upload asks the shipped router which test each image is for; noise is for none. */
    @Test
    fun shippedRouterSortsAnUploadedImage() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val shipped = CaseRunner(PackLoader.fromAssets(app.assets), routerFactory = { RouterModel.fromAssets(app.assets) })
        val random = java.util.Random(11)
        val noise = Bitmap.createBitmap(IntArray(400 * 300) { 0xff000000.toInt() or random.nextInt(0x1000000) }, 400, 300, Bitmap.Config.ARGB_8888)
        val file = File(root, "upload.png").apply { parentFile?.mkdirs(); outputStream().use { noise.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        val progress = mutableListOf<Int>()

        val routed = shipped.route(listOf(file)) { done, _ -> progress += done }.single()

        assertEquals("reject", routed.label)
        assertTrue("score ${routed.score}", routed.score > 0.5)
        assertEquals(listOf(1), progress)
    }

    @Test
    fun packThatFailsToLoadDoesNotBreakTheNextPack() = runBlocking {
        addSmokeField("c1")
        assertTrue(runCatching { runner.run("broken", "c1", store.fields("c1")) }.isFailure) // garbage model bytes
        assertTrue(runner.run("smoke", "c1", store.fields("c1")).fields.single().quality.pass)
    }
}
