package com.goreecloud.since.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import com.goreecloud.since.domain.model.TrackerKind
import kotlinx.coroutines.flow.Flow

data class PersistedTrackerAggregate(
    val tracker: TrackedEventEntity,
    val periods: List<EventPeriodEntity>,
    val goal: EventGoalEntity?,
)

@Dao
abstract class TrackerDao {
    @Query(
        "SELECT * FROM tracked_events " +
            "WHERE is_archived = 0 " +
            "ORDER BY sort_order, created_at_epoch_ms, id"
    )
    abstract fun observeActiveTrackedEvents(): Flow<List<TrackedEventEntity>>

    @Query(
        "SELECT * FROM tracked_events " +
            "WHERE is_archived = 1 " +
            "ORDER BY updated_at_epoch_ms DESC, title COLLATE NOCASE, id"
    )
    abstract fun observeArchivedTrackedEvents(): Flow<List<TrackedEventEntity>>

    @Query("SELECT * FROM event_periods ORDER BY event_id, sequence")
    abstract fun observeAllPeriods(): Flow<List<EventPeriodEntity>>

    @Query("SELECT * FROM event_goals ORDER BY event_id")
    abstract fun observeAllGoals(): Flow<List<EventGoalEntity>>

    @Query("SELECT * FROM tracked_events WHERE id = :eventId")
    protected abstract suspend fun readTrackedEvent(eventId: String): TrackedEventEntity?

    @Query(
        "SELECT * FROM tracked_events WHERE is_archived = 0 " +
            "ORDER BY sort_order, created_at_epoch_ms, id"
    )
    protected abstract suspend fun readActiveTrackedEvents(): List<TrackedEventEntity>

    @Query(
        "UPDATE tracked_events SET sort_order = :sortOrder, updated_at_epoch_ms = :updatedAtEpochMs " +
            "WHERE id = :eventId AND is_archived = 0"
    )
    protected abstract suspend fun updateSortOrder(
        eventId: String,
        sortOrder: Int,
        updatedAtEpochMs: Long,
    ): Int

    @Query("SELECT * FROM event_periods WHERE event_id = :eventId ORDER BY sequence")
    protected abstract suspend fun readPeriods(eventId: String): List<EventPeriodEntity>

    @Query("SELECT * FROM event_goals WHERE event_id = :eventId")
    protected abstract suspend fun readGoal(eventId: String): EventGoalEntity?

    @Query("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM tracked_events")
    protected abstract suspend fun nextSortOrder(): Int

    @Query(
        "SELECT COUNT(*) FROM event_periods " +
            "WHERE event_id = :eventId AND end_epoch_ms IS NULL"
    )
    abstract suspend fun openPeriodCount(eventId: String): Int

    @Query(
        "SELECT MAX(end_epoch_ms) FROM event_periods " +
            "WHERE event_id = :eventId AND end_epoch_ms IS NOT NULL"
    )
    protected abstract suspend fun latestClosedPeriodEnd(eventId: String): Long?

    @Query(
        "UPDATE tracked_events SET " +
            "title = :title, note = :note, default_display_format = :displayFormat, " +
            "updated_at_epoch_ms = :updatedAtEpochMs " +
            "WHERE id = :eventId AND is_archived = 0"
    )
    protected abstract suspend fun updateTrackerMetadata(
        eventId: String,
        title: String,
        note: String?,
        displayFormat: String,
        updatedAtEpochMs: Long,
    ): Int

    @Query(
        "UPDATE tracked_events SET " +
            "is_archived = :isArchived, updated_at_epoch_ms = :updatedAtEpochMs " +
            "WHERE id = :eventId AND is_archived != :isArchived"
    )
    protected abstract suspend fun updateArchiveState(
        eventId: String,
        isArchived: Boolean,
        updatedAtEpochMs: Long,
    ): Int

    @Query(
        "UPDATE event_periods SET " +
            "start_epoch_ms = :startEpochMs, start_zone_id = :startZoneId, " +
            "updated_at_epoch_ms = :updatedAtEpochMs " +
            "WHERE event_id = :eventId AND end_epoch_ms IS NULL"
    )
    protected abstract suspend fun updateCurrentPeriodStart(
        eventId: String,
        startEpochMs: Long,
        startZoneId: String,
        updatedAtEpochMs: Long,
    ): Int

    @Query(
        "UPDATE tracked_events " +
            "SET default_display_format = :displayFormat, updated_at_epoch_ms = :updatedAtEpochMs " +
            "WHERE id = :eventId AND is_archived = 0"
    )
    abstract suspend fun updateDisplayFormat(
        eventId: String,
        displayFormat: String,
        updatedAtEpochMs: Long,
    ): Int

