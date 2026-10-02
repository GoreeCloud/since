package com.goreecloud.since.domain.portability

import com.goreecloud.since.domain.model.DisplayFormat
import com.goreecloud.since.domain.model.Goal
import com.goreecloud.since.domain.model.Tracker
import com.goreecloud.since.domain.model.TrackerAggregate
import com.goreecloud.since.domain.model.TrackerKind
import com.goreecloud.since.domain.model.TrackerPeriod
import java.time.Instant
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class SinceExportJsonTest {
    @Test
    fun exportIsVersionedDeterministicAndEscapesUserText() {
        val later = sampleAggregate(
            id = "later",
            sortOrder = 5,
            title = "Tea \"daily\"\n",
            note = "Backslash \\ and tab\t",
            archived = true,
        )
        val earlier = sampleAggregate(
            id = "earlier",
            sortOrder = 1,
            title = "Read",
            note = null,
            archived = false,
        )

        val json = SinceExportJson.encode(
            aggregates = listOf(later, earlier),
            exportedAtEpochMs = 1_790_640_000_000L,
        )

        assertTrue(json.startsWith("{\n  \"format\": \"goreecloud-since-export\""))
        assertTrue(json.contains("\"schemaVersion\": 1"))
        assertTrue(json.contains("\"title\": \"Tea \\\"daily\\\"\\n\""))
        assertTrue(json.contains("\"note\": \"Backslash \\\\ and tab\\t\""))
        assertTrue(json.contains("\"isArchived\": true"))
        assertTrue(json.contains("\"goal\": {\"targetAmount\": 30, \"targetUnit\": \"DAYS\""))
        assertTrue(json.indexOf("\"id\": \"earlier\"") < json.indexOf("\"id\": \"later\""))
        assertTrue(json.endsWith("\n"))
    }

    @Test
    fun exportFileNameUsesStableUtcCalendarDate() {
        val exportedAt = Instant.parse("2026-09-29T23:59:59Z").toEpochMilli()
        assertEquals("GoreeCloud-Since-2026-09-29.json", SinceExportJson.fileName(exportedAt))
    }

    private fun sampleAggregate(
        id: String,
        sortOrder: Int,
        title: String,
        note: String?,
        archived: Boolean,
    ): TrackerAggregate = TrackerAggregate(
        tracker = Tracker(
            id = id,
            title = title,
            note = note,
            kind = TrackerKind.STREAK,
            iconKey = null,
            accentKey = null,
            defaultDisplayFormat = DisplayFormat.DAYS,
            sortOrder = sortOrder,
            isArchived = archived,
            createdAtEpochMs = 1_000L + sortOrder,
            updatedAtEpochMs = 2_000L + sortOrder,
        ),
        periods = listOf(
            TrackerPeriod(
                id = "period-$id",
                trackerId = id,
                sequence = 0,
                startEpochMs = 10_000L,
                startZoneId = "UTC",
                endEpochMs = null,
                endZoneId = null,
                resetReason = null,
                resetNote = null,
                createdAtEpochMs = 1_000L,
                updatedAtEpochMs = 1_000L,
            ),
        ),
        goal = Goal(
            trackerId = id,
            targetAmount = 30,
            targetUnit = DisplayFormat.DAYS,
            createdAtEpochMs = 1_000L,
            updatedAtEpochMs = 1_000L,
        ),
    )
}
