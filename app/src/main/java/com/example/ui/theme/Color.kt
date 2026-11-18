package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.example.ui.cordly.AppAccentColor
import com.example.ui.cordly.AppThemeMode

object CordlyThemeState {
    var themeMode by mutableStateOf(AppThemeMode.NIGHT)
    var accentColor by mutableStateOf(AppAccentColor.BLURPLE)

    val isDark: Boolean
        get() = themeMode == AppThemeMode.NIGHT
}

// Dynamic Night (AMOLED Black) / Day (Clean Light) & Custom App Color Tokens
val CordlyRailBg: Color
    get() = if (CordlyThemeState.isDark) Color(0xFF000000) else Color(0xFFEEF2F7)

val CordlySurfaceDark: Color
    get() = if (CordlyThemeState.isDark) Color(0xFF060608) else Color(0xFFF8FAFC)

val CordlyCanvasBg: Color
    get() = if (CordlyThemeState.isDark) Color(0xFF000000) else Color(0xFFFFFFFF)

val CordlyCardSurface: Color
    get() = if (CordlyThemeState.isDark) Color(0xFF0E0E13) else Color(0xFFF1F5F9)

val CordlyElevatedSurface: Color
    get() = if (CordlyThemeState.isDark) Color(0xFF171720) else Color(0xFFE2E8F0)

val CordlyDivider: Color
    get() = if (CordlyThemeState.isDark) Color(0xFF232330) else Color(0xFFCBD5E1)

val CordlyBlurple: Color
    get() = CordlyThemeState.accentColor.primary

val CordlyBlurpleBright: Color
    get() = CordlyThemeState.accentColor.primaryBright

val CordlyCyan: Color
    get() = if (CordlyThemeState.isDark) {
        CordlyThemeState.accentColor.darkSecondary
    } else {
        CordlyThemeState.accentColor.lightSecondary
    }

val CordlyEmerald: Color
    get() = if (CordlyThemeState.isDark) Color(0xFF23A55A) else Color(0xFF15803D)

val CordlyAmber: Color
    get() = if (CordlyThemeState.isDark) Color(0xFFF0B232) else Color(0xFFB45309)

val CordlyCrimson: Color
    get() = if (CordlyThemeState.isDark) Color(0xFFF23F43) else Color(0xFFDC2626)

val CordlyTextPrimary: Color
    get() = if (CordlyThemeState.isDark) Color(0xFFF7F8FA) else Color(0xFF0F172A)

val CordlyTextSecondary: Color
    get() = if (CordlyThemeState.isDark) Color(0xFFB5BAC1) else Color(0xFF475569)

val CordlyTextMuted: Color
    get() = if (CordlyThemeState.isDark) Color(0xFF80848E) else Color(0xFF64748B)

