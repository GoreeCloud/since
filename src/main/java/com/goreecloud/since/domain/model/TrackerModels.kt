package com.goreecloud.since.domain.model

enum class TrackerKind {
    EVENT,
    STREAK,
}

enum class DisplayFormat {
    DAYS,
    WEEKS,
    MONTHS,
    YEARS,
}

data class Tracker(
    val id: String,
    val title: String,
    val note: String?,
    val kind: TrackerKind,
    val iconKey: String?,
    val accentKey: String?,
    val defaultDisplayFormat: DisplayFormat,
    val sortOrder: Int,
    val isArchived: Boolean,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

data class TrackerPeriod(
    val id: String,
    val trackerId: String,
    val sequence: Int,
    val startEpochMs: Long,
    val startZoneId: String,
    val endEpochMs: Long?,
    val endZoneId: String?,
    val resetReason: String?,
    val resetNote: String?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

data class Goal(
    val trackerId: String,
    val targetAmount: Int,
    val targetUnit: DisplayFormat,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)
