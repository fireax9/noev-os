package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val CordlyDarkColorScheme = darkColorScheme(
    primary = CordlyBlurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3942A3),
    onPrimaryContainer = CordlyTextPrimary,
    secondary = CordlyCyan,
    onSecondary = Color(0xFF082026),
    secondaryContainer = Color(0xFF153844),
    onSecondaryContainer = CordlyCyan,
    tertiary = CordlyEmerald,
    onTertiary = Color.White,
    background = CordlyCanvasBg,
    onBackground = CordlyTextPrimary,
    surface = CordlySurfaceDark,
    onSurface = CordlyTextPrimary,
    surfaceVariant = CordlyCardSurface,
    onSurfaceVariant = CordlyTextSecondary,
    surfaceContainerLowest = CordlyRailBg,
    surfaceContainerLow = CordlySurfaceDark,
    surfaceContainer = CordlyCanvasBg,
    surfaceContainerHigh = CordlyCardSurface,
    surfaceContainerHighest = CordlyElevatedSurface,
    outline = CordlyDivider,
    error = CordlyCrimson,
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
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CordlyDarkColorScheme,
        typography = CordlyTypography,
        shapes = CordlyShapes,
        content = content
    )
}
