package com.fitness.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.fitness.app.data.prefs.ThemeMode

private val LightColors = lightColorScheme(
    primary = Orange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCE),
    onPrimaryContainer = Color(0xFF3B0A00),
    secondary = Coral,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9DD),
    onSecondaryContainer = Color(0xFF400010),
    tertiary = Teal,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC4F1FB),
    onTertiaryContainer = Color(0xFF001F26),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    outline = LightOutline,
    outlineVariant = LightOutline
)

private val DarkColors = darkColorScheme(
    primary = OrangeLight,
    onPrimary = Color(0xFF4A1500),
    primaryContainer = Color(0xFF7A2A14),
    onPrimaryContainer = Color(0xFFFFDBCE),
    secondary = Coral,
    onSecondary = Color(0xFF5B0010),
    secondaryContainer = Color(0xFF7A2A2E),
    onSecondaryContainer = Color(0xFFFFD9DD),
    tertiary = Teal,
    onTertiary = Color(0xFF00363D),
    tertiaryContainer = Color(0xFF004F58),
    onTertiaryContainer = Color(0xFFC4F1FB),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    outline = DarkOutline,
    outlineVariant = DarkOutline
)

@Composable
fun FitnessTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
