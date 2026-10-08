package com.goreecloud.since.domain.time

import com.goreecloud.since.domain.model.DisplayFormat
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeEngineTest {
    private val zone = ZoneId.of("America/Chicago")
    private val engine = TimeEngine(Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("UTC")))

    @Test
    fun dayMode_countsCalendarDayAcrossSpringDstGap() {
        val start = ZonedDateTime.of(2026, 3, 7, 12, 0, 0, 0, zone).toInstant()
        val end = ZonedDateTime.of(2026, 3, 8, 12, 0, 0, 0, zone).toInstant()

        val result = engine.elapsedBetween(start, end, zone, DisplayFormat.DAYS)

        val value = result as ElapsedResult.Value
        assertEquals(1, value.breakdown.days)
        assertEquals(0, value.breakdown.hours)
    }

    @Test
    fun monthMode_usesCalendarMonthAtMonthEnd() {
        val start = ZonedDateTime.of(2026, 1, 31, 10, 0, 0, 0, zone).toInstant()
        val end = ZonedDateTime.of(2026, 2, 28, 10, 0, 0, 0, zone).toInstant()

        val result = engine.elapsedBetween(start, end, zone, DisplayFormat.MONTHS)

        val value = result as ElapsedResult.Value
        assertEquals(1, value.breakdown.months)
        assertEquals(0, value.breakdown.days)
    }

    @Test
    fun yearMode_handlesLeapDayUsingCalendarArithmetic() {
        val start = ZonedDateTime.of(2024, 2, 29, 9, 0, 0, 0, zone).toInstant()
        val end = ZonedDateTime.of(2025, 2, 28, 9, 0, 0, 0, zone).toInstant()

        val result = engine.elapsedBetween(start, end, zone, DisplayFormat.YEARS)

        val value = result as ElapsedResult.Value
        assertEquals(1, value.breakdown.years)
        assertEquals(0, value.breakdown.months)
        assertEquals(0, value.breakdown.days)
    }

    @Test
    fun weekMode_preservesRemainingCalendarDaysAndHours() {
        val start = ZonedDateTime.of(2026, 4, 1, 8, 0, 0, 0, zone).toInstant()
        val end = ZonedDateTime.of(2026, 4, 16, 11, 0, 0, 0, zone).toInstant()

        val result = engine.elapsedBetween(start, end, zone, DisplayFormat.WEEKS)

        val value = result as ElapsedResult.Value
        assertEquals(2, value.breakdown.weeks)
        assertEquals(1, value.breakdown.days)
        assertEquals(3, value.breakdown.hours)
    }

    @Test
    fun calendarComparison_prefersMoreCalendarDaysAcrossSpringDstGap() {
        val twoCalendarDaysStart =
            ZonedDateTime.of(2026, 3, 7, 12, 0, 0, 0, zone).toInstant()
        val twoCalendarDaysEnd =
            ZonedDateTime.of(2026, 3, 9, 12, 0, 0, 0, zone).toInstant()
        val longerRawDurationStart =
            ZonedDateTime.of(2026, 3, 5, 12, 0, 0, 0, zone).toInstant()
        val longerRawDurationEnd =
            ZonedDateTime.of(2026, 3, 7, 11, 30, 0, 0, zone).toInstant()

        assertTrue(
            twoCalendarDaysEnd.toEpochMilli() - twoCalendarDaysStart.toEpochMilli() <
                longerRawDurationEnd.toEpochMilli() - longerRawDurationStart.toEpochMilli()
        )
        assertTrue(
            engine.compareCalendarElapsed(
                firstStart = twoCalendarDaysStart,
                firstEnd = twoCalendarDaysEnd,
                firstZone = zone,
                secondStart = longerRawDurationStart,
                secondEnd = longerRawDurationEnd,
                secondZone = zone,
            ) > 0
        )
    }

    @Test
    fun earlierEndFailsClosedAsClockInconsistency() {
        val start = Instant.parse("2026-01-02T00:00:00Z")
        val end = Instant.parse("2026-01-01T00:00:00Z")

        assertTrue(engine.elapsedBetween(start, end, zone, DisplayFormat.DAYS) is ElapsedResult.ClockInconsistency)
    }
}
