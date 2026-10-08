package com.goreecloud.since.domain.portability

import android.util.JsonReader
import android.util.JsonToken
import com.goreecloud.since.domain.model.DisplayFormat
import com.goreecloud.since.domain.model.TrackerKind
import java.io.StringReader
import java.time.ZoneId

data class SinceImportReviewSummary(
    val trackerCount: Int,
    val archivedTrackerCount: Int,
    val periodCount: Int,
    val goalCount: Int,
    val exportedAtEpochMs: Long,
    val trackerIds: Set<String>,
)

data class SinceImportReplacementPlan(
    val currentTrackerCount: Int,
    val importedTrackerCount: Int,
    val matchingTrackerCount: Int,
)

object SinceImportReplacementPlanner {
    fun plan(
        summary: SinceImportReviewSummary,
        currentTrackerIds: Set<String>,
    ): SinceImportReplacementPlan {
        val normalizedCurrentIds = currentTrackerIds
            .asSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toSet()
        return SinceImportReplacementPlan(
            currentTrackerCount = normalizedCurrentIds.size,
            importedTrackerCount = summary.trackerCount,
            matchingTrackerCount = summary.trackerIds.count { it in normalizedCurrentIds },
        )
    }
}

sealed interface SinceImportReviewResult {
    data class Valid(
        val summary: SinceImportReviewSummary,
    ) : SinceImportReviewResult

    data object Invalid : SinceImportReviewResult
}

/**
 * Strictly reviews a bounded GoreeCloud Since export without mutating application data.
 *
 * Import review intentionally shares the export schema but not a database write path. Unknown
 * fields, duplicate fields/IDs, unsupported schema versions, invalid enum values, malformed zone
 * IDs, broken period chronology, invalid goal shapes, and trailing JSON all fail closed.
 */
object SinceImportReviewJson {
    const val MAX_IMPORT_BYTES: Int = 4 * 1024 * 1024
    private const val MAX_TRACKERS = 10_000
    private const val MAX_PERIODS = 100_000

    fun review(payload: String): SinceImportReviewResult =
        runCatching {
            JsonReader(StringReader(payload)).use { reader ->
                reader.isLenient = false
                val result = parseRoot(reader)
                check(reader.peek() == JsonToken.END_DOCUMENT)
                result
            }
        }.getOrElse {
            SinceImportReviewResult.Invalid
        }

    private fun parseRoot(reader: JsonReader): SinceImportReviewResult.Valid {
        var format: String? = null
        var schemaVersion: Int? = null
        var exportedAtEpochMs: Long? = null
        var trackerSummary: TrackerSummary? = null
        val seen = mutableSetOf<String>()
        val trackerIds = mutableSetOf<String>()
        val periodIds = mutableSetOf<String>()

        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            check(seen.add(name))
            when (name) {
                "format" -> format = reader.nextString()
                "schemaVersion" -> schemaVersion = reader.nextInt()
                "exportedAtEpochMs" -> exportedAtEpochMs = reader.nextLong()
                "trackers" -> trackerSummary = parseTrackers(reader, trackerIds, periodIds)
                else -> error("Unsupported field")
            }
        }
        reader.endObject()

        check(seen == setOf("format", "schemaVersion", "exportedAtEpochMs", "trackers"))
        check(format == SinceExportJson.FORMAT_ID)
        check(schemaVersion == SinceExportJson.SCHEMA_VERSION)

