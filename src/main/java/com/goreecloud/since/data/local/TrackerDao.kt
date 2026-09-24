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

    @Query("SELECT * FROM event_periods ORDER BY event_id, sequence")
    abstract fun observeAllPeriods(): Flow<List<EventPeriodEntity>>

    @Query("SELECT * FROM event_goals ORDER BY event_id")
    abstract fun observeAllGoals(): Flow<List<EventGoalEntity>>

    @Query("SELECT * FROM tracked_events WHERE id = :eventId")
    protected abstract suspend fun readTrackedEvent(eventId: String): TrackedEventEntity?

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

    @Insert
    abstract suspend fun insertTrackedEvent(entity: TrackedEventEntity)

    @Insert
    abstract suspend fun insertPeriod(entity: EventPeriodEntity)

    @Insert
    abstract suspend fun insertGoal(entity: EventGoalEntity)

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
    open suspend fun readAggregate(eventId: String): PersistedTrackerAggregate? {
        val tracker = readTrackedEvent(eventId) ?: return null
        return PersistedTrackerAggregate(
            tracker = tracker,
            periods = readPeriods(eventId),
            goal = readGoal(eventId),
        )
    }
}
