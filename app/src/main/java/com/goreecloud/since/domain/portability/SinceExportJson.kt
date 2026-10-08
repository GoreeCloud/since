package com.goreecloud.since.domain.portability

import com.goreecloud.since.domain.model.TrackerAggregate
import java.time.Instant
import java.time.ZoneOffset

object SinceExportJson {
    const val FORMAT_ID = "goreecloud-since-export"
    const val SCHEMA_VERSION = 1

    fun fileName(exportedAtEpochMs: Long): String {
        val date = Instant.ofEpochMilli(exportedAtEpochMs)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
        return "GoreeCloud-Since-$date.json"
    }

    fun encode(
        aggregates: List<TrackerAggregate>,
        exportedAtEpochMs: Long,
    ): String = buildString {
        val ordered = aggregates.sortedWith(
            compareBy(
                { it.tracker.sortOrder },
                { it.tracker.createdAtEpochMs },
                { it.tracker.id },
            ),
        )

        append("{\n")
        append("  \"format\": ")
        appendJsonString(FORMAT_ID)
        append(",\n  \"schemaVersion\": ")
        append(SCHEMA_VERSION)
        append(",\n  \"exportedAtEpochMs\": ")
        append(exportedAtEpochMs)
        append(",\n  \"trackers\": [")

        ordered.forEachIndexed { index, aggregate ->
            if (index > 0) append(",")
            append("\n    {\n")
            append("      \"id\": ")
            appendJsonString(aggregate.tracker.id)
            append(",\n      \"title\": ")
            appendJsonString(aggregate.tracker.title)
            append(",\n      \"note\": ")
            appendNullableJsonString(aggregate.tracker.note)
            append(",\n      \"kind\": ")
            appendJsonString(aggregate.tracker.kind.name)
            append(",\n      \"iconKey\": ")
            appendNullableJsonString(aggregate.tracker.iconKey)
            append(",\n      \"accentKey\": ")
            appendNullableJsonString(aggregate.tracker.accentKey)
            append(",\n      \"defaultDisplayFormat\": ")
            appendJsonString(aggregate.tracker.defaultDisplayFormat.name)
            append(",\n      \"sortOrder\": ")
            append(aggregate.tracker.sortOrder)
            append(",\n      \"isArchived\": ")
            append(aggregate.tracker.isArchived)
            append(",\n      \"createdAtEpochMs\": ")
            append(aggregate.tracker.createdAtEpochMs)
            append(",\n      \"updatedAtEpochMs\": ")
            append(aggregate.tracker.updatedAtEpochMs)
            append(",\n      \"periods\": [")

            aggregate.periods
                .sortedWith(compareBy({ it.sequence }, { it.id }))
                .forEachIndexed { periodIndex, period ->
                    if (periodIndex > 0) append(",")
                    append("\n        {")
                    append("\"id\": ")
                    appendJsonString(period.id)
                    append(", \"sequence\": ")
                    append(period.sequence)
                    append(", \"startEpochMs\": ")
                    append(period.startEpochMs)
                    append(", \"startZoneId\": ")
                    appendJsonString(period.startZoneId)
                    append(", \"endEpochMs\": ")
                    appendNullableLong(period.endEpochMs)
                    append(", \"endZoneId\": ")
                    appendNullableJsonString(period.endZoneId)
                    append(", \"resetReason\": ")
                    appendNullableJsonString(period.resetReason)
                    append(", \"resetNote\": ")
                    appendNullableJsonString(period.resetNote)
                    append(", \"createdAtEpochMs\": ")
                    append(period.createdAtEpochMs)
                    append(", \"updatedAtEpochMs\": ")
                    append(period.updatedAtEpochMs)
                    append("}")
                }

            if (aggregate.periods.isNotEmpty()) append("\n      ")
            append("],\n      \"goal\": ")
            val goal = aggregate.goal
            if (goal == null) {
                append("null")
            } else {
                append("{")
                append("\"targetAmount\": ")
                append(goal.targetAmount)
                append(", \"targetUnit\": ")
                appendJsonString(goal.targetUnit.name)
                append(", \"createdAtEpochMs\": ")
                append(goal.createdAtEpochMs)
                append(", \"updatedAtEpochMs\": ")
                append(goal.updatedAtEpochMs)
                append("}")
            }
            append("\n    }")
        }

        if (ordered.isNotEmpty()) append("\n  ")
        append("]\n}\n")
    }

    private fun StringBuilder.appendNullableLong(value: Long?) {
        if (value == null) append("null") else append(value)
    }

    private fun StringBuilder.appendNullableJsonString(value: String?) {
        if (value == null) append("null") else appendJsonString(value)
    }

    private fun StringBuilder.appendJsonString(value: String) {
        append('"')
        value.forEach { character ->
            when (character) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> {
                    if (character.code < 0x20) {
                        append("\\u")
                        append(character.code.toString(16).padStart(4, '0'))
                    } else {
                        append(character)
                    }
                }
            }
        }
        append('"')
    }
}
