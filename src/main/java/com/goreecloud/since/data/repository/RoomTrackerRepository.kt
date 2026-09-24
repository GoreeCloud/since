package com.goreecloud.since.data.repository

import com.goreecloud.since.data.local.EventGoalEntity
import com.goreecloud.since.data.local.EventPeriodEntity
import com.goreecloud.since.data.local.PersistedTrackerAggregate
import com.goreecloud.since.data.local.TrackedEventEntity
import com.goreecloud.since.data.local.TrackerDao
import com.goreecloud.since.domain.model.DisplayFormat
import com.goreecloud.since.domain.model.Goal
import com.goreecloud.since.domain.model.Tracker
import com.goreecloud.since.domain.model.TrackerAggregate
import com.goreecloud.since.domain.model.TrackerKind
import com.goreecloud.since.domain.model.TrackerPeriod
import com.goreecloud.since.domain.repository.TrackerRepository
import com.goreecloud.since.domain.validation.ValidatedTrackerDraft
import java.time.Clock
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class RoomTrackerRepository(
    private val dao: TrackerDao,
    private val clock: Clock,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
) : TrackerRepository {
    override fun observeActiveTrackers(): Flow<List<Tracker>> =
        dao.observeActiveTrackedEvents().map { rows ->
            rows.map { it.toDomain() }
        }

    override fun observeActiveTrackerAggregates(): Flow<List<TrackerAggregate>> =
        combine(
            dao.observeActiveTrackedEvents(),
            dao.observeAllPeriods(),
            dao.observeAllGoals(),
        ) { rows, _, _ ->
            rows.mapNotNull { row ->
                dao.readAggregate(row.id)?.toDomain()
            }
        }

    override suspend fun createTracker(
        draft: ValidatedTrackerDraft,
    ): TrackerAggregate {
        val eventId = idFactory().also { require(it.isNotBlank()) }
        val periodId = idFactory().also {
            require(it.isNotBlank())
            require(it != eventId) { "Generated tracker and period IDs must be distinct." }
        }
        val now = clock.millis()

        val tracker = TrackedEventEntity(
            id = eventId,
            title = draft.title,
            note = draft.note,
            kind = draft.kind.name,
            iconKey = null,
            accentKey = null,
            defaultDisplayFormat = draft.displayFormat.name,
            sortOrder = 0,
            isArchived = false,
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
        )
        val period = EventPeriodEntity(
            id = periodId,
            eventId = eventId,
            sequence = 0,
            startEpochMs = draft.startEpochMs,
            startZoneId = draft.startZoneId,
            endEpochMs = null,
            endZoneId = null,
            resetReason = null,
            resetNote = null,
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
        )
        val goal = draft.goalAmount?.let { amount ->
            EventGoalEntity(
                eventId = eventId,
                targetAmount = amount,
                targetUnit = checkNotNull(draft.goalUnit).name,
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
            )
        }

        return dao.createTrackerAggregate(tracker, period, goal).toDomain()
    }

    override suspend fun loadTracker(
        trackerId: String,
    ): TrackerAggregate? = dao.readAggregate(trackerId)?.toDomain()

    override suspend fun updateTracker(
        trackerId: String,
        draft: ValidatedTrackerDraft,
    ): TrackerAggregate? {
        val existing = dao.readAggregate(trackerId)?.toDomain() ?: return null
        if (existing.tracker.kind != draft.kind) return null

        return dao.updateTrackerAggregate(
            eventId = trackerId,
            title = draft.title,
            note = draft.note,
            displayFormat = draft.displayFormat.name,
            startEpochMs = draft.startEpochMs,
            startZoneId = draft.startZoneId,
            updatedAtEpochMs = clock.millis(),
        )?.toDomain()
    }

    override suspend fun updateDisplayFormat(
        trackerId: String,
        displayFormat: DisplayFormat,
    ): Boolean = dao.updateDisplayFormat(
        eventId = trackerId,
        displayFormat = displayFormat.name,
        updatedAtEpochMs = clock.millis(),
    ) == 1

    override suspend fun updateGoal(
        trackerId: String,
        targetAmount: Int,
        targetUnit: DisplayFormat,
    ): Goal? = dao.upsertGoal(
        eventId = trackerId,
        targetAmount = targetAmount,
        targetUnit = targetUnit.name,
        updatedAtEpochMs = clock.millis(),
    )?.toDomain()

    override suspend fun removeGoal(trackerId: String): Boolean =
        dao.removeGoal(trackerId)

    private fun PersistedTrackerAggregate.toDomain(): TrackerAggregate =
        TrackerAggregate(
            tracker = tracker.toDomain(),
            periods = periods.map { it.toDomain() },
            goal = goal?.toDomain(),
        )

    private fun TrackedEventEntity.toDomain(): Tracker =
        Tracker(
            id = id,
            title = title,
            note = note,
            kind = enumValue<TrackerKind>(kind, "tracker kind"),
            iconKey = iconKey,
            accentKey = accentKey,
            defaultDisplayFormat = enumValue<DisplayFormat>(
                defaultDisplayFormat,
                "display format",
            ),
            sortOrder = sortOrder,
            isArchived = isArchived,
            createdAtEpochMs = createdAtEpochMs,
            updatedAtEpochMs = updatedAtEpochMs,
        )

    private fun EventPeriodEntity.toDomain(): TrackerPeriod =
        TrackerPeriod(
            id = id,
            trackerId = eventId,
            sequence = sequence,
            startEpochMs = startEpochMs,
            startZoneId = startZoneId,
            endEpochMs = endEpochMs,
            endZoneId = endZoneId,
            resetReason = resetReason,
            resetNote = resetNote,
            createdAtEpochMs = createdAtEpochMs,
            updatedAtEpochMs = updatedAtEpochMs,
        )

    private fun EventGoalEntity.toDomain(): Goal =
        Goal(
            trackerId = eventId,
            targetAmount = targetAmount,
            targetUnit = enumValue<DisplayFormat>(targetUnit, "goal unit"),
            createdAtEpochMs = createdAtEpochMs,
            updatedAtEpochMs = updatedAtEpochMs,
        )

    private inline fun <reified T : Enum<T>> enumValue(
        raw: String,
        label: String,
    ): T = runCatching { enumValueOf<T>(raw) }
        .getOrElse { error("Persisted $label is unsupported: $raw") }
}