    @Query(
        "UPDATE event_periods SET " +
            "end_epoch_ms = :resetEpochMs, end_zone_id = :resetZoneId, " +
            "reset_reason = :reason, reset_note = :note, " +
            "updated_at_epoch_ms = :updatedAtEpochMs " +
            "WHERE id = :periodId AND event_id = :eventId AND end_epoch_ms IS NULL"
    )
    protected abstract suspend fun closeCurrentPeriod(
        eventId: String,
        periodId: String,
        resetEpochMs: Long,
        resetZoneId: String,
        reason: String?,
        note: String?,
        updatedAtEpochMs: Long,
    ): Int

    @Query(
        "UPDATE tracked_events SET updated_at_epoch_ms = :updatedAtEpochMs " +
            "WHERE id = :eventId AND is_archived = 0"
    )
    protected abstract suspend fun touchTracker(
        eventId: String,
        updatedAtEpochMs: Long,
    ): Int

    @Query(
        "UPDATE event_goals SET target_amount = :targetAmount, target_unit = :targetUnit, " +
            "updated_at_epoch_ms = :updatedAtEpochMs WHERE event_id = :eventId"
    )
    protected abstract suspend fun updateGoalDefinition(
        eventId: String,
        targetAmount: Int,
        targetUnit: String,
        updatedAtEpochMs: Long,
    ): Int

    @Query("DELETE FROM event_goals WHERE event_id = :eventId")
    protected abstract suspend fun deleteGoal(eventId: String): Int

    @Query("DELETE FROM tracked_events WHERE id = :eventId AND is_archived = 1")
    protected abstract suspend fun deleteArchivedTrackerRow(eventId: String): Int

    @Insert
    abstract suspend fun insertTrackedEvent(entity: TrackedEventEntity)

    @Insert
    abstract suspend fun insertPeriod(entity: EventPeriodEntity)

    @Insert
    abstract suspend fun insertGoal(entity: EventGoalEntity)

    @Transaction
    open suspend fun moveActiveTracker(
        eventId: String,
        delta: Int,
        updatedAtEpochMs: Long,
    ): Boolean {
        if (eventId.isBlank() || delta == 0) return false
        val rows = readActiveTrackedEvents().toMutableList()
        val from = rows.indexOfFirst { it.id == eventId }
        if (from < 0) return false
        val to = (from + delta).coerceIn(0, rows.lastIndex)
        if (from == to) return false
        val moved = rows.removeAt(from)
        rows.add(to, moved)
        rows.forEachIndexed { index, row ->
            if (row.sortOrder != index) {
                check(updateSortOrder(row.id, index, updatedAtEpochMs) == 1) {
                    "tracker reorder did not update exactly one active row"
                }
            }
        }
        return true
    }

    @Transaction
    open suspend fun createTrackerAggregate(
        tracker: TrackedEventEntity,
        initialPeriod: EventPeriodEntity,
        goal: EventGoalEntity?,
    ): PersistedTrackerAggregate {
        require(tracker.id.isNotBlank())
        require(initialPeriod.id.isNotBlank())
        require(initialPeriod.eventId == tracker.id)
        require(initialPeriod.sequence == 0)
        require(initialPeriod.endEpochMs == null)
        require(initialPeriod.endZoneId == null)
        require(initialPeriod.resetReason == null)
        require(initialPeriod.resetNote == null)
        require(goal == null || goal.eventId == tracker.id)
        require(goal == null || tracker.kind == TrackerKind.STREAK.name)

        val orderedTracker = tracker.copy(sortOrder = nextSortOrder())

        insertTrackedEvent(orderedTracker)
        insertPeriod(initialPeriod)
        if (goal != null) insertGoal(goal)

        check(openPeriodCount(tracker.id) == 1) {
            "tracker creation did not persist exactly one open period"
        }

        return PersistedTrackerAggregate(
            tracker = orderedTracker,
            periods = listOf(initialPeriod),
            goal = goal,
        )
    }

    @Transaction
    open suspend fun updateTrackerAggregate(
        eventId: String,
        title: String,
        note: String?,
        displayFormat: String,
        startEpochMs: Long,
        startZoneId: String,
        updatedAtEpochMs: Long,
    ): PersistedTrackerAggregate? {
        val tracker = readTrackedEvent(eventId) ?: return null
        if (tracker.isArchived) return null
        if (openPeriodCount(eventId) != 1) return null

        val latestClosedEnd = latestClosedPeriodEnd(eventId)
        if (latestClosedEnd != null && startEpochMs < latestClosedEnd) {
            return null
        }

        check(
            updateTrackerMetadata(
                eventId = eventId,
                title = title,
                note = note,
                displayFormat = displayFormat,
                updatedAtEpochMs = updatedAtEpochMs,
            ) == 1
        ) { "tracker edit did not update exactly one tracker row" }

        check(
            updateCurrentPeriodStart(
                eventId = eventId,
                startEpochMs = startEpochMs,
                startZoneId = startZoneId,
                updatedAtEpochMs = updatedAtEpochMs,
            ) == 1
        ) { "tracker edit did not update exactly one open current period" }

        return readAggregate(eventId)
    }

