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
import org.junit.Assert.assertNotNull
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
