package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.cordly.AppAccentColor
import com.example.ui.cordly.AppThemeMode

val LocalAppThemeMode = staticCompositionLocalOf { AppThemeMode.NIGHT }
val LocalOnAppThemeModeSelected = staticCompositionLocalOf<(AppThemeMode) -> Unit> { {} }
val LocalAppAccentColor = staticCompositionLocalOf { AppAccentColor.BLURPLE }
val LocalOnAppAccentColorSelected = staticCompositionLocalOf<(AppAccentColor) -> Unit> { {} }

private fun buildCordlyDarkColorScheme(accent: AppAccentColor): ColorScheme = darkColorScheme(
    primary = accent.primary,
    onPrimary = Color.White,
    primaryContainer = accent.primary.copy(alpha = 0.28f),
    onPrimaryContainer = Color(0xFFF7F8FA),
    secondary = accent.darkSecondary,
    onSecondary = Color(0xFF082026),
    secondaryContainer = accent.darkSecondary.copy(alpha = 0.18f),
    onSecondaryContainer = accent.darkSecondary,
    tertiary = Color(0xFF23A55A),
    onTertiary = Color.White,
    background = Color(0xFF000000),
    onBackground = Color(0xFFF7F8FA),
    surface = Color(0xFF060608),
    onSurface = Color(0xFFF7F8FA),
    surfaceVariant = Color(0xFF0E0E13),
    onSurfaceVariant = Color(0xFFB5BAC1),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF060608),
    surfaceContainer = Color(0xFF000000),
    surfaceContainerHigh = Color(0xFF0E0E13),
    surfaceContainerHighest = Color(0xFF171720),
    outline = Color(0xFF232330),
    error = Color(0xFFF23F43),
    onError = Color.White
)

private fun buildCordlyLightColorScheme(accent: AppAccentColor): ColorScheme = lightColorScheme(
    primary = accent.primary,
    onPrimary = Color.White,
    primaryContainer = accent.primary.copy(alpha = 0.16f),
    onPrimaryContainer = Color(0xFF0F172A),
    secondary = accent.lightSecondary,
    onSecondary = Color.White,
    secondaryContainer = accent.lightSecondary.copy(alpha = 0.14f),
    onSecondaryContainer = accent.lightSecondary,
    tertiary = Color(0xFF15803D),
    onTertiary = Color.White,
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFF8FAFC),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    surfaceContainerLowest = Color(0xFFEEF2F7),
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF1F5F9),
    surfaceContainerHighest = Color(0xFFE2E8F0),
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626),
    onError = Color.White
)

private val CordlyShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CordlyTheme(
    themeMode: AppThemeMode = AppThemeMode.NIGHT,
    accentColor: AppAccentColor = AppAccentColor.BLURPLE,
    content: @Composable () -> Unit
) {
    CordlyThemeState.themeMode = themeMode
    CordlyThemeState.accentColor = accentColor

    val colorScheme = if (themeMode == AppThemeMode.NIGHT) {
        buildCordlyDarkColorScheme(accentColor)
    } else {
        buildCordlyLightColorScheme(accentColor)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CordlyTypography,
        shapes = CordlyShapes,
        content = content
    )
}
