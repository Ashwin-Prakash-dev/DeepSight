package com.deepsight.batch

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The trained router's answers as suggestions on the allocation screen. */
class TrainedFieldRouterTest {
    private val packs = listOf("malaria_thin", "breast_breakhis", "leukaemia_wbc")
    private val malaria = File("m.jpg")
    private val breast = File("b.png")
    private val blank = File("blank.png")
    private val fungal = File("f.jpg")
    private val routed = listOf(
        RoutedImage(malaria, "malaria_thin", 0.99),
        RoutedImage(breast, "breast_breakhis", 0.97),
        RoutedImage(blank, "reject", 0.95),
        RoutedImage(fungal, "fungal", 0.90), // the router knows fungal; the app no longer has that pack
    )

    @Test
    fun suggestsTheModuleTheRouterChose() {
        val router = fieldRouterOf(routed)
        assertEquals("malaria_thin", router.route(malaria, packs))
        assertEquals("breast_breakhis", router.route(breast, packs))
    }

    @Test
    fun rejectedOrNotInstalledImagesAreLeftForThePersonToAllocate() {
        val router = fieldRouterOf(routed)
        assertNull("reject means not a slide of any test", router.route(blank, packs))
        assertNull("a test this build doesn't have can't be suggested", router.route(fungal, packs))
        assertNull("an image the router never saw", router.route(File("other.jpg"), packs))
    }

    @Test
    fun inADraftTheyBecomeUnallocatedAndBlockAnalysisUntilPlaced() {
        val draft = BatchDraft().add(listOf(malaria, blank, fungal), fieldRouterOf(routed), packs)
        assertEquals(listOf("malaria_thin", null, null), draft.images.map { it.suggested })
        assertEquals(2, draft.unassigned.size)
        assertEquals(false, draft.setVerified(true).verified)
    }

    @Test
    fun withoutARouterEveryImageIsUnallocated() {
        val draft = BatchDraft().add(listOf(malaria, breast), NoFieldRouter, packs)
        assertEquals(listOf<String?>(null, null), draft.images.map { it.suggested })
    }
}
