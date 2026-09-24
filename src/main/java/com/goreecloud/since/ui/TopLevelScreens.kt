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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.goreecloud.since.BuildConfig
import com.goreecloud.since.R
import com.goreecloud.since.data.preferences.ThemePreference
import com.goreecloud.since.domain.model.TrackerAggregate
import com.goreecloud.since.domain.model.TrackerKind
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
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
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
            top = 24.dp,
            end = 20.dp,
            bottom = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
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
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
) {
    var plannedDialog by remember { mutableStateOf<PlannedSetting?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .testTag("settings-list"),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 24.dp,
            end = 20.dp,
            bottom = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
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
                            .padding(vertical = 8.dp),
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
            }
        }

        item {
            SettingsSection(title = stringResourceCompat(R.string.settings_data_recovery)) {
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
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
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
private fun SettingsActionRow(
    title: String,
    supporting: String,
    status: String,
    testTag: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
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
            .padding(vertical = 6.dp),
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
