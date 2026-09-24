package com.goreecloud.since.domain.time

import com.goreecloud.since.domain.model.DisplayFormat
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalEstimatorTest {
    private val chicago = ZoneId.of("America/Chicago")

    @Test
    fun oneDayGoalUsesCalendarDayAcrossSpringDstGap() {
        val start = ZonedDateTime.of(2026, 3, 7, 12, 0, 0, 0, chicago).toInstant()
        val target = ZonedDateTime.of(2026, 3, 8, 12, 0, 0, 0, chicago).toInstant()
        val estimator = GoalEstimator(Clock.fixed(target, ZoneId.of("UTC")))

        val result = estimator.estimate(
            startEpochMs = start.toEpochMilli(),
            zoneId = chicago.id,
            targetAmount = 1,
            targetUnit = DisplayFormat.DAYS,
        ) as GoalEstimateResult.Value

        assertEquals(target.toEpochMilli(), result.estimate.targetEpochMs)
        assertEquals(100L, result.estimate.percent)
        assertTrue(result.estimate.isComplete)
    }

    @Test
    fun oneMonthGoalClampsCalendarMonthEnd() {
        val start = ZonedDateTime.of(2026, 1, 31, 10, 0, 0, 0, chicago).toInstant()
        val target = ZonedDateTime.of(2026, 2, 28, 10, 0, 0, 0, chicago).toInstant()
        val estimator = GoalEstimator(Clock.fixed(target, ZoneId.of("UTC")))

        val result = estimator.estimate(
            startEpochMs = start.toEpochMilli(),
            zoneId = chicago.id,
            targetAmount = 1,
            targetUnit = DisplayFormat.MONTHS,
        ) as GoalEstimateResult.Value

        assertEquals(target.toEpochMilli(), result.estimate.targetEpochMs)
        assertTrue(result.estimate.isComplete)
    }

    @Test
    fun oneYearGoalHandlesLeapDay() {
        val start = ZonedDateTime.of(2024, 2, 29, 9, 0, 0, 0, chicago).toInstant()
        val target = ZonedDateTime.of(2025, 2, 28, 9, 0, 0, 0, chicago).toInstant()
        val estimator = GoalEstimator(Clock.fixed(target, ZoneId.of("UTC")))

        val result = estimator.estimate(
            startEpochMs = start.toEpochMilli(),
            zoneId = chicago.id,
            targetAmount = 1,
            targetUnit = DisplayFormat.YEARS,
        ) as GoalEstimateResult.Value

        assertEquals(target.toEpochMilli(), result.estimate.targetEpochMs)
        assertEquals(100L, result.estimate.percent)
    }

    @Test
    fun progressCanExceedOneHundredPercentWithoutFreezing() {
        val start = Instant.parse("2026-09-20T00:00:00Z")
        val now = Instant.parse("2026-09-21T12:00:00Z")
        val estimator = GoalEstimator(Clock.fixed(now, ZoneId.of("UTC")))

        val result = estimator.estimate(
            startEpochMs = start.toEpochMilli(),
            zoneId = "UTC",
            targetAmount = 1,
            targetUnit = DisplayFormat.DAYS,
        ) as GoalEstimateResult.Value

        assertEquals(150L, result.estimate.percent)
        assertTrue(result.estimate.progressFraction > 1.0)
        assertTrue(result.estimate.isComplete)
    }

    @Test
    fun newCurrentPeriodRestartsGoalProgress() {
        val start = Instant.parse("2026-09-24T00:00:00Z")
        val now = Instant.parse("2026-09-24T12:00:00Z")
        val estimator = GoalEstimator(Clock.fixed(now, ZoneId.of("UTC")))

        val result = estimator.estimate(
            startEpochMs = start.toEpochMilli(),
            zoneId = "UTC",
            targetAmount = 1,
            targetUnit = DisplayFormat.DAYS,
        ) as GoalEstimateResult.Value

        assertEquals(50L, result.estimate.percent)
        assertFalse(result.estimate.isComplete)
    }

    @Test
    fun wallClockBeforeCurrentPeriodFailsClosed() {
        val estimator = GoalEstimator(
            Clock.fixed(Instant.parse("2026-09-23T23:59:00Z"), ZoneId.of("UTC"))
        )

        val result = estimator.estimate(
            startEpochMs = Instant.parse("2026-09-24T00:00:00Z").toEpochMilli(),
            zoneId = "UTC",
            targetAmount = 2,
            targetUnit = DisplayFormat.WEEKS,
        )

        assertTrue(result is GoalEstimateResult.ClockInconsistency)
    }
}
