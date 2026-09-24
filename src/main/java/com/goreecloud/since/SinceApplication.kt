package com.goreecloud.since

import android.app.Application
import com.goreecloud.since.data.local.SinceDatabaseFactory
import com.goreecloud.since.data.repository.RoomTrackerRepository
import java.time.Clock

class SinceApplication : Application() {
    val database by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        SinceDatabaseFactory.build(this)
    }

    val trackerRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        RoomTrackerRepository(
            dao = database.trackerDao(),
            clock = Clock.systemUTC(),
        )
    }
}
