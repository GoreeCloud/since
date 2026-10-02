package com.goreecloud.since.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.goreecloud.since.R
import com.goreecloud.since.data.preferences.SincePreferencesRepository

@Composable
internal fun SinceSetupWizard(
    currentStep: Int,
    replay: Boolean,
    contextualHintsEnabled: Boolean,
    onContextualHintsEnabledChange: (Boolean) -> Unit,
    onStepChange: (Int) -> Unit,
    onFinish: () -> Unit,
    onExitReplay: () -> Unit,
) {
    val step = currentStep.coerceIn(0, SincePreferencesRepository.ONBOARDING_STEP_COUNT - 1)
    val isLast = step == SincePreferencesRepository.ONBOARDING_STEP_COUNT - 1

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("since-setup"),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    modifier = Modifier.semantics { heading() },
                    text = stringResource(
                        if (replay) R.string.setup_review_title else R.string.setup_title
                    ),
                    style = MaterialTheme.typography.displaySmall,
                )
                Text(
                    text = stringResource(
                        R.string.setup_progress,
                        step + 1,
                        SincePreferencesRepository.ONBOARDING_STEP_COUNT,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
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
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        modifier = Modifier.semantics { heading() },
                        text = stringResource(
                            when (step) {
                                0 -> R.string.setup_role_title
                                1 -> R.string.setup_privacy_title
                                else -> R.string.setup_guidance_title
                            }
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = stringResource(
                            when (step) {
                                0 -> R.string.setup_role_body
                                1 -> R.string.setup_privacy_body
                                else -> R.string.setup_guidance_body
                            }
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    if (step == 2) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("setup-contextual-hints")
                                .toggleable(
                                    value = contextualHintsEnabled,
                                    role = Role.Switch,
                                    onValueChange = onContextualHintsEnabledChange,
                                )
                                .semantics(mergeDescendants = true) {}
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.contextual_hints),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = stringResource(R.string.contextual_hints_supporting),
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
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (step > 0) {
                    TextButton(
                        modifier = Modifier.testTag("setup-back"),
                        onClick = { onStepChange(step - 1) },
                    ) {
                        Text(stringResource(R.string.back))
                    }
                } else if (replay) {
                    TextButton(
                        modifier = Modifier.testTag("setup-return"),
                        onClick = onExitReplay,
                    ) {
                        Text(stringResource(R.string.setup_return))
                    }
                }

                Button(
                    modifier = Modifier.testTag(if (isLast) "setup-finish" else "setup-continue"),
                    onClick = {
                        if (isLast) {
                            onFinish()
                        } else {
                            onStepChange(step + 1)
                        }
                    },
                ) {
                    Text(
                        stringResource(
                            if (isLast) R.string.setup_finish else R.string.setup_continue
                        )
                    )
                }
            }
        }
    }
}
