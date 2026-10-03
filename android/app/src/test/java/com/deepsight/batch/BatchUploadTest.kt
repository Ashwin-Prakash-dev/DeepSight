package com.deepsight.batch

import com.deepsight.capture.CaseStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** Batch upload: the router proposes a test per image, then each test's images become one queued batch. */
class BatchUploadTest {
    @get:Rule val tmp = TemporaryFolder()

    private val packs = linkedMapOf("malaria_thin" to "Malaria (thin smear)", "leukaemia_wbc" to "Leukaemia", "breast_breakhis" to "Breast")
    private fun routed(name: String, label: String) = RoutedImage(File(name), label, 0.9)

    @Test
    fun planGroupsByTestInPickerOrderAndSetsAsideTheRest() {
        val m1 = routed("m1", "malaria_thin"); val b1 = routed("b1", "breast_breakhis"); val r1 = routed("r1", "reject")
        val f1 = routed("f1", "fungal"); val m2 = routed("m2", "malaria_thin")

        val plan = planOf(listOf(m1, b1, r1, f1, m2), packs)

        assertEquals(listOf("malaria_thin", "breast_breakhis"), plan.groups.map { it.packId })
        assertEquals(listOf("Malaria (thin smear)", "Breast"), plan.groups.map { it.packName })
        assertEquals(listOf(m1, m2), plan.groups[0].images)
        assertEquals(listOf(r1), plan.rejected)
        assertEquals(listOf(f1), plan.notInstalled) // the router knows fungal; this build has no fungal pack
    }

    @Test
    fun nothingRecognisedGivesNoBatches() {
        val plan = planOf(listOf(routed("r1", "reject")), packs)
        assertTrue(plan.groups.isEmpty())
        assertEquals(1, plan.rejected.size)
    }

    @Test
    fun submitPutsEachTestsImagesInItsOwnCaseUnchangedAndQueuesItAsABatch() = runBlocking {
        val cases = CaseStore(tmp.newFolder("cases"))
        val submitted = mutableListOf<Triple<String?, String, String>>()
        val labels = listOf("malaria_thin", "reject", "breast_breakhis", "malaria_thin")
        val upload = BatchUpload(
            uploads = CaseStore(tmp.newFolder("uploads")),
            cases = cases,
            route = { files, progress -> files.mapIndexed { i, f -> progress(i + 1, files.size); RoutedImage(f, labels[i], 0.9) } },
            submit = { patient, pack, caseId -> submitted += Triple(patient, pack, caseId) },
            newCaseId = { "case-$it" },
        )
        val bytes = List(4) { i -> ByteArray(16) { (it * 7 + i).toByte() } }
        upload.stage("u1", bytes.map { it.inputStream() to "png" })
        val progress = mutableListOf<Int>()

        val plan = upload.sort("u1", packs) { done, _ -> progress += done }
        val caseIds = upload.submit("u1", plan, patientUid = "P-1")

        assertEquals(listOf(1, 2, 3, 4), progress)
        assertEquals(listOf("case-0", "case-1"), caseIds)
        assertEquals(listOf(Triple("P-1", "malaria_thin", "case-0"), Triple("P-1", "breast_breakhis", "case-1")), submitted)
        assertEquals(2, cases.fields("case-0").size)
        assertArrayEquals(bytes[0], cases.fields("case-0")[0].file.readBytes())
        assertArrayEquals(bytes[3], cases.fields("case-0")[1].file.readBytes())
        assertArrayEquals(bytes[2], cases.fields("case-1").single().file.readBytes())
        assertFalse("staged images must be gone", upload.hasStaged("u1"))
    }

    @Test
    fun discardRemovesTheStagedImages() {
        val upload = BatchUpload(CaseStore(tmp.newFolder("uploads")), CaseStore(tmp.newFolder("cases")), { _, _ -> emptyList() }, { _, _, _ -> })
        upload.stage("u1", listOf(ByteArray(4).inputStream() to "jpg"))
        assertTrue(upload.hasStaged("u1"))
        upload.discard("u1")
        assertFalse(upload.hasStaged("u1"))
    }
}
