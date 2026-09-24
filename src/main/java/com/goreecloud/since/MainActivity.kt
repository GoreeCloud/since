package com.goreecloud.since

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goreecloud.since.data.preferences.ThemePreference
import com.goreecloud.since.ui.SinceApp
import com.goreecloud.since.ui.theme.SinceTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val sinceApplication = application as SinceApplication

        setContent {
            val themePreference by sinceApplication.preferencesRepository
                .themePreference
                .collectAsStateWithLifecycle(initialValue = ThemePreference.SYSTEM)
            val systemDarkTheme = isSystemInDarkTheme()
            val scope = rememberCoroutineScope()
            val darkTheme = when (themePreference) {
                ThemePreference.SYSTEM -> systemDarkTheme
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
            }

            SinceTheme(darkTheme = darkTheme) {
                SinceApp(
                    repository = sinceApplication.trackerRepository,
                    clock = sinceApplication.clock,
                    themePreference = themePreference,
                    onThemePreferenceChange = { preference ->
                        scope.launch {
                            sinceApplication.preferencesRepository.setThemePreference(preference)
                        }
                    },
                )
            }
        }
    }
}
