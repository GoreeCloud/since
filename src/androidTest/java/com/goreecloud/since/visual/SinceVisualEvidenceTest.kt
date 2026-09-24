package com.goreecloud.since.visual

import android.graphics.Bitmap
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
        composeRule.onNodeWithText("Since").assertIsDisplayed()
        capture("dashboard-empty")

        composeRule.onNodeWithText("Add tracker").performClick()
        composeRule.onNodeWithText("Choose tracker type").assertIsDisplayed()
        capture("tracker-type-chooser")

        composeRule.onNodeWithText("Streak").performClick()
        composeRule.onNodeWithText("Create Streak").assertIsDisplayed()
        capture("create-streak")

        composeRule.onNodeWithTag("title-field").performTextInput("Read daily")
        composeRule.onNodeWithText("Save").performScrollTo().performClick()

        composeRule.onNodeWithText("Read daily").assertIsDisplayed()
        composeRule.onNodeWithText("Elapsed").assertIsDisplayed()
        capture("tracker-details")

        composeRule.onNodeWithText("Back").performClick()
        composeRule.onNodeWithText("Read daily").assertIsDisplayed()
        capture("dashboard-populated")
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
