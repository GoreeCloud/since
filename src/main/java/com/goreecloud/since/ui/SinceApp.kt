package com.goreecloud.since.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goreecloud.since.R
import com.goreecloud.since.domain.model.DisplayFormat
import com.goreecloud.since.domain.model.TrackerAggregate
import com.goreecloud.since.domain.model.TrackerKind
import com.goreecloud.since.domain.repository.TrackerRepository
import com.goreecloud.since.domain.time.ElapsedResult
import com.goreecloud.since.domain.time.GoalEstimateResult
import com.goreecloud.since.domain.time.GoalEstimator
import com.goreecloud.since.domain.time.TimeEngine
import com.goreecloud.since.domain.time.TrackerStartInput
import com.goreecloud.since.domain.time.TrackerStartResolution
import com.goreecloud.since.domain.validation.TrackerDraft
import com.goreecloud.since.domain.validation.TrackerDraftValidation
import com.goreecloud.since.domain.validation.TrackerDraftValidator
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

@Composable
fun SinceApp(
    repository: TrackerRepository,
    clock: Clock,
) {
    val aggregates by repository
        .observeActiveTrackerAggregates()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    val validator = remember(clock) { TrackerDraftValidator(clock) }

    var showTypeChooser by rememberSaveable { mutableStateOf(false) }
    var editorKindName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedTrackerId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingTrackerId by rememberSaveable { mutableStateOf<String?>(null) }
    var validationErrors by remember { mutableStateOf(emptyList<String>()) }
    var saveFailed by rememberSaveable { mutableStateOf(false) }
    var detailUpdateFailed by rememberSaveable { mutableStateOf(false) }
    var goalUpdateFailed by rememberSaveable { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isGoalSaving by remember { mutableStateOf(false) }
    val historyConflictMessage = stringResource(R.string.edit_history_conflict)

    val editorKind = editorKindName?.let { runCatching { TrackerKind.valueOf(it) }.getOrNull() }
    if (editorKind != null) {
        CreateTrackerScreen(
            kind = editorKind,
            clock = clock,
            validationErrors = validationErrors,
            saveFailed = saveFailed,
            isSaving = isSaving,
            onCancel = {
                validationErrors = emptyList()
                saveFailed = false
                editorKindName = null
            },
            onSave = { draft ->
                when (val validation = validator.validate(draft)) {
                    is TrackerDraftValidation.Invalid -> {
                        validationErrors = validation.errors
                        saveFailed = false
                    }

                    is TrackerDraftValidation.Valid -> {
                        validationErrors = emptyList()
                        saveFailed = false
                        isSaving = true
                        scope.launch {
                            runCatching {
                                repository.createTracker(validation.draft)
                            }.onSuccess { created ->
                                selectedTrackerId = created.tracker.id
                                detailUpdateFailed = false
                                editorKindName = null
                            }.onFailure {
                                saveFailed = true
                            }
                            isSaving = false
                        }
                    }
                }
            },
        )
        return
    }

    val selectedAggregate = selectedTrackerId?.let { trackerId ->
        aggregates.firstOrNull { it.tracker.id == trackerId }
    }
    val editingAggregate = editingTrackerId?.let { trackerId ->
        aggregates.firstOrNull { it.tracker.id == trackerId }
    }

    if (editingAggregate != null) {
        EditTrackerScreen(
            aggregate = editingAggregate,
            clock = clock,
            validationErrors = validationErrors,
            saveFailed = saveFailed,
            isSaving = isSaving,
            onCancel = {
                validationErrors = emptyList()
                saveFailed = false
                editingTrackerId = null
            },
            onSave = { draft ->
                when (val validation = validator.validate(draft)) {
                    is TrackerDraftValidation.Invalid -> {
                        validationErrors = validation.errors
                        saveFailed = false
                    }

                    is TrackerDraftValidation.Valid -> {
                        val latestClosedEnd = editingAggregate.periods
                            .mapNotNull { it.endEpochMs }
                            .maxOrNull()
                        if (
                            latestClosedEnd != null &&
                            validation.draft.startEpochMs < latestClosedEnd
                        ) {
                            validationErrors = listOf(historyConflictMessage)
                            saveFailed = false
                        } else {
                            validationErrors = emptyList()
                            saveFailed = false
                            isSaving = true
                            scope.launch {
                                runCatching {
                                    repository.updateTracker(
                                        trackerId = editingAggregate.tracker.id,
                                        draft = validation.draft,
                                    )
                                }.onSuccess { updated ->
                                    if (updated == null) {
                                        saveFailed = true
                                    } else {
                                        detailUpdateFailed = false
                                        editingTrackerId = null
                                    }
                                }.onFailure {
                                    saveFailed = true
                                }
                                isSaving = false
                            }
                        }
                    }
                }
            },
        )
        return
    }

    if (selectedAggregate != null) {
        TrackerDetailsScreen(
            aggregate = selectedAggregate,
            clock = clock,
            updateFailed = detailUpdateFailed,
            goalUpdateFailed = goalUpdateFailed,
            isGoalSaving = isGoalSaving,
            onBack = {
                selectedTrackerId = null
                editingTrackerId = null
                detailUpdateFailed = false
                goalUpdateFailed = false
            },
            onEdit = {
                validationErrors = emptyList()
                saveFailed = false
                editingTrackerId = selectedAggregate.tracker.id
            },
            onDisplayFormatChange = { format ->
                if (format != selectedAggregate.tracker.defaultDisplayFormat) {
                    detailUpdateFailed = false
                    scope.launch {
                        val updated = runCatching {
                            repository.updateDisplayFormat(
                                trackerId = selectedAggregate.tracker.id,
                                displayFormat = format,
                            )
                        }.getOrDefault(false)
                        if (!updated) detailUpdateFailed = true
                    }
                }
            },
            onUpdateGoal = { amount, unit ->
                goalUpdateFailed = false
                isGoalSaving = true
                scope.launch {
                    val updated = runCatching {
                        repository.updateGoal(
                            trackerId = selectedAggregate.tracker.id,
                            targetAmount = amount,
                            targetUnit = unit,
                        )
                    }.getOrNull()
                    goalUpdateFailed = updated == null
                    isGoalSaving = false
                }
            },
            onRemoveGoal = {
                goalUpdateFailed = false
                isGoalSaving = true
                scope.launch {
                    val removed = runCatching {
                        repository.removeGoal(selectedAggregate.tracker.id)
                    }.getOrDefault(false)
                    goalUpdateFailed = !removed
                    isGoalSaving = false
                }
            },
        )
        return
    }

    val onAddTracker = {
        selectedTrackerId = null
        showTypeChooser = true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (aggregates.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onAddTracker,
                    shape = MaterialTheme.shapes.large,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    content = { Text(stringResource(R.string.add_tracker)) },
                )
            }
        },
    ) { innerPadding ->
        Dashboard(
            innerPadding = innerPadding,
            aggregates = aggregates,
            clock = clock,
            onAddTracker = onAddTracker,
            onOpenTracker = { trackerId ->
                detailUpdateFailed = false
                goalUpdateFailed = false
                selectedTrackerId = trackerId
            },
        )
    }

    if (showTypeChooser) {
        TrackerTypeChooser(
            onDismiss = { showTypeChooser = false },
            onChoose = { kind ->
                showTypeChooser = false
                validationErrors = emptyList()
                saveFailed = false
                editorKindName = kind.name
            },
        )
    }
}

