package com.goreecloud.since.data.local

import android.content.Context
import androidx.sqlite.SQLiteException
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.goreecloud.since.data.repository.RoomTrackerRepository
import com.goreecloud.since.domain.model.DisplayFormat
import com.goreecloud.since.domain.model.TrackerKind
import com.goreecloud.since.domain.validation.TrackerDraft
import com.goreecloud.since.domain.validation.TrackerDraftValidation
import com.goreecloud.since.domain.validation.TrackerDraftValidator
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SinceDatabaseRuntimeTest {
    private lateinit var database: SinceDatabase
    private lateinit var dao: TrackerDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = SinceDatabaseFactory.buildInMemoryForTest(context)
        dao = database.trackerDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun streakAllowsHistoryButRejectsSecondOpenPeriod() = runBlocking {
        val tracker = trackerEntity(id = "streak-1", kind = TrackerKind.STREAK)
        val current = periodEntity(
            id = "period-0",
            eventId = tracker.id,
            sequence = 0,
            start = 1_000L,
        )

        val created = dao.createTrackerAggregate(tracker, current, null)

        assertEquals(1, created.periods.size)
        assertEquals(1, dao.openPeriodCount(tracker.id))

        expectSQLiteFailure {
            dao.insertPeriod(
                periodEntity(
                    id = "period-open-duplicate",
                    eventId = tracker.id,
                    sequence = 1,
                    start = 2_000L,
                )
            )
        }

        dao.insertPeriod(
            periodEntity(
                id = "period-closed-history",
                eventId = tracker.id,
                sequence = 1,
                start = 2_000L,
                end = 3_000L,
            )
        )

        val aggregate = dao.readAggregate(tracker.id)
        assertNotNull(aggregate)
        assertEquals(2, aggregate!!.periods.size)
        assertEquals(1, dao.openPeriodCount(tracker.id))
    }

    @Test
    fun eventRejectsAdditionalPeriod() = runBlocking {
        val tracker = trackerEntity(id = "event-1", kind = TrackerKind.EVENT)
        dao.createTrackerAggregate(
            tracker = tracker,
            initialPeriod = periodEntity(
                id = "event-period-0",
                eventId = tracker.id,
                sequence = 0,
                start = 1_000L,
            ),
            goal = null,
        )

        expectSQLiteFailure {
            dao.insertPeriod(
                periodEntity(
                    id = "event-period-1",
                    eventId = tracker.id,
                    sequence = 1,
                    start = 2_000L,
                    end = 3_000L,
                )
            )
        }

        assertEquals(1, dao.readAggregate(tracker.id)!!.periods.size)
    }

    @Test
    fun repositoryCreatesValidatedStreakAndEmitsDashboardAggregate() = runBlocking {
        val now = Instant.parse("2026-09-23T18:00:00Z")
        val clock = Clock.fixed(now, ZoneId.of("UTC"))
        val generatedIds = mutableListOf("tracker-created", "period-created").iterator()
        val repository = RoomTrackerRepository(
            dao = dao,
            clock = clock,
            idFactory = { generatedIds.next() },
        )
        val validation = TrackerDraftValidator(clock).validate(
            TrackerDraft(
                title = "  Read daily  ",
                note = "  Keep going.  ",
                kind = TrackerKind.STREAK,
                startEpochMs = now.minusSeconds(60).toEpochMilli(),
                startZoneId = "America/Chicago",
                displayFormat = DisplayFormat.DAYS,
                goalAmount = 30,
                goalUnit = DisplayFormat.DAYS,
            )
        )
        val validated = (validation as TrackerDraftValidation.Valid).draft

        val created = repository.createTracker(validated)
        val observed = repository.observeActiveTrackerAggregates().first().single()

        assertEquals("tracker-created", created.tracker.id)
        assertEquals("Read daily", created.tracker.title)
        assertEquals("Keep going.", created.tracker.note)
        assertEquals(TrackerKind.STREAK, observed.tracker.kind)
        assertEquals("period-created", observed.periods.single().id)
        assertEquals(30, observed.goal!!.targetAmount)
        assertEquals(1, dao.openPeriodCount(observed.tracker.id))
    }

    @Test
    fun manualTrackerMovementPersistsContiguousActiveOrder() = runBlocking {
        val now = Instant.parse("2026-10-02T12:00:00Z")
        val repository = RoomTrackerRepository(
            dao = dao,
            clock = Clock.fixed(now, ZoneId.of("UTC")),
        )
        listOf("first", "second", "third").forEachIndexed { index, id ->
            dao.createTrackerAggregate(
                tracker = trackerEntity(id = id, kind = TrackerKind.EVENT),
                initialPeriod = periodEntity(
                    id = "$id-period",
                    eventId = id,
                    sequence = 0,
                    start = 1_000L + index,
                ),
                goal = null,
            )
        }

        assertTrue(repository.moveTracker("third", -2))
        val moved = repository.observeActiveTrackerAggregates().first()
        assertEquals(listOf("third", "first", "second"), moved.map { it.tracker.id })
        assertEquals(listOf(0, 1, 2), moved.map { it.tracker.sortOrder })

        assertTrue(repository.moveTracker("first", 1))
        val movedAgain = repository.observeActiveTrackerAggregates().first()
        assertEquals(listOf("third", "second", "first"), movedAgain.map { it.tracker.id })
        assertEquals(listOf(0, 1, 2), movedAgain.map { it.tracker.sortOrder })

        assertFalse(repository.moveTracker("third", -1))
        assertFalse(repository.moveTracker("missing", 1))
    }

    @Test
    fun persistedDatabaseReopensWithManualInvariantsIntact() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(SinceDatabase.NAME)

        var persistent: SinceDatabase? = null
        try {
            persistent = SinceDatabaseFactory.build(context)
            val firstDao = persistent.trackerDao()
            val tracker = trackerEntity(id = "reopen-streak", kind = TrackerKind.STREAK)
            firstDao.createTrackerAggregate(
                tracker = tracker,
                initialPeriod = periodEntity(
                    id = "reopen-period-0",
                    eventId = tracker.id,
                    sequence = 0,
                    start = 1_000L,
                ),
                goal = null,
            )
            persistent.close()
            persistent = null

            persistent = SinceDatabaseFactory.build(context)
            val reopenedDao = persistent.trackerDao()
            assertEquals(1, reopenedDao.openPeriodCount(tracker.id))

            expectSQLiteFailure {
                reopenedDao.insertPeriod(
                    periodEntity(
                        id = "reopen-period-duplicate",
                        eventId = tracker.id,
                        sequence = 1,
                        start = 2_000L,
                    )
                )
            }
        } finally {
            persistent?.close()
            context.deleteDatabase(SinceDatabase.NAME)
        }
    }

    @Test
    fun databaseRejectsInvalidPeriodChronologyAndEventGoal() = runBlocking {
        val streak = trackerEntity(id = "streak-2", kind = TrackerKind.STREAK)
        dao.createTrackerAggregate(
            tracker = streak,
            initialPeriod = periodEntity(
                id = "streak-2-period-0",
                eventId = streak.id,
                sequence = 0,
                start = 5_000L,
            ),
            goal = null,
        )

        expectSQLiteFailure {
            dao.insertPeriod(
                periodEntity(
                    id = "streak-2-invalid-period",
                    eventId = streak.id,
                    sequence = 1,
                    start = 8_000L,
                    end = 7_000L,
                )
            )
        }

        val event = trackerEntity(id = "event-2", kind = TrackerKind.EVENT)
        dao.createTrackerAggregate(
            tracker = event,
            initialPeriod = periodEntity(
                id = "event-2-period-0",
                eventId = event.id,
                sequence = 0,
                start = 1_000L,
            ),
            goal = null,
        )

        expectSQLiteFailure {
            dao.insertGoal(
                EventGoalEntity(
                    eventId = event.id,
                    targetAmount = 10,
                    targetUnit = DisplayFormat.DAYS.name,
                    createdAtEpochMs = 10_000L,
                    updatedAtEpochMs = 10_000L,
                )
            )
        }
    }


    @Test
    fun trackerEditUpdatesOnlyMetadataAndOpenCurrentPeriod() = runBlocking {
        val now = Instant.parse("2026-09-23T18:00:00Z")
        val clock = Clock.fixed(now, ZoneId.of("UTC"))
        val repository = RoomTrackerRepository(dao = dao, clock = clock)
        val tracker = trackerEntity(id = "edit-streak", kind = TrackerKind.STREAK)
        dao.createTrackerAggregate(
            tracker = tracker,
            initialPeriod = periodEntity(
                id = "edit-current",
                eventId = tracker.id,
                sequence = 0,
                start = 1_000L,
            ),
            goal = null,
        )
        dao.insertPeriod(
            periodEntity(
                id = "edit-history",
                eventId = tracker.id,
                sequence = 1,
                start = 2_000L,
                end = 3_000L,
            )
        )

        val validation = TrackerDraftValidator(clock).validate(
            TrackerDraft(
                title = "  Edited streak  ",
                note = "  Current period only.  ",
                kind = TrackerKind.STREAK,
                startEpochMs = 4_000L,
                startZoneId = "America/Chicago",
                displayFormat = DisplayFormat.WEEKS,
            )
        )
        val edited = repository.updateTracker(
            trackerId = tracker.id,
            draft = (validation as TrackerDraftValidation.Valid).draft,
        )

        assertNotNull(edited)
        assertEquals("Edited streak", edited!!.tracker.title)
        assertEquals("Current period only.", edited.tracker.note)
        assertEquals(DisplayFormat.WEEKS, edited.tracker.defaultDisplayFormat)
        assertEquals(4_000L, edited.periods.single { it.endEpochMs == null }.startEpochMs)
        assertEquals(
            "America/Chicago",
            edited.periods.single { it.endEpochMs == null }.startZoneId,
        )
        val history = edited.periods.single { it.endEpochMs != null }
        assertEquals(2_000L, history.startEpochMs)
        assertEquals(3_000L, history.endEpochMs)
    }

    @Test
    fun trackerEditRejectsCurrentStartBeforeLatestClosedHistory() = runBlocking {
        val now = Instant.parse("2026-09-23T18:00:00Z")
        val clock = Clock.fixed(now, ZoneId.of("UTC"))
        val repository = RoomTrackerRepository(dao = dao, clock = clock)
        val tracker = trackerEntity(id = "edit-conflict", kind = TrackerKind.STREAK)
        dao.createTrackerAggregate(
            tracker = tracker,
            initialPeriod = periodEntity(
                id = "edit-conflict-current",
                eventId = tracker.id,
                sequence = 0,
                start = 1_000L,
            ),
            goal = null,
        )
        dao.insertPeriod(
            periodEntity(
                id = "edit-conflict-history",
                eventId = tracker.id,
                sequence = 1,
                start = 2_000L,
                end = 3_000L,
            )
        )

        val validation = TrackerDraftValidator(clock).validate(
            TrackerDraft(
                title = "Rejected edit",
                note = null,
                kind = TrackerKind.STREAK,
                startEpochMs = 2_500L,
                startZoneId = "UTC",
                displayFormat = DisplayFormat.MONTHS,
            )
        )
        val edited = repository.updateTracker(
            trackerId = tracker.id,
            draft = (validation as TrackerDraftValidation.Valid).draft,
        )

        assertEquals(null, edited)
        val unchanged = repository.loadTracker(tracker.id)!!
        assertEquals(tracker.title, unchanged.tracker.title)
        assertEquals(DisplayFormat.DAYS, unchanged.tracker.defaultDisplayFormat)
        assertEquals(
            1_000L,
            unchanged.periods.single { it.endEpochMs == null }.startEpochMs,
        )
    }

    @Test
    fun displayFormatUpdatePersistsAndReemitsAggregate() = runBlocking {
        val now = Instant.parse("2026-09-23T18:00:00Z")
        val clock = Clock.fixed(now, ZoneId.of("UTC"))
        val repository = RoomTrackerRepository(dao = dao, clock = clock)
        val tracker = trackerEntity(id = "format-event", kind = TrackerKind.EVENT)
        dao.createTrackerAggregate(
            tracker = tracker,
            initialPeriod = periodEntity(
                id = "format-period",
                eventId = tracker.id,
                sequence = 0,
                start = 1_000L,
            ),
            goal = null,
        )

        val changed = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(5_000) {
                repository.observeActiveTrackerAggregates().first { aggregates ->
                    aggregates.singleOrNull()?.tracker?.defaultDisplayFormat == DisplayFormat.MONTHS
                }
            }
        }

        assertTrue(repository.updateDisplayFormat(tracker.id, DisplayFormat.MONTHS))
        assertEquals(DisplayFormat.MONTHS, changed.await().single().tracker.defaultDisplayFormat)
        assertEquals(
            DisplayFormat.MONTHS,
            repository.loadTracker(tracker.id)!!.tracker.defaultDisplayFormat,
        )
    }

    @Test
    fun goalUpdateAndRemovalArePersistedAndReactiveForStreaksOnly() = runBlocking {
        val now = Instant.parse("2026-09-24T12:00:00Z")
        val clock = Clock.fixed(now, ZoneId.of("UTC"))
        val repository = RoomTrackerRepository(dao = dao, clock = clock)

        val streak = trackerEntity(id = "goal-streak", kind = TrackerKind.STREAK)
        dao.createTrackerAggregate(
            tracker = streak,
            initialPeriod = periodEntity(
                id = "goal-streak-period",
                eventId = streak.id,
                sequence = 0,
                start = now.minusSeconds(3_600).toEpochMilli(),
            ),
            goal = null,
        )

        val emitted = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(5_000) {
                repository.observeActiveTrackerAggregates().first { aggregates ->
                    aggregates.singleOrNull()?.goal?.targetAmount == 14
                }
            }
        }

        val created = repository.updateGoal(
            trackerId = streak.id,
            targetAmount = 14,
            targetUnit = DisplayFormat.DAYS,
        )
        assertNotNull(created)
        assertEquals(14, emitted.await().single().goal!!.targetAmount)

        val createdAt = created!!.createdAtEpochMs
        val changed = repository.updateGoal(
            trackerId = streak.id,
            targetAmount = 2,
            targetUnit = DisplayFormat.WEEKS,
        )
        assertNotNull(changed)
        assertEquals(createdAt, changed!!.createdAtEpochMs)
        assertEquals(2, changed.targetAmount)
        assertEquals(DisplayFormat.WEEKS, changed.targetUnit)

        assertTrue(repository.removeGoal(streak.id))
        assertEquals(null, repository.loadTracker(streak.id)!!.goal)
        assertTrue(repository.removeGoal(streak.id))

        val event = trackerEntity(id = "goal-event", kind = TrackerKind.EVENT)
        dao.createTrackerAggregate(
            tracker = event,
            initialPeriod = periodEntity(
                id = "goal-event-period",
                eventId = event.id,
                sequence = 0,
                start = 1_000L,
            ),
            goal = null,
        )
        assertEquals(
            null,
            repository.updateGoal(event.id, 10, DisplayFormat.DAYS),
        )
        assertFalse(repository.removeGoal(event.id))
    }

    @Test
    fun repositoryResetStreakClosesHistoryAndStartsNextPeriodAtomically() = runBlocking {
        val now = Instant.parse("2026-09-24T18:00:00Z")
        val resetAt = now.minusSeconds(3_600)
        val generatedIds = mutableListOf("reset-next-period").iterator()
        val repository = RoomTrackerRepository(
            dao = dao,
            clock = Clock.fixed(now, ZoneId.of("UTC")),
            idFactory = { generatedIds.next() },
        )
        val tracker = trackerEntity(id = "reset-streak", kind = TrackerKind.STREAK)
        dao.createTrackerAggregate(
            tracker = tracker,
            initialPeriod = periodEntity(
                id = "reset-period-0",
                eventId = tracker.id,
                sequence = 0,
                start = now.minusSeconds(86_400).toEpochMilli(),
            ),
            goal = EventGoalEntity(
                eventId = tracker.id,
                targetAmount = 30,
                targetUnit = DisplayFormat.DAYS.name,
                createdAtEpochMs = 10_000L,
                updatedAtEpochMs = 10_000L,
            ),
        )

        val reset = repository.resetStreak(
            trackerId = tracker.id,
            resetEpochMs = resetAt.toEpochMilli(),
            resetZoneId = "America/Chicago",
            reason = "  Restarted plan  ",
            note = "  Kept for history.  ",
        )

        assertNotNull(reset)
        assertEquals(2, reset!!.periods.size)
        assertEquals(1, dao.openPeriodCount(tracker.id))
        val closed = reset.periods.single { it.endEpochMs != null }
        assertEquals(resetAt.toEpochMilli(), closed.endEpochMs)
        assertEquals("America/Chicago", closed.endZoneId)
        assertEquals("Restarted plan", closed.resetReason)
        assertEquals("Kept for history.", closed.resetNote)
        val current = reset.periods.single { it.endEpochMs == null }
        assertEquals(1, current.sequence)
        assertEquals("reset-next-period", current.id)
        assertEquals(resetAt.toEpochMilli(), current.startEpochMs)
        assertEquals("America/Chicago", current.startZoneId)
        assertEquals(30, reset.goal!!.targetAmount)
        assertEquals(now.toEpochMilli(), reset.tracker.updatedAtEpochMs)
    }

    @Test
    fun repositoryResetStreakRollsBackIfNextPeriodInsertFails() = runBlocking {
        val now = Instant.parse("2026-09-24T18:00:00Z")
        val clock = Clock.fixed(now, ZoneId.of("UTC"))
        val collisionPeriodId = "existing-period-id"
        val repository = RoomTrackerRepository(
            dao = dao,
            clock = clock,
            idFactory = { collisionPeriodId },
        )
        val streak = trackerEntity(id = "reset-rollback", kind = TrackerKind.STREAK)
        val streakStart = now.minusSeconds(7_200).toEpochMilli()
        dao.createTrackerAggregate(
            tracker = streak,
            initialPeriod = periodEntity(
                id = "reset-rollback-period",
                eventId = streak.id,
                sequence = 0,
                start = streakStart,
            ),
            goal = null,
        )

        val other = trackerEntity(id = "collision-owner", kind = TrackerKind.EVENT)
        dao.createTrackerAggregate(
            tracker = other,
            initialPeriod = periodEntity(
                id = collisionPeriodId,
                eventId = other.id,
                sequence = 0,
                start = streakStart,
            ),
            goal = null,
        )

        val resetFailure = runCatching {
            repository.resetStreak(
                trackerId = streak.id,
                resetEpochMs = now.minusSeconds(60).toEpochMilli(),
                resetZoneId = "UTC",
                reason = "Should roll back",
                note = null,
            )
        }

        assertTrue(resetFailure.isFailure)
        val preserved = repository.loadTracker(streak.id)!!
        assertEquals(1, preserved.periods.size)
        assertEquals(null, preserved.periods.single().endEpochMs)
        assertEquals(10_000L, preserved.tracker.updatedAtEpochMs)
        assertEquals(1, dao.openPeriodCount(streak.id))
    }

    @Test
    fun repositoryResetStreakRejectsInvalidZoneAndOversizedMetadataWithoutMutation() = runBlocking {
        val now = Instant.parse("2026-09-24T18:00:00Z")
        val repository = RoomTrackerRepository(
            dao = dao,
            clock = Clock.fixed(now, ZoneId.of("UTC")),
        )
        val streak = trackerEntity(id = "reset-input-validation", kind = TrackerKind.STREAK)
        val start = now.minusSeconds(7_200).toEpochMilli()
        dao.createTrackerAggregate(
            tracker = streak,
            initialPeriod = periodEntity(
                id = "reset-input-validation-period",
                eventId = streak.id,
                sequence = 0,
                start = start,
            ),
            goal = null,
        )

        suspend fun assertRejected(
            resetZoneId: String,
            reason: String?,
            note: String?,
        ) {
            assertEquals(
                null,
                repository.resetStreak(
                    trackerId = streak.id,
                    resetEpochMs = now.minusSeconds(60).toEpochMilli(),
                    resetZoneId = resetZoneId,
                    reason = reason,
                    note = note,
                ),
            )
            val unchanged = repository.loadTracker(streak.id)!!
            assertEquals(1, unchanged.periods.size)
            assertEquals(null, unchanged.periods.single().endEpochMs)
            assertEquals(10_000L, unchanged.tracker.updatedAtEpochMs)
            assertEquals(1, dao.openPeriodCount(streak.id))
        }

        assertRejected(
            resetZoneId = "Not/A_Real_Zone",
            reason = null,
            note = null,
        )
        assertRejected(
            resetZoneId = "UTC",
            reason = "r".repeat(121),
            note = null,
        )
        assertRejected(
            resetZoneId = "UTC",
            reason = null,
            note = "n".repeat(2_001),
        )
    }

    @Test
    fun repositoryResetStreakRejectsInvalidChronologyAndPermanentEvents() = runBlocking {
        val now = Instant.parse("2026-09-24T18:00:00Z")
        val clock = Clock.fixed(now, ZoneId.of("UTC"))
        val repository = RoomTrackerRepository(dao = dao, clock = clock)
        val streak = trackerEntity(id = "reset-invalid", kind = TrackerKind.STREAK)
        val start = now.minusSeconds(7_200).toEpochMilli()
        dao.createTrackerAggregate(
            tracker = streak,
            initialPeriod = periodEntity(
                id = "reset-invalid-period",
                eventId = streak.id,
                sequence = 0,
                start = start,
            ),
            goal = null,
        )

        assertEquals(
            null,
            repository.resetStreak(
                trackerId = streak.id,
                resetEpochMs = start - 1,
                resetZoneId = "UTC",
                reason = null,
                note = null,
            ),
        )
        assertEquals(
            null,
            repository.resetStreak(
                trackerId = streak.id,
                resetEpochMs = now.plusSeconds(1).toEpochMilli(),
                resetZoneId = "UTC",
                reason = null,
                note = null,
            ),
        )
        assertEquals(1, dao.openPeriodCount(streak.id))
        assertEquals(1, repository.loadTracker(streak.id)!!.periods.size)

        val event = trackerEntity(id = "reset-event", kind = TrackerKind.EVENT)
        dao.createTrackerAggregate(
            tracker = event,
            initialPeriod = periodEntity(
                id = "reset-event-period",
                eventId = event.id,
                sequence = 0,
                start = start,
            ),
            goal = null,
        )
        assertEquals(
            null,
            repository.resetStreak(
                trackerId = event.id,
                resetEpochMs = now.minusSeconds(60).toEpochMilli(),
                resetZoneId = "UTC",
                reason = null,
                note = null,
            ),
        )
        assertEquals(1, repository.loadTracker(event.id)!!.periods.size)
    }

    @Test
    fun aggregateObservationReactsToPeriodChanges() = runBlocking {
        val clock = Clock.fixed(Instant.parse("2026-09-23T18:00:00Z"), ZoneId.of("UTC"))
        val repository = RoomTrackerRepository(dao = dao, clock = clock)
        val tracker = trackerEntity(id = "reactive-streak", kind = TrackerKind.STREAK)
        dao.createTrackerAggregate(
            tracker = tracker,
            initialPeriod = periodEntity(
                id = "reactive-period-0",
                eventId = tracker.id,
                sequence = 0,
                start = 1_000L,
            ),
            goal = null,
        )

        val changed = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(5_000) {
                repository.observeActiveTrackerAggregates().first { aggregates ->
                    aggregates.singleOrNull()?.periods?.size == 2
                }
            }
        }

        dao.insertPeriod(
            periodEntity(
                id = "reactive-period-1",
                eventId = tracker.id,
                sequence = 1,
                start = 2_000L,
                end = 3_000L,
            )
        )

        assertEquals(2, changed.await().single().periods.size)
    }

    @Test
    fun aggregateObservationReactsToGoalChanges() = runBlocking {
        val clock = Clock.fixed(Instant.parse("2026-09-23T18:00:00Z"), ZoneId.of("UTC"))
        val repository = RoomTrackerRepository(dao = dao, clock = clock)
        val tracker = trackerEntity(id = "reactive-goal", kind = TrackerKind.STREAK)
        dao.createTrackerAggregate(
            tracker = tracker,
            initialPeriod = periodEntity(
                id = "reactive-goal-period",
                eventId = tracker.id,
                sequence = 0,
                start = 1_000L,
            ),
            goal = null,
        )

        val changed = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(5_000) {
                repository.observeActiveTrackerAggregates().first { aggregates ->
                    aggregates.singleOrNull()?.goal?.targetAmount == 14
                }
            }
        }

        dao.insertGoal(
            EventGoalEntity(
                eventId = tracker.id,
                targetAmount = 14,
                targetUnit = DisplayFormat.DAYS.name,
                createdAtEpochMs = 10_000L,
                updatedAtEpochMs = 10_000L,
            )
        )

        assertEquals(14, changed.await().single().goal!!.targetAmount)
    }


    @Test
    fun archiveAndRestorePreserveTrackerHistoryAndGoal() = runBlocking {
        val clock = Clock.fixed(Instant.parse("2026-09-28T20:00:00Z"), ZoneId.of("UTC"))
        val repository = RoomTrackerRepository(dao = dao, clock = clock)
        val tracker = trackerEntity(id = "archive-round-trip", kind = TrackerKind.STREAK)
        dao.createTrackerAggregate(
            tracker = tracker,
            initialPeriod = periodEntity(
                id = "archive-current",
                eventId = tracker.id,
                sequence = 0,
                start = 1_000L,
            ),
            goal = EventGoalEntity(
                eventId = tracker.id,
                targetAmount = 30,
                targetUnit = DisplayFormat.DAYS.name,
                createdAtEpochMs = 10_000L,
                updatedAtEpochMs = 10_000L,
            ),
        )

        val archived = repository.archiveTracker(tracker.id)
        assertNotNull(archived)
        assertTrue(archived!!.tracker.isArchived)
        assertTrue(repository.observeActiveTrackerAggregates().first().isEmpty())

        val archivedList = repository.observeArchivedTrackerAggregates().first()
        assertEquals(1, archivedList.size)
        assertEquals(tracker.id, archivedList.single().tracker.id)
        assertEquals(1, archivedList.single().periods.size)
        assertEquals(30, archivedList.single().goal?.targetAmount)

        val restored = repository.restoreTracker(tracker.id)
        assertNotNull(restored)
        assertFalse(restored!!.tracker.isArchived)
        assertTrue(repository.observeArchivedTrackerAggregates().first().isEmpty())

        val active = repository.observeActiveTrackerAggregates().first().single()
        assertEquals(tracker.id, active.tracker.id)
        assertEquals(1, active.periods.size)
        assertEquals(30, active.goal?.targetAmount)
    }

    @Test
    fun deleteArchivedTrackerRejectsActiveAndCascadesArchivedData() = runBlocking {
        val clock = Clock.fixed(Instant.parse("2026-09-29T12:00:00Z"), ZoneId.of("UTC"))
        val repository = RoomTrackerRepository(dao = dao, clock = clock)
        val tracker = trackerEntity(id = "delete-archived", kind = TrackerKind.STREAK)
        dao.createTrackerAggregate(
            tracker = tracker,
            initialPeriod = periodEntity(
                id = "delete-current",
                eventId = tracker.id,
                sequence = 0,
                start = 1_000L,
            ),
            goal = EventGoalEntity(
                eventId = tracker.id,
                targetAmount = 21,
                targetUnit = DisplayFormat.DAYS.name,
                createdAtEpochMs = 10_000L,
                updatedAtEpochMs = 10_000L,
            ),
        )

        assertFalse(repository.deleteArchivedTracker(tracker.id))
        assertNotNull(repository.loadTracker(tracker.id))

        assertNotNull(repository.archiveTracker(tracker.id))
        assertTrue(repository.deleteArchivedTracker(tracker.id))
        assertNull(repository.loadTracker(tracker.id))
        assertTrue(repository.observeActiveTrackerAggregates().first().isEmpty())
        assertTrue(repository.observeArchivedTrackerAggregates().first().isEmpty())
        assertTrue(dao.observeAllPeriods().first().isEmpty())
        assertTrue(dao.observeAllGoals().first().isEmpty())
    }

    private suspend fun expectSQLiteFailure(
        block: suspend () -> Unit,
    ) {
        try {
            block()
            fail("Expected SQLite to reject the invalid persisted state.")
        } catch (_: SQLiteException) {
            // Expected invariant enforcement.
        }
    }

    private fun trackerEntity(
        id: String,
        kind: TrackerKind,
    ) = TrackedEventEntity(
        id = id,
        title = id,
        note = null,
        kind = kind.name,
        iconKey = null,
        accentKey = null,
        defaultDisplayFormat = DisplayFormat.DAYS.name,
        sortOrder = 0,
        isArchived = false,
        createdAtEpochMs = 10_000L,
        updatedAtEpochMs = 10_000L,
    )

    private fun periodEntity(
        id: String,
        eventId: String,
        sequence: Int,
        start: Long,
        end: Long? = null,
    ) = EventPeriodEntity(
        id = id,
        eventId = eventId,
        sequence = sequence,
        startEpochMs = start,
        startZoneId = "UTC",
        endEpochMs = end,
        endZoneId = end?.let { "UTC" },
        resetReason = null,
        resetNote = null,
        createdAtEpochMs = 10_000L,
        updatedAtEpochMs = 10_000L,
    )
}
