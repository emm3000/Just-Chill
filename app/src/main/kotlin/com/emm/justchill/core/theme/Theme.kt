package com.emm.justchill.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Material3 color scheme derived from [emmDarkColors]. This keeps Material components
 * (Scaffold, TopAppBar, etc.) consistent with the design system while screens migrate
 * to the explicit `LocalEmm*` tokens.
 */
private val materialDarkScheme = darkColorScheme(
    background = emmDarkColors.bg,
    onBackground = emmDarkColors.textPrimary,
    surface = emmDarkColors.surface1,
    onSurface = emmDarkColors.textPrimary,
    surfaceVariant = emmDarkColors.surface2,
    onSurfaceVariant = emmDarkColors.textSecondary,
    surfaceContainer = emmDarkColors.surface1,
    surfaceContainerLow = emmDarkColors.surface1,
    surfaceContainerHigh = emmDarkColors.surface2,
    surfaceContainerHighest = emmDarkColors.surface3,
    primary = emmDarkColors.accent,
    onPrimary = emmDarkColors.textOnAccent,
    primaryContainer = emmDarkColors.surface2,
    onPrimaryContainer = emmDarkColors.textPrimary,
    secondary = emmDarkColors.accentMuted,
    onSecondary = emmDarkColors.textPrimary,
    error = emmDarkColors.danger,
    onError = emmDarkColors.textPrimary,
    outline = emmDarkColors.border,
    outlineVariant = emmDarkColors.border,
)

@Composable
fun EmmTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalEmmColors provides emmDarkColors,
        LocalEmmType provides emmType,
        LocalEmmSpacing provides emmSpacing,
        LocalEmmRadii provides emmRadii,
    ) {
        MaterialTheme(
            colorScheme = materialDarkScheme,
            typography = AppTypography,
            content = content,
        )
    }
}
