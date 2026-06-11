package com.emm.justchill.hh.report

import androidx.compose.ui.graphics.Color
import com.emm.justchill.core.theme.emmDarkColors
import java.text.NumberFormat
import java.util.Locale

internal fun formatSoles(cents: Long): String {
    val soles = cents.toDouble() / 100.0
    val nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-PE"))
    nf.minimumFractionDigits = 0
    nf.maximumFractionDigits = 0
    return "S/ ${nf.format(soles)}"
}

internal fun formatSolesWithDecimals(cents: Long): String {
    val soles = cents.toDouble() / 100.0
    val nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-PE"))
    nf.minimumFractionDigits = 2
    nf.maximumFractionDigits = 2
    return "S/ ${nf.format(soles)}"
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