@Composable
private fun Dashboard(
    innerPadding: PaddingValues,
    aggregates: List<TrackerAggregate>,
    clock: Clock,
    onAddTracker: () -> Unit,
    onOpenTracker: (String) -> Unit,
) {
    val dashboardTick by rememberMinuteTick(
        clock = clock,
        key = "dashboard",
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 24.dp,
            end = 20.dp,
            bottom = if (aggregates.isEmpty()) 32.dp else 112.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    modifier = Modifier.semantics { heading() },
                    text = stringResource(R.string.dashboard_title),
                    style = MaterialTheme.typography.displaySmall,
                )
                if (aggregates.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.dashboard_empty_message),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }

        if (aggregates.isEmpty()) {
            item {
                DashboardEmptyState(onAddTracker = onAddTracker)
            }
        } else {
            items(
                items = aggregates,
                key = { it.tracker.id },
            ) { aggregate ->
                TrackerCard(
                    aggregate = aggregate,
                    clock = clock,
                    tick = dashboardTick,
                    onClick = { onOpenTracker(aggregate.tracker.id) },
                )
            }
        }
    }
}

@Composable
private fun DashboardEmptyState(
    onAddTracker: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.padding(4.dp),
                )
            }
            Text(
                text = stringResource(R.string.dashboard_empty_status),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.dashboard_empty_message),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onAddTracker,
                shape = MaterialTheme.shapes.large,
            ) {
                Text(stringResource(R.string.add_tracker))
            }
        }
    }
}

