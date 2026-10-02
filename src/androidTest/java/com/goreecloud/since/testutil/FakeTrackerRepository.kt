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
        aggregates.map { values ->
            values.filterNot { it.tracker.isArchived }.map { it.tracker }
        }

    override fun observeActiveTrackerAggregates(): Flow<List<TrackerAggregate>> =
        aggregates.map { values -> values.filterNot { it.tracker.isArchived } }

    override fun observeArchivedTrackerAggregates(): Flow<List<TrackerAggregate>> =
        aggregates.map { values ->
            values
                .filter { it.tracker.isArchived }
                .sortedByDescending { it.tracker.updatedAtEpochMs }
        }

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

    override suspend fun archiveTracker(trackerId: String): TrackerAggregate? =
        setArchiveState(trackerId = trackerId, isArchived = true)

    override suspend fun restoreTracker(trackerId: String): TrackerAggregate? =
        setArchiveState(trackerId = trackerId, isArchived = false)

    override suspend fun deleteArchivedTracker(trackerId: String): Boolean {
        val existing = aggregates.value.firstOrNull { it.tracker.id == trackerId } ?: return false
        if (!existing.tracker.isArchived) return false
        aggregates.value = aggregates.value.filterNot { it.tracker.id == trackerId }
        return true
    }

    private fun setArchiveState(
        trackerId: String,
        isArchived: Boolean,
    ): TrackerAggregate? {
        val existing = aggregates.value.firstOrNull { it.tracker.id == trackerId } ?: return null
        if (existing.tracker.isArchived == isArchived) return existing
        val updated = existing.copy(
            tracker = existing.tracker.copy(
                isArchived = isArchived,
                updatedAtEpochMs = clock.millis(),
            )
        )
        aggregates.value = aggregates.value.map { row ->
            if (row.tracker.id == trackerId) updated else row
        }
        return updated
    }

    override suspend fun updateTracker(
        trackerId: String,
        draft: ValidatedTrackerDraft,
    ): TrackerAggregate? {
        val existing = loadTracker(trackerId) ?: return null
        if (existing.tracker.isArchived || existing.tracker.kind != draft.kind) return null
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
        if (
            existing.tracker.isArchived ||
            existing.tracker.kind != TrackerKind.STREAK ||
            targetAmount !in 1..100_000
        ) return null
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
        if (existing.tracker.isArchived || existing.tracker.kind != TrackerKind.STREAK) return false
        val updated = existing.copy(goal = null)
        aggregates.value = aggregates.value.map { row ->
            if (row.tracker.id == trackerId) updated else row
        }
        return true
    }

    override suspend fun resetStreak(
        trackerId: String,
        resetEpochMs: Long,
        resetZoneId: String,
        reason: String?,
        note: String?,
    ): TrackerAggregate? {
        val existing = loadTracker(trackerId) ?: return null
        if (existing.tracker.isArchived || existing.tracker.kind != TrackerKind.STREAK) return null
        if (runCatching { java.time.ZoneId.of(resetZoneId) }.isFailure) return null
        val current = existing.periods.singleOrNull { it.endEpochMs == null } ?: return null
        val now = clock.millis()
        if (resetEpochMs < current.startEpochMs || resetEpochMs > now) return null

        val normalizedReason = reason?.trim()?.takeIf { it.isNotEmpty() }
        val normalizedNote = note?.trim()?.takeIf { it.isNotEmpty() }
        if (normalizedReason != null && normalizedReason.length > 120) return null
        if (normalizedNote != null && normalizedNote.length > 2_000) return null

        val nextSequence = (existing.periods.maxOfOrNull { it.sequence } ?: current.sequence) + 1
        val closed = current.copy(
            endEpochMs = resetEpochMs,
            endZoneId = resetZoneId,
            resetReason = normalizedReason,
            resetNote = normalizedNote,
            updatedAtEpochMs = now,
        )
        val next = TrackerPeriod(
            id = trackerId + "-period-" + nextSequence,
            trackerId = trackerId,
            sequence = nextSequence,
            startEpochMs = resetEpochMs,
            startZoneId = resetZoneId,
            endEpochMs = null,
            endZoneId = null,
            resetReason = null,
            resetNote = null,
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
        )
        val updated = existing.copy(
            tracker = existing.tracker.copy(updatedAtEpochMs = now),
            periods = existing.periods
                .map { period -> if (period.id == current.id) closed else period } + next,
        )
        aggregates.value = aggregates.value.map { row ->
            if (row.tracker.id == trackerId) updated else row
        }
        return updated
    }

    override suspend fun updateDisplayFormat(
        trackerId: String,
        displayFormat: DisplayFormat,
    ): Boolean {
        val existing = loadTracker(trackerId) ?: return false
        if (existing.tracker.isArchived) return false
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
