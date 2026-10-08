package com.goreecloud.since.domain.model

data class TrackerAggregate(
    val tracker: Tracker,
    val periods: List<TrackerPeriod>,
    val goal: Goal?,
) {
    init {
        require(periods.isNotEmpty()) { "A tracker aggregate requires at least one period." }
        require(periods.all { it.trackerId == tracker.id }) {
            "Every period must belong to the aggregate tracker."
        }
        require(periods.count { it.endEpochMs == null } == 1) {
            "A tracker aggregate requires exactly one open period."
        }
        require(goal == null || goal.trackerId == tracker.id) {
            "Goal must belong to the aggregate tracker."
        }
    }
}
