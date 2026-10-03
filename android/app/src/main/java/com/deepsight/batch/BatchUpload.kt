package com.deepsight.batch

import com.deepsight.capture.CaseStore
import java.io.File
import java.io.InputStream

/** One uploaded image and the router's proposal for it: a pack id, or "reject" for not a supported slide. */
data class RoutedImage(val file: File, val label: String, val score: Double)

/** Uploaded images sorted by the router: one batch per installed test, plus the images that can't run. */
data class BatchPlan(val groups: List<Group>, val rejected: List<RoutedImage>, val notInstalled: List<RoutedImage>) {
    data class Group(val packId: String, val packName: String, val images: List<RoutedImage>)
}

/**
 * Groups [routed] by proposed test, in the order of [packs] (pack id -> display name, as the picker lists them).
 * "reject" and tests this build doesn't have are set aside, never run.
 */
fun planOf(routed: List<RoutedImage>, packs: Map<String, String>): BatchPlan {
    val byLabel = routed.groupBy { it.label }
    return BatchPlan(
        groups = packs.mapNotNull { (id, name) -> byLabel[id]?.let { BatchPlan.Group(id, name, it) } },
        rejected = byLabel[REJECT].orEmpty(),
        notInstalled = routed.filter { it.label != REJECT && it.label !in packs },
    )
}

/** What the Batch tab shows for an upload in progress. */
sealed interface UploadUiState {
    data object Idle : UploadUiState
    data class Copying(val total: Int) : UploadUiState
    data class Sorting(val done: Int, val total: Int) : UploadUiState
    data class Review(val plan: BatchPlan) : UploadUiState
    data class Failed(val message: String) : UploadUiState
}

/**
 * Batch upload: picked images are copied unchanged into a staging folder ([uploads]), the router proposes a test for
 * each ([route]), and on submit each test's images move into a new case ([cases]) that is queued as a batch
 * ([submit]). The user sees the proposal before anything runs; every field is still checked by its pack's router guard.
 */
class BatchUpload(
    private val uploads: CaseStore,
    private val cases: CaseStore,
    private val route: suspend (List<File>, (Int, Int) -> Unit) -> List<RoutedImage>,
    private val submit: suspend (patientUid: String?, packId: String, caseId: String) -> Unit,
    private val newCaseId: (Int) -> String = { "case-${System.currentTimeMillis()}-$it" },
) {
    /** Copies each (stream, file extension) as-is, in order. */
    fun stage(uploadId: String, inputs: List<Pair<InputStream, String>>): List<File> =
        inputs.map { (input, ext) -> uploads.import(uploadId, input, ext) }

    fun hasStaged(uploadId: String): Boolean = uploads.fields(uploadId).isNotEmpty()

    suspend fun sort(uploadId: String, packs: Map<String, String>, onProgress: (Int, Int) -> Unit = { _, _ -> }): BatchPlan =
        planOf(route(uploads.fields(uploadId).map { it.file }, onProgress), packs)

    /** Queues one batch per group, for [patientUid] (optional), and returns their case ids; the staged images are removed. */
    suspend fun submit(uploadId: String, plan: BatchPlan, patientUid: String?): List<String> {
        val ids = plan.groups.mapIndexed { i, group ->
            newCaseId(i).also { caseId -> group.images.forEach { cases.import(caseId, it.file.inputStream(), it.file.extension) } }
        }
        discard(uploadId)
        plan.groups.zip(ids).forEach { (group, caseId) -> submit(patientUid, group.packId, caseId) }
        return ids
    }

    fun discard(uploadId: String) = uploads.delete(uploadId)
}

private const val REJECT = "reject"
