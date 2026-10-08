package com.goreecloud.since.data.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SinceDashboardSortPreferenceRuntimeTest {
    @Test
    fun dashboardSortPersistsAcrossRepositoryInstances() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val first = SincePreferencesRepository(context)

        try {
            first.setDashboardSort(DashboardSortPreference.NEWEST_START)

            val reopened = SincePreferencesRepository(context)
            assertEquals(
                DashboardSortPreference.NEWEST_START,
                reopened.dashboardSort.first(),
            )

            reopened.setDashboardSort(DashboardSortPreference.LONGEST_CURRENT)

            val secondReopen = SincePreferencesRepository(context)
            assertEquals(
                DashboardSortPreference.LONGEST_CURRENT,
                secondReopen.dashboardSort.first(),
            )
        } finally {
            first.setDashboardSort(DashboardSortPreference.MANUAL)
        }
    }
}
