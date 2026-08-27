package com.emm.justchill.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class EmmColors(
    val bg: Color,
    val surface1: Color,
    val surface2: Color,
    val surface3: Color,
    val border: Color,
    val borderFocus: Color,

    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textDisabled: Color,
    val textOnAccent: Color,

    val accent: Color,
    val accentMuted: Color,
    val accentFocus: Color,

    val success: Color,
    val warning: Color,
    val danger: Color,
    val info: Color,

    val posMuted: Color,
    val negMuted: Color,

    val catSlate: Color,
    val catSage: Color,
    val catTerracotta: Color,
    val catMauve: Color,
    val catOchre: Color,
    val catGraphite: Color,
)

val emmDarkColors: EmmColors = EmmColors(
    bg = Color(0xFF191919),
    surface1 = Color(0xFF202020),
    surface2 = Color(0xFF262626),
    surface3 = Color(0xFF2D2D2D),
    border = Color(0xFF2E2E2E),
    borderFocus = Color(0xFF383838),

    textPrimary = Color(0xFFE6E6E6),
    textSecondary = Color(0xFFA8A8A8),
    textTertiary = Color(0xFF6E6E6E),
    textDisabled = Color(0xFF4A4A4A),
    textOnAccent = Color(0xFFFFFFFF),

    accent = Color(0xFFE07856),
    accentMuted = Color(0x24E07856),
    accentFocus = Color(0xFFEA8E70),

    success = Color(0xFF6FA876),
    warning = Color(0xFFB3935A),
    danger = Color(0xFFCB6A5C),
    info = Color(0xFF7C8B99),

    posMuted = Color(0x246FA876),
    negMuted = Color(0x24CB6A5C),

    catSlate = Color(0xFF7C8B99),
    catSage = Color(0xFF7FA075),
    catTerracotta = Color(0xFFC97A5C),
    catMauve = Color(0xFF9C7A95),
    catOchre = Color(0xFFB3935A),
    catGraphite = Color(0xFF7A7A7A),
)

val LocalEmmColors = staticCompositionLocalOf<EmmColors> {
    error("EmmColors not provided. Wrap your composable in EmmTheme.")
}
