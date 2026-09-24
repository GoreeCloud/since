package com.goreecloud.since.domain.validation

import com.goreecloud.since.domain.model.DisplayFormat
import com.goreecloud.since.domain.model.TrackerKind
import java.text.Normalizer
import java.time.Clock
import java.time.DateTimeException
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

data class TrackerDraft(
    val title: String,
    val note: String?,
    val kind: TrackerKind,
    val startEpochMs: Long,
    val startZoneId: String,
    val displayFormat: DisplayFormat,
    val goalAmount: Int? = null,
    val goalUnit: DisplayFormat? = null,
)

data class ValidatedTrackerDraft(
    val title: String,
    val note: String?,
    val kind: TrackerKind,
    val startEpochMs: Long,
    val startZoneId: String,
    val displayFormat: DisplayFormat,
    val goalAmount: Int?,
    val goalUnit: DisplayFormat?,
)

sealed interface TrackerDraftValidation {
    data class Valid(val draft: ValidatedTrackerDraft) : TrackerDraftValidation
    data class Invalid(val errors: List<String>) : TrackerDraftValidation
}

class TrackerDraftValidator(
    private val clock: Clock,
    private val futureTolerance: Duration = Duration.ofSeconds(2),
) {
    fun validate(draft: TrackerDraft): TrackerDraftValidation {
        val errors = mutableListOf<String>()
        val title = Normalizer.normalize(draft.title.trim(), Normalizer.Form.NFC)
        val note = draft.note
            ?.let { Normalizer.normalize(it.trim(), Normalizer.Form.NFC) }
            ?.takeIf { it.isNotEmpty() }

        val titleLength = title.codePointCount(0, title.length)
        if (titleLength !in 1..80) {
            errors += "Title must contain between 1 and 80 Unicode characters."
        }

        if (note != null && note.codePointCount(0, note.length) > 2_000) {
            errors += "Note must not exceed 2,000 Unicode characters."
        }

        try {
            ZoneId.of(draft.startZoneId)
        } catch (_: DateTimeException) {
            errors += "Start time zone must be a valid IANA ZoneId."
        }

        val latestAllowedStart = clock.instant().plus(futureTolerance)
        val startInstant = runCatching { Instant.ofEpochMilli(draft.startEpochMs) }.getOrNull()
        if (startInstant == null || startInstant.isAfter(latestAllowedStart)) {
            errors += "Start time must not be in the future."
        }

        val hasAnyGoalField = draft.goalAmount != null || draft.goalUnit != null
        if (hasAnyGoalField && draft.kind != TrackerKind.STREAK) {
            errors += "Goals are supported only for streak trackers."
        }

        if ((draft.goalAmount == null) != (draft.goalUnit == null)) {
            errors += "Goal amount and unit must be supplied together."
        }

        if (draft.goalAmount != null && draft.goalAmount !in 1..100_000) {
            errors += "Goal amount must be between 1 and 100,000."
        }

        return if (errors.isEmpty()) {
            TrackerDraftValidation.Valid(
                ValidatedTrackerDraft(
                    title = title,
                    note = note,
                    kind = draft.kind,
                    startEpochMs = draft.startEpochMs,
                    startZoneId = draft.startZoneId,
                    displayFormat = draft.displayFormat,
                    goalAmount = draft.goalAmount,
                    goalUnit = draft.goalUnit,
                )
            )
        } else {
            TrackerDraftValidation.Invalid(errors.toList())
        }
    }
}
