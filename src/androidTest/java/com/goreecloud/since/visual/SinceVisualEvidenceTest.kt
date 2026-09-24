package com.goreecloud.since.visual

import android.app.UiModeManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.goreecloud.since.MainActivity
import java.io.File
import java.io.FileOutputStream
import org.junit.Rule
import org.junit.Test

/**
 * Captures exact Android-rendered Development evidence for the principal GoreeCloud Since flow.
 *
 * These images are evidence inputs for human visual review. They do not, by themselves, establish
 * physical-device acceptance or downstream Glaze UI consumer conformance.
 */
class SinceVisualEvidenceTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun capturePrincipalSinceFlow() {
        try {
            setNightMode(UiModeManager.MODE_NIGHT_NO)
            waitForDisplayedText("Since")
            capture("dashboard-empty")

            setNightMode(UiModeManager.MODE_NIGHT_YES)
            waitForDisplayedText("Since")
            capture("dashboard-empty-dark")

            composeRule.onNodeWithText("Add tracker").performClick()
            composeRule.onNodeWithText("Choose tracker type").assertIsDisplayed()
            capture("tracker-type-chooser-dark")

            composeRule.onNodeWithTag("tracker-type-streak").performClick()
            composeRule.onNodeWithText("Create Streak").assertIsDisplayed()
            capture("create-streak-dark")

            composeRule.onNodeWithTag("title-field").performTextInput("Read daily")
            composeRule.onNodeWithText("Save").performScrollTo().performClick()

            composeRule.onNodeWithText("Read daily").assertIsDisplayed()
            composeRule.onNodeWithText("Elapsed").assertIsDisplayed()
            capture("tracker-details-dark")

            composeRule.onNodeWithText("Back").performClick()
            composeRule.onNodeWithText("Read daily").assertIsDisplayed()
            capture("dashboard-populated-dark")

            setNightMode(UiModeManager.MODE_NIGHT_NO)
            waitForDisplayedText("Read daily")
            capture("dashboard-populated")

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

            composeRule.onNodeWithText("Cancel").performClick()
        } finally {
            runCatching { setNightMode(UiModeManager.MODE_NIGHT_NO) }
        }
    }

    private fun waitForDisplayedText(text: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNodeWithText(text).assertIsDisplayed()
            }.isSuccess
        }
    }

    private fun setNightMode(mode: Int) {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            "Rendered night-mode evidence requires Android 12 or newer."
        }

        val activityBeforeChange = composeRule.activity
        val previousNightMask = activityBeforeChange.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK
        val uiModeManager = InstrumentationRegistry.getInstrumentation()
            .targetContext
            .getSystemService(UiModeManager::class.java)
        uiModeManager.setApplicationNightMode(mode)

        val expectedNightMask = when (mode) {
            UiModeManager.MODE_NIGHT_YES -> Configuration.UI_MODE_NIGHT_YES
            else -> Configuration.UI_MODE_NIGHT_NO
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            val currentActivity = runCatching { composeRule.activity }.getOrNull()
            currentActivity != null &&
                (
                    currentActivity.resources.configuration.uiMode and
                        Configuration.UI_MODE_NIGHT_MASK
                ) == expectedNightMask &&
                (
                    previousNightMask == expectedNightMask ||
                        currentActivity !== activityBeforeChange
                )
        }
        composeRule.waitForIdle()
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
