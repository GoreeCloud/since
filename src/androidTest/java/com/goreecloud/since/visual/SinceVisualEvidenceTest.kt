package com.goreecloud.since.visual

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.goreecloud.since.testutil.FakeTrackerRepository
import com.goreecloud.since.ui.SinceApp
import com.goreecloud.since.ui.theme.SinceTheme
import java.io.File
import java.io.FileOutputStream
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test

/**
 * Captures deterministic Android-rendered Development evidence for the principal GoreeCloud Since
 * flow.
 *
 * Theme evidence is switched inside the Compose tree instead of recreating the host Activity.
 * Persistence and Activity-recreation behavior remain covered by the dedicated runtime tests; this
 * visual test is intentionally responsible only for stable, reviewable UI evidence.
 *
 * These images are evidence inputs for human visual review. They do not, by themselves, establish
 * physical-device acceptance or downstream Glaze UI consumer conformance.
 */
class SinceVisualEvidenceTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val clock = Clock.fixed(
        Instant.parse("2026-09-24T19:00:00Z"),
        ZoneId.of("UTC"),
    )

    @Test
    fun capturePrincipalSinceFlow() {
        val repository = FakeTrackerRepository(
            initial = emptyList(),
            clock = clock,
        )
        var darkTheme by mutableStateOf(false)

        composeRule.setContent {
            SinceTheme(darkTheme = darkTheme) {
                SinceApp(
                    repository = repository,
                    clock = clock,
                )
            }
        }

        composeRule.onNodeWithText("Since").assertIsDisplayed()
        capture("dashboard-empty")

        composeRule.runOnIdle {
            darkTheme = true
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Since").assertIsDisplayed()
        capture("dashboard-empty-dark")

        composeRule.onNodeWithText("Add tracker").performClick()
        composeRule.onNodeWithText("Choose tracker type").assertIsDisplayed()
        capture("tracker-type-chooser-dark")

        composeRule.onNodeWithTag("tracker-type-streak").performClick()
        composeRule.onNodeWithText("Create Streak").assertIsDisplayed()
        capture("create-streak-dark")

        composeRule.onNodeWithTag("start-zone-picker").performScrollTo().performClick()
        composeRule.onNodeWithTag("start-zone-picker-dialog").assertIsDisplayed()
        capture("time-zone-picker-dark")
        composeRule.onNodeWithTag("device-time-zone-option").performClick()

        composeRule.onNodeWithTag("title-field").performTextInput("Read daily")
        composeRule.onNodeWithText("Save").performScrollTo().performClick()

        composeRule.onNodeWithText("Read daily").assertIsDisplayed()
        composeRule.onNodeWithText("Elapsed").assertIsDisplayed()
        capture("tracker-details-dark")

        composeRule.onNodeWithText("Back").performClick()
        composeRule.onNodeWithText("Read daily").assertIsDisplayed()
        capture("dashboard-populated-dark")

        composeRule.runOnIdle {
            darkTheme = false
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Read daily").assertIsDisplayed()
        capture("dashboard-populated")

        composeRule.onNodeWithTag("nav-achievements").performClick()
        composeRule.onNodeWithTag("achievements-screen").assertIsDisplayed()
        capture("achievements")

        composeRule.onNodeWithTag("nav-settings").performClick()
        composeRule.onNodeWithTag("settings-screen").assertIsDisplayed()
        capture("settings")
        composeRule.onNodeWithTag("settings-list").performScrollToIndex(2)
        capture("settings-recovery")
        composeRule.onNodeWithTag("settings-list").performScrollToIndex(4)
        composeRule.onNodeWithText("App version").assertIsDisplayed()
        capture("settings-about")

        composeRule.runOnIdle {
            darkTheme = true
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings-list").performScrollToIndex(0)
        composeRule.onNodeWithTag("settings-screen").assertIsDisplayed()
        capture("settings-dark")
        composeRule.onNodeWithTag("settings-list").performScrollToIndex(2)
        capture("settings-recovery-dark")
        composeRule.onNodeWithTag("settings-list").performScrollToIndex(4)
        composeRule.onNodeWithText("App version").assertIsDisplayed()
        capture("settings-about-dark")

        composeRule.onNodeWithTag("nav-achievements").performClick()
        composeRule.onNodeWithTag("achievements-screen").assertIsDisplayed()
        capture("achievements-dark")

        composeRule.runOnIdle {
            darkTheme = false
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav-home").performClick()
        composeRule.onNodeWithText("Read daily").assertIsDisplayed()

        composeRule.onNodeWithText("Read daily").performClick()
        composeRule.onNodeWithText("Elapsed").assertIsDisplayed()
        capture("tracker-details")

        composeRule.onNodeWithText("Back").performClick()
        composeRule.onNodeWithText("Add tracker").performClick()
        composeRule.onNodeWithText("Choose tracker type").assertIsDisplayed()
        capture("tracker-type-chooser")

        composeRule.onNodeWithTag("tracker-type-streak").performClick()
        composeRule.onNodeWithText("Create Streak").assertIsDisplayed()
        capture("create-streak")

        composeRule.onNodeWithTag("start-zone-picker").performScrollTo().performClick()
        composeRule.onNodeWithTag("start-zone-picker-dialog").assertIsDisplayed()
        capture("time-zone-picker")
        composeRule.onNodeWithTag("device-time-zone-option").performClick()

        composeRule.onNodeWithText("Cancel").performClick()
    }

    private fun capture(name: String) {
        composeRule.waitForIdle()

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val outputDirectory = File(
            instrumentation.targetContext.filesDir,
            "visual-evidence",
        ).apply {
            check(mkdirs() || isDirectory)
        }
        val outputFile = File(outputDirectory, "$name.png")
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot()) {
            "Android UI automation did not return a screenshot."
        }

        FileOutputStream(outputFile).use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                "Failed to encode visual evidence screenshot: $name"
            }
        }
        bitmap.recycle()
        check(outputFile.isFile && outputFile.length() > 0L) {
            "Visual evidence screenshot was not written: $name"
        }
    }
}

