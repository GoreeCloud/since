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
import com.goreecloud.since.domain.repository.TrackerRepository
import com.goreecloud.since.domain.validation.ValidatedTrackerDraft
import com.goreecloud.since.ui.SinceApp
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
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
    fun customPastStartCreateThenEditRemainsReachableAndPersists() {
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
        composeRule.onNodeWithTag("start-date-time-field").performTextClearance()
        composeRule.onNodeWithTag("start-date-time-field").performTextInput("2026-09-20 12:30")
        composeRule.onNodeWithTag("start-zone-field").performTextClearance()
        composeRule.onNodeWithTag("start-zone-field").performTextInput("America/Chicago")
        composeRule.onNodeWithText("Save").performScrollTo().performClick()

        composeRule.waitForIdle()
        composeRule.onNodeWithText("First car").assertIsDisplayed()
        composeRule.onNodeWithText("America/Chicago").assertIsDisplayed()

        composeRule.onNodeWithText("Edit").performClick()
        composeRule.onNodeWithTag("title-field").performTextClearance()
        composeRule.onNodeWithTag("title-field").performTextInput("First car edited")
        composeRule.onNodeWithText("Save").performScrollTo().performClick()

        composeRule.waitForIdle()
        composeRule.onNodeWithText("First car edited").assertIsDisplayed()
        assertEquals("First car edited", repository.current.single().tracker.title)
        assertEquals(
            ZoneId.of("America/Chicago").id,
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
        composeRule.onNodeWithTag("start-date-time-field").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("start-zone-field").performScrollTo().assertIsDisplayed()
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
        composeRule.onNodeWithTag("start-zone-field").performScrollTo().assertIsDisplayed()
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

private class FakeTrackerRepository(
    initial: List<TrackerAggregate>,
    private val clock: Clock = Clock.systemUTC(),
) : TrackerRepository {
    private val aggregates = MutableStateFlow(initial)
    private var nextId = initial.size + 1

    val current: List<TrackerAggregate>
        get() = aggregates.value

    override fun observeActiveTrackers(): Flow<List<Tracker>> =
        aggregates.map { values -> values.map { it.tracker } }

    override fun observeActiveTrackerAggregates(): Flow<List<TrackerAggregate>> = aggregates

    override suspend fun createTracker(draft: ValidatedTrackerDraft): TrackerAggregate {
        val id = "created-" + nextId++
        val timestamp = clock.millis()
        val created = TrackerAggregate(
            tracker = Tracker(
                id = id,
                title = draft.title,
                note = draft.note,
                kind = draft.kind,
                iconKey = null,
                accentKey = null,
                defaultDisplayFormat = draft.displayFormat,
                sortOrder = aggregates.value.size,
                isArchived = false,
                createdAtEpochMs = timestamp,
                updatedAtEpochMs = timestamp,
            ),
            periods = listOf(
                TrackerPeriod(
                    id = id + "-period",
                    trackerId = id,
                    sequence = 0,
                    startEpochMs = draft.startEpochMs,
                    startZoneId = draft.startZoneId,
                    endEpochMs = null,
                    endZoneId = null,
                    resetReason = null,
                    resetNote = null,
                    createdAtEpochMs = timestamp,
                    updatedAtEpochMs = timestamp,
                )
            ),
            goal = draft.goalAmount?.let { amount ->
                Goal(
                    trackerId = id,
                    targetAmount = amount,
                    targetUnit = checkNotNull(draft.goalUnit),
                    createdAtEpochMs = timestamp,
                    updatedAtEpochMs = timestamp,
                )
            },
        )
        aggregates.value = aggregates.value + created
        return created
    }

    override suspend fun loadTracker(trackerId: String): TrackerAggregate? =
        aggregates.value.firstOrNull { it.tracker.id == trackerId }

    override suspend fun updateTracker(
        trackerId: String,
        draft: ValidatedTrackerDraft,
    ): TrackerAggregate? {
        val existing = loadTracker(trackerId) ?: return null
        if (existing.tracker.kind != draft.kind) return null
        val timestamp = clock.millis()
        val currentPeriod = existing.periods.single { it.endEpochMs == null }
        val updated = existing.copy(
            tracker = existing.tracker.copy(
                title = draft.title,
                note = draft.note,
                defaultDisplayFormat = draft.displayFormat,
                updatedAtEpochMs = timestamp,
            ),
            periods = existing.periods.map { period ->
                if (period.id == currentPeriod.id) {
                    period.copy(
                        startEpochMs = draft.startEpochMs,
                        startZoneId = draft.startZoneId,
                        updatedAtEpochMs = timestamp,
                    )
                } else {
                    period
                }
            },
        )
        aggregates.value = aggregates.value.map { row ->
            if (row.tracker.id == trackerId) updated else row
        }
        return updated
    }

    override suspend fun updateDisplayFormat(
        trackerId: String,
        displayFormat: DisplayFormat,
    ): Boolean {
        val existing = loadTracker(trackerId) ?: return false
        val updated = existing.copy(
            tracker = existing.tracker.copy(
                defaultDisplayFormat = displayFormat,
                updatedAtEpochMs = clock.millis(),
            )
        )
        aggregates.value = aggregates.value.map { row ->
            if (row.tracker.id == trackerId) updated else row
        }
        return true
    }
}
