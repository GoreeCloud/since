package com.goreecloud.since.domain.validation

import com.goreecloud.since.domain.model.DisplayFormat
import com.goreecloud.since.domain.model.TrackerKind
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackerDraftValidatorTest {
    private val now = Instant.parse("2026-09-23T18:00:00Z")
    private val validator = TrackerDraftValidator(Clock.fixed(now, ZoneId.of("UTC")))

    @Test
    fun normalizesAndAcceptsValidStreakGoal() {
        val result = validator.validate(
            TrackerDraft(
                title = "  No Junk  ",
                note = "  One day at a time.  ",
                kind = TrackerKind.STREAK,
                startEpochMs = now.minusSeconds(60).toEpochMilli(),
                startZoneId = "America/Chicago",
                displayFormat = DisplayFormat.DAYS,
                goalAmount = 50,
                goalUnit = DisplayFormat.DAYS,
            )
        )

        val valid = result as TrackerDraftValidation.Valid
        assertEquals("No Junk", valid.draft.title)
        assertEquals("One day at a time.", valid.draft.note)
        assertEquals(50, valid.draft.goalAmount)
    }

    @Test
    fun rejectsGoalForPermanentEvent() {
        val result = validator.validate(
            TrackerDraft(
                title = "First car",
                note = null,
                kind = TrackerKind.EVENT,
                startEpochMs = now.minusSeconds(60).toEpochMilli(),
                startZoneId = "America/Chicago",
                displayFormat = DisplayFormat.YEARS,
                goalAmount = 1,
                goalUnit = DisplayFormat.YEARS,
            )
        )

        assertTrue(result is TrackerDraftValidation.Invalid)
    }

    @Test
    fun rejectsFutureStartBeyondTolerance() {
        val result = validator.validate(
            TrackerDraft(
                title = "Future",
                note = null,
                kind = TrackerKind.STREAK,
                startEpochMs = now.plusSeconds(10).toEpochMilli(),
                startZoneId = "America/Chicago",
                displayFormat = DisplayFormat.DAYS,
            )
        )

        val invalid = result as TrackerDraftValidation.Invalid
        assertTrue(invalid.errors.any { it.contains("future", ignoreCase = true) })
    }

    @Test
    fun rejectsUnknownZone() {
        val result = validator.validate(
            TrackerDraft(
                title = "Zone",
                note = null,
                kind = TrackerKind.EVENT,
                startEpochMs = now.toEpochMilli(),
                startZoneId = "Not/A_Zone",
                displayFormat = DisplayFormat.DAYS,
            )
        )

        val invalid = result as TrackerDraftValidation.Invalid
        assertTrue(invalid.errors.any { it.contains("ZoneId") })
    }
}
