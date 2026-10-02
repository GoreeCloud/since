package com.goreecloud.since

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goreecloud.since.data.preferences.DashboardSortPreference
import com.goreecloud.since.data.preferences.SincePreferencesRepository
import com.goreecloud.since.data.preferences.ThemePreference
import com.goreecloud.since.domain.model.DisplayFormat
import com.goreecloud.since.ui.SinceApp
import com.goreecloud.since.ui.SinceSetupWizard
import com.goreecloud.since.ui.theme.SinceTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val sinceApplication = application as SinceApplication

        setContent {
            val preferencesRepository = sinceApplication.preferencesRepository
            val themePreference by preferencesRepository
                .themePreference
                .collectAsStateWithLifecycle(initialValue = ThemePreference.SYSTEM)
            val defaultDisplayFormat by preferencesRepository
                .defaultDisplayFormat
                .collectAsStateWithLifecycle(initialValue = DisplayFormat.DAYS)
            val showSeconds by preferencesRepository
                .showSeconds
                .collectAsStateWithLifecycle(initialValue = true)
            val dashboardSort by preferencesRepository
                .dashboardSort
                .collectAsStateWithLifecycle(initialValue = DashboardSortPreference.MANUAL)
            val confirmReset by preferencesRepository
                .confirmReset
                .collectAsStateWithLifecycle(initialValue = true)
            val onboardingComplete by preferencesRepository
                .onboardingComplete
                .collectAsStateWithLifecycle(initialValue = false)
            val onboardingStep by preferencesRepository
                .onboardingStep
                .collectAsStateWithLifecycle(initialValue = 0)
            val contextualHintsEnabled by preferencesRepository
                .contextualHintsEnabled
                .collectAsStateWithLifecycle(initialValue = true)
            val homeContextualHintDismissed by preferencesRepository
                .homeContextualHintDismissed
                .collectAsStateWithLifecycle(initialValue = false)
            val systemDarkTheme = isSystemInDarkTheme()
            val scope = rememberCoroutineScope()
            var replaySetup by rememberSaveable { mutableStateOf(false) }
            var replayStep by rememberSaveable { mutableIntStateOf(0) }
            val darkTheme = when (themePreference) {
                ThemePreference.SYSTEM -> systemDarkTheme
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
            }

            SinceTheme(darkTheme = darkTheme) {
                if (!onboardingComplete || replaySetup) {
                    val currentStep = if (replaySetup) replayStep else onboardingStep
                    SinceSetupWizard(
                        currentStep = currentStep,
                        replay = replaySetup,
                        contextualHintsEnabled = contextualHintsEnabled,
                        onContextualHintsEnabledChange = { enabled ->
                            scope.launch {
                                preferencesRepository.setContextualHintsEnabled(enabled)
                            }
                        },
                        onStepChange = { step ->
                            if (replaySetup) {
                                replayStep = step
                            } else {
                                scope.launch {
                                    preferencesRepository.setOnboardingStep(step)
                                }
                            }
                        },
                        onFinish = {
                            if (replaySetup) {
                                replayStep = 0
                                replaySetup = false
                            } else {
                                scope.launch {
                                    preferencesRepository.completeOnboarding()
                                }
                            }
                        },
                        onExitReplay = {
                            replayStep = 0
                            replaySetup = false
                        },
                    )
                } else {
                    SinceApp(
                        repository = sinceApplication.trackerRepository,
                        clock = sinceApplication.clock,
                        themePreference = themePreference,
                        onThemePreferenceChange = { preference ->
                            scope.launch {
                                preferencesRepository.setThemePreference(preference)
                            }
                        },
                        defaultDisplayFormat = defaultDisplayFormat,
                        onDefaultDisplayFormatChange = { format ->
                            scope.launch {
                                preferencesRepository.setDefaultDisplayFormat(format)
                            }
                        },
                        showSeconds = showSeconds,
                        onShowSecondsChange = { enabled ->
                            scope.launch {
                                preferencesRepository.setShowSeconds(enabled)
                            }
                        },
                        dashboardSort = dashboardSort,
                        onDashboardSortChange = { preference ->
                            scope.launch {
                                preferencesRepository.setDashboardSort(preference)
                            }
                        },
                        confirmReset = confirmReset,
                        onConfirmResetChange = { enabled ->
                            scope.launch {
                                preferencesRepository.setConfirmReset(enabled)
                            }
                        },
                        contextualHintsEnabled = contextualHintsEnabled,
                        onContextualHintsEnabledChange = { enabled ->
                            scope.launch {
                                preferencesRepository.setContextualHintsEnabled(enabled)
                            }
                        },
                        homeContextualHintDismissed = homeContextualHintDismissed,
                        onHomeContextualHintDismissedChange = { dismissed ->
                            scope.launch {
                                preferencesRepository.setHomeContextualHintDismissed(dismissed)
                            }
                        },
                        onResetDismissedContextualHints = {
                            scope.launch {
                                preferencesRepository.resetDismissedContextualHints()
                            }
                        },
                        onReplaySetup = {
                            replayStep = 0
                            replaySetup = true
                        },
                    )
                }
            }
        }
    }
}
