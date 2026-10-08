package com.goreecloud.since.domain.repository

import com.goreecloud.since.domain.model.DisplayFormat
import com.goreecloud.since.domain.model.Goal
import com.goreecloud.since.domain.model.Tracker
import com.goreecloud.since.domain.model.TrackerAggregate
import com.goreecloud.since.domain.validation.ValidatedTrackerDraft
import kotlinx.coroutines.flow.Flow

interface TrackerRepository {
    fun observeActiveTrackers(): Flow<List<Tracker>>

    fun observeActiveTrackerAggregates(): Flow<List<TrackerAggregate>>

    fun observeArchivedTrackerAggregates(): Flow<List<TrackerAggregate>>

    suspend fun createTracker(draft: ValidatedTrackerDraft): TrackerAggregate

    suspend fun loadTracker(trackerId: String): TrackerAggregate?

    suspend fun updateTracker(
        trackerId: String,
        draft: ValidatedTrackerDraft,
    ): TrackerAggregate?

    suspend fun updateDisplayFormat(
        trackerId: String,
        displayFormat: DisplayFormat,
    ): Boolean

    suspend fun updateGoal(
        trackerId: String,
        targetAmount: Int,
        targetUnit: DisplayFormat,
    ): Goal?

    suspend fun removeGoal(trackerId: String): Boolean

    suspend fun archiveTracker(trackerId: String): TrackerAggregate?

    suspend fun restoreTracker(trackerId: String): TrackerAggregate?

    suspend fun deleteArchivedTracker(trackerId: String): Boolean

    suspend fun moveTracker(trackerId: String, delta: Int): Boolean

    suspend fun resetStreak(
        trackerId: String,
        resetEpochMs: Long,
        resetZoneId: String,
        reason: String?,
        note: String?,
    ): TrackerAggregate?
}
