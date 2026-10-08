package com.goreecloud.since.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.goreecloud.since.data.repository.RoomTrackerRepository
import com.goreecloud.since.domain.model.DisplayFormat
import com.goreecloud.since.domain.model.TrackerKind
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SinceResetPersistenceRuntimeTest {
    @Test
    fun resetHistoryAndGoalSurviveFileBackedDatabaseReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(SinceDatabase.NAME)

        var database: SinceDatabase? = null
        try {
            val now = Instant.parse("2026-09-26T18:00:00Z")
            val resetAt = now.minusSeconds(3_600)
            val startAt = now.minusSeconds(7_200)
            val trackerId = "persisted-reset-streak"
            val nextPeriodId = "persisted-reset-period-1"

            database = SinceDatabaseFactory.build(context)
            val dao = database.trackerDao()
            dao.createTrackerAggregate(
                tracker = TrackedEventEntity(
                    id = trackerId,
                    title = "Persistence streak",
                    note = null,
                    kind = TrackerKind.STREAK.name,
                    iconKey = null,
                    accentKey = null,
                    defaultDisplayFormat = DisplayFormat.DAYS.name,
                    sortOrder = 0,
                    isArchived = false,
                    createdAtEpochMs = startAt.toEpochMilli(),
                    updatedAtEpochMs = startAt.toEpochMilli(),
                ),
                initialPeriod = EventPeriodEntity(
                    id = "persisted-reset-period-0",
                    eventId = trackerId,
                    sequence = 0,
                    startEpochMs = startAt.toEpochMilli(),
                    startZoneId = "UTC",
                    endEpochMs = null,
                    endZoneId = null,
                    resetReason = null,
                    resetNote = null,
                    createdAtEpochMs = startAt.toEpochMilli(),
                    updatedAtEpochMs = startAt.toEpochMilli(),
                ),
                goal = EventGoalEntity(
                    eventId = trackerId,
                    targetAmount = 30,
                    targetUnit = DisplayFormat.DAYS.name,
                    createdAtEpochMs = startAt.toEpochMilli(),
                    updatedAtEpochMs = startAt.toEpochMilli(),
                ),
            )

            val repository = RoomTrackerRepository(
                dao = dao,
                clock = Clock.fixed(now, ZoneId.of("UTC")),
                idFactory = { nextPeriodId },
            )
            val reset = repository.resetStreak(
                trackerId = trackerId,
                resetEpochMs = resetAt.toEpochMilli(),
                resetZoneId = "America/Chicago",
                reason = "  Restarted after interruption  ",
                note = "  Preserve this history.  ",
            )
            assertNotNull(reset)

            database.close()
            database = null

            database = SinceDatabaseFactory.build(context)
            val reopenedDao = database.trackerDao()
            val reopened = RoomTrackerRepository(
                dao = reopenedDao,
                clock = Clock.fixed(now, ZoneId.of("UTC")),
            ).loadTracker(trackerId)
            assertNotNull(reopened)

            val aggregate = checkNotNull(reopened)
            assertEquals(2, aggregate.periods.size)
            assertEquals(1, reopenedDao.openPeriodCount(trackerId))

            val closed = aggregate.periods.single { it.endEpochMs != null }
            assertEquals(resetAt.toEpochMilli(), closed.endEpochMs)
            assertEquals("America/Chicago", closed.endZoneId)
            assertEquals("Restarted after interruption", closed.resetReason)
            assertEquals("Preserve this history.", closed.resetNote)

            val current = aggregate.periods.single { it.endEpochMs == null }
            assertEquals(nextPeriodId, current.id)
            assertEquals(1, current.sequence)
            assertEquals(resetAt.toEpochMilli(), current.startEpochMs)
            assertEquals("America/Chicago", current.startZoneId)

            assertEquals(30, aggregate.goal?.targetAmount)
            assertEquals(DisplayFormat.DAYS, aggregate.goal?.targetUnit)
            assertEquals(now.toEpochMilli(), aggregate.tracker.updatedAtEpochMs)
        } finally {
            database?.close()
            context.deleteDatabase(SinceDatabase.NAME)
        }
    }
}
