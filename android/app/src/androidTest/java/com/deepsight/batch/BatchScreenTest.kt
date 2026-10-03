package com.deepsight.batch

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deepsight.data.CaseStatus
import com.deepsight.ui.theme.DeepSightTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BatchScreenTest {
    @get:Rule val rule = createComposeRule()

    private val running = BatchItem("c1", "Ada Example · P-0000-0001", "Malaria (thin smear)", CaseStatus.RUNNING, 2 to 4, null, null)
    private val queued = BatchItem("c2", "Ben Sample · P-0000-0002", "Malaria (thin smear)", CaseStatus.QUEUED, null, 1, null)
    private val ready = BatchItem("c3", "Cara Test · P-0000-0003", "Malaria (thin smear)", CaseStatus.DONE, null, null, null)

    @Test
    fun listsEveryUnsignedBatchAndOpensOnlyAFinishedOne() {
        val opened = mutableListOf<String>()
        rule.setContent { DeepSightTheme { BatchScreen(listOf(running, queued, ready), onOpen = { opened += it }, onUseSingle = {}) } }

        rule.onNodeWithText("Analysing field 2 of 4…").performScrollTo().assertExists()
        rule.onNodeWithText("Queued · next").performScrollTo().assertExists()
        rule.onNodeWithText("Ben Sample · P-0000-0002").performScrollTo().performClick() // still queued: nothing to open
        rule.onNodeWithText("Cara Test · P-0000-0003").performScrollTo().performClick()
        assertEquals(listOf("c3"), opened)
    }

    @Test
    fun uploadIsAvailableNotComingSoon() {
        rule.setContent { DeepSightTheme { BatchScreen(emptyList(), onOpen = {}, onUseSingle = {}) } }
        rule.onNodeWithText("Select images").assertIsEnabled()
        rule.onNodeWithText("Coming soon").assertDoesNotExist()
    }

    @Test
    fun sortingShowsProgress() {
        rule.setContent { DeepSightTheme { BatchScreen(emptyList(), onOpen = {}, onUseSingle = {}, upload = UploadUiState.Sorting(3, 10)) } }
        rule.onNodeWithText("Sorting image 3 of 10…").assertExists()
    }

    @Test
    fun reviewShowsTheRoutersProposalAndSubmitsIt() {
        fun img(name: String, label: String) = RoutedImage(File(name), label, 0.97)
        val plan = BatchPlan(
            groups = listOf(
                BatchPlan.Group("malaria_thin", "Malaria (thin smear)", listOf(img("a", "malaria_thin"), img("b", "malaria_thin"))),
                BatchPlan.Group("breast_breakhis", "Breast", listOf(img("c", "breast_breakhis"))),
            ),
            rejected = listOf(img("d", "reject")),
            notInstalled = listOf(img("e", "fungal")),
        )
        var submitted = 0
        var discarded = 0
        rule.setContent {
            DeepSightTheme {
                BatchScreen(emptyList(), onOpen = {}, onUseSingle = {}, upload = UploadUiState.Review(plan),
                    onSubmit = { submitted++ }, onDiscard = { discarded++ })
            }
        }

        rule.onNodeWithText("Malaria (thin smear) · 2 images").performScrollTo().assertExists()
        rule.onNodeWithText("Breast · 1 image").performScrollTo().assertExists()
        rule.onNodeWithText("Not recognised as a slide · 1 image (skipped)").performScrollTo().assertExists()
        rule.onNodeWithText("Looks like fungal, not installed · 1 image (skipped)").performScrollTo().assertExists()
        rule.onNodeWithText("Submit 2 batches").performScrollTo().performClick()
        rule.onNodeWithText("Discard").performScrollTo().performClick()
        assertEquals(1, submitted)
        assertEquals(1, discarded)
    }

    @Test
    fun noBatchesShowsTheEmptyState() {
        rule.setContent { DeepSightTheme { BatchScreen(emptyList(), onOpen = {}, onUseSingle = {}) } }
        rule.onNodeWithText("No batches yet").performScrollTo().assertExists()
    }
}
