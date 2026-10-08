package com.goreecloud.since.data.local

import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * SQLite invariants that Room 3.0 cannot currently express through entity annotations.
 *
 * In particular, Room's Index annotation has no WHERE predicate, so the one-open-period rule is
 * installed as a partial unique index after Room creates or opens the database. Future migrations
 * must drop these manual objects before Room schema validation when required, then allow this
 * callback to reinstall them after the migrated database opens.
 */
internal object SinceSchemaInvariants {
    const val OPEN_PERIOD_INDEX =
        "index_event_periods_one_open_per_event"

    private val statements = listOf(
        """
        CREATE UNIQUE INDEX IF NOT EXISTS $OPEN_PERIOD_INDEX
        ON event_periods(event_id)
        WHERE end_epoch_ms IS NULL
        """.trimIndent(),
        """
        CREATE TRIGGER IF NOT EXISTS event_periods_valid_end_insert
        BEFORE INSERT ON event_periods
        WHEN NEW.end_epoch_ms IS NOT NULL AND NEW.end_epoch_ms < NEW.start_epoch_ms
        BEGIN
            SELECT RAISE(ABORT, 'event period end precedes start');
        END
        """.trimIndent(),
        """
        CREATE TRIGGER IF NOT EXISTS event_periods_valid_end_update
        BEFORE UPDATE OF start_epoch_ms, end_epoch_ms ON event_periods
        WHEN NEW.end_epoch_ms IS NOT NULL AND NEW.end_epoch_ms < NEW.start_epoch_ms
        BEGIN
            SELECT RAISE(ABORT, 'event period end precedes start');
        END
        """.trimIndent(),
        """
        CREATE TRIGGER IF NOT EXISTS event_periods_single_event_period
        BEFORE INSERT ON event_periods
        WHEN (
            SELECT kind FROM tracked_events WHERE id = NEW.event_id
        ) = 'EVENT'
        AND EXISTS (
            SELECT 1 FROM event_periods WHERE event_id = NEW.event_id
        )
        BEGIN
            SELECT RAISE(ABORT, 'permanent events may have only one period');
        END
        """.trimIndent(),
        """
        CREATE TRIGGER IF NOT EXISTS event_goals_streak_only_insert
        BEFORE INSERT ON event_goals
        WHEN NEW.target_amount <= 0
        OR (
            SELECT kind FROM tracked_events WHERE id = NEW.event_id
        ) != 'STREAK'
        BEGIN
            SELECT RAISE(ABORT, 'goals require a streak tracker and positive target');
        END
        """.trimIndent(),
        """
        CREATE TRIGGER IF NOT EXISTS event_goals_streak_only_update
        BEFORE UPDATE OF target_amount, target_unit, event_id ON event_goals
        WHEN NEW.target_amount <= 0
        OR (
            SELECT kind FROM tracked_events WHERE id = NEW.event_id
        ) != 'STREAK'
        BEGIN
            SELECT RAISE(ABORT, 'goals require a streak tracker and positive target');
        END
        """.trimIndent(),
    )

    fun install(connection: SQLiteConnection) {
        statements.forEach(connection::execSQL)
    }
}

internal object SinceDatabaseCallback : RoomDatabase.Callback() {
    override suspend fun onCreate(connection: SQLiteConnection) {
        SinceSchemaInvariants.install(connection)
    }

    override suspend fun onOpen(connection: SQLiteConnection) {
        SinceSchemaInvariants.install(connection)
    }
}
