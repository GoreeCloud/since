package com.goreecloud.since.data.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.goreecloud.since.domain.model.DisplayFormat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SincePreferencesRepositoryRuntimeTest {
    @Test
    fun displayPreferencesPersistAcrossRepositoryInstances() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val first = SincePreferencesRepository(context)

        try {
            first.setDefaultDisplayFormat(DisplayFormat.MONTHS)
            first.setShowSeconds(false)
            first.setConfirmReset(false)

            val reopened = SincePreferencesRepository(context)

            assertEquals(DisplayFormat.MONTHS, reopened.defaultDisplayFormat.first())
            assertFalse(reopened.showSeconds.first())
            assertFalse(reopened.confirmReset.first())

            reopened.setDefaultDisplayFormat(DisplayFormat.YEARS)
            reopened.setShowSeconds(true)
            reopened.setConfirmReset(true)

            val secondReopen = SincePreferencesRepository(context)
            assertEquals(DisplayFormat.YEARS, secondReopen.defaultDisplayFormat.first())
            assertTrue(secondReopen.showSeconds.first())
            assertTrue(secondReopen.confirmReset.first())
        } finally {
            first.setDefaultDisplayFormat(DisplayFormat.DAYS)
            first.setShowSeconds(true)
            first.setConfirmReset(true)
        }
    }
}
