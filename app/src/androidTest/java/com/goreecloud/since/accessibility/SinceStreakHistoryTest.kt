package com.goreecloud.since.accessibility

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import com.goreecloud.since.domain.model.DisplayFormat
import com.goreecloud.since.domain.model.Goal
import com.goreecloud.since.domain.model.Tracker
import com.goreecloud.since.domain.model.TrackerAggregate
import com.goreecloud.since.domain.model.TrackerKind
import com.goreecloud.since.domain.model.TrackerPeriod
import com.goreecloud.since.testutil.FakeTrackerRepository
import com.goreecloud.since.ui.SinceApp
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SinceStreakHistoryTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val now = Instant.parse("2026-09-24T18:00:00Z")
    private val clock = Clock.fixed(now, ZoneId.of("UTC"))

    @Test
    fun resetPreservesHistoryAndExposesDerivedStatistics() {
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

        composeRule.onNodeWithText("Read daily").performClick()
        composeRule.onNodeWithText("Statistics").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Reset count").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("6%").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("reset-streak").assertIsDisplayed().performClick()
        composeRule.onNodeWithText(
            "The current period will be preserved in History",
            substring = true,
        ).assertIsDisplayed()
        composeRule.onNodeWithTag("reset-reason").performTextInput("Restarted plan")
        composeRule.onNodeWithTag("reset-note").performTextInput("Private reflection")
        composeRule.onNodeWithTag("review-reset-streak").performClick()
        composeRule.onNodeWithTag("confirm-reset-streak").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            val aggregate = repository.current.single()
            assertEquals(2, aggregate.periods.size)
            assertEquals(1, aggregate.periods.count { it.endEpochMs == null })
            val closed = aggregate.periods.single { it.endEpochMs != null }
            assertEquals("Restarted plan", closed.resetReason)
            assertEquals("Private reflection", closed.resetNote)
            assertEquals(30, aggregate.goal!!.targetAmount)
            assertEquals(now.toEpochMilli(), aggregate.tracker.updatedAtEpochMs)
        }
        composeRule.onNodeWithText("0%").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("open-history").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("history-screen").assertIsDisplayed()
        composeRule.onNodeWithText("1 completed period").assertIsDisplayed()
        composeRule.onNodeWithText("Restarted plan").assertIsDisplayed()
        composeRule.onNodeWithText("Note saved").assertIsDisplayed()
        composeRule.onNodeWithText("Private reflection").assertDoesNotExist()
        composeRule.onNodeWithText("Current").assertIsDisplayed()
    }

    @Test
    fun derivedStatisticsUsePersistedPeriods() {
        val repository = FakeTrackerRepository(
            initial = listOf(statisticsAggregate()),
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

        composeRule.onNodeWithText("Read daily").performClick()
        composeRule.onNodeWithText("Longest streak").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("3 d · 0 h · 0 min · 0 sec").assertIsDisplayed()
        composeRule.onNodeWithText("Reset count").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Last reset").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun resetConfirmationCanBeDisabledWithoutSkippingResetEditor() {
        val repository = FakeTrackerRepository(
            initial = listOf(sampleAggregate()),
            clock = clock,
        )

        composeRule.setContent {
            MaterialTheme {
                SinceApp(
                    repository = repository,
                    clock = clock,
                    confirmReset = false,
                )
            }
        }

        composeRule.onNodeWithText("Read daily").performClick()
        composeRule.onNodeWithTag("reset-streak").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("reset-reason").assertIsDisplayed()
        composeRule.onNodeWithTag("review-reset-streak").assertDoesNotExist()
        composeRule.onNodeWithTag("confirm-reset-streak").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(2, repository.current.single().periods.size)
        }
    }

    @Test
    fun largeFontResetAndHistoryKeepPrimaryActionsReachable() {
        val repository = FakeTrackerRepository(
            initial = listOf(sampleAggregate()),
            clock = clock,
        )

        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = 1f,
                    fontScale = 2f,
                )
            ) {
                MaterialTheme {
                    SinceApp(
                        repository = repository,
                        clock = clock,
                    )
                }
            }
        }

        composeRule.onNodeWithTag("dashboard-list").performScrollToIndex(3)
        composeRule
            .onNodeWithText("Read daily")
            .assertIsDisplayed()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithTag("reset-streak", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty() &&
                composeRule
                    .onAllNodesWithTag("open-history", useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
        }
        composeRule.onNodeWithTag("reset-streak").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("review-reset-streak").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onNodeWithTag("open-history").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("history-screen").assertIsDisplayed()
        composeRule.onNodeWithText("Back").assertIsDisplayed()
    }

    @Test
    fun forcedRtlResetAndHistoryKeepPrimaryActionsReachable() {
        val repository = FakeTrackerRepository(
            initial = listOf(sampleAggregate()),
            clock = clock,
        )

        composeRule.setContent {
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                MaterialTheme {
                    SinceApp(
                        repository = repository,
                        clock = clock,
                    )
                }
            }
        }

        composeRule.onNodeWithText("Read daily").performClick()
        composeRule.onNodeWithTag("reset-streak").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("review-reset-streak").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onNodeWithTag("open-history").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("history-screen").assertIsDisplayed()
        composeRule.onNodeWithText("Back").assertIsDisplayed()
    }

    private fun statisticsAggregate(): TrackerAggregate {
        val trackerId = "tracker-statistics"
        return TrackerAggregate(
            tracker = Tracker(
                id = trackerId,
                title = "Read daily",
                note = null,
                kind = TrackerKind.STREAK,
                iconKey = null,
                accentKey = null,
                defaultDisplayFormat = DisplayFormat.DAYS,
                sortOrder = 0,
                isArchived = false,
                createdAtEpochMs = now.minusSeconds(604_800).toEpochMilli(),
                updatedAtEpochMs = now.minusSeconds(86_400).toEpochMilli(),
            ),
            periods = listOf(
                TrackerPeriod(
                    id = "period-0",
                    trackerId = trackerId,
                    sequence = 0,
                    startEpochMs = now.minusSeconds(604_800).toEpochMilli(),
                    startZoneId = "UTC",
                    endEpochMs = now.minusSeconds(345_600).toEpochMilli(),
                    endZoneId = "UTC",
                    resetReason = "First reset",
                    resetNote = null,
                    createdAtEpochMs = now.minusSeconds(604_800).toEpochMilli(),
                    updatedAtEpochMs = now.minusSeconds(345_600).toEpochMilli(),
                ),
                TrackerPeriod(
                    id = "period-1",
                    trackerId = trackerId,
                    sequence = 1,
                    startEpochMs = now.minusSeconds(345_600).toEpochMilli(),
                    startZoneId = "UTC",
                    endEpochMs = now.minusSeconds(172_800).toEpochMilli(),
                    endZoneId = "UTC",
                    resetReason = "Second reset",
                    resetNote = null,
                    createdAtEpochMs = now.minusSeconds(345_600).toEpochMilli(),
                    updatedAtEpochMs = now.minusSeconds(172_800).toEpochMilli(),
                ),
                TrackerPeriod(
                    id = "period-2",
                    trackerId = trackerId,
                    sequence = 2,
                    startEpochMs = now.minusSeconds(86_400).toEpochMilli(),
                    startZoneId = "UTC",
                    endEpochMs = null,
                    endZoneId = null,
                    resetReason = null,
                    resetNote = null,
                    createdAtEpochMs = now.minusSeconds(86_400).toEpochMilli(),
                    updatedAtEpochMs = now.minusSeconds(86_400).toEpochMilli(),
                ),
            ),
            goal = null,
        )
    }

    private fun sampleAggregate(): TrackerAggregate {
        val trackerId = "tracker-history"
        return TrackerAggregate(
            tracker = Tracker(
                id = trackerId,
                title = "Read daily",
                note = "Keep the streak going.",
                kind = TrackerKind.STREAK,
                iconKey = null,
                accentKey = null,
                defaultDisplayFormat = DisplayFormat.DAYS,
                sortOrder = 0,
                isArchived = false,
                createdAtEpochMs = now.minusSeconds(172_800).toEpochMilli(),
                updatedAtEpochMs = now.minusSeconds(172_800).toEpochMilli(),
            ),
            periods = listOf(
                TrackerPeriod(
                    id = "period-current",
                    trackerId = trackerId,
                    sequence = 0,
                    startEpochMs = now.minusSeconds(172_800).toEpochMilli(),
                    startZoneId = "UTC",
                    endEpochMs = null,
                    endZoneId = null,
                    resetReason = null,
                    resetNote = null,
                    createdAtEpochMs = now.minusSeconds(172_800).toEpochMilli(),
                    updatedAtEpochMs = now.minusSeconds(172_800).toEpochMilli(),
                )
            ),
            goal = Goal(
                trackerId = trackerId,
                targetAmount = 30,
                targetUnit = DisplayFormat.DAYS,
                createdAtEpochMs = now.minusSeconds(172_800).toEpochMilli(),
                updatedAtEpochMs = now.minusSeconds(172_800).toEpochMilli(),
            ),
        )
    }
}