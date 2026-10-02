package com.goreecloud.since.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.goreecloud.since.domain.model.DisplayFormat
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class DashboardSortPreference {
    MANUAL,
    TITLE,
    NEWEST_START,
    OLDEST_START,
    LONGEST_CURRENT,
}

private val Context.sincePreferencesDataStore by preferencesDataStore(
    name = "since_preferences",
)

class SincePreferencesRepository(
    private val context: Context,
) {
    private val themePreferenceKey = stringPreferencesKey("theme_preference")
    private val defaultDisplayFormatKey = stringPreferencesKey("default_display_format")
    private val showSecondsKey = booleanPreferencesKey("show_seconds")
    private val dashboardSortKey = stringPreferencesKey("dashboard_sort")
    private val confirmResetKey = booleanPreferencesKey("confirm_reset")
    private val onboardingCompleteKey = booleanPreferencesKey("onboarding_complete")
    private val onboardingStepKey = intPreferencesKey("onboarding_step")
    private val contextualHintsEnabledKey = booleanPreferencesKey("contextual_hints_enabled")
    private val homeContextualHintDismissedKey =
        booleanPreferencesKey("home_contextual_hint_dismissed")

    private val preferences = context
        .sincePreferencesDataStore
        .data
        .catch { throwable ->
            if (throwable is IOException) {
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }

    private val upgradedInstallationWithoutOnboardingState: Boolean by lazy {
        runCatching {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.lastUpdateTime > packageInfo.firstInstallTime
        }.getOrDefault(false)
    }

    val themePreference: Flow<ThemePreference> = preferences
        .map { values ->
            values[themePreferenceKey]
                ?.let { stored -> runCatching { ThemePreference.valueOf(stored) }.getOrNull() }
                ?: ThemePreference.SYSTEM
        }
        .distinctUntilChanged()

    val defaultDisplayFormat: Flow<DisplayFormat> = preferences
        .map { values ->
            values[defaultDisplayFormatKey]
                ?.let { stored -> runCatching { DisplayFormat.valueOf(stored) }.getOrNull() }
                ?: DisplayFormat.DAYS
        }
        .distinctUntilChanged()

    val showSeconds: Flow<Boolean> = preferences
        .map { values -> values[showSecondsKey] ?: true }
        .distinctUntilChanged()

    val dashboardSort: Flow<DashboardSortPreference> = preferences
        .map { values ->
            values[dashboardSortKey]
                ?.let { stored ->
                    runCatching { DashboardSortPreference.valueOf(stored) }.getOrNull()
                }
                ?: DashboardSortPreference.MANUAL
        }
        .distinctUntilChanged()

    val confirmReset: Flow<Boolean> = preferences
        .map { values -> values[confirmResetKey] ?: true }
        .distinctUntilChanged()

    val onboardingComplete: Flow<Boolean> = preferences
        .map { values ->
            values[onboardingCompleteKey] ?: upgradedInstallationWithoutOnboardingState
        }
        .distinctUntilChanged()

    val onboardingStep: Flow<Int> = preferences
        .map { values ->
            values[onboardingStepKey]
                ?.coerceIn(0, ONBOARDING_STEP_COUNT - 1)
                ?: 0
        }
        .distinctUntilChanged()

    val contextualHintsEnabled: Flow<Boolean> = preferences
        .map { values -> values[contextualHintsEnabledKey] ?: true }
        .distinctUntilChanged()

    val homeContextualHintDismissed: Flow<Boolean> = preferences
        .map { values -> values[homeContextualHintDismissedKey] ?: false }
        .distinctUntilChanged()

    suspend fun setThemePreference(preference: ThemePreference) {
        context.sincePreferencesDataStore.edit { values ->
            values[themePreferenceKey] = preference.name
        }
    }

    suspend fun setDefaultDisplayFormat(format: DisplayFormat) {
        context.sincePreferencesDataStore.edit { values ->
            values[defaultDisplayFormatKey] = format.name
        }
    }

    suspend fun setShowSeconds(enabled: Boolean) {
        context.sincePreferencesDataStore.edit { values ->
            values[showSecondsKey] = enabled
        }
    }

    suspend fun setDashboardSort(preference: DashboardSortPreference) {
        context.sincePreferencesDataStore.edit { values ->
            values[dashboardSortKey] = preference.name
        }
    }

    suspend fun setConfirmReset(enabled: Boolean) {
        context.sincePreferencesDataStore.edit { values ->
            values[confirmResetKey] = enabled
        }
    }

    suspend fun setOnboardingStep(step: Int) {
        context.sincePreferencesDataStore.edit { values ->
            values[onboardingStepKey] = step.coerceIn(0, ONBOARDING_STEP_COUNT - 1)
        }
    }

    suspend fun completeOnboarding() {
        context.sincePreferencesDataStore.edit { values ->
            values[onboardingCompleteKey] = true
            values[onboardingStepKey] = 0
        }
    }

    suspend fun setContextualHintsEnabled(enabled: Boolean) {
        context.sincePreferencesDataStore.edit { values ->
            values[contextualHintsEnabledKey] = enabled
        }
    }

    suspend fun setHomeContextualHintDismissed(dismissed: Boolean) {
        context.sincePreferencesDataStore.edit { values ->
            values[homeContextualHintDismissedKey] = dismissed
        }
    }

    suspend fun resetDismissedContextualHints() {
        context.sincePreferencesDataStore.edit { values ->
            values[homeContextualHintDismissedKey] = false
        }
    }

    companion object {
        const val ONBOARDING_STEP_COUNT = 3
    }
}
