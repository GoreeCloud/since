package com.goreecloud.since.accessibility

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.InputMode.Companion.Keyboard
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.goreecloud.since.domain.model.DisplayFormat
import com.goreecloud.since.domain.model.Goal
import com.goreecloud.since.domain.model.Tracker
import com.goreecloud.since.domain.model.TrackerAggregate
import com.goreecloud.since.domain.model.TrackerKind
import com.goreecloud.since.domain.model.TrackerPeriod
import com.goreecloud.since.ui.SinceApp
import com.goreecloud.since.testutil.FakeTrackerRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SinceAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val clock = Clock.fixed(
        Instant.parse("2026-09-24T05:00:00Z"),
        ZoneId.of("UTC"),
    )

    @Test
    fun trackerCardIsOneCoherentClickableAccessibleUnit() {
        composeRule.setContent {
            MaterialTheme {
                SinceApp(
                    repository = FakeTrackerRepository(listOf(sampleAggregate())),
                    clock = clock,
                )
            }
        }

        composeRule
            .onNodeWithText("Since")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))

        composeRule
            .onNodeWithText("Read daily")
            .assertHasClickAction()

        composeRule
            .onNodeWithText("Goal: 30 Days")
            .assertHasClickAction()
    }

    @Test
    fun editorHeadingsRadioRowsAndValidationErrorsExposeAccessibleSemantics() {
        composeRule.setContent {
            MaterialTheme {
                SinceApp(
                    repository = FakeTrackerRepository(emptyList()),
                    clock = clock,
                )
            }
        }

        composeRule.onNodeWithText("Add tracker").performClick()
        composeRule.onNodeWithText("Permanent Event").performClick()

        composeRule
            .onNodeWithText("Create Permanent Event")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))

        composeRule
            .onNodeWithText("Days")
            .assertHasClickAction()

        composeRule
            .onNodeWithText("Save")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        composeRule
            .onNodeWithText("Title must contain between 1 and 80 Unicode characters.")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.LiveRegion,
                    LiveRegionMode.Assertive,
                )
            )
    }

    @Test
    fun dateTimeAndZonePickersAreReachableAndCreateThenEditPersists() {
        val repository = FakeTrackerRepository(
            initial = emptyList(),
            clock = clock,
        )

        composeRule.setContent {
            MaterialTheme {
                SinceApp(
                    repository = repository,
                    clock = clock,
                )
            }
        }

        composeRule.onNodeWithText("Add tracker").performClick()
        composeRule.onNodeWithText("Permanent Event").performClick()

        composeRule.onNodeWithTag("title-field").performTextInput("First car")

        composeRule
            .onNodeWithTag("start-date-picker")
            .performScrollTo()
            .assertHasClickAction()
            .performClick()
        composeRule.onNodeWithTag("start-date-picker-dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Done").performClick()

        composeRule
            .onNodeWithTag("start-time-picker")
            .performScrollTo()
            .assertHasClickAction()
            .performClick()
        composeRule.onNodeWithTag("start-time-picker-dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Done").performClick()

        composeRule
            .onNodeWithTag("start-zone-picker")
            .performScrollTo()
            .assertHasClickAction()
            .performClick()
        composeRule.onNodeWithTag("start-zone-picker-dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("start-zone-search").performTextInput("Asia/Tokyo")
        composeRule.onNodeWithTag("start-zone-option-Asia/Tokyo").performClick()

        composeRule.onNodeWithText("Save").performScrollTo().performClick()

        composeRule.waitForIdle()
        composeRule.onNodeWithText("First car").assertIsDisplayed()

        composeRule.onNodeWithText("Edit").performClick()
        composeRule.onNodeWithTag("title-field").performTextClearance()
        composeRule.onNodeWithTag("title-field").performTextInput("First car edited")
        composeRule.onNodeWithText("Save").performScrollTo().performClick()

        composeRule.waitForIdle()
        composeRule.onNodeWithText("First car edited").assertIsDisplayed()
        assertEquals("First car edited", repository.current.single().tracker.title)
        assertEquals(
            "Asia/Tokyo",
            repository.current.single().periods.single().startZoneId,
        )
    }

    @Test
    fun largeFontEditorKeepsPrimaryFieldsAndSaveReachable() {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = 1f,
                    fontScale = 2f,
                )
            ) {
                MaterialTheme {
                    SinceApp(
                        repository = FakeTrackerRepository(emptyList()),
                        clock = clock,
                    )
                }
            }
        }

        composeRule.onNodeWithText("Add tracker").performClick()
        composeRule.onNodeWithText("Permanent Event").performClick()

        composeRule.onNodeWithTag("title-field").assertIsDisplayed()
        composeRule.onNodeWithTag("start-date-picker").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("start-time-picker").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("start-zone-picker").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Save").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun forcedRtlEditorKeepsPrimaryActionsReachable() {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                MaterialTheme {
                    SinceApp(
                        repository = FakeTrackerRepository(emptyList()),
                        clock = clock,
                    )
                }
            }
        }

        composeRule.onNodeWithText("Add tracker").performClick()
        composeRule.onNodeWithText("Permanent Event").performClick()

        composeRule
            .onNodeWithText("Create Permanent Event")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        composeRule.onNodeWithTag("title-field").assertIsDisplayed()
        composeRule.onNodeWithTag("start-zone-picker").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Save").performScrollTo().assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
    @Test
    fun keyboardTabMovesFromCancelToSaveAndEnterActivatesSave() {
        lateinit var inputModeManager: InputModeManager

        composeRule.setContent {
            inputModeManager = LocalInputModeManager.current
            MaterialTheme {
                SinceApp(
                    repository = FakeTrackerRepository(emptyList()),
                    clock = clock,
                )
            }
        }

        composeRule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
        }

        composeRule.onNodeWithText("Add tracker").performClick()
        composeRule.onNodeWithText("Permanent Event").performClick()

        val cancel = composeRule
            .onNodeWithText("Cancel")
            .performScrollTo()
            .requestFocus()
            .assertIsFocused()

        cancel.performKeyInput {
            keyDown(Key.Tab)
            keyUp(Key.Tab)
        }

        composeRule
            .onNodeWithText("Save")
            .assertIsFocused()
            .performKeyInput {
                keyDown(Key.Enter)
                keyUp(Key.Enter)
            }

        composeRule
            .onNodeWithText("Title must contain between 1 and 80 Unicode characters.")
            .assertIsDisplayed()
    }

    @Test
    fun goalEditorUpdatesAndRemovesGoalWithoutChangingStreakHistory() {
        val repository = FakeTrackerRepository(
            initial = listOf(sampleAggregate()),
            clock = clock,
        )

        composeRule.setContent {
            MaterialTheme {
                SinceApp(
                    repository = repository,
                    clock = clock,
                )
            }
        }

        val originalPeriod = repository.current.single().periods.single()

        composeRule.onNodeWithText("Read daily").performClick()
        composeRule.onNodeWithText("Edit goal").performScrollTo().performClick()
        composeRule.onNodeWithTag("goal-amount-field", useUnmergedTree = true).performTextClearance()
        composeRule.onNodeWithTag("goal-amount-field", useUnmergedTree = true).performTextInput("14")
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitForIdle()

        assertEquals(14, repository.current.single().goal!!.targetAmount)
        assertEquals(originalPeriod, repository.current.single().periods.single())

        composeRule.onNodeWithText("Edit goal").performScrollTo().performClick()
        composeRule.onNodeWithText("Remove goal").performClick()
        composeRule.onNodeWithText("Remove").performClick()
        composeRule.waitForIdle()

        assertEquals(null, repository.current.single().goal)
        assertEquals(originalPeriod, repository.current.single().periods.single())
    }

    private fun sampleAggregate(): TrackerAggregate {
        val trackerId = "tracker-accessibility"
        return TrackerAggregate(
            tracker = Tracker(
                id = trackerId,
                title = "Read daily",
                note = "Keep going.",
                kind = TrackerKind.STREAK,
                iconKey = null,
                accentKey = null,
                defaultDisplayFormat = DisplayFormat.DAYS,
                sortOrder = 0,
                isArchived = false,
                createdAtEpochMs = 1_000L,
                updatedAtEpochMs = 1_000L,
            ),
            periods = listOf(
                TrackerPeriod(
                    id = "period-accessibility",
                    trackerId = trackerId,
                    sequence = 0,
                    startEpochMs = Instant.parse("2026-09-23T05:00:00Z").toEpochMilli(),
                    startZoneId = "UTC",
                    endEpochMs = null,
                    endZoneId = null,
                    resetReason = null,
                    resetNote = null,
                    createdAtEpochMs = 1_000L,
                    updatedAtEpochMs = 1_000L,
                )
            ),
            goal = Goal(
                trackerId = trackerId,
                targetAmount = 30,
                targetUnit = DisplayFormat.DAYS,
                createdAtEpochMs = 1_000L,
                updatedAtEpochMs = 1_000L,
            ),
        )
    }
}
