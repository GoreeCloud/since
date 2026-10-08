package com.goreecloud.since.data.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "tracked_events",
    indices = [
        Index(
            value = ["sort_order"],
            name = "index_tracked_events_sort_order",
        ),
    ],
)
data class TrackedEventEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val note: String?,
    val kind: String,
    @ColumnInfo(name = "icon_key")
    val iconKey: String?,
    @ColumnInfo(name = "accent_key")
    val accentKey: String?,
    @ColumnInfo(name = "default_display_format")
    val defaultDisplayFormat: String,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean,
    @ColumnInfo(name = "created_at_epoch_ms")
    val createdAtEpochMs: Long,
    @ColumnInfo(name = "updated_at_epoch_ms")
    val updatedAtEpochMs: Long,
)

@Entity(
    tableName = "event_periods",
    foreignKeys = [
        ForeignKey(
            entity = TrackedEventEntity::class,
            parentColumns = ["id"],
            childColumns = ["event_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(
            value = ["event_id"],
            name = "index_event_periods_event_id",
        ),
        Index(
            value = ["event_id", "sequence"],
            name = "index_event_periods_event_id_sequence",
            unique = true,
        ),
    ],
)
data class EventPeriodEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "event_id")
    val eventId: String,
    val sequence: Int,
    @ColumnInfo(name = "start_epoch_ms")
    val startEpochMs: Long,
    @ColumnInfo(name = "start_zone_id")
    val startZoneId: String,
    @ColumnInfo(name = "end_epoch_ms")
    val endEpochMs: Long?,
    @ColumnInfo(name = "end_zone_id")
    val endZoneId: String?,
    @ColumnInfo(name = "reset_reason")
    val resetReason: String?,
    @ColumnInfo(name = "reset_note")
    val resetNote: String?,
    @ColumnInfo(name = "created_at_epoch_ms")
    val createdAtEpochMs: Long,
    @ColumnInfo(name = "updated_at_epoch_ms")
    val updatedAtEpochMs: Long,
)

@Entity(
    tableName = "event_goals",
    foreignKeys = [
        ForeignKey(
            entity = TrackedEventEntity::class,
            parentColumns = ["id"],
            childColumns = ["event_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class EventGoalEntity(
    @PrimaryKey
    @ColumnInfo(name = "event_id")
    val eventId: String,
    @ColumnInfo(name = "target_amount")
    val targetAmount: Int,
    @ColumnInfo(name = "target_unit")
    val targetUnit: String,
    @ColumnInfo(name = "created_at_epoch_ms")
    val createdAtEpochMs: Long,
    @ColumnInfo(name = "updated_at_epoch_ms")
    val updatedAtEpochMs: Long,
)
