package com.goreecloud.since.domain.time

import com.goreecloud.since.domain.model.DisplayFormat
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

data class ElapsedBreakdown(
    val format: DisplayFormat,
    val years: Long = 0,
    val months: Long = 0,
    val weeks: Long = 0,
    val days: Long = 0,
    val hours: Long = 0,
    val minutes: Long = 0,
    val seconds: Long = 0,
)

sealed interface ElapsedResult {
    data class Value(val breakdown: ElapsedBreakdown) : ElapsedResult
    data object ClockInconsistency : ElapsedResult
}

class TimeEngine(
    private val clock: Clock,
) {
    fun elapsedSince(
        startEpochMs: Long,
        zoneId: String,
        format: DisplayFormat,
    ): ElapsedResult = elapsedBetween(
        start = Instant.ofEpochMilli(startEpochMs),
        end = clock.instant(),
        zone = ZoneId.of(zoneId),
        format = format,
    )

    fun elapsedBetween(
        start: Instant,
        end: Instant,
        zone: ZoneId,
        format: DisplayFormat,
    ): ElapsedResult {
        if (end.isBefore(start)) return ElapsedResult.ClockInconsistency

        val startZoned = start.atZone(zone)
        val endZoned = end.atZone(zone)
        var cursor = startZoned

        var years = 0L
        var months = 0L
        var weeks = 0L
        var days = 0L

        when (format) {
            DisplayFormat.YEARS -> {
                val countedYears = countCalendarUnits(cursor, endZoned, ChronoUnit.YEARS)
                years = countedYears.first
                cursor = countedYears.second

                val countedMonths = countCalendarUnits(cursor, endZoned, ChronoUnit.MONTHS)
                months = countedMonths.first
                cursor = countedMonths.second

                val countedDays = countCalendarUnits(cursor, endZoned, ChronoUnit.DAYS)
                days = countedDays.first
                cursor = countedDays.second
            }

            DisplayFormat.MONTHS -> {
                val countedMonths = countCalendarUnits(cursor, endZoned, ChronoUnit.MONTHS)
                months = countedMonths.first
                cursor = countedMonths.second

                val countedDays = countCalendarUnits(cursor, endZoned, ChronoUnit.DAYS)
                days = countedDays.first
                cursor = countedDays.second
            }

            DisplayFormat.WEEKS -> {
                val totalDays = countCalendarUnits(cursor, endZoned, ChronoUnit.DAYS).first
                weeks = totalDays / 7
                cursor = cursor.plusWeeks(weeks)

                val countedDays = countCalendarUnits(cursor, endZoned, ChronoUnit.DAYS)
                days = countedDays.first
                cursor = countedDays.second
            }

            DisplayFormat.DAYS -> {
                val countedDays = countCalendarUnits(cursor, endZoned, ChronoUnit.DAYS)
                days = countedDays.first
                cursor = countedDays.second
            }
        }

        val remainder = Duration.between(cursor.toInstant(), endZoned.toInstant())
        val hours = remainder.toHours()
        val minutes = remainder.minusHours(hours).toMinutes()
        val seconds = remainder.minusHours(hours).minusMinutes(minutes).seconds

        return ElapsedResult.Value(
            ElapsedBreakdown(
                format = format,
                years = years,
                months = months,
                weeks = weeks,
                days = days,
                hours = hours,
                minutes = minutes,
                seconds = seconds,
            )
        )
    }

    private fun countCalendarUnits(
        start: ZonedDateTime,
        end: ZonedDateTime,
        unit: ChronoUnit,
    ): Pair<Long, ZonedDateTime> {
        var count = unit.between(start.toLocalDate(), end.toLocalDate()).coerceAtLeast(0)
        var candidate = plus(start, unit, count)

        while (count > 0 && candidate.toInstant().isAfter(end.toInstant())) {
            count -= 1
            candidate = plus(start, unit, count)
        }

        while (!plus(start, unit, count + 1).toInstant().isAfter(end.toInstant())) {
            count += 1
            candidate = plus(start, unit, count)
        }

        return count to candidate
    }

    private fun plus(
        value: ZonedDateTime,
        unit: ChronoUnit,
        amount: Long,
    ): ZonedDateTime = when (unit) {
        ChronoUnit.YEARS -> value.plusYears(amount)
        ChronoUnit.MONTHS -> value.plusMonths(amount)
        ChronoUnit.DAYS -> value.plusDays(amount)
        else -> error("Unsupported calendar unit: $unit")
    }
}
