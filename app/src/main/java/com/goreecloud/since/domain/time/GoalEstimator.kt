package com.goreecloud.since.domain.time

import com.goreecloud.since.domain.model.DisplayFormat
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.math.floor

data class GoalEstimate(
    val targetEpochMs: Long,
    val progressFraction: Double,
    val percent: Long,
    val isComplete: Boolean,
)

sealed interface GoalEstimateResult {
    data class Value(val estimate: GoalEstimate) : GoalEstimateResult
    data object ClockInconsistency : GoalEstimateResult
}

class GoalEstimator(
    private val clock: Clock,
) {
    fun estimate(
        startEpochMs: Long,
        zoneId: String,
        targetAmount: Int,
        targetUnit: DisplayFormat,
    ): GoalEstimateResult {
        require(targetAmount in 1..100_000) {
            "Goal target amount must be between 1 and 100,000."
        }

        val start = Instant.ofEpochMilli(startEpochMs)
        val zone = ZoneId.of(zoneId)
        val startZoned = start.atZone(zone)
        val targetZoned = when (targetUnit) {
            DisplayFormat.DAYS -> startZoned.plusDays(targetAmount.toLong())
            DisplayFormat.WEEKS -> startZoned.plusWeeks(targetAmount.toLong())
            DisplayFormat.MONTHS -> startZoned.plusMonths(targetAmount.toLong())
            DisplayFormat.YEARS -> startZoned.plusYears(targetAmount.toLong())
        }
        val target = targetZoned.toInstant()
        val now = clock.instant()

        if (now.isBefore(start)) return GoalEstimateResult.ClockInconsistency

        val targetDurationMillis = Duration.between(start, target).toMillis()
        check(targetDurationMillis > 0) { "Goal target must resolve after the period start." }

        val elapsedMillis = Duration.between(start, now).toMillis()
        val progress = elapsedMillis.toDouble() / targetDurationMillis.toDouble()
        val percent = floor(progress * 100.0).toLong().coerceAtLeast(0L)

        return GoalEstimateResult.Value(
            GoalEstimate(
                targetEpochMs = target.toEpochMilli(),
                progressFraction = progress,
                percent = percent,
                isComplete = now >= target,
            )
        )
    }
}
