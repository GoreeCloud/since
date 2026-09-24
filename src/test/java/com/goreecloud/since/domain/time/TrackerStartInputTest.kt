package com.goreecloud.since.domain.time

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackerStartInputTest {
    @Test
    fun resolvesOrdinaryLocalTimeInRequestedZone() {
        val result = TrackerStartInput.resolve(
            localDateTimeText = "2026-09-23 12:30",
            zoneIdText = "America/Chicago",
        )

        val valid = result as TrackerStartResolution.Valid
        assertEquals(
            Instant.parse("2026-09-23T17:30:00Z").toEpochMilli(),
            valid.start.epochMs,
        )
        assertEquals("America/Chicago", valid.start.zoneId)
        assertTrue(!valid.start.overlapResolvedToEarlierOffset)
    }

    @Test
    fun rejectsDstGapInsteadOfSilentlyShiftingWallTime() {
        val result = TrackerStartInput.resolve(
            localDateTimeText = "2026-03-08 02:30",
            zoneIdText = "America/New_York",
        )

        val invalid = result as TrackerStartResolution.Invalid
        assertTrue(invalid.errors.any { it.contains("does not exist") })
    }

    @Test
    fun resolvesDstOverlapToEarlierOffsetDeterministically() {
        val result = TrackerStartInput.resolve(
            localDateTimeText = "2026-11-01 01:30",
            zoneIdText = "America/New_York",
        )

        val valid = result as TrackerStartResolution.Valid
        assertEquals(
            Instant.parse("2026-11-01T05:30:00Z").toEpochMilli(),
            valid.start.epochMs,
        )
        assertTrue(valid.start.overlapResolvedToEarlierOffset)
    }

    @Test
    fun rejectsMalformedDateTimeAndUnknownZone() {
        val result = TrackerStartInput.resolve(
            localDateTimeText = "09/23/2026 12:30",
            zoneIdText = "Not/A_Zone",
        )

        val invalid = result as TrackerStartResolution.Invalid
        assertEquals(2, invalid.errors.size)
    }
}
