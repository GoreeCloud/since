package com.goreecloud.since.testutil

import com.goreecloud.since.domain.model.DisplayFormat
import com.goreecloud.since.domain.model.Goal
import com.goreecloud.since.domain.model.Tracker
import com.goreecloud.since.domain.model.TrackerAggregate
import com.goreecloud.since.domain.model.TrackerKind
import com.goreecloud.since.domain.model.TrackerPeriod
import com.goreecloud.since.domain.repository.TrackerRepository
import com.goreecloud.since.domain.validation.ValidatedTrackerDraft
import java.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class FakeTrackerRepository(
    initial: List<TrackerAggregate>,
    private val clock: Clock = Clock.systemUTC(),
) : TrackerRepository {
    private val aggregates = MutableStateFlow(initial)
    private var nextId = initial.size + 1

    val current: List<TrackerAggregate>
        get() = aggregates.value

    override fun observeActiveTrackers(): Flow<List<Tracker>> =
        aggregates.map { values -> values.map { it.tracker } }

    override fun observeActiveTrackerAggregates(): Flow<List<TrackerAggregate>> = aggregates

    override suspend fun createTracker(draft: ValidatedTrackerDraft): TrackerAggregate {
        val id = "created-" + nextId++
        val timestamp = clock.millis()
        val created = TrackerAggregate(
            tracker = Tracker(
                id = id,
                title = draft.title,
                note = draft.note,
                kind = draft.kind,
                iconKey = null,
                accentKey = null,
                defaultDisplayFormat = draft.displayFormat,
                sortOrder = aggregates.value.size,
                isArchived = false,
                createdAtEpochMs = timestamp,
                updatedAtEpochMs = timestamp,
            ),
            periods = listOf(
                TrackerPeriod(
                    id = id + "-period",
                    trackerId = id,
                    sequence = 0,
                    startEpochMs = draft.startEpochMs,
                    startZoneId = draft.startZoneId,
                    endEpochMs = null,
                    endZoneId = null,
                    resetReason = null,
                    resetNote = null,
                    createdAtEpochMs = timestamp,
                    updatedAtEpochMs = timestamp,
                )
            ),
            goal = draft.goalAmount?.let { amount ->
                Goal(
                    trackerId = id,
                    targetAmount = amount,
                    targetUnit = checkNotNull(draft.goalUnit),
                    createdAtEpochMs = timestamp,
                    updatedAtEpochMs = timestamp,
                )
            },
        )
        aggregates.value = aggregates.value + created
        return created
    }

    override suspend fun loadTracker(trackerId: String): TrackerAggregate? =
        aggregates.value.firstOrNull { it.tracker.id == trackerId }

    override suspend fun updateTracker(
        trackerId: String,
        draft: ValidatedTrackerDraft,
    ): TrackerAggregate? {
        val existing = loadTracker(trackerId) ?: return null
        if (existing.tracker.kind != draft.kind) return null
        val timestamp = clock.millis()
        val currentPeriod = existing.periods.single { it.endEpochMs == null }
        val updated = existing.copy(
            tracker = existing.tracker.copy(
                title = draft.title,
                note = draft.note,
                defaultDisplayFormat = draft.displayFormat,
                updatedAtEpochMs = timestamp,
            ),
            periods = existing.periods.map { period ->
                if (period.id == currentPeriod.id) {
                    period.copy(
                        startEpochMs = draft.startEpochMs,
                        startZoneId = draft.startZoneId,
                        updatedAtEpochMs = timestamp,
                    )
                } else {
                    period
                }
            },
        )
        aggregates.value = aggregates.value.map { row ->
            if (row.tracker.id == trackerId) updated else row
        }
        return updated
    }

    override suspend fun updateGoal(
        trackerId: String,
        targetAmount: Int,
        targetUnit: DisplayFormat,
    ): Goal? {
        val existing = loadTracker(trackerId) ?: return null
        if (existing.tracker.kind != TrackerKind.STREAK || targetAmount !in 1..100_000) return null
        val timestamp = clock.millis()
        val currentGoal = existing.goal
        val updatedGoal = Goal(
            trackerId = trackerId,
            targetAmount = targetAmount,
            targetUnit = targetUnit,
            createdAtEpochMs = currentGoal?.createdAtEpochMs ?: timestamp,
            updatedAtEpochMs = timestamp,
        )
        val updated = existing.copy(goal = updatedGoal)
        aggregates.value = aggregates.value.map { row ->
            if (row.tracker.id == trackerId) updated else row
        }
        return updatedGoal
    }

    override suspend fun removeGoal(trackerId: String): Boolean {
        val existing = loadTracker(trackerId) ?: return false
        if (existing.tracker.kind != TrackerKind.STREAK) return false
        val updated = existing.copy(goal = null)
        aggregates.value = aggregates.value.map { row ->
            if (row.tracker.id == trackerId) updated else row
        }
        return true
    }

    override suspend fun updateDisplayFormat(
        trackerId: String,
        displayFormat: DisplayFormat,
    ): Boolean {
        val existing = loadTracker(trackerId) ?: return false
        val updated = existing.copy(
            tracker = existing.tracker.copy(
                defaultDisplayFormat = displayFormat,
                updatedAtEpochMs = clock.millis(),
            )
        )
        aggregates.value = aggregates.value.map { row ->
            if (row.tracker.id == trackerId) updated else row
        }
        return true
    }
}