        val exportedAt = checkNotNull(exportedAtEpochMs)
        val trackers = checkNotNull(trackerSummary)
        return SinceImportReviewResult.Valid(
            SinceImportReviewSummary(
                trackerCount = trackers.count,
                archivedTrackerCount = trackers.archivedCount,
                periodCount = trackers.periodCount,
                goalCount = trackers.goalCount,
                exportedAtEpochMs = exportedAt,
                trackerIds = trackerIds.toSet(),
            ),
        )
    }

    private fun parseTrackers(
        reader: JsonReader,
        trackerIds: MutableSet<String>,
        periodIds: MutableSet<String>,
    ): TrackerSummary {
        var count = 0
        var archivedCount = 0
        var periodCount = 0
        var goalCount = 0

        reader.beginArray()
        while (reader.hasNext()) {
            check(count < MAX_TRACKERS)
            val tracker = parseTracker(reader, trackerIds, periodIds)
            count += 1
            if (tracker.archived) archivedCount += 1
            periodCount += tracker.periodCount
            goalCount += if (tracker.hasGoal) 1 else 0
            check(periodCount <= MAX_PERIODS)
        }
        reader.endArray()

        return TrackerSummary(
            count = count,
            archivedCount = archivedCount,
            periodCount = periodCount,
            goalCount = goalCount,
        )
    }

    private fun parseTracker(
        reader: JsonReader,
        trackerIds: MutableSet<String>,
        periodIds: MutableSet<String>,
    ): ParsedTracker {
        val seen = mutableSetOf<String>()
        var id: String? = null
        var title: String? = null
        var kind: TrackerKind? = null
        var displayFormat: DisplayFormat? = null
        var sortOrder: Int? = null
        var archived: Boolean? = null
        var createdAtEpochMs: Long? = null
        var updatedAtEpochMs: Long? = null
        var periods: PeriodSummary? = null
        var goal: GoalSummary? = null
        var goalWasRead = false

        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            check(seen.add(name))
            when (name) {
                "id" -> id = reader.nextString()
                "title" -> title = reader.nextString()
                "note" -> reader.nextNullableString()
                "kind" -> kind = TrackerKind.valueOf(reader.nextString())
                "iconKey" -> reader.nextNullableString()
                "accentKey" -> reader.nextNullableString()
                "defaultDisplayFormat" ->
                    displayFormat = DisplayFormat.valueOf(reader.nextString())
                "sortOrder" -> sortOrder = reader.nextInt()
                "isArchived" -> archived = reader.nextBoolean()
                "createdAtEpochMs" -> createdAtEpochMs = reader.nextLong()
                "updatedAtEpochMs" -> updatedAtEpochMs = reader.nextLong()
                "periods" -> periods = parsePeriods(reader, periodIds)
                "goal" -> {
                    goalWasRead = true
                    goal = parseNullableGoal(reader)
                }
                else -> error("Unsupported field")
            }
        }
        reader.endObject()

        check(
            seen == setOf(
                "id",
                "title",
                "note",
                "kind",
                "iconKey",
                "accentKey",
                "defaultDisplayFormat",
                "sortOrder",
                "isArchived",
                "createdAtEpochMs",
                "updatedAtEpochMs",
                "periods",
                "goal",
            ),
        )

        val trackerId = checkNotNull(id)
        check(trackerId.isNotBlank())
        check(trackerIds.add(trackerId))
        check(!title.isNullOrBlank())
        checkNotNull(displayFormat)
        checkNotNull(sortOrder)
        val trackerKind = checkNotNull(kind)
        val trackerArchived = checkNotNull(archived)
        val createdAt = checkNotNull(createdAtEpochMs)
        val updatedAt = checkNotNull(updatedAtEpochMs)
        check(updatedAt >= createdAt)
        check(goalWasRead)
        val periodSummary = checkNotNull(periods)
        check(periodSummary.count > 0)
        check(periodSummary.openCount == 1)
        check(periodSummary.sequences == (0 until periodSummary.count).toSet())

        if (trackerKind == TrackerKind.EVENT) {
            check(periodSummary.count == 1)
            check(goal == null)
        }

        return ParsedTracker(
            archived = trackerArchived,
            periodCount = periodSummary.count,
            hasGoal = goal != null,
        )
    }

    private fun parsePeriods(
        reader: JsonReader,
        periodIds: MutableSet<String>,
    ): PeriodSummary {
        var count = 0
        var openCount = 0
        val sequences = mutableSetOf<Int>()

        reader.beginArray()
        while (reader.hasNext()) {
            check(count < MAX_PERIODS)
            val period = parsePeriod(reader, periodIds)
            count += 1
            if (period.open) openCount += 1
            check(sequences.add(period.sequence))
        }
        reader.endArray()

        return PeriodSummary(
            count = count,
            openCount = openCount,
            sequences = sequences,
        )
    }

    private fun parsePeriod(
        reader: JsonReader,
        periodIds: MutableSet<String>,
    ): ParsedPeriod {
        val seen = mutableSetOf<String>()
        var id: String? = null
        var sequence: Int? = null
        var startEpochMs: Long? = null
        var startZoneId: String? = null
        var endEpochMs: Long? = null
        var endEpochWasRead = false
        var endZoneId: String? = null
        var endZoneWasRead = false
        var createdAtEpochMs: Long? = null
        var updatedAtEpochMs: Long? = null

        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            check(seen.add(name))
            when (name) {
                "id" -> id = reader.nextString()
                "sequence" -> sequence = reader.nextInt()
                "startEpochMs" -> startEpochMs = reader.nextLong()
                "startZoneId" -> startZoneId = reader.nextString()
                "endEpochMs" -> {
                    endEpochWasRead = true
                    endEpochMs = reader.nextNullableLong()
                }
                "endZoneId" -> {
                    endZoneWasRead = true
                    endZoneId = reader.nextNullableString()
                }
                "resetReason" -> reader.nextNullableString()
                "resetNote" -> reader.nextNullableString()
                "createdAtEpochMs" -> createdAtEpochMs = reader.nextLong()
                "updatedAtEpochMs" -> updatedAtEpochMs = reader.nextLong()
                else -> error("Unsupported field")
            }
        }
        reader.endObject()

        check(
            seen == setOf(
                "id",
                "sequence",
                "startEpochMs",
                "startZoneId",
                "endEpochMs",
                "endZoneId",
                "resetReason",
                "resetNote",
                "createdAtEpochMs",
                "updatedAtEpochMs",
            ),
        )

        val periodId = checkNotNull(id)
        check(periodId.isNotBlank())
        check(periodIds.add(periodId))
        val periodSequence = checkNotNull(sequence)
        check(periodSequence >= 0)
        val start = checkNotNull(startEpochMs)
        ZoneId.of(checkNotNull(startZoneId))
        check(endEpochWasRead)
        check(endZoneWasRead)
        val end = endEpochMs
        if (end == null) {
            check(endZoneId == null)
        } else {
            check(end >= start)
            ZoneId.of(checkNotNull(endZoneId))
        }
        val createdAt = checkNotNull(createdAtEpochMs)
        val updatedAt = checkNotNull(updatedAtEpochMs)
        check(updatedAt >= createdAt)

        return ParsedPeriod(
            sequence = periodSequence,
            open = end == null,
        )
    }

    private fun parseNullableGoal(reader: JsonReader): GoalSummary? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }

        val seen = mutableSetOf<String>()
        var targetAmount: Int? = null
        var targetUnit: DisplayFormat? = null
        var createdAtEpochMs: Long? = null
        var updatedAtEpochMs: Long? = null

        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            check(seen.add(name))
            when (name) {
                "targetAmount" -> targetAmount = reader.nextInt()
                "targetUnit" -> targetUnit = DisplayFormat.valueOf(reader.nextString())
                "createdAtEpochMs" -> createdAtEpochMs = reader.nextLong()
                "updatedAtEpochMs" -> updatedAtEpochMs = reader.nextLong()
                else -> error("Unsupported field")
            }
        }
        reader.endObject()

        check(seen == setOf("targetAmount", "targetUnit", "createdAtEpochMs", "updatedAtEpochMs"))
        check(checkNotNull(targetAmount) in 1..100_000)
        checkNotNull(targetUnit)
        val createdAt = checkNotNull(createdAtEpochMs)
        val updatedAt = checkNotNull(updatedAtEpochMs)
        check(updatedAt >= createdAt)
        return GoalSummary
    }

    private fun JsonReader.nextNullableString(): String? =
        if (peek() == JsonToken.NULL) {
            nextNull()
            null
        } else {
            nextString()
        }

    private fun JsonReader.nextNullableLong(): Long? =
        if (peek() == JsonToken.NULL) {
            nextNull()
            null
        } else {
            nextLong()
        }

    private data class TrackerSummary(
        val count: Int,
        val archivedCount: Int,
        val periodCount: Int,
        val goalCount: Int,
    )

    private data class ParsedTracker(
        val archived: Boolean,
        val periodCount: Int,
        val hasGoal: Boolean,
    )

    private data class PeriodSummary(
        val count: Int,
        val openCount: Int,
        val sequences: Set<Int>,
    )

    private data class ParsedPeriod(
        val sequence: Int,
        val open: Boolean,
    )

    private data object GoalSummary
}
