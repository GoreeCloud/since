package com.goreecloud.since.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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
    background = Color(0xFFF5F8F7),
    onBackground = Color(0xFF172021),
    surface = Color(0xFFFBFDFC),
    onSurface = Color(0xFF172021),
    surfaceVariant = Color(0xFFE5ECEA),
    onSurfaceVariant = Color(0xFF465252),
    outline = Color(0xFF758180),
    outlineVariant = Color(0xFFC5CFCD),
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
    outline = Color(0xFF899493),
    outlineVariant = Color(0xFF3E4949),
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
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) SinceDarkColors else SinceLightColors,
        shapes = SinceShapes,
        content = content,
    )
}
