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
    open suspend fun readAggregate(eventId: String): PersistedTrackerAggregate? {
        val tracker = readTrackedEvent(eventId) ?: return null
        return PersistedTrackerAggregate(
            tracker = tracker,
            periods = readPeriods(eventId),
            goal = readGoal(eventId),
        )
    }
}