    @Transaction
    open suspend fun resetStreak(
        eventId: String,
        nextPeriodId: String,
        resetEpochMs: Long,
        resetZoneId: String,
        reason: String?,
        note: String?,
        nowEpochMs: Long,
    ): PersistedTrackerAggregate? {
        val tracker = readTrackedEvent(eventId) ?: return null
        if (tracker.isArchived || tracker.kind != TrackerKind.STREAK.name) return null
        if (resetZoneId.isBlank() || nextPeriodId.isBlank()) return null
        if (resetEpochMs > nowEpochMs) return null

        val periods = readPeriods(eventId)
        val current = periods.singleOrNull { it.endEpochMs == null } ?: return null
        if (resetEpochMs < current.startEpochMs) return null
        val nextSequence = (periods.maxOfOrNull { it.sequence } ?: current.sequence) + 1

        check(
            closeCurrentPeriod(
                eventId = eventId,
                periodId = current.id,
                resetEpochMs = resetEpochMs,
                resetZoneId = resetZoneId,
                reason = reason,
                note = note,
                updatedAtEpochMs = nowEpochMs,
            ) == 1
        ) { "streak reset did not close exactly one current period" }

        insertPeriod(
            EventPeriodEntity(
                id = nextPeriodId,
                eventId = eventId,
                sequence = nextSequence,
                startEpochMs = resetEpochMs,
                startZoneId = resetZoneId,
                endEpochMs = null,
                endZoneId = null,
                resetReason = null,
                resetNote = null,
                createdAtEpochMs = nowEpochMs,
                updatedAtEpochMs = nowEpochMs,
            )
        )

        check(openPeriodCount(eventId) == 1) {
            "streak reset did not leave exactly one open current period"
        }
        check(
            touchTracker(
                eventId = eventId,
                updatedAtEpochMs = nowEpochMs,
            ) == 1
        ) { "streak reset did not update exactly one tracker mutation timestamp" }

        return readAggregate(eventId)
    }

    @Transaction
    open suspend fun upsertGoal(
        eventId: String,
        targetAmount: Int,
        targetUnit: String,
        updatedAtEpochMs: Long,
    ): EventGoalEntity? {
        val tracker = readTrackedEvent(eventId) ?: return null
        if (tracker.isArchived || tracker.kind != TrackerKind.STREAK.name) return null
        if (targetAmount !in 1..100_000) return null

        val existing = readGoal(eventId)
        if (existing == null) {
            insertGoal(
                EventGoalEntity(
                    eventId = eventId,
                    targetAmount = targetAmount,
                    targetUnit = targetUnit,
                    createdAtEpochMs = updatedAtEpochMs,
                    updatedAtEpochMs = updatedAtEpochMs,
                )
            )
        } else {
            check(
                updateGoalDefinition(
                    eventId = eventId,
                    targetAmount = targetAmount,
                    targetUnit = targetUnit,
                    updatedAtEpochMs = updatedAtEpochMs,
                ) == 1
            ) { "goal edit did not update exactly one goal row" }
        }

        return readGoal(eventId)
    }

    @Transaction
    open suspend fun removeGoal(eventId: String): Boolean {
        val tracker = readTrackedEvent(eventId) ?: return false
        if (tracker.isArchived || tracker.kind != TrackerKind.STREAK.name) return false
        val existing = readGoal(eventId) ?: return true
        return deleteGoal(existing.eventId) == 1
    }

    @Transaction
    open suspend fun setTrackerArchived(
        eventId: String,
        isArchived: Boolean,
        updatedAtEpochMs: Long,
    ): PersistedTrackerAggregate? {
        val tracker = readTrackedEvent(eventId) ?: return null
        if (tracker.isArchived == isArchived) return readAggregate(eventId)
        check(
            updateArchiveState(
                eventId = eventId,
                isArchived = isArchived,
                updatedAtEpochMs = updatedAtEpochMs,
            ) == 1
        ) { "archive state did not update exactly one tracker row" }
        return readAggregate(eventId)
    }

    @Transaction
    open suspend fun deleteArchivedTracker(eventId: String): Boolean {
        val tracker = readTrackedEvent(eventId) ?: return false
        if (!tracker.isArchived) return false
        return deleteArchivedTrackerRow(eventId) == 1
    }

    @Transaction
    open suspend fun readAggregate(eventId: String): PersistedTrackerAggregate? {
        val tracker = readTrackedEvent(eventId) ?: return null
        return PersistedTrackerAggregate(
            tracker = tracker,
            periods = readPeriods(eventId),
            goal = readGoal(eventId),
        )
    }
}
