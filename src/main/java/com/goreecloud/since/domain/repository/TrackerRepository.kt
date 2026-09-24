package com.goreecloud.since.domain.repository

import com.goreecloud.since.domain.model.DisplayFormat
import com.goreecloud.since.domain.model.Tracker
import com.goreecloud.since.domain.model.TrackerAggregate
import com.goreecloud.since.domain.validation.ValidatedTrackerDraft
import kotlinx.coroutines.flow.Flow

interface TrackerRepository {
    fun observeActiveTrackers(): Flow<List<Tracker>>

    fun observeActiveTrackerAggregates(): Flow<List<TrackerAggregate>>

    suspend fun createTracker(draft: ValidatedTrackerDraft): TrackerAggregate

    suspend fun loadTracker(trackerId: String): TrackerAggregate?

    suspend fun updateDisplayFormat(
        trackerId: String,
        displayFormat: DisplayFormat,
    ): Boolean
}
