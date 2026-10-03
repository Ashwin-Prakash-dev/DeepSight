package com.deepsight

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The bottom bar: Single is the start tab, each tab keeps its own back stack, and back from another tab returns to Single. */
@RunWith(AndroidJUnit4::class)
class BottomNavTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private val disclaimer = "Screening aid. A clinician decides."

    @Test
    fun tabsSwitchAndKeepTheirOwnStack() {
        rule.onNodeWithText("Choose test").assertExists()

        rule.onNodeWithText("Batch").performClick()
        rule.onNodeWithText("Batch upload").assertExists()
        rule.onNodeWithText("Select images").assertExists()
        rule.onNodeWithText("Choose test").assertDoesNotExist()
        rule.onNodeWithText(disclaimer).assertExists()

        // Single keeps History open while another tab is shown; tapping Single again goes back to its first screen.
        rule.onNodeWithText("Single").performClick()
        rule.onNodeWithText("History").performScrollTo().performClick() // below the test packs
        rule.onNodeWithText("Profile").performClick()
        rule.onNodeWithText("Search by name or ID").assertExists()
        rule.onNodeWithText("Single").performClick()
        rule.onNodeWithText("Choose test").assertDoesNotExist()
        rule.onNodeWithText("Single").performClick()
        rule.onNodeWithText("Choose test").assertExists()
    }

    @Test
    fun backFromAnotherTabReturnsToSingle() {
        rule.onNodeWithText("Profile").performClick()
        rule.onNodeWithText("Choose test").assertDoesNotExist()
        pressBack()
        rule.onNodeWithText("Choose test").assertExists()
    }

    /** The Profile tab holds patient profiles, not the phone's staff (clinicians, health workers). */
    @Test
    fun profileTabShowsPatientProfilesNotStaff() {
        rule.onNodeWithText("Profile").performClick()
        rule.onNodeWithText("Search by name or ID").assertExists()
        rule.onNodeWithText("profiles", substring = true).assertExists() // "N profiles"
        rule.onNodeWithText("Add profile").assertDoesNotExist()
        rule.onNodeWithText("No profile selected").assertDoesNotExist()
    }
}
