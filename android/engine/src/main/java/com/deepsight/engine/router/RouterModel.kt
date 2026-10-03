package com.deepsight.engine.router

import android.content.res.AssetManager
import com.deepsight.engine.onnx.OnnxModel
import com.deepsight.engine.quality.PixelImage
import java.security.MessageDigest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

class RouterLoadException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** The fields of ml/router/router_meta.json the app relies on (the notebook writes many more). */
@Serializable
private data class RouterMeta(
    val labels: List<String>,
    val input: Input,
    val outputs: List<String>,
    @SerialName("resize_rule") val resizeRule: ResizeRule,
    @SerialName("model_sha256") val modelSha256: String,
) {
    @Serializable
    data class Input(val layout: String, val dtype: String, val shape: List<JsonElement>, val range: List<Double>)

    @Serializable
    data class ResizeRule(@SerialName("short_side") val shortSide: Int)
}

/**
 * The trained router (ml/router, from the Colab notebook): one field image in, a probability per label out. Labels
 * are pack ids plus "reject", in output order. [runModel] gets an NCHW tensor and its shape and returns the first
 * output ("probs"). Not thread-safe, like the [OnnxModel] behind it.
 */
class RouterModel(
    val labels: List<String>,
    val shortSide: Int,
    val inputSize: Int,
    private val runModel: (FloatArray, LongArray) -> FloatArray,
    private val closeModel: () -> Unit = {},
) : AutoCloseable {
    fun probabilities(image: PixelImage): FloatArray = probabilitiesOf(RouterInput.tensor(image, shortSide, inputSize))

    /** The model alone, on an already preprocessed [RouterInput] tensor (golden tests). */
    internal fun probabilitiesOf(tensor: FloatArray): FloatArray {
        val probs = runModel(tensor, longArrayOf(1, 3, inputSize.toLong(), inputSize.toLong()))
        check(probs.size == labels.size) { "router returned ${probs.size} values for ${labels.size} labels" }
        return probs
    }

    /** The guard for the pack the user picked; a pack the router was not trained on keeps [AlwaysMatchRouterGuard]. */
    fun guardFor(packId: String): RouterGuard =
        if (packId != REJECT && packId in labels) ScoreRouterGuard(labels, ::probabilities) else AlwaysMatchRouterGuard

    override fun close() = closeModel()

    companion object {
        const val ASSET_ROOT = "router"
        private const val REJECT = "reject"
        private val json = Json { ignoreUnknownKeys = true }

        /**
         * Reads router_meta.json, labels.json and router.onnx through [readFile] and checks that they agree, including
         * the model's sha256. [openModel] starts ONNX Runtime on the first field, not here.
         */
        fun load(readFile: (String) -> ByteArray, openModel: (ByteArray) -> OnnxModel = { OnnxModel(it) }): RouterModel {
            val meta = parse("router_meta.json") { json.decodeFromString<RouterMeta>(readFile(it).decodeToString()) }
            val labels = parse("labels.json") { json.decodeFromString<List<String>>(readFile(it).decodeToString()) }
            if (labels != meta.labels) fail("labels.json $labels differs from router_meta.json labels ${meta.labels}")
            if (REJECT !in labels || labels.distinct().size != labels.size) fail("labels must be unique and include '$REJECT': $labels")
            if (meta.outputs.firstOrNull() != "probs") fail("the first output must be 'probs', got ${meta.outputs}")
            val shape = meta.input.shape.map { it.jsonPrimitive.intOrNull }
            val size = shape.getOrNull(2)
            if (meta.input.layout != "NCHW" || meta.input.dtype != "float32" || shape.size != 4 || shape[1] != 3 ||
                size == null || size <= 0 || shape[3] != size || meta.input.range != listOf(0.0, 1.0)
            ) fail("unsupported input ${meta.input}")
            if (meta.resizeRule.shortSide < size) fail("short side ${meta.resizeRule.shortSide} is smaller than the $size crop")

            val modelBytes = parse("router.onnx") { readFile(it) }
            val sha = MessageDigest.getInstance("SHA-256").digest(modelBytes).joinToString("") { "%02x".format(it) }
            if (sha != meta.modelSha256) fail("router.onnx sha256 $sha differs from router_meta.json ${meta.modelSha256}")
            var bytes: ByteArray? = modelBytes // dropped once ONNX Runtime holds the model

            val model = lazy { openModel(requireNotNull(bytes)).also { bytes = null } }
            return RouterModel(
                labels, meta.resizeRule.shortSide, size,
                runModel = { input, inputShape -> model.value.run(input, inputShape) },
                closeModel = { if (model.isInitialized()) model.value.close() },
            )
        }

        fun fromAssets(
            assets: AssetManager,
            root: String = ASSET_ROOT,
            accelerator: OnnxModel.Accelerator = OnnxModel.Accelerator.XNNPACK,
        ): RouterModel = load({ name -> assets.open("$root/$name").use { it.readBytes() } }) { OnnxModel(it, accelerator) }

        private fun <T> parse(file: String, block: (String) -> T): T =
            try {
                block(file)
            } catch (e: RouterLoadException) {
                throw e
            } catch (e: Exception) {
                throw RouterLoadException("router: cannot read $file: ${e.message}", e)
            }

        private fun fail(message: String): Nothing = throw RouterLoadException("router: $message")
    }
}
