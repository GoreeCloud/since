package com.goreecloud.since.ui

import android.content.Intent
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goreecloud.since.R
import com.goreecloud.since.data.preferences.DashboardSortPreference
import com.goreecloud.since.data.preferences.ThemePreference
import com.goreecloud.since.domain.model.DisplayFormat
import com.goreecloud.since.domain.model.TrackerAggregate
import com.goreecloud.since.domain.model.TrackerKind
import com.goreecloud.since.domain.portability.SinceExportJson
import com.goreecloud.since.domain.portability.SinceImportReviewJson
import com.goreecloud.since.domain.portability.SinceImportReviewResult
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
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SinceApp(
    repository: TrackerRepository,
    clock: Clock,
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    onThemePreferenceChange: (ThemePreference) -> Unit = {},
    defaultDisplayFormat: DisplayFormat = DisplayFormat.DAYS,
    onDefaultDisplayFormatChange: (DisplayFormat) -> Unit = {},
    showSeconds: Boolean = true,
    onShowSecondsChange: (Boolean) -> Unit = {},
    dashboardSort: DashboardSortPreference = DashboardSortPreference.MANUAL,
    onDashboardSortChange: (DashboardSortPreference) -> Unit = {},
    confirmReset: Boolean = true,
    onConfirmResetChange: (Boolean) -> Unit = {},
    contextualHintsEnabled: Boolean = true,
    onContextualHintsEnabledChange: (Boolean) -> Unit = {},
    homeContextualHintDismissed: Boolean = false,
    onHomeContextualHintDismissedChange: (Boolean) -> Unit = {},
    onResetDismissedContextualHints: () -> Unit = {},
    onReplaySetup: () -> Unit = {},
) {
    val aggregates by repository
        .observeActiveTrackerAggregates()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val archivedAggregates by repository
        .observeArchivedTrackerAggregates()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    val validator = remember(clock) { TrackerDraftValidator(clock) }
    val context = LocalContext.current
    var isExportingData by remember { mutableStateOf(false) }
    var exportStatus by remember { mutableStateOf<SinceExportStatus?>(null) }
    var isReviewingImport by remember { mutableStateOf(false) }
    var importReviewResult by remember { mutableStateOf<SinceImportReviewResult?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) {
            isExportingData = false
        } else {
            val exportedAtEpochMs = clock.millis()
            val snapshot = aggregates + archivedAggregates
            scope.launch {
                val succeeded = withContext(Dispatchers.IO) {
                    runCatching {
                        val payload = SinceExportJson.encode(
                            aggregates = snapshot,
                            exportedAtEpochMs = exportedAtEpochMs,
                        )
                        val output = context.contentResolver.openOutputStream(uri, "wt")
                            ?: error("Selected export destination could not be opened")
                        output.bufferedWriter(Charsets.UTF_8).use { writer ->
                            writer.write(payload)
                        }
                    }.isSuccess
                }
                exportStatus = if (succeeded) {
                    SinceExportStatus.SUCCESS
                } else {
                    SinceExportStatus.FAILURE
                }
                isExportingData = false
            }
        }
    }

    val importReviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) {
            isReviewingImport = false
        } else {
            scope.launch {
                val review = withContext(Dispatchers.IO) {
                    runCatching {
                        val input = context.contentResolver.openInputStream(uri)
                            ?: error("Selected import file could not be opened")
                        val payloadBytes = input.use { stream ->
                            val output = java.io.ByteArrayOutputStream()
                            val buffer = ByteArray(8 * 1024)
                            var total = 0
                            while (true) {
                                val read = stream.read(buffer)
                                if (read < 0) break
                                total += read
                                check(total <= SinceImportReviewJson.MAX_IMPORT_BYTES)
                                output.write(buffer, 0, read)
                            }
                            output.toByteArray()
                        }
                        val decoder = Charsets.UTF_8.newDecoder()
                            .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                            .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                        val payload = decoder
                            .decode(java.nio.ByteBuffer.wrap(payloadBytes))
                            .toString()
                        SinceImportReviewJson.review(payload)
                    }.getOrDefault(SinceImportReviewResult.Invalid)
                }
                importReviewResult = review
                isReviewingImport = false
            }
        }
    }

    var showTypeChooser by rememberSaveable { mutableStateOf(false) }
    var editorKindName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedTrackerId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingTrackerId by rememberSaveable { mutableStateOf<String?>(null) }
    var validationErrors by remember { mutableStateOf(emptyList<String>()) }
    var saveFailed by rememberSaveable { mutableStateOf(false) }
    var detailUpdateFailed by rememberSaveable { mutableStateOf(false) }
    var goalUpdateFailed by rememberSaveable { mutableStateOf(false) }
    var resetFailed by rememberSaveable { mutableStateOf(false) }
    var archiveFailed by rememberSaveable { mutableStateOf(false) }
    var restoreFailedTrackerId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteFailedTrackerId by rememberSaveable { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var isGoalSaving by remember { mutableStateOf(false) }
    var isResetting by remember { mutableStateOf(false) }
    var isArchiving by remember { mutableStateOf(false) }
    var restoringTrackerId by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingTrackerId by rememberSaveable { mutableStateOf<String?>(null) }
    var historyTrackerId by rememberSaveable { mutableStateOf<String?>(null) }
    var topLevelDestinationName by rememberSaveable {
        mutableStateOf(TopLevelDestination.HOME.name)
    }
    val topLevelDestination = runCatching {
        TopLevelDestination.valueOf(topLevelDestinationName)
    }.getOrDefault(TopLevelDestination.HOME)
    val historyConflictMessage = stringResource(R.string.edit_history_conflict)

    val editorKind = editorKindName?.let { runCatching { TrackerKind.valueOf(it) }.getOrNull() }
    if (editorKind != null) {
        CreateTrackerScreen(
            kind = editorKind,
            clock = clock,
            initialDisplayFormat = defaultDisplayFormat,
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
    val historyAggregate = historyTrackerId?.let { trackerId ->
        aggregates.firstOrNull { it.tracker.id == trackerId }
    }

    if (historyAggregate != null && historyAggregate.tracker.kind == TrackerKind.STREAK) {
        StreakHistoryScreen(
            aggregate = historyAggregate,
            clock = clock,
            showSeconds = showSeconds,
            onBack = { historyTrackerId = null },
        )
        return
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
            showSeconds = showSeconds,
            confirmReset = confirmReset,
            updateFailed = detailUpdateFailed,
            goalUpdateFailed = goalUpdateFailed,
            resetFailed = resetFailed,
            archiveFailed = archiveFailed,
            isGoalSaving = isGoalSaving,
            isResetting = isResetting,
            isArchiving = isArchiving,
            onBack = {
                selectedTrackerId = null
                editingTrackerId = null
                historyTrackerId = null
                detailUpdateFailed = false
                goalUpdateFailed = false
                resetFailed = false
                archiveFailed = false
            },
            onEdit = {
                validationErrors = emptyList()
                saveFailed = false
                editingTrackerId = selectedAggregate.tracker.id
            },
            onOpenHistory = {
                historyTrackerId = selectedAggregate.tracker.id
            },
            onArchive = {
                archiveFailed = false
                isArchiving = true
                scope.launch {
                    val archived = runCatching {
                        repository.archiveTracker(selectedAggregate.tracker.id)
                    }.getOrNull()
                    if (archived == null) {
                        archiveFailed = true
                    } else {
                        selectedTrackerId = null
                        editingTrackerId = null
                        historyTrackerId = null
                        detailUpdateFailed = false
                        goalUpdateFailed = false
                        resetFailed = false
                    }
                    isArchiving = false
                }
            },
            onResetStreak = { resetEpochMs, resetZoneId, reason, note ->
                resetFailed = false
                isResetting = true
                scope.launch {
                    val updated = runCatching {
                        repository.resetStreak(
                            trackerId = selectedAggregate.tracker.id,
                            resetEpochMs = resetEpochMs,
                            resetZoneId = resetZoneId,
                            reason = reason,
                            note = note,
                        )
                    }.getOrNull()
                    resetFailed = updated == null
                    isResetting = false
                }
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
        topLevelDestinationName = TopLevelDestination.HOME.name
        selectedTrackerId = null
        showTypeChooser = true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            SinceTopLevelNavigationBar(
                selected = topLevelDestination,
                onSelect = { destination ->
                    topLevelDestinationName = destination.name
                },
            )
        },
        floatingActionButton = {
            if (
                topLevelDestination == TopLevelDestination.HOME &&
                aggregates.isNotEmpty()
            ) {
                ExtendedFloatingActionButton(
                    onClick = onAddTracker,
                    shape = MaterialTheme.shapes.large,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    content = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                modifier = Modifier.clearAndSetSemantics {},
                                text = "+",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(stringResource(R.string.add_tracker))
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        when (topLevelDestination) {
            TopLevelDestination.HOME -> Dashboard(
                innerPadding = innerPadding,
                aggregates = aggregates,
                clock = clock,
                showSeconds = showSeconds,
                dashboardSort = dashboardSort,
                onDashboardSortChange = onDashboardSortChange,
                contextualHintsEnabled = contextualHintsEnabled,
                homeContextualHintDismissed = homeContextualHintDismissed,
                onDismissHomeContextualHint = {
                    onHomeContextualHintDismissedChange(true)
                },
                onAddTracker = onAddTracker,
                onOpenTracker = { trackerId ->
                    detailUpdateFailed = false
                    goalUpdateFailed = false
                    archiveFailed = false
                    selectedTrackerId = trackerId
                },
            )

            TopLevelDestination.ACHIEVEMENTS -> AchievementsScreen(
                innerPadding = innerPadding,
                aggregates = aggregates,
                clock = clock,
            )

            TopLevelDestination.SETTINGS -> SettingsScreen(
                innerPadding = innerPadding,
                themePreference = themePreference,
                onThemePreferenceChange = onThemePreferenceChange,
                defaultDisplayFormat = defaultDisplayFormat,
                onDefaultDisplayFormatChange = onDefaultDisplayFormatChange,
                showSeconds = showSeconds,
                onShowSecondsChange = onShowSecondsChange,
                dashboardSort = dashboardSort,
                onDashboardSortChange = onDashboardSortChange,
                confirmReset = confirmReset,
                onConfirmResetChange = onConfirmResetChange,
                archivedTrackers = archivedAggregates,
                restoringTrackerId = restoringTrackerId,
                restoreFailedTrackerId = restoreFailedTrackerId,
                deletingTrackerId = deletingTrackerId,
                deleteFailedTrackerId = deleteFailedTrackerId,
                onRestoreTracker = { trackerId ->
                    restoreFailedTrackerId = null
                    deleteFailedTrackerId = null
                    restoringTrackerId = trackerId
                    scope.launch {
                        val restored = runCatching {
                            repository.restoreTracker(trackerId)
                        }.getOrNull()
                        if (restored == null) {
                            restoreFailedTrackerId = trackerId
                        }
                        restoringTrackerId = null
                    }
                },
                onDeleteArchivedTracker = { trackerId ->
                    restoreFailedTrackerId = null
                    deleteFailedTrackerId = null
                    deletingTrackerId = trackerId
                    scope.launch {
                        val deleted = runCatching {
                            repository.deleteArchivedTracker(trackerId)
                        }.getOrDefault(false)
                        if (!deleted) {
                            deleteFailedTrackerId = trackerId
                        }
                        deletingTrackerId = null
                    }
                },
                isExportingData = isExportingData,
                exportStatus = exportStatus,
                onExportData = {
                    if (!isExportingData && !isReviewingImport) {
                        exportStatus = null
                        isExportingData = true
                        runCatching {
                            exportLauncher.launch(SinceExportJson.fileName(clock.millis()))
                        }.onFailure {
                            isExportingData = false
                            exportStatus = SinceExportStatus.FAILURE
                        }
                    }
                },
                isReviewingImport = isReviewingImport,
                importReviewResult = importReviewResult,
                currentTrackerIds = (aggregates + archivedAggregates)
                    .mapTo(linkedSetOf()) { it.tracker.id },
                onReviewImport = {
                    if (!isReviewingImport && !isExportingData) {
                        importReviewResult = null
                        isReviewingImport = true
                        runCatching {
                            importReviewLauncher.launch(
                                arrayOf("application/json", "text/json", "text/plain"),
                            )
                        }.onFailure {
                            isReviewingImport = false
                            importReviewResult = SinceImportReviewResult.Invalid
                        }
                    }
                },
                contextualHintsEnabled = contextualHintsEnabled,
                onContextualHintsEnabledChange = onContextualHintsEnabledChange,
                onResetDismissedContextualHints = onResetDismissedContextualHints,
                onReplaySetup = onReplaySetup,
            )
        }
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
    showSeconds: Boolean,
    dashboardSort: DashboardSortPreference,
    onDashboardSortChange: (DashboardSortPreference) -> Unit,
    contextualHintsEnabled: Boolean,
    homeContextualHintDismissed: Boolean,
    onDismissHomeContextualHint: () -> Unit,
    onAddTracker: () -> Unit,
    onOpenTracker: (String) -> Unit,
) {
    val dashboardTick by rememberElapsedTick(
        clock = clock,
        key = "dashboard",
        showSeconds = showSeconds,
    )
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val visibleAggregates = remember(aggregates, searchQuery, dashboardSort) {
        SinceDashboardQuery.apply(
            aggregates = aggregates,
            query = searchQuery,
            sort = dashboardSort,
        )
    }
    val summary = remember(aggregates, dashboardTick, clock) {
        calculateDashboardSummary(
            aggregates = aggregates,
            clock = clock,
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .testTag("dashboard-list"),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 18.dp,
            end = 20.dp,
            bottom = if (aggregates.isEmpty()) 32.dp else 116.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            DashboardHeroHeader()
        }

        if (aggregates.isEmpty()) {
            item {
                DashboardEmptyState(onAddTracker = onAddTracker)
            }
        } else {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard-search"),
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(stringResource(R.string.dashboard_search)) },
                        leadingIcon = { DashboardSearchGlyph() },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                    )
                    DashboardSortControls(
                        selected = dashboardSort,
                        onSelect = onDashboardSortChange,
                    )
                    if (visibleAggregates.isEmpty()) {
                        Text(
                            modifier = Modifier.testTag("dashboard-no-matches"),
                            text = stringResource(R.string.dashboard_no_matches),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }

            item {
                DashboardSummaryRow(summary = summary)
            }

            items(
                items = visibleAggregates,
                key = { it.tracker.id },
            ) { aggregate ->
                TrackerCard(
                    aggregate = aggregate,
                    clock = clock,
                    tick = dashboardTick,
                    showSeconds = showSeconds,
                    onClick = { onOpenTracker(aggregate.tracker.id) },
                )
            }

            if (contextualHintsEnabled && !homeContextualHintDismissed) {
                item {
                    DashboardContextualHint(
                        onDismiss = onDismissHomeContextualHint,
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardContextualHint(
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home-contextual-hint"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(
                start = 16.dp,
                top = 10.dp,
                end = 12.dp,
                bottom = 10.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.home_contextual_hint),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(
                modifier = Modifier.testTag("home-contextual-hint-dismiss"),
                onClick = onDismiss,
            ) {
                Text(stringResource(R.string.dismiss_hint))
            }
        }
    }
}

@Composable
private fun DashboardHeroHeader() {
    val fontScale = LocalDensity.current.fontScale
    val largeText = fontScale >= 1.5f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp),
    ) {
        if (!largeText) {
            DashboardLandscape(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(width = 190.dp, height = 112.dp),
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(if (largeText) 1f else 0.79f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                modifier = Modifier.semantics { heading() },
                text = stringResource(R.string.dashboard_title),
                color = MaterialTheme.colorScheme.onBackground,
                style = if (largeText) {
                    MaterialTheme.typography.headlineLarge
                } else {
                    MaterialTheme.typography.displayLarge
                },
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.dashboard_empty_message),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun DashboardLandscape(
    modifier: Modifier = Modifier,
) {
    val backMountain = MaterialTheme.colorScheme.secondaryContainer
    val frontMountain = MaterialTheme.colorScheme.primaryContainer
    val lowHill = MaterialTheme.colorScheme.surfaceContainerHigh
    val sun = MaterialTheme.colorScheme.tertiaryContainer
    val tree = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        drawCircle(
            color = sun.copy(alpha = 0.56f),
            radius = width * 0.12f,
            center = Offset(width * 0.80f, height * 0.19f),
        )

        val rear = Path().apply {
            moveTo(width * 0.12f, height * 0.76f)
            lineTo(width * 0.42f, height * 0.32f)
            lineTo(width * 0.60f, height * 0.58f)
            lineTo(width * 0.74f, height * 0.43f)
            lineTo(width, height * 0.72f)
            lineTo(width, height)
            lineTo(width * 0.12f, height)
            close()
        }
        drawPath(rear, backMountain.copy(alpha = 0.82f))

        val front = Path().apply {
            moveTo(0f, height * 0.82f)
            lineTo(width * 0.28f, height * 0.56f)
            lineTo(width * 0.50f, height * 0.75f)
            lineTo(width * 0.72f, height * 0.61f)
            lineTo(width, height * 0.82f)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(front, frontMountain.copy(alpha = 0.92f))

        val foreground = Path().apply {
            moveTo(0f, height * 0.93f)
            cubicTo(
                width * 0.24f,
                height * 0.78f,
                width * 0.56f,
                height * 0.82f,
                width,
                height * 0.95f,
            )
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(foreground, lowHill)

        fun drawPine(centerX: Float, baseY: Float, scale: Float) {
            val pine = Path().apply {
                moveTo(centerX, baseY - 38f * scale)
                lineTo(centerX - 11f * scale, baseY - 13f * scale)
                lineTo(centerX - 4f * scale, baseY - 13f * scale)
                lineTo(centerX - 14f * scale, baseY + 7f * scale)
                lineTo(centerX + 14f * scale, baseY + 7f * scale)
                lineTo(centerX + 4f * scale, baseY - 13f * scale)
                lineTo(centerX + 11f * scale, baseY - 13f * scale)
                close()
            }
            drawPath(pine, tree.copy(alpha = 0.84f))
            drawLine(
                color = tree,
                start = Offset(centerX, baseY + 5f * scale),
                end = Offset(centerX, baseY + 16f * scale),
                strokeWidth = 2.2f * scale,
                cap = StrokeCap.Round,
            )
        }

        drawPine(width * 0.70f, height * 0.78f, 0.78f)
        drawPine(width * 0.83f, height * 0.72f, 0.95f)
        drawPine(width * 0.91f, height * 0.80f, 0.66f)
    }
}

@Composable
private fun DashboardSearchGlyph() {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = Modifier.size(24.dp)) {
        val strokeWidth = 2.2.dp.toPx()
        val center = Offset(size.width * 0.42f, size.height * 0.42f)
        val radius = size.minDimension * 0.27f
        drawCircle(
            color = color,
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth),
        )
        drawLine(
            color = color,
            start = Offset(center.x + radius * 0.72f, center.y + radius * 0.72f),
            end = Offset(size.width * 0.82f, size.height * 0.82f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
internal fun DashboardSortControls(
    selected: DashboardSortPreference,
    onSelect: (DashboardSortPreference) -> Unit,
) {
    val options = listOf(
        DashboardSortPreference.MANUAL to R.string.dashboard_sort_manual,
        DashboardSortPreference.TITLE to R.string.dashboard_sort_name,
        DashboardSortPreference.NEWEST_START to R.string.dashboard_sort_newest_start,
        DashboardSortPreference.OLDEST_START to R.string.dashboard_sort_oldest_start,
        DashboardSortPreference.LONGEST_CURRENT to R.string.dashboard_sort_longest_current,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .testTag("dashboard-sort-row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (preference, labelRes) ->
            FilterChip(
                modifier = Modifier
                    .heightIn(min = 46.dp)
                    .testTag("dashboard-sort-" + preference.name.lowercase()),
                selected = selected == preference,
                onClick = { onSelect(preference) },
                shape = MaterialTheme.shapes.large,
                label = {
                    Text(
                        text = stringResource(labelRes),
                        textAlign = TextAlign.Center,
                        fontWeight = if (selected == preference) {
                            FontWeight.SemiBold
                        } else {
                            FontWeight.Normal
                        },
                    )
                },
            )
        }
    }
}

private data class DashboardSummaryUi(
    val totalTrackers: Int,
    val activeStreaks: Int,
    val longestStreakDays: Long,
)

private fun calculateDashboardSummary(
    aggregates: List<TrackerAggregate>,
    clock: Clock,
): DashboardSummaryUi {
    val streaks = aggregates.filter { it.tracker.kind == TrackerKind.STREAK }
    val engine = TimeEngine(clock)
    val now = clock.instant()
    val longestDays = streaks
        .flatMap { it.periods }
        .mapNotNull { period ->
            val end = period.endEpochMs?.let(Instant::ofEpochMilli) ?: now
            when (
                val elapsed = engine.elapsedBetween(
                    start = Instant.ofEpochMilli(period.startEpochMs),
                    end = end,
                    zone = ZoneId.of(period.startZoneId),
                    format = DisplayFormat.DAYS,
                )
            ) {
                ElapsedResult.ClockInconsistency -> null
                is ElapsedResult.Value -> elapsed.breakdown.days
            }
        }
        .maxOrNull()
        ?: 0L

    return DashboardSummaryUi(
        totalTrackers = aggregates.size,
        activeStreaks = streaks.size,
        longestStreakDays = longestDays,
    )
}

@Composable
private fun DashboardSummaryRow(
    summary: DashboardSummaryUi,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard-summary"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DashboardStatCard(
            modifier = Modifier
                .weight(1f)
                .testTag("dashboard-summary-total"),
            glyph = "▦",
            label = stringResource(R.string.dashboard_total_trackers),
            value = summary.totalTrackers.toString(),
        )
        DashboardStatCard(
            modifier = Modifier
                .weight(1f)
                .testTag("dashboard-summary-streaks"),
            glyph = "◆",
            label = stringResource(R.string.dashboard_active_streaks),
            value = summary.activeStreaks.toString(),
        )
        DashboardStatCard(
            modifier = Modifier
                .weight(1f)
                .testTag("dashboard-summary-longest"),
            glyph = "★",
            label = stringResource(R.string.dashboard_longest_streak),
            value = stringResource(
                R.string.dashboard_days_value,
                summary.longestStreakDays,
            ),
            emphasized = true,
        )
    }
}

@Composable
private fun DashboardStatCard(
    modifier: Modifier,
    glyph: String,
    label: String,
    value: String,
    emphasized: Boolean = false,
) {
    Surface(
        modifier = modifier.heightIn(min = 92.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Surface(
                modifier = Modifier.size(34.dp),
                shape = MaterialTheme.shapes.small,
                color = if (emphasized) {
                    MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
                contentColor = if (emphasized) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        modifier = Modifier.clearAndSetSemantics {},
                        text = glyph,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
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
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
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
                fontWeight = FontWeight.SemiBold,
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
    showSeconds: Boolean,
    onClick: () -> Unit,
) {
    val currentPeriod = aggregate.periods.single { it.endEpochMs == null }
    val compactWidth = LocalConfiguration.current.screenWidthDp < 360
    val floatingActionSafeEnd = if (compactWidth) 92.dp else 0.dp
    val elapsed = remember(aggregate, tick, clock) {
        TimeEngine(clock).elapsedSince(
            startEpochMs = currentPeriod.startEpochMs,
            zoneId = currentPeriod.startZoneId,
            format = aggregate.tracker.defaultDisplayFormat,
        )
    }
    val startedText = remember(currentPeriod.startEpochMs, currentPeriod.startZoneId) {
        DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .format(
                Instant
                    .ofEpochMilli(currentPeriod.startEpochMs)
                    .atZone(ZoneId.of(currentPeriod.startZoneId))
            )
    }
    val tip = stringResource(
        if (aggregate.tracker.kind == TrackerKind.STREAK) {
            R.string.dashboard_tip_streak
        } else {
            R.string.dashboard_tip_event
        }
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = floatingActionSafeEnd),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!compactWidth) {
                    Surface(
                        modifier = Modifier.size(58.dp),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                modifier = Modifier.clearAndSetSemantics {},
                                text = if (aggregate.tracker.kind == TrackerKind.STREAK) "↟" else "◇",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = aggregate.tracker.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(
                            R.string.dashboard_started,
                            startedText,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (compactWidth) {
                        Text(
                            text = trackerKindLabel(aggregate.tracker.kind),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                if (!compactWidth) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            text = trackerKindLabel(aggregate.tracker.kind),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.elapsed_label),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = elapsedSummary(elapsed, showSeconds),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    modifier = Modifier.clearAndSetSemantics {},
                    text = "›",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.displaySmall,
                )
            }

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
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
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

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier.clearAndSetSemantics {},
                        text = "✦",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = tip,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}


@Composable
private fun TrackerDetailsScreen(
    aggregate: TrackerAggregate,
    clock: Clock,
    showSeconds: Boolean,
    confirmReset: Boolean,
    updateFailed: Boolean,
    goalUpdateFailed: Boolean,
    resetFailed: Boolean,
    archiveFailed: Boolean,
    isGoalSaving: Boolean,
    isResetting: Boolean,
    isArchiving: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenHistory: () -> Unit,
    onArchive: () -> Unit,
    onResetStreak: (Long, String, String?, String?) -> Unit,
    onDisplayFormatChange: (DisplayFormat) -> Unit,
    onUpdateGoal: (Int, DisplayFormat) -> Unit,
    onRemoveGoal: () -> Unit,
) {
    val currentPeriod = aggregate.periods.single { it.endEpochMs == null }
    val tick by rememberElapsedTick(
        clock = clock,
        key = "details-" + aggregate.tracker.id,
        showSeconds = showSeconds,
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
    var showResetDialog by rememberSaveable(aggregate.tracker.id) { mutableStateOf(false) }
    var showArchiveDialog by rememberSaveable(aggregate.tracker.id) { mutableStateOf(false) }
    val closedPeriods = remember(aggregate.periods) {
        aggregate.periods.filter { it.endEpochMs != null }
    }
    val longestPeriod = remember(aggregate.periods, tick, clock) {
        val nowEpochMs = clock.millis()
        val timeEngine = TimeEngine(clock)
        aggregate.periods.maxWithOrNull(
            Comparator { first, second ->
                timeEngine.compareCalendarElapsed(
                    firstStart = Instant.ofEpochMilli(first.startEpochMs),
                    firstEnd = Instant.ofEpochMilli(first.endEpochMs ?: nowEpochMs),
                    firstZone = ZoneId.of(first.startZoneId),
                    secondStart = Instant.ofEpochMilli(second.startEpochMs),
                    secondEnd = Instant.ofEpochMilli(second.endEpochMs ?: nowEpochMs),
                    secondZone = ZoneId.of(second.startZoneId),
                )
            }
        )
    }
    val longestElapsed = longestPeriod?.let { period ->
        remember(period, aggregate.tracker.defaultDisplayFormat, tick, clock) {
            TimeEngine(clock).elapsedBetween(
                start = Instant.ofEpochMilli(period.startEpochMs),
                end = Instant.ofEpochMilli(period.endEpochMs ?: clock.millis()),
                zone = ZoneId.of(period.startZoneId),
                format = aggregate.tracker.defaultDisplayFormat,
            )
        }
    }
    val lastResetText = remember(closedPeriods) {
        closedPeriods
            .maxByOrNull { it.endEpochMs ?: Long.MIN_VALUE }
            ?.let { period ->
                val endEpochMs = checkNotNull(period.endEpochMs)
                val endZoneId = period.endZoneId ?: period.startZoneId
                DateTimeFormatter
                    .ofLocalizedDateTime(FormatStyle.MEDIUM)
                    .format(
                        Instant
                            .ofEpochMilli(endEpochMs)
                            .atZone(ZoneId.of(endZoneId))
                    )
            }
    }
    val context = LocalContext.current
    val shareSummary = stringResource(
        R.string.share_tracker_summary,
        aggregate.tracker.title,
        elapsedSummary(elapsed, showSeconds),
        startedOn,
    )
    val shareChooserTitle = stringResource(R.string.share_tracker_chooser)

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
        bottomBar = {
            if (aggregate.tracker.kind == TrackerKind.STREAK) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 3.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(
                            start = 16.dp,
                            top = 8.dp,
                            end = 16.dp,
                            bottom = 8.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (resetFailed) {
                            Text(
                                modifier = Modifier.semantics {
                                    liveRegion = LiveRegionMode.Assertive
                                },
                                text = stringResource(R.string.reset_failed),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        StreakDetailActionButtons(
                            stacked = LocalDensity.current.fontScale >= 1.5f,
                            isResetting = isResetting,
                            onOpenHistory = onOpenHistory,
                            onReset = { showResetDialog = true },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
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
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareSummary)
                            }
                            runCatching {
                                context.startActivity(
                                    Intent.createChooser(sendIntent, shareChooserTitle),
                                )
                            }
                        },
                    ) {
                        Text(stringResource(R.string.share_tracker))
                    }
                    TextButton(onClick = onEdit) {
                        Text(stringResource(R.string.edit_tracker))
                    }
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
                            text = elapsedSummary(elapsed, showSeconds),
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

            if (aggregate.tracker.kind == TrackerKind.STREAK) {
                SectionCard {
                    Text(
                        text = stringResource(R.string.statistics_label),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    DetailValueRow(
                        label = stringResource(R.string.current_streak_label),
                        value = elapsedSummary(elapsed, showSeconds),
                    )
                    DetailValueRow(
                        label = stringResource(R.string.longest_streak_label),
                        value = longestElapsed?.let { elapsedSummary(it, showSeconds) }
                            ?: stringResource(R.string.no_history_value),
                    )
                    DetailValueRow(
                        label = stringResource(R.string.reset_count_label),
                        value = closedPeriods.size.toString(),
                    )
                    if (lastResetText != null) {
                        DetailValueRow(
                            label = stringResource(R.string.last_reset_label),
                            value = lastResetText,
                        )
                    }
                }

                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.history_label),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = stringResource(
                                    if (closedPeriods.size == 1) {
                                        R.string.history_summary_one
                                    } else {
                                        R.string.history_summary_other
                                    },
                                    closedPeriods.size,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }

                    }
                }

                SectionCard {
                    Text(
                        text = stringResource(R.string.reset_streak_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.reset_streak_supporting),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )

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

            SectionCard {
                Text(
                    text = stringResource(R.string.archive_tracker),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.archive_tracker_supporting),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(
                    modifier = Modifier.testTag("archive-tracker"),
                    onClick = { showArchiveDialog = true },
                    enabled = !isArchiving,
                ) {
                    Text(stringResource(R.string.archive))
                }
                if (archiveFailed) {
                    Text(
                        modifier = Modifier.semantics {
                            liveRegion = LiveRegionMode.Assertive
                        },
                        text = stringResource(R.string.archive_failed),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }


        }
    }

    if (showArchiveDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isArchiving) showArchiveDialog = false
            },
            title = { Text(stringResource(R.string.archive_tracker_title)) },
            text = { Text(stringResource(R.string.archive_tracker_message)) },
            confirmButton = {
                TextButton(
                    modifier = Modifier.testTag("confirm-archive-tracker"),
                    onClick = {
                        showArchiveDialog = false
                        onArchive()
                    },
                    enabled = !isArchiving,
                ) {
                    Text(stringResource(R.string.archive))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showArchiveDialog = false },
                    enabled = !isArchiving,
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showResetDialog && aggregate.tracker.kind == TrackerKind.STREAK) {
        ResetStreakDialog(
            currentPeriod = currentPeriod,
            clock = clock,
            isSaving = isResetting,
            confirmReset = confirmReset,
            onDismiss = { showResetDialog = false },
            onConfirm = { resetEpochMs, resetZoneId, reason, note ->
                onResetStreak(resetEpochMs, resetZoneId, reason, note)
                showResetDialog = false
            },
        )
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
private fun DetailValueRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            modifier = Modifier.weight(1f),
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun StreakHistoryScreen(
    aggregate: TrackerAggregate,
    clock: Clock,
    showSeconds: Boolean,
    onBack: () -> Unit,
) {
    val currentPeriod = aggregate.periods.single { it.endEpochMs == null }
    val orderedPeriods = remember(aggregate.periods) {
        listOf(currentPeriod) +
            aggregate.periods
                .filter { it.endEpochMs != null }
                .sortedByDescending { it.sequence }
    }
    val tick by rememberElapsedTick(
        clock = clock,
        key = "history-" + aggregate.tracker.id,
        showSeconds = showSeconds,
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("history-screen"),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = 16.dp,
                end = 20.dp,
                bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.back))
                    }
                    Text(
                        modifier = Modifier.semantics { heading() },
                        text = stringResource(R.string.history_title, aggregate.tracker.title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = stringResource(
                            if (orderedPeriods.count { it.endEpochMs != null } == 1) {
                                R.string.history_completed_count_one
                            } else {
                                R.string.history_completed_count_other
                            },
                            orderedPeriods.count { it.endEpochMs != null },
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            items(
                items = orderedPeriods,
                key = { it.id },
            ) { period ->
                val isCurrent = period.endEpochMs == null
                val endEpochMs = period.endEpochMs ?: tick
                val duration = remember(
                    period,
                    endEpochMs,
                    aggregate.tracker.defaultDisplayFormat,
                    clock,
                ) {
                    TimeEngine(clock).elapsedBetween(
                        start = Instant.ofEpochMilli(period.startEpochMs),
                        end = Instant.ofEpochMilli(endEpochMs),
                        zone = ZoneId.of(period.startZoneId),
                        format = aggregate.tracker.defaultDisplayFormat,
                    )
                }
                val startText = remember(period.startEpochMs, period.startZoneId) {
                    DateTimeFormatter
                        .ofLocalizedDateTime(FormatStyle.MEDIUM)
                        .format(
                            Instant
                                .ofEpochMilli(period.startEpochMs)
                                .atZone(ZoneId.of(period.startZoneId))
                        )
                }
                val endText = period.endEpochMs?.let { completedAt ->
                    val zoneId = period.endZoneId ?: period.startZoneId
                    remember(completedAt, zoneId) {
                        DateTimeFormatter
                            .ofLocalizedDateTime(FormatStyle.MEDIUM)
                            .format(
                                Instant
                                    .ofEpochMilli(completedAt)
                                    .atZone(ZoneId.of(zoneId))
                            )
                    }
                }

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
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (isCurrent) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                        ) {
                            Text(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                text = if (isCurrent) {
                                    stringResource(R.string.history_current)
                                } else {
                                    stringResource(
                                        R.string.history_period_number,
                                        period.sequence + 1,
                                    )
                                },
                                color = if (isCurrent) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }

                        DetailValueRow(
                            label = stringResource(R.string.history_started),
                            value = startText,
                        )
                        if (endText != null) {
                            DetailValueRow(
                                label = stringResource(R.string.history_ended),
                                value = endText,
                            )
                        }
                        DetailValueRow(
                            label = stringResource(R.string.history_duration),
                            value = elapsedSummary(duration, showSeconds),
                        )
                        period.resetReason?.let { reason ->
                            DetailValueRow(
                                label = stringResource(R.string.reset_reason_label),
                                value = reason,
                            )
                        }
                        if (period.resetNote != null) {
                            DetailValueRow(
                                label = stringResource(R.string.reset_note_label),
                                value = stringResource(R.string.reset_note_saved),
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class PendingResetRequest(
    val epochMs: Long,
    val zoneId: String,
    val reason: String?,
    val note: String?,
)

@Composable
private fun ResetStreakDialog(
    currentPeriod: com.goreecloud.since.domain.model.TrackerPeriod,
    clock: Clock,
    isSaving: Boolean,
    confirmReset: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Long, String, String?, String?) -> Unit,
) {
    val defaultZoneId = ZoneId.systemDefault().id
    var resetDateTime by rememberSaveable(currentPeriod.id) {
        mutableStateOf(TrackerStartInput.format(clock.millis(), defaultZoneId))
    }
    var resetZoneId by rememberSaveable(currentPeriod.id) {
        mutableStateOf(defaultZoneId)
    }
    var reason by rememberSaveable(currentPeriod.id) { mutableStateOf("") }
    var note by rememberSaveable(currentPeriod.id) { mutableStateOf("") }
    var inputErrors by remember { mutableStateOf(emptyList<String>()) }

    var pendingReset by remember(currentPeriod.id) {
        mutableStateOf<PendingResetRequest?>(null)
    }

    val beforeStartError = stringResource(R.string.reset_before_start_error)
    val futureError = stringResource(R.string.reset_future_error)
    val reasonLengthError = stringResource(R.string.reset_reason_length_error)
    val noteLengthError = stringResource(R.string.reset_note_length_error)

    pendingReset?.let { request ->
        AlertDialog(
            onDismissRequest = {
                if (!isSaving) pendingReset = null
            },
            title = {
                Text(
                    modifier = Modifier.semantics { heading() },
                    text = stringResource(R.string.reset_confirmation_title),
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.reset_confirmation_message),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    modifier = Modifier.testTag("confirm-reset-streak"),
                    onClick = {
                        onConfirm(
                            request.epochMs,
                            request.zoneId,
                            request.reason,
                            request.note,
                        )
                    },
                    enabled = !isSaving,
                ) {
                    Text(
                        if (isSaving) {
                            stringResource(R.string.resetting)
                        } else {
                            stringResource(R.string.reset_streak)
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(
                    modifier = Modifier.testTag("back-from-reset-confirmation"),
                    onClick = { pendingReset = null },
                    enabled = !isSaving,
                ) {
                    Text(stringResource(R.string.back))
                }
            },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                modifier = Modifier.semantics { heading() },
                text = stringResource(R.string.reset_streak_title),
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.reset_history_explanation),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )

                StartEditorFields(
                    clock = clock,
                    startDateTime = resetDateTime,
                    onStartDateTimeChange = {
                        resetDateTime = it
                        inputErrors = emptyList()
                    },
                    startZoneId = resetZoneId,
                    onStartZoneIdChange = {
                        resetZoneId = it
                        inputErrors = emptyList()
                    },
                    startInputErrors = inputErrors,
                    enabled = !isSaving,
                    onUseNow = {
                        val currentZoneId = ZoneId.systemDefault().id
                        resetZoneId = currentZoneId
                        resetDateTime = TrackerStartInput.format(
                            clock.millis(),
                            currentZoneId,
                        )
                        inputErrors = emptyList()
                    },
                    headingRes = R.string.reset_time_label,
                    zoneHintRes = R.string.reset_zone_picker_hint,
                    useNowRes = R.string.reset_use_now,
                    testTagPrefix = "reset",
                )

                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reset-reason"),
                    value = reason,
                    onValueChange = {
                        reason = it
                        inputErrors = emptyList()
                    },
                    label = { Text(stringResource(R.string.reset_reason_label)) },
                    supportingText = {
                        Text(stringResource(R.string.reset_reason_optional))
                    },
                    singleLine = true,
                    enabled = !isSaving,
                )

                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reset-note"),
                    value = note,
                    onValueChange = {
                        note = it
                        inputErrors = emptyList()
                    },
                    label = { Text(stringResource(R.string.reset_note_label)) },
                    supportingText = {
                        Text(stringResource(R.string.reset_note_optional))
                    },
                    minLines = 2,
                    enabled = !isSaving,
                )
            }
        },
        confirmButton = {
            TextButton(
                modifier = Modifier.testTag(
                    if (confirmReset) "review-reset-streak" else "confirm-reset-streak"
                ),
                onClick = {
                    val errors = mutableListOf<String>()
                    when (
                        val reset = TrackerStartInput.resolve(
                            resetDateTime,
                            resetZoneId,
                        )
                    ) {
                        is TrackerStartResolution.Invalid -> {
                            errors += reset.errors
                        }

                        is TrackerStartResolution.Valid -> {
                            if (reset.start.epochMs < currentPeriod.startEpochMs) {
                                errors += beforeStartError
                            }
                            if (reset.start.epochMs > clock.millis()) {
                                errors += futureError
                            }
                            if (reason.trim().length > 120) {
                                errors += reasonLengthError
                            }
                            if (note.trim().length > 2_000) {
                                errors += noteLengthError
                            }

                            if (errors.isEmpty()) {
                                if (confirmReset) {
                                    pendingReset = PendingResetRequest(
                                        epochMs = reset.start.epochMs,
                                        zoneId = reset.start.zoneId,
                                        reason = reason,
                                        note = note,
                                    )
                                } else {
                                    onConfirm(
                                        reset.start.epochMs,
                                        reset.start.zoneId,
                                        reason,
                                        note,
                                    )
                                }
                            }
                        }
                    }
                    inputErrors = errors
                },
                enabled = !isSaving,
            ) {
                Text(
                    if (isSaving) {
                        stringResource(R.string.resetting)
                    } else if (confirmReset) {
                        stringResource(R.string.reset_review)
                    } else {
                        stringResource(R.string.reset_streak)
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
private fun StreakDetailActionButtons(
    stacked: Boolean,
    isResetting: Boolean,
    onOpenHistory: () -> Unit,
    onReset: () -> Unit,
) {
    val actionMinHeight = if (stacked) 64.dp else 48.dp
    val actionMaxLines = if (stacked) 2 else 1
    val actionStyle = if (stacked) {
        MaterialTheme.typography.labelMedium
    } else {
        MaterialTheme.typography.labelLarge
    }
    val history: @Composable (Modifier) -> Unit = { modifier ->
        TextButton(
            modifier = modifier
                .heightIn(min = actionMinHeight)
                .testTag("open-history"),
            onClick = onOpenHistory,
        ) {
            Text(
                text = stringResource(R.string.view_history),
                style = actionStyle,
                maxLines = actionMaxLines,
                softWrap = stacked,
                textAlign = TextAlign.Center,
            )
        }
    }
    val reset: @Composable (Modifier) -> Unit = { modifier ->
        Button(
            modifier = modifier
                .heightIn(min = actionMinHeight)
                .testTag("reset-streak"),
            onClick = onReset,
            enabled = !isResetting,
            shape = MaterialTheme.shapes.large,
        ) {
            Text(
                text = if (isResetting) {
                    stringResource(R.string.resetting)
                } else {
                    stringResource(R.string.reset_streak)
                },
                style = actionStyle,
                maxLines = actionMaxLines,
                softWrap = stacked,
                textAlign = TextAlign.Center,
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        history(Modifier.weight(1f))
        reset(Modifier.weight(1f))
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
private fun rememberElapsedTick(
    clock: Clock,
    key: String,
    showSeconds: Boolean,
) = remember(clock, key, showSeconds) {
    flow {
        val intervalMs = if (showSeconds) 1_000L else 60_000L
        while (true) {
            val now = clock.millis()
            emit(now)
            val untilNextTick = intervalMs - (now % intervalMs)
            delay(untilNextTick.coerceIn(1_000L, intervalMs))
        }
    }
}.collectAsStateWithLifecycle(initialValue = clock.millis())

@Composable
private fun elapsedSummary(
    elapsed: ElapsedResult,
    showSeconds: Boolean,
): String = when (elapsed) {
    ElapsedResult.ClockInconsistency ->
        stringResource(R.string.clock_inconsistency)

    is ElapsedResult.Value -> {
        val breakdown = elapsed.breakdown
        when (breakdown.format) {
            DisplayFormat.DAYS ->
                if (showSeconds) {
                    stringResource(
                        R.string.elapsed_days_detail_seconds,
                        breakdown.days,
                        breakdown.hours,
                        breakdown.minutes,
                        breakdown.seconds,
                    )
                } else {
                    stringResource(
                        R.string.elapsed_days_detail,
                        breakdown.days,
                        breakdown.hours,
                        breakdown.minutes,
                    )
                }

            DisplayFormat.WEEKS ->
                if (showSeconds) {
                    stringResource(
                        R.string.elapsed_weeks_detail_seconds,
                        breakdown.weeks,
                        breakdown.days,
                        breakdown.hours,
                        breakdown.minutes,
                        breakdown.seconds,
                    )
                } else {
                    stringResource(
                        R.string.elapsed_weeks_detail,
                        breakdown.weeks,
                        breakdown.days,
                        breakdown.hours,
                        breakdown.minutes,
                    )
                }

            DisplayFormat.MONTHS ->
                if (showSeconds) {
                    stringResource(
                        R.string.elapsed_months_detail_seconds,
                        breakdown.months,
                        breakdown.days,
                        breakdown.hours,
                        breakdown.minutes,
                        breakdown.seconds,
                    )
                } else {
                    stringResource(
                        R.string.elapsed_months_detail,
                        breakdown.months,
                        breakdown.days,
                        breakdown.hours,
                        breakdown.minutes,
                    )
                }

            DisplayFormat.YEARS ->
                if (showSeconds) {
                    stringResource(
                        R.string.elapsed_years_detail_seconds,
                        breakdown.years,
                        breakdown.months,
                        breakdown.days,
                        breakdown.hours,
                        breakdown.minutes,
                        breakdown.seconds,
                    )
                } else {
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
    val largeText = LocalDensity.current.fontScale >= 1.5f
    val optionVerticalPadding = if (largeText) 12.dp else 16.dp

    AlertDialog(
        modifier = Modifier.testTag("tracker-type-dialog"),
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = stringResource(R.string.choose_tracker_type),
                style = if (largeText) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.headlineSmall
                },
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .testTag("tracker-type-list"),
                verticalArrangement = Arrangement.spacedBy(if (largeText) 8.dp else 12.dp),
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {}
                        .testTag("tracker-type-event"),
                    onClick = { onChoose(TrackerKind.EVENT) },
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = 18.dp,
                            vertical = optionVerticalPadding,
                        ),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.tracker_kind_event),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (!largeText) {
                            Text(
                                text = stringResource(R.string.event_description),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {}
                        .testTag("tracker-type-streak"),
                    onClick = { onChoose(TrackerKind.STREAK) },
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = 18.dp,
                            vertical = optionVerticalPadding,
                        ),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.tracker_kind_streak),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (!largeText) {
                            Text(
                                text = stringResource(R.string.streak_description),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
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
    initialDisplayFormat: DisplayFormat,
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
        mutableStateOf(initialDisplayFormat.name)
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
                    clock = clock,
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
                    clock = clock,
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

private val startEditorDateTimeFormatter =
    DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartEditorFields(
    clock: Clock,
    startDateTime: String,
    onStartDateTimeChange: (String) -> Unit,
    startZoneId: String,
    onStartZoneIdChange: (String) -> Unit,
    startInputErrors: List<String>,
    enabled: Boolean,
    onUseNow: () -> Unit,
    headingRes: Int = R.string.start_label,
    zoneHintRes: Int = R.string.start_zone_picker_hint,
    useNowRes: Int = R.string.use_now,
    testTagPrefix: String = "start",
) {
    val context = LocalContext.current
    val fallbackZone = remember(startZoneId) {
        runCatching { ZoneId.of(startZoneId.trim()) }
            .getOrDefault(ZoneId.systemDefault())
    }
    val startLocalDateTime = remember(startDateTime, fallbackZone, clock) {
        runCatching {
            LocalDateTime.parse(startDateTime.trim(), startEditorDateTimeFormatter)
        }.getOrElse {
            LocalDateTime.ofInstant(
                Instant.ofEpochMilli(clock.millis()),
                fallbackZone,
            )
        }
    }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showTimeZonePicker by rememberSaveable { mutableStateOf(false) }

    Text(
        text = stringResource(headingRes),
        style = MaterialTheme.typography.titleMedium,
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StartPickerField(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.start_date_label),
            value = DateTimeFormatter
                .ofLocalizedDate(FormatStyle.SHORT)
                .format(startLocalDateTime.toLocalDate()),
            testTag = testTagPrefix + "-date-picker",
            enabled = enabled,
            onClick = { showDatePicker = true },
        )
        StartPickerField(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.start_time_label),
            value = DateTimeFormatter
                .ofLocalizedTime(FormatStyle.SHORT)
                .format(startLocalDateTime.toLocalTime()),
            testTag = testTagPrefix + "-time-picker",
            enabled = enabled,
            onClick = { showTimePicker = true },
        )
    }

    StartPickerField(
        modifier = Modifier.fillMaxWidth(),
        label = stringResource(R.string.start_zone_label),
        value = startZoneId,
        testTag = testTagPrefix + "-zone-picker",
        enabled = enabled,
        onClick = { showTimeZonePicker = true },
    )
    Text(
        modifier = Modifier.padding(horizontal = 16.dp),
        text = stringResource(zoneHintRes),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
    )
    TextButton(
        onClick = onUseNow,
        enabled = enabled,
    ) {
        Text(stringResource(useNowRes))
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

    if (showTimeZonePicker) {
        TimeZonePickerDialog(
            selectedZoneId = startZoneId,
            referenceLocalDateTime = startLocalDateTime,
            onSelect = { selectedZoneId ->
                onStartZoneIdChange(selectedZoneId)
                showTimeZonePicker = false
            },
            onDismiss = { showTimeZonePicker = false },
        )
    }

    if (showDatePicker) {
        val initialDateMillis = startLocalDateTime
            .toLocalDate()
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDateMillis,
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedDateMillis = datePickerState.selectedDateMillis
                        if (selectedDateMillis != null) {
                            val selectedDate = Instant
                                .ofEpochMilli(selectedDateMillis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            onStartDateTimeChange(
                                startEditorDateTimeFormatter.format(
                                    LocalDateTime.of(
                                        selectedDate,
                                        startLocalDateTime.toLocalTime(),
                                    )
                                )
                            )
                        }
                        showDatePicker = false
                    },
                ) {
                    Text(stringResource(R.string.done))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) {
            DatePicker(
                state = datePickerState,
                modifier = Modifier.testTag("start-date-picker-dialog"),
                title = {
                    Text(
                        modifier = Modifier.padding(
                            start = 24.dp,
                            top = 16.dp,
                            end = 24.dp,
                        ),
                        text = stringResource(R.string.select_date_title),
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
                showModeToggle = false,
            )
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = startLocalDateTime.hour,
            initialMinute = startLocalDateTime.minute,
            is24Hour = DateFormat.is24HourFormat(context),
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = {
                Text(stringResource(R.string.select_time_title))
            },
            text = {
                TimePicker(
                    state = timePickerState,
                    modifier = Modifier.testTag("start-time-picker-dialog"),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onStartDateTimeChange(
                            startEditorDateTimeFormatter.format(
                                startLocalDateTime
                                    .withHour(timePickerState.hour)
                                    .withMinute(timePickerState.minute)
                            )
                        )
                        showTimePicker = false
                    },
                ) {
                    Text(stringResource(R.string.done))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

private data class TimeZoneOption(
    val zoneId: String,
    val offsetSeconds: Int,
    val offsetLabel: String,
)

@Composable
private fun TimeZonePickerDialog(
    selectedZoneId: String,
    referenceLocalDateTime: LocalDateTime,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val deviceZoneId = remember { ZoneId.systemDefault().id }
    val options = remember(referenceLocalDateTime) {
        ZoneId.getAvailableZoneIds()
            .map { zoneId ->
                val offset = ZoneId.of(zoneId).rules.getOffset(referenceLocalDateTime)
                TimeZoneOption(
                    zoneId = zoneId,
                    offsetSeconds = offset.totalSeconds,
                    offsetLabel = buildString {
                        append("UTC")
                        if (offset == ZoneOffset.UTC) {
                            append("+00:00")
                        } else {
                            append(offset.id)
                        }
                    },
                )
            }
            .sortedWith(
                compareBy<TimeZoneOption> { it.offsetSeconds }
                    .thenBy { it.zoneId }
            )
    }
    var query by remember { mutableStateOf("") }
    val filteredOptions = remember(query, options) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) {
            options
        } else {
            options.filter { option ->
                option.zoneId.contains(normalizedQuery, ignoreCase = true) ||
                    option.zoneId
                        .replace('_', ' ')
                        .contains(normalizedQuery, ignoreCase = true) ||
                    option.offsetLabel.contains(normalizedQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        modifier = Modifier.testTag("start-zone-picker-dialog"),
        onDismissRequest = onDismiss,
        title = {
            Text(
                modifier = Modifier.semantics { heading() },
                text = stringResource(R.string.select_time_zone_title),
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("start-zone-search"),
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.search_time_zones)) },
                    supportingText = {
                        Text(stringResource(R.string.search_time_zones_hint))
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                )

                if (query.isBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("device-time-zone-option")
                            .selectable(
                                selected = selectedZoneId == deviceZoneId,
                                role = Role.RadioButton,
                                onClick = { onSelect(deviceZoneId) },
                            )
                            .semantics(mergeDescendants = true) {}
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RadioButton(
                            modifier = Modifier.clearAndSetSemantics {},
                            selected = selectedZoneId == deviceZoneId,
                            onClick = null,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.use_device_time_zone),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = deviceZoneId,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                if (filteredOptions.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_time_zones_found),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                    ) {
                        items(
                            items = filteredOptions,
                            key = { it.zoneId },
                        ) { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("start-zone-option-${option.zoneId}")
                                    .selectable(
                                        selected = selectedZoneId == option.zoneId,
                                        role = Role.RadioButton,
                                        onClick = { onSelect(option.zoneId) },
                                    )
                                    .semantics(mergeDescendants = true) {}
                                    .padding(horizontal = 4.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                RadioButton(
                                    modifier = Modifier.clearAndSetSemantics {},
                                    selected = selectedZoneId == option.zoneId,
                                    onClick = null,
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = option.zoneId,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                    Text(
                                        text = option.offsetLabel,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
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
private fun StartPickerField(
    modifier: Modifier,
    label: String,
    value: String,
    testTag: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .testTag(testTag)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) {
                MaterialTheme.colorScheme.outline
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
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
internal fun FormatSelector(
    title: String,
    supporting: String? = null,
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
        if (supporting != null) {
            Text(
                text = supporting,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

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
internal fun displayFormatLabel(
    format: DisplayFormat,
): String = when (format) {
    DisplayFormat.DAYS -> stringResource(R.string.format_days)
    DisplayFormat.WEEKS -> stringResource(R.string.format_weeks)
    DisplayFormat.MONTHS -> stringResource(R.string.format_months)
    DisplayFormat.YEARS -> stringResource(R.string.format_years)
}
