package com.goreecloud.since.data.local

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver

@Database(
    entities = [
        TrackedEventEntity::class,
        EventPeriodEntity::class,
        EventGoalEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class SinceDatabase : RoomDatabase() {
    abstract fun trackerDao(): TrackerDao

    companion object {
        const val NAME = "since.db"
    }
}

object SinceDatabaseFactory {
    fun build(context: Context): SinceDatabase =
        Room.databaseBuilder(
            context.applicationContext,
            SinceDatabase::class.java,
            SinceDatabase.NAME,
        )
            .setDriver(AndroidSQLiteDriver())
            .addCallback(SinceDatabaseCallback)
            .build()

    internal fun buildInMemoryForTest(context: Context): SinceDatabase =
        Room.inMemoryDatabaseBuilder(
            context.applicationContext,
            SinceDatabase::class.java,
        )
            .setDriver(AndroidSQLiteDriver())
            .addCallback(SinceDatabaseCallback)
            .build()
}
