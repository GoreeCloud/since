package com.goreecloud.since.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val SinceLightColors = lightColorScheme(
    primary = Color(0xFF0F656A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD5F0EF),
    onPrimaryContainer = Color(0xFF083E42),
    secondary = Color(0xFF4D6566),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCE8E7),
    onSecondaryContainer = Color(0xFF233D3E),
    tertiary = Color(0xFF956B1C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE6AF),
    onTertiaryContainer = Color(0xFF4C3600),
    background = Color(0xFFF4F8F7),
    onBackground = Color(0xFF172021),
    surface = Color(0xFFFBFDFC),
    onSurface = Color(0xFF172021),
    surfaceVariant = Color(0xFFE5ECEA),
    onSurfaceVariant = Color(0xFF465252),
    surfaceDim = Color(0xFFD7E0DE),
    surfaceBright = Color(0xFFFBFDFC),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F5F4),
    surfaceContainer = Color(0xFFE9F0EF),
    surfaceContainerHigh = Color(0xFFE2EAE8),
    surfaceContainerHighest = Color(0xFFD9E3E1),
    outline = Color(0xFF758180),
    outlineVariant = Color(0xFFC5CFCD),
    surfaceTint = Color(0xFF0F656A),
)

private val SinceDarkColors = darkColorScheme(
    primary = Color(0xFF91D5D5),
    onPrimary = Color(0xFF00373A),
    primaryContainer = Color(0xFF164F53),
    onPrimaryContainer = Color(0xFFC2EEEE),
    secondary = Color(0xFFB6CCCB),
    onSecondary = Color(0xFF213738),
    secondaryContainer = Color(0xFF374D4E),
    onSecondaryContainer = Color(0xFFD2E7E5),
    tertiary = Color(0xFFF0C46C),
    onTertiary = Color(0xFF432C00),
    tertiaryContainer = Color(0xFF624500),
    onTertiaryContainer = Color(0xFFFFE3A3),
    background = Color(0xFF0D1415),
    onBackground = Color(0xFFDDE5E3),
    surface = Color(0xFF11191A),
    onSurface = Color(0xFFDDE5E3),
    surfaceVariant = Color(0xFF293334),
    onSurfaceVariant = Color(0xFFC1CBC9),
    surfaceDim = Color(0xFF0D1415),
    surfaceBright = Color(0xFF323D3E),
    surfaceContainerLowest = Color(0xFF0B1112),
    surfaceContainerLow = Color(0xFF151E1F),
    surfaceContainer = Color(0xFF1A2425),
    surfaceContainerHigh = Color(0xFF202B2C),
    surfaceContainerHighest = Color(0xFF283334),
    outline = Color(0xFF899493),
    outlineVariant = Color(0xFF3E4949),
    surfaceTint = Color(0xFF91D5D5),
)

private val SinceShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/**
 * GoreeCloud Since visual theme.
 *
 * This maps the current Since Android surface onto the current Stable Glaze UI visual direction
 * while keeping consumer conformance evidence repository-local. It intentionally uses native
 * Material 3 primitives for Android accessibility and platform behavior.
 */
@Composable
fun SinceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) SinceDarkColors else SinceLightColors,
        shapes = SinceShapes,
    ) {
        SinceSystemBars(darkTheme = darkTheme)
        content()
    }
}

@Composable
private fun SinceSystemBars(darkTheme: Boolean) {
    val view = LocalView.current
    val lightBackground = SinceLightColors.background.toArgb()
    val darkBackground = SinceDarkColors.background.toArgb()

    SideEffect {
        val activity = view.context.findActivity() as? ComponentActivity ?: return@SideEffect

        // The Activity edge-to-edge helper owns system-bar icon appearance. Re-apply it with
        // the app-selected theme rather than the device theme so an explicit Since Light/Dark
        // preference cannot inherit the opposite system-icon style.
        activity.enableEdgeToEdge(
            statusBarStyle = if (darkTheme) {
                SystemBarStyle.dark(darkBackground)
            } else {
                SystemBarStyle.light(
                    scrim = lightBackground,
                    darkScrim = darkBackground,
                )
            },
            navigationBarStyle = SystemBarStyle.dark(darkBackground),
        )

        // Android 15+ ignores navigationBarColor for gesture navigation, but three-button
        // navigation still renders a real navigation surface. Keep that surface dark with
        // light navigation symbols in both Since themes so OEM/platform fallback behavior
        // cannot produce white controls on the light application background.
        activity.window.navigationBarColor = darkBackground
        activity.window.isNavigationBarContrastEnforced = true
        WindowCompat.getInsetsController(
            activity.window,
            activity.window.decorView,
        ).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = false
        }
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
