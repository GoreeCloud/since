package com.goreecloud.since.ui

import com.goreecloud.since.data.preferences.DashboardSortPreference
import com.goreecloud.since.domain.model.TrackerAggregate
import java.text.Normalizer
import java.util.Locale

object SinceDashboardQuery {
    private val combiningMarks = Regex("\\p{M}+")

    fun apply(
        aggregates: List<TrackerAggregate>,
        query: String,
        sort: DashboardSortPreference,
    ): List<TrackerAggregate> {
        val needle = normalize(query.trim())
        val filtered = if (needle.isEmpty()) {
            aggregates
        } else {
            aggregates.filter { aggregate ->
                normalize(aggregate.tracker.title).contains(needle) ||
                    normalize(aggregate.tracker.note.orEmpty()).contains(needle)
            }
        }

        return when (sort) {
            DashboardSortPreference.MANUAL -> filtered.sortedWith(
                compareBy<TrackerAggregate> { it.tracker.sortOrder }
                    .thenBy { it.tracker.createdAtEpochMs }
                    .thenBy { it.tracker.id },
            )

            DashboardSortPreference.TITLE -> filtered.sortedWith(
                compareBy<TrackerAggregate> { normalize(it.tracker.title) }
                    .thenBy { it.tracker.createdAtEpochMs }
                    .thenBy { it.tracker.id },
            )

            DashboardSortPreference.NEWEST_START -> filtered.sortedWith(
                compareByDescending<TrackerAggregate> { currentStartEpochMs(it) }
                    .thenBy { normalize(it.tracker.title) }
                    .thenBy { it.tracker.id },
            )

            DashboardSortPreference.OLDEST_START -> filtered.sortedWith(
                compareBy<TrackerAggregate> { currentStartEpochMs(it) }
                    .thenBy { normalize(it.tracker.title) }
                    .thenBy { it.tracker.id },
            )

            DashboardSortPreference.LONGEST_CURRENT -> filtered.sortedWith(
                compareBy<TrackerAggregate> { currentStartEpochMs(it) }
                    .thenBy { it.tracker.createdAtEpochMs }
                    .thenBy { it.tracker.id },
            )
        }
    }

    private fun currentStartEpochMs(aggregate: TrackerAggregate): Long =
        aggregate.periods.single { it.endEpochMs == null }.startEpochMs

    private fun normalize(value: String): String =
        combiningMarks
            .replace(Normalizer.normalize(value, Normalizer.Form.NFD), "")
            .lowercase(Locale.ROOT)
}
