package com.emm.justchill.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Design system color tokens. Mirrors `docs/DESIGN_SYSTEM.md §2`.
 *
 * Read via `LocalEmmColors.current.<token>` inside any composable wrapped by `EmmTheme`.
 */
@Immutable
data class EmmColors(
    // Surface
    val bg: Color,
    val surface1: Color,
    val surface2: Color,
    val surface3: Color,
    val border: Color,
    val borderFocus: Color,

    // Text
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textDisabled: Color,
    val textOnAccent: Color,

    // Accent (single)
    val accent: Color,
    val accentMuted: Color,
    val accentFocus: Color,

    // Semantic (status — not for income/expense)
    val success: Color,
    val warning: Color,
    val danger: Color,
    val info: Color,

    // Category palette
    val catSlate: Color,
    val catSage: Color,
    val catTerracotta: Color,
    val catMauve: Color,
    val catOchre: Color,
    val catGraphite: Color,
)

internal val emmDarkColors: EmmColors = EmmColors(
    bg = Color(0xFF000000),
    surface1 = Color(0xFF0E0E0E),
    surface2 = Color(0xFF171717),
    surface3 = Color(0xFF1F1F1F),
    border = Color(0xFF262626),
    borderFocus = Color(0xFF3D3D3D),

    textPrimary = Color(0xFFFAFAFA),
    textSecondary = Color(0xFFA3A3A3),
    textTertiary = Color(0xFF6B6B6B),
    textDisabled = Color(0xFF404040),
    textOnAccent = Color(0xFF0A0A0A),

    accent = Color(0xFFE8E8E8),
    accentMuted = Color(0xFF737373),
    accentFocus = Color(0xFFFFFFFF),

    success = Color(0xFF7BB47B),
    warning = Color(0xFFD9A95C),
    danger = Color(0xFFC57070),
    info = Color(0xFF7B9EC5),

    catSlate = Color(0xFF5A6B7A),
    catSage = Color(0xFF6B8268),
    catTerracotta = Color(0xFFA87060),
    catMauve = Color(0xFF8A6F8C),
    catOchre = Color(0xFFB89A5F),
    catGraphite = Color(0xFF6E6E6E),
)

val LocalEmmColors = staticCompositionLocalOf<EmmColors> {
    error("EmmColors not provided. Wrap your composable in EmmTheme.")
}
