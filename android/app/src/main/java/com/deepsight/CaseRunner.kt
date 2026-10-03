package com.deepsight

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.Log
import com.deepsight.batch.RoutedImage
import com.deepsight.capture.FieldImage
import com.deepsight.engine.contract.CaseResult
import com.deepsight.engine.contract.FieldResult
import com.deepsight.engine.contract.PackManifest
import com.deepsight.engine.pack.PackLoader
import com.deepsight.engine.pipeline.CellFinders
import com.deepsight.engine.pipeline.FieldPipeline
import com.deepsight.engine.quality.PixelImage
import com.deepsight.engine.router.AlwaysMatchRouterGuard
import com.deepsight.engine.router.RouterModel
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Field ids are unique across cases: Room keys fields by field_id alone. The index is the file's, so Delete leaves gaps. */
fun fieldId(caseId: String, index: Int) = "${caseId}_field_$index"

/** One analysed case: every field's result, the triaged case result, and [analysedAt] (epoch millis) when the analysis finished. */
data class CaseRun(val fields: List<FieldResult>, val case: CaseResult, val analysedAt: Long)

/**
 * Runs a case through the real engine off the main thread. Keeps the last pack's [FieldPipeline], so its model loads
 * once across cases; picking another pack frees it first, so only one pack model is in memory.
 * [routerFactory] gives the trained router (ml/router), loaded on the first run and kept for every pack; null keeps
 * the always-match stub (tests on packs the router does not know).
 */
class CaseRunner(
    private val loader: PackLoader,
    private val routerFactory: (() -> RouterModel)? = null,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val lock = Mutex() // FieldPipeline is not thread-safe
    private var catalog: List<PackManifest>? = null
    private var current: Pair<String, FieldPipeline>? = null
    // A failed load is not cached: the next run tries again, and the error shows on the case screen.
    private val router by lazy { routerFactory?.invoke() }
    // Batch upload sorts with its own copy, so sorting doesn't wait behind a running batch (one more 45 MB model).
    private val sortLock = Mutex()
    private val sortingRouter by lazy { routerFactory?.invoke() }

    /** How many times a pack was loaded; for tests. */
    var loads = 0
        private set

    /** Installed packs for the picker. The first call reads and hashes every model; later calls reuse that. */
    suspend fun packs(): List<PackManifest> = lock.withLock {
        catalog ?: withContext(Dispatchers.IO) {
            val found = loader.discover()
            found.rejected.forEach { Log.w(TAG, "pack ${it.id} not offered: ${it.reason}") }
            found.installed
        }.also { catalog = it }
    }

    /** [onProgress] gets (fields started, total) before each field, on a background thread. */
    suspend fun run(packId: String, caseId: String, images: List<FieldImage>, onProgress: (Int, Int) -> Unit = { _, _ -> }): CaseRun = lock.withLock {
        withContext(Dispatchers.Default) {
            val pipeline = pipeline(packId)
            val fields = images.mapIndexed { i, image ->
                onProgress(i + 1, images.size)
                val bitmap = decode(image.file)
                try {
                    pipeline.analyzeField(caseId, fieldId(caseId, image.index), bitmap)
                } catch (e: OutOfMemoryError) {
                    error("${image.file.name} is too large to analyse (${bitmap.width} x ${bitmap.height})")
                } finally {
                    bitmap.recycle()
                }
            }
            val case = pipeline.closeCase(caseId, fields)
            CaseRun(fields, case, analysedAt = clock()) // taken after triage, so it is when the result exists
        }
    }

    /**
     * Batch upload: the router's proposed test for each image (its best label: a pack id or "reject") and that
     * label's probability. Same decode as a case run, on a router copy of its own behind [sortLock]: a router is not
     * thread-safe, and sorting shouldn't wait for the batch the queue is running.
     */
    suspend fun route(files: List<File>, onProgress: (Int, Int) -> Unit = { _, _ -> }): List<RoutedImage> = sortLock.withLock {
        withContext(Dispatchers.Default) {
            val model = checkNotNull(sortingRouter) { "This build has no router" }
            files.mapIndexed { i, file ->
                onProgress(i + 1, files.size)
                val bitmap = decode(file)
                val probs = try { model.probabilities(pixelsOf(bitmap)) } finally { bitmap.recycle() }
                val best = probs.indices.maxBy { probs[it] } // first of equal maxima, as ScoreRouterGuard picks
                RoutedImage(file, model.labels[best], probs[best].toDouble())
            }
        }
    }

    private fun pipeline(packId: String): FieldPipeline {
        current?.let { (id, pipeline) ->
            if (id == packId) return pipeline
            current = null
            // A model that never loaded throws again on close (FieldPipeline's lazy model); it must not block the next pack.
            runCatching { pipeline.close() }.onFailure { Log.w(TAG, "closing pack $id failed", it) }
        }
        val routerGuard = router?.guardFor(packId) ?: AlwaysMatchRouterGuard
        val pack = loader.load(packId).also { loads++ }
        return FieldPipeline(pack, cellFinder = CellFinders.forPack(pack), routerGuard = routerGuard).also { current = packId to it }
    }

    companion object {
        private const val TAG = "DeepSight"

        @Volatile private var instance: CaseRunner? = null

        /** ponytail: one runner per process and its pipeline is never closed; it lives until the process dies. */
        fun get(context: Context): CaseRunner = instance ?: synchronized(this) {
            instance ?: context.applicationContext.assets.let { assets ->
                CaseRunner(PackLoader.fromAssets(assets), routerFactory = { RouterModel.fromAssets(assets) }).also { instance = it }
            }
        }

        private fun pixelsOf(bitmap: Bitmap): PixelImage {
            val argb = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(argb, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            return PixelImage(bitmap.width, bitmap.height, argb)
        }

        /** Full resolution, then the EXIF rotation, as cv2.imread does in the Python reference (DebugAnalyzeActivity too). */
        private fun decode(file: File): Bitmap {
            val raw = try {
                BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 })
            } catch (e: OutOfMemoryError) {
                null
            } ?: error("Could not decode ${file.name}")
            val degrees = when (ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> return raw
            }
            return Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, Matrix().apply { postRotate(degrees) }, false)
                .also { raw.recycle() }
        }
    }
}
