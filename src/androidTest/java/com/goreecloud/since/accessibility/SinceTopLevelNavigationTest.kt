package com.goreecloud.since.accessibility

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import com.goreecloud.since.data.preferences.ThemePreference
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

class SinceTopLevelNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val clock = Clock.fixed(
        Instant.parse("2026-09-24T05:00:00Z"),
        ZoneId.of("UTC"),
    )

    @Test
    fun homeAchievementsAndSettingsRemainReachable() {
        var selectedTheme = ThemePreference.SYSTEM

        composeRule.setContent {
            MaterialTheme {
                SinceApp(
                    repository = FakeTrackerRepository(listOf(sampleAggregate())),
                    clock = clock,
                    themePreference = selectedTheme,
                    onThemePreferenceChange = { selectedTheme = it },
                )
            }
        }

        composeRule.onNodeWithTag("nav-settings").assertHasClickAction().performClick()
        composeRule.onNodeWithTag("settings-screen").assertIsDisplayed()
        composeRule.onNodeWithTag("theme-dark").assertHasClickAction().performClick()

        composeRule.runOnIdle {
            assertEquals(ThemePreference.DARK, selectedTheme)
        }

        composeRule.onNodeWithTag("settings-list").performScrollToIndex(2)
        composeRule.onNodeWithTag("settings-backup")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()
        composeRule.onNodeWithText("Backup is not enabled yet.", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("Done").performClick()

        composeRule.onNodeWithTag("settings-restore")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()
        composeRule.onNodeWithText("Restore is not enabled yet.", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("Done").performClick()

        composeRule.onNodeWithTag("settings-list").performScrollToIndex(3)
        composeRule.onNodeWithText("Privacy").assertIsDisplayed()
        composeRule.onNodeWithText("Security").assertIsDisplayed()

        composeRule.onNodeWithTag("settings-list").performScrollToIndex(4)
        composeRule.onNodeWithText("App version").assertIsDisplayed()

        composeRule.onNodeWithTag("nav-achievements").assertHasClickAction().performClick()
        composeRule.onNodeWithTag("achievements-screen").assertIsDisplayed()
        composeRule.onNodeWithText("Getting started").assertIsDisplayed()
        composeRule.onNodeWithText("Goal setter").assertIsDisplayed()

        composeRule.onNodeWithTag("nav-home").assertHasClickAction().performClick()
        composeRule.onNodeWithText("Read daily").assertIsDisplayed()
    }

    private fun sampleAggregate(): TrackerAggregate {
        val trackerId = "tracker-top-level"
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
                createdAtEpochMs = 1_000L,
                updatedAtEpochMs = 1_000L,
            ),
            periods = listOf(
                TrackerPeriod(
                    id = "period-top-level",
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
