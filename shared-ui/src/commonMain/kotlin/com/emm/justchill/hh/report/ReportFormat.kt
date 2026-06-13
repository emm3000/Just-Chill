package com.emm.justchill.hh.report

import androidx.compose.ui.graphics.Color
import com.emm.justchill.core.theme.emmDarkColors
import com.emm.justchill.hh.shared.NumberFormatEs

internal fun formatSoles(cents: Long): String {
    val soles = cents.toDouble() / 100.0
    return "S/ ${NumberFormatEs.integerRounded(soles)}"
}

internal fun formatSolesWithDecimals(cents: Long): String {
    val soles = cents.toDouble() / 100.0
    return "S/ ${NumberFormatEs.decimal2(soles)}"
}

internal fun domainColorToUi(color: String): Color = when (color) {
    "green" -> emmDarkColors.catSage
    "blue" -> emmDarkColors.catSlate
    "purple" -> emmDarkColors.catMauve
    "orange" -> emmDarkColors.catOchre
    "red" -> emmDarkColors.catTerracotta
    "brown" -> emmDarkColors.catTerracotta
    "yellow" -> emmDarkColors.catOchre
    "teal" -> emmDarkColors.catSage
    "pink" -> emmDarkColors.catMauve
    "gray" -> emmDarkColors.catGraphite
    else -> emmDarkColors.catGraphite
}
