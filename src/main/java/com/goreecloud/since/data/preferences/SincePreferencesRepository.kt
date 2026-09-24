package com.goreecloud.since.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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

private val Context.sincePreferencesDataStore by preferencesDataStore(
    name = "since_preferences",
)

class SincePreferencesRepository(
    private val context: Context,
) {
    private val themePreferenceKey = stringPreferencesKey("theme_preference")

    val themePreference: Flow<ThemePreference> = context
        .sincePreferencesDataStore
        .data
        .catch { throwable ->
            if (throwable is IOException) {
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { preferences ->
            preferences[themePreferenceKey]
                ?.let { stored -> runCatching { ThemePreference.valueOf(stored) }.getOrNull() }
                ?: ThemePreference.SYSTEM
        }
        .distinctUntilChanged()

    suspend fun setThemePreference(preference: ThemePreference) {
        context.sincePreferencesDataStore.edit { preferences ->
            preferences[themePreferenceKey] = preference.name
        }
    }
}
