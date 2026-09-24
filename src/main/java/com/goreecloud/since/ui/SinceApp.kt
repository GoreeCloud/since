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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
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
    var isSaving by remember { mutableStateOf(false) }
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
            onBack = {
                selectedTrackerId = null
                editingTrackerId = null
                detailUpdateFailed = false
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

    val dashboardTick by rememberMinuteTick(
        clock = clock,
        key = "dashboard",
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                modifier = Modifier.semantics { heading() },
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
                tick = dashboardTick,
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
            modifier = Modifier.semantics { heading() },
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
    onEdit: () -> Unit,
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

            Text(
                modifier = Modifier.semantics { heading() },
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
                    modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Assertive
                    },
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
                modifier = Modifier.semantics { heading() },
                text = stringResource(
                    R.string.create_tracker_title,
                    trackerKindLabel(kind),
                ),
                style = MaterialTheme.typography.headlineMedium,
            )

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("title-field"),
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

            Text(
                text = stringResource(R.string.icon_accent_deferred),
                style = MaterialTheme.typography.bodySmall,
            )

            FormatSelector(
                title = stringResource(R.string.display_format_label),
                selected = displayFormat,
                enabled = !isSaving,
                onSelect = { displayFormatName = it.name },
            )

            if (kind == TrackerKind.STREAK) {
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
                            style = MaterialTheme.typography.bodySmall,
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
                modifier = Modifier.semantics { heading() },
                text = stringResource(R.string.edit_tracker_title, trackerKindLabel(tracker.kind)),
                style = MaterialTheme.typography.headlineMedium,
            )

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("title-field"),
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
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Text(
                text = stringResource(R.string.icon_accent_deferred),
                style = MaterialTheme.typography.bodySmall,
            )

            FormatSelector(
                title = stringResource(R.string.display_format_label),
                selected = displayFormat,
                enabled = !isSaving,
                onSelect = { displayFormatName = it.name },
            )

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
                        text = stringResource(R.string.goal_edit_deferred),
                        style = MaterialTheme.typography.bodySmall,
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            enabled = enabled,
        )
        TextButton(
            onClick = onUseNow,
            enabled = enabled,
        ) {
            Text(stringResource(R.string.use_now))
        }
        Text(
            text = stringResource(R.string.dst_overlap_policy),
            style = MaterialTheme.typography.bodySmall,
        )
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
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        DisplayFormat.entries.forEach { format ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selected == format,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = { onSelect(format) },
                    )
                    .semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    modifier = Modifier.clearAndSetSemantics {},
                    selected = selected == format,
                    onClick = null,
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
