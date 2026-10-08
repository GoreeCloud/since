package com.goreecloud.since.ui

import com.goreecloud.since.data.preferences.DashboardSortPreference
import com.goreecloud.since.domain.model.DisplayFormat
import com.goreecloud.since.domain.model.Tracker
import com.goreecloud.since.domain.model.TrackerAggregate
import com.goreecloud.since.domain.model.TrackerKind
import com.goreecloud.since.domain.model.TrackerPeriod
import org.junit.Assert.assertEquals
import org.junit.Test

class SinceDashboardQueryTest {
    @Test
    fun searchMatchesTitleAndNoteIgnoringCaseAndAccents() {
        val rows = listOf(
            aggregate("a", "Café", "Morning routine", updatedAt = 100),
            aggregate("b", "Exercise", "CAFÉ break follows", updatedAt = 200),
            aggregate("c", "Reading", null, updatedAt = 300),
        )

        val result = SinceDashboardQuery.apply(
            rows,
            "cafe",
            DashboardSortPreference.MANUAL,
        )

        assertEquals(listOf("a", "b"), result.map { it.tracker.id })
    }

    @Test
    fun manualSortUsesSortOrderThenCreatedOrder() {
        val rows = listOf(
            aggregate("c", "Third", null, updatedAt = 300, sortOrder = 2, createdAt = 30),
            aggregate("b", "Second", null, updatedAt = 200, sortOrder = 0, createdAt = 20),
            aggregate("a", "First", null, updatedAt = 100, sortOrder = 0, createdAt = 10),
        )

        val result = SinceDashboardQuery.apply(
            rows,
            "",
            DashboardSortPreference.MANUAL,
        )

        assertEquals(listOf("a", "b", "c"), result.map { it.tracker.id })
    }

    @Test
    fun titleSortIsAccentAndCaseInsensitiveWithDeterministicTies() {
        val rows = listOf(
            aggregate("b", "beta", null, updatedAt = 300, createdAt = 30),
            aggregate("a", "Álpha", null, updatedAt = 100, createdAt = 10),
            aggregate("c", "alpha", null, updatedAt = 200, createdAt = 20),
        )

        val result = SinceDashboardQuery.apply(
            rows,
            "",
            DashboardSortPreference.TITLE,
        )

        assertEquals(listOf("a", "c", "b"), result.map { it.tracker.id })
    }

    @Test
    fun newestAndOldestStartUseCurrentOpenPeriod() {
        val rows = listOf(
            aggregate("a", "A", null, updatedAt = 100, startAt = 10),
            aggregate("b", "B", null, updatedAt = 200, startAt = 30),
            aggregate("c", "C", null, updatedAt = 300, startAt = 20),
        )

        val newest = SinceDashboardQuery.apply(
            rows,
            "",
            DashboardSortPreference.NEWEST_START,
        )
        val oldest = SinceDashboardQuery.apply(
            rows,
            "",
            DashboardSortPreference.OLDEST_START,
        )

        assertEquals(listOf("b", "c", "a"), newest.map { it.tracker.id })
        assertEquals(listOf("a", "c", "b"), oldest.map { it.tracker.id })
    }

    @Test
    fun longestCurrentElapsedUsesEarliestCurrentStart() {
        val rows = listOf(
            aggregate("a", "A", null, updatedAt = 100, startAt = 90),
            aggregate("b", "B", null, updatedAt = 200, startAt = 10),
            aggregate("c", "C", null, updatedAt = 300, startAt = 50),
        )

        val result = SinceDashboardQuery.apply(
            rows,
            "",
            DashboardSortPreference.LONGEST_CURRENT,
        )

        assertEquals(listOf("b", "c", "a"), result.map { it.tracker.id })
    }

    private fun aggregate(
        id: String,
        title: String,
        note: String?,
        updatedAt: Long,
        sortOrder: Int = 0,
        createdAt: Long = 1,
        startAt: Long = 0,
    ): TrackerAggregate = TrackerAggregate(
        tracker = Tracker(
            id = id,
            title = title,
            note = note,
            kind = TrackerKind.EVENT,
            iconKey = null,
            accentKey = null,
            defaultDisplayFormat = DisplayFormat.DAYS,
            sortOrder = sortOrder,
            isArchived = false,
            createdAtEpochMs = createdAt,
            updatedAtEpochMs = updatedAt,
        ),
        periods = listOf(
            TrackerPeriod(
                id = "$id-period",
                trackerId = id,
                sequence = 0,
                startEpochMs = startAt,
                startZoneId = "UTC",
                endEpochMs = null,
                endZoneId = null,
                resetReason = null,
                resetNote = null,
                createdAtEpochMs = createdAt,
                updatedAtEpochMs = updatedAt,
            ),
        ),
        goal = null,
    )
}
