package com.goreecloud.since.data.local

import android.content.Context
import androidx.sqlite.SQLiteException
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.goreecloud.since.domain.model.DisplayFormat
import com.goreecloud.since.domain.model.TrackerKind
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
