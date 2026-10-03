package com.deepsight.history

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deepsight.HistoryItem
import com.deepsight.data.CaseStatus
import com.deepsight.engine.contract.TriageLevel
import com.deepsight.ui.theme.DeepSightTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** History's delete: only for records the queue is done with, and only after the user confirms. */
@RunWith(AndroidJUnit4::class)
class HistoryScreenTest {
    @get:Rule val rule = createComposeRule()

    private val signed = HistoryItem("signed", "Malaria (thin smear)", TriageLevel.ABNORMAL_FLAG, 2L, "Dr Test", "ACCEPT", CaseStatus.SIGNED, createdAt = 1L)
    private val queued = HistoryItem("queued", "Breast histology (H&E)", null, null, null, null, CaseStatus.QUEUED, createdAt = 3L)

    @Test
    fun deletingAsksFirstAndOnlyFinishedRecordsOfferIt() {
        val deleted = mutableListOf<String>()
        rule.setContent { DeepSightTheme { HistoryScreen(listOf(queued, signed), onOpen = {}, onDelete = { deleted += it }) } }

        rule.onAllNodesWithContentDescription("Delete record").assertCountEquals(1) // the signed one; the queued one is the queue's

        rule.onNodeWithContentDescription("Delete record").performClick()
        rule.onNodeWithText("Delete this record?").assertExists()
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithText("Delete this record?").assertDoesNotExist()
        assertEquals(emptyList<String>(), deleted)

        rule.onNodeWithContentDescription("Delete record").performClick()
        rule.onNodeWithText("Delete").performClick()
        assertEquals(listOf("signed"), deleted)
    }
}
