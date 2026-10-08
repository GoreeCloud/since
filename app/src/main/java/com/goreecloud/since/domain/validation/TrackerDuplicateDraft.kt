package com.goreecloud.since.domain.validation

import com.goreecloud.since.domain.model.TrackerAggregate

/**
 * Creates a new-tracker draft from the currently active configuration only.
 *
 * Closed periods and reset metadata are intentionally excluded so duplication never clones history.
 */
object TrackerDuplicateDraft {
    fun from(aggregate: TrackerAggregate): TrackerDraft {
        val currentPeriod = aggregate.periods.single { it.endEpochMs == null }
        return TrackerDraft(
            title = aggregate.tracker.title,
            note = aggregate.tracker.note,
            kind = aggregate.tracker.kind,
            startEpochMs = currentPeriod.startEpochMs,
            startZoneId = currentPeriod.startZoneId,
            displayFormat = aggregate.tracker.defaultDisplayFormat,
            goalAmount = aggregate.goal?.targetAmount,
            goalUnit = aggregate.goal?.targetUnit,
        )
    }
}