@Composable
private fun TrackerCard(
    aggregate: TrackerAggregate,
    clock: Clock,
    tick: Long,
    onClick: () -> Unit,
) {
    val currentPeriod = aggregate.periods.single { it.endEpochMs == null }
    val elapsed = remember(aggregate, tick, clock) {
        TimeEngine(clock).elapsedSince(
            startEpochMs = currentPeriod.startEpochMs,
            zoneId = currentPeriod.startZoneId,
            format = aggregate.tracker.defaultDisplayFormat,
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = aggregate.tracker.title,
                    style = MaterialTheme.typography.titleLarge,
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        text = trackerKindLabel(aggregate.tracker.kind),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            Text(
                text = stringResource(R.string.elapsed_label),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = elapsedSummary(elapsed),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.headlineSmall,
            )

            aggregate.goal?.let { goal ->
                val estimate = remember(aggregate, tick, clock) {
                    GoalEstimator(clock).estimate(
                        startEpochMs = currentPeriod.startEpochMs,
                        zoneId = currentPeriod.startZoneId,
                        targetAmount = goal.targetAmount,
                        targetUnit = goal.targetUnit,
                    )
                }
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(
                                R.string.goal_summary,
                                goal.targetAmount,
                                displayFormatLabel(goal.targetUnit),
                            ),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            style = MaterialTheme.typography.labelLarge,
                        )
                        when (estimate) {
                            GoalEstimateResult.ClockInconsistency -> Text(
                                text = stringResource(R.string.clock_inconsistency),
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                style = MaterialTheme.typography.bodySmall,
                            )

                            is GoalEstimateResult.Value -> Text(
                                text = if (estimate.estimate.isComplete) {
                                    stringResource(
                                        R.string.goal_progress_complete,
                                        estimate.estimate.percent,
                                    )
                                } else {
                                    stringResource(
                                        R.string.goal_progress_percent,
                                        estimate.estimate.percent,
                                    )
                                },
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackerDetailsScreen(
    aggregate: TrackerAggregate,
    clock: Clock,
    updateFailed: Boolean,
    goalUpdateFailed: Boolean,
    isGoalSaving: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDisplayFormatChange: (DisplayFormat) -> Unit,
    onUpdateGoal: (Int, DisplayFormat) -> Unit,
    onRemoveGoal: () -> Unit,
) {
    val currentPeriod = aggregate.periods.single { it.endEpochMs == null }
    val tick by rememberMinuteTick(
        clock = clock,
        key = "details-" + aggregate.tracker.id,
    )
    val elapsed = remember(aggregate, tick, clock) {
        TimeEngine(clock).elapsedSince(
            startEpochMs = currentPeriod.startEpochMs,
            zoneId = currentPeriod.startZoneId,
            format = aggregate.tracker.defaultDisplayFormat,
        )
    }
    val startedOn = remember(currentPeriod.startEpochMs, currentPeriod.startZoneId) {
        val zone = ZoneId.of(currentPeriod.startZoneId)
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            .format(Instant.ofEpochMilli(currentPeriod.startEpochMs).atZone(zone))
    }

    var showGoalEditor by rememberSaveable(aggregate.tracker.id) { mutableStateOf(false) }
    val goalEstimate = aggregate.goal?.let { goal ->
        remember(aggregate, tick, clock) {
            GoalEstimator(clock).estimate(
                startEpochMs = currentPeriod.startEpochMs,
                zoneId = currentPeriod.startZoneId,
                targetAmount = goal.targetAmount,
                targetUnit = goal.targetUnit,
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.back))
                }
                TextButton(onClick = onEdit) {
                    Text(stringResource(R.string.edit_tracker))
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            text = trackerKindLabel(aggregate.tracker.kind),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    Text(
                        modifier = Modifier.semantics { heading() },
                        text = aggregate.tracker.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = stringResource(R.string.elapsed_label),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    SelectionContainer {
                        Text(
                            text = elapsedSummary(elapsed),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.displaySmall,
                        )
                    }
                    Text(
                        text = stringResource(R.string.started_on_label),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = startedOn,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = currentPeriod.startZoneId,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            SectionCard {
                FormatSelector(
                    title = stringResource(R.string.display_format_label),
                    selected = aggregate.tracker.defaultDisplayFormat,
                    enabled = true,
                    onSelect = onDisplayFormatChange,
                )

                if (updateFailed) {
                    Text(
                        modifier = Modifier.semantics {
                            liveRegion = LiveRegionMode.Assertive
                        },
                        text = stringResource(R.string.display_format_update_failed),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            if (aggregate.tracker.kind == TrackerKind.STREAK) {
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.goal_label),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        TextButton(
                            onClick = { showGoalEditor = true },
                            enabled = !isGoalSaving,
                        ) {
                            Text(
                                if (aggregate.goal == null) {
                                    stringResource(R.string.add_goal)
                                } else {
                                    stringResource(R.string.edit_goal)
                                }
                            )
                        }
                    }

                    val goal = aggregate.goal
                    if (goal == null) {
                        Text(
                            text = stringResource(R.string.no_goal),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    } else {
                        Text(
                            text = stringResource(
                                R.string.goal_summary,
                                goal.targetAmount,
                                displayFormatLabel(goal.targetUnit),
                            ),
                            color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        when (val estimate = goalEstimate) {
                            GoalEstimateResult.ClockInconsistency -> Text(
                                text = stringResource(R.string.clock_inconsistency),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                            )

                            is GoalEstimateResult.Value -> {
                                Text(
                                    text = stringResource(R.string.goal_progress_label),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                                LinearProgressIndicator(
                                    progress = {
                                        estimate.estimate.progressFraction
                                            .toFloat()
                                            .coerceIn(0f, 1f)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Text(
                                    text = if (estimate.estimate.isComplete) {
                                        stringResource(
                                            R.string.goal_progress_complete,
                                            estimate.estimate.percent,
                                        )
                                    } else {
                                        stringResource(
                                            R.string.goal_progress_percent,
                                            estimate.estimate.percent,
                                        )
                                    },
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                val targetZone = ZoneId.of(currentPeriod.startZoneId)
                                val targetText = DateTimeFormatter
                                    .ofLocalizedDateTime(FormatStyle.MEDIUM)
                                    .format(
                                        Instant
                                            .ofEpochMilli(estimate.estimate.targetEpochMs)
                                            .atZone(targetZone)
                                    )
                                Text(
                                    text = stringResource(
                                        R.string.goal_estimated_completion,
                                        targetText,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }

                            null -> Unit
                        }
                    }

                    if (goalUpdateFailed) {
                        Text(
                            modifier = Modifier.semantics {
                                liveRegion = LiveRegionMode.Assertive
                            },
                            text = stringResource(R.string.goal_save_failed),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            SectionCard {
                Text(
                    text = stringResource(R.string.note_label),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = aggregate.tracker.note ?: stringResource(R.string.no_note),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }

    if (showGoalEditor && aggregate.tracker.kind == TrackerKind.STREAK) {
        GoalEditorDialog(
            currentGoal = aggregate.goal,
            currentPeriod = currentPeriod,
            clock = clock,
            isSaving = isGoalSaving,
            onDismiss = { showGoalEditor = false },
            onSave = { amount, unit ->
                onUpdateGoal(amount, unit)
                showGoalEditor = false
            },
            onRemove = if (aggregate.goal == null) {
                null
            } else {
                {
                    onRemoveGoal()
                    showGoalEditor = false
                }
            },
        )
    }
}

@Composable
private fun GoalEditorDialog(
    currentGoal: com.goreecloud.since.domain.model.Goal?,
    currentPeriod: com.goreecloud.since.domain.model.TrackerPeriod,
    clock: Clock,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (Int, DisplayFormat) -> Unit,
    onRemove: (() -> Unit)?,
) {
    var amountText by rememberSaveable(currentPeriod.trackerId) {
        mutableStateOf(currentGoal?.targetAmount?.toString().orEmpty())
    }
    var unitName by rememberSaveable(currentPeriod.trackerId) {
        mutableStateOf(currentGoal?.targetUnit?.name ?: DisplayFormat.DAYS.name)
    }
    var amountError by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }
    val unit = DisplayFormat.valueOf(unitName)
    val amount = amountText.toIntOrNull()
    val preview = if (amount != null && amount in 1..100_000) {
        remember(currentPeriod, amount, unit, clock) {
            GoalEstimator(clock).estimate(
                startEpochMs = currentPeriod.startEpochMs,
                zoneId = currentPeriod.startZoneId,
                targetAmount = amount,
                targetUnit = unit,
            )
        }
    } else {
        null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                modifier = Modifier.semantics { heading() },
                text = stringResource(R.string.goal_editor_title),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goal-amount-field"),
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter(Char::isDigit)
                        amountError = false
                    },
                    label = { Text(stringResource(R.string.goal_amount_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !isSaving,
                    singleLine = true,
                    isError = amountError,
                )
                FormatSelector(
                    title = stringResource(R.string.goal_unit_label),
                    selected = unit,
                    enabled = !isSaving,
                    onSelect = { unitName = it.name },
                )
                if (amountError) {
                    Text(
                        modifier = Modifier.semantics {
                            liveRegion = LiveRegionMode.Assertive
                        },
                        text = stringResource(R.string.goal_amount_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (preview is GoalEstimateResult.Value) {
                    val zone = ZoneId.of(currentPeriod.startZoneId)
                    val targetText = DateTimeFormatter
                        .ofLocalizedDateTime(FormatStyle.MEDIUM)
                        .format(
                            Instant
                                .ofEpochMilli(preview.estimate.targetEpochMs)
                                .atZone(zone)
                        )
                    Text(
                        text = stringResource(
                            R.string.goal_estimated_completion,
                            targetText,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (onRemove != null) {
                    TextButton(
                        onClick = { confirmRemove = true },
                        enabled = !isSaving,
                    ) {
                        Text(stringResource(R.string.remove_goal))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsed = amountText.toIntOrNull()
                    if (parsed == null || parsed !in 1..100_000) {
                        amountError = true
                    } else {
                        onSave(parsed, unit)
                    }
                },
                enabled = !isSaving,
            ) {
                Text(
                    if (isSaving) {
                        stringResource(R.string.saving)
                    } else {
                        stringResource(R.string.save)
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving,
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
    )

    if (confirmRemove && onRemove != null) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text(stringResource(R.string.remove_goal_title)) },
            text = { Text(stringResource(R.string.remove_goal_message)) },
            confirmButton = {
                TextButton(
                    onClick = onRemove,
                    enabled = !isSaving,
                ) {
                    Text(stringResource(R.string.remove))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmRemove = false },
                    enabled = !isSaving,
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun SectionCard(
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun rememberMinuteTick(
    clock: Clock,
    key: String,
) = remember(clock, key) {
    flow {
        while (true) {
            val now = clock.millis()
            emit(now)
            val untilNextMinute = 60_000L - (now % 60_000L)
            delay(untilNextMinute.coerceIn(1_000L, 60_000L))
        }
    }
}.collectAsStateWithLifecycle(initialValue = clock.millis())

@Composable
private fun elapsedSummary(
    elapsed: ElapsedResult,
): String = when (elapsed) {
    ElapsedResult.ClockInconsistency ->
        stringResource(R.string.clock_inconsistency)

    is ElapsedResult.Value -> {
        val breakdown = elapsed.breakdown
        when (breakdown.format) {
            DisplayFormat.DAYS ->
                stringResource(
                    R.string.elapsed_days_detail,
                    breakdown.days,
                    breakdown.hours,
                    breakdown.minutes,
                )

            DisplayFormat.WEEKS ->
                stringResource(
                    R.string.elapsed_weeks_detail,
                    breakdown.weeks,
                    breakdown.days,
                    breakdown.hours,
                    breakdown.minutes,
                )

            DisplayFormat.MONTHS ->
                stringResource(
                    R.string.elapsed_months_detail,
                    breakdown.months,
                    breakdown.days,
                    breakdown.hours,
                    breakdown.minutes,
                )

            DisplayFormat.YEARS ->
                stringResource(
                    R.string.elapsed_years_detail,
                    breakdown.years,
                    breakdown.months,
                    breakdown.days,
                    breakdown.hours,
                    breakdown.minutes,
                )
        }
    }
}

@Composable
private fun trackerKindLabel(
    kind: TrackerKind,
): String = when (kind) {
    TrackerKind.EVENT -> stringResource(R.string.tracker_kind_event)
    TrackerKind.STREAK -> stringResource(R.string.tracker_kind_streak)
}

@Composable
private fun TrackerTypeChooser(
    onDismiss: () -> Unit,
    onChoose: (TrackerKind) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = stringResource(R.string.choose_tracker_type),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tracker-type-event")
                        .semantics(mergeDescendants = true) {},
                    onClick = { onChoose(TrackerKind.EVENT) },
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.tracker_kind_event),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.event_description),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tracker-type-streak")
                        .semantics(mergeDescendants = true) {},
                    onClick = { onChoose(TrackerKind.STREAK) },
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.tracker_kind_streak),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.streak_description),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun CreateTrackerScreen(
    kind: TrackerKind,
    clock: Clock,
    validationErrors: List<String>,
    saveFailed: Boolean,
    isSaving: Boolean,
    onCancel: () -> Unit,
    onSave: (TrackerDraft) -> Unit,
) {
    val defaultZoneId = ZoneId.systemDefault().id
    var title by rememberSaveable(kind.name) { mutableStateOf("") }
    var note by rememberSaveable(kind.name) { mutableStateOf("") }
    var startDateTime by rememberSaveable(kind.name) {
        mutableStateOf(TrackerStartInput.format(clock.millis(), defaultZoneId))
    }
    var startZoneId by rememberSaveable(kind.name) { mutableStateOf(defaultZoneId) }
    var startInputErrors by remember { mutableStateOf(emptyList<String>()) }
    var displayFormatName by rememberSaveable(kind.name) {
        mutableStateOf(DisplayFormat.DAYS.name)
    }
    var goalEnabled by rememberSaveable(kind.name) { mutableStateOf(false) }
    var goalAmount by rememberSaveable(kind.name) { mutableStateOf("") }
    var goalUnitName by rememberSaveable(kind.name) {
        mutableStateOf(DisplayFormat.DAYS.name)
    }

    val displayFormat = DisplayFormat.valueOf(displayFormatName)
    val goalUnit = DisplayFormat.valueOf(goalUnitName)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                modifier = Modifier.semantics { heading() },
                text = stringResource(
                    R.string.create_tracker_title,
                    trackerKindLabel(kind),
                ),
                style = MaterialTheme.typography.headlineMedium,
            )

            SectionCard {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("title-field"),
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.title_label)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    enabled = !isSaving,
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.note_label)) },
                    minLines = 3,
                    shape = MaterialTheme.shapes.medium,
                    enabled = !isSaving,
                )
            }

            SectionCard {
                StartEditorFields(
                    startDateTime = startDateTime,
                    onStartDateTimeChange = {
                        startDateTime = it
                        startInputErrors = emptyList()
                    },
                    startZoneId = startZoneId,
                    onStartZoneIdChange = {
                        startZoneId = it
                        startInputErrors = emptyList()
                    },
                    startInputErrors = startInputErrors,
                    enabled = !isSaving,
                    onUseNow = {
                        val currentZoneId = ZoneId.systemDefault().id
                        startZoneId = currentZoneId
                        startDateTime = TrackerStartInput.format(clock.millis(), currentZoneId)
                        startInputErrors = emptyList()
                    },
                )
            }

            SectionCard {
                FormatSelector(
                    title = stringResource(R.string.display_format_label),
                    selected = displayFormat,
                    enabled = !isSaving,
                    onSelect = { displayFormatName = it.name },
                )
            }

            if (kind == TrackerKind.STREAK) {
                SectionCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = goalEnabled,
                                enabled = !isSaving,
                                role = Role.Switch,
                                onValueChange = { goalEnabled = it },
                            )
                            .semantics(mergeDescendants = true) {},
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.goal_label),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = stringResource(R.string.goal_optional_description),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Switch(
                            modifier = Modifier.clearAndSetSemantics {},
                            checked = goalEnabled,
                            onCheckedChange = null,
                            enabled = !isSaving,
                        )
                    }

                    if (goalEnabled) {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = goalAmount,
                            onValueChange = { goalAmount = it.filter(Char::isDigit) },
                            label = { Text(stringResource(R.string.goal_amount_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            enabled = !isSaving,
                        )
                        FormatSelector(
                            title = stringResource(R.string.goal_unit_label),
                            selected = goalUnit,
                            enabled = !isSaving,
                            onSelect = { goalUnitName = it.name },
                        )
                    }
                }
            }

            EditorStatus(
                validationErrors = validationErrors,
                saveFailed = saveFailed,
            )

            EditorActions(
                isSaving = isSaving,
                onCancel = onCancel,
                onSave = {
                    when (val start = TrackerStartInput.resolve(startDateTime, startZoneId)) {
                        is TrackerStartResolution.Invalid -> {
                            startInputErrors = start.errors
                        }

                        is TrackerStartResolution.Valid -> {
                            startInputErrors = emptyList()
                            onSave(
                                TrackerDraft(
                                    title = title,
                                    note = note,
                                    kind = kind,
                                    startEpochMs = start.start.epochMs,
                                    startZoneId = start.start.zoneId,
                                    displayFormat = displayFormat,
                                    goalAmount = if (kind == TrackerKind.STREAK && goalEnabled) {
                                        goalAmount.toIntOrNull() ?: 0
                                    } else {
                                        null
                                    },
                                    goalUnit = if (kind == TrackerKind.STREAK && goalEnabled) {
                                        goalUnit
                                    } else {
                                        null
                                    },
                                )
                            )
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun EditTrackerScreen(
    aggregate: TrackerAggregate,
    clock: Clock,
    validationErrors: List<String>,
    saveFailed: Boolean,
    isSaving: Boolean,
    onCancel: () -> Unit,
    onSave: (TrackerDraft) -> Unit,
) {
    val tracker = aggregate.tracker
    val currentPeriod = aggregate.periods.single { it.endEpochMs == null }
    var title by rememberSaveable(tracker.id) { mutableStateOf(tracker.title) }
    var note by rememberSaveable(tracker.id) { mutableStateOf(tracker.note.orEmpty()) }
    var startDateTime by rememberSaveable(tracker.id) {
        mutableStateOf(
            TrackerStartInput.format(
                epochMs = currentPeriod.startEpochMs,
                zoneId = currentPeriod.startZoneId,
            )
        )
    }
    var startZoneId by rememberSaveable(tracker.id) {
        mutableStateOf(currentPeriod.startZoneId)
    }
    var startInputErrors by remember { mutableStateOf(emptyList<String>()) }
    var displayFormatName by rememberSaveable(tracker.id) {
        mutableStateOf(tracker.defaultDisplayFormat.name)
    }
    val displayFormat = DisplayFormat.valueOf(displayFormatName)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                modifier = Modifier.semantics { heading() },
                text = stringResource(R.string.edit_tracker_title, trackerKindLabel(tracker.kind)),
                style = MaterialTheme.typography.headlineMedium,
            )

            SectionCard {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("title-field"),
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.title_label)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    enabled = !isSaving,
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.note_label)) },
                    minLines = 3,
                    shape = MaterialTheme.shapes.medium,
                    enabled = !isSaving,
                )
            }

            SectionCard {
                StartEditorFields(
                    startDateTime = startDateTime,
                    onStartDateTimeChange = {
                        startDateTime = it
                        startInputErrors = emptyList()
                    },
                    startZoneId = startZoneId,
                    onStartZoneIdChange = {
                        startZoneId = it
                        startInputErrors = emptyList()
                    },
                    startInputErrors = startInputErrors,
                    enabled = !isSaving,
                    onUseNow = {
                        val currentZoneId = ZoneId.systemDefault().id
                        startZoneId = currentZoneId
                        startDateTime = TrackerStartInput.format(clock.millis(), currentZoneId)
                        startInputErrors = emptyList()
                    },
                )

                if (aggregate.periods.any { it.endEpochMs != null }) {
                    Text(
                        text = stringResource(R.string.edit_history_boundary),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            SectionCard {
                FormatSelector(
                    title = stringResource(R.string.display_format_label),
                    selected = displayFormat,
                    enabled = !isSaving,
                    onSelect = { displayFormatName = it.name },
                )
            }

            aggregate.goal?.let { goal ->
                SectionCard {
                    Text(
                        text = stringResource(R.string.goal_label),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(
                            R.string.goal_summary,
                            goal.targetAmount,
                            displayFormatLabel(goal.targetUnit),
                        ),
                        color = MaterialTheme.colorScheme.tertiary,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            }

            EditorStatus(
                validationErrors = validationErrors,
                saveFailed = saveFailed,
            )

            EditorActions(
                isSaving = isSaving,
                onCancel = onCancel,
                onSave = {
                    when (val start = TrackerStartInput.resolve(startDateTime, startZoneId)) {
                        is TrackerStartResolution.Invalid -> {
                            startInputErrors = start.errors
                        }

                        is TrackerStartResolution.Valid -> {
                            startInputErrors = emptyList()
                            onSave(
                                TrackerDraft(
                                    title = title,
                                    note = note,
                                    kind = tracker.kind,
                                    startEpochMs = start.start.epochMs,
                                    startZoneId = start.start.zoneId,
                                    displayFormat = displayFormat,
                                )
                            )
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun StartEditorFields(
    startDateTime: String,
    onStartDateTimeChange: (String) -> Unit,
    startZoneId: String,
    onStartZoneIdChange: (String) -> Unit,
    startInputErrors: List<String>,
    enabled: Boolean,
    onUseNow: () -> Unit,
) {
    Text(
        text = stringResource(R.string.start_label),
        style = MaterialTheme.typography.titleMedium,
    )
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("start-date-time-field"),
        value = startDateTime,
        onValueChange = onStartDateTimeChange,
        label = { Text(stringResource(R.string.start_date_time_label)) },
        supportingText = { Text(TrackerStartInput.FORMAT_HINT) },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        enabled = enabled,
    )
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("start-zone-field"),
        value = startZoneId,
        onValueChange = onStartZoneIdChange,
        label = { Text(stringResource(R.string.start_zone_label)) },
        supportingText = { Text(stringResource(R.string.start_zone_hint)) },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        enabled = enabled,
    )
    TextButton(
        onClick = onUseNow,
        enabled = enabled,
    ) {
        Text(stringResource(R.string.use_now))
    }
    if (startInputErrors.isNotEmpty()) {
        Text(
            modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Assertive
            },
            text = startInputErrors.joinToString(separator = "\n"),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun EditorStatus(
    validationErrors: List<String>,
    saveFailed: Boolean,
) {
    if (validationErrors.isNotEmpty()) {
        Text(
            modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Assertive
            },
            text = validationErrors.joinToString(separator = "\n"),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
    }

    if (saveFailed) {
        Text(
            modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Assertive
            },
            text = stringResource(R.string.save_failed),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun EditorActions(
    isSaving: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
    ) {
        TextButton(
            onClick = onCancel,
            enabled = !isSaving,
        ) {
            Text(stringResource(R.string.cancel))
        }
        Button(
            onClick = onSave,
            enabled = !isSaving,
        ) {
            Text(
                if (isSaving) {
                    stringResource(R.string.saving)
                } else {
                    stringResource(R.string.save)
                }
            )
        }
    }
}

@Composable
private fun FormatSelector(
    title: String,
    selected: DisplayFormat,
    enabled: Boolean,
    onSelect: (DisplayFormat) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )

        DisplayFormat.entries.chunked(2).forEach { formats ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                formats.forEach { format ->
                    val isSelected = selected == format
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .selectable(
                                selected = isSelected,
                                enabled = enabled,
                                role = Role.RadioButton,
                                onClick = { onSelect(format) },
                            )
                            .semantics(mergeDescendants = true) {},
                        shape = MaterialTheme.shapes.medium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        contentColor = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                        ),
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                            text = displayFormatLabel(format),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun displayFormatLabel(
    format: DisplayFormat,
): String = when (format) {
    DisplayFormat.DAYS -> stringResource(R.string.format_days)
    DisplayFormat.WEEKS -> stringResource(R.string.format_weeks)
    DisplayFormat.MONTHS -> stringResource(R.string.format_months)
    DisplayFormat.YEARS -> stringResource(R.string.format_years)
}
