package com.goreecloud.since.domain.time

import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

data class ResolvedTrackerStart(
    val epochMs: Long,
    val zoneId: String,
    val overlapResolvedToEarlierOffset: Boolean,
)

sealed interface TrackerStartResolution {
    data class Valid(val start: ResolvedTrackerStart) : TrackerStartResolution
    data class Invalid(val errors: List<String>) : TrackerStartResolution
}

object TrackerStartInput {
    private val formatter = DateTimeFormatter
        .ofPattern("uuuu-MM-dd HH:mm")
        .withResolverStyle(ResolverStyle.STRICT)

    const val FORMAT_HINT = "YYYY-MM-DD HH:MM"

    fun format(
        epochMs: Long,
        zoneId: String,
    ): String {
        val zone = ZoneId.of(zoneId)
        return formatter.format(Instant.ofEpochMilli(epochMs).atZone(zone))
    }

    fun resolve(
        localDateTimeText: String,
        zoneIdText: String,
    ): TrackerStartResolution {
        val errors = mutableListOf<String>()
        val zone = try {
            ZoneId.of(zoneIdText.trim())
        } catch (_: DateTimeException) {
            errors += "Start time zone must be a valid IANA ZoneId."
            null
        }

        val localDateTime = try {
            LocalDateTime.parse(localDateTimeText.trim(), formatter)
        } catch (_: DateTimeParseException) {
            errors += "Start date/time must use YYYY-MM-DD HH:MM."
            null
        }

        if (zone == null || localDateTime == null) {
            return TrackerStartResolution.Invalid(errors)
        }

        val offsets = zone.rules.getValidOffsets(localDateTime)
        if (offsets.isEmpty()) {
            return TrackerStartResolution.Invalid(
                listOf(
                    "Selected local time does not exist in " + zone.id +
                        " because of a daylight-saving transition."
                )
            )
        }

        val selectedOffset = offsets.first()
        val instant = localDateTime.atOffset(selectedOffset).toInstant()
        return TrackerStartResolution.Valid(
            ResolvedTrackerStart(
                epochMs = instant.toEpochMilli(),
                zoneId = zone.id,
                overlapResolvedToEarlierOffset = offsets.size > 1,
            )
        )
    }
}
