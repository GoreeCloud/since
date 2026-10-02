package com.goreecloud.since.domain.validation

import com.goreecloud.since.domain.model.DisplayFormat
import com.goreecloud.since.domain.model.Goal
import com.goreecloud.since.domain.model.Tracker
import com.goreecloud.since.domain.model.TrackerAggregate
import com.goreecloud.since.domain.model.TrackerKind
import com.goreecloud.since.domain.model.TrackerPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TrackerDuplicateDraftTest {
    @Test
    fun duplicatesOnlyCurrentConfigurationAndGoal() {
        val aggregate = TrackerAggregate(
            tracker = Tracker(
                id = "tracker-1",
                title = "Read daily",
                note = "Keep going",
                kind = TrackerKind.STREAK,
                iconKey = null,
                accentKey = null,
                defaultDisplayFormat = DisplayFormat.DAYS,
                sortOrder = 4,
                isArchived = false,
                createdAtEpochMs = 100L,
                updatedAtEpochMs = 500L,
            ),
            periods = listOf(
                TrackerPeriod(
                    id = "closed",
                    trackerId = "tracker-1",
                    sequence = 0,
                    startEpochMs = 100L,
                    startZoneId = "UTC",
                    endEpochMs = 200L,
                    endZoneId = "UTC",
                    resetReason = "Travel",
                    resetNote = "Historical reset metadata",
                    createdAtEpochMs = 100L,
                    updatedAtEpochMs = 200L,
                ),
                TrackerPeriod(
                    id = "open",
                    trackerId = "tracker-1",
                    sequence = 1,
                    startEpochMs = 300L,
                    startZoneId = "America/New_York",
                    endEpochMs = null,
                    endZoneId = null,
                    resetReason = null,
                    resetNote = null,
                    createdAtEpochMs = 300L,
                    updatedAtEpochMs = 300L,
                ),
            ),
            goal = Goal(
                trackerId = "tracker-1",
                targetAmount = 30,
                targetUnit = DisplayFormat.DAYS,
                createdAtEpochMs = 120L,
                updatedAtEpochMs = 120L,
            ),
        )

        val draft = TrackerDuplicateDraft.from(aggregate)

        assertEquals("Read daily", draft.title)
        assertEquals("Keep going", draft.note)
        assertEquals(TrackerKind.STREAK, draft.kind)
        assertEquals(300L, draft.startEpochMs)
        assertEquals("America/New_York", draft.startZoneId)
        assertEquals(DisplayFormat.DAYS, draft.displayFormat)
        assertEquals(30, draft.goalAmount)
        assertEquals(DisplayFormat.DAYS, draft.goalUnit)
        assertFalse(draft.note.orEmpty().contains("Historical reset metadata"))
    }

    @Test
    fun eventDuplicateCarriesNoGoal() {
        val aggregate = TrackerAggregate(
            tracker = Tracker(
                id = "event-1",
                title = "Moved in",
                note = null,
                kind = TrackerKind.EVENT,
                iconKey = null,
                accentKey = null,
                defaultDisplayFormat = DisplayFormat.YEARS,
                sortOrder = 1,
                isArchived = false,
                createdAtEpochMs = 100L,
                updatedAtEpochMs = 100L,
            ),
            periods = listOf(
                TrackerPeriod(
                    id = "event-period",
                    trackerId = "event-1",
                    sequence = 0,
                    startEpochMs = 100L,
                    startZoneId = "UTC",
                    endEpochMs = null,
                    endZoneId = null,
                    resetReason = null,
                    resetNote = null,
                    createdAtEpochMs = 100L,
                    updatedAtEpochMs = 100L,
                ),
            ),
            goal = null,
        )

        val draft = TrackerDuplicateDraft.from(aggregate)

        assertEquals(null, draft.goalAmount)
        assertEquals(null, draft.goalUnit)
    }
}
