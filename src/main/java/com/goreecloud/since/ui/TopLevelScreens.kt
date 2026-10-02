package com.goreecloud.since.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.goreecloud.since.BuildConfig
import com.goreecloud.since.R
import com.goreecloud.since.data.preferences.DashboardSortPreference
import com.goreecloud.since.data.preferences.ThemePreference
import com.goreecloud.since.domain.model.TrackerAggregate
import com.goreecloud.since.domain.model.TrackerKind
import com.goreecloud.since.domain.portability.SinceImportReplacementPlanner
import com.goreecloud.since.domain.portability.SinceImportReviewResult
import java.time.Clock
import com.goreecloud.since.domain.model.DisplayFormat
import com.goreecloud.since.domain.time.ElapsedResult
import com.goreecloud.since.domain.time.TimeEngine

internal enum class TopLevelDestination {
    HOME,
    ACHIEVEMENTS,
    SETTINGS,
}

@Composable
internal fun SinceTopLevelNavigationBar(
    selected: TopLevelDestination,
    onSelect: (TopLevelDestination) -> Unit,
) {
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.onSurface,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 6.dp,
    ) {
        NavigationBarItem(
            selected = selected == TopLevelDestination.HOME,
            onClick = { onSelect(TopLevelDestination.HOME) },
            icon = {
                Text(
                    modifier = Modifier.clearAndSetSemantics {},
                    text = "⌂",
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            label = { Text(stringResourceCompat(R.string.nav_home)) },
            colors = itemColors,
            modifier = Modifier.testTag("nav-home"),
        )
        NavigationBarItem(
            selected = selected == TopLevelDestination.ACHIEVEMENTS,
            onClick = { onSelect(TopLevelDestination.ACHIEVEMENTS) },
            icon = {
                Text(
                    modifier = Modifier.clearAndSetSemantics {},
                    text = "★",
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            label = { Text(stringResourceCompat(R.string.nav_achievements)) },
            colors = itemColors,
            modifier = Modifier.testTag("nav-achievements"),
        )
        NavigationBarItem(
            selected = selected == TopLevelDestination.SETTINGS,
            onClick = { onSelect(TopLevelDestination.SETTINGS) },
            icon = {
                Text(
                    modifier = Modifier.clearAndSetSemantics {},
                    text = "⚙",
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            label = { Text(stringResourceCompat(R.string.nav_settings)) },
            colors = itemColors,
            modifier = Modifier.testTag("nav-settings"),
        )
    }
}

@Composable
internal fun AchievementsScreen(
    innerPadding: PaddingValues,
    aggregates: List<TrackerAggregate>,
    clock: Clock,
) {
    val streaks = aggregates.filter { it.tracker.kind == TrackerKind.STREAK }
    val timeEngine = remember(clock) { TimeEngine(clock) }
    val maxOpenStreakDays = streaks.maxOfOrNull { aggregate ->
        val openPeriod = aggregate.periods.single { it.endEpochMs == null }
        when (
            val elapsed = timeEngine.elapsedSince(
                startEpochMs = openPeriod.startEpochMs,
                zoneId = openPeriod.startZoneId,
                format = DisplayFormat.DAYS,
            )
        ) {
            ElapsedResult.ClockInconsistency -> 0L
            is ElapsedResult.Value -> elapsed.breakdown.days
        }
    } ?: 0L

    val achievements = listOf(
        AchievementUi(
            title = stringResourceCompat(R.string.achievement_first_tracker),
            description = stringResourceCompat(R.string.achievement_first_tracker_description),
            unlocked = aggregates.isNotEmpty(),
        ),
        AchievementUi(
            title = stringResourceCompat(R.string.achievement_first_streak),
            description = stringResourceCompat(R.string.achievement_first_streak_description),
            unlocked = streaks.isNotEmpty(),
        ),
        AchievementUi(
            title = stringResourceCompat(R.string.achievement_goal_setter),
            description = stringResourceCompat(R.string.achievement_goal_setter_description),
            unlocked = aggregates.any { it.goal != null },
        ),
        AchievementUi(
            title = stringResourceCompat(R.string.achievement_seven_days),
            description = stringResourceCompat(R.string.achievement_seven_days_description),
            unlocked = maxOpenStreakDays >= 7L,
        ),
        AchievementUi(
            title = stringResourceCompat(R.string.achievement_thirty_days),
            description = stringResourceCompat(R.string.achievement_thirty_days_description),
            unlocked = maxOpenStreakDays >= 30L,
        ),
    )
    val unlockedCount = achievements.count { it.unlocked }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 20.dp,
            end = 20.dp,
            bottom = 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    modifier = Modifier
                        .testTag("achievements-screen")
                        .semantics { heading() },
                    text = stringResourceCompat(R.string.achievements_title),
                    style = MaterialTheme.typography.displaySmall,
                )
                Text(
                    text = stringResourceCompat(
                        R.string.achievements_summary,
                        unlockedCount,
                        achievements.size,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResourceCompat(R.string.achievements_privacy_note),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        items(achievements) { achievement ->
            AchievementCard(achievement)
        }
    }
}

private data class AchievementUi(
    val title: String,
    val description: String,
    val unlocked: Boolean,
)

@Composable
private fun AchievementCard(
    achievement: AchievementUi,
) {
    val achievementState = stringResourceCompat(
        if (achievement.unlocked) R.string.achievement_unlocked else R.string.achievement_locked,
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .semantics(mergeDescendants = true) {
                    stateDescription = achievementState
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (achievement.unlocked) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                contentColor = if (achievement.unlocked) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            ) {
                Text(
                    modifier = Modifier
                        .clearAndSetSemantics {}
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    text = if (achievement.unlocked) "✓" else "○",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = achievement.title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = achievement.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
internal fun SettingsScreen(
    innerPadding: PaddingValues,
    themePreference: ThemePreference,
    onThemePreferenceChange: (ThemePreference) -> Unit,
    defaultDisplayFormat: DisplayFormat,
    onDefaultDisplayFormatChange: (DisplayFormat) -> Unit,
    showSeconds: Boolean,
    onShowSecondsChange: (Boolean) -> Unit,
    dashboardSort: DashboardSortPreference,
    onDashboardSortChange: (DashboardSortPreference) -> Unit,
    confirmReset: Boolean,
    onConfirmResetChange: (Boolean) -> Unit,
    archivedTrackers: List<TrackerAggregate>,
    restoringTrackerId: String?,
    restoreFailedTrackerId: String?,
    deletingTrackerId: String?,
    deleteFailedTrackerId: String?,
    onRestoreTracker: (String) -> Unit,
    onDeleteArchivedTracker: (String) -> Unit,
    isExportingData: Boolean,
    exportStatus: SinceExportStatus?,
    onExportData: () -> Unit,
    isReviewingImport: Boolean,
    importReviewResult: SinceImportReviewResult?,
    currentTrackerIds: Set<String>,
    onReviewImport: () -> Unit,
    contextualHintsEnabled: Boolean,
    onContextualHintsEnabledChange: (Boolean) -> Unit,
    onResetDismissedContextualHints: () -> Unit,
    onReplaySetup: () -> Unit,
) {
    var plannedDialog by remember { mutableStateOf<PlannedSetting?>(null) }
    var pendingDeleteTrackerId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .testTag("settings-list"),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 20.dp,
            end = 20.dp,
            bottom = 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    modifier = Modifier
                        .testTag("settings-screen")
                        .semantics { heading() },
                    text = stringResourceCompat(R.string.settings_title),
                    style = MaterialTheme.typography.displaySmall,
                )
                Text(
                    text = stringResourceCompat(R.string.settings_summary),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        item {
            SettingsSection(title = stringResourceCompat(R.string.settings_general)) {
                Text(
                    text = stringResourceCompat(R.string.settings_dashboard_sort),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResourceCompat(R.string.settings_dashboard_sort_supporting),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                DashboardSortControls(
                    selected = dashboardSort,
                    onSelect = onDashboardSortChange,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings-confirm-reset")
                        .toggleable(
                            value = confirmReset,
                            role = Role.Switch,
                            onValueChange = onConfirmResetChange,
                        )
                        .semantics(mergeDescendants = true) {}
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = stringResourceCompat(R.string.settings_confirm_reset),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResourceCompat(R.string.settings_confirm_reset_supporting),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Switch(
                        modifier = Modifier.clearAndSetSemantics {},
                        checked = confirmReset,
                        onCheckedChange = null,
                    )
                }
            }
        }

        item {
            SettingsSection(title = stringResourceCompat(R.string.settings_appearance)) {
                Text(
                    text = stringResourceCompat(R.string.settings_theme),
                    style = MaterialTheme.typography.titleMedium,
                )
                ThemePreference.entries.forEach { preference ->
                    val label = when (preference) {
                        ThemePreference.SYSTEM -> stringResourceCompat(R.string.theme_system)
                        ThemePreference.LIGHT -> stringResourceCompat(R.string.theme_light)
                        ThemePreference.DARK -> stringResourceCompat(R.string.theme_dark)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("theme-${preference.name.lowercase()}")
                            .selectable(
                                selected = themePreference == preference,
                                role = Role.RadioButton,
                                onClick = { onThemePreferenceChange(preference) },
                            )
                            .semantics(mergeDescendants = true) {}
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        RadioButton(
                            modifier = Modifier.clearAndSetSemantics {},
                            selected = themePreference == preference,
                            onClick = null,
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                FormatSelector(
                    title = stringResourceCompat(R.string.settings_default_display_format),
                    supporting = stringResourceCompat(
                        R.string.settings_default_display_format_supporting,
                    ),
                    selected = defaultDisplayFormat,
                    enabled = true,
                    onSelect = onDefaultDisplayFormatChange,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings-show-seconds")
                        .toggleable(
                            value = showSeconds,
                            role = Role.Switch,
                            onValueChange = onShowSecondsChange,
                        )
                        .semantics(mergeDescendants = true) {}
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = stringResourceCompat(R.string.settings_show_seconds),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResourceCompat(
                                R.string.settings_show_seconds_supporting,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Switch(
                        modifier = Modifier.clearAndSetSemantics {},
                        checked = showSeconds,
                        onCheckedChange = null,
                    )
                }
            }
        }

        item {
            SettingsSection(title = stringResourceCompat(R.string.settings_data_recovery)) {
                SettingsActionRow(
                    title = stringResourceCompat(R.string.settings_export_data),
                    supporting = stringResourceCompat(R.string.settings_export_data_supporting),
                    status = stringResourceCompat(
                        if (isExportingData) R.string.settings_exporting else R.string.settings_export,
                    ),
                    testTag = "settings-export-data",
                    enabled = !isExportingData,
                    onClick = onExportData,
                )
                exportStatus?.let { status ->
                    Text(
                        modifier = Modifier
                            .testTag("settings-export-status")
                            .semantics { liveRegion = LiveRegionMode.Polite },
                        text = stringResourceCompat(
                            when (status) {
                                SinceExportStatus.SUCCESS -> R.string.settings_export_success
                                SinceExportStatus.FAILURE -> R.string.settings_export_failed
                            },
                        ),
                        color = if (status == SinceExportStatus.FAILURE) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                SettingsActionRow(
                    title = stringResourceCompat(R.string.settings_review_import),
                    supporting = stringResourceCompat(R.string.settings_review_import_supporting),
                    status = stringResourceCompat(
                        if (isReviewingImport) {
                            R.string.settings_reviewing_import
                        } else {
                            R.string.settings_choose_file
                        },
                    ),
                    testTag = "settings-review-import",
                    enabled = !isReviewingImport && !isExportingData,
                    onClick = onReviewImport,
                )
                importReviewResult?.let { result ->
                    val valid = result as? SinceImportReviewResult.Valid
                    Text(
                        modifier = Modifier
                            .testTag("settings-import-review-status")
                            .semantics { liveRegion = LiveRegionMode.Polite },
                        text = if (valid == null) {
                            stringResourceCompat(R.string.settings_import_review_invalid)
                        } else {
                            stringResourceCompat(
                                R.string.settings_import_review_valid,
                                valid.summary.trackerCount,
                                valid.summary.archivedTrackerCount,
                                valid.summary.periodCount,
                                valid.summary.goalCount,
                            )
                        },
                        color = if (valid == null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (valid != null) {
                        val plan = SinceImportReplacementPlanner.plan(
                            summary = valid.summary,
                            currentTrackerIds = currentTrackerIds,
                        )
                        Text(
                            modifier = Modifier.testTag("settings-import-replacement-plan"),
                            text = stringResourceCompat(
                                R.string.settings_import_replacement_plan,
                                plan.currentTrackerCount,
                                plan.importedTrackerCount,
                                plan.matchingTrackerCount,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                SettingsActionRow(
                    title = stringResourceCompat(R.string.settings_backup),
                    supporting = stringResourceCompat(R.string.settings_backup_supporting),
                    status = stringResourceCompat(R.string.settings_planned),
                    testTag = "settings-backup",
                    onClick = {
                        plannedDialog = PlannedSetting.BACKUP
                    },
                )
                SettingsActionRow(
                    title = stringResourceCompat(R.string.settings_restore),
                    supporting = stringResourceCompat(R.string.settings_restore_supporting),
                    status = stringResourceCompat(R.string.settings_planned),
                    testTag = "settings-restore",
                    onClick = {
                        plannedDialog = PlannedSetting.RESTORE
                    },
                )
            }
        }

        item {
            SettingsSection(title = stringResourceCompat(R.string.archived_trackers)) {
                if (archivedTrackers.isEmpty()) {
                    Text(
                        modifier = Modifier.testTag("archived-trackers-empty"),
                        text = stringResourceCompat(R.string.archived_trackers_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    archivedTrackers.forEach { aggregate ->
                        ArchivedTrackerRow(
                            aggregate = aggregate,
                            restoring = restoringTrackerId == aggregate.tracker.id,
                            deleting = deletingTrackerId == aggregate.tracker.id,
                            actionsEnabled = restoringTrackerId == null && deletingTrackerId == null,
                            restoreFailed = restoreFailedTrackerId == aggregate.tracker.id,
                            deleteFailed = deleteFailedTrackerId == aggregate.tracker.id,
                            onRestore = { onRestoreTracker(aggregate.tracker.id) },
                            onDelete = { pendingDeleteTrackerId = aggregate.tracker.id },
                        )
                    }
                }
            }
        }

        item {
            SettingsSection(title = stringResourceCompat(R.string.settings_privacy_security)) {
                SettingsInfoRow(
                    title = stringResourceCompat(R.string.settings_privacy),
                    supporting = stringResourceCompat(R.string.settings_privacy_supporting),
                )
                SettingsInfoRow(
                    title = stringResourceCompat(R.string.settings_security),
                    supporting = stringResourceCompat(R.string.settings_security_supporting),
                )
            }
        }

        item {
            SettingsSection(title = stringResourceCompat(R.string.settings_about)) {
                SettingsInfoRow(
                    title = stringResourceCompat(R.string.settings_app_version),
                    supporting = "${BuildConfig.VERSION_NAME} · ${BuildConfig.VERSION_CODE}",
                )
                SettingsInfoRow(
                    title = stringResourceCompat(R.string.settings_build_status),
                    supporting = stringResourceCompat(R.string.settings_development_build),
                )
            }
        }

        item {
            SettingsSection(title = stringResourceCompat(R.string.settings_guidance)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings-contextual-hints")
                        .toggleable(
                            value = contextualHintsEnabled,
                            role = Role.Switch,
                            onValueChange = onContextualHintsEnabledChange,
                        )
                        .semantics(mergeDescendants = true) {}
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = stringResourceCompat(R.string.contextual_hints),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResourceCompat(R.string.contextual_hints_supporting),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Switch(
                        modifier = Modifier.clearAndSetSemantics {},
                        checked = contextualHintsEnabled,
                        onCheckedChange = null,
                    )
                }
                SettingsActionRow(
                    title = stringResourceCompat(R.string.reset_dismissed_hints),
                    supporting = stringResourceCompat(R.string.reset_dismissed_hints_supporting),
                    status = stringResourceCompat(R.string.settings_reset),
                    testTag = "settings-reset-dismissed-hints",
                    onClick = onResetDismissedContextualHints,
                )
                SettingsActionRow(
                    title = stringResourceCompat(R.string.replay_setup),
                    supporting = stringResourceCompat(R.string.replay_setup_supporting),
                    status = stringResourceCompat(R.string.settings_open),
                    testTag = "settings-replay-setup",
                    onClick = onReplaySetup,
                )
            }
        }
    }

    plannedDialog?.let { setting ->
        val title = when (setting) {
            PlannedSetting.BACKUP -> stringResourceCompat(R.string.settings_backup)
            PlannedSetting.RESTORE -> stringResourceCompat(R.string.settings_restore)
        }
        val message = when (setting) {
            PlannedSetting.BACKUP -> stringResourceCompat(R.string.settings_backup_not_ready)
            PlannedSetting.RESTORE -> stringResourceCompat(R.string.settings_restore_not_ready)
        }
        AlertDialog(
            onDismissRequest = { plannedDialog = null },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { plannedDialog = null }) {
                    Text(stringResourceCompat(R.string.done))
                }
            },
        )
    }

    pendingDeleteTrackerId?.let { trackerId ->
        AlertDialog(
            onDismissRequest = {
                if (deletingTrackerId == null) pendingDeleteTrackerId = null
            },
            title = { Text(stringResourceCompat(R.string.delete_archived_tracker_title)) },
            text = { Text(stringResourceCompat(R.string.delete_archived_tracker_message)) },
            confirmButton = {
                TextButton(
                    modifier = Modifier.testTag("confirm-delete-archived-tracker"),
                    enabled = deletingTrackerId == null,
                    onClick = {
                        pendingDeleteTrackerId = null
                        onDeleteArchivedTracker(trackerId)
                    },
                ) {
                    Text(stringResourceCompat(R.string.delete_permanently))
                }
            },
            dismissButton = {
                TextButton(
                    enabled = deletingTrackerId == null,
                    onClick = { pendingDeleteTrackerId = null },
                ) {
                    Text(stringResourceCompat(R.string.cancel))
                }
            },
        )
    }
}

internal enum class SinceExportStatus {
    SUCCESS,
    FAILURE,
}

private enum class PlannedSetting {
    BACKUP,
    RESTORE,
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
            content()
        }
    }
}

@Composable
private fun ArchivedTrackerRow(
    aggregate: TrackerAggregate,
    restoring: Boolean,
    deleting: Boolean,
    actionsEnabled: Boolean,
    restoreFailed: Boolean,
    deleteFailed: Boolean,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("archived-tracker-" + aggregate.tracker.id),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = aggregate.tracker.title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResourceCompat(R.string.archived_tracker_supporting),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    modifier = Modifier.testTag("delete-archived-tracker-" + aggregate.tracker.id),
                    onClick = onDelete,
                    enabled = actionsEnabled,
                ) {
                    Text(
                        if (deleting) {
                            stringResourceCompat(R.string.deleting_tracker)
                        } else {
                            stringResourceCompat(R.string.delete_tracker)
                        }
                    )
                }
                TextButton(
                    modifier = Modifier.testTag("restore-tracker-" + aggregate.tracker.id),
                    onClick = onRestore,
                    enabled = actionsEnabled,
                ) {
                    Text(
                        if (restoring) {
                            stringResourceCompat(R.string.restoring_tracker)
                        } else {
                            stringResourceCompat(R.string.restore_tracker)
                        }
                    )
                }
            }
            if (restoreFailed) {
                Text(
                    text = stringResourceCompat(R.string.restore_tracker_failed),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (deleteFailed) {
                Text(
                    text = stringResourceCompat(R.string.delete_tracker_failed),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    supporting: String,
    status: String,
    testTag: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = supporting,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = status,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun SettingsInfoRow(
    title: String,
    supporting: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = supporting,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun stringResourceCompat(
    id: Int,
    vararg formatArgs: Any,
): String = androidx.compose.ui.res.stringResource(id, *formatArgs)
