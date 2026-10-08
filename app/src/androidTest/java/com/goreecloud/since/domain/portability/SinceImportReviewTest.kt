package com.goreecloud.since.domain.portability

import com.goreecloud.since.domain.model.DisplayFormat
import com.goreecloud.since.domain.model.Goal
import com.goreecloud.since.domain.model.Tracker
import com.goreecloud.since.domain.model.TrackerAggregate
import com.goreecloud.since.domain.model.TrackerKind
import com.goreecloud.since.domain.model.TrackerPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SinceImportReviewTest {
    @Test
    fun exportedPayloadReviewsWithoutMutatingData() {
        val payload = SinceExportJson.encode(
            aggregates = listOf(
                sampleAggregate(
                    id = "active",
                    archived = false,
                    closedPeriods = 1,
                    withGoal = true,
                ),
                sampleAggregate(
                    id = "archived",
                    archived = true,
                    closedPeriods = 0,
                    withGoal = false,
                ),
            ),
            exportedAtEpochMs = 1_790_640_000_000L,
        )

        val result = SinceImportReviewJson.review(payload)

        assertTrue(result is SinceImportReviewResult.Valid)
        val summary = (result as SinceImportReviewResult.Valid).summary
        assertEquals(2, summary.trackerCount)
        assertEquals(1, summary.archivedTrackerCount)
        assertEquals(3, summary.periodCount)
        assertEquals(1, summary.goalCount)
        assertEquals(1_790_640_000_000L, summary.exportedAtEpochMs)
        assertEquals(setOf("active", "archived"), summary.trackerIds)

        val plan = SinceImportReplacementPlanner.plan(
            summary = summary,
            currentTrackerIds = setOf("active", "local-only"),
        )
        assertEquals(2, plan.currentTrackerCount)
        assertEquals(2, plan.importedTrackerCount)
        assertEquals(1, plan.matchingTrackerCount)
    }

    @Test
    fun unsupportedSchemaFailsClosed() {
        val payload = SinceExportJson.encode(
            aggregates = listOf(sampleAggregate("tracker", false, 0, false)),
            exportedAtEpochMs = 1_790_640_000_000L,
        ).replace("\"schemaVersion\": 1", "\"schemaVersion\": 2")

        assertTrue(SinceImportReviewJson.review(payload) is SinceImportReviewResult.Invalid)
    }

    @Test
    fun unknownFieldFailsClosed() {
        val payload = SinceExportJson.encode(
            aggregates = listOf(sampleAggregate("tracker", false, 0, false)),
            exportedAtEpochMs = 1_790_640_000_000L,
        ).replace(
            "\"exportedAtEpochMs\": 1790640000000,",
            "\"exportedAtEpochMs\": 1790640000000,\n  \"unexpected\": true,",
        )

        assertTrue(SinceImportReviewJson.review(payload) is SinceImportReviewResult.Invalid)
    }

    @Test
    fun trailingJsonFailsClosed() {
        val payload = SinceExportJson.encode(
            aggregates = listOf(sampleAggregate("tracker", false, 0, false)),
            exportedAtEpochMs = 1_790_640_000_000L,
        ) + "{}"

        assertTrue(SinceImportReviewJson.review(payload) is SinceImportReviewResult.Invalid)
    }

    @Test
    fun duplicateTrackerIdentityFailsClosed() {
        val aggregate = sampleAggregate("same-id", false, 0, false)
        val payload = SinceExportJson.encode(
            aggregates = listOf(aggregate, aggregate.copy()),
            exportedAtEpochMs = 1_790_640_000_000L,
        )

        assertTrue(SinceImportReviewJson.review(payload) is SinceImportReviewResult.Invalid)
    }

    private fun sampleAggregate(
        id: String,
        archived: Boolean,
        closedPeriods: Int,
        withGoal: Boolean,
    ): TrackerAggregate {
        val periods = buildList {
            repeat(closedPeriods) { index ->
                add(
                    TrackerPeriod(
                        id = "$id-period-$index",
                        trackerId = id,
                        sequence = index,
                        startEpochMs = 10_000L + (index * 10_000L),
                        startZoneId = "UTC",
                        endEpochMs = 15_000L + (index * 10_000L),
                        endZoneId = "UTC",
                        resetReason = "reset",
                        resetNote = null,
                        createdAtEpochMs = 1_000L,
                        updatedAtEpochMs = 2_000L,
                    ),
                )
            }
            add(
                TrackerPeriod(
                    id = "$id-current",
                    trackerId = id,
                    sequence = closedPeriods,
                    startEpochMs = 20_000L + (closedPeriods * 10_000L),
                    startZoneId = "UTC",
                    endEpochMs = null,
                    endZoneId = null,
                    resetReason = null,
                    resetNote = null,
                    createdAtEpochMs = 2_000L,
                    updatedAtEpochMs = 2_000L,
                ),
            )
        }

        return TrackerAggregate(
            tracker = Tracker(
                id = id,
                title = "Tracker $id",
                note = null,
                kind = TrackerKind.STREAK,
                iconKey = null,
                accentKey = null,
                defaultDisplayFormat = DisplayFormat.DAYS,
                sortOrder = if (archived) 1 else 0,
                isArchived = archived,
                createdAtEpochMs = 1_000L,
                updatedAtEpochMs = 2_000L,
            ),
            periods = periods,
            goal = if (withGoal) {
                Goal(
                    trackerId = id,
                    targetAmount = 30,
                    targetUnit = DisplayFormat.DAYS,
                    createdAtEpochMs = 1_000L,
                    updatedAtEpochMs = 2_000L,
                )
            } else {
                null
            },
        )
    }
}
