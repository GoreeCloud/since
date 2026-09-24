package com.goreecloud.since.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.stringResource
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
import com.goreecloud.since.domain.time.TimeEngine
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
    var validationErrors by remember { mutableStateOf(emptyList<String>()) }
    var saveFailed by rememberSaveable { mutableStateOf(false) }
    var detailUpdateFailed by rememberSaveable { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

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
    if (selectedAggregate != null) {
        TrackerDetailsScreen(
            aggregate = selectedAggregate,
            clock = clock,
            updateFailed = detailUpdateFailed,
            onBack = {
                selectedTrackerId = null
                detailUpdateFailed = false
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
        )
        return
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    selectedTrackerId = null
                    showTypeChooser = true
                },
                content = { Text(stringResource(R.string.add_tracker)) },
            )
        },
    ) { innerPadding ->
        Dashboard(
            innerPadding = innerPadding,
            aggregates = aggregates,
            clock = clock,
            onOpenTracker = { trackerId ->
                detailUpdateFailed = false
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
    onOpenTracker: (String) -> Unit,
) {
    if (aggregates.isEmpty()) {
        DashboardEmptyState(innerPadding)
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.dashboard_title),
                style = MaterialTheme.typography.headlineLarge,
            )
        }

        items(
            items = aggregates,
            key = { it.tracker.id },
        ) { aggregate ->
            TrackerCard(
                aggregate = aggregate,
                clock = clock,
                onClick = { onOpenTracker(aggregate.tracker.id) },
            )
        }
    }
}

@Composable
private fun DashboardEmptyState(
    innerPadding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.dashboard_title),
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            modifier = Modifier.padding(top = 12.dp),
            text = stringResource(R.string.dashboard_empty_message),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            modifier = Modifier.padding(top = 16.dp),
            text = stringResource(R.string.dashboard_empty_status),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun TrackerCard(
    aggregate: TrackerAggregate,
    clock: Clock,
    onClick: () -> Unit,
) {
    val currentPeriod = aggregate.periods.single { it.endEpochMs == null }
    val tick by rememberMinuteTick(
        clock = clock,
        key = aggregate.tracker.id,
    )
    val elapsed = remember(aggregate, tick, clock) {
        TimeEngine(clock).elapsedSince(
            startEpochMs = currentPeriod.startEpochMs,
            zoneId = currentPeriod.startZoneId,
            format = aggregate.tracker.defaultDisplayFormat,
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = aggregate.tracker.title,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = trackerKindLabel(aggregate.tracker.kind),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = elapsedSummary(elapsed),
                style = MaterialTheme.typography.headlineSmall,
            )
            aggregate.goal?.let { goal ->
                Text(
                    text = stringResource(
                        R.string.goal_summary,
                        goal.targetAmount,
                        displayFormatLabel(goal.targetUnit),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun TrackerDetailsScreen(
    aggregate: TrackerAggregate,
    clock: Clock,
    updateFailed: Boolean,
    onBack: () -> Unit,
    onDisplayFormatChange: (DisplayFormat) -> Unit,
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

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.back))
            }

            Text(
                text = aggregate.tracker.title,
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = trackerKindLabel(aggregate.tracker.kind),
                style = MaterialTheme.typography.labelLarge,
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.elapsed_label),
                    style = MaterialTheme.typography.titleMedium,
                )
                SelectionContainer {
                    Text(
                        text = elapsedSummary(elapsed),
                        style = MaterialTheme.typography.displaySmall,
                    )
                }
            }

            FormatSelector(
                title = stringResource(R.string.display_format_label),
                selected = aggregate.tracker.defaultDisplayFormat,
                enabled = true,
                onSelect = onDisplayFormatChange,
            )

            if (updateFailed) {
                Text(
                    text = stringResource(R.string.display_format_update_failed),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.started_on_label),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = startedOn,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = currentPeriod.startZoneId,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            aggregate.goal?.let { goal ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.goal_progress_deferred),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.note_label),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = aggregate.tracker.note ?: stringResource(R.string.no_note),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            Text(
                text = stringResource(R.string.details_development_boundary),
                style = MaterialTheme.typography.bodySmall,
            )
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
        title = { Text(stringResource(R.string.choose_tracker_type)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onChoose(TrackerKind.EVENT) },
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.tracker_kind_event))
                        Text(
                            text = stringResource(R.string.event_description),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onChoose(TrackerKind.STREAK) },
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.tracker_kind_streak))
                        Text(
                            text = stringResource(R.string.streak_description),
                            style = MaterialTheme.typography.bodySmall,
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
    var title by rememberSaveable(kind.name) { mutableStateOf("") }
    var note by rememberSaveable(kind.name) { mutableStateOf("") }
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
    val zoneId = ZoneId.systemDefault().id

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(
                    R.string.create_tracker_title,
                    when (kind) {
                        TrackerKind.EVENT -> stringResource(R.string.tracker_kind_event)
                        TrackerKind.STREAK -> stringResource(R.string.tracker_kind_streak)
                    },
                ),
                style = MaterialTheme.typography.headlineMedium,
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.title_label)) },
                singleLine = true,
                enabled = !isSaving,
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.note_label)) },
                minLines = 3,
                enabled = !isSaving,
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.start_label),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.start_now_value, zoneId),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.custom_start_development_boundary),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            FormatSelector(
                title = stringResource(R.string.display_format_label),
                selected = displayFormat,
                enabled = !isSaving,
                onSelect = { displayFormatName = it.name },
            )

            if (kind == TrackerKind.STREAK) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = stringResource(R.string.goal_label),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.goal_optional_description),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(
                        checked = goalEnabled,
                        onCheckedChange = { goalEnabled = it },
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

            if (validationErrors.isNotEmpty()) {
                Text(
                    text = validationErrors.joinToString(separator = "\n"),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (saveFailed) {
                Text(
                    text = stringResource(R.string.save_failed),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

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
                    onClick = {
                        onSave(
                            TrackerDraft(
                                title = title,
                                note = note,
                                kind = kind,
                                startEpochMs = clock.millis(),
                                startZoneId = zoneId,
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
            }
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
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        DisplayFormat.entries.forEach { format ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = selected == format,
                    onClick = { onSelect(format) },
                    enabled = enabled,
                )
                Text(
                    text = displayFormatLabel(format),
                    style = MaterialTheme.typography.bodyLarge,
                )
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
