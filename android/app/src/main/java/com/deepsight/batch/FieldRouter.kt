package com.deepsight.batch

import java.io.File
import kotlin.random.Random

/**
 * Suggests which test module a field image belongs to, for [BatchDraft]. Only a suggestion: a person checks and may
 * change every allocation before anything is analysed, because a router can be wrong.
 *
 * The app uses the trained router (`ml/router`, run by [com.deepsight.CaseRunner.route]) through [fieldRouterOf]. It
 * answers null for an image that is no known test (its reject class) or a test this build doesn't have.
 */
fun interface FieldRouter {
    /** The id of one of [packIds], or null when the image is not recognised as any of them. */
    fun route(image: File, packIds: List<String>): String?
}

/** One image and the trained router's best label for it: a pack id, or "reject" for not a supported slide. */
data class RoutedImage(val file: File, val label: String, val score: Double)

/** The trained router's answers, per file. "reject", a test not in the offered modules, or an unknown file give null. */
fun fieldRouterOf(routed: List<RoutedImage>): FieldRouter {
    val byFile = routed.associate { it.file to it.label }
    return FieldRouter { image, packIds -> byFile[image]?.takeIf { it in packIds } }
}

/** When the router can't run: nothing is suggested, so the person allocates every image. */
val NoFieldRouter = FieldRouter { _, _ -> null }

/**
 * Unused by the app since the trained router landed; kept for tests that need any router. Picks a module at random and never looks at the image. It exists so the batch flow (upload, allocate,
 * verify, submit) can be built and tested before the router is trained. Its suggestions mean nothing.
 */
class RandomFieldRouter(private val random: Random = Random.Default) : FieldRouter {
    override fun route(image: File, packIds: List<String>): String? = packIds.randomOrNull(random)
}
